/*-------------------------------------------------------------------------
 *
 * nodeCreateForecastModel.c
 *	  Routines to handle the creation of a forecast Model.
 *

 * IDENTIFICATION
 *	  $PostgreSQL: pgsql/src/backend/executor/nodeCreateModel.c,v 1.0 2010/08/12$
 *
 *-------------------------------------------------------------------------
 */

#include "postgres.h"
#include "forecast/modelindex/modelIndex.h"
#include "nodes/nodeFuncs.h"
#include "catalog/pg_model.h"
#include "executor/nodeCreateForecastModel.h"
#include "executor/executor.h"
#include "ctype.h"
#include "forecast/modelGraph/modelGraph.h"

CreateForecastModelState *ExecInitCreateForecastModel(CreateForecastModel *node, EState *estate, int eflags)
{
	CreateForecastModelState 	*createState;
	Plan						*outerPlan;
	Plan						*trainingPlan;

	createState = makeNode(CreateForecastModelState);
	createState->ss.ps.plan = (Plan *) node;
	createState->ss.ps.state = estate;

	createState->sourcetext = node->sourcetext;
	createState->count = 0;
	createState->modelInfo = node->modelInfo;

	//determine if the model is created explicit or implicit
	if(node->end==-2)
		createState->implicit=false;
	else
		createState->implicit=true;
	
	//forward the algorithmparameter if there are any
	if(((AlgorithmClause *)node->algorithmStmt) && ((AlgorithmClause *)node->algorithmStmt)->algorithmparameter)
		createState->modelInfo->parameterList = (((AlgorithmClause *)node->algorithmStmt)->algorithmparameter);
	
	if ((createState->modelInfo->storeModel == 0) || (createState->modelInfo->storeModel == 10)||(createState->modelInfo->storeModel == 2))
		initForecastModel(createState->modelInfo, getModelMemoryContext());
	else if((createState->modelInfo->storeModel == 4) || (createState->modelInfo->storeModel == 14))
		initForecastModel(createState->modelInfo, getModelGraphContext());
	else
		initForecastModel(createState->modelInfo, CurrentMemoryContext);

	//put query to forecast to the left
	outerPlan = outerPlan(node);
	trainingPlan = innerPlan(node);

	//put trainingdata to the right
	if(node->algorithmStmt != NULL && ((AlgorithmClause *)node->algorithmStmt)->trainingdata){
		innerPlanState(createState) = ExecInitNode(trainingPlan, estate, eflags);
	}
	else
	{
		int nSlots = estate->es_tupleTable->size;
		/* Slots for the main plan tree */
		nSlots += ExecCountSlotsNode(outerPlan);
		/* Add slots for subplans and initplans */
		ExecDropTupleTable(estate->es_tupleTable, true);
		estate->es_tupleTable = ExecCreateTupleTable(nSlots);
		innerPlanState(createState) = ExecInitNode(outerPlan, estate, eflags);
	}
	outerPlanState(createState) = ExecInitNode(outerPlan, estate, eflags);


	/*
	* tuple table initialization
	*/
	#define FORECAST_NSLOTS 1
	ExecInitResultTupleSlot(estate, &createState->ss.ps);

	/*
	 * forecast nodes do no projections, so initialize projection info for this
	 * node appropriately
	 */
	ExecAssignResultTypeFromTL(&createState->ss.ps);
	createState->ss.ps.ps_ProjInfo = NULL;

	return createState;
}

