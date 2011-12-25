package components.dialogs;

import components.widgets.TableWidget;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.sql.ResultSet;

import modules.misc.Constants;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Shell;

/** A dialog which prints an exception.<br/>
 * <br/>
 * Example:</br>
 * <code>new InfoDialog(Display.getCurrent().getActiveShell(), anException);</code>
 * 
 * @author rg, Sebastian Seifert ,Christopher Schildt
 * @version $Rev: 805 $, $LastChangedDate: 2011-09-18 11:43:28 +0200 (Wed, 23 Jul 2008) $
 */
public class InfoDialog /* extends Thread */{

	// -- variables -------------------------------------------------------------------------------
	
	/** shell of the dialog */
	private Shell shell;
	
	/** parent shell of the dialog */
	private Shell parent;
	

        private ResultSet es;
        int size;
	
	/** the dialog which shows the exceptions in tabs */
	private static ExceptionTabDialog tabVersion;
    private TableWidget table;
	
	
	// -- constructors ----------------------------------------------------------------------------
	
        protected InfoDialog(Shell parent, ResultSet e,int size) {


		this.es = e;
		this.parent = parent;
                this.size=size;

		initComponents();
	}
	

	
	/**
	 * inits the window
	 */
	private void initComponents() {
		shell = new Shell(parent, SWT.SHELL_TRIM | SWT.APPLICATION_MODAL);
		shell.setText(Constants.INFONDIALOG_TITLE);
		shell.setLayout(new GridLayout());
		
		// MAIN COMPOSITE
		Composite composite = new Composite(shell, SWT.NONE);
		composite.setLayout(new GridLayout());
		composite.setLayoutData(new GridData(SWT.FILL, SWT.None, true, true));
		
		// label
		new Label(composite, SWT.NONE).setText(Constants.INFODIALOG_LABEL);
		table = new TableWidget(composite,SWT.H_SCROLL | SWT.V_SCROLL);
                GridData TL2Data = new GridData();

            TL2Data.grabExcessHorizontalSpace = true;
            TL2Data.horizontalAlignment = GridData.FILL;
            TL2Data.heightHint=Math.min(300, 60+this.size*30);
            table.setLayoutData(TL2Data);

            if(es!=null)
                table.createTable(this.es);
                table.addListener(SWT.Resize, new Listener() {
			public void handleEvent(Event event) {
					table.setSizeNew();
			}
		});
	
		
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
		
		composite.pack();
		//shell.pack();

		shell.setSize(250, Math.min(450, 190+this.size*30));
		shell.open();
	}
	


        public static void show_result(final Shell parent, final ResultSet e, final boolean showInTab,final int rows){
        // run this in the gui thread, when the method call comes not from the gui thread
		Display.getDefault().asyncExec(new Runnable() {
            public void run() {
        		
        			new InfoDialog(parent, e,rows);
       }});

	}
	
	// -- thread ----------------------------------------------------------------------------------
	// remove
	public void run() {
		Display display = parent.getDisplay();
		while (!shell.isDisposed()) {
			if (!display.readAndDispatch()) display.sleep();
		}
	}
}
