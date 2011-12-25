package components.dialogs;

import components.listeners.ChangeModelParameterListener;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.LinkedList;
import java.util.List;

import modules.databaseif.JDBCInterface;
import modules.misc.Constants;
import modules.misc.ResourceRegistry;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.layout.RowLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

import components.listeners.DefaultLineStyleListener;
import components.listeners.SQLKeywordLineStyleListener;
import components.listeners.UpperKeyListener;

/**
 * This is a helper dialog for the create model statement
 * @author Christopher Schildt
 * @date   08.05.2006
 *
 */
public class CreateModelDialog {

    private String result = null;
    private Shell parent;
    private Shell shell;
    // widgets
    private Group creationGroup;
    private Label createModelLabel;
    private Text createModelName;
    private Label asLabel;
    private StyledText selectText;
    private Combo meassurecolumn;
    private Combo timecolumn;
    private Combo storage;
    private Composite sizeContainer;
    private Button tuplesRadioButton;
    private Label meassureLabel;
    private Label algLabel;
    private Label timeLabel;
    private Label storageLabel;
    private Text sizeText;
    private Text parameters;
    private Label parametersLabel;
    private Combo algorithmComboBox;
    private Group optimizationGroup;
    private Button useOptimization;
    private StyledText aggregationsST;
    private Button aggregationsRadioButton;
    private StyledText groupByST;
    private Button groupByRadioButton;
    private Text periodText;
    private Button everyRadioButton;
    private Button onDemandRadioButton;
    private Button completeRadioButton;
    private Button fastRadioButton;
    private Button immediateRadioButton;
    private Group statementGroup;
    private StyledText statementText;
    private Composite buttonComposite;
    private Button cancelButton;
    private Button okButton;
    private Button optiButton;
    private String[] relSamples;


    // **********************************************************************
    // Constructors
    // **********************************************************************
    public CreateModelDialog(Shell parent) {
        this.parent = parent;
        this.relSamples = new String[2];
        relSamples[0] = "HWMODEL";
        relSamples[1] = "ARMODEL";

    }


    /**
     * this method opens the dialog
     * @return only a string indicating the button state
     */
    public String open() {
        shell = new Shell(parent, SWT.DIALOG_TRIM | SWT.APPLICATION_MODAL | SWT.RESIZE);
        shell.setText(getText());


        initComponents();
        initListeners();

        activatecomponents();
        shell.setImage(ResourceRegistry.getInstance().getImage("icon"));

        shell.open();
        Display display = parent.getDisplay();
        while (!shell.isDisposed()) {
            if (!display.readAndDispatch()) {
                display.sleep();
            }
        }
        return result;
    }


    /**
     * this method opens the dialog
     * @return only a string indicating the button state
     */
    public String open(String training,String time,String measure) {
        shell = new Shell(parent, SWT.DIALOG_TRIM | SWT.APPLICATION_MODAL | SWT.RESIZE);
        shell.setText(getText());


        initComponents();
        initListeners();

        selectText.setText(training);
        timecolumn.setText(time);
        meassurecolumn.setText(measure);
        createModelName.setText(time+"_"+measure);
        storage.setText("ModelGraph");
        activatecomponents();
        shell.setImage(ResourceRegistry.getInstance().getImage("icon"));

        shell.open();
        Display display = parent.getDisplay();
        while (!shell.isDisposed()) {
            if (!display.readAndDispatch()) {
                display.sleep();
            }
        }
        return result;
    }

