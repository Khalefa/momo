/*
 * commonar.h
 *
 *  This is a common ar interface for all methods which use ar interface
 *
 *  Created on:
 *      Author: schildt
 */

#ifndef COMMONAR_H_
#define COMMONAR_H_


#define errmsg g_errmsg
// undefine some constructs to avoid errors and warnings but we wont use them anyway
#undef _
#undef gettext
#undef errmsg

#include "forecast/algorithm.h"
#include "forecast/methods/commonARIMA.h"

typedef enum gretlLagsDataItems {
	gretl_Arma_P,
	gretl_Arma_Q,
	gretl_Arima_d,
	gretl_Arima_D,
	gretl_Arma_pd
}gretlLagsDataItems;



typedef struct ARModel
{
	NodeTag			type;
	int				obsCount;
	ArimaModel 		super;

	MemoryContext 	dataCtx;
	double			**data;
	int				maxObs;
	
	int sflag;		//Exponent for the backshift operator
}ARModel;



/*
 * definitions for the forecast interface
 */
extern void initarModel(PG_FUNCTION_ARGS);
extern void processarModel(PG_FUNCTION_ARGS);
extern void finalizearModel(PG_FUNCTION_ARGS);
extern Datum getNextarmodelValue(PG_FUNCTION_ARGS);
extern void storearModelParameters(PG_FUNCTION_ARGS);
extern void loadarModelParameters(PG_FUNCTION_ARGS);
extern void incrementalUpdatearModel(PG_FUNCTION_ARGS);
extern void parseArParameters(List *parameterList, ArimaModel *specificModel);
extern void restoreArModelParameterForModelGraph(ArimaModel *model,Relation parameterRelation,Oid modelOid);
extern void backupArModelParameterForModelGraph(ArimaModel *model,Relation parameterRelation,Oid modelOid);
extern double arima_nonSeasonal_SMAPE(unsigned n, const double *x, double *grad, void *my_func_data); 
extern double arima_nonSeasonal_CSS(unsigned n, const double *x, double *grad, void *my_func_data);
extern double arima_nonSeasonal_ML(unsigned n, const double *x, double *grad, void *my_func_data);
extern double arima_AddSeasonal_CSS(unsigned n, const double *x, double *grad, void *my_func_data);
extern double arima_AddSeasonal_SMAPE(unsigned n, const double *x, double *grad, void *my_func_data);
extern double arima_MulSeasonal_SMAPE(unsigned n, const double *x, double *grad, void *my_func_data);
extern double arima_MulSeasonal_CSS(unsigned n, const double *x, double *grad, void *my_func_data);
extern double arima_MulSeasonal_ML(unsigned n, const double *x, double *grad, void *my_func_data);
extern double arima_AddSeasonal_ML(unsigned n, const double *x, double *grad, void *my_func_data);
extern double **expandTDDataArrayTo(double** array, int size,int v);
extern void incrementalUpdatearModel1(PG_FUNCTION_ARGS);
extern void incrementalUpdatearModel2(PG_FUNCTION_ARGS);
extern void reestimateArModelParameters(PG_FUNCTION_ARGS);

/*
 * utility functions
 */
#endif /* COMMONHW_H_ */
