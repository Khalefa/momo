/*-------------------------------------------------------------------------
 *
 * pg_fcalg.h
 *	  definition of the system "forecast algorithm" relation (pg_fcalg)
 *	  along with the relation's initial contents.
 *
 * $PostgreSQL: pgsql/src/include/catalog/pg_fcalg.h,v 1.0 2010/01/xx 14:35:00 mj Exp $
 *
 * NOTES
 *	  the genbki.sh script reads this file and generates .bki
 *	  information from the DATA() statements.
 *
 *-------------------------------------------------------------------------
 */
#ifndef PG_FCALG_H
#define PG_FCALG_H

#include "catalog/genbki.h"


/* ----------------
 *		pg_fcalg definition.
 * ----------------
 */
#define ForecastAlgorithmRelationId  6100

CATALOG(pg_fcalg,6100)
{
	NameData	algName;	 				/* name of algorithm */
	regproc		algInitModel;				/* initialization method of the according model */
	regproc		algLoadModelParameters;		/* loading of a stored Model */
	regproc		algProcessForecast;			/* Process Tuple */
	regproc		algFinalizeForecastModel;	/* finalize Forecast Model */
	regproc		algStoreModelParameters;		/* stores the Parameters of the Model */
	regproc		algGetNextValue	;			/* makes a Forecast*/
	regproc		algIncrementalUpdate;
	regproc		algIncrementalUpdate2;
	regproc		algReestimateUpdate;
} FormData_pg_fcalg;

/* ----------------
 *		Form_pg_fcalg corresponds to a pointer to a tuple with
 *		the format of pg_fcalg relation.
 * ----------------
 */
typedef FormData_pg_fcalg *Form_pg_fcalg;

/* ----------------
 *		compiler constants for pg_model
 * ----------------
 */
#define Anum_pg_fcalg_name				1
#define Natts_pg_fcalg					7
#define Anum_pg_fcalg_init				2
#define Anum_pg_fcalg_load				3
#define Anum_pg_fcalg_process			4
#define Anum_pg_fcalg_finalize			5
#define Anum_pg_fcalg_store				6
#define Anum_pg_fcalg_getnext			7
#define Anum_pg_fcalg_incremental		8
#define Anum_pg_fcalg_incremental2		9
#define Anum_pg_fcalg_reestimate		10

/* ----------------
 *		initial contents of pg_fcalg
 * ----------------
 */
DATA(insert OID = 1578396 (  medium   mediuminitmodel mediumloadmodel mediumprocessmodel mediumfinalizemodel mediumstoreparameters mediumnextvalue mediumincrementalupdate mediumincrementalupdate mediumincrementalupdate));
DESCR("medium forecast method");
#define MEDIUM_FCALG_OID 1578396
DATA(insert OID = 1578397 (  linreg   linreginitmodel linregloadmodel linregprocessmodel linregfinalizemodel linregstoreparameters linregnextvalue linregincrementalupdate linregincrementalupdate linregincrementalupdate));
DESCR("linreg forecast method");
#define LINREG_FCALG_OID 1578397
DATA(insert OID = 1578398 (  gnur   rinitmodel rloadmodel rprocessmodel rfinalizemodel rstoreparameters rnextvalue rincrementalupdate rincrementalupdate rincrementalupdate));
DESCR("gnuR forecast method");
#define GNUR_FCALG_OID 1578398


DATA(insert OID = 1578400 (	armodel	armodelinitmodel armodelloadmodel	armodelprocessmodel	armodelfinalizemodel	armodelstoreparameters	armodelnextvalue	armodelincrementalupdate	armodelincrementalupdate2 armodelreestimate));
DESCR("ar forecast method");
#define ARMODEL_FCALG_OID 1578400

DATA(insert OID = 1578401 (	hwmodel	hwmodelinitmodel hwmodelloadmodel	hwmodelprocessmodel	hwmodelfinalizemodel	hwmodelstoreparameters	hwmodelnextvalue	hwmodelincrementalupdate	armodelincrementalupdate2 hwmodelreestimate));
DESCR("hw forecast method");
#define HWMODEL_FCALG_OID 1578401

#endif   /* PG_FCALG_H */
