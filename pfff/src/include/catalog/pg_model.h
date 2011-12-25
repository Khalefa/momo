/*-------------------------------------------------------------------------
 *
 * pg_model.h
 *	  definition of the system "model" relation (pg_model)
 *	  along with the relation's initial contents.
 *
 *
 * Portions Copyright (c) 1996-2009, PostgreSQL Global Development Group
 * Portions Copyright (c) 1994, Regents of the University of California
 *
 * $PostgreSQL: pgsql/src/include/catalog/pg_model.h,v 1.0 2009/09/02 14:35:00 momjian Exp $
 *
 * NOTES
 *	  the genbki.sh script reads this file and generates .bki
 *	  information from the DATA() statements.
 *
 *-------------------------------------------------------------------------
 */
#ifndef PG_MODEL_H
#define PG_MODEL_H

#include "catalog/genbki.h"

#include "nodes/primnodes.h"
#include "forecast/algorithm.h"
#include "catalog/pg_parameter.h"

/* ----------------
 *		pg_model definition.	cpp turns this into
 *		typedef struct FormData_pg_model
 * ----------------
 */
#define ModelRelationId  6000
#define ParameterRelationId  6001

CATALOG(pg_model,6000)
{
	NameData	name;	 				/* name of model */
	Oid 		table;					/* relation model is attached to */
	int2 		time;					/* ordering/time column */
	int2 		measure;				/* measure column */
	NameData 	algorithm;				/* model type */
	int2 		granularity;			/* granularity of time column */
	Oid 		aggtype;				/* aggregation type */
	bool 		implicit;				/* implicit model creation? */
	bool 		enable;					/* use model for queries? */
	int4		timestamp;
	float8		forecast;
	float8		ecount;
	float8		eval;
	text		constraints;			/* constraints on x column (usually from other dimension columns) */
	text 		sql;					/* creation statement */
} FormData_pg_model;

/* ----------------
 *		Form_pg_model corresponds to a pointer to a tuple with
 *		the format of pg_model relation.
 * ----------------
 */
typedef FormData_pg_model *Form_pg_model;


 

 
/* ----------------
 *		compiler constants for pg_model
 * ----------------
 */
#define Natts_pg_model					15
#define Anum_pg_model_name				1

#define Anum_pg_model_relid				2
#define Anum_pg_model_time				3
#define Anum_pg_model_dimension			4

#define Anum_pg_model_algorithm			5

#define Anum_pg_model_granularity		6
#define Anum_pg_model_aggtype			7

#define Anum_pg_model_implicit			8

#define Anum_pg_model_enable			9

#define Anum_pg_model_timestamp			10
#define Anum_pg_model_forecast			11
#define Anum_pg_model_ecount			12
#define Anum_pg_model_eval				13

#define Anum_pg_model_constraints		14
#define Anum_pg_model_sql				15


void
SetModelName(ModelInfo *model);

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
					double value);



		
List*
FindModel(Node *whereExpr, ModelInfo *model,char* targetDateString);

void
ReplaceTimeStamp(Oid model,
				int timestamp);


#endif   /* PG_MODEL_H */

