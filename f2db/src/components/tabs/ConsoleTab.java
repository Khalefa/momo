package components.tabs;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.sql.SQLWarning;

import modules.config.Configuration;
import modules.databaseif.History;
import modules.databaseif.HistoryEntry;
import modules.databaseif.JDBCInterface;
import modules.databaseif.ScriptModule;
import modules.generic.DemoEvents;
import modules.generic.GenericModelChangeEvent;
import modules.generic.GenericModelChangeListener;
import modules.misc.Constants;
import modules.misc.ResourceRegistry;
import modules.timeseries.TimeseriesImporter;

import org.eclipse.jface.dialogs.InputDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StyleRange;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.custom.VerifyKeyListener;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.VerifyEvent;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.FormAttachment;
import org.eclipse.swt.layout.FormData;
import org.eclipse.swt.layout.FormLayout;
import org.eclipse.swt.layout.RowLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.swt.widgets.MessageBox;
import org.eclipse.swt.widgets.Sash;
import org.eclipse.swt.widgets.Text;
import org.eclipse.swt.widgets.ToolBar;
import org.eclipse.swt.widgets.ToolItem;

import components.AbstractComponent;
import components.dialogs.CreateModelDialog;
import components.dialogs.ExceptionDialog;
import components.dialogs.WaitingDialog;
import components.listeners.DefaultLineStyleListener;
import components.listeners.GenericBGColorListener;
import components.listeners.GenericResizeListener;
import components.listeners.GenericStyleListener;
import components.listeners.SQLKeywordLineStyleListener;
import components.listeners.UpperKeyListener;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * This Tab is the center component of the whole demo. It allows the user to execute 
 * statements and to view their results. The user is able to create samples and to
 * run scripts. He/She is also able to review and to re-execute former statements, which
 * were recorded in a history.
 * @author Felix Beyer, Christopher Schildt
 * @date   30.04.2006
 *
 */
public class ConsoleTab extends AbstractComponent implements GenericModelChangeListener {

    // main user interface widgets
    private Composite parent;				// the reference to the parent of this tab
    private StyledText mainST;				// the output area of the console
    private StyledText inputST;				// the input area of the console
    private Sash sash;						// the sash component, used for resizing input and output regions
    private Composite tools;				// the tools composite
    private ToolBar tools2;					// the toolbar
    private ToolItem createModel;			// the toolitem for the create sample dialog
    private ToolItem loadQuery;				// the toolitem for the load query menu
    private ToolItem saveQuery;				// the toolitem for the save query dialog
    private ToolItem cleanConsole;			// the toolitem to clean the console output
    private ToolItem loadTimeseries;		// the toolitem for the load timeseries menu
    private Composite limitComposite;		// the composite for the limit text box
    private Button limitRowsCheckbox;		// the limit result rows checkbox
    private Text limitText;					// the text box
    private Menu loadQueryMenu;				// the load query menu
    private Map<String, List<MenuItem>> loadQueryMenuItems;	// the load query menu toolitems
    private Listener loadQueryListener;		// the load query listener
    private Menu loadTimeseriesMenu;				// the load timeseries menu
    private MenuItem[] loadTimeseriesMenuItems;	// the load timeseries menu toolitems
    private Listener loadTimeseriesListener;		// the load timeseries listener
    // the formdata attachment, used by the sash component to apply the resize
    private FormData mainGD;
    // the background color listener for the output console
    private GenericBGColorListener outputBGColorListener;
    // the SQL Keyword listener for the output console
    private GenericStyleListener outputListener;
    // the SQLLineStyleListener for the output area
    private SQLKeywordLineStyleListener sqlListener;
    // the UpperKeyListener for the input area
    private UpperKeyListener inputUpperKeyListener;
    // the background resize listeners for the main and input Styledtext widgets
    private GenericResizeListener bgListenerMain;
    private GenericResizeListener bgListenerInput;
    // flag which is true if statements were recognized by the application	
    private boolean statementsReceived = false;
    // default 0, ->history is empty at startup
    private int currentHistoryIndex = 0;