    /**
     * this method inits all ui widgets of this dialog
     */
    private void initComponents() {
        GridLayout dialogShellLayout = new GridLayout();
        shell.setLayout(dialogShellLayout);
        dialogShellLayout.numColumns = 6;
        shell.layout();
        shell.pack();
        shell.setSize(730, 687);
        {
            creationGroup = new Group(shell, SWT.NONE);
            GridLayout creationGroupLayout = new GridLayout();

            creationGroup.setLayout(creationGroupLayout);
            GridData creationGroupLData = new GridData();
            creationGroupLData.grabExcessHorizontalSpace = true;
            creationGroupLData.horizontalAlignment = GridData.FILL;
            creationGroupLData.horizontalSpan = 6;
            creationGroup.setLayoutData(creationGroupLData);
            creationGroup.setText(Constants.model_create_title3);
            {
                createModelLabel = new Label(creationGroup, SWT.NONE);
                GridData CreateModelLabelLData = new GridData();
                CreateModelLabelLData.grabExcessHorizontalSpace = true;
                createModelLabel.setLayoutData(CreateModelLabelLData);
                createModelLabel.setText(Constants.model_create_name);
            }
            {
                createModelName = new Text(creationGroup, SWT.BORDER);
                GridData createModelNameLData = new GridData();
                createModelNameLData.horizontalAlignment = GridData.FILL;
                createModelNameLData.horizontalSpan = 5;
                createModelNameLData.grabExcessHorizontalSpace = true;
                createModelName.setLayoutData(createModelNameLData);
            }
            {
                asLabel = new Label(creationGroup, SWT.NONE);
                GridData asLabelLData = new GridData();
                asLabelLData.verticalAlignment = GridData.BEGINNING;
                asLabel.setLayoutData(asLabelLData);
                asLabel.setText(Constants.model_create_select);
            }
            {
                GridData selectTextLData = new GridData();
                selectTextLData.horizontalAlignment = GridData.FILL;
                selectTextLData.verticalAlignment = GridData.FILL;
                selectTextLData.horizontalSpan = 5;
                selectTextLData.widthHint = 600;
                selectTextLData.heightHint = 103;
                selectTextLData.grabExcessHorizontalSpace = true;
                selectTextLData.grabExcessVerticalSpace = true;
                selectText = new StyledText(creationGroup, SWT.H_SCROLL
                        | SWT.V_SCROLL
                        | SWT.BORDER);
                selectText.setLayoutData(selectTextLData);
                selectText.setText("SELECT e_time, SUM(e_amount) am \n\tFROM edemand\n\tWHERE e_customer=\'me0\'\n\tGROUP BY e_time ORDER BY e_time");
            }
            {
                sizeContainer = new Composite(creationGroup, SWT.NONE);
                RowLayout sizeContainerLayout = new RowLayout(
                        org.eclipse.swt.SWT.HORIZONTAL);
                GridData sizeContainerLData = new GridData();
                sizeContainerLData.grabExcessHorizontalSpace = true;
                sizeContainerLData.horizontalAlignment = GridData.FILL;
                sizeContainerLData.horizontalSpan = 3;
                sizeContainerLData.verticalAlignment = GridData.FILL;
                sizeContainer.setLayoutData(sizeContainerLData);
                sizeContainer.setLayout(sizeContainerLayout);
                {
 

                    meassureLabel = new Label(sizeContainer, SWT.NONE);
                    meassureLabel.setText(Constants.model_create_of);

                }
                {
 

                    meassurecolumn = new Combo(sizeContainer, SWT.DROP_DOWN);

                    meassurecolumn.setEnabled(true);


                }
                {
                    timeLabel = new Label(sizeContainer, SWT.NONE);
                    timeLabel.setText(Constants.model_create_on);

                }
                {


                    timecolumn = new Combo(sizeContainer, SWT.DROP_DOWN);

                    timecolumn.setEnabled(true);

                }
                 {
                    storageLabel = new Label(sizeContainer, SWT.NONE);
                    storageLabel.setText(Constants.model_create_storage);

                }
                {


                    storage = new Combo(sizeContainer, SWT.DROP_DOWN);
                    storage.add("OFF");
                    storage.add("TABLE");
                    storage.add("MODELGRAPH");
                    storage.add("MODELINDEX");

                    timecolumn.setEnabled(true);

                }
            }
        }
        {
            optimizationGroup = new Group(shell, SWT.NONE);
            GridLayout optimizationGroupLayout = new GridLayout();

            optimizationGroup.setLayout(optimizationGroupLayout);
            GridData optimizationGroupLData = new GridData();

            optimizationGroupLData.horizontalAlignment = GridData.FILL;
            optimizationGroupLData.grabExcessHorizontalSpace = true;
            optimizationGroupLData.horizontalSpan = 6;
            optimizationGroupLayout.numColumns=7;
            optimizationGroup.setLayoutData(optimizationGroupLData);
            optimizationGroup.setText(Constants.model_create_Alg_section);
            {

                {
                                       GridData lData=new GridData();
                    lData.verticalAlignment = GridData.BEGINNING;
                    algLabel = new Label(optimizationGroup, SWT.NONE);
                    algLabel.setText(Constants.model_create_alg);
                    algLabel.setLayoutData(lData);

                }
                {
 
                    algorithmComboBox = new Combo(optimizationGroup, SWT.READ_ONLY);
                    algorithmComboBox.setEnabled(true);
                }
            }
            {
                                   GridData lData=new GridData();
                    lData.verticalAlignment = GridData.BEGINNING;
                parametersLabel = new Label(optimizationGroup, SWT.NONE);
                parametersLabel.setText(Constants.model_create_para);
                parametersLabel.setLayoutData(lData);
            }
            {
                parameters = new Text(optimizationGroup, SWT.BORDER | SWT.V_SCROLL | SWT.H_SCROLL);

                GridData ConnectionTextLData = new GridData();
                ConnectionTextLData.horizontalAlignment = GridData.FILL;
                ConnectionTextLData.verticalAlignment = GridData.FILL;
                ConnectionTextLData.horizontalSpan = 4;
                ConnectionTextLData.verticalSpan=2;
                ConnectionTextLData.heightHint=30;

                ConnectionTextLData.grabExcessHorizontalSpace = true;
                ConnectionTextLData.grabExcessVerticalSpace = true;
                parameters.setLayoutData(ConnectionTextLData);
            }



        }

        {
            statementGroup = new Group(shell, SWT.NONE);
            GridLayout statementGroupLayout = new GridLayout();
            statementGroupLayout.makeColumnsEqualWidth = true;
            statementGroupLayout.numColumns = 6;
            statementGroup.setLayout(statementGroupLayout);
            GridData statementGroupLData = new GridData();
            statementGroupLData.verticalAlignment = GridData.FILL;
            statementGroupLData.grabExcessVerticalSpace = true;
            statementGroupLData.horizontalSpan=6;
            statementGroupLData.grabExcessHorizontalSpace = true;
            statementGroupLData.horizontalAlignment = GridData.FILL;
            statementGroupLData.heightHint = 171;
            statementGroup.setLayoutData(statementGroupLData);
            statementGroup.setText(Constants.model_create_title2);
            {
                GridData statementTextLData = new GridData();
                statementTextLData.grabExcessHorizontalSpace = true;
                statementTextLData.horizontalAlignment = GridData.FILL;
                statementTextLData.verticalAlignment = GridData.FILL;
                statementTextLData.horizontalSpan=6;
                statementTextLData.grabExcessVerticalSpace = true;
                
                statementText = new StyledText(statementGroup, SWT.MULTI
                        | SWT.V_SCROLL
                        | SWT.H_SCROLL
                        | SWT.READ_ONLY
                        | SWT.BORDER);
                statementText.setLayoutData(statementTextLData);
            }
        }
        {
            buttonComposite = new Composite(shell, SWT.NONE);
            GridLayout buttonCompositeLayout = new GridLayout();
            buttonCompositeLayout.makeColumnsEqualWidth = true;
            buttonCompositeLayout.numColumns = 1;
            GridData buttonCompositeLData = new GridData();
            buttonCompositeLData.grabExcessHorizontalSpace = true;
            buttonCompositeLData.horizontalAlignment = GridData.FILL;
            buttonCompositeLData.verticalAlignment = GridData.FILL;
            buttonCompositeLData.horizontalSpan = 2;
            buttonComposite.setLayoutData(buttonCompositeLData);
            buttonComposite.setLayout(buttonCompositeLayout);
            {
                optiButton = new Button(buttonComposite, SWT.PUSH
                        | SWT.CENTER);
                GridData optiButtonLData = new GridData();
                optiButtonLData.grabExcessHorizontalSpace = true;
                optiButtonLData.horizontalSpan=2;
                optiButtonLData.horizontalAlignment = GridData.FILL;
                optiButton.setLayoutData(optiButtonLData);
                optiButton.setText(Constants.model_create_opti);
                optiButton.setEnabled(false);
            }
            {
                okButton = new Button(buttonComposite, SWT.PUSH
                        | SWT.CENTER);
                GridData okButtonLData = new GridData();
                okButtonLData.grabExcessHorizontalSpace = true;
                okButtonLData.horizontalAlignment = GridData.FILL;
                okButton.setLayoutData(okButtonLData);
                okButton.setText(Constants.model_create_ok);
                okButton.setEnabled(false);
            }
            {
                cancelButton = new Button(buttonComposite, SWT.PUSH
                        | SWT.CENTER);
                GridData cancelButtonLData = new GridData();
                cancelButtonLData.grabExcessHorizontalSpace = true;
                cancelButtonLData.horizontalAlignment = GridData.FILL;
                cancelButton.setLayoutData(cancelButtonLData);
                cancelButton.setText(Constants.model_create_cancel);
            }
        }
    }

