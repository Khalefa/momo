/*
 * nodeDisAgg.c
 * 		Routines to handle disaggregation.
 *
 * IDENTIFICATION
 *	  $PostgreSQL: pgsql/src/backend/executor/nodeDisAgg.c,v 1.0 2010/12/31$
 */

#include "postgres.h"
#include "executor/nodeDisAgg.h"
#include "catalog/pg_type.h"
#include "parser/parsetree.h"
#include "executor/executor.h"


// determines which strategy should be used for disaggregation
DisAggStrategy getDisAggStrat(int startIdentifier){
	switch(startIdentifier){
		case 1:{
			return &DisAggStratMult;
		}
		case 2:{
			return &DisAggStratDiv;
		}
		case 3:{
			return &DisAggStratAdd;
		}
		case 4:{
			return &DisAggStratSub;
		}
		default:{
			elog(INFO, "Incorrect DisAggregation-Strategy chosen, identity returned!");
			return &DisAggStratIdentity;
		}
	}
}

// multiplies the column^th date of values with key and returns everything as the resulttuple
HeapTuple DisAggStratMult(Datum *values, bool *isnull, TupleDesc outTdesc, int column, A_Const *key){
	HeapTuple		tuple;
	double 			newValue;

	// check the type of the column we want to manipulate, so we can manipulate it properly
	Oid typOid = outTdesc->attrs[column]->atttypid;

	// do the calculation
	if(key->val.type == T_Integer){
		newValue = GetDatumAsInt(typOid, values[column]) * key->val.val.ival;
	}else if(key->val.type == T_Float){
		newValue = GetDatumAsDouble(typOid, values[column]) * atof(key->val.val.str);
	}else{
		ereport(ERROR, (errcode(ERRCODE_DATATYPE_MISMATCH), errmsg("keytype not supported")));
	}
	values[column] = GetDoubleAsDatum(typOid, newValue);

	tuple = heap_form_tuple(outTdesc, values, isnull);

	return tuple;
}

// divides the column^th date of values by key and returns everything as the resulttuple
HeapTuple DisAggStratDiv(Datum *values, bool *isnull, TupleDesc outTdesc, int column, A_Const *key){
	HeapTuple		tuple;
	double 			newValue;
	Oid typOid;
	if(key->val.val.ival == 0)
		ereport(ERROR, (errcode(ERRCODE_DIVISION_BY_ZERO), errmsg("Keys mustn't be 0!")));

	// check the type of the column we want to manipulate, so we can manipulate it properly
	typOid = outTdesc->attrs[column]->atttypid;

	// do the calculation
	if(key->val.type == T_Integer){
		newValue = GetDatumAsDouble(typOid, values[column]) / key->val.val.ival;
	}else if(key->val.type == T_Float){
		newValue = GetDatumAsDouble(typOid, values[column]) / atof(key->val.val.str);
	}else{
		ereport(ERROR, (errcode(ERRCODE_DATATYPE_MISMATCH), errmsg("Keytype not supported!")));
	}
	values[column] = GetDoubleAsDatum(typOid, newValue);

	tuple = heap_form_tuple(outTdesc, values, isnull);

	return tuple;
}

// adds key to the column^th date of values and returns everything as the resulttuple
HeapTuple DisAggStratAdd(Datum *values, bool *isnull, TupleDesc outTdesc, int column, A_Const *key){
	HeapTuple		tuple;
	double 			newValue;

	// check the type of the column we want to manipulate, so we can manipulate it properly
	Oid typOid = outTdesc->attrs[column]->atttypid;

	// do the calculation
	if(key->val.type == T_Integer){
		newValue = GetDatumAsDouble(typOid, values[column]) + key->val.val.ival;
	}else if(key->val.type == T_Float){
		newValue = GetDatumAsDouble(typOid, values[column]) + atof(key->val.val.str);
	}else{
		ereport(ERROR, (errcode(ERRCODE_DATATYPE_MISMATCH), errmsg("keytype not supported")));
	}
	values[column] = GetDoubleAsDatum(typOid, newValue);

	tuple = heap_form_tuple(outTdesc, values, isnull);

	return tuple;
}

// subtracts key from the column^th date of values and returns everything as the resulttuple
HeapTuple DisAggStratSub(Datum *values, bool *isnull, TupleDesc outTdesc, int column, A_Const *key){
	HeapTuple		tuple;
	double 			newValue;

	// check the type of the column we want to manipulate, so we can manipulate it properly
	Oid typOid = outTdesc->attrs[column]->atttypid;

	// do the calculation
	if(key->val.type == T_Integer){
		newValue = GetDatumAsDouble(typOid, values[column]) - key->val.val.ival;
	}else if(key->val.type == T_Float){
		newValue = GetDatumAsDouble(typOid, values[column]) - atof(key->val.val.str);
	}else{
		ereport(ERROR, (errcode(ERRCODE_DATATYPE_MISMATCH), errmsg("keytype not supported")));
	}
	values[column] = GetDoubleAsDatum(typOid, newValue);

	tuple = heap_form_tuple(outTdesc, values, isnull);

	return tuple;
}

// just return the inputData
HeapTuple DisAggStratIdentity(Datum *values, bool *isnull, TupleDesc outTdesc, int column, A_Const *key){
	return heap_form_tuple(outTdesc, values, isnull);;
}

