/*
 * modelGraph.c
 *
 *  Created on: 01.03.2011
 *      Author: b1anchi
 */

#include "forecast/methods/armodel.h"
#include "forecast/modelGraph/modelGraph.h"
#include "forecast/modelGraph/modelGraphReceiver.h"
#include "forecast/modelGraph/greedyAlgorithm.h"
#include "forecast/methods/ExpSmooth.h"
#include "access/heapam.h"
#include "utils/rel.h"
#include "utils/tqual.h"
#include "utils/portal.h"
#include "utils/fmgroids.h"
#include "utils/builtins.h"
#include "utils/datum.h"
#include "utils/memutils.h"
#include "utils/lsyscache.h"
#include "utils/numeric.h"
#include "tcop/pquery.h"
#include "tcop/utility.h"
#include "parser/parse_utilcmd.h"
#include "parser/parse_expr.h"
#include "nodes/makefuncs.h"
#include <time.h>
#include "parser/parse_coerce.h"
#include "utils/guc.h"
#include "catalog/indexing.h"
#include "catalog/catalog.h"
#include <inttypes.h>

ModelGraphIndexNode *modelGraphRoot = NULL;
ModelGraphIndex *modelGraphIdx = NULL;

char *graphCreationSourceText;
char *fullGraphCreationSourceText;
char *trainingSourceTextFromFill = NULL;

// memory context of model tree
MemoryContext 	modelGraphContext = NULL,
                oldContext;
//remember the number of references to the modelGraphRoot to determine when to switch back to oldContext
int modelGraphRootRefCount = 0;
int leafCount=0;

List *modelsToReestimate = NIL;
double confError = 0;

void setConfError(double newError){
	confError = newError;
}

double getConfError(void){
	return confError;
}

void setTrainingDataQuery(char *sourceText, int length){
	trainingSourceTextFromFill = palloc0(sizeof(char)*length);
	trainingSourceTextFromFill = strncpy(trainingSourceTextFromFill, sourceText, length-1);
}

char *getTrainingDataQuery(void){
	return trainingSourceTextFromFill;
}

void runQuery(char* queryString,char* tag)
{
	List			*query_list = NIL,
	                *planned_list = NIL;
	const char		*commandTag;
	Portal			portal;

	// create command Tag
	commandTag = tag;

	//  plan the query
	query_list =pg_parse_and_rewrite(queryString,NULL,0);
	planned_list = pg_plan_queries(query_list, 0, NULL);

	// Create a new portal to run the query in
	portal = CreateNewPortal();

	//Don't display the portal in pg_cursors, it is for internal use only
	portal->visible = false;

	PortalDefineQuery(portal, NULL, queryString, commandTag, planned_list, NULL);

	//  Start the portal.  No parameters here.
	PortalStart(portal, NULL, InvalidSnapshot);

	(void) PortalRun(portal, FETCH_ALL, false, None_Receiver, None_Receiver, NULL);
	PortalDrop(portal,true);

}

Var *getTEVar(TargetEntry *te){
	if(!IsA(te->expr, Aggref)) {
		return ((Var *)te->expr);
	} else { /*Aggref case*/
		return ((Var *)lfirst(list_head(((Aggref *)te->expr)->args)));
	}
}
/*
 * Grants access to the RootNode of the ModelGraph
 * THE ROOT MUST ALWAYS BE ACCESED BY THIS FUNCTION!!!
 * AND IT MUST BE RELEASED AGAIN WITH releaseModelGraphRoot!!!
 */
ModelGraphIndexNode *
getModelGraphRoot(void) {

	if(!modelGraphRoot) {
		modelGraphContext = AllocSetContextCreate(TopMemoryContext,
		                    "ModelGraphContext",
		                    ALLOCSET_DEFAULT_MINSIZE,
		                    ALLOCSET_DEFAULT_INITSIZE,
		                    ALLOCSET_DEFAULT_MAXSIZE);
		oldContext = MemoryContextSwitchTo(modelGraphContext);

		modelGraphRoot = palloc0(sizeof(ModelGraphIndexNode));
		modelGraphIdx = palloc0(sizeof(ModelGraphIndex));
		modelGraphRoot->models = NULL;
		modelGraphRoot->type = T_RootNode;
		modelGraphRoot->aggIndicator = 0;
	} else {
		//only switch MemoryContext if the current MemoryContext is NOT the modelGraphContext
		int i=strcmp(CurrentMemoryContext->name, "ModelGraphContext");
		if(i!=0)
			oldContext = MemoryContextSwitchTo(modelGraphContext);
	}
	modelGraphRootRefCount+=1;
	return modelGraphRoot;
}

void
releaseModelGraphRoot(void) {
	if(modelGraphRootRefCount == 1)
		MemoryContextSwitchTo(oldContext);

	modelGraphRootRefCount-=1;
}

MemoryContext
getModelGraphContext(void) {
	return modelGraphContext;
}

void
extractSubquery(const char *sourcetext) {

	char	*res,
	        *sourceCpy = palloc0(strlen(sourcetext)+1),
	         *sourceCpy2 = palloc0(strlen(sourcetext)+1);
	int		braketCount = 1,
	        length = 0;

	sourceCpy = strcpy(sourceCpy, sourcetext);

	sourceCpy2 = strstr(sourceCpy, "FROM");

	if(!sourceCpy2)
		sourceCpy = strstr(sourceCpy, "from");
	else
		sourceCpy = sourceCpy2;

	sourceCpy = strpbrk(sourceCpy, "(");
	sourceCpy++;
	res = sourceCpy;
	while(braketCount) {
		if(*res == '(')
			braketCount++;
		else if(*res == ')')
			braketCount--;
		res++;
		length++;
		if(*res == '\0' && braketCount)
			elog(ERROR,"MUHAHAHA WITHOUT ME THIS COULD SEGFAULT HERE");
	}
	graphCreationSourceText = palloc0(length);
	graphCreationSourceText = strncpy(graphCreationSourceText, sourceCpy, length-1);
	graphCreationSourceText[length-1] = '\0';
//		pfree(sourceCpy);
//		pfree(sourceCpy2);
}

char *
getSourceText(void) {

	char * res;

	Assert(modelGraphContext);
	oldContext = MemoryContextSwitchTo(modelGraphContext);

	res = malloc(strlen(graphCreationSourceText)+1);

	res = strcpy(res, graphCreationSourceText);

	MemoryContextSwitchTo(oldContext);

	return res;
}

bool modelGraphDepthCheck(void) {
	int height=0;
	List* typeInfo=NIL;
	ModelGraphIndexNode *iter=modelGraphRoot;

	if(!modelGraphRoot)
		return true;

	
	while(iter->aggChild) { //test height of agg->agg->agg and get type and Col Infos
		iter=iter->aggChild;
		typeInfo=lappend(typeInfo,iter->target);
		height++;
	}
	//Check if every path has the same heightand same type
	iter=modelGraphRoot;
	return checkDepthrekur(height-1,-1,typeInfo,iter);
}

bool checkDepthrekur(int maxheight,int currentheight, List* typeInfo,ModelGraphIndexNode * iter) {
	ArListCell* arc;
	bool result=true;
	//found a leaf, but have not reached the depth of agg->agg...->agg
	if((!iter->children && currentheight>maxheight)) {
		elog(WARNING,"Tree not deep enough at attrNumber:%i and TableOid:%i",iter->target->resno,iter->target->resorigtbl);
		return false;
	}
	//found a leaf, but are deeper than the depth of agg->agg...->agg
	if((!iter->children && currentheight<maxheight)) {
		elog(WARNING,"Tree too deep at attrNumber:%i and TableOid:%i",iter->target->resno,iter->target->resorigtbl);
		return false;
	}

	//Check if attcol and tableoid matches the type found at agg->agg...->agg. no check for rootnode needed, he has no target entry
	if(iter!=modelGraphRoot && currentheight>=0 &&(iter->target->resno != ((TargetEntry*)list_nth(typeInfo,currentheight))->resno ||iter->target->resorigtbl != ((TargetEntry*)list_nth(typeInfo,currentheight))->resorigtbl)) {
		elog(WARNING,"Wrong typing detected at height:%i",currentheight);

		return false;
	}
	if(!iter->children && currentheight==maxheight)
		return true;


	arforeach(arc, iter->children) {
		result=result && checkDepthrekur(maxheight,currentheight+1,typeInfo,arlfirst(arc));
		if(!result)
			break;
	}
	return result;

}

Datum
determineWhichNodeToUse(Node **whereExpr, TargetEntry *tle, List *rTable) {

	RangeTblEntry	*rte;
	Var 			*v;
	OpExpr 			*opex;
	Datum 			result = 0;
	if(!tle) {
		return 0;
	}

	switch(nodeTag(*whereExpr)) {
	case T_OpExpr: {

//TODO: check if opno is '=', else throw an error
		switch(nodeTag(lfirst(list_head(((OpExpr *)*whereExpr)->args)))){
		case T_Var:
			v = (Var *)lfirst(list_head(((OpExpr *)*whereExpr)->args));
			break;
		case T_RelabelType:
			v = (Var *)((RelabelType *)lfirst(list_head(((OpExpr *)*whereExpr)->args)))->arg;
			break;
		default:
			elog(ERROR, "Unhandeled type in determineWhichNodeToUse.");
			break;
		}
		if(((Var *)tle->expr)->varattno != v->varattno)
			return result;
		rte = (RangeTblEntry*) list_nth(rTable, v->varno-1);
		if(tle->resorigtbl != rte->relid)
			return result;
		//we found a matching TargetEntry

		result = ((Const *)lfirst(list_tail(((OpExpr *)*whereExpr)->args)))->constvalue;
		*whereExpr = NULL;
		return  result;
	}
	case T_BoolExpr: {
		BoolExpr *boolep=(BoolExpr*) *whereExpr;
		ListCell *lc,*prev,*toKill,*toKillPrev;
		prev=NULL;
		toKill=NULL;
		toKillPrev=NULL;
		if(boolep->boolop==OR_EXPR || boolep->boolop==NOT_EXPR)
			elog(ERROR,"Cannot store Models with OR or NOT as part of the where clause");
		foreach(lc, boolep->args) {
			if(toKill) {
				if(!prev)
					list_delete_first(boolep->args);
				else
					list_delete_cell(boolep->args,toKill,toKillPrev);
				toKill=NULL;
				toKillPrev=NULL;
			}
			if(nodeTag(lfirst(lc))==T_BoolExpr) { //Since we know there are only And Expressions, we can append all condition to one big list
				boolep->args=list_concat(boolep->args,((BoolExpr*)lfirst(lc))->args);
				toKill=lc;
				toKillPrev=prev; //we cannot delete the current listelement, thatfore we remember our position to kill it from the next position
				continue;
			}

			opex = (OpExpr *)(lfirst(lc));
			if(nodeTag(lfirst(list_head(opex->args))) == T_Var && nodeTag(lfirst(list_tail(opex->args))) == T_Var) {
				toKill=lc;
				toKillPrev=prev;
				continue;
			}

			switch(nodeTag(lfirst(list_head(opex->args)))){
			case T_Var:
				v = (Var *)lfirst(list_head(opex->args));
				break;
			case T_RelabelType:
				v = (Var *)((RelabelType *)lfirst(list_head(opex->args)))->arg;
				break;
			default:
				elog(ERROR, "Unhandeled type in determineWhichNodeToUse.");
				break;
			}
			if(((Var *)tle->expr)->varattno != v->varattno) {
				prev=lc;
				continue;
			}
			rte = (RangeTblEntry*) list_nth(rTable, v->varno-1);
			if(tle->resorigtbl != rte->relid) {
				prev=lc;
				continue;
			}
			//found a matching TargetEntry
			result = ((Const *)lfirst(list_tail(((OpExpr *)(lfirst(lc)))->args)))->constvalue;
			if(boolep->args->length==1)
				*whereExpr = NULL;
			else {
				if(!prev)
					list_delete_first(boolep->args);
				else
					list_delete_cell(boolep->args,lc,prev);
			}
			if(toKill) { //maybe we have something to kill
				if(!prev)
					list_delete_first(boolep->args);
				else
					list_delete_cell(boolep->args,toKill,toKillPrev);
				toKill=NULL;
				toKillPrev=NULL;
			}
			return result;
		}

		if(toKill) { //maybe we have something to kill
			if(!prev)
				list_delete_first(boolep->args);
			else
				list_delete_cell(boolep->args,toKill,toKillPrev);
			toKill=NULL;
			toKillPrev=NULL;
		}
		return result;

	}
	default : {
		elog(ERROR,"Unknown Nodetype in ModelGraphSearch");
		return 0;
	}
	}
}

ModelGraphIndexNode *
FindCorrectNode(ModelGraphIndexNode *mgin, Node *whereCopy, List *rTable) {
	ModelGraphIndexNode *mgin2;
	Datum 				searchHit;

	if(!mgin)
		mgin2 =getModelGraphRoot();
	else
		mgin2 = mgin;


	while(mgin2->children) {

		//when there is nothing left of the WhereExpr just root to the aggChild
		if(!whereCopy) {
			mgin2 = mgin2->aggChild;
			continue;
		} else {
			//determine which of mgin2's children to use to root on
			searchHit = determineWhichNodeToUse(&whereCopy, ((ModelGraphIndexNode *)arlfirst(arlist_head(mgin2->children)))->target, rTable);

//TODO: this should be something other, because if we don't hit it could mean we entered a wrong value as predicate
			if(!searchHit) {
				mgin2 = mgin2->aggChild;
				continue;
			}

			switch(((Var *)((ModelGraphIndexNode *)arlfirst(arlist_head(mgin2->children)))->target->expr)->vartype) {
			case 20:
			case 21:
			case 23: {
				mgin2 = arlist_get(mgin2->children,find_int(mgin2->children, searchHit, Mgin_Int_SorterAsc));
				if(!mgin2)
					elog(ERROR,"Cannot store model, column: %i is not present in current Modelgraph", DatumGetInt32(searchHit));
				continue;
			}
			case 700:
			case 701: {
				continue;
			}
			case 1042: {
				mgin2 = (ModelGraphIndexNode *)arlist_get(mgin2->children,find(mgin2->children, (void *)searchHit, Mgin_Char_SorterAsc));
				if(!mgin2)
					elog(ERROR,"Cannot store model, column: %s is not present in current Modelgraph",DatumGetCString(DirectFunctionCall1(bpcharout,searchHit)));
				continue;
			}
			case 1043: {
				mgin2 = (ModelGraphIndexNode *)arlist_get(mgin2->children,find(mgin2->children, (void *)searchHit, Mgin_Char_SorterAsc));
				if(!mgin2)
					elog(ERROR,"Cannot store model, column: %s is not present in current Modelgraph",DatumGetCString(DirectFunctionCall1(varcharout,searchHit)));
				continue;
			}
			}
		}
	}

	if(!mgin)
		releaseModelGraphRoot();

	return mgin2;
}

void updateModelGraphIdx(ModelInfo *resInfo)
{
    if(!modelGraphIdx->modellistMaxSize)
	{
		modelGraphIdx->modellist=palloc0(10*sizeof(ModelInfo*));
		modelGraphIdx->modellistMaxSize=10;
	}
	else if((modelGraphIdx->models+1>=modelGraphIdx->modellistMaxSize))
	{
		modelGraphIdx->modellist=repalloc(modelGraphIdx->modellist,(10+modelGraphIdx->modellistMaxSize)*sizeof(ModelInfo*));
		modelGraphIdx->modellistMaxSize+=10;
	}
    modelGraphIdx->modellist[modelGraphIdx->models] = resInfo;
    modelGraphIdx->models++;
}

void
AddModelToModelGraph(ModelInfo *modelInfo, List *rTable) {

	Node *whereCopy = (Node *)copyObject(modelInfo->mix);
	ModelGraphIndexNode *mgin = getModelGraphRoot(),
	                     *mgin2;
	ModelInfo 			*resInfo = NULL;


	mgin2 = FindCorrectNode(mgin, whereCopy, rTable);

	//copy the ModelInfo to have it in the right MemoryContext

	resInfo = copyObject(modelInfo);
	resInfo->mix = (Node *)mgin2;
	mgin2->models = lappend(mgin2->models, resInfo);

    updateModelGraphIdx(resInfo);

	releaseModelGraphRoot();
}

void
GetTuplesFromSubquery(CreateModelGraphStmt *stmt, char *completionTag) {

	List			*query_list = NIL,
	                 *planned_list = NIL;
	ListCell		*lc;
	const char		*commandTag;
	Portal			portal;
	DestReceiver	*tupledest;
	Datum			*values;

	// create command Tag
	commandTag = "SELECT";

	//  plan the query
	query_list = lappend(query_list, stmt->subquery);
	planned_list = pg_plan_queries(query_list, 0, NULL);

	// results should be send to the ModelReceiver
	tupledest = CreateModelGraphDestReceiver();

	// Create a new portal to run the query in
	portal = CreateNewPortal();

	//Don't display the portal in pg_cursors, it is for internal use only
	portal->visible = false;

	PortalDefineQuery(portal, NULL, stmt->sourcetext, commandTag, planned_list, NULL);

	//  Start the portal.  No parameters here.
	PortalStart(portal, NULL, InvalidSnapshot);

	(void) PortalRun(portal, FETCH_ALL, false, tupledest, tupledest, completionTag);

	stmt->tDesc = CreateTupleDescCopy(((ModelGraphState *)tupledest)->tDesc);

	foreach(lc, ((ModelGraphState *)tupledest)->tupleList) {
		int i;
		values = palloc0(sizeof(Datum)*((ModelGraphState *)tupledest)->tDesc->natts);
		for(i = 0; i < ((ModelGraphState *)tupledest)->tDesc->natts; ++i) {

			values[i] = datumCopy(((Datum *)lfirst(lc))[i], ((ModelGraphState *)tupledest)->tDesc->attrs[i]->attbyval, ((ModelGraphState *)tupledest)->tDesc->attrs[i]->attlen);
		}
		stmt->tupleList = lappend(stmt->tupleList, values);
	}

	(*tupledest->rDestroy) (tupledest);

	PortalDrop(portal, false);
}

/*
 * transforms a raw GraphAttribute by transforming its attributeCol-ColumnRef to a TargetEntry
 */
GraphAttribute *
transformGraphAttribute(GraphAttribute *gAtt, ParseState *pstate, List *teList) {

	TargetEntry		*attCol;
	char			*colName;
	RangeTblEntry 	*rte;
	List			*helpList = NIL;
	ListCell		*lc;
	A_Const 		*a_const;
	Const			*c;

	//at this point these are still ColumnRefs!, but handling as TargetEntry saves some ugly casting
	attCol = (TargetEntry *)gAtt->attributeCol;
	colName = ((Value *)lfirst(list_tail(((ColumnRef *)attCol)->fields)))->val.str;

	//find the TargetEntry that fits the actual graphAttribute

//TODO: maybe here we need the targetList of the executed Query from transformCreateModelGraphStmt, because the columns of the TargetEntries don't fit
	attCol = findTargetlistEntry(pstate, (Node *)attCol, &teList, ORDER_CLAUSE);
	rte = (RangeTblEntry *)list_nth(pstate->p_rtable, ((Var *)attCol->expr)->varno-1);

	//forward the ColumnName an the RelationOid, so we don`t need any other structures later(it's not done automatically!)
	attCol->resname = palloc0((strlen(colName)+1)*sizeof(char));
	attCol->resname = strcpy(attCol->resname,colName);
	attCol->resorigtbl = rte->relid;

	//save back the TargetEntry for later use
	gAtt->attributeCol = (Node *)attCol;

	//convert all A_Consts from the excludeList to Consts, for easier comparison with Tuples from a heapscan
	helpList = NIL;
	foreach(lc, gAtt->excludeList) {
		a_const = (A_Const *)lfirst(lc);

		c = (Const *)transformExpr(pstate, (Node *)a_const);
		if(c->consttype == 705) {
			helpList = lappend(helpList, (void *)coerce_to_common_type(NULL, (Node*)c, 1042, "CREATE MODELTREE"));
		} else {
			helpList = lappend(helpList, c);
		}
	}
	gAtt->excludeList = helpList;

	return gAtt;
}

