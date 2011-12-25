package components.tabs;

import java.util.List;

import modules.chart.XYLineChartAdapter;
import modules.databaseif.History;
import modules.databaseif.HistoryEntry;
import modules.databaseif.JDBCInterface;
import modules.generic.DemoEvents;
import modules.generic.GenericModelChangeEvent;
import modules.generic.GenericModelChangeListener;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StyleRange;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.custom.VerifyKeyListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Spinner;
import org.eclipse.swt.widgets.Text;
import org.jfree.data.xy.XYSeries;

import components.AbstractComponent;
import components.listeners.DefaultLineStyleListener;
import components.listeners.GenericStyleListener;
import components.listeners.SQLKeywordLineStyleListener;
import components.listeners.UpperKeyListener;

import data.type.Tuple;

/**
 * ***********************************************
 * NOTE: NEW FOR MY DIPLOM
 * @author Sebastian Seifert
 * ***********************************************
 * 
 * This tab shows the acf and pacf of a timeseries
 *
 * @author Sebastian Seifert
 */
public class ACFaPACF_GuiTab extends AbstractComponent implements GenericModelChangeListener {

	// GUI
	private StyledText selectTimeseries;
	private Spinner maxLag;
	private Button execute;
	
	private XYLineChartAdapter chartAdapter;
	private XYSeries seriesACF;
	private XYSeries seriesPACF;
	private XYSeries seriesZero; // hack
	private XYSeries seriesSignificantTop;
	private XYSeries seriesSignificantBottom;

	// data
	private String stmtText;
	
	public ACFaPACF_GuiTab(Composite parent, int style) {
		super(parent, style);
	}

	/** inits the GUI components */
	protected void initComponents(){
		setLayout(new GridLayout(11, false));

		selectTimeseries=new StyledText(this, SWT.BORDER | SWT.V_SCROLL | SWT.H_SCROLL);
		{
			GridData ConnectionTextLData = new GridData();
			ConnectionTextLData.horizontalSpan = 8;
			ConnectionTextLData.verticalSpan=10;
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
             inputUpperKeyListener = new UpperKeyListener(selectTimeseries,true);
             selectTimeseries.addVerifyKeyListener(inputUpperKeyListener);
		
		maxLag = new Spinner(this, SWT.BORDER);
		maxLag.setMinimum(0);
		maxLag.setSelection(0);
		maxLag.setIncrement(1);

		execute = new Button(this, SWT.None);
		execute.setText("execute");
		
		chartAdapter = new XYLineChartAdapter(this, SWT.NONE, false);
		chartAdapter.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 11, 1));

		seriesACF = new XYSeries("acf", false, true);
		chartAdapter.addSeries(seriesACF);
		
		seriesPACF = new XYSeries("pacf", false, true);
		chartAdapter.addSeries(seriesPACF);
		
		seriesZero = new XYSeries("zero", false, true);
		chartAdapter.addSeries(seriesZero);

		seriesSignificantTop = new XYSeries("significant", false, true);
		chartAdapter.addSeries(seriesSignificantTop);
		
