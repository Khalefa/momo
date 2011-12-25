package components.tabs;

import java.io.File;

import org.eclipse.swt.SWT;
import org.eclipse.swt.browser.Browser;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Composite;


/**
 * This is the About Tab.
 * @author Rainer Gemulla
 * @date   27.07.2006
 *
 */
public class AboutTab extends Composite {
	
	/**
	 * default constructor
	 */
	public AboutTab(Composite parent, int style) {
		super(parent, style);
		initComponents();
	}
	
	/**
	 * inits the gui components of this tab
	 */
	private void initComponents() {
		setLayout(new FillLayout());
		Browser browser = new Browser(this, SWT.NONE);
		browser.setUrl("file://" + new File(".").getAbsolutePath() 
				+ "/resources/about/about.html");
	}
}