void
PrintNode(FILE *file, ModelGraphIndexNode *mgin) {

	ArListCell			*alc;
	Oid					typeOid;
	ModelGraphIndexNode	*tempMgin;
	char* color;

	if(file) {

		arforeach(alc, mgin->children) {
			tempMgin = (ModelGraphIndexNode *)arlfirst(alc);
			if (tempMgin->disAggModels || tempMgin->aggModels || tempMgin->models)
				color="violet";
			else
				color="black";
			//if an AggNode should be printed and its actual considered child has more than 1 parent(e.g. when there is a correlation)...
			if(nodeTag(mgin)==T_AggNode && tempMgin->parents->length>1) {
				//...only the edges to the child should be printed and NOTHING else
				if(!mgin->target) {
					fprintf(file, "root -> node%p;\n", tempMgin);
				} else {
					fprintf(file, "node%p -> node%p;\n", mgin, tempMgin);
				}
			} else {
				typeOid = ((Var *)tempMgin->target->expr)->vartype;

				switch(typeOid) {
				case 20:
				case 21:
				case 23: {
					if(tempMgin->children)
						fprintf(file, "node%p [color=%s, fontcolor=%s, shape=record, label=\"%s|%i\"];\n", tempMgin, color, color, tempMgin->target->resname, GetDatumAsInt(typeOid,tempMgin->value));
					else
						fprintf(file, "node%p [color=%s, fontcolor=%s, shape=record, label=\"{%i|{%s|%i}|{Models|%i}|{AggModels|%i}|{DisAggModels|%i}}\"];\n", tempMgin, color, color, tempMgin->id, tempMgin->target->resname, GetDatumAsInt(typeOid,tempMgin->value), list_length(tempMgin->models), list_length(tempMgin->aggModels), list_length(tempMgin->disAggModels));
					break;
				}
				case 700:
				case 701: {
					if(tempMgin->children)
						fprintf(file, "node%p [color=%s, fontcolor=%s, shape=record, label=\"%s|%f\"];\n", tempMgin, color, color, tempMgin->target->resname, GetDatumAsDouble(typeOid,tempMgin->value));
					else
						fprintf(file, "node%p [color=%s, fontcolor=%s, shape=record, label=\"{%i|{%s|%f}|{Models|%i}|{AggModels|%i}|{DisAggModels|%i}}\"];\n", tempMgin, color, color, tempMgin->id, tempMgin->target->resname, GetDatumAsDouble(typeOid,tempMgin->value), list_length(tempMgin->models), list_length(tempMgin->aggModels), list_length(tempMgin->disAggModels));
					break;
				}
				case 1042: {
					if(tempMgin->children)
						fprintf(file, "node%p [color=%s, fontcolor=%s, shape=record, label=\"%s|%s\"];\n", tempMgin, color, color, tempMgin->target->resname, DatumGetCString(DirectFunctionCall1(bpcharout, tempMgin->value)));
					else
						fprintf(file, "node%p [color=%s, fontcolor=%s, shape=record, label=\"{%i|{%s|%s}|{Models|%i}|{AggModels|%i}|{DisAggModels|%i}}\"];\n", tempMgin, color, color, tempMgin->id, tempMgin->target->resname, DatumGetCString(DirectFunctionCall1(bpcharout, tempMgin->value)), list_length(tempMgin->models), list_length(tempMgin->aggModels), list_length(tempMgin->disAggModels));
					break;
				}
				case 1043: {
					if(tempMgin->children)
						fprintf(file, "node%p [color=%s, fontcolor=%s, shape=record, label=\"%s|%s\"];\n", tempMgin, color, color, tempMgin->target->resname, DatumGetCString(DirectFunctionCall1(varcharout, tempMgin->value)));
					else
						fprintf(file, "node%p [color=%s, fontcolor=%s, shape=record, label=\"{%i|{%s|%s}|{Models|%i}|{AggModels|%i}|{DisAggModels|%i}}\"];\n", tempMgin, color, color, tempMgin->id, tempMgin->target->resname, DatumGetCString(DirectFunctionCall1(bpcharout, tempMgin->value)), list_length(tempMgin->models), list_length(tempMgin->aggModels), list_length(tempMgin->disAggModels));
					break;
				}
				default:
					elog(ERROR, "PrintNode can't handle type %i of column %s!", typeOid, tempMgin->target->resname);
				}

				if(!mgin->target) {
					fprintf(file, "root -> node%p;\n", tempMgin);
				} else {
					fprintf(file, "node%p -> node%p;\n", mgin, tempMgin);
				}

				PrintNode(file, tempMgin);
			}
		}

		if(mgin->aggChild) {
			tempMgin = mgin->aggChild;

			if(tempMgin->children)
				fprintf(file, "node%p [color=blue, fontcolor=blue, shape=record, label=\"Agg\"];\n", tempMgin);
			else
				fprintf(file, "node%p [color=blue, fontcolor=blue, shape=record, label=\"{%i|Agg|{Models|%i}|{AggModels|%i}|{DisAggModels|%i}}\"];\n", tempMgin, tempMgin->id, list_length(tempMgin->models), list_length(tempMgin->aggModels), list_length(tempMgin->disAggModels));

			if(!mgin->target) {
				fprintf(file, "root -> node%p;\n", tempMgin);
			} else {
				fprintf(file, "node%p -> node%p;\n", mgin, tempMgin);
			}

			PrintNode(file, tempMgin);
		}
	}
}

/*
 * add ALL the entries in the childList to ALL leaves that are reachable from the passed ModelGraphIndexNode
 *
 * node - the ModelGraphIndexNode that should get the new children (or whose child should get the new children)
 * childList - contains all distinct Datums a child should be created for
 * tle - is the TargetEntry corresponding to the childList
 * aggFlag - is used to determine if a node with children should root to its children or only to its aggChild
 */
void
AddChildren(ModelGraphIndexNode *node, List *childList, TargetEntry *tle, bool unique, int aggFlag) {

	ListCell 			*lc;
	ArListCell			*alc = NULL;
	ModelGraphIndexNode *child;
	Oid					typeOid;
	int16				resultTypLen;
	bool				resultTypByVal;

	//if node has no children yet, add all entries of childList as a child...
	if(!node->children) {

		foreach(lc, childList) {

			child = palloc0(sizeof(ModelGraphIndexNode));
			child->models = NIL;
			child->type = T_IndexNode;
			child->parents = arlappend2(child->parents, node,2);
			child->aggIndicator = node->aggIndicator<<1;
			

			typeOid = ((Var *)tle->expr)->vartype;
			switch(typeOid) {
			case 20:
			case 21:
			case 23: {
				child->value = datumCopy((Datum)lfirst_int(lc), true, (typeOid==20)? 8 : ((typeOid==21)? 2 : 4));
				break;
			}
			case 700:
			case 701: {
				child->value = datumCopy((Datum)lfirst_int(lc), false, (typeOid==700)? 4 : 8);
				break;
			}
			case 1042:
			case 1043: {
				get_typlenbyval(((Var *)tle->expr)->vartype, &resultTypLen, &resultTypByVal);
				child->value = datumCopy((Datum)lfirst(lc), resultTypByVal, resultTypLen);
				break;
			}
			case 1700: {
				child->value = (Datum)lfirst_int(lc);
				break;
			}
			default:
				elog(ERROR, "Unknown type for ModelGraph!");
				break;
			}
			child->target = tle;


			node->children = arslinsert(node->children, child, MginSorterAsc, childList->length);
		}

		//...and add a sibling aggNode
		node->aggChild = palloc0(sizeof(ModelGraphIndexNode));
		node->aggChild->models = NIL;
		node->aggChild->type = T_AggNode;
		node->aggChild->target = tle;
		node->aggChild->parents = arslinsert(node->aggChild->parents, node, MginSorterAsc, 2);
		node->aggChild->aggIndicator = (node->aggIndicator<<1)+1;

	} else { //otherwise root to node's children to find leaves further down the graph
		//if aggFlag is enabled DON'T root to node's children (because then children in a correlation will get every childlevel twice)
		if(!aggFlag) {
			alc = NULL;
			arforeach(alc, node->children) {
				AddChildren((ModelGraphIndexNode *)arlfirst(alc), childList, tle, unique, 0);
			}
		}

		if(!unique ) {
			if(node->aggChild->children && ((ModelGraphIndexNode *)arlfirst(arlist_head(node->aggChild->children)))->parents->length>1) {
				AddChildren(node->aggChild, childList, tle, unique, 1);
			} else {
				AddChildren(node->aggChild, childList, tle, unique, 0);
			}
		}
	}
}

/*
 * add the whole correlation to EVERY leaf that is reachable from the passed ModelGraphIndexNode
 *
 * corrElements - a List which contains all Elements of the correlation(GraphAttributes)
 * corrData - contains all data necessary to build the correlation, grouped by the distinct values of the first corrElements
 * corrLevel - shows the actual level of the graph that should be built now
 */
void
AddCorrelationChild(ModelGraphIndexNode *node, List *corrElements, List *corrData, int corrLevel) {

	GraphAttribute		*nextCorrElement;
	TargetEntry			*tle;
	ModelGraphIndexNode	*child,
	                    *parent;
	ArListCell			*alc,
	                *alc2;
	ListCell			*lc1,
	                *lc2;
	List				*attValList = NIL,
	                     *helpList = NIL;
	CorrAttribute		*corrAtt,
	                 *newCorrAtt;
	Datum				*tuple;
	Var					*v;
	Datum				dat;
	bool 				exclude = false,
	                    found = false,
	                    equal = false;


	//if there are no children yet, add all entries of childList as a child
	if(!node->children) {
		if(corrLevel<corrElements->length) {
			nextCorrElement = (GraphAttribute *)list_nth(corrElements, corrLevel);
			tle = copyObject((TargetEntry *)nextCorrElement->attributeCol);

			//find the right CorrelationAttribute for node to extract the children from it
			foreach(lc1, corrData) { //foreach higher level-value
				corrAtt = (CorrAttribute *)lfirst(lc1);
				equal = compareDatum(node->value, corrAtt->dat, ((Var *)node->target->expr)->vartype);
				if(equal) {
					break;
				}
			}

			foreach(lc1, corrAtt->tupleList) { //foreach tuple of the corrAtt
				tuple = (Datum*)lfirst(lc1);

				//get needed datum from the current tuple
				dat = tuple[tle->resno-1];

				//check if the datum is in the exclude list
				exclude = false;
				v = (Var *)tle->expr;
				foreach(lc2, nextCorrElement->excludeList) {
					//if it is continue with the next tuple
					equal = compareDatum(((Const *)lfirst(lc2))->constvalue, dat, ((Const *)lfirst(lc2))->consttype);
					if(equal) {
						exclude = true;
						break;
					}
				}
				if(exclude) {
					continue;
				}

				//check if a tuple with the same datum was already found.
				found = false; //is necessary, because when attValList is empty, we would use an old value
				foreach(lc2, attValList) {
					found = compareDatum(((CorrAttribute *)lfirst(lc2))->dat, dat, v->vartype);
					if(found) {
						newCorrAtt = (CorrAttribute *)lfirst(lc2);
						break;
					}
				}

				//...if not, add it to the attValList
				if(!found) {
					newCorrAtt = palloc0(sizeof(CorrAttribute));
					newCorrAtt->dat = dat;
					newCorrAtt->tDesc = corrAtt->tDesc;
					attValList = lappend(attValList, newCorrAtt);
					helpList = lappend(helpList, &dat);
				}
				newCorrAtt->tupleList = lappend(newCorrAtt->tupleList, tuple);
			}

			//the first level can be built like normal children...
			AddChildren(node, helpList, tle, true,0);

			//the currently created children must also be added to the childList of the fathers AggNode(because of the unique constraint of the correlation)
			arforeach(alc, node->parents) {
				parent = (ModelGraphIndexNode *)arlfirst(alc);
				arforeach(alc2, node->children) {
					child = (ModelGraphIndexNode *)arlfirst(alc2);
					parent->aggChild->children = arslinsert(parent->aggChild->children, child, MginSorterAsc, 1);
					if(!parent->aggChild->aggChild) {
						parent->aggChild->aggChild = palloc0(sizeof(ModelGraphIndexNode));
						parent->aggChild->aggChild->models = NIL;
						parent->aggChild->aggChild->type = T_AggNode;
						parent->aggChild->aggChild->target = tle;
						parent->aggChild->aggChild->parents = arslinsert(parent->aggChild->aggChild->parents, parent->aggChild, MginSorterAsc, 2);
						parent->aggChild->aggChild->aggIndicator = (parent->aggChild->aggIndicator<<1)+1;
					}


					child->parents = arslinsert(child->parents, parent->aggChild,MginSorterAsc,2);
				}
			}
			//...the rest should get some special treatment
			AddCorrelationChild(node, corrElements, attValList, corrLevel+1);

			list_free(helpList);
			list_free_deep(attValList);
		}
	} else { //otherwise root to node's children to find leaves further down the graph

		arforeach(alc, node->children) {
			AddCorrelationChild((ModelGraphIndexNode *)arlfirst(alc), corrElements, corrData, corrLevel);
		}
	}
}


void CreateBackupTables(void)
{
	char* tableDef="CREATE TABLE pg_mgmodel (fillid oid, mginid int ,modelid int, name name, meassure text, time text, measueretbl smallint,ALGORITHMNAME name,granularity smallint, aggtype oid, timestamp integer, ecount DOUBLE precision, eval DOUBLE precision, SQL text,parameterList text) WITH OIDS;	CREATE TABLE pg_modelgraph (id name unique,SQL text);CREATE TABLE pg_mgdisagg (modelid int,modelmgin int, fillid oid, mginid int, disaggkeynumerator DOUBLE precision,disaggkeydenominator DOUBLE precision, upperBound int, lowerBound int);CREATE TABLE pg_mgparameter(modelid Oid, parameter smallint, value DOUBLE precision[]);CREATE TABLE pg_mgfilling(fillid oid, modelgraphid name, fillingname name) WITH OIDS;";
	Relation r=try_heap_openrv((const RangeVar *)makeRangeVar(NULL,"pg_mgmodel",-1), NoLock);
	if(r)
	{
		heap_close(r,NoLock);
		return;
	}

	runQuery(tableDef,"CREATE");
}

//XXX: TYPICAL FAIL: attValList = NIL; WAS FORGOTTEN!!!
void
CreateModelGraph(CreateModelGraphStmt *stmt) {

	GraphAttribute		*gAtt;
	TargetEntry 		*tle;
	Datum				*tuple;
	Var 				*v;
	Datum 				dat;
	CorrAttribute		*corrAtt;
	bool 				exclude = false,
	                    found = false;
	ListCell 			*lc1,
	               *lc2,
	               *lc3;
	List 				*attValList = NIL,
	                     *helpList = NIL;
	int 				attributeCount = 0, //counts how many levels the graph will have
	                    i;
	ModelGraphIndexNode *mgin = getModelGraphRoot(),
	                     *mgin2;
	List 				**datumCache = NULL; //Array of Lists to handle all distinct occurrences of each attribute
	if(mgin->children) elog(ERROR,"Modelgraph already existent");
	
	CreateBackupTables();
	
	fullGraphCreationSourceText=palloc0(strlen(stmt->sourcetext)+1);
	strcpy(fullGraphCreationSourceText,stmt->sourcetext);
	if(stmt->graphAttributeList) {
		datumCache = palloc0(sizeof(List *) * stmt->graphAttributeList->length);
	}

	//save the subquery of the creationQuery
	extractSubquery(stmt->sourcetext);

	/*
	 *build the correlations
	 */
	foreach(lc1, stmt->corrLists) { //foreach correlation
		gAtt = (GraphAttribute *)lfirst(list_head((List *)lfirst(lc1))); //for the first element of the correlation
		tle = (TargetEntry *)gAtt->attributeCol;

		attValList = NIL;

		foreach(lc2, stmt->tupleList) { //foreach cached tuple
			tuple = (Datum *)lfirst(lc2);

			//get needed datum from the current tuple
			dat = tuple[tle->resno-1];

			//check if the datum is in the exclude list...
			exclude = false;
			v = (Var *)tle->expr;
			foreach(lc3, gAtt->excludeList) {
				//...if it is remember that...
				found = compareDatum(((Const *)lfirst(lc3))->constvalue, dat, v->vartype);
				if(found) {
					exclude = true;
					break;
				}
			}
			if(exclude) {
				//...and continue with the next tuple
				continue;
			}

			//check if a tuple with the same datum was already found.
			found = false; //is necessary, because when attValList is empty, we would use an old value
			foreach(lc3, attValList) {
				found = compareDatum(((CorrAttribute *)lfirst(lc3))->dat, dat, v->vartype);
				if(found) {
					corrAtt = (CorrAttribute *)lfirst(lc3);
					break;
				}
			}

			//...if not, add it to the attValList
			if(!found) {
				corrAtt = palloc0(sizeof(CorrAttribute));
				corrAtt->dat = dat;
				corrAtt->tDesc = stmt->tDesc;
				attValList = lappend(attValList, corrAtt);
				helpList = lappend(helpList, &dat);
			}
			corrAtt->tupleList = lappend(corrAtt->tupleList, tuple);
		}

		//the first level can be built like normal children...
		AddChildren(mgin, helpList, copyObject(tle), true,0);
		//...the rest should get some special treatment
		AddCorrelationChild(mgin, (List *)lfirst(lc1), attValList, 1);

		list_free_deep(attValList);
		list_free(helpList);
	}

	//prepare all the values that should be modeled in the graph
	foreach(lc1, stmt->graphAttributeList) {
		gAtt = (GraphAttribute *)lfirst(lc1);
		tle = (TargetEntry *)gAtt->attributeCol;

		attValList = NIL;

		foreach(lc2, stmt->tupleList) {
			tuple = (Datum *)lfirst(lc2);

			if(!tuple)
				break;

			//get needed datum from the current tuple
			dat = tuple[tle->resno-1];

			//check if the datum is in the exclude list
			exclude = false;
			v = (Var *)tle->expr;
			foreach(lc3, gAtt->excludeList) {
				//if it is continue with the next tuple
				exclude = compareDatum(((Const *)lfirst(lc3))->constvalue, dat, v->vartype);
				if(exclude) {
					break;
				}
			}
			if(exclude) {
				continue;
			}

			//check if the datum already is in the attValList...
			found = false; //is necessary, because when attValList is empty, we would use an old value
			foreach(lc3, attValList) {
				found = compareDatum(((Datum)lfirst(lc3)), dat, v->vartype);
				if(found) {
					break;
				}
			}

			//...if not, add it
			if(!found) {
				attValList = lappend(attValList, (void *)dat);
			}
		}

		//save the attValList to the cache
		datumCache[attributeCount++] = attValList;
	}

	for(i = 0; i<attributeCount; ++i) {

		/*
		 * the Lists in datumCache are in the same order like the GraphAttributes in the graphAttributeList of the CreateModelGraphStmt
		 * we can use this here for synchronization of the TargetEntries an Datums, but have to keep it this way
		 */
		AddChildren(mgin, datumCache[i], copyObject((TargetEntry *)((GraphAttribute *)list_nth(stmt->graphAttributeList, i))->attributeCol), false,0);
	}

	//as a last step fill the targetEntryList of the rootNode for easy checking of compatibility
	mgin2 = mgin;
	while(mgin2->children) {
		mgin2 = (ModelGraphIndexNode *)arlfirst(arlist_head(mgin2->children));

		mgin->targetEntryList = lappend(mgin->targetEntryList, mgin2->target);
	}

	if(datumCache) {

		for(i = 0; i < attributeCount; ++i) {
			list_free(datumCache[i]); //no free_deep, because it elements of datumCache may contain int-Datums
		}
		pfree(datumCache);
	}

	elog(INFO, "ERGEBNIS: %i",modelGraphDepthCheck());
//	if(modelgraph_with_ids)
		SetModelGraphIds(NULL);
		modelGraphIdx->maxid=leafCount-1;
		SetModelGraphArray();
	releaseModelGraphRoot();

	foreach(lc1, stmt->tupleList) {
		pfree((Datum *)lfirst(lc1));
	}
	FreeTupleDesc(stmt->tDesc);
	list_free_deep(stmt->graphAttributeList);
	foreach(lc1, stmt->corrLists) {
		list_free_deep((List *)lfirst(lc1));
	}
}



