package components.tabs;

import java.text.SimpleDateFormat;
import java.util.Date;

import modules.databaseif.History;
import modules.databaseif.HistoryEntry;
import modules.generic.DemoEvents;
import modules.generic.GenericModelChangeEvent;
import modules.generic.GenericModelChangeListener;
import modules.misc.Constants;
import modules.misc.ResourceRegistry;
import modules.misc.StringUtils;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseListener;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;

import components.AbstractComponent;
import components.dialogs.ExceptionDialog;
import components.dialogs.ResultSetDialog;

public class HistoryTab extends AbstractComponent 
 						implements GenericModelChangeListener {

	// -- constants -------------------------------------------------------------------------------
	
	// -- variables -------------------------------------------------------------------------------

	/** the history */
	private Table tHistory;
	
	private final SimpleDateFormat df;
	
	// -- constructors ----------------------------------------------------------------------------
	
	public HistoryTab(Composite parent, int style) {
		super(parent, style);

		// date formatting
		df = new SimpleDateFormat(Constants.tree_date_format);
	}
	
	// -- gui stuff -------------------------------------------------------------------------------

	/** Initialize the components of this tab */
	protected void initComponents() {
		setLayout(new GridLayout());
		
		// MAIN COMPOSITE
		Composite cMain = new Composite(this, SWT.NONE);
		cMain.setLayout(new GridLayout(2, false));
		cMain.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		
		// create table
		tHistory = new Table (cMain, SWT.BORDER | 
                                     SWT.V_SCROLL | 
                                     SWT.H_SCROLL | 
                                     SWT.MULTI | 
                                     SWT.FULL_SELECTION);
		tHistory.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		tHistory.setLinesVisible(true);
		tHistory.setHeaderVisible(true);
		
		// create columns
		for (int i=0; i<Constants.HISTORYTAB_COLUMNS.length; i++) {
			new TableColumn (tHistory, SWT.NONE).setText(Constants.HISTORYTAB_COLUMNS[i]);
		}
	}

	// -- listeners -------------------------------------------------------------------------------
	
	/** Initialize the listeners of this tab */
	protected void initListeners() {
		// add the history Tab as listener for the history
		History.getInstance().addModelChangeListener(this);

		tHistory.addMouseListener(new MouseListener() {
		     public void mouseDoubleClick(MouseEvent e) {
		    	    Point pt = new Point(e.x, e.y);
					TableItem item = tHistory.getItem(pt);
					if(item==null)return;
					//System.out.println(item);
					int index = Integer.parseInt(item.getText(0))-1;
		    	   //int index = tHistory.getSelectionIndex();
		        if (index == -1) return;
		        HistoryEntry entry = History.getInstance().getEntry(index);		        
			        
		        if (!entry.executedSuccessfully()) {
		        	ExceptionDialog.show(Display.getCurrent().getActiveShell(), 
						entry.getException(), false);
		        } else if (entry.getQueryResult() != null) {
		        	new ResultSetDialog(Display.getCurrent().getActiveShell(), 
						entry.getQueryResult());
		        }
		     }
			     
		     public void mouseDown(MouseEvent e) { 
		     }
			     
		     public void mouseUp(MouseEvent e) { 
		     }
		});
	}
	
	/** Events from the GUI */
	public void modelChanged(GenericModelChangeEvent event) {
		switch (event.detail){
			// gets called when the history gets loaded
			case DemoEvents.LOAD_HISTORY: 
				updateHistory(); 
				break;
		}

	}
	
	// -- business logic -------------------------------------------------------------------------

	/** Updates the history table */
	private void updateHistory() {
		// clear table
		tHistory.removeAll();
		
		// add history
		History history = History.getInstance();
		for (int i=0; i<history.size(); i++) {
			addEntry(i, history.getEntry(i));
		}
		
		// pack columns
		for (int i=0; i < tHistory.getColumnCount(); i++) {
			tHistory.getColumn(i).pack();
		}
	}
	
	/** Adds a history entry to the history table */
	private void addEntry(final int index, HistoryEntry entry) {
		TableItem item = new TableItem(tHistory, SWT.NONE);
		if(index % 2 == 1) {
			item.setBackground(ResourceRegistry.getInstance().getColor(
					tHistory.getDisplay(), Constants.tableOddBGColor));
		}
		int i = 0;		
		item.setText(i++, Integer.toString(index+1));
		item.setText(i++, entry.getDbName());
		item.setText(i++, df.format(new Date(entry.getCreationTime())));
		if (entry.executedSuccessfully()) {
			if (entry.isInterrupted()) {
				item.setText(i++, Constants.HISTORYTAB_INTERRUPTED);
			} else {
				item.setText(i++, Constants.HISTORYTAB_SUCCESSFUL);
			}			
			item.setText(i++, Integer.toString(entry.getAffectedRows()));
			item.setText(i++, entry.formatExecutionTime());
			item.setText(i++, entry.hasCompilePlan() ? Constants.YES : Constants.NO);
            item.setText(i++, entry.hasExecutionPlan() ? Constants.YES : Constants.NO);
        } else {
			item.setText(i++, Constants.HISTORYTAB_FAILED);
			item.setText(i++, "-");
			item.setText(i++, "-");
			item.setText(i++, "-");
            item.setText(i++, "-");
		}
		item.setText(i++, StringUtils.replaceDelChars(entry.getSQL()));
	}


	protected void activateControls() {
		// nothing to activate
	}

	protected void deactivateControls() {
		// nothing to deactivate
	}

	protected void updateComponent() {
		updateHistory();
	}


	public void reset() {
		// nothing to reset
	}	
}
