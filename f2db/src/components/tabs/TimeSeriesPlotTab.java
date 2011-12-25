package components.tabs;

import org.eclipse.swt.graphics.Color;
import java.util.List;

import modules.chart.XYLineChartAdapter;
import modules.config.Configuration;
import modules.databaseif.ConnectionInfo;
import modules.databaseif.History;
import modules.databaseif.HistoryEntry;
import modules.databaseif.JDBCInterface;
import modules.generic.DemoEvents;
import modules.generic.GenericModelChangeEvent;
import modules.generic.GenericModelChangeListener;
import modules.misc.ResourceRegistry;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StyleRange;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.custom.VerifyKeyListener;
import org.eclipse.swt.events.FocusListener;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Spinner;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Text;
import org.jfree.data.xy.XYSeries;

import components.AbstractComponent;
import components.dialogs.AddEditConnectionDialog;
import components.dialogs.EditOptimConfs;
import components.dialogs.ExceptionDialog;
import components.listeners.AbstractLineStyleListener;
import components.listeners.ChangeModelParameterListener;
import components.listeners.DefaultLineStyleListener;
import components.listeners.GenericStyleListener;

import components.listeners.SQLKeywordLineStyleListener;
import components.listeners.UpperKeyListener;

import data.type.Tuple;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Scanner;
import java.util.Map;
import modules.databaseif.ScriptModule;
import modules.misc.Constants;
import org.eclipse.swt.events.VerifyEvent;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;

/**
 * ***********************************************
 * NOTE: NEW FOR MY DIPLOM
 * @author Sebastian Seifert, Christopher Schildt
 * ***********************************************
 * 
 * This tab shows a plot of a timeseries
 *
 * @author Sebastian Seifert, Christopher Schildt
 */
public class TimeSeriesPlotTab extends AbstractComponent implements GenericModelChangeListener {


    private static TimeSeriesPlotTab instance;
    // GUI
    private StyledText selectTimeseries;
    StyledText forecastText;
    private Text parameters;
    private Spinner numberOfForecastValues;
    private Combo meassurecolumn;
    private Combo timecolumn;
    private Combo algorithms;
    private Combo storage;
    private Button execute;
    private Button optprobs;
    private Button parahelp;
    private XYLineChartAdapter chartAdapter;
    private XYSeries seriesOrignialTimeseries;
    private XYSeries seriesForecastTimeseries;
    // data
    private String stmtText;
	private Label elapsedTimeLabel1;
private Label elapsedTimeLabel2;
    private boolean timeseriesOrForecast;
	private double elapsedNano;
    private double xMin;
    private Group tools2;
    private Button loadQuery;
    private Button saveQuery;
    private Button cleanConsole;
    private Menu loadQueryMenu;
    private HashMap<String, List<MenuItem>> loadQueryMenuItems;
    private Listener loadQueryListener;

    public Combo getAlgorithms() {
        return algorithms;
    }

    public Combo getMeassurecolumn() {
        return meassurecolumn;
    }

    public Spinner getNumberOfForecastValues() {
        return numberOfForecastValues;
    }

    public Text getParameters() {
        return parameters;
    }

    public StyledText getSelectTimeseries() {
        return selectTimeseries;
    }

    public Combo getStorage() {
        return storage;
    }

    public Combo getTimecolumn() {
        return timecolumn;
    }


    public static TimeSeriesPlotTab getInstance(Composite parent, int style)
    {
        if(instance==null)
            instance=new TimeSeriesPlotTab(parent, style);
        return instance;
    }

    private TimeSeriesPlotTab(Composite parent, int style) {
        super(parent, style);
    }

