package modules.databaseif;

import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
/**
 * Helper Thread which counts seconds and updates a provided 
 * SWT Label with the current seconds passed by.
 * 
 * @author Felix Beyer
 * @date   27.07.2006
 *
 */
public class SecondsCounterThread extends Thread {
	
	private boolean stop=false; // stop flag
	private Label current; // the label to update
	private long startTime; // the start timestamp as basis of counting
	/**
	 * The constructor
	 * @param startTime the start time to count from
	 * @param current the current active label, which gets updated
	 */
	public SecondsCounterThread(long startTime, Label current){
		super("counter"+startTime);
		this.startTime = startTime;
		this.current = current;
	}
	/**
	 * sets a new start time to count up from
	 * @param startTime the start time as timestamp
	 */
	public void setStartTime(long startTime){
		this.startTime = startTime;
	}
	/**
	 * sets the current active label, which gets updated from the counter
	 * @param label
	 */
	public void setCurrentLabel(Label label){
		this.current = label;
	}
	/**
	 * sets the stop flag
	 * @param stop true indicates the counter to return from run loop and
	 * end thread execution
	 */
	public void setStop(boolean stop){
		this.stop = stop;
	}
	/**
	 * when it gets active, the current seconds passed by are printed into the label.
	 * Thread tries to sleep every 100ms.
	 */
	public void run(){
		while(!stop){
			// send seconds update event
			if (current != null)
				Display.getDefault().asyncExec(new Runnable() {
					public void run() {
						if(!current.isDisposed()){
							long time = System.currentTimeMillis() - startTime; 
							current.setText((time/1000) + "." + (time/100 % 10)+ "s");
							current.pack();
						}
					}});
			try {
				Thread.sleep(100);
			} catch (InterruptedException e) {
				System.out.println("InterruptException occured... in secCounterThread!");
				e.printStackTrace();
			}

		}
	}

}
