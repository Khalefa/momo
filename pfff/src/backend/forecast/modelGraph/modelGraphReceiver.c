/*
 * modelGraphReceiver.c
 *
 *  Created on: 15.03.2011
 *      Author: b1anchi
 */

#include "postgres.h"
#include "forecast/modelGraph/modelGraphNodes.h"
#include "nodes/nodeFuncs.h"
#include "forecast/modelGraph/modelGraphReceiver.h"
#include "utils/datum.h"

static void
modelGraphReceiveSlot(TupleTableSlot *slot, DestReceiver *tupledest)
{
	Datum	*values;
	bool	*isnull;
	int 	i;

	slot_getallattrs(slot);

	if(!((ModelGraphState *)tupledest)->tDesc){
		((ModelGraphState *)tupledest)->tDesc = CreateTupleDescCopy(slot->tts_tupleDescriptor);
	}

	values = palloc0(sizeof(Datum)*((ModelGraphState *)tupledest)->tDesc->natts);
	isnull = palloc0(sizeof(bool)*((ModelGraphState *)tupledest)->tDesc->natts);

	for(i = 0; i < ((ModelGraphState *)tupledest)->tDesc->natts; ++i){

		values[i] = datumCopy(slot->tts_values[i], ((ModelGraphState *)tupledest)->tDesc->attrs[i]->attbyval, ((ModelGraphState *)tupledest)->tDesc->attrs[i]->attlen);
		isnull[i] = slot->tts_isnull[i];
	}

	((ModelGraphState *)tupledest)->tupleList = lappend(((ModelGraphState *)tupledest)->tupleList, values);

	((ModelGraphState *)tupledest)->isnullList = lappend(((ModelGraphState *)tupledest)->isnullList, isnull);
}

static void
modelGraphStartupReceiver(DestReceiver *self, int operation, TupleDesc typeinfo)
{
}

static void
modelGraphShutdownReceiver(DestReceiver *self)
{
}

static void
modelGraphDestroyReceiver(DestReceiver *self)
{
	ListCell	*lc;
	foreach(lc, ((ModelGraphState *)self)->tupleList){
		pfree((Datum *)lfirst(lc));
	}
	FreeTupleDesc(((ModelGraphState *)self)->tDesc);
	pfree(self);
}

/*
 * Initially create a ModelDestReceiver
 */
DestReceiver *
CreateModelGraphDestReceiver(void)
{
	ModelGraphState *self = (ModelGraphState *) palloc0(sizeof(ModelGraphState));

	self->pub.rStartup = modelGraphStartupReceiver;
	self->pub.receiveSlot = modelGraphReceiveSlot;
	self->pub.rShutdown = modelGraphShutdownReceiver;
	self->pub.rDestroy = modelGraphDestroyReceiver;
	self->tupleList = NIL;
	self->isnullList = NIL;

	return (DestReceiver *)self;
}
