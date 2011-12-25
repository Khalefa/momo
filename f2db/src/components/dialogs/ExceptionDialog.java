package components.dialogs;

import java.io.PrintWriter;
import java.io.StringWriter;

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
 * <code>new ExceptionDialog(Display.getCurrent().getActiveShell(), anException);</code> 
 * 
 * @author rg, Sebastian Seifert ,Christopher Schildt
 * @version $Rev: 805 $, $LastChangedDate: 2011-09-18 11:43:28 +0200 (Wed, 23 Jul 2008) $
 */
public class ExceptionDialog /* extends Thread */{

	// -- variables -------------------------------------------------------------------------------
	
	/** shell of the dialog */
	private Shell shell;
	
	/** parent shell of the dialog */
	private Shell parent;
	
	/** the exception */
	private Exception e;
        private String es;
	
	/** the dialog which shows the exceptions in tabs */
	private static ExceptionTabDialog tabVersion;
	
	
	// -- constructors ----------------------------------------------------------------------------
	
	/**
	 * @params e an exception (must not be null)
	 * 
	 * @throws IllegalArgumentException if e is null
	 */
	protected ExceptionDialog(Shell parent, Exception e) {
		if (e == null) {
			throw new IllegalArgumentException("e must not be null");
		}
		
		this.e = e;
		this.parent = parent;

		initComponents();
	}
        protected ExceptionDialog(Shell parent, String e) {
		if (e == null) {
			throw new IllegalArgumentException("e must not be null");
		}

		this.es = e;
		this.parent = parent;

		initComponents();
	}
	
	// -- gui stuff -------------------------------------------------------------------------------

	/**
	 * inits the textbox where the exception is displayed
	 * @param composite
	 * @param e
	 * @return
	 */
	protected StyledText createStyledTextForException(Composite composite, Exception e){
		// text with exception
		StyledText text = new StyledText(composite, SWT.MULTI | SWT.V_SCROLL | SWT.H_SCROLL);
		text.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		text.setEditable(false);
		if (e == null || e.getMessage()==null) {
			text.setText("NULL");
		} else {
			StringWriter sw = new StringWriter();
			PrintWriter pw = new PrintWriter(sw);
			Throwable t = e;
			while (t != null){

                            t.printStackTrace(pw);
				t = t.getCause();
			}
			text.setText(sw.toString());
                         text.setText(e.getMessage());
                      
		}
               
		
		return text;
	}
        protected StyledText createStyledTextForString(Composite composite, String e){
		// text with exception
		StyledText text = new StyledText(composite, SWT.MULTI | SWT.V_SCROLL | SWT.H_SCROLL);
		text.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		text.setEditable(false);
		if (e == null) {
			text.setText("NULL");
		} else {
			StringWriter sw = new StringWriter();
			PrintWriter pw = new PrintWriter(sw);
			text.setText(sw.toString());
		}
                text.setText(e);

		return text;
	}
	
	/**
	 * inits the window
	 */
	private void initComponents() {
		shell = new Shell(parent, SWT.SHELL_TRIM | SWT.APPLICATION_MODAL);
		shell.setText(Constants.EXCEPTIONDIALOG_TITLE);
		shell.setLayout(new GridLayout());
		
		// MAIN COMPOSITE
		Composite composite = new Composite(shell, SWT.NONE);
		composite.setLayout(new GridLayout());
		composite.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		
		// label
		new Label(composite, SWT.NONE).setText(Constants.EXCEPTIONDIALOG_LABEL);
		
		initSpecialComponent(composite);
		
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
		shell.setSize(500, 200);
		shell.open();
	}
	
	/**
	 * inits the main part where the exceptions are shown
	 * @param composite
	 */
	protected void initSpecialComponent(Composite composite){
		createStyledTextForException(composite, e);
	}
	
	/**
	 * shows this exception in a guiwindow
	 * @param parent
	 * @param e
	 * @param showInTab if its true, the exceptions are shown in tabs. its false, the exceptions are show each in separate dialogs.
	 */
	public static void show(final Shell parent, final Exception e, final boolean showInTab){
		if (Constants.showExceptionsInConsoleToo)
			e.printStackTrace();

        // run this in the gui thread, when the method call comes not from the gui thread
		Display.getDefault().asyncExec(new Runnable() {
            public void run() {
        		if (showInTab) {
        			if (tabVersion == null){
        				tabVersion = new ExceptionTabDialog(parent, e);
        				// add a DisposeListener which get called when the user closes the application
        				tabVersion.addDisposeListener(new DisposeListener(){
        		            public void widgetDisposed(DisposeEvent e) {
        		            	// set it to null, a new window can then be create 
        		            	tabVersion = null;
        		            }});

        			}
        			
        			// TODO different parent ???
        			
        			tabVersion.addException(e);
        		} else
        			new ExceptionDialog(parent, e);
       }});

	}
        public static void show_string(final Shell parent, final String e, final boolean showInTab){
		if (Constants.showExceptionsInConsoleToo)
			System.out.println(e);

        // run this in the gui thread, when the method call comes not from the gui thread
		Display.getDefault().asyncExec(new Runnable() {
            public void run() {
        		if (showInTab) {
        			if (tabVersion == null){
        				tabVersion = new ExceptionTabDialog(parent, e);
        				// add a DisposeListener which get called when the user closes the application
        				tabVersion.addDisposeListener(new DisposeListener(){
        		            public void widgetDisposed(DisposeEvent e) {
        		            	// set it to null, a new window can then be create
        		            	tabVersion = null;
        		            }});

        			}

        			// TODO different parent ???

        			tabVersion.addString(e);
        		} else
        			new ExceptionDialog(parent, e);
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
