package components.dialogs;

import java.io.File;

import modules.databaseif.ConnectionInfo;
import modules.databaseif.JDBCInterface;
import modules.misc.Constants;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Dialog;
import org.eclipse.swt.widgets.DirectoryDialog;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;


import components.main.MainStatusLine;

/**
 * <b>Add/Edit Connection Dialog</b>, <br/> allows the manipulation of a connection.
 * Derby specifics extracted from 
 * http://db.apache.org/derby/docs/dev/getstart/
 * @author Felix Beyer, Christopher Schildt
 * @date   05.10.2006
 *
 */
public class AddEditConnectionDialog extends Dialog{

	private Shell dialogShell;
	
	private Label connectionLabel;
    private Text connectionText;

    private Label typeLabel;
    private Button embeddedTypeRadioButton;
    private Button netTypeRadioButton;
    
    private Label nameLabel;
    private Text  nameText;

    private Label dbportLabel;
    private Text  dbportText;
    
    private Button db2modeCheckbox;
    private Button postgresModeCheckbox;
    
    private Label SummaryLabel2;
	private Composite composite1;
	private Label SummaryLabel;
	private Text passwordText;
	private Label passwordLabel;
	private Text userText;
	private Label userLabel;
	private Button createCheckbox;
	private Button meCheckbox;
	private Button createTpchCheckbox;
	private Button createTpchSamplesCheckbox;
	private Composite buttonComposite;
	private Button cancelButton;
	private Button okButton;
	private Button testButton;
	private Label statusLabel2;
	private Label statusLabel;
	private Button directoryButton;
	
    private Text dblocText;
	private Label dblocLabel;
	private Label fillerLabel;
	
    
    private Button dbLocale;
    private Combo dbLocaleCombobox;
    
    /** the supported locales from the dialog */
    private String[] locales = new String[]{"en_US", "de_DE"};
    
	/** the different test states*/
	private final String[] states = Constants.config_db_states;
	
	/** flag which indicates if the connection is testable */
	private boolean testable = false;

    /** flag which indicates if the current specified ConInfo object should be kept and returned to the caller of this dialog*/
    private boolean keepResults = false;
    
	/** the returned result object */
	private ConnectionInfo result; 
	