    /**
     * the constructor simply initializes the components and the listeners
     * @param parent the parent widget
     * @param style the style of this composite widget,
     * which contains all ui widgets of this tab; usually SWT.NONE
     */
    public ConsoleTab(Composite parent, int style) {
        super(parent, style);
        this.parent = parent;
        loadConfiguration();
    }

    /**
     * convenience method to add a background decoration, a GenericResizeListener 
     * @param key specifies the config information base key, e.g. console.output.bg 
     * @param st the Styledtext widget to assign this new created listener 
     * @return new pre-configured listener to apply
     */
    private GenericResizeListener addBGDecoration(String key, StyledText st) {

        GenericResizeListener listener = new GenericResizeListener();

        listener.parent = st;

        listener.useImage = Boolean.parseBoolean(Configuration.getInstance().getProperty(key + ".showLogo"));
        listener.type = Configuration.getInstance().getProperty(key + ".type");
        listener.bgColor = ResourceRegistry.getInstance().getColor(Configuration.getInstance().getProperty(key + ".color"));
        listener.gradienttype = Configuration.getInstance().getProperty(key + ".gradient.type");
        listener.one = ResourceRegistry.getInstance().getColor(Configuration.getInstance().getProperty(key + ".gradient.fromcolor"));
        listener.two = ResourceRegistry.getInstance().getColor(Configuration.getInstance().getProperty(key + ".gradient.tocolor"));

        listener.setup();
        //listener.decorateImage = ResourceRegistry.getInstance().getImage("smalllogo");

        return listener;
    }

