package components;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;

/**
 * This abstract component implements a generic behaviour of the component.
 * It consists of hook methods, which have to be implemented by sub-classes.
 * 
 * 
 * @author Felix Beyer
 * @date   27.07.2006
 *
 */
public abstract class AbstractComponent extends Composite 
										implements Component{
	/**
	 * The default constructor of a component.
	 * @param parent the parent composite
	 * @param style the style constants for this component
	 */
	public AbstractComponent(Composite parent, int style) {
		// call super implementation
		super(parent, style);
		
		// init components and listeners and deactivate all controls
		initComponents();
		deactivateControls();
		initListeners();
		
		// register listener which calls update method, when
		// the show event is thrown
		addListener(SWT.Show, new Listener() {
			public void handleEvent(Event event) {				
				updateComponent();
			}
		});
        
	}
	
	/** 
     * This method is responsible for the initialization 
	 * of the GUI. It gets called from the constructor at first.
	 */
	protected abstract void initComponents();
	
	/**
	 * This method initializes the different listeners which 
	 * are bound to the components. The listeners describe the
	 * control part of the component. This method gets called
	 * from the constructor right after the components 
	 * have been initialized.
	 *
	 */
	protected abstract void initListeners();
	
	/**
	 * This method should activate all controls. It can be called after
	 * a special event occurs (mostly after the connection_established event)
	 *
	 */
	protected abstract void activateControls();
	
	/**
	 * This method should deactivate all controls. It can be called after
	 * a special event occurs (mostly after the disconnect event)
	 *
	 */
	protected abstract void deactivateControls();
	
	/**
	 * This method is responsible for the update of the contents 
	 * of the component. It gets called from a default listener,
	 * which is initialized in the constructor and listens for the
	 * SWT Show Event. Everytime the user switches to this component,
	 * this method gets called automatically.
	 */
	protected abstract void updateComponent();
	
}
