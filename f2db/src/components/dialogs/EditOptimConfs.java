package components.dialogs;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import modules.databaseif.ConnectionInfo;
import modules.databaseif.ConstantStatements;
import modules.databaseif.JDBCInterface;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Dialog;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Spinner;


/**
 * This is a helper dialog to edit optim properties
 * @author Christopher Schildt
 * @date   08.05.2006
 *
 */
public class EditOptimConfs extends Dialog{
	
	private Shell dialogShell;
	private Label label1;
	private Combo comboOptimMethodGlobal;
	private Combo comboOptimMethodLocal;
	private Spinner spinnerME;
	private Button  spinnerMECheck;
	private Spinner spinnerTI;
	private Button  spinnerTICheck;
	private Spinner spinnerFA;
	private Button  spinnerFACheck;
	private Spinner spinnerFR;
	private Button  spinnerFRCheck;
	private Spinner spinnerXA;
	private Button  spinnerXACheck;
	private Spinner spinnerXR;
	private Button  spinnerXRCheck;
	private Spinner spinner;
	private Button check;
	private Button cancelButton;
	private Button okButton;

	public EditOptimConfs(Shell arg0, int arg1) {
		super(arg0, arg1);
		// TODO Auto-generated constructor stub
	}
	
	
public void open(String title) {
		
		Shell parent = getParent();
		dialogShell = new Shell(parent, SWT.DIALOG_TRIM | SWT.APPLICATION_MODAL | SWT.RESIZE);
		dialogShell.setSize(500, 300);
		initComponents();
		setComponents();
		initListeners();
		dialogShell.setText(title);
		
		dialogShell.open();
		Display display = parent.getDisplay();
		while (!dialogShell.isDisposed()) {
			if (!display.readAndDispatch()) display.sleep();
		}
	}


private void setComponents() {
	if (!JDBCInterface.getInstance().isPostgres_connected()) 
		return; //should never happen
	Statement stmt;
	try {
		stmt = JDBCInterface.getInstance().getConnection().createStatement();
		//check global opt
		stmt.execute(ConstantStatements.GET_ACTGLOBALOPTIM);
		ResultSet rs = stmt.getResultSet();
		rs.next();
		comboOptimMethodGlobal.setText(rs.getString(1));
		rs.close();
		//check local opt
		stmt.execute(ConstantStatements.GET_ACTLOCALOPTIM);
		rs = stmt.getResultSet();
		rs.next();
		comboOptimMethodLocal.setText(rs.getString(1));
		rs.close();
		//check max eval
		stmt.execute(ConstantStatements.GET_MAXEVAL);
		rs = stmt.getResultSet();
		rs.next();
		spinnerME.setSelection((rs.getInt((1))));
		if(spinnerME.getSelection()==0)
		{
			spinnerMECheck.setSelection(false);
			spinnerME.setEnabled(false);
		}
		else
			spinnerMECheck.setSelection(true);
		rs.close();
		//check max time
		stmt.execute(ConstantStatements.GET_MAXTIME);
		rs = stmt.getResultSet();
		rs.next();
		System.out.println(rs.getDouble((1)));
		spinnerTI.setDigits(3);
	    // set the maximum value to 20
	    spinnerTI.setMaximum(Integer.MAX_VALUE);

		int front=(int)(((rs.getDouble((1)))*1000.0));
		spinnerTI.setSelection(front);
		if(spinnerTI.getSelection()==0)
		{
			spinnerTICheck.setSelection(false);
			spinnerTI.setEnabled(false);
		}
		else
			spinnerTICheck.setSelection(true);

		rs.close();
		//check ftol abs
		stmt.execute(ConstantStatements.GET_FTOLA);
		rs = stmt.getResultSet();
		rs.next();
		spinnerFA.setDigits(3);
	    // set the maximum value to 20
	    spinnerFA.setMaximum(Integer.MAX_VALUE);

		front=(int)(((rs.getDouble((1)))*1000.0));
		spinnerFA.setSelection(front);
		if(spinnerFA.getSelection()==0)
		{
			spinnerFACheck.setSelection(false);
			spinnerFA.setEnabled(false);
		}
		else
			spinnerFACheck.setSelection(true);
		rs.close();
		//check ftol rel
		stmt.execute(ConstantStatements.GET_FTOLR);
		rs = stmt.getResultSet();
		rs.next();
		spinnerFR.setDigits(3);
	    // set the maximum value to 20
	    spinnerFR.setMaximum(Integer.MAX_VALUE);

		front=(int)(((rs.getDouble((1)))*1000.0));
		spinnerFR.setSelection(front);
		if(spinnerFR.getSelection()==0)
		{
			spinnerFRCheck.setSelection(false);
			spinnerFR.setEnabled(false);
		}
		else
			spinnerFRCheck.setSelection(true);
		rs.close();
		//check xtol abs
		stmt.execute(ConstantStatements.GET_XTOLA);
		rs = stmt.getResultSet();
		rs.next();
		spinnerXA.setDigits(3);
	    // set the maximum value to 20
	    spinnerXA.setMaximum(Integer.MAX_VALUE);

		front=(int)(((rs.getDouble((1)))*1000.0));
		spinnerXA.setSelection(front);
		if(spinnerXA.getSelection()==0)
		{
			spinnerXACheck.setSelection(false);
			spinnerXA.setEnabled(false);
		}
		else
			spinnerXACheck.setSelection(true);
		rs.close();
		//check xtol rel
		stmt.execute(ConstantStatements.GET_XTOLR);
		rs = stmt.getResultSet();
		rs.next();
		spinnerXR.setDigits(3);
	    // set the maximum value to 20
	    spinnerXR.setMaximum(Integer.MAX_VALUE);

		front=(int)(((rs.getDouble((1)))*1000.0));
		spinnerXR.setSelection(front);
		if(spinnerXR.getSelection()==0)
		{
			spinnerXRCheck.setSelection(false);
			spinnerXR.setEnabled(false);
		}
		else
			spinnerXRCheck.setSelection(true);
		rs.close();
		


	} catch (SQLException e) {
		// TODO Auto-generated catch block
		e.printStackTrace();
	}
	
	
	
}


private void initListeners() {
	okButton.addListener(SWT.Selection, new Listener() {
		public void handleEvent(Event event) {
			Statement stmt;
			//Set stuff
			try {
				stmt = JDBCInterface.getInstance().getConnection().createStatement();
				stmt.execute("Set optim_method_general to \'"+comboOptimMethodGlobal.getText()+"\'");
				stmt.execute("Set optim_method_local to \'"+comboOptimMethodLocal.getText()+"\'");
				if(spinnerTICheck.getSelection())
					stmt.execute("Set optim_term_maxtime to "+spinnerTI.getText().replace(",","."));
				else
					stmt.execute("Set optim_term_maxtime to 0");
				if(spinnerMECheck.getSelection())
					stmt.execute("Set optim_term_maxeval to "+spinnerME.getText());
				else
					stmt.execute("Set optim_term_maxeval to 0");
				if(spinnerFACheck.getSelection())
					stmt.execute("Set optim_term_ftol_abs to "+spinnerFA.getText().replace(",","."));
				else
					stmt.execute("Set optim_term_ftol_abs to 0");
				if(spinnerFRCheck.getSelection())
					stmt.execute("Set optim_term_ftol_rel to "+spinnerFR.getText().replace(",","."));
				else
					stmt.execute("Set optim_term_ftol_rel to 0");
				if(spinnerXACheck.getSelection())
					stmt.execute("Set optim_term_xtol_abs to "+spinnerXA.getText().replace(",","."));
				else
					stmt.execute("Set optim_term_xtol_abs to 0");
				if(spinnerXRCheck.getSelection())
					stmt.execute("Set optim_term_xtol_rel to "+spinnerXR.getText().replace(",","."));
				else
					stmt.execute("Set optim_term_xtol_rel to 0");
			} catch (SQLException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			//check global opt

            dialogShell.dispose();
                
		}}); 

	cancelButton.addListener(SWT.Selection, new Listener() {
		public void handleEvent(Event event) {
			// close dialog
			dialogShell.dispose();
			// the return object
		}}); 

	spinnerFACheck.addListener(SWT.Selection, new Listener() {
		public void handleEvent(Event event) {
			spinnerFA.setEnabled(spinnerFACheck.getSelection());	

		}});
	spinnerFRCheck.addListener(SWT.Selection, new Listener() {
		public void handleEvent(Event event) {
			spinnerFR.setEnabled(spinnerFRCheck.getSelection());	

		}});
	spinnerXACheck.addListener(SWT.Selection, new Listener() {
		public void handleEvent(Event event) {
			spinnerXA.setEnabled(spinnerXACheck.getSelection());	

		}});
	spinnerXRCheck.addListener(SWT.Selection, new Listener() {
		public void handleEvent(Event event) {
			spinnerXR.setEnabled(spinnerXRCheck.getSelection());	

		}});
	spinnerTICheck.addListener(SWT.Selection, new Listener() {
		public void handleEvent(Event event) {
			spinnerTI.setEnabled(spinnerTICheck.getSelection());	

		}});
	spinnerMECheck.addListener(SWT.Selection, new Listener() {
		public void handleEvent(Event event) {
			spinnerME.setEnabled(spinnerMECheck.getSelection());	

		}});
	

}


private void initComponents() {
	GridLayout dialogShellLayout = new GridLayout();
	dialogShell.setLayout(dialogShellLayout);
	dialogShellLayout.numColumns = 2;
	
	{
		check= new Button(dialogShell,SWT.CHECK);
		GridData connectionLabelLData = new GridData();
		connectionLabelLData.horizontalAlignment = GridData.FILL;
		check.setLayoutData(connectionLabelLData);	
		check.setText("Global Optim method");
		check.setSelection(true);
		check.setEnabled(false);
	}
	{
		comboOptimMethodGlobal=new Combo(dialogShell, SWT.DROP_DOWN);
		GridData connectionLabelLData = new GridData();
		connectionLabelLData.horizontalAlignment = GridData.FILL;
		comboOptimMethodGlobal.setLayoutData(connectionLabelLData);
		comboOptimMethodGlobal.setItems(JDBCInterface.getInstance().getOptim_optnames());
	}
	{
		check= new Button(dialogShell,SWT.CHECK);
		GridData connectionLabelLData = new GridData();
		connectionLabelLData.horizontalAlignment = GridData.FILL;
		check.setLayoutData(connectionLabelLData);	
		check.setText("Local Optim method");
		check.setSelection(true);
		check.setEnabled(false);
	}
	{
		comboOptimMethodLocal=new Combo(dialogShell, SWT.DROP_DOWN);
		GridData connectionLabelLData = new GridData();
		connectionLabelLData.horizontalAlignment = GridData.FILL;
		comboOptimMethodLocal.setLayoutData(connectionLabelLData);
		comboOptimMethodLocal.setItems(JDBCInterface.getInstance().getOptim_optnames());
	}
	{
		spinnerTICheck= new Button(dialogShell,SWT.CHECK);
		GridData connectionLabelLData = new GridData();
		connectionLabelLData.horizontalAlignment = GridData.FILL;
		spinnerTICheck.setLayoutData(connectionLabelLData);
		spinnerTICheck.setText("Max. Time (seconds)");
	}

	{
		spinnerTI=new Spinner(dialogShell, SWT.None);
		GridData connectionLabelLData = new GridData();
		connectionLabelLData.horizontalAlignment = GridData.FILL;
		spinnerTI.setLayoutData(connectionLabelLData);

	}
	{
		spinnerMECheck= new Button(dialogShell,SWT.CHECK);
		GridData connectionLabelLData = new GridData();
		connectionLabelLData.horizontalAlignment = GridData.FILL;
		spinnerMECheck.setLayoutData(connectionLabelLData);
		spinnerMECheck.setText("Max. Evaluations");
		
	}

	{
		spinnerME=new Spinner(dialogShell, SWT.None);
		GridData connectionLabelLData = new GridData();
		connectionLabelLData.horizontalAlignment = GridData.FILL;
		spinnerME.setLayoutData(connectionLabelLData);

	}
	{
		spinnerXACheck= new Button(dialogShell,SWT.CHECK);
		GridData connectionLabelLData = new GridData();
		connectionLabelLData.horizontalAlignment = GridData.FILL;
		spinnerXACheck.setLayoutData(connectionLabelLData);
		spinnerXACheck.setText("Max. abs. xtol");
		
	}

	{
		spinnerXA=new Spinner(dialogShell, SWT.None);
		GridData connectionLabelLData = new GridData();
		connectionLabelLData.horizontalAlignment = GridData.FILL;
		spinnerXA.setLayoutData(connectionLabelLData);

	}
	{
		spinnerXRCheck= new Button(dialogShell,SWT.CHECK);
		GridData connectionLabelLData = new GridData();
		connectionLabelLData.horizontalAlignment = GridData.FILL;
		spinnerXRCheck.setLayoutData(connectionLabelLData);
		spinnerXRCheck.setText("Max. rel. xtol");
		
	}
	{
		spinnerXR=new Spinner(dialogShell, SWT.None);
		GridData connectionLabelLData = new GridData();
		connectionLabelLData.horizontalAlignment = GridData.FILL;
		spinnerXR.setLayoutData(connectionLabelLData);

	}
	{
		spinnerFACheck= new Button(dialogShell,SWT.CHECK);
		GridData connectionLabelLData = new GridData();
		connectionLabelLData.horizontalAlignment = GridData.FILL;
		spinnerFACheck.setLayoutData(connectionLabelLData);
		spinnerFACheck.setText("Max. abs. ftol");
		
	}
	{
		spinnerFA=new Spinner(dialogShell, SWT.None);
		GridData connectionLabelLData = new GridData();
		connectionLabelLData.horizontalAlignment = GridData.FILL;
		spinnerFA.setLayoutData(connectionLabelLData);

	}
	{
		spinnerFRCheck= new Button(dialogShell,SWT.CHECK);
		GridData connectionLabelLData = new GridData();
		connectionLabelLData.horizontalAlignment = GridData.FILL;
		spinnerFRCheck.setLayoutData(connectionLabelLData);
		spinnerFRCheck.setText("Max. rel. ftol");

		
	}
	{
		spinnerFR=new Spinner(dialogShell, SWT.None);
		GridData connectionLabelLData = new GridData();
		connectionLabelLData.horizontalAlignment = GridData.FILL;
		spinnerFR.setLayoutData(connectionLabelLData);

	}
	
	
	
	
	
	
	{
		okButton = new Button(dialogShell, SWT.PUSH | SWT.CENTER);
		GridData okButtonLData = new GridData();
		okButtonLData.horizontalAlignment = GridData.FILL;
		okButton.setLayoutData(okButtonLData);
		okButton.setText("OK");

	}
	{
		cancelButton = new Button(dialogShell, SWT.PUSH
			| SWT.CENTER);
		GridData cancelButtonLData = new GridData();
		cancelButtonLData.grabExcessHorizontalSpace = true;
		cancelButtonLData.horizontalAlignment = GridData.FILL;
		cancelButton.setLayoutData(cancelButtonLData);
		cancelButton.setText("Cancel");
	}
	
	
}

}
