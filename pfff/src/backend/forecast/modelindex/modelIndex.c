#include "postgres.h"
#include "forecast/modelindex/modelIndex.h"
#include "forecast/algorithm.h"
#include "access/skey.h"
#include "access/genam.h"
#include "access/heapam.h"
#include "catalog/pg_namespace.h"
#include "utils/fmgroids.h"
#include "utils/tqual.h"
#include "utils/rel.h"
#include "utils/date.h"
#include "utils/array.h"
#include "nodes/parsenodes.h"
#include "nodes/pg_list.h"
#include "utils/lsyscache.h"
#include "utils/syscache.h"
#include "catalog/catalog.h"
#include "catalog/indexing.h"
#include "utils/builtins.h"
#include "executor/spi.h"
#include "utils/memutils.h"
#include "nodes/print.h"
#include "nodes/pg_list.h"
#include "utils/nabstime.h"
#include "utils/array.h"
#include "forecast/modelindex/hashtable/hashtable.h"
#include "forecast/modelindex/hashtable/hashtable_itr.h"
#include "access/hash.h"
#include <sys/time.h>
#include "forecast/methods/forecastUtilitys.h"
#include "forecast/algorithm.h"
#include "nodes/nodeFuncs.h"



// global model tree structure
ModelTree 		*modelTree = NULL;

// memory context of model tree
MemoryContext 	modelContext = NULL;

// context before entering model matching and maintenance
MemoryContext	oldContext = NULL;

// ids for model and connection nodes (and, or)
int				condId;
int				modelId;

// Used to sort predicates for model matching
Condition 		**conditions;
Join 			**joins;
int				ccount;
int				jcount;

// EXPERIMENTS FOR THE PAPER "Indexing Forecast Models for Matching und Maintenance"
List				*completeModelList = NULL;
int					count;


// methods for b-tree
static unsigned int btreevalue(int key) {
	return key;
}
static unsigned int btreekeysize(int key) {
        return sizeof(int);
}
static unsigned int btreedatasize(void * data) {
        return sizeof(Predicate*);
}

// methods for hash table
DEFINE_HASHTABLE_INSERT(insert_some, int, struct Predicate);
DEFINE_HASHTABLE_SEARCH(search_some, int, struct Predicate);
DEFINE_HASHTABLE_REMOVE(remove_some, int, struct Predicate);
DEFINE_HASHTABLE_ITERATOR_SEARCH(search_itr_some, int);
static unsigned int
hashfromkey(int ky)
{
    return oid_hash(&ky, sizeof(ky));
}
static int
equalkeys(int k1, int k2)
{
    return (k1 == k2);
}


void
StartIndexing(void)
{			
	if (modelTree == NULL) {
		
		modelContext = AllocSetContextCreate(TopMemoryContext,
										  "model tree context",
										  ALLOCSET_DEFAULT_MINSIZE,
										  ALLOCSET_DEFAULT_INITSIZE,
										  ALLOCSET_DEFAULT_MAXSIZE);
		oldContext = MemoryContextSwitchTo(modelContext);
		
		modelTree = (ModelTree*) makeNode(ModelTree);
		modelTree->children = NULL;;
		modelTree->numModels = 0;
		condId = 0;
		modelId = 0;
		
		conditions = palloc(MAXCONDITIONS * sizeof(struct Condition*));
		ccount = 0;
		joins = palloc(MAXJOINS * sizeof(struct JoinNode*));
		jcount = 0;
	} 
	else 
	{
		oldContext = MemoryContextSwitchTo(modelContext);
	}
}



void
EndIndexing(void)
{
	int i;
	MemoryContextSwitchTo(oldContext);
	
	for (i=0; i<ccount; i++)
	{
		pfree(conditions[i]);
		conditions[i] = NULL;
	}
	ccount = 0;
	for (i=0; i<jcount; i++)
	{
		joins[i] = NULL;
	}
	jcount = 0;
}

MemoryContext 
getModelMemoryContext(void)
{
	return modelContext;
}

void
PrintDetails(void)
{
		FILE 		*file; 
	ListCell	*lc, *lc2,*lc3;
	// print memory context informationen
	MemoryContextStats(modelContext);
	printf("number of models: %i \n", modelTree->numModels);
	fflush(stdout);
	
	// create dot script

	file = fopen("/home/b1anchi/graph.txt", "w");
	if(file)
	{
		fprintf(file, "strict digraph g {\n");
			foreach(lc, modelTree->children)
			{
				TableNode *tb = (TableNode*) lfirst(lc);

				HeapTuple sourceTable = SearchSysCache(RELOID, ObjectIdGetDatum(tb->relid), 0, 0, 0);
				if (sourceTable != NULL) {
					Form_pg_class sourceTableDesc = (Form_pg_class) GETSTRUCT(sourceTable);
					fprintf(file, "N%p[label=%s, color=red] \n", tb, NameStr(sourceTableDesc->relname));
					ReleaseSysCache(sourceTable);
				} else {
					fprintf(file, "N%p[label=%i, color=red] \n", tb, tb->relid);
				}

				foreach(lc2, tb->children)
				{
					ColumnNode *co = (ColumnNode*) lfirst(lc2);
					HeapTuple attTuple = SearchSysCache(ATTNUM, ObjectIdGetDatum(tb->relid), Int16GetDatum(co->no), 0, 0);
					if (attTuple != NULL) {
						Form_pg_attribute attrDesc = (Form_pg_attribute) GETSTRUCT(attTuple);
						fprintf(file, "N%p[label=%s, color=blue] \n", co, NameStr(attrDesc->attname));
						ReleaseSysCache(attTuple);
					} else {
						fprintf(file, "N%p[label=%i, color=blue] \n", co, co->no);
					}
					fprintf(file, "N%p -> N%p \n", tb, co);

					if (co->equal != NULL)
					{
						struct hashtable_itr *itr = hashtable_iterator(co->equal);
						int i = 0;
					    if (hashtable_count(co->equal) > 0)
					    {
					        do {
					        	Predicate *ep = hashtable_iterator_value(itr);
					        	fprintf(file, "N%p[label=\"=%i\", color=green] \n", ep, ep->value);
								fprintf(file, "N%p -> N%p \n", co, ep);

								PrintModels(file, ep, ep->models);
								PrintConnections(file, ep, ep->connections);
					            i++;
			
					        } while (hashtable_iterator_advance(itr));
					        pfree(itr);
					    }
					}
					if (co->lessthan != NULL)
					{
						PrintBtree(file, co, co->lessthan, co->lessthan->root, 0);
					}
					if (co->greaterthan != NULL)
					{
						PrintBtree(file, co, co->greaterthan, co->greaterthan->root, 1);
					}
					foreach(lc3, co->joins)
					{
						JoinNode *jn = (JoinNode*) lfirst(lc3);
						fprintf(file, "N%p[label=JOIN, color=lightblue] \n", jn);
						fprintf(file, "N%p -> N%p \n", co, jn);
						
						PrintConnections(file, jn, jn->connections);
					}
				}
				
				PrintModels(file, tb, tb->models);
			}
			fprintf(file,"} \n");
			fclose(file);
	}

}


