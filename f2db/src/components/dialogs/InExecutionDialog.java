package components.dialogs;

import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.SQLWarning;
import java.sql.Statement;
import java.util.logging.Level;
import java.util.logging.Logger;

import modules.databaseif.ConnectionInfo;
import modules.databaseif.History;
import modules.databaseif.HistoryEntry;
import modules.databaseif.JDBCInterface;
import modules.databaseif.PostgresUtils;
import modules.databaseif.ResultSetModel;
import modules.databaseif.SecondsCounterThread;
import modules.generic.DemoEvents;
import modules.generic.GenericModelChangeEvent;
import modules.misc.Constants;
import modules.misc.ResourceRegistry;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.ProgressBar;
import org.eclipse.swt.widgets.Shell;

import data.iterator.input.ResultSetIterator;
import modules.databaseif.ScriptRunner;

/** This dialog waits for a thread to finish. */
public class InExecutionDialog extends Thread {

//	private Display display; // the parent display to use
	private Shell shell; // the new shell which is created for this dialog
	private Shell parentShell; // the parent shell

	private Label currentLabel; // the seconds which have passed label
	private Label statusLabel; // the label to print current operation

	private Button cancelButton; // the cancel button

	private Label exTimeLabel; // the current execution time label
//	private Label parseTimeLabel; // the current parse time label
	private Label rowNoLabel; // the current row count label
	private ProgressBar rowProgress; // the row progress label
	private long startTime; // the starttime of the thread
	private long endTime; // the endtime to calculate the difference to the starttime
	private long executionTime; // the execution time in ms

	private boolean executionPlan; // was execution plan generation on?

	private String stmtText; // the statement text for execution
	private Statement stmt; // the statement from the connection
	private boolean canceled=false; // this flag indicates that the execution has been canceled by the user
	private int rowCounter=0; // the row counter
	private boolean queryTree = false;

	private HistoryEntry entry = null;

	private int upperRowBound = 0;

	private SecondsCounterThread secCounterThread; // the seconds counter thread
	
	private boolean showResultInConsole;

	public InExecutionDialog(Shell parent) {
		this(parent, 0);
	}

	public InExecutionDialog(Shell parent, int upperRowBound, boolean showResultInConsole) {
		super("ExecutionThread");
		this.parentShell = parent;
		this.upperRowBound = upperRowBound;
		this.showResultInConsole = showResultInConsole;
	}

	public InExecutionDialog(Shell parent, int upperRowBound) {
		this(parent, upperRowBound, true);
	}
	
	/**
	 * this method returns the Title Text Headline of the dialog
	 */
	public String getText(){
		return Constants.InExecutionDialog_Dialog_Title;
	}
	
	/**
	 * this method setups the statement
	 * @param stmt the statement to use
	 * @param text the statement text to execute
	 */
	public void setupStatement(Statement stmt, String text){
		this.stmt = stmt;
		this.stmtText = text;
	}

	/** Toggle query tree extraction */
	public void setQueryTree(boolean queryTree) {
		this.queryTree = queryTree;
	}

	/** set the status of query explanation */
	public void setExecutionPlan(boolean executionPlan){
		this.executionPlan = executionPlan;
	}

