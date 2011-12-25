package modules.config;

import components.config.DbImport;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.Properties;

import modules.Module;
import modules.databaseif.ConnectionInfo;
import modules.generic.GenericModel;
import modules.generic.GenericModelChangeEvent;
import modules.generic.GenericModelChangeListener;
import modules.misc.ModuleRegistry;

/**
 * This class encapsulates the whole application configuration. <br/>
 * It provides:
 * <li> database connection settings </li>
 * <li> color configuration settings </li>
 * <li> tab specific settings </li> <br/><br/>
 * Derby reference for connection string http://db.apache.org/derby/docs/dev/ref/
 * @author Felix Beyer
 * @date   11.04.2006
 *
 */
public class Configuration implements Module, GenericModel{

	private final String ConnectionFileProps = "./properties/connections.properties"; 
	private final String MiscProps = "./properties/misc.properties";
	
	private static Configuration singleton; // the singleton instance
	
	// member variables
	private List<ConnectionInfo> cons; // the array of ConnectionInfo Objects, the app knows
	private List<GenericModelChangeListener> listeners; // an array for the different listeners
	
	private Properties misc; // the miscellaneous Properties
	private boolean miscChanged; // flag indicating the miscellaneous props have changed, -> save them
	
	
	// ---------------------- private constructor ------------------------
	
	private Configuration(){
		
		// create new Arraylist for ConnectionInfo Objects
		cons = new ArrayList<ConnectionInfo>();
		
		// create a new Arraylist for the listeners
		listeners = new ArrayList<GenericModelChangeListener>();
		
        // create a new properties object
        misc = new Properties();
        
	}
	
    /**
	 * @return one instance of a Configuration
	 */
	public static Configuration getInstance(){
		if(singleton == null) {
			singleton = new Configuration();
			ModuleRegistry.addModule(singleton);
		}
		return singleton;
	}
	
	// ----------------------- methods -----------------------------
	
    /** load the configured connections as ConnectionInfo in a list */
	private void loadMiscProperties(){
		try {
			// load misc properties 
			misc.load(new FileInputStream(MiscProps));
			System.out.println("Application properties found and successfully loaded.");
		
		} catch (FileNotFoundException e) {
			System.out.println("Sorry, misc properties "+this.MiscProps+" not found...");
			e.printStackTrace();
		} catch (IOException e) {
			System.out.println("Sorry, during reading from misc properties file, an IOError occured.");
			e.printStackTrace();
		}
	}
	
    /** save the configured connections from the list into a properties file */
	public void saveMiscProperties(){
		// check flag, if something has changed, save them, otherwise continue
		if(miscChanged){
			try {
				System.out.println("Now, save misc properties in properties file:");
				System.out.print("Storing Misc Properties...");
				misc.store(new FileOutputStream(this.MiscProps),"This File contains all application properties");
				System.out.println("done.");
			} catch (IOException e) {
				System.out.println("Sorry, during writing the misc properties, an IOError occured.");
				e.printStackTrace();
			}
		}
	}

    /**
     * @return the current loaded miscellaneous properties
     */
    public Properties getMiscProperties(){
        return misc;
    }
    
    /**
     * Copy a set of properties 
     * <p>
     *
     * @param src_prop  Source set of properties to copy from.
     * @param dest_prop Dest properties to copy into.
     *
     **/
    public void copyProperties(Properties src_prop, Properties dest_prop)
    {
        for (Enumeration propertyNames = src_prop.propertyNames();
             propertyNames.hasMoreElements(); )
        {
            Object key = propertyNames.nextElement();
            dest_prop.put(key, src_prop.get(key));
        }
    }
    
    
	/** add a new ConnectionInfo object */
	public void addConnectionInfo(ConnectionInfo ci){
		cons.add(ci);
                DbImport.getInstance().updateComboBox();
		saveConnections();
    }
	
	/** delete a ConnectionInfo object */
	public void delConnectionInfo(ConnectionInfo ci){
		cons.remove(ci);
        saveConnections();
	}
    
	/** return the ConnectionInfo at specified index */
	public ConnectionInfo getConnectionInfo(int index){
		if(index>=0 && index<cons.size()){
			return cons.get(index);
		} else {
			return null;
		}
	}
	
	/** return current active array of ConnectionInfo objects */
	public ConnectionInfo[] getConnectionInfos(){
		ConnectionInfo[] coninfos = new ConnectionInfo[this.cons.size()];
		for (int i=0;i<cons.size();i++){
			coninfos[i] = cons.get(i);
		}
		return coninfos;
	}
	
