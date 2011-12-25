
#include "postgres.h"

#include "nodes/ArList.h"
#include "nodes/primnodes.h"
#include "forecast/modelGraph/modelGraphNodes.h"
#include "utils/builtins.h"


/*
 * Routines to simplify writing assertions about the type of a list; a
 * NIL list is considered to be an empty list of any type.
 */
#define IsPointerList(l)		((l) == ARNIL || IsA((l), List))
#define IsIntegerList(l)		((l) == ARNIL || IsA((l), IntList))
#define IsOidList(l)			((l) == ARNIL || IsA((l), OidList))

#ifdef USE_ASSERT_CHECKING
/*
 * Check that the specified List is valid (so far as we can tell).
 */
static void
archeck_list_invariants(ArList *list)
{
	if (list == ARNIL)
		return;

	Assert(list->length >= 0);

	Assert(list->type == T_List ||
		   list->type == T_IntList ||
		   list->type == T_OidList);

}
#else
#define archeck_list_invariants(l)
#endif   /* USE_ASSERT_CHECKING */

/*
 * Return a freshly allocated List. Since empty non-NIL lists are
 * invalid, new_list() also allocates the head cell of the new list:
 * the caller should be sure to fill in that cell's data.
 */
static ArList *
arnew_list(NodeTag type)
{
	ArList	   *new_list;
	ArListCell   *new_head;

	new_head = (ArListCell *) palloc(sizeof(*new_head));
	
	/* new_head->data is left undefined! */

	new_list = (ArList *) palloc(sizeof(*new_list));
	new_head->root=new_list;
	new_list->type = type;
	new_list->length = 1;
	new_list->sortStrat=NULL;
	new_list->maxLength=2;
	new_list->current=0;
	new_list->container=(ArListCell **) calloc(new_list->maxLength,sizeof(void*));
	new_list->container[0]=new_head;
	new_list->container[1]=palloc(sizeof(ArListCell));
		new_list->container[1]->index=-1;
		new_list->container[1]->root=new_list;

	return new_list;
}
static ArList *
arnew_list2(NodeTag type,int size)
{
	ArList	   *new_list;
	ArListCell   *new_head;
	int i;
	new_head = (ArListCell *) palloc(sizeof(ArListCell));
	
	
	/* new_head->data is left undefined! */

	new_list = (ArList *) palloc(sizeof(ArList));
	new_head->root=new_list;
	new_list->type = type;
	new_list->length = 1;
	new_list->sortStrat=NULL;
	new_list->maxLength=size;
	new_list->current=0;
	new_list->container=(ArListCell **) calloc((new_list->maxLength+1),sizeof(void*));
	new_list->container[0]=new_head;
	
	for(i=1;i<=size;i++){
	new_list->container[i]=palloc(sizeof(ArListCell));
	new_list->container[i]->index=-1;
	new_list->container[i]->root=new_list;
	}

	return new_list;
}

static void arlist_resize(ArList* list)
{
	list->maxLength+=1;
	list->container=realloc(list->container,(list->maxLength+1)*sizeof(ArListCell*));
	list->container[list->maxLength]=palloc(sizeof(ArListCell));
	list->container[list->maxLength]->index=-1;
	list->container[list->maxLength]->root=list;
}


ArList* arlist_copy(ArList *l1)
{
	int i=0;
	ArList *result=palloc(sizeof(ArList));
	result->maxLength=l1->maxLength;
	result->length=l1->length;
	result->container=malloc(result->maxLength*sizeof(ArListCell*));
	
	for(i=0;i<l1->maxLength;i++)
	{
		result->container[i]=palloc(sizeof(ArListCell));
		(result->container[i])->data=l1->container[i]->data;
		result->container[i]->index=i;
		result->container[i]->root=result;
	}

	return result;
}
/*
 * Allocate a new cell and make it the head of the specified
 * list. Assumes the list it is passed is non-NIL.
 *
 * The data in the new head cell is undefined; the caller should be
 * sure to fill it in
 */
static void
arnew_head_cell(ArList *list)
{
	ArListCell   *new_head;
	
	new_head = (ArListCell *) palloc(sizeof(*new_head));
	new_head->root=list;
	if(list->length>=list->maxLength)
		arlist_resize(list);
	memcpy(&list[1],&list[0],list->length*sizeof(ArListCell*));
	list->container[0]=new_head;
	list->current++;
	list->length++;
}

/*
 * Allocate a new cell and make it the tail of the specified
 * list. Assumes the list it is passed is non-NIL.
 *
 * The data in the new tail cell is undefined; the caller should be
 * sure to fill it in
 */
static void
arnew_tail_cell(ArList *list)
{
	ArListCell   *new_tail;

	new_tail = (ArListCell *) palloc(sizeof(*new_tail));
	new_tail->root=list;
	if(list->length>=list->maxLength)
		arlist_resize(list);
	list->container[list->length]=new_tail;
	list->length++;
}

