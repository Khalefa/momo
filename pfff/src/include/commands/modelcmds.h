#include "nodes/parsenodes.h"
#include "tcop/dest.h"

void CreateModel(CreateModelStmt *stmt,const char *queryString, DestReceiver *dest, char *completionTag);
