package components.config;

import java.util.Enumeration;
import java.util.Properties;

import modules.config.Configuration;
import modules.misc.Constants;
import modules.misc.ResourceRegistry;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StyleRange;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.layout.RowData;
import org.eclipse.swt.layout.RowLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.ColorDialog;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.FontDialog;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;

import components.dialogs.GradientSelectionDialog;
import components.listeners.DefaultLineStyleListener;
import components.listeners.GenericBGColorListener;
import components.listeners.GenericResizeListener;
import components.listeners.GenericStyleListener;
import components.listeners.SQLKeywordLineStyleListener;
import components.listeners.UpperKeyListener;
/**
 * This class holds all configuration settings for the console tab
 * @author Felix Beyer
 * @date   08.05.2006
 *
 */
public class ConsoleConfig extends Composite{

	private Composite parent;

	// the ui widgets
	private Button inputBGBlankButton;
	private Composite finalbuttonComposite;
	private Button saveSettings;
	private Button loadDefaults;
	private Combo outpuAreaChooseComboBox;
	private Button inputSolidBGColor;
	private Composite inputBGColorComposite;
	private Button inputGradient;
	private Button inputSolidColor;
	private Button gradientSelectButton;
	private Button areaFontButton;
	private Button areaCustomFontRadioButton;
	private Button areaDefaultFontRadioButton;
	private Button areaBGColorButton;
	private Button areaDefaultBGColorRadioButton;
	private Button areaFGColorButton;
	private Button areaCustomFGColorRadioButton;
	private Button areaDefaultFGColorRadioButton;
	private Composite composite1;
	private Group StyleRangeFormatGroup;
	private Button inputSQLStyleFontButton;
	private Button customFontSQLStyleRadioButton;
	private Button defaultFontSQLStyleRadioButton;
	private Button inputSQLStyleBGColorButton;
	private Button defaultBGColorSQLStyleRadioButton;
	private Button inputSQLStyleFGColorButton;
	private Button customFGColorSQLStyleRadioButton;
	private Button defaultFGColorSQLStyleRadioButton;
	private Composite composite2;
	private Composite composite3;
	private Group SQLFormatGroup;
	private Combo IOChooser;
	private StyledText exampleOutputST;
	private Group exampleOutputGroup;
	private Group tabconfig;
	private Group bginput;
	private StyledText exampleInputST;
	private Button inputUseTransform;
	private Button inputUseSQL;
	private Button tab_console_enable_button;
	private Button tab_tree_enable_button;
	private Button tab_refresh_enable_button;
	private Button tab_olap_enable_button;
	private Button tab_onlineSampling_enable_button;
	private Button tab_system_enable_button;
	private Button tab_sampleCatalog_enable_button;
	private Button tab_kernelDensityPlot_enable_button;
	private Button tab_workload_enable_button;
	private Button tab_plan_enable_button;
	private Button tab_diagnosis_enable_button;
	private Button tab_history_enable_button;
	private Button tab_config_enable_button;
	private Button tab_help_enable_button;
	private Button tab_about_enable_button;
	private Label tabVisibility_information_labele;

	private Group exampleinput;
	private Group fginput;
	private Group BackgroundGroup;
	private Label ForegroundLabel;
	private Label BackgroundLabel;
	private Button customSQLStyleBGColorRadioButton;
	private Label FontLabel;
	private Label label1;
	private Label label2;
	private Button areaCustomBGColorRadioButton;
	private Label label3;

	// the configuration properties
	private Properties props;

	// the background color listener for the example output console
	private GenericBGColorListener outputBGColorListener; 

	// the SQL Keyword listener for the example output console
	private GenericStyleListener outputListener; 

	// the SQLLineStyleListener for the example output area
	private SQLKeywordLineStyleListener sqlListener; 

	// the UpperKeyListener for the example input area
	private UpperKeyListener inputUpperKeyListener;

	// the background resize listeners for the example Styledtext widgets
	private GenericResizeListener bgListenerMain;
	private GenericResizeListener bgListenerInput;


	// allowed prefixes, which get selected through the combo boxes
	// input output prefixes
	private String[] genPrefixes    = {"console.input",
	"console.output"};
	// the different style formats
	private String[] formatPrefixes = {"console.input.fg.default",
			"console.output.fg.default",
			"console.output.stmts",
			"console.output.results",
			"console.output.warnings",
	"console.output.exceptions"}; 

	// prefixes
	private String generalPrefix;
	private String outputFormatPrefix;

	/** the constructor */
	public ConsoleConfig(Composite parent, int style) {
		super(parent, style);
		this.parent = parent;

		generalPrefix      = "console.input";
		outputFormatPrefix = "console.input.fg.default";

		initProperties();

		initComponents();
		initListeners();

		setupGeneralSettings();
		setupFormatSettings();

		updateExamples();

	}

	/** initialize properties */
	private void initProperties(){
		// initialize the properties
		props = new Properties();
		Properties propsSrc = Configuration.getInstance().getMiscProperties();
		Configuration.getInstance().copyProperties(propsSrc,props);
	}

	/** this method sets up the UI settings, depending on the current selected 
	 * general settings (either input console, or output console)*/
	private void setupGeneralSettings(){
		boolean enabled;
		String value;
		// background settings
		// blank bg setting
		enabled = props.getProperty(generalPrefix+".bg.type").equals("blank");
		inputBGBlankButton.setSelection(enabled);

		// solid color bg setting
		enabled = props.getProperty(generalPrefix+".bg.type").equals("solid");
		inputSolidColor.setSelection(enabled);
		inputSolidBGColor.setEnabled(enabled);

		value   = props.getProperty(generalPrefix+".bg.color");
		inputSolidBGColor.setImage(ResourceRegistry.getInstance().getImage(
				inputSolidBGColor,
				ResourceRegistry.getInstance().getColor(value),
				15,
				10));
		inputSolidBGColor.setText(value);

		// gradient bg setting
		enabled = props.getProperty(generalPrefix+".bg.type").equals("gradient");
		inputGradient.setSelection(enabled);
		gradientSelectButton.setEnabled(enabled);

		// if gradient type, enable directions and toggle the right direction
		/*boolean gradientEnabled = enabled;
        value = props.getProperty(generalPrefix+".bg.gradient.type");

        topDownDirection.setSelection(value.equals("topdown"));
        topDownDirection.setEnabled(gradientEnabled);

        bottomUpDirection.setSelection(value.equals("bottomup"));
        bottomUpDirection.setEnabled(gradientEnabled);

        leftRightDirection.setSelection(value.equals("leftright"));
        leftRightDirection.setEnabled(gradientEnabled);

        rightLeftDirection.setSelection(value.equals("rightleft"));
        rightLeftDirection.setEnabled(gradientEnabled);*/

		// format sql92 keywords ?
		enabled = props.getProperty(generalPrefix+".fg.formatSQL92").equals("true");
		inputUseSQL.setSelection(enabled);

		// transform sql92 keywords into upper case characters?
		enabled = props.getProperty(generalPrefix+".fg.transformSQL92").equals("true");
		inputUseTransform.setSelection(enabled);

		// transform sql92 keywords into upper case characters?
		enabled = props.getProperty(generalPrefix+".fg.transformSQL92").equals("true");
		inputUseTransform.setSelection(enabled);


		// enables tab tab_console
		enabled = props.getProperty("console.output.tab.console.show").equals("true");
		tab_console_enable_button.setSelection(enabled);

		// enables tab tab_tree
		enabled = props.getProperty("console.output.tab.tree.show").equals("true");
		tab_tree_enable_button.setSelection(enabled);

		//enables tab tab_plan
		enabled = props.getProperty("console.output.tab.plan.show").equals("true");
		tab_plan_enable_button.setSelection(enabled);

		//enables tab tab_refresh
		enabled = props.getProperty("console.output.tab.refresh.show").equals("true");
		tab_refresh_enable_button.setSelection(enabled);

		// enables tab tab_olap
		enabled = props.getProperty("console.output.tab.olap.show").equals("true");
		tab_olap_enable_button.setSelection(enabled);

		// enables tab tab_onlineSampling
		enabled = props.getProperty("console.output.tab.onlineSampling.show").equals("true");
		tab_onlineSampling_enable_button.setSelection(enabled);

		// enables tab tab_system
		enabled = props.getProperty("console.output.tab.system.show").equals("true");
		tab_system_enable_button.setSelection(enabled);

		// enables tab tab_sampleCatalog
		enabled = props.getProperty("console.output.tab.sampleCatalog.show").equals("true");
		tab_sampleCatalog_enable_button.setSelection(enabled);

		// enables tab tab_kernelDensityPlot
		enabled = props.getProperty("console.output.tab.kernelDensityPlot.show").equals("true");
		tab_kernelDensityPlot_enable_button.setSelection(enabled);

		// enables tab tab_workload
		enabled = props.getProperty("console.output.tab.workload.show").equals("true");
		tab_workload_enable_button.setSelection(enabled);

		//enables tab tab_plan
		enabled = props.getProperty("console.output.tab.plan.show").equals("true");
		tab_plan_enable_button.setSelection(enabled);

		// enables tab tab_diagnosis
		enabled = props.getProperty("console.output.tab.diagnosis.show").equals("true");
		tab_diagnosis_enable_button.setSelection(enabled);

		//enables tab tab_history
		enabled = props.getProperty("console.output.tab.history.show").equals("true");
		tab_history_enable_button.setSelection(enabled);

		// enables tab tab_config
		enabled = props.getProperty("console.output.tab.config.show").equals("true");
		tab_config_enable_button.setSelection(enabled);

		// enables tab tab_help
		enabled = props.getProperty("console.output.tab.help.show").equals("true");
		tab_help_enable_button.setSelection(enabled);

		// enables tab tab_about
		enabled = props.getProperty("console.output.tab.about.show").equals("true");
		tab_about_enable_button.setSelection(enabled);



		// get fg color for sql92 keywords
		value   = props.getProperty(generalPrefix+".fg.sql.fgcolor");
		defaultFGColorSQLStyleRadioButton.setSelection(false);
		customFGColorSQLStyleRadioButton.setSelection(true);
		inputSQLStyleFGColorButton.setEnabled(true);

		inputSQLStyleFGColorButton.setImage(ResourceRegistry.getInstance().getImage(
				inputSQLStyleFGColorButton,
				ResourceRegistry.getInstance().getColor(value),
				15,
				10));
		inputSQLStyleFGColorButton.setText(value);



		// get bg color for sql92 keywords
		value   = props.getProperty(generalPrefix+".fg.sql.bgcolor");
		enabled = value.equals("null");
		defaultBGColorSQLStyleRadioButton.setSelection(enabled);
		customSQLStyleBGColorRadioButton.setSelection(!enabled);
		inputSQLStyleBGColorButton.setEnabled(!enabled);

		if(!value.equalsIgnoreCase("null")){
			inputSQLStyleBGColorButton.setImage(ResourceRegistry.getInstance().getImage(
					inputSQLStyleBGColorButton,
					ResourceRegistry.getInstance().getColor(value),
					15,
					10));
		} else {
			inputSQLStyleBGColorButton.setImage(
					ResourceRegistry.getInstance().getImage("nullColor"));
		}
		inputSQLStyleBGColorButton.setText(value);


		// get font for sql92 keywords
		value   = props.getProperty(generalPrefix+".fg.sql.font");
		defaultFontSQLStyleRadioButton.setSelection(false);
		customFontSQLStyleRadioButton.setSelection(true);
		inputSQLStyleFontButton.setEnabled(true);
		ResourceRegistry.getInstance().getFont(value);
		inputSQLStyleFontButton.setFont(
				ResourceRegistry.getInstance().getPreviewFont10pt(value));

	}

