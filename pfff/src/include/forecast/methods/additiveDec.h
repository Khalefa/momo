/*
 * AdditiveDec.h
 *
 *  Decomposes an additive timeseries into trend, season and rest.
 * 
 *  Created on: 07.01.2010
 *      Author: uf
 */

#ifndef ADDITIVEDEC_H_
#define ADDITIVEDEC_H_

#include "postgres.h"
#include "forecast/algorithm.h"

void InitAdditiveDec(ModelInfo* modelState, int window, int period);
void ProcessAdditiveDec(ModelInfo* modelState, double y);
void FinalizeAdditiveDec(ModelInfo *modelState);
void ClearAdditiveDec(ModelInfo *modelState);
double GetTrendValue(ModelInfo *modelState, int position);
double GetSeasonValue(ModelInfo *modelState, int position);
double GetRestValue(ModelInfo *modelState, int position);

#endif /* ADDITIVEDEC_H_ */
