#ifndef MODELINDEX_H_
#define MODELINDEX_H_

#include <time.h>
#include <stdlib.h>
#include "access/htup.h"
#include "utils/relcache.h"
#include "forecast/modelindex/hashtable/hashtable.h"
#include "nodes/primnodes.h"
#include "nodes/parsenodes.h"
#include "forecast/modelindex/modelnodes.h"
#include "forecast/modelindex/btree/btree.h"


#define			MAXCONDITIONS   20
#define			MAXJOINS		5
#define			MAXINLISTSIZE	30

void 
StartIndexing(void);

void
EndIndexing(void);

extern MemoryContext getModelMemoryContext(void);

void
PrintDetails(void);

void
PrintConnections(FILE *file, void *source, List *connections);

void
PrintModels(FILE *file, void *source, List *models);

int 
condition_cmp(const struct Condition *c1, const struct Condition *c2);

void
PreprocessWhereClause(Node *whereExpr, List *rtable, Condition** conditions, int* ccount, JoinNode **joins, int *jcount);

void
AddModelToModelIndex(TableNode *parent, TargetEntry *measure, TargetEntry *time, short granularity, Oid aggType, int timestamp, double forecast, Model *model, ModelType forecastMethod,List* para);

Node*
AddJoin(Oid tab1, AttrNumber col1, Oid tab2, AttrNumber col2);

Node*
AddConnection(Node *leftPred, Node *rightPred, short type);

Node*
AddPredicate(Oid reloid, AttrNumber num, int value, short type);


List *
FindMatchingNode(ModelInfo* modelInfo, Node *whereExpr, List *rtable,char* targetDateString);

TableNode*
GetTableNode(Oid relid);

int
GetJoinTuple(Oid tab, Datum val, Datum **heapValues, bool **heapIsNull);

void
FindConnections(List *conditions, List **models, List **connections);

void
CheckPredicates(ColumnNode *node, int value, List **models, List **connections);

void 
AddLessThan(bt_node *node, int key, List **models, List **connections);

void
AddGreaterThan(bt_node *node, int key, List **models, List **connections);

void 
MaintenanceInsert(HeapTuple newTuple, Relation sourceRelation);

void
MaintainModel(ModelNode *modelNode, Datum *sourceValues, bool *sourceIsNull);

void
UpdateModelEvaluation(ModelNode *modelNode, double newValue);


// EXPERIMENTS FOR THE PAPER "Indexing Forecast Models for Matching und Maintenance" 
void
MaintenanceEntryPoint(HeapTuple newTuple, Relation sourceRelation);

void
MaintenanceTest(HeapTuple newTuple, Relation sourceRelation);

Node*
TestIndexTimeSeries(ModelInfo *modelInfo, Node* whereExpr, List* rTable);

bool
TestQueryString(ModelInfo *modelInfo, const char *sourcetext);

bool
SearchQueryString(ModelInfo *modelInfo, const char *sourcetext);

void
AddModelToHashTable(TargetEntry *measure, TargetEntry *time, int granularity, Oid aggType, const char *sourcetext, int timestamp, double forecast, Model *model);

void
PrintBtree(FILE *file, void *src, btree *btree, bt_node *node, int type);


#endif /*MODELINDEX_H_*/