package components.dialogs;

import java.util.Iterator;
import java.util.List;

import modules.databaseif.JDBCInterface;
import modules.databaseif.SecondsCounterThread;
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

/**
 * This class executes multi queries in separate thread without returns results
 * 
 * @author torsten weber, Sebastian Seifert
 *
 */
public class InExecutionMultiQueriesDialog extends Thread {

	private Shell					shell;
	private Shell					parentShell;
	
	private Label					currentStatement;
	private Label					executionTime;
	private ProgressBar				statementProgress;
	private Button					cancelButton;
	private int						executedStatementCount;
	
	private boolean					canceled;
	
	private long					startTime;
	private long					endTime;
	
	private SecondsCounterThread	secCounterThread;
	private List<String>			executionStatements;		// sample names as list
	
	public InExecutionMultiQueriesDialog(
			Shell parent,
			List<String> executionStatements) {
		super("ExecutionMultiQueriesThread");
		this.parentShell = parent;
		this.executionStatements = executionStatements;
	}

	/** show this dialog */
	public void show(){

		//Display display = Display.getDefault();
		shell = new Shell(parentShell, SWT.DIALOG_TRIM | SWT.RESIZE | SWT.APPLICATION_MODAL);
		shell.setSize(300,150);
		shell.setText(Constants.InExecutionMultiQueriesDialog_Dialog_Title);
		
		shell.setImage(ResourceRegistry.getInstance().getImage("icon"));

		// create dialog components and listeners 
		initComponents();
		initListeners();

		// open dialog
		shell.open();
	}

	/**
	 * this method inits all the dialog components
	 */
	private void initComponents(){
		Composite c = new Composite(shell,SWT.NONE);
		GridLayout thisLayout = new GridLayout();
		thisLayout.numColumns = 2;
		c.setLayout(thisLayout);
		c.setSize(350, 250);
		{
			Label currentStatementLabel = new Label(c, SWT.NONE);
			GridData currentStatementLabelGD = new GridData();
			currentStatementLabelGD.horizontalAlignment = GridData.END;
			currentStatementLabel.setLayoutData(currentStatementLabelGD);
			currentStatementLabel.setText(Constants.InExecutionMultiQueriesDialog_Current_Statement);

			currentStatement = new Label(c, SWT.NONE);
			GridData currentSampleGD = new GridData();
			currentSampleGD.horizontalAlignment = GridData.FILL;
			currentStatement.setLayoutData(currentSampleGD);
			currentStatement.setText("");
		}
		{
			Label exTimeLabel = new Label(c, SWT.NONE);
			GridData exTimeLabelGD = new GridData();
			exTimeLabelGD.horizontalAlignment = GridData.END;
			exTimeLabel.setLayoutData(exTimeLabelGD);
			exTimeLabel.setText(Constants.InExecutionDialog_executeLabelText);

			executionTime = new Label(c, SWT.NONE);
			GridData executeTimeGD = new GridData();
			executeTimeGD.horizontalAlignment = GridData.FILL;
			executionTime.setLayoutData(executeTimeGD);
			executionTime.setText("0.0");
		}
		{
			statementProgress = new ProgressBar(c, SWT.INDETERMINATE);
			GridData sampleProgressGD = new GridData();
			sampleProgressGD.grabExcessHorizontalSpace = true;
			sampleProgressGD.horizontalAlignment = GridData.FILL;
			sampleProgressGD.horizontalSpan = 2;
			statementProgress.setLayoutData(sampleProgressGD);

			statementProgress.setMaximum(executionStatements.size());
			statementProgress.setMinimum(0);
			statementProgress.setSelection(0);
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
			cancelButton.setEnabled(true);
		}
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

				Display.getDefault().asyncExec(new Runnable() {
					public void run() {
						cancelButton.setEnabled(false);
					}});
			}});
	}

	/**
	 * This is the run method, in which 
	 * <li>the statements are being executed </li>
	 * <li>if a resultset is returned, the dialog will be informed about the current parsed in rows</li>
	 * <li>furthermore the history is being updated with the creation of history entries</li>
	 * <li>and important application events are sent after important states have been reached 
	 * during the execution </li>
	 */
	public void run() {
		Iterator<String> it = executionStatements.iterator();
		// for each using sample algorithm enable it, send approximate query and disable it again
		while (it.hasNext() && !canceled) {
			String thisStatementName = it.next();
			// update currentStatement label of dialog
			final String statementText = thisStatementName;
			Display.getDefault().asyncExec(new Runnable() {
				public void run() {
					currentStatement.setText(statementText);
					currentStatement.pack(true);
					executionTime.setText("0.0");
					executionTime.pack(true);
				}});

			// query
			executeStmt(statementText); // approximate statement
		}
		
		// dispose the run dialog shell    
		Display.getDefault().asyncExec(new Runnable() {
			public void run() {
				shell.dispose();
			}});
	}
	
	private void executeStmt(final String statementText) {
		// query
		// save start time of execution
		startTime = System.currentTimeMillis();
		// create new seconds counter
		secCounterThread = new SecondsCounterThread(startTime, executionTime);
		// start the counter
		secCounterThread.start();
		// execute
//		JDBCInterface.getInstance().setWorkloadRecording(false);
		JDBCInterface.getInstance().executeStatement(
				statementText,
				0,
				false,
				false,
				false,
				true);
//		JDBCInterface.getInstance().setWorkloadRecording(true);
		// get end timestamp after execution to store the
		// execution time
		endTime = System.currentTimeMillis();
		// stop counter
		secCounterThread.setStop(true);
		// save executionTime
		// update executed sample counter
		executedStatementCount++;
		// now toggle on cancel button and reset the seconds counter
		// to record the parse in of the results
		Display.getDefault().asyncExec(new Runnable() {
			public void run() {
				// enable cancelButton
				if (!cancelButton.isDisposed()) {
					cancelButton.setEnabled(true);
				}
				// set elapsed time needed for execution
				if (!executionTime.isDisposed()) {
					long time = endTime - startTime; 
					executionTime.setText((time/1000) + "." + (time/100 % 10)+ "s");
					executionTime.pack();
				}
				// set progress bar
				if (!statementProgress.isDisposed()) {
					statementProgress.setSelection(executedStatementCount);
				}
			}});		
	}
}
