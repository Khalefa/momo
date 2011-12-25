package components.config;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;

import components.widgets.TableWidget;

public class SystemConfig extends Composite{

    private Composite parent;
    private TableWidget sysProps;
    
    
    public SystemConfig(Composite parent, int style) {
        super(parent, style);
        this.parent = parent;
        initComponents();
        initListeners();
    }
    
    private void initComponents(){
       setLayout(new FillLayout());
       sysProps = new TableWidget(this, SWT.NONE);
       sysProps.createTable(System.getProperties(),"System key","System value");
    }
    
    private void initListeners(){
        addListener(SWT.Show, new Listener() {
            public void handleEvent(Event event) {              
                sysProps.createTable(System.getProperties(),"System key","System value");
            }
        });

    }
    
   
}
