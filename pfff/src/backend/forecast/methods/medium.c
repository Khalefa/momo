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
#include "forecast/methods/medium.h"
#include "utils/array.h"
#include "catalog/pg_type.h"
#include "catalog/pg_parameter.h"


void
initMediumForecastModel(PG_FUNCTION_ARGS)
{
	ModelInfo *modelInfo = (ModelInfo *)PG_GETARG_POINTER(0);
	
	MediumModel *medModel = makeNode(MediumModel);
	medModel->sum = 0;
	medModel->count = 0;

	modelInfo->model = (Model*) medModel;
}

void
loadMediumModelParameters(PG_FUNCTION_ARGS)
{
	Datum		*entries = NULL;
	int			nelemsp;
	
	MediumModel *model = (MediumModel *) PG_GETARG_POINTER(0);
	Oid modelOid = PG_GETARG_OID(1);

	ArrayType **parameters = (ArrayType **) palloc(sizeof(ArrayType));
	RetrieveParameters(modelOid, parameters, 1);

	deconstruct_array(parameters[0], FLOAT8OID, sizeof(FLOAT8OID), FLOAT8PASSBYVAL, 'i', &entries, NULL, &nelemsp);
	model->sum = DatumGetFloat8(entries[0]);
	model->count = DatumGetFloat8(entries[1]);
}

void
processMediumForecastModel(PG_FUNCTION_ARGS)
{

	MediumModel *medModel = (MediumModel *) PG_GETARG_POINTER(0);
	double val = PG_GETARG_FLOAT8(1);
	medModel->sum = medModel->sum + val;
	medModel->count++;
}

void
finalizeMediumForecastModel(PG_FUNCTION_ARGS)
{
		/* do nothing */
}

void
storeMediumModelParameters(PG_FUNCTION_ARGS)
{
	MediumModel *model = (MediumModel*) PG_GETARG_POINTER(0);
	Oid modelOid = PG_GETARG_OID(1);

	ArrayType *arry;
	Datum *numdatums = (Datum *) palloc(2 * sizeof(Datum));
	numdatums[0] = Float8GetDatum(model->sum);
	numdatums[1] = Float8GetDatum(model->count);
	arry = construct_array(numdatums, 2, FLOAT8OID, sizeof(float8), FLOAT8PASSBYVAL, 'i');
	
	InsertOrReplaceParameters(modelOid, 1, arry);
}

Datum
getNextMediumValue(PG_FUNCTION_ARGS)
{
	MediumModel *medModel = (MediumModel *) PG_GETARG_POINTER(0);
	
	PG_RETURN_FLOAT8(medModel->sum/medModel->count);
}

void 
incrementalUpdateMediumModel(PG_FUNCTION_ARGS)
{
	MediumModel *model = (MediumModel *) PG_GETARG_POINTER(0);
	double value = PG_GETARG_FLOAT8(1);
	
	DirectFunctionCall2(processMediumForecastModel, PointerGetDatum(model), Float8GetDatum(value));
}
