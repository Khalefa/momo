package components.widgets;

import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.text.Collator;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Locale;
import java.util.Map;

import modules.databaseif.ResultSetModel;
import modules.misc.Constants;
import modules.misc.ResourceRegistry;
import modules.misc.StringUtils;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;

import data.type.Tuple;

/**
 * Generic, reusable table widget, which uses either 
 * <li>a ResultSetModel object as data basis or a 
 * <li>java.sql.Resultset object
 * <li>or a simple map containing of key/value pairs.<br>
 * The table is embedded in a SrcolledComposite and tries to 
 * fill up the whole space, it can gather.
 * 
 * @author Ulrike Fischer, Felix Beyer
 */
public class TableWidget extends Composite {
	
	private Table table; // the table
    private boolean sort;
    
    private int cellpadding = 1; // the padding of each cell

	/**
	 * Creates a new table widget object
	 */
	public TableWidget(Composite parent, int style, boolean sort) {
		super(parent, style);
		this.sort = sort;
		
        setLayout(new GridLayout(1, true));
	}

	/**
	 * Creates a new table widget object
	 */
	public TableWidget(Composite parent, int style) {
		this(parent, style, true);
	}

    /** sort the TableItems */
    private static void sort(TableItem[] items, final int index, final boolean up, final Collator collator){
        Arrays.sort(items, new Comparator<TableItem>() {
        	public int compare(TableItem t1, TableItem t2) {
        		int c = collator.compare(t1.getText(index), t2.getText(index));
        		if (up)
        			return c;
        		else
        			return -c;
        	}
        });
    }

    /** create a sort listeners which sorts the tableitems when a TableColumn is clicked */
    private void createSortListener(){
        Listener sortListener = new Listener() {
            private int sortColumnIndex = -2; // the current sort column index
            public void handleEvent(Event e) {
                TableItem[] items = table.getItems();
                Collator collator = Collator.getInstance(Locale.getDefault());
                TableColumn column = (TableColumn)e.widget;
                TableColumn[] columns = table.getColumns();

                // set the sort column 
                table.setSortColumn(column);

                // determine column index
                int index = -1;
                int sortDir;
                for (int l=0;l<columns.length;l++){
                    if(column.equals(columns[l])){
                        index = l;
                    }
                }
                // already a sort column ? -> then reverse column order
                if (index == sortColumnIndex){
                    sortDir = table.getSortDirection();
                    if (sortDir == SWT.UP){
                        table.setSortDirection(SWT.DOWN);
                    } else {
                        table.setSortDirection(SWT.UP);
                    }
                } else {
                    // save new sortcolumn index
                    sortColumnIndex = index;
                    table.setSortDirection(SWT.UP);
                }
                sortDir = table.getSortDirection();
                
                // call sort method
                sort(items, index, sortDir == SWT.UP, collator);

                // add the ordered array to the table as new tableitems
                //  copy values from old item into new item
                // table.removeAll();
                int l = items.length;
                int cc = table.getColumnCount();
                String[] values;
                for (int i = 0; i < l; i++) {
                    values = new String[cc];
                    for (int k=0;k<cc;k++){
                        values[k] = items[i].getText(k);
                    }
                    
                    TableItem item = new TableItem(table, SWT.NONE);
                    item.setText(values);
                    //items[i].dispose();
                }
                // remove the first l Tableitems, so that the table only contains the new added TableItems
                for (int i = 0; i < l; i++) {
                    table.remove(0);
                    //table.getItem(i).dispose();
                }
            }
        };
        
        // add to each column the sort listener
        for (TableColumn column : table.getColumns()){
            column.addListener(SWT.Selection, sortListener);
        }
    }
    
    private void createPaintListenerForTable(Table t){
        // insert own paint listener
        Listener paintListener = new Listener() {
            public void handleEvent(Event event) {
                switch(event.type) {        
                    case SWT.MeasureItem: {
                        TableItem item = (TableItem)event.item;
                        String text = getText(item, event.index);
                        Point size = event.gc.textExtent(text);
                        event.width = size.x + 2 * cellpadding;
                        //event.height = Math.max(event.height, size.y);
                        event.height = size.y + + 2 * cellpadding;
                        break;
                    }
                    case SWT.PaintItem: {
                        TableItem item = (TableItem)event.item;
                        String text = getText(item, event.index);
                        Point size = event.gc.textExtent(text);                 
                        int offset2 = Math.max(0, (event.height - size.y) / 2);
                        event.gc.drawText(text, event.x, event.y + offset2, true);
                        break;
                    }
                    case SWT.EraseItem: {   
                        event.detail &= ~SWT.FOREGROUND;
                        break;
                    }
                }
            }

            String getText(TableItem item, int column) {
                String text = item.getText(column);
                return text;
            }
        };
        
        t.addListener(SWT.MeasureItem, paintListener);
        t.addListener(SWT.PaintItem, paintListener);
        t.addListener(SWT.EraseItem, paintListener);
    }

    /** creates the common table information which all table creation 
     * methods must call at first */
    private void createCommonTableInformation(){
        // if the table is renewed, then first dispose the old one
        if(table!=null) table.dispose();

        // create a new table
        table = new Table (this, SWT.BORDER | 
                                     SWT.V_SCROLL | 
                                     SWT.H_SCROLL |
                                     SWT.MULTI |
                                     SWT.FULL_SELECTION);
           
        // make lines and headers visible
        table.setLinesVisible (true);
        table.setHeaderVisible (true);          

        // add the gridlayout data object
        
        GridData tableData = new GridData();
        tableData.grabExcessHorizontalSpace = true;
        tableData.horizontalAlignment = GridData.FILL_HORIZONTAL;
        tableData.grabExcessVerticalSpace = true;
        tableData.verticalAlignment = GridData.FILL_VERTICAL;
        table.setLayoutData(tableData);
    }
    
