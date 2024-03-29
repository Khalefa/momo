/*-------------------------------------------------------------------------
 *
 * parse_clause.c
 *	  handle clauses in parser
 *
 * Portions Copyright (c) 1996-2009, PostgreSQL Global Development Group
 * Portions Copyright (c) 1994, Regents of the University of California
 *
 *
 * IDENTIFICATION
 *	  $PostgreSQL: pgsql/src/backend/parser/parse_clause.c,v 1.189 2009/06/11 14:49:00 momjian Exp $
 *
 *-------------------------------------------------------------------------
 */

#include "postgres.h"

#include "access/heapam.h"
#include "catalog/heap.h"
#include "catalog/pg_type.h"
#include "commands/defrem.h"
#include "nodes/makefuncs.h"
#include "nodes/nodeFuncs.h"
#include "optimizer/tlist.h"
#include "optimizer/var.h"
#include "parser/analyze.h"
#include "parser/parsetree.h"
#include "parser/parse_clause.h"
#include "parser/parse_coerce.h"
#include "parser/parse_expr.h"
#include "parser/parse_oper.h"
#include "parser/parse_relation.h"
#include "parser/parse_target.h"
#include "rewrite/rewriteManip.h"
#include "utils/guc.h"
#include "utils/lsyscache.h"
#include "utils/rel.h"
#include "utils/builtins.h"
#include "forecast/algorithm.h"


/*#define ORDER_CLAUSE 0
#define GROUP_CLAUSE 1
#define DISTINCT_ON_CLAUSE 2
#define PARTITION_CLAUSE 3

static const char *const clauseText[] = {
	"ORDER BY",
	"GROUP BY",
	"DISTINCT ON",
	"PARTITION BY"
};*/

static void extractRemainingColumns(List *common_colnames,
						List *src_colnames, List *src_colvars,
						List **res_colnames, List **res_colvars);
static Node *transformJoinUsingClause(ParseState *pstate,
						 RangeTblEntry *leftRTE, RangeTblEntry *rightRTE,
						 List *leftVars, List *rightVars);
static Node *transformJoinOnClause(ParseState *pstate, JoinExpr *j,
					  RangeTblEntry *l_rte,
					  RangeTblEntry *r_rte,
					  List *relnamespace,
					  Relids containedRels);
static RangeTblEntry *transformTableEntry(ParseState *pstate, RangeVar *r);
static RangeTblEntry *transformCTEReference(ParseState *pstate, RangeVar *r,
					  CommonTableExpr *cte, Index levelsup);
static RangeTblEntry *transformRangeSubselect(ParseState *pstate,
						RangeSubselect *r);
static RangeTblEntry *transformRangeFunction(ParseState *pstate,
					   RangeFunction *r);
static Node *transformFromClauseItem(ParseState *pstate, Node *n,
						RangeTblEntry **top_rte, int *top_rti,
						List **relnamespace,
						Relids *containedRels);
static Node *buildMergedJoinVar(ParseState *pstate, JoinType jointype,
				   Var *l_colvar, Var *r_colvar);
//static TargetEntry *findTargetlistEntry(ParseState *pstate, Node *node,
	//				List **tlist, int clause);
static int get_matching_location(int sortgroupref,
					  List *sortgrouprefs, List *exprs);
static List *addTargetToSortList(ParseState *pstate, TargetEntry *tle,
					List *sortlist, List *targetlist, SortBy *sortby,
					bool resolveUnknown);
static List *addTargetToGroupList(ParseState *pstate, TargetEntry *tle,
					 List *grouplist, List *targetlist, int location,
					 bool resolveUnknown);
static WindowClause *findWindowClause(List *wclist, const char *name);


/*
 * transformFromClause -
 *	  Process the FROM clause and add items to the query's range table,
 *	  joinlist, and namespaces.
 *
 * Note: we assume that pstate's p_rtable, p_joinlist, p_relnamespace, and
 * p_varnamespace lists were initialized to NIL when the pstate was created.
 * We will add onto any entries already present --- this is needed for rule
 * processing, as well as for UPDATE and DELETE.
 *
 * The range table may grow still further when we transform the expressions
 * in the query's quals and target list. (This is possible because in
 * POSTQUEL, we allowed references to relations not specified in the
 * from-clause.  PostgreSQL keeps this extension to standard SQL.)
 */
void
transformFromClause(ParseState *pstate, List *frmList)
{
	ListCell   *fl;

	/*
	 * The grammar will have produced a list of RangeVars, RangeSubselects,
	 * RangeFunctions, and/or JoinExprs. Transform each one (possibly adding
	 * entries to the rtable), check for duplicate refnames, and then add it
	 * to the joinlist and namespaces.
	 */
	foreach(fl, frmList)
	{
		Node	   *n = lfirst(fl);
		RangeTblEntry *rte;
		int			rtindex;
		List	   *relnamespace;
		Relids		containedRels;

		n = transformFromClauseItem(pstate, n,
									&rte,
									&rtindex,
									&relnamespace,
									&containedRels);
		checkNameSpaceConflicts(pstate, pstate->p_relnamespace, relnamespace);
		pstate->p_joinlist = lappend(pstate->p_joinlist, n);
		pstate->p_relnamespace = list_concat(pstate->p_relnamespace,
											 relnamespace);
		pstate->p_varnamespace = lappend(pstate->p_varnamespace, rte);
		bms_free(containedRels);
	}
}

/*
 * setTargetTable
 *	  Add the target relation of INSERT/UPDATE/DELETE to the range table,
 *	  and make the special links to it in the ParseState.
 *
 *	  We also open the target relation and acquire a write lock on it.
 *	  This must be done before processing the FROM list, in case the target
 *	  is also mentioned as a source relation --- we want to be sure to grab
 *	  the write lock before any read lock.
 *
 *	  If alsoSource is true, add the target to the query's joinlist and
 *	  namespace.  For INSERT, we don't want the target to be joined to;
 *	  it's a destination of tuples, not a source.	For UPDATE/DELETE,
 *	  we do need to scan or join the target.  (NOTE: we do not bother
 *	  to check for namespace conflict; we assume that the namespace was
 *	  initially empty in these cases.)
 *
 *	  Finally, we mark the relation as requiring the permissions specified
 *	  by requiredPerms.
 *
 *	  Returns the rangetable index of the target relation.
 */
int
setTargetTable(ParseState *pstate, RangeVar *relation,
			   bool inh, bool alsoSource, AclMode requiredPerms)
{
	RangeTblEntry *rte;
	int			rtindex;

	/* Close old target; this could only happen for multi-action rules */
	if (pstate->p_target_relation != NULL)
		heap_close(pstate->p_target_relation, NoLock);

	/*
	 * Open target rel and grab suitable lock (which we will hold till end of
	 * transaction).
	 *
	 * free_parsestate() will eventually do the corresponding heap_close(),
	 * but *not* release the lock.
	 */
	pstate->p_target_relation = parserOpenTable(pstate, relation,
												RowExclusiveLock);

	/*
	 * Now build an RTE.
	 */
	rte = addRangeTableEntryForRelation(pstate, pstate->p_target_relation,
										relation->alias, inh, false);
	pstate->p_target_rangetblentry = rte;

	/* assume new rte is at end */
	rtindex = list_length(pstate->p_rtable);
	Assert(rte == rt_fetch(rtindex, pstate->p_rtable));

	/*
	 * Override addRangeTableEntry's default ACL_SELECT permissions check, and
	 * instead mark target table as requiring exactly the specified
	 * permissions.
	 *
	 * If we find an explicit reference to the rel later during parse
	 * analysis, we will add the ACL_SELECT bit back again; see
	 * markVarForSelectPriv and its callers.
	 */
	rte->requiredPerms = requiredPerms;

	/*
	 * If UPDATE/DELETE, add table to joinlist and namespaces.
	 */
	if (alsoSource)
		addRTEtoQuery(pstate, rte, true, true, true);

	return rtindex;
}

/*
 * Simplify InhOption (yes/no/default) into boolean yes/no.
 *
 * The reason we do things this way is that we don't want to examine the
 * SQL_inheritance option flag until parse_analyze() is run.	Otherwise,
 * we'd do the wrong thing with query strings that intermix SET commands
 * with queries.
 */
bool
interpretInhOption(InhOption inhOpt)
{
	switch (inhOpt)
	{
		case INH_NO:
			return false;
		case INH_YES:
			return true;
		case INH_DEFAULT:
			return SQL_inheritance;
	}
	elog(ERROR, "bogus InhOption value: %d", inhOpt);
	return false;				/* keep compiler quiet */
}

/*
 * Given a relation-options list (of DefElems), return true iff the specified
 * table/result set should be created with OIDs. This needs to be done after
 * parsing the query string because the return value can depend upon the
 * default_with_oids GUC var.
 */
bool
interpretOidsOption(List *defList)
{
	ListCell   *cell;

	/* Scan list to see if OIDS was included */
	foreach(cell, defList)
	{
		DefElem    *def = (DefElem *) lfirst(cell);

		if (def->defnamespace == NULL &&
			pg_strcasecmp(def->defname, "oids") == 0)
			return defGetBoolean(def);
	}

	/* OIDS option was not specified, so use default. */
	return default_with_oids;
}

/*
 * Extract all not-in-common columns from column lists of a source table
 */
static void
extractRemainingColumns(List *common_colnames,
						List *src_colnames, List *src_colvars,
						List **res_colnames, List **res_colvars)
{
	List	   *new_colnames = NIL;
	List	   *new_colvars = NIL;
	ListCell   *lnames,
			   *lvars;

	Assert(list_length(src_colnames) == list_length(src_colvars));

	forboth(lnames, src_colnames, lvars, src_colvars)
	{
		char	   *colname = strVal(lfirst(lnames));
		bool		match = false;
		ListCell   *cnames;

		foreach(cnames, common_colnames)
		{
			char	   *ccolname = strVal(lfirst(cnames));

			if (strcmp(colname, ccolname) == 0)
			{
				match = true;
				break;
			}
		}

		if (!match)
		{
			new_colnames = lappend(new_colnames, lfirst(lnames));
			new_colvars = lappend(new_colvars, lfirst(lvars));
		}
	}

	*res_colnames = new_colnames;
	*res_colvars = new_colvars;
}

