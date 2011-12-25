#ifndef NODECREATEFORECASTMODEL_H_
#define NODECREATEFORECASTMODEL_H_

#include "nodes/execnodes.h"

//extern int	ExecCountSlotsSingleForecast(SingleForecast *node);
//extern SingleForecastState *ExecInitSingleForecast(SingleForecast *node, EState *estate, int eflags);
//extern TupleTableSlot *ExecSingleForecast(SingleForecastState *node);
//extern void ExecEndSingleForecast(SingleForecastState *node);
extern CreateForecastModelState *ExecInitCreateForecastModel(CreateForecastModel *node, EState *estate, int eflags);
extern TupleTableSlot *ExecCreateForecastModel(CreateForecastModelState *node);
extern int ExecCountSlotsCreateModel(CreateForecastModel *node);
extern void ExecEndCreateForecastModel(CreateForecastModelState *node);
char* strrstr(const char *haystack, const char *needle);

#endif /*NODECREATEFORECASTMODEL_H_*/
