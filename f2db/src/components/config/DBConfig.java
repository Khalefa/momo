package components.config;

import modules.config.Configuration;
import modules.databaseif.ConnectionInfo;
import modules.databaseif.JDBCInterface;
import modules.generic.DemoEvents;
import modules.generic.GenericModelChangeEvent;
import modules.generic.GenericModelChangeListener;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;

import components.dialogs.AddEditConnectionDialog;
/**
 *  Database Connection Settings contents for Configuration Tab.
 *  GUI for Add/Edit/Delete database connections
 * @author Felix Beyer
 * @date   09.10.2006
 */
public class DBConfig extends Composite
					implements GenericModelChangeListener{

	private Composite parent;
	
    // displayed info about connection
    private Combo connectionComboBox;
    private Label aliasConnectionLabel;
	private Label dbLocationLabel;
	private Label passwordText;
	private Label passwordLabel;
	private Label userNameText;
	private Label usernameLabel;
	private Label dbLocationText;
	
//  private Label fillerLabel;
    
    // the buttons
    private Button addConnectionButton;
    private Button editButton;
    private Button deleteButton;
	
    private int currentConIndex=-1; // holds the current index of the combo box
	private boolean testenabled = true; // flag which indicates if a connection could be tested
	
	/** constructor */
	public DBConfig(Composite parent, int style) {
		super(parent, style);
		this.parent = parent;
		initComponents();
		initListeners();
		updateComboBox();
		// display first connection, if at least one was found in the connection properties
		if(currentConIndex>=0){
			updateDisplayedConnection();
		}
	}

	private void initComponents(){
		try {
			setLayout(new GridLayout(3, false));
			setSize(306, 146);
			{
				aliasConnectionLabel = new Label(this, SWT.NONE);
				aliasConnectionLabel.setText("Connection alias:");
			}
			{
				GridData connectionComboBoxLData = new GridData();
				connectionComboBoxLData.horizontalAlignment = GridData.FILL;
				connectionComboBoxLData.grabExcessHorizontalSpace = true;
				connectionComboBox = new Combo(this, SWT.READ_ONLY);
				connectionComboBox.setLayoutData(connectionComboBoxLData);
			}
			{
//				fillerLabel = 
					new Label(this, SWT.NONE);
			}
			{
				dbLocationLabel = new Label(this, SWT.NONE);
				dbLocationLabel.setText("DB Location :");
			}
			{
				GridData dbnameTextLData = new GridData();
				dbnameTextLData.horizontalAlignment = GridData.FILL;
				dbnameTextLData.grabExcessHorizontalSpace = true;
				dbLocationText = new Label(this, SWT.NONE);
				dbLocationText.setLayoutData(dbnameTextLData);
			}
			{
				addConnectionButton = new Button(this, SWT.PUSH | SWT.CENTER);
				GridData addConnectionButtonLData = new GridData();
				addConnectionButtonLData.horizontalAlignment = GridData.FILL;
				addConnectionButton.setLayoutData(addConnectionButtonLData);
				addConnectionButton.setText("add connection");
			}
			{
				usernameLabel = new Label(this, SWT.NONE);
				usernameLabel.setText("User Name :");
			}
			{
				GridData userNameTextLData = new GridData();
				userNameTextLData.horizontalAlignment = GridData.FILL;
				userNameTextLData.grabExcessHorizontalSpace = true;
				userNameText = new Label(this, SWT.NONE);
				userNameText.setLayoutData(userNameTextLData);
			}
			{
				editButton = new Button(this, SWT.PUSH | SWT.CENTER);
				GridData editButtonLData = new GridData();
				editButtonLData.horizontalAlignment = GridData.FILL;
				editButton.setLayoutData(editButtonLData);
				editButton.setText("edit connection");
			}
			{
				passwordLabel = new Label(this, SWT.NONE);
				passwordLabel.setText("User Password :");
			}
			{
				GridData passwordTextLData = new GridData();
				passwordTextLData.horizontalAlignment = GridData.FILL;
				passwordTextLData.grabExcessHorizontalSpace = true;
				passwordText = new Label(this, SWT.NONE);
				passwordText.setLayoutData(passwordTextLData);
			}
			{
				deleteButton = new Button(this, SWT.PUSH | SWT.CENTER);
				GridData deleteButtonLData = new GridData();
				deleteButtonLData.horizontalAlignment = GridData.FILL;
				deleteButton.setLayoutData(deleteButtonLData);
				deleteButton.setText("delete connection");
			}
			this.layout();
		} catch (Exception e) {
			e.printStackTrace();
		}

	}

	private void initListeners(){
		connectionComboBox.addSelectionListener(
				new SelectionAdapter(){
					public void widgetSelected(SelectionEvent e){
						int index = connectionComboBox.getSelectionIndex();
						if((index!=-1) && (index!=currentConIndex)){
							currentConIndex = index;
							updateDisplayedConnection();
						}
					}
				}
		);

		addConnectionButton.addListener(SWT.Selection, new Listener () {
			public void handleEvent (Event e) {
				AddEditConnectionDialog dialog = 
					new AddEditConnectionDialog(parent.getShell(),SWT.NONE);
				Object result = dialog.open(
						null,
						"Add Connection",
						testenabled);


				// check the returned result from the dialog
				if(result!=null){
					// must be a valid connection information
					// add it
					Configuration.getInstance().addConnectionInfo((ConnectionInfo)result);	
					updateComboBox();
					updateDisplayedConnection();
				}

				//notify connect button listener that connections have changed
				GenericModelChangeEvent event = new GenericModelChangeEvent();
				event.detail = DemoEvents.CONFIG_CONNECTION_CHANGED;
				event.source = this.toString();
				Configuration.getInstance().fireModelChangeEvent(event);
			}
		});

		editButton.addListener(SWT.Selection, new Listener () {
			public void handleEvent (Event e) {
				
				if (currentConIndex!=-1){
					AddEditConnectionDialog dialog = 
						new AddEditConnectionDialog(parent.getShell(),SWT.NONE);
					Object result = dialog.open(
							Configuration.getInstance().
							getConnectionInfo(currentConIndex),
							"Edit Connection",
							testenabled);
					
					// check to see if something has changed
					if(result!=null){
						updateComboBox();
                        updateDisplayedConnection();
                        Configuration.getInstance().saveConnections();
                        //notify listeners that something has changed
					    GenericModelChangeEvent event = new GenericModelChangeEvent();
						event.detail = DemoEvents.CONFIG_CONNECTION_CHANGED;
						event.source = this.toString();
						Configuration.getInstance().fireModelChangeEvent(event);
					}
				}
			}
		});
		
		deleteButton.addListener(SWT.Selection, new Listener () {
			public void handleEvent (Event e) {
				
				if (currentConIndex!=-1){
					ConnectionInfo ci = 
					Configuration.getInstance().getConnectionInfo(currentConIndex);
					Configuration.getInstance().delConnectionInfo(ci);
					updateComboBox();
                    updateDisplayedConnection();
					//notify connect button listener that connections have changed
				    GenericModelChangeEvent event = new GenericModelChangeEvent();
					event.detail = DemoEvents.CONFIG_CONNECTION_CHANGED;
					event.source = this.toString();
					Configuration.getInstance().fireModelChangeEvent(event);
				}
				
			}
		});
		
	}
	
	// --------------------------- business logic ------------------------
	
	/** update the items of the connection combo box */
	private void updateComboBox(){
		connectionComboBox.removeAll();
		ConnectionInfo[] cons = Configuration.getInstance().getConnectionInfos();
		for(int i=0;i<cons.length;i++){
			connectionComboBox.add(cons[i].getAlias());
		}
		
		if(cons.length>0){ 
			connectionComboBox.setText(cons[0].getAlias());
		    currentConIndex = 0;
		} else {
			currentConIndex = -1;
		}
		
	}
	
	/** update the widgets with information */
	private void updateDisplayedConnection(){
	    if(currentConIndex>-1){
	        ConnectionInfo ci = Configuration.getInstance().getConnectionInfo(currentConIndex);
	        if (ci.isEmbedded()) {
	            dbLocationText.setText(ci.getDblocation()+"/"+ci.getDbname());
	            userNameText.setText(ci.getUser());
	            passwordText.setText(ci.getPassword());
	            passwordText.setText("<hidden>");
            } else {
                dbLocationText.setText(ci.getDblocation()+":"+ci.getDbport()+"/"+ci.getDbname());
                userNameText.setText(ci.getUser());
                passwordText.setText("<hidden>");
            }
        } else {
            dbLocationText.setText("");
            userNameText.setText("");
            passwordText.setText("");
            
        }
	}
	
	// ---------------------- GenericModelChangeLister implementation -----
	
	public void modelChanged(GenericModelChangeEvent event) {
		switch(event.detail){
			case DemoEvents.CONNECTION_ESTABLISHED: testenabled = false; break;
			case DemoEvents.CONNECTION_CLOSED:      testenabled = true ; break;
		}
	}
}
