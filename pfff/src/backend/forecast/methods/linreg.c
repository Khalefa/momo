#include "forecast/methods/linreg.h"
#include "nodes/execnodes.h"
#include "utils/array.h"
#include "catalog/pg_type.h"
#include "catalog/pg_parameter.h"
#include "forecast/modelindex/modelIndex.h"

void
initLinRegModel(PG_FUNCTION_ARGS)
{	
	ModelInfo *modelInfo = (ModelInfo *)PG_GETARG_POINTER(0);
	
	LinRegModel *model = makeNode(LinRegModel);
	model->yTotal = 0;
	model->xyTotal = 0;
	model->count = 0;
	
	modelInfo->model = (Model*) model;
}


void
processLinRegModel(PG_FUNCTION_ARGS)
{
	LinRegModel *model = (LinRegModel*) PG_GETARG_POINTER(0);
	double value = PG_GETARG_FLOAT8(1);

	model->count++;
	model->yTotal += value;
	model->xyTotal += model->count * value;
}


void
finalizeLinRegModel(PG_FUNCTION_ARGS)
{
	LinRegModel *model = (LinRegModel*) PG_GETARG_POINTER(0);

	double xTotal = (model->count*(model->count+1))/2;
	double xSquaredTotal = (model->count*(model->count+1)*((2*model->count)+1))/6;
	model->b = ((model->count * model->xyTotal) - (xTotal * model->yTotal))/((model->count * xSquaredTotal) - (xTotal * xTotal));
	model->a = (model->yTotal - (model->b * xTotal)) / model->count;
}


Datum
getNextLinRegValue(PG_FUNCTION_ARGS)
{
	LinRegModel *model = (LinRegModel*) PG_GETARG_POINTER(0);
	int num = PG_GETARG_INT32(1);

	PG_RETURN_FLOAT8(model->a + (model->b * (model->count + num)));
}


void 
incrementalUpdateLinRegModel(PG_FUNCTION_ARGS)
{
	LinRegModel *model = (LinRegModel*) PG_GETARG_POINTER(0);
	double value = PG_GETARG_FLOAT8(1);
	
	DirectFunctionCall2(processLinRegModel, PointerGetDatum(model), Float8GetDatum(value));
	DirectFunctionCall1(finalizeLinRegModel, PointerGetDatum(model));
}


void 
loadLinRegModelParameters(PG_FUNCTION_ARGS)
{
	Datum		*entries = NULL;
	int			nelemsp;
	
	LinRegModel *model = (LinRegModel*) PG_GETARG_POINTER(0);
	Oid modelOid = PG_GETARG_OID(1);

	ArrayType **parameters = (ArrayType **) palloc(sizeof(ArrayType));
	RetrieveParameters(modelOid, parameters, 1);

	deconstruct_array(parameters[0], FLOAT8OID, sizeof(float8), FLOAT8PASSBYVAL, 'i', &entries, NULL, &nelemsp);
	model->yTotal = DatumGetFloat8(entries[0]);
	model->xyTotal = DatumGetFloat8(entries[1]);
	model->count = DatumGetFloat8(entries[2]);
	
	DirectFunctionCall1(finalizeLinRegModel, PointerGetDatum(model));
}


void
storeLinRegModelParameters(PG_FUNCTION_ARGS)
{
	LinRegModel *model = (LinRegModel*) PG_GETARG_POINTER(0);
	Oid modelOid = PG_GETARG_OID(1);
	
	ArrayType *arry;
	Datum *numdatums = (Datum *) palloc(3 * sizeof(Datum));
	numdatums[0] = Float8GetDatum(model->yTotal);
	numdatums[1] = Float8GetDatum(model->xyTotal);
	numdatums[2] = Float8GetDatum(model->count);
	arry = construct_array(numdatums, 3, FLOAT8OID, sizeof(float8), FLOAT8PASSBYVAL, 'i');

	InsertOrReplaceParameters(modelOid, 1, arry);
}
