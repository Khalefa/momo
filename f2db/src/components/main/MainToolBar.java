package components.main;

import java.io.FileNotFoundException;
import java.io.IOException;

import modules.config.Configuration;
import modules.databaseif.ConnectionInfo;
import modules.databaseif.History;
import modules.databaseif.JDBCInterface;
import modules.generic.DemoEvents;
import modules.generic.GenericModelChangeEvent;
import modules.generic.GenericModelChangeListener;
import modules.misc.Constants;
import modules.misc.ModuleRegistry;
import modules.misc.ResourceRegistry;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ControlAdapter;
import org.eclipse.swt.events.ControlEvent;
import org.eclipse.swt.events.ControlListener;
import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swt.events.PaintListener;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.swt.widgets.MessageBox;
import org.eclipse.swt.widgets.ToolBar;
import org.eclipse.swt.widgets.ToolItem;

import components.widgets.MenuWidget;
/**
 * This class provides the main toolbar, which is located at the top area of 
 * the application. it provides functions which should operate at all application
 * states.
 * @author Felix Beyer, Sebastian Seifert
 * @date   08.05.2006
 *
 */
public class MainToolBar extends Composite 
						 implements GenericModelChangeListener {

	private Composite parent; // the parent composite
	private String[] toolButtonNames = Constants.main_toolButtonNames; // the tool names
	
	private ToolBar  mainTools; // the main toolbar widget
	private ToolBar  toolItemExtensionToolBar;
	private Label 	 logo;
	
	// the different tool items
//	private ToolItem connectTI;     // the connect toolitem 
//	private ToolItem disconnectTI;  // the disconnect toolitem
//	private ToolItem reconnectTI;   // the reconnect toolitem
//	private ToolItem treeExtractTI; // the tree extraction toggle button
//    private ToolItem xplainModeTI;  // the xplain mode toolitem
//    private ToolItem xplainTimingTI;// the xplain timings toolitem
//    private ToolItem workloadTI;// the xplain timings toolitem
//	private ToolItem loadHistoryTI; // the load history toolitem
//	private ToolItem saveHistoryTI; // the save history toolitem
//	private ToolItem resetTI;       // the reset toolitem
//	private ToolItem exitTI;        // the exit toolitem

	private MenuWidget connectMW;     // the connect toolitem 
	private MenuWidget disconnectMW;  // the disconnect toolitem
	private MenuWidget reconnectMW;   // the reconnect toolitem
//	private MenuWidget treeExtractMW; // the tree extraction toggle button
//    private MenuWidget xplainModeMW;  // the xplain mode toolitem
//    private MenuWidget xplainTimingMW;// the xplain timings toolitem
//    private MenuWidget workloadMW;    // the xplain timings toolitem
	private MenuWidget loadHistoryMW; // the load history toolitem
	private MenuWidget saveHistoryMW; // the save history toolitem
	private MenuWidget resetMW;       // the reset toolitem
	private MenuWidget exitMW;        // the exit toolitem

    // the menu which appears when the user clicks the connect button
	private Menu connectMenuForToolBar; //the connect menu
	private MenuItem[] connectMenuItemsForToolBar; // the connect menu items
//	private Menu connectMenuForDropDownMenu; //the connect menu
//	private MenuItem[] connectMenuItemsForDropDownMenu; // the connect menu items

    private Listener connectListener; // the connect menu listener
    
//    // the menu which appears when the user clicks the drop down button of the xplain mode
//    private Menu xplainModeMenu; // the xplain mode menu
//    private MenuItem[] xplainModeMenuItems;
//    
//    private Listener xplainModeListener; // the xplain mode listener
    
//    private Button allToolItemsButton;
    private Menu toolItemExtensionMenu;
    private Menu extensionMenu;
    private ToolItem toolItemExtension;
    
//	private boolean oldExplainMode; // saves the old explain mode
    
	/**
	 * the constructor of the maintoolbar
	 * @param parent the parent widget
	 * @param style the SWT Style
	 */
	public MainToolBar(Composite parent, int style) {
		super(parent, style);
		this.parent = parent;
		initComponents();
		initListeners();
	}
	
    private void initMenus(){
    	createConnectMenuItems();
    	
//        // create the different connection menuitems and the connect menu
//        // from the connection info objects
//        connectMenuForToolBar = new Menu (getShell(), SWT.POP_UP);
//        //connectMenuForDropDownMenu = new Menu (getShell(), SWT.DROP_DOWN);
//        ConnectionInfo[] cis = Configuration.getInstance().getConnectionInfos();
//        connectMenuItemsForToolBar = new MenuItem[cis.length];
//        //connectMenuItemsForDropDownMenu = new MenuItem[cis.length];
//        for (int i=0; i<cis.length; i++) {
//            connectMenuItemsForToolBar[i] = new MenuItem (connectMenuForToolBar, SWT.PUSH);
//            connectMenuItemsForToolBar[i].setText (cis[i].getAlias());
//            //connectMenuItemsForDropDownMenu[i] = new MenuItem (connectMenuForDropDownMenu, SWT.PUSH);
//            //connectMenuItemsForDropDownMenu[i].setText (cis[i].getAlias());
//        }
        
//        // create the xplain mode menu and its menuitems from the Constants class
//        xplainModeMenu = new Menu(getShell(), SWT.POP_UP);
//        xplainModeMenuItems = new MenuItem[Constants.main_xplainModeNames.length];
//        for(int i=0;i<Constants.main_xplainModeNames.length;i++){
//            xplainModeMenuItems[i] = new MenuItem (xplainModeMenu, SWT.RADIO);
//            xplainModeMenuItems[i].setText(Constants.main_xplainModeNames[i]);
//        }
//        // TODO must reflect default status of a connection
//        xplainModeMenuItems[0].setSelection(true);
    }
    
	/** this method inits all the widgets and components of the toolbar */
	private void initComponents(){
        initMenus();
        
        setLayout(new GridLayout(3, false));
		
        // create the Toolbar
		mainTools = new ToolBar(this, SWT.DROP_DOWN);

		GridData gd = new GridData();
		gd.horizontalAlignment = GridData.FILL;
		gd.grabExcessHorizontalSpace = true;
		mainTools.setLayoutData(gd);
		

		
        toolItemExtensionMenu = new Menu(getShell(), SWT.CASCADE);

        
		
		// create MenuWidgets
		
		// connect
		connectMW = new MenuWidget(mainTools, toolItemExtensionMenu, SWT.DROP_DOWN, SWT.CASCADE);
		connectMW.setText(toolButtonNames[0]);
		connectMW.setImage(ResourceRegistry.getInstance().getImage("connect"));
		connectMW.setHotImage(ResourceRegistry.getInstance().getImage("connecthot"));
		connectMW.setDisabledImage(ResourceRegistry.getInstance().getImage("connectdisabled"));
		connectMW.setToolTipText(Constants.main_connect_tooltip);
		//connectMW.setMenu(connectMenuForDropDownMenu);
		for (int i = 0; i < connectMenuItemsForToolBar.length; i++)
			connectMW.addSubMenuItem(connectMenuItemsForToolBar[i]);
		
		// disconnect
		disconnectMW = new MenuWidget(mainTools, toolItemExtensionMenu, SWT.PUSH, SWT.NONE);
		disconnectMW.setText(toolButtonNames[1]);
		disconnectMW.setImage(ResourceRegistry.getInstance().getImage("disconnect"));
		disconnectMW.setHotImage(ResourceRegistry.getInstance().getImage("disconnecthot"));
		disconnectMW.setDisabledImage(ResourceRegistry.getInstance().getImage("disconnectdisabled"));
		disconnectMW.setEnabled(false);
		disconnectMW.setToolTipText(Constants.main_disconnect_tooltip);

		// an anonymous place holder for the separator
		new MenuWidget(mainTools, toolItemExtensionMenu, SWT.SEPARATOR, SWT.SEPARATOR);
		
		// reconnect
		reconnectMW = new MenuWidget(mainTools, toolItemExtensionMenu, SWT.PUSH, SWT.NONE);
		reconnectMW.setText(toolButtonNames[2]);
		reconnectMW.setImage(ResourceRegistry.getInstance().getImage("reconnect"));
		reconnectMW.setHotImage(ResourceRegistry.getInstance().getImage("reconnecthot"));
		reconnectMW.setDisabledImage(ResourceRegistry.getInstance().getImage("reconnectdisabled"));
		reconnectMW.setEnabled(false);
		reconnectMW.setToolTipText(Constants.main_reconnect_tooltip);
		
		// an anonymous place holder for the separator
		new MenuWidget(mainTools, toolItemExtensionMenu, SWT.SEPARATOR, SWT.SEPARATOR);
		
//		// tree extract
//		treeExtractMW = new MenuWidget(mainTools, toolItemExtensionMenu, SWT.PUSH, SWT.NONE);
//		treeExtractMW.setText(toolButtonNames[3]);
//		treeExtractMW.setImage(ResourceRegistry.getInstance().getImage("compileoff"));
//		treeExtractMW.setEnabled(false);
//		treeExtractMW.setToolTipText(Constants.main_tree_tooltip);
//		
//        // xplain mode
//        xplainModeMW = new MenuWidget(mainTools, toolItemExtensionMenu, SWT.DROP_DOWN, SWT.CASCADE);
//        xplainModeMW.setText(toolButtonNames[4]);
//        xplainModeMW.setImage(ResourceRegistry.getInstance().getImage("executionoff"));
//        xplainModeMW.setEnabled(false);
//        xplainModeMW.setToolTipText(Constants.main_xplain_tooltip);
//        //register item in xplainModeMenuWidget
//        for (int i = 0; i < Constants.main_xplainModeNames.length; i++)
//        	xplainModeMW.addSubMenuItem(xplainModeMenuItems[i]);
//        xplainModeMW.setSubMenu(xplainModeMenu);
//        
//                
//        // xplain timings
//        xplainTimingMW = new MenuWidget(mainTools, toolItemExtensionMenu, SWT.PUSH, SWT.NONE);
//        xplainTimingMW.setText(toolButtonNames[5]);
//        xplainTimingMW.setImage(ResourceRegistry.getInstance().getImage("timing"));
//        xplainTimingMW.setEnabled(false);
//        xplainTimingMW.setToolTipText(Constants.main_timing_tooltip);
//        
//        // workload
//        workloadMW = new MenuWidget(mainTools, toolItemExtensionMenu, SWT.PUSH, SWT.NONE);
//        workloadMW.setText(toolButtonNames[6]);
//        workloadMW.setImage(ResourceRegistry.getInstance().getImage("workload"));
//        workloadMW.setEnabled(false);
//        workloadMW.setToolTipText(Constants.main_workload_tooltip);
        
		// an anonymous place holder for the separator
		new MenuWidget(mainTools, toolItemExtensionMenu, SWT.SEPARATOR, SWT.SEPARATOR);
        
        // load history
		loadHistoryMW = new MenuWidget(mainTools, toolItemExtensionMenu, SWT.PUSH, SWT.NONE);
		loadHistoryMW.setText(toolButtonNames[7]);
		loadHistoryMW.setImage(ResourceRegistry.getInstance().getImage("loadhistory"));
		loadHistoryMW.setHotImage(ResourceRegistry.getInstance().getImage("loadhistoryhot"));
		loadHistoryMW.setToolTipText(Constants.main_load_tooltip);
		
        // save history
		saveHistoryMW = new MenuWidget(mainTools, toolItemExtensionMenu, SWT.PUSH, SWT.NONE);
		saveHistoryMW.setText(toolButtonNames[8]);
		saveHistoryMW.setImage(ResourceRegistry.getInstance().getImage("savehistory"));
		saveHistoryMW.setHotImage(ResourceRegistry.getInstance().getImage("savehistoryhot"));
		saveHistoryMW.setToolTipText(Constants.main_save_tooltip);
		
		// an anonymous place holder for the separator
		new MenuWidget(mainTools, toolItemExtensionMenu, SWT.SEPARATOR, SWT.SEPARATOR);

        // reset
		resetMW = new MenuWidget(mainTools, toolItemExtensionMenu, SWT.PUSH, SWT.NONE);
		resetMW.setText(toolButtonNames[9]);
		resetMW.setImage(ResourceRegistry.getInstance().getImage("reset"));
		resetMW.setToolTipText(Constants.main_reset_tooltip);
		
		// an anonymous place holder for the separator
		new MenuWidget(mainTools, toolItemExtensionMenu, SWT.SEPARATOR, SWT.SEPARATOR);
		
        // exit
        exitMW = new MenuWidget(mainTools, toolItemExtensionMenu, SWT.PUSH, SWT.NONE);
        exitMW.setText(toolButtonNames[10]);
        exitMW.setImage(ResourceRegistry.getInstance().getImage("exit"));
        exitMW.setToolTipText(Constants.main_exit_tooltip);
		
        //connectMW.setMenu(connectMenuForToolBar);
        
        toolItemExtensionToolBar = new ToolBar(this, SWT.NONE);
        toolItemExtensionToolBar.setLayoutData(new GridData());
		//tb.setLayoutData(new GridData(GridData.VERTICAL_ALIGN_FILL));
		toolItemExtension = new ToolItem(toolItemExtensionToolBar, SWT.NONE);
		//ti.setText("v");
		toolItemExtension.setImage(ResourceRegistry.getInstance().getImage("arrow"));
		
//        allToolItemsButton = new Button(this, SWT.ARROW | SWT.BOTTOM);
//        allToolItemsButton.setLayoutData(new GridData(GridData.VERTICAL_ALIGN_FILL));

//Torsten        allToolItemsButton = new Button(this, SWT.ARROW | SWT.BOTTOM);
        
//        for (int i = 0; i < mainTools.getItemCount(); i++){
//        	ToolItem ti = mainTools.getItem(i);
//        	
//        	MenuItem mi = new MenuItem(allToolItemsMenu, ti.getStyle() == SWT.DROP_DOWN ? SWT.CASCADE : ti.getStyle());
//        	mi.setText(ti.getText());
//        	mi.setImage(ti.getImage());
//        	mi.setEnabled(ti.getEnabled());
//        	
//        	if (mi.getStyle() != SWT.CASCADE)
//        		continue;
//        	
//        	Menu m = new Menu(mi);
//        	
//        	for (int j = 0; j < connectMenuItemsForToolBar.length; j++){
//        		MenuItem mmm = new MenuItem(m, SWT.PUSH);
//        		mmm.setText(connectMenuItemsForToolBar[j].getText());
//        		
//        		
//        	}
//        	
//        	
//        	mi.setMenu(m);
//        	
//        }
        
        
        
        // logo image
		logo = new Label(this, SWT.NONE);
		logo.setImage(ResourceRegistry.getInstance().getImage("logo"));

	}
	
	/**
	 * this method creates all listeners and connects them with the 
	 * different widgets.
	 */
	private void initListeners(){
		
		Configuration.getInstance().addModelChangeListener(this);
		
		// Connect Button Listener
		connectMW.addListener(SWT.Selection, new Listener () {
			public void handleEvent (Event e) {
				int state = JDBCInterface.getInstance().getConnectionStatus();
				if(state == JDBCInterface.DISCONNECTED){
					// try to establish a connection
					
					if (e.widget instanceof ToolItem){
						Rectangle rect = ((ToolItem)e.widget).getBounds ();
						//Rectangle rect = connectTI.getBounds ();
						Point pt = new Point (rect.x, rect.y + rect.height);
						pt = ((ToolItem)e.widget).getParent().toDisplay (pt);
						//pt = mainTools.toDisplay (pt);
						connectMenuForToolBar.setLocation (pt.x, pt.y);
						connectMenuForToolBar.setVisible (true);
					} else if (e.widget instanceof MenuItem){ // TODO
						// done by the menu itself
						
//						Rectangle rect = ((MenuItem)e.widget).getBounds ();
//						//Rectangle rect = connectTI.getBounds ();
//						Point pt = new Point (rect.x, rect.y + rect.height);
//						pt = ((MenuItem)e.widget).getParent().toDisplay (pt);
//						//pt = mainTools.toDisplay (pt);
//						connectMenuForToolBar.setLocation (pt.x, pt.y);
//						connectMenuForToolBar.setVisible (true);
					}
				}
			}
		});

        // apply one listener to all the Connect Menu Items
        // create a listener, which gets called when a menuitem was selected from the connect menu
        connectListener = new Listener(){
            public void handleEvent(Event event) {
                    ConnectionInfo[] cis = Configuration.getInstance().getConnectionInfos();
                    String widgetText = ((MenuItem)event.widget).getText();
                    boolean test;
                    for(int i=0;i<cis.length;i++){
                        if(cis[i].getAlias().equals(widgetText)){
                            test = JDBCInterface.getInstance().connect(cis[i]);
                            if(test){
                                connectMW.setEnabled(false);
                                disconnectMW.setEnabled(true);
                                reconnectMW.setEnabled(true);
//                                if (cis[i].isEmbedded()){
//                                    treeExtractMW.setEnabled(true);
//                                }
                                MainStatusLine.message("Connection to '"+widgetText+"' established.",false);
                                //treeExtractTI.setImage(ResourceRegistry.getInstance().getImage("compileoff"));
//                                xplainModeMW.setEnabled(true);
//                                xplainTimingMW.setEnabled(true);
//                                workloadMW.setEnabled(true);
                                break;
                            } else {
                                MainStatusLine.message("could not connect to "+widgetText+" !",true);
                            }
                        }
                    }
            }
        };
        
        // apply this listener to all the connect menu items
        registerConnectListenerForMenuItems();
        
		// create the Disconnect Button Listener
		disconnectMW.addListener(SWT.Selection, new Listener () {
			public void handleEvent (Event e) {
				int state = JDBCInterface.getInstance().getConnectionStatus();
				if(state == JDBCInterface.CONNECTED){
					// try to disconnect from current connected database
					JDBCInterface.getInstance().disconnect();
					MainStatusLine.message("disconnected...",false);
					connectMW.setEnabled(true);
					disconnectMW.setEnabled(false);
					reconnectMW.setEnabled(false);
//					treeExtractMW.setEnabled(false);
//                    xplainModeMW.setEnabled(false);
//                    xplainTimingMW.setEnabled(false);
//                    workloadMW.setEnabled(false);
				}
			}
		});
		
        // ReConnect Button Listener
        reconnectMW.addListener(SWT.Selection, new Listener () {
            public void handleEvent (Event e) {
                JDBCInterface iface = JDBCInterface.getInstance();
                ConnectionInfo conInfo = iface.getCurrentConnectionInfo();
                iface.disconnect();
                iface.connect(conInfo);
            }
        });
        
//		// create the compile tree Extraction Button Listener
//		treeExtractMW.addListener(SWT.Selection, new Listener () {
//			public void handleEvent (Event e) {
//				int status;
//				if(JDBCInterface.getInstance().getTreeExtraction()){
//					status = JDBCInterface.getInstance().toggleTreeExtraction();
//					if(status == 2 ){ // everything went ok
//						MainStatusLine.message("Tree Extraction toggled off",false);
//						treeExtractMW.setImage(ResourceRegistry.getInstance().getImage("compileoff"));
//					} else 
//					if(status == -1 ){ // Error
//						MainStatusLine.message("Tree Extraction could not be toggled off",true);
//					}
//				} else {
//					status = JDBCInterface.getInstance().toggleTreeExtraction();
//					if(status == 2){ // everything went ok
//						MainStatusLine.message("Tree Extraction toggled on",false);
//						treeExtractMW.setImage(ResourceRegistry.getInstance().getImage("compileon"));
//					} else 
//					if(status == -1 ){ // Error
//						MainStatusLine.message("Tree Extraction could not be toggled on",true);
//					}
//				}
//			}
//		});
//		
//        // create the more complex explain mode selection/toggle on/off listener
//        xplainModeMW.addListener (SWT.Selection, new Listener () {
//            public void handleEvent (Event event) {
//                // was the down-arrow pressed?
//                if (event.detail == SWT.ARROW) {// TODO
//                	
//					if (event.widget instanceof ToolItem){
//	                    Rectangle rect = ((ToolItem)event.widget).getBounds ();
//	                    Point pt = new Point (rect.x, rect.y + rect.height);
//	                    pt = ((ToolItem)event.widget).getParent().toDisplay (pt);
//	                    xplainModeMenu.setLocation (pt.x, pt.y);
//	                    xplainModeMenu.setVisible (true);
//					} else if (event.widget instanceof MenuItem){
//						
//						// done by the menu itself
//						
//					}
//
////                    Rectangle rect = xplainModeMW.getBounds ();
////                    Point pt = new Point (rect.x, rect.y + rect.height);
////                    pt = mainTools.toDisplay (pt);
////                    xplainModeMenu.setLocation (pt.x, pt.y);
////                    xplainModeMenu.setVisible (true);
//                } else {
//                    // just a normal push of the big button
//                    int status;
//                    if(JDBCInterface.getInstance().getQEPExplanation()){
//                        status = JDBCInterface.getInstance().toggleQueryExplanation();
//                        if(status == 2 ){ // everything went ok
//                            MainStatusLine.message("Query Explanation toggled off",false);
//                            xplainModeMW.setImage(ResourceRegistry.getInstance().getImage("executionoff"));
//                        } else 
//                        if(status == -1 ){ // Error
//                            MainStatusLine.message("Query Explanation could not be toggled off",true);
//                        }
//                    } else {
//                        status = JDBCInterface.getInstance().toggleQueryExplanation();
//                        if(status == 2){ // everything went ok
//                            MainStatusLine.message("Query Explanation toggled on",false);
//                            xplainModeMW.setImage(ResourceRegistry.getInstance().getImage("executionon"));
//                        } else 
//                        if(status == -1 ){ // Error
//                            MainStatusLine.message("Query Explanation could not be toggled on",true);
//                        }
//                    }
//                    
//                }
//            }
//        });
//		
//        // create the listener for the xplain mode menuitems and 
//        // apply it to the menuitems
//        xplainModeListener = new Listener(){
//            public void handleEvent(Event event) {
//                
//                String itemText = ((MenuItem)event.widget).getText();
//                if ((itemText.equalsIgnoreCase(Constants.main_xplainModeNames[0]) && oldExplainMode) ||
//                    (itemText.equalsIgnoreCase(Constants.main_xplainModeNames[1]) && !oldExplainMode)    ){
//                        // toggle inverse mode
//                        int ret_code = JDBCInterface.getInstance().toggleQueryExplanationMode();
//                        
//                        // evaluate return code and print according message in the statusline
//                        if(ret_code==2){
//                            // if only mode -> then now full mode
//                            if (oldExplainMode){
//                                MainStatusLine.message("switched to explain query with execution mode ",false);
//                                oldExplainMode = false;
//                            // if full mode -> then now only mode
//                            } else {
//                                MainStatusLine.message("switched to explain query without execution mode",false);
//                                oldExplainMode = true;
//                            }
//                            // switch explain mode on
//                            if (JDBCInterface.getInstance().getQEPExplanation()) {
//                            	xplainModeMW.setImage(ResourceRegistry.getInstance().getImage("executionon"));
//                            } else {
//                                int status = JDBCInterface.getInstance().toggleQueryExplanation();
//                                if(status == 2){ // everything went ok
//                                    MainStatusLine.message("Query Explanation toggled on", false);
//                                    xplainModeMW.setImage(ResourceRegistry.getInstance().getImage("executionon"));
//                                } else 
//                                if(status == -1 ){ // Error
//                                    MainStatusLine.message("Query Explanation could not be toggled on",true);
//                                }
//                            	
//                            }
//                        } else {
//                            MainStatusLine.message("could not switch to chosen explain mode",true);
//                        }
//                        // change selection for cascade menu
//                        for (int i = 0; i < Constants.main_xplainModeNames.length; i++)
//                        {
//                       		xplainModeMenuItems[i].setSelection(itemText.equalsIgnoreCase(Constants.main_xplainModeNames[i]));                        	
//                        }
//                } else if ((itemText.equalsIgnoreCase(Constants.main_xplainModeNames[0]) && !oldExplainMode) ||
//                    (itemText.equalsIgnoreCase(Constants.main_xplainModeNames[1]) && oldExplainMode)) {
//                    // switch explain mode on
//                    if (JDBCInterface.getInstance().getQEPExplanation()) {
//                    	xplainModeMW.setImage(ResourceRegistry.getInstance().getImage("executionon"));
//                    } else {
//                        int status = JDBCInterface.getInstance().toggleQueryExplanation();
//                        if(status == 2){ // everything went ok
//                            MainStatusLine.message("Query Explanation toggled on", false);
//                            xplainModeMW.setImage(ResourceRegistry.getInstance().getImage("executionon"));
//                        } else 
//                        if(status == -1 ){ // Error
//                            MainStatusLine.message("Query Explanation could not be toggled on",true);
//                        }
//                    	
//                    }                	
//                }
//            }
//        };
//        
//        // and register this xplainModeListener to the xplain mode menu items
//        registerXplainModeListenerForMenuItems();
//        
//        // switch between timing mode on or off listener
//        xplainTimingMW.addListener(SWT.Selection, new Listener () {
//            public void handleEvent (Event e) {
//                int status;
//                if(JDBCInterface.getInstance().getTimingMode()){
//                    status = JDBCInterface.getInstance().toggleTimingMode();
//                    if(status == 2 ){ // everything went ok
//                        MainStatusLine.message("Time Measurement toggled off",false);
//                        xplainTimingMW.setImage(ResourceRegistry.getInstance().getImage("timing"));
//                    } else 
//                    if(status == -1 ){ // Error
//                        MainStatusLine.message("Time Measurement could not be toggled off",true);
//                    }
//                } else {
//                    status = JDBCInterface.getInstance().toggleTimingMode();
//                    if(status == 2){ // everything went ok
//                        MainStatusLine.message("Time Measurement toggled on",false);
//                        xplainTimingMW.setImage(ResourceRegistry.getInstance().getImage("timingon"));
//                    } else 
//                    if(status == -1 ){ // Error
//                        MainStatusLine.message("Time Measurement could not be toggled on",true);
//                    }
//                }
//            }
//        });
//        
//        // switch between timing mode on or off listener
//        workloadMW.addListener(SWT.Selection, new Listener () {
//            public void handleEvent (Event e) {
//                int status;
//                if(JDBCInterface.getInstance().getWorkloadMode()){
//                    status = JDBCInterface.getInstance().toggleWorkloadMode();
//                    if(status == 2 ){ // everything went ok
//                        MainStatusLine.message("Workload capturing toggled off",false);
//                        workloadMW.setImage(ResourceRegistry.getInstance().getImage("workload"));
//                    } else 
//                    if(status == -1 ){ // Error
//                        MainStatusLine.message("Workload capturing could not be toggled off",true);
//                    }
//                } else {
//                    status = JDBCInterface.getInstance().toggleWorkloadMode();
//                    if(status == 2){ // everything went ok
//                        MainStatusLine.message("Workload capturing toggled on",false);
//                        workloadMW.setImage(ResourceRegistry.getInstance().getImage("workloadon"));
//                    } else 
//                    if(status == -1 ){ // Error
//                        MainStatusLine.message("Workload capturing could not be toggled on",true);
//                    }
//                }
//            }
//        });
//        
		// Load History Button Listener
		loadHistoryMW.addListener(SWT.Selection, new Listener () {
			public void handleEvent (Event e) {
				FileDialog fd = new FileDialog(parent.getShell(),SWT.OPEN);
				fd.setFilterExtensions(new String[]{Constants.historyWildcardFilter});
				String name = fd.open();
				if(name!=null){
					try {
						History.getInstance().load(name);
					} catch (FileNotFoundException e1) {
						MessageBox mb = new MessageBox(parent.getShell(), SWT.ICON_ERROR | SWT.OK);
						mb.setText("Load Error");
						mb.setMessage(Constants.main_FileNotFound_History_open_Dialog);
						mb.open();
						e1.printStackTrace();
					} catch (IOException e1) {
						MessageBox mb = new MessageBox(parent.getShell(), SWT.ICON_ERROR | SWT.OK);
						mb.setText("Load Error");
						mb.setMessage(Constants.main_IOException_History_open_Dialog);
						mb.open();
						e1.printStackTrace();

					} catch (ClassNotFoundException e1) {
						MessageBox mb = new MessageBox(parent.getShell(), SWT.ICON_ERROR | SWT.OK);
						mb.setText("Load Error");
						mb.setMessage(Constants.main_ClassNotFound_History_open_Dialog);
						mb.open();
						e1.printStackTrace();
					}
				}
				
			}
		});
		
		// Save History Button Listener
		saveHistoryMW.addListener(SWT.Selection, new Listener () {
			public void handleEvent (Event e) {
				FileDialog fd = new FileDialog(parent.getShell(),SWT.SAVE);
				fd.setFilterExtensions(new String[]{Constants.historyWildcardFilter});
				String name = fd.open();
				if(name!=null){
					try {
						if (name.indexOf(".history") < 0 || (name.length() - name.indexOf(".history") != 8))
								name += ".history";
						History.getInstance().save(name);
					} catch (FileNotFoundException e1) {
						MessageBox mb = new MessageBox(parent.getShell(), SWT.ICON_ERROR | SWT.OK);
						mb.setText("Save Error");
						mb.setMessage(Constants.main_FileNotFound_History_save_Dialog);
						mb.open();
						e1.printStackTrace();
					} catch (IOException e1) {
						MessageBox mb = new MessageBox(parent.getShell(), SWT.ICON_ERROR | SWT.OK);
						mb.setText("Save Error");
						mb.setMessage(Constants.main_IOException_History_save_Dialog);
						mb.open();
						e1.printStackTrace();
					}
				}
				
			}
		});
		
		// Reset Button Listener
		resetMW.addListener(SWT.Selection, new Listener () {
			public void handleEvent (Event e) {
				ModuleRegistry.getInstance().reset();
			}
		});
		
        // Exit Button Listener
        exitMW.addListener(SWT.Selection, new Listener () {
            public void handleEvent (Event e) {
                // just dispose the parent, which must be the 
                // shell into which the maintoolbar was integrated
                getParent().dispose();
            }
        });
        
        
//        allToolItemsButton.addListener(SWT.Selection, new Listener () {
//            public void handleEvent (Event e) {
//				Rectangle rect = allToolItemsButton.getBounds ();
//				Point pt = new Point (rect.x, rect.y + rect.height);
//				pt = mainTools.toDisplay (pt);
//				allToolItemsMenu.setLocation (pt.x, pt.y);
//				allToolItemsMenu.setVisible (true);
//            }
//        });
        
        toolItemExtension.addListener(SWT.Selection, new Listener () {
            public void handleEvent (Event e) {
				Rectangle rect = toolItemExtension.getBounds();
				Point pt = new Point (toolItemExtension.getParent().getLocation().x, rect.y + rect.height);
				// create a new toolbar
				if (extensionMenu != null)
					extensionMenu.dispose();
		        extensionMenu = new Menu(getShell(), SWT.CASCADE);
				// input not shown items into this tollbar
				int cumulativeWidth = 1;//4;
				int width = mainTools.getSize().x;
				for (int i = 0; i < toolItemExtensionMenu.getItemCount(); i++)
				{
					MenuItem child = toolItemExtensionMenu.getItem(i);
					MenuWidget childWidget = MenuWidget.getMenuWidget(child);
					cumulativeWidth += childWidget.getWidth();
					if (cumulativeWidth > width)
					{
						childWidget.setNewInMenu(extensionMenu);
					}
				}
				
				pt = mainTools.toDisplay (pt);
				extensionMenu.setLocation(pt.x, pt.y);
				extensionMenu.setVisible(true);
//				toolItemExtensionMenu.setLocation (pt.x, pt.y);
//				toolItemExtensionMenu.setVisible (true);
				
            }
        });
        
        final ControlListener controlListener = new ControlAdapter(){
        	public void controlResized(ControlEvent e) {
        		boolean allVisible = true;
				int cumulativeWidth = 1;
				int width = MainToolBar.this.getSize().x - toolItemExtensionToolBar.getSize().x - logo.getSize().x - 20;
				for (int i = 0; i < toolItemExtensionMenu.getItemCount(); i++) {
					MenuItem child = toolItemExtensionMenu.getItem(i);
					MenuWidget childWidget = MenuWidget.getMenuWidget(child);
					cumulativeWidth += childWidget.getWidth();
					if (cumulativeWidth > width) {
						allVisible = false;
						break;
					}
				}
				
				//change the visibility of the whole toolbar 
				toolItemExtension.getParent().setVisible(!allVisible);
//				System.out.println("CHANGE to " + !allVisible);
        	}
        };
        addControlListener(controlListener);
        
        // to first set the toolItemExtension to enable/disable when the application starts
        addPaintListener(new PaintListener(){
        	public void paintControl(PaintEvent e){
        		controlListener.controlResized(null);
        		MainToolBar.this.removePaintListener(this);
        	}
        });
	}
	
	// ----------------- business logic ----------------------------------
	
	/** this creates the connect menu items */
	private void createConnectMenuItems(){
		// Connections PopUp Menu
		if (connectMenuForToolBar != null)
			connectMenuForToolBar.dispose();
		connectMenuForToolBar = new Menu (getShell(), SWT.POP_UP);
		ConnectionInfo[] cis = Configuration.getInstance().getConnectionInfos();
		connectMenuItemsForToolBar = new MenuItem[cis.length];
		//clean registered items
		if (connectMW != null)
			connectMW.cleanSubMenuItems();
		for (int i=0; i<cis.length; i++) {
			connectMenuItemsForToolBar[i] = new MenuItem (connectMenuForToolBar, SWT.PUSH);
			connectMenuItemsForToolBar[i].setText (cis[i].getAlias());
			//register item in connectMenuWidget
			if (connectMW != null)
				connectMW.addSubMenuItem(connectMenuItemsForToolBar[i]);
		}
	}
	
	/** this method registers the connect listener for all menu items */
	private void registerConnectListenerForMenuItems(){
		for(int i=0;i<connectMenuItemsForToolBar.length;i++){
			connectMenuItemsForToolBar[i].addListener(SWT.Selection,connectListener);
		}
		connectMW.setSubMenuListener(connectListener);
	}

//    /** this method registers the connect listener for all menu items */
//    private void registerXplainModeListenerForMenuItems(){
//        for(int i=0;i<xplainModeMenuItems.length;i++){
//            xplainModeMenuItems[i].addListener(SWT.Selection,xplainModeListener);
//        }
//        xplainModeMW.setSubMenuListener(xplainModeListener);
//    }
    
	// ----- GenericModelChangeListener implementation ------------------
	
	/** gets called when a new connection was saved, 
	 *  changes the connect menu
	 */
	public void modelChanged(GenericModelChangeEvent event) {
		switch(event.detail){
			case DemoEvents.CONFIG_CONNECTION_CHANGED: 
				createConnectMenuItems();
				registerConnectListenerForMenuItems();
				break;
		}
	}
}