/*
 * Append a pointer to the list. A pointer to the modified list is
 * returned. Note that this function may or may not destructively
 * modify the list; callers should always use this function's return
 * value, rather than continuing to use the pointer passed as the
 * first argument.
 */
ArList *
arlappend(ArList *list, void *datum)
{
	
	if (list == ARNIL)
		list = arnew_list(T_List);
	else
		arnew_tail_cell(list);

	arlfirst(list->container[list->length-1]) = datum;
	return list;
}

ArList *
arlappend2(ArList *list, void *datum,int size)
{
	
	if (list == ARNIL)
		list = arnew_list2(T_List,size);
	else
		arnew_tail_cell(list);

	arlfirst(list->container[list->length-1]) = datum;
	return list;
}
/*
 * Append an integer to the specified list. See lappend()
 */
ArList *
arlappend_int(ArList *list, int datum)
{


	if (list == ARNIL)
		list = arnew_list(T_IntList);
	else
		arnew_tail_cell(list);

	arlfirst_int(list->container[list->length-1]) = datum;

	return list;
}
ArList *
arlappend_int2(ArList *list, int datum,int size)
{


	if (list == ARNIL)
		list = arnew_list2(T_IntList,size);
	else
		arnew_tail_cell(list);

	arlfirst_int(list->container[list->length-1]) = datum;

	return list;
}

/*
 * Append an OID to the specified list. See lappend()
 */
ArList *
arlappend_oid(ArList *list, Oid datum)
{


	if (list == ARNIL)
		list = arnew_list(T_OidList);
	else
		arnew_tail_cell(list);

	arlfirst_oid(list->container[list->length-1]) = datum;
	return list;
}
ArList *
arlappend_oid2(ArList *list, Oid datum,int size)
{


	if (list == ARNIL)
		list = arnew_list2(T_OidList,size);
	else
		arnew_tail_cell(list);

	arlfirst_oid(list->container[list->length-1]) = datum;
	return list;
}

/*
 * Add a new cell to the list, in the position after 'prev_cell'. The
 * data in the cell is left undefined, and must be filled in by the
 * caller. 'list' is assumed to be non-NIL, and 'prev_cell' is assumed
 * to be non-NULL and a member of 'list'.
 */
static ArListCell *
aradd_new_cell(ArList *list, int prev_cell)
{
	ArListCell   *new_cell;

	if(list->length>=list->maxLength)
		arlist_resize(list);
	new_cell = (ArListCell *) palloc(sizeof(*new_cell));
	/* new_cell->data is left undefined! */
	memmove(&(list->container[prev_cell+1]),&(list->container[prev_cell]),(list->length-prev_cell)*sizeof(ArListCell*));
	list->container[prev_cell]=new_cell;
	new_cell->root=list;
	list->length++;

	return new_cell;
}

/*
 * Add a new cell to the specified list (which must be non-NIL);
 * it will be placed after the list cell 'prev' (which must be
 * non-NULL and a member of 'list'). The data placed in the new cell
 * is 'datum'. The newly-constructed cell is returned.
 */
ArListCell *
arlappend_cell(ArList *list, int prev, void *datum)
{
	ArListCell   *new_cell;

	

	new_cell = aradd_new_cell(list, prev);
	arlfirst(list->container[prev]) = datum;
	return new_cell;
}

ArListCell *
arlappend_cell_int(ArList *list, int prev, int datum)
{
	ArListCell   *new_cell;



	new_cell = aradd_new_cell(list, prev);
	arlfirst_int(list->container[prev]) = datum;
	return new_cell;
}

ArListCell *
arlappend_cell_oid(ArList *list, int prev, Oid datum)
{
	ArListCell   *new_cell;



	new_cell = aradd_new_cell(list, prev);
	arlfirst_oid(list->container[prev]) = datum;

	return new_cell;
}


/*
 * Prepend a new element to the list. A pointer to the modified list
 * is returned. Note that this function may or may not destructively
 * modify the list; callers should always use this function's return
 * value, rather than continuing to use the pointer passed as the
 * second argument.
 *
 * Caution: before Postgres 8.0, the original List was unmodified and
 * could be considered to retain its separate identity.  This is no longer
 * the case.
 */
ArList *
arlcons(void *datum, ArList *list)
{

	if (list == ARNIL)
		list = arnew_list(T_List);
	else
		arnew_head_cell(list);

	arlfirst(list->container[0]) = datum;
	return list;
}

/*
 * Prepend an integer to the list. See lcons()
 */
ArList *
arlcons_int(int datum, ArList *list)
{
	
	if (list == ARNIL)
		list = arnew_list(T_IntList);
	else
		arnew_head_cell(list);

	arlfirst_int(list->container[0]) = datum;
	return list;
}

/*
 * Prepend an OID to the list. See lcons()
 */
ArList *
arlcons_oid(Oid datum, ArList *list)
{
	

	if (list == ARNIL)
		list = arnew_list(T_OidList);
	else
		arnew_head_cell(list);

	arlfirst_oid(list->container[0]) = datum;
	return list;
}

