package modules.chart;

import java.awt.Color;
import java.awt.Frame;
import java.awt.GradientPaint;

import org.eclipse.swt.SWT;
import org.eclipse.swt.awt.SWT_AWT;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Composite;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.CategoryPlot;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.renderer.category.StatisticalBarRenderer;
import org.jfree.data.statistics.DefaultStatisticalCategoryDataset;

public class BarChartAdapter extends Composite{
	

	// -- variables -------------------------------------------------------------------------------
	
	/** the chart */
	protected JFreeChart chart;
	
//	/** the dataset */
//	protected DefaultStatisticalCategoryDataset dataSet;
	
	private Frame frame;
	private ChartPanel panel;
	
	// -- constructors ----------------------------------------------------------------------------
	
	public BarChartAdapter(Composite parent, int style) {
		super(parent, style | SWT.EMBEDDED);
		initComponents();
	}
	
	// -- gui stuff -------------------------------------------------------------------------------

	/** Create the chart */
	private void initComponents() {
		setLayout(new FillLayout());
		
		// create the chart...
        chart = ChartFactory.createBarChart(
            null,         			  // chart title
            null,               	  // domain axis label
            null,                     // range axis label
            null,                  // data
            PlotOrientation.VERTICAL, // orientation
            true,                     // include legend
            true,                     // tooltips?
            false                     // URLs?
        );
		
        panel = new ChartPanel(chart, false /* use buffer? */);
		
        // set the background color for the chart...
        chart.setBackgroundPaint(Color.white);
        
        // get a reference to the plot for further customisation...
        CategoryPlot plot = chart.getCategoryPlot();
        
        //plot.setBackgroundPaint(Color.lightGray);
        plot.setDomainGridlinePaint(Color.white);
        plot.setDomainGridlinesVisible(true);
        plot.setRangeGridlinePaint(Color.white);
        
        // disable bar outlines...
        StatisticalBarRenderer renderer = new StatisticalBarRenderer();
        plot.setRenderer(renderer);
        
        renderer.setDrawBarOutline(false);
        renderer.setItemMargin(0.0);
        
        // no gradient paints for series...
        GradientPaint gp0 = new GradientPaint(
            0.0f, 0.0f, Color.blue, 
            0.0f, 0.0f, Color.blue
        );
        GradientPaint gp1 = new GradientPaint(
            0.0f, 0.0f, Color.green, 
            0.0f, 0.0f, Color.green
        );
        renderer.setSeriesPaint(0, gp0);
        renderer.setSeriesPaint(1, gp1);
		
		// display
		frame = SWT_AWT.new_Frame(this);
		frame.add(panel);
	    
	}
	
	
	// -- getters/setters -------------------------------------------------------------------------
	
//	/**
//	 * returns the dataSet associated with this chart
//	 */
//	public DefaultStatisticalCategoryDataset getDataSet() {
//		return dataSet;
//	}
	
	public void setDataset(String range,String domain, DefaultStatisticalCategoryDataset set){
		
        
        // get a reference to the plot for further customisation...
        CategoryPlot plot = chart.getCategoryPlot();
        
        plot.getDomainAxis().setLabel(domain);
        plot.getRangeAxis().setLabel(range);
        
        // set the new dataset
        plot.setDataset(set);

	}

	public JFreeChart getChart() {
		return chart;
	}

}
