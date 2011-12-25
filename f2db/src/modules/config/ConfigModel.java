package modules.config;

import java.util.ArrayList;
import java.util.List;

import modules.generic.GenericModel;
import modules.generic.GenericModelChangeEvent;
import modules.generic.GenericModelChangeListener;
/**
 * The ConfigModel is model tailored to the needs of the configuration.
 * It implements the GenericModel interface. Listeners can observe the status of
 * the configuration. When something has changed a Configuration_Changed Event
 * will be thrown and all listeners which are observing this model, get informed about that.
 * 
 * @author Felix Beyer
 * @date   27.07.2006
 *
 */
public class ConfigModel implements GenericModel {
	
	//member variables
	private static ConfigModel singleton; // the singleton instance
	
	private List<GenericModelChangeListener> listeners; // an array for the different listeners
	
	/**
	 * private constructor
	 *
	 */
	private ConfigModel() {
		this.listeners = new ArrayList<GenericModelChangeListener>();
	}
	
	/**
	 * @return Instance of a ConfigModel
	 */
	public static ConfigModel getInstance(){
		if(singleton == null) {
			singleton = new ConfigModel();
		}
		return singleton;
	}
	

	// --------------------------- GenericModel Interface ----------------
	
	public void addModelChangeListener(GenericModelChangeListener listener) {
		this.listeners.add(listener);
	}

	public void removeModelChangeListener(GenericModelChangeListener listener) {
		this.listeners.remove(listener);
	}

	public void fireModelChangeEvent(GenericModelChangeEvent event) {
		for(GenericModelChangeListener listener:this.listeners){
			listener.modelChanged(event);
		}
	}

}
