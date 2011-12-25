package modules.generic;

/**
 * This interface defines all methods, which have to be implemented by a model to listen 
 * @author Felix Beyer
 * @date   06.05.2006
 *
 */
public interface GenericModel {
	/**
	 * add a ModelChangeListener to the listener list
	 * @param listener the listener to add
	 */
	public void addModelChangeListener(GenericModelChangeListener listener);
	/**
	 * remove the ModelChangeListener from the listener list
	 * @param listener the listener to remove from the list
	 */
	public void removeModelChangeListener(GenericModelChangeListener listener);
	/**
	 * this method notifies all listeners about the specified ModelChangeEvent 
	 * @param event the ModelChangeEvent the listeners will be informed about
	 */
	public void fireModelChangeEvent(GenericModelChangeEvent event);

}
