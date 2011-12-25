/*-------------------------------------------------------------------------
 *
 * pg_parameter.c
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
#include "catalog/pg_parameter.h"
#include "utils/builtins.h"
#include "utils/fmgroids.h"
#include "utils/tqual.h"


/**
 * Stores a forecast model in the modelgraph backuprelation .
 */
void
InsertOrReplaceParametersMg(Oid model,Relation mrel,
				int parameter,
				ArrayType *array)
{
	Datum 		pgValues[Natts_pg_parameter];
	bool 		pgNulls[Natts_pg_parameter];
	ScanKeyData scanoid[2];
	HeapTuple 	pgTuple;
	HeapTuple 	tuple;
	bool 		updated = false;
	HeapScanDesc result;

	// Build the new pg_model tuple.
	memset(pgNulls, false, sizeof(pgNulls));

	pgValues[Anum_pg_parameter_model - 1] = ObjectIdGetDatum(model);
	pgValues[Anum_pg_parameter_name - 1] = Int16GetDatum(parameter);
	pgValues[Anum_pg_parameter_value - 1] = PointerGetDatum(array);

	// create tuple
	pgTuple = heap_form_tuple(mrel->rd_att, pgValues, pgNulls);
	

	
	ScanKeyInit(&scanoid[0],1,InvalidStrategy,F_OIDEQ,model);
	ScanKeyInit(&scanoid[1],2,InvalidStrategy,F_INT4EQ,parameter);
	result= heap_beginscan(mrel,SnapshotNow,2,scanoid);

	if((tuple = heap_getnext(result, ForwardScanDirection)) != NULL)
	{
		Form_pg_parameter modeltuple = (Form_pg_parameter) GETSTRUCT(tuple);
		Assert(((uint)modeltuple->model)==((uint)model) && modeltuple->parameter==parameter);
		simple_heap_update(mrel,(ItemPointer)(&(tuple->t_self)),pgTuple);
		updated = true;
	}
	heap_endscan(result);

	if(!updated){
		// Insert tuple into pg_model
		simple_heap_insert(mrel, pgTuple);
	}

	// Update indexes
	CatalogUpdateIndexes(mrel, pgTuple);

	heap_freetuple(pgTuple);			
}


/**
 * Stores a forecast model in the system catalog.
 */
Oid
InsertOrReplaceParameters(Oid model,
				int parameter,
				ArrayType *array)
{
	Datum 		pgValues[Natts_pg_parameter];
	bool 		pgNulls[Natts_pg_parameter];
	Relation 	mrel;
	Oid			moid;
	HeapTuple 	pgTuple;
	HeapTuple 	tuple;
	bool 		updated = false;
	ScanKeyData scanoid[2];
	HeapScanDesc result;

	// open parameter relation
	mrel = heap_open(ParameterRelationId, RowExclusiveLock);

	// generate new unique model oid
	moid = GetNewOid(mrel);

	// Build the new pg_model tuple.
	memset(pgNulls, false, sizeof(pgNulls));

	pgValues[Anum_pg_parameter_model - 1] = ObjectIdGetDatum(model);
	pgValues[Anum_pg_parameter_name - 1] = Int16GetDatum(parameter);
	pgValues[Anum_pg_parameter_value - 1] = PointerGetDatum(array);

	// create tuple
	pgTuple = heap_form_tuple(mrel->rd_att, pgValues, pgNulls);
	
	// force tuple to have the desired OID
	HeapTupleSetOid(pgTuple, moid);

	
	ScanKeyInit(&scanoid[0],1,InvalidStrategy,F_OIDEQ,model);
	ScanKeyInit(&scanoid[1],2,InvalidStrategy,F_INT4EQ,parameter);
	result= heap_beginscan(mrel,SnapshotNow,2,scanoid);

	if((tuple = heap_getnext(result, ForwardScanDirection)) != NULL)
	{
		Form_pg_parameter modeltuple = (Form_pg_parameter) GETSTRUCT(tuple);
		Assert(((uint)modeltuple->model)==((uint)model) && modeltuple->parameter==parameter);
		simple_heap_update(mrel,(ItemPointer)(&(tuple->t_self)),pgTuple);
		updated = true;
	}
	heap_endscan(result);

	if(!updated){
		// Insert tuple into pg_model
		simple_heap_insert(mrel, pgTuple);
	}

	// Update indexes
	CatalogUpdateIndexes(mrel, pgTuple);

	heap_freetuple(pgTuple);
	heap_close(mrel, RowExclusiveLock);

	return 1;
}

void
RetrieveParameters(Oid modelOid, ArrayType **parameters, int numParameters)
{

	Relation 			rel;
	ScanKeyData			scankey;
	SysScanDesc 		scan;
	//HeapTuple			tuple;
	//Form_pg_parameter 	parameterTuple;
	Datum				datum;
	bool				isnull;
	int					i;
	Form_pg_parameter parameterTuple;

	// open parameter relation and index
	rel = heap_open(ParameterRelationId, RowExclusiveLock);

	ScanKeyInit(&scankey,
				Anum_pg_parameter_model,
				BTEqualStrategyNumber, F_OIDEQ,
				ObjectIdGetDatum(modelOid));

	// TODO: Use an index instad of a sequential scan
	scan = systable_beginscan(rel, ParameterModelOidIndexId, false, SnapshotNow, 1, &scankey);
	//scan = heap_beginscan(rel, SnapshotNow, 1, &scankey);

	// get all parameter arrays
	for (i=0; i<numParameters; i++)
	{
		HeapTuple tuple = systable_getnext(scan);
		if (!HeapTupleIsValid(tuple))
		{
			elog(ERROR, "error while retrieving model parameters");
		}

		 parameterTuple = (Form_pg_parameter) GETSTRUCT(tuple);
		if (parameterTuple->parameter != (i+1))
		{
			elog(ERROR, "error while retrieving model parameters");
		}

		datum = heap_getattr(tuple, Anum_pg_parameter_value, RelationGetDescr(rel), &isnull);
		parameters[i] = DatumGetArrayTypeP(datum);
	}

	systable_endscan(scan);
	heap_close(rel, RowExclusiveLock);
}
void
RetrieveParametersMg(Oid modelOid, ArrayType **parameters, int numParameters,Relation rel)
{


	ScanKeyData			scankey;
	HeapScanDesc 		scan;
	//HeapTuple			tuple;
	//Form_pg_parameter 	parameterTuple;
	Datum				datum;
	bool				isnull;
	int					i;
	Form_pg_parameter   parameterTuple;

	// open parameter relation and index


	ScanKeyInit(&scankey,
				Anum_pg_parameter_model,
				BTEqualStrategyNumber, F_OIDEQ,
				ObjectIdGetDatum(modelOid));

	// TODO: Use an index instad of a sequential scan
	scan = heap_beginscan(rel,SnapshotNow,1,&scankey);


	// get all parameter arrays
	for (i=0; i<numParameters; i++)
	{
		HeapTuple tuple = heap_getnext(scan, ForwardScanDirection);
		if (!HeapTupleIsValid(tuple))
		{
			elog(ERROR, "error while retrieving model parameters");
		}

		parameterTuple = (Form_pg_parameter) GETSTRUCT(tuple);
		if (parameterTuple->parameter != (i+1))
		{
			elog(ERROR, "error while retrieving model parameters");
		}

		datum = heap_getattr(tuple, Anum_pg_parameter_value, RelationGetDescr(rel), &isnull);
		parameters[i] = DatumGetArrayTypeP(datum);
	}

	heap_endscan(scan);
}

