package modules.databaseif;

import java.io.BufferedInputStream;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;

import modules.Module;
import modules.generic.DemoEvents;
import modules.generic.GenericModel;
import modules.generic.GenericModelChangeEvent;
import modules.generic.GenericModelChangeListener;
import modules.misc.ModuleRegistry;
import modules.misc.StringUtils;

import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;

import components.dialogs.ExceptionDialog;
import components.dialogs.InExecutionDialog;

import data.iterator.input.ResultSetIterator;
import data.type.Value;

/**
 * This class encapsulates the whole stuff to communicate
 * with derby databases over JDBC. It offers convenient methods to
 * handle the database:
 * <p><code>
 * <li> connect(), connects to a database <br/>
 * <li> disconnect(), disconnects from a database <br/>
 * <li> testConnection(), tests a database connection <br/>
 * <li> executeStatementXXX() methods offer easy setup and execution of statements ... <br/>
 * </code></p>
 * @author Felix Beyer, Sebastian Seifert, Christopher Schildt
 * @date   10.04.2006
 *
 */
public class JDBCInterface implements Module,
									  ConstantStatements,
									  GenericModel{

    // -----------------------------------------------------------
    // Constants

	// Connection States
	/** status code of DISCONNECTED State */
    public static final int DISCONNECTED = 0;

    /** status code of CONNECTED State */
	public static final int CONNECTED = 1;

    // ---------------------------------------------
	// member variables

    /** the singleton instance */
	private static JDBCInterface singleton;

    /** the current state of the connection */
    private int state;

//    /** flag for tree extraction, true if active, otherwise false */
//	private boolean treeExtraction;
//
//    /** flag for query plan explanation, true if on, false otherwise */
//    private boolean qepExplanation;
//
//    /** flag for query plan explanation, true if on, false otherwise */
//    private boolean qepExplanationMode;
//
//    /** flag for the current set timing mode */
//    private boolean timingMode;

    /** flag for the current set workload mode */
    private boolean workloadMode;
    
    /** flag for the current set workload mode */
    private boolean postgres_connected;
    
    /** the current active connection */
    private Connection currentConnection;

    /** the associated ConnectionInfo object of the current connection*/
    private ConnectionInfo currentConInfo;

    /** the current Statement for reuse */
    private Statement currentStatement;

    /** the current first exception of the last failed stmt execution */
	private Exception currentException;

	/** flag if embedded engine started */
    private boolean startedEmbeddedEngine = false;

    /** backup reference of main shell for the InExecutionDialog */
    private Shell shell;

    /** an array for the different listeners of this module */
	private List<GenericModelChangeListener> listeners;

    /** an associative map of prepared statements */
    private Map<String,PreparedStatement> preparedStatements;

    /** a map containing the tablenames of the indexes */
    private Map<String, Object> tableNames;
    
    private String[] optim_optnames;

    public Statement getCurrentStatement() {
        return currentStatement;
    }

    public void setCurrentStatement(Statement currentStatement) {
        this.currentStatement = currentStatement;
    }
    

	/** private constructor, singleton ! */
	private JDBCInterface(){
		listeners = new ArrayList<GenericModelChangeListener>();
        preparedStatements = new HashMap<String,PreparedStatement>();
        this.postgres_connected=true;
	}

	/** @return Instance of a JDBCInterface */
	public static JDBCInterface getInstance(){
		if(singleton == null) {
			singleton = new JDBCInterface();
			ModuleRegistry.addModule(singleton);
		}
		return singleton;
	}

    /** @return the current connection info object of the current connection */
	public ConnectionInfo getCurrentConnectionInfo(){
		return this.currentConInfo;
	}

    /** returns the current connection status as constants,
     *  defined through constants of this class.
     *  either CONNECTED, or DISCONNECTED
     * @return the status of th connection
     */
	public int getConnectionStatus(){
		return state;
	}

    /**
	 * sets the parent shell, only used once right after initialization of main app
	 * @param shell the parent shell to use for the execution dialog
	 */
	public void setShell(Shell shell){
		this.shell = shell;
	}

    /** returns the current main app shell */
	public Shell getShell(){
		return this.shell;
	}


	/**
	 * closes current connection (if present), disconnects, releases system resources and
	 * shuts down embedded derby engine, if necessary.
	 *
	 */
	public void shutdown(){

        // check if we are still connected
		if(state == CONNECTED){
			disconnect();
		}

		// shutdown embedded derby
		boolean gotSQLExc = false;
        if (this.startedEmbeddedEngine){
            // try to shutdown derby
        	try{
                DriverManager.getConnection("jdbc:derby:;shutdown=true");
            }catch (SQLException se){
                gotSQLExc = true;
            }

            if (!gotSQLExc){
            	System.out.println("Database did not shut down normally!");
            } else{
                System.out.println("Database shut down normally.");
            }
        }
	}
	/**
     * This method sets the locale virtual machine system properties:
     * <li> user.language describing the user`s language
     * <li> and user.country describing the region the user resides <br>
     * Furthermore it sets the default locale of the virtual machine.
     * @param con the connection info object holding the locale info to set
	 */
    private void setLocaleVMProperties(ConnectionInfo con){
        String locale = con.getLocale();
        if(locale!=null){
            int index = locale.indexOf("_");
            String language = locale.substring(0,index);
            String region   = locale.substring(index+1);
            Locale.setDefault(new Locale(language,region));
            System.setProperty("user.language",language);
            System.setProperty("user.country", region);
            System.out.println("Language:"+ language + "; Region:"+region);
        }
    }

	/**
	 * connects to a database using Connection Information specified
	 * @param con the ConnectionInfo Object describing all parameters needed for
	 * a successful connection.
	 * @return true if connect() was successful, otherwise false
	 */
	public boolean connect(ConnectionInfo con){
        try {
			// first check if we are still connected
        	if(this.state == CONNECTED){
				// disconnect from the old db
        		this.disconnect();
			}
            String connectString = "";
            // load appropriate Derby JDBC driver (only embedded supported for now)
        	if(con.isEmbedded()){
        	    Class.forName(con.getDriver()).newInstance();
        	    Class.forName("com.ibm.db2.jcc.DB2Driver");
        	    Class.forName("org.postgresql.Driver");
        	    connectString = con.getConnectionString();
                setLocaleVMProperties(con);
                //System.out.println("Connection: "+connectString);
                currentConnection = DriverManager.getConnection(connectString,con.getConnectionProperties());
            } else {
                    Class.forName(con.getDriver());
                    connectString = con.getConnectionString();
                    Properties props = con.getConnectionProperties();
                    currentConnection = DriverManager.getConnection(connectString, props);
            }

            System.out.println("Connected...");

            // set state to connected
            state = CONNECTED;

            // save connectionInfo object
            currentConInfo = con;

            // set flag if embedded engine started
            if (con.isEmbedded()) startedEmbeddedEngine = true;

            // set autocommit mode
            currentConnection.setAutoCommit(con.isAutoCommit());

            // create a new statement for reuse
            currentStatement = currentConnection.createStatement();

            // fill table names map
            fillTableNamesMap();
            
            filloptimMethods();

            // fire a Connection_Established Event
            GenericModelChangeEvent event = new GenericModelChangeEvent();
            event.detail = DemoEvents.CONNECTION_ESTABLISHED;
            event.source = toString();
   
            fireModelChangeEvent(event);
 
            	


		} catch (InstantiationException e) {
			System.out.println("Could not retrieve a new instance of "+con.getDriver());
			ExceptionDialog.show(shell, e, true);
			return false;
		} catch (IllegalAccessException e) {
			System.out.println("Illegal access "+con.getDbname());
			ExceptionDialog.show(shell, e, true);
			return false;
		} catch (ClassNotFoundException e) {
			System.out.println("Class "+con.getDriver()+" was not found in Classpath.");
			ExceptionDialog.show(shell, e, true);
			return false;
		} catch (SQLException e){
			System.out.println("Could not get a connection.");
			ExceptionDialog.show(shell, e, true);
			return false;
		} catch (Exception e){
			System.out.println("An exception occur.");
			ExceptionDialog.show(shell, e, true);
			return false;
		}
		return true;
	}

	private void filloptimMethods() {

		if (!postgres_connected) 
			return;
		executeStatementImmediateWithoutQueryTreeExtraction(GET_OPTIMNAMESPOSTGRES);

		ResultSet rs = getResultSet();
		try {
			rs.next();
			String test=rs.getString(1);
			rs.close();

			this.setOptim_optnames(test.split("[\\{,\\}]"));
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		
		
		
		}

		
		

	/**
	 * disconnects from the current database
	 *
	 */
	public void disconnect(){
		try {
			if(state == CONNECTED){
				// do a commit if autocommit was turned off
				if(!currentConInfo.isAutoCommit()){
					currentConnection.commit();
				}
				// close connection
				currentConnection.close();
				state = DISCONNECTED;

				// dispose all infos about last connection
				currentConInfo = null;
				currentConnection = null;
				currentStatement = null;

                // clear PreparedStmt map
                preparedStatements.clear();

				System.out.println("Disconnected...");

				// fire a connection_closed event
				GenericModelChangeEvent event = new GenericModelChangeEvent();
	            event.detail = DemoEvents.CONNECTION_CLOSED;
	            event.source = this.toString();
	            fireModelChangeEvent(event);
			}
		} catch (SQLException e) {
			System.out.println("SQLException occured during disconnect...");
			ExceptionDialog.show(shell, e, true);
		} catch (Exception e){
			System.out.println("An exception occur.");
			ExceptionDialog.show(shell, e, true);
		}
	}


        	/**
	 * imports data to a given connection
	 * @param con the ConnectionInfo Object for import
         * @param me the bool flag to specify if meregio data should be imported
         * @param sales the bool flag to specify if sales data should be imported
         * @param australia the bool flag to specify if australia data should be imported
         * @param tpch the bool flag to specify if tpch data should be imported
	 * @return <b>true</b> if import was successful, otherwise <b>false</b>
	 * @throws SQLException
	 * @throws IOException
	 * @throws FileNotFoundException
	 */

        public boolean importfiles(ConnectionInfo con,boolean me,boolean sales,boolean australia, boolean tpch)
    {           if(me)
		{
			String filePath = con.getmeImportPathPostgres();
			ScriptRunner runner = ScriptRunner.getInstance(currentConnection, true, false);
			try {
				FileReader a= new FileReader(filePath);
				runner.runScript(new BufferedReader(a));
				a.close();
			} catch (Exception e) {
                        ExceptionDialog.show(shell, e, sales);
                        return false;
			}
		}
                if(tpch){
			String filePath2 = con.getTpchImportPathPostgres();
			BufferedInputStream inStream;
			int result = 0;
			try 
			{

					FileReader a= new FileReader(filePath2);
					ScriptRunner runner = ScriptRunner.getInstance(currentConnection, true, false);
					runner.runScript(new BufferedReader(a));
					a.close();
			
			} catch (Exception e) {
                        ExceptionDialog.show(shell, e, sales);
                        return false;
			}
		}
                if(australia)
		{
			String filePath = con.getaustImportPathPostgres();
			ScriptRunner runner = ScriptRunner.getInstance(currentConnection, true, false);
			try {
				FileReader a= new FileReader(filePath);
				runner.runScript(new BufferedReader(a));
				a.close();
			} catch (Exception e) {
                        ExceptionDialog.show(shell, e, sales);
                        return false;
			}
		}
                if(sales){
			String filePath2 = con.getsalesImportPathPostgres();
			BufferedInputStream inStream;
			int result = 0;
			try
			{

					FileReader a= new FileReader(filePath2);
					ScriptRunner runner = ScriptRunner.getInstance(currentConnection, true, false);
                                        runner.runScript(new BufferedReader(a));
                                        BufferedReader temp=new BufferedReader(a);

					a.close();

			} catch (Exception e) {
                        ExceptionDialog.show(shell, e, sales);
                        return false;
			}
		}




            return true;
        }
	/**
	 * tests a connection specified through a ConnectionInfo Object
     * (simply tries to connect and disconnect to the database)
	 * @param con the ConnectionInfo Object to test
	 * @return <b>true</b> if connection was successful, otherwise <b>false</b>
	 * @throws SQLException 
	 * @throws IOException 
	 * @throws FileNotFoundException 
	 */
	public boolean testConnection(ConnectionInfo con){
		boolean test = connect(con);
		
		
		if (test)
			disconnect();
		
		return test;
	}

    /** @return the resultset of the last executed select stmt */
	public ResultSet getResultSet(){
		try {
			return currentStatement.getResultSet();
		} catch (SQLException e) {
			this.currentException = e;
			ExceptionDialog.show(shell, e, true);
			return null;
		} catch (Exception e){
			System.out.println("An exception occur.");
			ExceptionDialog.show(shell, e, true);
			return null;
		}
	}

    /** @return the update count of the last successfully run DML statement */
	public int getUpdateCount(){
		try {
			return currentStatement.getUpdateCount();
		} catch (SQLException e) {
			this.currentException = e;
			ExceptionDialog.show(shell, e, true);
			return -1;
		} catch (Exception e){
			System.out.println("An exception occur.");
			ExceptionDialog.show(shell, e, true);
			return -1;
		}
	}

    /** @return the exception of the last failed, executed stmt */
	public Exception getException(){
		return currentException;
	}

    /**
     * toggles autocommit mode
     * @param autoCommit boolean value, <b>true</b>=on, <b>false</b>=off
	 */
	public void setAutoCommit(boolean autoCommit) {
		if (state == CONNECTED) {
			try {
				currentConnection.setAutoCommit(autoCommit);
				currentConInfo.setAutoCommit(autoCommit);
			} catch (SQLException e) {
				ExceptionDialog.show(shell, e, true);
			} catch (Exception e){
				System.out.println("An exception occur.");
				ExceptionDialog.show(shell, e, true);
			}
		}
	}

    /** @return the status of autocommit mode of this connection */
	public boolean isAutoCommit() {
		if (state == CONNECTED) {
			return currentConInfo.isAutoCommit();
		}
		return false;
	}

	/** sends a commit() call to the active db connection, if connected */
	public void commit() {
		if (state == CONNECTED) {
			try {
				currentConnection.commit();
			} catch (SQLException e) {
				ExceptionDialog.show(shell, e, true);
			} catch (Exception e){
				System.out.println("An exception occur.");
				ExceptionDialog.show(shell, e, true);
			}
		}
	}


    /** get the current workload mode, either true for on, or false for off */
    public boolean getWorkloadMode(){
        return workloadMode;
    }

    /**
     * executes a statement
     * you can specify following options for the statement:
     *  (a) execute statement in a seperate thread (runs asynchronously and doesn`t block the GUI)
     *  (b) with (or without) history entries and firing events
     *  (c) with (or without) query tree and exectution tree
     *  (d) set a upper row bound until which the row are fetched. "0" means fetch all tuple
     *  you can specify these options only in the following combinations:
     *  	a		b		c		d
     *  	x		x		x		is used
     *  	-		x		x		is used
     *  	-		-		x		is ignored
     *  	-		-		-		is ignored
     *  	x		x		-		is used
     *  
     *  Be aware of that if you don't want to have an query execution workload recorded you have 
     *  to explicitly turn off the workload with JDBCInterface.setWorkload(false), 
     *  then execute your query with this method 
     *  and when the query work is finished, which includes the possible work with a result set (!),
     *  you have to explicitly turn on the workload with JDBCInterface.setWorkload(true)!
     *  If the workload is turned off at execution time the method JDBCInterface.setWorkload(..)
     *  is ignored.
     *  showInConsole is only used when inASeperateThread is true
     *  
     * @param upperRowBound
     * @param inASeperateThread
     * @param withHistoryAndEvents
     * @param queryTreeExtraction
     * @param showResultInConsole
     * @param statement
     */
    
    public int executeStatement(String stmt, int upperRowBound, boolean inASeperateThread, boolean withHistoryAndEvents, boolean queryTreeExtraction, boolean showResultInConsole){
    	
    	int result = -1;
    	
        
    	if(inASeperateThread && withHistoryAndEvents && queryTreeExtraction){
    		executeStatementInASeperateThread(stmt, upperRowBound, showResultInConsole); result = 0;
    	} else if(!inASeperateThread&&withHistoryAndEvents&&queryTreeExtraction){
    		executeStatementWithHistoryAndEvents(stmt, upperRowBound, showResultInConsole);result = 0;
    	} else if(!inASeperateThread&&!withHistoryAndEvents&&queryTreeExtraction)
    		result = executeStatementImmediate(stmt);
    	else if(!inASeperateThread&&!withHistoryAndEvents&&!queryTreeExtraction)
    		result = executeStatementImmediateWithoutQueryTreeExtraction(stmt);
    	else if (inASeperateThread && withHistoryAndEvents && !queryTreeExtraction) {
    		executeStatementInASeperateThread(stmt, upperRowBound, showResultInConsole); result = 0;
    	} else
    		throw new IllegalArgumentException("These parameters are not supported!");
    	return result;
    }
	/**
	 * executes a Statement in a seperate thread
     * (runs asynchronously and doesn`t block the GUI)
     * @param stmt the statement SQL text to execute
	 */
	private void executeStatementInASeperateThread(String stmt, int upperRowBound, boolean showResultInConsole) {
		try {
			System.out.println("executing: "
					+ StringUtils.replaceDelChars(stmt));
			Statement s = this.currentConnection.createStatement();
			// create new dialog
			InExecutionDialog inExecDialog = new InExecutionDialog(shell, upperRowBound, showResultInConsole);
			// setup dialog

			inExecDialog.setupStatement(s, stmt);


			// show dialog
			inExecDialog.show();
			// start thread
			inExecDialog.start();
		} catch (SQLException e) {
			System.out.println("Something went wrong during creation of a new statement.");
			ExceptionDialog.show(shell, e, true);
		} catch (Exception e) {
			System.out.println("An exception occur.");
			ExceptionDialog.show(shell, e, true);
		}
	}


	/**
	 * This method executes a statement similiar like the threaded version:<br>
	 * <b>executeStatement(String stmt)</b><br>
	 * It creates a HistoryEntry and fires an RESULT_READY Event after
	 * completion. Be careful, this version blocks the GUI during execution!
	 * This is the non-thread version to execute statements !
	 * 
	 * @param stmt
	 *            the stmt sql text to execute
	 */
	private void executeStatementWithHistoryAndEvents(String stmt, int upperRowBound, boolean showResultInConsole) {
		int result=0;
		long startTime=0;
		long executionTime=0;
		boolean canceled = false;
		Exception ex=null;

		try {
			System.out.println("executing: "+StringUtils.replaceDelChars(stmt));
			// execute statement and stop time
			startTime = System.currentTimeMillis();
			result = currentStatement.execute(stmt) ? 1 : 2;
			executionTime = System.currentTimeMillis() - startTime;
		} catch (SQLException e) {
			result = -1;
			ex = e;
			System.out.println("SQLException occured, while executing "+stmt);
			ExceptionDialog.show(shell, e, true);
		} catch (Exception e){
			System.out.println("An exception occur.");
			ExceptionDialog.show(shell, e, true);
		}

		// check result return code and do the right stuff:

		if(result==1) {
		try {
			// we have a resultset to parse in
			// create new ResultSetModel
			ResultSetModel rsm = new ResultSetModel(false);

			// reset starttime for parse in timing
			startTime = System.currentTimeMillis();

			// get first resultset
			ResultSet rs = currentStatement.getResultSet();

			// extract column headers
			ResultSetMetaData rsmd = rs.getMetaData();

			rsm.extractColumnHeaders(rsmd);

	        // now parse in tuples
	        int rowCounter=0; // counts the current parsed in rows
	        ResultSetIterator rsiter=null;
	        if(!postgres_connected)
	        	rsiter = new ResultSetIterator(rs);

            boolean fetchAll = (upperRowBound==0)?true:false;

            if (!postgres_connected) {
				while (rsiter.hasNext()) {
					if (fetchAll || (rowCounter < upperRowBound)) {
						rsm.addTuple(rsiter.next());
						rowCounter++;
					}
				}
				if (rsiter.hasNext())
					canceled = true; // upper bound ...
			}
            else
            {
            	while (rs.next())
            	{
            		if (fetchAll || (rowCounter < upperRowBound)) {
						rsm.addTuple(PostgresUtils.createTuple(rs));
						rowCounter++;
					}
            	}
            }
			// store execution time
	        executionTime += System.currentTimeMillis() - startTime;

	        // create History Entry
			HistoryEntry entry = new HistoryEntry(System.currentTimeMillis(), stmt);
		    entry.setExecutionTime(executionTime);
			entry.setQueryResult(rsm);
			entry.setInterrupted(canceled);
			entry.setAffectedRows(rowCounter);
			entry.setWarning(currentStatement.getWarnings());
			ConnectionInfo conInfo = getCurrentConnectionInfo();
			entry.setDbName(conInfo.getDbname());


			History.getInstance().add(entry);

		    rs.close();

		} catch (SQLException e) {
			   // Error occured during closing of ResultSet
			   System.out.println("Error during process of ResultSet...");
				ExceptionDialog.show(shell, e, true);
		} catch (Exception e){
			System.out.println("An exception occur.");
			ExceptionDialog.show(shell, e, true);
		}
		}

		if(result==2){
			HistoryEntry entry = new HistoryEntry(System.currentTimeMillis(), stmt);
			try {
				entry.setAffectedRows(currentStatement.getUpdateCount());
			} catch (SQLException e) {
				System.out.println("Error occured during determination of updated rows.");
				e.printStackTrace();
			}
		    entry.setExecutionTime(executionTime);
			ConnectionInfo conInfo = getCurrentConnectionInfo();
			entry.setDbName(conInfo.getDbname());


		    // add Entry
  		    History.getInstance().add(entry);

		}

		if(result==-1){
			// we got an exception
			System.out.println("SQLException occured during execution...");
			// create history entry
			HistoryEntry entry = new HistoryEntry(System.currentTimeMillis(), stmt);
			entry.setException(ex);
			ConnectionInfo conInfo = JDBCInterface.getInstance().getCurrentConnectionInfo();
			entry.setDbName(conInfo.getDbname());

		    // add Entry
		    History.getInstance().add(entry);

		}

		// fire Result_Ready Event
		GenericModelChangeEvent event3 = new GenericModelChangeEvent();
		event3.detail = DemoEvents.RESULT_READY;
		event3.source = this.toString();
		event3.showResultInConsole = showResultInConsole;
		fireModelChangeEvent(event3);
	}


	/**
	 * Immediately executes a statement!<br>
     * <li>No history entry will be created and
     * <li>no events are fired.<br>
     * Thus all other tabs don`t get informed about the execution.
     * Useful for short & fast statements
     * <li>call stmts,
     * <li>small dml stmts,
     * <li>dynamic one-shot stmts, etc. ...)
     * @param stmt the stmt text to execute
     * @return status code: -1=error occured, 1=result with resultset, 2=result with no resultset
	 */
	private int executeStatementImmediate(String stmt) {
		try {
			System.out.println("executing: "+StringUtils.replaceDelChars(stmt));
			// execute statement
			boolean result =  currentStatement.execute(stmt);

			// return result code
			return result ? 1:2;
		} catch (SQLException e) {
			System.out.println("SQLException occured, while executing "+stmt);
			ExceptionDialog.show(shell, e, true);
			return -1;
		} catch (Exception e){
			System.out.println("An exception occur.");
			ExceptionDialog.show(shell, e, true);
			return -1;
		}
	}

	/**
	 * executes a statement without displaying a dialog, without event notification and
	 * without regarding history & w/o query tree
     * @param stmt the stmt text to execute
     * @return status code: -1=error occured, 1=result with resultset, 2=result with no resultset
	 */
	private int executeStatementImmediateWithoutQueryTreeExtraction(String stmt) {

        try {

			System.out.println("executing: "+StringUtils.replaceDelChars(stmt));

			// execute statement
			boolean result =  currentStatement.execute(stmt);

			return result?1:2;
		} catch (SQLException e) {
			System.out.println("SQLException occured, while executing "+stmt);
			ExceptionDialog.show(shell, e, true);
			return -1;
		} catch (Exception e){
			System.out.println("An exception occur.");
			ExceptionDialog.show(shell, e, true);
			return -1;
		}
	}

	/**
	 * executes a statement without displaying a dialog, without event notification and
	 * without regarding history & w/o query tree, but returns a result set directly
     * @param stmt the stmt text to execute
     * @return SQL ResultSet or null, if there was an error
	 */
	public ResultSet executeStatementWithResult(String stmt) {

        try {


			System.out.println("executing: "+StringUtils.replaceDelChars(stmt));

			// execute statement
			ResultSet result = currentStatement.executeQuery(stmt);

			return result;
		} catch (SQLException e) {
			System.out.println("SQLException occured, while executing "+stmt);
			ExceptionDialog.show(shell, e, true);
			return null;
		} catch (Exception e){
			System.out.println("An exception occur.");
			ExceptionDialog.show(shell, e, true);
			return null;
		}
	}

    /**
     * returns a PreparedStatement<br>
     * After first usage, this PreparedStatemnt will be cached in a map and
     * will be looked up for each next coming call.
     * @param key the sql text and the key of the preparedstmt
     * @return the PreparedStmt
     */
    public PreparedStatement getPreparedStatement(String key){
        if(!preparedStatements.containsKey(key)){
            try {
                PreparedStatement pstmt = currentConnection.prepareStatement(key);
                preparedStatements.put(key,pstmt);
            } catch (SQLException e) {
                System.out.println("Error during preparing the Statement: "+key);
        		ExceptionDialog.show(shell, e, true);
    		} catch (Exception e){
    			System.out.println("An exception occur.");
    			ExceptionDialog.show(shell, e, true);
            }
        }
        return preparedStatements.get(key);
    }


    /**
     * transforms the contents of the resultset into a list of map objects
     * @param rs the ResultSet containing the information
     * @return a List of Map objects containing the information for each tuple
     */
    public List<Map<String, Object>> transformRSintoListOfMaps(ResultSet rs){

        try {
            // create list object
            List<Map<String,Object>> l  = new ArrayList<Map<String,Object>>();
            ResultSetMetaData rsmd = rs.getMetaData();
            int noColumns = rsmd.getColumnCount();

            // traverse resultset
            while(rs.next()){

                // create map object
                Map<String, Object> map = new HashMap<String, Object>();

                // traverse current row, extract information,
                // create an appropriate object for each column
                // and store it in the map with the columnname as key and the object as value
                for (int i = 1;i<noColumns+1;i++){
                    int type = rsmd.getColumnType(i);
                    Object value = rs.getObject(i);
                    String key = rsmd.getColumnName(i);
                    // only create an entry, if the column value is not null
                    if (value!=null){
                        switch(type){
                        case Types.BIT     : map.put(key, Boolean.valueOf(rs.getBoolean(i)));break;
                        case Types.BIGINT  : map.put(key, Long.valueOf(rs.getLong(i)));break;
                        case Types.INTEGER : map.put(key, Integer.valueOf(rs.getInt(i)));break;
                        case Types.BOOLEAN : map.put(key, Boolean.valueOf(rs.getBoolean(i)));break;
                        case Types.CHAR    : map.put(key, StringUtils.resolveAbbreviation(rs.getString(i)));break;
                        case Types.DATE    : map.put(key, rs.getDate(i));break;
                        case Types.DOUBLE  : map.put(key, new Double(rs.getDouble(i)));break;
                        case Types.DECIMAL : map.put(key, new Double(rs.getDouble(i)));break;
                        case Types.VARCHAR : map.put(key, StringUtils.resolveAbbreviation(rs.getString(i)));break;

                        default            : map.put(key, StringUtils.resolveAbbreviation(rs.getString(i)));break;
                        }
                    }
                }
                // add the map to the list
                l.add(map);
            }
            return l;
        } catch (SQLException e) {
    		ExceptionDialog.show(shell, e, true);
		} catch (Exception e){
			System.out.println("An exception occur.");
			ExceptionDialog.show(shell, e, true);
        }
        return null;
    }

    /** fill the table names map with some values to allow easy look up */
    private void fillTableNamesMap(){
    	if (!postgres_connected) {
			executeStatementImmediateWithoutQueryTreeExtraction(ConstantStatements.GET_TABLENAMESDERBY);
		}
    	else {
    		executeStatementImmediateWithoutQueryTreeExtraction(ConstantStatements.GET_TABLENAMESPOSTGRES);
    	}
		ResultSet rs = getResultSet();
        tableNames = new HashMap<String,Object>();
        try {
            while(rs.next()){
                String key = rs.getString(1);
                String value = rs.getString(2);
                tableNames.put(key, value);
            }
            rs.close();
        } catch (SQLException e) {
            System.out.println("Error during fillTableNamesMap() in JDBCInterface Module");
    		ExceptionDialog.show(shell, e, true);
		} catch (Exception e){
			System.out.println("An exception occur.");
			ExceptionDialog.show(shell, e, true);
        }

    }

    /** resolve the table name of an index */
    public String resolveTableName(String index){
        Object ret = tableNames.get(index);
        if(ret!=null){
            return ret.toString();
        } else {
            fillTableNamesMap();
            ret = tableNames.get(index);
            if(ret!=null){
                return ret.toString();
            } else {
                return "NOT AVAILABLE";
            }
        }
    }

    /** resets this module */
	public void reset() {
		// nothing to reset now...
	}

    /** initialiazation code of this module */
	public void init() {
		System.out.println("loading JDBCInterface module...");
		System.out.println("JDBCInterface module ready");
	}

	//********************************************************************************
	// GenericModel Implementation
	//********************************************************************************

	public void addModelChangeListener(GenericModelChangeListener listener) {
		listeners.add(listener);
	}

	public void removeModelChangeListener(GenericModelChangeListener listener) {
		listeners.remove(listener);
	}

	public void fireModelChangeEvent(final GenericModelChangeEvent event) {
		Display.getDefault().syncExec(new Runnable() {
			public void run() {
				for(GenericModelChangeListener listener:listeners){
					listener.modelChanged(event);
				}
			}});
	}

	public Connection getConnection() {
		return currentConnection;
	}

	public void setPostgres_connected(boolean postgres_connected) {
		this.postgres_connected = postgres_connected;
	}

	public boolean isPostgres_connected() {
		return postgres_connected;
	}

	public void setOptim_optnames(String[] optim_optnames) {
		this.optim_optnames = optim_optnames;
	}

	public String[] getOptim_optnames() {
		return optim_optnames;
	}
}