/*
 * Concatenate list2 to the end of list1, and return list1. list1 is
 * destructively changed. Callers should be sure to use the return
 * value as the new pointer to the concatenated list: the 'list1'
 * input pointer may or may not be the same as the returned pointer.
 *
 * The nodes in list2 are merely appended to the end of list1 in-place
 * (i.e. they aren't copied; the two lists will share some of the same
 * storage). Therefore, invoking list_free() on list2 will also
 * invalidate a portion of list1.
 */
ArList *
arlist_concat(ArList *list1, ArList *list2)
{
	if (list1 == ARNIL)
		return list2;
	if (list2 == ARNIL)
		return list1;
	if (list1 == list2)
		elog(ERROR, "cannot list_concat() a list to itself");

	Assert(list1->type == list2->type);

	while(list1->length+list2->length<list1->maxLength)
		arlist_resize(list1);
	memcpy(&list1->container[list2->length],&list2->container[0],list2->length*sizeof(ArListCell*));
	list1->length += list2->length;
	return list1;
}

/*
 * Truncate 'list' to contain no more than 'new_size' elements. This
 * modifies the list in-place! Despite this, callers should use the
 * pointer returned by this function to refer to the newly truncated
 * list -- it may or may not be the same as the pointer that was
 * passed.
 *
 * Note that any cells removed by list_truncate() are NOT pfree'd.
 */
ArList *
arlist_truncate(ArList *list, int new_size)
{
	if(list->length>new_size)
		list->length=new_size;
	return list;
}

/*
 * Locate the n'th cell (counting from 0) of the list.  It is an assertion
 * failure if there is no such cell.
 */
static ArListCell *
arlist_nth_cell(ArList *list, int n)
{

	Assert(list != ARNIL);
	Assert(n >= 0);
	Assert(n < list->length);

	return list->container[n];
}

/*
 * Return the data value contained in the n'th element of the
 * specified list. (List elements begin at 0.)
 */
void *
arlist_nth(ArList *list, int n)
{
	return arlfirst(arlist_nth_cell(list, n));
}

/*
 * Return the integer value contained in the n'th element of the
 * specified list.
 */
int
arlist_nth_int(ArList *list, int n)
{

	return arlfirst_int(arlist_nth_cell(list, n));
}

/*
 * Return the OID value contained in the n'th element of the specified
 * list.
 */
Oid
arlist_nth_oid(ArList *list, int n)
{
	
	return arlfirst_oid(arlist_nth_cell(list, n));
}

/*
 * Return true iff 'datum' is a member of the list. Equality is
 * determined via equal(), so callers should ensure that they pass a
 * Node as 'datum'.
 */
bool
arlist_member(ArList *list, void *datum)
{
	ArListCell   *cell;


	arforeach(cell, list)
	{
		if (equal(arlfirst(cell), datum))
			return true;
	}

	return false;
}

/*
 * Return true iff 'datum' is a member of the list. Equality is
 * determined by using simple pointer comparison.
 */
bool
arlist_member_ptr(ArList *list, void *datum)
{
	ArListCell   *cell;



	arforeach(cell, list)
	{
		if (arlfirst(cell) == datum)
			return true;
	}

	return false;
}

/*
 * Return true iff the integer 'datum' is a member of the list.
 */
bool
arlist_member_int(ArList *list, int datum)
{
	ArListCell   *cell;



	arforeach(cell, list)
	{
		if (arlfirst_int(cell) == datum)
			return true;
	}

	return false;
}

/*
 * Return true iff the OID 'datum' is a member of the list.
 */
bool
arlist_member_oid(ArList *list, Oid datum)
{
	ArListCell   *cell;



	arforeach(cell, list)
	{
		if (arlfirst_oid(cell) == datum)
			return true;
	}

	return false;
}

/*
 * Delete 'cell' from 'list'; 'prev' is the previous element to 'cell'
 * in 'list', if any (i.e. prev == NULL iff list->head == cell)
 *
 * The cell is pfree'd, as is the List header if this was the last member.
 */
ArList *
arlist_delete_cell(ArList *list, int cell, int prev)
{
	Assert(prev >0);

	/*
	 * If we're about to delete the last node from the list, free the whole
	 * list instead and return NIL, which is the only valid representation of
	 * a zero-length list.
	 */
	if (list->length == 1)
	{
		arlist_free(list);
		return ARNIL;
	}

	/*
	 * Otherwise, adjust the necessary list links, deallocate the particular
	 * node we have just removed, and return the list we were given.
	 */
	

	

	pfree(list->container[cell]);
	memcpy(&(list->container[cell]),&list->container[cell+1],(list->length-cell)*sizeof(ArListCell*));
	list->length--;
	return list;
}

/*
 * Delete the first cell in list that matches datum, if any.
 * Equality is determined via equal().
 */
