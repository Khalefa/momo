/*-------------------------------------------------------------------------
 *
 * pg_model.c
 *	  routines to support manipulation of the pg_model relation
 *
 * Portions Copyright (c) 1996-2009, PostgreSQL Global Development Group
 * Portions Copyright (c) 1994, Regents of the University of California
 *
 *
 * IDENTIFICATION
 *	  $PostgreSQL: pgsql/src/backend/catalog/pg_model.c,v 1.0 2009/09/14$
 *
 *-------------------------------------------------------------------------
 */
#include "postgres.h"
#include "funcapi.h"

#include "catalog/catalog.h"
#include "catalog/indexing.h"
#include "catalog/pg_model.h"
#include "utils/array.h"
#include "utils/date.h"
#include "utils/builtins.h"
#include "utils/tqual.h"
#include "utils/syscache.h"
#include "utils/fmgroids.h"
#include "catalog/pg_tablespace.h"
#include "catalog/pg_namespace.h"
#include "catalog/pg_type.h"


void
ReplaceTimeStamp(Oid model,
				int timestamp)
{
	Relation 	mrel;
	ScanKeyData scanoid[1];


	HeapTuple 	tuple;
	HeapScanDesc result;
	Datum				*values;
	bool				*isNull;

	// open parameter relation
	mrel = heap_open(ModelRelationId, RowExclusiveLock);

	ScanKeyInit(&scanoid[1],1,InvalidStrategy,F_OIDEQ,model);
	result= heap_beginscan(mrel,SnapshotNow,1,scanoid);

	if((tuple = heap_getnext(result, ForwardScanDirection)) != NULL)
	{

		values = (Datum *) palloc(RelationGetDescr(mrel)->natts * sizeof(Datum));
		isNull = (bool *) palloc(RelationGetDescr(mrel)->natts * sizeof(bool));
		heap_deform_tuple(tuple, RelationGetDescr(mrel), values, isNull);
		values[9]=timestamp;
		tuple=heap_form_tuple(RelationGetDescr(mrel),values,isNull);
		simple_heap_update(mrel,(ItemPointer)(&(tuple->t_self)),tuple);
		pfree(values);
		pfree(isNull);
	}
	heap_endscan(result);


	// Update indexes
	CatalogUpdateIndexes(mrel, tuple);

	heap_close(mrel, RowExclusiveLock);

}



/**
 * Creates a name for this model
 */
void
SetModelName(ModelInfo *model)
{
	HeapTuple		sourceTable;
	Form_pg_class 	sourceTableDesc;
	Relation 	mrel;
	Oid			moid;
	char buffer [255];
	
	model->modelName = (char*) palloc(NAMEDATALEN * sizeof(char));
	
	sourceTable = SearchSysCache(RELOID, ObjectIdGetDatum(model->measure->resorigtbl), 0, 0, 0);
	sourceTableDesc = (Form_pg_class) GETSTRUCT(sourceTable);
	
	strcpy((char*)model->modelName, (const char*)NameStr(sourceTableDesc->relname));
	strcat((char*)model->modelName, (const char*)"_");
	strncat((char*)model->modelName, (const char*)model->measure->resname, NAMEDATALEN-strlen(model->modelName));
	strcat((char*)model->modelName, (const char*)"_");
	strncat((char*)model->modelName, (const char*)getModelTypeAsString(model->forecastMethod), NAMEDATALEN-strlen(model->modelName));
	mrel = heap_open(6001, RowExclusiveLock);
	moid = GetNewOid(mrel);
	heap_close(mrel, RowExclusiveLock);
	
	snprintf (buffer, (size_t)255, "%i", (uint)moid);
	strcat((char*)model->modelName, (const char*)buffer);
	ReleaseSysCache(sourceTable);
}

/**
 * Stores a forecast model in the system catalog.
 */