/* transformJoinUsingClause()
 *	  Build a complete ON clause from a partially-transformed USING list.
 *	  We are given lists of nodes representing left and right match columns.
 *	  Result is a transformed qualification expression.
 */
static Node *
transformJoinUsingClause(ParseState *pstate,
						 RangeTblEntry *leftRTE, RangeTblEntry *rightRTE,
						 List *leftVars, List *rightVars)
{
	Node	   *result = NULL;
	ListCell   *lvars,
			   *rvars;

	/*
	 * We cheat a little bit here by building an untransformed operator tree
	 * whose leaves are the already-transformed Vars.  This is OK because
	 * transformExpr() won't complain about already-transformed subnodes.
	 * However, this does mean that we have to mark the columns as requiring
	 * SELECT privilege for ourselves; transformExpr() won't do it.
	 */
	forboth(lvars, leftVars, rvars, rightVars)
	{
		Var		   *lvar = (Var *) lfirst(lvars);
		Var		   *rvar = (Var *) lfirst(rvars);
		A_Expr	   *e;

		/* Require read access to the join variables */
		markVarForSelectPriv(pstate, lvar, leftRTE);
		markVarForSelectPriv(pstate, rvar, rightRTE);

		/* Now create the lvar = rvar join condition */
		e = makeSimpleA_Expr(AEXPR_OP, "=",
							 copyObject(lvar), copyObject(rvar),
							 -1);

		/* And combine into an AND clause, if multiple join columns */
		if (result == NULL)
			result = (Node *) e;
		else
		{
			A_Expr	   *a;

			a = makeA_Expr(AEXPR_AND, NIL, result, (Node *) e, -1);
			result = (Node *) a;
		}
	}

	/*
	 * Since the references are already Vars, and are certainly from the input
	 * relations, we don't have to go through the same pushups that
	 * transformJoinOnClause() does.  Just invoke transformExpr() to fix up
	 * the operators, and we're done.
	 */
	result = transformExpr(pstate, result);

	result = coerce_to_boolean(pstate, result, "JOIN/USING");

	return result;
}

/* transformJoinOnClause()
 *	  Transform the qual conditions for JOIN/ON.
 *	  Result is a transformed qualification expression.
 */
static Node *
transformJoinOnClause(ParseState *pstate, JoinExpr *j,
					  RangeTblEntry *l_rte,
					  RangeTblEntry *r_rte,
					  List *relnamespace,
					  Relids containedRels)
{
	Node	   *result;
	List	   *save_relnamespace;
	List	   *save_varnamespace;
	Relids		clause_varnos;
	int			varno;

	/*
	 * This is a tad tricky, for two reasons.  First, the namespace that the
	 * join expression should see is just the two subtrees of the JOIN plus
	 * any outer references from upper pstate levels.  So, temporarily set
	 * this pstate's namespace accordingly.  (We need not check for refname
	 * conflicts, because transformFromClauseItem() already did.) NOTE: this
	 * code is OK only because the ON clause can't legally alter the namespace
	 * by causing implicit relation refs to be added.
	 */
	save_relnamespace = pstate->p_relnamespace;
	save_varnamespace = pstate->p_varnamespace;

	pstate->p_relnamespace = relnamespace;
	pstate->p_varnamespace = list_make2(l_rte, r_rte);

	result = transformWhereClause(pstate, j->quals, "JOIN/ON");

	pstate->p_relnamespace = save_relnamespace;
	pstate->p_varnamespace = save_varnamespace;

	/*
	 * Second, we need to check that the ON condition doesn't refer to any
	 * rels outside the input subtrees of the JOIN.  It could do that despite
	 * our hack on the namespace if it uses fully-qualified names. So, grovel
	 * through the transformed clause and make sure there are no bogus
	 * references.	(Outer references are OK, and are ignored here.)
	 */
	clause_varnos = pull_varnos(result);
	clause_varnos = bms_del_members(clause_varnos, containedRels);
	if ((varno = bms_first_member(clause_varnos)) >= 0)
	{
		ereport(ERROR,
				(errcode(ERRCODE_INVALID_COLUMN_REFERENCE),
		 errmsg("JOIN/ON clause refers to \"%s\", which is not part of JOIN",
				rt_fetch(varno, pstate->p_rtable)->eref->aliasname),
				 parser_errposition(pstate,
								 locate_var_of_relation(result, varno, 0))));
	}
	bms_free(clause_varnos);

	return result;
}

/*
 * transformTableEntry --- transform a RangeVar (simple relation reference)
 */
static RangeTblEntry *
transformTableEntry(ParseState *pstate, RangeVar *r)
{
	RangeTblEntry *rte;

	/*
	 * mark this entry to indicate it comes from the FROM clause. In SQL, the
	 * target list can only refer to range variables specified in the from
	 * clause but we follow the more powerful POSTQUEL semantics and
	 * automatically generate the range variable if not specified. However
	 * there are times we need to know whether the entries are legitimate.
	 */
	rte = addRangeTableEntry(pstate, r, r->alias,
							 interpretInhOption(r->inhOpt), true);

	return rte;
}

/*
 * transformCTEReference --- transform a RangeVar that references a common
 * table expression (ie, a sub-SELECT defined in a WITH clause)
 */
static RangeTblEntry *
transformCTEReference(ParseState *pstate, RangeVar *r,
					  CommonTableExpr *cte, Index levelsup)
{
	RangeTblEntry *rte;

	rte = addRangeTableEntryForCTE(pstate, cte, levelsup, r->alias, true);

	return rte;
}

/*
 * transformRangeSubselect --- transform a sub-SELECT appearing in FROM
 */
static RangeTblEntry *
transformRangeSubselect(ParseState *pstate, RangeSubselect *r)
{
	Query	   *query;
	RangeTblEntry *rte;

	/*
	 * We require user to supply an alias for a subselect, per SQL92. To relax
	 * this, we'd have to be prepared to gin up a unique alias for an
	 * unlabeled subselect.  (This is just elog, not ereport, because the
	 * grammar should have enforced it already.)
	 */
	if (r->alias == NULL)
		elog(ERROR, "subquery in FROM must have an alias");

	/*
	 * Analyze and transform the subquery.
	 */
	query = parse_sub_analyze(r->subquery, pstate);

	/*
	 * Check that we got something reasonable.	Many of these conditions are
	 * impossible given restrictions of the grammar, but check 'em anyway.
	 */
	if (!IsA(query, Query) ||
		query->commandType != CMD_SELECT ||
		query->utilityStmt != NULL)
		elog(ERROR, "unexpected non-SELECT command in subquery in FROM");
	if (query->intoClause)
		ereport(ERROR,
				(errcode(ERRCODE_SYNTAX_ERROR),
				 errmsg("subquery in FROM cannot have SELECT INTO"),
				 parser_errposition(pstate,
								 exprLocation((Node *) query->intoClause))));

	/*
	 * The subquery cannot make use of any variables from FROM items created
	 * earlier in the current query.  Per SQL92, the scope of a FROM item does
	 * not include other FROM items.  Formerly we hacked the namespace so that
	 * the other variables weren't even visible, but it seems more useful to
	 * leave them visible and give a specific error message.
	 *
	 * XXX this will need further work to support SQL99's LATERAL() feature,
	 * wherein such references would indeed be legal.
	 *
	 * We can skip groveling through the subquery if there's not anything
	 * visible in the current query.  Also note that outer references are OK.
	 */
	if (pstate->p_relnamespace || pstate->p_varnamespace)
	{
		if (contain_vars_of_level((Node *) query, 1))
			ereport(ERROR,
					(errcode(ERRCODE_INVALID_COLUMN_REFERENCE),
					 errmsg("subquery in FROM cannot refer to other relations of same query level"),
					 parser_errposition(pstate,
								   locate_var_of_level((Node *) query, 1))));
	}

	/*
	 * OK, build an RTE for the subquery.
	 */
	rte = addRangeTableEntryForSubquery(pstate, query, r->alias, true);

	return rte;
}


/*
 * transformRangeFunction --- transform a function call appearing in FROM
 */
