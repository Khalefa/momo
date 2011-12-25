package modules.databaseif;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.sql.SQLWarning;

import modules.misc.Constants;

/**
 * This class represents a History Entry. It will be created for every statement,
 * which is sent to the current connected database. It depends on the type of the
 * statement, which information it further holds. <br>
 * <li>If the statement is a select statement, a corresponding ResultSetModel will be hold </li>
 * <li>If the statement is a insert/update/delete/call statement, an updateCounter will be refreshed</li>
 * <li>If the statement is not coming to an end and an exception occurs, the exception is stored by this entry</li>
 *
 *
 * Instances of this class are stored in the <code>History</code>.
 * @see History
 *
 * @author Felix Beyer, Rainer Gemulla
 * @date   10.04.2006
 *
 */
public class HistoryEntry implements Serializable {

	// -- private variables -----------------------------------------------------------------------

	private static final long serialVersionUID = -378737928121137041L;

	/** the statement text */
	private String sql;

	/** result of select statements, or <code>null</code> otherwise */
	private ResultSetModel queryResult = null;

	/** exception, if an error occured during statement execution */
	private Exception exception = null;

    /** warning, if a warning occured during statement execution */
    private SQLWarning warning = null;

	/** number of affected rows */
	private int affectedRows = -1;

	/** creation time */
	private long creationTime;

	/** data of the compile plan query tree (GXL), or <code>null</code> */
	private String compilePlan = null;

    /** has this history entry a corresponding execution plan ?*/
    private boolean executionPlan = false;

	/** the dbname of the corresponding database */
	private String dbName = "";

	/** execution time in milliseconds */
	private long executionTime=-1;

	/**
	 *  stores the indexes of the history entrys, which references the
	 *  approximate statement which is comparable to this one
	 */
	private int[] approximatePartnerIndex;

	/** the statement has been interrupted during its execution? */
	private boolean interrupted = false;

    /** Does the statement have results? */
    private boolean hasResult = true;

	/** query result stored in memory? */
	private transient boolean isInMemory = true;

	/** query result serialized on disk? */
	private transient boolean isOnDisk = false;

	/** on-disk location of the query result */
	private transient File tempFile = null;
	
	/** true if this instance is empty - used to bind approximate results */
	private boolean isDummy;

	// -- constructors ----------------------------------------------------------------------------

	public HistoryEntry(long timestamp, String sql) {
		this.creationTime = timestamp;
		this.sql = sql;
	}

	// -- printing --------------------------------------------------------------------------------

	/**
	 * this method uses the saved executiontime and constructs a string
	 *
	 */
	public String formatExecutionTime() {
		String result = Long.toString(executionTime % 1000);
		while (result.length() < 3) result = "0" + result;
		result = Long.toString(executionTime / 1000) + "." + result + "s";
		return result;
	}

	/** (auto-restore).
	 * gets a StringBuffer from the contents of this history entry to print it out in the console.
	 * the contents of the stringbuffer of this entry depends on the result of the execution
	 * @param newLine the newline delimiter
	 * @return a StringBuffer containing the contents
	 */
	public StringBuffer getConsoleOutput(String newLine){
		// check to see if resultsetmodel is in memory, and restore it eventually
        if (!isInMemory) {restore();}

        // create new StringBuffer to hold contents
        StringBuffer sb = new StringBuffer();

        // do we have a result which returned rows
        if(queryResult!=null){
			// TODO Bottleneck: for large resultsets, stringbuffer creation requires a long time, and gui blocks, maybe create extra thread and fire new event, when creation is ready
			sb.append(queryResult.getStringBufferFromRSM(newLine));
        	if (isInterrupted()){
              sb.append(newLine + "Note: Query execution has been interrupted.");
            }
			sb.append(newLine + "Total time: " + formatExecutionTime() + newLine);
			return sb;
		} else {
            // or do we have an update result
		    sb.append(affectedRows + " row(s) inserted/updated/deleted.");
		    sb.append(newLine + "Total time: " + formatExecutionTime() + newLine);
		    return sb;
        }
	}

    /**
     * @param newLine the newline character, depending on the current VM and its OS
     * @return a String representation of the exception message
	 */
	public StringBuffer getExceptionForConsoleOutput(String newLine){
        StringBuffer sb = new StringBuffer();
        sb.append("Exception:" + newLine);
		sb.append(exception.getMessage());
        sb.append(newLine);
        Throwable e = exception.getCause();
        while(e != null){
        	sb.append("Caused by:" + newLine);
        	sb.append(e.getMessage());
        	sb.append(newLine);
        	
        	e = e.getCause();
        }
        return sb;
    }
	// -- getters & setters -----------------------------------------------------------------------

