/*-------------------------------------------------------------------------
 *
 * parse_clause.h
 *	  handle clauses in parser
 *
 *
 * Portions Copyright (c) 1996-2009, PostgreSQL Global Development Group
 * Portions Copyright (c) 1994, Regents of the University of California
 *
 * $PostgreSQL: pgsql/src/include/parser/parse_clause.h,v 1.55 2009/06/11 14:49:11 momjian Exp $
 *
 *-------------------------------------------------------------------------
 */
#ifndef PARSE_CLAUSE_H
#define PARSE_CLAUSE_H

#include "parser/parse_node.h"

#define ORDER_CLAUSE 0
#define GROUP_CLAUSE 1
#define DISTINCT_ON_CLAUSE 2
#define PARTITION_CLAUSE 3

static const char *const clauseText[] = {
	"ORDER BY",
	"GROUP BY",
	"DISTINCT ON",
	"PARTITION BY"
};

extern void transformFromClause(ParseState *pstate, List *frmList);
extern int setTargetTable(ParseState *pstate, RangeVar *relation,
			   bool inh, bool alsoSource, AclMode requiredPerms);
extern bool interpretInhOption(InhOption inhOpt);
extern bool interpretOidsOption(List *defList);

extern Node *transformWhereClause(ParseState *pstate, Node *clause,
					 const char *constructName);
extern Node *transformLimitClause(ParseState *pstate, Node *clause,
					 const char *constructName);
extern List *transformGroupClause(ParseState *pstate, List *grouplist,
					 List **targetlist, List *sortClause,
					 bool isPartition);
extern List *transformSortClause(ParseState *pstate, List *orderlist,
					List **targetlist, bool resolveUnknown);

extern List *transformWindowDefinitions(ParseState *pstate,
						   List *windowdefs,
						   List **targetlist);

extern List *transformDistinctClause(ParseState *pstate,
						List **targetlist, List *sortClause);
extern List *transformDistinctOnClause(ParseState *pstate, List *distinctlist,
						  List **targetlist, List *sortClause);

extern Index assignSortGroupRef(TargetEntry *tle, List *tlist);
extern bool targetIsInSortList(TargetEntry *tle, Oid sortop, List *sortList);

extern ForecastExpr *transformForecastClause(ParseState *pstate, ForecastExpr *expr, Query *query);
extern DecomposeExpr *transformDecomposeClause(ParseState *pstate, DecomposeExpr *expr, Query *query);

extern DisAggExpr *transformDisAggClause(ParseState *pstate, DisAggExpr *expr, Query *query);

extern TargetEntry *
findTargetlistEntry(ParseState *pstate, Node *node, List **tlist, int clause);

#endif   /* PARSE_CLAUSE_H */