int
modelGraphCompatible(Node* whereExpr, Query *qry) {
	ModelGraphIndexNode *root = getModelGraphRoot();
	List *tgel = root->targetEntryList;
	ListCell *lc;
	RangeTblEntry *rte;
	int found=0;
	Var *opvar;
	if(!root->children)
		elog(ERROR,"No Modelgraph found");

	if(whereExpr==NULL)
		return 1;

	switch (nodeTag(whereExpr)) {
	case T_OpExpr: {
		OpExpr *opExpr = (OpExpr *) whereExpr;

		switch(nodeTag(lfirst(list_head(opExpr->args)))){
		case T_Var:
			opvar = lfirst(list_head(opExpr->args));
			break;
		case T_RelabelType:
			opvar = (Var *)((RelabelType *)lfirst(list_head(opExpr->args)))->arg;
			break;
		default:
			elog(ERROR, "Unhandeled type in _LookUpAndClause.");
			break;
		}

		//if there is just one predicate and THIS IS A JOIN treat it as if there was no whereExpr
		if(nodeTag(opvar) == T_Var && nodeTag(lfirst(list_tail(opExpr->args))) == T_Var) {
			return 1;
		}

		//Check Var with TargetListEntrys in ModelgraphrootNode
		foreach(lc,tgel) {
			TargetEntry *te=lfirst(lc);
			if(((Var *)te->expr)->varattno != opvar->varattno)
				continue;
			rte = (RangeTblEntry*) list_nth(qry->rtable, opvar->varno-1);
			if(te->resorigtbl != rte->relid)
				continue;
			//found a matching TargetEntry
			found=1;
			break;
		}
		break;

	}
	case T_BoolExpr: {
		BoolExpr *boolExpr=(BoolExpr*) whereExpr;
		foreach(lc,boolExpr->args) {
			OpExpr *opex = lfirst(lc);
			if(nodeTag(opex) == T_OpExpr) {
				if(nodeTag(lfirst(list_head(opex->args))) == T_Var && nodeTag(lfirst(list_tail(opex->args))) == T_Var) {
					continue;
				}
			}
			found=modelGraphCompatible(lfirst(lc),qry);
//TODO: expectation of leftrecursion is WRONG
			if(!found)
				break;
		}
		break;

	}
	case T_ScalarArrayOpExpr: {
		ScalarArrayOpExpr* sAExpr =(ScalarArrayOpExpr*)whereExpr;
		Var *opvar= lfirst(list_head(sAExpr->args));
		//Check var with TargetListEntrys in ModelgraphrootNode
		foreach(lc,tgel) {
			TargetEntry *te=lfirst(lc);
			if(te->resno!=opvar->varattno)
				continue;
			rte = (RangeTblEntry*) list_nth(qry->rtable, opvar->varno-1);
			if(te->resorigtbl!=rte->relid)
				continue;
			//we found a matching TargetEntry
			found=1;
			break;
		}
		break;

	}
	default:
		elog(ERROR, "error while matching whereExpr, unknown Nodetype");
		break;
	}

	releaseModelGraphRoot();

	return found;
}


A_Expr *
CreateA_Expr(TargetEntry *target, Datum value) {

	int16		resultTypLen;
	bool		resultTypByVal;
	A_Const		*acon = makeNode(A_Const);
	Var			*v;
	ColumnRef	*colRef = makeNode(ColumnRef);
	colRef->fields = NIL;
	colRef->fields = lappend(colRef->fields, makeString(target->resname));
	colRef->location = -1;

	if(!IsA(target->expr, Aggref)) {
		v = (Var *)target->expr;
	} else { //Aggref case
		v = (Var *)lfirst(list_head(((Aggref *)target->expr)->args));
	}

	get_typlenbyval(v->vartype, &resultTypLen, &resultTypByVal);

	switch(v->vartype) {
	case 20:
	case 21:
	case 23: {
		acon->val = *makeInteger(GetDatumAsInt(v->vartype, value));
		break;
	}
	case 700: {
		char *temp = DatumGetCString(DirectFunctionCall1(float4out, DatumGetFloat4(value)));
		acon->val = *makeFloat(temp);
		break;
	}
	case 701: {
		char *temp = DatumGetCString(DirectFunctionCall1(float8out, DatumGetFloat4(value)));
		acon->val = *makeFloat(temp);
		break;
	}
	case 1042: {
		char *temp = DatumGetCString(DirectFunctionCall1(bpcharout,value));
		acon->val = *makeString(temp);
		break;
	}
	case 1043: {
		acon->val = *makeString(DatumGetCString(DirectFunctionCall1(varcharout,value)));
		break;
	}
	case 1700: {
		char *temp = DatumGetCString(DirectFunctionCall1(numeric_out, NumericGetDatum(value)));
		acon->val = *makeFloat(temp);
		break;
	}
	}

	acon->location = -1;


	return makeSimpleA_Expr(AEXPR_OP, "=", (Node*)colRef, (Node*)acon, -1);
}

void
FreeA_Expr(A_Expr *aexp, bool withJoins) {

	if(aexp->kind == AEXPR_AND) {
		FreeA_Expr((A_Expr *)aexp->lexpr, withJoins);
		FreeA_Expr((A_Expr *)aexp->rexpr, withJoins);
		pfree(aexp);
	} else if(aexp->kind == AEXPR_OP) {
		if(!withJoins && nodeTag(((A_Expr *)aexp)->lexpr) == T_ColumnRef && nodeTag(((A_Expr *)aexp)->rexpr) == T_ColumnRef)
			return;
		//free the lexpr
		list_free_deep(((ColumnRef *)aexp->lexpr)->fields);
		pfree(aexp->lexpr);

		//free the rexpr
		pfree(aexp->rexpr);

		//free the nameList
		list_free_deep(aexp->name);
	} else {
		elog(INFO, "The function FreeA_Expr was not designed for A_Expr_Kind %i!", aexp->kind);
	}
}

List *
ExtractJoins(Node *whereExpr) {

	List 	*res = NIL;
	A_Expr	*aexp = (A_Expr *)whereExpr;

	if(!whereExpr)
		return NIL;

	switch(aexp->kind) {
	case AEXPR_OP: {
		//check if it is a JOIN and, in case it is, add it to res
		if(nodeTag(aexp->lexpr) == T_ColumnRef && nodeTag(aexp->rexpr) == T_ColumnRef) {
			res = lappend(res, aexp);
		}
		break;
	}
	default: {
		res = list_concat(res, ExtractJoins(aexp->lexpr));
		res = list_concat(res, ExtractJoins(aexp->rexpr));
		break;
	}
	}

	return res;
}

A_Expr *
RestructureA_Expr(List *exprList) {

	if(list_length(exprList) == 2) {
		return makeSimpleA_Expr(AEXPR_AND, "=", lfirst(list_head(exprList)), lfirst(list_tail(exprList)), -1);
	} else {
		return makeSimpleA_Expr(AEXPR_AND, "=", (Node*)RestructureA_Expr(list_delete_first(list_copy(exprList))), lfirst(list_head(exprList)), -1);
	}
}

double getNextDisAggValue(DisAggModel *dam, int num){
	if(dam->givenDisAggKey!=0){
		return getNextValue(dam->model, num)*dam->givenDisAggKey;
	}else{
		return getNextValue(dam->model, num)*(dam->disAggKeyNumerator/dam->model->disAggKeyDenominator);
	}
}

