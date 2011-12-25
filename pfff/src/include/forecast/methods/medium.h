/*--------------------------------------------------------------------------
 * medium.h
 *	  header file for forecast algorithm interface.
 *
 *
 *	$PostgreSQL: pgsql/src/include/forecast/method/medium.h,v 1.34 2009/06/11 14:49:08 momjian Exp $
 *--------------------------------------------------------------------------
 */
#ifndef MEDIUM_H
#define MEDIUM_H

#include "fmgr.h"

/*
 *
 * Interface definition for the forecast algorithms. Every Forecast Algorithm has to implement
 * these methods and hast to register their implementation in the system catalog (pg_proc and pg_fcalg)
 *
 */
extern void initMediumForecastModel(PG_FUNCTION_ARGS);
extern void loadMediumModelParameters(PG_FUNCTION_ARGS);
extern void processMediumForecastModel(PG_FUNCTION_ARGS);
extern void finalizeMediumForecastModel(PG_FUNCTION_ARGS);

extern void storeMediumModelParameters(PG_FUNCTION_ARGS);
extern Datum getNextMediumValue(PG_FUNCTION_ARGS);

extern void incrementalUpdateMediumModel(PG_FUNCTION_ARGS);

#endif   /* MEDIUM */
