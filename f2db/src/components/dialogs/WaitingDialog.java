package components.dialogs;

import modules.misc.ResourceRegistry;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.ProgressBar;
import org.eclipse.swt.widgets.Shell;

/**
 * This class implements a little dialog with a progressbar. It executes a
 * runnable and shows that it works in the progressbar. The dialog can be
 * closed, but then the underlying workerthread is interrupted but it must not
 * be stopped.
 * 
 * @author Sebastian Seifert
 */
public class WaitingDialog extends Thread {
	
	/**
	 * The dialog shell
	 */
	private Shell shell;
	
	/**
	 * the cancel button
	 */
	private Button cancelButton;
	
	/**
	 * The underlying thread that is executed
	 */
	private Thread thread;
	
	/**
	 * The progressbar
	 */
	private ProgressBar progressBar;
	
	/**
	 * Creates a new Dialog.
	 * 
	 * @param parent the parent shell
	 * @param runnable the runnable object which should be executed
	 */
	private WaitingDialog(Shell parent, Runnable runnable, boolean interruptable){
		super("WaitingDialog");
		thread = new Thread(runnable, "WaitingDialog - Runnable");
		
		shell = new Shell(parent, SWT.APPLICATION_MODAL | SWT.TITLE);
		shell.setText("Waiting ...");
		shell.setLayout(new GridLayout());
		
		shell.setImage(ResourceRegistry.getInstance().getImage("icon"));
		
		progressBar = new ProgressBar(shell, SWT.HORIZONTAL | SWT.INDETERMINATE);
		progressBar.setMinimum(0);
		progressBar.setMaximum(100);
		progressBar.setSelection(0);
		
		cancelButton = new Button(shell, SWT.None);
		cancelButton.setText("cancel");
		cancelButton.setEnabled(interruptable);
		cancelButton.setLayoutData(new GridData(GridData.CENTER, GridData.CENTER, false, false));
		
		if (interruptable)
			cancelButton.addListener(SWT.Selection, new Listener() {
				public void handleEvent(Event e) {
					thread.interrupt();
				}
			});
		
		shell.pack();
		
		shell.addDisposeListener(new DisposeListener(){
			public void widgetDisposed(DisposeEvent e){
				thread.interrupt();
			}
		});

		shell.open();
	}
	
	/**
	 * @see java.lang.Thread#run()
	 */
	public void run(){
		thread.start();
		while (thread.isAlive() && !shell.isDisposed()){
			Display.getDefault().asyncExec(new Runnable() {
				public void run() {
					if (!shell.isDisposed())
						progressBar.setSelection((progressBar.getSelection() + 1) % 100);
				}});
			try {
				Thread.sleep(100);
			} catch (InterruptedException e){}
		}
		
		Display.getDefault().asyncExec(new Runnable() {
			public void run() {
				shell.dispose();
			}});
	}

	public static void show(Shell parent, Runnable runnable){
		show(parent, runnable, false);
	}

	public static void show(Shell parent, Runnable runnable, boolean interruptable){
		new WaitingDialog(parent, runnable, interruptable)
			.start();
	}

}