void
PrintConnections(FILE *file, void *source, List *connections)
{
	ListCell *lc, *lc2;
	foreach(lc, connections)
	{
		Connection *and = (Connection*) lfirst(lc);
		if (and->tp == 0)
			fprintf(file, "N%p[shape=diamond, label=AND] \n", and);
		else
			fprintf(file, "N%p[shape=diamond, label=OR] \n", and);
		fprintf(file,"N%p -> N%p \n", source, and);
		
		PrintConnections(file, and, and->connections);
		PrintModels(file, and, and->models);
	}
}


void
PrintModels(FILE *file, void *source, List *models)
{
	ListCell *lc;
	HeapTuple attTuple;
	foreach(lc, models) 
	{
		ModelNode *mn = lfirst(lc);
		
		// model
		fprintf(file, "N%p[shape=box, color=cyan, label=\"error=%.2f, timestamp=%i, forecast=%.2f \"] \n", mn, mn->evaluation, mn->timestamp, mn->forecast);
		fprintf(file,"N%p -> N%p \n", source, mn);
		
		// granularity
		fprintf(file, "GRANULARITY%i[label=\"granularity = %s\", color=green] \n", mn->granularity, getGranularityAsString(mn->granularity));
		fprintf(file, "N%p -> GRANULARITY%i [dir=back] \n", mn, mn->granularity);
				
		// time
		attTuple = SearchSysCache(ATTNUM, ObjectIdGetDatum(mn->timerel), Int16GetDatum(mn->time), 0, 0);
		if (attTuple != NULL) {
			Form_pg_attribute attrDesc = (Form_pg_attribute) GETSTRUCT(attTuple);
			fprintf(file, "GRANULARITY%iTIME%i[label=\"time= %s\", color=blue] \n", mn->granularity, mn->time, NameStr(attrDesc->attname));
			ReleaseSysCache(attTuple);
			attTuple = NULL;
		} else {
			fprintf(file, "GRANULARITY%iTIME%i[label=\"time= %i\", color=blue] \n", mn->granularity, mn->time, mn->time);
		}
		fprintf(file, "GRANULARITY%i -> GRANULARITY%iTIME%i [dir=back] \n", mn->granularity, mn->granularity, mn->time);
		
		// measure
		attTuple = SearchSysCache(ATTNUM, ObjectIdGetDatum(mn->measurerel), Int16GetDatum(mn->measure), 0, 0);
		if (attTuple != NULL) {
			Form_pg_attribute attrDesc = (Form_pg_attribute) GETSTRUCT(attTuple);
			fprintf(file, "GRANULARITY%iTIME%iMEASURE%i[label=\"measure= %s\", color=red] \n", mn->granularity, mn->time, mn->measure, NameStr(attrDesc->attname));
			ReleaseSysCache(attTuple);
			attTuple = NULL;
		} else {
			fprintf(file, "GRANULARITY%iTIME%iMEASURE%i[label=\"measure= %i\", color=red] \n", mn->granularity, mn->time, mn->measure, mn->measure);
		}
		fprintf(file, "GRANULARITY%iTIME%i -> GRANULARITY%iTIME%iMEASURE%i [dir=back] \n", mn->granularity, mn->time, mn->granularity, mn->time, mn->measure);
	}
}

void
PrintBtree(FILE *file, void *src, btree *btree, bt_node *node, int type)
{		
	int i = 0;
	unsigned int current_level;

	bt_node * head, * tail;
	bt_node * child;
	
	current_level = node->level;
	head = node;
	head->parent = src;
	tail = node;

	while(true) {
		if(head == NULL) {
			break;
		}
		if (head->level < current_level) {
			current_level = head->level;
			printf("\n");
		}
		PrintBtreeNode(btree, head, file, type);

		if(head->leaf == false) {	
			for(i = 0 ; i < head->nr_active + 1; i++) {
				child = head->children[i];
				child->parent = head;
				tail->next = child;
				tail = child;
				child->next = NULL;
			}
		}
		head = head->next;	
	}
}


void 
PrintBtreeNode(btree *btree, bt_node * node, FILE *file, int type) 
{	
	int i = 0;
	fprintf(file, "N%p[shape=Mrecord, color=green, label=\"", node);
	
	if (type == 0)
		fprintf(file, "<f%i> &lt;%i ", btree->value(node->key_vals[i]->key), btree->value(node->key_vals[i]->key));
	else
		fprintf(file, "<f%i> &gt;%i ", btree->value(node->key_vals[i]->key), btree->value(node->key_vals[i]->key));
	i++;
	
	while(i < node->nr_active) 
	{
		if (type == 0)
			fprintf(file, "|<f%i> &lt;%i ", btree->value(node->key_vals[i]->key), btree->value(node->key_vals[i]->key));
		else
			fprintf(file, "|<f%i> &gt;%i ", btree->value(node->key_vals[i]->key), btree->value(node->key_vals[i]->key));
		i++;
	}
	fprintf(file, "\"]\n");
	fprintf(file, "N%p -> N%p \n", node->parent, node);
	
	// now print models
	i=0;
	while(i < node->nr_active) 
	{
		PrintModels(file, node, ((Predicate*) node->key_vals[i]->val)->models);
		i++;
	}
}

void
AddModelToModelIndex(TableNode *parent, TargetEntry *measure, TargetEntry *time, short granularity, Oid aggType, int timestamp, double forecast, Model *model, ModelType forecastMethod, List *parameterList)
{
	
	
	ModelNode *modelNode = NULL;
	ListCell *lc;
	AlgorithmParameter *toCopy,*yeha;
	A_Const *resultConst;
	StartIndexing();
	modelNode = makeNode(ModelNode);
	modelNode->id = modelId++;
	modelNode->timerel = time->resorigtbl;
	modelNode->time = time->resorigcol;
	modelNode->timeType = (Oid) exprType((Node*) time->expr);
	modelNode->measurerel = measure->resorigtbl;
	modelNode->measure = measure->resorigcol;
	modelNode->measureType = (Oid) exprType((Node*) measure->expr);
	modelNode->granularity = granularity;
	modelNode->agg = aggType;
	modelNode->timestamp = timestamp;
	modelNode->forecast = forecast;
	modelNode->evaluation = 0;
	modelNode->ecount = 0;
	modelNode->eval = 0;
	modelNode->model = model;
	modelNode->sourcetext = NULL;
	modelNode->method = forecastMethod;
	modelNode->parameterList=NIL;
	
	foreach(lc, parameterList)
	{
		toCopy=(AlgorithmParameter*) lfirst(lc);
		yeha = makeNode(AlgorithmParameter);
		yeha->key=toCopy->key;
		yeha->type=toCopy->type;
		resultConst = makeNode(A_Const);
		resultConst->location=toCopy->value->location;
		resultConst->type=toCopy->value->type;
		Value *resultVal = makeNode(Value);
		resultVal->type=toCopy->value->val.type;
		resultVal->val.ival=toCopy->value->val.val.ival;	
		resultConst->val=*resultVal;
		yeha->value=resultConst;	
		modelNode->parameterList=lappend(modelNode->parameterList,yeha);
	}
	
	// count number of indexed models
	modelTree->numModels++;
	
	// index model
	parent->models = lappend(parent->models, modelNode);
	//PrintDetails();
	
	EndIndexing();
}