    /** creates a table from a map object<br> 
     * <li>keys go into column 1, 
     * <li>values go into column 2 
     * @param keyTitle is the title of the key column
     * @param valueTitle is the title of the value column
     * */
    public void createTable(Map props, String keyTitle, String valueTitle){
    	createCommonTableInformation();

    	// a table from a map contains only 2 columns (key, value)

    	// create key and value columns
    	TableColumn column = new TableColumn (table, SWT.LEFT);
    	column.setText (keyTitle);

    	column = new TableColumn (table, SWT.LEFT);
    	column.setText (valueTitle);

    	// create items
    	boolean odd = true;
    	if (props!=null){
    		String valueValue;

    		// iterate over the keys and create the table body from them
    		for(Object o : props.entrySet()){
    			String keyValue   = ((Map.Entry)o).getKey().toString();
    			Object value      = ((Map.Entry)o).getValue();
    			if(value!=null){
    				valueValue = StringUtils.replaceDelChars(value.toString());
    			} else {
    				valueValue = "Not available";
    			}
    			// create a table item
    			TableItem item = new TableItem(table,SWT.NONE);
    			if(odd){
    				item.setBackground(ResourceRegistry.getInstance().
    						getColor(table.getDisplay(),Constants.tableOddBGColor));
    			}
    			odd = !odd;
    			item.setText(0,keyValue);
    			item.setText(1,valueValue);
    		}

    		createPaintListenerForTable(table);
        	if (sort)
        		createSortListener();
    	}

    	// set columns to minmum size
    	table.getColumn(0).pack ();
    	table.getColumn(1).pack ();
    	// recalculate the size of the table
    	setSizeNew();
    }
    
	/**
	 * Creates a table from a jdbc result set.
     * @param rs the ResultSet which provides the content to create the table. 
	 */
	public void createTable(ResultSet rs) {
        createCommonTableInformation();

		try {
			ResultSetMetaData rsmd = rs.getMetaData();
			int noColumns = rsmd.getColumnCount();
			
			// create columns
			for (int i=1; i<=noColumns; i++) {
				TableColumn column = new TableColumn (table, SWT.CENTER);
				column.setText (rsmd.getColumnName(i));
			}
		
			// create items
			boolean odd = true;
			while (rs.next()) {
				TableItem item = new TableItem(table,SWT.NONE);
				if(odd){
					item.setBackground(ResourceRegistry.getInstance().
							getColor(table.getDisplay(),Constants.tableOddBGColor));
				}
				odd = !odd;
				for (int j=0; j<noColumns; j++) {
					String v = rs.getString(j+1);
					if (v != null) {
						item.setText(j,v);
                        //item.setText(j,StringUtils.replaceDelChars(v));
					} else {
						item.setText(j,Constants.PRINT_NULL);
					}
				}
			}
			rs.close();
			createPaintListenerForTable(table);			
        	if (sort)
        		createSortListener();
			
			// set columns to minmum size
			for (int i=0; i < noColumns; i++) {
				table.getColumn(i).pack ();
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}	
		setSizeNew();
	}
	
	/**
	 * Creates the table from a ResultSetModel.
     * @param rsm the ResultSetModel, from which the table gets its contents
	 */
	public void createTable(ResultSetModel rsm) {
            createCommonTableInformation();
			
            int noColumns = rsm.getColumnCount();
			
			// create columns
			for (int i=0; i<noColumns; i++) {
				TableColumn column = new TableColumn (table, SWT.CENTER);
				column.setText (rsm.getColumnName(i));
			}
			
			// create items
			boolean odd = true;
			for (Tuple t : rsm.getTuples()) {
				TableItem item = new TableItem(table,SWT.NONE);
				if(odd){
					item.setBackground(ResourceRegistry.getInstance().
							getColor(table.getDisplay(),Constants.tableOddBGColor));
				}
				odd = !odd;
				for (int j=0; j<noColumns; j++) {
					String v = t.getValue(j).getString();
					if (v != null) {
						item.setText(j,v);
					} else {
						item.setText(j,Constants.PRINT_NULL);
					}
				}
			}
			
            createPaintListenerForTable(table);         
        	if (sort)
        		createSortListener();
            
			// set columns to minmum size
			for (int i=0; i < noColumns; i++) {
				table.getColumn(i).pack();				
			}
		setSizeNew();
		//table.pack();
	}
	
	/**
	 * Disposes the table and explicitely sets the table to<b>null</b>!
	 */
	public void removeTable() {
		if (table != null) {
			table.dispose();
			table = null;
		}
	}
	
	/**
	 * Sets the size of the table to the minimum of its own size 
     * and the one of its composite container.
	 */
	public void setSizeNew() {
		if (table != null) {
			Rectangle area = getClientArea();
			Point p = table.computeSize(SWT.DEFAULT,SWT.DEFAULT);
			table.setSize(Math.min(area.width,p.x), Math.min(area.height,p.y));			
		}
	}
	
	/**
	 * Sets the size of the table to match the one of its composite container.
	 */
	public void setSize2() {
		if (table != null) {
			Rectangle area = this.getClientArea();
			table.setSize(area.width, area.height);			
		}
	}
	
    /** Computes the default table size,
     *  using the method computeSize(SWT.Default,SWT.Default) method of the table*/ 
	public Point computeTableSize() {
		return table.computeSize(SWT.DEFAULT,SWT.DEFAULT);
	}
}
