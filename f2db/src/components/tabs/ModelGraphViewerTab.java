package components.tabs;

import com.mxgraph.model.mxCell;
import com.mxgraph.util.mxPoint;
import com.sun.org.apache.xml.internal.dtm.ref.DTMDefaultBaseIterators.ParentIterator;
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

import modules.databaseif.History;
import modules.databaseif.JDBCInterface;
import modules.generic.DemoEvents;
import modules.generic.GenericModelChangeEvent;
import modules.generic.GenericModelChangeListener;
import modules.modelgraphview.ModelgraphViewerTools;
import modules.modelgraphview.PostgresModelGraphNode;
import modules.planview.PostgresPlanNode;
import modules.planview.QueryPlanViewerTools;

import components.AbstractComponent;
import components.dialogs.CreateModelDialog;
import components.dialogs.InfoDialog;
import components.dialogs.TimeMeasureQuestion;
import components.listeners.DefaultLineStyleListener;
import components.listeners.GenericStyleListener;
import components.listeners.SQLKeywordLineStyleListener;
import components.listeners.UpperKeyListener;
import components.tabs.MiscUtils;
import components.tabs.TimeSeriesPlotTab;
import components.widgets.JGraphWidget;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import modules.databaseif.HistoryEntry;
import modules.misc.Constants;
import modules.modelgraphview.ModelInfo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Spinner;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeItem;

public class ModelGraphViewerTab extends AbstractComponent implements GenericModelChangeListener {

    private StyledText selectTimeseries;
    AbstractComponent tab;
    private Button toviewTS;
    private Button createModel;
    private Button toOutput;
    private Spinner numberOfForecastValues;
    private Button explain;
    private Button equalsize;
    private Composite parent;
    private Listener loadQueryListener;
    JGraphWidget jgraph;
    private Tree navTree;
    private Table modelInfos;

    public ModelGraphViewerTab(Composite tabFolder, int none) {
        super(tabFolder, none);
        parent=tabFolder;
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

        explain = new Button(this, SWT.None);
        explain.setText("Explain!");
        equalsize = new Button(this, SWT.CHECK);
        equalsize.setText("Equal node width");
        equalsize.setSelection(true);


        {
            Label fillerLabel = new Label(this, SWT.NONE);
            GridData fillerLabelLData = new GridData();
            fillerLabelLData.horizontalSpan = 4;
            fillerLabelLData.horizontalAlignment = GridData.FILL;
            fillerLabel.setLayoutData(fillerLabelLData);
        }



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


        ConnectionTextLData.horizontalAlignment = GridData.FILL;
        ConnectionTextLData.verticalAlignment = GridData.FILL;
        rightSide.setLayoutData(ConnectionTextLData);
        {
            Group models = new Group(rightSide, SWT.None);
            models.setLayout(new GridLayout(1, false));
            ConnectionTextLData = new GridData();
            ConnectionTextLData.grabExcessHorizontalSpace = true;
            ConnectionTextLData.horizontalAlignment = GridData.FILL;
            ConnectionTextLData.verticalAlignment = GridData.FILL;
            models.setLayoutData(ConnectionTextLData);
            models.setText("ModelSelect");

            navTree = new Tree(models, SWT.SINGLE);

            ConnectionTextLData = new GridData();
            ConnectionTextLData.heightHint = (this.getShell().getSize().y / 3) - 30;
            ConnectionTextLData.grabExcessHorizontalSpace = true;
            ConnectionTextLData.horizontalAlignment = GridData.FILL;
            ConnectionTextLData.verticalAlignment = GridData.FILL;
            navTree.setLayoutData(ConnectionTextLData);

            navTree.setEnabled(true);



            Group models2 = new Group(rightSide, SWT.None);
            models2.setLayout(new GridLayout(1, false));
            ConnectionTextLData = new GridData();
            ConnectionTextLData.grabExcessHorizontalSpace = true;
            ConnectionTextLData.grabExcessVerticalSpace = true;
            ConnectionTextLData.horizontalAlignment = GridData.FILL;
            ConnectionTextLData.verticalAlignment = GridData.FILL;
            models2.setLayoutData(ConnectionTextLData);
            models2.setText("ModelInfo");


            modelInfos = new Table(models2, SWT.None);
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



        }

        Group buttongroup = new Group(rightSide, SWT.None);
        buttongroup.setLayout(new GridLayout(2, false));
        GridData TL2Data = new GridData();
        TL2Data.horizontalSpan = 2;
        TL2Data.grabExcessHorizontalSpace = true;
        TL2Data.horizontalAlignment = GridData.FILL;
        TL2Data.verticalAlignment = GridData.FILL;
        buttongroup.setLayoutData(TL2Data);

        Label typeLabel = new Label(buttongroup, SWT.NONE);
        TL2Data = new GridData();
        typeLabel.setLayoutData(TL2Data);
        typeLabel.setText("  # of forecasts :");
        numberOfForecastValues = new Spinner(buttongroup, SWT.BORDER);
        numberOfForecastValues.setMinimum(0);
        numberOfForecastValues.setMaximum(1000);
        numberOfForecastValues.setSelection(10);
        numberOfForecastValues.setIncrement(1);

        {
            toOutput = new Button(buttongroup, SWT.PUSH);
            TL2Data = new GridData();
            TL2Data.horizontalSpan = 2;
            TL2Data.grabExcessHorizontalSpace = true;
            TL2Data.horizontalAlignment = GridData.FILL;
            TL2Data.verticalAlignment = GridData.FILL;
            toOutput.setLayoutData(TL2Data);
            toOutput.setText(Constants.mgview_toolButtonNames[0]);
            toOutput.setEnabled(true);
            toOutput.pack();
        }
        {
            createModel = new Button(buttongroup, SWT.PUSH);
            TL2Data = new GridData();
            TL2Data.horizontalSpan = 2;
            TL2Data.grabExcessHorizontalSpace = true;
            TL2Data.horizontalAlignment = GridData.FILL;
            TL2Data.verticalAlignment = GridData.FILL;
            createModel.setLayoutData(TL2Data);
            createModel.setText(Constants.mgview_toolButtonNames[1]);
            createModel.setEnabled(true);
            createModel.pack();
        }
        {
            toviewTS = new Button(buttongroup, SWT.PUSH);
            TL2Data = new GridData();
            TL2Data.horizontalSpan = 2;
            TL2Data.grabExcessHorizontalSpace = true;
            TL2Data.horizontalAlignment = GridData.FILL;
            TL2Data.verticalAlignment = GridData.FILL;
            toviewTS.setLayoutData(TL2Data);
            toviewTS.setText(Constants.mgview_toolButtonNames[2]);
            toviewTS.setEnabled(true);
            toviewTS.pack();
        }
        toOutput.setEnabled(false);
        createModel.setEnabled(false);
        toviewTS.setEnabled(false);
    }

