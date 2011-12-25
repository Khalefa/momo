package modules.databaseif;

/**
 * ***********************************************
 * @author Sebastian Seifert,Christopher Schildt
 * ***********************************************
 * 
 */
public interface ConstantStatements {
	

	public static String[] STMT_SYSTEMCATALOGS = new String[]
		{"SELECT * FROM SYS.SYSXPLAIN_STATEMENTS",
         "SELECT * FROM SYS.SYSXPLAIN_RESULTSETS",
         "SELECT * FROM SYS.SYSXPLAIN_SCAN_PROPS",
         "SELECT * FROM SYS.SYSXPLAIN_SORT_PROPS",
         "SELECT * FROM SYS.SYSXPLAIN_STATEMENT_TIMINGS",
         "SELECT * FROM SYS.SYSXPLAIN_RESULTSET_TIMINGS",
         "SELECT * FROM SYS.syslogicalsamples",
		 "SELECT * FROM SYS.sysphysicalsamples",
		 "SELECT * FROM SYS.sysphysicalsampleprops",
		 "SELECT * FROM SYS.syssampledependencies",
		 "SELECT * FROM SYS.sysaliases",
		 "SELECT * FROM SYS.syschecks",
		 "SELECT * FROM SYS.syscolumns",
		 "SELECT * FROM SYS.sysconglomerates",
		 "SELECT * FROM SYS.sysconstraints",
		 "SELECT * FROM SYS.sysdepends",
		 "SELECT * FROM SYS.sysfiles", 
		 "SELECT * FROM SYS.sysforeignkeys", 
		 "SELECT * FROM SYS.syskeys",
		 "SELECT * FROM SYS.sysschemas", 
		 "SELECT * FROM SYS.sysstatistics", 
		 "SELECT * FROM SYS.sysstatements",
		 "SELECT * FROM SYS.systables", 
		 "SELECT * FROM SYS.systriggers", 
		 "SELECT * FROM SYS.sysviews",
         "SELECT * FROM SYS.systableperms",
         "SELECT * FROM SYS.syscolperms",
         "SELECT * FROM SYS.sysroutineperms",
         "SELECT * FROM SYS.SYSWORKLOAD"};
	
	public static String STMT_ALLSYSTEMCATALOGSDERBY = "SELECT tablename FROM sys.systables WHERE tabletype = \'S\'";
	public static String STMT_ALLSYSTEMCATALOGSPG = "SELECT tablename FROM pg_tables WHERE tablename LIKE \'pg%\' order by tablename";
	public static String STMT_SELECTSYSTEMCATALOGDERBY = "SELECT * FROM SYS.";
	public static String STMT_SELECTSYSTEMCATALOGPOSTGRES = "SELECT * FROM ";
	
	// VTI Tables
	public static String[] STMT_VTITABLES = 
		{"SELECT * FROM new org.apache.derby.diag.LockTable() LT",
		 "SELECT ST.* FROM SYS.SYSSCHEMAS s, SYS.SYSTABLES t," +
         "new org.apache.derby.diag.SpaceTable(SCHEMANAME,TABLENAME) ST " +
         "WHERE s.SCHEMAID = t.SCHEMAID",
		 "SELECT * FROM new org.apache.derby.diag.StatementCache() SC",
		 "SELECT * FROM new org.apache.derby.diag.StatementDuration() SD",
		 "SELECT * FROM new org.apache.derby.diag.TransactionTable() TT",

		 "SELECT * FROM new org.apache.derby.diag.ErrorMessages() EM",
		 "SELECT * FROM new org.apache.derby.diag.ErrorLogReader() ELR",
		"SELECT * FROM new org.apache.derby.diag.ForecastModelAlgorithmRepositoryVTI() FMAR",
		};
	

    
    /** used to resolve the tablenames of the indexes/constraints, during index/constraint scans*/
    public static String GET_TABLENAMESDERBY = 
        "SELECT CONGLOMERATENAME, TABLENAME " + 
        "FROM SYS.SYSTABLES AS T, SYS.SYSCONGLOMERATES AS C " + 
        "WHERE T.TABLEID = C.TABLEID " +
        "UNION " + 
        "SELECT CONSTRAINTNAME, TABLENAME " +
        "FROM SYS.SYSCONSTRAINTS AS C, SYS.SYSTABLES AS T "+
        "WHERE T.TABLEID = C.TABLEID";
    
    public static String GET_TABLENAMESPOSTGRES =
    	"SELECT schemaname,tablename from pg_tables";

	public static String GET_OPTIMNAMESPOSTGRES = "select enumvals from pg_settings where name=\'optim_method_general\';";

	public static String GET_ACTGLOBALOPTIM = "SHOW optim_method_general";

	public static String GET_ACTLOCALOPTIM = "SHOW optim_method_local";

	public static String GET_MAXEVAL = "SHOW optim_term_maxeval";
	public static String GET_MAXTIME = "SHOW optim_term_maxtime";

	public static String GET_FTOLA = "SHOW optim_term_ftol_abs";
	public static String GET_FTOLR = "SHOW optim_term_ftol_rel";
	public static String GET_XTOLA = "SHOW optim_term_xtol_abs";
	public static String GET_XTOLR = "SHOW optim_term_xtol_rel";
    

}
