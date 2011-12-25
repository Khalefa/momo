/*-------------------------------------------------------------------------
 *
 * pg_parameter.h
 *	  definition of the system "parameter" relation (pg_parameter)
 *	  along with the relation's initial contents.
 *
 *
 * Portions Copyright (c) 1996-2009, PostgreSQL Global Development Group
 * Portions Copyright (c) 1994, Regents of the University of California
 *
 * $PostgreSQL: pgsql/src/include/catalog/pg_parameter.h,v 1.0$
 *
 * NOTES
 *	  the genbki.sh script reads this file and generates .bki
 *	  information from the DATA() statements.
 *
 *-------------------------------------------------------------------------
 */
#ifndef PG_PARAMETER_H
#define PG_PARAMETER_H

#include "access/htup.h"
#include "catalog/genbki.h"
#include "utils/array.h"

/* ----------------
 *		pg_parameter definition.	cpp turns this into
 *		typedef struct FormData_pg_parameter
 * ----------------
 */
 #define ModelRelationId  6000
#define ParameterRelationId  6001

CATALOG(pg_parameter,6001)
{
	Oid 		model; 					/* id of corresponding model */
	int2 		parameter;				/* id of parameter */
	float8 		value[1];				/* value of parameter */
} FormData_pg_parameter;


/* ----------------
 *		Form_pg_model corresponds to a pointer to a tuple with
 *		the format of pg_model relation.
 * ----------------
 */
typedef FormData_pg_parameter *Form_pg_parameter;



/* ----------------
 *		compiler constants for pg_model
 * ----------------
 */
#define Natts_pg_parameter				3

#define Anum_pg_parameter_model			1
#define Anum_pg_parameter_name			2
#define Anum_pg_parameter_value			3


Oid
InsertParameters(Oid model,
				int parameter,
				ArrayType *array);

void
RetrieveParameters(Oid modelOid, ArrayType **parameters, int numParameters);

void
InsertOrReplaceParametersMg(Oid model,Relation mrel,
				int parameter,
				ArrayType *array);
				Oid
InsertOrReplaceParameters(Oid model,
				int parameter,
				ArrayType *array);
				void
RetrieveParametersMg(Oid modelOid, ArrayType **parameters, int numParameters,Relation rel);


#endif   /* PG_PARAMETER_H */
