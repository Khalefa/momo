/*--------------------------------------------------------------------------
 * algorithm.h
 *	  header file for forecast algorithm interface.
 *
 *
 *	$PostgreSQL: pgsql/src/include/forecast/algorithm.h,v 1.34 2009/06/11 14:49:08 momjian Exp $
 *--------------------------------------------------------------------------
 */
#ifndef ALGORITHM_H
#define ALGORITHM_H

#include "postgres.h"
#include "fmgr.h"
#include "nodes/nodes.h"
#include "nodes/primnodes.h"
#include "executor/tuptable.h"
#include <nlopt.h>
#include "utils/snapshot.h"


typedef enum ModelType {
	Undefined = 0,
	Medium = 1,
	R = 2,
	LinReg = 3,
	HwModel = 4,
	GretlArima = 5,
	ArModel = 6
} ModelType;

typedef enum Granularity {
	day,
	week,
	month,
	quarter,
	year
} Granularity;



 
 
typedef struct AlgorithmInfoData
{
	const char		*algName;				/* name of the algorithm */
	FmgrInfo		algInitModel;
	FmgrInfo		algLoadModelParameters;
	FmgrInfo		algProcessForecast;
	FmgrInfo		algFinalizeForecastModel;
	FmgrInfo		algStoreModelParameters;
	FmgrInfo		algGetNextValue;
	FmgrInfo		algIncrementalUpdate;
	FmgrInfo		algIncrementalUpdate2;
	FmgrInfo		algReestimateUpdate;
}AlgorithmInfoData;

typedef struct AlgorithmInfoData *AlgorithmInfo;


typedef struct Model
{
	NodeTag		type;
	int			trainingTupleCount;
} Model;


typedef struct ModelInfo
{
	NodeTag			type;
	
	// forecast method
	ModelType		forecastMethod;
	
	// optional name of this model
	const char		*modelName;
	
	// meta information
	TargetEntry		*time;
	TargetEntry		*measure;
	Oid				aggType;
	Granularity		granularity;
	
	// timestamp until this model was built
	int				timestamp;

	// associated model parameters
	Model			*model;
	
	// number of values to forecast
	int				numForecastValues;
	
	// needs this model to be build?
	bool			buildModel;
	
	// where to store this model; 0: no storage, 1: model index (main memory), 2: hash table(main memory), 3: system table
	short			storeModel;
	
	// storage specific information; pointer to node in model index OF in the modelgraph
	Node			*mix;
	
	// temporary
	Oid				modelOid;
	
	// A generic List of parsed Parameters
	List			*parameterList;

	// The Info to the algorithm which created this model
	AlgorithmInfo	algInfo;

	char			*trainingData;
	double			disaggkey; //used in the executor

	double			disAggKeyDenominator;

	double			errorSMAPE;
	double			errorSSE;
	double			errorML;

	double**		otherErrors;

	int				lowerBound;
	int				upperBound;
	int				sizeOfErrorArray;
	double*			errorArray;

	int 			insertCount;
} ModelInfo;


typedef struct MediumModel
{
	Model			model;
	int				count;
	double			sum;
} MediumModel;


typedef struct RModel
{
	Model			model;
	
	char*			functionName;
	int				numForecastValues;
	Datum			*newvalues;
	
	// used for value storage
	Datum	 	*memvalues;		/* array of pointers to palloc'd values */
	int			memtupcount;	/* number of values currently present */
	int			memtupsize;		/* allocated length of memvalues array */
	long		availMem;		/* remaining memory available, in bytes */
} RModel;


typedef struct LinRegModel
{
	Model			model;
	double			yTotal;
	double			xyTotal;
	double			count;
	double			a;
	double			b;
} LinRegModel;


typedef struct AdditiveDec
{
	Model		model;
	int			windowSize;
	int			period;
	int			currentPos;
	double		*sums;
	short		status;
	short		even;

	int			tupcount;
	double	 	*values;
	double		valuessize;
	double		*trend;
	double		trendsize;
	double		*season;
	double		*rest;
} AdditiveDec;


typedef double(*ModelMergeStrategy)(List*,int);

/*
 *
 * Interface definition for the forecast algorithms. Every Forecast Algorithm has to implement
 * these methods and has to register their implementation in the system catalog (pg_proc and pg_fcalg)
 *
 */
extern TupleTableSlot *BuildModelInfoTupleTableSlot(ModelInfo *modelInfo);
extern ModelMergeStrategy getMergeStrat(int stratIdentifier);
extern double firstCandidate(List* candidates,int current);
extern double lastCandidate(List* candidates,int current);
extern double averageCand(List* candidates,int current);
extern double sumCand(List* candidates,int current);
extern double sumWithDisaggCand(List* candidates,int current);
extern double modelGraphSum(List* candidates,int current);
extern ModelInfo *initModelInfo(const char *algName,const char *modelName);
extern void initForecastModel(ModelInfo *modelInfo, MemoryContext memoryContext);
extern void processForecastModel(ModelInfo *model, Datum value);
extern void finalizeForecastModel(ModelInfo *model);
extern void storeModelParameters(ModelInfo *model, Oid modelOid);
extern void loadModelParameters(ModelInfo *model, Oid modelOid);
extern double getNextValue(ModelInfo *model, int num);
extern void incrementalUpdate(ModelInfo *modelInfo, double value, int timestamp);
extern void reestimateParameters(ModelInfo *modelInfo, Node *whereExpr);

extern ModelType getStringAsModelType(char* type);

extern char* getModelTypeAsString(ModelType type);

extern char* getGranularityAsString(Granularity type);
extern Datum GetDoubleAsDatum(Oid source, double value);
extern Datum GetIntAsDatum(Oid source, int value);
extern double GetDatumAsDouble(Oid source, Datum value);
extern int GetDatumAsInt(Oid source, Datum value);

extern double SSE(double xhat, double x);
extern double SMAPE(double xhat, double x);
extern double ABS(double xhat, double x);

void printDebug(const char * filePath, char *message);
extern nlopt_algorithm getOptimMethod(int value);
/*
 *
 * Utility Methods
 *
 */
extern AlgorithmInfo initAlgorithmInfo(const char *algName);


#endif   /* ALGORITHM_H */