	/** the constructor */
	public AddEditConnectionDialog(Shell parent, int style) {
		super(parent, style);
	}
	/** init gui components (widgets & controls) */
	private void initComponents(){

		GridLayout dialogShellLayout = new GridLayout();
		dialogShell.setLayout(dialogShellLayout);
		dialogShellLayout.numColumns = 5;
		//dialogShell.layout();
		//dialogShell.pack();
		//dialogShell.setSize(430, 280);
		{
			connectionLabel = new Label(dialogShell, SWT.NONE);
			GridData connectionLabelLData = new GridData();
			connectionLabelLData.horizontalAlignment = GridData.FILL;
			connectionLabel.setLayoutData(connectionLabelLData);
			connectionLabel.setText("Connection Alias :");
		}
		{
			GridData ConnectionTextLData = new GridData();
			ConnectionTextLData.horizontalSpan = 3;
			ConnectionTextLData.grabExcessHorizontalSpace = true;
			ConnectionTextLData.horizontalAlignment = GridData.FILL;
			connectionText = new Text(dialogShell, SWT.BORDER);
			connectionText.setText(result.getAlias());
			connectionText.setLayoutData(ConnectionTextLData);
		}
        {
            fillerLabel = new Label(dialogShell, SWT.NONE);
            GridData fillerLabelLData = new GridData();
            fillerLabelLData.horizontalAlignment = GridData.FILL;
            fillerLabel.setLayoutData(fillerLabelLData);
        }
        {
            typeLabel = new Label(dialogShell, SWT.NONE);
            GridData typeLabelLData = new GridData();
            typeLabelLData.horizontalAlignment = GridData.FILL;
            typeLabel.setLayoutData(typeLabelLData);
            typeLabel.setText("Type :");
        }
        
        
        Composite test = new Composite(dialogShell, SWT.SHADOW_ETCHED_IN);
        GridLayout a = new GridLayout();
        a.numColumns=6;
        a.makeColumnsEqualWidth=true;
        test.setLayout(a);
        GridData testest = new GridData();
        testest.horizontalSpan = 4;
        test.setLayoutData(testest);
        
        {
        	postgresModeCheckbox=new Button(test, SWT.CHECK
	                | SWT.RIGHT);
	        postgresModeCheckbox.setText("Use Postgres");
	        postgresModeCheckbox.setSelection(true);
	        postgresModeCheckbox.pack();
        }
        {
            fillerLabel = new Label(test, SWT.NONE);
            GridData fillerLabelLData = new GridData();
            fillerLabelLData.horizontalAlignment = GridData.FILL;
            fillerLabel.setLayoutData(fillerLabelLData);
        }
        
        
        {   embeddedTypeRadioButton = new Button(test, SWT.CHECK);
 
            GridData embeddedTypeGData = new GridData();
            embeddedTypeGData.horizontalAlignment = GridData.FILL;
            embeddedTypeRadioButton.setLayoutData(embeddedTypeGData);
            embeddedTypeRadioButton.setEnabled(!JDBCInterface.getInstance().isPostgres_connected());
            embeddedTypeRadioButton.setText("embedded");
            embeddedTypeRadioButton.setSelection(result.isEmbedded());
            embeddedTypeRadioButton.pack();
            
            
            netTypeRadioButton = new Button(test, SWT.RADIO);
            GridData netTypeGData = new GridData();
            embeddedTypeGData.horizontalAlignment = GridData.FILL;
            netTypeRadioButton.setLayoutData(netTypeGData);
            netTypeRadioButton.setEnabled(!JDBCInterface.getInstance().isPostgres_connected());
            netTypeRadioButton.setText("net client");
            netTypeRadioButton.setSelection(!result.isEmbedded());
            netTypeRadioButton.pack();
        }

        
            db2modeCheckbox = new Button(test, SWT.CHECK
                | SWT.LEFT);
            GridData db2modeData = new GridData();
            db2modeData.grabExcessVerticalSpace = true;
            db2modeCheckbox.setLayoutData(db2modeData);
            db2modeCheckbox.setText("use DB2 Driver");
            db2modeCheckbox.setEnabled(!JDBCInterface.getInstance().isPostgres_connected() && !result.isEmbedded());
            db2modeCheckbox.setSelection(result.isDB2Mode());
            db2modeCheckbox.pack();
            
            
        {
            nameLabel = new Label(dialogShell, SWT.NONE);
            GridData nameLabelData = new GridData();
            nameLabelData.horizontalAlignment = GridData.FILL;
            nameLabel.setLayoutData(nameLabelData);
            nameLabel.setText("DB name :");
        }
        {
            GridData nameTData = new GridData();
            nameTData.horizontalSpan = 3;
            nameTData.grabExcessHorizontalSpace = true;
            nameTData.horizontalAlignment = GridData.FILL;
            nameText = new Text(dialogShell, SWT.BORDER);
            nameText.setText(result.getDbname());
            nameText.setLayoutData(nameTData);
        }

        {
            fillerLabel = new Label(dialogShell, SWT.NONE);
            GridData fillerLabelLData = new GridData();
            fillerLabelLData.horizontalAlignment = GridData.FILL;
            fillerLabel.setLayoutData(fillerLabelLData);
        }
        {
			dblocLabel = new Label(dialogShell, SWT.NONE);
			GridData dblocLabelLData = new GridData();
			dblocLabelLData.horizontalAlignment = GridData.FILL;
			dblocLabel.setLayoutData(dblocLabelLData);
			dblocLabel.setText("DB location (url vs. path) :");
		}
		{
			GridData text1LData = new GridData();
			text1LData.horizontalSpan = 3;
			text1LData.grabExcessHorizontalSpace = true;
			text1LData.horizontalAlignment = GridData.FILL;
			dblocText = new Text(dialogShell, SWT.BORDER);
			dblocText.setText(result.getDblocation());
			dblocText.setLayoutData(text1LData);
		}
		{
			directoryButton = new Button(dialogShell, SWT.PUSH | SWT.CENTER);
			GridData directoryButtonLData = new GridData();
			directoryButtonLData.horizontalAlignment = GridData.FILL;
			directoryButtonLData.grabExcessVerticalSpace = true;
			directoryButton.setLayoutData(directoryButtonLData);
			directoryButton.setText("...");
		}
        {
            dbportLabel = new Label(dialogShell, SWT.NONE);
            GridData dbportData = new GridData();
            dbportData.horizontalAlignment = GridData.FILL;
            dbportLabel.setLayoutData(dbportData);
            dbportLabel.setText("DB port :");
        }
        {
            GridData portTData = new GridData();
            portTData.horizontalSpan = 1;
            portTData.grabExcessHorizontalSpace = true;
            portTData.horizontalAlignment = GridData.FILL;
            dbportText = new Text(dialogShell, SWT.BORDER);
            dbportText.setText(result.getDbport());
            dbportText.setLayoutData(portTData);
            dbportText.setEnabled(true);
        }
        {
            dbLocale = new Button(dialogShell, SWT.CHECK);
            GridData dblocaleLabelData = new GridData();
            dblocaleLabelData.horizontalAlignment = GridData.FILL;
            dbLocale.setLayoutData(dblocaleLabelData);
            dbLocale.setText("DB locale :");
        }

        {   dbLocaleCombobox = new Combo(dialogShell, SWT.READ_ONLY);
            GridData dbLocaleData = new GridData();
            dbLocaleData.horizontalSpan = 1;
            dbLocaleData.grabExcessHorizontalSpace = true;
            dbLocaleData.horizontalAlignment = GridData.FILL;
            dbLocaleCombobox.setLayoutData(dbLocaleData);
            // fill with locale infos
            dbLocaleCombobox.setItems(locales);
            dbLocaleCombobox.select(0);
        }
        {   String loc = result.getLocale();
            if (loc!=null){
                dbLocale.setSelection(true);
                dbLocaleCombobox.setEnabled(true);
                int selectable=0;
                for(int i=0;i<this.locales.length;i++){
                    if(loc.equalsIgnoreCase(locales[i])){
                        selectable = i;
                        break;
                    }
                }
                dbLocaleCombobox.select(selectable);
            } else {
                dbLocale.setSelection(false);
                dbLocaleCombobox.setEnabled(false);
            }
            
        }
        {
            fillerLabel = new Label(dialogShell, SWT.NONE);
            GridData fillerLabelLData = new GridData();
            fillerLabelLData.horizontalAlignment = GridData.FILL;
            fillerLabel.setLayoutData(fillerLabelLData);
        }
        {
			userLabel = new Label(dialogShell, SWT.NONE);
			GridData userLabelLData = new GridData();
			userLabelLData.horizontalAlignment = GridData.FILL;
			userLabel.setLayoutData(userLabelLData);
			userLabel.setText("User :");
		}
		{
			GridData text2LData = new GridData();
			text2LData.horizontalSpan = 3;
			text2LData.grabExcessHorizontalSpace = true;
			text2LData.horizontalAlignment = GridData.FILL;
			userText = new Text(dialogShell, SWT.BORDER);
			userText.setText(result.getUser());
			userText.setLayoutData(text2LData);
		}
		{
			new Label(dialogShell, SWT.NONE);
		}
		{
			passwordLabel = new Label(dialogShell, SWT.NONE);
			GridData passwordLabelLData = new GridData();
			passwordLabelLData.horizontalAlignment = GridData.FILL;
			passwordLabel.setLayoutData(passwordLabelLData);
			passwordLabel.setText("Password :");
		}
		{
			GridData text3LData = new GridData();
			text3LData.horizontalSpan = 3;
			text3LData.grabExcessHorizontalSpace = true;
			text3LData.horizontalAlignment = GridData.FILL;
			passwordText = new Text(dialogShell, SWT.BORDER);
			passwordText.setText(result.getPassword());
			passwordText.setEchoChar('*');
			passwordText.setLayoutData(text3LData);
		}
		{
			new Label(dialogShell, SWT.NONE);
		}
		{
			SummaryLabel = new Label(dialogShell, SWT.NONE);
			GridData SummaryLabelLData = new GridData();
			SummaryLabelLData.horizontalAlignment = GridData.FILL;
			SummaryLabel.setLayoutData(SummaryLabelLData);
			SummaryLabel.setText("Connection string :");
		}
		{
			GridData SummaryLabel2LData = new GridData();
			SummaryLabel2LData.horizontalSpan = 3;
			SummaryLabel2LData.grabExcessHorizontalSpace = true;
			SummaryLabel2LData.horizontalAlignment = GridData.FILL;
			SummaryLabel2 = new Label(dialogShell, SWT.BORDER);
			SummaryLabel2.setLayoutData(SummaryLabel2LData);
		}
		{
			new Label(dialogShell, SWT.NONE);
		}
		{
			statusLabel = new Label(dialogShell, SWT.NONE);
			GridData statusLabelLData = new GridData();
			statusLabelLData.horizontalAlignment = GridData.FILL;
			statusLabel.setLayoutData(statusLabelLData);
			statusLabel.setText("Status :");
		}
		{
			GridData statusLabel2LData = new GridData();
			statusLabel2LData.horizontalSpan = 3;
			statusLabel2LData.grabExcessHorizontalSpace = true;
			statusLabel2LData.horizontalAlignment = GridData.FILL;
			statusLabel2 = new Label(dialogShell, SWT.BORDER);
			statusLabel2.setText(states[0]);
			statusLabel2.setLayoutData(statusLabel2LData);
		}
		{
			new Label(dialogShell, SWT.NONE);
		}
		
			{
				composite1 = new Composite(dialogShell, SWT.NONE);
				GridLayout composite1Layout = new GridLayout();
				composite1Layout.makeColumnsEqualWidth = true;
				composite1Layout.numColumns = 3;
				GridData composite1LData = new GridData();
				composite1LData.horizontalSpan = 3;
				composite1LData.horizontalAlignment = GridData.FILL;
				composite1LData.grabExcessHorizontalSpace = true;
				composite1.setLayoutData(composite1LData);
				composite1.setLayout(composite1Layout);
				{
					testButton = new Button(composite1, SWT.PUSH
						| SWT.CENTER);
					GridData testButtonLData = new GridData();
					testButtonLData.grabExcessHorizontalSpace = true;
					testButtonLData.horizontalAlignment = GridData.FILL;
					testButton.setLayoutData(testButtonLData);
					testButton.setText("Test");
					testButton.setEnabled(testable); //enable/disable test button
				}
				{
					okButton = new Button(composite1, SWT.PUSH | SWT.CENTER);
					GridData okButtonLData = new GridData();
					okButtonLData.horizontalAlignment = GridData.FILL;
					okButton.setLayoutData(okButtonLData);
					okButton.setText("OK");
					okButton.setEnabled(checkInput());
				}
				{
					cancelButton = new Button(composite1, SWT.PUSH
						| SWT.CENTER);
					GridData cancelButtonLData = new GridData();
					cancelButtonLData.grabExcessHorizontalSpace = true;
					cancelButtonLData.horizontalAlignment = GridData.FILL;
					cancelButton.setLayoutData(cancelButtonLData);
					cancelButton.setText("Cancel");
				}
			}
		
        dialogShell.layout();
        dialogShell.pack();

	}
	
