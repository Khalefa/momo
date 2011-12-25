
/* A Bison parser, made by GNU Bison 2.4.1.  */

/* Skeleton interface for Bison's Yacc-like parsers in C
   
      Copyright (C) 1984, 1989, 1990, 2000, 2001, 2002, 2003, 2004, 2005, 2006
   Free Software Foundation, Inc.
   
   This program is free software: you can redistribute it and/or modify
   it under the terms of the GNU General Public License as published by
   the Free Software Foundation, either version 3 of the License, or
   (at your option) any later version.
   
   This program is distributed in the hope that it will be useful,
   but WITHOUT ANY WARRANTY; without even the implied warranty of
   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
   GNU General Public License for more details.
   
   You should have received a copy of the GNU General Public License
   along with this program.  If not, see <http://www.gnu.org/licenses/>.  */

/* As a special exception, you may create a larger work that contains
   part or all of the Bison parser skeleton and distribute that work
   under terms of your choice, so long as that work isn't itself a
   parser generator using the skeleton or a modified version thereof
   as a parser skeleton.  Alternatively, if you modify or redistribute
   the parser skeleton itself, you may (at your option) remove this
   special exception, which will cause the skeleton and the resulting
   Bison output files to be licensed under the GNU General Public
   License without this special exception.
   
   This special exception was added by the Free Software Foundation in
   version 2.2 of Bison.  */


