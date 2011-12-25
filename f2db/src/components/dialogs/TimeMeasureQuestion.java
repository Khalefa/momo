package components.dialogs;

import components.listeners.ChangeModelParameterListener;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.LinkedList;
import java.util.List;

import modules.databaseif.JDBCInterface;
import modules.misc.Constants;
import modules.misc.ResourceRegistry;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.layout.RowLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

import components.listeners.DefaultLineStyleListener;
import components.listeners.SQLKeywordLineStyleListener;
import components.listeners.UpperKeyListener;

/**
 * This is a helper dialog for the create model statement
 * @author Christopher Schildt
 * @date   08.05.2006
 *
 */
public class TimeMeasureQuestion {

    private String result = null;
    private Shell parent;
    private Shell shell;
    private Label timeL;
    private Label measureL;
    private Text timeT;
    private Text measueT;
    private Composite buttonComposite;
    private Button optiButton;
    private Button okButton;
    private Button cancelButton;



    // **********************************************************************
    // Constructors
    // **********************************************************************
    public TimeMeasureQuestion(Shell parent) {
        this.parent = parent;

    }


    /**
     * this method opens the dialog
     * @return only a string indicating the button state
     */
    public String open() {
        shell = new Shell(parent, SWT.DIALOG_TRIM | SWT.APPLICATION_MODAL | SWT.RESIZE);
        shell.setText("test");


        initComponents();
        initListeners();

        activatecomponents();
        shell.setImage(ResourceRegistry.getInstance().getImage("icon"));

        shell.open();
        Display display = parent.getDisplay();
        while (!shell.isDisposed()) {
            if (!display.readAndDispatch()) {
                display.sleep();
            }
        }
        return result;
    }



    /**
     * this method inits all ui widgets of this dialog
     */
    private void initComponents() {
        GridLayout dialogShellLayout = new GridLayout();
        shell.setLayout(dialogShellLayout);
        dialogShellLayout.numColumns = 2;
        shell.layout();
        shell.pack();
        shell.setSize(250, 160);
        {

 
                {
 

                    timeL = new Label(shell, SWT.NONE);
                    timeL.setText("TimeColumn");

                }
                {
 

                    measueT = new Text(shell, SWT.NONE);

                    measueT.setEnabled(true);


                }
                {
                   measureL = new Label(shell, SWT.NONE);
                    measureL.setText("MeasureColumn");

                }
                {


                    timeT = new Text(shell, SWT.NONE);

                    timeT.setEnabled(true);

                }
                
            }
     
 
        {
            buttonComposite = new Composite(shell, SWT.NONE);
            GridLayout buttonCompositeLayout = new GridLayout();
            buttonCompositeLayout.makeColumnsEqualWidth = true;
            buttonCompositeLayout.numColumns = 1;
            GridData buttonCompositeLData = new GridData();
            buttonCompositeLData.grabExcessHorizontalSpace = true;
            buttonCompositeLData.horizontalAlignment = GridData.FILL;
            buttonCompositeLData.verticalAlignment = GridData.FILL;
            buttonCompositeLData.horizontalSpan = 2;
            buttonComposite.setLayoutData(buttonCompositeLData);
            buttonComposite.setLayout(buttonCompositeLayout);

            {
                okButton = new Button(buttonComposite, SWT.PUSH
                        | SWT.CENTER);
                GridData okButtonLData = new GridData();
                okButtonLData.grabExcessHorizontalSpace = true;
                okButtonLData.horizontalAlignment = GridData.FILL;
                okButton.setLayoutData(okButtonLData);
                okButton.setText("Ok");
                okButton.setEnabled(true);
            }
            {
                cancelButton = new Button(buttonComposite, SWT.PUSH
                        | SWT.CENTER);
                GridData cancelButtonLData = new GridData();
                cancelButtonLData.grabExcessHorizontalSpace = true;
                cancelButtonLData.horizontalAlignment = GridData.FILL;
                cancelButton.setLayoutData(cancelButtonLData);
                cancelButton.setText(Constants.model_create_cancel);
            }
        }
}
    

    /**
     * this method inits all the different listeners which get called through
     * interaction with this dialog
     */
    private void initListeners() {







        cancelButton.addListener(SWT.Selection, new Listener() {

            public void handleEvent(Event event) {
                // close dialog
                shell.dispose();
                // the return string
                result = "CANCEL";
            }
        });


        okButton.addListener(SWT.Selection, new Listener() {

          

            public void handleEvent(Event event) {
                
                  result=timeT.getText()+"#"+measueT.getText();
                  shell.dispose();
            }
        });
    }


   
    /**
     * this method gets the title text of the dialog
     * used in the initcomponents method te determine the title string
     */
    public String getText() {
        return Constants.model_create_title;
    }



    private void activatecomponents() {
  
    }
}