ArList *
arlist_delete(ArList *list, void *datum)
{
	ArListCell   *cell;
	ArListCell   *prev;


	prev = NULL;
	arforeach(cell, list)
	{
		if (equal(arlfirst(cell), datum))
			return arlist_delete_cell(list, cell->index, prev->index);

		prev = cell;
	}

	/* Didn't find a match: return the list unmodified */
	return list;
}

/* As above, but use simple pointer equality */
ArList *
arlist_delete_ptr(ArList *list, void *datum)
{
	ArListCell   *cell;
	ArListCell   *prev;


	prev = NULL;
	arforeach(cell, list)
	{
		if (arlfirst(cell) == datum)
			return arlist_delete_cell(list, cell->index, prev->index);

		prev = cell;
	}

	/* Didn't find a match: return the list unmodified */
	return list;
}

/* As above, but for integers */
ArList *
arlist_delete_int(ArList *list, int datum)
{
	ArListCell   *cell;
	ArListCell   *prev;



	prev = NULL;
	arforeach(cell, list)
	{
		if (arlfirst_int(cell) == datum)
			return arlist_delete_cell(list, cell->index, prev->index);

		prev = cell;
	}

	/* Didn't find a match: return the list unmodified */
	return list;
}

/* As above, but for OIDs */
ArList *
arlist_delete_oid(ArList *list, Oid datum)
{
	ArListCell   *cell;
	ArListCell   *prev;


	prev = NULL;
	arforeach(cell, list)
	{
		if (arlfirst_oid(cell) == datum)
			return arlist_delete_cell(list, cell->index, prev->index);

		prev = cell;
	}

	/* Didn't find a match: return the list unmodified */
	return list;
}

/*
 * Delete the first element of the list.
 *
 * This is useful to replace the Lisp-y code "list = lnext(list);" in cases
 * where the intent is to alter the list rather than just traverse it.
 * Beware that the removed cell is freed, whereas the lnext() coding leaves
 * the original list head intact if there's another pointer to it.
 */
ArList *
arlist_delete_first(ArList *list)
{

	if (list == ARNIL)
		return ARNIL;				/* would an error be better? */

	return arlist_delete_cell(list, ((ArListCell*)arlist_head(list))->index, 0);
}

/*
 * Generate the union of two lists. This is calculated by copying
 * list1 via list_copy(), then adding to it all the members of list2
 * that aren't already in list1.
 *
 * Whether an element is already a member of the list is determined
 * via equal().
 *
 * The returned list is newly-allocated, although the content of the
 * cells is the same (i.e. any pointed-to objects are not copied).
 *
 * NB: this function will NOT remove any duplicates that are present
 * in list1 (so it only performs a "union" if list1 is known unique to
 * start with).  Also, if you are about to write "x = list_union(x, y)"
 * you probably want to use list_concat_unique() instead to avoid wasting
 * the list cells of the old x list.
 *
 * This function could probably be implemented a lot faster if it is a
 * performance bottargetneck.
 */
ArList *
arlist_union(ArList *list1, ArList *list2)
{
	ArList	   *result;
	ArListCell   *cell;


	result = arlist_copy(list1);
	arforeach(cell, list2)
	{
		if (!arlist_member(result, arlfirst(cell)))
			result = arlappend(result, arlfirst(cell));
	}

	
	return result;
}

/*
 * This variant of list_union() determines duplicates via simple
 * pointer comparison.
 */
ArList *
arlist_union_ptr(ArList *list1, ArList *list2)
{
	ArList	   *result;
	ArListCell   *cell;



	result = arlist_copy(list1);
	arforeach(cell, list2)
	{
		if (!arlist_member_ptr(result, arlfirst(cell)))
			result = arlappend(result, arlfirst(cell));
	}

	return result;
}

/*
 * This variant of list_union() operates upon lists of integers.
 */
ArList *
arlist_union_int(ArList *list1, ArList *list2)
{
	ArList	   *result;
	ArListCell   *cell;


	result = arlist_copy(list1);
	arforeach(cell, list2)
	{
		if (!arlist_member_int(result, arlfirst_int(cell)))
			result = arlappend_int(result, arlfirst_int(cell));
	}

	return result;
}

/*
 * This variant of list_union() operates upon lists of OIDs.
 */
ArList *
arlist_union_oid(ArList *list1, ArList *list2)
{
	ArList	   *result;
	ArListCell   *cell;


	result = arlist_copy(list1);
	arforeach(cell, list2)
	{
		if (!arlist_member_oid(result, arlfirst_oid(cell)))
			result = arlappend_oid(result, arlfirst_oid(cell));
	}

	return result;
}

/*
 * Return a list that contains all the cells that are in both list1 and
 * list2.  The returned list is freshly allocated via palloc(), but the
 * cells themselves point to the same objects as the cells of the
 * input lists.
 *
 * Duplicate entries in list1 will not be suppressed, so it's only a true
 * "intersection" if list1 is known unique beforehand.
 *
 * This variant works on lists of pointers, and determines list
 * membership via equal().	Note that the list1 member will be pointed
 * to in the result.
 */
