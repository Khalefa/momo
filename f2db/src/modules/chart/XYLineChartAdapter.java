package modules.chart;

import java.awt.Color;
import java.awt.Frame;
import java.awt.GradientPaint;
import java.text.DecimalFormat;

import org.eclipse.swt.SWT;
import org.eclipse.swt.awt.SWT_AWT;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Composite;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.StandardXYItemRenderer;
import org.jfree.chart.renderer.xy.XYItemRenderer;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;
import org.jfree.ui.RectangleInsets;

/**
 * This class is a AWT-SWT wrapper for JFreeChart.   
 * 
 * @author rg
 * @version $Rev: 1102 $, $LastChangedDate: 2008-09-30 11:55:05 +0200 (Tue, 30 Sep 2008) $
 */
public class XYLineChartAdapter extends Composite {

	// -- variables -------------------------------------------------------------------------------
	
	/** the chart */
	protected JFreeChart chart;
	
	/** the dataset */
	protected XYSeriesCollection xySeriesCollection;
        NumberAxis ya=new NumberAxis();
         NumberAxis xa=new NumberAxis();
	protected boolean autoRange;
	
	// -- constructors ----------------------------------------------------------------------------
	
	public XYLineChartAdapter(Composite parent, int style, boolean autoRange) {
		super(parent, style | SWT.EMBEDDED);
		this.autoRange = autoRange;
		initComponents();
                

	}

	
	// -- gui stuff -------------------------------------------------------------------------------
	
	/** Create the chart */
	private void initComponents() {
		setLayout(new FillLayout());
		
		// create chart
		xySeriesCollection = new XYSeriesCollection();
		chart = ChartFactory.createXYLineChart(null, null, null, 
				xySeriesCollection /*dataset*/, PlotOrientation.VERTICAL, 
				true /* with legend */,	true /* with tooltips */, false /* with urls */);
		ChartPanel chartPanel = new ChartPanel(chart, false /* use buffer? */);
		
		
		XYPlot plot = (XYPlot)chart.getPlot();
                plot.setRangeAxis(ya);
                plot.setDomainAxis(xa);
		plot.getDomainAxis().setAutoRange(autoRange);
		
		XYItemRenderer renderer = new StandardXYItemRenderer(StandardXYItemRenderer.LINES);
		
		plot.setRenderer(renderer);
		
        
        // set up gradient paints for series...
        GradientPaint gp0 = new GradientPaint(
            0.0f, 0.0f, Color.blue, 
            0.0f, 0.0f, new Color(0, 0, 64)
        );
        GradientPaint gp1 = new GradientPaint(
            0.0f, 0.0f, Color.green, 
            0.0f, 0.0f, new Color(0, 64, 0)
        );
        renderer.setSeriesPaint(0, gp0);
        renderer.setSeriesPaint(1, gp1);

		
			
		// display
		Frame frame = SWT_AWT.new_Frame(this);
		frame.add(chartPanel);
	}
	
	
	// -- getters/setters -------------------------------------------------------------------------
	
	/** Returns the dataset associated with this chart */
	public XYSeriesCollection getXYSeriesCollection() {
		return xySeriesCollection;
	}
	
	/** Adds a new series (=curve) to the dataset associated with this chart */
	public void addSeries(XYSeries xySeries) {
            
		xySeriesCollection.addSeries(xySeries);

	}
	
	public void setXRange(double min, double max) {
		((XYPlot)chart.getPlot()).getDomainAxis().setRange(min, max);
	}


        public void setexpAx(boolean expAx)
    {
            if(expAx)
                ya.setNumberFormatOverride(new DecimalFormat("0.###E0"));
            else
            {
                ya = new NumberAxis();
                ((XYPlot)chart.getPlot()).setRangeAxis(ya);
            }
        }
}