	/** show this dialog */
	public void show(){

		//Display display = Display.getDefault();
		shell = new Shell(parentShell, SWT.DIALOG_TRIM | SWT.RESIZE /*| SWT.APPLICATION_MODAL*/);
		shell.setSize(300,150);
		shell.setText(getText());

		shell.setImage(ResourceRegistry.getInstance().getImage("icon"));
		
		// create dialog components and listeners 
		initComponents();
		initListeners();

		// open dialog
		shell.open();
//		display = parentShell.getDisplay();

		// start execution thread
		startTime = System.currentTimeMillis();
	}
	/**
	 * this method inits all the dialog components
	 *
	 */
	private void initComponents(){
		Composite c = new Composite(shell,SWT.NONE);
		GridLayout thisLayout = new GridLayout();
		thisLayout.numColumns = 2;
		c.setLayout(thisLayout);
		c.setSize(350, 250);
		{
			Label exTimeLabel2 = new Label(c, SWT.NONE);
			GridData label1LData1 = new GridData();
			label1LData1.horizontalAlignment = GridData.END;

			exTimeLabel2.setLayoutData(label1LData1);
			exTimeLabel2.setText(Constants.InExecutionDialog_executeLabelText);
		}
		{
			exTimeLabel = new Label(c, SWT.NONE);
			GridData executeTImeLData = new GridData();
			executeTImeLData.horizontalAlignment = GridData.FILL;
			exTimeLabel.setLayoutData(executeTImeLData);
			exTimeLabel.setText("0.0");
		}
		// make one time only
		/*{
			Label label5 = new Label(c, SWT.NONE);
			GridData label5LData = new GridData();
			label5LData.horizontalAlignment = GridData.END;
			label5.setLayoutData(label5LData);
			label5.setText(Constants.InExecutionDialog_rowFetchLabelText);
);
		}
		{
			parseTimeLabel = new Label(c, SWT.NONE);
			parseTimeLabel.setText("0.0");
		}*/

		{
			Label label2 = new Label(c, SWT.NONE);
			GridData label2LData = new GridData();
			label2LData.heightHint = 13;
			label2LData.horizontalAlignment = GridData.END;
			label2.setLayoutData(label2LData);
			label2.setText(Constants.InExecutionDialog_readingRowText);
		}
		{
			rowNoLabel = new Label(c, SWT.NONE);
			GridData label3LData = new GridData();
			label3LData.horizontalAlignment = GridData.FILL;
			label3LData.grabExcessHorizontalSpace = true;
			rowNoLabel.setLayoutData(label3LData);
			rowNoLabel.setText("----");
		}
		{
			GridData label4LData = new GridData();
			label4LData.horizontalAlignment = GridData.FILL;
			label4LData.grabExcessHorizontalSpace = true;
			label4LData.horizontalSpan = 2;
			Label label4 = new Label(c, SWT.SEPARATOR | SWT.HORIZONTAL);
			label4.setLayoutData(label4LData);
		}
		{
			GridData progressBar1LData = new GridData();
			progressBar1LData.grabExcessHorizontalSpace = true;
			progressBar1LData.horizontalAlignment = GridData.FILL;
			progressBar1LData.horizontalSpan = 2;
			rowProgress = new ProgressBar(c, SWT.INDETERMINATE);
			rowProgress.setLayoutData(progressBar1LData);

			if(upperRowBound > 0) {
				rowProgress.setMaximum(upperRowBound);
				rowProgress.setMinimum(0);
				rowProgress.setSelection(0);
			}
		}
		{
			statusLabel = new Label(c,SWT.NONE);
			statusLabel.setText("");
			GridData gd = new GridData();
			gd.grabExcessHorizontalSpace = true;
			gd.horizontalAlignment = GridData.FILL;
			gd.horizontalSpan = 2;
			statusLabel.setLayoutData(gd);
		}
		{
			cancelButton = new Button(c, SWT.PUSH | SWT.CENTER);
			GridData button1LData = new GridData();
			button1LData.horizontalSpan = 2;
			button1LData.horizontalAlignment = GridData.CENTER;
			button1LData.grabExcessHorizontalSpace = true;
			button1LData.widthHint = 100;
			button1LData.heightHint = 23;
			cancelButton.setLayoutData(button1LData);
			cancelButton.setText("cancel");
			cancelButton.setSize(100, 23);
			cancelButton.setEnabled(true); // deactivate button during execution of statement
		}
		this.currentLabel = exTimeLabel;
		c.layout();


	}
	/**
	 * this method registers all the different listeners for this
	 *
	 */
	private void initListeners(){

		cancelButton.addListener(SWT.Selection, new Listener(){
			public void handleEvent(Event event) {
				// if execution was canceled

				// toggle cancel flag
				canceled = true;

				// stop seconds counter thread
				secCounterThread.setStop(true);
                try {
                    JDBCInterface.getInstance().getCurrentStatement().cancel();
                } catch (SQLException ex) {
                    Logger.getLogger(InExecutionDialog.class.getName()).log(Level.SEVERE, null, ex);
                }

			}});
	}