	private void initListeners(){
		
		// ----------- User Input Listeners -------------------
		connectionText.addModifyListener(new ModifyListener(){
			public void modifyText(ModifyEvent e) {
				result.setAlias(connectionText.getText());
				updateSummary();
			}
		});

        embeddedTypeRadioButton.addListener(SWT.Selection, new Listener () {
            public void handleEvent (Event e) {
                    result.setEmbedded(true);
                    dbportText.setEnabled(false);
                    directoryButton.setEnabled(true);
                    db2modeCheckbox.setEnabled(false);
                    String userDir = System.getProperty("user.dir") + File.separator + "databases";
                    result.setDblocation(userDir);
                    dblocText.setText(userDir);
                    updateSummary();
            }
        });
        
        postgresModeCheckbox.addListener(SWT.Selection, new Listener () {
            public void handleEvent (Event e) {
            	    if(postgresModeCheckbox.getSelection())
                    {
            	    	embeddedTypeRadioButton.setEnabled(false);
	                    netTypeRadioButton.setEnabled(false);
	                    db2modeCheckbox.setEnabled(false);
	                    JDBCInterface.getInstance().setPostgres_connected(true);
                    }
            	    else
            	    {
            	    	embeddedTypeRadioButton.setEnabled(true);
	                    netTypeRadioButton.setEnabled(true);
	                    db2modeCheckbox.setEnabled(true);
	                    JDBCInterface.getInstance().setPostgres_connected(false);
            	    }
                    updateSummary();
            }
        });

        netTypeRadioButton.addListener(SWT.Selection, new Listener () {
            public void handleEvent (Event e) {
                    result.setEmbedded(false);
                    dbportText.setEnabled(true);
                    directoryButton.setEnabled(false);
                    db2modeCheckbox.setEnabled(true);
                    result.setDblocation("localhost");
                    dblocText.setText("localhost");
                    updateSummary();
            }
        });
        
        
        
        nameText.addModifyListener(new ModifyListener(){
            public void modifyText(ModifyEvent e) {
                result.setDbname(nameText.getText());
                updateSummary();
            }
        });
        
		dblocText.addModifyListener(new ModifyListener(){
			public void modifyText(ModifyEvent e) {
				result.setDblocation(dblocText.getText());
				updateSummary();
			}
		});

        dbportText.addModifyListener(new ModifyListener(){
            public void modifyText(ModifyEvent e) {
                result.setDbport(dbportText.getText());
                updateSummary();
            }
        });
        
        dbLocale.addListener(SWT.Selection, new Listener(){
            public void handleEvent(Event event) {
                boolean selected = dbLocale.getSelection();
                if(selected){
                    dbLocaleCombobox.setEnabled(true);
                    int index = dbLocaleCombobox.getSelectionIndex();
                    result.setLocale(locales[index]);
                } else {
                    dbLocaleCombobox.setEnabled(false);
                    result.setLocale(null);
                }
                updateSummary();
            }});
        
        dbLocaleCombobox.addListener(SWT.Selection,new Listener(){
            public void handleEvent(Event event) {
                int index = dbLocaleCombobox.getSelectionIndex();
                result.setLocale(locales[index]);
                updateSummary();
            }});
        
        userText.addModifyListener(new ModifyListener(){
            public void modifyText(ModifyEvent e) {
                result.setUser(userText.getText());
                updateSummary();
            }
        });
        
        passwordText.addModifyListener(new ModifyListener(){
			public void modifyText(ModifyEvent e) {
				result.setPassword(passwordText.getText());
				updateSummary();
			}
		});
		

		
        db2modeCheckbox.addListener(SWT.Selection, new Listener () {
            public void handleEvent (Event e) {
                result.setDB2Mode(db2modeCheckbox.getSelection());
                updateSummary();
            }
        });
        
        
		// -------  Button Listeners -----------
		
		directoryButton.addListener(SWT.Selection, new Listener () {
			public void handleEvent (Event e) {
				DirectoryDialog dd = new DirectoryDialog(dialogShell);
				String loctext = dblocText.getText();
                String dbtext  = nameText.getText();
                if (loctext!=null){
                    if(dbtext !=null){
                        loctext += "/"+dbtext;
                    }
                    dd.setFilterPath(loctext);
                }
                dd.setMessage("Select DB Directory");
                String name = dd.open();
				if(name!=null){
					result.setDblocation(name);
					updateDisplayedConnectionInfo();
					updateSummary();
				}
			}
		});
		
		okButton.addListener(SWT.Selection, new Listener() {
			public void handleEvent(Event event) {
				keepResults = true;	
                dialogShell.dispose();
                    
			}}); 

		cancelButton.addListener(SWT.Selection, new Listener() {
			public void handleEvent(Event event) {
				// close dialog
				dialogShell.dispose();
				// the return object
				result = null;
			}}); 
		
		testButton.addListener(SWT.Selection, new Listener() {
			public void handleEvent(Event event) {
				MainStatusLine.message("Testing current connection...",false);
				// TODO modify testConnection method of JDBCInterface
				System.out.println(result.getConnectionString());
                System.out.println(result.getConnectionProperties());
                
                boolean test = JDBCInterface.getInstance().testConnection(result);
				if(test){
					statusLabel2.setText(states[1]);
					MainStatusLine.message("",false);
				} else{
					statusLabel2.setText(states[2]);
					MainStatusLine.message("Testing of current connection settings failed",true);
				}
			}}); 
		// add a dispose listener to get notified when the user pressed 
        // the close button on the corner of the dialog and to reset the 
        // the returned result
        dialogShell.addDisposeListener(new DisposeListener(){
            public void widgetDisposed(DisposeEvent e) {
                // the return object
                if(!keepResults){
                    result = null;
                }
            }});
		
	}
	