static RangeTblEntry *
transformRangeFunction(ParseState *pstate, RangeFunction *r)
{
	Node	   *funcexpr;
	char	   *funcname;
	RangeTblEntry *rte;

	/*
	 * Get function name for possible use as alias.  We use the same
	 * transformation rules as for a SELECT output expression.	For a FuncCall
	 * node, the result will be the function name, but it is possible for the
	 * grammar to hand back other node types.
	 */
	funcname = FigureColname(r->funccallnode);

	/*
	 * Transform the raw expression.
	 */
	funcexpr = transformExpr(pstate, r->funccallnode);

	/*
	 * The function parameters cannot make use of any variables from other
	 * FROM items.	(Compare to transformRangeSubselect(); the coding is
	 * different though because we didn't parse as a sub-select with its own
	 * level of namespace.)
	 *
	 * XXX this will need further work to support SQL99's LATERAL() feature,
	 * wherein such references would indeed be legal.
	 */
	if (pstate->p_relnamespace || pstate->p_varnamespace)
	{
		if (contain_vars_of_level(funcexpr, 0))
			ereport(ERROR,
					(errcode(ERRCODE_INVALID_COLUMN_REFERENCE),
					 errmsg("function expression in FROM cannot refer to other relations of same query level"),
					 parser_errposition(pstate,
										locate_var_of_level(funcexpr, 0))));
	}

	/*
	 * Disallow aggregate functions in the expression.	(No reason to postpone
	 * this check until parseCheckAggregates.)
	 */
	if (pstate->p_hasAggs &&
		checkExprHasAggs(funcexpr))
		ereport(ERROR,
				(errcode(ERRCODE_GROUPING_ERROR),
				 errmsg("cannot use aggregate function in function expression in FROM"),
				 parser_errposition(pstate,
									locate_agg_of_level(funcexpr, 0))));
	if (pstate->p_hasWindowFuncs &&
		checkExprHasWindowFuncs(funcexpr))
		ereport(ERROR,
				(errcode(ERRCODE_WINDOWING_ERROR),
		 errmsg("cannot use window function in function expression in FROM"),
				 parser_errposition(pstate,
									locate_windowfunc(funcexpr))));

	/*
	 * OK, build an RTE for the function.
	 */
	rte = addRangeTableEntryForFunction(pstate, funcname, funcexpr,
										r, true);

	/*
	 * If a coldeflist was supplied, ensure it defines a legal set of names
	 * (no duplicates) and datatypes (no pseudo-types, for instance).
	 * addRangeTableEntryForFunction looked up the type names but didn't check
	 * them further than that.
	 */
	if (r->coldeflist)
	{
		TupleDesc	tupdesc;

		tupdesc = BuildDescFromLists(rte->eref->colnames,
									 rte->funccoltypes,
									 rte->funccoltypmods);
		CheckAttributeNamesTypes(tupdesc, RELKIND_COMPOSITE_TYPE);
	}

	return rte;
}


/*
 * transformFromClauseItem -
 *	  Transform a FROM-clause item, adding any required entries to the
 *	  range table list being built in the ParseState, and return the
 *	  transformed item ready to include in the joinlist and namespaces.
 *	  This routine can recurse to handle SQL92 JOIN expressions.
 *
 * The function return value is the node to add to the jointree (a
 * RangeTblRef or JoinExpr).  Additional output parameters are:
 *
 * *top_rte: receives the RTE corresponding to the jointree item.
 * (We could extract this from the function return node, but it saves cycles
 * to pass it back separately.)
 *
 * *top_rti: receives the rangetable index of top_rte.	(Ditto.)
 *
 * *relnamespace: receives a List of the RTEs exposed as relation names
 * by this item.
 *
 * *containedRels: receives a bitmap set of the rangetable indexes
 * of all the base and join relations represented in this jointree item.
 * This is needed for checking JOIN/ON conditions in higher levels.
 *
 * We do not need to pass back an explicit varnamespace value, because
 * in all cases the varnamespace contribution is exactly top_rte.
 */
static Node *
transformFromClauseItem(ParseState *pstate, Node *n,
						RangeTblEntry **top_rte, int *top_rti,
						List **relnamespace,
						Relids *containedRels)
{
	if (IsA(n, RangeVar))
	{
		/* Plain relation reference, or perhaps a CTE reference */
		RangeVar   *rv = (RangeVar *) n;
		RangeTblRef *rtr;
		RangeTblEntry *rte = NULL;
		int			rtindex;

		/* if it is an unqualified name, it might be a CTE reference */
		if (!rv->schemaname)
		{
			CommonTableExpr *cte;
			Index		levelsup;

			cte = scanNameSpaceForCTE(pstate, rv->relname, &levelsup);
			if (cte)
				rte = transformCTEReference(pstate, rv, cte, levelsup);
		}

		/* if not found as a CTE, must be a table reference */
		if (!rte)
			rte = transformTableEntry(pstate, rv);

		/* assume new rte is at end */
		rtindex = list_length(pstate->p_rtable);
		Assert(rte == rt_fetch(rtindex, pstate->p_rtable));
		*top_rte = rte;
		*top_rti = rtindex;
		*relnamespace = list_make1(rte);
		*containedRels = bms_make_singleton(rtindex);
		rtr = makeNode(RangeTblRef);
		rtr->rtindex = rtindex;
		return (Node *) rtr;
	}
	else if (IsA(n, RangeSubselect))
	{
		/* sub-SELECT is like a plain relation */
		RangeTblRef *rtr;
		RangeTblEntry *rte;
		int			rtindex;

		rte = transformRangeSubselect(pstate, (RangeSubselect *) n);
		/* assume new rte is at end */
		rtindex = list_length(pstate->p_rtable);
		Assert(rte == rt_fetch(rtindex, pstate->p_rtable));
		*top_rte = rte;
		*top_rti = rtindex;
		*relnamespace = list_make1(rte);
		*containedRels = bms_make_singleton(rtindex);
		rtr = makeNode(RangeTblRef);
		rtr->rtindex = rtindex;
		return (Node *) rtr;
	}
	else if (IsA(n, RangeFunction))
	{
		/* function is like a plain relation */
		RangeTblRef *rtr;
		RangeTblEntry *rte;
		int			rtindex;

		rte = transformRangeFunction(pstate, (RangeFunction *) n);
		/* assume new rte is at end */
		rtindex = list_length(pstate->p_rtable);
		Assert(rte == rt_fetch(rtindex, pstate->p_rtable));
		*top_rte = rte;
		*top_rti = rtindex;
		*relnamespace = list_make1(rte);
		*containedRels = bms_make_singleton(rtindex);
		rtr = makeNode(RangeTblRef);
		rtr->rtindex = rtindex;
		return (Node *) rtr;
	}
	else if (IsA(n, JoinExpr))
	{
		/* A newfangled join expression */
		JoinExpr   *j = (JoinExpr *) n;
		RangeTblEntry *l_rte;
		RangeTblEntry *r_rte;
		int			l_rtindex;
		int			r_rtindex;
		Relids		l_containedRels,
					r_containedRels,
					my_containedRels;
		List	   *l_relnamespace,
				   *r_relnamespace,
				   *my_relnamespace,
				   *l_colnames,
				   *r_colnames,
				   *res_colnames,
				   *l_colvars,
				   *r_colvars,
				   *res_colvars;
		RangeTblEntry *rte;
		int			k;

		/*
		 * Recursively process the left and right subtrees
		 */
		j->larg = transformFromClauseItem(pstate, j->larg,
										  &l_rte,
										  &l_rtindex,
										  &l_relnamespace,
										  &l_containedRels);
		j->rarg = transformFromClauseItem(pstate, j->rarg,
										  &r_rte,
										  &r_rtindex,
										  &r_relnamespace,
										  &r_containedRels);

		/*
		 * Check for conflicting refnames in left and right subtrees. Must do
		 * this because higher levels will assume I hand back a self-
		 * consistent namespace subtree.
		 */
		checkNameSpaceConflicts(pstate, l_relnamespace, r_relnamespace);

		/*
		 * Generate combined relation membership info for possible use by
		 * transformJoinOnClause below.
		 */
		my_relnamespace = list_concat(l_relnamespace, r_relnamespace);
		my_containedRels = bms_join(l_containedRels, r_containedRels);

		pfree(r_relnamespace);	/* free unneeded list header */

		/*
		 * Extract column name and var lists from both subtrees
		 *
		 * Note: expandRTE returns new lists, safe for me to modify
		 */
		expandRTE(l_rte, l_rtindex, 0, -1, false,
				  &l_colnames, &l_colvars);
		expandRTE(r_rte, r_rtindex, 0, -1, false,
				  &r_colnames, &r_colvars);

		/*
		 * Natural join does not explicitly specify columns; must generate
		 * columns to join. Need to run through the list of columns from each
		 * table or join result and match up the column names. Use the first
		 * table, and check every column in the second table for a match.
		 * (We'll check that the matches were unique later on.) The result of
		 * this step is a list of column names just like an explicitly-written
		 * USING list.
		 */
		if (j->isNatural)
		{
			List	   *rlist = NIL;
			ListCell   *lx,
					   *rx;

			Assert(j->using == NIL);	/* shouldn't have USING() too */

			foreach(lx, l_colnames)
			{
				char	   *l_colname = strVal(lfirst(lx));
				Value	   *m_name = NULL;

				foreach(rx, r_colnames)
				{
					char	   *r_colname = strVal(lfirst(rx));

					if (strcmp(l_colname, r_colname) == 0)
					{
						m_name = makeString(l_colname);
						break;
					}
				}

				/* matched a right column? then keep as join column... */
				if (m_name != NULL)
					rlist = lappend(rlist, m_name);
			}

			j->using = rlist;
		}

		/*
		 * Now transform the join qualifications, if any.
		 */
		res_colnames = NIL;
		res_colvars = NIL;

		if (j->using)
		{
			/*
			 * JOIN/USING (or NATURAL JOIN, as transformed above). Transform
			 * the list into an explicit ON-condition, and generate a list of
			 * merged result columns.
			 */
			List	   *ucols = j->using;
			List	   *l_usingvars = NIL;
			List	   *r_usingvars = NIL;
			ListCell   *ucol;

			Assert(j->quals == NULL);	/* shouldn't have ON() too */

			foreach(ucol, ucols)
			{
				char	   *u_colname = strVal(lfirst(ucol));
				ListCell   *col;
				int			ndx;
				int			l_index = -1;
				int			r_index = -1;
				Var		   *l_colvar,
						   *r_colvar;

				/* Check for USING(foo,foo) */
				foreach(col, res_colnames)
				{
					char	   *res_colname = strVal(lfirst(col));

					if (strcmp(res_colname, u_colname) == 0)
						ereport(ERROR,
								(errcode(ERRCODE_DUPLICATE_COLUMN),
								 errmsg("column name \"%s\" appears more than once in USING clause",
										u_colname)));
				}

				/* Find it in left input */
				ndx = 0;
				foreach(col, l_colnames)
				{
					char	   *l_colname = strVal(lfirst(col));

					if (strcmp(l_colname, u_colname) == 0)
					{
						if (l_index >= 0)
							ereport(ERROR,
									(errcode(ERRCODE_AMBIGUOUS_COLUMN),
									 errmsg("common column name \"%s\" appears more than once in left table",
											u_colname)));
						l_index = ndx;
					}
					ndx++;
				}
				if (l_index < 0)
					ereport(ERROR,
							(errcode(ERRCODE_UNDEFINED_COLUMN),
							 errmsg("column \"%s\" specified in USING clause does not exist in left table",
									u_colname)));

				/* Find it in right input */
				ndx = 0;
				foreach(col, r_colnames)
				{
					char	   *r_colname = strVal(lfirst(col));

					if (strcmp(r_colname, u_colname) == 0)
					{
						if (r_index >= 0)
							ereport(ERROR,
									(errcode(ERRCODE_AMBIGUOUS_COLUMN),
									 errmsg("common column name \"%s\" appears more than once in right table",
											u_colname)));
						r_index = ndx;
					}
					ndx++;
				}
				if (r_index < 0)
					ereport(ERROR,
							(errcode(ERRCODE_UNDEFINED_COLUMN),
							 errmsg("column \"%s\" specified in USING clause does not exist in right table",
									u_colname)));

				l_colvar = list_nth(l_colvars, l_index);
				l_usingvars = lappend(l_usingvars, l_colvar);
				r_colvar = list_nth(r_colvars, r_index);
				r_usingvars = lappend(r_usingvars, r_colvar);

				res_colnames = lappend(res_colnames, lfirst(ucol));
				res_colvars = lappend(res_colvars,
									  buildMergedJoinVar(pstate,
														 j->jointype,
														 l_colvar,
														 r_colvar));
			}

			j->quals = transformJoinUsingClause(pstate,
												l_rte,
												r_rte,
												l_usingvars,
												r_usingvars);
		}
		else if (j->quals)
		{
			/* User-written ON-condition; transform it */
			j->quals = transformJoinOnClause(pstate, j,
											 l_rte, r_rte,
											 my_relnamespace,
											 my_containedRels);
		}
		else
		{
			/* CROSS JOIN: no quals */
		}

		/* Add remaining columns from each side to the output columns */
		extractRemainingColumns(res_colnames,
								l_colnames, l_colvars,
								&l_colnames, &l_colvars);
		extractRemainingColumns(res_colnames,
								r_colnames, r_colvars,
								&r_colnames, &r_colvars);
		res_colnames = list_concat(res_colnames, l_colnames);
		res_colvars = list_concat(res_colvars, l_colvars);
		res_colnames = list_concat(res_colnames, r_colnames);
		res_colvars = list_concat(res_colvars, r_colvars);

		/*
		 * Check alias (AS clause), if any.
		 */
		if (j->alias)
		{
			if (j->alias->colnames != NIL)
			{
				if (list_length(j->alias->colnames) > list_length(res_colnames))
					ereport(ERROR,
							(errcode(ERRCODE_SYNTAX_ERROR),
							 errmsg("column alias list for \"%s\" has too many entries",
									j->alias->aliasname)));
			}
		}

		/*
		 * Now build an RTE for the result of the join
		 */
		rte = addRangeTableEntryForJoin(pstate,
										res_colnames,
										j->jointype,
										res_colvars,
										j->alias,
										true);

		/* assume new rte is at end */
		j->rtindex = list_length(pstate->p_rtable);
		Assert(rte == rt_fetch(j->rtindex, pstate->p_rtable));

		*top_rte = rte;
		*top_rti = j->rtindex;

		/* make a matching link to the JoinExpr for later use */
		for (k = list_length(pstate->p_joinexprs) + 1; k < j->rtindex; k++)
			pstate->p_joinexprs = lappend(pstate->p_joinexprs, NULL);
		pstate->p_joinexprs = lappend(pstate->p_joinexprs, j);
		Assert(list_length(pstate->p_joinexprs) == j->rtindex);

		/*
		 * Prepare returned namespace list.  If the JOIN has an alias then it
		 * hides the contained RTEs as far as the relnamespace goes;
		 * otherwise, put the contained RTEs and *not* the JOIN into
		 * relnamespace.
		 */
		if (j->alias)
		{
			*relnamespace = list_make1(rte);
			list_free(my_relnamespace);
		}
		else
			*relnamespace = my_relnamespace;

		/*
		 * Include join RTE in returned containedRels set
		 */
		*containedRels = bms_add_member(my_containedRels, j->rtindex);

		return (Node *) j;
	}
	else
		elog(ERROR, "unrecognized node type: %d", (int) nodeTag(n));
	return NULL;				/* can't get here, keep compiler quiet */
}

