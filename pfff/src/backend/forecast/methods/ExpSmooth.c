
#include <time.h>
#include "forecast/methods/ExpSmooth.h"
#include "utils/memutils.h"
#include "forecast/algorithm.h"
#include "nodes/value.h"
#include "nodes/parsenodes.h"
#include "nodes/nodes.h"
#include "catalog/pg_type.h"
#include "catalog/pg_model.h"
#include "utils/array.h"
#include "catalog/pg_parameter.h"
#include "forecast/modelGraph/modelGraphReceiver.h"
#include "utils/portal.h"
#include <float.h>
#include <math.h>
#include "utils/guc.h"
#include "tcop/pquery.h"
#include "tcop/tcopprot.h"
#include "nodes/nodeFuncs.h"

double exp1Var(unsigned n, double *x, double *grad, void *my_func_data) {
	
	HWModel 		*model=(HWModel*)my_func_data;
	int 			fup,fdown;
	double			*level=model->super.level;
	double			*trend=model->super.trend;
	double			*season=model->super.season;
	double			dotrend=(model->super.dotrend);
	double			doseasonal=(model->super.doseasonal);
	int				period=(model->super.period);
	int				seasonal=(model->super.seasonType); //Season additive oder multiplikative
	int 			xl=(model->obsCount);
	double 			a=(model->super.a);
	double 			b=(model->super.b);
	double			alpha=x[0];
	double			beta=model->super.beta;
	double			gamma=model->super.gamma;
	int    			start_time=(model->super.start_time);
	

	

	double smape=0;
	double ga=1e-5;
	
    double xhat = 0, stmp = 0;
    int i, i0, s0;

    /* copy start values to the beginning of the vectors */
    level[0] = a;
    if (dotrend == 1) trend[0] = b;
	

    for (i = start_time; i < xl; i++) {
    	/* indices for period i */
    	i0 = i - start_time + 1;
    	s0 = i0 + period - 1;

    	/* forecast *for* period i */
    	xhat = level[i0 - 1] + (dotrend == 1 ? trend[i0 - 1] : 0);
    	stmp = doseasonal == 1 ? season[s0 - period] : (seasonal != 1);
    	if (seasonal == 1)
    		xhat += stmp;
    	else
    		xhat *= stmp;

    	
    	switch(model->super.optflag[3]){
    	case 0:	smape += SMAPE(xhat,model->data[i]);
    			break;

    	case 1:	smape+= SSE(xhat,model->data[i]);
    			break;

    	case 2: smape+= ABS(xhat,model->data[i]);
    			break;
    	}

    	/* estimate of level *in* period i */
    	if (seasonal == 1)
    		level[i0] = alpha       * (model->data[i] - stmp)
    		+ (1 - alpha) * (level[i0 - 1] + trend[i0 - 1]);
    	else
    		level[i0] = alpha      * (model->data[i] / stmp)
    		+ (1 - alpha) * (level[i0 - 1] + trend[i0 - 1]);

    	/* estimate of trend *in* period i */
    	if (dotrend == 1)
    		trend[i0] = beta        * (level[i0] - level[i0 - 1])
    		+ (1 - beta)  * trend[i0 - 1];

    	/* estimate of seasonal component *in* period i */
    	if (doseasonal == 1) {
    		if (seasonal == 1)
    			season[s0] = gamma       * (model->data[i] - level[i0])
    			+ (1 - gamma) * stmp;
    		else
    			season[s0] = gamma       * (model->data[i] / level[i0])
    			+ (1 - gamma) * stmp;
    	}
    }
	
	if(grad && model->gradflag)
	{
		model->gradflag=0;
		for(i=0;i<n;i++)
		{
			if(model->super.optflag[i]==0)
			{
				grad[i]=0;
				continue;
			}
			x[i]+=ga;
			fup=exp1Var(n, x,grad,my_func_data);
			x[i]-=2*ga;
			fdown=exp1Var(n, x,grad,my_func_data);
			x[i]+=ga;
			grad[i]=(fup-fdown)/(2*ga);
		}
		model->gradflag=1;
	}

	return smape/(model->obsCount-(model->super.start_time-1));
}

double exp2Var(unsigned n, double *x, double *grad, void *my_func_data) {
	
	HWModel 		*model=(HWModel*)my_func_data;
	int				fup,fdown;
	double			*level=model->super.level;
	double			*trend=model->super.trend;
	double			*season=model->super.season;
	double			dotrend=(model->super.dotrend);
	double			doseasonal=(model->super.doseasonal);
	int				period=(model->super.period);
	int				seasonal=(model->super.seasonType); //Season additive oder multiplikative
	int 			xl=(model->obsCount);
	double 			a=(model->super.a);
	double 			b=(model->super.b);
	double			alpha=x[0];
	double			beta;
	double			gamma;
	int    			start_time=(model->super.start_time);
		double smape=0;
	double ga=1e-5;
	    double xhat = 0, stmp = 0;
    int i, i0, s0;
	if(model->super.optflag[1]==1)
		{
						beta=x[1];
						gamma=model->super.gamma;
		}
	else
		{
			 beta=model->super.beta;
			 gamma=x[1];
		}

	
	

	


	


    /* copy start values to the beginning of the vectors */
    level[0] = a;
    if (dotrend == 1) trend[0] = b;
	

    for (i = start_time; i < xl; i++) {
    	/* indices for period i */
    	i0 = i - start_time + 1;
    	s0 = i0 + period - 1;

    	/* forecast *for* period i */
    	xhat = level[i0 - 1] + (dotrend == 1 ? trend[i0 - 1] : 0);
    	stmp = doseasonal == 1 ? season[s0 - period] : (seasonal != 1);
    	if (seasonal == 1)
    		xhat += stmp;
    	else
    		xhat *= stmp;

    	switch(model->super.optflag[3]){
    	case 0:	smape += SMAPE(xhat,model->data[i]);
    			break;

    	case 1:	smape+= SSE(xhat,model->data[i]);
    			break;

    	case 2: smape+= ABS(xhat,model->data[i]);
    			break;
    	}
    	
    	/* estimate of level *in* period i */
    	if (seasonal == 1)
    		level[i0] = alpha       * (model->data[i] - stmp)
    		+ (1 - alpha) * (level[i0 - 1] + trend[i0 - 1]);
    	else
    		level[i0] = alpha      * (model->data[i] / stmp)
    		+ (1 - alpha) * (level[i0 - 1] + trend[i0 - 1]);

    	/* estimate of trend *in* period i */
    	if (dotrend == 1)
    		trend[i0] = beta        * (level[i0] - level[i0 - 1])
    		+ (1 - beta)  * trend[i0 - 1];

    	/* estimate of seasonal component *in* period i */
    	if (doseasonal == 1) {
    		if (seasonal == 1)
    			season[s0] = gamma       * (model->data[i] - level[i0])
    			+ (1 - gamma) * stmp;
    		else
    			season[s0] = gamma       * (model->data[i] / level[i0])
    			+ (1 - gamma) * stmp;
    	}
    }
	
	if(grad && model->gradflag)
	{
		model->gradflag=0;
		for(i=0;i<n;i++)
		{
			if(model->super.optflag[i]==0)
			{
				grad[i]=0;
				continue;
			}
			x[i]+=ga;
			fup=exp2Var(n, x,grad,my_func_data);
			x[i]-=2*ga;
			fdown=exp2Var(n, x,grad,my_func_data);
			x[i]+=ga;
			grad[i]=(fup-fdown)/(2*ga);
		}
		model->gradflag=1;
	}

	return smape/(model->obsCount-(model->super.start_time-1));
}

