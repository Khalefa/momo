/*
 * greedyAlgorithm.h
 *
 *  Created on: 23.05.2011
 *      Author: hartmann
 */

#ifndef GREEDYALGORITHM_H_
#define GREEDYALGORITHM_H_

#include "nodes/parsenodes.h"
#include "tcop/tcopprot.h"
#include "tcop/pquery.h"
#include "utils/lsyscache.h"

List *
ExtractWorkload(void);

double
EvaluateConfiguration(double *conf, double lcpError, int modelCnt, int maxModels);

void
FillWorkloadElementWithValuesAndModelInfo(FillModelGraphStmt *stmt, WorkloadElement *elem);

List *
GetAllDisaggregaties(ModelGraphIndexNode *elem);

void
FillWorkloadElementWithDisAggSchemesAndErrors(WorkloadElement *elem, List *workload);

double
meanOfDoubleArray(double *array, int arrayLength);

void
FillModelGraphGreedy(FillModelGraphStmt *stmt);

#endif /* GREEDYALGORITHM_H_ */