	/** this method sets up the UI settings, depending on the current selected area
	 *  between stmts, results, warnings, exceptions
	 * */
	private void setupFormatSettings(){

		// FG Color settings
		String value = props.getProperty(outputFormatPrefix+".fg.fgcolor");
		boolean enabled = value.equals("default");

		areaDefaultFGColorRadioButton.setSelection(enabled);
		areaCustomFGColorRadioButton.setSelection(!enabled);
		areaFGColorButton.setEnabled(!enabled);
		if(!enabled){
			areaFGColorButton.setImage(ResourceRegistry.getInstance().getImage(
					areaFGColorButton,
					ResourceRegistry.getInstance().getColor(value),
					15,
					10));
			areaFGColorButton.setText(value);
		} else {
			areaFGColorButton.setImage(ResourceRegistry.getInstance().getImage(
					areaFGColorButton,
					ResourceRegistry.getInstance().getColor(0,0,255),
					15,
					10));
			areaFGColorButton.setText("000,000,255");
		}


		// BG Color settings
		value   = props.getProperty(outputFormatPrefix+".fg.bgcolor");
		enabled = value.equals("null");
		areaDefaultBGColorRadioButton.setSelection(enabled);
		areaCustomBGColorRadioButton.setSelection(!enabled);
		areaBGColorButton.setEnabled(!enabled);

		if(!value.equalsIgnoreCase("null")){
			areaBGColorButton.setImage(ResourceRegistry.getInstance().getImage(
					areaBGColorButton,
					ResourceRegistry.getInstance().getColor(value),
					15,
					10));
		} else {
			areaBGColorButton.setImage(
					ResourceRegistry.getInstance().getImage("nullColor"));
		}
		areaBGColorButton.setText(value);


		// Font settings
		value   = props.getProperty(outputFormatPrefix+".fg.font");

		enabled = value.equals("default");
		areaDefaultFontRadioButton.setSelection(enabled);
		areaCustomFontRadioButton.setSelection(!enabled);
		areaFontButton.setEnabled(!enabled);

		if (enabled) {
			value = "1|Courier New|10|2|WINDOWS|1|-21|0|0|0|700|0|0|0|1|0|0|0|Courier New";
		}
		ResourceRegistry.getInstance().getFont(value);
		areaFontButton.setFont(
				ResourceRegistry.getInstance().getPreviewFont10pt(value));


	}

	/** overwrites the current chosen props with the configuration props */
	private void saveSettings(){
		Enumeration en = props.keys();
		//Iterator iter = en.iterator();
		while (en.hasMoreElements()){
			Object key = en.nextElement();
			// set property
			Configuration.getInstance().
			setProperty((String)key,props.getProperty((String)key));
		}
	}

	/** this method loads the default settings for the console look'n'feel*/ 
	private void loadDefaultSettings(){
		props.setProperty("console.input.fg.formatSQL92","true");
		props.setProperty("console.input.fg.transformSQL92","true");

		//# ---- SQL Formatting ------ 
		props.setProperty("console.input.fg.sql.fgcolor","000,000,255");
		props.setProperty("console.input.fg.sql.bgcolor","null");
		props.setProperty("console.input.fg.sql.font","1|Courier New|10|2|WINDOWS|1|-21|0|0|0|700|0|0|0|1|0|0|0|Courier New");

		//# ---- Default Formatting ------ 
		props.setProperty("console.input.fg.default.fgcolor","000,000,000");
		props.setProperty("console.input.fg.default.bgcolor","255,255,255");
		props.setProperty("console.input.fg.default.font","1|Courier New|10|0|WINDOWS|1|-13|0|0|0|0|0|0|0|1|0|0|0|0|Courier New");

		//# --- Background decoration ----
		props.setProperty("console.input.bg.showLogo","false");
		props.setProperty("console.input.bg.type","gradient");
		props.setProperty("console.input.bg.color","255,255,255");
		props.setProperty("console.input.bg.gradient.type","topdown");
		props.setProperty("console.input.bg.gradient.fromcolor","255,255,255");
		props.setProperty("console.input.bg.gradient.tocolor","150,150,255");

		//# --------------- output area settings -----------------

		//# -- additional formatting
		props.setProperty("console.output.fg.formatSQL92","true");
		props.setProperty("console.output.fg.transformSQL92","true");

		//# --- Background decoration ----
		props.setProperty("console.output.bg.showLogo","false");
		props.setProperty("console.output.bg.type","blank");
		props.setProperty("console.output.bg.color","255,255,255");
		props.setProperty("console.output.bg.gradient.type","leftright");
		props.setProperty("console.output.bg.gradient.fromcolor","255,255,255");
		props.setProperty("console.output.bg.gradient.tocolor","200,200,255");

		//# ---- Default Formatting ------ 
		props.setProperty("console.output.fg.default.fgcolor","000,000,000");
		props.setProperty("console.output.fg.default.bgcolor","null");
		props.setProperty("console.output.fg.default.font","1|Courier New|10|0|WINDOWS|1|-13|0|0|0|0|0|0|0|1|0|0|0|0|Courier New");


		//# ---- SQL Formatting ------ 
		props.setProperty("console.output.fg.sql.fgcolor","000,064,128");
		props.setProperty("console.output.fg.sql.bgcolor","null");
		props.setProperty("console.output.fg.sql.font","1|Courier New|10|2|WINDOWS|1|-13|0|0|0|0|1|0|0|1|0|0|0|0|Courier New");


		//# ------------- stmt settings --------------------------

		//# -- additional formatting
		props.setProperty("console.output.stmts.fg.formatSQL92","true");
		props.setProperty("console.output.stmts.fg.transformSQL92","true");

		props.setProperty("console.output.stmts.fg.fgcolor","000,000,000");
		props.setProperty("console.output.stmts.fg.bgcolor","255,255,155");
		props.setProperty("console.output.stmts.fg.font","1|Courier New|10|2|WINDOWS|1|-13|0|0|0|0|1|0|0|1|0|0|0|0|Courier New");

		//# ------------- results settings --------------------------

		//# -- additional formatting
		props.setProperty("console.output.results.fg.formatSQL92","true");
		props.setProperty("console.output.results.fg.transformSQL92","true");

		props.setProperty("console.output.results.fg.fgcolor","000,000,200");
		props.setProperty("console.output.results.fg.bgcolor","255,255,255");
		props.setProperty("console.output.results.fg.font","1|Courier New|10|2|WINDOWS|1|-13|0|0|0|0|1|0|0|1|0|0|0|0|Courier New");

		//# ------------- warnings settings --------------------------

		//# -- additional formatting
		props.setProperty("console.output.warnings.fg.formatSQL92","true");
		props.setProperty("console.output.warnings.fg.transformSQL92","true");

		props.setProperty("console.output.warnings.fg.fgcolor","000,000,000");
		props.setProperty("console.output.warnings.fg.bgcolor","255,255,255");
		props.setProperty("console.output.warnings.fg.font","1|Courier New|10|2|WINDOWS|1|-13|0|0|0|0|1|0|0|1|0|0|0|0|Courier New");

		//# ------------- exception settings --------------------------

		//# -- additional formatting
		props.setProperty("console.output.exceptions.fg.formatSQL92","false");
		props.setProperty("console.output.exceptions.fg.transformSQL92","true");

		props.setProperty("console.output.exceptions.fg.fgcolor","255,000,000");
		props.setProperty("console.output.exceptions.fg.bgcolor","255,255,255");
		props.setProperty("console.output.exceptions.fg.font","1|Courier New|10|2|WINDOWS|1|-13|0|0|0|0|1|0|0|1|0|0|0|0|Courier New");

		//# ------------- tab settings --------------------------

		//# -- additional formatting by Stephan Gneist
		props.setProperty("console.output.tab.console.show", "true");
		props.setProperty("console.output.tab.tree.show", "false");
		props.setProperty("console.output.tab.plan.show", "true");
		props.setProperty("console.output.tab.refresh.show", "true");
		props.setProperty("console.output.tab.olap.show", "true");
		props.setProperty("console.output.tab.onlineSampling.show", "true");
		props.setProperty("console.output.tab.system.show", "true");
		props.setProperty("console.output.tab.sampleCatalog.show", "true");
		props.setProperty("console.output.tab.workload.show", "true");
		props.setProperty("console.output.tab.plan.show", "true");
		props.setProperty("console.output.tab.diagnosis.show", "true");
		props.setProperty("console.output.tab.history.show", "true");
		props.setProperty("console.output.tab.config.show", "true");
		props.setProperty("console.output.tab.help.show", "true");
		props.setProperty("console.output.tab.about.show", "true");



		//# ++++++++++++++++++++++++++++++++++++++++++++++++++


	}

