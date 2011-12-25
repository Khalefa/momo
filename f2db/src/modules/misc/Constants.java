package modules.misc;

import java.io.File;
import java.text.SimpleDateFormat;

import modules.config.Configuration;

public class Constants {
	
	// --- global constants --------------------------------------------------
	
	/** This constant describes the default Demo Width */
	public static final int Demo_Width  = 1024;

	/** This constant describes the default Demo Height */
	public static final int Demo_Height = 768; 
	
	/** This is the application title, appearing in the title bar*/
	public static final String Demo_Title = "F2DB";

    /** all exceptions are shown in a ExceptionDialog. should they although be display on the console? */
   public static final boolean showExceptionsInConsoleToo = "true".equals(Configuration.getInstance().getMiscProperties().getProperty("execution.showExceptionsInConsoleToo"));

	// -- console colors ------------------------------------------------------
	
	/** this is the background color for the stmts in the output console */
	public static final int[] stmtBGColor      = {255,255,200};
	
	/** this is the background color for the exceptions in the output console */
	public static final int[] exceptionBGColor = {222,222,222};
	
	/** this is the background color for odd tuples in the table widgets */
	public static final int[] tableOddBGColor  = {235,235,235};
	
	/** used for storing temporary data */
	public static final File TEMP_DIR          = new File("./build/temp");
	
	// default directory of the script files
	public static final String script_directory = "./sql-scripts/";

	public static final String timeseries_directory = "./timeseries/";

	
	public static final String singleStmtWildcardFilter = "*.stmt";
public static final String singleFCStmtWildcardFilter = "*.fcstmt";
	public static final String multiStmtWildcardFilter = "*.sql";
	
	public static final String historyWildcardFilter = "*.history";
	
	
	// -- history ---------------------------------------------------------------------------------
	
	// -- printing values -------------------------------------------------------------------------
	
	/** This string is printed for column values which are null */
	public static final String PRINT_NULL          = "-";
	
	/** The maximum width of a column if printed (console tab) */
	public static final int MAX_PRINT_WIDTH        = 200;
	
	/** The default width of a column if printed (console tab) */
	public static final int DEFAULT_PRINT_WIDTH    = 50;
	
	/**	The upper row bound which limits the read in of tuples during resultsetmodel transformation */
	//public static final int UpperResultSetRowBound = 1000;  

	
	// -- translations ----------------------------------------------------------------------------
	
	// general
	public static final String YES    = "Yes";
	public static final String NO     = "No";
	public static final String OK     = "OK";
	public static final String Cancel = "Cancel";
	
    // components.main.maintoolbar
    // -----------------------------

    public static final String[] main_toolButtonNames = 
	{"   Connect   ",
	 "  Disconnect ",
	 "  Reconnect  ",
	 "Compile plan ",
     "Execution plan",
     "   Timings   ",
     "   Workload  ",
	 " Load history",
	 " Save history",
	 "    Reset    ",
     "    Quit     "};

    public static final String[] main_xplainModeNames =
    {" explain and execute statement ",
     " explain without execute statement"};
    
	// components.main.maintoolbar.history
	public static final String main_FileNotFound_History_open_Dialog   = "History file not found.";
	public static final String main_IOException_History_open_Dialog    = "An I/O-error while reading history file.";
	public static final String main_ClassNotFound_History_open_Dialog  = "A ClassNotFoundException occured while reading history file.";
	
    public static final String console_NumberFormatException           = "Please enter a numeric value!";
    
	public static final String main_FileNotFound_History_save_Dialog   = "Error creating history file.";
	public static final String main_IOException_History_save_Dialog    = "An I/O-error occured while saving history file.";