    /**
     * This method loads the config information stored in the Configuration class.
     */
    private void loadConfiguration() {

        // clean contents
        inputST.setText(""); // clear example input console
        inputST.setCaretOffset(0); // reset caret position

        mainST.setText(""); // clear example input console
        mainST.setCaretOffset(0); // reset caret position

        // set default fonts
        mainST.setFont(ResourceRegistry.getInstance().getFont(
                Configuration.getInstance().getProperty("console.output.fg.default.fg.font")));

        inputST.setFont(ResourceRegistry.getInstance().getFont(
                Configuration.getInstance().getProperty("console.input.fg.default.fg.font")));

        inputST.setForeground(ResourceRegistry.getInstance().getColor(
                Configuration.getInstance().getProperty("console.input.fg.default.fg.fgcolor")));

        // format the output console
        // 1st create the different StyleRanges to format the output console

        // create Stylerange array to hold the different style ranges
        StyleRange[] srs = new StyleRange[4];

        srs[0] = MiscUtils.loadStyleRangeFromConfiguration("console.output.stmts.fg");
        srs[1] = MiscUtils.loadStyleRangeFromConfiguration("console.output.results.fg");
        srs[2] = MiscUtils.loadStyleRangeFromConfiguration("console.output.warnings.fg");
        srs[3] = MiscUtils.loadStyleRangeFromConfiguration("console.output.exceptions.fg");

        // create one more StyleRange to format the SQL Text in the output console
        StyleRange sqlTextSR = MiscUtils.loadStyleRangeFromConfiguration("console.output.fg.sql");

        // now create the decorated listeners, which are responsible for the formatting
        // the order of creation is important for fallback strategies

        // this is the main outer listener which gets called first 
        outputListener = new GenericStyleListener(new DefaultLineStyleListener(), srs);

        // if sql formatting is on create a wrapping SQLListener
        boolean sqlon = Boolean.parseBoolean(Configuration.getInstance().getProperty("console.output.fg.formatSQL92"));
        if (sqlon) {
            sqlListener = new SQLKeywordLineStyleListener(outputListener, sqlTextSR);

            // toggle cutoffendchar on, default after creation is off!
            sqlListener.switchCutOffChar();

            // Add the decorated listener to the widget 
            mainST.addLineStyleListener(sqlListener);
        } else {
            // only add the generic listener
            mainST.addLineStyleListener(outputListener);
        }

        // Now format the bg coloring of the output console

        // get the four colors which format the background  
        Color[] colors = new Color[4];
        colors[0] = ResourceRegistry.getInstance().getColor(
                Configuration.getInstance().getProperty("console.output.stmts.fg.bgcolor"));
        colors[1] = ResourceRegistry.getInstance().getColor(
                Configuration.getInstance().getProperty("console.output.results.fg.bgcolor"));
        colors[2] = ResourceRegistry.getInstance().getColor(
                Configuration.getInstance().getProperty("console.output.warnings.fg.bgcolor"));
        colors[3] = ResourceRegistry.getInstance().getColor(
                Configuration.getInstance().getProperty("console.output.exceptions.fg.bgcolor"));

        // Create a new GenericBGColorListener, to handle the color information
        outputBGColorListener = new GenericBGColorListener(colors);

        // And add this to the output console
        mainST.addLineBackgroundListener(outputBGColorListener);

        // finally add a Background decoration for the output console

        mainST.setBackground(null);
        mainST.setBackgroundImage(null);

        if (bgListenerMain != null) {
            mainST.removeListener(SWT.Resize, bgListenerMain);
        }
        bgListenerMain = addBGDecoration("console.output.bg", mainST);
        mainST.addListener(SWT.Resize, bgListenerMain);


        // add this new listener to the widget 
        mainST.addListener(SWT.Resize, bgListenerMain);

        // add a DefaultLineStyleListener, decorated with a default SQLLinestyleListener 
        // to the input console
        StyleRange[] inputSRanges = new StyleRange[1];

        // get the configuration for the SQLLineStyleListener 
        inputSRanges[0] = MiscUtils.loadStyleRangeFromConfiguration("console.input.fg.default");

        // check to see if the input shoult be formatted with SQL Highlightning
        boolean result = Boolean.parseBoolean(Configuration.getInstance().getProperty("console.input.fg.formatSQL92"));

        // setup the StyleRange to use for SQL Keyword formatting for the input console
        StyleRange sqlInputSR = MiscUtils.loadStyleRangeFromConfiguration("console.input.fg.sql");

        // add the different decorated LineStyleListeners to the input console

        inputST.addLineStyleListener(new GenericStyleListener(
                new SQLKeywordLineStyleListener(
                new DefaultLineStyleListener(),
                sqlInputSR),
                inputSRanges));

        // add an Upper-Key-SQL-Keyword-Transformer if flag is set 
        result = Boolean.parseBoolean(Configuration.getInstance().getProperty("console.input.fg.transformSQL92"));

        if (result) {
            if (inputUpperKeyListener == null) {
                inputUpperKeyListener = new UpperKeyListener(inputST, true);
                inputST.addVerifyKeyListener(inputUpperKeyListener);
            }
        } else {
            if (inputUpperKeyListener != null) {
                inputST.removeVerifyKeyListener(inputUpperKeyListener);
            }
        }

        // finally add the background decoration for the input console

        if (bgListenerInput != null) {
            // reset the StyledText component
            inputST.setBackground(null);
            inputST.setBackgroundImage(null);
            inputST.removeListener(SWT.Resize, bgListenerInput);
        }
        // create a ResizeListener from the current configured bg decoration
        bgListenerInput = addBGDecoration("console.input.bg", inputST);
        // add this new listener to the widget
        inputST.addListener(SWT.Resize, bgListenerInput);
    }

