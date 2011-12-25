package components.dialogs;

import java.sql.ResultSet;

import modules.databaseif.ResultSetModel;
import modules.misc.Constants;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Shell;

import components.widgets.TableWidget;

/** A dialog which prints a result set.<br/>
 * <br/>
 * Example:</br>
 * <code>new ExceptionDialog(Display.getCurrent().getActiveShell(), anResultSetModel);</code> 
 * 
 * @author rg
 * @version $Rev: 823 $, $LastChangedDate: 2008-07-27 17:16:26 +0200 (Sun, 27 Jul 2008) $ 
 */
public class ResultSetDialog /*extends Thread*/ {

	// -- variables -------------------------------------------------------------------------------
	
	/** shell of the dialog */
	private Shell shell;
	
	/** parent shell of the dialog */
	private Shell parent;
	
	/** the resultsetmodel for the contents of the tablewidget */
	private ResultSetModel result;

	/** the resultset for the contents of the tablewidget */
	private ResultSet resultSet;

	
	// -- constructors ----------------------------------------------------------------------------
	
	/**
	 * @params result a result set model (must not be null)
	 * 
	 * @throws IllegalArgumentException if result is null
	 */
	public ResultSetDialog(Shell parent, ResultSetModel result) {
		if (result == null) {
			throw new IllegalArgumentException("result must not be null");
		}
		
		this.result = result;
		this.parent = parent;
		initComponents();
	}

	/**
	 * @params result a result set model (must not be null)
	 * 
	 * @throws IllegalArgumentException if result is null
	 */
	public ResultSetDialog(Shell parent, ResultSet resultSet) {
		if (resultSet == null) {
			throw new IllegalArgumentException("resultSet must not be null");
		}
		
		this.resultSet = resultSet;
		this.parent = parent;
		initComponents();
	}

	// -- gui stuff -------------------------------------------------------------------------------
	
	private void initComponents() {
		shell = new Shell(parent, SWT.SHELL_TRIM | SWT.PRIMARY_MODAL);
		shell.setText(Constants.RESULTSETDIALOG_TITLE);
		shell.setLayout(new GridLayout());
		
		// MAIN COMPOSITE
		Composite composite = new Composite(shell, SWT.NONE);
		composite.setLayout(new GridLayout());
		composite.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		
		// label
		Label l = new Label(composite, SWT.NONE);
		l.setText(Constants.RESULTSETDIALOG_LABEL);
		
		// table
		final TableWidget t = new TableWidget(composite, SWT.NONE);
		t.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		if (result != null)
			t.createTable(result);
		else
			t.createTable(resultSet);
		
		// ok button
		Button okButton = new Button(composite, SWT.PUSH | SWT.CENTER);
		okButton.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_CENTER));
		okButton.setText(" " + Constants.OK + " ");
		okButton.addListener(SWT.Selection, new Listener(){
			public void handleEvent(Event event) {
				if (!shell.isDisposed()) {
					shell.dispose();
				}
			}
		});
		okButton.setFocus();
		
		t.addListener(SWT.Resize, new Listener() {
			public void handleEvent(Event event) {
				if (!t.isDisposed()) {
					t.setSize2();
				}
			}
		});
		
		composite.pack();
		Point pTable = t.computeTableSize();
		Point pLabel = l.computeSize(SWT.DEFAULT, SWT.DEFAULT);
		shell.setSize(Math.max(pLabel.x+50, Math.min(640, pTable.x+50)), Math.min(480, pTable.y + 100));
		shell.open();		
	}
	
//	// -- thread ----------------------------------------------------------------------------------
//	// remove
//	public void run() {
//		Display display = parent.getDisplay();
//		while (!shell.isDisposed()) {
//			if (!display.readAndDispatch()) display.sleep();
//		}
//	}
}
