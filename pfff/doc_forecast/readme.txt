####################################################
## F2DB Backend	(pfff)							  ##
####################################################
##                                                ##
## December 02, 2011                              ##
##                                                ##
## Contact Information: 						  ##
## Ulrike Fischer (ulrike.fischer@tu-dresden.de)  ##
####################################################


Preconditions:
--------------
1) Linux/Unix OS (32bit or 64bit); 

2) gcc, fortran, readline, zlib, bison, flex


Compilation (in main directory pfff/):
--------------------------------------
1) NLOPT
	- cd nlopt-2.2.1
	- ./configure
	- make 
	- make install
	- cd ..

2) Postgres
	- CFLAGS="-lrt -lgomp" ./configure --prefix=[INSTALL_DIR]
	- make
	- make install


Init database:
--------------
1) cd [INSTALL_DIR]/bin

2) ./initdb -D [DATABASE_DIR]


Enable remote access:
---------------------
1) cd [DATABASE_DIR]

2) In file pg_hba.conf
	append "host all all 0.0.0.0/0 trust"
	
3) In file postgresql.conf
	change "#listen_addresses='localhost'" to "listen_addresses='*'"
	
4) open port 5432 of your firewall


Start database:
---------------
1) cd [INSTALL_DIR]/bin

2) ./pg_ctl -D [DATABASE_DIR] -l logfile start

3) ./createdb test

4) ./createuser postgres