double exp3Var(unsigned n, double *x, double *grad, void *my_func_data) {
	
	HWModel 		*model=(HWModel*)my_func_data;
	int 			fup,fdown;
	double			*level=model->super.level;
	double			*trend=model->super.trend;
	double			*season=model->super.season;
	double			dotrend=(model->super.dotrend);
	double			doseasonal=(model->super.doseasonal);
	int				period=(model->super.period);
	int				seasonal=(model->super.seasonType); //Season additive oder multiplikative
	int 			xl=(model->obsCount);
	double 			a=(model->super.a);
	double 			b=(model->super.b);
	double			alpha2=x[0];
	double			beta2=x[1];
	double			gamma2=x[2];
	int    			start_time=(model->super.start_time);
	

	

	double smape=0;
	double ga=1e-5;
	
    double xhat = 0, stmp = 0;
    int i, i0, s0;

    /* copy start values to the beginning of the vectors */
    level[0] = a;
    if (dotrend == 1) trend[0] = b;
	

    for (i = start_time; i < xl; i++) {
    	/* indices for period i */
    	i0 = i - start_time + 1;
    	s0 = i0 + period - 1;

    	/* forecast *for* period i */
    	xhat = level[i0 - 1] + (dotrend == 1 ? trend[i0 - 1] : 0);
    	stmp = doseasonal == 1 ? season[s0 - period] : (seasonal != 1);
    	if (seasonal == 1)
    		xhat += stmp;
    	else
    		xhat *= stmp;

    	switch(model->super.optflag[3]){
    	case 0:	smape += SMAPE(xhat,model->data[i]);
    			break;

    	case 1:	smape+= SSE(xhat,model->data[i]);
    			break;

    	case 2: smape+= ABS(xhat,model->data[i]);
    			break;
    	}
    	/* estimate of level *in* period i */
    	if (seasonal == 1)
    		level[i0] = alpha2       * (model->data[i] - stmp)
    		+ (1 - alpha2) * (level[i0 - 1] + trend[i0 - 1]);
    	else
    		level[i0] = alpha2      * (model->data[i] / stmp)
    		+ (1 - alpha2) * (level[i0 - 1] + trend[i0 - 1]);

    	/* estimate of trend *in* period i */
    	if (dotrend == 1)
    		trend[i0] = beta2        * (level[i0] - level[i0 - 1])
    		+ (1 - beta2)  * trend[i0 - 1];

    	/* estimate of seasonal component *in* period i */
    	if (doseasonal == 1) {
    		if (seasonal == 1)
    			season[s0] = gamma2       * (model->data[i] - level[i0])
    			+ (1 - gamma2) * stmp;
    		else
    			season[s0] = gamma2       * (model->data[i] / level[i0])
    			+ (1 - gamma2) * stmp;
    	}
    }
	
	if(grad && model->gradflag)
	{
		model->gradflag=0;
		for(i=0;i<n;i++)
		{
			if(model->super.optflag[i]==0)
			{
				grad[i]=0;
				continue;
			}
			x[i]+=ga;
			fup=exp3Var(n, x,grad,my_func_data);
			x[i]-=2*ga;
			fdown=exp3Var(n, x,grad,my_func_data);
			x[i]+=ga;
			grad[i]=(fup-fdown)/(2*ga);
		}
		model->gradflag=1;
	}
	
	return smape/(model->obsCount-(model->super.start_time-1));
	
}


double
*expandoDDataArrayTo(double* array, int size) {
	array = (double *) repalloc(array, size* sizeof(*(array)));
	return array;
}


