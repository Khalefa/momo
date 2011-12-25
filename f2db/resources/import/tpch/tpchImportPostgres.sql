---------------------------------------------------------------------------------------------------
-- Erstellt alle Tabellen des TPC-H Benchmarks.
---------------------------------------------------------------------------------------------------

-- Tabellen anlegen -------------------------------------------------------------------------------
drop table lineitem CASCADE;
drop table orders CASCADE;
drop table customer CASCADE;
drop table partsupp CASCADE;
drop table part CASCADE;
drop table supplier CASCADE;
drop table nation CASCADE;
drop table region CASCADE;








create table nation (
	n_nationkey  integer not null,
	n_name       char(25) not null,
	n_regionkey  integer not null,
	n_comment    varchar(152)
);

create table region (
	r_regionkey  integer not null,
	r_name       char(25) not null,
	r_comment    varchar(152)
);

create table part (
	p_partkey     integer not null,
	p_name        varchar(55) not null,
	p_mfgr        char(25) not null,
	p_brand       char(10) not null,
	p_type        varchar(25) not null,
	p_size        integer not null,
	p_container   char(10) not null,
	p_retailprice decimal(15,2) not null,
	p_comment     varchar(23) not null
);

create table supplier (
	s_suppkey     integer not null,
	s_name        char(25) not null,
	s_address     varchar(40) not null,
	s_nationkey   integer not null,
	s_phone       char(15) not null,
	s_acctbal     decimal(15,2) not null,
	s_comment     varchar(101) not null
);

create table partsupp (
	ps_partkey     integer not null,
	ps_suppkey     integer not null,
	ps_availqty    integer not null,
	ps_supplycost  decimal(15,2)  not null,
	ps_comment     varchar(199) not null
);

create table customer (
	c_custkey     integer not null,
	c_name        varchar(25) not null,
	c_address     varchar(40) not null,
	c_nationkey   integer not null,
	c_phone       char(15) not null,
	c_acctbal     decimal(15,2)   not null,
	c_mktsegment  char(10) not null,	
	c_comment     varchar(117) not null
);

create table orders (
	o_orderkey       integer not null,
	o_custkey        integer not null,
	o_orderstatus    char(1) not null,
	o_totalprice     decimal(15,2) not null,
	o_orderdate      date not null,
	o_orderpriority  char(15) not null,
	o_clerk          char(15) not null,
	o_shippriority   integer not null,
	o_comment        varchar(79) not null
);

create table lineitem (
	l_orderkey    integer not null,
	l_partkey     integer not null,
	l_suppkey     integer not null,
	l_linenumber  integer not null,
	l_quantity    decimal(15,2) not null,
	l_extendedprice  decimal(15,2) not null,
	l_discount    decimal(15,2) not null,
	l_tax         decimal(15,2) not null,
	l_returnflag  char(1) not null,
	l_linestatus  char(1) not null,
	l_shipdate    varchar(30) not null,
	l_commitdate  varchar(30) not null,
	l_receiptdate varchar(30) not null,
	l_shipinstruct char(25) not null,
	l_shipmode     char(10) not null,
	l_comment      varchar(44) not null
);

---------------------------------------------------------------------------------------------------
-- Erstellt alle Primär- und Fremdschlüsselconstraints für den TPC-H Benchmarks.
---------------------------------------------------------------------------------------------------


-- Constraints anlegen ----------------------------------------------------------------------------

-- Primaerschluessel

alter table orders
add constraint orders_pk primary key (o_orderkey);

alter table customer 
add constraint customer_pk primary key (c_custkey);

alter table part 
add constraint part_pk primary key (p_partkey);

alter table supplier 
add constraint supplier_pk primary key (s_suppkey);

alter table partsupp 
add constraint partsupp_pk primary key (ps_partkey, ps_suppkey);

alter table lineitem 
add constraint lineitem_pk primary key (l_orderkey, l_linenumber);

alter table nation 
add constraint nation_pk primary key (n_nationkey);

alter table region 
add constraint region_pk primary key (r_regionkey);

-- Fremdschluessel

alter table nation 
add constraint nation_fk_1 foreign key (n_regionkey) references region (r_regionkey);

alter table lineitem 
add constraint lineitem_fk_1 foreign key (l_orderkey) references orders (o_orderkey);

alter table lineitem 
add constraint lineitem_fk_2 
foreign key (l_partkey, l_suppkey) references partsupp (ps_partkey, ps_suppkey);

alter table lineitem 
add constraint lineitem_fk_3 foreign key (l_partkey) references part (p_partkey);

alter table lineitem 
add constraint lineitem_fk_4 foreign key (l_suppkey) references supplier (s_suppkey);

alter table partsupp 
add constraint partsupp_fk_1 foreign key (ps_partkey) references part (p_partkey);

alter table partsupp 
add constraint partsupp_fk_2 foreign key (ps_suppkey) references supplier (s_suppkey);

alter table customer 
add constraint customer_fk_1 foreign key (c_nationkey) references nation (n_nationkey);

alter table supplier 
add constraint supplier_fk_1 foreign key (s_nationkey) references nation (n_nationkey);

alter table orders 
add constraint orders_fk_1 foreign key (o_custkey) references customer (c_custkey);

COPY region FROM '#PATH#region.tbl' WITH DELIMITER '|';
COPY nation FROM '#PATH#nation.tbl' WITH DELIMITER '|';
COPY supplier FROM '#PATH#supplier.tbl' WITH DELIMITER '|';
COPY part FROM '#PATH#part.tbl' WITH DELIMITER '|';
COPY customer FROM '#PATH#customer.tbl' WITH DELIMITER '|';
COPY partsupp FROM '#PATH#partsupp.tbl' WITH DELIMITER '|';
COPY orders FROM '#PATH#orders.tbl' WITH DELIMITER '|';
COPY lineitem FROM '#PATH#lineitem.tbl' WITH DELIMITER '|';
