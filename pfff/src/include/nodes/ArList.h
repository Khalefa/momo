/* Same Interface as List hopefully
 * changes: add new list cell needs integer intex instead listcell
 * arlappend_cell same
 * next operation ist global list based
 * 
 * list copy tail nicht mehr vvorhanden
 */
#ifndef PG_ARLIST_H
#define PG_ARLIST_H

#include "nodes/nodes.h"


typedef struct ArListCell ArListCell;
typedef int(*SortStrategy)(ArListCell*,ArListCell*);
extern int intSorterAsc(ArListCell* lc1,ArListCell* lc2);
extern int intSorterDes(ArListCell* lc1,ArListCell* lc2);

typedef struct ArList
{
	NodeTag		type;			/* T_List, T_IntList, or T_OidList */
	int			length;
	int current;
	int maxLength;
	SortStrategy 	sortStrat;
	ArListCell   **container;
} ArList;

struct ArListCell
{	ArList *root;
	int index;
	union
	{
		void	   *ptr_value;
		int			int_value;
		Oid			oid_value;
	}			data;
};

#define ARNIL						((ArList *) NULL)

#ifdef __GNUC__

static __inline__ ArListCell *
arlist_head(ArList *l)
{
	return l ? l->container[0] : NULL;
}
static __inline__ ArListCell *
arlist_head2(ArList *l)
{
	ArListCell *acc;
	if(l)
		l->current=0;
	acc= l ? l->container[0] : NULL;
	if(acc) acc->index=0;
	return acc;
}

static __inline__ ArListCell *
arlist_tail(ArList *l)
{
	if(!l)
		return NULL;
	l->container[l->length-1]->index = l->length-1;
	return l->container[l->length-1];
}

static __inline__ int
arlist_length(ArList *l)
{
	return l ? l->length : 0;
}
#else

extern ArListCell *arlist_head(ArList *l);
extern ArListCell *arlist_tail(ArList *l);
extern int	arlist_length(ArList *l);
#endif   /* __GNUC__ */

#define arlnext(lc)				((lc)->root->container[++(lc->root->current)])
#define arlnext2(lc)				((lc)->root->container[(lc->root->current+1)])

#define arlnext3(lc)				((lc)->root->container[(lc->index)+1])


#define arlfirst(lc)				((lc)->data.ptr_value)
#define arlfirst_int(lc)			((lc)->data.int_value)
#define arlfirst_oid(lc)			((lc)->data.oid_value)

#define arlinitial(l)				arlfirst(arlist_head(l))
#define arlinitial_int(l)			arlfirst_int(arlist_head(l))
#define arlinitial_oid(l)			arlfirst_oid(arlist_head(l))

#define arlsecond(l)				arlfirst(arlnext(arlist_head(l)))
#define arlsecond_int(l)			arlfirst_int(arlnext(arlist_head(l)))
#define arlsecond_oid(l)			arlfirst_oid(arlnext(arlist_head(l)))

#define arlthird(l)				arlfirst(arlnext(arlnext(arlist_head(l))))
#define arlthird_int(l)			arlfirst_int(arlnext(arlnext(arlist_head(l))))
#define arlthird_oid(l)			arlfirst_oid(arlnext(arlnext(arlist_head(l))))

#define arlfourth(l)				arlfirst(arlnext(arlnext(arlnext(arlist_head(l)))))
#define arlfourth_int(l)			arlfirst_int(arlnext(arlnext(arlnext(arlist_head(l)))))
#define arlfourth_oid(l)			arlfirst_oid(arlnext(arlnext(arlnext(arlist_head(l)))))

#define arllast(l)				arlfirst(arlist_tail(l))
#define arllast_int(l)			arlfirst_int(arlist_tail(l))
#define arllast_oid(l)			arlfirst_oid(arlist_tail(l))


/*
 * Convenience macros for building fixed-length lists
 */
#define arlist_make1(x1)				arlcons(x1, NIL)
#define arlist_get(arlist,index)		(index >= 0 && index<arlist->length) ? (arlfirst(arlist->container[index])) : NULL
#define arlist_make2(x1,x2)			arlcons(x1, arlist_make1(x2))
#define arlist_make3(x1,x2,x3)		arlcons(x1, arlist_make2(x2, x3))
#define arlist_make4(x1,x2,x3,x4)		arlcons(x1, arlist_make3(x2, x3, x4))

#define arlist_make1_int(x1)			arlcons_int(x1, NIL)
#define arlist_make2_int(x1,x2)		arlcons_int(x1, arlist_make1_int(x2))
#define arlist_make3_int(x1,x2,x3)	arlcons_int(x1, arlist_make2_int(x2, x3))
#define arlist_make4_int(x1,x2,x3,x4) arlcons_int(x1, arlist_make3_int(x2, x3, x4))

#define arlist_make1_oid(x1)			arlcons_oid(x1, NIL)
#define arlist_make2_oid(x1,x2)		arlcons_oid(x1, arlist_make1_oid(x2))
#define arlist_make3_oid(x1,x2,x3)	arlcons_oid(x1, arlist_make2_oid(x2, x3))
#define arlist_make4_oid(x1,x2,x3,x4) arlcons_oid(x1, arlist_make3_oid(x2, x3, x4))



/*
 * foreach -
 *	  a convenience macro which loops through the list
 */
#define arforeach(cell, l)	\
	for ((cell) = arlist_head2(l); cell && cell->index<cell->root->length; cell->root->current=cell->index,(cell) = arlnext3(cell),cell->index=cell->root->current+1)