void
parseHwParameters(List *parameterList, HoltWintersModel *specificModel) {
	ListCell				*cell;
	foreach(cell,parameterList) {
		AlgorithmParameter		*param = lfirst(cell);

		/* Seasonflag*/
		if(strcmp(param->key,"has_season") == 0) {
			if(IsA(&(param->value->val),Integer)) {
				specificModel->doseasonal = intVal(&param->value->val);
				specificModel->optflag[2]=specificModel->doseasonal;
			} else
				ereport(ERROR,
				        (errcode(ERRCODE_INVALID_PARAMETER_VALUE),
				         errmsg("Parameter value has to be an Integer value"),
				         errposition(param->value->location)));
		} else if(strcmp(param->key,"has_trend") == 0) {
			if(IsA(&(param->value->val),Integer)) {
				specificModel->dotrend = intVal(&param->value->val);
				specificModel->optflag[1]=specificModel->dotrend;
			} else
				ereport(ERROR,
				        (errcode(ERRCODE_INVALID_PARAMETER_VALUE),
				         errmsg("Parameter value has to be an Integer value"),
				         errposition(param->value->location)));
					
		} else if(strcmp(param->key,"seasontype") == 0) {
			if(IsA(&(param->value->val),Integer)) {
				specificModel->seasonType = intVal(&param->value->val);
			} else
				ereport(ERROR,
				        (errcode(ERRCODE_INVALID_PARAMETER_VALUE),
				         errmsg("Parameter value has to be an Integer value"),
				         errposition(param->value->location)));
					
		} else if(strcmp(param->key,"alpha") == 0) {
			if(IsA(&(param->value->val),Float)) {
				specificModel->alpha = floatVal(&param->value->val);
				specificModel->optflag[0]=0;
			} else
				ereport(ERROR,
				        (errcode(ERRCODE_INVALID_PARAMETER_VALUE),
				         errmsg("Parameter value has to be an float value"),
				         errposition(param->value->location)));
					
		}else if(strcmp(param->key,"beta") == 0) {
			if(IsA(&(param->value->val),Float)) {
				specificModel->beta = floatVal(&param->value->val);
				specificModel->optflag[1]=0;
				specificModel->dotrend=1;
			} else
				ereport(ERROR,
				        (errcode(ERRCODE_INVALID_PARAMETER_VALUE),
				         errmsg("Parameter value has to be an float value"),
				         errposition(param->value->location)));
					
		}else if(strcmp(param->key,"gamma") == 0) {
			if(IsA(&(param->value->val),Float)) {
				specificModel->gamma = floatVal(&param->value->val);
				specificModel->optflag[2]=0;
				specificModel->doseasonal=1;
			} else
				ereport(ERROR,
				        (errcode(ERRCODE_INVALID_PARAMETER_VALUE),
				         errmsg("Parameter value has to be an float value"),
				         errposition(param->value->location)));
					
		}else if(strcmp(param->key,"period") == 0) {
			if(IsA(&(param->value->val),Integer)) {
				specificModel->period = intVal(&param->value->val);
			} else
				ereport(ERROR,
				        (errcode(ERRCODE_INVALID_PARAMETER_VALUE),
				         errmsg("Parameter value has to be an Integer value"),
				         errposition(param->value->location)));
		}  else if(strcmp(param->key,"error") == 0) {
			if(IsA(&(param->value->val),String)) {
				specificModel->errorfunction = palloc0((strlen(strVal(&param->value->val))+1)*sizeof(char));
				strcpy(specificModel->errorfunction,strVal(&param->value->val));
			} else
				ereport(ERROR,
				        (errcode(ERRCODE_INVALID_PARAMETER_VALUE),
				         errmsg("Parameter value has to be an String value"),
				         errposition(param->value->location)));
		} else
			ereport(WARNING,
			        (errcode(ERRCODE_INVALID_PARAMETER_VALUE),
			         errmsg("Parameter not known"),
			         errposition(((A_Const *)param->value)->location)));
	}
}


void
inithwModel(PG_FUNCTION_ARGS) {
	HWModel 		*m;
	MemoryContext 	old;
	ModelInfo		*model;


	model = (ModelInfo *)PG_GETARG_POINTER(0);

	m = makeNode(HWModel);

	/* create new MemoryContext for the data
	 * 1. it can get huge
	 * 2. it has a possible shorter lifespan than the parent context
	 */
	m->dataCtx = AllocSetContextCreate(CurrentMemoryContext,"HwData",ALLOCSET_DEFAULT_MINSIZE,ALLOCSET_DEFAULT_INITSIZE,MaxAllocSize);
	old = MemoryContextSwitchTo(m->dataCtx);

	// initialize data array
	m->data = palloc(512*sizeof(double));
	m->maxObs = 512;
	m->obsCount = 0;

	MemoryContextSwitchTo(old);

	/* initialize standard values now much early in createplan*/
	m->super.doseasonal = 0;
	m->super.dotrend = 0;
	m->super.optflag[0]=1; 
	m->super.optflag[1]=0;
	m->super.optflag[2]=0;
	m->super.optflag[3]=1;
	m->super.period=1;
	m->super.seasonType=1;
	m->super.start_time=0;
	m->super.actualForecast=0;
	m->super.scount=m->super.period;
	m->super.a=0;
	m->super.b=0;
	m->super.alpha=0;
	m->super.tempLevel=0;
	m->super.tempTrend=0;
	m->super.beta=0;
	m->super.gamma=0;
	m->super.errorfunction="SSE";
	/* parse the Input Parameters. Be careful. Parameter list has to be set in ModelInfo */
	if(model->parameterList)
		parseHwParameters(model->parameterList,&(m->super));

	model->model=(Model *)m;

}


void
processhwModel(PG_FUNCTION_ARGS) {
	double 			value;
	HWModel		*model;
	MemoryContext	old;

	model = (HWModel *)PG_GETARG_POINTER(0);
	value = PG_GETARG_FLOAT8(1);

	old = MemoryContextSwitchTo(model->dataCtx);

	if (model->obsCount == model->maxObs) {
		// not enough space -> expand the data array
		model->maxObs *= 2;
		model->data = expandoDDataArrayTo(model->data,model->maxObs);
	}

	model->data[model->obsCount] = value;
	Assert(model->data[model->obsCount] == value);

	model->obsCount++;

	MemoryContextSwitchTo(old);

}



void
initHoltWintersForecast(HoltWintersModel *forecast_model)
{
	forecast_model->actualForecast = 0;
	forecast_model->tempLevel=*(forecast_model->level);
	forecast_model->tempTrend=*(forecast_model->trend);
	memcpy(forecast_model->tempSeason,forecast_model->season,sizeof(double)*forecast_model->period);
}


void
expandSArray(HoltWintersModel *forecastModel,int to)
{
	forecastModel->tempSeason = repalloc(forecastModel->tempSeason,to * sizeof(*forecastModel->tempSeason));
	forecastModel->scount = to;
}

