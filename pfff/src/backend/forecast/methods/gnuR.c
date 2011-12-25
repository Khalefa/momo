#include "postgres.h"
#include "forecast/methods/gnuR.h"
#include "catalog/pg_parameter.h"
#include "utils/array.h"
#include "utils/lsyscache.h"
#include "catalog/indexing.h"
#include "utils/memutils.h"
#include "parser/parse_func.h"
#include "nodes/execnodes.h"
#include "catalog/pg_type.h"

#define USEMEM(state,amt)	((state)->availMem -= (amt))
#define FREEMEM(state,amt)	((state)->availMem += (amt))


void
initRModel(PG_FUNCTION_ARGS)
{
	ModelInfo *modelInfo = (ModelInfo *)PG_GETARG_POINTER(0);
	
	RModel *model = makeNode(RModel);
	
	// TODO: use function name as parameter
	model->functionName = "etsforecast";
	model->numForecastValues = modelInfo->numForecastValues;

	// init structures for value storage
	model->memtupcount = 0;
	model->memtupsize = 1024;	/* initial guess */
	model->memvalues = (Datum *) palloc(model->memtupsize * sizeof(Datum));
	USEMEM(model, GetMemoryChunkSpace(model->memvalues));
	
	// init model
	model->newvalues = NULL;
	
	modelInfo->model = (Model*) model;
}

void
processRModel(PG_FUNCTION_ARGS)
{
	RModel *model = (RModel *) PG_GETARG_POINTER(0);
	double value = PG_GETARG_FLOAT8(1);

	// store value
	model->memvalues[model->memtupcount] = Float8GetDatum(value);
	model->memtupcount++;

	// grow the array as needed
	if (model->memtupcount >= model->memtupsize - 1)
	{
		// NOTE: no handling if we run out of main memory, but calling ETS in R with high number of values doesn't make sense anyway
		FREEMEM(model, GetMemoryChunkSpace(model->memvalues));
		model->memtupsize *= 2;
		model->memvalues = (Datum *) repalloc(model->memvalues, model->memtupsize * sizeof(Datum));
		USEMEM(model, GetMemoryChunkSpace(model->memvalues));
	}

}

void
finalizeRModel(PG_FUNCTION_ARGS)
{
	int			nelemsp;
	ArrayType	*array;
	ArrayType	*array2;
	List		*funcname;
	Oid			funcargtypes[2];
	Oid			funcoid;
	Datum 		result;

	RModel *model = (RModel *) PG_GETARG_POINTER(0);
	array = construct_array(model->memvalues, model->memtupcount, FLOAT8OID, sizeof(float8), FLOAT8PASSBYVAL, 'i');

	// Get the oid of the etsforecast function
	funcname = list_make1(makeString(model->functionName));
	funcargtypes[0] = FLOAT8ARRAYOID;
	funcargtypes[1] = INT4OID;
	funcoid = LookupFuncName(funcname, 2, funcargtypes, false);

	// Call etsforecast function
	result = OidFunctionCall2(funcoid, PointerGetDatum(array), Int32GetDatum(model->numForecastValues));
	array2 = DatumGetArrayTypeP(result);

	deconstruct_array(array2, FLOAT8OID, sizeof(float8), FLOAT8PASSBYVAL, 'i', &(model->newvalues), NULL, &nelemsp);
}


Datum
getNextRValue(PG_FUNCTION_ARGS)
{
	RModel *model = (RModel *) PG_GETARG_POINTER(0);
	int num = PG_GETARG_INT32(1);
	
	PG_RETURN_FLOAT8(DatumGetFloat8(model->newvalues[num - 1]));
}


void 
storeRModelParameters(PG_FUNCTION_ARGS)
{
	/* should never be called */
	elog(ERROR, "parameter storage for r models not supported");
}

void 
loadRModelParameters(PG_FUNCTION_ARGS)
{
	/* should never be called */
	elog(ERROR, "parameter load for r models not supported");
}

void 
incrementalUpdateRModel(PG_FUNCTION_ARGS)
{
	/* should never be called */
	elog(ERROR, "incremental update for r models not supported");
}