Oid
InsertForecastModel(const char *modelName,
					TargetEntry *columnReferenceT,
					TargetEntry *columnReferenceX,
					char *algorithm,
					int16 granularity,
					Oid aggType,
					Node *whereExpr,
					bool implicit,
					const char *sql,
					bool enable,
					int timestamp,
					double forecast)
{
	Datum 			pgValues[Natts_pg_model];
	bool 			pgNulls[Natts_pg_model];
	Relation 		mrel;
	Oid				moid;
	HeapTuple 		pgTuple;

	// open model relation
	mrel = heap_open(ModelRelationId, RowExclusiveLock);

	// generate new unique model oid
	moid = GetNewOid(mrel);
	
	// Build the new pg_model tuple.
	memset(pgNulls, false, sizeof(pgNulls));

	// look if model already exists
	pgTuple = SearchSysCache(MODELNAME, PointerGetDatum(modelName), 0,0,0);
	if(HeapTupleIsValid(pgTuple))
	{
		ReleaseSysCache(pgTuple);
		ereport(ERROR, (errcode(ERRCODE_DUPLICATE_OBJECT), errmsg("model \"%s\" already exists", modelName)));
	}
	pgValues[Anum_pg_model_name - 1] = 	DirectFunctionCall1(namein, CStringGetDatum(modelName));
	
	// store measure column
	pgValues[Anum_pg_model_relid - 1] = ObjectIdGetDatum(columnReferenceX->resorigtbl);


	pgValues[Anum_pg_model_time - 1] = Int16GetDatum(columnReferenceT->resorigcol);
	pgValues[Anum_pg_model_dimension - 1] = Int16GetDatum(columnReferenceX->resorigcol);
	pgValues[Anum_pg_model_algorithm - 1] = DirectFunctionCall1(namein, CStringGetDatum(algorithm));
	pgValues[Anum_pg_model_aggtype - 1] = ObjectIdGetDatum(aggType);
	pgValues[Anum_pg_model_sql - 1] = CStringGetTextDatum(sql);
	pgValues[Anum_pg_model_granularity - 1] = Int16GetDatum(granularity);

	if (whereExpr == NULL)
	{
		pgNulls[Anum_pg_model_constraints - 1] = CStringGetTextDatum("");
	} else {
		pgValues[Anum_pg_model_constraints - 1] = CStringGetTextDatum(nodeToString(whereExpr));
	}

	pgValues[Anum_pg_model_implicit - 1] = BoolGetDatum(implicit);
	
	pgValues[Anum_pg_model_enable - 1] = BoolGetDatum(enable);
	
	pgValues[Anum_pg_model_timestamp - 1] = Int32GetDatum(timestamp);
	pgValues[Anum_pg_model_forecast - 1] = Float8GetDatum(forecast);
	pgValues[Anum_pg_model_ecount - 1] = Float8GetDatum(0.0);
	pgValues[Anum_pg_model_eval - 1] = Float8GetDatum(0.0);

	// create tuple
	pgTuple = heap_form_tuple(mrel->rd_att, pgValues, pgNulls);

	// force tuple to have the desired OID
	HeapTupleSetOid(pgTuple, moid);
	
	// Insert tuple into pg_model
	simple_heap_insert(mrel, pgTuple);

	// Update indexes
	CatalogUpdateIndexes(mrel, pgTuple);

	// Release memory
	heap_freetuple(pgTuple);

	// Close relation
	heap_close(mrel, RowExclusiveLock);

	return moid;
}




/**
 * Model Matching:
 * Finds for the given column a List of existing models.
 *
 * Open aspects:
 * - Granularity
 * - Aggregation
 * - Model Rewriting (usage of existing models with different granularity, different dimensional constraints etc.)
 *
 */
