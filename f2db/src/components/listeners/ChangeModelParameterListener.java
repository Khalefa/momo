package components.listeners;

import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Text;



/**
 * A listener to set default parameters
 * @author Christopher Schildt
 * @date   08.05.2006
 *
 */
public class ChangeModelParameterListener implements SelectionListener {
	
	Text par;

	public ChangeModelParameterListener(Text parameters) {
		this.par=parameters;
		// TODO Auto-generated constructor stub
	}

	@Override
	public void widgetDefaultSelected(SelectionEvent arg0) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void widgetSelected(SelectionEvent arg0) {
		// TODO Auto-generated method stub
		Combo a=((Combo)arg0.getSource());
		String selected=a.getItem(a.getSelectionIndex());
		if(selected.compareTo("HWMODEL")==0)
		{
			par.setText("(has_season=1,has_trend=1,period=12,error=\'SSE\')");
			par.setToolTipText("Available Parameters are: alpha, beta, gamma, err, has_season, has_trend, period");
		}
		else
		{
			par.setText("(ar=1,ma=0,d=0,sar=1,sma=0,sd=0,period=12,error=\'SSE\')");
			par.setToolTipText("Available Parameters are: ar,sar,ma,sma,d,sd,period,err");
		}
		
	}

}