Node*
AddJoin(Oid tab1, AttrNumber col1, Oid tab2, AttrNumber col2)
{
	TableNode	*tabNode1=NULL, *tabNode2=NULL;
	ColumnNode	*colNode1=NULL, *colNode2=NULL;
	JoinNode	*joinNode=NULL;
	ListCell	*lc1, *lc2;
	
	// Seach if first table and column already exist 
	foreach(lc1, modelTree->children) {
		TableNode *temp = (TableNode*) lfirst(lc1);
		if (temp->relid == tab1) {
			tabNode1 = temp;
		} else if (temp->relid == tab2) {
			tabNode2 = temp;
		}
	}
	if (tabNode1 != NULL) {
		foreach(lc1, tabNode1->children) {
			ColumnNode *temp = (ColumnNode*) lfirst(lc1);
			if (temp->no == col1) {
				colNode1 = temp;
				break;
			}
		}
	} else {
		tabNode1 = (TableNode*) makeNode(TableNode);
		tabNode1->relid = tab1;
		modelTree->children = lappend(modelTree->children, tabNode1);
	}
	if (colNode1 == NULL) {
		colNode1 = (ColumnNode*) makeNode(ColumnNode);
		colNode1->no = col1;
		colNode1->parent = tabNode1;
		tabNode1->children = lappend(tabNode1->children, colNode1);
	}
	
	// Seach if second table and column already exist 
	if (tabNode2 != NULL) {
		foreach(lc1, tabNode2->children) {
			ColumnNode *temp = (ColumnNode*) lfirst(lc1);
			if (temp->no == col2) {
				colNode2 = temp;
				break;
			}
		}
	} else {
		tabNode2 = (TableNode*) makeNode(TableNode);
		tabNode2->relid = tab2;
		modelTree->children = lappend(modelTree->children, tabNode2);
	}
	if (colNode2 == NULL) {
		colNode2 = (ColumnNode*) makeNode(ColumnNode);
		colNode2->no = col2;
		colNode2->parent = tabNode2;
		tabNode2->children = lappend(tabNode2->children, colNode2);
	}
	
	// search existing join node
	foreach(lc1, colNode1->joins) {
		foreach(lc2, colNode2->joins) {
			JoinNode *temp1 = (JoinNode*) lfirst(lc1);
			JoinNode *temp2 = (JoinNode*) lfirst(lc2);
			if (temp1 == temp2)
			{
				joinNode = temp1;
				break;
			}
		}
	}
	if (joinNode == NULL) {
		joinNode = makeNode(JoinNode);
		
		if (tab1 < tab2)
		{
			joinNode->leftParent = colNode1;
			joinNode->rightParent = colNode2;
		}
		else
		{
			joinNode->leftParent = colNode2;
			joinNode->rightParent = colNode1;
		}
		colNode1->joins = lappend(colNode1->joins, joinNode);
		colNode2->joins = lappend(colNode2->joins, joinNode);
	}
	
	return (Node*) joinNode;
}

Node*
AddConnection(Node *leftPred, Node *rightPred, short type)
{
	Connection	*node=NULL;
	ListCell		*lc1, *lc2;
	
	Predicate *left = (Predicate*) leftPred;
	Predicate *right = (Predicate*) rightPred;
	
	// search for an existing connection
	foreach(lc1, left->connections) {
		foreach(lc2, right->connections) {
			Connection *temp1 = lfirst(lc1);
			Connection *temp2 = lfirst(lc2);
			
			if ((temp1 == temp2) && (temp1->tp == type)){
				node = temp1;
				break;
			}
		}
	}
	
	// create a new one if none was found
	if (node == NULL) {
		node = (Connection*) makeNode(Connection);
		node->id = condId++;
		node->tp = type;
		left->connections = lappend(left->connections, node);
		right->connections = lappend(right->connections, node);
	} 
	
	return (Node*) node;
}


Node*
AddPredicate(Oid relid, AttrNumber num, int value, short type)
{
	ListCell		*lc;
	TableNode		*tableNode = NULL;
	ColumnNode 		*columnNode = NULL;
	Predicate 	*predNode = NULL;
	
	// Search if table node already exisits
	foreach(lc, modelTree->children) {
		TableNode *temp = (TableNode*) lfirst(lc);
		if (temp->relid == relid) {
			tableNode = temp;
			break;
		}
	}

	// Search if column node already has an existing attribute
	if (tableNode != NULL) {
		foreach(lc, tableNode->children) {
			ColumnNode *temp = (ColumnNode*) lfirst(lc);
			if (temp->no == num) {
				columnNode = temp;
				break;
			}
		}
	} else {
		tableNode = (TableNode*) makeNode(TableNode);
		tableNode->relid = relid;
		modelTree->children = lappend(modelTree->children, tableNode);
	}
	
	// Search if predicate already exists
	if (columnNode != NULL) {
		if ((type == pequal) && (columnNode->equal != NULL))
			predNode = search_some(columnNode->equal, value);
		else if ((type == plessthan) && (columnNode->lessthan != NULL)){
			bt_key_val *kv = btree_search(columnNode->lessthan, value);
			if (kv != NULL)		
				predNode = (Predicate*) kv->val;
		}
		else if ((type == pgreaterthan) && (columnNode->greaterthan != NULL)){
			bt_key_val *kv = btree_search(columnNode->greaterthan, value);
			if (kv != NULL)
				predNode = (Predicate*) kv->val;
		}
	} else {
		columnNode = (ColumnNode*) makeNode(ColumnNode);
		columnNode->no = num;
		columnNode->parent = tableNode;
		
		tableNode->children = lappend(tableNode->children, columnNode);
	}
	
	// Build new predicate if necessary
	if (predNode == NULL) {
		predNode = (Predicate*) makeNode(Predicate);
		predNode->value = value;	
		predNode->connections = NULL;
		
		if (type == pequal) 
		{
			if (columnNode->equal == NULL)
			{
				columnNode->equal = create_hashtable(16, hashfromkey, equalkeys);
			}
			insert_some(columnNode->equal, value, predNode);
		}
		else if (type == plessthan) {
			// create b-tree if it is null
			if (columnNode->lessthan == NULL)
			{
				columnNode->lessthan = btree_create(2);
				columnNode->lessthan->value = btreevalue;
				columnNode->lessthan->key_size = btreekeysize;
				columnNode->lessthan->data_size = btreedatasize;
			}
			
			bt_key_val *kv = (bt_key_val*)palloc(sizeof(*kv));
			kv->key = palloc(sizeof(int));
			kv->key = value;
			kv->val = predNode;
			btree_insert_key(columnNode->lessthan,kv);
		}
		else if (type == pgreaterthan) {
			// create b-tree if it is null
			if (columnNode->greaterthan == NULL)
			{
				columnNode->greaterthan = btree_create(2);
				columnNode->greaterthan->value = btreevalue;
				columnNode->greaterthan->key_size = btreekeysize;
				columnNode->greaterthan->data_size = btreedatasize;
			}
			
			bt_key_val *kv = (bt_key_val*)palloc(sizeof(*kv));
			kv->key = palloc(sizeof(int));		
			kv->key = value;
			kv->val = predNode;
			btree_insert_key(columnNode->greaterthan,kv);
		}
	}	
	
	return (Node*) predNode;
}