List*
FindModel(Node *whereExpr, ModelInfo *model,char* targetDateString)
{
	Relation		relModel;
	HeapScanDesc 	scanModel;
	HeapTuple		tuple;
	FromExpr 		*node = NULL;
	List*			possibleModels=NIL;
	ListCell*		lc;
	bool 			paraFound=false;
	Datum			*values = NULL;
	int				numvalues = 0;
	ArrayType		**array;
	bool				*isNull;
	AlgorithmParameter *findPar;
	ModelInfo *newM;
	Datum				*parvalues;
	//If we get a targetDate, we will check our found models in place
	Datum targetDate=0;
	if(targetDateString!=NULL)
		targetDate = DirectFunctionCall1(date_in, CStringGetDatum(targetDateString));

	
	// open model relation and begin scan
	relModel = heap_open(ModelRelationId, AccessShareLock);
	scanModel = heap_beginscan(relModel, SnapshotNow, 0, NULL);

	// search the system table via heap scan
	while ((tuple = heap_getnext(scanModel, ForwardScanDirection)) != NULL)
	{
			Form_pg_model modeltuple = (Form_pg_model) GETSTRUCT(tuple);

			
			values = (Datum *) palloc(RelationGetDescr(relModel)->natts * sizeof(Datum));
			isNull = (bool *) palloc0(RelationGetDescr(relModel)->natts * sizeof(bool));
			heap_deform_tuple(tuple, RelationGetDescr(relModel), values, isNull);
			
			
			// check first if t and x column are the same
			// check also if we have constraints on the model to use
			if ((modeltuple->table == model->measure->resorigtbl) &&
					(modeltuple->time == model->time->resorigcol) &&
					(modeltuple->measure == model->measure->resorigcol) &&
					((getStringAsModelType((char*)NameStr(modeltuple->algorithm)) == model->forecastMethod) || (model->forecastMethod == Undefined)) &&
					(modeltuple->granularity == model->granularity) &&
					(modeltuple->aggtype == model->aggType) &&
					(modeltuple->enable == true)
				)
			{
				// now check for any dimensional restrictions
				if (!isNull[13]) {
					node = (FromExpr*) stringToNode(TextDatumGetCString(DatumGetPointer(values[13])));
				}
				if ((node==NULL && whereExpr==NULL)|| equal(node,whereExpr)) 
				{
					
					//check parameters if existing
						if(model->forecastMethod==GretlArima || model->forecastMethod==ArModel)
						{
							array = palloc(10*sizeof(*array));
							RetrieveParameters(HeapTupleGetOid(tuple),array,5);
							deconstruct_array(array[0],INT4OID,sizeof(int4),true,'i',&parvalues,NULL,&numvalues);
							if(model->parameterList!=NIL)
							{
							foreach(lc, model->parameterList)
							{
								paraFound=false;
								findPar = (AlgorithmParameter*) lfirst(lc);
								//compare parameters
								if(strcmp(findPar->key,"ma")==0 && (int)(findPar->value->val.val.ival) == DatumGetInt32(parvalues[3]))
								{
									paraFound=true;
									continue;
								}
								if(strcmp(findPar->key,"ar")==0 && (int)(findPar->value->val.val.ival) == DatumGetInt32(parvalues[1]))
								{
									paraFound=true;
									continue;
								}
								if(strcmp(findPar->key,"sma")==0 && (int)(findPar->value->val.val.ival) == DatumGetInt32(parvalues[4]))
								{
									paraFound=true;
									continue;
								}
								if(strcmp(findPar->key,"sar")==0 && (int)(findPar->value->val.val.ival) == DatumGetInt32(parvalues[2]))
								{
									paraFound=true;
									continue;
								}
								break; //para not found
							}
							}
							else
							{
								paraFound=true;
							}
							if(paraFound)
							{
								//check date if needed
								if(targetDate && (DatumGetBool(DirectFunctionCall2(date_le, targetDate, model->timestamp)))) //targetDate is not prospective enough
									continue;
								//add a new ModelInfoNode as possible Model
								newM = (ModelInfo*)palloc(sizeof(ModelInfo));
								newM=memcpy(newM,model,sizeof(ModelInfo));
								newM->modelName=(char*)palloc0((strlen((char*)values[0])+1)*sizeof(char));
								newM->timestamp=DatumGetInt32(values[9]);
								strcpy((char*)newM->modelName,(const char*)values[0]);
								newM->modelOid=HeapTupleGetOid(tuple);
								newM->algInfo = initAlgorithmInfo(getModelTypeAsString(newM->forecastMethod));
								possibleModels=lappend(possibleModels,newM);

							}

						}
						else{
							//check date if needed
							if(targetDate && (DatumGetBool(DirectFunctionCall2(date_le, targetDate, model->timestamp)))) //targetDate is not prospective enough
								continue;
							//add a new ModelInfoNode as possible Model
							newM = (ModelInfo*)palloc(sizeof(ModelInfo));

							newM=memcpy(newM,model,sizeof(ModelInfo));
							if(strcmp(getModelTypeAsString(newM->forecastMethod), "undef")==0){
								newM->forecastMethod = getStringAsModelType((char*)values[4]);
								newM->algInfo = initAlgorithmInfo((const char*)values[4]);
							}
							newM->modelName=(char*)palloc0((strlen((char*)values[0])+1)*sizeof(char));
							newM->timestamp=DatumGetInt32(values[9]);
							strcpy((char*)newM->modelName,(const char*)values[0]);

							newM->modelOid=HeapTupleGetOid(tuple);
							newM->algInfo = initAlgorithmInfo(getModelTypeAsString(newM->forecastMethod));
							possibleModels=lappend(possibleModels,newM);
						}
					}
				}
					pfree(values);
	pfree(isNull);
			}
	
	// end scan
	heap_endscan(scanModel);
	heap_close(relModel, AccessShareLock);

	
	return possibleModels;
}
