/*-------------------------------------------------------------------------
 *
 * algorithm.c
 *
 *
 * Portions Copyright (c) 1996-2009, PostgreSQL Global Development Group
 *
 * IDENTIFICATION
 *		  $PostgreSQL: pgsql/src/backend/forecast/algorithm.c,v 1.5 2009/06/11 16:14:18 tgl Exp $
 *
 *-------------------------------------------------------------------------
 */
#include "postgres.h"


#include "forecast/algorithm.h"
#include "utils/syscache.h"
#include "access/htup.h"
#include "catalog/pg_fcalg.h"
#include "catalog/pg_model.h"
#include "catalog/pg_type.h"
#include "nodes/nodeFuncs.h"
#include "utils/builtins.h"
#include "utils/date.h"
#include "utils/guc.h"
#include "access/heapam.h"
#include "access/tupdesc.h"
#include "utils/tqual.h"
#include "executor/executor.h"
#include "access/xact.h"


/*
 * macro for getting forecast algorithm procedures
 */

#define ASSIGN_ALGORITHM_PROCEDURE(pname) \
do{ \
	proc = fcalgSysData->pname; \
	if(!RegProcedureIsValid(proc)) \
		elog(ERROR, "invalid %s regproc", CppAsString(pname)); \
	procedureInfo = palloc(sizeof(FmgrInfo)); \
	fmgr_info(proc,procedureInfo); \
	algInfo->pname = *procedureInfo; \
} while(0)

/*
 * Initializes an empty forecast model for the specified algorithm
 */
ModelInfo
*initModelInfo(const char *algName,const char *modelName)
{
	AlgorithmInfo algInfo;
	ModelInfo *model;
	algInfo = initAlgorithmInfo(algName);

	model = makeNode(ModelInfo);

	model->algInfo 			= algInfo;
	model->modelOid 		= InvalidOid;
	model->aggType 			= InvalidOid;
	model->time 			= InvalidAttrNumber;
	model->measure 			= InvalidAttrNumber;
	//model->dataType		= InvalidOid;
	model->modelName 			= modelName;
	model->parameterList	= NIL;
	model->numForecastValues = 0;
	model->insertCount =0;
	
	//initForecastModel(model, CurrentMemoryContext);

	return model;
}

nlopt_algorithm getOptimMethod(int value) {
	if(value==0)
		return NLOPT_GN_DIRECT_L;
	if(value==1)
		return NLOPT_GN_CRS2_LM;
	if(value==2)
		return NLOPT_GN_MLSL;
	if(value==3)
		return NLOPT_GN_ISRES;
	if(value==4)
		return NLOPT_LN_COBYLA;
	if(value==5)
		return NLOPT_LN_BOBYQA;
	if(value==6)
		return NLOPT_LN_NELDERMEAD;
	if(value==7)
		return NLOPT_LN_SBPLX;
	if(value==8)
		return NLOPT_LD_MMA;
	if(value==9)
		return NLOPT_LD_SLSQP;
	if(value==10)
		return NLOPT_LD_LBFGS;
	else
		elog(ERROR,"Unknown method. Codekey:goM1");
	return 0;
}

TupleTableSlot* BuildModelInfoTupleTableSlot(ModelInfo *modelInfo){

	TupleTableSlot *resultSlot;
	HeapTuple tuple;
	TupleDesc tdesc;
	Datum *values;
	bool *isNull;
	Form_pg_attribute *attrs;
	// open the relation pg_model to lend the TupleDescriptor from it
	Relation rel = heap_open(ModelRelationId, 0);
	// we have to copy the lent TupleDesc, otherwise the relation pg_model won't talk to us later
	tdesc = CreateTupleDescCopy(rel->rd_att);
	/*hsdesc = heap_beginscan(rel, SnapshotNow, 0, NULL);
	tuple = heap_getnext(hsdesc, ForwardScanDirection);
	resultSlot = MakeSingleTupleTableSlot(tdesc);*/

	attrs = palloc(sizeof(FormData_pg_attribute)*7);
	attrs[0] = tdesc->attrs[0]; //name
	attrs[1] = tdesc->attrs[4]; //algorithm
	attrs[2] = tdesc->attrs[2]; //time
	attrs[3] = tdesc->attrs[3]; //measure
	attrs[4] = tdesc->attrs[6]; //aggtype
	attrs[5] = tdesc->attrs[5]; //granularity
	attrs[6] = tdesc->attrs[9]; //timestamp

	tdesc = CreateTupleDesc(7, false, attrs);
	resultSlot = MakeSingleTupleTableSlot(tdesc);

	values = (Datum *)palloc(sizeof(Datum)*7);
	isNull = (bool *)palloc0(sizeof(bool)*7);

	if(modelInfo->modelName){
		values[0] = PointerGetDatum(modelInfo->modelName);
	}else{
		// if a model is not stored, it has no name
		isNull[0] = true;
	}

	values[1] = PointerGetDatum(getModelTypeAsString((ModelType)(modelInfo->forecastMethod)));
	isNull[1] = false;
	values[2] = Int16GetDatum(modelInfo->time->resorigcol);
	isNull[2] = false;
	values[3] = Int16GetDatum(modelInfo->measure->resorigcol);
	isNull[3] = false;
	values[4] = ObjectIdGetDatum(modelInfo->aggType);
	isNull[4] = false;
	values[5] = Int16GetDatum(modelInfo->granularity);
	isNull[5] = false;
	values[6] = Int32GetDatum(modelInfo->timestamp);
	isNull[6] = false;

	tuple = heap_form_tuple(tdesc, values, isNull);

	if(tuple){
		ExecStoreTuple(tuple, resultSlot, InvalidBuffer, false);
	}

//	if(hsdesc){
//		heap_endscan(hsdesc);
//	}

	heap_close(rel, 0);
	return resultSlot;

}

