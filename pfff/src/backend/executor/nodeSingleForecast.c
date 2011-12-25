/*-------------------------------------------------------------------------
 *
 * nodeSingleForecast.c
 *	  Routines to handle forecasting.
 *
 * IDENTIFICATION
 *	  $PostgreSQL: pgsql/src/backend/executor/nodeForecast.c,v 1.0 2009/08/26$
 *
 *-------------------------------------------------------------------------
 */

#include "postgres.h"
#include "funcapi.h"
#include "utils/date.h"
#include "executor/execdebug.h"
#include "executor/nodeSingleForecast.h"
#include "utils/numeric.h"
#include "utils/builtins.h"
#include "utils/guc.h"
#include "catalog/pg_model.h"
#include "catalog/pg_type.h"
#include "nodes/nodeFuncs.h"
#include "forecast/algorithm.h"
#include "forecast/modelindex/modelIndex.h"
#include "forecast/methods/forecastUtilitys.h"
#include "forecast/methods/ExpSmooth.h"


/* ----------------------------------------------------------------
 *		ExecForecast
 * ----------------------------------------------------------------
 */
TupleTableSlot *
ExecSingleForecast(SingleForecastState *forecastState)
{
	PlanState 		*outerPlan;
	TupleTableSlot 	*outSlot = NULL;
	Datum 			*values;
	bool 			*isnull;
	HeapTuple 		tuple;
	TupleDesc 		outerTupDesc;
	int				i;
	double 			value;
	ModelInfo 		*modelInfo;
	AttrNumber 		attrNumber;
	
	// we needn't do all the execution, if we just created a model
	if(forecastState->end == -2){

		forecastState->end = -100;
		return forecastState->ss.ps.ps_ResultTupleSlot;
	}

	//for testing purpose only, should be set through grammar

	forecastState->modelInfo=(((ModelInfo*)(lfirst(list_head(forecastState->candidateModels)))));
		modelInfo=forecastState->modelInfo;

	attrNumber = modelInfo->measure->resno - 1;

	// get information from the node
	outerPlan 		= outerPlanState(forecastState);
		
	if(forecastState->current <= forecastState->end)
	{

		outerTupDesc = forecastState->ss.ps.ps_ResultTupleSlot->tts_tupleDescriptor;

		// init output tuple
		values = (Datum *) palloc0(outerTupDesc->natts * sizeof(Datum));
		isnull = (bool *) palloc0(outerTupDesc->natts * sizeof(bool));
		for (i=0; i<outerTupDesc->natts; i++) {
			isnull[i] = true;
		}
		
		// set time column
		values[modelInfo->time->resno-1] = GetIntAsDatum(exprType((Node*) modelInfo->time->expr), forecastState->timestamp + forecastState->current);
		isnull[modelInfo->time->resno-1] = false;
		
		// set measure column
		//set strathook here!
		value = forecastState->modelMergeStrategy(forecastState->candidateModels, forecastState->current);
		values[modelInfo->measure->resno-1] = GetDoubleAsDatum(exprType((Node*)modelInfo->measure->expr), value);
		isnull[modelInfo->measure->resno-1] = false;
		
		// create output
		tuple = heap_form_tuple(outerTupDesc, values, isnull);
		outSlot = forecastState->ss.ps.ps_ResultTupleSlot;
		ExecStoreTuple(tuple,outSlot,InvalidBuffer,true);

		forecastState->current++;
	}
	return outSlot;
}



/* ----------------------------------------------------------------
 *		ExecInitForecast
 *
 *		Creates the run-time state information for the forecast node
 *		produced by the planner and the node's subplan.
 * ----------------------------------------------------------------
 */
SingleForecastState *
ExecInitSingleForecast(SingleForecast *node, EState *estate, int eflags)
{
	SingleForecastState 	*forecastState;
	Plan	   				*outerPlan;
	TupleTableSlot			*resultSlot;

	/*
	 * create state structure
	 */
	forecastState = makeNode(SingleForecastState);
	forecastState->ss.ps.plan = (Plan *) node;
	forecastState->ss.ps.state = estate;
	forecastState->start = node->start;
	forecastState->end = node->end;
	forecastState->current = node->start;
	forecastState->scanDone = false;
	forecastState->count = 0;
	forecastState->modelMergeStrategy = getMergeStrat(node->choose);
	//for now take the first model found
	//copy all models
	forecastState->candidateModels=node->candidatemodelInfos;
	forecastState->sourcetext = node->sourcetext;

	if(((ModelInfo*)(lfirst(list_head(node->candidatemodelInfos))))->buildModel) {

		outerPlan = outerPlan(node);
		outerPlanState(forecastState) = ExecInitNode(outerPlan, estate, eflags);
	}

	//first create the model if we have to
	if ((((ModelInfo*)(lfirst(list_head(node->candidatemodelInfos))))->buildModel) && (forecastState->start == forecastState->current) && eflags != 1)
		resultSlot = ExecProcNode(outerPlanState(forecastState));
	else
		resultSlot=NULL;
		
	forecastState->timestamp = ((ModelInfo*)(lfirst(list_head(node->candidatemodelInfos))))->timestamp;
	
	//Despite we checked this already in the planer, we still need this check here if the model creation is implicit. In this case there is only one Model in the candidatelist!
	if((node->end == -1) && (strlen(node->targetDateString) > 0)){
		Datum targetDate = DirectFunctionCall1(date_in, CStringGetDatum(node->targetDateString));
	
		if(DatumGetBool(DirectFunctionCall2(date_le, targetDate, forecastState->timestamp))) //targetDate is not prospective enough
		{
			ereport(ERROR, (errcode(ERRCODE_INVALID_PARAMETER_VALUE), errmsg("targetDate must be greater than last value from query")));
		}
	
		forecastState->end = DirectFunctionCall2(date_mi, targetDate, forecastState->timestamp);
	}
				
	/*
	 * tuple table initialization
	 */
	#define FORECAST_NSLOTS 1
	ExecInitResultTupleSlot(estate, &forecastState->ss.ps);
	
	/*
	 * forecast nodes do no projections, so initialize projection info for this
	 * node appropriately
	 */
	ExecAssignResultTypeFromTL(&forecastState->ss.ps);
	// if we just created a model we only return a part of the modelInfo
	if(node->end == -2 && eflags!=1)
	{
		forecastState->ss.ps.ps_ResultTupleSlot = resultSlot;
	}

	forecastState->ss.ps.ps_ProjInfo = NULL;

	return forecastState;
}


int
ExecCountSlotsSingleForecast(SingleForecast *node)
{
	return ExecCountSlotsNode(outerPlan(node)) +
		ExecCountSlotsNode(innerPlan(node)) +
		FORECAST_NSLOTS;
}


/* ----------------------------------------------------------------
 *		ExecEndForecast(node)
 * ----------------------------------------------------------------
 */
void
ExecEndSingleForecast(SingleForecastState *node)
{
	ListCell *lc;
	/*
	 * clean out the tuple table
	 */
	if(node->ss.ps.ps_ResultTupleSlot)
		ExecClearTuple(node->ss.ps.ps_ResultTupleSlot);
	
	/*
	 *	free ModelNodes 
	 */
	
	foreach(lc,node->candidateModels)
	{
		pfree(lfirst(lc));
	}
	
	/*
	 * shut down the subplan
	 */
	ExecEndNode(outerPlanState(node));
}