	/** initialize components */
	private void initComponents(){
		try {

			GridLayout thisLayout = new GridLayout();
			thisLayout.numColumns = 2;
			thisLayout.makeColumnsEqualWidth = true;
			this.setLayout(thisLayout);
			this.setSize(902, 567);
			{
				bginput = new Group(this, SWT.NONE);
				GridLayout bginputLayout = new GridLayout();
				bginputLayout.makeColumnsEqualWidth = true;
				bginput.setLayout(bginputLayout);
				GridData bginputLData = new GridData();
				bginputLData.verticalAlignment = GridData.BEGINNING;
				bginputLData.grabExcessVerticalSpace = true;
				bginputLData.grabExcessHorizontalSpace = true;
				bginputLData.horizontalAlignment = GridData.FILL;
				bginputLData.heightHint = 272;
				bginput.setLayoutData(bginputLData);
				bginput.setText("General settings");
				{
					IOChooser = new Combo(bginput, SWT.READ_ONLY);
					GridData IOChooserLData = new GridData();
					IOChooserLData.horizontalSpan = 2;
					GridData IOChooserLData1 = new GridData();
					IOChooserLData1.heightHint = 21;
					IOChooserLData1.grabExcessHorizontalSpace = true;
					IOChooserLData1.grabExcessVerticalSpace = true;
					IOChooserLData1.verticalAlignment = GridData.BEGINNING;
					IOChooserLData1.horizontalAlignment = GridData.FILL;
					IOChooser.setLayoutData(IOChooserLData1);

					IOChooser.add("input console");
					IOChooser.add("output console");

					IOChooser.setText("input console");
				}
				{
					BackgroundGroup = new Group(bginput, SWT.NONE);
					GridLayout BackgroundGroupLayout = new GridLayout();
					BackgroundGroupLayout.makeColumnsEqualWidth = true;
					BackgroundGroup.setLayout(BackgroundGroupLayout);
					GridData BackgroundGroupLData = new GridData();
					BackgroundGroupLData.horizontalSpan = 2;
					BackgroundGroupLData.grabExcessHorizontalSpace = true;
					BackgroundGroupLData.horizontalAlignment = GridData.FILL;
					BackgroundGroup.setLayoutData(BackgroundGroupLData);
					BackgroundGroup.setText("Background");
					{
						inputBGColorComposite = new Composite(
								BackgroundGroup,
								SWT.NONE);
						RowLayout inputBGColorCompositeLayout = new RowLayout(
								org.eclipse.swt.SWT.HORIZONTAL);
						inputBGColorCompositeLayout.fill = true;
						inputBGColorCompositeLayout.justify = true;
						GridData inputBGColorCompositeLData = new GridData();
						inputBGColorCompositeLData.grabExcessHorizontalSpace = true;
						inputBGColorCompositeLData.horizontalAlignment = GridData.FILL;
						inputBGColorComposite.setLayoutData(inputBGColorCompositeLData);
						inputBGColorComposite
						.setLayout(inputBGColorCompositeLayout);
						{
							inputBGBlankButton = new Button(
									inputBGColorComposite,
									SWT.RADIO | SWT.LEFT);
							inputBGBlankButton.setText("blank");
							inputBGBlankButton.setSelection(true);
						}
						{
							inputSolidColor = new Button(
									inputBGColorComposite,
									SWT.RADIO | SWT.LEFT);
							RowData inputSolidColorLData = new RowData();
							inputSolidColor.setLayoutData(inputSolidColorLData);
							inputSolidColor.setText("solid");

						}
						{
							inputSolidBGColor = new Button(
									inputBGColorComposite,
									SWT.PUSH | SWT.CENTER);
							RowData inputSolidBGColorLData = new RowData();
							inputSolidBGColor
							.setLayoutData(inputSolidBGColorLData);
							//inputSolidBGColor.setText("BG Color...");
						}
						{
							inputGradient = new Button(
									inputBGColorComposite,
									SWT.RADIO | SWT.LEFT);
							RowData inputGradientLData = new RowData();
							inputGradient.setLayoutData(inputGradientLData);
							inputGradient.setText("gradient");

						}
						{
							gradientSelectButton = new Button(
									inputBGColorComposite,
									SWT.PUSH | SWT.CENTER);
							GridData gradientFromColorLData = new GridData();
							gradientFromColorLData.horizontalAlignment = GridData.FILL;
							RowData gradientFromColorLData1 = new RowData();
							gradientSelectButton
							.setLayoutData(gradientFromColorLData1);
							gradientSelectButton.setText("...");
						}
					}
				}
				{
					SQLFormatGroup = new Group(bginput, SWT.NONE);
					GridLayout SQLFormatGroupLayout = new GridLayout();
					SQLFormatGroup.setLayout(SQLFormatGroupLayout);
					GridData SQLFormatGroupLData = new GridData();
					SQLFormatGroupLData.grabExcessHorizontalSpace = true;
					SQLFormatGroupLData.horizontalAlignment = GridData.FILL;
					SQLFormatGroup.setLayoutData(SQLFormatGroupLData);
					SQLFormatGroup.setText("SQL Format Settings");
					{
						inputUseSQL = new Button(SQLFormatGroup, SWT.CHECK
								| SWT.LEFT);
						inputUseSQL.setText("format SQL92 Keywords");
						GridData inputUseSQLLData = new GridData();
						inputUseSQL.setLayoutData(inputUseSQLLData);
					}
					{
						inputUseTransform = new Button(
								SQLFormatGroup,
								SWT.CHECK | SWT.LEFT);
						GridData inputUseTransformLData = new GridData();
						inputUseTransform.setLayoutData(inputUseTransformLData);
						inputUseTransform.setText("transform SQL92 Keywords into upper case");
					}
					{
						composite2 = new Composite(SQLFormatGroup, SWT.NONE);
						GridLayout composite2Layout = new GridLayout();
						composite2Layout.numColumns = 4;
						composite2Layout.makeColumnsEqualWidth = true;
						composite2.setLayout(composite2Layout);
						GridData composite2LData = new GridData();
						composite2LData.heightHint = 33;
						composite2LData.horizontalAlignment = GridData.FILL;
						composite2LData.grabExcessHorizontalSpace = true;
						//composite2LData.widthHint = 214;
						composite2.setLayoutData(composite2LData);
						{
							ForegroundLabel = new Label(composite2, SWT.NONE);
							GridData ForegroundLabelLData = new GridData();
							ForegroundLabelLData.horizontalAlignment = GridData.FILL;
							ForegroundLabelLData.grabExcessHorizontalSpace = true;
							ForegroundLabel.setLayoutData(ForegroundLabelLData);
							ForegroundLabel.setText("Foreground");
						}
						{
							customFGColorSQLStyleRadioButton = new Button(
									composite2,
									SWT.RADIO | SWT.LEFT);
							GridData button2LData = new GridData();
							button2LData.grabExcessHorizontalSpace = true;
							button2LData.horizontalAlignment = GridData.FILL;
							customFGColorSQLStyleRadioButton.setLayoutData(button2LData);
						}
						{
							inputSQLStyleFGColorButton = new Button(
									composite2,
									SWT.PUSH | SWT.CENTER);
							GridData inputSQLStyleFGColorButtonLData = new GridData();
							inputSQLStyleFGColorButtonLData.grabExcessHorizontalSpace = true;
							inputSQLStyleFGColorButtonLData.horizontalAlignment = GridData.FILL;
							inputSQLStyleFGColorButton.setLayoutData(inputSQLStyleFGColorButtonLData);
							inputSQLStyleFGColorButton.setText("FG Color...");
							GridData button11LData = new GridData();
							button11LData.verticalAlignment = GridData.FILL;
						}
						{
							defaultFGColorSQLStyleRadioButton = new Button(
									composite2,
									SWT.RADIO | SWT.LEFT);
							defaultFGColorSQLStyleRadioButton.setSelection(true);
							GridData defaultFGColorSQLStyleRadioButtonLData = new GridData();
							defaultFGColorSQLStyleRadioButtonLData.horizontalAlignment = GridData.FILL;
							defaultFGColorSQLStyleRadioButtonLData.grabExcessHorizontalSpace = true;
							defaultFGColorSQLStyleRadioButton.setLayoutData(defaultFGColorSQLStyleRadioButtonLData);
							defaultFGColorSQLStyleRadioButton.setText("default");
							GridData button1LData = new GridData();
							button1LData.grabExcessHorizontalSpace = true;
						}
						Composite composite2b = new Composite(
								SQLFormatGroup,
								SWT.NONE);
						GridLayout composite2bLayout = new GridLayout();
						composite2bLayout.numColumns = 4;
						composite2bLayout.makeColumnsEqualWidth = true;
						composite2b.setLayout(composite2bLayout);
						GridData composite2bLData = new GridData();
						composite2bLData.verticalAlignment = GridData.END;
						composite2bLData.heightHint = 33;
						composite2bLData.horizontalAlignment = GridData.FILL;
						composite2bLData.grabExcessHorizontalSpace = true;
						composite2b.setLayoutData(composite2bLData);
						{
							BackgroundLabel = new Label(composite2b, SWT.NONE);
							GridData BackgroundLabelLData = new GridData();
							BackgroundLabelLData.horizontalAlignment = GridData.FILL;
							BackgroundLabelLData.grabExcessHorizontalSpace = true;
							BackgroundLabel.setLayoutData(BackgroundLabelLData);
							BackgroundLabel.setText("Background");
						}
						{
							GridData customBGColorLData = new GridData();
							customBGColorLData.grabExcessHorizontalSpace = true;
							GridData customSQLStyleBGColorRadioButtonLData = new GridData();
							customSQLStyleBGColorRadioButtonLData.horizontalAlignment = GridData.FILL;
							customSQLStyleBGColorRadioButtonLData.grabExcessHorizontalSpace = true;
							customSQLStyleBGColorRadioButton = new Button(
									composite2b,
									SWT.RADIO | SWT.LEFT);
							customSQLStyleBGColorRadioButton.setLayoutData(customSQLStyleBGColorRadioButtonLData);
						}

						{
							inputSQLStyleBGColorButton = new Button(
									composite2b,
									SWT.PUSH | SWT.CENTER);
							GridData inputSQLStyleBGColorButtonLData = new GridData();
							inputSQLStyleBGColorButtonLData.horizontalAlignment = GridData.FILL;
							inputSQLStyleBGColorButtonLData.grabExcessHorizontalSpace = true;
							inputSQLStyleBGColorButton.setLayoutData(inputSQLStyleBGColorButtonLData);
							inputSQLStyleBGColorButton.setText("BG Color...");
							GridData button13LData = new GridData();
							button13LData.grabExcessHorizontalSpace = true;
						}
						{
							defaultBGColorSQLStyleRadioButton = new Button(
									composite2b,
									SWT.RADIO | SWT.LEFT);
							defaultBGColorSQLStyleRadioButton.setText("default (none)");
							GridData defaultBGColorSQLStyleRadioButtonLData = new GridData();
							defaultBGColorSQLStyleRadioButtonLData.horizontalAlignment = GridData.FILL;
							defaultBGColorSQLStyleRadioButtonLData.grabExcessHorizontalSpace = true;
							defaultBGColorSQLStyleRadioButton.setLayoutData(defaultBGColorSQLStyleRadioButtonLData);
							defaultBGColorSQLStyleRadioButton.setSelection(true);
							GridData button12LData = new GridData();
							button12LData.horizontalAlignment = GridData.CENTER;
						}
						Composite composite2c = new Composite(
								SQLFormatGroup,
								SWT.NONE);
						GridLayout composite2cLayout = new GridLayout();
						composite2cLayout.numColumns = 4;
						composite2cLayout.makeColumnsEqualWidth = true;
						composite2c.setLayout(composite2cLayout);
						GridData composite2cLData = new GridData();
						composite2cLData.grabExcessHorizontalSpace = true;
						composite2cLData.heightHint = 33;
						composite2cLData.horizontalAlignment = GridData.FILL;
						composite2c.setLayoutData(composite2cLData);
						{
							FontLabel = new Label(composite2c, SWT.NONE);
							GridData FontLabelLData = new GridData();
							FontLabelLData.heightHint = 13;
							FontLabelLData.horizontalAlignment = GridData.FILL;
							FontLabelLData.grabExcessHorizontalSpace = true;
							FontLabel.setLayoutData(FontLabelLData);
							FontLabel.setText("Font");
						}

						{
							GridData customFontSQLStyleRadioButtonLData = new GridData();
							customFontSQLStyleRadioButtonLData.horizontalAlignment = GridData.FILL;
							customFontSQLStyleRadioButtonLData.grabExcessHorizontalSpace = true;
							customFontSQLStyleRadioButton = new Button(
									composite2c,
									SWT.RADIO | SWT.LEFT);
							customFontSQLStyleRadioButton.setLayoutData(customFontSQLStyleRadioButtonLData);
							GridData button15LData = new GridData();
							button15LData.grabExcessHorizontalSpace = true;
						}
						{
							inputSQLStyleFontButton = new Button(
									composite2c,
									SWT.PUSH | SWT.CENTER);
							inputSQLStyleFontButton.setText("Font...");
							GridData button16LData = new GridData();
							button16LData.heightHint = 23;
							button16LData.horizontalAlignment = GridData.FILL;
							button16LData.grabExcessHorizontalSpace = true;
							inputSQLStyleFontButton.setLayoutData(button16LData);
						}
						{
							defaultFontSQLStyleRadioButton = new Button(
									composite2c,
									SWT.RADIO | SWT.LEFT);
							defaultFontSQLStyleRadioButton.setSelection(true);
							GridData defaultFontSQLStyleRadioButtonLData = new GridData();
							defaultFontSQLStyleRadioButtonLData.horizontalAlignment = GridData.FILL;
							defaultFontSQLStyleRadioButtonLData.grabExcessHorizontalSpace = true;
							defaultFontSQLStyleRadioButton.setLayoutData(defaultFontSQLStyleRadioButtonLData);
							defaultFontSQLStyleRadioButton.setText("default");
							GridData button14LData = new GridData();
							button14LData.horizontalAlignment = GridData.CENTER;
						}
					}
				}
			}
			{
				fginput = new Group(this, SWT.NONE);
				GridLayout fginputLayout = new GridLayout();
				fginputLayout.makeColumnsEqualWidth = true;
				fginput.setLayout(fginputLayout);
				GridData fginputLData = new GridData();
				fginputLData.horizontalAlignment = GridData.FILL;
				fginputLData.grabExcessHorizontalSpace = true;
				fginputLData.verticalAlignment = GridData.BEGINNING;
				fginputLData.grabExcessVerticalSpace = true;
				fginputLData.heightHint = 271;
				fginput.setLayoutData(fginputLData);
				fginput.setText("Special format settings");
				{
					outpuAreaChooseComboBox = new Combo(fginput, SWT.READ_ONLY);
					GridData outpuAreaChooseComboBoxLData = new GridData();
					outpuAreaChooseComboBoxLData.horizontalAlignment = GridData.FILL;
					outpuAreaChooseComboBoxLData.grabExcessHorizontalSpace = true;
					outpuAreaChooseComboBox.setLayoutData(outpuAreaChooseComboBoxLData);

					outpuAreaChooseComboBox.add("input default");
					outpuAreaChooseComboBox.add("output default");
					outpuAreaChooseComboBox.add("output query");
					outpuAreaChooseComboBox.add("output results");
					outpuAreaChooseComboBox.add("output warnings");
					outpuAreaChooseComboBox.add("output exceptions");

					outpuAreaChooseComboBox.setText("input default");

				}
				{
					StyleRangeFormatGroup = new Group(fginput, SWT.NONE);
					GridLayout StyleRangeFormatGroupLayout = new GridLayout();
					StyleRangeFormatGroupLayout.makeColumnsEqualWidth = true;
					StyleRangeFormatGroup
					.setLayout(StyleRangeFormatGroupLayout);
					GridData StyleRangeFormatGroupLData = new GridData();
					StyleRangeFormatGroupLData.verticalAlignment = GridData.FILL;
					StyleRangeFormatGroupLData.horizontalAlignment = GridData.FILL;
					StyleRangeFormatGroupLData.grabExcessHorizontalSpace = true;
					StyleRangeFormatGroup.setLayoutData(StyleRangeFormatGroupLData);
					StyleRangeFormatGroup.setText("Format style");
					{
						composite1 = new Composite(
								StyleRangeFormatGroup,
								SWT.NONE);
						GridLayout composite1Layout = new GridLayout();
						composite1Layout.numColumns = 4;
						composite1Layout.makeColumnsEqualWidth = true;
						GridData composite1LData1 = new GridData();
						composite1LData1.horizontalAlignment = GridData.FILL;
						GridData composite1LData2 = new GridData();
						composite1LData2.horizontalAlignment = GridData.FILL;
						composite1LData2.grabExcessHorizontalSpace = true;
						composite1.setLayoutData(composite1LData2);
						composite1.setLayout(composite1Layout);
						{
							label1 = new Label(composite1, SWT.NONE);
							GridData label1LData = new GridData();
							label1LData.horizontalAlignment = GridData.FILL;
							label1LData.grabExcessHorizontalSpace = true;
							label1.setLayoutData(label1LData);
							label1.setText("Foreground");
						}
						GridData composite1LData = new GridData();
						composite1LData.horizontalAlignment = GridData.CENTER;
						{
							GridData areacustomFGColorRadioButtonLData = new GridData();
							areacustomFGColorRadioButtonLData.horizontalAlignment = GridData.CENTER;
							GridData areaCustomFGColorRadioButtonLData = new GridData();
							areaCustomFGColorRadioButtonLData.horizontalAlignment = GridData.FILL;
							areaCustomFGColorRadioButtonLData.grabExcessHorizontalSpace = true;
							areaCustomFGColorRadioButton = new Button(
									composite1,
									SWT.RADIO | SWT.LEFT);
							areaCustomFGColorRadioButton.setLayoutData(areaCustomFGColorRadioButtonLData);
							GridData button4LData = new GridData();
							button4LData.horizontalAlignment = GridData.CENTER;
						}
						{
							areaFGColorButton = new Button(composite1, SWT.PUSH
									| SWT.CENTER);
							areaFGColorButton.setText("FG Color...");
							GridData button5LData = new GridData();
							button5LData.verticalAlignment = GridData.BEGINNING;
							button5LData.horizontalAlignment = GridData.FILL;
							button5LData.grabExcessHorizontalSpace = true;
							areaFGColorButton.setLayoutData(button5LData);
							areaFGColorButton.setEnabled(false);
						}
						{
							areaDefaultFGColorRadioButton = new Button(
									composite1,
									SWT.RADIO | SWT.LEFT);
							areaDefaultFGColorRadioButton.setSelection(true);
							areaDefaultFGColorRadioButton.setText("default");
							GridData button3LData = new GridData();
							button3LData.verticalAlignment = GridData.BEGINNING;
							button3LData.horizontalAlignment = GridData.FILL;
							button3LData.grabExcessHorizontalSpace = true;
							areaDefaultFGColorRadioButton.setLayoutData(button3LData);
						}
						Composite composite1b = new Composite(
								StyleRangeFormatGroup,
								SWT.NONE);
						GridLayout composite1bLayout = new GridLayout();
						composite1bLayout.numColumns = 4;
						composite1bLayout.makeColumnsEqualWidth = true;
						composite1b.setLayout(composite1bLayout);
						GridData composite1bLData = new GridData();
						composite1bLData.verticalAlignment = GridData.BEGINNING;
						composite1bLData.grabExcessHorizontalSpace = true;
						composite1bLData.horizontalAlignment = GridData.FILL;
						composite1b.setLayoutData(composite1bLData);
						{
							label2 = new Label(composite1b, SWT.NONE);
							GridData label2LData = new GridData();
							label2LData.horizontalAlignment = GridData.FILL;
							label2LData.grabExcessHorizontalSpace = true;
							label2.setLayoutData(label2LData);
							label2.setText("Background");
						}
						{
							GridData customBGColorRadioButtonLData = new GridData();
							customBGColorRadioButtonLData.horizontalAlignment = GridData.CENTER;
							GridData areaCustomBGColorRadioButtonLData = new GridData();
							areaCustomBGColorRadioButtonLData.horizontalAlignment = GridData.FILL;
							areaCustomBGColorRadioButtonLData.grabExcessHorizontalSpace = true;
							areaCustomBGColorRadioButton = new Button(
									composite1b,
									SWT.RADIO | SWT.LEFT);
							areaCustomBGColorRadioButton.setLayoutData(areaCustomBGColorRadioButtonLData);
						}
						{
							areaBGColorButton = new Button(
									composite1b,
									SWT.PUSH | SWT.CENTER);
							areaBGColorButton.setText("BG Color...");
							GridData button7LData = new GridData();
							button7LData.horizontalAlignment = GridData.CENTER;
							GridData areaBGColorButtonLData = new GridData();
							areaBGColorButtonLData.horizontalAlignment = GridData.FILL;
							areaBGColorButtonLData.grabExcessHorizontalSpace = true;
							areaBGColorButton.setLayoutData(areaBGColorButtonLData);
							areaBGColorButton.setEnabled(false);
						}
						{
							areaDefaultBGColorRadioButton = new Button(
									composite1b,
									SWT.RADIO | SWT.LEFT);
							GridData areaDefaultBGColorRadioButtonLData = new GridData();
							areaDefaultBGColorRadioButtonLData.horizontalAlignment = GridData.FILL;
							areaDefaultBGColorRadioButtonLData.grabExcessHorizontalSpace = true;
							areaDefaultBGColorRadioButton.setLayoutData(areaDefaultBGColorRadioButtonLData);
							areaDefaultBGColorRadioButton.setText("default (none)");
							GridData button6LData = new GridData();
							button6LData.horizontalAlignment = GridData.CENTER;
						}
						Composite composite1c = new Composite(
								StyleRangeFormatGroup,
								SWT.NONE);
						GridLayout composite1cLayout = new GridLayout();
						composite1cLayout.numColumns = 4;
						composite1cLayout.makeColumnsEqualWidth = true;
						composite1c.setLayout(composite1cLayout);
						GridData composite1cLData = new GridData();
						composite1cLData.verticalAlignment = GridData.BEGINNING;
						composite1cLData.horizontalAlignment = GridData.FILL;
						composite1cLData.grabExcessHorizontalSpace = true;
						composite1c.setLayoutData(composite1cLData);
						{
							label3 = new Label(composite1c, SWT.NONE);
							label3.setText("Font");
							GridData label3LData = new GridData();
							label3LData.heightHint = 13;
							label3LData.horizontalAlignment = GridData.FILL;
							label3LData.grabExcessHorizontalSpace = true;
							label3.setLayoutData(label3LData);
						}

						{
							GridData areacustomFontRadioButtonLData = new GridData();
							areacustomFontRadioButtonLData.horizontalAlignment = GridData.CENTER;
							GridData areaCustomFontRadioButtonLData = new GridData();
							areaCustomFontRadioButtonLData.horizontalAlignment = GridData.FILL;
							areaCustomFontRadioButtonLData.grabExcessHorizontalSpace = true;
							areaCustomFontRadioButton = new Button(
									composite1c,
									SWT.RADIO | SWT.LEFT);
							areaCustomFontRadioButton.setLayoutData(areaCustomFontRadioButtonLData);
							GridData button9LData = new GridData();
							button9LData.horizontalAlignment = GridData.CENTER;
						}
						{
							areaFontButton = new Button(composite1c, SWT.PUSH
									| SWT.CENTER);
							areaFontButton.setText("Font...");
							GridData button10LData = new GridData();
							button10LData.horizontalAlignment = GridData.CENTER;
							GridData areaFontButtonLData = new GridData();
							areaFontButtonLData.heightHint = 23;
							areaFontButtonLData.horizontalAlignment = GridData.FILL;
							areaFontButtonLData.grabExcessHorizontalSpace = true;
							areaFontButton.setLayoutData(areaFontButtonLData);
							areaFontButton.setEnabled(false);
						}
						{
							areaDefaultFontRadioButton = new Button(
									composite1c,
									SWT.RADIO | SWT.LEFT);
							areaDefaultFontRadioButton.setSelection(true);
							GridData areaDefaultFontRadioButtonLData = new GridData();
							areaDefaultFontRadioButtonLData.horizontalAlignment = GridData.FILL;
							areaDefaultFontRadioButtonLData.grabExcessHorizontalSpace = true;
							areaDefaultFontRadioButton.setLayoutData(areaDefaultFontRadioButtonLData);
							areaDefaultFontRadioButton.setText("default");
							GridData button8LData = new GridData();
							button8LData.horizontalAlignment = GridData.CENTER;
						}
						Composite composite1d = new Composite(
								StyleRangeFormatGroup,
								SWT.NONE);
						GridLayout composite1dLayout = new GridLayout();
						composite1dLayout.numColumns = 3;
						composite1dLayout.makeColumnsEqualWidth = true;
						composite1d.setLayout(composite1dLayout);
						GridData composite1dLData = new GridData();
						composite1dLData.verticalAlignment = GridData.BEGINNING;
						composite1dLData.horizontalAlignment = GridData.CENTER;
						composite1d.setLayoutData(composite1dLData);

					}
				}
			}
			{
				exampleinput = new Group(this, SWT.NONE);
				GridLayout exampleinputLayout = new GridLayout();
				exampleinputLayout.makeColumnsEqualWidth = true;
				exampleinput.setLayout(exampleinputLayout);
				GridData exampleinputLData = new GridData();
				exampleinputLData.grabExcessHorizontalSpace = true;
				exampleinputLData.verticalAlignment = GridData.BEGINNING;
				exampleinputLData.grabExcessVerticalSpace = true;
				exampleinputLData.horizontalSpan = 2;
				exampleinputLData.horizontalAlignment = GridData.FILL;
				exampleinputLData.heightHint = 50;
				exampleinput.setLayoutData(exampleinputLData);
				exampleinput.setText("Example Console Input");
				{
					exampleInputST = new StyledText(exampleinput, SWT.BORDER
							| SWT.READ_ONLY);
					GridData inputExampleSTLData = new GridData();
					inputExampleSTLData.horizontalAlignment = GridData.FILL;
					inputExampleSTLData.grabExcessHorizontalSpace = true;
					inputExampleSTLData.verticalAlignment = GridData.FILL;
					inputExampleSTLData.grabExcessVerticalSpace = true;
					exampleInputST.setLayoutData(inputExampleSTLData);
				}
			}
			{
				exampleOutputGroup = new Group(this, SWT.NONE);
				GridLayout exampleOutputGroupLayout = new GridLayout();
				exampleOutputGroupLayout.makeColumnsEqualWidth = true;
				exampleOutputGroup.setLayout(exampleOutputGroupLayout);
				GridData exampleOutputGroupLData = new GridData();
				exampleOutputGroupLData.horizontalAlignment = GridData.FILL;
				exampleOutputGroupLData.grabExcessHorizontalSpace = true;
				exampleOutputGroupLData.verticalAlignment = GridData.BEGINNING;
				exampleOutputGroupLData.grabExcessVerticalSpace = true;
				exampleOutputGroupLData.horizontalSpan = 2;
				exampleOutputGroupLData.heightHint = 152;
				exampleOutputGroup.setLayoutData(exampleOutputGroupLData);
				exampleOutputGroup.setText("Example Console Output");
				{
					GridData exampleOutputSTLData = new GridData();
					exampleOutputSTLData.horizontalAlignment = GridData.FILL;
					exampleOutputSTLData.grabExcessHorizontalSpace = true;
					exampleOutputSTLData.verticalAlignment = GridData.FILL;
					exampleOutputSTLData.grabExcessVerticalSpace = true;
					exampleOutputST = new StyledText(
							exampleOutputGroup,
							SWT.BORDER | SWT.READ_ONLY | SWT.V_SCROLL);
					exampleOutputST.setLayoutData(exampleOutputSTLData);
				}
			}
			{

				/*
				 * 
                    SQLFormatGroup = new Group(bginput, SWT.NONE);
                    GridLayout SQLFormatGroupLayout = new GridLayout();
                    SQLFormatGroup.setLayout(SQLFormatGroupLayout);
                    GridData SQLFormatGroupLData = new GridData();
                    SQLFormatGroupLData.grabExcessHorizontalSpace = true;
                    SQLFormatGroupLData.horizontalAlignment = GridData.FILL;
                    SQLFormatGroup.setLayoutData(SQLFormatGroupLData);
                    SQLFormatGroup.setText("SQL Format Settings");
				 */


				tabconfig = new Group(this, SWT.NONE);
				GridLayout tabconfigLayout = new GridLayout();
				tabconfigLayout.numColumns = 1;
				//tabconfigLayout.makeColumnsEqualWidth = true;
				tabconfig.setLayout(tabconfigLayout);
				GridData tabconfigLData = new GridData();
				tabconfigLData.grabExcessHorizontalSpace = true;
				// tabconfigLData.verticalAlignment = GridData.BEGINNING;
				tabconfigLData.grabExcessVerticalSpace = true;
				tabconfigLData.horizontalSpan = 2;
				tabconfigLData.horizontalAlignment = GridData.FILL;
				//  tabconfigLData.heightHint = 90;
				tabconfig.setLayoutData(tabconfigLData);
				tabconfig.setText("Visible Tabs");
				{   
					/* 
                    composite2 = new Composite(SQLFormatGroup, SWT.NONE);
                    GridLayout composite2Layout = new GridLayout();
                    composite2Layout.numColumns = 4;
                    composite2Layout.makeColumnsEqualWidth = true;
                    composite2.setLayout(composite2Layout);
                    GridData composite2LData = new GridData();
                    composite2LData.heightHint = 33;
                    composite2LData.horizontalAlignment = GridData.FILL;
                    composite2LData.grabExcessHorizontalSpace = true;
                    //composite2LData.widthHint = 214;
                    composite2.setLayoutData(composite2LData);
					 */

					composite3 = new Composite(tabconfig, SWT.NONE);
					GridLayout composite3Layout = new GridLayout();
					composite3Layout.numColumns = 9;
					composite3Layout.makeColumnsEqualWidth = true;
					tabconfigLData.horizontalSpan = 2;
					composite3.setLayout(composite3Layout);
					GridData composite3LData = new GridData();
					composite3LData.horizontalAlignment = GridData.FILL;
					composite3LData.grabExcessHorizontalSpace = true;
					composite3.setLayoutData(composite3LData);
					// Klammern  einfuegen           
					{


						{
							tab_console_enable_button = new Button(composite3, SWT.CHECK | SWT.LEFT);
							tab_console_enable_button.setText(Constants.tab_console);
							GridData tab_console_enable_buttonLData = new GridData();
							tab_console_enable_button.setLayoutData(tab_console_enable_buttonLData);
						}
						{
							tab_tree_enable_button = new Button(composite3, SWT.CHECK | SWT.LEFT);
							tab_tree_enable_button.setText(Constants.tab_tree);
							GridData tab_tree_enable_buttonLData = new GridData();
							tab_tree_enable_button.setLayoutData(tab_tree_enable_buttonLData);
						}
						{
							tab_plan_enable_button = new Button(composite3, SWT.CHECK | SWT.LEFT);
							tab_plan_enable_button.setText(Constants.tab_plan);
							GridData tab_plan_enable_buttonLData = new GridData();
							tab_plan_enable_button.setLayoutData(tab_plan_enable_buttonLData);
						}
						{
							tab_olap_enable_button = new Button( composite3, SWT.CHECK | SWT.LEFT);
							tab_olap_enable_button.setText(Constants.tab_olap);
							GridData tab_olad_enable_buttonLData = new GridData();
							tab_olap_enable_button.setLayoutData(tab_olad_enable_buttonLData);
						}
						{
							tab_onlineSampling_enable_button = new Button( composite3, SWT.CHECK | SWT.LEFT);
							tab_onlineSampling_enable_button.setText(Constants.tab_onlineSampling);
							GridData tab_onlineSampling_enable_buttonLData = new GridData();
							tab_onlineSampling_enable_button.setLayoutData(tab_onlineSampling_enable_buttonLData);
						}
						{
							tab_refresh_enable_button = new Button(composite3, SWT.CHECK | SWT.LEFT);
							tab_refresh_enable_button.setText(Constants.tab_refresh);
							GridData tab_refresh_enable_buttonLData = new GridData();
							tab_refresh_enable_button.setLayoutData(tab_refresh_enable_buttonLData);
						}
						{
							tab_system_enable_button = new Button(composite3, SWT.CHECK | SWT.LEFT);
							tab_system_enable_button.setText(Constants.tab_system);
							GridData tab_system_enable_buttonLData = new GridData();
							tab_system_enable_button.setLayoutData(tab_system_enable_buttonLData);
						}
						{
							tab_sampleCatalog_enable_button = new Button( composite3, SWT.CHECK | SWT.LEFT);
							tab_sampleCatalog_enable_button.setText(Constants.tab_sampleCatalog);
							GridData tab_sampleCatalog_enable_buttonLData = new GridData();
							tab_sampleCatalog_enable_button.setLayoutData(tab_sampleCatalog_enable_buttonLData);
						} 
						{
							tab_workload_enable_button = new Button(composite3, SWT.CHECK | SWT.LEFT);
							tab_workload_enable_button.setText(Constants.tab_workload);
							GridData tab_workload_enable_buttonLData = new GridData();
							tab_workload_enable_button.setLayoutData(tab_workload_enable_buttonLData);
						}
						{
							tab_kernelDensityPlot_enable_button = new Button( composite3, SWT.CHECK | SWT.LEFT);
							tab_kernelDensityPlot_enable_button.setText(Constants.tab_kernelDensityPlot);
							GridData tab_kernelDensityPlot_enable_buttonLData = new GridData();
							tab_kernelDensityPlot_enable_button.setLayoutData(tab_kernelDensityPlot_enable_buttonLData);
						} 
						{
							tab_diagnosis_enable_button = new Button(composite3, SWT.CHECK | SWT.LEFT);
							tab_diagnosis_enable_button.setText(Constants.tab_diagnosis);
							GridData tab_diagnosis_enable_buttonLData = new GridData();
							tab_diagnosis_enable_button.setLayoutData(tab_diagnosis_enable_buttonLData);
						}
						{
							tab_history_enable_button = new Button( composite3, SWT.CHECK | SWT.LEFT);
							tab_history_enable_button.setText(Constants.tab_history);
							GridData tab_history_enable_buttonLData = new GridData();
							tab_history_enable_button.setLayoutData(tab_history_enable_buttonLData);
						} 
						{
							tab_config_enable_button = new Button(composite3, SWT.CHECK | SWT.LEFT);
							tab_config_enable_button.setText(Constants.tab_config);
							tab_config_enable_button.setEnabled(false);
							GridData tab_config_enable_buttonLData = new GridData();
							tab_config_enable_button.setLayoutData(tab_config_enable_buttonLData);
						}
						{
							tab_help_enable_button = new Button( composite3, SWT.CHECK | SWT.LEFT);
							tab_help_enable_button.setText(Constants.tab_help);
							GridData tab_help_enable_buttonLData = new GridData();
							tab_help_enable_button.setLayoutData(tab_help_enable_buttonLData);
						}

						{
							tab_about_enable_button = new Button( composite3, SWT.CHECK | SWT.LEFT);
							tab_about_enable_button.setText(Constants.tab_about);
							GridData tab_about_enable_buttonLData = new GridData();
							tab_about_enable_button.setLayoutData(tab_about_enable_buttonLData);
						}
					}
					tabVisibility_information_labele = new Label(tabconfig,SWT.SINGLE);
					tabVisibility_information_labele.setText(Constants.config_tabVisibility_information_text);

				}}

			{
				finalbuttonComposite = new Composite(this, SWT.NONE);
				GridLayout finalbuttonCompositeLayout = new GridLayout();
				finalbuttonCompositeLayout.makeColumnsEqualWidth = true;
				finalbuttonCompositeLayout.numColumns = 2;
				GridData finalbuttonCompositeLData = new GridData();
				finalbuttonCompositeLData.horizontalAlignment = GridData.FILL;
				finalbuttonCompositeLData.grabExcessHorizontalSpace = true;
				finalbuttonCompositeLData.horizontalSpan = 2;
				finalbuttonComposite.setLayoutData(finalbuttonCompositeLData);
				finalbuttonComposite.setLayout(finalbuttonCompositeLayout);
				{
					loadDefaults = new Button(finalbuttonComposite, SWT.PUSH
							| SWT.CENTER);
					GridData loadDefaultsLData = new GridData();
					loadDefaultsLData.horizontalAlignment = GridData.FILL;
					loadDefaultsLData.grabExcessHorizontalSpace = true;
					loadDefaults.setLayoutData(loadDefaultsLData);
					loadDefaults.setText("restore default settings");
				}
				{
					saveSettings = new Button(finalbuttonComposite, SWT.PUSH
							| SWT.CENTER);
					GridData saveSettingsLData = new GridData();
					saveSettingsLData.horizontalAlignment = GridData.FILL;
					saveSettingsLData.grabExcessHorizontalSpace = true;
					saveSettings.setLayoutData(saveSettingsLData);
					saveSettings.setText("save current settings");
				}
			}
			this.layout();
		} catch (Exception e) {
			e.printStackTrace();
		}


	}