double get_next_hwforecast(HoltWintersModel *forecast_model)
{
	double result,temptempTrend,temptempLevel,stmp;
	if(forecast_model->scount <= forecast_model->scount+forecast_model->actualForecast)
		expandSArray(forecast_model,forecast_model->scount+forecast_model->actualForecast+1);
		
	
	if(forecast_model->seasonType) //additive
	{
		result=forecast_model->tempLevel;
		result+= forecast_model->dotrend ? forecast_model->tempTrend :0;
		result+= forecast_model->doseasonal ? forecast_model->tempSeason[forecast_model->actualForecast] :0;
	}
	else
	{
		result=forecast_model->tempLevel;
		result+= forecast_model->dotrend ? forecast_model->tempTrend :0;
		result*= forecast_model->doseasonal ? forecast_model->tempSeason[forecast_model->actualForecast] :1;
		
	}
	//actualize tempTrend and tempLevel and Season
	temptempTrend=forecast_model->tempTrend;
	temptempLevel=forecast_model->tempLevel;
	stmp= forecast_model->doseasonal == 1 ?  forecast_model->tempSeason[forecast_model->actualForecast] : (forecast_model->seasonType != 1);

	if (forecast_model->seasonType == 1)
	    forecast_model->tempLevel = forecast_model->alpha       * (result - stmp)
		      + (1 - forecast_model->alpha) * (temptempLevel + temptempTrend);
	else
	    forecast_model->tempLevel = forecast_model->alpha      * (result / stmp)
		      + (1 - forecast_model->alpha) * (temptempLevel + temptempTrend);

	/* estimate of trend *in* period i */
	if (forecast_model->dotrend == 1)
	    forecast_model->tempTrend = forecast_model->beta       * (forecast_model->tempLevel - temptempLevel)
		      + (1 - forecast_model->beta)  * temptempTrend;

	/* estimate of seasonal component *in* period i */
	if (forecast_model->doseasonal == 1) {
	    if (forecast_model->seasonType == 1)
		 forecast_model->tempSeason[forecast_model->actualForecast+(int)forecast_model->period] = forecast_model->gamma       * (result - forecast_model->tempLevel)
			   + (1 - forecast_model->gamma) * stmp;
	    else
		 forecast_model->tempSeason[forecast_model->actualForecast+(int)forecast_model->period] = forecast_model->gamma       * (result / forecast_model->tempLevel)
			   + (1 - forecast_model->gamma) * stmp;
	}
	forecast_model->actualForecast++;
	return result;
}

Datum
getNexthwmodelValue(PG_FUNCTION_ARGS) {
	int 				num;
	HoltWintersModel			*model;
	double				forecast;

	num = PG_GETARG_INT32(1);
	model = &(((HWModel *)PG_GETARG_POINTER(0))->super);

	if(num == 1) {
		/* we start a new forecast cycle -> reset the state of previous forecasts */
		initHoltWintersForecast(model);
	}

	forecast = get_next_hwforecast(model);

	PG_RETURN_FLOAT8(forecast);
}

void restoreHwModelParameterForModelGraph(HoltWintersModel *model,Relation parameterRelation,Oid modelOid)
{
	Datum			*values = NULL;
	int				numvalues = 0;
	ArrayType		**array;
	int				i;


	array = palloc(3*sizeof(*array));
	RetrieveParametersMg(modelOid,array,3,parameterRelation);

	/* load type and alpha,beta,gamma */
	deconstruct_array(array[0],FLOAT8OID,sizeof(float8),FLOAT8PASSBYVAL,'d',&values,NULL,&numvalues);
	model->doseasonal = DatumGetFloat8(values[0]);
	model->dotrend = DatumGetFloat8(values[1]);
	model->seasonType=DatumGetFloat8(values[2]);
	model->alpha = DatumGetFloat8(values[3]);
	model->beta = DatumGetFloat8(values[4]);
	model->gamma = DatumGetFloat8(values[5]);
	model->period = DatumGetFloat8(values[6]);
	model->tempLevel=0;
	model->tempTrend=0;
	pfree(values);

	/* load trend and level */
	deconstruct_array(array[1],FLOAT8OID,sizeof(float8),FLOAT8PASSBYVAL,'d',&values,NULL,&numvalues);
	model->tempTrend=DatumGetFloat8(values[0]);
	model->tempLevel=DatumGetFloat8(values[1]);
	model->level=palloc0(sizeof(double));
	model->trend=palloc0(sizeof(double));
	*(model->level)=model->tempLevel;
	*(model->trend)=model->tempTrend;
	pfree(values);

	/* load seasonal component*/
	deconstruct_array(array[2],FLOAT8OID,sizeof(float8),FLOAT8PASSBYVAL,'i',&values,NULL,&numvalues);
	model->season = palloc(model->period * sizeof(double));
	model->tempSeason = palloc(model->period * sizeof(double));
	for(i = 0; i<model->period; i++)
		{model->season[i]= DatumGetFloat8(values[i]);
		}
		
	pfree(values);


	pfree(array);

model->scount=model->period;
model->start_time=0;
model->optflag[0]=1;
	model->optflag[1]=1;
	model->optflag[2]=1;
}

void backupHwModelParameterForModelGraph(HoltWintersModel *model,Relation parameterRelation,Oid modelOid)
{
	ArrayType		*array;
	Datum			*values;
	int				i;

	/* save type and alpha,beta,gamma */
	values = palloc(7*sizeof(*values));
	values[0] = Float8GetDatum(model->doseasonal);
	values[1] = Float8GetDatum(model->dotrend);
	values[2] = Float8GetDatum(model->seasonType);
	values[3] = Float8GetDatum(model->alpha);
	values[4] = Float8GetDatum(model->beta);
	values[5] = Float8GetDatum(model->gamma);
	values[6] = Float8GetDatum(model->period);
	array = construct_array(values,7,FLOAT8OID,sizeof(float8),FLOAT8PASSBYVAL,'d');
	InsertOrReplaceParametersMg(modelOid,parameterRelation,1,array);
	pfree(values);
	pfree(array);



	/* save trend and level*/
	values = palloc(2*sizeof(*values));
	values[0] = Float8GetDatum(*(model->trend));
	values[1] = Float8GetDatum(*(model->level));

	array = construct_array(values,2,FLOAT8OID,sizeof(float8),FLOAT8PASSBYVAL,'d');
	InsertOrReplaceParametersMg(modelOid,parameterRelation,2,array);

	pfree(values);

	pfree(array);


	/* save seasonal component */
	values = palloc0((model->period*sizeof(*values)));
	for(i=0; i<model->period; i++)
		values[i] = Float8GetDatum(model->season[i]);
	array = construct_array(values,model->period,FLOAT8OID,sizeof(float8),FLOAT8PASSBYVAL,'d');
	InsertOrReplaceParametersMg(modelOid,parameterRelation,3,array);
	pfree(values);
	pfree(array);
}