/*
 * buildMergedJoinVar -
 *	  generate a suitable replacement expression for a merged join column
 */
static Node *
buildMergedJoinVar(ParseState *pstate, JoinType jointype,
				   Var *l_colvar, Var *r_colvar)
{
	Oid			outcoltype;
	int32		outcoltypmod;
	Node	   *l_node,
			   *r_node,
			   *res_node;

	/*
	 * Choose output type if input types are dissimilar.
	 */
	outcoltype = l_colvar->vartype;
	outcoltypmod = l_colvar->vartypmod;
	if (outcoltype != r_colvar->vartype)
	{
		outcoltype = select_common_type(pstate,
										list_make2(l_colvar, r_colvar),
										"JOIN/USING",
										NULL);
		outcoltypmod = -1;		/* ie, unknown */
	}
	else if (outcoltypmod != r_colvar->vartypmod)
	{
		/* same type, but not same typmod */
		outcoltypmod = -1;		/* ie, unknown */
	}

	/*
	 * Insert coercion functions if needed.  Note that a difference in typmod
	 * can only happen if input has typmod but outcoltypmod is -1. In that
	 * case we insert a RelabelType to clearly mark that result's typmod is
	 * not same as input.  We never need coerce_type_typmod.
	 */
	if (l_colvar->vartype != outcoltype)
		l_node = coerce_type(pstate, (Node *) l_colvar, l_colvar->vartype,
							 outcoltype, outcoltypmod,
							 COERCION_IMPLICIT, COERCE_IMPLICIT_CAST, -1);
	else if (l_colvar->vartypmod != outcoltypmod)
		l_node = (Node *) makeRelabelType((Expr *) l_colvar,
										  outcoltype, outcoltypmod,
										  COERCE_IMPLICIT_CAST);
	else
		l_node = (Node *) l_colvar;

	if (r_colvar->vartype != outcoltype)
		r_node = coerce_type(pstate, (Node *) r_colvar, r_colvar->vartype,
							 outcoltype, outcoltypmod,
							 COERCION_IMPLICIT, COERCE_IMPLICIT_CAST, -1);
	else if (r_colvar->vartypmod != outcoltypmod)
		r_node = (Node *) makeRelabelType((Expr *) r_colvar,
										  outcoltype, outcoltypmod,
										  COERCE_IMPLICIT_CAST);
	else
		r_node = (Node *) r_colvar;

	/*
	 * Choose what to emit
	 */
	switch (jointype)
	{
		case JOIN_INNER:

			/*
			 * We can use either var; prefer non-coerced one if available.
			 */
			if (IsA(l_node, Var))
				res_node = l_node;
			else if (IsA(r_node, Var))
				res_node = r_node;
			else
				res_node = l_node;
			break;
		case JOIN_LEFT:
			/* Always use left var */
			res_node = l_node;
			break;
		case JOIN_RIGHT:
			/* Always use right var */
			res_node = r_node;
			break;
		case JOIN_FULL:
			{
				/*
				 * Here we must build a COALESCE expression to ensure that the
				 * join output is non-null if either input is.
				 */
				CoalesceExpr *c = makeNode(CoalesceExpr);

				c->coalescetype = outcoltype;
				c->args = list_make2(l_node, r_node);
				c->location = -1;
				res_node = (Node *) c;
				break;
			}
		default:
			elog(ERROR, "unrecognized join type: %d", (int) jointype);
			res_node = NULL;	/* keep compiler quiet */
			break;
	}

	return res_node;
}


/*
 * transformWhereClause -
 *	  Transform the qualification and make sure it is of type boolean.
 *	  Used for WHERE and allied clauses.
 *
 * constructName does not affect the semantics, but is used in error messages
 */
Node *
transformWhereClause(ParseState *pstate, Node *clause,
					 const char *constructName)
{
	Node	   *qual;

	if (clause == NULL)
		return NULL;

	qual = transformExpr(pstate, clause);

	qual = coerce_to_boolean(pstate, qual, constructName);

	return qual;
}


/*
 * transformLimitClause -
 *	  Transform the expression and make sure it is of type bigint.
 *	  Used for LIMIT and allied clauses.
 *
 * Note: as of Postgres 8.2, LIMIT expressions are expected to yield int8,
 * rather than int4 as before.
 *
 * constructName does not affect the semantics, but is used in error messages
 */
Node *
transformLimitClause(ParseState *pstate, Node *clause,
					 const char *constructName)
{
	Node	   *qual;

	if (clause == NULL)
		return NULL;

	qual = transformExpr(pstate, clause);

	qual = coerce_to_specific_type(pstate, qual, INT8OID, constructName);

	/*
	 * LIMIT can't refer to any vars or aggregates of the current query
	 */
	if (contain_vars_of_level(qual, 0))
	{
		ereport(ERROR,
				(errcode(ERRCODE_INVALID_COLUMN_REFERENCE),
		/* translator: %s is name of a SQL construct, eg LIMIT */
				 errmsg("argument of %s must not contain variables",
						constructName),
				 parser_errposition(pstate,
									locate_var_of_level(qual, 0))));
	}
	if (pstate->p_hasAggs &&
		checkExprHasAggs(qual))
	{
		ereport(ERROR,
				(errcode(ERRCODE_GROUPING_ERROR),
		/* translator: %s is name of a SQL construct, eg LIMIT */
				 errmsg("argument of %s must not contain aggregate functions",
						constructName),
				 parser_errposition(pstate,
									locate_agg_of_level(qual, 0))));
	}
	if (pstate->p_hasWindowFuncs &&
		checkExprHasWindowFuncs(qual))
	{
		ereport(ERROR,
				(errcode(ERRCODE_WINDOWING_ERROR),
		/* translator: %s is name of a SQL construct, eg LIMIT */
				 errmsg("argument of %s must not contain window functions",
						constructName),
				 parser_errposition(pstate,
									locate_windowfunc(qual))));
	}

	return qual;
}