float
CreateDisaggScheme(CreateDisAggSchemeStmt *stmt, float sourceSum){

	ModelGraphIndexNode *mgin = getModelGraphRoot(),
						*target,
						*source,
						*iter;
	ModelInfo 			*temp;
	List 				*parsetree_list = NIL,
						*query_list = NIL,
						*planned_list = NIL,
						*exprList = NIL;
	ListCell			*lc,
						*lc2,
						*lc3;
	AlgorithmParameter	*findPar;
	bool				paraFound = true;
	DisAggModel			*disAggModel = palloc0(sizeof(DisAggModel));
	SelectStmt			*selStmt;
	const char			*commandTag;
	Portal				portal;
	DestReceiver 		*tupledest;
	TupleDesc			targettDesc;
	A_Expr				*apex;
	bool 				timeMeasureRigth,
						isnull = true;
	Datum				tarDat;
	float				targetSum;
	Oid					typOid;
	char 				*queryString;

	if(!mgin->children)
		elog(ERROR, "There is no Modelgraph to store the DisAggScheme, you have to create one first!");


	//find the ModelGraphIndexNode which contain the Model to disaggregate from
	source = FindCorrectNode(mgin, copyObject(stmt->source), stmt->rTable);

	//find the ModelGraphIndexNode to store the disaggregationModel
	target = FindCorrectNode(mgin, copyObject(stmt->target), stmt->rTable);
//TODO:_getModels nutzen nachdem Chris die gefixt(measurespaltenvergleich) hat
	//search the models in source to find a fitting one
	foreach(lc, source->models){
		temp = (ModelInfo *)lfirst(lc);

		//compare this Model with the ModelInfo from stmt
		if(IsA(((TargetEntry *)stmt->measure)->expr, Var))
		{
			timeMeasureRigth = (temp->measure->resorigcol == ((Var *)((TargetEntry *)stmt->measure)->expr)->varattno) && (temp->time->resorigcol == ((Var *)((TargetEntry *)stmt->time)->expr)->varattno);
			//provide the measure typOid of the measure column for later use
			typOid = ((Var *)((TargetEntry *)(stmt->measure))->expr)->vartype;
		}
		else//case for T_Aggref
		{
			Var	*v = lfirst(list_head(((Aggref *)((TargetEntry *)stmt->measure)->expr)->args));
			timeMeasureRigth = (temp->measure->resorigcol ==  v->varattno) && (temp->time->resorigcol == ((Var *)((TargetEntry *)stmt->time)->expr)->varattno);
			//provide the measure typOid of the measure column for later use
			typOid = v->vartype;
		}

		if (timeMeasureRigth) {
			// exact model found
//TODO: check Granularity ?!               if (temp->granularity == granularity){
			//check used algorithm and go on with the next model if its not the same
			if(stmt->algorithm->algorithmname && strcmp(stmt->algorithm->algorithmname, getModelTypeAsString(temp->forecastMethod)) != 0){
				paraFound=false;
				continue;
			}
			if(stmt->algorithm->algorithmparameter != NIL)
			{
				foreach(lc3, stmt->algorithm->algorithmparameter)
				{
					AlgorithmParameter *searchPar = (AlgorithmParameter*) lfirst(lc3);
					foreach(lc2, temp->parameterList)
					{
						int paraEqual;
						paraFound=false;
						findPar = (AlgorithmParameter*) lfirst(lc2);

						switch (searchPar->value->val.type) {
							case T_Integer:
								if(findPar->value->val.type!=T_Integer)
									continue;
								paraEqual = !(((searchPar->value->val.val.ival)==(int)(findPar->value->val.val.ival)));
								break;
							case T_String:
								if(findPar->value->val.type!=T_String)
									continue;
								paraEqual = strcmp(searchPar->value->val.val.str, findPar->value->val.val.str);
								break;
							default:
								break;
						}
						if(paraEqual==0 && strcmp(searchPar->key,findPar->key)==0)
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
				disAggModel->model = temp;
			}
		}
	}

	//when no model was found -> error
	if(!disAggModel->model){
		elog(ERROR, "No fitting Model was found!");
	}

	//when there is an unsergiven disAggKey we foreward it to the new disAggModel...
	if(stmt->disAggKey){
		disAggModel->updatable = false;
		if(((Value *)stmt->disAggKey)->type == T_Integer){
			disAggModel->givenDisAggKey = ((Value *)stmt->disAggKey)->val.ival;
		}else if(((Value *)stmt->disAggKey)->type == T_Float){
			disAggModel->givenDisAggKey =  atof(((Value *)stmt->disAggKey)->val.str);
		}else{
			ereport(ERROR, (errcode(ERRCODE_DATATYPE_MISMATCH), errmsg("keytype not supported")));
		}
	}else{ //otherwise calculate the disAggKey by executing two queries
		disAggModel->givenDisAggKey=0.0;
	//first the part of the existing Model
		//prepend an aggregation to the training_data-String
		queryString = palloc0(strlen("SELECT sum(") + strlen(disAggModel->model->measure->resname) + strlen(") FROM(") + strlen(disAggModel->model->trainingData) + strlen(") as tbl")+1);

		queryString = strcat(queryString, "SELECT sum(");
		queryString = strcat(queryString, disAggModel->model->measure->resname);
		queryString = strcat(queryString, ") FROM(");
		queryString = strcat(queryString, disAggModel->model->trainingData);
		queryString = strcat(queryString, ") as tbl");

		//plan the stored trainingData from the found model and execute the Query via a portal
		parsetree_list = pg_parse_query(queryString);

		//now swap the whereClause of the SelectStmt in parsetree_list, to fit target
		selStmt = (SelectStmt *)lfirst(list_head(parsetree_list));

		//copy the JOIN-predicates from the training_data-Query of the found Model
		exprList = ExtractJoins(((SelectStmt *)((RangeSubselect *)lfirst(list_head(selStmt->fromClause)))->subquery)->whereClause);

		//iterate from target to root and collect the corresponding predicates for the NEW whereClause
		iter = target;
		while(iter->parents){

			//if iter is an AggNode, don't add a predicate to the NEW whereClause
			if(iter->type != T_AggNode)
				exprList = lappend(exprList, CreateA_Expr(iter->target, iter->value));

			if(((ModelGraphIndexNode *)arlfirst(arlist_head(iter->parents)))->type == T_IndexNode){
				iter = (ModelGraphIndexNode *)arlfirst(arlist_head(iter->parents));
			}else{
				iter = (ModelGraphIndexNode *)arlfirst(arlist_tail(iter->parents));
			}
		}

		//restructure the NEW whereClause to be left-recursive
		if(exprList){
			if(list_length(exprList)>=2){
				apex = RestructureA_Expr(exprList);
			}else{
				apex = lfirst(list_head(exprList));
			}
		}
//XXX:works not properly for aggregated columns it seems
		//put the NEW whereClause
		((SelectStmt *)((RangeSubselect *)lfirst(list_head(selStmt->fromClause)))->subquery)->whereClause = (Node *)apex;

		query_list = pg_analyze_and_rewrite((Node *)selStmt, queryString, NULL, 0);

		planned_list = pg_plan_queries(query_list, 0, NULL);
		// create command Tag
		commandTag = "SELECT";

		// results should be send to the ModelReceiver, to use the CreateModelGraphDestReceiver is kind of abuse here, but he can anything we need ;)
		tupledest = CreateModelGraphDestReceiver();

		// Create a new portal to run the query in
		portal = CreateNewPortal();

		//Don't display the portal in pg_cursors, it is for internal use only
		portal->visible = false;

		PortalDefineQuery(portal, NULL, stmt->sourcetext, commandTag, planned_list, NULL);

		//  Start the portal.  No parameters here.
		PortalStart(portal, NULL, InvalidSnapshot);

		(void) PortalRun(portal, FETCH_ALL, false, tupledest, tupledest, NULL);

		//copy the resulttuple and the corresponding TupleDesc for the existingModel
		targettDesc = CreateTupleDescCopy(((ModelGraphState *)tupledest)->tDesc);

		//if isnull is set for THE ONE received field, don't use it
		if(!((bool *)lfirst(list_head(((ModelGraphState *)tupledest)->isnullList)))[0]){
			tarDat = datumCopy(((Datum *)lfirst(list_head(((ModelGraphState *)tupledest)->tupleList)))[0], ((ModelGraphState *)tupledest)->tDesc->attrs[0]->attbyval, ((ModelGraphState *)tupledest)->tDesc->attrs[0]->attlen);

			targetSum = GetDatumAsDouble(typOid, tarDat);
			isnull = false;
		}

		(*tupledest->rDestroy) (tupledest);

		PortalDrop(portal, false);

		//if isnull is true we don't received a targetTuple, this happens in case there exist no Tuple for this path in the tree
		if(isnull){
			FreeTupleDesc(targettDesc);
			pfree(disAggModel);
			list_free_deep(parsetree_list);
			list_free_deep(query_list);
			list_free_deep(planned_list);
			list_free_deep(exprList);

			releaseModelGraphRoot();
			return 0;
		}
		//calculate the disAggKey
//XXX:should work this way, previously there was a distinction bewtween Aggref and Var, but I think this is NOT important, the mistake must be somewhere else
		disAggModel->disAggKeyNumerator = targetSum;

		elog(INFO, "Calculated DisAggKey = %f", disAggModel->disAggKeyNumerator/disAggModel->model->disAggKeyDenominator);

		list_free_deep(parsetree_list);
		list_free_deep(query_list);
		list_free_deep(planned_list);
		list_free_deep(exprList);
	}

	//save the now complete disModel
	target->disAggModels = lappend(target->disAggModels, disAggModel);

	FreeTupleDesc(targettDesc);

	releaseModelGraphRoot();


	return sourceSum;
}

/*
 * Returns a List of ALL children that are reachable from the given node
 *
 * inMgin - the ModelGraphIndexNode from which to all his reachable children is rooted
 * 			if inMgin = NULL the ModelGraphRootNode is accessed -> so ALL children of the ModelGraph are returned
 * withAggs - used to determine if the AggLeafNodes should be delivered or not
 */
List *
GetAllChildren(ModelGraphIndexNode *inMgin, bool withAggs) {

	ModelGraphIndexNode	*mgin;
	List				*res = NIL;
	ArListCell			*alc;

	//if an inMgin is given use it, otherwise use the rootNode
	if(inMgin)
		mgin = inMgin;
	else
		mgin = getModelGraphRoot();

	//when mgin is a childNode return a List only containing mgin
	if(!mgin->children) {
		res = lappend(res, mgin);
		if(!inMgin)
			releaseModelGraphRoot();
		return res;
	}

	//when mgin is no child root to mgins children...
	if(IsA(mgin, AggNode) && ((ModelGraphIndexNode *)arlfirst(arlist_head(mgin->children)))->parents->length>1) {
		//this is the bad case and we skip all relevant things here ;)
		//so no duplicate Nodes will appear in the returned List
	} else { //otherwise mgin is no AggNode or its don't have two parents
		arforeach(alc, mgin->children) {
			res = list_concat(res, GetAllChildren(arlfirst(alc), withAggs));
		}
	}

	//...and, if wished, also root to the aggChild
	if(withAggs)
		res = list_concat_unique_ptr(res, GetAllChildren(mgin->aggChild, withAggs));

	if(!inMgin)
		releaseModelGraphRoot();

	return res;
}


static void _setModelGraphArray(ModelGraphIndexNode *mgin)
{
	ArListCell *alc;
	if(!mgin->children) {
		modelGraphIdx->leafArray[mgin->id]=mgin;
		return;
	}

	//when mgin is no child root to mgins children...
	if(IsA(mgin, AggNode) && ((ModelGraphIndexNode *)arlfirst(arlist_head(mgin->children)))->parents->length>1) {
		//this is the bad case and we skip all relevant things here ;)
		//so no duplicate Nodes will appear in the returned List
	} else { //otherwise mgin is no AggNode or its don't have two parents
		arforeach(alc, mgin->children) {
			_setModelGraphArray(arlfirst(alc));
		}
	}

	_setModelGraphArray(mgin->aggChild);
	return;
	
}

void
SetModelGraphArray(void)
{
	modelGraphIdx->leafArray=(ModelGraphIndexNode**)palloc0(sizeof(ModelGraphIndexNode*)*(modelGraphIdx->maxid+1));
	_setModelGraphArray(getModelGraphRoot());
	releaseModelGraphRoot();
}



List *
SetModelGraphIds(ModelGraphIndexNode *inMgin) {

	ModelGraphIndexNode	*mgin;
	List				*res = NIL;
	ArListCell			*alc;

	//if an inMgin is given use it, otherwise use the rootNode
	if(inMgin)
		mgin = inMgin;
	else
		mgin = getModelGraphRoot();

	//when mgin is a childNode return a List only containing mgin
	if(!mgin->children) {
		res = lappend(res, mgin);
		mgin->id=leafCount++;
		if(!inMgin)
			releaseModelGraphRoot();
		return res;
	}

	//when mgin is no child root to mgins children...
	if(IsA(mgin, AggNode) && ((ModelGraphIndexNode *)arlfirst(arlist_head(mgin->children)))->parents->length>1) {
		//this is the bad case and we skip all relevant things here ;)
		//so no duplicate Nodes will appear in the returned List
	} else { //otherwise mgin is no AggNode or its don't have two parents
		arforeach(alc, mgin->children) {
			res = SetModelGraphIds(arlfirst(alc));
		}
	}

		

	SetModelGraphIds(mgin->aggChild);

	if(!inMgin)
		releaseModelGraphRoot();
	return res;
}


/*
 *
 */
void
FillModelGraph(FillModelGraphStmt *stmt) {

	struct timespec start, end;
	uint64_t timeElapsed;
	if(!isModelGraphExistent())
		elog(ERROR,"No modelgraph found");
	EmptyModelGraph();

	clock_gettime(CLOCK_MONOTONIC, &start);

	switch(stmt->fillMethode) {
	case 0:	//TOPDOWN
		FillModelGraphTopDown(stmt);
		break;
	case 1: //BOTTONUP
		FillModelGraphBottomUp(stmt);
		break;
	case 2: //GREEDY
		FillModelGraphGreedy(stmt);
		break;
	}

	clock_gettime(CLOCK_MONOTONIC, &end);

	timeElapsed = (((&end)->tv_sec * 1000000) + (&end)->tv_nsec/1000) - (((&start)->tv_sec * 1000000) + (&start)->tv_nsec/1000);
	printf("Time elapsed: %llu ms\n", timeElapsed/1000);
}

void
FillModelGraphTopDown(FillModelGraphStmt *stmt) {

	List			*query_list = NIL,
	                 *planned_list = NIL,
	                  *exprList = NIL,
	                   *exprList2 = NIL,
	                    *children = NIL;
	ListCell		*lc;
	ForecastExpr	*forecast = makeNode(ForecastExpr);
	SelectStmt 		*selStmt;
	A_Expr			*aexp = NULL,
	                 *aexp2 = NULL;
	Portal			portal;
	char			*commandTag,
	            *compleationTag;
	DestReceiver 	*tupledest;
	float			sourceSum = 0;

	//create a "ForecastStmt" to create a Model for root->Agg->Agg->...->Agg
	forecast->modelName = "TheModel";
	forecast->measure = stmt->measure;
	forecast->time = stmt->time;
	forecast->length = -2; /*important for detection of CreateModelStmt*/
	forecast->algorithm = stmt->algorithm;
	forecast->storeModel = 4;

	//if the user entered some predicates within the training_data, kill 'em all, except they are JOINS
	if(((SelectStmt *)stmt->algorithm->trainingdata)->whereClause) {
		exprList = ExtractJoins(((SelectStmt *)stmt->algorithm->trainingdata)->whereClause);

		switch(list_length(exprList)) {
		case 0:
			aexp = NULL;
			break;
		case 1:
			aexp = lfirst(list_head(exprList));
			break;
		default:
			aexp = RestructureA_Expr(exprList);
			break;
		}
		((SelectStmt *)stmt->algorithm->trainingdata)->whereClause = (Node *)aexp;
		elog(INFO, "All nonJOIN-predicates of the training_data have been deleted!");
	}

	//instantiate a SelecctStmt to perform the ForecastStmt with, plan it and execute it with a Portal
	selStmt = copyObject(stmt->algorithm->trainingdata);
	selStmt->forecastExpr = (Node *)forecast;

	query_list = pg_analyze_and_rewrite((Node *)selStmt, stmt->sourcetext, NULL, 0);

	planned_list = pg_plan_queries(query_list, 0, NULL);
	// create command Tag
	commandTag = "SELECT";

	// results are not needed here but only one Tuple is returned, so the overhead is bearable
	tupledest = CreateModelGraphDestReceiver();

	// Create a new portal to run the query in
	portal = CreateNewPortal();

	//Don't display the portal in pg_cursors, it is for internal use only
	portal->visible = false;

	PortalDefineQuery(portal, NULL, stmt->sourcetext, commandTag, planned_list, NULL);

	//  Start the portal.  No parameters here.
	PortalStart(portal, NULL, InvalidSnapshot);

	(void) PortalRun(portal, FETCH_ALL, false, tupledest, tupledest, NULL);

	(*tupledest->rDestroy) (tupledest);

	PortalDrop(portal, false);

	//Execute a CreateDisAggSchemeStmt for every other leave of the ModelGraph
	children = GetAllChildren(NULL, true);

	foreach(lc, children) {
		ModelGraphIndexNode *iter = lfirst(lc),
		                     *iter2 = lfirst(lc);

		//instantiate a CreateDisAggSchemeStmt for every child to equip it with a DisAggModel
		CreateDisAggSchemeStmt *cdsStmt = makeNode(CreateDisAggSchemeStmt);
		cdsStmt->source = (Node *)aexp;
		//need a second exprList for the foreach-loop
		exprList2 = list_copy(exprList);

		//build the new whereClause for the current node by rooting to the rootNode
		while(iter->parents) {
			//if iter is an AggNode, don't add a predicate to the NEW whereClause
			if(iter->type != T_AggNode)
				exprList2 = lappend(exprList2, CreateA_Expr(iter->target, iter->value));

			if(((ModelGraphIndexNode *)arlfirst(arlist_head(iter->parents)))->type == T_IndexNode) {
				iter = (ModelGraphIndexNode *)arlfirst(arlist_head(iter->parents));
			} else {
				iter = (ModelGraphIndexNode *)arlfirst(arlist_tail(iter->parents));
			}
		}

		//restructure the NEW whereClause to be left-recursive
		if(exprList2){

			if(list_length(exprList2)>=2) {
				aexp2 = RestructureA_Expr(exprList2);
			} else if(exprList2) {
				aexp2 = lfirst(list_head(exprList2));
			}
			cdsStmt->target = (Node *)aexp2;
		}
		else
		{
			cdsStmt->target = (Node *)NULL;
		}


			cdsStmt->time = stmt->time;
			cdsStmt->measure = stmt->measure;
			cdsStmt->algorithm = stmt->algorithm;
			compleationTag = "FILL MODELGRAPH";

			transformCreateDisAggSchemeStmt(cdsStmt, stmt->sourcetext, compleationTag);
		

		//skip root->Agg->Agg->...->Agg
		if(iter2 == FindCorrectNode(NULL, cdsStmt->source, cdsStmt->rTable))
			continue;

		if(sourceSum == 0)
		{
			sourceSum = CreateDisaggScheme(cdsStmt, 0);
		} else
		{
			CreateDisaggScheme(cdsStmt, sourceSum);
		}

		//an flat free is sufficient because everything that is to free will be free'd from the following FreeA_Expr(aexp2, false)
		list_free(exprList2);
		FreeA_Expr(aexp2, false);
		pfree(cdsStmt);

	}

	if(aexp)
		FreeA_Expr(aexp, true);
	list_free(children); //NEVER deep_free this list, this would destroy some parts of the ModelGraph
	list_free_deep(query_list);
	list_free_deep(planned_list);
}

void
FillModelGraphBottomUp(FillModelGraphStmt *stmt) {

	List				*query_list = NIL,
						*planned_list = NIL,
						*exprList = NIL,
						*nodesConsidered = NIL;
	StringInfo 			name;
	ListCell			*lc;
	ForecastExpr		*forecast = makeNode(ForecastExpr);
	SelectStmt 			*selStmt;
	A_Expr				*aexp = NULL;
	Portal				portal;
	char				*commandTag;
	DestReceiver 		*tupledest;
	ModelGraphIndexNode *iter;

	//initialize the forecastExpression, so only the name has to be changed
	forecast->measure = stmt->measure;
	forecast->time = stmt->time;
	forecast->length = -2; /*important for detection of CreateModelStmt*/
	forecast->algorithm = stmt->algorithm;
	forecast->storeModel = 4;

	//instantiate a SelecctStmt to perform the ForecastStmt with
	selStmt = copyObject(stmt->algorithm->trainingdata);
	selStmt->forecastExpr = (Node *)forecast;

	//get all nodes we need to build a model for
	nodesConsidered = GetAllChildren(NULL, false);

	foreach(lc, nodesConsidered){

		exprList = NIL;
		//if the user entered some predicates within the training_data, kill 'em all, except they are JOINS
		if(((SelectStmt *)stmt->algorithm->trainingdata)->whereClause){
			exprList = ExtractJoins(((SelectStmt *)stmt->algorithm->trainingdata)->whereClause);
			elog(INFO, "All nonJOIN-predicates of the training_data have been deleted!");
		}

		iter = (ModelGraphIndexNode *)lfirst(lc);

		while(iter->parents)
		{
			//if iter is an AggNode, don't add a predicate to the NEW whereClause
			if(iter->type != T_AggNode)
				exprList = lappend(exprList, CreateA_Expr(iter->target, iter->value));

			if(((ModelGraphIndexNode *)arlfirst(arlist_head(iter->parents)))->type == T_IndexNode){
				iter = (ModelGraphIndexNode *)arlfirst(arlist_head(iter->parents));
			}else{
				iter = (ModelGraphIndexNode *)arlfirst(arlist_tail(iter->parents));
			}
		}

		switch(list_length(exprList)){
		case 0:
			aexp = NULL;
			break;
		case 1:
			aexp = lfirst(list_head(exprList));
			break;
		default:
			aexp = RestructureA_Expr(exprList);
			break;
		}

		selStmt = copyObject(stmt->algorithm->trainingdata);
		selStmt->whereClause = (Node *)aexp;
		selStmt->forecastExpr = copyObject(forecast);

		//set the name for the next new model
		name = makeStringInfo();
		appendStringInfo(name, "m%i", ((ModelGraphIndexNode *)lfirst(lc))->id);
		((ForecastExpr *)selStmt->forecastExpr)->modelName= name->data;

		query_list = pg_analyze_and_rewrite((Node *)selStmt, stmt->sourcetext, NULL, 0);

		planned_list = pg_plan_queries(query_list, 0, NULL);
		// create command Tag
		commandTag = "SELECT";

		// results are not needed here but only one Tuple is returned, so the overhead is bearable
		tupledest = CreateModelGraphDestReceiver();

		// Create a new portal to run the query in
		portal = CreateNewPortal();

		//Don't display the portal in pg_cursors, it is for internal use only
		portal->visible = false;

		PortalDefineQuery(portal, NULL, stmt->sourcetext, commandTag, planned_list, NULL);

		//  Start the portal.  No parameters here.
		PortalStart(portal, NULL, InvalidSnapshot);

		(void) PortalRun(portal, FETCH_ALL, false, tupledest, tupledest, NULL);

		(*tupledest->rDestroy) (tupledest);

		PortalDrop(portal, false);

	}
}

void EmptyModelGraph(void){
	List				*allChildren = GetAllChildren(NULL, true);
	ListCell			*lc;
	ModelGraphIndexNode	*mgin;

	foreach(lc, allChildren){
		mgin = (ModelGraphIndexNode *)lfirst(lc);

		list_free_deep(mgin->models);
		mgin->models = NIL;
		list_free_deep(mgin->disAggModels);
		mgin->disAggModels = NIL;
	}

	modelGraphIdx = palloc0(sizeof(ModelGraphIndex));
}

void UpdateModelInfoErrorArray(ModelInfo *model, double error){


//		StringInfo s = makeStringInfo();
//		appendStringInfo(s, "/home/hartmann/Desktop/BelegSVN/ClaudioHartmann/data/Potentialanalyse/modelParameter.csv");
//
//if(((ModelGraphIndexNode *)model->mix)->id==39){
//		StringInfo data = makeStringInfo();
//		appendStringInfo(data, "%lf\t %lf\t  %lf", ((HWModel *)model->model)->super.season[((HWModel *)model->model)->super.actualForecast], *(((HWModel *)model->model)->super.trend), *(((HWModel *)model->model)->super.level));
//		printDebug(s->data,data->data);
//}

	if(model->sizeOfErrorArray<model->lowerBound)//not enough tuples to calculate an errorValue
	{
		model->errorArray[model->sizeOfErrorArray++] = error;

		//the array once was filled, but reestimation resetted lengthOfErrorArray
		if(model->errorArray[model->upperBound-1] != 0){

			model->errorSMAPE = meanOfDoubleArray(model->errorArray, model->upperBound);
		}else
			model->errorSMAPE = meanOfDoubleArray(model->errorArray, model->sizeOfErrorArray);

		if(model->errorSMAPE>model->errorSSE*1.2  || sdf==1 || sdf==4){
			modelsToReestimate = lappend(modelsToReestimate, model);
		}
	}
	else {
		if(model->sizeOfErrorArray<model->upperBound)//calculate errorValue, but no shift of the array is neccesarry
		{
			if(model->sizeOfErrorArray==model->lowerBound){
				if(model->errorArray[model->upperBound-1] != 0)
					model->errorSSE = meanOfDoubleArray(model->errorArray, model->upperBound);
				else
					model->errorSSE = meanOfDoubleArray(model->errorArray, model->sizeOfErrorArray);
			}

			model->errorArray[model->sizeOfErrorArray++] = error;
			model->errorSMAPE = meanOfDoubleArray(model->errorArray, model->sizeOfErrorArray);

			//TODO fehler Prüfen und ggf. Aktion einleiten
			if(model->errorSMAPE>model->errorSSE*1.2  || sdf==1 || sdf==4){
				modelsToReestimate = lappend(modelsToReestimate, model);
			}
		}else{ //shift the array before the error is inserted

			if(model->sizeOfErrorArray==model->lowerBound && model->errorML==0){
				if(model->errorArray[model->upperBound-1] != 0)
					model->errorSSE = meanOfDoubleArray(model->errorArray, model->upperBound);
				else
					model->errorSSE = meanOfDoubleArray(model->errorArray, model->sizeOfErrorArray);
				model->errorML=1.0;
			}

			memmove(model->errorArray, &(model->errorArray[1]), (model->sizeOfErrorArray-1)*sizeof(double));
			model->errorArray[model->sizeOfErrorArray-1] = error;
			model->errorSMAPE = meanOfDoubleArray(model->errorArray, model->sizeOfErrorArray);

			//TODO fehler Prüfen und ggf. Aktion einleiten
			if(model->errorSMAPE>model->errorSSE*1.2 || sdf==1 || sdf==4){
				modelsToReestimate = lappend(modelsToReestimate, model);
			}
		}
	}
}

void UpdateDisAggModelErrorArray(DisAggModel *dam, double error){

	if(dam->sizeOfErrorArray<dam->lowerBound)//not enough tuples to calculate an errorValue
	{
		dam->errorArray[dam->sizeOfErrorArray++] = error;

		if(dam->errorArray[dam->upperBound-1] != 0){
			dam->errorSMAPE = meanOfDoubleArray(dam->errorArray, dam->upperBound);
		}else
			dam->errorSMAPE = meanOfDoubleArray(dam->errorArray, dam->sizeOfErrorArray);
		//TODO fehler Prüfen und ggf. Aktion einleiten
		//TODO ggf. ein Modell f. d. Knoten erstellen
	}
	else
	{
		if(dam->sizeOfErrorArray<dam->upperBound)//calculate errorValue, but no shift of the array is neccesarry
		{
			dam->errorArray[dam->sizeOfErrorArray++] = error;

			dam->errorSMAPE = meanOfDoubleArray(dam->errorArray, dam->sizeOfErrorArray);
			//TODO fehler Prüfen und ggf. Aktion einleiten
			//TODO ggf. ein Modell f. d. Knoten erstellen
		}
		else //shift the array before the error is calculated
		{
			memmove(dam->errorArray, &(dam->errorArray[1]), (dam->sizeOfErrorArray-1)*sizeof(double));
			dam->errorArray[dam->sizeOfErrorArray-1] = error;
			dam->errorSMAPE = meanOfDoubleArray(dam->errorArray, dam->sizeOfErrorArray);
			//TODO fehler Prüfen und ggf. Aktion einleiten
			//TODO ggf. ein Modell f. d. Knoten erstellen
		}
	}
}

void MaintainModelGraph(HeapTuple newTuple, Relation sourceRelation){

	ModelGraphIndexNode	*mgin = getModelGraphRoot();
	Oid 				relOid,
						typOid;
	TargetEntry			*tle;
	Datum				*values;
	bool				*isNull;
	TupleDesc			tDesc;
	ListCell			*lc;
	int					node,
						i;
	ModelInfo			*model;
	DisAggModel			*dam;
	Var					*v;
	double				f,
						t,
						error = 0.0;
	struct timespec 	start, end;
	uint64_t timeElapsed;

	//if no Modelgraph was created -> there is nothing to update
	if(mgin->children)
		relOid = ((ModelGraphIndexNode *)arlinitial(mgin->children))->target->resorigtbl;
	else //root has no children, nothing to update
		return;

	if(RelationGetRelid(sourceRelation) != relOid)//the inserted Tuples don't affect the relation the ModelGraph was created for
		return;

	tDesc = RelationGetDescr(sourceRelation);
	values = (Datum *) palloc(tDesc->natts * sizeof(Datum));
	isNull = (bool *) palloc(tDesc->natts * sizeof(bool));
	heap_deform_tuple(newTuple, tDesc, values, isNull);

	while(mgin->children){
		tle = ((ModelGraphIndexNode *)arlinitial(mgin->children))->target;
		typOid = getTEVar(tle)->vartype;

		node = find(mgin->children, (void *)values[tle->resno-1], Mgin_Char_SorterAsc);
		mgin = arlist_get(mgin->children,node);

		//in case a tuple for modeled attribute was inserted but the value has no node at this level just return, there is nothing to do for this inserted tuple
		if(mgin==NULL)
			return;
	}

	foreach(lc, mgin->models){
		model = ((ModelInfo *)lfirst(lc));
		v = getTEVar(model->measure);
		t = GetDatumAsDouble(v->vartype, values[v->varoattno-1]);

		f = getNextValue(model, 1);

		error = SMAPE(f,t);

		UpdateModelInfoErrorArray(model, error);

		incrementalUpdate(model, GetDatumAsDouble(v->vartype, values[v->varoattno-1]), GetDatumAsInt(((Var *)model->time->expr)->vartype, values[((Var *)model->time->expr)->varoattno-1]));
		model->insertCount = model->insertCount+1;
	}


	foreach(lc, mgin->disAggModels){
		dam = ((DisAggModel *)lfirst(lc));
		v = getTEVar(dam->model->measure);
		t = GetDatumAsDouble(v->vartype, values[v->varoattno-1]);
		f = getNextDisAggValue(dam, 1);

		error = SMAPE(f,t);

		UpdateDisAggModelErrorArray(dam, error);







		clock_gettime(CLOCK_MONOTONIC, &start);



		if (sdf==2||sdf==4) {
			v = getTEVar(dam->model->measure);
			dam->disAggKeyNumerator += t;
		}




		//MEASURING INSERT-PERFORMANCE
		clock_gettime(CLOCK_MONOTONIC, &end);

		 timeElapsed = (((&end)->tv_sec * 1000000000) + (&end)->tv_nsec) - (((&start)->tv_sec * 1000000000) + (&start)->tv_nsec);
		if(printDbg)
			printf("time for key maintenence: %llu µs\n", timeElapsed);

	}

	//TODO: maybe later each mgin will need an own Staging table, this would help to update aggnodes properly
	mgin->lastInsertedTuple = (Datum *) palloc(tDesc->natts * sizeof(Datum));

	for(i=0 ; i<tDesc->natts; ++i){
		mgin->lastInsertedTuple[i] = datumCopy(values[i],tDesc->attrs[i]->attbyval, tDesc->attrs[i]->attlen);
	}

	releaseModelGraphRoot();
}

void MaintainModelGraphAggs(void){

	List				*nodeList = GetAllChildren(NULL, true),
						*disAggList = NIL,
						*evalConf;
	ListCell			*lc1,
						*lc2,
						*lc3;
	ModelGraphIndexNode	*mgin = getModelGraphRoot();
	DisAggModel			*dam;
	double				t = 0.0,
						f,
						error=0.0,
						confErr;
	ModelInfo			*model;
	Var					*v;
	ModelGraphIndexNode *flag;
	
	
	//if no Modelgraph was created -> there is nothing to update
	if(!mgin->children)
		return;

	foreach(lc1,nodeList){
		mgin = lfirst(lc1);

		if(mgin->aggIndicator < 1)
			continue;

		disAggList = GetAllDisaggregaties(mgin);

		//TODO: later we have to respect different measurcolumns of different models here-> _getModel maybe can help to achieve this, but it can be hard to manage this for normal Models AND DisAggModels
		t=0;
		if (mgin->models)
		{
			model = (ModelInfo *)linitial(mgin->models);
			v = getTEVar(model->measure);

			foreach(lc3, disAggList){




				//MEASURE-CHANGE
				ModelGraphIndexNode *disAggTargetNode=(ModelGraphIndexNode *)lfirst(lc3);
				double disAggKeyNumerator;
				if(disAggTargetNode->models){
					disAggKeyNumerator = ((ModelInfo *)linitial(disAggTargetNode->models))->disAggKeyDenominator;
				}
				else if(disAggTargetNode->disAggModels){
					disAggKeyNumerator = ((DisAggModel *)linitial(disAggTargetNode->disAggModels))->disAggKeyNumerator;
				}

				if(disAggKeyNumerator){
					f = getNextValue(model, 1)*disAggKeyNumerator/model->disAggKeyDenominator;
					model->otherErrors[disAggTargetNode->id][model->sizeOfErrorArray]=SMAPE(f, disAggTargetNode->lastInsertedTuple[v->varoattno-1]);
				}




				if(((ModelGraphIndexNode *)lfirst(lc3))->aggIndicator<1)
					t += GetDatumAsDouble(v->vartype, ((ModelGraphIndexNode *)lfirst(lc3))->lastInsertedTuple[v->varoattno-1]);
			}
		}
		else if (mgin->disAggModels)
		{
			model = ((DisAggModel *)linitial(mgin->disAggModels))->model;
			v = getTEVar(model->measure);

			foreach(lc3, disAggList){
				if(((ModelGraphIndexNode *)lfirst(lc3))->aggIndicator<1)
					t += GetDatumAsDouble(v->vartype, ((ModelGraphIndexNode *)lfirst(lc3))->lastInsertedTuple[v->varoattno-1]);
			}
		}

		if(t>0){
			if(!(mgin->lastInsertedTuple))
				mgin->lastInsertedTuple = palloc0(sizeof(double)*(v->varoattno-1));

			mgin->lastInsertedTuple[v->varoattno-1] = GetDoubleAsDatum(v->vartype,t);
		}


//TODO:support several Models in every Node
		if (mgin->models) {
			model = (ModelInfo *)linitial(mgin->models);
			v = getTEVar(model->measure);

			f = getNextValue(model, 1);
			error = SMAPE(f,t);

			UpdateModelInfoErrorArray(model, error);

			incrementalUpdate(model, t, ((ModelInfo *)linitial(mgin->models))->timestamp+1);

			model->insertCount = model->insertCount+1;
		}

		if (mgin->disAggModels) {
			dam = linitial(mgin->disAggModels);

			v = getTEVar(dam->model->measure);
			f = getNextDisAggValue(dam, 1);

			error = SMAPE(f,t);

			UpdateDisAggModelErrorArray(dam, error);

			if (sdf==2||sdf==4) {//update the own disaggKeyNumerator
				v = getTEVar(dam->model->measure);
				dam->disAggKeyNumerator += t;
			}
		}


	}

//	StringInfo data = makeStringInfo();

	confErr = 0.0;
	evalConf = GetAllChildren(NULL, true);

	foreach(lc1, evalConf){
		flag = lfirst(lc1);
		if(flag->models){
			if(flag->disAggModels){
				confErr += Min(((ModelInfo *)linitial(flag->models))->errorSMAPE, ((DisAggModel *)linitial(flag->disAggModels))->errorSMAPE);
//				elog(INFO, "1+2: %f", Min(((ModelInfo *)linitial(flag->models))->errorSMAPE, ((DisAggModel *)linitial(flag->disAggModels))->errorSMAPE));

//				appendStringInfo(data, "%f\t", Min(((ModelInfo *)linitial(flag->models))->errorSMAPE, ((DisAggModel *)linitial(flag->disAggModels))->errorSMAPE));
//				printDebug("/home/hartmann/Desktop/BelegSVN/ClaudioHartmann/data/Potentialanalyse/amokFehler.csv", data->data);
			}
			else{
				confErr += ((ModelInfo *)linitial(flag->models))->errorSMAPE;
//				elog(INFO, "1: %f", ((ModelInfo *)linitial(flag->models))->errorSMAPE);

//				resetStringInfo(data);
//				appendStringInfo(data, "%f\t", ((ModelInfo *)linitial(flag->models))->errorSMAPE);
//				printDebug("/home/hartmann/Desktop/BelegSVN/ClaudioHartmann/data/Potentialanalyse/amokFehler.csv", data->data);
			}
		}else{
			confErr += ((DisAggModel *)linitial(flag->disAggModels))->errorSMAPE;
//			elog(INFO, "2: %f", ((DisAggModel *)linitial(flag->disAggModels))->errorSMAPE);

//			resetStringInfo(data);
//			appendStringInfo(data, "%f\t", ((DisAggModel *)linitial(flag->disAggModels))->errorSMAPE);
//			printDebug("/home/hartmann/Desktop/BelegSVN/ClaudioHartmann/data/Potentialanalyse/amokFehler.csv", data->data);
		}
	}
//	elog(INFO, "SUM: %f", confErr);
	setConfError(confErr);

//	if(confErr > (confError*1.15))
//	{
//
//		elog(INFO, "Now we should have restartet the Greedy-Algorithm!");
//	}
//	else
//	{
		if (sdf==1||sdf==4) {
			foreach(lc1, modelsToReestimate)
			{
				model = (ModelInfo *)lfirst(lc1);
				ReestimateModelGraphModel(model);
				disAggList = GetAllDisaggregaties((ModelGraphIndexNode *)model->mix);
				foreach(lc2, disAggList){
					foreach(lc3, ((ModelGraphIndexNode *)lfirst(lc2))->disAggModels){
						dam = (DisAggModel *)lfirst(lc3);
						if(dam->model == model){

							if(dam->upperBound < model->upperBound){
								dam->errorArray = repalloc(dam->errorArray, model->upperBound*sizeof(double));
								dam->errorArray[model->upperBound-1] = 0.0;
							}
							dam->lowerBound = model->lowerBound;
							dam->upperBound = model->upperBound;
//							dam->sizeOfErrorArray = 0;
						}
					}
				}
				//TODO: respect other stuff like resetting errorArrays etc.
			}
		}
//	}


//		printDebug("/home/hartmann/Desktop/BelegSVN/ClaudioHartmann/data/Potentialanalyse/modelParameter.csv", "\n");
//
//		printDebug("/home/hartmann/Desktop/BelegSVN/ClaudioHartmann/data/Potentialanalyse/amokFehler.csv", "\n");

	modelsToReestimate = NIL;
//TODO: free all the last inserted Tuples of all Nodes

//	resetStringInfo(data);
//	appendStringInfo(data, "%f\n", confErr);
//	printDebug("/home/hartmann/Desktop/BelegSVN/ClaudioHartmann/data/Potentialanalyse/potentialAnalyse.txt",data->data);

	releaseModelGraphRoot();
}

void ReestimateModelGraphModel(ModelInfo *model){

	ModelGraphIndexNode	*iter;
	List				*exprList = NIL;
	A_Expr				*apex = NULL;
	StringInfo 			s,data;

	//iterate from target to root and collect the corresponding predicates for the NEW whereClause
	iter = (ModelGraphIndexNode *)model->mix;
	while(iter->parents){

		//if iter is an AggNode, don't add a predicate to the NEW whereClause
		if(iter->type != T_AggNode)
			exprList = lappend(exprList, CreateA_Expr(iter->target, iter->value));

		if(((ModelGraphIndexNode *)arlfirst(arlist_head(iter->parents)))->type == T_IndexNode){
			iter = (ModelGraphIndexNode *)arlfirst(arlist_head(iter->parents));
		}else{
			iter = (ModelGraphIndexNode *)arlfirst(arlist_tail(iter->parents));
		}
	}

	//restructure the NEW whereClause to be left-recursive
	if(exprList){
		if(list_length(exprList)>=2){
			apex = RestructureA_Expr(exprList);
		}else{
			apex = lfirst(list_head(exprList));
		}
	}

	reestimateParameters(model, (Node *)apex);

		//elog(WARNING, "Reestimated%i @ %i, %lf, %lf, %lf, %lf", ((ModelGraphIndexNode *)model->mix)->id, model->timestamp, model->errorSMAPE, ((HWModel *)model->model)->super.alpha, ((HWModel *)model->model)->super.beta, ((HWModel *)model->model)->super.gamma);

		s = makeStringInfo();
		appendStringInfo(s, "/home/hartmann/Desktop/BelegSVN/ClaudioHartmann/data/Potentialanalyse/ZZmodelParameter/modelParameter%i.csv", ((ModelGraphIndexNode *)model->mix)->id);

		data = makeStringInfo();
	//	appendStringInfo(data, "%lf\t %lf\t %lf\t %lf\n", ((HWModel *)model->model)->super.alpha, ((HWModel *)model->model)->super.beta, ((HWModel *)model->model)->super.gamma, *((HWModel *)model->model)->super.level);
		printDebug(s->data,data->data);
}

char* _getModelGraphNameForActiveGraph(void)
{
	HeapTuple 	tuple;
	Relation 	r=try_heap_openrv(makeRangeVar(NULL,"pg_modelgraph",-1),AccessShareLock);
	HeapScanDesc result;
	ScanKeyData scanoid[1];
	Datum		*values;
	bool		*isNull;
	char		*name;

	if(!r)
	{
		elog(ERROR,"no backuptable found: pg_modelgraph.Codekey:_getModelGraphNameForActiveGraph");
	}
	if(!fullGraphCreationSourceText)
		{
			heap_close(r, AccessShareLock);
			return NULL;
		}

	ScanKeyInit(&scanoid[0],2,InvalidStrategy,F_TEXTEQ,CStringGetTextDatum(fullGraphCreationSourceText));
	result= heap_beginscan(r,SnapshotNow,1,scanoid);
	if((tuple = heap_getnext(result, ForwardScanDirection)) == NULL)
	{
		heap_endscan(result);
		heap_close(r, AccessShareLock);
		return NULL;
	}
	values = (Datum *) palloc(RelationGetDescr(r)->natts * sizeof(Datum));
	isNull = (bool *) palloc0(RelationGetDescr(r)->natts * sizeof(bool));
	heap_deform_tuple(tuple, RelationGetDescr(r), values, isNull);
	name = (char *)DirectFunctionCall1(nameout,values[0]);
	pfree(values);
	pfree(isNull);
	heap_endscan(result);
	heap_close(r, AccessShareLock);
	return name;


}

void deleteModelGraph(void)
{
	if(!modelGraphContext) return;
	(*modelGraphContext->methods->reset)(modelGraphContext);
	modelGraphRoot=NULL;
	modelGraphIdx=NULL;
	leafCount=0;
}

void _restoreModelGraphStructure(char* name)
{
	Relation r=try_heap_openrv(makeRangeVar(NULL,"pg_modelgraph",-1),AccessShareLock);
	HeapTuple		tuple;
	HeapScanDesc result;
	ScanKeyData scanoid[1];
	Datum		*values;
	bool		*isNull;
	if(!r)
	{
		elog(ERROR,"no backuptable found: pg_modelgraph");
	}
	ScanKeyInit(&scanoid[0],1,InvalidStrategy,F_NAMEEQ, CStringGetDatum(name));
	result= heap_beginscan(r,SnapshotNow,1,scanoid);
	if((tuple=(heap_getnext(result, ForwardScanDirection))) != NULL)
	{
		//Delete ModelGraph is needed
		deleteModelGraph();
		values = (Datum *) palloc(RelationGetDescr(r)->natts * sizeof(Datum));
		isNull = (bool *) palloc0(RelationGetDescr(r)->natts * sizeof(bool));
		heap_deform_tuple(tuple, RelationGetDescr(r), values, isNull);


		runQuery(TextDatumGetCString(values[1]),"CREATE MODELGRAPH");


		pfree(values);
		pfree(isNull);
		heap_endscan(result);
		heap_close(r, AccessShareLock);

	}
	else
	{
		heap_endscan(result);
		heap_close(r, AccessShareLock);
		elog(ERROR,"Modelgraph not found");
	}

}

Oid _getFillingId(char* mginid, char*fillingname)
{
	Oid resultOid;
	Relation r=try_heap_openrv(makeRangeVar(NULL,"pg_mgfilling",-1),AccessShareLock);
	HeapTuple		tuple;
	HeapScanDesc result;
	ScanKeyData scanoid[2];
	Datum		*values;
	bool		*isNull;
	if(!r)
	{
		elog(ERROR,"no backuptable found: pg_mgfilling");
	}
	ScanKeyInit(&scanoid[0],2,InvalidStrategy,F_NAMEEQ,DirectFunctionCall1(namein, CStringGetDatum(mginid)));
	ScanKeyInit(&scanoid[1],3,InvalidStrategy,F_NAMEEQ,DirectFunctionCall1(namein, CStringGetDatum(fillingname)));
	result= heap_beginscan(r,SnapshotNow,2,scanoid);
	if((tuple=(heap_getnext(result, ForwardScanDirection))) == NULL)
	{
		elog(ERROR,"Modelgraph-Filling Combination does not exist");
	}
	values = (Datum *) palloc(RelationGetDescr(r)->natts * sizeof(Datum));
	isNull = (bool *) palloc0(RelationGetDescr(r)->natts * sizeof(bool));
	heap_deform_tuple(tuple, RelationGetDescr(r), values, isNull);
	resultOid=DatumGetObjectId(values[0]);

	heap_endscan(result);
		heap_close(r, AccessShareLock);


	return resultOid;
}



void _insetModelIntoModelgraph(ModelGraphIndexNode *mgin,Datum* values,Relation p)
{
	ModelInfo *newNode=initModelInfo((const char*)DatumGetCString(DirectFunctionCall1(nameout, values[7])),(const char*)DatumGetCString(DirectFunctionCall1(nameout, values[3])));
	newNode->modelName= DatumGetCString(DirectFunctionCall1(nameout, values[3]));
	newNode->timestamp=DatumGetInt32(values[10]);
	newNode->trainingData=TextDatumGetCString(values[13]);
	newNode->parameterList=stringToNode(TextDatumGetCString(values[14]));
	newNode->aggType=DatumGetObjectId(values[9]);
	newNode->granularity=DatumGetInt16(values[8]);
	newNode->forecastMethod=getStringAsModelType((DatumGetCString(DirectFunctionCall1(nameout, values[7]))));

	newNode->measure=stringToNode(TextDatumGetCString(values[4]));
	newNode->time=stringToNode(TextDatumGetCString(values[5]));

	initForecastModel(newNode,modelGraphContext);
	switch(newNode->forecastMethod)
	{
		case HwModel:
			restoreHwModelParameterForModelGraph(&((HWModel*)newNode->model)->super,p,DatumGetObjectId(values[2]));
			break;
		case ArModel:
			restoreArModelParameterForModelGraph(&((ARModel*)newNode->model)->super,p,DatumGetObjectId(values[2]));
			break;
		default:
			elog(ERROR,"forecastmethod not supported. Codekey:imim");
	}

	mgin->models=lappend(mgin->models,newNode);


}

void _restoreModels(Oid fillid,ModelGraphIndexNode** mginIndex)
{
	HeapTuple tuple;
	Relation p;
	ScanKeyData scanoid[1];
	HeapScanDesc result;
	Relation r=try_heap_openrv(makeRangeVar(NULL,"pg_mgmodel",-1),AccessShareLock);
	if(!r)
	{
		elog(ERROR,"no backuptable found: pg_mgmodel.Codekey:_restoreModels1");
	}
	p=try_heap_openrv(makeRangeVar(NULL,"pg_mgparameter",-1),AccessShareLock);
	if(!p)
	{
		elog(ERROR,"no backuptable found: pg_mgparameter.Codekey:_restoreModels2");
	}

	ScanKeyInit(&scanoid[0],1,InvalidStrategy,F_OIDEQ,ObjectIdGetDatum(fillid));
	result= heap_beginscan(r,SnapshotNow,1,scanoid);

	while((tuple = heap_getnext(result, ForwardScanDirection)) != NULL)
	{
		Datum				*values = (Datum *) palloc(RelationGetDescr(r)->natts * sizeof(Datum));
		bool				*isNull = (bool *) palloc0(RelationGetDescr(r)->natts * sizeof(bool));
		heap_deform_tuple(tuple, RelationGetDescr(r), values, isNull);
		_insetModelIntoModelgraph(mginIndex[DatumGetInt16(values[1])],values,p);
		pfree(values);
		pfree(isNull);
	}


	heap_endscan(result);
		heap_close(r, AccessShareLock);
		heap_close(p, AccessShareLock);
		return;

}


void _insetDisaggIntoModelgraph(ModelGraphIndexNode* to,ModelGraphIndexNode* from,Datum* values)
{
	DisAggModel* dag=palloc0(sizeof(DisAggModel));


	dag->model=linitial(from->models);
	dag->disAggKeyNumerator=DatumGetFloat8(values[4]);
	dag->model->disAggKeyDenominator=DatumGetFloat8(values[5]);
	dag->upperBound=DatumGetInt32(values[6]);
	dag->lowerBound=DatumGetInt32(values[7]);
	to->disAggModels=lappend(to->disAggModels,dag);
}

void _restoreDisaggs(Oid fillid,ModelGraphIndexNode** mginIndex)
{
	HeapTuple tuple;
	ScanKeyData scanoid[1];
	HeapScanDesc result;
	Datum				*values;
	bool				*isNull;
	Relation r=try_heap_openrv(makeRangeVar(NULL,"pg_mgdisagg",-1),AccessShareLock);
	if(!r)
	{
		elog(ERROR,"no backuptable found: pg_mgdisagg.Codekey:_restoreModels1");
	}
	ScanKeyInit(&scanoid[0],3,InvalidStrategy,F_OIDEQ,ObjectIdGetDatum(fillid));
	result= heap_beginscan(r,SnapshotNow,1,scanoid);

	while((tuple = heap_getnext(result, ForwardScanDirection)) != NULL)
	{
		values = (Datum *) palloc(RelationGetDescr(r)->natts * sizeof(Datum));
		isNull = (bool *) palloc0(RelationGetDescr(r)->natts * sizeof(bool));
		heap_deform_tuple(tuple, RelationGetDescr(r), values, isNull);
		_insetDisaggIntoModelgraph(mginIndex[DatumGetInt16(values[3])],mginIndex[DatumGetInt16(values[1])],values);
		pfree(values);
		pfree(isNull);
	}


	heap_endscan(result);
	heap_close(r, AccessShareLock);
	return;

}

void _restoreFilling(Oid fillid)
{
	ModelGraphIndexNode *mgin=getModelGraphRoot();
	List * childList= GetAllChildren(mgin,true);
	ModelGraphIndexNode* mginIndex[childList->length];
	ListCell *lc1;
	int i=0;
	foreach(lc1,childList)
		mginIndex[i++]=lfirst(lc1);
	//restore filling

	_restoreModels(fillid,mginIndex);
	_restoreDisaggs(fillid,mginIndex);
	releaseModelGraphRoot();
}

void _restoreModelGraphFilling(char*mginid,char *fillingname)
{
	//get FillingId
	Oid fillId=_getFillingId(mginid,fillingname);

	if(fillId==0)
		elog(ERROR,"Unknown Error CODEKEY:rest1");

	_restoreFilling(fillId);


	//elog(ERROR,"not supported yet:%i",fillId);
	return;
}

void RestoreModelGraph(RestoreStmt* stmt)
{
		char *detectName=_getModelGraphNameForActiveGraph();
	if(detectName && stmt->name && strcmp(detectName,stmt->name)!=0)
		elog(ERROR,"Modelgraph already exists! Please execute a Drop modelgraph stmt first.");
	if(!stmt->name)
		{
			stmt->name=detectName;
		}
	if(!stmt->name)
		elog(ERROR,"No Modelgraph existent. Please use RESTORE MODELGRAPH or CREATE MODELGRAPH first.");

	if(!stmt->fillingname)
	{
		_restoreModelGraphStructure(stmt->name);
		return;
	}
	_restoreModelGraphStructure(stmt->name);
	_restoreModelGraphFilling(stmt->name,stmt->fillingname);
}



Oid _generateFilling(char *fillingName,char* name)
{
	HeapTuple tuple;
	HeapScanDesc result;
	ScanKeyData scanoid[2];
		Oid 	id;
	Datum 		pgValues[3];
	bool 		pgNulls[3];
	HeapTuple 		pgTuple;
	Relation r=try_heap_openrv(makeRangeVar(NULL,"pg_mgfilling",-1),RowExclusiveLock);
	if(!r)
	{
		elog(ERROR,"no backuptable found: pg_modelgraph.Codekey:_getModelGraphNameForActiveGraph");
	}
	
	ScanKeyInit(&scanoid[0],3,InvalidStrategy,F_NAMEEQ,DirectFunctionCall1(namein,CStringGetDatum(name)));
	ScanKeyInit(&scanoid[1],2,InvalidStrategy,F_NAMEEQ,DirectFunctionCall1(namein,CStringGetDatum(fillingName)));
	result= heap_beginscan(r,SnapshotNow,2,scanoid);
	if((tuple = heap_getnext(result, ForwardScanDirection)) != NULL)
	{
		heap_endscan(result);
		heap_close(r, AccessShareLock);
		elog(ERROR,"Combination of Modelgraph and Fillingname already exists");
	}
	heap_endscan(result);
	id= GetNewOid(r);
	memset(pgNulls, false, sizeof(pgNulls));
	pgValues[0] = ObjectIdGetDatum(id);
	pgValues[1] = DirectFunctionCall1(namein,CStringGetDatum(name));
	pgValues[2] = DirectFunctionCall1(namein,CStringGetDatum(fillingName));

	
	// create tuple
	pgTuple = heap_form_tuple(r->rd_att, pgValues, pgNulls);

	  PG_TRY();
  {
	// Insert tuple into pg_model

	simple_heap_insert(r, pgTuple);

	// Update indexes
	CatalogUpdateIndexes(r, pgTuple);

	// Release memory
	heap_freetuple(pgTuple);

	// Close relation
	heap_close(r, RowExclusiveLock);
  }
  PG_CATCH();
  {
	  	heap_freetuple(pgTuple);
	heap_close(r, RowExclusiveLock);
	FlushErrorState();
	elog(ERROR,"Failed to insert filling. Maybe a model/filling combination with this name already exists");

  }
    PG_END_TRY();

	return id;
}



Oid _insertForecastModel(ModelInfo *mio,Relation r,Oid fillid,int mginId)
{
	Datum 			pgValues[15];
	bool 			pgNulls[15];
	Oid 			moid;
	HeapTuple 		pgTuple;
	memset(pgNulls, false, sizeof(pgNulls));

	moid = GetNewOid(r);

	pgValues[0]=ObjectIdGetDatum(fillid);
	pgValues[1]=Int32GetDatum(mginId);
	pgValues[2]=Int32GetDatum(moid);
	pgValues[3]=DirectFunctionCall1(namein, CStringGetDatum(mio->modelName));
	pgValues[4]=CStringGetTextDatum(nodeToString(mio->measure));
	pgValues[5]=CStringGetTextDatum(nodeToString(mio->time));
	pgValues[6]=Int16GetDatum(mio->measure->resorigcol);
	pgValues[7]=DirectFunctionCall1(namein, CStringGetDatum(getModelTypeAsString(mio->forecastMethod)));
	pgValues[8]= Int16GetDatum(mio->granularity);
	pgValues[9]=ObjectIdGetDatum(mio->aggType);
	pgValues[10]=Int32GetDatum(mio->timestamp);
	pgValues[11]=Float8GetDatum(0.0);
	pgValues[12]=Float8GetDatum(0.0);
	pgValues[13]=CStringGetTextDatum(mio->trainingData);
	pgValues[14]=CStringGetTextDatum(nodeToString(mio->parameterList));
		
	// create tuple
	pgTuple = heap_form_tuple(r->rd_att, pgValues, pgNulls);

	  PG_TRY();
  {
	// Insert tuple into pg_model

	simple_heap_insert(r, pgTuple);

	// Update indexes
	CatalogUpdateIndexes(r, pgTuple);

	// Release memory
	heap_freetuple(pgTuple);


  }
  PG_CATCH();
  {
	  	heap_freetuple(pgTuple);
	heap_close(r, RowExclusiveLock);
	FlushErrorState();
	elog(ERROR,"Failed to insert filling. Maybe a model/filling combination with this name already exists");

  }
    PG_END_TRY();



	return moid;
}
void _storeModel(ModelInfo *mio,Relation parameterRelation,Relation modelRelation,int fillid,int mginId)
{

	Oid mid=_insertForecastModel(mio,modelRelation,fillid,mginId);

	switch(mio->forecastMethod)
	{
		case HwModel:
			backupHwModelParameterForModelGraph(&((HWModel*)mio->model)->super,parameterRelation,mid);
			break;
		case ArModel:
			backupArModelParameterForModelGraph(&((ARModel*)mio->model)->super,parameterRelation,mginId);
			break;
		default:
			elog(ERROR,"forecastmethod not supported. Codekey:imim");
	}
}

void _storeDisAgg(DisAggModel *dag,Relation r, Oid fillid,int mginid)
{
	Datum 		pgValues[8];
	bool 		pgNulls[8];
	HeapTuple 		pgTuple;
	memset(pgNulls, false, sizeof(pgNulls));
	pgValues[0] = Int16GetDatum(((ModelGraphIndexNode*)(dag->model->mix))->id);
	pgValues[1] = Int16GetDatum(((ModelGraphIndexNode*)(dag->model->mix))->id);
	pgValues[2] = ObjectIdGetDatum(fillid);
	pgValues[3] = Int16GetDatum(mginid);
	pgValues[4] = Float8GetDatum(dag->disAggKeyNumerator);
	pgValues[5] = Float8GetDatum(dag->model->disAggKeyDenominator);
	pgValues[6] = Int32GetDatum(dag->upperBound);
	pgValues[7] = Int32GetDatum(dag->lowerBound);
		
	// create tuple
	pgTuple = heap_form_tuple(r->rd_att, pgValues, pgNulls);

	  PG_TRY();
  {
	// Insert tuple into pg_model

	simple_heap_insert(r, pgTuple);

	// Update indexes
	CatalogUpdateIndexes(r, pgTuple);

	// Release memory
	heap_freetuple(pgTuple);

  }
  PG_CATCH();
  {
	  	heap_freetuple(pgTuple);
	FlushErrorState();
	elog(ERROR,"Failed to insert filling. Maybe a model/filling combination with this name already exists");

  }
    PG_END_TRY();

}
void _storeModelsandDisaggs(Oid id)
{
	Relation modelRelation;
	Relation parameterRelation;
	Relation disaggRelation;
	ModelGraphIndexNode *mgin = getModelGraphRoot();
	List *leafs=GetAllChildren(mgin,true);
	ListCell *lc1,*lcdisagg,*lcmodel;


	modelRelation=try_heap_openrv(makeRangeVar(NULL,"pg_mgmodel",-1),RowExclusiveLock);
	if(!modelRelation)
	{
		elog(ERROR,"no backuptable found: pg_mgmodel.Codekey:_storeModelsandDisaggs");
	}
	parameterRelation=try_heap_openrv(makeRangeVar(NULL,"pg_mgparameter",-1),RowExclusiveLock);
	if(!parameterRelation)
	{
		elog(ERROR,"no backuptable found: parameterRelation.Codekey:_storeModelsandDisaggs");
	}
	disaggRelation=try_heap_openrv(makeRangeVar(NULL,"pg_mgdisagg",-1),RowExclusiveLock);
	if(!disaggRelation)
	{
		elog(ERROR,"no backuptable found: disaggRelation.Codekey:_storeModelsandDisaggs");
	}

	foreach(lc1,leafs)
	{
		ModelGraphIndexNode *actMgin=lfirst(lc1);
		foreach(lcdisagg,actMgin->disAggModels)
		{
			DisAggModel *dag=lfirst(lcdisagg);
			_storeDisAgg(dag,disaggRelation,id,actMgin->id);
		}
		foreach(lcmodel,actMgin->models)
		{
			ModelInfo *mio=lfirst(lcmodel);
			_storeModel(mio,parameterRelation,modelRelation,id,actMgin->id);
		}
	}
	heap_close(modelRelation, RowExclusiveLock);
	heap_close(parameterRelation, RowExclusiveLock);
	heap_close(disaggRelation, RowExclusiveLock);
	releaseModelGraphRoot();
}
void _storeModelGraphFilling(char* fillingname,char* name)
{
	Oid id;
	//Get Name if not given
	if(!name)
		name=_getModelGraphNameForActiveGraph();
	if(!name)
		elog(ERROR,"No modelgraph matching the actual graph was found. To store the actual filling please use STORE MODELGRAPH Statement");

	//Get ID for filling
	id=_generateFilling(fillingname,name);

	//Store Filling
	_storeModelsandDisaggs(id);
}
void _storeModelGraphStructure(char* name)
{
	ModelGraphIndexNode *mgin = getModelGraphRoot();
	Relation r=try_heap_openrv(makeRangeVar(NULL,"pg_modelgraph",-1),RowExclusiveLock);
	Datum 		pgValues[2];
	bool 		pgNulls[2];
	HeapTuple 		pgTuple;
	if(!r)
	{
		elog(ERROR,"no backuptable found: pg_modelgraph");
	}

	if(!mgin->children)
		elog(ERROR,"No Modelgraph exists");

	memset(pgNulls, false, sizeof(pgNulls));
	pgValues[0] = DirectFunctionCall1(namein, CStringGetDatum(name));
	pgValues[1] = CStringGetTextDatum(fullGraphCreationSourceText);


	// create tuple
	pgTuple = heap_form_tuple(r->rd_att, pgValues, pgNulls);


	  PG_TRY();
  {
	// Insert tuple into pg_model

	simple_heap_insert(r, pgTuple);

	// Update indexes
	CatalogUpdateIndexes(r, pgTuple);

	// Release memory
	heap_freetuple(pgTuple);

	// Close relation
	heap_close(r, RowExclusiveLock);
  }
  PG_CATCH();
  {
	  	heap_freetuple(pgTuple);
	heap_close(r, RowExclusiveLock);
	FlushErrorState();
	elog(ERROR,"Failed to insert model. Maybe a model with this name already exists");

  }
    PG_END_TRY();
	releaseModelGraphRoot();
}

void StoreModelGraph(StoreStmt *stmt)
{
	ModelGraphIndexNode *mgin = getModelGraphRoot();
	if(!mgin->children)
		elog(ERROR,"No Modelgraph to store exists");
	releaseModelGraphRoot();
	if(!stmt->fillingname)
		{
			_storeModelGraphStructure(stmt->name);
			return;
		}
	if(!stmt->name)
	{
		_storeModelGraphFilling(stmt->fillingname,NULL);
		return;
	}
	_storeModelGraphStructure(stmt->name);
	_storeModelGraphFilling(stmt->fillingname,stmt->name);

}



void ReestimateAllModelGraphModels(void){

	ListCell			*lc1;
	List 				*allNodes = GetAllChildren(NULL, true);

	ModelGraphIndexNode *flag;
	double confErr = 0.0;
	foreach(lc1, allNodes){
		flag = lfirst(lc1);
		if(flag->models){
			if(flag->disAggModels){
				confErr += Min(((ModelInfo *)linitial(flag->models))->errorSMAPE, ((DisAggModel *)linitial(flag->disAggModels))->errorSMAPE);
				elog(INFO, "1+2: %f", confErr);
			}
			else{
				confErr += ((ModelInfo *)linitial(flag->models))->errorSMAPE;
				elog(INFO, "1: %f", confErr);
			}
		}else{
			confErr += ((DisAggModel *)linitial(flag->disAggModels))->errorSMAPE;
			elog(INFO, "2: %f", confErr);
		}
	}
}

char *getWhereExprAsString(ModelGraphIndexNode *mgin){
	StringInfo res = makeStringInfo();
	Oid typeOid = getTEVar(mgin->target)->vartype;

	appendStringInfo(res , "WHERE ");


loop:
if(mgin->type != T_AggNode){
	switch(typeOid){
	case 20:
	case 21:
	case 23:
		appendStringInfo(res , "%s = %i", mgin->target->resname, GetDatumAsInt(typeOid, mgin->value));
		break;
	case 1042:
		appendStringInfo(res , "%s = '%s'", mgin->target->resname, DatumGetCString(DirectFunctionCall1(bpcharout, mgin->value)));
		break;
	case 1043:
		appendStringInfo(res , "%s = '%s'", mgin->target->resname, DatumGetCString(DirectFunctionCall1(varcharout, mgin->value)));
		break;
	}
}

	if(mgin->parents){
		mgin = arlinitial(mgin->parents);

		while(mgin->type == T_AggNode && mgin->parents)
			mgin = arlinitial(mgin->parents);

		if(mgin->type==T_RootNode)
			goto end;

		if(strcmp(res->data, "WHERE ")!=0)
			appendStringInfo(res , " AND ");
		goto loop;
	}

end:
	return res->data;
}

#define getOpVar(boolep) lfirst(list_head(((BoolExpr*)(boolep))->args))

void stagingAddTupel(int mginId)
{
	int i;
	if(!modelGraphIdx->stagingmaxSize)
	{
		modelGraphIdx->staging=palloc0(sizeof(int)*10);
		modelGraphIdx->stagingmaxSize=10;

		for(i=0;i<10;i++)
		{
			modelGraphIdx->staging[i]=2^(modelGraphIdx->maxid);
		}
	}
	i=0;
	while(true)
	{
		if(modelGraphIdx->staging[i] & (2^mginId)) //gt right staging
		{
			modelGraphIdx->staging[i]-=2^mginId;
			return;
		}
		i++;
		if(i>=modelGraphIdx->stagingmaxSize)
		{
			modelGraphIdx->staging=repalloc(modelGraphIdx->staging,sizeof(int)*(modelGraphIdx->stagingmaxSize+10));
			modelGraphIdx->stagingmaxSize+=10;
			for(i=modelGraphIdx->stagingmaxSize-10;i<modelGraphIdx->stagingmaxSize;i++)
			{
				modelGraphIdx->staging[i]=2^(modelGraphIdx->maxid);
			}
		}
	}
}

bool staging_updatable(void)
{
	return (modelGraphIdx->staging[0]==0);
}

bool staging_update(void)
{
	memmove(modelGraphIdx->staging,&(modelGraphIdx->staging[1]),sizeof(int)*(modelGraphIdx->stagingmaxSize-1));
	modelGraphIdx->staging[modelGraphIdx->stagingmaxSize-1]=2^modelGraphIdx->maxid;
	return true;
}


//eliminate everything on this  level!
int _eliminateAndTransformAndClause(OpExpr *clauseToKill, BoolExpr *clause,List *rtable,int*foundClause) {
	ListCell *alc,*prev=NULL;
	int result=0;
	Var *v, *v2;

	switch(nodeTag(lfirst(list_head(((OpExpr*)clauseToKill)->args)))){
	case T_Var:
		v2 = (Var*)(lfirst(list_head(((OpExpr*)clauseToKill)->args)));
		break;
	case T_RelabelType:
		v2 = (Var *)((RelabelType *)lfirst(list_head(((OpExpr*)clauseToKill)->args)))->arg;
		break;
	default:
		elog(ERROR, "Unhandeled type in _LookUpAndClause.");
		break;
	}

	if((void *)clauseToKill == (void *)clause)
		return 1;
	foreach(alc,clause->args) {
		switch(nodeTag(lfirst(alc))) {
		case T_OpExpr: {

			switch(nodeTag(lfirst(list_head(((OpExpr*)(lfirst(alc)))->args)))){
			case T_Var:
				v = (Var*)(lfirst(list_head(((OpExpr*)(lfirst(alc)))->args)));
				break;
			case T_RelabelType:
				v = (Var *)((RelabelType *)lfirst(list_head(((OpExpr*)(lfirst(alc)))->args)))->arg;
				break;
			default:
				elog(ERROR, "Unhandeled type in _LookUpAndClause.");
				break;
			}

			if(((RangeTblEntry*)list_nth(rtable,v->varno-1))->relid== ((RangeTblEntry*)list_nth(rtable,v2->varno-1))->relid  &&
			        v->varattno==v2->varattno) {
				if(compareDatum(((Const*)(lfirst(list_tail(((OpExpr*)(lfirst(alc)))->args))))->constvalue,((Const*)(lfirst(list_tail(((OpExpr*)(clauseToKill))->args))))->constvalue,((Var*)(lfirst(list_head(((OpExpr*)(lfirst(alc)))->args))))->vartype)) {
					*foundClause=1;
					if(clause->args->length==1)
						return 1;
					else {
						if(!prev) {
							list_delete_first_wf(clause->args);
							result=1;
						} else {
							list_delete_cell_wf(clause->args,alc,prev);
							result=1;
						}
					}
				}
			}
			break;
		}
		default: {
			Node* temp=getOpVar(lfirst(alc));

			switch(nodeTag(lfirst(list_head(((OpExpr*)temp)->args)))){
			case T_Var:
				v = (Var*)(lfirst(list_head(((OpExpr*)temp)->args)));
				break;
			case T_RelabelType:
				v = (Var *)((RelabelType *)lfirst(list_head(((OpExpr*)temp)->args)))->arg;
				break;
			default:
				elog(ERROR, "Unhandeled type in _LookUpAndClause.");
				break;
			}

			if(((RangeTblEntry*)list_nth(rtable,v->varno-1))->relid== ((RangeTblEntry*)list_nth(rtable,v2->varno-1))->relid  &&
			        v->varattno==v2->varattno) {
				if(compareDatum(((Const*)(lfirst(list_tail(((OpExpr*)(temp))->args))))->constvalue,((Const*)(lfirst(list_tail(((OpExpr*)(clauseToKill))->args))))->constvalue,v->vartype)) {
					*foundClause=1;
					if(clause->args->length==1)
						return 1;
					if(!prev) {
						list_delete_first_wf(clause->args);
						result=1;
					} else {
						list_delete_cell_wf(clause->args,alc,prev);
						result=1;
					}
				}
			}
			break;
		}//Maybe the level is matching, shoulld be killed too
		}
		if(result) break;
		prev=alc;
	}
	return 0;
}

//Kill everything that matches this level
Node* _eliminateAndTransformWhere(OpExpr *clauseToKill, BoolExpr *whereRoot, List *rtable) {
	int kill=0;
	ListCell *lc1;
	Node *temp;
	int *foundClause=palloc0(sizeof(int));
	*foundClause=0;
	foreach(lc1,(whereRoot)->args) {
		*foundClause=0;
		temp=lfirst(lc1);
		kill=0;
		kill=_eliminateAndTransformAndClause(clauseToKill,(BoolExpr*)temp,rtable,foundClause);
		if(*foundClause && kill) {
			pfree(foundClause);
			return NULL;
		}
		if(*foundClause) {
			List *newArgs=NIL;
			newArgs=lappend(newArgs,temp);
			whereRoot->args=newArgs;
			pfree(foundClause);
			return (Node*)whereRoot;
		}

	}
	pfree(foundClause);
	return (Node*)whereRoot;
}



//Called for every AND Clause within the Root OR CLause
static int _LookUpAndClause(ModelGraphIndexNode *mgin, List **possiblePaths,int *added,BoolExpr *whereExpr, BoolExpr *whereRoot,List *rtable,List** addedChilds) {
	ListCell *lc1;
	int matchIndex;
	Node *newBool,*whereresult;
	SearchPathNode *newNode;
	Var *v;
	foreach(lc1,(whereExpr)->args) {
		OpExpr *opex;
		if((nodeTag(lfirst(lc1))==T_OpExpr))
			opex=lfirst(lc1);
		else
			opex=lfirst(list_head(((BoolExpr*)(lfirst(lc1)))->args));


		//else check if we match this treelevel
		switch(nodeTag(lfirst(list_head(opex->args)))){
		case T_Var:
			v = (Var*)(lfirst(list_head(opex->args)));
			break;
		case T_RelabelType:
			v = (Var *)((RelabelType *)lfirst(list_head(opex->args)))->arg;
			break;
		default:
			elog(ERROR, "Unhandeled type in _LookUpAndClause.");
			break;
		}
		if(((ModelGraphIndexNode *)arlfirst(arlist_head(mgin->children)))->target->resorigtbl== ((RangeTblEntry*)list_nth(rtable,v->varno-1))->relid  &&
		        ((Var*)((ModelGraphIndexNode *)arlfirst(arlist_head(mgin->children)))->target->expr)->varattno==v->varattno) {
			switch(((Var *)((ModelGraphIndexNode *)arlfirst(arlist_head(mgin->children)))->target->expr)->vartype) {
			case 20:
			case 21:
			case 23: {
				matchIndex= find_int(mgin->children, ((Const*)(lfirst(list_tail(opex->args))))->constvalue, Mgin_Int_SorterAsc);
				if(matchIndex<0) {
					(*added)++;
					return 1;
				}
				break;
			}
			case 700:
			case 701: {
				elog(ERROR,"Float not supported yet");
			}
			case 1042:
			case 1043:{
				matchIndex=find(mgin->children, (void*)((Const*)(lfirst(list_tail(opex->args))))->constvalue, Mgin_Char_SorterAsc);
				if(matchIndex<0) {
					(*added)++;
					return 1;
				}
				break;
			}
			default:
				elog(ERROR,"Unknown treeleveltype. Codekey:LookMatchCheck1");
			}
			//switch optype, at this moment we only support equal operations
			switch(opex->opno) {
			case 670:
			case 620:
			case 96:
			case 1054:
			case 410:
			case 94:
			case 98:{
				//equal;
				newNode =palloc0(sizeof(SearchPathNode));
				newNode->mgin=arlfirst(mgin->children->container[matchIndex]);
				*addedChilds=lappend(*addedChilds,mgin->children->container[matchIndex]);
				(*added)++;

				newBool=copyObject(whereRoot);
				whereresult=_eliminateAndTransformWhere(opex,(BoolExpr*)newBool,rtable); //delete everything on this level
				//elog(INFO,"Added: %s\n\n",nodeToString(whereresult));

				if(!whereresult)
					newNode->whreExpr=NULL;
				else
					newNode->whreExpr=whereresult;
				*possiblePaths=lappend(*possiblePaths,newNode);

				return 1;
			}
			default:
				elog(ERROR,"Unknow optype: %i, This should have been forwarded to the ModelIndex, CodeKey:LookMatchCheck2",opex->opno);
			}
		}



	}
	return 0;
}



void LookUpAndAddPath(ModelGraphIndexNode *mgin, BoolExpr *whereExpr, BoolExpr *whereRoot, List **possiblePaths, int *added,List *rtable) {

	ListCell *lc1;
	int found;
	BoolExpr *clause;
	List *addedChilds=NIL; //If the _LookUp  adds any new Paths, it writes them into this list.
	List *notmatched=NIL; //every AND Clause that has no matches at the current level is remembered in this list

	if(!whereExpr) { //If the whereclause for the given Node is empty, all Conditions are fullfilled, therefore we can take the agg Node
		SearchPathNode *newPath=calloc(1,sizeof(SearchPathNode));
		newPath->mgin=mgin->aggChild;
		newPath->whreExpr=NULL;
		*possiblePaths=lappend(*possiblePaths,newPath);
		return;
	}

	if(nodeTag(whereExpr)!=T_BoolExpr) //In everystep we generate an OR(AND....) or NULL, therefore no none BoolExpr can exist at this level
		elog(ERROR,"There should be no damn OpExpr. CodeKey:LookUpAndAddPath1");

	*added=0;


	foreach(lc1,(whereExpr)->args) {
		//for every and within the or-clause. Remark: After Where Transformation, there are only Ands within the OR Clause. All Existing OpExprs were transformed to AND(OpExpr)
		found=0;
		clause=lfirst(lc1);
		found=_LookUpAndClause(mgin,possiblePaths,added,clause,whereRoot,rtable,&addedChilds);
		if(!found)
			notmatched=lappend(notmatched,lfirst(lc1));

	}
	if(!(*added)) { //got no match at this level
		SearchPathNode *newPath=palloc0(sizeof(SearchPathNode));
		newPath->mgin=mgin->aggChild;

		newPath->whreExpr=copyObject(whereRoot);
		*possiblePaths=lappend(*possiblePaths,newPath);
		return;
	} else if(notmatched!=NIL) { //some clauses have not matched despite we got an overall match
		ArListCell *arl;
		BoolExpr *newOr=makeNode(BoolExpr);
		newOr->boolop=OR_EXPR;
		newOr->args=NIL;
		foreach(lc1,notmatched) {
			newOr->args=lappend(newOr->args,copyObject((lfirst(lc1))));
		};
		arforeach(arl,mgin->children) {
			SearchPathNode *newPath=palloc0(sizeof(SearchPathNode));
			newPath->mgin=arlfirst(arl);
			newPath->whreExpr=copyObject(newOr);
			*possiblePaths=lappend(*possiblePaths,newPath);
		}

		pfree(newOr);
	}
}

List** _mergeAndClauses(BoolExpr *boolep,List** result) {
	ListCell *lc1;
	foreach(lc1,boolep->args) {
		switch(nodeTag(lfirst(lc1))) {
		case T_OpExpr: {
			(*result)=lappend(*result,lfirst(lc1));
			break;
		}
		case T_BoolExpr: {
			BoolExpr *bolep=(BoolExpr*)lfirst(lc1);
			if(bolep->boolop!=AND_EXPR) {
				elog(ERROR,"Unknown Nodetag in mergeAndClauses_2");
			}
			result=_mergeAndClauses(lfirst(lc1),result);
			break;

		}
		default:
			elog(ERROR,"Unknown Nodetag in mergeAndClauses");
		}
	}
	return result;

}

//merges all and Exprs found
Node *mergeAndClauses(Node* whereExpr) {
	switch(nodeTag(whereExpr)) {
	case T_OpExpr:
		break;
	case T_BoolExpr: {
		BoolExpr *bolep=(BoolExpr*)whereExpr;
		switch(bolep->boolop) {
		case OR_EXPR: {
			ListCell *lc1;
			foreach(lc1,bolep->args) {
				lfirst(lc1)=mergeAndClauses(lfirst(lc1));
			}
			break;
		}
		case AND_EXPR: {
			List *result=NIL;
			bolep->args=*(_mergeAndClauses(bolep,&result));
			break;
		}
		default:{
			elog(ERROR, "Expressiontype not supported! errormarkMAC");
			break;
		}
		}
		break;
	}
	default:
		elog(ERROR,"Unknown Nodetag in transformWhereExpr");
	}


	return whereExpr;

}

Node* transformOpExpr(Node* whereExpr) {
	switch(nodeTag(whereExpr)) {
	case T_OpExpr: {
		BoolExpr* newAnd=makeNode(BoolExpr);
		newAnd->boolop=AND_EXPR;
		newAnd->args=NIL;
		newAnd->args=lappend(newAnd->args,whereExpr);
		return (Node*)newAnd;
	}
	case T_BoolExpr: {
		ListCell *lc1;
		foreach(lc1,((BoolExpr*)whereExpr)->args) {
			lfirst(lc1)=transformOpExpr(lfirst(lc1));
		}
		return whereExpr;
	}
	default:
		elog(ERROR,"Unknown nodetag in transformOpExpr. Codekey:transformOpExpr1");
		return NULL; //ceep compielr quiet
	}
}

Node* sortBoolExprs(BoolExpr* whereExpr) {
	if(whereExpr->args->length==1)
		return (Node*)whereExpr;
	else {
		if(((BoolExpr*)lfirst(list_head((whereExpr->args))))->boolop==OR_EXPR && ((BoolExpr*)lfirst(list_tail((whereExpr->args))))->boolop==AND_EXPR) {
			List* result=NIL;
			ListCell *lc1;
			foreach(lc1, whereExpr->args) {
				result=lcons(lfirst(lc1),result);
			}
			foreach(lc1, result) {
				lfirst(lc1)=sortBoolExprs(lfirst(lc1));
			}
			whereExpr->args=result;

		}
	}
	return (Node*)whereExpr;
}

void _mergeOrClauses(BoolExpr* whereExpr,List** result) {
	BoolExpr *boolhead=lfirst(list_head((whereExpr->args)));
	BoolExpr *booltail=lfirst(list_tail((whereExpr->args)));
	if(boolhead->boolop==AND_EXPR)
		(*result)=lappend((*result),boolhead);
	if(booltail->boolop==AND_EXPR && boolhead!=booltail)
		(*result)=lappend((*result),booltail);
	if(boolhead->boolop==OR_EXPR)
		_mergeOrClauses(boolhead,result);
	if(booltail->boolop==OR_EXPR && boolhead!=booltail)
		_mergeOrClauses(booltail,result);
}

Node* mergeOrClauses(BoolExpr* whereExpr) {
	if(whereExpr->boolop==AND_EXPR)
		return (Node*) whereExpr;
	else if(whereExpr->boolop!=OR_EXPR)
		elog(ERROR,"There should be nothing else than And or Or Expresions in this step. Codekey:mergeOrClause1");
	else {
		List *result=NIL;
		_mergeOrClauses(whereExpr,&result);
		whereExpr->args=result;
	}
	return (Node*)whereExpr;
}

Node* transformWhereExpr(Node* whereEpxr) {
	BoolExpr *resultExpr;
	ListCell *lc1;
	if(!whereEpxr)
		return NULL;
//
//	elog(INFO,"Original: %s",nodeToString(whereEpxr));
	//merge and clauses
	whereEpxr=mergeAndClauses(whereEpxr);
//	elog(INFO,"\n");
//	elog(INFO,"MergedAnds: %s",nodeToString(whereEpxr));
	//transform Ops to And(op)
	whereEpxr=transformOpExpr(whereEpxr);
//	elog(INFO,"\n");
//	elog(INFO,"Transformed Ops: %s",nodeToString(whereEpxr));
	//sort and and Ors
	whereEpxr=sortBoolExprs((BoolExpr*)whereEpxr);
//	elog(INFO,"\n");
//	elog(INFO,"Sorted: %s",nodeToString(whereEpxr));
	//merge or clauses
	whereEpxr=mergeOrClauses((BoolExpr*)whereEpxr);
//	elog(INFO,"\n");
//	elog(INFO,"Merged Ors: %s",nodeToString(whereEpxr));
//	elog(INFO,"\n");

	if(((BoolExpr*)(whereEpxr))->boolop==AND_EXPR) {
		resultExpr=makeNode(BoolExpr);
		resultExpr->boolop=OR_EXPR;
		resultExpr->args=NIL;
		resultExpr->args=lappend(resultExpr->args,whereEpxr);
		
		foreach(lc1,((BoolExpr*)(resultExpr))->args) {
		//	elog(INFO,"%s\n\n",nodeToString((lfirst(lc1))));
		}
		return (Node*)resultExpr;
	}

	foreach(lc1,((BoolExpr*)(whereEpxr))->args) {
	//	elog(INFO,"%s\n\n",nodeToString((lfirst(lc1))));
	}
	return whereEpxr;
}

static ModelInfo* _getModel(ModelGraphIndexNode *mgin,ModelInfo *modelinfo) {
	List *result=NIL;
	ListCell *lc2,*lc3,*lc4;
	bool 			paraFound=true;
	bool timeMeasureRigth;
	Var	*v1,
		*v2;
	foreach(lc2,mgin->models) {
		ModelInfo *tempModel=lfirst(lc2);
		
		
		
		//compare this Model with the ModelInfo from stmt
		if(IsA(((TargetEntry *)modelinfo->measure)->expr, Var))
		{
			v1 = (Var *)((TargetEntry *)modelinfo->measure)->expr;
			v2 = (Var *)tempModel->measure->expr;

			timeMeasureRigth = (v1->varoattno == v2->varoattno)
								&& (tempModel->measure->resorigtbl ==  modelinfo->measure->resorigtbl)
								&& (tempModel->time->resorigcol == modelinfo->time->resorigcol)
								&& (tempModel->time->resorigtbl == modelinfo->time->resorigtbl);
			//provide the measure typOid of the measure column for later use
		}
		else//case for T_Aggref
		{
			v1 = lfirst(list_head(((Aggref *)((TargetEntry *)modelinfo->measure)->expr)->args));
			v2 = lfirst(list_head(((Aggref *)tempModel->measure->expr)->args));

			timeMeasureRigth = (v1->varoattno == v2->varoattno)
								&& (tempModel->measure->resorigtbl ==  modelinfo->measure->resorigtbl)
								&& (tempModel->time->resorigcol == modelinfo->time->resorigcol)
								&& (tempModel->time->resorigtbl == modelinfo->time->resorigtbl);

//			timeMeasureRigth = (tempModel->measure->resorigcol ==  modelinfo->measure->resorigcol)
//									&& (tempModel->measure->resorigtbl ==  modelinfo->measure->resorigtbl)
//									&& (tempModel->time->resorigcol == modelinfo->time->resorigcol)
//									&& (tempModel->time->resorigtbl == modelinfo->time->resorigtbl);
			//provide the measure typOid of the measure column for later use
		}
		
		if(!timeMeasureRigth)
			continue;
		
		
	
		if(modelinfo->forecastMethod== tempModel->forecastMethod) {
			foreach(lc3, modelinfo->parameterList) {
				paraFound=false;
				foreach(lc4,tempModel->parameterList) {
					AlgorithmParameter *findPar = (AlgorithmParameter*) lfirst(lc3);
					AlgorithmParameter *searchPar = (AlgorithmParameter*) lfirst(lc4);
					int paraEqual;
					switch (searchPar->value->val.type) {
						case T_Integer:
							if(findPar->value->val.type!=T_Integer)
								continue;
							if(strcmp(findPar->key,searchPar->key))
								continue;
							paraEqual = !(((searchPar->value->val.val.ival)==(int)(findPar->value->val.val.ival)));
							break;
						case T_String:
							if(findPar->value->val.type!=T_String)
								continue;
							if(strcmp(findPar->key,searchPar->key))
								continue;
							paraEqual = strcmp(searchPar->value->val.val.str, findPar->value->val.val.str);
							break;
						default:
							break;
					}
					//compare parameters
					if(paraEqual==0) {
						paraFound=true;
						break;
					}
				}
				if(!paraFound)
					break;
			}
			if(!paraFound)
				continue;
			else {
				result=lappend(result,tempModel);
				continue;
			}
		} else if(!modelinfo->forecastMethod) {
			result=lappend(result,tempModel);
		} else
			continue;
	}
	//TODO Select best model through to Errorvalue
	if(result!=NIL) {
		return lfirst(list_head((result)));
	} else
		return NULL;
}

static DisAggModel* _getDisAgg(ModelGraphIndexNode *mgin,ModelInfo *modelinfo) {
	List 		*result=NIL;
	ListCell	*lc2,
				*lc3,
				*lc4;
	bool 		timeMeasureRigth;
	Var 		*v1,
				*v2;
	bool 		paraFound=true;
	int			paraEqual=1;

	foreach(lc2,mgin->disAggModels) {
		DisAggModel *tempD=lfirst(lc2);
		ModelInfo *tempModel=tempD->model;

		//compare this Model with the ModelInfo from stmt
		if(IsA(((TargetEntry *)modelinfo->measure)->expr, Var))
		{
			v1 = (Var *)((TargetEntry *)modelinfo->measure)->expr;
			v2 = (Var *)tempModel->measure->expr;

			timeMeasureRigth = (v1->varoattno == v2->varoattno)
								&& (tempModel->measure->resorigtbl ==  modelinfo->measure->resorigtbl)
								&& (tempModel->time->resorigcol == modelinfo->time->resorigcol)
								&& (tempModel->time->resorigtbl == modelinfo->time->resorigtbl);
			//provide the measure typOid of the measure column for later use
		}
		else//case for T_Aggref
		{
			v1 = lfirst(list_head(((Aggref *)((TargetEntry *)modelinfo->measure)->expr)->args));
			v2 = lfirst(list_head(((Aggref *)tempModel->measure->expr)->args));

			timeMeasureRigth = (v1->varoattno == v2->varoattno)
								&& (tempModel->measure->resorigtbl ==  modelinfo->measure->resorigtbl)
								&& (tempModel->time->resorigcol == modelinfo->time->resorigcol)
								&& (tempModel->time->resorigtbl == modelinfo->time->resorigtbl);
		}

		if(!timeMeasureRigth)
			continue;



		if(modelinfo->forecastMethod== tempModel->forecastMethod) {
			foreach(lc3, modelinfo->parameterList) {
				paraFound=false;
				paraEqual=1;
				foreach(lc4,tempModel->parameterList) {
					AlgorithmParameter *findPar = (AlgorithmParameter*) lfirst(lc3);
					AlgorithmParameter *searchPar = (AlgorithmParameter*) lfirst(lc4);
					switch (searchPar->value->val.type) {
					case T_Integer:
						if(findPar->value->val.type!=T_Integer)
							continue;
						if(strcmp(findPar->key,searchPar->key))
							continue;
						paraEqual = !(((searchPar->value->val.val.ival)==(int)(findPar->value->val.val.ival)));
						break;
					case T_String:
						if(findPar->value->val.type!=T_String)
							continue;
						if(strcmp(findPar->key,searchPar->key))
							continue;
						paraEqual = strcmp(searchPar->value->val.val.str, findPar->value->val.val.str);
						break;
					default:
						break;
					}
					if(paraEqual==0) {
						paraFound=true;
						break;
					}
				}
			}
			if(!paraFound)
				continue;
			else {
				result=lappend(result,tempD);
				continue;
			}
		} else if(!modelinfo->forecastMethod) {
			result=lappend(result,tempD);
		} else
			continue;
	}
	//TODO Select best model through to Aggs
	if(result!=NIL) {
		return lfirst(list_head((result)));
	} else
		return NULL;


}

static int _searchModel(ModelInfo *model,ModelGraphIndexNode *mgin,List **resultAggModels,Node*resultWhere,List *rtable, bool allowDisaggAggs) {
	ArListCell *arl;
	ListCell *lc1,*lc4;
	List *resultNodes=NIL;
	arforeach(arl, mgin->children) {
		resultNodes=lappend(resultNodes,FindCorrectNode(arlfirst(arl),copyObject(resultWhere),rtable));
	}
	foreach(lc1,resultNodes) {
		ModelGraphIndexNode *temp=lfirst(lc1);
		ModelInfo *modelTemp=_getModel(temp,model);
		if(modelTemp) {
			ModelInfo *resmodel=palloc0(sizeof(ModelInfo));
			memcpy(resmodel,modelTemp,sizeof(ModelInfo));
			*resultAggModels=lappend(*resultAggModels,resmodel);
			continue;
		}
		if (allowDisaggAggs) {
			DisAggModel *searchHit2=_getDisAgg(temp,model);
			if(searchHit2) {
					int found=0;
					//Maybe the model already exists, therefore the disaggkey should be increased
					foreach(lc4,*resultAggModels) {
						ModelInfo *searchM=lfirst(lc4);
						if(searchM==((DisAggModel*)(searchHit2))->model) {
							searchM->disaggkey+=((DisAggModel*)(searchHit2))->disAggKeyNumerator/((DisAggModel*)(searchHit2))->model->disAggKeyDenominator;
							found=1;
							break;
						}
					}
					if(!found) {
						ModelInfo *resmodel=palloc0(sizeof(ModelInfo));
						memcpy(resmodel,((DisAggModel*)(searchHit2))->model,sizeof(ModelInfo));
						*resultAggModels=lappend(*resultAggModels,resmodel);
						((ModelInfo*)lfirst(list_tail(*resultAggModels)))->disaggkey=((DisAggModel*)(searchHit2))->disAggKeyNumerator/((DisAggModel*)(searchHit2))->model->disAggKeyDenominator;
						continue;
					} else
						continue;

			}
		}
		//is there still an agg for this?
		if(!temp->aggIndicator)
			{
				if(resultWhere)
					pfree(resultWhere);
				return 0;
			}
		else //search Aggs
		{
			int foundagg=1;
			int foundFlag=0;
			ModelGraphIndexNode *iter =temp;
			while(iter->parents && iter->parents->length<=1) {
				iter=arlfirst(arlist_head(iter->parents));
				if(!(iter->type==T_AggNode)) {
					continue;
				}
				//we got an agg Node on Path up to Root, check for aggregation values
				foundagg= foundagg && _searchModel(model,arlfirst(arlist_head(iter->parents)),resultAggModels,copyObject(resultWhere),rtable, allowDisaggAggs); //XXX
				foundFlag=foundagg;
			}
			if(!foundFlag)
			{
				if(resultWhere)
					pfree(resultWhere);
				return 0;
			}
		}
			
		
	}
	if(resultWhere)
		pfree(resultWhere);
	return 1;


}

int FindModelGraphModels(ModelInfo* modelinfo,List **resultList,List **possiblePaths, List **createModels,List *rtable,Node **whereExpr) {
	
	ModelInfo *searchM;
	List* workingList=NIL;
	int *added,*ormatch;
	Node *wherecopy;
	List *uniqueResultMgins=NIL;
	List *resultAggModels=NIL;
	List *possiblePath = NIL;
	ListCell *lc1,*lc2,*lc4;
	int kill;
	int mcounter=0;
	int dacounter=0;
	int agcounter=0;
	int crcounter=0;
	Node *whereExpr2;
	ParseState 		*pstate;
	int foundagg=0; //flag, meaning 0=not found
	SearchPathNode *root=calloc(1,sizeof(SearchPathNode));
	root->mgin=getModelGraphRoot();
	wherecopy=copyObject(*whereExpr);
	wherecopy=transformWhereExpr(wherecopy);
	
	root->whreExpr=wherecopy;
	*possiblePaths=lappend(*possiblePaths,root);
	added=palloc(sizeof(int));
	*added=0; //flag if something was added at given tree level, needed to determine if AggNode need to be added
	ormatch=palloc(sizeof(int));
	*ormatch=0; //flag if an orExpression was evaluated
	pstate = make_parsestate(NULL);


elog(INFO,"Starting Modelgraphlookup");
	while(*possiblePaths!=NIL) { //If This List is empty, we have followed every possible Path to a leaf
		//get Next Path and his whereExpr
		ModelGraphIndexNode *mgin=((SearchPathNode*)lfirst(list_head(*possiblePaths)))->mgin;
		if(!mgin->children) { //found a leaf Node
			//just add the mgin to the resultlist, we will choose a model later
			workingList=lappend(workingList,mgin);
			//free pathnode and delete it from possiblePaths
			*possiblePaths=list_delete_first(*possiblePaths);
			continue;
		}
		//if we have not reached a leaf yet, get the whereExpr, that still needs to be matchd to the graph
		whereExpr2=((SearchPathNode*)lfirst(list_head(*possiblePaths)))->whreExpr;

		//free and delete Path from List, since we will evaluate it now


		*added=0;
		LookUpAndAddPath(mgin,(BoolExpr*)whereExpr2,(BoolExpr*)whereExpr2 ,possiblePaths,added,rtable);
		*possiblePaths=list_delete_first(*possiblePaths);
	}
	if(!workingList)
		elog(ERROR,"The given query does not match the modelgraph");
	elog(INFO,"Evaluated %i Paths",workingList->length);

	//Make Results unique
	foreach(lc1,workingList) {
		kill=0;
		foreach(lc2,uniqueResultMgins) {
			if(lfirst(lc1)==lfirst(lc2)) {
				kill=1;
				break;
			}
		}
		if(!kill)
			uniqueResultMgins=lappend(uniqueResultMgins,lfirst(lc1));

	}
	elog(INFO,"Found %i unique ModelNodes",uniqueResultMgins->length);

	foreach(lc1,uniqueResultMgins) { //fetch models
		ModelGraphIndexNode *mgin=lfirst(lc1);
		ModelInfo *searchHit=_getModel(mgin,modelinfo);
		if(searchHit) {
			ModelInfo *resmodel=palloc0(sizeof(ModelInfo));
			memcpy(resmodel,searchHit,sizeof(ModelInfo));
			*resultList=lappend(*resultList,resmodel);
			((ModelInfo*)lfirst(list_tail(*resultList)))->disaggkey=1;
			mcounter++;
			continue;
		}

		if(mgin->disAggModels!=NIL) {
			DisAggModel *searchHit2= _getDisAgg(mgin,modelinfo);
			if(searchHit2) {
				int found=0;
				//Maybe the model already exists, therefore the disaggkey should be increase
				foreach(lc4,*resultList) {
					ModelInfo *searchM=lfirst(lc4);
					if(searchM==((DisAggModel*)(searchHit2))->model) {
						searchM->disaggkey+=((DisAggModel*)(searchHit2))->disAggKeyNumerator/((DisAggModel*)(searchHit2))->model->disAggKeyDenominator;
						found=1;
						dacounter++;
						break;
					}
				}
				if(!found) {
					ModelInfo *resmodel=palloc0(sizeof(ModelInfo));
					memcpy(resmodel,((DisAggModel*)(searchHit2))->model,sizeof(ModelInfo));
					*resultList=lappend(*resultList,resmodel);
					((ModelInfo*)lfirst(list_tail(*resultList)))->disaggkey=((DisAggModel*)(searchHit2))->disAggKeyNumerator/((DisAggModel*)(searchHit2))->model->disAggKeyDenominator;
					dacounter++;
					continue;
				} else
					continue;

			}

		}

		//Find aggs
		if((mgin->aggIndicator)) {
			ModelGraphIndexNode *iter =mgin;
			List *exprList=NIL;
			A_Expr *apex;
			Node *resultWhere=NULL;
			while(iter->parents) {//Create where expr for the node, we try to find an aggregation for
				//if iter is an AggNode, don't add a predicate to the NEW whereClause
				if(iter->type != T_AggNode)
					exprList = lappend(exprList, CreateA_Expr(iter->target, iter->value));

				if(((ModelGraphIndexNode *)arlfirst(arlist_head(iter->parents)))->type == T_IndexNode) {
					iter = (ModelGraphIndexNode *)arlfirst(arlist_head(iter->parents));
				} else {
					iter = (ModelGraphIndexNode *)arlfirst(arlist_tail(iter->parents));
				}
			}
			if(exprList) { //form an usable whereExpr if needed
				//restructure the NEW whereClause to be left-recursive
				if(list_length(exprList)>=2) {
					apex = RestructureA_Expr(exprList);
				} else {
					apex = lfirst(list_head(exprList));
				}

				pstate->p_sourcetext = NULL;
				pstate->p_variableparams = false;

				//for ColumnRefs with prepended table
				pstate->p_relnamespace = rtable;
				//for ColumnRefs without prepended table
				pstate->p_varnamespace = rtable;
				//to obtain RTEs
				pstate->p_rtable = rtable;
				resultWhere=transformExpr(pstate, (Node*)apex);
				pfree(pstate);
			}


			
			iter =mgin;
			while(iter->parents && iter->parents->length<2) {
				foundagg=0;
				if(!(iter->type==T_AggNode)) {
					iter=arlfirst(arlist_head(iter->parents));
					continue;
				}
				//we got an agg Node on Path up to Root, check for aggregation values
				foundagg=_searchModel(modelinfo,arlfirst(arlist_head(iter->parents)),&resultAggModels,copyObject(resultWhere),rtable, false);
				if(!foundagg) {
					//check if whereExpr is empty, if so we have another possibility to aggregate
					if(resultWhere==NULL)
					{
						List *createModels2=NIL;
						List *result1;
						ArListCell *alc1;
						arforeach(alc1,((ModelGraphIndexNode*)arlfirst(arlist_head(iter->parents)))->children)
						{
							Node *buildWhere;

							//build whereExpr for every sibling of the node we search an aggregation for
							ModelGraphIndexNode *iter2=arlfirst(alc1);
							exprList=NIL;
							apex=NULL;
							while(iter2->parents) {//Create where expr for the node, we try to find an aggregation for
							//if iter is an AggNode, don't add a predicate to the NEW whereClause
								if(iter2->type != T_AggNode)
									exprList = lappend(exprList, CreateA_Expr(iter2->target, iter2->value));

								if(((ModelGraphIndexNode *)arlfirst(arlist_head(iter2->parents)))->type == T_IndexNode) {
									iter2 = (ModelGraphIndexNode *)arlfirst(arlist_head(iter2->parents));
								} else {
									iter2 = (ModelGraphIndexNode *)arlfirst(arlist_tail(iter2->parents));
								}
							}
							if(exprList) { //form an usable whereExpr if needed
							//restructure the NEW whereClause to be left-recursive
								if(list_length(exprList)>=2) {
									apex = RestructureA_Expr(exprList);
								} else {
									apex = lfirst(list_head(exprList));
								}
								pstate = make_parsestate(NULL);
								pstate->p_sourcetext = NULL;
								pstate->p_variableparams = false;

								//for ColumnRefs with prepended table
								pstate->p_relnamespace = rtable;
								//for ColumnRefs without prepended table
								pstate->p_varnamespace = rtable;
								//to obtain RTEs
								pstate->p_rtable = rtable;
								buildWhere=transformExpr(pstate, (Node*)apex);
								pfree(pstate);
							}

							//execute modelgraph-search for the sibling
							result1=NIL;
							
							FindModelGraphModels(modelinfo,&result1,&possiblePath, &createModels2,rtable,&(buildWhere));
							if((createModels2)!=NIL)//sibling can't be aggregated
								break;
							foreach(lc1,result1)//elements of result1List has to be copied to resultAggModels because an empty resultList is needed for next sibling
							{
								resultAggModels=lappend(resultAggModels,lfirst(lc1));
							}
						}


						if(!((createModels2)!=NIL))
							{
								foundagg=1;

							}

					}
					if(!foundagg)
					{
						resultAggModels=NIL;
						iter=arlfirst(arlist_head(iter->parents));
						continue;
					}
				}
				foundagg=3;
				break;


			}
			if(foundagg==3) {
				ListCell *lc1;
				int found=0;
				foreach(lc1,resultAggModels) {
					foreach(lc4,*resultList) {
						searchM=lfirst(lc4);
						if(searchM==(ModelInfo*)((DisAggModel*)(lfirst(lc1)))) {
							searchM->disaggkey+=((DisAggModel*)(lfirst(lc1)))->disAggKeyNumerator/((DisAggModel*)(lfirst(lc1)))->model->disAggKeyDenominator;
							found=1;
							break;
						}
						
						
					}
					if(!found) {
							*resultList=lappend(*resultList,(lfirst(lc1)));
							((ModelInfo*)lfirst(list_tail(*resultList)))->disaggkey=1;
						}
				}
				agcounter++;
				continue;
			}

		}

		//nothing found build a create Model

		{
			
			modelinfo->buildModel = true;
			//Add the model, we plan to create to the candidates
			*createModels=lappend(*createModels,modelinfo);
			crcounter++;

		}

	}
	elog(INFO,"Use %i model(s)",mcounter);
	elog(INFO,"Use %i Disaggregation(s)",dacounter);
	elog(INFO,"Use %i Aggregation(s)",agcounter);
	elog(INFO,"Have to create %i model(s)",crcounter);


	releaseModelGraphRoot();

	return (*resultList) ? 1 : 0;
}


static void parameterListToString(StringInfo str,List *pl)
{
	ListCell *lc1;
	if(pl==NIL)
		return;
	
	foreach(lc1,pl) {
		AlgorithmParameter		*param = lfirst(lc1);
		appendStringInfo(str, "%s=",param->key);
		if(IsA(&(param->value->val),Integer)) {
			appendStringInfo(str,"%li;",intVal(&param->value->val));
			continue;
		}
		if(IsA(&(param->value->val),Float)) {
			appendStringInfo(str,"%f;",floatVal(&param->value->val));
			continue;
		}
		if(IsA(&(param->value->val),String)) {
			appendStringInfo(str,"'%s';",strVal(&param->value->val));
			continue;
		}
	}
}

void
PrintNode2(ModelGraphIndexNode *mgin,StringInfo str)
{
	ListCell *lc1;
	ArListCell			*alc;
	Oid					typeOid;
	ModelGraphIndexNode	*tempMgin;
	DisAggModel *dim;
	int i=0;
	ModelInfo *mi;
	char* color;
	arforeach(alc, mgin->children) {
		tempMgin = (ModelGraphIndexNode *)arlfirst(alc);
		if (tempMgin->disAggModels || tempMgin->aggModels || tempMgin->models)
			color="violet";
		else
			color="black";
		//if an AggNode should be printed and its actual considered child has more than 1 parent(e.g. when there is a correlation)...
		if(nodeTag(mgin)==T_AggNode && tempMgin->parents->length>1) {
			//...only the edges to the child should be printed and NOTHING else
			if(!mgin->target) {
				appendStringInfo(str, "root -> node%p;\n", tempMgin);
			} else {
				appendStringInfo(str, "node%p -> node%p;\n", mgin, tempMgin);
			}
		} else {
			typeOid = getTEVar(tempMgin->target)->vartype;
			switch(typeOid) {
			case 20:
			case 21:
			case 23: {
				if(tempMgin->children)
					appendStringInfo(str, "node%p [label=\"{%s|%i}\"];\n", tempMgin, tempMgin->target->resname, GetDatumAsInt(typeOid,tempMgin->value));
				else {
					appendStringInfo(str, "node%p [mginid={%i},label=\"{%s|%i}\", modelCount={%i}", tempMgin, tempMgin->id,tempMgin->target->resname, GetDatumAsInt(typeOid,tempMgin->value),list_length(tempMgin->models)+list_length(tempMgin->disAggModels));
			
				i=0;
				foreach(lc1,tempMgin->disAggModels) {
				dim=lfirst(lc1);
				mi=dim->model;
				appendStringInfo(str, "  t%i=\"{%s}\",m%i=\"{%s}\",name%i=\"{%s}\",parameter%i={",i,mi->time->resname,i,mi->measure->resname,i,mi->modelName,i);

				appendStringInfo(str, " model name=%s; ",mi->modelName);
				appendStringInfo(str, " model type=%s; ",getModelTypeAsString(mi->forecastMethod));
				appendStringInfo(str, " training data=%s; ",mi->trainingData);
				appendStringInfo(str, " type=Disagg; ");
				appendStringInfo(str, " disag key=%f; ",dim->disAggKeyNumerator/dim->model->disAggKeyDenominator);
				parameterListToString(str,mi->parameterList);
				appendStringInfo(str, "}");
				i++;
			}
					foreach(lc1,tempMgin->models) {
						ModelInfo *mi=lfirst(lc1);
						appendStringInfo(str, "  t%i=\"{%s}\",m%i=\"{%s}\",name%i=\"{%s}\",parameter%i={",i,mi->time->resname,i,mi->measure->resname,i,mi->modelName,i);
						appendStringInfo(str, " model name=%s; ",mi->modelName);
						appendStringInfo(str, " model type=%s; ",getModelTypeAsString(mi->forecastMethod));
						appendStringInfo(str, " training data=%s; ",mi->trainingData);
						appendStringInfo(str, " type=Model; ");
						parameterListToString(str,mi->parameterList);
						appendStringInfo(str, "}");
						i++;
					}
				
					appendStringInfo(str, "];\n");
				}
				break;
			}
			case 700:
			case 701: {
				if(tempMgin->children)
					appendStringInfo(str, "node%p [label=\"{%s|%f}\"];\n", tempMgin, tempMgin->target->resname, GetDatumAsDouble(typeOid,tempMgin->value));
				else {
					appendStringInfo(str, "mginid={%i},node%p [label=\"{%s|%f}\", modelCount={%i}", tempMgin->id,tempMgin,tempMgin->target->resname, GetDatumAsDouble(typeOid,tempMgin->value),list_length(tempMgin->models)+list_length(tempMgin->disAggModels));
		
				i=0;
				foreach(lc1,tempMgin->disAggModels) {
				dim=lfirst(lc1);
				mi=dim->model;
				appendStringInfo(str, "  t%i=\"{%s}\",m%i=\"{%s}\",name%i=\"{%s}\",parameter%i={",i,mi->time->resname,i,mi->measure->resname,i,mi->modelName,i);

				appendStringInfo(str, " model name=%s; ",mi->modelName);
				appendStringInfo(str, " model type=%s; ",getModelTypeAsString(mi->forecastMethod));
				appendStringInfo(str, " training data=%s; ",mi->trainingData);
				appendStringInfo(str, " type=Disagg; ");
				appendStringInfo(str, " disag key=%f; ",dim->disAggKeyNumerator/dim->model->disAggKeyDenominator);
				parameterListToString(str,mi->parameterList);
				appendStringInfo(str, "}");
				i++;
			}
					foreach(lc1,tempMgin->models) {
						mi=lfirst(lc1);
						appendStringInfo(str, "  t%i=\"{%s}\",m%i=\"{%s}\",name%i=\"{%s}\",parameter%i={",i,mi->time->resname,i,mi->measure->resname,i,mi->modelName,i);
						appendStringInfo(str, " model name=%s; ",mi->modelName);
						appendStringInfo(str, " model type=%s; ",getModelTypeAsString(mi->forecastMethod));
						appendStringInfo(str, " training data=%s; ",mi->trainingData);
						appendStringInfo(str, " type=Model; ");
						parameterListToString(str,mi->parameterList);
						appendStringInfo(str, "}");
						i++;
					}
				
					appendStringInfo(str, "];\n");
				}
				break;
			}
			case 1042: {
				if(tempMgin->children)
					appendStringInfo(str, "node%p [label=\"{%s|%s}\"];\n", tempMgin,  tempMgin->target->resname, DatumGetCString(DirectFunctionCall1(bpcharout, tempMgin->value)));
				else {
					appendStringInfo(str, "node%p [mginid={%i},label=\"{%s|%s}\", modelCount={%i}",tempMgin, tempMgin->id, tempMgin->target->resname, DatumGetCString(DirectFunctionCall1(bpcharout, tempMgin->value)),list_length(tempMgin->models)+list_length(tempMgin->disAggModels));
				
				i=0;
				foreach(lc1,tempMgin->disAggModels) {
				dim=lfirst(lc1);
				mi=dim->model;
				appendStringInfo(str, "  t%i=\"{%s}\",m%i=\"{%s}\",name%i=\"{%s}\",parameter%i={",i,mi->time->resname,i,mi->measure->resname,i,mi->modelName,i);

				appendStringInfo(str, " model name=%s; ",mi->modelName);
				appendStringInfo(str, " model type=%s; ",getModelTypeAsString(mi->forecastMethod));
				appendStringInfo(str, " training data=%s; ",mi->trainingData);
				appendStringInfo(str, " type=Disagg; ");
				appendStringInfo(str, " disag key=%f; ",dim->disAggKeyNumerator/dim->model->disAggKeyDenominator);
				parameterListToString(str,mi->parameterList);
				appendStringInfo(str, "}");
				i++;
			}
					foreach(lc1,tempMgin->models) {
						ModelInfo *mi=lfirst(lc1);
						appendStringInfo(str, "  t%i=\"{%s}\",m%i=\"{%s}\",name%i=\"{%s}\",parameter%i={",i,mi->time->resname,i,mi->measure->resname,i,mi->modelName,i);
						appendStringInfo(str, " model name=%s; ",mi->modelName);
						appendStringInfo(str, " model type=%s; ",getModelTypeAsString(mi->forecastMethod));
						appendStringInfo(str, " training data=%s; ",mi->trainingData);
						appendStringInfo(str, " type=Model; ");
						parameterListToString(str,mi->parameterList);
						appendStringInfo(str, "}");
						i++;
					}
				
					appendStringInfo(str, "];\n");
				}
				break;
			}
			case 1043: {
				if(tempMgin->children)
					appendStringInfo(str, "node%p [label=\"{%s|%s}\"];\n", tempMgin,tempMgin->target->resname, DatumGetCString(DirectFunctionCall1(varcharout, tempMgin->value)));
				else {
					appendStringInfo(str, "node%p [mginid={%i},label=\"{%s|%s}\", modelCount={%i}",tempMgin, tempMgin->id,tempMgin->target->resname, DatumGetCString(DirectFunctionCall1(bpcharout, tempMgin->value)),list_length(tempMgin->models)+list_length(tempMgin->disAggModels));
				
			i=0;
				foreach(lc1,tempMgin->disAggModels) {
				dim=lfirst(lc1);
				mi=dim->model;
				appendStringInfo(str, "  t%i=\"{%s}\",m%i=\"{%s}\",name%i=\"{%s}\",parameter%i={",i,mi->time->resname,i,mi->measure->resname,i,mi->modelName,i);

				appendStringInfo(str, " model name=%s; ",mi->modelName);
				appendStringInfo(str, " model type=%s; ",getModelTypeAsString(mi->forecastMethod));
				appendStringInfo(str, " training data=%s; ",mi->trainingData);
				appendStringInfo(str, " type=Disagg; ");
				appendStringInfo(str, " disag key=%f; ",dim->disAggKeyNumerator/dim->model->disAggKeyDenominator);
				parameterListToString(str,mi->parameterList);
				appendStringInfo(str, "}");
				i++;
			}
					foreach(lc1,tempMgin->models) {
						mi=lfirst(lc1);
						appendStringInfo(str, "  t%i=\"{%s}\",m%i=\"{%s}\",name%i=\"{%s}\",parameter%i={",i,mi->time->resname,i,mi->measure->resname,i,mi->modelName,i);
						appendStringInfo(str, " model name=%s; ",mi->modelName);
						appendStringInfo(str, " model type=%s; ",getModelTypeAsString(mi->forecastMethod));
						appendStringInfo(str, " training data=%s; ",mi->trainingData);
						appendStringInfo(str, " type=Model; ");
						parameterListToString(str,mi->parameterList);
						appendStringInfo(str, "}");
						i++;
					}
				
					appendStringInfo(str, "];\n");
				}
				break;
			}
			default:
				elog(ERROR, "PrintNode can't handle type %i of column %s!", typeOid, tempMgin->target->resname);
			}
			if(!mgin->target) {
				appendStringInfo(str, "root -> node%p;\n", tempMgin);
			} else {
				appendStringInfo(str, "node%p -> node%p;\n", mgin, tempMgin);
			}
			PrintNode2(tempMgin,str);
		}
	}
	if(mgin->aggChild) {
		tempMgin = mgin->aggChild;
		if(tempMgin->children)
			appendStringInfo(str,  "node%p [label=\"{Agg}\"];\n", tempMgin);
		else {
			appendStringInfo(str, "node%p [mginid={%i},label=\"{Agg}\", modelCount={%i}",  tempMgin, tempMgin->id,list_length(tempMgin->models)+list_length(tempMgin->disAggModels));
			
			i=0;
			foreach(lc1,tempMgin->disAggModels) {
				dim=lfirst(lc1);
				mi=dim->model;
				appendStringInfo(str, "  t%i=\"{%s}\",m%i=\"{%s}\",name%i=\"{%s}\",parameter%i={",i,mi->time->resname,i,mi->measure->resname,i,mi->modelName,i);

				appendStringInfo(str, " model name=%s; ",mi->modelName);
				appendStringInfo(str, " model type=%s; ",getModelTypeAsString(mi->forecastMethod));
				appendStringInfo(str, " training data=%s; ",mi->trainingData);
				appendStringInfo(str, " type=Disagg; ");
				appendStringInfo(str, " disag key=%f; ",dim->disAggKeyNumerator/dim->model->disAggKeyDenominator);
				parameterListToString(str,mi->parameterList);
				appendStringInfo(str, "}");
				i++;
			}
			foreach(lc1,tempMgin->models) {
				mi=lfirst(lc1);
				appendStringInfo(str, "  t%i=\"{%s}\",m%i=\"{%s}\",name%i=\"{%s}\",parameter%i={",i,mi->time->resname,i,mi->measure->resname,i,mi->modelName,i);

				appendStringInfo(str, " model name=%s; ",mi->modelName);
				appendStringInfo(str, " model type=%s; ",getModelTypeAsString(mi->forecastMethod));
				appendStringInfo(str, " training data=%s; ",mi->trainingData);
				appendStringInfo(str, " type=Model; ");
				parameterListToString(str,mi->parameterList);
				appendStringInfo(str, "}");
				i++;
			}
			
			appendStringInfo(str, "];\n");
		}
		if(!mgin->target) {
			appendStringInfo(str,  "root -> node%p;\n", tempMgin);
		} else {
			appendStringInfo(str,  "node%p -> node%p;\n", mgin, tempMgin);
		}
		PrintNode2(tempMgin,str);
	}
}

char* printModelGraph(void)
{
	StringInfoData buf;
	ModelGraphIndexNode *mgin;
	ListCell *lc1;
	initStringInfo(&buf);

	mgin=getModelGraphRoot();
	
	foreach(lc1,mgin->targetEntryList)
	{
		Var* temp=getTEVar(lfirst(lc1));
		if(temp->vartype>1000)
			appendStringInfo(&buf, "%s:true;",((TargetEntry*)lfirst(lc1))->resname);
		else
			appendStringInfo(&buf, "%s:false;",((TargetEntry*)lfirst(lc1))->resname);
	}
	appendStringInfo(&buf, "\n");
	
	appendStringInfo(&buf, "\n");
	if(mgin->children)
		appendStringInfo(&buf, "root [%s]\n",graphCreationSourceText);
	else
		appendStringInfo(&buf, "root [Unknown]\n");
	PrintNode2(mgin,&buf);
	releaseModelGraphRoot();
	return buf.data;
}

bool isModelGraphExistent(void)
{
	bool result;
	ModelGraphIndexNode *mgin=getModelGraphRoot();
	if(mgin->children)
		result=true;
	else
		result=false;
	releaseModelGraphRoot();
	return result;
	
}