ArList *
Arlist_intersection(ArList *list1, ArList *list2)
{
	ArList	   *result;
	ArListCell   *cell;

	if (list1 == ARNIL || list2 == ARNIL)
		return ARNIL;



	result = ARNIL;
	arforeach(cell, list1)
	{
		if (arlist_member(list2, arlfirst(cell)))
			result = arlappend(result, arlfirst(cell));
	}


	return result;
}

/*
 * Return a list that contains all the cells in list1 that are not in
 * list2. The returned list is freshly allocated via palloc(), but the
 * cells themselves point to the same objects as the cells of the
 * input lists.
 *
 * This variant works on lists of pointers, and determines list
 * membership via equal()
 */
ArList *
arlist_difference(ArList *list1, ArList *list2)
{
	ArListCell   *cell;
	ArList	   *result = ARNIL;

	
	if (list2 == ARNIL)
		return arlist_copy(list1);

	arforeach(cell, list1)
	{
		if (!arlist_member(list2, arlfirst(cell)))
			result = arlappend(result, arlfirst(cell));
	}

	
	return result;
}

/*
 * This variant of list_difference() determines list membership via
 * simple pointer equality.
 */
ArList *
arlist_difference_ptr(ArList *list1, ArList *list2)
{
	ArListCell   *cell;
	ArList	   *result = ARNIL;

	if (list2 == ARNIL)
		return arlist_copy(list1);

	arforeach(cell, list1)
	{
		if (!arlist_member_ptr(list2, arlfirst(cell)))
			result = arlappend(result, arlfirst(cell));
	}

	
	return result;
}

/*
 * This variant of list_difference() operates upon lists of integers.
 */
ArList *
arlist_difference_int(ArList *list1, ArList *list2)
{
	ArListCell   *cell;
	ArList	   *result = ARNIL;

	
	if (list2 == ARNIL)
		return arlist_copy(list1);

	arforeach(cell, list1)
	{
		if (!arlist_member_int(list2, arlfirst_int(cell)))
			result = arlappend_int(result, arlfirst_int(cell));
	}


	return result;
}

/*
 * This variant of list_difference() operates upon lists of OIDs.
 */
ArList *
arlist_difference_oid(ArList *list1, ArList *list2)
{
	ArListCell   *cell;
	ArList	   *result = ARNIL;

	

	if (list2 == ARNIL)
		return arlist_copy(list1);

	arforeach(cell, list1)
	{
		if (!arlist_member_oid(list2, arlfirst_oid(cell)))
			result = arlappend_oid(result, arlfirst_oid(cell));
	}


	return result;
}

/*
 * Append datum to list, but only if it isn't already in the list.
 *
 * Whether an element is already a member of the list is determined
 * via equal().
 */
ArList *
arlist_append_unique(ArList *list, void *datum)
{
	if (arlist_member(list, datum))
		return list;
	else
		return arlappend(list, datum);
}

/*
 * This variant of list_append_unique() determines list membership via
 * simple pointer equality.
 */
ArList *
arlist_append_unique_ptr(ArList *list, void *datum)
{
	if (arlist_member_ptr(list, datum))
		return list;
	else
		return arlappend(list, datum);
}

/*
 * This variant of list_append_unique() operates upon lists of integers.
 */
ArList *
arlist_append_unique_int(ArList *list, int datum)
{
	if (arlist_member_int(list, datum))
		return list;
	else
		return arlappend_int(list, datum);
}

/*
 * This variant of list_append_unique() operates upon lists of OIDs.
 */
ArList *
arlist_append_unique_oid(ArList *list, Oid datum)
{
	if (arlist_member_oid(list, datum))
		return list;
	else
		return arlappend_oid(list, datum);
}

/*
 * Append to list1 each member of list2 that isn't already in list1.
 *
 * Whether an element is already a member of the list is determined
 * via equal().
 *
 * This is almost the same functionality as list_union(), but list1 is
 * modified in-place rather than being copied.	Note also that list2's cells
 * are not inserted in list1, so the analogy to list_concat() isn't perfect.
 */
ArList *
arlist_concat_unique(ArList *list1, ArList *list2)
{
	ArListCell   *cell;

	

	arforeach(cell, list2)
	{
		if (!arlist_member(list1, arlfirst(cell)))
			list1 = arlappend(list1, arlfirst(cell));
	}

	
	return list1;
}

/*
 * This variant of list_concat_unique() determines list membership via
 * simple pointer equality.
 */
ArList *
arlist_concat_unique_ptr(ArList *list1,ArList *list2)
{
	ArListCell   *cell;

	

	arforeach(cell, list2)
	{
		if (!arlist_member_ptr(list1, arlfirst(cell)))
			list1 = arlappend(list1, arlfirst(cell));
	}

	return list1;
}

/*
 * This variant of list_concat_unique() operates upon lists of integers.
 */
ArList *
arlist_concat_unique_int(ArList *list1, ArList *list2)
{
	ArListCell   *cell;


	arforeach(cell, list2)
	{
		if (!arlist_member_int(list1, arlfirst_int(cell)))
			list1 = arlappend_int(list1, arlfirst_int(cell));
	}

	return list1;
}