/*
 *	findTargetlistEntry -
 *	  Returns the targetlist entry matching the given (untransformed) node.
 *	  If no matching entry exists, one is created and appended to the target
 *	  list as a "resjunk" node.
 *
 * node		the ORDER BY, GROUP BY, or DISTINCT ON expression to be matched
 * tlist	the target list (passed by reference so we can append to it)
 * clause	identifies clause type being processed
 */
TargetEntry *
findTargetlistEntry(ParseState *pstate, Node *node, List **tlist, int clause)
{
	TargetEntry *target_result = NULL;
	ListCell   *tl;
	Node	   *expr;

	/*----------
	 * Handle two special cases as mandated by the SQL92 spec:
	 *
	 * 1. Bare ColumnName (no qualifier or subscripts)
	 *	  For a bare identifier, we search for a matching column name
	 *	  in the existing target list.	Multiple matches are an error
	 *	  unless they refer to identical values; for example,
	 *	  we allow	SELECT a, a FROM table ORDER BY a
	 *	  but not	SELECT a AS b, b FROM table ORDER BY b
	 *	  If no match is found, we fall through and treat the identifier
	 *	  as an expression.
	 *	  For GROUP BY, it is incorrect to match the grouping item against
	 *	  targetlist entries: according to SQL92, an identifier in GROUP BY
	 *	  is a reference to a column name exposed by FROM, not to a target
	 *	  list column.	However, many implementations (including pre-7.0
	 *	  PostgreSQL) accept this anyway.  So for GROUP BY, we look first
	 *	  to see if the identifier matches any FROM column name, and only
	 *	  try for a targetlist name if it doesn't.  This ensures that we
	 *	  adhere to the spec in the case where the name could be both.
	 *	  DISTINCT ON isn't in the standard, so we can do what we like there;
	 *	  we choose to make it work like ORDER BY, on the rather flimsy
	 *	  grounds that ordinary DISTINCT works on targetlist entries.
	 *
	 * 2. IntegerConstant
	 *	  This means to use the n'th item in the existing target list.
	 *	  Note that it would make no sense to order/group/distinct by an
	 *	  actual constant, so this does not create a conflict with our
	 *	  extension to order/group by an expression.
	 *	  GROUP BY column-number is not allowed by SQL92, but since
	 *	  the standard has no other behavior defined for this syntax,
	 *	  we may as well accept this common extension.
	 *
	 * Note that pre-existing resjunk targets must not be used in either case,
	 * since the user didn't write them in his SELECT list.
	 *
	 * If neither special case applies, fall through to treat the item as
	 * an expression.
	 *----------
	 */
	if (IsA(node, ColumnRef) &&
		list_length(((ColumnRef *) node)->fields) == 1 &&
		IsA(linitial(((ColumnRef *) node)->fields), String))
	{
		char	   *name = strVal(linitial(((ColumnRef *) node)->fields));
		int			location = ((ColumnRef *) node)->location;

		if (clause == GROUP_CLAUSE || clause == PARTITION_CLAUSE)
		{
			/*
			 * In GROUP BY, we must prefer a match against a FROM-clause
			 * column to one against the targetlist.  Look to see if there is
			 * a matching column.  If so, fall through to let transformExpr()
			 * do the rest.  NOTE: if name could refer ambiguously to more
			 * than one column name exposed by FROM, colNameToVar will
			 * ereport(ERROR).	That's just what we want here.
			 *
			 * Small tweak for 7.4.3: ignore matches in upper query levels.
			 * This effectively changes the search order for bare names to (1)
			 * local FROM variables, (2) local targetlist aliases, (3) outer
			 * FROM variables, whereas before it was (1) (3) (2). SQL92 and
			 * SQL99 do not allow GROUPing BY an outer reference, so this
			 * breaks no cases that are legal per spec, and it seems a more
			 * self-consistent behavior.
			 *
			 * Window PARTITION BY clauses should act exactly like GROUP BY.
			 */
			if (colNameToVar(pstate, name, true, location) != NULL)
				name = NULL;
		}

		if (name != NULL)
		{
			foreach(tl, *tlist)
			{
				TargetEntry *tle = (TargetEntry *) lfirst(tl);

				if (!tle->resjunk &&
					strcmp(tle->resname, name) == 0)
				{
					if (target_result != NULL)
					{
						if (!equal(target_result->expr, tle->expr))
							ereport(ERROR,
									(errcode(ERRCODE_AMBIGUOUS_COLUMN),

							/*------
							  translator: first %s is name of a SQL construct, eg ORDER BY */
									 errmsg("%s \"%s\" is ambiguous",
											clauseText[clause], name),
									 parser_errposition(pstate, location)));
					}
					else
						target_result = tle;
					/* Stay in loop to check for ambiguity */
				}
			}
			if (target_result != NULL)
				return target_result;	/* return the first match */
		}
	}
	if (IsA(node, A_Const))
	{
		Value	   *val = &((A_Const *) node)->val;
		int			location = ((A_Const *) node)->location;
		int			targetlist_pos = 0;
		int			target_pos;

		if (!IsA(val, Integer))
			ereport(ERROR,
					(errcode(ERRCODE_SYNTAX_ERROR),
			/* translator: %s is name of a SQL construct, eg ORDER BY */
					 errmsg("non-integer constant in %s",
							clauseText[clause]),
					 parser_errposition(pstate, location)));

		target_pos = intVal(val);
		foreach(tl, *tlist)
		{
			TargetEntry *tle = (TargetEntry *) lfirst(tl);

			if (!tle->resjunk)
			{
				if (++targetlist_pos == target_pos)
					return tle; /* return the unique match */
			}
		}
		ereport(ERROR,
				(errcode(ERRCODE_INVALID_COLUMN_REFERENCE),
		/* translator: %s is name of a SQL construct, eg ORDER BY */
				 errmsg("%s position %d is not in select list",
						clauseText[clause], target_pos),
				 parser_errposition(pstate, location)));
	}

	/*
	 * Otherwise, we have an expression (this is a Postgres extension not
	 * found in SQL92).  Convert the untransformed node to a transformed
	 * expression, and search for a match in the tlist. NOTE: it doesn't
	 * really matter whether there is more than one match.	Also, we are
	 * willing to match a resjunk target here, though the above cases must
	 * ignore resjunk targets.
	 */
	expr = transformExpr(pstate, node);

	foreach(tl, *tlist)
	{
		TargetEntry *tle = (TargetEntry *) lfirst(tl);

		if (equal(expr, tle->expr))
			return tle;
	}

	/*
	 * If no matches, construct a new target entry which is appended to the
	 * end of the target list.	This target is given resjunk = TRUE so that it
	 * will not be projected into the final tuple.
	 */
	target_result = transformTargetEntry(pstate, node, expr, NULL, true);

	*tlist = lappend(*tlist, target_result);

	return target_result;
}

/*
 * transformGroupClause -
 *	  transform a GROUP BY clause
 *
 * GROUP BY items will be added to the targetlist (as resjunk columns)
 * if not already present, so the targetlist must be passed by reference.
 *
 * This is also used for window PARTITION BY clauses (which actually act
 * just the same, except for the clause name used in error messages).
 */
List *
transformGroupClause(ParseState *pstate, List *grouplist,
					 List **targetlist, List *sortClause,
					 bool isPartition)
{
	List	   *result = NIL;
	int			clause = isPartition ? PARTITION_CLAUSE : GROUP_CLAUSE;
	ListCell   *gl;

	foreach(gl, grouplist)
	{
		Node	   *gexpr = (Node *) lfirst(gl);
		TargetEntry *tle;
		bool		found = false;

		tle = findTargetlistEntry(pstate, gexpr, targetlist, clause);

		/* Eliminate duplicates (GROUP BY x, x) */
		if (targetIsInSortList(tle, InvalidOid, result))
			continue;

		/*
		 * If the GROUP BY tlist entry also appears in ORDER BY, copy operator
		 * info from the (first) matching ORDER BY item.  This means that if
		 * you write something like "GROUP BY foo ORDER BY foo USING <<<", the
		 * GROUP BY operation silently takes on the equality semantics implied
		 * by the ORDER BY.  There are two reasons to do this: it improves the
		 * odds that we can implement both GROUP BY and ORDER BY with a single
		 * sort step, and it allows the user to choose the equality semantics
		 * used by GROUP BY, should she be working with a datatype that has
		 * more than one equality operator.
		 */
		if (tle->ressortgroupref > 0)
		{
			ListCell   *sl;

			foreach(sl, sortClause)
			{
				SortGroupClause *sc = (SortGroupClause *) lfirst(sl);

				if (sc->tleSortGroupRef == tle->ressortgroupref)
				{
					result = lappend(result, copyObject(sc));
					found = true;
					break;
				}
			}
		}

		/*
		 * If no match in ORDER BY, just add it to the result using default
		 * sort/group semantics.
		 */
		if (!found)
			result = addTargetToGroupList(pstate, tle,
										  result, *targetlist,
										  exprLocation(gexpr),
										  true);
	}

	return result;
}

/*
 * transformSortClause -
 *	  transform an ORDER BY clause
 *
 * ORDER BY items will be added to the targetlist (as resjunk columns)
 * if not already present, so the targetlist must be passed by reference.
 */
