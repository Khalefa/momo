package components.tabs;

import modules.databaseif.JDBCInterface;
import modules.misc.Constants;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.ExpandBar;
import org.eclipse.swt.widgets.ExpandItem;

import components.AbstractComponent;
import components.config.ConsoleConfig;
import components.config.DBConfig;
import components.config.DbImport;
import components.config.SystemConfig;

/**
 * This class is the configuration tab of the application. In this tab database connections 
 * can be configured, colors, fonts and styles can be adjusted and many things more
 * It consists of different expandbars - each of them is responsible for a tab or a module
 * 
 * @author Felix Beyer, Christopher Schildt
 * @date   08.05.2006
 *
 */
public class ConfigTab extends AbstractComponent{
	
	// the different config expandable categories
	private DBConfig dbconfig; // the database connection configuration
        private DbImport dbimport; // the database connection configuration
	private ConsoleConfig cconfig; // the console configuration
	private SystemConfig sysconfig; // the system configuration
	
	/** the constructor */
	public ConfigTab(Composite parent, int style) {
		super(parent, style);
	}
	
	/** this method inits the control components */
	protected void initComponents(){
		setLayout(new FillLayout(SWT.HORIZONTAL));
		ExpandBar bar = new ExpandBar (this, SWT.V_SCROLL);

		// Color color = this.getBackground();
		//bar.setBackground(this.getBackground());
		
		ExpandItem item0 = new ExpandItem (bar, SWT.NONE | SWT.BORDER, 0);
		item0.setText(Constants.config_db_title);
		dbconfig = new DBConfig (bar, SWT.NONE);
		item0.setHeight(dbconfig.computeSize(SWT.DEFAULT, SWT.DEFAULT).y);
		item0.setControl(dbconfig);
		//item0.setImage(image);

                ExpandItem item4 = new ExpandItem (bar, SWT.NONE | SWT.BORDER, 0);
		item4.setText(Constants.config_db_import);
		dbimport = DbImport.getInstance(bar, SWT.NONE);
		item4.setHeight(dbimport.computeSize(SWT.DEFAULT, SWT.DEFAULT).y);
		item4.setControl(dbimport);
		
		ExpandItem item1 = new ExpandItem (bar, SWT.NONE, 1);
		item1.setText(Constants.config_console_title);
		cconfig = new ConsoleConfig(bar,SWT.NONE);
		item1.setHeight(cconfig.computeSize(SWT.DEFAULT, SWT.DEFAULT).y);
		item1.setControl(cconfig);

		ExpandItem item2 = new ExpandItem (bar, SWT.NONE, 2);
		item2.setText(Constants.config_history_title);
		
        ExpandItem item3 = new ExpandItem (bar, SWT.NONE, 3);
        item3.setText(Constants.config_system_title);
        sysconfig = new SystemConfig(bar, SWT.NONE);
        item3.setHeight(150);
        item3.setControl(sysconfig);
		
		bar.setSpacing(8);
		//item0.setExpanded(true);
	}

	/** this method inits the listeners */
	protected void initListeners(){
		// add the dbconfig as listener for the JDBCInterface model
		JDBCInterface.getInstance().addModelChangeListener(dbconfig);
	}

	protected void activateControls() {}

	protected void deactivateControls() {}

	protected void updateComponent() {}

	public void reset() {}

}
