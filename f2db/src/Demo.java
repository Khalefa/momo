import modules.databaseif.JDBCInterface;
import modules.misc.Constants;
import modules.misc.ModuleRegistry;
import modules.misc.ResourceRegistry;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;

import components.main.MainContent;
import components.main.MainStatusLine;
import components.main.MainToolBar;
/**
 * This is the main class of the application.
 * 
 * @author Felix Beyer
 * @date   08.05.2006
 *
 */
public class Demo {
	
	public static void main(String[] args) {
		
		//initialize registry for modules & load modules
		ModuleRegistry.getInstance().init();

		// create display, load images and create shell 
		Display display = new Display();
		ResourceRegistry.getInstance().loadImages(display);
		
		// create main shell (the main window) 
		Shell mainShell = new Shell(display);
		// set default size, layout, title & window icon of main shell
		mainShell.setSize(Constants.Demo_Width,Constants.Demo_Height);
		mainShell.setLayout(new GridLayout());
		//mainShell.setMaximized(true);
		mainShell.setText(Constants.Demo_Title);
		mainShell.setImage(ResourceRegistry.getInstance().getImage("icon"));
		
		// create the top area with a MainToolBar
		MainToolBar mtb = new MainToolBar(mainShell,SWT.NONE);
		GridData gd = new GridData();
		gd.horizontalAlignment = GridData.FILL;
		gd.grabExcessHorizontalSpace = true;
		mtb.setLayoutData(gd);
		
		// create the main content area with all the different tabs
		MainContent main = new MainContent(mainShell,SWT.NONE);
		gd = new GridData();
		gd.verticalAlignment = GridData.FILL;
		gd.horizontalAlignment = GridData.FILL;
		gd.grabExcessHorizontalSpace = true;
		gd.grabExcessVerticalSpace = true;
		main.setLayoutData(gd);
		
		// Bottom Area StatusLine
		MainStatusLine sl = new MainStatusLine(mainShell,SWT.NONE);
		gd = new GridData();
		gd.horizontalAlignment = GridData.FILL;
		gd.grabExcessHorizontalSpace = true;
		sl.setLayoutData(gd);
		
		// inform the JDBCInterface about the main Shell
		JDBCInterface.getInstance().setShell(mainShell);
		
        // add a DisposeListener which get called when the user closes the application
		mainShell.addDisposeListener(new DisposeListener(){
            public void widgetDisposed(DisposeEvent e) {
                // shutdown modules
                ModuleRegistry.getInstance().shutdown();
                // dispose all resources held from the ResourceRegistry
                ResourceRegistry.getInstance().disposeAll();
            }});
        
        //shell.pack ();		
		
		// Main Application Loop
		mainShell.open();
		while (!mainShell.isDisposed()) {
			if (!display.readAndDispatch())
				display.sleep();
		}
		
		// last but not least, dispose display
		display.dispose();
        System.out.println("Good Bye :-)");
	}

}