	/**
	 * this method creates the resultsetmodel, if the executed stmt
	 * returned rows 
	 *
	 */
	private HistoryEntry createResultSetModel(){
		try {
			// **************************************************
			// in every case create a Model and fill it with 
			// the header titles extracted from metadata of the rs
			// ***************************************************

			// 1st create new ResultSetModel
			ResultSetModel rsm = new ResultSetModel(false);

			// then reset start time for parsing
			startTime = System.currentTimeMillis();

			// tell the seconds counter about this new starttime
			secCounterThread.setStartTime(startTime - executionTime); // that's a trick

			// get first resultset
			ResultSet rs = stmt.getResultSet();

			// extract column headers from metadata
			ResultSetMetaData rsmd = rs.getMetaData();

			// let the ResultSetModel extract the column headers
			rsm.extractColumnHeaders(rsmd);

			// ***************************************************
			// now parse in the tuples
			// ***************************************************

			rowCounter=0; // counts the current parsed in rows
			int updateCounter=0; // counts up, when 100 is reached, an update event is sent into queue

			ResultSetIterator rsiter=null;
			if (!JDBCInterface.getInstance().isPostgres_connected()) {
				// create new ResultSetIterator to iterate over the ResultSet
				rsiter = new ResultSetIterator(rs);
			}
			// if the resultset has more rows &
			// the user didn`t cancel the execution &
			// the rows read do not exceed the maximum upper bound
			// simply fetch the rows
			// the fetchall attribute
			boolean fetchAll = (upperRowBound==0)?true:false;
			boolean stop = false;
			if (!JDBCInterface.getInstance().isPostgres_connected()) {
				while ((rsiter.hasNext()) && (!canceled) && !stop) {
					if (fetchAll || (rowCounter < upperRowBound)) {
						// add the next tuple to the rsm
						rsm.addTuple(rsiter.next());
						// increase row counter and update counter
						rowCounter++;
						updateCounter++;
						// update the counter label in the dialog every 100 rows
						if (updateCounter == 100) {
							Display.getDefault().asyncExec(new Runnable() {
								public void run() {
									// update row counter
									if (!rowNoLabel.isDisposed()) {
										rowNoLabel.setText(Integer
												.toString(rowCounter));
										rowNoLabel.pack(true);
									}

									//update progress bar
									if (!rowProgress.isDisposed()) {
										rowProgress.setSelection(rowProgress
												.getSelection() + 1);
									}
								}
							});
							// reset update counter for next round
							updateCounter = 0;
						}
					} else {
						stop = true;
					}
				}
			}
			else
			{
				while ((rs.next()) && (!canceled) && !stop) {
					if (fetchAll || (rowCounter < upperRowBound)) {
						// add the next tuple to the rsm
						rsm.addTuple(PostgresUtils.createTuple(rs));
						// increase row counter and update counter
						rowCounter++;
						updateCounter++;
						// update the counter label in the dialog every 100 rows
						if (updateCounter == 100) {
							Display.getDefault().asyncExec(new Runnable() {
								public void run() {
									// update row counter
									if (!rowNoLabel.isDisposed()) {
										rowNoLabel.setText(Integer
												.toString(rowCounter));
										rowNoLabel.pack(true);
									}

									//update progress bar
									if (!rowProgress.isDisposed()) {
										rowProgress.setSelection(rowProgress
												.getSelection() + 1);
									}
								}
							});
							// reset update counter for next round
							updateCounter = 0;
						}
					} else {
						stop = true;
					}
				}
			}
			// if we have still rows left in the resultset, 
			// but reached the upper bound, toggle the cancel flag
			if (rs.next()) canceled = true; // upper bound ...

			// remember endTime
			endTime = System.currentTimeMillis();

			// calculate execution time
			executionTime += endTime - startTime;

			// create History Entry to save the results
			final HistoryEntry entry = new HistoryEntry(System.currentTimeMillis(), stmtText);
			entry.setExecutionTime(executionTime); // the execution time
			entry.setQueryResult(rsm); // the resultsetmodel
			entry.setInterrupted(canceled); // the cancel flag
			entry.setAffectedRows(rowCounter); // the number of rows
			ConnectionInfo conInfo = JDBCInterface.getInstance().getCurrentConnectionInfo();
			entry.setDbName(conInfo.getDbname()); // the db name
			// if a query tree was generated store him as a String
			if (queryTree) {
				String tree="";

				entry.setCompilePlan(tree);
			}
			if(executionPlan){
				entry.setExecutionPlan(true);
			}

			// and close the ResultSet
			rs.close();

			return entry;

		} catch (SQLException e) {
			e.printStackTrace();
			ExceptionDialog.show(parentShell, e, true);
		} catch (Exception e) {
			System.out.println("An exception occur.");
			e.printStackTrace();
			ExceptionDialog.show(parentShell, e, true);
		}

		return null;

	}