    /**
     * this method inits all the different listeners which get called through
     * interaction with this dialog
     */
    private void initListeners() {
        // add sqllinestylelisteners
        this.statementText.addLineStyleListener(new SQLKeywordLineStyleListener(
                new DefaultLineStyleListener()));
        this.statementText.addVerifyKeyListener(new UpperKeyListener(statementText));

        this.selectText.addLineStyleListener(new SQLKeywordLineStyleListener(
                new DefaultLineStyleListener()));
        this.selectText.addVerifyKeyListener(new UpperKeyListener(selectText));


        Listener updateListener = new Listener() {

            public void handleEvent(Event event) {
                updateStatement();
            }
        };
         Listener updateListenerBS = new Listener() {

            public void handleEvent(Event event) {
                updateCombosStatement();
                 updateStatement();
            }
        };

        // Creation Group
        this.createModelName.addListener(SWT.Modify, updateListener);
        this.selectText.addListener(SWT.Modify, updateListenerBS);
        updateCombosStatement();


        SelectionListener sl = new SelectionAdapter() {

            public void widgetSelected(SelectionEvent e) {
                algorithmComboBox.setItems(relSamples);
            }
        };
        sl.widgetSelected(null); // HACK

        meassurecolumn.addListener(SWT.Selection, updateListener);
        timecolumn.addListener(SWT.Selection, updateListener);
        algorithmComboBox.addSelectionListener(new ChangeModelParameterListener(parameters));
        parameters.addListener(SWT.Modify, updateListener);
        storage.addListener(SWT.Selection, updateListener);
        


        cancelButton.addListener(SWT.Selection, new Listener() {

            public void handleEvent(Event event) {
                // close dialog
                shell.dispose();
                // the return string
                result = "CANCEL";
            }
        });

        optiButton.addListener(SWT.Selection, new Listener() {

            public void handleEvent(Event e) {
                EditOptimConfs dialog =
                        new EditOptimConfs(shell, SWT.NONE);
                dialog.open("Edit Optim Parameters");
            }
        });

        okButton.addListener(SWT.Selection, new Listener() {

            public void handleEvent(Event e) {
                JDBCInterface.getInstance().executeStatement(statementText.getText(), 0, true, true, false, false);
            }
        });
        updateStatement();
    }