/*
 * Initializes the AlgorithmInfo. Looks up the values in the pg_fcalg SystemCatalog Table
 */
AlgorithmInfo
initAlgorithmInfo(const char *algName)
{
	HeapTuple			pgTuple;
	AlgorithmInfo		algInfo;
	Form_pg_fcalg   fcalgSysData;
	FmgrInfo		*procedureInfo;
	RegProcedure	proc;

	pgTuple = SearchSysCache(FORECASTALGORITHMNAME,PointerGetDatum(algName),0,0,0);

	if(!HeapTupleIsValid(pgTuple)) {

		ereport(ERROR,(errcode(ERRCODE_UNDEFINED_OBJECT),errmsg("algorithm \"%s\" not registered",algName)));
	}

	fcalgSysData = (Form_pg_fcalg) GETSTRUCT(pgTuple);

	// create and fill AlgorithmInfo structure
	algInfo = palloc(sizeof(AlgorithmInfoData));
	algInfo->algName = algName;
	ASSIGN_ALGORITHM_PROCEDURE(algInitModel);
	ASSIGN_ALGORITHM_PROCEDURE(algLoadModelParameters);
	ASSIGN_ALGORITHM_PROCEDURE(algProcessForecast);
	ASSIGN_ALGORITHM_PROCEDURE(algFinalizeForecastModel);
	ASSIGN_ALGORITHM_PROCEDURE(algStoreModelParameters);
	ASSIGN_ALGORITHM_PROCEDURE(algGetNextValue);
	ASSIGN_ALGORITHM_PROCEDURE(algIncrementalUpdate);
	ASSIGN_ALGORITHM_PROCEDURE(algIncrementalUpdate2);
	ASSIGN_ALGORITHM_PROCEDURE(algReestimateUpdate);
	ReleaseSysCache(pgTuple);

	return algInfo;
}

void 
initForecastModel(ModelInfo *modelInfo, MemoryContext memoryContext)
{
	MemoryContext oldContext = NULL;
		
	if (memoryContext != CurrentMemoryContext)
		oldContext = MemoryContextSwitchTo(memoryContext);
		
	FunctionCall1(&(modelInfo->algInfo->algInitModel), PointerGetDatum(modelInfo));
	
	modelInfo->disAggKeyDenominator=0.0;
	modelInfo->disaggkey=1;

	if (oldContext != NULL)
		MemoryContextSwitchTo(oldContext);
}


/*
 * processForecastModel
 *
 * gets called every time an input tuple is processed
 */
void
processForecastModel(ModelInfo *model, Datum value)
{
	// process value internally as double
	double fValue = GetDatumAsDouble(exprType((Node*) model->measure->expr), value);
	
	if((sdf==2||sdf==4)){
		model->disAggKeyDenominator += fValue;
	}
	// call algorithm specific method
	FunctionCall2(&(model->algInfo->algProcessForecast),PointerGetDatum(model->model),Float8GetDatum(fValue));
}

/*
 * finalizeForecastModel
 *
 * no more input tuples to read
 */
