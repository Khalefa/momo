/**
 * Implementation of linear regression for equidistant time series.
 */

#ifndef LINREG_H
#define LINREG_H

#include "postgres.h"
#include "forecast/algorithm.h"

extern void initLinRegModel(PG_FUNCTION_ARGS);
extern void processLinRegModel(PG_FUNCTION_ARGS);
extern void finalizeLinRegModel(PG_FUNCTION_ARGS);
extern Datum getNextLinRegValue(PG_FUNCTION_ARGS);

extern void storeLinRegModelParameters(PG_FUNCTION_ARGS);
extern void loadLinRegModelParameters(PG_FUNCTION_ARGS);

extern void incrementalUpdateLinRegModel(PG_FUNCTION_ARGS);


#endif /* LINREG_H_ */
