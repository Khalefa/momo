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
public class TraceTab extends AbstractComponent
							  implements GenericModelChangeListener {


	/** creates a new system catalog tab */
	public TraceTab(Composite parent, int style) {
		super(parent, style);
	}

	/** inits the GUI components */
	protected void initComponents(){
		
	}

	/** inits the listeners */
	protected void initListeners() {
		
	}

	/**
     * this method executes a statement to request the contents of the
     * current selected system table.
     *
	 */
    

	/** this method gets called when a connection was closed */
	protected void deactivateControls() {
		if(!this.isDisposed()){

		}
	}

	/** this method gets called when a connection was established */
	protected void activateControls() {
		
	}

	/** this method gets called when the user pressed the tab title */
	protected void updateComponent() {

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
