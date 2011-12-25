/*
 * modelGraphNodes.h
 *
 *  Created on: 02.03.2011
 *      Author: b1anchi
 */

#ifndef MODELGRAPHNODES_H_
#define MODELGRAPHNODES_H_

#include "postgres.h"
#include "nodes/nodes.h"
#include "nodes/pg_list.h"
#include "nodes/ArList.h"
#include "nodes/value.h"
#include "tcop/dest.h"
#include "forecast/algorithm.h"

typedef struct ModelGraphState{
	DestReceiver 			pub;
	List					*tupleList;
	List					*isnullList;
	struct RangeTblEntry	*rte;
	TupleDesc				tDesc;
}ModelGraphState;

typedef struct GraphAttribute{

	Node	*attributeCol;

	List	*excludeList;
} GraphAttribute;

typedef struct CorrAttribute{

	Datum		dat;

	TupleDesc	tDesc;

	List		*tupleList;
} CorrAttribute;

typedef struct DisAggModel{

	ModelInfo	*model;
	bool		updatable;
	double		givenDisAggKey;
	double		disAggKeyNumerator;

	double			errorSMAPE;
	double			errorSSE;
	double			errorML;

	int				lowerBound;
	int				upperBound;
	int				sizeOfErrorArray;
	double*			errorArray;
} DisAggModel;


typedef struct ModelGraphIndexNode{

	NodeTag						type;

	TargetEntry					*target;
	Datum						value;

	ArList						*children;
	ArList						*parents;
	struct ModelGraphIndexNode	*aggChild;

	List						*targetEntryList; //contains all TargetEntries of the lower levels of the graph(for compatibilitychecks of queries)

	List 						*models; //all Models that belong to this Node
	List 						*aggModels; //Array with Arrays of Models to be aggregated

	List		 				*disAggModels; //Array with DisAggregationModels

	int							aggIndicator;
	int							id;

	Datum						*lastInsertedTuple;

} ModelGraphIndexNode;

typedef struct SearchPathNode{
	ModelGraphIndexNode	*mgin;
	Node 				*whreExpr;
} SearchPathNode;

typedef struct WorkloadElement{
	ModelGraphIndexNode	*mgin;
	List				*horizons;

	ModelInfo			*modelInfo; //the model for this WorkloadElement
	Datum				*measureValues; //all measureValues of the corresponding timeseries
	int					sizeOfMeasureValues;

	//both Arrays are ordered like the Workload AND POSSIBLY CONTAIN ZREO-VALUES
	double				*keyNumerators; //all disAggKeys for ALL WorkloadElemnts of the Workload
	double				*disAggErrors; //disAggErrors corresponding to the disAggKeys above
	double				*disAggErrorForLaterUse;

	bool				isInBestConf;
}WorkloadElement;

typedef struct ModelGraphIndex{
	ModelInfo** 			modellist;
	int					maxid;
	int					modellistMaxSize;
	int					models;
	int*				staging;
	int					stagingmaxSize;
	ModelGraphIndexNode **leafArray;
}ModelGraphIndex;


#define foreachModelInMG(modelinfo)	\
	int i; for (i=0; (modelinfo) = modelGraphIdx->modellist[i]; i<modelGraphIdx->models; i++)

typedef struct Horizon{
	int 	horizon;
	double	frequency;
	bool	stillCheck;
	double	error;
}Horizon;
#endif /* MODELGRAPHNODES_H_ */