    /**
     * inits the components of the console
     */
    protected void initComponents() {
        setLayout(new FormLayout());

        // the sash component
        sash = new Sash(this, SWT.BORDER | SWT.HORIZONTAL | SWT.SEPARATOR);

        FormData sashData = new FormData();
        sashData.bottom = new FormAttachment(100, -140);
        sashData.left = new FormAttachment(0, 5);
        sashData.right = new FormAttachment(100, -5);
        sash.setLayoutData(sashData);

        // create StyledText output console
        mainST = new StyledText(this, SWT.BORDER | SWT.V_SCROLL | SWT.H_SCROLL);
        mainST.setText("");
        mainST.setCaretOffset(0);
        mainST.setWordWrap(false);

        mainGD = new FormData();
        mainGD.top = new FormAttachment(0, 5);
        mainGD.left = new FormAttachment(0, 5);
        mainGD.right = new FormAttachment(100, -5);
        mainGD.bottom = new FormAttachment(sash, -5);

        mainST.setLayoutData(mainGD);
        mainST.setEditable(false);
        mainST.setEnabled(false);

        // create StyledText input console
//		ScrolledComposite input = new ScrolledComposite(this, SWT.WRAP);
//		input.setMinHeight(50);
        inputST = new StyledText(this, SWT.WRAP | SWT.BORDER | SWT.V_SCROLL);
        inputST.setText("");

        // custom caret implementation
        // maybe later for customization features
		/*
        Caret c = new Caret(inputST,SWT.NONE);
        c.setImage(ResourceRegistry.getInstance().getImage("caret"));
        inputST.setCaret(c);
         */

        inputST.setCaretOffset(0);

        FormData gd = new FormData();
        gd.top = new FormAttachment(sash, 38);
        gd.left = new FormAttachment(0, 5);
        gd.right = new FormAttachment(100, -5);
        gd.bottom = new FormAttachment(100, -5);

        inputST.setLayoutData(gd);
        inputST.setCaretOffset(0);
        inputST.setEnabled(false);

        tools = new Composite(this, SWT.NONE);
        tools.setLayout(new RowLayout());

        {
            // create the toolbar
            tools2 = new ToolBar(tools, SWT.HORIZONTAL);
        }

        gd = new FormData();
        gd.top = new FormAttachment(sash, 5);
        gd.left = new FormAttachment(0, 5);
        gd.right = new FormAttachment(100, -5);
        gd.bottom = new FormAttachment(inputST, -5);
        tools.setLayoutData(gd);
        tools.setEnabled(false);

        loadQueryScriptList();
        loadTimeseriesList();

        {
            // create ToolItems
            createModel = new ToolItem(tools2, SWT.PUSH);
            //createSample.setImage(ResourceRegistry.getInstance().getImage("createsample"));
            //createSample.setHotImage(ResourceRegistry.getInstance().getImage("createsamplehot"));
            createModel.setText(Constants.console_toolButtonNames[0]);
            createModel.setEnabled(false);
        }
        {
            loadQuery = new ToolItem(tools2, SWT.PUSH);
            //loadQuery.setImage(ResourceRegistry.getInstance().getImage("loadquery"));
            //loadQuery.setHotImage(ResourceRegistry.getInstance().getImage("loadqueryhot"));
            loadQuery.setText(Constants.console_toolButtonNames[1]);
            loadQuery.setEnabled(false);
        }
        {
            saveQuery = new ToolItem(tools2, SWT.PUSH);
            //saveQuery.setImage(ResourceRegistry.getInstance().getImage("savequery"));
            //saveQuery.setHotImage(ResourceRegistry.getInstance().getImage("savequeryhot"));
            saveQuery.setText(Constants.console_toolButtonNames[2]);
            saveQuery.setEnabled(false);
        }
        {
            cleanConsole = new ToolItem(tools2, SWT.PUSH);
            //cleanConsole.setImage(ResourceRegistry.getInstance().getImage("savequery"));
            cleanConsole.setText(Constants.console_toolButtonNames[3]);
            cleanConsole.setEnabled(false);
        }
        {
            loadTimeseries = new ToolItem(tools2, SWT.PUSH);
            loadTimeseries.setText(Constants.console_toolButtonNames[4]);
            loadTimeseries.setEnabled(false);
        }
        {
            limitComposite = new Composite(tools, SWT.NONE);
            limitComposite.setLayout(new FormLayout());
            {
                limitRowsCheckbox = new Button(limitComposite, SWT.CHECK);
                limitRowsCheckbox.setText("limit result rows:");
                limitRowsCheckbox.setSelection(!"0".equals(Configuration.getInstance().getProperty("execution.defaultUpperResultSetRowBound")));
                limitRowsCheckbox.setEnabled(false);

                FormData limitRowsCheckboxFD = new FormData();
                limitRowsCheckboxFD.top = new FormAttachment(0, 3);
                limitRowsCheckboxFD.left = new FormAttachment(0, 5);
                //limitRowsCheckboxFD.bottom   = new FormAttachment(0,2);
                limitRowsCheckbox.setLayoutData(limitRowsCheckboxFD);
            }
            {
                limitText = new Text(limitComposite, SWT.SINGLE | SWT.BORDER);

                FormData limitTextFD = new FormData();
                limitTextFD.top = new FormAttachment(0, 3);
                limitTextFD.left = new FormAttachment(limitRowsCheckbox, 5);
                limitTextFD.width = 50;
                limitText.setLayoutData(limitTextFD);
                limitText.setEnabled(false);
                limitText.setText(Configuration.getInstance().getProperty("execution.defaultUpperResultSetRowBound"));
            }
        }
    }

