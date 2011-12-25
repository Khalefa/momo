/*-------------------------------------------------------------------------
 *
 * nodeDecompose.c
 *	  Routines to handle decomposition of timeseries into trend, season and rest.
 *		Handles only standard decomposition of a additive model. 

 * IDENTIFICATION
 *	  $PostgreSQL: pgsql/src/backend/executor/nodeForecast.c,v 1.0 2009/08/26$
 *
 *-------------------------------------------------------------------------
 */

#include "postgres.h"
#include "funcapi.h"

#include "executor/execdebug.h"
#include "executor/nodeDecompose.h"
#include "utils/numeric.h"
#include "utils/builtins.h"
#include "nodes/nodeFuncs.h"
#include "catalog/pg_type.h"

#include "forecast/methods/additiveDec.h"


DecomposeState 
*ExecInitDecompose(Decompose *node, EState *estate, int eflags)
{
	DecomposeState 	*decomposeState;
	Plan	   		*outerPlan;
	ModelInfo		*model;

	elog(DEBUG5, "ExecInitDecompose: initializing decompose node");

	/*
	 * create state structure
	 */
	decomposeState = makeNode(DecomposeState);
	decomposeState->ss.ps.plan = (Plan *) node;
	decomposeState->ss.ps.state = estate;
	decomposeState->scanDone = false;
	
	/* init model */
	model = (ModelInfo*) node->model;
	InitAdditiveDec(model, node->window, node->season);
	decomposeState->model = (Node*) model;
	
	/*
	 * tuple table initialization
	 */
	#define DECOMPOSE_NSLOTS 1
	ExecInitResultTupleSlot(estate, &decomposeState->ss.ps);

	/*
	 * Initialize outer plan
	 */
	outerPlan = outerPlan(node);
	outerPlanState(decomposeState) = ExecInitNode(outerPlan, estate, eflags);

	/*
	* Initialize result tuple type
	*/
	ExecAssignResultTypeFromTL(&decomposeState->ss.ps);
	decomposeState->ss.ps.ps_ProjInfo = NULL;

	elog(DEBUG5, "ExecInitDecompose: decompose node initialized");

	return decomposeState;
}


/* ----------------------------------------------------------------
 *		ExecDecompose
 * ----------------------------------------------------------------
 */
TupleTableSlot *
ExecDecompose(DecomposeState *decomposeState)
{
	PlanState 		*outerPlan;
	TupleTableSlot 	*outSlot = NULL;
	ModelInfo		*model;
	bool 			isnull;

	outerPlan = outerPlanState(decomposeState);
	model = (ModelInfo *) decomposeState->model;

	// Get next tuples
	while (!decomposeState->scanDone)
	{
		// get next tuple from the plan node underneath
		outSlot = ExecProcNode(outerPlan);

		// tuple is null, we are done with the scan and can compute the decompose result
		if (TupIsNull(outSlot))
		{
			decomposeState->scanDone = true;
			FinalizeAdditiveDec(model);
			decomposeState->position = 0;
		} else
		{
			Datum datum = slot_getattr(outSlot, model->measure->resno, &isnull);
			ProcessAdditiveDec(model, GetDatumAsDouble(exprType((Node*) model->measure->expr), datum));
		}
	}


	if ((decomposeState->scanDone) && (decomposeState->position < ((AdditiveDec*) model->model)->tupcount))
	{
		// We are done with the scan, create decomposition tuples
		Datum *values;
		bool *isnull;
		HeapTuple tuple;
		TupleDesc tupDesc;
		double v1, v2, v3;

		outSlot = decomposeState->ss.ps.ps_ResultTupleSlot;
		tupDesc = outSlot->tts_tupleDescriptor;
		
		values = (Datum *) palloc0(tupDesc->natts * sizeof(Datum));
		isnull = (bool *) palloc0(tupDesc->natts * sizeof(bool));

		// trend
		v1 = GetTrendValue(model, decomposeState->position);
		values[0] = GetDoubleAsDatum(exprType((Node*) model->measure->expr), v1);
		isnull[0] = false;
		// season	
		v2 = GetSeasonValue(model, decomposeState->position);
		values[1] = GetDoubleAsDatum(exprType((Node*) model->measure->expr), v2);
		isnull[1] = false;
		// rest		
		v3 = GetRestValue(model, decomposeState->position);
		values[2] = GetDoubleAsDatum(exprType((Node*) model->measure->expr), v3);
		isnull[2] = false;

		tuple = heap_form_tuple(tupDesc, values, isnull);

		ExecStoreTuple(tuple,outSlot,InvalidBuffer,true);

		decomposeState->position++;
	}

	return outSlot;
}


int
ExecCountSlotsDecompose(Decompose *node)
{
	return ExecCountSlotsNode(outerPlan(node)) +
		ExecCountSlotsNode(innerPlan(node)) +
		DECOMPOSE_NSLOTS;
}


/* ----------------------------------------------------------------
 *		ExecEndForecast(node)
 * ----------------------------------------------------------------
 */
void
ExecEndDecompose(DecomposeState *node)
{
	elog(DEBUG5, "ExecEndDecompose: shutting down decompose node");

	/*
	 * clean out the tuple table
	 */
	ExecClearTuple(node->ss.ps.ps_ResultTupleSlot);

	/*
	 * shut down the subplan
	 */
	ExecEndNode(outerPlanState(node));
	
	/*
	 * clear temporal storage for additive model
	 */
	ClearAdditiveDec((ModelInfo*) node->model);

	elog(DEBUG5, "ExecEndDecompose: decompose node shutdown");
}