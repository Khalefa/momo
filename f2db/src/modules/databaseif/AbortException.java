package modules.databaseif;
/**
 * An AbortException is thrown when the user canceled the execution of a statement
 * @author Felix Beyer
 * @date   27.07.2006
 *
 */
public class AbortException extends Exception {

	private static final long serialVersionUID = -2198020738708075509L;
	
	// ----------------------------- constructor ----------------------
	
	public AbortException(){}
	
	// ----------------------------- methods --------------------------
	
	public String getMessage(){
		return "Execution was canceled";
	}
	
}