    public void deactivateControls() {
        explain.setEnabled(false);
        // TODO Auto-generated method stub

    }

    public void activateControls() {
        // TODO Auto-generated method stub
        explain.setEnabled(true);

    }

    @Override
    protected void initListeners() {
        JDBCInterface.getInstance().addModelChangeListener(this);

        explain.addListener(SWT.Selection, new Listener() {

            public void handleEvent(Event event) {
                ResultSet rs = JDBCInterface.getInstance().executeStatementWithResult("Print modelgraph");
                jgraph.refresh();
                navTree.removeAll();
                jgraph.getJGraph().getModel().beginUpdate();
                ModelgraphViewerTools.initializeStyles(jgraph.getJGraph(), navTree, jgraph.getSize().x / 2 - 100);
                ModelgraphViewerTools.createTreeModel(rs, jgraph.getJGraph(), jgraph.getSize().x / 2 - 100, navTree);
                ModelgraphViewerTools.doLayout();
                jgraph.getJGraph().getModel().endUpdate();



            }
        });


        toOutput.addListener(SWT.Selection, new Listener() {

            public void handleEvent(Event event) {
                String result = buildQuery(true);
                if (result == null) {
                    return;
                }
                ResultSet rs = JDBCInterface.getInstance().executeStatementWithResult(result);
                InfoDialog.show_result(getShell(), rs, true, numberOfForecastValues.getSelection());



            }
        });


        createModel.addListener(SWT.Selection, new Listener() {

            public void handleEvent(Event event) {

                //get Knowledge aboput time and meassure column
                String tm=new TimeMeasureQuestion(parent.getShell()).open();
                System.out.println(tm);
                if (tm == null) {
                    return;
                }
                int level=0;
                TreeItem iter=navTree.getSelection()[0];
                while(iter.getItemCount()>0)
                {
                    level++;
                    iter=iter.getItem(0);
                }
                String result = buildCMQuery(tm.split("#")[1],tm.split("#")[0],level);
                if (result == null) {
                    return;
                }
                String resultS = new CreateModelDialog(parent.getShell()).open(result,tm.split("#")[1],"MC");
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
                ResultSet rs = JDBCInterface.getInstance().executeStatementWithResult("Print modelgraph");
                jgraph.refresh();
                navTree.removeAll();
                jgraph.getJGraph().getModel().beginUpdate();
                ModelgraphViewerTools.initializeStyles(jgraph.getJGraph(), navTree, jgraph.getSize().x / 2 - 100);
                ModelgraphViewerTools.createTreeModel(rs, jgraph.getJGraph(), jgraph.getSize().x / 2 - 100, navTree);
                ModelgraphViewerTools.doLayout();
                jgraph.getJGraph().getModel().endUpdate();



            }
        });

        toviewTS.addListener(SWT.Selection, new Listener() {

            public void handleEvent(Event event) {
                new Thread(new Runnable() {

                            public void run() {
                                Display.getDefault().asyncExec(new Runnable() {
                                    public void run() {
                                
                Map<String, String> paras = ((ModelInfo) navTree.getSelection()[0].getData()).getParameter();
                TimeSeriesPlotTab a = TimeSeriesPlotTab.getInstance(null, 0);
                a.getSelectTimeseries().setText(buildQuery(false));
                a.updateCombosStatement();
                a.getAlgorithms().setText(paras.get("model type").toUpperCase());
                a.getParameters().setText(buildParameter());
                String[] temp = navTree.getSelection()[0].getParentItem().getText().split("on");
                a.getMeassurecolumn().setText(temp[0].trim());
                a.getTimecolumn().setText(temp[1].trim());
                a.getStorage().setText("MODELGRAPH");
                 }});
                    }
                }).start();





            }
        });

        navTree.addListener(SWT.Selection, new Listener() {

            public void handleEvent(Event event) {
                modelInfos.removeAll();
                if (navTree.getSelection() == null || navTree.getSelection().length == 0) {
                    return;
                }
                int a=navTree.getSelection()[0].getItemCount();
                if(navTree.getSelection()[0].getItemCount()==0)
                {
                    createModel.setEnabled(true);
                }
                if (navTree.getSelection()[0].getData() == null) {
                    toOutput.setEnabled(false);
                     createModel.setEnabled(false);
                    toviewTS.setEnabled(false);
                    return;
                }
                if ((navTree.getSelection()[0].getData()!=null && !(navTree.getSelection()[0].getData() instanceof ModelInfo))) {
                    //must be a non leaf node
                    mxCell[] temp=new mxCell[1];
                    temp[0]=(mxCell)navTree.getSelection()[0].getData();
                    //center hier view
                    jgraph.redraw();
                    toOutput.setEnabled(false);
                    createModel.setEnabled(true);
                    toviewTS.setEnabled(false);
                    return;
                }

                Map<String, String> paras = ((ModelInfo) navTree.getSelection()[0].getData()).getParameter();


                TableItem test = new TableItem(modelInfos, SWT.None);
                if (paras.get("model name") != null) {
                     createModel.setEnabled(false);
                    test.setText(0, "model name");
                    test.setText(1, paras.get("model name"));
                }
                if (paras.get("model type") != null) {
                    test = new TableItem(modelInfos, SWT.None);
                    test.setText(0, "model type");
                    test.setText(1, paras.get("model type").toLowerCase());
                }
                if (paras.get("where expr") != null) {
                    test = new TableItem(modelInfos, SWT.None);
                    test.setText(0, "where expr");
                    test.setText(1, paras.get("where expr").toLowerCase());
                }
                 if (paras.get("error") != null) {
                    test = new TableItem(modelInfos, SWT.None);
                    test.setText(0, "error");
                    test.setText(1, paras.get("error").toLowerCase());
                }
                if (paras.get("disag key") != null) {
                    test = new TableItem(modelInfos, SWT.None);
                    test.setText(0, "disag key");
                    test.setText(1, paras.get("disag key").toLowerCase());
                }


                List<String> para1= new ArrayList<String>(paras.keySet());
                                        Collections.sort(para1);
                for (String key : para1) {
                    if (key.equals("model name") || key.equals("model type") || key.equals("where expr")|| key.equals("disag key")|| key.equals("error")) {
                        continue;
                    }
                    test = new TableItem(modelInfos, SWT.None);
                    test.setText(0, key);
                    test.setText(1, paras.get(key).toLowerCase());

                }
                for (int i = 0; i < 2; i++) {
                    modelInfos.getColumn(i).pack();
                }
                toOutput.setEnabled(true);
               
                toviewTS.setEnabled(true);
            }
        });

        equalsize.addListener(SWT.Selection, new Listener() {

            public void handleEvent(Event event) {
                ModelgraphViewerTools.equalsize = ((Button) event.widget).getSelection();
            }
        });
        loadQueryListener = new Listener() {

            public void handleEvent(Event event) {
                selectTimeseries.setText(((MenuItem) event.widget).getText());
                selectTimeseries.setCaretOffset(selectTimeseries.getText().length());
            }
        };




    }