	// components.main.maintoolbar.tooltips
	public static final String main_connect_tooltip    = "Connect to a database.";
	public static final String main_disconnect_tooltip = "Disconnect from a database.";
	public static final String main_reconnect_tooltip  = "Reconnect to the current connected database.";
	public static final String main_tree_tooltip       = "Toggle tree extraction in the compile phase.";
    public static final String main_xplain_tooltip     = "Switch between explain modes (explain only, explain & execute) and switch explain facility on or off";
    public static final String main_timing_tooltip     = "Gather some additional timing information during explanation of query";
    public static final String main_workload_tooltip   = "Enable/disable collection of workload data.";
	public static final String main_load_tooltip       = "Load a previously saved history.";
	public static final String main_save_tooltip       = "Save the current history.";
	public static final String main_reset_tooltip      = "Reset the whole application.";
    public static final String main_exit_tooltip       = "Exits the application.";
	
	// components.main.maincontent
    // -----------------------------
    
	public static final String tab_console        = "Console";
	public static final String tab_planview        = "View QueryPlan";
	public static final String tab_modelgraphview        = "View ModelGraph";
	public static final String tab_timeseriesPlot = "Timeseries Plot";
	public static final String tab_timeseriesAcfaPacf = "Timeseries ACF";
	public static final String tab_tree           = "Compile plan";
    public static final String tab_plan           = "Execution plan";
	public static final String tab_olap           = "Group by";
	public static final String tab_onlineSampling = "Online sampling";
	public static final String tab_refresh        = "Refresh sample";
	public static final String tab_system         = "System catalog";
	public static final String tab_sampleCatalog  = "Sample catalog";
	public static final String tab_kernelDensityPlot = "Kernel Density Plot";
    public static final String tab_diagnosis      = "Diagnosis";
	public static final String tab_workload       = "SQL Workload"; 
	public static final String tab_history        = "History";
	public static final String tab_config         = "Configuration";
    public static final String tab_help           = "Online help";
	public static final String tab_about          = "About";	
	public static String tab_trace="Trace";

	// components.main.mainstatusline
    // -----------------------------
    
	public static final String main_status        = "";
	
	// components.console	
    // -----------------------------
    
	public static final String[] console_toolButtonNames = 
		{"Create Model...",
		 "Load SQL Stmt...",
		 "Save SQL Stmt...",
		 "Clear Console",
		 "Load Timeseries..."
		};

        	// components.console
    // -----------------------------
    
          public static String[] mgview_toolButtonNames =
 {
              "Query Model",
              "Create Model",
              "Copy Query to TSPlot"

          };
	public static final String console_output_rowReturn = " row(s) returned.";
	
	
	// components.detailtree
    // -----------------------------
    
	public static final String tree_select          = "Statement:";
	/**
	 * the simple date format string, which formats the timestamp
	 * @see SimpleDateFormat for regex pattern
	 */
	public static final String tree_date_format            = "yyyy-MM-dd HH:mm"; //:ss.S"; 
	public static final String tree_date_format_w_seconds  = "yyyy-MM-dd HH:mm:ss"; 
	public static final String tree_formal_RadioButtonText = "Simple";
	public static final String tree_exact_RadioButtonText  = "Full";
	public static final String tree_dbName_Text            = "Database:";
	public static final String tree_TimeStamp_Text         = "Timestamp: ";
	public static final String tree_stmtText_Text          = "SQL:";
	
	public static final String tree_detail_NodeID_Text     = "ID:";
	public static final String tree_detail_NodeName_Text   = "Name:";
	public static final String tree_detail_JavaClass_Text  = "Class name:";
	public static final String tree_detail_noChildren_Text = "Number of children:";
	public static final String tree_detail_details_Text    = "Details:";
	
	// components.refreshsample
    // -----------------------------
    
	public static final String REFRESHTAB_RESET              = "Reset";
	public static final String REFRESHTAB_RESET_DESC         = "Creates new table and sample.";
	public static final String REFRESHTAB_INSERT             = "Insert";
	public static final String REFRESHTAB_INSERT100_DESC     = "Inserts 100 tuples.";
	public static final String REFRESHTAB_INSERT200_DESC     = "Inserts 200 tuples.";
	public static final String REFRESHTAB_SERIES_EXACT       = "Exact";
	public static final String REFRESHTAB_SERIES_APPROXIMATE = "Approximate";
	public static final String REFRESHTAB_LOG                = "Statement log";
	public static final String REFRESHTAB_ACTIONS            = "Actions";
	
