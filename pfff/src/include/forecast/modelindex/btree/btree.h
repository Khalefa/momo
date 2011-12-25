#ifndef _BTREE_H_
#define _BTREE_H_

// Platform dependent headers
#include <stdlib.h>
#include <stdio.h>
#include <strings.h>
#include "c.h"
#include "utils/palloc.h"

#define mem_alloc palloc
#define mem_free pfree
#define bcopy bcopy

typedef struct {
        int  key;
        void * val;
} bt_key_val;

typedef struct bt_node {
	struct bt_node * next;		// Pointer used for linked list 
	bool leaf;			// Used to indicate whether leaf or not
    unsigned int nr_active;		// Number of active keys
	unsigned int level;		// Level in the B-Tree
    bt_key_val ** key_vals; 	// Array of keys and values
    struct bt_node ** children;	// Array of pointers to child nodes
        
    void *parent; 
}bt_node;

typedef struct {
	unsigned int order;			// B-Tree order
	bt_node * root;				// Root of the B-Tree
	unsigned int (*value)(int key);	// Generate uint value for the key
        unsigned int (*key_size)(int key);    // Return the key size
        unsigned int (*data_size)(void * data);  // Return the data size
	void (*print_key)(int key);		// Print the key
}btree; 

extern btree * btree_create(unsigned int order);
extern int btree_insert_key(btree * btree, bt_key_val * key_val);
extern int btree_delete_key(btree * btree,bt_node * subtree ,int key);
extern bt_key_val * btree_search(btree * btree,  int key);
extern void btree_destroy(btree * btree);
extern int btree_get_max_key(btree * btree);
extern int btree_get_min_key(btree * btree);

//#ifdef DEBUG
extern void print_subtree(btree * btree,bt_node * node);
//#endif


#endif