ColumnNode*
GetColumnNode(Oid relid, AttrNumber num)
{
	ListCell		*lc;
	TableNode		*tableNode = NULL;
	ColumnNode 		*columnNode = NULL;
	
	// Search if table node already exisits
	foreach(lc, modelTree->children) {
		TableNode *temp = (TableNode*) lfirst(lc);
		if (temp->relid == relid) {
			tableNode = temp;
			break;
		}
	}

	// Search if column node already has an existing attribute
	if (tableNode != NULL) {
		foreach(lc, tableNode->children) {
			ColumnNode *temp = (ColumnNode*) lfirst(lc);
			if (temp->no == num) {
				columnNode = temp;
				break;
			}
		}
	} else {
		tableNode = (TableNode*) makeNode(TableNode);
		tableNode->relid = relid;
		modelTree->children = lappend(modelTree->children, tableNode);
	}
	
	// Search if predicate already exists
	if (columnNode == NULL) {
		columnNode = (ColumnNode*) makeNode(ColumnNode);
		columnNode->no = num;
		
		tableNode->children = lappend(tableNode->children, columnNode);
	}
	
	return columnNode;
}


Node*
GetPredicate(ColumnNode *columnNode, int value, short type)
{
	Predicate 	*predNode = NULL;
	
	if ((type == pequal) && (columnNode->equal != NULL))
		predNode = search_some(columnNode->equal, value);
	else if ((type == plessthan) && (columnNode->lessthan != NULL)){
		bt_key_val *kv = btree_search(columnNode->lessthan, value);
		if (kv != NULL)
			predNode = (Predicate*) kv->val;
	}
	else if ((type == pgreaterthan) && (columnNode->greaterthan != NULL)){
		bt_key_val *kv = btree_search(columnNode->greaterthan, value);
		if (kv != NULL)
			predNode = (Predicate*) kv->val;
	}
		
	if (predNode == NULL) {
		predNode = (Predicate*) makeNode(Predicate);
		predNode->value = value;	
		predNode->connections = NULL;
		
		if (type == pequal) 
		{
			if (columnNode->equal == NULL)
			{
				columnNode->equal = create_hashtable(16, hashfromkey, equalkeys);
			}
			insert_some(columnNode->equal, value, predNode);
		}
		else if (type == plessthan) {
			// create b-tree if it is null
			if (columnNode->lessthan == NULL)
			{
				columnNode->lessthan = btree_create(2);
				columnNode->lessthan->value = btreevalue;
				columnNode->lessthan->key_size = btreekeysize;
				columnNode->lessthan->data_size = btreedatasize;
			}
			
			bt_key_val *kv = (bt_key_val*)palloc(sizeof(*kv));
			kv->key = palloc(sizeof(int));
			kv->key = value;
			kv->val = predNode;
			btree_insert_key(columnNode->lessthan,kv);
		}
		else if (type == pgreaterthan) {
			// create b-tree if it is null
			if (columnNode->greaterthan == NULL)
			{
				columnNode->greaterthan = btree_create(2);
				columnNode->greaterthan->value = btreevalue;
				columnNode->greaterthan->key_size = btreekeysize;
				columnNode->greaterthan->data_size = btreedatasize;
			}
			
			bt_key_val *kv = (bt_key_val*)palloc(sizeof(*kv));
			kv->key = palloc(sizeof(int));		
			kv->key = value;
			kv->val = predNode;
			btree_insert_key(columnNode->greaterthan,kv);
		}
	}	
	
	return (Node*) predNode;
}


void 
print_condition (const struct Condition *c)
{
	if (c == NULL)
		return;
	printf ("%i, %i, %i, %i\n", c->table, c->column, c->type, c->values[0]);
	fflush(stdout);
}

void
SortedInsert(Node **result, Node *insert, int pos, int (*cmp_function) (void*,void*))
{
	Node *temp1 = NULL;
	Node *temp2 = NULL;
	int i=0;
	int cmp;
	while (true)
	{
		if (i == pos)
		{
			result[i] = insert;
			break;
		}
		
		cmp = cmp_function(result[i],insert);
		if (cmp < 0)
		{
			i++;
		}
		else 
		{
			temp1 = result[i];
			result[i] = insert;
			while (i != pos)
			{
				i++;
				temp2 = result[i];
				result[i] = temp1;
				temp1 = temp2;
			}
			break;
		}
	}
}