	/** initialize listeners */
	private void initListeners(){

		// many controls -> many listeners

		// left top area listeners

		// the blank radio button
		inputBGBlankButton.addListener(SWT.Selection, new Listener () {
			public void handleEvent (Event e) {
				// disable buttons
				inputSolidBGColor.setEnabled(false);
				gradientSelectButton.setEnabled(false);
				// set property 
				props.put(generalPrefix+".bg.type","blank");

				// update examples
				updateExamples();
			}
		});

		// the solid color radio button
		inputSolidColor.addListener(SWT.Selection, new Listener () {
			public void handleEvent (Event e) {
				// disable buttons
				inputSolidBGColor.setEnabled(true);
				gradientSelectButton.setEnabled(false);

				// set property 
				props.put(generalPrefix+".bg.type","solid");

				// update examples
				updateExamples();
			}
		});

		// the BG Color choose Button from the general settings
		inputSolidBGColor.addListener(SWT.Selection, new Listener () {
			public void handleEvent (Event e) {
				ColorDialog dialog = new ColorDialog(parent.getShell());
				Color color = ResourceRegistry.getInstance().getColor(
						props.getProperty(generalPrefix+".bg.color")); 
				if (color!=null){
					dialog.setRGB(new RGB(color.getRed(),
							color.getGreen(),
							color.getBlue()));
				}
				RGB rgb = dialog.open();
				if(rgb!=null){
					//System.out.println("The chosen color was "+rgb);
					String colorCode = ResourceRegistry.getInstance().convertRGBtoColorCode(rgb);
					props.setProperty(generalPrefix+".bg.color",colorCode);
					props.setProperty(generalPrefix+".bg.type","solid");
					inputSolidBGColor.setImage(
							ResourceRegistry.getInstance().getImage(
									inputSolidBGColor,
									ResourceRegistry.getInstance().getColor(rgb.red,rgb.green,rgb.blue),
									15,
									10));
					inputSolidBGColor.setText(colorCode);

				} else {
					// set up default bg color white 
					//props.setProperty(generalPrefix+".bg.color","255,000,255");
					//props.setProperty(generalPrefix+".bg.type","solid");
				}
				// update examples
				updateExamples();
			}
		});


		// the gradient bg radio button
		inputGradient.addListener(SWT.Selection, new Listener () {
			public void handleEvent (Event e) {
				// disable buttons
				inputSolidBGColor.setEnabled(false);
				gradientSelectButton.setEnabled(true);

				// set property 
				props.put(generalPrefix+".bg.type","gradient");

				// update examples
				updateExamples();
			}
		});

		gradientSelectButton.addListener(SWT.Selection, new Listener () {
			public void handleEvent (Event e) {
				// create and open gradient selection dialog
				GradientSelectionDialog dialog = new GradientSelectionDialog(parent.getShell(),SWT.NONE);
				dialog.setGeneralPrefix(generalPrefix);
				Object ret = dialog.open(props);

				if(ret!=null){
					// user pressed the ok button and the new props are returned 
					Properties p =(Properties)ret;

					// write relevant properties back
					props.setProperty(generalPrefix+".bg.gradient.tocolor",p.getProperty(generalPrefix+".bg.gradient.tocolor"));
					props.setProperty(generalPrefix+".bg.gradient.fromcolor",p.getProperty(generalPrefix+".bg.gradient.fromcolor"));
					props.setProperty(generalPrefix+".bg.gradient.type",p.getProperty(generalPrefix+".bg.gradient.type"));

					// store the relevant values in the current props of the config

				} else {
					// do nothing, user canceled the gradient selection dialog
				}

				// update examples
				updateExamples();
			}
		});

		// the format sql92 check box
		inputUseSQL.addListener(SWT.Selection, new Listener () {
			public void handleEvent (Event e) {
				boolean state = ((Button)e.widget).getSelection();
				// set property 
				props.put(generalPrefix+".fg.formatSQL92",Boolean.toString(state));
				// update examples
				updateExamples();
			}
		});

		// tab_console_enable_button (tab_console) check box
		tab_console_enable_button.addListener(SWT.Selection, new Listener () {
			public void handleEvent (Event e) {
				boolean state = ((Button)e.widget).getSelection();
				// set property 
				props.put("console.output.tab.console.show",Boolean.toString(state));
				// update examples
				updateExamples();
			}
		});

		// tab_tree_enable_button (tab_tree) check box
		tab_tree_enable_button.addListener(SWT.Selection, new Listener () {
			public void handleEvent (Event e) {
				boolean state = ((Button)e.widget).getSelection();
				// set property 
				props.put("console.output.tab.tree.show",Boolean.toString(state));
				// update examples
				updateExamples();
			}
		});

		// tab_plan
		tab_plan_enable_button.addListener(SWT.Selection, new Listener () {
			public void handleEvent (Event e) {
				boolean state = ((Button)e.widget).getSelection();
				// set property 
				props.put("console.output.tab.plan.show",Boolean.toString(state));
				// update examples
				updateExamples();
			}
		}); 

		// tab_olad explorer
		tab_olap_enable_button.addListener(SWT.Selection, new Listener () {
			public void handleEvent (Event e) {
				boolean state = ((Button)e.widget).getSelection();
				// set property 
				props.put("console.output.tab.olap.show",Boolean.toString(state));
				// update examples
				updateExamples();
			}
		}); 

		// tab_onlineSampling explorer
		tab_onlineSampling_enable_button.addListener(SWT.Selection, new Listener () {
			public void handleEvent (Event e) {
				boolean state = ((Button)e.widget).getSelection();
				// set property 
				props.put("console.output.tab.onlineSampling.show",Boolean.toString(state));
				// update examples
				updateExamples();
			}
		}); 

		// tab_refresh
		tab_refresh_enable_button.addListener(SWT.Selection, new Listener () {
			public void handleEvent (Event e) {
				boolean state = ((Button)e.widget).getSelection();
				// set property 
				props.put("console.output.tab.refresh.show",Boolean.toString(state));
				// update examples
				updateExamples();
			}
		}); 


		// enables tab tab_system
		tab_system_enable_button.addListener(SWT.Selection, new Listener () {
			public void handleEvent (Event e) {
				boolean state = ((Button)e.widget).getSelection();
				// set property 
				props.put("console.output.tab.system.show",Boolean.toString(state));
				// update examples
				updateExamples();
			}
		}); 

		// enables tab tab_sampleCatalog
		tab_sampleCatalog_enable_button.addListener(SWT.Selection, new Listener () {
			public void handleEvent (Event e) {
				boolean state = ((Button)e.widget).getSelection();
				// set property 
				props.put("console.output.tab.sampleCatalog.show",Boolean.toString(state));
				// update examples
				updateExamples();
			}
		}); 

		// enables tab tab_workload
		tab_workload_enable_button.addListener(SWT.Selection, new Listener () {
			public void handleEvent (Event e) {
				boolean state = ((Button)e.widget).getSelection();
				// set property 
				props.put("console.output.tab.workload.show",Boolean.toString(state));
				// update examples
				updateExamples();
			}
		}); 

		// enables tab tab_kernelDesnsityPlot
		tab_kernelDensityPlot_enable_button.addListener(SWT.Selection, new Listener () {
			public void handleEvent (Event e) {
				boolean state = ((Button)e.widget).getSelection();
				// set property 
				props.put("console.output.tab.kernelDensityPlot.show",Boolean.toString(state));
				// update examples
				updateExamples();
			}
		}); 

		// enables tab tab_diagnosis
		tab_diagnosis_enable_button.addListener(SWT.Selection, new Listener () {
			public void handleEvent (Event e) {
				boolean state = ((Button)e.widget).getSelection();
				// set property 
				props.put("console.output.tab.diagnosis.show",Boolean.toString(state));
				// update examples
				updateExamples();
			}
		}); 

		//enables tab tab_history
		tab_history_enable_button.addListener(SWT.Selection, new Listener () {
			public void handleEvent (Event e) {
				boolean state = ((Button)e.widget).getSelection();
				// set property 
				props.put("console.output.tab.history.show",Boolean.toString(state));
				// update examples
				updateExamples();
			}
		}); 

		// enables tab tab_config
		tab_config_enable_button.addListener(SWT.Selection, new Listener () {
			public void handleEvent (Event e) {
				boolean state = ((Button)e.widget).getSelection();
				// set property 
				props.put("console.output.tab.config.show",Boolean.toString(state));
				// update examples
				updateExamples();
			}
		}); 

		// enables tab tab_help
		tab_help_enable_button.addListener(SWT.Selection, new Listener () {
			public void handleEvent (Event e) {
				boolean state = ((Button)e.widget).getSelection();
				// set property 
				props.put("console.output.tab.help.show",Boolean.toString(state));
				// update examples
				updateExamples();
			}
		}); 


		// enables tab tab_about
		tab_about_enable_button.addListener(SWT.Selection, new Listener () {
			public void handleEvent (Event e) {
				boolean state = ((Button)e.widget).getSelection();
				// set property 
				props.put("console.output.tab.about.show",Boolean.toString(state));
				// update examples
				updateExamples();
			}
		}); 

		// the transform sql92 check box
		inputUseTransform.addListener(SWT.Selection, new Listener () {
			public void handleEvent (Event e) {
				boolean state = ((Button)e.widget).getSelection();
				// set property 
				props.put(generalPrefix+".fg.transformSQL92",Boolean.toString(state));
				// update examples
				updateExamples();
			}
		});

		// combo box listeners
		// the input/output choose combo
		IOChooser.addListener(SWT.Selection, new Listener() {
			public void handleEvent(Event event) {
				int index = ((Combo)event.widget).getSelectionIndex();
				generalPrefix = genPrefixes[index];
				setupGeneralSettings();
			}
		});

		// the format output area combo
		outpuAreaChooseComboBox.addListener(SWT.Selection, new Listener() {
			public void handleEvent(Event event) {
				int index = ((Combo)event.widget).getSelectionIndex();
				outputFormatPrefix = formatPrefixes[index];
				setupFormatSettings();
			}
		});

		// the left bottom area listeners

		// the custom fg color radio button
		customFGColorSQLStyleRadioButton.addListener(SWT.Selection, new Listener() {
			public void handleEvent(Event event) {
				inputSQLStyleFGColorButton.setEnabled(true);
				updateExamples();
			}
		});

		// the default fg color radio button
		defaultFGColorSQLStyleRadioButton.addListener(SWT.Selection, new Listener() {
			public void handleEvent(Event event) {
				inputSQLStyleFGColorButton.setEnabled(false);
				// blue is default color
				props.setProperty(generalPrefix+".fg.sql.fgcolor","000,000,255");
				inputSQLStyleFGColorButton.setImage(
						ResourceRegistry.getInstance().getImage(
								inputSQLStyleFGColorButton,
								ResourceRegistry.getInstance().getColor(0,0,255),
								15,
								10));
				inputSQLStyleFGColorButton.setText("000,000,255");

				updateExamples();
			}
		});

		// the fg color choose button
		inputSQLStyleFGColorButton.addListener(SWT.Selection, new Listener () {
			public void handleEvent (Event e) {
				ColorDialog dialog = new ColorDialog(parent.getShell());
				Color color = ResourceRegistry.getInstance().getColor(
						props.getProperty(generalPrefix+".fg.sql.fgcolor")); 
				if (color!=null){
					dialog.setRGB(new RGB(color.getRed(),color.getGreen(),color.getBlue()));
				}                
				RGB rgb = dialog.open();
				if(rgb!=null){
					//System.out.println("The chosen color was "+rgb);
					String colorCode = ResourceRegistry.getInstance().convertRGBtoColorCode(rgb);
					props.setProperty(generalPrefix+".fg.sql.fgcolor",colorCode);
					inputSQLStyleFGColorButton.setImage(
							ResourceRegistry.getInstance().getImage(
									inputSQLStyleFGColorButton,
									ResourceRegistry.getInstance().getColor(rgb.red,rgb.green,rgb.blue),
									15,
									10));
					inputSQLStyleFGColorButton.setText(colorCode);


				} else {
					// do nothing
					// set up default sql fg color : red
					// props.setProperty(generalPrefix+".fg.sql.fgcolor","255,000,000");
				}
				// update examples
				updateExamples();
			}
		});

		// the input custom bg color radio button
		customSQLStyleBGColorRadioButton.addListener(SWT.Selection, new Listener() {
			public void handleEvent(Event event) {
				boolean state = ((Button)event.widget).getSelection();
				inputSQLStyleBGColorButton.setEnabled(state);
				updateExamples();
			}
		});

		// the default bg color radio button
		defaultBGColorSQLStyleRadioButton.addListener(SWT.Selection, new Listener() {
			public void handleEvent(Event event) {
				boolean state = ((Button)event.widget).getSelection();
				inputSQLStyleBGColorButton.setEnabled(!state);
				// default bg color: red
				props.setProperty(generalPrefix+".fg.sql.bgcolor","null");

				inputSQLStyleBGColorButton.setImage(
						ResourceRegistry.getInstance().getImage("nullColor"));
				inputSQLStyleBGColorButton.setText("null");

				updateExamples();
			}
		});


		// the bg color choose button
		inputSQLStyleBGColorButton.addListener(SWT.Selection, new Listener () {
			public void handleEvent (Event e) {
				ColorDialog dialog = new ColorDialog(parent.getShell());
				Color color = ResourceRegistry.getInstance().getColor(
						props.getProperty(generalPrefix+".fg.sql.bgcolor"));
				if (color!=null){
					dialog.setRGB(new RGB(color.getRed(),color.getGreen(),color.getBlue()));
				}

				RGB rgb = dialog.open();
				if(rgb!=null){
					//System.out.println("The chosen color was "+rgb);
					String colorCode = ResourceRegistry.getInstance().convertRGBtoColorCode(rgb);
					props.setProperty(generalPrefix+".fg.sql.bgcolor",colorCode);
					inputSQLStyleBGColorButton.setImage(
							ResourceRegistry.getInstance().getImage(
									inputSQLStyleFGColorButton,
									ResourceRegistry.getInstance().getColor(rgb.red,rgb.green,rgb.blue),
									15,
									10));
					inputSQLStyleBGColorButton.setText(colorCode);

				} else {
					// do nothing
					// set up default sql fg color : null
					//props.setProperty(generalPrefix+".fg.sql.bgcolor","null");
				}
				// update examples
				updateExamples();
			}
		});

		// the custom font radio button
		customFontSQLStyleRadioButton.addListener(SWT.Selection, new Listener() {
			public void handleEvent(Event event) {
				inputSQLStyleFontButton.setEnabled(true);
				updateExamples();

			}
		});

		// the default font radio button
		defaultFontSQLStyleRadioButton.addListener(SWT.Selection, new Listener() {
			public void handleEvent(Event event) {
				inputSQLStyleFontButton.setEnabled(false);
				String value = "1|Courier New|10|2|WINDOWS|1|-21|0|0|0|700|0|0|0|1|0|0|0|Courier New";
				props.setProperty(generalPrefix+".fg.sql.font",value);

				// get font for sql92 keywords
				ResourceRegistry.getInstance().getFont(value);
				inputSQLStyleFontButton.setFont(
						ResourceRegistry.getInstance().getPreviewFont10pt(value));

				// TODO set default sql font style
				updateExamples();

			}
		});

		// the font choose button
		inputSQLStyleFontButton.addListener(SWT.Selection, new Listener () {
			public void handleEvent (Event e) {
				FontDialog dialog = new FontDialog(parent.getShell());
				Font currentFont = ResourceRegistry.getInstance().getFont(
						props.getProperty(generalPrefix+".fg.sql.font")); 
				if (currentFont!=null) {
					dialog.setFontList(currentFont.getFontData());
				}
				FontData font = dialog.open();
				if(font!=null){
					//System.out.println("The chosen font was "+font);
					props.setProperty(generalPrefix+".fg.sql.font",font.toString());
					// get normal font first
					ResourceRegistry.getInstance().getFont(font.toString());
					// and then the reduced adapted font for the button from the original font
					inputSQLStyleFontButton.setFont(
							ResourceRegistry.getInstance().getPreviewFont10pt(font.toString()));

				} else {
					// do nothing
					// TODO get defaultfont, which is standard on all platforms

				}
				// update examples
				updateExamples();
			}
		});


		// the area format style radio buttons

		// the custom fg color radio button
		areaCustomFGColorRadioButton.addListener(SWT.Selection, new Listener() {
			public void handleEvent(Event event) {
				areaFGColorButton.setEnabled(true);
				updateExamples();
			}
		});

		// default area style fg color radio button
		areaDefaultFGColorRadioButton.addListener(SWT.Selection, new Listener() {
			public void handleEvent(Event event) {
				areaFGColorButton.setEnabled(false);
				// set default sql fg color red
				props.setProperty(outputFormatPrefix+".fg.fgcolor","000,000,255");
				areaFGColorButton.setImage(
						ResourceRegistry.getInstance().getImage(
								areaFGColorButton,
								ResourceRegistry.getInstance().getColor(0,0,255),
								15,
								10));
				areaFGColorButton.setText("000,000,255");

				updateExamples();
			}
		});

		areaFGColorButton.addListener(SWT.Selection, new Listener () {
			public void handleEvent (Event e) {
				ColorDialog dialog = new ColorDialog(parent.getShell());
				Color color = ResourceRegistry.getInstance().getColor(
						props.getProperty(outputFormatPrefix+".fg.fgcolor")); 
				if (color!=null){
					dialog.setRGB(new RGB(color.getRed(),color.getGreen(),color.getBlue()));
				}                
				RGB rgb = dialog.open();
				if(rgb!=null){
					//System.out.println("The chosen color was "+rgb);
					String colorCode = ResourceRegistry.getInstance().convertRGBtoColorCode(rgb);
					props.setProperty(outputFormatPrefix+".fg.fgcolor",colorCode);
					areaFGColorButton.setImage(
							ResourceRegistry.getInstance().getImage(
									areaFGColorButton,
									ResourceRegistry.getInstance().getColor(rgb.red,rgb.green,rgb.blue),
									15,
									10));
					areaFGColorButton.setText(colorCode);

				} else {
					// do nothing
					// props.setProperty(outputFormatPrefix+".fg.fgcolor","000,000,000");
				}
				// update examples
				updateExamples();
			}
		});

		// the area custom bg color radio button
		areaCustomBGColorRadioButton.addListener(SWT.Selection, new Listener() {
			public void handleEvent(Event event) {
				areaBGColorButton.setEnabled(true);
				updateExamples();
			}
		});

		// the area default bg color radio button
		areaDefaultBGColorRadioButton.addListener(SWT.Selection, new Listener() {
			public void handleEvent(Event event) {
				boolean state = ((Button)event.widget).getSelection();

				areaBGColorButton.setEnabled(!state);

				// set default to null background color 
				props.setProperty(outputFormatPrefix+".fg.bgcolor","null");

				areaBGColorButton.setImage(
						ResourceRegistry.getInstance().getImage("nullColor"));
				areaBGColorButton.setText("null");

				updateExamples();

			}
		});

		// the area bg color choose button
		areaBGColorButton.addListener(SWT.Selection, new Listener () {
			public void handleEvent (Event e) {
				ColorDialog dialog = new ColorDialog(parent.getShell());
				Color color = ResourceRegistry.getInstance().getColor(
						props.getProperty(outputFormatPrefix+".fg.bgcolor")); 
				if (color!=null){
					dialog.setRGB(new RGB(color.getRed(),color.getGreen(),color.getBlue()));
				}                
				RGB rgb = dialog.open();
				if(rgb!=null){
					//System.out.println("The chosen color was "+rgb);
					String colorCode = ResourceRegistry.getInstance().convertRGBtoColorCode(rgb);
					props.setProperty(outputFormatPrefix+".fg.bgcolor",colorCode);
					areaBGColorButton.setImage(
							ResourceRegistry.getInstance().getImage(
									areaBGColorButton,
									ResourceRegistry.getInstance().getColor(rgb.red,rgb.green,rgb.blue),
									15,
									10));
					areaBGColorButton.setText(colorCode);

				} else {
					// do nothing !
					// props.setProperty(outputFormatPrefix+".fg.bgcolor","null");
				}
				// update examples
				updateExamples();
			}
		});

		// the custom font radio button
		areaCustomFontRadioButton.addListener(SWT.Selection, new Listener() {
			public void handleEvent(Event event) {
				areaFontButton.setEnabled(true);
				updateExamples();
			}
		});

		// default font radio button
		areaDefaultFontRadioButton.addListener(SWT.Selection, new Listener() {
			public void handleEvent(Event event) {
				areaFontButton.setEnabled(false);
				String value = "1|Courier New|10|2|WINDOWS|1|-21|0|0|0|700|0|0|0|1|0|0|0|Courier New";
				props.setProperty(outputFormatPrefix+".fg.font",value);

				// get font 
				ResourceRegistry.getInstance().getFont(value);
				// set font to preview on button
				areaFontButton.setFont(
						ResourceRegistry.getInstance().getPreviewFont10pt(value));

				updateExamples();
			}
		});

		// the area font choose button
		areaFontButton.addListener(SWT.Selection, new Listener () {
			public void handleEvent (Event e) {
				FontDialog dialog = new FontDialog(parent.getShell());
				Font currentFont = ResourceRegistry.getInstance().getFont(
						props.getProperty(outputFormatPrefix+".fg.font")); 
				if (currentFont!=null) {
					dialog.setFontList(currentFont.getFontData());
				}
				FontData font = dialog.open();
				if(font!=null){
					//System.out.println("The chosen font was "+font);
					props.setProperty(outputFormatPrefix+".fg.font",font.toString());
					// first get font to create original one
					ResourceRegistry.getInstance().getFont(font.toString());
					// and then create the small adapted version for the button
					areaFontButton.setFont(
							ResourceRegistry.getInstance().getPreviewFont10pt(font.toString()));
				} else {
					// do nothing
				}
				// update examples
				updateExamples();
			}
		});

		// the load defaults button
		loadDefaults.addListener(SWT.Selection, new Listener () {
			public void handleEvent (Event e) {
				loadDefaultSettings();
				setupGeneralSettings();
				setupFormatSettings();
				updateExamples();
			}
		});

		// the save settings button
		saveSettings.addListener(SWT.Selection, new Listener () {
			public void handleEvent (Event e) {
				saveSettings();
			}
		});

	}

