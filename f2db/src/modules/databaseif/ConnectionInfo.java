package modules.databaseif;

import java.util.Properties;

/**
 * This class represents all information needed to connect to a
 * Derby Database.
 * @author Felix Beyer, Christopher Schildt
 *
 */
public class ConnectionInfo {

	private String  alias       		= "New Connection Name"; // the alias name for this connection
	private String  driver      		= "org.apache.derby.jdbc.EmbeddedDriver"; // default embedded
	private String  protocol    		= "jdbc:derby:"; // default protocol
    private String  dblocation  		= "localhost";      // to be defined by the user, either URL for net connections, or path for embedded connections
	private String  dbport      		= "5432";  // the connect port for net connections (default)
    private String  dbname      		= "";      // to be defined by the user
	private String  dbuser      		= "postgres"; // to be defined by the user, default is derby
	private String  dbpassword  		= "postgres"; // to be defined by the user, default is derby
	private boolean embedded    		= false;    // default true for embedded environment, false for net client
	private boolean autoCommit  		= true;    // default autocommit on
	private boolean create      		= false;   // create database flag, will be appended if true
	private boolean createme      		= false;   // create medatabase flag, will be appended if true
	private boolean tpchImport			= false;   // will import tpch database from the script in "src/resources/tpch" if true
	private boolean createTpchSamples	= false;   // will import tpch database from the script in "src/resources/tpch" if true
	private String  locale      		= null;    // the default locale
    /** this flag indicates if we are in DB2 compatible mode and therefore
     *  use the DB2 Universal Driver or not */
    private boolean db2mode = false;

    // the different driver class names
    private final String embeddedDriverName = "org.apache.derby.jdbc.EmbeddedDriver";
    private final String netDriverName = "org.apache.derby.jdbc.ClientDriver";
    private final String netDB2DriverName = "com.ibm.db2.jcc.DB2Driver";


    private final String tpchImportPathPostgres = "./resources/import/tpch/tpchImportPostgres.sql";
    private final String meImportPathPostgres = "./resources/import/meregio/meImportPostgres.sql";
    private final String salesImportPathPostgres = "./resources/import/sales/salesImportPostgres.sql";
    private final String austImportPathPostgres = "./resources/import/australia/austImportPostgres.sql";

	// ------------------ constructors ---------------------------
	/** empty constructor */
	public ConnectionInfo(){}

	// ------------------ getter & setter ------------------------

	public boolean isCreate() {return create;}
	public void setCreate(boolean create) {this.create = create;}

	public boolean isTpchImport() {return tpchImport;}
	public void setTpchImport(boolean tpchImport) {this.tpchImport = tpchImport;}

	public boolean isCreateTpchSamples() {return createTpchSamples;}
	public void setCreateTpchSamples(boolean createTpchSamples) {this.createTpchSamples = createTpchSamples;}

	

	public String getmeImportPathPostgres() {
		return meImportPathPostgres;
	}

	public String getTpchImportPathPostgres() {
		return tpchImportPathPostgres;
	}
        public String getaustImportPathPostgres() {
		return austImportPathPostgres;
	}

	public String getsalesImportPathPostgres() {
		return salesImportPathPostgres;
	}


	public boolean isAutoCommit() {return autoCommit;}
	public void setAutoCommit(boolean autoCommit) {this.autoCommit = autoCommit;}

	public String getDbname() {return dbname;}
	public void setDbname(String dbname) {this.dbname = dbname;}

    /**
     * @return the appropriate driver name
	 */
	public String getDriver() {
	    if(embedded) {
            return embeddedDriverName;
        } else {
            if (db2mode){
                return netDB2DriverName;
            } else {
                return netDriverName;
            }
        }
    }
    public void setDriver(String driver){this.driver = driver;}

	public String getPassword() {return dbpassword;}
	public void setPassword(String password) {this.dbpassword = password;}

	public String getProtocol() {return protocol;}
	public void setProtocol(String protocol) {this.protocol = protocol;}

	public String getUser() {return dbuser;}
	public void setUser(String user) {this.dbuser = user;}

	public boolean isEmbedded() {return embedded;}
	public void setEmbedded(boolean embedded) {this.embedded = embedded;}

	public String getAlias() {return alias;}
	public void setAlias(String alias) {this.alias = alias;}

    public String getDblocation() {return dblocation;}
    public void setDblocation(String dblocation) {this.dblocation = dblocation;}

    public String getDbport() {return dbport;}
    public void setDbport(String dbport) {this.dbport = dbport;}

    public String getLocale() {return locale;}
    public void setLocale(String locale) {this.locale = locale;}

    public void setDB2Mode(boolean db2mode){this.db2mode = db2mode;}
    public boolean isDB2Mode(){ return db2mode;}

    /**
     * convenience get method to get a full connection string
     * @return a full connection string
     */
    public String getConnectionString() {
        if (!JDBCInterface.getInstance().isPostgres_connected()) {
			if (embedded) {
				return "jdbc:derby:" + dblocation + "/" + dbname
						+ (this.create ? ";create=true" : "")
						+ (locale != null ? (";locale=" + locale) : "")
						+ ";user=" + dbuser + ";password=" + dbpassword;
			} else {
				if (db2mode) {
					// we are using the DB2 Driver
					return "jdbc:derby:net://" + dblocation + ":" + dbport
							+ "/" + dbname
							+ (this.create ? ";create=true" : "");
				} else {
					// we try to use the ClientDriver
					return "jdbc:derby://" + dblocation + ":" + dbport + "/"
							+ dbname + ";user=" + dbuser + ";password="
							+ dbpassword + (this.create ? ";create=true" : "")
							+ (locale != null ? (";locale=" + locale) : "");
				}

			}
		}
        else
        {
        	return "jdbc:postgresql://" + dblocation + "/" + dbname;
        }
    }

    /**
     * convenience method returning the connection settings as properties
     * @return the connection properties
     */
    public Properties getConnectionProperties() {
        Properties props = new Properties();
        props.put("user",dbuser);
        props.put("password",dbpassword);
        //props.put("retrieveMessagesFromServerOnGetMessage","true");

        return props;
    }

	// ------------------------ String representation --------------

	public String toString(){
		return "Alias      : " + this.alias      + "\n" +
			   "Driver     : " + this.driver     + "\n" +
			   "Protocol   : " + this.protocol   + "\n" +
			   "Database   : " + this.dbname     + "\n" +
               "Location   : " + this.dblocation + "\n" +
               "Port       : " + this.dbport     + "\n" +
			   "Login      : " + this.dbuser     + "\n" +
			   "Password   : " + this.dbpassword + "\n" +
               "Locale     : " + this.locale     + "\n" +
			   "Embedded   : " + this.embedded   + "\n" +
			   "AutoCommit : " + this.autoCommit + "\n" +
			   "Create     : " + this.create     + "\n" +
			   "TpchImport : " + this.tpchImport + "\n" +
			   "TpchSamples: " + this.createTpchSamples;
	}

	public void setmeCreate(boolean selection) {
		this.createme=selection;
		// TODO Auto-generated method stub
		
	}

	public boolean ismeCreate() {
		return this.createme;
	}

}