int
join_cmp(const struct JoinNode *j1, const struct JoinNode *j2)
{
	if (((TableNode*) ((ColumnNode*) j1->leftParent)->parent)->relid < ((TableNode*) ((ColumnNode*) j2->leftParent)->parent)->relid)
		return -1;
	else if (((TableNode*) ((ColumnNode*) j1->leftParent)->parent)->relid > ((TableNode*) ((ColumnNode*) j2->leftParent)->parent)->relid)
		return 1;
	else if (((TableNode*) ((ColumnNode*) j1->rightParent)->parent)->relid < ((TableNode*) ((ColumnNode*) j2->rightParent)->parent)->relid)
		return -1;
	else if (((TableNode*) ((ColumnNode*) j1->rightParent)->parent)->relid > ((TableNode*) ((ColumnNode*) j2->rightParent)->parent)->relid)
		return 1;
	else if (((ColumnNode*) j1->leftParent)->no < ((ColumnNode*) j2->leftParent)->no)
		return -1;
	else if (((ColumnNode*) j1->leftParent)->no > ((ColumnNode*) j2->leftParent)->no)
		return 1;
	else if (((ColumnNode*) j1->rightParent)->no < ((ColumnNode*) j2->rightParent)->no)
		return -1;
	else if (((ColumnNode*) j1->rightParent)->no > ((ColumnNode*) j2->rightParent)->no)
		return 1;
	else
		ereport(ERROR, (errmsg("error in model matching, join clause appears twice")));
}

int 
condition_cmp(const struct Condition *c1, const struct Condition *c2)
{
	if (c1->table > c2->table)
		return 1;
	else if (c1->table < c2->table)
		return -1;
	else
	{
		if (c1->column > c2->column)
			return 1;
		else if (c1->column < c2->column)
			return -1;
		else
		{
			if (c1->type > c2->type)
				return 1;
			else if (c1->type < c2->type)
				return -1;
			else
			{
				if (c1->values[0] > c2->values[0])
					return 1;
				else if (c1->values[0] < c2->values[0])
					return -1;
				else
					return 0;
			}
		}
	}
}


/**
 * Returns a list of conditions connected by and. 
 * A condition is of the form TABLE.COLUMN SIGN{=, <, >, IN} VALUE(S).
 * The result is sorted on table, column, sign, value.
 */
void
PreprocessWhereClause(Node *whereExpr, List *rtable, Condition** conditions, int* ccount, JoinNode **joins, int *jcount)
{
	if (whereExpr != NULL)
	{
		switch (nodeTag(whereExpr))
		{
			case T_OpExpr:
			{
				OpExpr *opExpr = (OpExpr *) whereExpr;
				switch (opExpr->opno)
				{
					case 96:		// INT4 equal
					case 94:		// INT2 equal
					case 97:		// INT4 less than
					case 95:		// INT2 less than
					case 521:		// INT4 greater than
					case 520:		// INT2 greater than
					{
						Node *left = linitial(opExpr->args);
						Node *right = lsecond(opExpr->args);
						
						Var *var1 = NULL;
						Var *var2 = NULL;
						Const *con = NULL;
						
						// expression of the form column=value
						if (IsA(left, Var) && IsA(right, Const))
						{
							var1 = (Var*) left;
							con = (Const*) right;
						} 
						// expression of the form value=column
						else if (IsA(left, Const) && IsA(right, Var))
						{
							con = (Const*) left;
							var1 = (Var*) right;
						}
						
						if ((con != NULL) && (var1 != NULL))
						{
							RangeTblEntry *rte = (RangeTblEntry*) list_nth(rtable, var1->varno-1);
													
							Condition* c = makeNode(Condition);
							c->table = rte->relid;
							c->column = var1->varoattno;
							
							if (opExpr->opno == 94 || opExpr->opno == 96)
								c->type = pequal;
							else if  (opExpr->opno == 97 || opExpr->opno == 95)
								c->type = plessthan;
							else if  (opExpr->opno == 520 || opExpr->opno == 521)
								c->type = pgreaterthan;
								
							c->values[0] = DatumGetInt32(con->constvalue);
							
							SortedInsert(conditions, c, *ccount, condition_cmp);
							(*ccount)++;
							if (*ccount == MAXCONDITIONS)
							{
								ereport(ERROR, (errmsg("error in model matching, too many predicates")));
							}
						}
						// join expression
						else if (IsA(left, Var) && IsA(right, Var))
						{
							Var *var1 = (Var*) left;
							Var *var2 = (Var*) right;
							
							RangeTblEntry *rte1 = (RangeTblEntry*) list_nth(rtable, var1->varno-1);
							RangeTblEntry *rte2 = (RangeTblEntry*) list_nth(rtable, var2->varno-1);
							
							SortedInsert(joins, AddJoin(rte1->relid, var1->varoattno, rte2->relid, var2->varoattno), *jcount, join_cmp);
							(*jcount)++;
							
							if (jcount == MAXJOINS)
								ereport(ERROR, (errmsg("error in model matching, maximum number of join terms is %i", MAXJOINS)));
						}
						else
						{
							ereport(ERROR, (errmsg("error in model matching, operator with type %i not supported", opExpr->opno)));
						}
						break;
					}
					default:
						ereport(ERROR, (errmsg("error in model matching, operator not supported")));
						break;
				}
				break;
			}
			case T_BoolExpr:
			{
				BoolExpr *boolExpr = (BoolExpr *) whereExpr;
				switch (boolExpr->boolop)
				{
					case AND_EXPR:
					{
						Node *left = linitial(boolExpr->args);
						Node *right = lsecond(boolExpr->args);
						
						PreprocessWhereClause(left, rtable, conditions, ccount, joins, jcount);
						PreprocessWhereClause(right, rtable, conditions, ccount, joins, jcount);
						
						break;
					}
					case OR_EXPR:
					{
						ereport(ERROR, (errmsg("error in model matching, or predicates not supported - use inlists instead")));
						break;
					}
					case NOT_EXPR:
						ereport(ERROR, (errmsg("error in model matching, not predicates not supported")));
						break;
					default:
						ereport(ERROR, (errmsg("error in model matching, bool type not recognized")));
						break;
				}
				break;
			}
			case T_ScalarArrayOpExpr:
			{
				ScalarArrayOpExpr *opExpr = (ScalarArrayOpExpr *) whereExpr;
				switch(opExpr->opno) 
				{
					case 96:		// INT4 equal
					case 94:		// INT2 equal
					{
						ListCell *lc;
						
						Var *var = (Var*) linitial(opExpr->args);
						RangeTblEntry *rte = (RangeTblEntry*) list_nth(rtable, var->varno-1);
						ArrayExpr *arr = (ArrayExpr*) lsecond(opExpr->args);
						
						Condition* c = makeNode(Condition);
						c->table = rte->relid;
						c->column = var->varoattno;
						c->type = pinlist;
						
						// sort inlist values
						Const *con;
						int i=0;
						foreach(lc, arr->elements)
						{
							con = (Const*) lfirst(lc);
							
							int k;
							int l;
							int temp1 = con->constvalue;
							int temp2;
							for (k=0; k<i; k++)
							{
								if (c->values[k] >= con->constvalue)
								{
									temp1 = c->values[k];
									c->values[k] = con->constvalue;
									for (l=(k+1); l<i; l++)
									{
										temp2 = c->values[l];
										c->values[l] = temp1;
										temp1 = temp2;
									}
									break;
								}
							}
							c->values[i] = temp1;
							
							i++;
							if (i == MAXINLISTSIZE)
								ereport(ERROR, (errmsg("error in model matching, maximum inlist size is %i", MAXINLISTSIZE)));
						}
						c->values[i] = -1;
						
						// add inlist condition to condition list
						SortedInsert(conditions, c, *ccount, condition_cmp);
						(*ccount)++;
						
						break;
					}
					default:
					{
						ereport(ERROR, (errmsg("error in model matching, scalar array type not supported")));
						break;
					}
				}
				break;
			}
			default:
				ereport(ERROR, (errmsg("error in model matching, node type not recognized")));
				break;
		}
	}
}

