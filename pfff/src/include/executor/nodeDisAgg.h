/*
 * nodeDisAgg.h
 *
 *  Created on: 31.12.2010
 *      Author: b1anchi
 */

#ifndef NODEDISAGG_H_
#define NODEDISAGG_H_

#include "nodes/execnodes.h"
#include "nodes/parsenodes.h"

extern DisAggStrategy getDisAggStrat(int startIdentifier);
extern HeapTuple DisAggStratMult(Datum *values, bool *isnull, TupleDesc outTdesc, int column, A_Const *key);
extern HeapTuple DisAggStratDiv(Datum *values, bool *isnull, TupleDesc outTdesc, int column, A_Const *key);
extern HeapTuple DisAggStratAdd(Datum *values, bool *isnull, TupleDesc outTdesc, int column, A_Const *key);
extern HeapTuple DisAggStratSub(Datum *values, bool *isnull, TupleDesc outTdesc, int column, A_Const *key);
extern HeapTuple DisAggStratIdentity(Datum *values, bool *isnull, TupleDesc outTdesc, int column, A_Const *key);

extern int ExecCountSlotsDisAgg(DisAgg *node);
extern DisAggState *ExecInitDisAgg(DisAgg *node, EState *estate, int eflags);
extern TupleTableSlot *ExecDisAgg(DisAggState *node);
extern void ExecEndDisAgg(DisAggState *node);

#endif /* NODEDISAGG_H_ */
