/*
 * commonGretl.h
 *
 *  This is a common gretl interface for all methods which use gretl interface
 *
 *  Created on:
 *      Author: moss
 */

#ifndef COMMONGRETL_H_
#define COMMONGRETL_H_


#define errmsg g_errmsg
// undefine some constructs to avoid errors and warnings but we wont use them anyway
#undef _
#undef gettext
#undef errmsg

#include "forecast/algorithm.h"
#include "forecast/methods/commonARIMA.h"

typedef enum GretlLagsDataItems {
	Gretl_Arma_P,
	Gretl_Arma_Q,
	Gretl_Arima_d,
	Gretl_Arima_D,
	Gretl_Arma_pd
}GretlLagsDataItems;

char *GreltDataItemsKey[] =
{
		"arma_P",
		"arma_Q",
		"arima_d",
		"arima_D",
		"arma_pd"
};

typedef struct GretlModel
{
	ArimaModel 		super;

	MemoryContext 	dataCtx;
	double			**data;
	int				maxObs;
	int				obsCount;
	//double			*forecastedValues;
	//int				p; /* non seasonal AR Order*/
	//int 			d; /* non-seasonal difference */
	//int 			q; /* max non-seasonal MA order */
	//int 			P; /* seasonal AR order */
	//int 			D; /* seasonal difference */
	//int 			Q; /* seasonal MA order */
	//int 			pd;/* periodicity of data */
	int				nq; /* the max number of MA lags */
	//double          *uhat; /*regression residuals MA part of the model*/
	//double			*y;
	int 			includeConstant;
}GretlModel;



/*
 * definitions for the forecast interface
 */
extern void initGretlgModel(PG_FUNCTION_ARGS);
extern void processGretlModel(PG_FUNCTION_ARGS);
extern void finalizeGretlModel(PG_FUNCTION_ARGS);
extern Datum getNextGretlValue(PG_FUNCTION_ARGS);
extern void storeGretlModelParameters(PG_FUNCTION_ARGS);
extern void loadGretlModelParameters(PG_FUNCTION_ARGS);
extern void incrementalUpdateGretlModel(PG_FUNCTION_ARGS);

/*
 * utility functions
 */
extern void copyGretlModel(MODEL mod);
#endif /* COMMONGRETL_H_ */
