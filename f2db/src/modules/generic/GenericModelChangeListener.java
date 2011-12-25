package modules.generic;
/**
 * All classes that want to get informed about some model changes, regardless of 
 * the model have <br>
 * <li>1st to implement this interface to react on changes
 * <li>2nd to be added to the model, through a call of addGenericModelChangeListener(listener who wants to listen)
 * <br><br>
 * The general contract is that parent components add their child widgets/classes/components
 * to the models they want them to listen to.
 * 
 * @author Felix Beyer
 * @date   13.01.2007
 *
 */
public interface GenericModelChangeListener {
    
    /** this method gets called by the model, if something has changed in the model
     * more details can be found in the event object 
     * @param event the event containing more information about the change
     */
	public void modelChanged(GenericModelChangeEvent event);
}