	// components.samplecatalog
    // -----------------------------
    
	public static final String sampleCatalog_logical         = "Logical sample";
	public static final String sampleCatalog_physical        = "Physical samples";
	
	// components.onlineSampling
	// -----------------------------
	
	public static final String onlineSampling_pageLevel		 = "PageLevelSample"; // default
	public static final String onlineSampling_biLevel		 = "BiLevelSample";
	public static final String onlineSampling_rowLevel		 = "RowLevelSample";
	public static final String onlineSampling_resultGroup	 = "Approximate results";
	public static final String onlineSampling_executionTimeGroup = "Execution time";
	public static final String onlineSampling_exact			 = "Exact";
	public static final String onlineSampling_approximate	 = "Approximate";
	public static final String onlineSampling_confidence	 = "Confidence";
	
	// components.history
    // -----------------------------
	public static final String[] HISTORYTAB_COLUMNS = new String[] {
		"ID", 
		"DBNAME", 
		"DATE", 
		"STATE", 
		"ROWS", 
		"EXECUTION_TIME", 
		"COMPILE_PLAN",
        "EXECUTION_PLAN",
		"SQL"
	};
	public static final String HISTORYTAB_SUCCESSFUL    = "Successful";
	public static final String HISTORYTAB_FAILED        = "Failed";
	public static final String HISTORYTAB_INTERRUPTED   = "Interrupted";
	
	// components.systemcatalog
    // -----------------------------
    
	public static final String system_select = "System catalog table";
	
	// components.diagnosis
    // -----------------------------
    
	public static final String diagnosis_select = "Diagnosis VTI Tables";
	
	// components.olap
	// -----------------------------
    
	// group titles
	public static final String olap_tableGroupTitle = "Table";
	public static final String olap_chartGroupTitle = "Chart";
	public static final String olap_statGroupTitle  = "Statistics";
	public static final String olap_resultsGroupTitle  = "Results";
	
	public static final String olap_approximate       = "Approximate";
	public static final String olap_exact             = "Exact";
	public static final String olap_stmt              = "SQL:";
	public static final String olap_exportTable       = "Export";
	public static final String olap_exactValue        = "Exact value";
	public static final String olap_approximatedValue = "Approximated value";
	public static final String olap_interval          = "Interval";
	
	
	// statistics
	public static final String olap_SampleName		= "Sample:";
	public static final String olap_SampleAlgorithm	= "Algorithm:";
	public static final String olap_EstimatorName	= "Estimator:";
	
	public static final String olap_appTupleCount   = "Number of tuples:";
	public static final String olap_exactTupleCount = "Number of tuples:";
	
	public static final String olap_relError        = "Average relative error:";
	public static final String olap_absError        = "Average absolute error:";

	public static final String olap_appExTime       = "Execution time:";
	public static final String olap_exactExTime     = "Execution time:";
	
	public static final String olap_minAbsError     = "Minimum absolute error:";
	public static final String olap_maxAbsError     = "Maximum absolute error:";

	public static final String olap_minRelError     = "Minimum relative error:";
	public static final String olap_maxRelError     = "Maximum relative error:";

	public static final String olap_definedTuples   = "Defined Tuple Intervals:";
	
	public static final String olap_inInterval      = "Correct error bounds:";
	public static final String olap_missingGroups   = "Number of missed groups:";
	
	public static final String olap_notDefined      = "not defined";
	public static final String olap_initialValue    = "                                                    ";	
	
	// components.config
    // -----------------------------
    
	public static final String config_db_title               = "Database connections";
	public static final String config_console_title 		 = "Console look'n'feel";
	public static final String config_history_title          = "History properties";
    public static final String config_system_title           = "Virtual machine properties";
	
	public static final String[] config_db_states = {
		"not tested", 
		"working", 
		"not working"};
	
