package components.listeners;

import org.eclipse.swt.custom.LineStyleEvent;
/**
 * this is a decoratable linestylelistener
 * @author Felix Beyer
 * @date   18.11.2006
 *
 */
public abstract class DecoratedLineStyleListener extends AbstractLineStyleListener {

    protected AbstractLineStyleListener listener;
    
    public DecoratedLineStyleListener(AbstractLineStyleListener listener){
        this.listener = listener;
    }
    
    public void decorate(LineStyleEvent event) {
        listener.decorate(event);
    }

    public void setListener(AbstractLineStyleListener listener){
        this.listener = null; // set old one to null
        this.listener = listener; // save new one
    }
}