	/**
	 * convenience method to add a background decoration, a GenericResizeListener 
	 * @param key specifies the config information base key, e.g. console.output.bg 
	 * @param st the Styledtext widget to assign this new created listener 
	 * @return new pre-configured listener to apply
	 */
	private GenericResizeListener addBGDecoration(String key, StyledText st){

		GenericResizeListener listener = new GenericResizeListener();

		listener.parent       = st; 

		listener.useImage     = Boolean.parseBoolean(props.getProperty(key+".showLogo"));
		listener.type         = props.getProperty(key+".type"); 
		listener.bgColor      = ResourceRegistry.getInstance().getColor(
				props.getProperty(key+".color")); 
		listener.gradienttype = props.getProperty(key+".gradient.type"); 
		listener.one          = ResourceRegistry.getInstance().getColor(
				props.getProperty(key+".gradient.fromcolor"));
		listener.two          = ResourceRegistry.getInstance().getColor(
				props.getProperty(key+".gradient.tocolor"));

		listener.setup();
		//listener.decorateImage = ResourceRegistry.getInstance().getImage("smalllogo");

		return listener;

	}

	/** convenience method to load specific style range settings for a base key 
	 *  @return new pre-configured StyleRange object
	 * */
	private StyleRange loadStyleRangeFromConfiguration(String key){

		StyleRange sr = new StyleRange();

		sr.foreground = ResourceRegistry.getInstance().getColor(
				props.getProperty(key+".fgcolor"));
		sr.background = ResourceRegistry.getInstance().getColor(
				props.getProperty(key+".bgcolor"));
		sr.font = ResourceRegistry.getInstance().getFont(
				props.getProperty(key+".font"));
		return sr;
	}