void storehwModelParameters(PG_FUNCTION_ARGS) {
	ArrayType		*array;
	HoltWintersModel		*model = &(((HWModel *)PG_GETARG_POINTER(0))->super);
	Oid				modelOid = PG_GETARG_OID(1);
	Datum			*values;
	int				i;

	/* save type and alpha,beta,gamma */
	values = palloc(7*sizeof(*values));
	values[0] = Float8GetDatum(model->doseasonal);
	values[1] = Float8GetDatum(model->dotrend);
	values[2] = Float8GetDatum(model->seasonType);
	values[3] = Float8GetDatum(model->alpha);
	values[4] = Float8GetDatum(model->beta);
	values[5] = Float8GetDatum(model->gamma);
	values[6] = Float8GetDatum(model->period);
	array = construct_array(values,7,FLOAT8OID,sizeof(float8),FLOAT8PASSBYVAL,'d');
	InsertOrReplaceParameters(modelOid,1,array);
	pfree(values);
	pfree(array);



	/* save trend and level*/
	values = palloc(2*sizeof(*values));
	values[0] = Float8GetDatum(*(model->trend));
	values[1] = Float8GetDatum(*(model->level));

	array = construct_array(values,2,FLOAT8OID,sizeof(float8),FLOAT8PASSBYVAL,'d');
	InsertOrReplaceParameters(modelOid,2,array);

	pfree(values);

	pfree(array);


	/* save seasonal component */
	values = palloc0((model->period*sizeof(*values)));
	for(i=0; i<model->period; i++)
		values[i] = Float8GetDatum(model->season[i]);
	array = construct_array(values,model->period,FLOAT8OID,sizeof(float8),FLOAT8PASSBYVAL,'d');
	InsertOrReplaceParameters(modelOid,3,array);
	pfree(values);
	pfree(array);
}


void reestimateHwModelParameters(PG_FUNCTION_ARGS){
	ModelInfo *model=(ModelInfo *)PG_GETARG_POINTER(0);
	Node *wh=(Node *)PG_GETARG_POINTER(1);
	HWModel* m=(HWModel *)model->model;

	MemoryContext 	old;
	ListCell *lc;
	List 				*helpList = NIL;
	DestReceiver 		*tupledest;
	Portal				portal;

	List *query_list=NIL,*planned_list=NIL,*parsetree_list=NIL;

	old = MemoryContextSwitchTo(m->dataCtx);
	m->data = palloc0(512*sizeof(double));
	m->maxObs = 512;
	m->obsCount = 0;

//	optim_term_maxeval = 1;
		m->super.alpha=0;
		m->super.beta=0;
		m->super.gamma=0;

	MemoryContextSwitchTo(old);

	parsetree_list = pg_parse_query(model->trainingData);
		((SelectStmt*)linitial(parsetree_list))->whereClause=wh;

	query_list = pg_analyze_and_rewrite((Node *)lfirst(list_head(parsetree_list)), model->trainingData, NULL, 0);
	planned_list = pg_plan_queries(query_list, 0, NULL);
	tupledest = CreateModelGraphDestReceiver();
	portal = CreateNewPortal();
	//Don't display the portal in pg_cursors, it is for internal use only
	portal->visible = false;
	PortalDefineQuery(portal, NULL, model->trainingData, "SELECT", planned_list, NULL);
	PortalStart(portal, NULL, InvalidSnapshot);
	(void) PortalRun(portal, FETCH_ALL, false, tupledest, tupledest, NULL);

	helpList = ((ModelGraphState *)tupledest)->tupleList;

	foreach(lc, helpList)
	{
//		if(i<list_length(helpList)-(m->super.period*2)-1)
//			++i;
//		else
			processForecastModel(model, ((Datum *)lfirst(lc))[model->measure->resno-1]);
	}

	model->timestamp = GetDatumAsInt(exprType((Node*) model->time->expr),((Datum*)llast(helpList))[model->time->resno-1]);

	finalizeForecastModel(model);








//	model->sizeOfErrorArray = 0;

//	model->insertCount = 0;


	(*tupledest->rDestroy) (tupledest);
	PortalDrop(portal, false);
	
//XXX CHANGE FOR MEASURING
	model->errorML = 0.0;

	//if table, update parameters
	if(model->storeModel==3 || model->storeModel==13)
	{
		storeModelParameters(model, model->modelOid);
	}
}
//done
void loadhwModelParameters(PG_FUNCTION_ARGS) {
	Datum			*values = NULL;
	int				numvalues = 0;
	HoltWintersModel		*model;
	ArrayType		**array;
	Oid				modelOid;
	int				i;

	model = &(((HWModel *)PG_GETARG_POINTER(0))->super);
	modelOid = PG_GETARG_OID(1);

	array = palloc(3*sizeof(*array));
	RetrieveParameters(modelOid,array,3);

	/* load type and alpha,beta,gamma */
	deconstruct_array(array[0],FLOAT8OID,sizeof(float8),FLOAT8PASSBYVAL,'d',&values,NULL,&numvalues);
	model->doseasonal = DatumGetFloat8(values[0]);
	model->dotrend = DatumGetFloat8(values[1]);
	model->seasonType=DatumGetFloat8(values[2]);
	model->alpha = DatumGetFloat8(values[3]);
	model->beta = DatumGetFloat8(values[4]);
	model->gamma = DatumGetFloat8(values[5]);
	model->period = DatumGetFloat8(values[6]);
	model->tempLevel=0;
	model->tempTrend=0;
	pfree(values);

	/* load trend and level */
	deconstruct_array(array[1],FLOAT8OID,sizeof(float8),FLOAT8PASSBYVAL,'d',&values,NULL,&numvalues);
	model->tempTrend=DatumGetFloat8(values[0]);
	model->tempLevel=DatumGetFloat8(values[1]);
	model->level=palloc0(sizeof(double));
	model->trend=palloc0(sizeof(double));
	*(model->level)=model->tempLevel;
	*(model->trend)=model->tempTrend;
	pfree(values);

	/* load seasonal component*/
	deconstruct_array(array[2],FLOAT8OID,sizeof(float8),FLOAT8PASSBYVAL,'i',&values,NULL,&numvalues);
	model->season = palloc(model->period * sizeof(double));
	model->tempSeason = palloc(model->period * sizeof(double));
	for(i = 0; i<model->period; i++)
		{model->season[i]= DatumGetFloat8(values[i]);
		}
		
	pfree(values);


	pfree(array);

model->scount=model->period;
model->start_time=0;
model->optflag[0]=1;
	model->optflag[1]=1;
	model->optflag[2]=1;
}