	// ------------------------- business logic -------------------------
	
	/** update the displayed connection infos */
	private void updateDisplayedConnectionInfo(){
		connectionText.setText(result.getAlias());
        nameText.setText(result.getDbname());
		dblocText.setText(result.getDblocation());
		dbportText.setText(result.getDbport());
        userText.setText(result.getUser());
		passwordText.setText(result.getPassword());
		createCheckbox.setSelection(result.isCreate());
		createTpchCheckbox.setSelection(result.isTpchImport());
		createTpchSamplesCheckbox.setSelection(result.isCreateTpchSamples());
        String loc = result.getLocale();
        if (loc!=null){
            dbLocale.setSelection(true);
            dbLocaleCombobox.setEnabled(true);
            int selectable=0;
            for(int i=0;i<this.locales.length;i++){
                if(loc.equalsIgnoreCase(locales[i])){
                    selectable = i;
                    break;
                }
            }
            dbLocaleCombobox.select(selectable);
        } else {
            dbLocale.setSelection(false);
            dbLocaleCombobox.setEnabled(false);
        }
	}
	
	/** updates the summary line & checks input to enable/disable okButton*/
	private void updateSummary(){
		
		SummaryLabel2.setText(result.getConnectionString());
		okButton.setEnabled(checkInput());

	}
	
