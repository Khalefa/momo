package components.main;
import modules.misc.Constants;
import modules.misc.ResourceRegistry;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
/**
 * This class represents the bottom area of the application.<br/>
 * It provides a status label, which can be used 
 * to print out status messages for the user.
 * @author Felix Beyer
 * @date   12.04.2006
 *
 */
public class MainStatusLine extends Composite {

	private static Label label;
	private static String spaces = "                                                                                ";// 80 space chars
	private static Color red,black;
	
	/** the constructor */
	public MainStatusLine(Composite parent, int style) {
		super(parent, style);
		this.setLayout(new FillLayout());
		label = new Label(this,SWT.NONE);
		label.setText(Constants.main_status);
		
		// get system colors and hold a reference
		black = ResourceRegistry.getInstance().getColor(this.getDisplay(),0,0,0);
		red   = ResourceRegistry.getInstance().getColor(this.getDisplay(),255,0,0);
		
	}
	/**
	 * This method prints out an message in the status line.
	 * 
	 * @param msg the message to print out
	 * @param error boolean value, which changes the text color<br/>
	 *  <li> when set to true, the color of the message will be <font color="255,0,0">red</font>, indicating an error message</li>
	 * 	<li> when set to false, the color of the message will be normal black, indicating a normal status message</li>
	 */
	public static void message(String msg,boolean error){
		if(msg!=null){
			int l = msg.length();
			if (l<80){
				msg += spaces.substring(l);
			} else {
				msg = msg.substring(0,79);
			}
			if(error){ 
				label.setForeground(red);
			} else {
				label.setForeground(black);
			}
			label.setText(msg);
		}
	}

}