void 
finalizeForecastModel(ModelInfo *model)
{
	int length = 0;

	FunctionCall1(&(model->algInfo->algFinalizeForecastModel),PointerGetDatum(model->model));

	length = model->model->trainingTupleCount;

	//MEASURE-CHANGE
//	length=20;

	if (model->upperBound==0) {
		if(((int)(length*0.1))<1)
			model->lowerBound = 1;
		else
			model->lowerBound = (((int)(length*0.1)));
		//XXX: CHANGE FOR MESURING

		if(!model->errorArray){
			if(((int)(length*0.1))<2)
				model->errorArray = palloc0(2*sizeof(double));
			else
				model->errorArray = palloc0(((int)(length*0.1))*sizeof(double));
		}
		else
			if(model->upperBound < ((int)(length*0.1))){
				model->errorArray = repalloc(model->errorArray, ((int)(length*0.1))*sizeof(double));
				model->errorArray[((int)(length*0.1))-1] = 0.0;
			}

		if(((int)(length*0.1))<2)
			model->upperBound = 2;
		else
			model->upperBound = ((int)(length*0.1));
	}
}

/*
 * storeModelParameters
 *
 * stores model parameters in system table
 */
void
storeModelParameters(ModelInfo *model, Oid modelOid)
{
	FunctionCall2(&model->algInfo->algStoreModelParameters,PointerGetDatum(model->model),ObjectIdGetDatum(modelOid));
}


/*
 * loadModelParameters
 *
 * retrieves model parameters from system table
 */
void
loadModelParameters(ModelInfo *model, Oid modelOid)
{
	FunctionCall2(&model->algInfo->algLoadModelParameters,PointerGetDatum(model->model),ObjectIdGetDatum(modelOid));
}


double 
getNextValue(ModelInfo *model, int num) 
{
	return DatumGetFloat8(FunctionCall2(&model->algInfo->algGetNextValue,PointerGetDatum(model->model),Int32GetDatum(num)));
}


void
incrementalUpdate(ModelInfo *modelInfo, double value, int timestamp)
{
	FunctionCall4(&(modelInfo->algInfo)->algIncrementalUpdate,PointerGetDatum(modelInfo->model),Float8GetDatum(value),Int32GetDatum(timestamp),PointerGetDatum(modelInfo));

	if(sdf==2||sdf==4){
		modelInfo->disAggKeyDenominator += value;
	}
	modelInfo->timestamp = timestamp;
}

void
reestimateParameters(ModelInfo *modelInfo, Node *whereExpr)
{
	//increment the commandCounter so ALL tuples (also the new INSERTED) can be accessed
	CommandCounterIncrement();

	if(sdf==2||sdf==4){
		modelInfo->disAggKeyDenominator = 0;
	}
	FunctionCall2(&(modelInfo->algInfo)->algReestimateUpdate,PointerGetDatum(modelInfo), PointerGetDatum(whereExpr));
}


char* getGranularityAsString(Granularity type)
{
	switch (type)
	{
		case day:
			return "day";
			break;
		case week:
			return "week";
			break;
		case month:
			return "month";
			break;
		case year:
			return "year";
			break;
		case quarter:
			return "quarter";
			break;
		default:
			return "undef";
			break;
	}
}

ModelMergeStrategy getMergeStrat(int stratIdentifier)
{
		if(stratIdentifier==-1)stratIdentifier=mmstrat;
		if(stratIdentifier==0)
		{
			return &firstCandidate;
		}
		else if(stratIdentifier==1)
		{
			return &lastCandidate;
		}
		else if(stratIdentifier==2)
		{
			return &averageCand;
		}
		else if(stratIdentifier==3)
		{
			return &sumCand;
		}
		else if(stratIdentifier==4)
		{
			return &sumWithDisaggCand;
		}
		
		
	else // nit implemented yet
		return NULL;
}

double sumCand(List* candidates,int current)
{
	double sum=0;
	ListCell *lc;
	foreach(lc,candidates)
	{
		ModelInfo *mdl=(((ModelInfo*)(lfirst(lc))));
		sum+=getNextValue(mdl, current);
	}

	return sum;
}

double sumWithDisaggCand(List* candidates,int current)
{
	double sum=0;
	ListCell *lc;
	foreach(lc,candidates)
	{
		ModelInfo *mdl=(((ModelInfo*)(lfirst(lc))));
		sum+=(getNextValue(mdl, current)*mdl->disaggkey);
	}

	return sum;
}

double averageCand(List* candidates,int current)
{
	double sum=0;
	ListCell *lc;
	foreach(lc,candidates)
	{
		ModelInfo *mdl=(((ModelInfo*)(lfirst(lc))));
		sum+=getNextValue(mdl, current);
	}
	
	return sum/candidates->length; 
}

double lastCandidate(List* candidates,int current)
{
	ModelInfo * mdl=(((ModelInfo*)(lfirst(list_tail(candidates)))));
	return getNextValue(mdl, current);
}

double firstCandidate(List* candidates,int current)
{
	
	ModelInfo * mdl=(((ModelInfo*)(lfirst(list_head(candidates)))));
	return getNextValue(mdl, current);
}