	/** simple check method which checks if the input text widgets aren`t empty and
	 *  the connection name isn`t the default "New Connection Name" 
	 */ 
	private boolean checkInput(){
		if(!dblocText.getText().equals("") &&
		   !userText.getText().equals("")  &&
		   !passwordText.getText().equals("") &&
		   !connectionText.getText().equals("New Connection Name")){
			 return true;
		} else {
			 return false;
		}
	}
	
	/** opens the dialog
	 * 
	 * @param ci either provide a ConnectionInfo object or null
	 * @param title the dialog title (dialog will be reused for edit 
	 * @return null if user canceled, or the new ConnectionInfo object
	 */
	public Object open(ConnectionInfo ci, String title, boolean testable) {
		
		// create new ConnectionInfo, if null was specified
		if(ci==null){
			result = new ConnectionInfo();
		} else {
			result = ci;
		}
		
		// save testable state
		this.testable = testable;
		
		Shell parent = getParent();
		dialogShell = new Shell(parent, SWT.DIALOG_TRIM | SWT.APPLICATION_MODAL | SWT.RESIZE);

		initComponents();
		initListeners();
		dialogShell.setText(title);
		updateSummary();
		
		dialogShell.open();
		Display display = parent.getDisplay();
		while (!dialogShell.isDisposed()) {
			if (!display.readAndDispatch()) display.sleep();
		}
			
		return result;
	}
	
}