/*
 * This variant of list_concat_unique() operates upon lists of OIDs.
 */
ArList *
arlist_concat_unique_oid(ArList *list1, ArList *list2)
{
	ArListCell   *cell;



	arforeach(cell, list2)
	{
		if (!arlist_member_oid(list1, arlfirst_oid(cell)))
			list1 = arlappend_oid(list1, arlfirst_oid(cell));
	}

	
	return list1;
}

int find_int(ArList *list, int datum, SortStrategy sort){
	ArListCell *insert;
	int lower=0;
	int value;
	int upper=list->length-1;
	int middle=(lower+upper)/2;;
	
	if(list==ARNIL)
		{
			return -1;
		}
	if((!list->sortStrat) && !sort)
		{
			elog(WARNING,"List does not have any sort strat, insert failed");
			return -1;
		}
	if(!sort)
		sort=list->sortStrat;
	insert=palloc(sizeof(ArListCell));
		arlfirst_int(insert)=datum;
	
	if(lower==upper)
	{
		value=sort(insert,list->container[middle]);
		if(value==0)
			{
				pfree(insert);
				return middle;
				
			}
		else
			{
				pfree(insert);
				return -1;
				
			}
	}
	while(lower<upper)
	{
		if(lower<0) {pfree(insert);return -1;}
		if(upper>=list->length) {pfree(insert);return -1;}
		middle=(lower+upper)/2;
		value=sort(insert,list->container[middle]);
		if(value==0) {pfree(insert);return middle;}
		if(value<0)
			upper=middle-1;
		else
			lower=middle+1;
	}
	middle=(lower+upper)/2;
	value=sort(insert,list->container[middle]);
	if(value==0) {pfree(insert);return middle;}
	pfree(insert);
	return -1;
}

int find_oid(ArList *list, Oid datum, SortStrategy sort){
	ArListCell *insert;
	int lower=0;
	int value;
	int upper=list->length-1;
	int middle=(lower+upper)/2;
	
		if(list==ARNIL)
		{
			return -1;
		}
	if((!list->sortStrat) && !sort)
		{
			elog(WARNING,"List does not hanve any sort strat, insert failed");
			return -1;
		}
	if(!sort)
		sort=list->sortStrat;
	insert=palloc(sizeof(ArListCell));
		arlfirst_int(insert)=datum;
	
	if(lower==upper)
	{
		value=sort(insert,list->container[middle]);
		if(value==0)
			{
				pfree(insert);
				return middle;
				
			}
		else
			{
				pfree(insert);
				return -1;
				
			}
	}
	while(lower<upper)
	{
		if(lower<0) {pfree(insert);return -1;}
		if(upper>=list->length) {pfree(insert);return -1;}
		middle=(lower+upper)/2;
		value=sort(insert,list->container[middle]);
		if(value==0) {pfree(insert);return middle;}
		if(value<0)
			upper=middle-1;
		else
			lower=middle+1;
	}
	middle=(lower+upper)/2;
	value=sort(insert,list->container[middle]);
	if(value==0) {pfree(insert);return middle;}
	pfree(insert);
	return -1;
}


int find(ArList *list, void* datum, SortStrategy sort){
	ArListCell *insert;
	int lower=0,upper,middle,value;
		if(list==ARNIL)
		{
			return -1;
		}
	if((!list->sortStrat) && !sort)
		{
			elog(WARNING,"List does not have any sort strat, insert failed");
			return -1;
		}
	if(!sort)
		sort=list->sortStrat;
	insert=palloc(sizeof(ArListCell));
		arlfirst(insert)=datum;
	lower=0;
	upper=list->length-1;
	middle=(lower+upper)/2;;
	if(lower==upper)
	{
		value=sort(insert,list->container[middle]);
		if(value==0)
			{
				pfree(insert);
				return middle;
				
			}
		else
			{
				pfree(insert);
				return -1;
				
			}
	}
	while(lower<upper)
	{
		if(lower<0) {pfree(insert);return -1;}
		if(upper>=list->length) {pfree(insert);return -1;}
		middle=(lower+upper)/2;
		value=sort(insert,list->container[middle]);
		if(value==0) {pfree(insert);return middle;}
		if(value<0)
			upper=middle-1;
		else
			lower=middle+1;
	}
	middle=(lower+upper)/2;
	value=sort(insert,list->container[middle]);
	if(value==0) {pfree(insert);return middle;}
	pfree(insert);
	return -1;

}

