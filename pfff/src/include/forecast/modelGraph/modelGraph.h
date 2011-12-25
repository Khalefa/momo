/*
 * modelGraph.h
 *
 *  Created on: 23.02.2011
 *      Author: b1anchi
 */

#ifndef MODELGRAPH_H_
#define MODELGRAPH_H_

#include "postgres.h"
#include "nodes/parsenodes.h"
#include "lib/stringinfo.h"
#include "parser/parse_clause.h"

extern ModelGraphIndex *modelGraphIdx;

void
setConfError(double newError);

double
getConfError(void);

void
setTrainingDataQuery(char *sourceText, int length);

char *
getTrainingDataQuery(void);

Var *
getTEVar(TargetEntry *te);

bool
modelGraphDepthCheck(void);

bool
checkDepthrekur(int maxheight,int currentheight,List* typeInfo,ModelGraphIndexNode * iter);

ModelGraphIndexNode *
getModelGraphRoot(void);

char * 
getWhereExprAsString(ModelGraphIndexNode* mgin);

char *
getSourceText(void);

void
extractSubquery(const char *sourcetext);

void
releaseModelGraphRoot(void);

MemoryContext
getModelGraphContext(void);

void
updateModelGraphIdx(ModelInfo *resInfo);

void
runQuery(char* queryString,char* tag);

Datum
determineWhichNodeToUse(Node **whereExpr, TargetEntry *tle, List *rTable);

ModelGraphIndexNode *
FindCorrectNode(ModelGraphIndexNode *mgin, Node *whereCopy, List *rTable);

void
AddModelToModelGraph(ModelInfo *modelInfo, List *rTable);

void
GetTuplesFromSubquery(CreateModelGraphStmt *stmt, char *completionTag);

GraphAttribute *
transformGraphAttribute(GraphAttribute *gAtt, ParseState *pstate, List *teList);

void
PrintNode(FILE *file, ModelGraphIndexNode *mgin);

void
AddChildren(ModelGraphIndexNode *node, List *childList, TargetEntry *tle, bool unique, int aggFlag);

void
AddCorrelationChild(ModelGraphIndexNode *node, List *corrElements, List *corrData, int corrLevel);

void
CreateBackupTables(void);

void
CreateModelGraph(CreateModelGraphStmt *stmt);

void
EmptyModelGraph(void);

void
UpdateModelInfoErrorArray(ModelInfo	*model, double error);

void
UpdateDisAggModelErrorArray(DisAggModel *dam, double error);

void
MaintainModelGraph(HeapTuple newTuple, Relation sourceRelation);

void
MaintainModelGraphAggs(void);

void
ReestimateModelGraphModel(ModelInfo *model);

void
ReestimateAllModelGraphModels(void);

char*
_getModelGraphNameForActiveGraph(void);

void
deleteModelGraph(void);

void
_restoreModelGraphStructure(char* name);

Oid
_getFillingId(char* mginid,char*fillingname);

void
_insetModelIntoModelgraph(ModelGraphIndexNode *mgin,Datum* values,Relation p);

void
StoreModelGraph(StoreStmt *stmt);

int
_eliminateAndTransformAndClause(OpExpr *clauseToKill, BoolExpr *clause,List *rtable,int*foundClause);

Node*
_eliminateAndTransformWhere(OpExpr *clauseToKill, BoolExpr *whereRoot, List *rtable);

int
modelGraphCompatible(Node* whereExpr,Query *qry);

A_Expr *
CreateA_Expr(TargetEntry *target, Datum value);

void
FreeA_Expr(A_Expr *aexp, bool withJoins);

A_Expr *
RestructureA_Expr(List *exprList);

List *
ExtractJoins(Node *whereExpr);

double
getNextDisAggValue(DisAggModel *dam, int num);

float
CreateDisaggScheme(CreateDisAggSchemeStmt *stmt, float sourceSum);

List *
GetAllChildren(ModelGraphIndexNode *inMgin, bool withAggs);

List *
SetModelGraphIds(ModelGraphIndexNode *inMgin);

void
FillModelGraph(FillModelGraphStmt *stmt);

void
FillModelGraphTopDown(FillModelGraphStmt *stmt);

void
FillModelGraphBottomUp(FillModelGraphStmt *stmt);

void
EmptyModelGraph();

void
LookUpAndAddPath(ModelGraphIndexNode *mgin, BoolExpr *whereExpr, BoolExpr *whereRoot, List **possiblePaths, int *added,List *rtable);

int
FindModelGraphModels(ModelInfo* modelinfo,List **resultList,List **possiblePaths, List **createModels,List *rtable,Node **whereExpr);

void
stagingAddTupel(int mginId);

bool
staging_updatable(void);

bool
staging_update(void);

bool isModelGraphExistent(void);
char* printModelGraph(void);
void
PrintNode2(ModelGraphIndexNode *mgin,StringInfo str);
void _storeModelGraphFilling(char* fillingname,char* name);
List** _mergeAndClauses(BoolExpr *boolep,List** result);
Node *mergeAndClauses(Node* whereExpr);
Node* transformOpExpr(Node* whereExpr);
Node* sortBoolExprs(BoolExpr* whereExpr);
void _mergeOrClauses(BoolExpr* whereExpr,List** result);
Node* mergeOrClauses(BoolExpr* whereExpr);
Node* transformWhereExpr(Node* whereEpxr);
void _restoreModels(Oid fillid,ModelGraphIndexNode** mginIndex);
void _insetDisaggIntoModelgraph(ModelGraphIndexNode* to,ModelGraphIndexNode* from,Datum* values);
void _storeModel(ModelInfo *mio,Relation parameterRelation,Relation modelRelation,int fillid,int mginId);
void _restoreDisaggs(Oid fillid,ModelGraphIndexNode** mginIndex);
void _restoreFilling(Oid fillid);
void _restoreModelGraphFilling(char*mginid,char *fillingname);
void RestoreModelGraph(RestoreStmt* stmt);
Oid _generateFilling(char *fillingName,char* name);
Oid _insertForecastModel(ModelInfo *mio,Relation r,Oid fillid,int mginId);
void _storeDisAgg(DisAggModel *dag,Relation r, Oid fillid,int mginid);
void _storeModelsandDisaggs(Oid id);
void _storeModelGraphStructure(char* name);
void
SetModelGraphArray(void); 

#endif /* MODELGRAPH_H_ */