	public String getCompilePlan() {
		return compilePlan;
	}

	public void setCompilePlan(String queryTree) {
		this.compilePlan = queryTree;
	}

	public long getExecutionTime() {
		return executionTime;
	}

	public void setExecutionTime(long executionTime) {
		this.executionTime = executionTime;
	}

	public Exception getException() {
		return exception;
	}

	public void setException(Exception exception) {
        this.hasResult = false;
		this.exception = exception;
	}

    public SQLWarning getSQLWarning() {
        return warning;
    }

    public void setWarning(SQLWarning warning) {
        this.warning = warning;
    }

	public String getSQL() {
		return sql;
	}

	public void setSQL(String sql) {
		this.sql = sql;
	}

	public long getCreationTime() {
		return creationTime;
	}

	public void setCreationTime(long timestamp) {
		this.creationTime = timestamp;
	}

	/** auto-restore */
	public ResultSetModel getQueryResult() {
		if (!isInMemory) {
			restore();
		}
		return queryResult;
	}

	public void setQueryResult(ResultSetModel rsm) {
		isOnDisk = false; // temp file not valid anymore
		this.queryResult = rsm;
		affectedRows = rsm.getTuples().size();
	}

	public int getAffectedRows() {
		return affectedRows;
	}

	public void setAffectedRows(int affectedRows) {
        this.affectedRows = affectedRows;
	}

	public boolean isInterrupted() {
		return interrupted;
	}

	public void setInterrupted(boolean interrupted) {
		this.interrupted = interrupted;
	}

	public String getDbName() {
		return dbName;
	}

	public void setDbName(String dbName) {
		this.dbName = dbName;
	}

	public int[] getApproximatePartnerIndices() {
		return approximatePartnerIndex;
	}

	public void setApproximatePartnerIndex(int[] approximatePartnerIndex) {
		this.approximatePartnerIndex = approximatePartnerIndex;
	}

    public void setExecutionPlan(boolean executionPlan) {
        this.executionPlan = executionPlan;
    }
    
	public void setDummy() {
		isDummy = true;
	}
	
	public boolean isDummy() {
		return isDummy;
	}


	// -- shortcuts -------------------------------------------------------------------------------

	public boolean executedSuccessfully() {
		return exception == null;
	}

	public boolean hasCompilePlan() {
		return compilePlan != null;
	}

	/**
	 * true when previously the method setApproximatePartnerIndexwas called.
	 * approximatePartnerIndex must be not null, but it can be empty
	 */
	public boolean hasApproximatePartners(){
		return approximatePartnerIndex != null;
	}

    public boolean hasWarnings(){
        return warning != null;
    }

    public boolean hasException(){
        return exception != null;
    }
    /**
     * @return true if this entry has printable results
     */
    public boolean hasResults(){
        if(!isInMemory){
            return hasResult || (affectedRows!=-1);
        } else {
            return (queryResult != null) || (affectedRows!=-1);
        }
    }

    public boolean hasExecutionPlan(){
        return this.executionPlan;
    }


	// -- materialization -------------------------------------------------------------------------

	/** load result set from temp file
	 *
	 * @return true if successful
	 */
	public boolean restore() {
		if (isInMemory) {
			return true;
		}

		try {
			ObjectInputStream in = new ObjectInputStream(new FileInputStream(tempFile));
			queryResult = (ResultSetModel)in.readObject();
			in.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
			return false;
		} catch (IOException e) {
			e.printStackTrace();
			return false;
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
			return false;
		}

		isInMemory = true;
		return true;
	}

	/** write result set to temp file
	 *
	 * @return true if successfully stored (= not in memory anymore)
	 */
	public boolean store() {
		if (!isOnDisk) {
			try {
				// write to temp file
				if (tempFile == null) {
					tempFile = File.createTempFile("entry", "", Constants.TEMP_DIR);
					tempFile.deleteOnExit();
				}
				ObjectOutputStream out = new ObjectOutputStream(new FileOutputStream(tempFile));
				out.writeObject(queryResult);
				out.close();
				isOnDisk = true;
			} catch (IOException e) {
				tempFile = null;
				e.printStackTrace();
				return false;
			}
		}

		isInMemory = false;
		queryResult = null; // free memory
		return true;
	}
}