int arlocateIn(ArList *list,ArListCell *object,SortStrategy sort)
{
	
	int lower=0;
	int value;
	int upper=list->length-1;
	int middle=(lower+upper)/2;;	
	if(list==ARNIL)return 0;
	if(lower==upper)
	{
		value=sort(object,list->container[middle]);
		if(value<0)
			return middle;
		else
			return middle+1;
	}
	while(lower<upper)
	{
		if(lower<0) return 0;
		if(upper>=list->length) return list->length-1;
		middle=(lower+upper)/2;
		value=sort(object,list->container[middle]);
		if(value==0) return middle;
		if(value<0)
			upper=middle-1;
		else
			lower=middle+1;
	}
	middle=(lower+upper)/2;
	value=sort(object,list->container[middle]);
	if(middle>=list->length)
		return list->length;
	if (value>0)
		return middle+1;
	return middle;
}

ArList *arslinsert_int(ArList *list, int datum,SortStrategy sort,int size)
{
	ArListCell *insert;
	int index;
	if(list==ARNIL)
		{
			list = arlappend_int2(list,datum,size);
			return list;
		}
	if((!list->sortStrat) && !sort)
		{
			elog(WARNING,"List does not hanve any sort strat, insert failed");
			return list;
		}
		if(!sort) sort=list->sortStrat;
		insert=palloc(sizeof(ArListCell));
		arlfirst_int(insert)=datum;
		insert->root=list;
		index=arlocateIn(list,insert,sort);
		arlappend_cell_int(list,index,datum);
		pfree(insert);
		return list;
		
}
ArList *arslinsert_oid(ArList *list, Oid datum,SortStrategy sort,int size)
{
	ArListCell *insert;
	int index;
	if(list==ARNIL)
		{
			list = arlappend_oid2(list,datum,size);
			return list;
		}
	if((!list->sortStrat) && !sort)
		{
			elog(WARNING,"List does not hanve any sort strat, insert failed");
			return list;
		}
		if(!sort) sort=list->sortStrat;
		insert=palloc(sizeof(ArListCell));
		arlfirst_oid(insert)=datum;
		insert->root=list;
		index=arlocateIn(list,insert,sort);
		arlappend_cell_oid(list,index,datum);
		pfree(insert);
		return list;
		
}
ArList *arslinsert(ArList *list, void* datum,SortStrategy sort,int size)
{
	ArListCell *insert;
	int index;
	if(list==ARNIL)
		{
			list = arlappend2(list,datum,size);
			return list;
		}
	if((!list->sortStrat) && !sort)
		{
			elog(WARNING,"List does not hanve any sort strat, insert failed");
			return list;
		}
		if(!sort) sort=list->sortStrat;
		insert=palloc(sizeof(ArListCell));
		arlfirst(insert)=datum;
		insert->root=list;
		index=arlocateIn(list,insert,sort);
		arlappend_cell(list,index,datum);
		pfree(insert);
		return list;
		
}


/*
 * Free all storage in a list, and optionally the pointed-to elements
 */
static void
arlist_free_private(ArList *list, bool deep)
{

	free(list->container);
	if (list)
		pfree(list);
}

/*
 * Free all the cells of the list, as well as the list itself. Any
 * objects that are pointed-to by the cells of the list are NOT
 * free'd.
 *
 * On return, the argument to this function has been freed, so the
 * caller would be wise to set it to NIL for safety's sake.
 */
void
arlist_free(ArList *list)
{
	arlist_free_private(list, false);
}


int MginSorterAsc(ArListCell* lc1,ArListCell* lc2)
{
	char *av,*bv;
	int i;
	ModelGraphIndexNode *a= arlfirst(lc1);
	ModelGraphIndexNode *b= arlfirst(lc2);
	Assert(((Var*)(a->target->expr))->vartype==((Var*)(b->target->expr))->vartype);
	if(((Var*)(a->target->expr))->vartype==20 || ((Var*)(a->target->expr))->vartype==21 || ((Var*)(a->target->expr))->vartype==23) //integer
		{
			if(a->value<0 && b->value>0)
				return -1;
			if(a->value>0 && b->value<0)
				return 1;
			return a->value-b->value;
		}
	else if(((Var*)(a->target->expr))->vartype==700) //float
	{

		if(a->value<0 && b->value>0)
				return -1;
			if(a->value>0 && b->value<0)
				return 1;
			return a->value-b->value;
	}
	else if(((Var*)(a->target->expr))->vartype==701) //float
	{

		if(a->value<0 && b->value>0)
				return -1;
			if(a->value>0 && b->value<0)
				return 1;
			return a->value-b->value;
	}
	else if(((Var*)(a->target->expr))->vartype==1042) //bpchar
	{
		av=DatumGetCString(DirectFunctionCall1(bpcharout,a->value));
		bv=DatumGetCString(DirectFunctionCall1(bpcharout,b->value));
		i=0;
		while(av[i]!='\0')
		{
			if((int)(av[i])<(int)(bv[i]))
				return -1;
			if((int)(av[i])>(int)(bv[i]))
				return 1;
			++i;
		}
		if(bv[i]=='\0')
			return 0;
		else
			return -1;
	}
	else if(((Var*)(a->target->expr))->vartype==1043) //varchar
	{
		char* av;
		if(a->value==0){
			av="Agg";
		}
		else
			av=DatumGetCString(DirectFunctionCall1(varcharout,a->value));
			bv=DatumGetCString(DirectFunctionCall1(varcharout,b->value));
		i=0;
		while(av[i]!='\0')
		{
			if((int)(av[i])<(int)(bv[i]))
				return -1;
			if((int)(av[i])>(int)(bv[i]))
				return 1;
			++i;
		}
		if(bv[i]=='\0')
			return 0;
		else
			return -1;
	}
	else
	{
		elog(ERROR,"Sortfunction does not know type: %i",((Var*)(a->target->expr))->vartype);
	}

	return 0;
}