List *
transformSortClause(ParseState *pstate,
					List *orderlist,
					List **targetlist,
					bool resolveUnknown)
{
	List	   *sortlist = NIL;
	ListCell   *olitem;

	foreach(olitem, orderlist)
	{
		SortBy	   *sortby = (SortBy *) lfirst(olitem);
		TargetEntry *tle;

		tle = findTargetlistEntry(pstate, sortby->node,
								  targetlist, ORDER_CLAUSE);

		sortlist = addTargetToSortList(pstate, tle,
									   sortlist, *targetlist, sortby,
									   resolveUnknown);
	}

	return sortlist;
}

/*
 * transformWindowDefinitions -
 *		transform window definitions (WindowDef to WindowClause)
 */
List *
transformWindowDefinitions(ParseState *pstate,
						   List *windowdefs,
						   List **targetlist)
{
	List	   *result = NIL;
	Index		winref = 0;
	ListCell   *lc;

	foreach(lc, windowdefs)
	{
		WindowDef  *windef = (WindowDef *) lfirst(lc);
		WindowClause *refwc = NULL;
		List	   *partitionClause;
		List	   *orderClause;
		WindowClause *wc;

		winref++;

		/*
		 * Check for duplicate window names.
		 */
		if (windef->name &&
			findWindowClause(result, windef->name) != NULL)
			ereport(ERROR,
					(errcode(ERRCODE_WINDOWING_ERROR),
					 errmsg("window \"%s\" is already defined", windef->name),
					 parser_errposition(pstate, windef->location)));

		/*
		 * If it references a previous window, look that up.
		 */
		if (windef->refname)
		{
			refwc = findWindowClause(result, windef->refname);
			if (refwc == NULL)
				ereport(ERROR,
						(errcode(ERRCODE_UNDEFINED_OBJECT),
						 errmsg("window \"%s\" does not exist",
								windef->refname),
						 parser_errposition(pstate, windef->location)));
		}

		/*
		 * Transform PARTITION and ORDER specs, if any.  These are treated
		 * exactly like top-level GROUP BY and ORDER BY clauses, including the
		 * special handling of nondefault operator semantics.
		 */
		orderClause = transformSortClause(pstate,
										  windef->orderClause,
										  targetlist,
										  true);
		partitionClause = transformGroupClause(pstate,
											   windef->partitionClause,
											   targetlist,
											   orderClause,
											   true);

		/*
		 * And prepare the new WindowClause.
		 */
		wc = makeNode(WindowClause);
		wc->name = windef->name;
		wc->refname = windef->refname;

		/*
		 * Per spec, a windowdef that references a previous one copies the
		 * previous partition clause (and mustn't specify its own).  It can
		 * specify its own ordering clause. but only if the previous one had
		 * none.  It always specifies its own frame clause, and the previous
		 * one must not have a frame clause.  (Yeah, it's bizarre that each of
		 * these cases works differently, but SQL:2008 says so; see 7.11
		 * <window clause> syntax rule 10 and general rule 1.)
		 */
		if (refwc)
		{
			if (partitionClause)
				ereport(ERROR,
						(errcode(ERRCODE_WINDOWING_ERROR),
				errmsg("cannot override PARTITION BY clause of window \"%s\"",
					   windef->refname),
						 parser_errposition(pstate, windef->location)));
			wc->partitionClause = copyObject(refwc->partitionClause);
		}
		else
			wc->partitionClause = partitionClause;
		if (refwc)
		{
			if (orderClause && refwc->orderClause)
				ereport(ERROR,
						(errcode(ERRCODE_WINDOWING_ERROR),
				   errmsg("cannot override ORDER BY clause of window \"%s\"",
						  windef->refname),
						 parser_errposition(pstate, windef->location)));
			if (orderClause)
			{
				wc->orderClause = orderClause;
				wc->copiedOrder = false;
			}
			else
			{
				wc->orderClause = copyObject(refwc->orderClause);
				wc->copiedOrder = true;
			}
		}
		else
		{
			wc->orderClause = orderClause;
			wc->copiedOrder = false;
		}
		if (refwc && refwc->frameOptions != FRAMEOPTION_DEFAULTS)
			ereport(ERROR,
					(errcode(ERRCODE_WINDOWING_ERROR),
					 errmsg("cannot override frame clause of window \"%s\"",
							windef->refname),
					 parser_errposition(pstate, windef->location)));
		wc->frameOptions = windef->frameOptions;
		wc->winref = winref;

		result = lappend(result, wc);
	}

	return result;
}

/*
 * transformDistinctClause -
 *	  transform a DISTINCT clause
 *
 * Since we may need to add items to the query's targetlist, that list
 * is passed by reference.
 *
 * As with GROUP BY, we absorb the sorting semantics of ORDER BY as much as
 * possible into the distinctClause.  This avoids a possible need to re-sort,
 * and allows the user to choose the equality semantics used by DISTINCT,
 * should she be working with a datatype that has more than one equality
 * operator.
 */
List *
transformDistinctClause(ParseState *pstate,
						List **targetlist, List *sortClause)
{
	List	   *result = NIL;
	ListCell   *slitem;
	ListCell   *tlitem;

	/*
	 * The distinctClause should consist of all ORDER BY items followed by all
	 * other non-resjunk targetlist items.	There must not be any resjunk
	 * ORDER BY items --- that would imply that we are sorting by a value that
	 * isn't necessarily unique within a DISTINCT group, so the results
	 * wouldn't be well-defined.  This construction ensures we follow the rule
	 * that sortClause and distinctClause match; in fact the sortClause will
	 * always be a prefix of distinctClause.
	 *
	 * Note a corner case: the same TLE could be in the ORDER BY list multiple
	 * times with different sortops.  We have to include it in the
	 * distinctClause the same way to preserve the prefix property. The net
	 * effect will be that the TLE value will be made unique according to both
	 * sortops.
	 */
	foreach(slitem, sortClause)
	{
		SortGroupClause *scl = (SortGroupClause *) lfirst(slitem);
		TargetEntry *tle = get_sortgroupclause_tle(scl, *targetlist);

		if (tle->resjunk)
			ereport(ERROR,
					(errcode(ERRCODE_INVALID_COLUMN_REFERENCE),
					 errmsg("for SELECT DISTINCT, ORDER BY expressions must appear in select list"),
					 parser_errposition(pstate,
										exprLocation((Node *) tle->expr))));
		result = lappend(result, copyObject(scl));
	}

	/*
	 * Now add any remaining non-resjunk tlist items, using default sort/group
	 * semantics for their data types.
	 */
	foreach(tlitem, *targetlist)
	{
		TargetEntry *tle = (TargetEntry *) lfirst(tlitem);

		if (tle->resjunk)
			continue;			/* ignore junk */
		result = addTargetToGroupList(pstate, tle,
									  result, *targetlist,
									  exprLocation((Node *) tle->expr),
									  true);
	}

	return result;
}

/*
 * transformDistinctOnClause -
 *	  transform a DISTINCT ON clause
 *
 * Since we may need to add items to the query's targetlist, that list
 * is passed by reference.
 *
 * As with GROUP BY, we absorb the sorting semantics of ORDER BY as much as
 * possible into the distinctClause.  This avoids a possible need to re-sort,
 * and allows the user to choose the equality semantics used by DISTINCT,
 * should she be working with a datatype that has more than one equality
 * operator.
 */
List *
transformDistinctOnClause(ParseState *pstate, List *distinctlist,
						  List **targetlist, List *sortClause)
{
	List	   *result = NIL;
	List	   *sortgrouprefs = NIL;
	bool		skipped_sortitem;
	ListCell   *lc;
	ListCell   *lc2;

	/*
	 * Add all the DISTINCT ON expressions to the tlist (if not already
	 * present, they are added as resjunk items).  Assign sortgroupref numbers
	 * to them, and make a list of these numbers.  (NB: we rely below on the
	 * sortgrouprefs list being one-for-one with the original distinctlist.
	 * Also notice that we could have duplicate DISTINCT ON expressions and
	 * hence duplicate entries in sortgrouprefs.)
	 */
	foreach(lc, distinctlist)
	{
		Node	   *dexpr = (Node *) lfirst(lc);
		int			sortgroupref;
		TargetEntry *tle;

		tle = findTargetlistEntry(pstate, dexpr,
								  targetlist, DISTINCT_ON_CLAUSE);
		sortgroupref = assignSortGroupRef(tle, *targetlist);
		sortgrouprefs = lappend_int(sortgrouprefs, sortgroupref);
	}

	/*
	 * If the user writes both DISTINCT ON and ORDER BY, adopt the sorting
	 * semantics from ORDER BY items that match DISTINCT ON items, and also
	 * adopt their column sort order.  We insist that the distinctClause and
	 * sortClause match, so throw error if we find the need to add any more
	 * distinctClause items after we've skipped an ORDER BY item that wasn't
	 * in DISTINCT ON.
	 */
	skipped_sortitem = false;
	foreach(lc, sortClause)
	{
		SortGroupClause *scl = (SortGroupClause *) lfirst(lc);

		if (list_member_int(sortgrouprefs, scl->tleSortGroupRef))
		{
			if (skipped_sortitem)
				ereport(ERROR,
						(errcode(ERRCODE_INVALID_COLUMN_REFERENCE),
						 errmsg("SELECT DISTINCT ON expressions must match initial ORDER BY expressions"),
						 parser_errposition(pstate,
								  get_matching_location(scl->tleSortGroupRef,
														sortgrouprefs,
														distinctlist))));
			else
				result = lappend(result, copyObject(scl));
		}
		else
			skipped_sortitem = true;
	}

	/*
	 * Now add any remaining DISTINCT ON items, using default sort/group
	 * semantics for their data types.	(Note: this is pretty questionable; if
	 * the ORDER BY list doesn't include all the DISTINCT ON items and more
	 * besides, you certainly aren't using DISTINCT ON in the intended way,
	 * and you probably aren't going to get consistent results.  It might be
	 * better to throw an error or warning here.  But historically we've
	 * allowed it, so keep doing so.)
	 */
	forboth(lc, distinctlist, lc2, sortgrouprefs)
	{
		Node	   *dexpr = (Node *) lfirst(lc);
		int			sortgroupref = lfirst_int(lc2);
		TargetEntry *tle = get_sortgroupref_tle(sortgroupref, *targetlist);

		if (targetIsInSortList(tle, InvalidOid, result))
			continue;			/* already in list (with some semantics) */
		if (skipped_sortitem)
			ereport(ERROR,
					(errcode(ERRCODE_INVALID_COLUMN_REFERENCE),
					 errmsg("SELECT DISTINCT ON expressions must match initial ORDER BY expressions"),
					 parser_errposition(pstate, exprLocation(dexpr))));
		result = addTargetToGroupList(pstate, tle,
									  result, *targetlist,
									  exprLocation(dexpr),
									  true);
	}

	return result;
}