/**
 * Entry point of model index
 */


List *
FindMatchingNode(ModelInfo* modelInfo, Node *whereExpr, List *rtable, char* targetDateString)
{	
		// start indexing
	StartIndexing();
	
	// meta information
	TargetEntry *measureColumn = modelInfo->measure;
	TargetEntry *timeColumn = modelInfo->time;
	int granularity = modelInfo->granularity;
	Oid aggType = modelInfo->aggType;
	List *parameters = modelInfo->parameterList;
	Datum targetDate=NULL;
	if(targetDateString!=NULL)
		targetDate = DirectFunctionCall1(date_in, targetDateString);
	
	// Transform where clause to a list of conditions
	PreprocessWhereClause(whereExpr, rtable, conditions, &ccount, joins, &jcount);
	
	Node *left = NULL;
	Node *right = NULL;
	
	// Add/search join nodes
	int i=0;
	if (jcount > 0)
	{
		if (left == NULL) 
		{
			left = joins[i];
			i++;
		}
		for (i; i < jcount; i++)
			left = AddConnection(left, joins[i], 0);
	}
	
	// Add/search column nodes
	for (i=0; i < ccount; i++)
	{
		if (conditions[i]->type == pinlist)
		{
			int k=0;
			ColumnNode *colNode = GetColumnNode(conditions[i]->table, conditions[i]->column);
			if (left == NULL)
			{
				left = GetPredicate(colNode, conditions[i]->values[k], pequal);
				//left = AddPredicate(conditions[i]->table, conditions[i]->column, conditions[i]->values[k], pequal);
				k++;
			}
			while (conditions[i]->values[k] != -1)
			{
				right = GetPredicate(colNode, conditions[i]->values[k], pequal);
				//right = AddPredicate(conditions[i]->table, conditions[i]->column, conditions[i]->values[k], pequal);
				left = AddConnection(left, right, 1);
				k++;
			}
		}
		else if (left == NULL) 
		{
			
			left = AddPredicate(conditions[i]->table, conditions[i]->column, conditions[i]->values[0], conditions[i]->type);
		} 
		else
		{
			right = AddPredicate(conditions[i]->table, conditions[i]->column, conditions[i]->values[0], conditions[i]->type);
			left = AddConnection(left, right, 0);
		}
	}
	TableNode *result = left;
	
	// model on table level
	if (result == NULL)
	{
		ListCell	*lc;
		TableNode 	*temp;
		foreach(lc, modelTree->children) {
			temp = (TableNode*) lfirst(lc);
			if (measureColumn->resorigtbl == temp->relid) {
				result = temp;
				break;
			}
		}
		if (result == NULL)
		{
			temp = makeNode(TableNode);
			temp->relid = measureColumn->resorigtbl;
			modelTree->children = lappend(modelTree->children, temp);
			result = temp;
		}
	}
	
	// save index node
	modelInfo->mix = (Node*) result;
	
	// choose a model from the resulting node
	ListCell	*lc, *lc2, *lc3;
	List* possibleResultModels=NIL;
	bool 		paraFound=false;
	foreach(lc, result->models) { 
		ModelNode *temp = (ModelNode*) lfirst(lc); 
		if ((temp->measure == measureColumn->resorigcol) && (temp->time == timeColumn->resorigcol) && (temp->agg == aggType)) {
			// exact model found
			if (temp->granularity == granularity) { 
				if(parameters != NIL)
				{
					foreach(lc3,parameters)
					{
						AlgorithmParameter *searchPar = (AlgorithmParameter*) lfirst(lc3);
						foreach(lc2, temp->parameterList)
						{

							paraFound=false;
							AlgorithmParameter *findPar = (AlgorithmParameter*) lfirst(lc2);
							if((int)(searchPar->value->val.val.ival)==(int)(findPar->value->val.val.ival) && strcmp(searchPar->key,findPar->key)==0)
							{
								paraFound=true;
								break;
							}
						}
						if(!paraFound)
							break;
					}
				}
				else
				{
					//partial match
					paraFound=true; 
				}
				if(paraFound)
				{
					if(targetDate && (DatumGetBool(DirectFunctionCall2(date_le, targetDate, temp->timestamp)))) //targetDate is not prospective enough
						continue;
					ModelInfo *resmodel=palloc0(sizeof(ModelInfo));
					memcpy(resmodel,modelInfo,sizeof(ModelInfo));
					resmodel->mix = (Node*) temp;
					resmodel->model = ((ModelNode*)temp)->model;
					resmodel->timestamp = ((ModelNode*)temp)->timestamp;
					possibleResultModels=lappend(possibleResultModels,resmodel);

				}
			}
			// TODO: support different time granularities
			
		}
	}
	
	// for debugging
	//PrintDetails();
	
	// end indexing
	EndIndexing();
	
	return possibleResultModels;
}

TableNode*
GetTableNode(Oid relid) {
	ListCell *lc;
	
	if(modelTree == NULL)
		return NULL;
	
	foreach(lc, modelTree->children) {
		if (((TableNode*) lfirst(lc))->relid == relid) {
			return (TableNode*) lfirst(lc);
		}
	}
	return NULL;
}


int
GetJoinTuple(Oid tab, Datum val, Datum **heapValues, bool **heapIsNull)
{
	HeapTuple 		tuple = NULL;
	
	Relation		heapRelation;
	TupleDesc		heapTupDesc;
			
	Oid				indexoid;
	List			*indexoidlist;
	Relation		indexRelation;
	IndexScanDesc 	scan;
	ScanKeyData 	keys[1];
	
	int				result = -1;
	
	heapRelation = heap_open(tab, AccessExclusiveLock);
	heapTupDesc = RelationGetDescr(heapRelation);
	*heapValues = (Datum *) palloc(heapTupDesc->natts * sizeof(Datum));
	*heapIsNull = (bool *) palloc(heapTupDesc->natts * sizeof(bool));
	
	// get index on connection_table
	indexoidlist = RelationGetIndexList(heapRelation);
	indexoid = lfirst_oid(list_head(indexoidlist));
					
	// open index on connection_table
	indexRelation = index_open(indexoid, AccessExclusiveLock);
				
	// init scan keys
	// RESTRICTION: only one join column per table
	ScanKeyInit(&keys[0],
				1,
				BTEqualStrategyNumber, F_INT4EQ,
				val);
	
	// start scan
	scan = index_beginscan(heapRelation, indexRelation, SnapshotNow, 1, keys);
	
	if ((tuple = index_getnext(scan, ForwardScanDirection)) != NULL)
	{
		heap_deform_tuple(tuple, heapTupDesc, *heapValues, *heapIsNull);
		result = heapTupDesc->natts;
	}
				
	// free ressource
	index_endscan(scan);
	index_close(indexRelation, AccessExclusiveLock);
	heap_close(heapRelation, AccessExclusiveLock);
	
	return result;
}


