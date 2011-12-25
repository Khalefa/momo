package components.tabs;

import java.sql.ResultSet;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StyleRange;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.custom.VerifyKeyListener;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;

import org.eclipse.swt.widgets.Event;

import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.swt.widgets.TabFolder;

import components.AbstractComponent;
import components.listeners.DefaultLineStyleListener;
import components.listeners.GenericStyleListener;
import components.listeners.SQLKeywordLineStyleListener;
import components.listeners.UpperKeyListener;
import components.widgets.JGraphWidget;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import modules.databaseif.History;
import modules.databaseif.HistoryEntry;

import modules.databaseif.JDBCInterface;
import modules.databaseif.ScriptModule;

import modules.generic.DemoEvents;
import modules.generic.GenericModelChangeEvent;
import modules.generic.GenericModelChangeListener;
import modules.misc.Constants;

import modules.planview.PostgresPlanNode;
import modules.planview.QueryPlanViewerTools;
import org.eclipse.swt.events.VerifyEvent;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;

/**

 *
 * @author Christopher Schildt
 * @date   08.05.2006
 *
 */
public class QueryPlanViewerTab extends AbstractComponent implements GenericModelChangeListener {
    
    public static Table modelInfos;

 

    private StyledText selectTimeseries;
    AbstractComponent tab;
    private Menu loadQueryMenu;				// the load query menu
    private Button loadQuery;
    private Button saveQuery;
    private Button explain;
    private Button equalsize;
    private Button collapsed;
    private Listener loadQueryListener;
    JGraphWidget jgraph;
    private HashMap<String, List<MenuItem>> loadQueryMenuItems;
    private GridData TL2Data;

    public QueryPlanViewerTab(TabFolder tabFolder, int none) {
        super(tabFolder, none);
        tab = this;
    }

    @Override
    public void modelChanged(GenericModelChangeEvent event) {
        switch (event.detail) {
            // JDBCInterface Model Change Events
            case DemoEvents.CONNECTION_ESTABLISHED:
                activateControls();
                break;
            case DemoEvents.CONNECTION_CLOSED:
                deactivateControls();
                break;
            default:
                break;
        }

    }

    protected void initComponents() {
        setLayout(new GridLayout(8, false));
        selectTimeseries = new StyledText(this, SWT.BORDER | SWT.V_SCROLL | SWT.H_SCROLL);
        {
            GridData ConnectionTextLData = new GridData();
            ConnectionTextLData.horizontalSpan = 8;
            ConnectionTextLData.verticalSpan = 10;
            ConnectionTextLData.grabExcessHorizontalSpace = true;
            ConnectionTextLData.horizontalAlignment = GridData.FILL;
            ConnectionTextLData.verticalAlignment = GridData.FILL;
            selectTimeseries.setLayoutData(ConnectionTextLData);
        }

        StyleRange sqlInputSR = MiscUtils.loadStyleRangeFromConfiguration("console.input.fg.sql");
        StyleRange[] inputSRanges = new StyleRange[1];

        // get the configuration for the SQLLineStyleListener
        inputSRanges[0] = MiscUtils.loadStyleRangeFromConfiguration("console.input.fg.default");
        selectTimeseries.addLineStyleListener(new GenericStyleListener(
                new SQLKeywordLineStyleListener(
                new DefaultLineStyleListener(),
                sqlInputSR),
                inputSRanges));


        selectTimeseries.setText("SELECT e_time, SUM(e_amount) am \n\tFROM edemand\n\tWHERE e_customer=\'me0\'\n\tGROUP BY e_time ORDER BY e_time");


        VerifyKeyListener inputUpperKeyListener;
        inputUpperKeyListener = new UpperKeyListener(selectTimeseries, true);
        selectTimeseries.addVerifyKeyListener(inputUpperKeyListener);

        {
            loadQuery = new Button(this, SWT.None);
            loadQuery.setText(Constants.console_toolButtonNames[1]);


        }
        {
            saveQuery = new Button(this, SWT.None);
            saveQuery.setText(Constants.console_toolButtonNames[2]);
        }

        explain = new Button(this, SWT.None);
        explain.setText("Explain!");
        equalsize = new Button(this, SWT.CHECK);
        equalsize.setText("Equal node width");
        equalsize.setSelection(true);
        collapsed = new Button(this, SWT.CHECK);
        collapsed.setText("Nodes collapsed");
        collapsed.setSelection(true);






        jgraph = new JGraphWidget(this, SWT.None);// JGraphAdapter(this, SWT.SCROLL_PAGE);
        GridData ConnectionTextLData = new GridData();
        ConnectionTextLData.horizontalSpan = 7;
        ConnectionTextLData.grabExcessHorizontalSpace = true;
        ConnectionTextLData.grabExcessVerticalSpace = true;
        ConnectionTextLData.horizontalAlignment = GridData.FILL;
        ConnectionTextLData.verticalAlignment = GridData.FILL;
        jgraph.setLayoutData(ConnectionTextLData);

        Composite rightSide = new Composite(this, SWT.None);
        rightSide.setLayout(new GridLayout(1, false));
        ConnectionTextLData = new GridData();
        ConnectionTextLData.widthHint = this.getShell().getSize().x / 4;
        modelInfos = new Table(rightSide, SWT.None);
            TableColumn co1 = new TableColumn(modelInfos, SWT.None);
            TableColumn co12 = new TableColumn(modelInfos, SWT.None);
            modelInfos.setLinesVisible(true);
            ConnectionTextLData = new GridData();
            ConnectionTextLData.grabExcessHorizontalSpace = true;
            ConnectionTextLData.grabExcessVerticalSpace = true;
            ConnectionTextLData.horizontalAlignment = GridData.FILL;
            ConnectionTextLData.verticalAlignment = GridData.FILL;
            modelInfos.setLayoutData(ConnectionTextLData);
            modelInfos.setEnabled(true);
            modelInfos.setVisible(true);



        ConnectionTextLData.horizontalAlignment = GridData.FILL;
        ConnectionTextLData.verticalAlignment = GridData.FILL;
        rightSide.setLayoutData(ConnectionTextLData);

    }