	/** load miscellaneous properties */
	private void loadConnections(){
        try {
            // create new Properties to read in connections file
            Properties props = new Properties();
            System.out.println("Current Path: "+System.getProperty("user.dir"));
            props.load(new FileInputStream(this.ConnectionFileProps));
            int count = Integer.parseInt(props.getProperty("noConnections"));
            System.out.println("connection infos found: "+count);
            System.out.println("parsing in connections:");

            ConnectionInfo ci;
            for(int i=0;i<count;i++){
                // create new ConnectionInfo
                ci = new ConnectionInfo();
                // set properties of connection number i
                ci.setAlias(props.getProperty("alias."+Integer.toString(i)));
                ci.setDriver(props.getProperty("driver."+Integer.toString(i)));
                ci.setProtocol(props.getProperty("protocol."+Integer.toString(i)));
                ci.setDbname(props.getProperty("dbname."+Integer.toString(i)));
                ci.setDblocation(props.getProperty("dblocation."+Integer.toString(i)));
                ci.setDbport(props.getProperty("dbport."+Integer.toString(i)));
                ci.setUser(props.getProperty("dbuser."+Integer.toString(i)));
                ci.setPassword(props.getProperty("dbpassword."+Integer.toString(i)));
                ci.setEmbedded(Boolean.parseBoolean(props.getProperty("embedded."+Integer.toString(i))));
                ci.setAutoCommit(Boolean.parseBoolean(props.getProperty("autocommit."+Integer.toString(i))));
                ci.setCreate(Boolean.parseBoolean(props.getProperty("create."+Integer.toString(i))));
                String locale =  props.getProperty("dblocale."+Integer.toString(i));
                if(locale!=null && !locale.equalsIgnoreCase("null")){ci.setLocale(locale);}
                cons.add(ci);
                ci.setDB2Mode(Boolean.parseBoolean(props.getProperty("db2mode."+Integer.toString(i))));
                System.out.println("connection No."+Integer.toString(i)+" ... done.");
            }
            
        
        } catch (FileNotFoundException e) {
            System.out.println("Sorry, connection settings "+this.ConnectionFileProps+" not found...");
            e.printStackTrace();
        } catch (IOException e) {
            System.out.println("Sorry, during reading from connection settings file, an IOError occured.");
            e.printStackTrace();
        }
		
	}
	
    /** save miscellaneous properties */
	public void saveConnections(){
            try {
                // create new Properties to read in connections file
                Properties props = new Properties();
                int count = cons.size();
                System.out.println("Number of found Connection Infos: "+count);
                System.out.println("Now, save found connections in properties file:");

                props.setProperty("noConnections",Integer.toString(count));
                ConnectionInfo ci;
                for(int i=0;i<count;i++){
                    // create new ConnectionInfo
                    ci = cons.get(i);
                    // set properties of connection number i
                    props.setProperty("alias."+Integer.toString(i),ci.getAlias());
                    props.setProperty("driver."+Integer.toString(i),ci.getDriver());
                    props.setProperty("protocol."+Integer.toString(i),ci.getProtocol());
                    props.setProperty("dbname."+Integer.toString(i),ci.getDbname());
                    props.setProperty("dblocation."+Integer.toString(i),ci.getDblocation());
                    props.setProperty("dbport."+Integer.toString(i),ci.getDbport());
                    props.setProperty("dbuser."+Integer.toString(i),ci.getUser());
                    props.setProperty("dbpassword."+Integer.toString(i),ci.getPassword());
                    props.setProperty("embedded."+Integer.toString(i),(ci.isEmbedded()?"true":"false"));
                    props.setProperty("autocommit."+Integer.toString(i),(ci.isAutoCommit()?"true":"false"));
                    props.setProperty("create."+Integer.toString(i),(ci.isCreate()?"true":"false"));
                    String locale = "null";
                    if (ci.getLocale()!=null) locale = ci.getLocale();
                    props.setProperty("dblocale."+Integer.toString(i),locale);
                    props.setProperty("db2mode."+Integer.toString(i),(ci.isDB2Mode()?"true":"false"));
                    //System.out.println("Connection No."+Integer.toString(i)+" saved in connection properties");
                }
                System.out.print("Storing Properties...");

                props.store(new FileOutputStream(this.ConnectionFileProps),"This File contains all specified connections");
                System.out.println("done.");
            } catch (IOException e) {
                System.out.println("Sorry, during writing the connection settings file, an IOError occured.");
                e.printStackTrace();
            }
		
	}
	
	// ------------------ getter & setter --------------------
	
	
    
    public String getProperty(String prop){
		return misc.getProperty(prop);
	}
	public void setProperty(String prop,String value){
        // store property
        misc.setProperty(prop, value);
		
        // and remember to save before shutdown
        miscChanged = true;
	}

	// ------------------- Module Interface ------------------
	
    /** module initialization */
	public void init(){
		System.out.println("loading configuration module...");
		loadConnections();
		loadMiscProperties();
		//testGXLImporter();
		System.out.println("configuration module ready");
	}
	
    /** module reset */
	public void reset() {}
	
	/** right before shutdown save all configuration stuff into properties files */
	public void shutdown() {
		saveConnections();
		saveMiscProperties();
	}
	
	// --------------------------- GenericModel Interface ----------------
	
	public void addModelChangeListener(GenericModelChangeListener listener) {
		this.listeners.add(listener);
	}

	public void removeModelChangeListener(GenericModelChangeListener listener) {
		this.listeners.remove(listener);
	}

	public void fireModelChangeEvent(GenericModelChangeEvent event) {
		for(GenericModelChangeListener listener:this.listeners){
			listener.modelChanged(event);
		}
	}

	
}