TupleTableSlot *ExecCreateForecastModel(CreateForecastModelState *createState)
{
		TupleTableSlot 				*outSlot = NULL;
		PlanState 					*trainingPlan;
		PlanState 					*outerPlan;
		TupleTableSlot				*resultTupleSlot;
		bool 						isnull;
		bool 						importantContent;
		char *ptr5;
		char	*res,
		*sourceCpy,
		*sourceCpy2,
		*sourceCpy3,
		*sourceCpy4;
		char* ptr;
		int		braketCount = 1,
					length = 0;

		if(innerPlanState(createState) == NULL){ //no explicit trainingdata given, use lefttree for training
			trainingPlan = outerPlanState(createState);
		}else{ //use trainingdata for training
			trainingPlan = innerPlanState(createState);
		}

		resultTupleSlot = createState->ss.ps.ps_ResultTupleSlot;
		outerPlan = outerPlanState(createState);

		//fetch and process trainingTuples
		while((outSlot = ExecProcNode(trainingPlan)) &&
				!TupIsNull(outSlot)) {

			//we have a tuple so process it
			Datum 	datum;
			//count tuples
			createState->count++;

			//process next tuple in model
			datum = slot_getattr(outSlot, createState->modelInfo->measure->resno, &isnull);
			processForecastModel(createState->modelInfo, datum);
		}

		/* As a last step finalize & store the forecast model */
		if(createState->count == 0) {
			// we have no tuples -> we don't need to build a model
			ereport(ERROR, (errcode(ERRCODE_INVALID_PARAMETER_VALUE), errmsg("Trainingdata-Query doesn't return any Tuples to train a model!")));
		}

		//fetch and process forecastTuples
		while((outSlot = ExecProcNode(outerPlanState(createState))) && !TupIsNull(outSlot)) {

			//always save last tuple, used for the time column
			ExecCopySlot(resultTupleSlot, outSlot);
		}

		createState->modelInfo->buildModel = false;

		//store last seen timestamp
		createState->timestamp = GetDatumAsInt(exprType((Node*) createState->modelInfo->time->expr), slot_getattr(resultTupleSlot, createState->modelInfo->time->resno, &isnull));
		
		createState->modelInfo->timestamp = createState->timestamp;
		//finalize forecast models
		finalizeForecastModel(createState->modelInfo);
					//copy the trainingdata, need these to calculate the disAggKeys
					sourceCpy = (char*)palloc0(strlen(createState->sourcetext)+1);
					sourceCpy2 = (char*)palloc(strlen(createState->sourcetext)+1);
					sourceCpy3 = (char*)palloc0(strlen(createState->sourcetext)+1);
					sourceCpy4 = (char*)palloc0(strlen(createState->sourcetext)+1);
			braketCount = 1;
					length = 0;


			strcpy(sourceCpy, createState->sourcetext);
			strcpy(sourceCpy2, createState->sourcetext);
			importantContent=false;
			while(sourceCpy[length])
			{
				
				if(importantContent)
				{
					length++;
					if((sourceCpy[length])=='\'')
						importantContent=false;

					continue;
				}
				else
				{
					(sourceCpy[length])=tolower(sourceCpy[length]);
					(sourceCpy2[length])=tolower(sourceCpy2[length]);
					length++;
					if((sourceCpy[length])=='\'')
						importantContent=true;
				}

			}
			length=0;
			
			sourceCpy = strstr(sourceCpy, "training_data");
			if(!sourceCpy) // implicit model creation!
			 {
				 sourceCpy = strstr(sourceCpy2, "forecast");
				 if(!sourceCpy){
					 sourceCpy = sourceCpy2;
					 sourceCpy4 = sourceCpy2;
					 sourceCpy2=strrstr(sourceCpy2,"number");
					 ptr5=sourceCpy2;
					 sourceCpy[ptr5-sourceCpy]='\0';
					 sourceCpy2 =strrstr(sourceCpy," on ");
					 if(sourceCpy2)
					 {
						 sourceCpy4[sourceCpy2-sourceCpy]='\0';
						  length=sourceCpy2-sourceCpy;
						  sourceCpy=sourceCpy4;
					 }
					 else
					 {
						  length=ptr5-sourceCpy;
						  sourceCpy=sourceCpy4;
					 }
					 
					 
					 
					 
					 length = strlen(sourceCpy)+1;
				 }else{
					 strncpy(sourceCpy3,sourceCpy2,sourceCpy-sourceCpy2);
					 length=sourceCpy-sourceCpy2;
					 sourceCpy=sourceCpy3;
				 }
			 }
			else {
				sourceCpy = strpbrk(sourceCpy, "(");
				sourceCpy++;
				res = sourceCpy;
				while(braketCount)
				{
					if(*res == '(')
						braketCount++;
					else if(*res == ')')
						braketCount--;
					res++;
					length++;
					if(*res == '\0' && braketCount)
						elog(ERROR,"MUHAHAHA WITHOUT ME THIS COULD SEGFAULT HERE");
				}
			}
			
			createState->modelInfo->trainingData = palloc0(length);
			createState->modelInfo->trainingData = strncpy(createState->modelInfo->trainingData, sourceCpy, length-1);
			createState->modelInfo->trainingData[length-1] = '\0';
			
			ptr=createState->modelInfo->trainingData;
			while(*ptr)
			{
				if(*ptr=='\n')
					*ptr=' ';
				ptr++;
			}

		//store Model at the chosen place
		if (createState->modelInfo->storeModel == 0 || createState->modelInfo->storeModel==10 ) // model index
		{
			if(createState->modelInfo->storeModel==10)
				 (((ModelNode *)createState->modelInfo->mix)->model) = createState->modelInfo->model;
			else
				AddModelToModelIndex((TableNode*) createState->modelInfo->mix, createState->modelInfo->measure, createState->modelInfo->time, createState->modelInfo->granularity, createState->modelInfo->aggType, createState->timestamp, -1, createState->modelInfo->model, createState->modelInfo->forecastMethod, createState->modelInfo->parameterList);
			
		}
		else if (createState->modelInfo->storeModel == 2 || createState->modelInfo->storeModel==12) // hash table
		{
			AddModelToHashTable(createState->modelInfo->measure, createState->modelInfo->time, createState->modelInfo->granularity, createState->modelInfo->aggType, createState->sourcetext, createState->timestamp, -1, createState->modelInfo->model);
		}
		else if (createState->modelInfo->storeModel == 3 || createState->modelInfo->storeModel==13) // system table
		{
			Oid modelOid;

			if (createState->modelInfo->modelName == NULL)
				SetModelName(createState->modelInfo);

			if (createState->modelInfo->storeModel != 13) {
				modelOid = InsertForecastModel(createState->modelInfo->modelName, createState->modelInfo->time,	createState->modelInfo->measure, getModelTypeAsString(createState->modelInfo->forecastMethod), createState->modelInfo->granularity,	createState->modelInfo->aggType, createState->modelInfo->mix, createState->implicit, createState->modelInfo->trainingData, true, createState->timestamp, -1);
				storeModelParameters(createState->modelInfo, modelOid);
			} else {
				storeModelParameters(createState->modelInfo, createState->modelInfo->modelOid);
			}
		}
		else if(createState->modelInfo->storeModel == 4 || createState->modelInfo->storeModel==14) // model graph
		{
			if (createState->modelInfo->modelName == NULL)
				SetModelName(createState->modelInfo);

			AddModelToModelGraph(createState->modelInfo, createState->ss.ps.state->es_range_table);
		}

		elog(INFO, "Model created!");

//TODO: this is a potential FAIL! need to manipulate the TargetList in the optimizer!
		// we can save time here, if we created the model implicitly we will never use this tuple
		if(!createState->implicit){
			resultTupleSlot = BuildModelInfoTupleTableSlot(createState->modelInfo);
		}

		return resultTupleSlot;
}

int
ExecCountSlotsCreateModel(CreateForecastModel *node)
{
	return ExecCountSlotsNode(outerPlan(node)) +
		ExecCountSlotsNode(innerPlan(node)) + 1;
}


/*
 * Utility routines.
 *
 * Copyright (C) 2008 Bernhard Reutner-Fischer
 *
 * Licensed under GPLv2 or later, see file License in this tarball for details.
 */
char* strrstr(const char *haystack, const char *needle)
{
	char *r = NULL;

	if (!needle[0])
		return (char*)haystack + strlen(haystack);
	while (1) {
		char *p = strstr(haystack, needle);
		if (!p)
			return r;
		r = p;
		haystack = p + 1;
	}
}

void
ExecEndCreateForecastModel(CreateForecastModelState *node)
{
	ExecClearTuple(node->ss.ps.ps_ResultTupleSlot);

	//quit the usage of the trainingPlan
	if(innerPlanState(node))
		ExecEndNode(innerPlanState(node));

	ExecEndNode(outerPlanState(node));
}