void incrementalUpdatehwModel2(PG_FUNCTION_ARGS) {
//	HWModel *forecast_model = ((HWModel *)PG_GETARG_POINTER(0));
//	double *result = (double*)PG_GETARG_POINTER(1);
	int num = PG_GETARG_INT32(1);
	int i;
	for(i=0;i<num;i++)
	{
	//	DirectFunctionCall2(incrementalUpdatehwModel1, PointerGetDatum(forecast_model),Float8GetDatum(result[i]));
	}
	elog(ERROR,"operation not supported.Codekey:iw2hw");
	
}

void incrementalUpdatehwModel1(PG_FUNCTION_ARGS) {
	HoltWintersModel *forecast_model = &(((HWModel *)PG_GETARG_POINTER(0))->super);
	double result = PG_GETARG_FLOAT8(1);
	int timestamp = PG_GETARG_INT32(2);
	ModelInfo *model=(ModelInfo*) PG_GETARG_POINTER(3);
	
	double oldLevel=*(forecast_model->level);
	double stmp= forecast_model->doseasonal == 1 ?  forecast_model->season[0] : (forecast_model->seasonType != 1);
	
	
	
	if (forecast_model->seasonType == 1)
	    *(forecast_model->level) = forecast_model->alpha       * (result - stmp)
		      + (1 - forecast_model->alpha) * (*(forecast_model->level) + *(forecast_model->trend));
	else
	    *(forecast_model->level) = forecast_model->alpha      * (result / stmp)
		      + (1 - forecast_model->alpha) * (*(forecast_model->level) + *(forecast_model->trend));

	/* estimate of trend *in* period i */
	if (forecast_model->dotrend == 1)
	    *(forecast_model->trend) = forecast_model->beta       * (*(forecast_model->level) - oldLevel)
		      + (1 - forecast_model->beta)  *  *(forecast_model->trend) ;

	/* estimate of seasonal component *in* period i */
	if (forecast_model->doseasonal == 1) {
		memmove(forecast_model->season, &(forecast_model->season[1]), sizeof(double)*(forecast_model->period-1));
		if (forecast_model->seasonType == 1)

	    	forecast_model->season[((int)forecast_model->period)-1] = forecast_model->gamma       * (result - *(forecast_model->level))
			   + (1 - forecast_model->gamma) * stmp;
	    else
	    	forecast_model->season[((int)forecast_model->period)-1] = forecast_model->gamma       * (result / *(forecast_model->level))
			   + (1 - forecast_model->gamma) * stmp;
	}

		if(model && (model->storeModel==3 || model->storeModel==13))
	{
		storeModelParameters(model, model->modelOid);
		ReplaceTimeStamp(model->modelOid,timestamp);
	}

	forecast_model->tempLevel=*(forecast_model->level);
	forecast_model->tempTrend=*(forecast_model->trend);
	memcpy(forecast_model->tempSeason,forecast_model->season,sizeof(double)*forecast_model->period);
}




