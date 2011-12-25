/*
 * AdditiveDec.c
 *
 * Decomposes an additive timeseries into trend, season and rest.
 * 
 *  Created on: 07.01.2010
 *      Author: uf
 */
#include <stdio.h>
#include <stdlib.h>
#include "forecast/methods/additiveDec.h"
#include "nodes/execnodes.h"


void InitAdditiveDec(ModelInfo *modelState, int window, int period)
{
	int i;
	AdditiveDec *ma = makeNode(AdditiveDec);
	
	if ((window % 2) == 0)
	{
		ma->windowSize = window + 1;
		ma->even = 1;
	} else
	{
		ma->even = 0;
		ma->windowSize = window;
	}
	ma->period = period;
	ma->currentPos = 0;
	ma->status = 0;

	// alloc memory
	ma->tupcount = 0;
	ma->valuessize = 1024;
	ma->values = (double*) malloc(ma->valuessize * sizeof(double));
	ma->trendsize = 1024;
	ma->trend = (double*) malloc(ma->trendsize * sizeof(double));

	ma->sums = (double*) malloc(ma->windowSize * sizeof(double));
	for (i=0; i<ma->windowSize; i++)
	{
		ma->sums[i] = 0;
	}
	
	modelState->model = (Model*) ma;
}


void ProcessAdditiveDec(ModelInfo *modelState, double y)
{
	int stop, start;
	AdditiveDec *ma = (AdditiveDec*) modelState->model;
	
	// save current value
	ma->values[ma->tupcount] = y;

	ma->currentPos++;
	
	if (ma->status == 0)
		stop = ma->currentPos;
	if (ma->status == 1)
		stop = ma->windowSize;


	if (ma->currentPos == ma->windowSize) {
		ma->currentPos = 0;

		// we are done with the start phase, set status to 1
		if (ma->status == 0)
			ma->status = 1;
	}

	// calculate sums in windows
	for (start=0; start<stop; start++)
	{
		// for even window sizes, first value and last value with lower weight
		if (ma->even && (ma->sums[start] == 0))
			ma->sums[start] = 0.5 * y;
		else if (ma->even && (start == ma->currentPos))
			ma->sums[start] += 0.5 * y;
		else
			ma->sums[start] += y;
	}

	// a value is done
	if (ma->status == 1)
	{
		ma->trend[ma->tupcount - (int) ma->windowSize/2] = (1/(double)ma->windowSize) * ma->sums[ma->currentPos];
		ma->sums[ma->currentPos] = 0;
	}

	ma->tupcount++;
}


void FinalizeAdditiveDec(ModelInfo* modelState)
{
	int i,k;
	double *seasonindex;
	double *seasoncount;
	AdditiveDec *ma = (AdditiveDec*) modelState->model;
	
	// TODO: improve calculating of start values
	
	k = (int)(ma->windowSize/2);
	for (i=0; i<k; i++)
	{
		ma->trend[i] = ma->trend[k];
	}

	// TODO: improve calculating of end values
	for (i=ma->tupcount-k; i<ma->tupcount; i++)
	{
		ma->trend[i] = ma->trend[ma->tupcount-k-1];
	}

	// calculate season indexes
	// TODO: perform incremental calculation of season indexes
	seasonindex = (double*) malloc(ma->period * sizeof(double));
	seasoncount  = (double*) malloc(ma->period * sizeof(double));
	for (i=0; i<ma->period; i++)
	{
		seasonindex[i] = 0;
		seasoncount[i] = 0;
	}
	for (i=0; i<ma->tupcount; i++)
	{
		seasonindex[i%ma->period] += (ma->values[i] - ma->trend[i]);
		seasoncount[i%ma->period]++;
	}
	for (i=0; i<ma->period; i++)
	{
		seasonindex[i] = seasonindex[i] / seasoncount[i];
	}
	// set season array
	ma->season = (double*) malloc(ma->tupcount * sizeof(double));
	for (i=0; i<ma->tupcount; i++)
	{
		ma->season[i] = seasonindex[i%ma->period];
	}

	// TODO: center seasonal figure (like R)
	/*double average = 0;
	for (i=0; i<ma->period; i++)
	{
		average += seasonindex[i];
	}
	average = average/ma->period;
	printf("average %f ", average);
	for (i=0; i<ma->tupcount; i++)
	{
		ma->season[i] = ma->season[i] - average;
	}*/

	free(seasonindex);
	free(seasoncount);

	// set rest array
	ma->rest = (double*) malloc(ma->tupcount * sizeof(double));
	for (i=0; i<ma->tupcount; i++)
	{
		ma->rest[i] = ma->values[i] - ma->trend[i] - ma->season[i];
	}
	
	ma->currentPos = 0;
}


double GetTrendValue(ModelInfo *modelState, int position)
{
	AdditiveDec *ma = (AdditiveDec*) modelState->model;
	if (position < ma->tupcount)
		return ma->trend[position];
	else
		return -99;
}

double GetSeasonValue(ModelInfo *modelState, int position)
{
	AdditiveDec *ma = (AdditiveDec*) modelState->model;
	if (position < ma->tupcount)
		return ma->season[position];
	else
		return -99;
}

double GetRestValue(ModelInfo *modelState, int position)
{
	AdditiveDec *ma = (AdditiveDec*) modelState->model;
	if (position < ma->tupcount)
		return ma->rest[position];
	else
		return -99;
}


void ClearAdditiveDec(ModelInfo *modelState)
{
	AdditiveDec *ma = (AdditiveDec*) modelState->model;
	
	free(ma->values);
	free(ma->trend);
	free(ma->season);
	free(ma->rest);
}