    /** inits the GUI components */
    protected void initComponents() {
        loadQueryScriptList();
        setLayout(new GridLayout(8, false));
        selectTimeseries = new StyledText(this, SWT.BORDER | SWT.V_SCROLL | SWT.H_SCROLL);
        {
            GridData ConnectionTextLData = new GridData();
            ConnectionTextLData.horizontalSpan = 6;
            ConnectionTextLData.heightHint = 100;
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


        tools2 = new Group(this, SWT.None);
        tools2.setLayout(new GridLayout(1, false));
        GridData TL2Data = new GridData();
        TL2Data.horizontalSpan = 2;
        TL2Data.grabExcessHorizontalSpace = true;
        TL2Data.horizontalAlignment = GridData.FILL;
        TL2Data.verticalAlignment = GridData.FILL;
        tools2.setLayoutData(TL2Data);
        {
            loadQuery = new Button(tools2, SWT.PUSH);
            TL2Data = new GridData();
            TL2Data.horizontalSpan = 1;
            TL2Data.grabExcessHorizontalSpace = true;
            TL2Data.horizontalAlignment = GridData.FILL;
            TL2Data.verticalAlignment = GridData.FILL;
            loadQuery.setLayoutData(TL2Data);
            loadQuery.setText(Constants.console_toolButtonNames[1]);
            loadQuery.setEnabled(true);
            loadQuery.pack();
        }
        {
            saveQuery = new Button(tools2, SWT.PUSH);
            TL2Data = new GridData();
            TL2Data.horizontalSpan = 1;
            TL2Data.grabExcessHorizontalSpace = true;
            TL2Data.horizontalAlignment = GridData.FILL;
            TL2Data.verticalAlignment = GridData.FILL;
            saveQuery.setLayoutData(TL2Data);
            saveQuery.setText(Constants.console_toolButtonNames[2]);
            saveQuery.setEnabled(true);
            saveQuery.pack();
        }
        Label separator = new Label(tools2, SWT.SEPARATOR | SWT.HORIZONTAL);
        TL2Data = new GridData();
        TL2Data.heightHint = 20;
        TL2Data.grabExcessHorizontalSpace = true;
        TL2Data.horizontalAlignment = GridData.FILL;
        TL2Data.verticalAlignment = GridData.FILL;
        separator.setLayoutData(TL2Data);
        {
            optprobs = new Button(tools2, SWT.None);
            TL2Data = new GridData();
            TL2Data.horizontalSpan = 1;
            TL2Data.grabExcessHorizontalSpace = true;
            TL2Data.horizontalAlignment = GridData.FILL;
            TL2Data.verticalAlignment = GridData.FILL;
            optprobs.setLayoutData(TL2Data);
            optprobs.setText("OptimProperties");
        }
        //Control elements




         forecastText = new StyledText(this, SWT.BORDER | SWT.V_SCROLL | SWT.H_SCROLL);
        {
            GridData ConnectionTextLData = new GridData();
            ConnectionTextLData.horizontalSpan = 6;
            ConnectionTextLData.heightHint = 60;
            ConnectionTextLData.grabExcessHorizontalSpace = true;
            ConnectionTextLData.horizontalAlignment = GridData.FILL;
            ConnectionTextLData.verticalAlignment = GridData.FILL;
            forecastText.setLayoutData(ConnectionTextLData);
            forecastText.setEnabled(true);
            forecastText.setEditable(false);
           
        }

         StyleRange sqlInputSR2 = MiscUtils.loadStyleRangeFromConfiguration("console.input.fg.sql");
        StyleRange[] inputSRanges2 = new StyleRange[1];

        // get the configuration for the SQLLineStyleListener
        inputSRanges2[0] = MiscUtils.loadStyleRangeFromConfiguration("console.input.fg.default");
        forecastText.addLineStyleListener(new GenericStyleListener(
                new SQLKeywordLineStyleListener(
                new DefaultLineStyleListener(),
                sqlInputSR),
                inputSRanges));

elapsedTimeLabel1=new Label(this,SWT.None);
elapsedTimeLabel1.setText("Elapsed Time:");
        //Label fillerLabel=new Label(this,SWT.None);
        {
            GridData ConnectionTextLData = new GridData();
            ConnectionTextLData.horizontalSpan = 2;
            ConnectionTextLData.grabExcessHorizontalSpace = true;
            ConnectionTextLData.horizontalAlignment = GridData.FILL;
            ConnectionTextLData.verticalAlignment = GridData.FILL;
            elapsedTimeLabel1.setLayoutData(ConnectionTextLData);
        }


        Label typeLabel;
        GridData typeLabelLData;


        typeLabel = new Label(this, SWT.NONE);
        typeLabelLData = new GridData();
        typeLabelLData.horizontalAlignment = GridData.FILL;
        typeLabel.setLayoutData(typeLabelLData);
        typeLabel.setText("MeassureCol:");

        meassurecolumn = new Combo(this, SWT.DROP_DOWN);





        typeLabel = new Label(this, SWT.NONE);
        typeLabelLData = new GridData();
        typeLabelLData.horizontalAlignment = GridData.FILL;
        typeLabel.setLayoutData(typeLabelLData);
        typeLabel.setText("TimeCol:");
        timecolumn = new Combo(this, SWT.DROP_DOWN);



        typeLabel = new Label(this, SWT.NONE);
        typeLabelLData = new GridData();
        typeLabelLData.horizontalAlignment = GridData.FILL;
        typeLabel.setLayoutData(typeLabelLData);
        typeLabel.setText("Algorithm:");
        algorithms = new Combo(this, SWT.DROP_DOWN);
        algorithms.add("HWMODEL");
        algorithms.add("ARMODEL");





        typeLabel = new Label(this, SWT.NONE);
        typeLabelLData = new GridData();
        typeLabelLData.horizontalAlignment = GridData.FILL;
        typeLabel.setLayoutData(typeLabelLData);
        typeLabel.setText("  # of forecasts :");
        numberOfForecastValues = new Spinner(this, SWT.BORDER);
        numberOfForecastValues.setMinimum(0);
        numberOfForecastValues.setMaximum(1000);
        numberOfForecastValues.setSelection(10);
        numberOfForecastValues.setIncrement(1);


        typeLabel = new Label(this, SWT.NONE);
        typeLabelLData = new GridData();
        typeLabelLData.horizontalAlignment = GridData.FILL;
        typeLabel.setLayoutData(typeLabelLData);
        typeLabel.setText("Parameters:");
        parameters = new Text(this, SWT.BORDER | SWT.V_SCROLL | SWT.H_SCROLL);

        GridData ConnectionTextLData = new GridData();
        ConnectionTextLData.horizontalSpan = 3;

        ConnectionTextLData.grabExcessHorizontalSpace = true;
        ConnectionTextLData.horizontalAlignment = GridData.FILL;
        ConnectionTextLData.verticalAlignment = GridData.FILL;
        parameters.setLayoutData(ConnectionTextLData);

        typeLabel = new Label(this, SWT.NONE);
        typeLabelLData = new GridData();
        typeLabelLData.horizontalAlignment = GridData.FILL;
        typeLabel.setLayoutData(typeLabelLData);
        typeLabel.setText("Storage:");
        storage = new Combo(this, SWT.DROP_DOWN);
        storage.add("OFF");
        storage.add("TABLE");
        storage.add("MODELGRAPH");
        storage.add("MODELINDEX");

        algorithms.addSelectionListener(new ChangeModelParameterListener(parameters));





        execute = new Button(this, SWT.FLAT);
        //execute.setBackground(new Color(execute.getBackground().getDevice(),230,255,255));
        ConnectionTextLData = new GridData();
        ConnectionTextLData.horizontalSpan = 2;
        ConnectionTextLData.grabExcessHorizontalSpace = true;
        ConnectionTextLData.horizontalAlignment = GridData.FILL;
        ConnectionTextLData.verticalAlignment = GridData.FILL;
        execute.setLayoutData(ConnectionTextLData);
        execute.setText("Execute!");

        chartAdapter = new XYLineChartAdapter(this, SWT.NONE, false);
        chartAdapter.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 11, 1));

