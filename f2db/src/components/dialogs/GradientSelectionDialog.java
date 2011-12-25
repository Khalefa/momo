package components.dialogs;

import java.util.Properties;

import modules.config.Configuration;
import modules.misc.ResourceRegistry;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.ColorDialog;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Dialog;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Shell;

public class GradientSelectionDialog extends Dialog{

    private Shell dialogShell;
    private Group gradientDirectionGroup;
    private Button cancelButton;
    private Button okButton;
    private Composite buttonComposite;
    private Label toColorLabel;
    private Label fromColorLabel;
    private Button rightLeftDirection;
    private Button leftRightDirection;
    private Button bottomUpDirection;
    private Button topDownDirection;
    private Composite gradientDirectionsComposite;
    private Button gradientToColor;
    private Button gradientFromColor;
    
    private Properties props;
    private String generalPrefix;
    
    private boolean okPressed = false;
    
    public GradientSelectionDialog (Shell parent, int style) {
        super(parent,style);
    }
    /** set the general prefix which is used to resolve the correct properties, which get changed */
    public void setGeneralPrefix(String prefix){
        this.generalPrefix = prefix;
    }
    
    /** init the gui components of this dialog */
    private void initComponents(){
        try {
            GridLayout dialogShellLayout = new GridLayout();
            dialogShell.setLayout(dialogShellLayout);
            dialogShellLayout.numColumns = 2;
            dialogShellLayout.makeColumnsEqualWidth = true;
            dialogShell.layout();
            dialogShell.pack();
            dialogShell.setSize(330, 194);
            dialogShell.setText("Gradient Selection Dialog");
            {
                fromColorLabel = new Label(dialogShell, SWT.NONE);
                GridData fromColorLabelLData = new GridData();
                fromColorLabelLData.horizontalAlignment = GridData.CENTER;
                fromColorLabel.setLayoutData(fromColorLabelLData);
                fromColorLabel.setText("From Color :");
            }
            {
                toColorLabel = new Label(dialogShell, SWT.NONE);
                GridData toColorLabelLData = new GridData();
                toColorLabelLData.horizontalAlignment = GridData.CENTER;
                toColorLabel.setLayoutData(toColorLabelLData);
                toColorLabel.setText("To Color :");
            }
            {
                gradientFromColor = new Button(dialogShell, SWT.PUSH
                    | SWT.CENTER);
                gradientFromColor.setText("from...");
                GridData gradientFromColorLData1 = new GridData();
                gradientFromColorLData1.horizontalAlignment = GridData.FILL;
                gradientFromColorLData1.grabExcessHorizontalSpace = true;
                gradientFromColor.setLayoutData(gradientFromColorLData1);
            }
            {
                gradientToColor = new Button(dialogShell, SWT.PUSH | SWT.CENTER);
                gradientToColor.setText("to...");
                GridData gradientToColorLData = new GridData();
                gradientToColorLData.horizontalAlignment = GridData.FILL;
                gradientToColorLData.grabExcessHorizontalSpace = true;
                gradientToColor.setLayoutData(gradientToColorLData);
            }
            {
                gradientDirectionGroup = new Group(dialogShell, SWT.NONE);
                GridLayout gradientDirectionGroupLayout = new GridLayout();
                gradientDirectionGroupLayout.makeColumnsEqualWidth = true;
                gradientDirectionGroup.setLayout(gradientDirectionGroupLayout);
                gradientDirectionGroup.setText("Gradient direction");
                GridData gradientDirectionGroupLData = new GridData();
                gradientDirectionGroupLData.horizontalSpan = 2;
                gradientDirectionGroupLData.horizontalAlignment = GridData.FILL;
                gradientDirectionGroupLData.grabExcessHorizontalSpace = true;
                gradientDirectionGroup.setLayoutData(gradientDirectionGroupLData);
                {
                    gradientDirectionsComposite = new Composite(
                        gradientDirectionGroup,
                        SWT.NONE);
                    GridLayout gradientDirectionsCompositeLayout = new GridLayout();
                    gradientDirectionsCompositeLayout.numColumns = 2;
                    gradientDirectionsCompositeLayout.makeColumnsEqualWidth = true;
                    gradientDirectionsComposite.setLayout(gradientDirectionsCompositeLayout);
                    GridData gradientDirectionsCompositeLData = new GridData();
                    gradientDirectionsCompositeLData.horizontalAlignment = GridData.FILL;
                    gradientDirectionsCompositeLData.grabExcessHorizontalSpace = true;
                    gradientDirectionsComposite
                        .setLayoutData(gradientDirectionsCompositeLData);
                    {
                        topDownDirection = new Button(
                            gradientDirectionsComposite,
                            SWT.RADIO | SWT.LEFT);
                        topDownDirection.setText("top-down");
                        GridData topDownDirectionLData = new GridData();
                        topDownDirectionLData.horizontalAlignment = GridData.FILL;
                        topDownDirectionLData.grabExcessHorizontalSpace = true;
                        topDownDirection.setLayoutData(topDownDirectionLData);
                        topDownDirection.setSelection(true);
                    }
                    {
                        bottomUpDirection = new Button(
                            gradientDirectionsComposite,
                            SWT.RADIO | SWT.LEFT);
                        bottomUpDirection.setText("bottom-Up");
                        GridData bottomUpDirectionLData = new GridData();
                        bottomUpDirectionLData.horizontalAlignment = GridData.FILL;
                        bottomUpDirectionLData.grabExcessHorizontalSpace = true;
                        bottomUpDirection.setLayoutData(bottomUpDirectionLData);
                    }
                    {
                        leftRightDirection = new Button(
                            gradientDirectionsComposite,
                            SWT.RADIO | SWT.LEFT);
                        leftRightDirection.setText("left-right");
                        GridData leftRightDirectionLData = new GridData();
                        leftRightDirectionLData.horizontalAlignment = GridData.FILL;
                        leftRightDirectionLData.grabExcessHorizontalSpace = true;
                        leftRightDirection
                            .setLayoutData(leftRightDirectionLData);
                    }
                    {
                        rightLeftDirection = new Button(
                            gradientDirectionsComposite,
                            SWT.RADIO | SWT.LEFT);
                        rightLeftDirection.setText("right-left");
                        GridData rightLeftDirectionLData = new GridData();
                        rightLeftDirectionLData.horizontalAlignment = GridData.FILL;
                        rightLeftDirectionLData.grabExcessHorizontalSpace = true;
                        rightLeftDirection
                            .setLayoutData(rightLeftDirectionLData);
                    }
                }
            }
            {
                buttonComposite = new Composite(dialogShell, SWT.NONE);
                GridLayout buttonCompositeLayout = new GridLayout();
                buttonCompositeLayout.makeColumnsEqualWidth = true;
                buttonCompositeLayout.numColumns = 2;
                GridData buttonCompositeLData = new GridData();
                buttonCompositeLData.grabExcessHorizontalSpace = true;
                buttonCompositeLData.grabExcessVerticalSpace = true;
                buttonCompositeLData.horizontalAlignment = GridData.FILL;
                buttonCompositeLData.horizontalSpan = 2;
                buttonCompositeLData.verticalAlignment = GridData.BEGINNING;
                buttonComposite.setLayoutData(buttonCompositeLData);
                buttonComposite.setLayout(buttonCompositeLayout);
                {
                    okButton = new Button(buttonComposite, SWT.PUSH
                        | SWT.CENTER);
                    GridData okButtonLData = new GridData();
                    okButtonLData.horizontalAlignment = GridData.FILL;
                    okButtonLData.grabExcessHorizontalSpace = true;
                    okButton.setLayoutData(okButtonLData);
                    okButton.setText("OK");
                }
                {
                    cancelButton = new Button(buttonComposite, SWT.PUSH
                        | SWT.CENTER);
                    GridData cancelButtonLData = new GridData();
                    cancelButtonLData.horizontalAlignment = GridData.FILL;
                    cancelButton.setLayoutData(cancelButtonLData);
                    cancelButton.setText("Cancel");
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        
    }

    /** init the listeners of this dialog , the control behaviour of the dialog*/
    private void initListeners(){
        
        // the gradient to color button
        gradientToColor.addListener(SWT.Selection, new Listener () {
            public void handleEvent (Event e) {
                ColorDialog dialog = new ColorDialog(getParent().getShell());
                Color color = ResourceRegistry.getInstance().getColor(
                             props.getProperty(generalPrefix+".bg.gradient.tocolor")); 
                if (color!=null){
                    dialog.setRGB(new RGB(color.getRed(),color.getGreen(),color.getBlue()));
                }                
                RGB rgb = dialog.open();
                if(rgb!=null){
                    //System.out.println("The chosen color was "+rgb);
                    String colorCode = ResourceRegistry.getInstance().convertRGBtoColorCode(rgb);
                    props.setProperty(generalPrefix+".bg.gradient.tocolor",colorCode);
                    props.setProperty(generalPrefix+".bg.type","gradient");
                } else {
                    // set up default gradient from color white 
                    //props.setProperty(generalPrefix+".bg.gradient.tocolor","255,255,255");
                    //props.setProperty(generalPrefix+".bg.type","gradient");
                }
                // update examples
                updateExamples();
            }
        });

        // the gradient from color button
        gradientFromColor.addListener(SWT.Selection, new Listener () {
            public void handleEvent (Event e) {
                ColorDialog dialog = new ColorDialog(getParent().getShell());
                Color color = ResourceRegistry.getInstance().getColor(
                             props.getProperty(generalPrefix+".bg.gradient.fromcolor")); 
                if (color!=null){
                    dialog.setRGB(new RGB(color.getRed(),color.getGreen(),color.getBlue()));
                }                
                RGB rgb = dialog.open();
                if(rgb!=null){
                    //System.out.println("The chosen color was "+rgb);
                    String colorCode = ResourceRegistry.getInstance().convertRGBtoColorCode(rgb);
                    props.setProperty(generalPrefix+".bg.gradient.fromcolor",colorCode);
                    props.setProperty(generalPrefix+".bg.type","gradient");
                } else {
                    // set up default gradient from color white 
                    //props.setProperty(generalPrefix+".bg.gradient.fromcolor","255,255,255");
                    //props.setProperty(generalPrefix+".bg.type","gradient");
                }
                // update examples
                updateExamples();
            }
        });
        
        
        // the direction radio buttons
        topDownDirection.addListener(SWT.Selection, new Listener () {
            public void handleEvent (Event e) {
                // set property 
                props.put(generalPrefix+".bg.gradient.type","topdown");
                // update examples
                updateExamples();
            }
        });
        
        bottomUpDirection.addListener(SWT.Selection, new Listener () {
            public void handleEvent (Event e) {
                // set property 
                props.put(generalPrefix+".bg.gradient.type","bottomup");
                // update examples
                updateExamples();
            }
        });

        leftRightDirection.addListener(SWT.Selection, new Listener () {
            public void handleEvent (Event e) {
                // set property 
                props.put(generalPrefix+".bg.gradient.type","leftright");
                // update examples
                updateExamples();
            }
        });

        rightLeftDirection.addListener(SWT.Selection, new Listener () {
            public void handleEvent (Event e) {
                // set property 
                props.put(generalPrefix+".bg.gradient.type","rightleft");
                // update examples
                updateExamples();
            }
        });

        okButton.addListener(SWT.Selection, new Listener () {
            public void handleEvent (Event e) {
                okPressed = true;
                dialogShell.dispose();
            }
        });

        cancelButton.addListener(SWT.Selection, new Listener () {
            public void handleEvent (Event e) {
                dialogShell.dispose();
            }
        });

        
    }
    
    private void updateExamples(){
        
    }
    
    public Object open(Properties srcprops){
        props = new Properties();
        Configuration.getInstance().copyProperties(srcprops,props);
        Shell parent = getParent();
        dialogShell = new Shell(parent, SWT.DIALOG_TRIM | SWT.APPLICATION_MODAL);
        initComponents();
        initListeners();
        dialogShell.open();
        Display display = dialogShell.getDisplay();
        while (!dialogShell.isDisposed()) {
            if (!display.readAndDispatch())
                display.sleep();
        }
        if(okPressed){
            return props;
        } else {
            return null;
        }
    }
    
    
}
