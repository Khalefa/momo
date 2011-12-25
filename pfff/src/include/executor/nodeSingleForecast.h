#ifndef NODESINGLEFORECAST_H_
#define NODESINGLEFORECAST_H_

#include "nodes/execnodes.h"

extern int	ExecCountSlotsSingleForecast(SingleForecast *node);
extern SingleForecastState *ExecInitSingleForecast(SingleForecast *node, EState *estate, int eflags);
extern TupleTableSlot *ExecSingleForecast(SingleForecastState *node);
extern void ExecEndSingleForecast(SingleForecastState *node);


#endif /*NODESINGLEFORECAST_H_*/
