package components.threads;

import java.sql.ResultSet;
import java.sql.SQLException;

import math.random.MyRandom;
import modules.databaseif.JDBCInterface;

import org.eclipse.swt.widgets.Shell;
import org.jfree.data.xy.XYSeries;

import components.dialogs.ExceptionDialog;

public class ExecuteApproximateQuery extends Thread {
	
	private String approximateStmtBegin;
	private String approximateStmtEnd;
	private XYSeries xySeriesApproximate;
	private XYSeries xySeriesTimeApproximate;
	private double maximumSampleSize;
	private Shell shell;

	public ExecuteApproximateQuery(
			String asb,
			String ase,
			XYSeries xysa,
			XYSeries xysta,
			double maximumSampleSize,
			Shell shell
			)
	{
		this.approximateStmtBegin = asb;
		this.approximateStmtEnd = ase;
		this.xySeriesApproximate = xysa;
		this.xySeriesTimeApproximate = xysta;
		this.maximumSampleSize = maximumSampleSize;
		this.shell = shell;
	}

	public void run() {
		MyRandom myRandom = MyRandom.getInstance();
		while(!isInterrupted())
		{
			double x = (double)(Math.round(myRandom.nextDouble() * maximumSampleSize * 100))/100;
			if (x >= 0.1)
				try
				{
					long beginTime = System.nanoTime();
					ResultSet rsa = JDBCInterface.getInstance().executeStatementWithResult(approximateStmtBegin + x + approximateStmtEnd);
					rsa.next();
					long time = System.nanoTime() - beginTime;
					
					xySeriesApproximate.add(x, rsa.getDouble(1));
					xySeriesApproximate.fireSeriesChanged();
			
					xySeriesTimeApproximate.add(x, (double)time/1000000000);
					xySeriesTimeApproximate.fireSeriesChanged();
					
					rsa.close();
				}
				catch (SQLException e)
				{
					ExceptionDialog.show(shell, e, true);
				}
		}
	}
}