/* Tokens.  */
#ifndef YYTOKENTYPE
# define YYTOKENTYPE
   /* Put the tokens into the symbol table, so that GDB and other debuggers
      know about them.  */
   enum yytokentype {
     ABORT_P = 258,
     ABSOLUTE_P = 259,
     ACCESS = 260,
     ACTION = 261,
     ADD_P = 262,
     ADMIN = 263,
     AFTER = 264,
     AGGREGATE = 265,
     ALGORITHM = 266,
     ALL = 267,
     ALSO = 268,
     ALTER = 269,
     ALWAYS = 270,
     ANALYSE = 271,
     ANALYZE = 272,
     AND = 273,
     ANY = 274,
     ARRAY = 275,
     AS = 276,
     ASC = 277,
     ASOF = 278,
     ASSERTION = 279,
     ASSIGNMENT = 280,
     ASYMMETRIC = 281,
     AT = 282,
     ATTRIBUTES = 283,
     AUTHORIZATION = 284,
     AVERAGE = 285,
     BACKWARD = 286,
     BEFORE = 287,
     BEGIN_P = 288,
     BETWEEN = 289,
     BIGINT = 290,
     BINARY = 291,
     BIT = 292,
     BOOLEAN_P = 293,
     BOTH = 294,
     BOTTOMUP = 295,
     BY = 296,
     CACHE = 297,
     CALLED = 298,
     CASCADE = 299,
     CASCADED = 300,
     CASE = 301,
     CAST = 302,
     CATALOG_P = 303,
     CHAIN = 304,
     CHAR_P = 305,
     CHARACTER = 306,
     CHARACTERISTICS = 307,
     CHECK = 308,
     CHECKPOINT = 309,
     CHOOSE = 310,
     CLASS = 311,
     CLOSE = 312,
     CLUSTER = 313,
     COALESCE = 314,
     COLLATE = 315,
     COLUMN = 316,
     COMMENT = 317,
     COMMIT = 318,
     COMMITTED = 319,
     CONCURRENTLY = 320,
     CONFIGURATION = 321,
     CONNECTION = 322,
     CONSTRAINT = 323,
     CONSTRAINTS = 324,
     CONTENT_P = 325,
     CONTINUE_P = 326,
     CONVERSION_P = 327,
     COPY = 328,
     CORRELATION = 329,
     COST = 330,
     CREATE = 331,
     CREATEDB = 332,
     CREATEROLE = 333,
     CREATEUSER = 334,
     CROSS = 335,
     CSV = 336,
     CURRENT_P = 337,
     CURRENT_CATALOG = 338,
     CURRENT_DATE = 339,
     CURRENT_ROLE = 340,
     CURRENT_SCHEMA = 341,
     CURRENT_TIME = 342,
     CURRENT_TIMESTAMP = 343,
     CURRENT_USER = 344,
     CURSOR = 345,
     CYCLE = 346,
     DATA_P = 347,
     DATABASE = 348,
     DAY_P = 349,
     DEALLOCATE = 350,
     DEC = 351,
     DECIMAL_P = 352,
     DECLARE = 353,
     DECOMPOSE = 354,
     DEFAULT = 355,
     DEFAULTS = 356,
     DEFERRABLE = 357,
     DEFERRED = 358,
     DEFINER = 359,
     DELETE_P = 360,
     DELIMITER = 361,
     DELIMITERS = 362,
     DESC = 363,
     DICTIONARY = 364,
     DIMENSION = 365,
     DISABLE_P = 366,
     DISAGGREGATE = 367,
     DISAGGSCHEME = 368,
     DISCARD = 369,
     DISTINCT = 370,
     DO = 371,
     DOCUMENT_P = 372,
     DOMAIN_P = 373,
     DOUBLE_P = 374,
     DROP = 375,
     EACH = 376,
     ELSE = 377,
     ENABLE_P = 378,
     ENCODING = 379,
     ENCRYPTED = 380,
     END_P = 381,
     ENUM_P = 382,
     ESCAPE = 383,
     EXCEPT = 384,
     EXCLUDING = 385,
     EXCLUSIVE = 386,
     EXECUTE = 387,
     EXISTS = 388,
     EXPLAIN = 389,
     EXTERNAL = 390,
     EXTRACT = 391,
     FALSE_P = 392,
     FAMILY = 393,
     FETCH = 394,
     FILL = 395,
     FILLING = 396,
     FIRST = 397,
     FIRST_P = 398,
     FLOAT_P = 399,
     FOLLOWING = 400,
     FOR = 401,
     FORCE = 402,
     FORECAST = 403,
     FOREIGN = 404,
     FORWARD = 405,
     FREEZE = 406,
     FROM = 407,
     FULL = 408,
     FUNCTION = 409,
     GLOBAL = 410,
     GRANT = 411,
     GRANTED = 412,
     GREATEST = 413,
     GREEDY = 414,
     GROUP_P = 415,
     HANDLER = 416,
     HAVING = 417,
     HEADER_P = 418,
     HOLD = 419,
     HOUR_P = 420,
     IDENTITY_P = 421,
     IF_P = 422,
     ILIKE = 423,
     IMMEDIATE = 424,
     IMMUTABLE = 425,
     IMPLICIT_P = 426,
     IN_P = 427,
     INCLUDING = 428,
     INCREMENT = 429,
     INDEX = 430,
     INDEXES = 431,
     INHERIT = 432,
     INHERITS = 433,
     INITIALLY = 434,
     INNER_P = 435,
     INOUT = 436,
     INPUT_P = 437,
     INSENSITIVE = 438,
     INSERT = 439,
     INSTEAD = 440,
     INT_P = 441,
     INTEGER = 442,
     INTERSECT = 443,
     INTERVAL = 444,
     INTO = 445,
     INVOKER = 446,
     IS = 447,
     ISNULL = 448,
     ISOLATION = 449,
     JOIN = 450,
     KEY = 451,
     KEYS = 452,
     LANCOMPILER = 453,
     LANGUAGE = 454,
     LARGE_P = 455,
     LAST_P = 456,
     LC_COLLATE_P = 457,
     LC_CTYPE_P = 458,
     LEADING = 459,
     LEAST = 460,
     LEFT = 461,
     LEVEL = 462,
     LIKE = 463,
     LIMIT = 464,
     LISTEN = 465,
     LOAD = 466,
     LOCAL = 467,
     LOCALTIME = 468,
     LOCALTIMESTAMP = 469,
     LOCATION = 470,
     LOCK_P = 471,
     LOGIN_P = 472,
     MAPPING = 473,
     MATCH = 474,
     MAXVALUE = 475,
     METHOD = 476,
     MINUTE_P = 477,
     MINVALUE = 478,
     MODE = 479,
     MODEL = 480,
     MODELGRAPH = 481,
     MODELINDEX = 482,
     MONTH_P = 483,
     MOVE = 484,
     MULT = 485,
     NAME_P = 486,
     NAMES = 487,
     NATIONAL = 488,
     NATURAL = 489,
     NCHAR = 490,
     NEW = 491,
     NEXT = 492,
     NO = 493,
     NOCREATEDB = 494,
     NOCREATEROLE = 495,
     NOCREATEUSER = 496,
     NOINHERIT = 497,
     NOLOGIN_P = 498,
     NONE = 499,
     NOSUPERUSER = 500,
     NOT = 501,
     NOTHING = 502,
     NOTIFY = 503,
     NOTNULL = 504,
     NOWAIT = 505,
     NULL_P = 506,
     NULLIF = 507,
     NULLS_P = 508,
     NUMBER = 509,
     NUMERIC = 510,
     OBJECT_P = 511,
     OF = 512,
     OFF = 513,
     OFFSET = 514,
     OIDS = 515,
     OLD = 516,
     ON = 517,
     ONLY = 518,
     OPERATOR = 519,
     OPTION = 520,
     OPTIONS = 521,
     OR = 522,
     ORDER = 523,
     OUT_P = 524,
     OUTER_P = 525,
     OVER = 526,
     OVERLAPS = 527,
     OVERLAY = 528,
     OWNED = 529,
     OWNER = 530,
     PARAMETERS = 531,
     PARSER = 532,
     PARTIAL = 533,
     PARTITION = 534,
     PASSWORD = 535,
     PLACING = 536,
     PLANS = 537,
     POSITION = 538,
     PRECEDING = 539,
     PRECISION = 540,
     PRESERVE = 541,
     PREPARE = 542,
     PREPARED = 543,
     PRIMARY = 544,
     PRINT = 545,
     PRIOR = 546,
     PRIVILEGES = 547,
     PROCEDURAL = 548,
     PROCEDURE = 549,
     QUOTE = 550,
     RANGE = 551,
     READ = 552,
     REAL = 553,
     REASSIGN = 554,
     RECHECK = 555,
     RECURSIVE = 556,
     REESTIMATE = 557,
     REFERENCES = 558,
     REINDEX = 559,
     RELATIVE_P = 560,
     RELEASE = 561,
     RENAME = 562,
     REPEATABLE = 563,
     REPLACE = 564,
     REPLICA = 565,
     RESET = 566,
     RESTART = 567,
     RESTORE = 568,
     RESTRICT = 569,
     RETURNING = 570,
     RETURNS = 571,
     REVOKE = 572,
     RIGHT = 573,
     ROLE = 574,
     ROLLBACK = 575,
     ROW = 576,
     ROWS = 577,
     RULE = 578,
     SAVEPOINT = 579,
     SCHEMA = 580,
     SCROLL = 581,
     SEARCH = 582,
     SEASON = 583,
     SECOND_P = 584,
     SECURITY = 585,
     SELECT = 586,
     SEQUENCE = 587,
     SERIALIZABLE = 588,
     SERVER = 589,
     SESSION = 590,
     SESSION_USER = 591,
     SET = 592,
     SETOF = 593,
     SHARE = 594,
     SHOW = 595,
     SIMILAR = 596,
     SIMPLE = 597,
     SMALLINT = 598,
     SOME = 599,
     STABLE = 600,
     STANDALONE_P = 601,
     START = 602,
     STATEMENT = 603,
     STATISTICS = 604,
     STRATEGY = 605,
     STDIN = 606,
     STDOUT = 607,
     STORAGE = 608,
     STORE = 609,
     STRICT_P = 610,
     STRIP_P = 611,
     SUBSTRING = 612,
     SUPERUSER_P = 613,
     SYMMETRIC = 614,
     SYSID = 615,
     SYSTEM_P = 616,
     TABLE = 617,
     TABLESPACE = 618,
     TEMP = 619,
     TEMPLATE = 620,
     TEMPORARY = 621,
     TEXT_P = 622,
     THEN = 623,
     TIME = 624,
     TIMESTAMP = 625,
     TO = 626,
     TOPDOWN = 627,
     TRAILING = 628,
     TRAINING_DATA = 629,
     TRANSACTION = 630,
     TREAT = 631,
     TRIGGER = 632,
     TRIM = 633,
     TRUE_P = 634,
     TRUNCATE = 635,
     TRUSTED = 636,
     TYPE_P = 637,
     UNBOUNDED = 638,
     UNCOMMITTED = 639,
     UNENCRYPTED = 640,
     UNION = 641,
     UNIQUE = 642,
     UNKNOWN = 643,
     UNLISTEN = 644,
     UNTIL = 645,
     UPDATE = 646,
     USER = 647,
     USING = 648,
     VACUUM = 649,
     VALID = 650,
     VALIDATOR = 651,
     VALUE_P = 652,
     VALUES = 653,
     VARCHAR = 654,
     VARIADIC = 655,
     VARYING = 656,
     VERBOSE = 657,
     VERSION_P = 658,
     VIEW = 659,
     VOLATILE = 660,
     WHEN = 661,
     WHERE = 662,
     WHITESPACE_P = 663,
     WINDOW = 664,
     WITH = 665,
     WITHOUT = 666,
     WORK = 667,
     WRAPPER = 668,
     WRITE = 669,
     XML_P = 670,
     XMLATTRIBUTES = 671,
     XMLCONCAT = 672,
     XMLELEMENT = 673,
     XMLFOREST = 674,
     XMLPARSE = 675,
     XMLPI = 676,
     XMLROOT = 677,
     XMLSERIALIZE = 678,
     YEAR_P = 679,
     YES_P = 680,
     ZONE = 681,
     NULLS_FIRST = 682,
     NULLS_LAST = 683,
     WITH_TIME = 684,
     IDENT = 685,
     FCONST = 686,
     SCONST = 687,
     BCONST = 688,
     XCONST = 689,
     Op = 690,
     ICONST = 691,
     PARAM = 692,
     POSTFIXOP = 693,
     UMINUS = 694,
     TYPECAST = 695
   };