int Mgin_Char_SorterAsc(ArListCell* lc1,ArListCell* lc2)
{
	char *av;
	char* bv;
	int i=0;
	ModelGraphIndexNode *b = arlfirst(lc2);
	

	if(((Var *)b->target->expr)->vartype == 1042)
	{
		av = DatumGetCString(DirectFunctionCall1(bpcharout, (Datum)arlfirst(lc1)));
		bv = DatumGetCString(DirectFunctionCall1(bpcharout, b->value));
	}
	else if(((Var *)b->target->expr)->vartype == 1043)
	{
		av = DatumGetCString(DirectFunctionCall1(varcharout, (Datum)arlfirst(lc1)));
		bv = DatumGetCString(DirectFunctionCall1(varcharout, b->value));
	}
	else
		elog(ERROR, "Mgin_Char_Sort is not appropriate for type: %i", ((Var *)b->target->expr)->vartype);

	i=0;
	while(av[i]!='\0')
	{
		if((int)(av[i])<(int)(bv[i]))
			return -1;
		if((int)(av[i])>(int)(bv[i]))
			return 1;
		++i;
	}
	if(bv[i]=='\0')
		return 0;
	else
	{
		while(bv[i]!='\0')
		{
			if(bv[i]!=' ')
				return -1;
			i++;
		}
		
	}

	return 0;
}


int Mgin_Int_SorterAsc(ArListCell* lc1,ArListCell* lc2)
{
	int a= arlfirst_int(lc1);
	ModelGraphIndexNode *b= arlfirst(lc2);
	if(a<0 && b->value>0)
					return -1;
				if(a>0 && b->value<0)
					return 1;
				return a-b->value;
}

//find_float needed!
//int Mgin_Float4_SorterAsc(ArListCell* lc1,ArListCell* lc2)
//{
//	float a= (float)arlfirst(lc1);
//	ModelGraphIndexNode *b= arlfirst(lc2);
//	float bv=DatumGetFloat4(b->value);
//			if(a<0 && b->value>0)
//					return -1;
//				if(a>0 && b->value<0)
//					return 1;
//				return a-b->value;
//}
//int Mgin_Float8_SorterAsc(ArListCell* lc1,ArListCell* lc2)
//{
//	float a= arlfirst(lc1);
//	ModelGraphIndexNode *b= arlfirst(lc2);
//	float bv=DatumGetFloat8(b->value);
//			if(a<0 && b->value>0)
//					return -1;
//				if(a>0 && b->value<0)
//					return 1;
//				return a-b->value;
//}


int intSorterAsc(ArListCell* lc1,ArListCell* lc2)
{
	int a,b;
	if(lc2==NULL) return 0;
	a=arlfirst_int(lc1);
	b=arlfirst_int(lc2);
	if(a<0 && b>0)
		return -1;
	if(a>0 && b<0)
	return 1;
	return a-b;

}
int intSorterDes(ArListCell* lc1,ArListCell* lc2)
{
	int a,b;
	if(lc2==NULL) return 0;
	a=arlfirst_int(lc1);
	b=arlfirst_int(lc2);
	if(a<0 && b>0)
		return 1;
	if(a>0 && b<0)
	return -1;
	return b-a;

}

/*
 * Free all the cells of the list, the list itself, and all the
 * objects pointed-to by the cells of the list (each element in the
 * list must contain a pointer to a palloc()'d region of memory!)
 *
 * On return, the argument to this function has been freed, so the
 * caller would be wise to set it to NIL for safety's sake.
 */
void
arlist_free_deep(ArList *list)
{
	/*
	 * A "deep" free operation only makes sense on a list of pointers.
	 */

	arlist_free_private(list, true);
}




/*
 * When using non-GCC compilers, we can't define these as inline
 * functions in pg_list.h, so they are defined here.
 *
 * TODO: investigate supporting inlining for some non-GCC compilers.
 */
#ifndef __GNUC__

ArListCell *
arlist_head(ArList *l)
{
	return l ? l->container[0] : NULL;
}

ArListCell *
arlist_head2(ArList *l)
{
		if(l)
			l->current=0;
		return l ? l->container[0] : NULL;
}

ArListCell *
arlist_tail(ArList *l)
{
	if(!l)
		return NULL;
	l->container[l->length-1]->index = l->length-1;
	return l->container[l->length-1];
}

int
arlist_length(ArList *l)
{
	return l ? l->length : 0;
}
#endif   /* ! __GNUC__ */