		seriesSignificantBottom = new XYSeries("significant", false, true);
		chartAdapter.addSeries(seriesSignificantBottom);
	}

	/** inits the listeners */
	protected void initListeners() {
		//add the system catalog tab as listener to the JDBCInterface(Model)
		JDBCInterface.getInstance().addModelChangeListener(this);

		execute.addListener(SWT.Selection, new Listener(){
			public void handleEvent(Event event){
				
				stmtText = selectTimeseries.getText();
				
				JDBCInterface.getInstance().executeStatement(stmtText, 0, true, true, false, false);
			}
		});
	}
	
	/**
	 * this method checks, if the last executed query was the query from this tab.
	 * if we were right, get the results from that query
	 */
	private void checkResult(){
		// get last executed result
		HistoryEntry entry = History.getInstance().getLastEntry();

		String sqltext = entry.getSQL(); 

		// do we have the correct result
		if (!sqltext.equals(stmtText))
			return;
			
		if (entry.getQueryResult() == null)
			return;
		
		List<Tuple> tuples = entry.getQueryResult().getTuples();

		double[][] data = new double[tuples.size()][2];
		boolean two = tuples.get(0).size() == 2;

		int i = 0;
		for (Tuple t : tuples){

			if (two)
				data[i][0] = t.getValue(0).getLong();
			else
				data[i][0] = i + 1;

			data[i][1] = t.getValue(two ? 1 : 0).getDouble();

			i++;
		}




		/////////////////////////////////
		// compute acf


		double sum = 0;
		int count = 0 ;

		for (i = 0; i < data.length; i++){
			sum += data[i][1];
			count++;
		}

		double avg = sum / count;

		int max_tau;
		if (maxLag.getSelection() == 0)
			max_tau = Math.min(count, Math.max(10, count/4));
		else
			max_tau = Math.min(count, maxLag.getSelection());
		
		
		
		double[] c = new double[max_tau];
		double[] acf = new double[max_tau];
		for (int tau = 0; tau <  max_tau; tau++){


			for (i = 0; i < data.length - tau; i++){

				c[tau] += (data[i][1] - avg) * (data[i + tau][1] - avg);

			}

			acf[tau] = c[tau]/c[0];
		}



		////////////////////////////////
		// compute pacf

		Double[][] pacf = new Double[max_tau][max_tau];

		pacf[1][1] = acf[1];

		for (int h = 2; h < max_tau; h++){

			double s1 = 0;
			for (int j = 1; j <= h - 1; j++)
				s1 += pacf[h - 1][j]*acf[h - j];

			double s2 = 0;
			for (int j = 1; j <= h - 1; j++)
				s2 += pacf[h - 1][j]*acf[j];

			pacf[h][h] = (acf[h] - s1)/(1 - s2);


			for (int j = 1; j <= h - 1; j++)
				pacf[h][j] = pacf[h - 1][j] - pacf[h][h]*pacf[h - 1][h - j];

		}
		pacf[0][0] = new Double(1);




		// plot

		seriesACF.clear();
		seriesPACF.clear();
		seriesZero.clear();
		chartAdapter.setXRange(0, max_tau);
		
		double significant = 2 / Math.sqrt(count);
		seriesSignificantTop.add(0, significant, false);
		seriesSignificantTop.add(max_tau, significant, false);
		seriesSignificantBottom.add(0, -significant, false);
		seriesSignificantBottom.add(max_tau, -significant, false);
		seriesSignificantTop.fireSeriesChanged();
		seriesSignificantBottom.fireSeriesChanged();
		
		seriesZero.add(0, 0, false);
		seriesZero.add(max_tau, 0, false);
		seriesZero.fireSeriesChanged();

		Thread t = Thread.currentThread();
		long lastRefreshTime = System.currentTimeMillis();
		for (i = 0; i < acf.length; i++) {
			if (t.isInterrupted())
				break;
			seriesACF.add(i, acf[i], false);
			seriesPACF.add(i, pacf[i][i], false);
			if (System.currentTimeMillis() - lastRefreshTime > 500 /*0.5 second*/) {
				seriesACF.fireSeriesChanged();
				seriesPACF.fireSeriesChanged();
				lastRefreshTime = System.currentTimeMillis();
			}
		}
		seriesACF.fireSeriesChanged();
		seriesPACF.fireSeriesChanged();
	}

	/** this method gets called when a connection was closed */
	protected void deactivateControls() {
		if(!isDisposed()){
			selectTimeseries.setEnabled(false);
			execute.setEnabled(false);
		}
	}

	/** this method gets called when a connection was established */
	protected void activateControls() {
		if(!isDisposed()){
			selectTimeseries.setEnabled(true);
			execute.setEnabled(true);
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
		switch (event.detail){
		  case DemoEvents.CONNECTION_ESTABLISHED : activateControls();   break;
		  case DemoEvents.CONNECTION_CLOSED	     : deactivateControls(); break;
		  case DemoEvents.RESULT_READY			 : checkResult();		 break;
		  default: break;
		}
	}
}