void
FindConnections(List *conditions, List **models, List **connections)
{
	ListCell *lc;
	
	if (list_length(conditions) == 0)
		return;
	
	foreach(lc, conditions) {
		Connection *conn = (Connection*) lfirst(lc);
		if (conn->tp == 0) {
			if (list_member_int(*connections, conn->id)) {
				*models = list_concat_unique(*models, conn->models);
				FindConnections(conn->connections, models, connections);
			} else {
				*connections = lappend_int(*connections, conn->id);
			}
		} else {
			if (!list_member_int(*connections, conn->id))
			{
				*connections = lappend_int(*connections, conn->id);
				*models = list_concat_unique(*models, conn->models);
				FindConnections(conn->connections, models, connections);
			}
		}
	}
}


void
CheckPredicates(ColumnNode *columnNode, int value, List **models, List **connections) {
	
	if (columnNode->equal != NULL)
	{
		Predicate	*eq;
		eq = search_some(columnNode->equal,value);
		if (eq != NULL) {
			*models = list_concat_unique(*models, eq->models);
			FindConnections(eq->connections, models, connections);
		}
	}
	if (columnNode->lessthan != NULL)
	{
		AddLessThan(columnNode->lessthan->root, value, models, connections);
	}
	
	if (columnNode->greaterthan != NULL)
	{
		AddGreaterThan(columnNode->greaterthan->root, value, models, connections);
	}
	
}


void 
AddLessThan(bt_node *node, int key, List **models, List **connections)
{
	unsigned int i = 0;
		
	for (i; i < node->nr_active; i++)
	{	
	    if (key < node->key_vals[i]->key) {
	    	
	    	if (!node->leaf)
	    		AddLessThan(node->children[i], key, models, connections);
	    	
		    Predicate *eq = (Predicate*) node->key_vals[i]->val;
		    *models = list_concat_unique(*models, eq->models);
		    
		    FindConnections(eq->connections, models, connections);    
	    } 
	    if (i == (node->nr_active - 1))
	    {
	    	if (!node->leaf)
	    		AddLessThan(node->children[i+1], key, models, connections);
	    }
	}
}

void
AddGreaterThan(bt_node *node, int key, List **models, List **connections)
{
	unsigned int i = 0;
	
	for (i; i < node->nr_active; i++)
	{	

	    if (key > node->key_vals[i]->key) {
	    	
	    	if (!node->leaf)
	    		AddGreaterThan(node->children[i], key, models, connections);
	    	
		    Predicate *eq = (Predicate*) node->key_vals[i]->val;
		    *models = list_concat_unique(*models, eq->models);
		    
		    FindConnections(eq->connections, models, connections);    
		    
		    if (i == (node->nr_active - 1))
		    {
		    	if (!node->leaf)
		    		AddGreaterThan(node->children[i+1], key, models, connections);
		    }
	    } 
	    else
	    {
	    	if (!node->leaf)
	    		AddGreaterThan(node->children[i], key, models, connections);
	    	break;
	    }
	}
}


void
MaintainModel(ModelNode *modelNode, Datum *sourceValues, bool *sourceIsNull)
{
	//ereport(LOG, (errmsg("maintain model")));
	
	int 			timestamp;
	double 			value;
	AlgorithmInfo 	algInfo;
	
	timestamp = GetDatumAsInt(modelNode->timeType, sourceValues[modelNode->time-1]);
	value = GetDatumAsDouble(modelNode->measureType, sourceValues[modelNode->measure-1]);
	
	// check if this is next timestamp ist greather than the last we have seen
	if (timestamp > (modelNode->timestamp)) {
		
		// calculate current evaluation
		UpdateModelEvaluation(modelNode, value);
		
		// update last seen timestamp
		modelNode->timestamp = timestamp;
		
		// get forecast specific functions
		algInfo = initAlgorithmInfo(getModelTypeAsString(modelNode->method));
		
		// update model
		FunctionCall4(&algInfo->algIncrementalUpdate,PointerGetDatum(modelNode->model),Float8GetDatum(value),Int32GetDatum(0), NULL);
		
		// calculate next forecast value
		modelNode->forecast = DatumGetFloat8(FunctionCall2(&algInfo->algGetNextValue,PointerGetDatum(modelNode->model),Int32GetDatum(1)));
	} else {
		ereport(ERROR, (errmsg("timestamps out of order")));
	}
}



void
UpdateModelEvaluation(ModelNode *modelNode, double newValue)
{
	modelNode->ecount++;
	modelNode->eval += calculateOneStepSMAPE(newValue, modelNode->forecast);
	modelNode->evaluation = modelNode->eval/modelNode->ecount;
}

