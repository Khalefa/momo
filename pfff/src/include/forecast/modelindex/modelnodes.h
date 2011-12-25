/*-------------------------------------------------------------------------
 *
 * forecast.h
 *	  Contains general routines and structures to handle forecasting functionality
 *
 * Portions Copyright (c) 1996-2009, PostgreSQL Global Development Group
 * Portions Copyright (c) 1994, Regents of the University of California
 *
 * $PostgreSQL: pgsql/src/include/executer/forecast/modelindex/modelnodes.h,v 1.0 2010/01/14 fischer$
 *-------------------------------------------------------------------------
 */
#ifndef MODELNODES_H_
#define MODELNODES_H_

#include "postgres.h"
#include "nodes/nodes.h"
#include "nodes/primnodes.h"
#include "catalog/pg_type.h"
#include "forecast/modelindex/btree/btree.h"
#include "forecast/algorithm.h"

#define			MAXCONDITIONS   20
#define			MAXJOINS		5
#define			MAXINLISTSIZE	30


typedef enum PredicateType {
	pequal,
	plessthan,
	pgreaterthan,
	pinlist
} PredicateType;


typedef struct ModelTree {
	NodeTag		type;
	List		*children;
	int			numModels;
} ModelTree;


typedef struct TableNode {
	NodeTag		type;
	List		*models;
	List		*children;
	Oid			relid;
} TableNode;

typedef struct ColumnNode {
	NodeTag				type;
	struct hashtable	*equal;
	btree				*lessthan;
	btree				*greaterthan;
	List				*joins;
	TableNode			*parent;
	AttrNumber			no;
} ColumnNode;


typedef struct Predicate {
	NodeTag		type;
	List		*models;
	List 		*connections;
	int			value;
} Predicate;


typedef struct Connection {
	NodeTag		type;
	List		*models;
	List		*connections;
	int			id;
	short		tp;
} Connection;


typedef struct JoinNode {
	NodeTag		type;
	List		*models;
	List		*connections;
	ColumnNode	*leftParent;
	ColumnNode	*rightParent;
} JoinNode;


typedef struct ModelNode {
	NodeTag				type;
	int					id;
	Oid					timerel;
	AttrNumber			time;
	Oid					measurerel;
	AttrNumber			measure;
	Oid					timeType;
	Oid					measureType;
	Oid					agg;
	Granularity			granularity;
	ModelType			method;
	List 				*parameterList;
	
	int					timestamp;
	double				forecast;
	double				evaluation;
	int					ecount;
	double				eval;
	
	// the actual model associated with this node
	Model				*model;
	
	// EXPERIMENTS FOR THE PAPER "Indexing Forecast Models for Matching und Maintenance" 
	char				*sourcetext;
} ModelNode;


typedef struct Condition {
	Oid				table;
	AttrNumber		column;
	PredicateType	type;
	int				values[MAXINLISTSIZE];
} Condition;

#endif /*MODELNODES_H_*/