    /**
     * This method initializes and creates all control listeners for the different ui widgets
     */
    protected void initListeners() {
        // add the console tab as listener to the JDBCInterface(Model)
        JDBCInterface.getInstance().addModelChangeListener(this);
        // add the console tab as listener for the history
        History.getInstance().addModelChangeListener(this);

        // add Listener which listens to special chars
        // to handle history functions, and execute statements
        inputST.addVerifyKeyListener(new VerifyKeyListener() {

            public void verifyKey(VerifyEvent event) {

                // CTRL + Return
                if ((event.keyCode == 13) && (event.stateMask == 262144)) {
                    // check to see if last character was a;
                    // parse in text and split it at ; chars
                    checkForStatements();
                    if (statementsReceived) {
                        event.doit = false;
                        statementsReceived = false;
                    }
                }
                // replace statement with statement in history if keyup or keydown was pressed
                // key up
                if ((event.keyCode == 16777217) && (event.stateMask == 262144)) {
                    int noEntries = History.getInstance().size();
                    cleanInputConsole();
                    String stmt = "";
                    if (currentHistoryIndex > 0 && noEntries > 0) {
                        HistoryEntry entry = History.getInstance().getEntry(currentHistoryIndex - 1);
                        stmt = entry.getSQL();
                    }
                    currentHistoryIndex = Math.max(-1, currentHistoryIndex - 1);
                    inputST.setText(stmt);
                    inputST.setCaretOffset(inputST.getText().length());
                }
                // key down
                if ((event.keyCode == 16777218) && (event.stateMask == 262144)) {
                    int noEntries = History.getInstance().size();
                    cleanInputConsole();
                    String stmt = "";
                    if (currentHistoryIndex < noEntries - 1) {
                        HistoryEntry entry = History.getInstance().getEntry(currentHistoryIndex + 1);
                        stmt = entry.getSQL();
                    }
                    currentHistoryIndex = Math.min(noEntries, currentHistoryIndex + 1);
                    inputST.setText(stmt);
                    inputST.setCaretOffset(inputST.getText().length());
                }
                // for debug, commment this out to resolve key inputs or to resolve char combinations
                //System.out.println("Keycode: "+event.keyCode+";Character: "+event.character+";stateMask: "+event.stateMask);

            }
        });

        // add this Listener to resize the console widgets
        sash.addListener(SWT.Selection, new Listener() {

            public void handleEvent(Event event) {
                //TODO limit minimum width of widgets
                ((FormData) sash.getLayoutData()).bottom = new FormAttachment(100, -(parent.getSize().y - event.y - 38));
                sash.getParent().layout();
            }
        });

        // Open the CreateSampleDialog if user presses the toolbutton
        createModel.addListener(SWT.Selection, new Listener() {

            public void handleEvent(Event event) {
                String result = new CreateModelDialog(parent.getShell()).open();
                if (result != null) {
                    //System.out.println(result.toString());
                    if (result.equals("CANCEL")) {
                        // user canceled the dialog
                    } else {
                        // user did setup a create sample statement
                        // execute it:
                        String stmt = result;
                        JDBCInterface.getInstance().executeStatement(stmt, 0, true, true, true, true);
                    }
                }
            }
        });

        // show the loadquery menu when user presses the toolbutton
        loadQuery.addListener(SWT.Selection, new Listener() {

            public void handleEvent(Event e) {
                Rectangle rect = loadQuery.getBounds();
                Point pt = new Point(rect.x, rect.y + rect.height);
                pt = tools.toDisplay(pt);
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
                            inputST.setText(buffer.toString());
                            inputST.setCaretOffset(inputST.getText().length());
                        }
                    }
                }
            }
        };

        // install listener for all created MenuItems
        for (String key : loadQueryMenuItems.keySet()) {
            List<MenuItem> temp = loadQueryMenuItems.get(key);
            for (MenuItem a : temp) {
                a.addListener(SWT.Selection, loadQueryListener);
            }
        }

        // open up a Dialog asking for the filename to save the current query of the inputconsole
        saveQuery.addListener(SWT.Selection, new Listener() {

            public void handleEvent(Event event) {
                FileDialog fd = new FileDialog(parent.getShell(), SWT.SAVE);
                fd.setFilterExtensions(new String[]{Constants.singleStmtWildcardFilter});
                String result = fd.open();

                if (result != null && result != "") {
                    if (!result.endsWith(".stmt")) {
                        result = result + ".stmt";
                    }
                    ScriptModule.getInstance().saveScriptFile(result, inputST.getText());

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

        // This listener calls the Clean method of the output console
        cleanConsole.addListener(SWT.Selection, new Listener() {

            public void handleEvent(Event event) {
                cleanOutputConsole();
            }
        });

        // This listener calls the Clean method of the output console 
        limitRowsCheckbox.addListener(SWT.Selection, new Listener() {

            public void handleEvent(Event event) {
                limitText.setEnabled(limitRowsCheckbox.getSelection());
            }
        });

        // show the timeseries menu when user presses the toolbutton
        loadTimeseries.addListener(SWT.Selection, new Listener() {

            public void handleEvent(Event e) {
                Rectangle rect = loadTimeseries.getBounds();
                Point pt = new Point(rect.x, rect.y + rect.height);
                pt = tools.toDisplay(pt);
                loadTimeseriesMenu.setLocation(pt.x, pt.y);
                loadTimeseriesMenu.setVisible(true);
            }
        });

        // this MenuItem Listener gets called when a MenuItem was selected
        loadTimeseriesListener = new Listener() {

            public void handleEvent(Event event) {
//				File[] files = ScriptModule.getInstance().getTimeseriesFiles();
                final String widgetText = ((MenuItem) event.widget).getText();
//				for(int i=0;i<files.length;i++){
//					if(files[i].getName().equals(widgetText)){

                int ind = widgetText.lastIndexOf('.');
                String tableNameExample;
                if (ind != -1) {
                    tableNameExample = widgetText.substring(0, ind);
                } else {
                    tableNameExample = widgetText;
                }
                InputDialog dialog = new InputDialog(getShell(), "Tablename?", null, tableNameExample, null);
                dialog.open();

                final String tablename = dialog.getValue();
                if (tablename != null && tablename.length() != 0) {
                    WaitingDialog.show(getShell(), new Runnable() {

                        public void run() {
                            try {
                                TimeseriesImporter.importTimeseries(JDBCInterface.getInstance().getConnection(), Constants.timeseries_directory + widgetText, tablename);
                            } catch (Exception e) {
                                ExceptionDialog.show(getShell(), e, false);
                            }
                        }
                    });
                }
//					}
//				}
            }
        };

        // install listener for all created MenuItems
        for (int i = 0; i < loadTimeseriesMenuItems.length; i++) {
            loadTimeseriesMenuItems[i].addListener(SWT.Selection, loadTimeseriesListener);
        }

        //this listener registers input in the limittext widget
        limitText.addModifyListener(new ModifyListener() {

            public void modifyText(ModifyEvent e) {
                String text = limitText.getText();
                try {
                    // only for checking
                    Integer.valueOf(text);
                } catch (NumberFormatException e1) {
                    MessageBox mb = new MessageBox(parent.getShell(), SWT.ICON_ERROR | SWT.OK);
                    mb.setText("Number Format Error");
                    mb.setMessage(Constants.console_NumberFormatException);
                    mb.open();
                }
            }
        });
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


 
    private void loadTimeseriesList() {
        // load timeseries PopUp menu
        loadTimeseriesMenu = new Menu(this.getShell(), SWT.POP_UP);
        File[] files = ScriptModule.getInstance().getTimeseriesFiles();
        loadTimeseriesMenuItems = new MenuItem[files.length];
        for (int i = 0; i < files.length; i++) {
            loadTimeseriesMenuItems[i] = new MenuItem(loadTimeseriesMenu, SWT.PUSH);
            loadTimeseriesMenuItems[i].setText(files[i].getName());
        }
    }

    /**
     * This method gets called from the main control listener of the input console.
     * After ctrl+enter was pressed, this one checks the input console for 
     * statements and tries to execute them.
     */
    private void checkForStatements() {
        String text = inputST.getText();
        if (text.length() > 0) {

            // check for connection
            if (JDBCInterface.getInstance().getConnectionStatus() == JDBCInterface.CONNECTED) {

                // execute statement
                // don`t cut the statement at the ;
                /*
                if(text.indexOf(";")>-1) {
                int index = text.indexOf(";");
                text = text.substring(0,index);
                }*/

                int limit;
                if (limitRowsCheckbox.getSelection()) {
                    try {
                        limit = Integer.valueOf(limitText.getText());
                    } catch (NumberFormatException e) {
                        limit = 0;
                    }
                } else {
                    limit = 0;
                }
                JDBCInterface.getInstance().executeStatement(text, limit, true, true, true, true);
            } else {
                mainST.append("You have to connect to a database before running statements." + mainST.getLineDelimiter());
            }
            statementsReceived = true;
            cleanInputConsole();
        }
    }

    /**
     * Cleans the input console (the lower one)
     */
    private void cleanInputConsole() {
        inputST.setText(""); // clear input console
        inputST.setCaretOffset(0); // reset caret position
    }

    /**
     * this method cleans the output console
     */
    private void cleanOutputConsole() {
        mainST.setText("");
        outputBGColorListener.reset();

        outputListener.reset();
    }

    /**
     * This method activates all controls of this component.
     */
    protected void activateControls() {
        if (!this.isDisposed()) {
            mainST.setEnabled(true);
            inputST.setEnabled(true);
            tools.setEnabled(true);
            createModel.setEnabled(true);
            loadQuery.setEnabled(true);
            saveQuery.setEnabled(true);
            cleanConsole.setEnabled(true);
            loadTimeseries.setEnabled(true);
            limitComposite.setEnabled(true);
            limitRowsCheckbox.setEnabled(true);
            if (limitRowsCheckbox.getSelection()) {
                limitText.setEnabled(true);
            }
        }

    }

    /**
     * This method deactivates all controls of this component.
     */
    protected void deactivateControls() {
        if (!this.isDisposed()) {
            mainST.setEnabled(false);
            inputST.setEnabled(false);
            tools.setEnabled(false);
            createModel.setEnabled(false);
            loadQuery.setEnabled(false);
            saveQuery.setEnabled(false);
            cleanConsole.setEnabled(false);
            loadTimeseries.setEnabled(false);
            limitComposite.setEnabled(false);
            limitRowsCheckbox.setEnabled(false);
            limitText.setEnabled(false);
        }
    }

    /**
     * This method gets called after each statement execution,
     * <br>no matter if successful, or not!
     * <br>This method is responsible to paste the results in the output console.  
     * <br>The results are encapsuled by an HistoryEntry. This one is also responsible 
     * <br>for formatting the output, depending on the type of result and the current settings. 
     */
    private void updateOutput() {
        String content;
        // get last history entry
        // because only statements, which were executed with the correct execute()
        // method, are kept in the history and will be displayed by the console
        HistoryEntry entry = History.getInstance().getLastEntry();
        currentHistoryIndex = History.getInstance().size(); // used for ctrl+up / ctrl+down

        String newLine = mainST.getLineDelimiter();

        // ------------------------------------------------
        // 1st append the stmt text of the executed stmt
        mainST.append(newLine);
        int start = mainST.getCharCount();
        content = entry.getSQL();
        int length = content.length();
        outputListener.addRange(0, start, length);
        outputBGColorListener.addRange(0, start, length);
        mainST.append(content);
        mainST.append(newLine);
        //System.out.println("stmt range:"+start+"-"+(start+length));

        // ------------------------------------------------
        // 2nd append its resultset contents/
        // exception message/update count/SQL Warning
        if (entry.hasResults()) {
            mainST.append(newLine);
            start = mainST.getCharCount();
            content = entry.getConsoleOutput(newLine).toString();
            length = content.length();
            // first get start and length of content to be added
            // because the LineStyle gets determined right after appending the content
            // otherwise a costly redraw would be necessary to reflect the exclusive style ranges
            outputListener.addRange(1, start, length);
            outputBGColorListener.addRange(1, start, length);
            // exclude the result area from begin formatted by the SQLListener
            sqlListener.addRange(start, length);
            mainST.append(content);

            // ------------------------------------------------
            // 3rd append warnings if available/
            // exception message/update count/SQL Warning
            if (entry.hasWarnings()) {
                mainST.append(newLine);
                start = mainST.getCharCount();
                SQLWarning warn = entry.getSQLWarning();
                content = "";
                while (warn != null) {
                    content += warn.getMessage();
                    content += "\n";
                    // first get start and length of content to be added
                    // because the LineStyle gets determined right after appending the content
                    // otherwise a costly redraw would be necessary to reflect the exclusive style ranges
                    warn = warn.getNextWarning();
                }
                length = content.length();
                outputListener.addRange(2, start, length);
                outputBGColorListener.addRange(2, start, length);
                // exclude the warning area from begin formatted by the SQLListener
                sqlListener.addRange(start, length);
                mainST.append(content);
            }
        }


        // if the stmt didn`t succeed, then color the bg with red
        if (entry.hasException()) {
            mainST.append(newLine);
            start = mainST.getCharCount();
            content = entry.getExceptionForConsoleOutput(newLine).toString();
            length = content.length();
            // first get start and length of content to be added
            // because the LineStyle gets determined right after appending the content
            // otherwise a costly redraw would be necessary to reflect the exclusive style ranges
            outputListener.addRange(3, start, length);
            outputBGColorListener.addRange(3, start, length);
            // exclude the exception are from begin formatted by the SQLListener
            sqlListener.addRange(start, length);

            mainST.append(content);

            //System.out.println("exception range:"+start+"-"+(start+length));
        }
        mainST.append("\n");

        // finally scroll to the end of the text
        mainST.setSelection(mainST.getText().length());

        // and redraw the widget, (not necessary anymore)
        mainST.redraw();
    }

    /**
     * This hook method listens to the following application events:
     * <ul>CONNECTION_ESTABLISHED</ul>
     * <ul>CONNECTION_CLOSED</ul>
     * <ul>RESULT_READY</ul>
     * <ul>LOAD_HISTORY</ul> */
    public void modelChanged(GenericModelChangeEvent event) {
        switch (event.detail) {
            // JDBCInterface Model Change Events
            case DemoEvents.CONNECTION_ESTABLISHED:
                activateControls();
                break;
            case DemoEvents.CONNECTION_CLOSED:
                deactivateControls();
                break;
            case DemoEvents.RESULT_READY:
                if (event.showResultInConsole) {
                    updateOutput();
                }
                break;
            case DemoEvents.LOAD_HISTORY:
                currentHistoryIndex = History.getInstance().size();
                break;
            default:
                break;
        }
    }

    /**
     * This method gets called when the user switches the Tab and clicks on the
     * tab title of this component.	 */
    protected void updateComponent() {
        // do nothing
    }

    /**
     * this resets the console
     */
    public void reset() {
        // currently do nothing during a reset
    }
}
