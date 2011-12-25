####################################################
## F2DB Demo						              ##
####################################################
##                                                ##
## November 30, 2011                              ##
##                                                ##
## Contact Information: 						  ##
## Ulrike Fischer (ulrike.fischer@tu-dresden.de)  ##
####################################################


Preconditions:
--------------
1) JDK 1.6 and Ant installed

2) Linux: mozilla-based browser required

3) F2DB backend (pfff) running


Compilation:
------------
1) Execute Ant (build.xml)

2) Specify appropriate SWT library (windows: start.bat, linux: start.sh)

3) Linux: Adapt MOZILLA_FIVE_HOME if necessary (start.sh)


Execution:
----------
1) Execute start.bat (windows) or start.sh (linux)


First Tests:
------------
1) Create a configuration to your database (configuration tab -> database connections)

2) Import some data (configuration tab -> import data)
	- Specify Connectin alias
	- Mark "create/import sales"
	- Execute
	
3) Execute a simple forecast query (console tab)
	- Load SQL Stmt ...
	- sales -> TVForecastStorageOff
	
4) Create a model graph (console tab)
	- Load SQL Stmt ...
	- sales -> SalesModelGraph
	- Model graph configurations: SalesGreedy/SalesTopDown/SalesBottomUp