package components.config;

import modules.config.Configuration;
import modules.databaseif.ConnectionInfo;
import modules.databaseif.JDBCInterface;
import modules.generic.GenericModelChangeEvent;
import modules.generic.GenericModelChangeListener;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;

/**
 *
 * @author Christopher Schildt
 */
public class DbImport extends Composite implements GenericModelChangeListener {

    private Composite parent;
    private Combo connectionComboBox;
    private Label aliasConnectionLabel;
    private Label statusLabel;
    private Label statusText;
    private Composite buttonComposite;
    private Button edemandCheck;
    private Button tcphCheckbox;
    private Button okButton;
    private Button salesCheckbox;
    private Button australiaCheckbox;

    private static DbImport instance;


    public static DbImport getInstance()
    {
         if(instance!=null)
                return instance;
            else
                return null;
    }

     public static DbImport getInstance(Composite parent, int style)
    {
         if(instance==null)
            {
                instance=new DbImport(parent, style);
                return instance;
            }
            if(instance.parent!=parent)
            {
              instance=new DbImport(parent, style);
                return instance;
            }
            return instance;
     }

    /** constructor */
    private DbImport(Composite parent, int style) {
        super(parent, style);
        this.parent = parent;
        initComponents();
        initListeners();
        updateComboBox();
    }

    public void modelChanged(GenericModelChangeEvent event) {
    }

    private void initComponents() {

        setLayout(new GridLayout(3, false));
        setSize(306, 146);
        {
            aliasConnectionLabel = new Label(this, SWT.NONE);
            aliasConnectionLabel.setText("Connection alias:");
        }
        {
            GridData connectionComboBoxLData = new GridData();
            connectionComboBoxLData.horizontalAlignment = GridData.FILL;
            connectionComboBoxLData.grabExcessHorizontalSpace = true;
            connectionComboBox = new Combo(this, SWT.READ_ONLY);
            connectionComboBox.setLayoutData(connectionComboBoxLData);
            connectionComboBox.setEnabled(true);
        }
        {
            new Label(this, SWT.NONE);
        }
        {
            buttonComposite = new Composite(this, SWT.NONE);
            GridLayout buttonCompositeLayout = new GridLayout();
            buttonCompositeLayout.makeColumnsEqualWidth = true;
            buttonCompositeLayout.numColumns = 4;
            GridData buttonCompositeLData = new GridData();
            buttonCompositeLData.horizontalSpan = 3;
            buttonCompositeLData.grabExcessHorizontalSpace = true;
            buttonCompositeLData.horizontalAlignment = GridData.FILL;
            buttonCompositeLData.grabExcessVerticalSpace = true;
            buttonComposite.setLayoutData(buttonCompositeLData);
            buttonComposite.setLayout(buttonCompositeLayout);
            edemandCheck = new Button(buttonComposite, SWT.CHECK
                    | SWT.LEFT);
            GridData createCheckboxLData = new GridData();
            //createCheckboxLData.grabExcessVerticalSpace = true;
            edemandCheck.setLayoutData(createCheckboxLData);
            edemandCheck.setText("create/import edmand");

            {
                tcphCheckbox = new Button(buttonComposite, SWT.CHECK
                        | SWT.LEFT);
                createCheckboxLData = new GridData();
                //createCheckboxLData.grabExcessVerticalSpace = true;
                tcphCheckbox.setLayoutData(createCheckboxLData);
                tcphCheckbox.setText("create/import tpch");
            }
            {
                salesCheckbox = new Button(buttonComposite, SWT.CHECK
                        | SWT.LEFT);
                GridData tpchCheckboxLData = new GridData();
                //tpchCheckboxLData.grabExcessVerticalSpace = true;
                salesCheckbox.setLayoutData(tpchCheckboxLData);
                salesCheckbox.setText("create/import sales");

            }
            {
                australiaCheckbox = new Button(buttonComposite, SWT.CHECK
                        | SWT.LEFT);
                GridData tpchCheckboxLData = new GridData();
                //tpchCheckboxLData.grabExcessVerticalSpace = true;
                australiaCheckbox.setLayoutData(tpchCheckboxLData);
                australiaCheckbox.setText("create/import australia");

            }
            {
                statusLabel = new Label(this, SWT.NONE);
                statusLabel.setText("Status:");
            }
            {
                GridData statusTextLData = new GridData();
                statusTextLData.horizontalAlignment = GridData.FILL;
                statusTextLData.grabExcessHorizontalSpace = true;
                statusText = new Label(this, SWT.NONE);
                statusText.setText("");
                statusText.setEnabled(true);
                statusText.setLayoutData(statusTextLData);

            }
            okButton = new Button(this, SWT.NONE);
            okButton.setText("Execute");
        }
    }

    private void initListeners() {

        okButton.addListener(SWT.Selection, new Listener() {

            public void handleEvent(org.eclipse.swt.widgets.Event event) {

                statusText.setText("working");
                ConnectionInfo old = JDBCInterface.getInstance().getCurrentConnectionInfo();
                if (old != null) {
                    JDBCInterface.getInstance().disconnect();
                }
                ConnectionInfo newCon = new ConnectionInfo();
                ConnectionInfo[] cis = Configuration.getInstance().getConnectionInfos();
                boolean test;
                boolean status = false;
              // DEBUG TEST// JDBCInterface.getInstance().importfiles(newCon, edemandCheck.getSelection(), salesCheckbox.getSelection(), australiaCheckbox.getSelection(), tcphCheckbox.getSelection());

                for (int i = 0; i < cis.length; i++) {
                    if (cis[i].getAlias().equals(connectionComboBox.getText())) {
                        test = JDBCInterface.getInstance().connect(cis[i]);
                       

                        if (test) {
                            status = JDBCInterface.getInstance().importfiles(newCon, edemandCheck.getSelection(), salesCheckbox.getSelection(), australiaCheckbox.getSelection(), tcphCheckbox.getSelection());

                        }



                    }
                    break;
                }
                JDBCInterface.getInstance().disconnect();
                if (old != null) {
                    JDBCInterface.getInstance().connect(old);
                }
                if (!status) {
                    statusText.setText("failed");
                } else {
                    statusText.setText("success");
                }
            }
        });

    }

    public void updateComboBox() {
        connectionComboBox.removeAll();
        ConnectionInfo[] cons = Configuration.getInstance().getConnectionInfos();
        for (int i = 0; i < cons.length; i++) {
            connectionComboBox.add(cons[i].getAlias());
        }


    }
}
