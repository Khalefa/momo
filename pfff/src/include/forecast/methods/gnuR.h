#ifndef GNUR_H
#define GNUR_H

#include "fmgr.h"
#include "forecast/algorithm.h"

extern void initRModel(PG_FUNCTION_ARGS);
extern void processRModel(PG_FUNCTION_ARGS);
extern void finalizeRModel(PG_FUNCTION_ARGS);
extern Datum getNextRValue(PG_FUNCTION_ARGS);

extern void storeRModelParameters(PG_FUNCTION_ARGS);
extern void loadRModelParameters(PG_FUNCTION_ARGS);
extern void incrementalUpdateRModel(PG_FUNCTION_ARGS);

#endif /* GNUR_H */
