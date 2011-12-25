package components.dialogs;

import java.util.Map;

import modules.misc.Constants;
import modules.misc.StringUtils;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Shell;

import components.widgets.TableWidget;

public class StmtDetailsDialog extends Thread {
    // -- variables -------------------------------------------------------------------------------
    
    /** shell of the dialog */
    private Shell shell;
    
    /** parent shell of the dialog */
    private Shell parent;
    
    /** the resultsetmodel for the contents of the tablewidget */
    private Map details;
    
    
    // -- constructors ----------------------------------------------------------------------------
    
    /**
     * @params result a result set model (must not be null)
     * 
     * @throws IllegalArgumentException if result is null
     */
    public StmtDetailsDialog(Shell parent, Map details) {
        if (details == null) {
            throw new IllegalArgumentException("details cannot be null!");
        }
        
        this.details = details;
        this.parent = parent;
        initComponents();
    }
    
    // -- gui stuff -------------------------------------------------------------------------------
    
    private void initComponents() {
        shell = new Shell(parent, SWT.SHELL_TRIM);
        
        // create a nice dialog title
        String stmtText = (String)details.get("STMT_TEXT");
        String title = Constants.STMTDETAILS_TITLE +
                       ": " + 
                       StringUtils.format(StringUtils.replaceDelChars(stmtText),50,false);
        shell.setText(title);
        shell.setLayout(new GridLayout());
        
        // MAIN COMPOSITE
        Composite composite = new Composite(shell, SWT.NONE);
        composite.setLayout(new GridLayout());
        composite.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
        
        // table
        final TableWidget t = new TableWidget(composite, SWT.NONE);
        t.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
        t.createTable(details,"Columnname","Value");      
        
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
        
        t.addListener(SWT.Resize, new Listener() {
            public void handleEvent(Event event) {
                if (!t.isDisposed()) {
                    t.setSize2();
                }
            }
        });
        
        composite.pack();
        Point pTable = t.computeTableSize();
        shell.setSize(Math.min(640, pTable.x+50), Math.min(480, pTable.y + 100));
        shell.open();       
    }
    
    // -- thread ----------------------------------------------------------------------------------
    
    public void run() {
        Display display = parent.getDisplay();
        while (!shell.isDisposed()) {
            if (!display.readAndDispatch()) display.sleep();
        }
    }

}