#endif



#if ! defined YYSTYPE && ! defined YYSTYPE_IS_DECLARED
typedef union YYSTYPE
{

/* Line 1676 of yacc.c  */
#line 150 "gram.y"

	int					ival;
	char				chr;
	char				*str;
	const char			*keyword;
	bool				boolean;
	JoinType			jtype;
	DropBehavior		dbehavior;
	OnCommitAction		oncommit;
	List				*list;
	Node				*node;
	Value				*value;
	ObjectType			objtype;

	TypeName			*typnam;
	FunctionParameter   *fun_param;
	FunctionParameterMode fun_param_mode;
	FuncWithArgs		*funwithargs;
	DefElem				*defelt;
	SortBy				*sortby;
	WindowDef			*windef;
	JoinExpr			*jexpr;
	IndexElem			*ielem;
	Alias				*alias;
	RangeVar			*range;
	IntoClause			*into;
	WithClause			*with;
	A_Indices			*aind;
	ResTarget			*target;
	struct PrivTarget	*privtarget;
	AccessPriv			*accesspriv;

	InsertStmt			*istmt;
	VariableSetStmt		*vsetstmt;
	
	GraphAttribute		*graphAttr;



/* Line 1676 of yacc.c  */
#line 532 "gram.h"
} YYSTYPE;
# define YYSTYPE_IS_TRIVIAL 1
# define yystype YYSTYPE /* obsolescent; will be withdrawn */
# define YYSTYPE_IS_DECLARED 1
#endif

extern YYSTYPE base_yylval;

#if ! defined YYLTYPE && ! defined YYLTYPE_IS_DECLARED
typedef struct YYLTYPE
{
  int first_line;
  int first_column;
  int last_line;
  int last_column;
} YYLTYPE;
# define yyltype YYLTYPE /* obsolescent; will be withdrawn */
# define YYLTYPE_IS_DECLARED 1
# define YYLTYPE_IS_TRIVIAL 1
#endif

extern YYLTYPE base_yylloc;

