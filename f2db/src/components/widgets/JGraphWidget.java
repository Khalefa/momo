package components.widgets;

import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.Frame;
import java.awt.Rectangle;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JScrollPane;

import modules.generic.DemoEvents;
import modules.generic.GenericModel;
import modules.generic.GenericModelChangeEvent;
import modules.generic.GenericModelChangeListener;
import modules.misc.Constants;
import modules.misc.ResourceRegistry;
import modules.misc.StringUtils;

import org.eclipse.swt.SWT;
import org.eclipse.swt.awt.SWT_AWT;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Scale;


import com.mxgraph.swing.mxGraphComponent;
import com.mxgraph.view.mxGraph;
/**
 * This widget wraps a Swing JGraph component in an SWT Composite.
 * It also generates a zoom slider box and displays the current zoom level in percent. 
 * @author Felix Beyer
 * @date   13.01.2007
 *
 */
public class JGraphWidget extends Composite
                          implements GenericModel{
    
    // JGraph stuff
    /** the displayed graph */
    public mxGraph graph=new mxGraph();
    public mxGraphComponent mxComp;
    /** the Tree Layout algorithm */

    
    // AWT components
    /** the container for the srcoll panel component */
    private Frame frame;
    
    /** the scroll panel as a container for the JGraph component */
    private JScrollPane panel;
    
    // SWT components
    // the zoom slider box on the right side of the widget
    private Scale scale;
    private Button zoomInButton;
    private Button zoomOutButton;
    private Button zoomFitButton;
    private Label  zoomPercent;
    
    // GenericModel stuff
    /** an array for the different listeners that are listening to a SelectNode Event */
    private List<GenericModelChangeListener> listeners; 

    
    /** the fit scale size of the graph, calculated when set*/
    private double fitSize;
    
    //private double scalefactor = 0.25; // the scale factor which is used for scaling
    
    /** the scale factor array providing the common scale factors */
    private double[] scaleValues = new double[]{
                    0.01d,
                    0.02d,
                    0.05d,
                    0.1d,
                    0.25d,
                    0.5d,
                    0.75d,
                    1.0d,
                    1.2d,
                    1.5d,
                    2.0d,
                    3.0d,
                    4.0d,
                    5.0d,
                    6.0d}; 

    /** the current set scale factor index */
    private int currentScaleFactorIndex = scaleValues.length/2;
    
    /** the max size of the slider */
    private int maxsize = 150;
    
    /** the increment of the slider */
    private int increment = 10;

    /** maxsize/increment = number of steps this slider supports */
    private int steps = maxsize/increment;
    
    /** the constructor */
    public JGraphWidget(Composite parent, int style) {
        super(parent, style);
 
        
        // init the list of listeners 
        listeners = new ArrayList<GenericModelChangeListener>();
        
        // init gui
        initComponents();
        
        // init listeners
        initListeners();

    }

    // ******************************************************
    // member methods
    
    /** initializes and rewires all components */
    private void initComponents(){
        GridLayout gridLayout = new GridLayout();
        gridLayout.numColumns = 2; 
        gridLayout.marginWidth = 0;
        gridLayout.horizontalSpacing = 0;
        gridLayout.verticalSpacing = 0;
        
        setLayout(gridLayout);
        
        Composite frameComp = new Composite(this,SWT.EMBEDDED);
        frameComp.setLayout(new FillLayout());
        GridData frameCompData = new GridData();
        frameCompData.horizontalAlignment = GridData.FILL;
        frameCompData.grabExcessVerticalSpace = true;
        frameCompData.verticalAlignment = GridData.FILL;
        frameCompData.grabExcessHorizontalSpace = true;
        frameComp.setLayoutData(frameCompData);
        
        frame = SWT_AWT.new_Frame(frameComp);
        panel = new JScrollPane();
      
        mxGraphComponent graphComponent = new mxGraphComponent(graph);
        mxComp=graphComponent;
        graphComponent.setDragEnabled(false);
        panel.setViewportView(graphComponent);

        frame.add(panel);
        Composite scaleComp = new Composite(this, SWT.NONE);
        GridLayout scaleCompLayout = new GridLayout();
        scaleComp.setLayout(scaleCompLayout);
        GridData compData = new GridData();
        compData.horizontalAlignment = GridData.END;
        compData.grabExcessVerticalSpace = true;
        compData.verticalAlignment = GridData.FILL;
        scaleComp.setLayoutData(compData);

        {
            zoomFitButton = new Button(scaleComp,SWT.PUSH);
            zoomFitButton.setText("Fit");
            zoomFitButton.setToolTipText(Constants.zoomFit_tooltip);
            GridData zoomFitButtonData = new GridData();
            zoomFitButtonData.horizontalAlignment = GridData.FILL;
            //zoomInButtonData.grabExcessVerticalSpace = true;
            //zoomInButtonData.verticalAlignment = GridData.FILL;
            zoomFitButton.setLayoutData(zoomFitButtonData);
            
        }

        {
            zoomInButton = new Button(scaleComp,SWT.PUSH);
            zoomInButton.setImage(ResourceRegistry.getInstance().getImage("zoomin"));
            zoomInButton.setToolTipText(Constants.zoomIn_tooltip);
            //zoomInButton.setText("+");
            GridData zoomInButtonData = new GridData();
            zoomInButtonData.horizontalAlignment = GridData.FILL;
            //zoomInButtonData.grabExcessVerticalSpace = true;
            //zoomInButtonData.verticalAlignment = GridData.FILL;
            zoomInButton.setLayoutData(zoomInButtonData);
            
        }
        
        {
            scale = new Scale(scaleComp,SWT.VERTICAL);
            GridData scaleData = new GridData();
            //scaleData.grabExcessHorizontalSpace = true;
            //scaleData.horizontalAlignment = GridData.FILL;
            scaleData.grabExcessVerticalSpace = true;
            scaleData.verticalAlignment = GridData.FILL;
            scale.setLayoutData(scaleData);
            scale.setMinimum(increment);
            scale.setIncrement(increment);
            scale.setMaximum(maxsize);
            
        }
        {
            zoomOutButton = new Button(scaleComp,SWT.PUSH);
            zoomOutButton.setImage(ResourceRegistry.getInstance().getImage("zoomout"));
            zoomOutButton.setToolTipText(Constants.zoomOut_tooltip);
            //zoomOutButton.setText("-");
            GridData zoomOutButtonData = new GridData();
            zoomOutButtonData.horizontalAlignment = GridData.FILL;
            //zoomInButtonData.grabExcessVerticalSpace = true;
            //zoomInButtonData.verticalAlignment = GridData.FILL;
            zoomOutButton.setLayoutData(zoomOutButtonData);
            
        }
        {
            zoomPercent  = new Label(scaleComp,SWT.NONE);
            zoomPercent.setText("100 %");
            GridData zoomPercentData = new GridData();
            zoomPercentData.horizontalAlignment = GridData.FILL;
            //zoomInButtonData.grabExcessVerticalSpace = true;
            //zoomInButtonData.verticalAlignment = GridData.FILL;
            zoomPercent.setLayoutData(zoomPercentData);
        }
    }
    
    public void refresh()
    {
    	graph=new mxGraph();
    	mxGraphComponent graphComponent = new mxGraphComponent(graph);
        graphComponent.setDragEnabled(false);
        panel.setViewportView(graphComponent);
        frame.add(panel);
    }
    
    /** initializes the listeners for this widget */
    private void initListeners(){
        // add focus listener to initiate repaint of frame after this
        // composite gained focus
        addListener(SWT.FocusIn, new Listener(){
            public void handleEvent(Event event) {
                EventQueue.invokeLater(new Runnable() {
                     public void run() {
                         frame.repaint();
                     }
                 });
            }});

        // add a paint listener to delegate repainting of graph after
        // the composite repainted itself
        /*
        addListener(SWT.Paint, new Listener(){
            public void handleEvent(Event event) {
                if (graph != null) {
                }
            }});
        */
        
        // ***********************************************************
        // The listeners for the Zoom Box on the right side of the tab

        // fit the graph when the fit button is pressed
        zoomFitButton.addListener(SWT.Selection, new Listener(){
            public void handleEvent(Event event) {
                if(graph!=null){
                    calculateFitSize();
                }
                updatePercent();
        }});
        
        // zoom in when the user presses the zoom In Button
        zoomInButton.addListener(SWT.Selection, new Listener(){
            public void handleEvent(Event event) {
                if(graph!=null){
                    if(currentScaleFactorIndex<scaleValues.length-1){
                        currentScaleFactorIndex++;
                        graph.getView().setScale(graph.getView().getScale()*1.1);
                        scale.setSelection((int)graph.getView().getScale());
                        updatePercent();
                    }
                }
                //System.out.println(graph.getScale());
        }});

        // zoom out when the user presses the zoom Out Button
        zoomOutButton.addListener(SWT.Selection, new Listener(){
            public void handleEvent(Event event) {
                if(graph!=null){
                    if(currentScaleFactorIndex>0){
                        currentScaleFactorIndex--;
                        graph.getView().setScale(graph.getView().getScale()*0.9);
                        scale.setSelection((int)graph.getView().getScale());
                        updatePercent();
                    }
                }
                //System.out.println(graph.getScale());
        }});
        
        // set scale and zoom in/out depending on the selection of the slider
        scale.addListener(SWT.Selection, new Listener(){
            public void handleEvent(Event event) {
                if(graph!=null){
                    int index = steps-scale.getSelection()/increment;
                    if(index>=steps){
                        index--;
                    }
                    currentScaleFactorIndex = index;
                    double value = scaleValues[index];
                    graph.getView().setScale(value);
                    updatePercent();
                    //System.out.println("Graph prefSize: "+graph.getPreferredSize());
                }
                //System.out.println(graph.getScale());
        }});
    }
    
    // **********************************************************
    // public interface
    
    /** 
     * sets a JGraph to be displayed by this widget 
     * @param jgraph the JGraph to display
     * */
    public void setJGraph(mxGraph jgraph){
        // remove the old graph from the panel, if current graph not null
        if(graph!=null)panel.removeAll();
        if(jgraph!=null){
            // set the new graph
        	mxGraphComponent graphComponent = new mxGraphComponent(graph);
            panel.setViewportView(graphComponent);
            graphComponent.setDragEnabled(false);
            // save graph reference
            graph = jgraph;
            
            // run layout algorithm
            if (graph != null) {
           //     layout.run(graph,DefaultGraphModel.getAll(graph.getModel()),null);
            }
            
            // add graph selection listener to the new graph 
            // to report the selection of a single node of the graph to recognize when to display details of a node
//            graph.getSelectionModel().addGraphSelectionListener(
//              new GraphSelectionListener(){
//                public void valueChanged(GraphSelectionEvent e) {
//                    // check to see if selection is not empty and of size 1
//                    if((graph.getSelectionCount()==1) && (!graph.isSelectionEmpty())){
//                        // get selected cell
//                        Object o = graph.getSelectionCell();
//                        // if not null
//                        if(o!=null){
//                            // cast it to a defaultgraphcell
//                            if(o instanceof DefaultGraphCell){
//                                DefaultGraphCell cell = (DefaultGraphCell)o;
//                                // check to see if it`s a vertex 
//                                if (DefaultGraphModel.isVertex(graph.getModel(),cell)){
//                                    // get userobject of vertex 
//                                    // (must be a TreeNode) and tell it the treemodel
//                                    
//                                    setCurrentSelectedNode((Node)cell.getUserObject());
//                                }
//                            }
//                        }
//                    }
//                    
//                }});
//            
            // calculate the fitting size
            //calculateFitSize();
            
            // set scale to the maximum, because is fully visible
            scale.setSelection(80);
            
            // calculate scale values
            //calculateScaleValues(fitSize);
            
            // use the bigger one of the two max sizes to scale the graph
            // see sysout to show
            //System.out.println("Size of all cells: "+rect+ ";\nSize of panel widget: "+gRect);
        } else {
            // do nothing
            panel.setViewportView(null);
            graph = null;
        }
        
        // get selected cell after jgraph was setup 
        // (assume that at least one cell is selected
        if(graph!=null){
            Object root = graph.getSelectionCell();
        }
    }
    
    /** calculate the scale factor so that the whole graph fits in its container */
    private void calculateFitSize(){
        // zoom out to see the whole graph
//        Object[] cells = graph.getView().getGraphLayoutCache().getCells(false,true,true,true);
//        // the bounding rectangle of the graph
//        Rectangle2D rect = graph.getCellBounds(cells);
//        // the bounding client rectangle of the panel
//        Rectangle gRect = graph.getParent().getBounds();
//
//        double height = rect.getHeight();
//        double width  = rect.getWidth();
//        double width2 = gRect.getWidth();
//        double height2 = gRect.getHeight();
        
        // check to see if panel is still bigger than the graph
//        // then use graph size as basis
//        if(width2>width && height2>height){
//            if(width>height){
//                // use width for scaling
//                fitSize = width2/width;
//            } else {
//                // use height for scaling
//                fitSize = height2/height;
//            }
//        } else {
//        // otherwise use panel size as basis
//            if(width2>height2){
//                // use width for scaling
//                fitSize = width2/width;
//            } else {
//                // use height for scaling
//                fitSize = height2/height;
//            }
//        }
        // scale the graph to the calculated fit size
        graph.getView().setScale(fitSize);
        
        // try to fix the bug that after scaling no scroll bars appear
        // manually set the size of the panel
        //graph.setSize((int)(graph.getX()*fitSize),(int)(graph.getY()*fitSize));
        
        // manually set the graph after scaling to see if scroll bars appear
        //panel.setViewportView(graph);
        
    }
    
    /** returns the displayed JGraph object */
    public mxGraph getJGraph(){
        return graph;
    }
    
  
    /** updates the percent label */ 
    private void updatePercent(){
        String percent = StringUtils.formatDouble(graph.getView().getScale()*100,1) 
                         + " %";
        zoomPercent.setText(percent);
    }
    
    // **********************************************************************
    // GenericModel Implementation

    public void addModelChangeListener(GenericModelChangeListener listener) {
        listeners.add(listener);
    }

    public void removeModelChangeListener(GenericModelChangeListener listener) {
        listeners.remove(listener);
    }

    public void fireModelChangeEvent(GenericModelChangeEvent event) {
        
        for(GenericModelChangeListener listener:listeners){
            listener.modelChanged(event);
        }
    }
    
    
}
