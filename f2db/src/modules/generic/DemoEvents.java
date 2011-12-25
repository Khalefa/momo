package modules.generic;
/**
 * This Class specifies all events which can occur during 
 * the runtime lifecycle of the application. These events 
 * are application specific events.
 * @author Felix Beyer
 * @date   06.05.2006
 *
 */
public class DemoEvents {

	// ***********************************************************************************
	// JDBCInterface ModelChangeEvents
	// ***********************************************************************************
	
	public static final int CONNECTION_ESTABLISHED 	= 100; // is sent, when a connection was establised
	public static final int CONNECTION_CLOSED      	= 101; // is sent, when a connection was closed
	
	public static final int EXECUTION_READY			= 102; // is sent, when the execution of a statement was successful
	public static final int RESULT_READY			= 103; // is sent, when the result generation of an executed statement was successful
	public static final int RESULT_CANCELED			= 104; // is sent, when the user cancels the parse in of the resultset rows
	
	public static final int TREE_ON				    = 105; // is sent, when tree extraction is toggled on
	public static final int TREE_OFF				= 106; // is sent, when tree extraction is toggled off
	
    public static final int XPLAIN_ON               = 107; // is sent, when xplain mode was toggled on
    public static final int XPLAIN_OFF              = 108; // is sent, when xplain mode was toggled off 
    
    public static final int WORKLOAD_ON               = 109; // is sent, when workload mode was toggled on
    public static final int WORKLOAD_OFF              = 110; // is sent, when workload mode was toggled off
    
	// ***********************************************************************************
	// Configuration ModelChangeEvents
	// ***********************************************************************************
	
	public static final int CONFIG_CONNECTION_CHANGED   = 200; // is sent, when the configuration has changed
	
	// ***********************************************************************************
	// TreeModel ModelChangeEvents
	// ***********************************************************************************
	
	public static final int NODE_SELECTED 			= 300; // is sent, when a single node was selected	 

	// ***********************************************************************************
	// History ModelChangeEvents
	// ***********************************************************************************
	public static final int LOAD_HISTORY				= 400; // is sent, after the history was loaded
}
