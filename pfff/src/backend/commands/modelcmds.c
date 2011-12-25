/*-------------------------------------------------------------------------
 *
 * modelcmds.c
 *		Cmds to create and maintain models (CREATE, ALTER, DROP).
 *
 *
 * Portions Copyright (c) 1996-2009, PostgreSQL Global Development Group
 * Portions Copyright (c) 1994, Regents of the University of California
 *
 *
 * IDENTIFICATION
 *	  $PostgreSQL: pgsql/src/backend/commands/modelcmds.c,v 1.0 2009/11/01 17:35:21 momjian Exp $
 *
 *-------------------------------------------------------------------------
 */
#include "postgres.h"
#include "commands/modelReceiver.h"
#include "commands/modelcmds.h"
#include "tcop/utility.h"
#include "utils/portal.h"
#include "tcop/pquery.h"
#include "forecast/algorithm.h"

//extern void CreateModel(CreateModelStmt *stmt, const char *queryString, DestReceiver *dest, char *completionTag);
//extern void CreateModel();

void
CreateModel(CreateModelStmt *stmt,const char *queryString, DestReceiver *dest, char *completionTag)
{

	List		*query_list;
	List		*planned_list;
	const char	*commandTag;
	Portal		portal;
	DestReceiver *tupledest;

	// create command Tag
	commandTag = CreateCommandTag(stmt->algorithmclause->trainingdata);

	// Rewrite the already analyzed Select query for the training data

	query_list = pg_rewrite_query((Query *)stmt->algorithmclause->trainingdata);

	//  plan the query

	planned_list = pg_plan_queries(query_list,0,NULL);

	// results should be send to the ModelReceiver
	tupledest = CreateModelDestReceiver(stmt->modelname,
										(TargetEntry *)stmt->outputcolumn,
										stmt->timecolumns,
										((Query *)stmt->algorithmclause->trainingdata)->jointree->quals,
										queryString,stmt->algorithmclause->algorithmname,
										((AlgorithmClause *)copyObject(stmt->algorithmclause))->algorithmparameter);

	// Create a new portal to run the query in
	portal = CreateNewPortal();

	//Don't display the portal in pg_cursors, it is for internal use only
	portal->visible = false;

	PortalDefineQuery(portal,
						  NULL,
						  queryString,
						  commandTag,
						  planned_list,
						  NULL);


	//  Start the portal.  No parameters here.
	PortalStart(portal, NULL, InvalidSnapshot);

	(void) PortalRun(portal, FETCH_ALL, false, tupledest, tupledest, completionTag);

	// Drop portal and receiver

	(*tupledest->rDestroy) (tupledest);

	PortalDrop(portal, false);

}