void
finalizehwModel(PG_FUNCTION_ARGS) {

	MemoryContext 	old;
	HWModel 		*model;
	int retv; //returnValue for optimization
	double minf; //the minimum objective value, upon return
	nlopt_opt opt;
	nlopt_opt localopt;
	double diff;
	double avgOnePeriod=0.0;
	double tempLevel,tempTrend;
	double temp=0.0,trend42seasons=0.0;
	int				i,j,optflagCount;
	double *z,*ubns,*lbns,*tempseason;
		clock_t start, end;
	model = (HWModel *)PG_GETARG_POINTER(0);

	optflagCount=model->super.optflag[0]+model->super.optflag[1]+model->super.optflag[2];
	z=palloc0(sizeof(double)*optflagCount); //Variable Array for optimization
	ubns=palloc0(sizeof(double)*optflagCount); //upper bound Array for optimization
	lbns=palloc0(sizeof(double)*optflagCount);//lower bound Array for optimization

	
	old = MemoryContextSwitchTo(model->dataCtx);
	
	
	/***********************************************************************/
	/*Variables for optimization
	 * *********************************************************************/

	model->super.obsCount=model->obsCount;
	if(!model->super.doseasonal && !model->super.dotrend)
		model->super.start_time=2;
	if(!model->super.doseasonal && model->super.dotrend)
		model->super.start_time=3;	
	if(model->super.doseasonal)
		model->super.start_time=model->super.period;	
	if(model->super.doseasonal==0)
		model->super.period=1;
	if(model->super.period<1)
	{
		model->super.period=1;
		elog(WARNING,"Period should be 1 or higher");
	}

	
	
	/***********************************************************************/
	/*Palloc some structures for optimization, 2 trend and level values ẃould be
	 * enough, but by using arrays, we can improve speed and allow better debugging 
	 * *********************************************************************/
	if(model->super.period==0)
	{
		elog(WARNING,"A period of length 0 is not possible, using 1 instead.");
		model->super.period=1;
	}
	model->super.trend=palloc0(((model->obsCount+(int)model->super.period))*sizeof(double));

	model->super.season=palloc0(((model->obsCount+(int)model->super.period))*sizeof(double));
	
	model->super.level=palloc0(((model->obsCount+(int)model->super.period))*sizeof(double));

	model->gradflag=1; //hijacked flag TODO:Perfomance can be increased, by using one Function call for Function+ gradient calculation
	//set upper and lower bound+ startvalues
	

	
	
	
	/***********************************************************************/
	/*Setting lower, uper bounds and starting values					   */
	/***********************************************************************/
	

	for (i = 0; i < optflagCount; i += 1) {
		ubns[i]=1.0-DBL_EPSILON;
		lbns[i]=DBL_EPSILON;
	}
				
	if(model->super.alpha==0) //we are not reestimating, therefore take 0.5 as starting value
	{
		z[0]=0.5;
	}
	else
		z[0]=model->super.alpha;  //reestimating, take the old one
		
	if(model->super.dotrend)
	{
		if(model->super.beta==0) //we are not reestimating, therefore take 0.1 as starting value
		{
			z[1]=0.1;
		}
		else
			z[1]=model->super.beta;
			
		if(model->super.doseasonal)
		{
			if(model->super.gamma==0) //we are not reestimating, therefore take 0.5 as starting value
			{
				z[2]=0.5;
			}
			else
				z[2]=model->super.gamma;
		}
	}
	else //no trend to calculate
	{
		if(model->super.doseasonal)
		{
			if(model->super.gamma==0) //we are not reestimating, therefore take 0.5 as starting value
			{
				z[1]=0.5;
			}
			else
				z[1]=model->super.gamma;
		}
	}
	
	
	/***********************************************************************/
	/*Calculate starting values for trend,season and level					*/
	/* *********************************************************************/



	/***********************************************************************/
	/*Start trend													       */
	/* *********************************************************************/
	
	if(model->super.dotrend)
	{		
		if(model->obsCount<=model->super.period*2)
		{
			elog(ERROR,"not enough observations. CODEKEY:finalizehw1");
		}

//		j=2*model->super.period;
//		int periodCounter;
//		for(periodCounter=0;periodCounter<model->super.period;periodCounter++)
//		{
//			oldperiod+=model->data[j-periodCounter-1];	//new: -1
//		}
//
//		oldperiod=oldperiod/model->super.period;
//		for(;periodCounter<2*model->super.period;periodCounter++)
//		{
//			olderperiod+=model->data[j-periodCounter-1];	//new: -1
//		}
//		olderperiod=olderperiod/model->super.period;
//
//		trend42seasons=(oldperiod-olderperiod)/model->super.period;
/////////////////////////////////////////////////////////////////////////
//		for(j=0;j<model->super.period;++j){
//			olderperiod+=model->data[j];
//		}
//		olderperiod /= model->super.period;
//
//		for(;j<model->super.period*2;++j){
//			oldperiod+=model->data[j];
//		}
//		oldperiod /= model->super.period;
//
//		trend42seasons=(oldperiod-olderperiod)/model->super.period;
/////////////////////////////////////////////////////////////////////////
		for(j=0;j<model->super.period*2;++j){
			trend42seasons+=model->data[j+1]-model->data[j];
		}
		trend42seasons/=(model->super.period*2)-1;
		model->super.b=trend42seasons;
		
			
	}

	/***********************************************************************/
	/*Start level														   */
	/* *********************************************************************/
	
		for(j=0;j<model->super.period;j++)
		{
			temp=temp+model->data[j];
		}
		avgOnePeriod=temp/model->super.period;
		model->super.a=avgOnePeriod;
	
	
	/***********************************************************************/
	/*Start season														   */
	/* *********************************************************************/
	
	//set model super s
	if(model->super.doseasonal) 
	{
		if(model->super.seasonType)
		{
			//additive
			for(i=0;i<model->super.period;i++)//anzahl periodenlänge viele
			{
				model->super.season[i]=model->data[i]-(avgOnePeriod+(trend42seasons/2)*i);
			}
		}
		else
		{
			//multiplicative
			for(i=0;i<model->super.period;i++)
			{
				model->super.season[i]=(model->data[i]-((trend42seasons/2)*i))/avgOnePeriod;
			}
		}
//		elog(INFO,"Got a level value of: %f",model->super.a);
//		elog(INFO,"Got a trend value of: %f",model->super.b);
//		for(i=0;i<model->super.period;i++)
//		{
//			elog(INFO,"Got a season value of: %f",model->super.season[i]);
//		}
	}
	
	
	
	/***********************************************************************/
	/*Initialize optimizer and set properties							   */
	/* *********************************************************************/
	
	
	opt = nlopt_create(getOptimMethod(optim_method_general), optflagCount); 

	localopt = nlopt_create(getOptimMethod(optim_method_local), optflagCount);
	//some Algorithms dislike bounds, therefor we have to check before setting them
	if(optim_method_general!=8 &&optim_method_general!=9) {
		nlopt_set_upper_bounds(opt, ubns);
		nlopt_set_lower_bounds(opt, lbns);
	}
	nlopt_set_local_optimizer(opt,localopt);
	if(model->super.errorfunction==NULL) model->super.errorfunction="DEFAULT";

	if(strcmp(model->super.errorfunction,"SMAPE")==0)
	{
		model->super.optflag[3]=0;
	}
	else if(strcmp(model->super.errorfunction,"SSE")==0)
	{
		model->super.optflag[3]=1;
	}
	else{
		model->super.optflag[3]=2;
	}
	if(optflagCount==3) //optimize all three
		nlopt_set_min_objective(opt, (nlopt_func)exp3Var, (void*)model);
	if(optflagCount==2) //optimize all two
		nlopt_set_min_objective(opt,  (nlopt_func)exp2Var, (void*)model);
	if(optflagCount==1) //optimize level only
		nlopt_set_min_objective(opt,  (nlopt_func)exp1Var, (void*)model);
		
		
	if(optim_term_maxtime)
		nlopt_set_maxtime(opt, optim_term_maxtime);
	if(optim_term_ftol_abs)
		nlopt_set_ftol_abs(opt,optim_term_ftol_abs);
	if(optim_term_ftol_rel)
		nlopt_set_ftol_rel(opt,optim_term_ftol_rel);
	if(optim_term_xtol_rel)
		nlopt_set_xtol_rel(opt,optim_term_xtol_rel);
	if(optim_term_xtol_abs)
		nlopt_set_xtol_abs1(opt,optim_term_xtol_abs);
	if(optim_term_maxeval)
		nlopt_set_maxeval(opt,optim_term_maxeval);
			
	/***********************************************************************
	 * Start optimization and check result
	 ***********************************************************************/

	if(optflagCount){
		start=clock();
		retv=nlopt_optimize(opt, z, &minf);
		end=clock();
	}else{
		start=clock();
		minf=exp1Var(1,z,NULL,model); //XXX: gradflag=1
		retv=1;
		end=clock();
	}
	diff=(double)(end-start)/CLOCKS_PER_SEC;
	elog(INFO,"Optimization time: %f",diff);
	//This happens due to several reasons, anyway this is the only Output the LBFGS Routine gives us
	if (retv < 0 && optim_method_general==10) {
		elog(WARNING,"nlopt  of non seasonal part failed with code: %i, LBFGS routine cannot reach terminiation criteria, anyway the solution may be acceptable.. Current val of f was:%f",retv,minf);
	} else if(retv<0)
		elog(WARNING,"nlopt of non seasonal part  failed with code: %i, Current val of f was:%f",retv,minf);
	else {
		if(retv==6)
			elog(INFO,"found minimum for non-seasonal part at f = %0.10g and stopped with maxtime reached\n", minf);
		else if(retv==5)
			elog(INFO,"found minimum for non-seasonal part at f = %0.10g and stopped with maxEval reached\n",  minf);
		else if(retv==4)
			elog(INFO,"found minimum for non-seasonal part at f = %0.10g and stopped with xtol reached\n", minf);
		else if(retv==3)
			elog(INFO,"found minimum for non-seasonal part at f = %0.10g and stopped with ftol reached\n", minf);
		else if(retv==2)
			elog(INFO,"found minimum for non-seasonal part at f = %0.10g and stopped with stopVal reached\n", minf);
		else if(retv==1)
			elog(INFO,"found minimum for non-seasonal part at f = %0.10g\n", minf);


//		StringInfo s = makeStringInfo();
//		appendStringInfo(s, "/home/hartmann/Desktop/BelegSVN/ClaudioHartmann/data/Potentialanalyse/modelParameter.csv");
//
//		StringInfo data = makeStringInfo();
//		appendStringInfo(data,"%lf\t",minf);
//		printDebug(s->data,data->data);
	}

	nlopt_destroy(opt);
	nlopt_destroy(localopt);

	//check solution, There are some effects can give hints about optimization problems
	for(i=0; i<3; i++) {
		if(z[i]<1e-13 && retv==6) {
			elog(WARNING,"Please check solution for, maybe you should rise max_time or change the algorithm");
				break;
		}
		if(z[i]/1.00 == (int)z[i] && z[i]!=0) {
			elog(WARNING,"Please check solution for, maybe you should rise max_time or change the algorithm");
			break;
		}
	}
	
	/***********************************************************************/
	/*Store optimization result											   */
	/* *********************************************************************/

	//save alpha
	model->super.alpha= Max(Min(z[0],1.0-DBL_EPSILON),DBL_EPSILON); //For some optimization routines we have to fight against numerical issues, therefor take the minimum to force the values to be <= 1
	if(model->super.dotrend)
	{
		model->super.beta=Max(Min(z[1],1.0-DBL_EPSILON),DBL_EPSILON);
		if(model->super.doseasonal)
			model->super.gamma=Max(Min(z[2],1.0-DBL_EPSILON),DBL_EPSILON);
	}	
	else
	{
		if(model->super.doseasonal)
			model->super.gamma=Max(Min(z[1],1.0-DBL_EPSILON),DBL_EPSILON);
	}
	

	tempLevel=model->super.level[model->obsCount -1 - model->super.start_time+1];
	tempTrend=model->super.trend[model->obsCount -1 - model->super.start_time+1];
	pfree(model->super.level);
	pfree(model->super.trend);
	model->super.trend=palloc0(1*sizeof(double));
	model->super.level=palloc0(1*sizeof(double));
	model->super.trend[0]=tempTrend;
	model->super.level[0]=tempLevel;
	model->super.tempTrend=tempTrend;
	model->super.tempLevel=tempLevel;
//	model->super.optflag[0]=0;
//	model->super.optflag[1]=0;
//	model->super.optflag[2]=0;
	
//	StringInfo s = makeStringInfo();
//	appendStringInfo(s, "/home/hartmann/Desktop/BelegSVN/ClaudioHartmann/data/Potentialanalyse/modelParameter.csv");
//
//	StringInfo data = makeStringInfo();
//	appendStringInfo(data, "%lf\t", minf);
//	printDebug(s->data,data->data);

	tempseason=palloc0(sizeof(double)*(int)(model->super.period));
	for(i=0;i<model->super.period;i++)
	{
		tempseason[i]=model->super.season[model->obsCount-model->super.start_time+i];
	}
	pfree(model->super.season);
	model->super.season=palloc0(model->super.period*sizeof(double));
	model->super.tempSeason=palloc0(model->super.period*sizeof(double));
	for(i=0;i<model->super.period;i++)
	{
		model->super.season[i]=tempseason[i];
		model->super.tempSeason[i]=tempseason[i];
	}
	pfree(model->data);

	pfree(z);
	pfree(ubns);
	pfree(lbns);
	pfree(tempseason);

	model->super.scount=model->super.period;

	MemoryContextSwitchTo(old);
}
