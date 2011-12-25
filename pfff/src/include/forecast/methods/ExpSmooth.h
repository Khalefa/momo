/*
 * commonexp.h
 *
 *  This is a common ar interface for all methods which use holt winters interface
 *
 *  Created on:
 *      Author: schildt
 */

#ifndef COMMONHW_H_
#define COMMONHW_H_


#define errmsg g_errmsg
// undefine some constructs to avoid errors and warnings but we wont use them anyway
#undef _
#undef gettext
#undef errmsg

#include "forecast/algorithm.h"
#include "utils/portal.h"



typedef struct HoltWintersModel{
	int    			start_time; //dunno yet
	double 			b;//trend
	double 			a;//level
	double			seasonType;//type of the model multiplicative oder additive
	double 			period;//period of a season if existing
	double			doseasonal; 
	double			dotrend;
	int			optflag[4];
	double			*level;//These arrays are needed for HW function evaluation during optimization, storing them in here does decrease the optimization-time significantly
	double			*trend;
	double			*season;
	double			tempLevel;
	double			tempTrend;
	double			*tempSeason;
	double			scount;
	char*			errorfunction;
	double 			alpha;
	double			beta;
	double			gamma;
	int				obsCount;
	int				actualForecast;  /* the point of time from which to forecast */
	double 			*s;
	
}HoltWintersModel;

typedef struct HWModel
{
	NodeTag			type;
	int				obsCount;

	HoltWintersModel 		super;

	MemoryContext 	dataCtx;
	double			*data;
	int				maxObs;

	int gradflag;		//flag to determine state of the function evaluation
}HWModel;



/*
 * definitions for the forecast interface
 */
extern void inithwModel(PG_FUNCTION_ARGS);
extern void processhwModel(PG_FUNCTION_ARGS);
extern void finalizehwModel(PG_FUNCTION_ARGS);
extern Datum getNexthwmodelValue(PG_FUNCTION_ARGS);
extern void storehwModelParameters(PG_FUNCTION_ARGS);
extern void loadhwModelParameters(PG_FUNCTION_ARGS);
extern void incrementalUpdatehwModel1(PG_FUNCTION_ARGS);
extern void incrementalUpdatehwModel2(PG_FUNCTION_ARGS);
extern void reestimateHwModelParameters(PG_FUNCTION_ARGS);
extern void restoreHwModelParameterForModelGraph(HoltWintersModel *model,Relation parameterRelation,Oid modelOid);
extern void backupHwModelParameterForModelGraph(HoltWintersModel *model,Relation parameterRelation,Oid modelOid);
extern double exp1Var(unsigned n, double *x, double *grad, void *my_func_data);
extern double exp2Var(unsigned n, double *x, double *grad, void *my_func_data);
extern double exp3Var(unsigned n, double *x, double *grad, void *my_func_data);
extern double *expandoDDataArrayTo(double* array, int size);
extern void parseHwParameters(List *parameterList, HoltWintersModel *specificModel);
extern void initHoltWintersForecast(HoltWintersModel *forecast_model);
extern void expandSArray(HoltWintersModel *forecastModel,int to);
extern double get_next_hwforecast(HoltWintersModel *forecast_model);
/*
 * utility functions
 */
#endif /* COMMONHW_H_ */