	/** updates the exmaples to reflect the changes of the user */
	private void updateExamples(){
		// clean contents
		exampleInputST.setText(""); // clear example input console
		exampleInputST.setCaretOffset(0); // reset caret position

		exampleOutputST.setText(""); // clear example input console
		exampleOutputST.setCaretOffset(0); // reset caret position

		// set default fonts
		exampleOutputST.setFont(ResourceRegistry.getInstance().getFont(
				props.getProperty("console.output.fg.default.fg.font")));

		exampleInputST.setFont(ResourceRegistry.getInstance().getFont(
				props.getProperty("console.input.fg.default.fg.font")));

		exampleInputST.setForeground(ResourceRegistry.getInstance().getColor(
				props.getProperty("console.input.fg.default.fg.fgcolor")));

		// format the output console
		// 1st create the different StyleRanges to format the output console

		// create Stylerange array to hold the different style ranges
		StyleRange[] srs = new StyleRange[4];

		srs[0] = loadStyleRangeFromConfiguration("console.output.stmts.fg");
		srs[1] = loadStyleRangeFromConfiguration("console.output.results.fg");
		srs[2] = loadStyleRangeFromConfiguration("console.output.warnings.fg");
		srs[3] = loadStyleRangeFromConfiguration("console.output.exceptions.fg");

		// create one more StyleRange to format the SQL Text in the output console
		StyleRange sqlTextSR =  loadStyleRangeFromConfiguration("console.output.fg.sql");

		// now create the decorated listeners, which are responsible for the formatting
		// the order of creation is important for fallback strategies

		// this is the main outer listener which gets called first 
		outputListener =   new GenericStyleListener(new DefaultLineStyleListener(),srs);

		// if sql formatting is on create a wrapping SQLListener
		boolean sqlon = Boolean.parseBoolean(props.getProperty("console.output.fg.formatSQL92"));
		if (sqlon){
			sqlListener = new SQLKeywordLineStyleListener(outputListener,sqlTextSR);

			// toggle cutoffendchar on, default after creation is off!
			sqlListener.switchCutOffChar();

			// Add the decorated listener to the widget 
			exampleOutputST.addLineStyleListener(sqlListener);
		} else {
			// only add the generic listener
			exampleOutputST.addLineStyleListener(outputListener);
		}

		// Now format the bg coloring of the output console

		// get the four colors which format the background  
		Color[] colors = new Color[4];
		colors[0] = ResourceRegistry.getInstance().getColor(
				props.getProperty("console.output.stmts.fg.bgcolor"));
		colors[1] = ResourceRegistry.getInstance().getColor(
				props.getProperty("console.output.results.fg.bgcolor"));
		colors[2] = ResourceRegistry.getInstance().getColor(
				props.getProperty("console.output.warnings.fg.bgcolor"));
		colors[3] = ResourceRegistry.getInstance().getColor(
				props.getProperty("console.output.exceptions.fg.bgcolor"));

		// Create a new GenericBGColorListener, to handle the color information
		outputBGColorListener = new GenericBGColorListener(colors);

		// And add this to the output console
		exampleOutputST.addLineBackgroundListener(outputBGColorListener);

		// finally add a Background decoration for the output console

		exampleOutputST.setBackground(null);
		exampleOutputST.setBackgroundImage(null);

		if (bgListenerMain!=null) exampleOutputST.removeListener(SWT.Resize, bgListenerMain);
		bgListenerMain = addBGDecoration("console.output.bg",exampleOutputST);
		exampleOutputST.addListener(SWT.Resize,bgListenerMain);


		// add this new listener to the widget 
		exampleOutputST.addListener (SWT.Resize, bgListenerMain);

		// add a DefaultLineStyleListener, decorated with a default SQLLinestyleListener 
		// to the input console
		StyleRange[] inputSRanges = new StyleRange[1];

		// get the configuration for the SQLLineStyleListener 
		inputSRanges[0] = loadStyleRangeFromConfiguration("console.input.fg.default");

		// check to see if the input shoult be formatted with SQL Highlightning
		boolean result = Boolean.parseBoolean(props.getProperty("console.input.fg.formatSQL92"));

		// setup the StyleRange to use for SQL Keyword formatting for the input console
		StyleRange sqlInputSR = loadStyleRangeFromConfiguration("console.input.fg.sql"); 

		// add the different decorated LineStyleListeners to the input console

		exampleInputST.addLineStyleListener(new GenericStyleListener(
				new SQLKeywordLineStyleListener(
						new DefaultLineStyleListener(),
						sqlInputSR),
						inputSRanges));

		// add an Upper-Key-SQL-Keyword-Transformer if flag is set 
		result = Boolean.parseBoolean(props.getProperty("console.input.fg.transformSQL92"));

		if(result) {
			if (inputUpperKeyListener==null) {
				inputUpperKeyListener = new UpperKeyListener(exampleInputST,true);
				exampleInputST.addVerifyKeyListener(inputUpperKeyListener);
			}
		} else {
			if (inputUpperKeyListener!=null) {
				exampleInputST.removeVerifyKeyListener(inputUpperKeyListener);
			}
		}

		String input;
		if(result){
			input = "SELECT COUNT(*) FROM lineitem ";
		} else {
			input = "select count(*) from lineitem ";
		}

		// finally add the background decoration for the input console


		if (bgListenerInput!=null) {
			// reset the StyledText component
			exampleInputST.setBackground(null);
			exampleInputST.setBackgroundImage(null);
			exampleInputST.removeListener(SWT.Resize, bgListenerInput);
		}
		// create a ResizeListener from the current configured bg decoration
		bgListenerInput = addBGDecoration("console.input.bg",exampleInputST);
		// add this new listener to the widget
		exampleInputST.addListener(SWT.Resize,bgListenerInput);

		// fill the examples with test content
		// get line delimiter to mimic input behaviour
		String newLine = exampleOutputST.getLineDelimiter();
		// force a redraw of the input widget
		exampleInputST.redraw();

		// apply example input text
		exampleInputST.setText(input);

		result = Boolean.parseBoolean(props.getProperty("console.output.fg.transformSQL92"));
		if(result){
			input = "SELECT COUNT(*) FROM lineitem ";
		} else {
			input = "select count(*) from lineitem ";
		}

		// create new example output text
		String[] output = new String[]{
				input,
				"1 \n------------ \n60005\n1 row(s) selected.\nTotal time: 0.031s ",
				"Warning",
		"Exception"};

		// add range for query

		int start = exampleOutputST.getCharCount();
		int length = output[0].length();
		outputListener.addRange(0,start,length);
		outputBGColorListener.addRange(0,start,length);
		exampleOutputST.append(output[0]);

		// add range for results
		exampleOutputST.append(newLine);
		start = exampleOutputST.getCharCount();
		length = output[1].length();
		outputListener.addRange(1, start,length);
		outputBGColorListener.addRange(1, start, length);
		// exclude the result area from begin formatted by the SQLListener
		sqlListener.addRange(start, length);
		exampleOutputST.append(output[1]);

		// add range for warning
		exampleOutputST.append(newLine);
		start = exampleOutputST.getCharCount();
		length = output[2].length();
		outputListener.addRange(2, start,length);
		outputBGColorListener.addRange(2, start, length);
		// exclude the warning area from begin formatted by the SQLListener
		sqlListener.addRange(start, length);
		exampleOutputST.append(output[2]);

		// add range for exception
		exampleOutputST.append(newLine);
		start = exampleOutputST.getCharCount();
		length = output[3].length();
		outputListener.addRange(3, start,length);
		outputBGColorListener.addRange(3, start, length);
		// exclude the exception are from begin formatted by the SQLListener
		sqlListener.addRange(start, length);
		exampleOutputST.append(output[3]);

		// redraw the widgets
		/*
        Point size = exampleInputST.getSize();
        exampleInputST.layout(true);
        exampleInputST.setSize(size.x,size.y);
        exampleInputST.redraw();

        size = exampleOutputST.getSize();
        exampleOutputST.layout(true);
        exampleOutputST.setSize(size.x,size.y);
        exampleOutputST.redraw();*/


	}

}