/*
 * get_matching_location
 *		Get the exprLocation of the exprs member corresponding to the
 *		(first) member of sortgrouprefs that equals sortgroupref.
 *
 * This is used so that we can point at a troublesome DISTINCT ON entry.
 * (Note that we need to use the original untransformed DISTINCT ON list
 * item, as whatever TLE it corresponds to will very possibly have a
 * parse location pointing to some matching entry in the SELECT list
 * or ORDER BY list.)
 */
static int
get_matching_location(int sortgroupref, List *sortgrouprefs, List *exprs)
{
	ListCell   *lcs;
	ListCell   *lce;

	forboth(lcs, sortgrouprefs, lce, exprs)
	{
		if (lfirst_int(lcs) == sortgroupref)
			return exprLocation((Node *) lfirst(lce));
	}
	/* if no match, caller blew it */
	elog(ERROR, "get_matching_location: no matching sortgroupref");
	return -1;					/* keep compiler quiet */
}

/*
 * addTargetToSortList
 *		If the given targetlist entry isn't already in the SortGroupClause
 *		list, add it to the end of the list, using the given sort ordering
 *		info.
 *
 * If resolveUnknown is TRUE, convert TLEs of type UNKNOWN to TEXT.  If not,
 * do nothing (which implies the search for a sort operator will fail).
 * pstate should be provided if resolveUnknown is TRUE, but can be NULL
 * otherwise.
 *
 * Returns the updated SortGroupClause list.
 */
static List *
addTargetToSortList(ParseState *pstate, TargetEntry *tle,
					List *sortlist, List *targetlist, SortBy *sortby,
					bool resolveUnknown)
{
	Oid			restype = exprType((Node *) tle->expr);
	Oid			sortop;
	Oid			eqop;
	bool		reverse;
	int			location;
	ParseCallbackState pcbstate;

	/* if tlist item is an UNKNOWN literal, change it to TEXT */
	if (restype == UNKNOWNOID && resolveUnknown)
	{
		tle->expr = (Expr *) coerce_type(pstate, (Node *) tle->expr,
										 restype, TEXTOID, -1,
										 COERCION_IMPLICIT,
										 COERCE_IMPLICIT_CAST,
										 -1);
		restype = TEXTOID;
	}

	/*
	 * Rather than clutter the API of get_sort_group_operators and the other
	 * functions we're about to use, make use of error context callback to
	 * mark any error reports with a parse position.  We point to the operator
	 * location if present, else to the expression being sorted.  (NB: use the
	 * original untransformed expression here; the TLE entry might well point
	 * at a duplicate expression in the regular SELECT list.)
	 */
	location = sortby->location;
	if (location < 0)
		location = exprLocation(sortby->node);
	setup_parser_errposition_callback(&pcbstate, pstate, location);

	/* determine the sortop, eqop, and directionality */
	switch (sortby->sortby_dir)
	{
		case SORTBY_DEFAULT:
		case SORTBY_ASC:
			get_sort_group_operators(restype,
									 true, true, false,
									 &sortop, &eqop, NULL);
			reverse = false;
			break;
		case SORTBY_DESC:
			get_sort_group_operators(restype,
									 false, true, true,
									 NULL, &eqop, &sortop);
			reverse = true;
			break;
		case SORTBY_USING:
			Assert(sortby->useOp != NIL);
			sortop = compatible_oper_opid(sortby->useOp,
										  restype,
										  restype,
										  false);

			/*
			 * Verify it's a valid ordering operator, fetch the corresponding
			 * equality operator, and determine whether to consider it like
			 * ASC or DESC.
			 */
			eqop = get_equality_op_for_ordering_op(sortop, &reverse);
			if (!OidIsValid(eqop))
				ereport(ERROR,
						(errcode(ERRCODE_WRONG_OBJECT_TYPE),
					   errmsg("operator %s is not a valid ordering operator",
							  strVal(llast(sortby->useOp))),
						 errhint("Ordering operators must be \"<\" or \">\" members of btree operator families.")));
			break;
		default:
			elog(ERROR, "unrecognized sortby_dir: %d", sortby->sortby_dir);
			sortop = InvalidOid;	/* keep compiler quiet */
			eqop = InvalidOid;
			reverse = false;
			break;
	}

	cancel_parser_errposition_callback(&pcbstate);

	/* avoid making duplicate sortlist entries */
	if (!targetIsInSortList(tle, sortop, sortlist))
	{
		SortGroupClause *sortcl = makeNode(SortGroupClause);

		sortcl->tleSortGroupRef = assignSortGroupRef(tle, targetlist);

		sortcl->eqop = eqop;
		sortcl->sortop = sortop;

		switch (sortby->sortby_nulls)
		{
			case SORTBY_NULLS_DEFAULT:
				/* NULLS FIRST is default for DESC; other way for ASC */
				sortcl->nulls_first = reverse;
				break;
			case SORTBY_NULLS_FIRST:
				sortcl->nulls_first = true;
				break;
			case SORTBY_NULLS_LAST:
				sortcl->nulls_first = false;
				break;
			default:
				elog(ERROR, "unrecognized sortby_nulls: %d",
					 sortby->sortby_nulls);
				break;
		}

		sortlist = lappend(sortlist, sortcl);
	}

	return sortlist;
}

/*
 * addTargetToGroupList
 *		If the given targetlist entry isn't already in the SortGroupClause
 *		list, add it to the end of the list, using default sort/group
 *		semantics.
 *
 * This is very similar to addTargetToSortList, except that we allow the
 * case where only a grouping (equality) operator can be found, and that
 * the TLE is considered "already in the list" if it appears there with any
 * sorting semantics.
 *
 * location is the parse location to be fingered in event of trouble.  Note
 * that we can't rely on exprLocation(tle->expr), because that might point
 * to a SELECT item that matches the GROUP BY item; it'd be pretty confusing
 * to report such a location.
 *
 * If resolveUnknown is TRUE, convert TLEs of type UNKNOWN to TEXT.  If not,
 * do nothing (which implies the search for an equality operator will fail).
 * pstate should be provided if resolveUnknown is TRUE, but can be NULL
 * otherwise.
 *
 * Returns the updated SortGroupClause list.
 */
static List *
addTargetToGroupList(ParseState *pstate, TargetEntry *tle,
					 List *grouplist, List *targetlist, int location,
					 bool resolveUnknown)
{
	Oid			restype = exprType((Node *) tle->expr);
	Oid			sortop;
	Oid			eqop;

	/* if tlist item is an UNKNOWN literal, change it to TEXT */
	if (restype == UNKNOWNOID && resolveUnknown)
	{
		tle->expr = (Expr *) coerce_type(pstate, (Node *) tle->expr,
										 restype, TEXTOID, -1,
										 COERCION_IMPLICIT,
										 COERCE_IMPLICIT_CAST,
										 -1);
		restype = TEXTOID;
	}

	/* avoid making duplicate grouplist entries */
	if (!targetIsInSortList(tle, InvalidOid, grouplist))
	{
		SortGroupClause *grpcl = makeNode(SortGroupClause);
		ParseCallbackState pcbstate;

		setup_parser_errposition_callback(&pcbstate, pstate, location);

		/* determine the eqop and optional sortop */
		get_sort_group_operators(restype,
								 false, true, false,
								 &sortop, &eqop, NULL);

		cancel_parser_errposition_callback(&pcbstate);

		grpcl->tleSortGroupRef = assignSortGroupRef(tle, targetlist);
		grpcl->eqop = eqop;
		grpcl->sortop = sortop;
		grpcl->nulls_first = false;		/* OK with or without sortop */

		grouplist = lappend(grouplist, grpcl);
	}

	return grouplist;
}

/*
 * assignSortGroupRef
 *	  Assign the targetentry an unused ressortgroupref, if it doesn't
 *	  already have one.  Return the assigned or pre-existing refnumber.
 *
 * 'tlist' is the targetlist containing (or to contain) the given targetentry.
 */
Index
assignSortGroupRef(TargetEntry *tle, List *tlist)
{
	Index		maxRef;
	ListCell   *l;

	if (tle->ressortgroupref)	/* already has one? */
		return tle->ressortgroupref;

	/* easiest way to pick an unused refnumber: max used + 1 */
	maxRef = 0;
	foreach(l, tlist)
	{
		Index		ref = ((TargetEntry *) lfirst(l))->ressortgroupref;

		if (ref > maxRef)
			maxRef = ref;
	}
	tle->ressortgroupref = maxRef + 1;
	return tle->ressortgroupref;
}