    private String buildParameter() {
        Map<String, String> paras = ((ModelInfo) navTree.getSelection()[0].getData()).getParameter();
        String result = "";
         boolean added=false;
        if (paras.keySet().size() > 2) {
            result += " (";
            for (String key : paras.keySet()) {
                if (key.equals("model type") || key.equals("model name") ||key.equals("training data") || key.equals("type") || key.equals("disag key")) {
                    continue;
                } else {
                    result += (key + "=" + paras.get(key) + ",");
                    added=true;
                }
            }
            result = result.substring(0, result.length() - 1);
            result += ") ";
        }
         if(added)
            return result;
         else return "";
    }


    private String buildCMQuery(String time, String measure,int level)
    {
        String table=ModelgraphViewerTools.trainingdata;
        if(table.indexOf("FROM")<0)
                return "no FROM clause found";
        String queryPart="Select "+time+",sum("+measure+") as mc \n "+ table.substring(table.indexOf("FROM"))+"\n";

        String rawQuery=queryPart;
        String result="";
        TreeItem iter = navTree.getSelection()[0];
        int i=level;
        String whereclause = "";
        while (iter != null) {
            if (iter.getText().equals("[Total]") || iter.getText().equals("Root")) {
                i++;
                iter = iter.getParentItem();
                continue;
            }
            if (ModelgraphViewerTools.getAtttype(i)) {
                whereclause += ModelgraphViewerTools.getAtt(i) + "='" + iter.getText() + "' AND ";
            } else {
                whereclause += ModelgraphViewerTools.getAtt(i) + "=" + iter.getText() + " AND ";
            }
            i++;
            iter = iter.getParentItem();
        }

        if (whereclause.length() > 4) {
            /********************************/
               int startidx=rawQuery.length();
            if (rawQuery.contains("where")) {
                result += rawQuery.substring(0, startidx - 1);
                result += " AND ";
                result += whereclause.substring(0, whereclause.length() - 4) + " ";
            } else {
                result += rawQuery.substring(0, startidx - 1);
                result += " WHERE ";
                result += whereclause.substring(0, whereclause.length() - 4) + " ";
            }
        } else {
            result = rawQuery;
        }

        result+=" \nGROUP BY " +time+" ORDER by "+time;
        return result;
    }
    private String buildQuery(boolean withforecast) {
        if (!(navTree.getSelection()[0].getData() instanceof ModelInfo)) {
            return null;
        }
        Map<String, String> paras = ((ModelInfo) navTree.getSelection()[0].getData()).getParameter();
        String rawQuery = paras.get("training data");
        String result = "";
        /********************************/
        /*CREATE QUERY EXTENSION        */
        /********************************/
        String whereclause = "";
        int i = 0;
        TreeItem iter = navTree.getSelection()[0].getParentItem().getParentItem();
        while (iter != null) {
            if (iter.getText().equals("[Total]") || iter.getText().equals("Root")) {
                i++;
                iter = iter.getParentItem();
                continue;
            }
            if (ModelgraphViewerTools.getAtttype(i)) {
                whereclause += ModelgraphViewerTools.getAtt(i) + "='" + iter.getText() + "' AND ";
            } else {
                whereclause += ModelgraphViewerTools.getAtt(i) + "=" + iter.getText() + " AND ";
            }
            i++;
            iter = iter.getParentItem();
        }

        if (whereclause.length() > 4) {
            /********************************/
            int startidx = rawQuery.lastIndexOf("group by");
            if(startidx<0)
                startidx=rawQuery.length();
            if (rawQuery.contains("where")) {
                result += rawQuery.substring(0, startidx - 1);
                result += " AND ";
                result += whereclause.substring(0, whereclause.length() - 4) + " ";
                result += rawQuery.substring(startidx);
            } else {
                result += rawQuery.substring(0, startidx - 1);
                result += " WHERE ";
                result += whereclause.substring(0, whereclause.length() - 4) + " ";
                result += rawQuery.substring(startidx);
            }
        } else {
            result = rawQuery;
        }
        if (!withforecast) {
            return result;
        }
        /********************************/
        /*CREATE Forecast EXTENSION      */
        /********************************/
        result += " FORECAST " + navTree.getSelection()[0].getParentItem().getText() + " NUMBER " + numberOfForecastValues.getSelection() + " ALGORITHM " + paras.get("model type");
        boolean added=false;
        if (paras.keySet().size() > 2) {
            result += " PARAMETERS (";
            for (String key : paras.keySet()) {
                if (key.equals("model type") || key.equals("training data") || key.equals("type") || key.equals("disag key") || key.equals("model name")) {
                    continue;
                } else {
                    result += (key + "=" + paras.get(key) + ",");
                    added=true;
                }
            }
            if(added)
            {
                result = result.substring(0, result.length() - 1);
                 result += ") ";
            }
            else
                result = result.substring(0, result.length()-" PARAMETERS (".length());
           
        }
        result += " STORAGE MODELGRAPH";

        return result;
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