    private void updateCombosStatement() {
        meassurecolumn.removeAll();
        timecolumn.removeAll();
		String textValue=selectText.getText().toUpperCase();
		if(!textValue.startsWith(("SELECT")))
                {
                    meassurecolumn.add("INVALID");
                    timecolumn.add("INVALID");
                    return;
                }

		textValue=textValue.substring(7);
		String[] pos = textValue.split("[,]");
		boolean breaker=false;
		for(String s : pos)
		{
			if(breaker)
				return;
			if (s.contains("FROM"))
			{
				int fromInd=s.indexOf("FROM");
				s=s.substring(0,fromInd);
				breaker=true;
			}
			String s2=s.toUpperCase().trim();
			if(s2.contains(" ") || s2.contains("AS"))//Alias
			{
				String[] pos2 = s2.split("[\\s]");
				this.meassurecolumn.add(pos2[pos2.length-1].trim());
                                this.timecolumn.add(pos2[pos2.length-1].trim());
				continue;
			}
			if(s.trim().length()<1) continue;
			this.meassurecolumn.add(s.trim());
                        this.timecolumn.add(s.trim());

		}
                if(meassurecolumn.getItems().length<=0)
                {
                    meassurecolumn.add("INVALID");
                    timecolumn.add("INVALID");
                }


        
    }
    private void updateStatement() {

        // create sample group
        String stmt = "CREATE MODEL ";
        stmt += createModelName.getText() + "\n FOR FORECAST OF " + meassurecolumn.getText() +" ON "+timecolumn.getText();

       

   
        stmt += "\n ALGORITHM " + algorithmComboBox.getText() + "\nPARAMETERS "+parameters.getText()+"\n TRAINING_DATA ("+selectText.getText()+ ")\n STORAGE "+storage.getText();

       
        // paste this combined string into styled text component

        statementText.setText(stmt);
        statementText.append(" "); // to trigger upper case formatting on
        if(!createModelName.getText().equals("") && !meassurecolumn.getText().equals("") && !timecolumn.getText().equals("") &&! parameters.getText().equals("")&& !storage.getText().equals(""))
            okButton.setEnabled(true);
        else
            okButton.setEnabled(false);
    }

    /**
     * this method gets the title text of the dialog
     * used in the initcomponents method te determine the title string
     */
    public String getText() {
        return Constants.model_create_title;
    }



    private void activatecomponents() {
        optiButton.setEnabled(true);
    }
}