	public static final String config_db_new            = "New configuration";
        public static String config_db_import               = "Import data";
	public static final String config_db_alias          = "Connection alias:";
	public static final String config_db_driver         = "Driver:";
	public static final String config_db_driver1        = "Embedded driver";
	public static final String config_db_driver2        = "Net driver";
	public static final String config_db_protocol       = "Protocol:";
	public static final String config_db_name           = "DB Name:";
	public static final String config_db_direcotry      = "select directory";
	public static final String config_db_user           = "Username:";
	public static final String config_db_passwd         = "Password:";
	public static final String config_db_sum            = "Summary:";
	public static final String config_db_status         = "Status:";
	public static final String config_db_create         = "create";
	public static final String config_db_test           = "Test connection";
	public static final String config_db_test_tooltip   = "Test current connection.";
	public static final String config_db_save           = "Save connection";
	public static final String config_db_save_tooltip   = "Save all connections.";
	public static final String config_db_delete         = "Delete connection";
	public static final String config_db_delete_tooltip = "Delete current connection.";
	public static final String config_tabVisibility_information_text = "Changes take effect after a restart!";

        	// components.console.CreateSampleDialog
    // -----------------------------

	public static final String model_create_of="Of: ";
        public static final String model_create_storage=" Storage: ";
        public static final String model_create_on=" On: ";
        public static final String model_create_alg="Algorithm: ";
        public static final String model_create_para="Parameters: ";
        public static final String model_create_train="TrainingData: ";
        public static final String model_create_number="Number";
        public static final String model_create_Alg_section="Algorithm";
        public static final String model_create_opti="OptimizationProperties";
         public static final String model_create_ok="Create";
           public static final String model_create_title="Create Forecastmodel";
               public static final String model_create_title2="Sql";
               public static final String model_create_title3="Basis Information";
             public static final String model_create_select="TrainingData: ";
                public static final String model_create_name="Name: ";
          public static final String model_create_cancel="Cancel";
	
    // components.about
    // -----------------------------
    public static final String zoomFit_tooltip   = "Zoom the graph to fit the screen.";
    public static final String zoomIn_tooltip   = "Zoom into the graph.";
    public static final String zoomOut_tooltip  = "Zoom out of the graph.";
	
    
    
	// components.about
    // -----------------------------

    // components.dialogs.SelectSamplesDialog
    public static final String select_samples_title			= "Select samples";
    public static final String select_samples_selectAll		= "Select all";
    public static final String select_samples_deselectAll	= "Deselect all";
    public static final String select_samples_exactStmt		= "Exact Statement";
    public static final String select_samples_autoEstimator	= "auto selection";
    
    // components.dialogs.OlapTabExecutionDialog
    public static final String olap_tab_execution_title			= "Executing OLAP-tab statements";
    public static final String olap_tab_execution_currentSample	= "Current sample:";
    public static final String olap_tab_execution_exectionTime	= "Execution time:";

    // modules.databaseif.InExecutionDialog
	public static final String InExecutionDialog_Dialog_Title      = "Executing statement...";
	public static final String InExecutionDialog_executeLabelText  = "Execution time:";
	public static final String InExecutionDialog_rowFetchLabelText = "Row fetch time:";
	public static final String InExecutionDialog_readingRowText    = "Number of rows:";
	public static final String InExecutionDialog_executeMessage    = "Executing statement...";
	
	// modules.databaseif.InExecutionMultiQueriesDialog
	public static final String InExecutionMultiQueriesDialog_Dialog_Title		= "Executing multiple statements ...";
	public static final String InExecutionMultiQueriesDialog_Current_Statement	= "Current statement:";
	
	// modules.databaseif.ExceptionDialog
	public static final String EXCEPTIONDIALOG_TITLE = "Exception";
	public static final String EXCEPTIONDIALOG_LABEL = "An exception has occured:";
	
	// modules.databaseif.ResultSetDialog
	public static final String RESULTSETDIALOG_TITLE = "Result set";
	public static final String RESULTSETDIALOG_LABEL = "The query returned the following result:";

	// StatementDetailsDialog
    public static final String STMTDETAILS_TITLE     = "Statement Details";
    public static String INFODIALOG_LABEL="Information";
    public static String INFONDIALOG_TITLE="Information";



    
    /**
     * do not allow to have an object of this class
     */
    private Constants(){
    }
}
