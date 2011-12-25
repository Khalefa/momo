/**
 * 
 */
package modules.chart;

import java.awt.Color;
import java.awt.Frame;
import java.awt.GradientPaint;
import java.awt.Rectangle;

import org.eclipse.swt.SWT;
import org.eclipse.swt.awt.SWT_AWT;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Composite;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;

/**
 * @author torsten
 *
 */
public class ScatterPlotAndLineChartAdapter extends Composite {

//	----- variables -----------------------------------------
	
	JFreeChart chart;						// chart
	XYSeriesCollection xySeriesCollection;	// dataset
	
//	----- constructor ---------------------------------------
	
	public ScatterPlotAndLineChartAdapter(Composite parent, int style) {
		super(parent, style | SWT.EMBEDDED);
		initComponents();
	}
	
	
//	----- gui stuff -----------------------------------------
	
	/** Create the chart */
	private void initComponents() {
		setLayout(new FillLayout());
		
		// create chart
		xySeriesCollection = new XYSeriesCollection();
		chart = ChartFactory.createScatterPlot(null, null, null, 
				xySeriesCollection /*dataset*/, PlotOrientation.VERTICAL, 
				true /* with legend */,	true /* with tooltips */, false /* with urls */);
		ChartPanel chartPanel = new ChartPanel(chart, false /* use buffer? */);
		
		
		XYPlot plot = (XYPlot)chart.getPlot();
		
		XYLineAndShapeRenderer renderer = new XYLineAndShapeRenderer();
		plot.setRenderer(renderer);
		
        
        // set up gradient paints for series...
        GradientPaint gp0 = new GradientPaint(
            0.0f, 0.0f, Color.red, 
            0.0f, 0.0f, new Color(0, 0, 64)
        );
        GradientPaint gp1 = new GradientPaint(
        	0.0f, 0.0f, Color.black,
        	0.0f, 0.0f, Color.black);
        
        GradientPaint gp2 = new GradientPaint(
            0.0f, 0.0f, Color.blue, 
            0.0f, 0.0f, new Color(0, 64, 0)
        );
        renderer.setSeriesPaint(0, gp0);
        renderer.setSeriesLinesVisible(0, true);
        renderer.setSeriesShapesVisible(0, false);

        renderer.setSeriesPaint(1, gp2);
        renderer.setSeriesLinesVisible(1, false);
        renderer.setSeriesShapesVisible(1, true);
        renderer.setSeriesShape(1, new Rectangle(-1, -1, 2, 2));
        renderer.setSeriesShapesFilled(1, false);
        
        renderer.setSeriesPaint(2, Color.black);
        renderer.setSeriesLinesVisible(2, true);
        renderer.setSeriesShapesVisible(2, false);

        renderer.setSeriesPaint(3, gp1);
        renderer.setSeriesLinesVisible(3, true);
        renderer.setSeriesShapesVisible(3, false);
        renderer.setSeriesVisibleInLegend(3, false);
        

		
			
		// display
		Frame frame = SWT_AWT.new_Frame(this);
		frame.add(chartPanel);
	}
	
	
//	----- setter -------------------------------------------
	/** Adds a new series (=curve) to the dataset associated with this chart */
	public void addSeries(XYSeries xySeries) {
		xySeriesCollection.addSeries(xySeries);
	}

}