	/**
	 * this method creates an update result history entry
	 *
	 */
	private HistoryEntry createUpdateResult(){
		try {
			// ***************************************************
			// atomic operation, user cannot cancel operation 
			// this must be an insert/update/delete/call statement
			// create history entry
			final HistoryEntry entry = new HistoryEntry(System.currentTimeMillis(), stmtText);
			entry.setAffectedRows(stmt.getUpdateCount()); // save update count
			entry.setExecutionTime(executionTime); // save execution time
			ConnectionInfo conInfo = JDBCInterface.getInstance().getCurrentConnectionInfo();
			entry.setDbName(conInfo.getDbname()); // save db name
			// if query tree was generated, store him
			if (queryTree) {
			}
			if(executionPlan){
				entry.setExecutionPlan(true);
			}
			return entry;

		} catch (SQLException e) {
			e.printStackTrace();
			ExceptionDialog.show(parentShell, e, true);
		} catch (Exception e) {
			System.out.println("An exception occur.");
			e.printStackTrace();
			ExceptionDialog.show(parentShell, e, true);
		}
		return null;
	}


	/**
	 * This is the run method, in which 
	 * <li>the statement is being executed </li>
	 * <li>if a resultset is returned, the dialog will be informed about the current parsed in rows</li>
	 * <li>furthermore the history is being updated with the creation of history entries</li>
	 * <li>and important application events are sent after important states have been reached 
	 * during the execution </li>
	 */
	public void run() {

		try {
			// save start time of execution
			startTime = System.currentTimeMillis();

			// update status label of dialog
			Display.getDefault().asyncExec(new Runnable() {
				public void run() {
					if(!statusLabel.isDisposed()){
						statusLabel.setText(Constants.InExecutionDialog_executeMessage); 
						statusLabel.pack(true);
					}
				}});

			// create new seconds counter
			secCounterThread = new SecondsCounterThread(startTime,currentLabel);

			// start the counter
			secCounterThread.start();

			// now execute the statement
			System.out.println("......"+stmtText);
			boolean result = stmt.execute(stmtText);

			// get end timestamp after execution to store the
			// execution time
			endTime = System.currentTimeMillis();

			// save executionTime
			executionTime = endTime - startTime;

			// now toggle on cancel button and reset the secounds counter
			// to record the parse in of the results
			Display.getDefault().asyncExec(new Runnable() {
				public void run() {
					// enable cancelButton
					if(!cancelButton.isDisposed()){
						cancelButton.setEnabled(true);
					}

					// set elapsed time needed for execution
					if(!exTimeLabel.isDisposed()){
						long time = endTime - startTime; 
						exTimeLabel.setText((time/1000) + "." + (time/100 % 10)+ "s");
						exTimeLabel.pack();
					}

				}});

			// now postprocess the results
			// if result==true than we have got a resultset to parse in,
			// otherwise we have an result, which didn`t return rows

			if(result){
				entry = createResultSetModel();
			} else {
				entry = createUpdateResult();
			}

			// check for warnings
			SQLWarning warning = stmt.getWarnings();
			if(warning!=null){
				entry.setWarning(warning);
			}

			// close the statement
			stmt.close();

			// add the entry
			if(entry!=null){
				Display.getDefault().asyncExec(new Runnable() {
					public void run() {
						History.getInstance().add(entry);
					}}
				);
			}

		} catch (SQLException e) {
			// ***************************************************
			// ups, the statement did not succeed during execution

			// create history entry to record the exception thrown
			final HistoryEntry entry = new HistoryEntry(System.currentTimeMillis(), stmtText);
			entry.setException(e);
			System.out.println(e.getErrorCode());
			ConnectionInfo conInfo = JDBCInterface.getInstance().getCurrentConnectionInfo();
			entry.setDbName(conInfo.getDbname());

			// add Entry through UI Thread
			Display.getDefault().asyncExec(new Runnable() {
				public void run() {
					History.getInstance().add(entry);
				}}
			);

			// ***************************************************
			// print exception stack tracer for debugging 
			System.out.println("SQLException occured during execution of statement: ");
			System.out.println(stmtText);
			System.out.println("Please review the history ");
			ExceptionDialog.show(parentShell, e, true);

		} catch (Exception e){
			ExceptionDialog.show(parentShell, e, true);
		} finally {
			// actions which are always done
			// stops the second counter thread
			secCounterThread.setStop(true);

			// dispose the run dialog shell    
			Display.getDefault().asyncExec(new Runnable() {
				public void run() {
					shell.dispose();
				}});

			// finally fire Result_Ready event
			Display.getDefault().syncExec(new Runnable() {
				public void run() {
					// fire Result_Ready Event
					GenericModelChangeEvent event3 = new GenericModelChangeEvent();
					event3.detail = DemoEvents.RESULT_READY;
					event3.source = this.toString();
					event3.showResultInConsole = showResultInConsole;
					JDBCInterface.getInstance().fireModelChangeEvent(event3);
				}}
			);

		}
	}
}
