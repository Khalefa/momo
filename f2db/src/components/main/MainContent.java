package components.main;

import components.tabs.ModelGraphViewerTab;
import java.util.Properties;

import modules.config.Configuration;
import modules.misc.Constants;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.TabFolder;
import org.eclipse.swt.widgets.TabItem;

import components.tabs.ACFaPACF_GuiTab;
import components.tabs.AboutTab;
import components.tabs.ConfigTab;
import components.tabs.ConsoleTab;

import components.tabs.HistoryTab;
import components.tabs.QueryPlanViewerTab;
import components.tabs.SystemCatalogTab;
import components.tabs.TimeSeriesPlotTab;
import components.tabs.TraceTab;

/**
 * This class is the main content area of the application. It simply creates a
 * bunch of different tabs, each of them fulfilling a special application function.
 * @author Felix Beyer, Sebastian Seifert, Christopher Schildt
 * @date   09.08.2011
 *
 */
public class MainContent extends Composite {
    
    // the different Tabs of the main content area
	private ConsoleTab        consoleTab;        // the ConsoleTab
	private TimeSeriesPlotTab timeSeriesPlotTab; // the timeseries plot tab
	private ACFaPACF_GuiTab   aCFaPACF_GuiTab;
//	private DetailTreeTab     treeTab;           // the Detail Tree Tab
//	private QueryPlanTab      planTab;           // the Query plan Tab
//	private OlapTab           olapTab;           // the group by Tab
//	private OnlineSamplingTab onlineSamplingTab; // the online sampling Tab
//    private RefreshSampleTab  refreshSampleTab;  // the refresh sample Tab
    private SystemCatalogTab  systemTab;         // the System Catalog Tab
//	private SampleCatalogTab  sampleCatalogTab;  // the Sample Catalog Tab 
//	private WorkloadTab       workloadTab;       // the workload Tab
	private HistoryTab        historyTab;        // the history Tab
	private ConfigTab         configTab;		 // the configuration tab
	private AboutTab          aboutTab;			 // the about tab
	private QueryPlanViewerTab planView;
	private ModelGraphViewerTab modelGraphView;
        private TraceTab tracetab;
	private Properties props;
    
	/** the constructor */
	public MainContent(Composite parent, int style) {
		super(parent, style);
		
		setLayout(new FillLayout());
		
		initComponents();
	}
	