    public void deactivateControls() {
        loadQuery.setEnabled(true);
        saveQuery.setEnabled(true);
        explain.setEnabled(false);
        // TODO Auto-generated method stub

    }

    public void activateControls() {
        // TODO Auto-generated method stub
        loadQuery.setEnabled(true);
        saveQuery.setEnabled(true);
        explain.setEnabled(true);

    }

    @Override
    protected void initListeners() {
        JDBCInterface.getInstance().addModelChangeListener(this);
        selectTimeseries.addVerifyKeyListener(new VerifyKeyListener() {

            private int currentHistoryIndex;
            private String stmtText;

            public void verifyKey(VerifyEvent event) {

                // CTRL + Return
                if ((event.keyCode == 13) && (event.stateMask == 262144)) {
                    stmtText = selectTimeseries.getText();
                ResultSet rs = JDBCInterface.getInstance().executeStatementWithResult("Explain " + selectTimeseries.getText());
                jgraph.refresh();
                jgraph.getJGraph().getModel().beginUpdate();
                QueryPlanViewerTools.initializeStyles(jgraph.getJGraph(), jgraph.getSize().x / 2 - 100);
                QueryPlanViewerTools.createTreeModel(rs, jgraph.getJGraph(), jgraph.getSize().x / 2 - 100);
                QueryPlanViewerTools.doLayout();
                jgraph.getJGraph().getModel().endUpdate();

                }
                // replace statement with statement in history if keyup or keydown was pressed
                // key up
                if ((event.keyCode == 16777217) && (event.stateMask == 262144)) {
                    int noEntries = History.getInstance().size();
                    String stmt = "";
                    if (currentHistoryIndex > 0 && noEntries > 0) {
                        HistoryEntry entry = History.getInstance().getEntry(currentHistoryIndex - 1);
                        stmt = entry.getSQL();
                    }
                    currentHistoryIndex = Math.max(-1, currentHistoryIndex - 1);
                    selectTimeseries.setText(stmt);
                    selectTimeseries.setCaretOffset(selectTimeseries.getText().length());
                }
                // key down
                if ((event.keyCode == 16777218) && (event.stateMask == 262144)) {
                    int noEntries = History.getInstance().size();
                    String stmt = "";
                    if (currentHistoryIndex < noEntries - 1) {
                        HistoryEntry entry = History.getInstance().getEntry(currentHistoryIndex + 1);
                        stmt = entry.getSQL();
                    }
                    currentHistoryIndex = Math.min(noEntries, currentHistoryIndex + 1);
                    selectTimeseries.setText(stmt);
                    selectTimeseries.setCaretOffset(selectTimeseries.getText().length());
                }
                // for debug, commment this out to resolve key inputs or to resolve char combinations
                //System.out.println("Keycode: "+event.keyCode+";Character: "+event.character+";stateMask: "+event.stateMask);

            }
        });
        saveQuery.addListener(SWT.Selection, new Listener() {

            public void handleEvent(Event event) {
                FileDialog fd = new FileDialog(getShell(), SWT.SAVE);
                fd.setFilterExtensions(new String[]{Constants.singleStmtWildcardFilter});
                String result = fd.open();

                if (result != null && result != "") {
                    if (!result.endsWith(".stmt")) {
                        result = result + ".stmt";
                    }
                    ScriptModule.getInstance().saveScriptFile(result, selectTimeseries.getText());

                    loadQueryScriptList();
                    for (String key : loadQueryMenuItems.keySet()) {
                        List<MenuItem> temp = loadQueryMenuItems.get(key);
                        for (MenuItem a : temp) {
                            a.addListener(SWT.Selection, loadQueryListener);
                        }
                    }
                }
            }
        });
        // show the loadquery menu when user presses the toolbutton
        loadQuery.addListener(SWT.Selection, new Listener() {

            public void handleEvent(Event e) {
                Rectangle rect = loadQuery.getBounds();
                Point pt = new Point(rect.x, rect.y-3*rect.height); //BUG: Buggy here, -3* should be -1*
                pt = loadQuery.toDisplay(pt);
                loadQueryMenu.setLocation(pt.x, pt.y);
                loadQueryMenu.setVisible(true);
            }
        });


        // this MenuItem Listener gets called when a MenuItem was selected
        loadQueryListener = new Listener() {

            public void handleEvent(Event event) {
                Map<String, File[]> filesMap = ScriptModule.getInstance().getScriptFiles();
                String widgetText = ((MenuItem) event.widget).getText();
                for (String key : filesMap.keySet()) {
                    File[] files = filesMap.get(key);
                    for (int i = 0; i < files.length; i++) {
                        if (files[i].getName().equals(widgetText + ".stmt")) {
                            StringBuffer buffer = new StringBuffer();
                            try {
                                FileReader in = new FileReader(files[i]);
                                int bytesRead = 0;
                                char[] textRead = new char[512];
                                while ((bytesRead = in.read(textRead)) > 0) {
                                    buffer.append(textRead, 0, bytesRead);
                                }
                            } catch (IOException e) {
                                e.printStackTrace(System.out);
                            }
                            selectTimeseries.setText(buffer.toString());
                            selectTimeseries.setCaretOffset(selectTimeseries.getText().length());
                        }
                    }
                }
            }
        };
        loadQueryScriptList();
        // install listener for all created MenuItems
        for (String key : loadQueryMenuItems.keySet()) {
            List<MenuItem> temp = loadQueryMenuItems.get(key);
            for (MenuItem a : temp) {
                a.addListener(SWT.Selection, loadQueryListener);
            }
        }
        explain.addListener(SWT.Selection, new Listener() {

            public void handleEvent(Event event) {
                ResultSet rs = JDBCInterface.getInstance().executeStatementWithResult("Explain " + selectTimeseries.getText());
                jgraph.refresh();
                jgraph.getJGraph().getModel().beginUpdate();
                QueryPlanViewerTools.initializeStyles(jgraph.getJGraph(), jgraph.getSize().x / 2 - 100);
                QueryPlanViewerTools.createTreeModel(rs, jgraph.getJGraph(), jgraph.getSize().x / 2 - 100);
                QueryPlanViewerTools.doLayout();
                jgraph.getJGraph().getModel().endUpdate();



            }
        });

        collapsed.addListener(SWT.Selection, new Listener() {

            public void handleEvent(Event event) {
                PostgresPlanNode.collapsedDefault = ((Button) event.widget).getSelection();
            }
        });
        equalsize.addListener(SWT.Selection, new Listener() {

            public void handleEvent(Event event) {
                QueryPlanViewerTools.equalsize = ((Button) event.widget).getSelection();
            }
        });
        loadQueryListener = new Listener() {

            public void handleEvent(Event event) {
                selectTimeseries.setText(((MenuItem) event.widget).getText());
                selectTimeseries.setCaretOffset(selectTimeseries.getText().length());
            }
        };




    }