        seriesOrignialTimeseries = new XYSeries("timeseries", false, true);
        chartAdapter.addSeries(seriesOrignialTimeseries);

        seriesForecastTimeseries = new XYSeries("forecast", false, true);
        chartAdapter.addSeries(seriesForecastTimeseries);
         forecastText.setText(createForecastText());
    }

    /** inits the listeners */
    protected void initListeners() {
        Listener updateListenerBS = new Listener() {

            public void handleEvent(Event event) {
                updateCombosStatement();
            }
        };
        // add Listener which listens to special chars
        // to handle history functions, and execute statements
        selectTimeseries.addVerifyKeyListener(new VerifyKeyListener() {

            private int currentHistoryIndex;

            public void verifyKey(VerifyEvent event) {

                // CTRL + Return
                if ((event.keyCode == 13) && (event.stateMask == 262144)) {
                    stmtText = selectTimeseries.getText();
                    timeseriesOrForecast = true;

                    JDBCInterface.getInstance().executeStatement(stmtText, 0, true, true, false, false);

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
        // open up a Dialog asking for the filename to save the current query of the inputconsole
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
                Point pt = new Point(rect.x, rect.y + rect.height);
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

			Map<String, File[]> filesMap2 = ScriptModule.getInstance().getFCScriptFiles();
			for (String key : filesMap2.keySet()) {
                    		File[] files2 = filesMap2.get(key);
                    		for (int i = 0; i < files2.length; i++) {
                        	if (files2[i].getName().equals(widgetText + ".fcstmt")) {
Scanner scanner=null;
try{
 				scanner = new Scanner (files2[i]);
				}
catch(Exception e)
{
return;
}

				String line=scanner.nextLine();
				String[] temp=line.split("#");
System.out.println(temp.toString()+":"+temp.length);
				if(temp.length<5) break;
				meassurecolumn.setText(temp[0]);
				timecolumn.setText(temp[1]);
				algorithms.setText(temp[2]);
				parameters.setText(temp[3]);
				storage.setText(temp[4]);

				


                }
            }
        }
 }};

        // install listener for all created MenuItems
        for (String key : loadQueryMenuItems.keySet()) {
            List<MenuItem> temp = loadQueryMenuItems.get(key);
            for (MenuItem a : temp) {
                a.addListener(SWT.Selection, loadQueryListener);
            }
        }

        //add the system catalog tab as listener to the JDBCInterface(Model)
        JDBCInterface.getInstance().addModelChangeListener(this);
        selectTimeseries.addListener(SWT.Modify, updateListenerBS);
        execute.addListener(SWT.Selection, new Listener() {

            public void handleEvent(Event event) {

                stmtText = selectTimeseries.getText();
                timeseriesOrForecast = true;

                JDBCInterface.getInstance().executeStatement(stmtText, 0, true, true, false, false);

            }
        });

        optprobs.addListener(SWT.Selection, new Listener() {

            public void handleEvent(Event e) {
                EditOptimConfs dialog =
                        new EditOptimConfs(getShell(), SWT.NONE);
                dialog.open("Edit Optim Parameters");
            }
        });



        meassurecolumn.addListener(SWT.Modify, new Listener() {

            public void handleEvent(Event e) {
                 forecastText.setText(createForecastText());
            }
        });
        timecolumn.addListener(SWT.Modify, new Listener() {

            public void handleEvent(Event e) {
                 forecastText.setText(createForecastText());
            }
        });
         algorithms.addListener(SWT.Modify, new Listener() {

            public void handleEvent(Event e) {
                 forecastText.setText(createForecastText());
            }
        });
         storage.addListener(SWT.Modify, new Listener() {

            public void handleEvent(Event e) {
                 forecastText.setText(createForecastText());
            }
        });
         parameters.addListener(SWT.Modify, new Listener() {

            public void handleEvent(Event e) {
                 forecastText.setText(createForecastText());
            }
        });

        updateCombosStatement();
    }

    /**
     * this method checks, if the last executed query was the query from this tab.
     * if we were right, get the results from that query
     */
    private void checkResult() {
        // get last executed result
        HistoryEntry entry = History.getInstance().getLastEntry();
        boolean exp=false;
        String sqltext = entry.getSQL();

        // do we have the correct result
        if (sqltext.equals(stmtText)) {

            List<Tuple> tuples;

            if (entry.getQueryResult() == null) {
                return;
            }


            tuples = entry.getQueryResult().getTuples();

            double[][] data = new double[tuples.size()][2];

            if (tuples.size() <= 0) {
                ExceptionDialog.show_string(null, "Query returned 0 items", timeseriesOrForecast);
                return;
            }
            boolean two = tuples.get(0).size() == 2;

            int i = 0;
            for (Tuple t : tuples) {

                if (two) {
                    data[i][0] = t.getValue(0).getLong();
                } else {
                    data[i][0] = i + 1;
                }

                data[i][1] = t.getValue(two ? 1 : 0).getDouble();

                i++;
            }


            if (timeseriesOrForecast) {

                seriesOrignialTimeseries.clear();
                seriesForecastTimeseries.clear();
                chartAdapter.setXRange(data[0][0], data[data.length - 1][0]);

                Thread t = Thread.currentThread();
                long lastRefreshTime = System.currentTimeMillis();
                for (i = 0; i < data.length; i++) {
                    if (t.isInterrupted()) {
                        break;
                    }
                    seriesOrignialTimeseries.add(data[i][0], data[i][1], false);
                    if (System.currentTimeMillis() - lastRefreshTime > 500 /*0.5 second*/) {
                        seriesOrignialTimeseries.fireSeriesChanged();
                        lastRefreshTime = System.currentTimeMillis();
                    }
                }
                seriesOrignialTimeseries.fireSeriesChanged();
                xMin = data[0][0];

                if (numberOfForecastValues.getSelection() != 0) {
                    if (JDBCInterface.getInstance().isPostgres_connected()) {
                        stmtText = selectTimeseries.getText();
                        stmtText += " FORECAST  " + meassurecolumn.getText() + " on " + timecolumn.getText() + " NUMBER " + numberOfForecastValues.getSelection()
                                + " ALGORITHM " + algorithms.getText();
                        stmtText+=(!parameters.getText().equals("")) ? " \nPARAMETERS  " + parameters.getText() :  "";
                        stmtText+=" STORAGE " + storage.getText();
                    } else {
                        stmtText = selectTimeseries.getText();
                        stmtText += " FORECAST  " + numberOfForecastValues.getSelection() + " VALUES ON t";
                    }

                    timeseriesOrForecast = false;

			//time meassure
                    long nanostart=System.nanoTime();
                    JDBCInterface.getInstance().executeStatement(stmtText, 0, false, true, true, false);
                    this.elapsedNano=((System.nanoTime()-nanostart)/1000000.0);
                    System.out.println("time:"+elapsedNano+"ms");
                    elapsedTimeLabel1.setText("Elapsed Time:\n"+elapsedNano+"ms");
                }
            } else {
               
                seriesForecastTimeseries.clear();
                chartAdapter.setXRange(Math.min(xMin, data[0][0]), data[data.length - 1][0]);

                Thread t = Thread.currentThread();
                long lastRefreshTime = System.currentTimeMillis();
                seriesForecastTimeseries.add(seriesOrignialTimeseries.getDataItem(seriesOrignialTimeseries.getItemCount()-1).getX(), seriesOrignialTimeseries.getDataItem(seriesOrignialTimeseries.getItemCount()-1).getY(), false);
                for (i = 0; i < data.length; i++) {
                    if (t.isInterrupted()) {
                        break;
                    }
                     if(!exp && data[i][1]>1000000)
                     {
                         exp=true;
                         chartAdapter.setexpAx(true);
                    }
                    seriesForecastTimeseries.add(data[i][0], data[i][1], false);
                    if (System.currentTimeMillis() - lastRefreshTime > 500 /*0.5 second*/) {
                        seriesForecastTimeseries.fireSeriesChanged();
                        lastRefreshTime = System.currentTimeMillis();
                    }
                }
                if(!exp)
                    chartAdapter.setexpAx(false);
                seriesForecastTimeseries.fireSeriesChanged();
            }

        }
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

    /** this method gets called when a connection was closed */
    protected void deactivateControls() {
        if (!isDisposed()) {
            selectTimeseries.setEnabled(false);
            optprobs.setEnabled(false);
            execute.setEnabled(false);
            meassurecolumn.setEnabled(false);
            timecolumn.setEnabled(false);
            algorithms.setEnabled(false);
            parameters.setEnabled(false);
            optprobs.setEnabled(false);
            storage.setEnabled(false);
        }
    }

    /** this method gets called when a connection was established */
    protected void activateControls() {
        if (!isDisposed()) {
            selectTimeseries.setEnabled(true);

            execute.setEnabled(true);
            if (JDBCInterface.getInstance().isPostgres_connected()) {
                meassurecolumn.setEnabled(true);
                timecolumn.setEnabled(true);
                algorithms.setEnabled(true);
                parameters.setEnabled(true);
                optprobs.setEnabled(true);
                storage.setEnabled(true);
            }
        }
    }

    /** this method gets called when the user pressed the tab title */
    protected void updateComponent() {
        // do nothing
    }

    public void reset() {
        // do nothing
    }

    // -------- GenericModelChangeListener implementation -------
    /** gets called when a model has changed */
    public void modelChanged(GenericModelChangeEvent event) {
        switch (event.detail) {
            case DemoEvents.CONNECTION_ESTABLISHED:
                activateControls();
                break;
            case DemoEvents.CONNECTION_CLOSED:
                deactivateControls();
                break;
            case DemoEvents.RESULT_READY:
                checkResult();
                break;
            default:
                break;
        }
    }


    public void updateCombosStatement() {
        int m = meassurecolumn.getSelectionIndex();
        meassurecolumn.removeAll();
        int t = timecolumn.getSelectionIndex();
        timecolumn.removeAll();
        String textValue = selectTimeseries.getText().toUpperCase();
        if (!textValue.startsWith(("SELECT"))) {
            meassurecolumn.add("INVALID");
            timecolumn.add("INVALID");
            return;
        }

        try{
        textValue = textValue.substring(7);
        }
        catch(Exception e)
        {
            return; //deleted to fast ;-)
        }
        String[] pos = textValue.split("[,]");
        boolean breaker = false;
        for (String s : pos) {
            if (breaker) {
                return;
            }
            if (s.contains("FROM")) {
                int fromInd = s.indexOf("FROM");
                s = s.substring(0, fromInd);
                breaker = true;
            }
            String s2 = s.toUpperCase().trim();
            if (s2.contains(" ") || s2.contains("AS"))//Alias
            {
                String[] pos2 = s2.split("[\\s]");
                this.meassurecolumn.add(pos2[pos2.length - 1].trim());
                this.timecolumn.add(pos2[pos2.length - 1].trim());
                continue;
            }
            if (s.trim().length() < 1) {
                continue;
            }
            this.meassurecolumn.add(s.trim());
            this.timecolumn.add(s.trim());

        }
        if (meassurecolumn.getItems().length <= 0) {
            meassurecolumn.add("INVALID");
            timecolumn.add("INVALID");
        }

     
        if (meassurecolumn.getItemCount() >= m && m >= 0) {
            meassurecolumn.select(m);
        }

        if (timecolumn.getItemCount() >= t && t >= 0) {
            timecolumn.select(t);
        }
    }

    private String createForecastText() {
        String stmt="";
         if (JDBCInterface.getInstance().isPostgres_connected()) {
                        stmt=(meassurecolumn.getText()!=null) ? stmt+ "FORECAST  " + meassurecolumn.getText() : stmt+ "FORECAST   ";
                        stmt=(timecolumn.getText()!=null) ? stmt+ " ON  " + timecolumn.getText() : stmt+ " ON   ";
                        stmt=(numberOfForecastValues.getSelection()!=0) ? stmt+ " NUMBER  " + numberOfForecastValues.getSelection() : stmt+ " NUMBER   ";
                        stmt=(algorithms.getText()!=null) ? stmt+ " ALGORITHM  " + algorithms.getText() : stmt+ " ALGORITHM   ";
                         stmt=(!parameters.getText().equals("")) ? stmt+ " \nPARAMETERS  " + parameters.getText() : stmt+ "";
                          stmt=(storage.getText()!=null) ? stmt+ " \nSTORAGE  " + storage.getText() : stmt+ " \nSTORAGE   ";

                    } else {
                        stmt = "";
                    }

    
    return stmt;
    }

}