    /** this method inits all the components */
	private void initComponents(){
		TabFolder tabFolder = new TabFolder (this, SWT.NONE);
		tabFolder.setLayout(new FillLayout());
		
        props = new Properties();
        Properties propsSrc = Configuration.getInstance().getMiscProperties();
        Configuration.getInstance().copyProperties(propsSrc, props);
        
        TabItem item;
		
        if ("true".equals(props.getProperty("console.output.tab.console.show"))){
        	item = new TabItem (tabFolder, SWT.NONE);
        	item.setText (Constants.tab_console);
        	consoleTab = new ConsoleTab(tabFolder, SWT.NONE);
        	item.setControl(consoleTab);
        }
        
        if ("true".equals(props.getProperty("console.output.tab.timeseriesPlot.show"))){		
        	item = new TabItem (tabFolder, SWT.NONE);
        	item.setText (Constants.tab_timeseriesPlot);
        	timeSeriesPlotTab = timeSeriesPlotTab.getInstance(tabFolder, SWT.NONE);
        	item.setControl(timeSeriesPlotTab);
        }	
        if ("true".equals(props.getProperty("console.output.tab.planView.show"))){		
        	item = new TabItem (tabFolder, SWT.NONE);
        	item.setText (Constants.tab_planview);
        	planView = new QueryPlanViewerTab(tabFolder, SWT.NONE);
        	item.setControl(planView);
        }	
        if ("true".equals(props.getProperty("console.output.tab.planView.show"))){		
        	item = new TabItem (tabFolder, SWT.NONE);
        	item.setText (Constants.tab_modelgraphview);
        	modelGraphView = new ModelGraphViewerTab(tabFolder, SWT.NONE);
        	item.setControl(modelGraphView);
        }	



        if ("true".equals(props.getProperty("console.output.tab.timeseriesAcfaPacf.show"))){		
        	item = new TabItem (tabFolder, SWT.NONE);
        	item.setText (Constants.tab_timeseriesAcfaPacf);
        	aCFaPACF_GuiTab = new ACFaPACF_GuiTab(tabFolder, SWT.NONE);
        	item.setControl(aCFaPACF_GuiTab);
        }	

        if ("true".equals(props.getProperty("console.output.tab.tracetab.show"))){
        	item = new TabItem (tabFolder, SWT.NONE);
        	item.setText (Constants.tab_trace);
        	tracetab = new TraceTab(tabFolder, SWT.NONE);
        	item.setControl(tracetab);
        }
        
//        if ("true".equals(props.getProperty("console.output.tab.tree.show"))){
//        	item = new TabItem (tabFolder, SWT.NONE);
//        	item.setText (Constants.tab_tree);
//        	treeTab = new DetailTreeTab(tabFolder, SWT.NONE);
//        	item.setControl(treeTab);
//        }	
//
//        if ("true".equals(props.getProperty("console.output.tab.plan.show"))){
//        	item = new TabItem (tabFolder, SWT.NONE);
//        	item.setText (Constants.tab_plan);
//        	planTab = new QueryPlanTab(tabFolder, SWT.NONE);
//        	item.setControl(planTab);
//        }	
//
//        if ("true".equals(props.getProperty("console.output.tab.olap.show"))){
//        	item = new TabItem (tabFolder, SWT.NONE);
//        	item.setText (Constants.tab_olap);
//        	olapTab = new OlapTab(tabFolder, SWT.NONE);
//        	item.setControl(olapTab);
//        }	
//
//        if ("true".equals(props.getProperty("console.output.tab.onlineSampling.show"))){
//        	item = new TabItem (tabFolder, SWT.NONE);
//        	item.setText (Constants.tab_onlineSampling);
//        	onlineSamplingTab = new OnlineSamplingTab(tabFolder, SWT.NONE);
//        	item.setControl(onlineSamplingTab);
//        }	
//
//        if ("true".equals(props.getProperty("console.output.tab.refresh.show"))){
//        	item = new TabItem (tabFolder, SWT.NONE);
//        	item.setText (Constants.tab_refresh);
//        	refreshSampleTab = new RefreshSampleTab(tabFolder, SWT.NONE);
//        	item.setControl(refreshSampleTab);
//        }	

        if ("true".equals(props.getProperty("console.output.tab.system.show"))){		
        	item = new TabItem (tabFolder, SWT.NONE);
        	item.setText (Constants.tab_system);
        	systemTab = new SystemCatalogTab(tabFolder, SWT.NONE);
        	item.setControl(systemTab);
        }	

//        if ("true".equals(props.getProperty("console.output.tab.sampleCatalog.show"))){		
//        	item = new TabItem (tabFolder, SWT.NONE);
//        	item.setText (Constants.tab_sampleCatalog);
//        	sampleCatalogTab = new SampleCatalogTab(tabFolder, SWT.NONE);
//        	item.setControl(sampleCatalogTab);
//        }	



//        if ("true".equals(props.getProperty("console.output.tab.workload.show"))){
//        	item = new TabItem (tabFolder, SWT.NONE);
//        	item.setText (Constants.tab_workload);
//        	workloadTab = new WorkloadTab(tabFolder, SWT.NONE);
//        	item.setControl(workloadTab);
//        }	

        if ("true".equals(props.getProperty("console.output.tab.history.show"))){		
        	item = new TabItem (tabFolder, SWT.NONE);
        	item.setText (Constants.tab_history);
        	historyTab = new HistoryTab(tabFolder,SWT.NONE);
        	item.setControl(historyTab);
        }	

        if ("true".equals(props.getProperty("console.output.tab.config.show"))){
        	item = new TabItem (tabFolder, SWT.NONE);
        	item.setText (Constants.tab_config);
        	configTab = new ConfigTab(tabFolder,SWT.NONE);
        	item.setControl(configTab);
        }	


        if ("true".equals(props.getProperty("console.output.tab.about.show"))){    
        	item = new TabItem (tabFolder, SWT.NONE);
        	item.setText (Constants.tab_about);
        	aboutTab = new AboutTab(tabFolder, SWT.NONE);
        	item.setControl(aboutTab);
        }
	}
}