void 
MaintenanceInsert(HeapTuple newTuple, Relation sourceRelation)
{
	TupleDesc	sourceTupDesc;
	Oid			sourceOid;
	Datum	  	*sourceValues;
	bool		*sourceIsNull;
	
	ListCell 	*listCell1, *listCell2, *listCell3;
	TableNode	*tabNode;
	
	List 		*models = NIL;
	List 		*conn = NIL;
	
	StartIndexing();
	EndIndexing();
	
	//ereport(LOG, (errmsg("model maintenance (insert)")));
		
	// get new tuple
	sourceOid = RelationGetRelid(sourceRelation);
	sourceTupDesc = RelationGetDescr(sourceRelation);
	sourceValues = (Datum *) palloc(sourceTupDesc->natts * sizeof(Datum));
	sourceIsNull = (bool *) palloc(sourceTupDesc->natts * sizeof(bool));
	heap_deform_tuple(newTuple, sourceTupDesc, sourceValues, sourceIsNull);
	
	// 1st find relevant table
	tabNode = GetTableNode(sourceOid);
	
	if (tabNode != NULL) {
		// 2nd maintain all models which apply directly to this table
		foreach(listCell1, tabNode->models) {
			models = lappend(models, lfirst(listCell1));
		}
		
		// 3nd check each relevant attribute
		foreach(listCell1, tabNode->children) {
			ColumnNode *colNode = (ColumnNode*) lfirst(listCell1);
			
			if (!sourceIsNull[colNode->no-1]) {
				
				int value = GetDatumAsInt(sourceTupDesc->attrs[colNode->no-1]->atttypid, sourceValues[colNode->no - 1]);
				//int value = DatumGetInt32(sourceValues[colNode->no - 1]);
				
				// 4th check predicates for each applying attribute (add children to connection and model nodes)
				CheckPredicates(colNode, value, &models, &conn);
				
				// 5th check joins
				foreach(listCell2, colNode->joins) {
					Datum		*heapValues;
					Datum		*isNull;
					int			numAttr;
					TableNode	*dimTable;
					JoinNode 	*joinNode = (JoinNode*) lfirst(listCell2);
					
					numAttr = GetJoinTuple(joinNode->rightParent->parent->relid, value, &heapValues, &isNull);
					if (numAttr > 0) {
						foreach(listCell3, joinNode->connections) {
							conn = lappend_int(conn, ((Connection*) lfirst(listCell3))->id);
						}
						dimTable = GetTableNode(joinNode->rightParent->parent->relid);
						
						// only star schema
						foreach(listCell3, dimTable->children) {
							ColumnNode *colNode2 = (ColumnNode*) lfirst(listCell3);
							
							if (!sourceIsNull[colNode->no-1]) {
								int value2 = DatumGetInt32(heapValues[colNode2->no - 1]);
								if (colNode2->equal != NULL)
									CheckPredicates(colNode2, value2, &models, &conn);
							}
						}
					}

				}
			}
		}
	}
	
	// update found models
	foreach(listCell1, models)
	{
		MaintainModel((ModelNode*) lfirst(listCell1), sourceValues, sourceIsNull);
	}
	//elog(LOG, "num models maintained: %i", list_length(models));
	
	// clean up
	list_free(models);
	list_free(conn);
	
	// for debugging
	//PrintDetails();
}



// EXPERIMENTS FOR THE PAPER "Indexing Forecast Models for Matching und Maintenance" 
void
MaintenanceEntryPoint(HeapTuple newTuple, Relation sourceRelation)
{
	if (completeModelList != NULL)
	{
		list_free_deep(completeModelList);
		modelTree->numModels = 0;
		completeModelList = NULL;
	}
	else if (modelTree == NULL)
	{
		return;
	}
	else
	{
		MaintenanceInsert(newTuple, sourceRelation);
	}
}

void
MaintenanceTest(HeapTuple newTuple, Relation sourceRelation)
{
	struct timeval	start, end;
	int i;
	printf("start maintenance test\n");
	fflush(stdout);
	gettimeofday(&start, 0);
	for (i=0; i<10000000; i++)
	{
		MaintenanceEntryPoint(newTuple, sourceRelation);
	}
	gettimeofday(&end, 0);
	printf("maintenance execution time: %f \n", end.tv_sec - start.tv_sec + 1e-6*(end.tv_usec - start.tv_usec));
	fflush(stdout);
}


Node*
TestIndexTimeSeries(ModelInfo *modelInfo, Node* whereExpr, List* rTable)
{
	List* 			modelFound = NIL;
	struct timeval	start, end;
	if (count >= 1000)
	{
		int i;
		printf("start index test\n");
		fflush(stdout);
		gettimeofday(&start, 0);
		for (i=0; i<10000000; i++)
		{
			modelFound = FindMatchingNode(modelInfo, whereExpr, rTable,NULL);
		}
		gettimeofday(&end, 0);
		printf("indexing execution time: %f \n", end.tv_sec - start.tv_sec + 1e-6*(end.tv_usec - start.tv_usec));
		fflush(stdout);
		count = 0;
	} 
	else 
	{
		modelFound = FindMatchingNode(modelInfo, whereExpr, rTable,NULL);
		count++;
	}
		
	return modelFound;
}

bool
TestQueryString(ModelInfo *modelInfo, const char *sourcetext)
{
	bool 			modelFound;
	struct timeval	start, end;
	if (count >= 1000)
	{
		int i;
		printf("start index test\n");
		fflush(stdout);
		gettimeofday(&start, 0);
		for (i=0; i<10000000; i++)
		{
			modelFound = SearchQueryString(modelInfo, sourcetext);
		}
		gettimeofday(&end, 0);
		printf("indexing execution time: %f \n", end.tv_sec - start.tv_sec + 1e-6*(end.tv_usec - start.tv_usec));
		fflush(stdout);
		//count = 0;
	} 
	else 
	{
		modelFound = SearchQueryString(modelInfo, sourcetext);
		count++;
	}
	return modelFound;
}


bool
SearchQueryString(ModelInfo *modelInfo, const char *sourcetext)
{
	ListCell 	*lc;
	bool		found;
	
	StartIndexing();
	
	ModelNode *modelNode = NULL;
	foreach(lc, completeModelList)
	{
		ModelNode *model = lfirst(lc);
		if (strncmp(model->sourcetext,sourcetext,strlen(sourcetext)) == 0)
		{
			modelNode = model;
		}	
	}
	
	if (modelNode == NULL)
	{
		found = false;
	} else {
		found = true;
		modelInfo->model = modelNode->model;
		modelInfo->timestamp = modelNode->timestamp;
	}
	
	EndIndexing();
	
	return found;
}

void
AddModelToHashTable(TargetEntry *measure, TargetEntry *time, int granularity, Oid aggType, const char *sourcetext, int timestamp, double forecast, Model *model)
{
	StartIndexing();
	
	int length = strlen(sourcetext);
	char *key = (char*) palloc(sizeof(char) * strlen(sourcetext));
	memcpy(key, sourcetext, strlen(sourcetext));
	
	ModelNode *modelNode = NULL;
	
	modelNode = makeNode(ModelNode);
	modelNode->timerel = time->resorigtbl;
	modelNode->time = time->resorigcol;
	modelNode->timeType = (Oid) exprType((Node*) time->expr);
	modelNode->measurerel = measure->resorigtbl;
	modelNode->measure = measure->resorigcol;
	modelNode->measureType = (Oid) exprType((Node*) measure->expr);
	modelNode->granularity = granularity;
	modelNode->agg = aggType;
	modelNode->timestamp = timestamp;
	modelNode->forecast = forecast;
	modelNode->evaluation = 0;
	modelNode->ecount = 0;
	modelNode->eval = 0;
	modelNode->model = model;
	modelNode->sourcetext = key;
	
	completeModelList = lappend(completeModelList, modelNode);
	modelTree->numModels++;
	
	EndIndexing();
}