char* getModelTypeAsString(ModelType type)
{
	switch (type)
	{
		case Medium:
			return "medium";
			break;
		case R:
			return "gnur";
			break;
		case LinReg:
			return "linreg";
			break;
		case GretlArima:
			elog(WARNING,"Gretl is no longer supported, ArModel is used instead");
			return "armodel";
			break;
		case ArModel:
			return "armodel";
			break;
		case HwModel:
			return "hwmodel";
			break;
		default:
			return "undef";
			break;
	}
}


ModelType getStringAsModelType(char* type)
{
	if (type == NULL)
		return Undefined;
	else if (strcmp(type, "medium") == 0)
		return Medium;
	else if (strcmp(type, "gnur") == 0)
		return R;
	else if (strcmp(type, "linreg") == 0)
		return LinReg;
	else if (strcmp(type, "gretl") == 0)
	{
		elog(WARNING,"Gretl is no longer supported, ArModel is used instead");
		return ArModel;
	}
		
	else if (strcmp(type, "armodel") == 0)
		return ArModel;
	else if (strcmp(type, "hwmodel") == 0)
		return HwModel;
	else
		return Undefined;
}


Datum
GetIntAsDatum(Oid source, int value)
{
	switch (source)
		{
			case INT2OID:
				return Int16GetDatum(value);
			case INT4OID:
				return Int32GetDatum(value);
			case INT8OID:
				return Int64GetDatum(value);
			case DATEOID:
				return DateADTGetDatum(value);
			case FLOAT4OID:
				return Float4GetDatum((float) value);
			case FLOAT8OID:
				return Float8GetDatum((double) value);
			default:
				elog(ERROR, "column type not supported");
				return -1;
		}
}


Datum
GetDoubleAsDatum(Oid source, double value)
{
	switch (source)
		{
			case INT2OID:
				return Int16GetDatum((short) value);
			case INT4OID:
				return Int32GetDatum((int) value);
			case INT8OID:
				return Int64GetDatum((int) value);
			case FLOAT4OID:
				return Float4GetDatum((float) value);
			case FLOAT8OID:
				return Float8GetDatum((double) value);
			case NUMERICOID:
				return DirectFunctionCall1(float8_numeric, Float8GetDatum(value));
			default:
				elog(ERROR, "column type not supported");
				return -1;
		}
}

/*
 * THIS FUNCTION IS NOT ADEQUATE FOR INTEGERS OF ANY SIZE !!!111
 */
double 
GetDatumAsDouble(Oid source, Datum value)
{
	switch (source)
	{
		case INT2OID:
			return (double) DatumGetInt16(value);
			break;
		case INT4OID:
			return (double) DatumGetInt32(value);
			break;
		case INT8OID:
			return (double) DatumGetInt64(value);
		case FLOAT4OID:
			return (double) DatumGetFloat4(value);
			break;
		case FLOAT8OID:
			return (double) DatumGetFloat8(value);
			break;
		case NUMERICOID:
			return (double) DatumGetFloat8(DirectFunctionCall1(numeric_float8, value));
			break;
		default:
			elog(ERROR, "column type not supported");
			return -1;
			break;
		}
}

int 
GetDatumAsInt(Oid source, Datum value)
{
	switch (source)
	{
		case INT2OID:
			return (int) DatumGetInt16(value);
			break;
		case INT4OID:
			return (int) DatumGetInt32(value);
			break;
		case INT8OID:
			return (int) DatumGetInt64(value);
		case DATEOID:
			return (int) DatumGetDateADT(value);
			break;
		case FLOAT4OID:
			return (int) DatumGetFloat4(value);
		case FLOAT8OID:
			return (int) DatumGetFloat8(value);
		case NUMERICOID:
			return (int) DatumGetInt32(DirectFunctionCall1(numeric_int4, value));
			break;
		default:
			elog(ERROR, "column type not supported");
			return -1;
			break;
		}
}

double inline SSE(double xhat, double x){
	return pow(x-xhat,2);
}

double inline ABS(double xhat, double x){
	return fabs(x-xhat);
}

double inline SMAPE(double xhat, double x){
	if(xhat == 0 && x == 0){//a perfect forecast for a zerovalue
		return 0.0;
	}
	else
	{
		if(xhat<0.0 && x==0.0)
			xhat=1.0;
		if(xhat<0.0 && x!=0.0)
			xhat=0.0;

		return (fabs(xhat-x)/(xhat+x));
	}
}

void printDebug(const char * filePath, char *message){
	if(printDbg){

		FILE *file = fopen( filePath, (const char*)"a");
		fprintf(file, (const char*)message);
		fclose(file);
	}
}
