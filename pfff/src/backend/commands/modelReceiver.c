/*-------------------------------------------------------------------------
 *
 * modelReceiver.c
 *	  An implementation of DestReceiver that recieves tuples and incrementally
 *	  builds and stores a forecasting model
 *
 *
 * Portions Copyright (c) 2009, TU Dresden, Faculty of Computer Science
 *
 * IDENTIFICATION
 *	  $PostgreSQL: pgsql/src/backend/commands/modelReceiver.c,v 1.0 2009/11/25 14:48:57 jm
 *
 *-------------------------------------------------------------------------
 */
#include "postgres.h"
#include "commands/modelReceiver.h"
#include "catalog/pg_model.h"
#include "forecast/algorithm.h"
#include "forecast/algorithm.h"
#include "nodes/nodeFuncs.h"

typedef struct ModelState
{

	DestReceiver 		pub;
	ModelInfo 			*info;
	const char 			*modelName;
	Node 				*whereExpression;
	const char 			*statement;
	const char			*algorithm;
	int					lastTimestamp;
}ModelState;

static void
modelReceiveSlot(TupleTableSlot *slot,DestReceiver *self)
{
	ModelState *state;
	Datum datum;
	bool isnull;

	state = (ModelState *)self;
	datum = slot_getattr(slot,state->info->measure->resno,&isnull);
	state->lastTimestamp = GetDatumAsInt(exprType((Node*) state->info->time->expr), slot_getattr(slot, state->info->time->resno, &isnull));

	//ProcessForecastModel(state->info, datum);
	processForecastModel(state->info,datum);
}

static void
modelStartupReceiver(DestReceiver *self, int operation, TupleDesc typeinfo)
{
	//ModelState *state = (ModelState *)self;

	//InitForecastModel(state->info);
}

static void
modelShutdownReceiver(DestReceiver *self)
{
	ModelState *state = (ModelState *)self;

	// finalize Forecast Model
	finalizeForecastModel(state->info);
	/*switch (state->info->modelType)
		{
			case Medium:
				FinalizeMediumModel(state->info);
				break;
			case Ets:
				FinalizeEtsModel(state->info);
				break;
			case LinReg:
				FinalizeLinRegModel(state->info);
				break;
			default:
				elog(ERROR, "model type not recognized");
				break;
		}*/

	// store Forecast Model
	//if(state->info->storeModel)
	//{
	state->info->modelOid = InsertForecastModel(state->modelName,
												state->info->time,
												state->info->measure,
												(char*)state->algorithm,
												day,
												2114,
												state->whereExpression,
												false,
												state->statement,
												true,
												state->lastTimestamp,
												-1.0);
	//StoreModelParameters(state->info, state->info->modelOid);
	storeModelParameters(state->info, state->info->modelOid);
	//}
}

static void
modelDestroyReceiver(DestReceiver *self)
{
	pfree(self);
}

/*
 * Initially create a ModelDestReceiver
 */

DestReceiver *
CreateModelDestReceiver(const char *modelName,
						TargetEntry *outputColumn,
						List *timeColumns,
						Node *whereExpression,
						const char *statement,
						const char *algorithm,
						List *algorithmParameter)
{
	ModelInfo *model;
	ModelState *self = (ModelState *) palloc0(sizeof(ModelState));

	//self->info = (ModelInfo *)makeNode(ModelInfo);
	model = initModelInfo((const char*)algorithm,(const char*)modelName);
	model->parameterList = algorithmParameter;
	self->info = model;

	/*if (algorithm != NULL) {
		if (memcmp(algorithm, "medium", 6) == 0)
			self->info->modelType = 1;
		else if (memcmp(algorithm, "ets", 3) == 0)
			self->info->modelType = 2;
		else if (memcmp(algorithm, "linreg", 6) == 0)
			self->info->modelType = 3;
		else
			ereport(ERROR, (errcode(ERRCODE_INVALID_PARAMETER_VALUE), errmsg("algorithm not know")));
		} else
			self->info->modelType = 0;*/

	self->info->measure = outputColumn;
	self->modelName = modelName;
	self->info->time = (TargetEntry *)lfirst(list_head(timeColumns));
	self->whereExpression = whereExpression;
	self->statement = statement;
	self->algorithm = algorithm;

	initForecastModel(model,CurrentMemoryContext);

	self->pub.rStartup = modelStartupReceiver;
	self->pub.receiveSlot = modelReceiveSlot;
	self->pub.rShutdown = modelShutdownReceiver;
	self->pub.rDestroy = modelDestroyReceiver;

	return (DestReceiver *)self;
}
