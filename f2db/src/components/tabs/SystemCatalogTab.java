package components.tabs;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.LinkedList;
import java.util.List;

import modules.databaseif.ConstantStatements;
import modules.databaseif.JDBCInterface;
import modules.generic.DemoEvents;
import modules.generic.GenericModelChangeEvent;
import modules.generic.GenericModelChangeListener;
import modules.misc.Constants;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Listener;

import components.AbstractComponent;
import components.widgets.TableWidget;

/**
 * This is the tab implementation of the system catalog tab.
 *
 * @author Ulrike Fischer, Felix Beyer, Sebastian Seifert,Christopher Schildt
 * @version 0.2 update to reflect new component class hierarchy
 */
public class SystemCatalogTab extends AbstractComponent
							  implements GenericModelChangeListener {

	private String[] catalogNames; // = {
/*			"XPLAIN_STATEMENTS",
            "XPLAIN_RESULTSETS",
            "XPLAIN_SCAN_PROPS",
            "XPLAIN_SORT_PROPS",
            "XPLAIN_STATEMENT_TIMINGS",
            "XPLAIN_RESULTSET_TIMINGS",
            "LOGICALSAMPLES",
			"PHYSICALSAMPLES",
			"PHYSICALSAMPLEPROPERTIES",
			"SAMPLEDEPENDENCIES",
			"SYSALIASES",
			"SYSCHECKS",
			"SYSCOLUMNS",
			"SYSCONGLOMERATES",
			"SYSCONSTRAINTS",
			"SYSDEPENDS",
			"SYSFILES",
			"SYSFOREIGNKEYS",
			"SYSKEYS",
			"SYSSCHEMAS",
			"SYSSTATISTICS",
			"SYSSTATEMENTS",
			"SYSTABLES",
			"SYSTRIGGERS",
			"SYSVIEWS",
            "SYSTABLEPERMS",
            "SYSCOLPERMS",
            "SYSROUTINEPERMS",
            "SYSWORKLOAD"};	*/

	private Combo comboBox; // the comboBox
	private TableWidget table; // the table

	// is true when a table is visible
	private boolean isVisible = false;

	/** creates a new system catalog tab */
	public SystemCatalogTab(Composite parent, int style) {
		super(parent, style);
	}

	/** inits the GUI components */
	protected void initComponents(){
		setLayout(new GridLayout());

		Group g1 = new Group(this,SWT.NONE);
		g1.setLayout(new GridLayout());
		g1.setLayoutData(new GridData(SWT.FILL, SWT.NONE, true, false));
		g1.setText(Constants.system_select);

		// create the combo box
		comboBox = new Combo(g1,SWT.READ_ONLY);

		//fill the combobox;
		catalogNames = new String[]{};

		comboBox.add("                             ");

		comboBox.setSize(120, comboBox.getSize().y);
		comboBox.setEnabled(false);
		comboBox.setVisibleItemCount(25);
		comboBox.pack();

		table = new TableWidget(this,SWT.NONE);
		table.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
	}

	/** inits the listeners */
	protected void initListeners() {
		//add the system catalog tab as listener to the JDBCInterface(Model)
		JDBCInterface.getInstance().addModelChangeListener(this);

		comboBox.addListener(SWT.Selection, new Listener() {
			public void handleEvent(Event event) {
			    fillTable();
            }
		});

		table.addListener(SWT.Resize, new Listener() {
			public void handleEvent(Event event) {
				if (isVisible)
					table.setSizeNew();
			}
		});
	}

	/**
     * this method executes a statement to request the contents of the
     * current selected system table.
     *
	 */
    private void fillTable(){
        isVisible = true;
        int result;
        int index = comboBox.getSelectionIndex();
        // check to see if we have an index greater than -1 (something selected)
        if(index>-1){
//        	JDBCInterface.getInstance().setWorkloadRecording(false);
        	if(!JDBCInterface.getInstance().isPostgres_connected())
        		result = JDBCInterface.getInstance().executeStatement(
        				ConstantStatements.STMT_SELECTSYSTEMCATALOGDERBY + catalogNames[index],
        				0, false, false, false, true);
        	else
        		result = JDBCInterface.getInstance().executeStatement(
        				ConstantStatements.STMT_SELECTSYSTEMCATALOGPOSTGRES + catalogNames[index],
        				0, false, false, false, true);
            if(result == 1) table.createTable(JDBCInterface.getInstance().getResultSet());
//            JDBCInterface.getInstance().setWorkloadRecording(true);
        }

    }

	/** this method gets called when a connection was closed */
	protected void deactivateControls() {
		if(!this.isDisposed()){
			comboBox.deselectAll();
			comboBox.setEnabled(false);
			table.removeTable();
			isVisible = false;
		}
	}

	/** this method gets called when a connection was established */
	protected void activateControls() {
		ResultSet result;
		if(!this.isDisposed()){
			if(JDBCInterface.getInstance().getConnectionStatus() == JDBCInterface.CONNECTED){
				if(!JDBCInterface.getInstance().isPostgres_connected())
					result = JDBCInterface.getInstance().executeStatementWithResult(ConstantStatements.STMT_ALLSYSTEMCATALOGSDERBY);
				else
					result = JDBCInterface.getInstance().executeStatementWithResult(ConstantStatements.STMT_ALLSYSTEMCATALOGSPG);
		        if(result != null){
		        	try {
		        		List<String> resultList = new LinkedList<String>();
						while(result.next()){
							if(!result.getString(1).equalsIgnoreCase("SYSDUMMY1")){
								resultList.add(result.getString(1));
							}
						}
						catalogNames = resultList.toArray(new String[resultList.size()]);
						result.close();
					} catch (SQLException e) {
					}
		        } else {
		        	catalogNames = new String[]{};
		        }
			}
			comboBox.removeAll();
			comboBox.setSize(120, comboBox.getSize().y);

			for (int i=0; i<catalogNames.length; i++) {
				comboBox.add(catalogNames[i]);
			}
			comboBox.pack();
			comboBox.setEnabled(true);
			comboBox.setVisibleItemCount(25);
		}
	}

	/** this method gets called when the user pressed the tab title */
	protected void updateComponent() {
		if (comboBox.isEnabled()) {
		    fillTable();
        }
	}

	public void reset() {
		// do nothing
	}

	// -------- GenericModelChangeListener implementation -------

	/** gets called when a model has changed */
	public void modelChanged(GenericModelChangeEvent event) {
		switch (event.detail){
		  case DemoEvents.CONNECTION_ESTABLISHED : activateControls();   break;
		  case DemoEvents.CONNECTION_CLOSED	     : deactivateControls(); break;
		default: break;
		}
	}

}