/*
 * targetIsInSortList
 *		Is the given target item already in the sortlist?
 *		If sortop is not InvalidOid, also test for a match to the sortop.
 *
 * It is not an oversight that this function ignores the nulls_first flag.
 * We check sortop when determining if an ORDER BY item is redundant with
 * earlier ORDER BY items, because it's conceivable that "ORDER BY
 * foo USING <, foo USING <<<" is not redundant, if <<< distinguishes
 * values that < considers equal.  We need not check nulls_first
 * however, because a lower-order column with the same sortop but
 * opposite nulls direction is redundant.  Also, we can consider
 * ORDER BY foo ASC, foo DESC redundant, so check for a commutator match.
 *
 * Works for both ordering and grouping lists (sortop would normally be
 * InvalidOid when considering grouping).  Note that the main reason we need
 * this routine (and not just a quick test for nonzeroness of ressortgroupref)
 * is that a TLE might be in only one of the lists.
 */
bool
targetIsInSortList(TargetEntry *tle, Oid sortop, List *sortList)
{
	Index		ref = tle->ressortgroupref;
	ListCell   *l;

	/* no need to scan list if tle has no marker */
	if (ref == 0)
		return false;

	foreach(l, sortList)
	{
		SortGroupClause *scl = (SortGroupClause *) lfirst(l);

		if (scl->tleSortGroupRef == ref &&
			(sortop == InvalidOid ||
			 sortop == scl->sortop ||
			 sortop == get_commutator(scl->sortop)))
			return true;
	}
	return false;
}

/*
 * findWindowClause
 *		Find the named WindowClause in the list, or return NULL if not there
 */
static WindowClause *
findWindowClause(List *wclist, const char *name)
{
	ListCell   *l;

	foreach(l, wclist)
	{
		WindowClause *wc = (WindowClause *) lfirst(l);

		if (wc->name && strcmp(wc->name, name) == 0)
			return wc;
	}

	return NULL;
}


/*
 * transformForecastClause
 *		transform an FORECAST clause
 * 		(annotate a timeseries)
 */
ForecastExpr*
transformForecastClause(ParseState *pstate, ForecastExpr *expr, Query *query)
{
	ListCell   		*tl;
	TargetEntry 	*timeCol 	= NULL;
	TargetEntry 	*tle 		= NULL;
	Oid 			type;
	List			*timeCols = NIL;
	List			*measureCols = NIL;
	List			*categoryCols = NIL;
elog(WARNING,"transform");
	if (expr != NULL)
	{
	
		// TIME COLUMN (TODO: support several time columns)
		if (expr->time)
		{
			timeCol = findTargetlistEntry(pstate, expr->time, &(query->targetList), ORDER_CLAUSE);
			timeCols = lappend_int(timeCols, timeCol->resno);
		}
		else
		{
			foreach(tl, query->targetList)
			{
				tle = (TargetEntry *) lfirst(tl);

				// search for usable time columns
				if (exprType((Node*) tle->expr) == DATEOID)
				{
					timeCol = tle;
					timeCols = lappend_int(timeCols, tle->resno);
				}
			}
			
			if (timeCols == NIL)
			{
				ereport(ERROR, (errcode(ERRCODE_INVALID_PARAMETER_VALUE), errmsg("no suitable time columns found")));
			}
		}
		
		// TIME GRANULARITY
		if (query->groupClause)
		{
			int numAggs = list_length(query->groupClause);
			Assert(numAggs == 1);
						
			// RESTRICTION: only support for year aggregation on one column
			// year
			foreach(tl, query->groupClause) {
				SortGroupClause *sortGroupClause = (SortGroupClause*) lfirst(tl);
				if(sortGroupClause->tleSortGroupRef != timeCol->resno)
				{
					elog(ERROR,"TimeColumn should be part of groupby-clause");
				}
				Assert(sortGroupClause->tleSortGroupRef == timeCol->resno);
				
				// is time column a function expression?, must be year
				if (IsA(timeCol->expr, FuncExpr)) {
					FuncExpr	*funcExpr;
					Const		*constant;
					Var			*var;
					
					funcExpr = (FuncExpr*) timeCol->expr;
					Assert(funcExpr->funcid = 1384);
									
					// check if we really extract the highest granularity, e.g. the year
					constant = (Const*) linitial(funcExpr->args);
					Assert(constant->consttype == 25);
					Assert(strcmp(TextDatumGetCString(constant->constvalue), "year") == 0);
									
					// retrieve original column
					var = (Var*) lsecond(funcExpr->args);
					timeCol->resorigcol = var->varattno;
					timeCol->resorigtbl = ((RangeTblEntry*) list_nth(query->rtable, var->varno-1))->relid;
					
					expr->granularity = year;
				}
				// otherwise we just did a grouping
				else 
				{
					expr->granularity = day;
				}
				
			}
		} 
		else 
		{
			expr->granularity = day;
		}
		
		// MEASURE COLUMN (TODO: support several measures)
		if (expr->measure)
		{
			tle = findTargetlistEntry(pstate, expr->measure, &(query->targetList), ORDER_CLAUSE);
			measureCols = lappend_int(measureCols, tle->resno);
		}
		else
		{
			foreach(tl, query->targetList)
			{
				tle = (TargetEntry *) lfirst(tl);
				type = exprType((Node*) tle->expr);
				
				if ((type == NUMERICOID) || (type == INT2OID) || (type == INT4OID)  || (type == INT8OID) || (type == FLOAT4OID) || (type == FLOAT8OID))
				{
					if (!list_member_int(timeCols, tle->resno))
					{
						measureCols = lappend_int(measureCols, tle->resno);
					}
				}
			}
			
			if (measureCols == NIL)
			{
				ereport(ERROR, (errcode(ERRCODE_INVALID_PARAMETER_VALUE), errmsg("no suitable measure columns found")));
			}
		}
			
			
		// AGGREGATION TYPE
		if (query->groupClause)
		{
			Aggref *agg;
			Var    *var;
						
			// retrieve aggregation of measure column
			Assert(IsA(tle->expr, Aggref));
			agg = (Aggref*) tle->expr;
														
			// TODO: support all aggregation types
			expr->aggType = agg->aggfnoid;
			Assert((expr->aggType == 2111) || (expr->aggType == 2114) || (expr->aggType == 2107) || (expr->aggType == 2108) || (expr->aggType == 2109));
						
			// retrieve original column
			if(nodeTag(agg)==T_Var)
				elog(ERROR,"Aggregation on any columns other than the meassure columns are not allowed.");
			var = (Var*) linitial(agg->args);
			tle->resorigcol = var->varattno;
			tle->resorigtbl = ((RangeTblEntry*) list_nth(query->rtable, var->varno-1))->relid;
		}
		else
		{
			// always assume sum aggregation
			expr->aggType = 2114;
		}

		
		// CATEGORY COLUMNS
		foreach(tl, query->targetList)
		{
			tle = (TargetEntry *) lfirst(tl);
		
			if ((!list_member_int(timeCols, tle->resno)) && (!list_member_int(measureCols, tle->resno)))
			{
				if (exprType((Node*) tle->expr) == INT4OID)
				{
					// category column must be part of the group by clause
					//SortGroupClause *sortGroupClause = (SortGroupClause*) linitial(query->groupClause);					
					//Assert(sortGroupClause->tleSortGroupRef == tle->resno);
					categoryCols = lappend_int(categoryCols, tle->resno);
				}
				else
				{
					ereport(ERROR, (errmsg("error while parsing forecast clause, only integer category columns supported")));
				}
			}
		}

		// set information
		expr->timeCols = timeCols;
		expr->measureCols = measureCols;
		expr->categoryCols = categoryCols;
		expr->whereExpr = query->jointree->quals;
		expr->sourcetext = pstate->p_sourcetext;
	}

	return expr;
}


DecomposeExpr*
transformDecomposeClause(ParseState *pstate, DecomposeExpr *expr, Query *query)
{
	List	   		*r_target 	= NIL;
	TargetEntry 	*tle 		= NULL;
	ModelInfo		*modelInfo	= NULL;
		
	if (expr != NULL)
	{
		// we just take the first column and decompose it
		tle = (TargetEntry *) linitial(query->targetList);
			
		if ((exprType((Node*) tle->expr) == NUMERICOID) || (exprType((Node*) tle->expr) == INT2OID) || (exprType((Node*) tle->expr) == INT4OID) || (exprType((Node*) tle->expr) == FLOAT4OID) || (exprType((Node*) tle->expr) == FLOAT8OID)) {
					
			// create model
			modelInfo = (ModelInfo*) makeNode(ModelInfo);
			modelInfo->measure = tle;
			expr->model = (Node*) modelInfo;
					
			// create columns for trend, season and rest
			tle = (TargetEntry*) copyObject(modelInfo->measure);
			tle->resname = "trend";
			r_target = lappend(r_target, tle);
					
			tle = (TargetEntry*) copyObject(modelInfo->measure);
			tle->resname = "season";
			r_target = lappend(r_target, tle);
										
			tle = (TargetEntry*) copyObject(modelInfo->measure);
			tle->resname = "rest";
			r_target = lappend(r_target, tle);
		}

		expr->targetlist = (Node*) r_target;
	}
	
	return expr;
}

DisAggExpr *
transformDisAggClause(ParseState *pstate, DisAggExpr *expr, Query *query){

			ListCell *clause;
		List *transformedAttributes = NIL;
	if (expr != NULL)
	{
		expr->sourcetext = pstate->p_sourcetext;



		// transform the targetCol into a TargetEntry and store it back
		expr->targetCol = (Node*)findTargetlistEntry(pstate, expr->targetCol, &(query->targetList), ORDER_CLAUSE);

		// we have to convert all the attributes
		foreach(clause, expr->attributes)
		{
			// transform the WHERE-Clause, the raw clause is useless for later concerns
			transformedAttributes = lappend(transformedAttributes, transformWhereClause(pstate, (Node *)(clause->data.ptr_value), "WHERE"));
		}
		expr->attributes = transformedAttributes;
	}
	return expr;
}