    /**
     * this convenience method loads the different stmt scripts, creates the menuitems and install the LoadQueryListener
     */
    private void loadQueryScriptList() {
        // load query PopUp menu
        loadQueryMenu = new Menu(this.getShell(), SWT.POP_UP | SWT.DROP_DOWN);
        loadQueryMenuItems = new HashMap<String, List<MenuItem>>();
        Map<String, File[]> filesMap = ScriptModule.getInstance().getScriptFiles();
        //Für jeden Ordner
        for (String key : filesMap.keySet()) {
            //Files im Ordner
            File[] files = filesMap.get(key);
            Menu submenu = new Menu(this.getShell(), SWT.DROP_DOWN);
            MenuItem tempMI = new MenuItem(loadQueryMenu, SWT.CASCADE);
            tempMI.setText(key);

            tempMI.setMenu(submenu);
            ArrayList<MenuItem> tempList = new ArrayList<MenuItem>();

            for (int i = 0; i < files.length; i++) {
                MenuItem temp2 = new MenuItem(submenu, SWT.PUSH);

                temp2.setText(files[i].getName().substring(0, files[i].getName().indexOf(".stmt")));
                tempList.add(temp2);
            }
            loadQueryMenuItems.put(key, tempList);
        }

    }

    @Override
    protected void updateComponent() {
        // TODO Auto-generated method stub
    }

    @Override
    public void reset() {
        // TODO Auto-generated method stub
    }
}
