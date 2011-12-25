package components.listeners;

import org.eclipse.swt.custom.LineStyleEvent;
import org.eclipse.swt.custom.LineStyleListener;

public abstract class AbstractLineStyleListener implements LineStyleListener{

    public abstract void decorate(LineStyleEvent event);
    
    public void lineGetStyle(LineStyleEvent event) {
        decorate(event);
    }

}