TupleTableSlot *
ExecDisAgg(DisAggState *disAggState){

	PlanState 		*outerPlan;
	HeapTuple		tuple;
	TupleTableSlot 	*tempRes = NULL;
	TupleTableSlot 	*outSlot = NULL;
	Datum 			*values;
	bool 			*isNull, *tempBool;
	int 			column, attCount, i;
	Oid 			attributeType;
	OpExpr			*opex;
	Const 			*ac;

	// get information from the lefttree
	outerPlan = outerPlanState(disAggState);

	// when there are no cached tuples OR we have given out all cached tuples calculate the next
	if(!disAggState->resultTuples || disAggState->resultTuples->length <= disAggState->count){
		tempRes = ExecProcNode(outerPlan);
		// in case tempRes contains data, we should disaggregate them
		if(!TupIsNull(tempRes)){
			outSlot = disAggState->ss.ps.ps_ResultTupleSlot;
			column = ((TargetEntry *)disAggState->targetCol)->resno; //-1, resno is counted from 1

			values = (Datum *)palloc0(sizeof(Datum)*outSlot->tts_tupleDescriptor->natts);
			isNull = (bool *)palloc0(sizeof(bool)*outSlot->tts_tupleDescriptor->natts);

			// if we just have one disaggregation-attribute we don't want a new first column containing it(KEEP THIS CONSISTENT WITH THE OPTIMZER!!!)
			// so check this and build everything if there are multiple attributes
			if(disAggState->attributes->length > 1){
				attributeType = outSlot->tts_tupleDescriptor->attrs[0]->atttypid;

				// for every attribute build a new tuple from tempRes
				for (attCount=0; attCount<disAggState->attributes->length; ++attCount) {
//TODO: check if there are tuple which really meet the attribute, elsewise don't enter the attribute(best would be to do this earlier than here ;-)e.g. the optimizer...
					// fill the value for the attribute-column
					opex = (OpExpr *)list_nth(disAggState->attributes, attCount);
					ac = (Const *)list_nth(opex->args, 1);
					values[0] = ac->constvalue;
					isNull[0] = false;

					// copy the other cols from the subnode
					tempBool = palloc(sizeof(bool));
					for(i = tempRes->tts_tupleDescriptor->natts; i>0; i--){
						values[i] = slot_getattr(tempRes, i, tempBool);
						isNull[i] = *tempBool;
					}

					// perform the disAgg-Strategy to get the disaggregated tuple
					tuple = disAggState->strategy(values, isNull, outSlot->tts_tupleDescriptor, column, (A_Const *)list_nth(disAggState->keys, attCount));
					disAggState->resultTuples = lappend(disAggState->resultTuples, tuple);
				}
			}else{ // for just one attribute it's nearly the same like for multiple except filling the attribute to the first col
				tempBool = palloc(sizeof(bool));
				for(i = tempRes->tts_tupleDescriptor->natts; i>0; i--){
					values[i-1] = slot_getattr(tempRes, i, tempBool);
					isNull[i-1] = *tempBool;
				}

				// perform the disAgg-Strategy to get the disaggregated tuple
				tuple = disAggState->strategy(values, isNull, outSlot->tts_tupleDescriptor, column-1, (A_Const *)list_nth(disAggState->keys, 0));
				disAggState->resultTuples = lappend(disAggState->resultTuples, tuple);
			}
		}
	}

	// if not all cached tuples are pushed out, push out the next one
	if(disAggState->resultTuples->length > disAggState->count){
		outSlot = disAggState->ss.ps.ps_ResultTupleSlot;
		ExecStoreTuple((HeapTuple)list_nth(disAggState->resultTuples, disAggState->count++), outSlot, InvalidBuffer, false);
	}

	return outSlot;
}

DisAggState *
ExecInitDisAgg(DisAgg *node, EState *estate, int eflags){

	DisAggState 		*disAggState;

	//create state structure
	disAggState = makeNode(DisAggState);
	disAggState->ss.ps.plan = (Plan *) node;
	disAggState->ss.ps.state = estate;
	disAggState->targetCol = node->targetCol;
	disAggState->attributes = node->attributes;
	disAggState->keys = node->keys;
	disAggState->resultTuples = NIL;
	disAggState->count = 0;
	disAggState->strategy = getDisAggStrat(node->strategy);
	disAggState->sourcetext = node->sourcetext;

	// put the data to disAgg to the lefttree
	outerPlanState(disAggState) = ExecInitNode(outerPlan(node), estate, eflags);
	// tuple table initialization
	ExecInitResultTupleSlot(estate, &disAggState->ss.ps);
	ExecInitScanTupleSlot(estate, &disAggState->ss);

	/*
	 * disaggregation nodes do no projections, so initialize projection info for this
	 * node appropriately
	 */

	ExecAssignResultTypeFromTL(&disAggState->ss.ps);
	ExecAssignScanTypeFromOuterPlan(&disAggState->ss);

	disAggState->ss.ps.ps_ProjInfo = NULL;

	return disAggState;
}

int
ExecCountSlotsDisAgg(DisAgg *node){
	return ExecCountSlotsNode(outerPlan(node)) +
			ExecCountSlotsNode(innerPlan(node)) + ExecCountSlotsNode(outerPlan(node))*node->attributes->length;
}

void
ExecEndDisAgg(DisAggState *node){

	/*
	 * clean out the tuple table
	 */
	ExecClearTuple(node->ss.ps.ps_ResultTupleSlot);

	/*
	 * shut down the subplan
	 */
	ExecEndNode(outerPlanState(node));
}
