/*-------------------------------------------------------------------------
 *
 * modelReceiver.h
 *	  prototypes for modelReceiver.c
 *
 * $PostgreSQL: pgsql/src/include/commands/modelReceiver.h,v 1.0 2009/11/25 14:49:11 jm
 *
 *-------------------------------------------------------------------------
 */

#ifndef MODELRECEIVER_H_
#define MODELRECEIVER_H_

#include "tcop/dest.h"
#include "nodes/primnodes.h"

extern DestReceiver *CreateModelDestReceiver(const char *modelName,
											TargetEntry *outputColumn,
											List *timeColumns,
											Node *whereExpression,
											const char *statement,
											const char *algorithm,
											List *algorithmParameter);

#endif /* MODELRECEIVER_H_ */
