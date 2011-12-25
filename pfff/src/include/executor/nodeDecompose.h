#ifndef NODEDECOMPOSE_H_
#define NODEDECOMPOSE_H_

#include "nodes/execnodes.h"

extern int	ExecCountSlotsDecompose(Decompose *node);
extern DecomposeState *ExecInitDecompose(Decompose *node, EState *estate, int eflags);
extern TupleTableSlot *ExecDecompose(DecomposeState *node);
extern void ExecEndDecompose(DecomposeState *node);
extern void ExecReScanDecompose(DecomposeState *node, ExprContext *exprCtxt);

Datum GetDatum(ModelInfo *modelState, float value);

#endif /*NODEDECOMPOSE_H_*/
