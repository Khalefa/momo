package components.dialogs;

import java.text.SimpleDateFormat;
import java.util.Date;

import modules.misc.StringUtils;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.TabFolder;
import org.eclipse.swt.widgets.TabItem;

/**
 * This dialog does the same as ExceptionDialog, but it shows more than one exceptions in tabs. 
 * 
 * @author Sebastian Seifert,Christopher Schildt
 *
 */
public class ExceptionTabDialog extends ExceptionDialog { 
	
	/** the tabfolder for the tabs */
	private TabFolder tabFolder; 
	
	protected ExceptionTabDialog(Shell parent, Exception e){
		super(parent, e);
	}

        protected ExceptionTabDialog(Shell parent, String e){
		super(parent, e);
	}
	/**
	 * @see components.dialogs.ExceptionDialog#initSpecialComponent(org.eclipse.swt.widgets.Composite)
	 */
	protected void initSpecialComponent(Composite composite){
		// TabFolder
		tabFolder = new TabFolder(composite, SWT.NONE);
		//tabFolder.setLayout(new FillLayout());
		tabFolder.setLayout(new GridLayout());
		tabFolder.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		//tabFolder.setSize(400,600);
		//tabFolder.getParent().pack();
        // add a DisposeListener which get called when the user closes the application

	}

	/**
	 * adds an exception to be show in an new tab on this dialog
	 * @param e
	 */
	public void addException(Exception e){
		TabItem item = new TabItem (tabFolder, SWT.NONE);
		item.setText(new SimpleDateFormat("HH:mm:ss.SSS").format(new Date()) + " " + StringUtils.cutPackageName(e.getClass().getCanonicalName()));
		
		item.setControl(createStyledTextForException(tabFolder, e));
		
		//tabFolder.computeSize(100, 200, true);
		//tabFolder.setSize(200, 300);
		//tabFolder.pack();
		
		//tabFolder.setSize(300, 300);
	}
        /**
	 * adds an exception to be show in an new tab on this dialog
	 * @param e
	 */
	public void addString(String e){
		TabItem item = new TabItem (tabFolder, SWT.NONE);
		item.setText(new SimpleDateFormat("HH:mm:ss.SSS").format(new Date()) + " " + e);

		item.setControl(createStyledTextForString(tabFolder, e));

		//tabFolder.computeSize(100, 200, true);
		//tabFolder.setSize(200, 300);
		//tabFolder.pack();

		//tabFolder.setSize(300, 300);
	}
	
	/**
	 * to know when the window is closed
	 * @param listener
	 */
	protected void addDisposeListener(DisposeListener listener){
		tabFolder.addDisposeListener(listener);
	}
}