/*
 * for_each_cell -
 *	  a convenience macro which loops through a list starting from a
 *	  specified cell
 */
#define arfor_each_cell(cell, initcell)	\
	for ((cell) = (initcell); (cell) != NULL; (cell) = arlnext(cell))

/*
 * forboth -
 *	  a convenience macro for advancing through two linked lists
 *	  simultaneously. This macro loops through both lists at the same
 *	  time, stopping when either list runs out of elements. Depending
 *	  on the requirements of the call site, it may also be wise to
 *	  assert that the lengths of the two lists are equal.
 */
#define arforboth(cell1, list1, cell2, list2)							\
	for ((cell1) = arlist_head(list1), (cell2) = arlist_head(list2);	\
		 (cell1) != NULL && (cell2) != NULL;						\
		 (cell1) = arlnext(cell1), (cell2) = arlnext(cell2))

extern ArList *arlappend(ArList *list, void *datum);
extern ArList *arlappend_int(ArList *list, int datum);
extern ArList *arlappend_oid(ArList *list, Oid datum);

extern ArList *arlappend2(ArList *list, void *datum,int size);
extern ArList *arlappend_int2(ArList *list, int datum,int size);
extern ArList *arlappend_oid2(ArList *list, Oid datum,int size);

extern ArList *arslinsert(ArList *list, void *datum,SortStrategy sort,int size);
extern ArList *arslinsert_int(ArList *list, int datum,SortStrategy sort,int size);
extern ArList *arslinsert_oid(ArList *list, Oid datum,SortStrategy sort,int size);
extern int arlocateIn(ArList *list,ArListCell* datum,SortStrategy sort);

extern ArListCell *arlappend_cell(ArList *list, int prev, void *datum);
extern ArListCell *arlappend_cell_int(ArList *list, int prev, int datum);
extern ArListCell *arlappend_cell_oid(ArList *list, int prev, Oid datum);

extern ArList *arlcons(void *datum, ArList *list);
extern ArList *arlcons_int(int datum, ArList *list);
extern ArList *arlcons_oid(Oid datum, ArList *list);

extern ArList *arlist_concat(ArList *list1, ArList *list2);
extern ArList *arlist_truncate(ArList *list, int new_size);

extern void *arlist_nth(ArList *list, int n);
extern int	arlist_nth_int(ArList *list, int n);
extern Oid	arlist_nth_oid(ArList *list, int n);

extern bool arlist_member(ArList *list, void *datum);
extern bool arlist_member_ptr(ArList *list, void *datum);
extern bool arlist_member_int(ArList *list, int datum);
extern bool arlist_member_oid(ArList *list, Oid datum);

extern int find_int(ArList *list, int datum, SortStrategy sort);
extern int find_oid(ArList *list, Oid datum, SortStrategy sort);
extern int find(ArList *list, void* datum, SortStrategy sort);

extern ArList *arlist_delete(ArList *list, void *datum);
extern ArList *arlist_delete_ptr(ArList *list, void *datum);
extern ArList *arlist_delete_int(ArList *list, int datum);
extern ArList *arlist_delete_oid(ArList *list, Oid datum);
extern ArList *arlist_delete_first(ArList *list);
extern ArList *arlist_delete_cell(ArList *list, int cell, int prev);

extern ArList *arlist_union(ArList *list1, ArList *list2);
extern ArList *arlist_union_ptr(ArList *list1, ArList *list2);
extern ArList *arlist_union_int(ArList *list1, ArList *list2);
extern ArList *arlist_union_oid(ArList *list1, ArList *list2);

extern ArList *arlist_intersection(ArList *list1, ArList *list2);

/* currently, there's no need for list_intersection_int etc */

extern ArList *arlist_difference(ArList *list1, ArList *list2);
extern ArList *arlist_difference_ptr(ArList *list1, ArList *list2);
extern ArList *arlist_difference_int(ArList *list1, ArList *list2);
extern ArList *arlist_difference_oid(ArList *list1, ArList *list2);

extern ArList *arlist_append_unique(ArList *list, void *datum);
extern ArList *arlist_append_unique_ptr(ArList *list, void *datum);
extern ArList *arlist_append_unique_int(ArList *list, int datum);
extern ArList *arlist_append_unique_oid(ArList *list, Oid datum);

extern ArList *arlist_concat_unique(ArList *list1, ArList *list2);
extern ArList *arlist_concat_unique_ptr(ArList *list1, ArList *list2);
extern ArList *arlist_concat_unique_int(ArList *list1, ArList *list2);
extern ArList *arlist_concat_unique_oid(ArList *list1, ArList *list2);

extern void arlist_free(ArList *list);
extern void arlist_free_deep(ArList *list);

extern ArList *arlist_copy(ArList *list);
extern ArList *arlist_copy_tail(ArList *list, int nskip);

extern int MginSorterAsc(ArListCell* lc1,ArListCell* lc2);
extern int Mgin_Char_SorterAsc(ArListCell* lc1,ArListCell* lc2);
extern int Mgin_Int_SorterAsc(ArListCell* lc1,ArListCell* lc2);
extern int Mgin_Float4_SorterAsc(ArListCell* lc1,ArListCell* lc2);
extern int Mgin_Float8_SorterAsc(ArListCell* lc1,ArListCell* lc2);

extern ArList *Arlist_intersection(ArList *list1, ArList *list2);
#endif   /* PG_LIST_H */
