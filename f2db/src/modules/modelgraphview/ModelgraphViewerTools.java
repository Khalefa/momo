package modules.modelgraphview;

import java.sql.ResultSet;
import java.util.HashMap;

import modules.planview.PostgresParser;
import modules.planview.PostgresPlanNode;

import modules.planview.Rect2;

import com.mxgraph.canvas.mxGraphics2DCanvas;
import com.mxgraph.layout.*;
import com.mxgraph.layout.orthogonal.mxOrthogonalLayout;

import com.mxgraph.util.mxConstants;
import com.mxgraph.util.mxEvent;
import com.mxgraph.util.mxEventObject;
import com.mxgraph.util.mxEventSource.mxIEventListener;
import com.mxgraph.view.mxGraph;
import com.mxgraph.view.mxGraphSelectionModel;
import com.mxgraph.view.mxStylesheet;
import com.mxgraph.view.mxGraph.mxICellVisitor;
import java.util.ArrayList;
import java.util.Map;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeItem;

/*
 * @author Christopher Schildt
 * @date   03.09.2011
 *
 */
public class ModelgraphViewerTools {
	static mxStylesheet stylesheet;
	static HashMap<String, Object> style;
        static HashMap<String,ModelInfo> modelMap=new HashMap<String,ModelInfo>();
        public static ArrayList<String> attrNames=new ArrayList<String>();
        static ArrayList<Boolean> attrTypes=new ArrayList<Boolean>();
        public static String trainingdata;
	static mxGraph jGraph;
        static Tree ts;
	static mxCompactTreeLayout layout;
	static double mid;
	public static boolean equalsize=true;
       
    private static HashMap<String, Object> style2;


        public static void addAtt(String attr,Boolean atttype)
        {
            attrNames.add(attr);
            attrTypes.add(atttype);
        }
        public static String getAtt(int attr)
        {
            return attrNames.get(attrNames.size()-attr-1);
        }
        public static Boolean getAtttype(int attr)
        {
            return attrTypes.get(attrNames.size()-attr-1);
        }
	public static void initializeStyles(mxGraph jGraph,Tree ts, double mid) {
		PostgresModelGraphNode.cellwidth=0;
		ModelgraphViewerTools.jGraph=jGraph;
                ModelgraphViewerTools.ts=ts;
		stylesheet = jGraph.getStylesheet();
		
		ModelgraphViewerTools.layout = new mxCompactTreeLayout(jGraph);
		ModelgraphViewerTools.layout.setLevelDistance(80);
		
		ModelgraphViewerTools.mid=mid;
		ModelgraphViewerTools.style = new HashMap<String, Object>();
		mxGraphics2DCanvas.putShape("test", new Rect2());
		style.put(mxConstants.STYLE_SHAPE, "test");
		style.put(mxConstants.STYLE_AUTOSIZE, true);
		style.put(mxConstants.STYLE_OPACITY, 50);
		style.put(mxConstants.STYLE_ROUNDED, true);
		style.put(mxConstants.STYLE_FOLDABLE, true);
		style.put(mxConstants.STYLE_FONTCOLOR, "#000000");
                style.put(mxConstants.STYLE_STROKEWIDTH,2);
		style.put(mxConstants.STYLE_EDGE,mxConstants.EDGESTYLE_TOPTOBOTTOM);  //where to set Edgestyke
		mxConstants.RECTANGLE_ROUNDING_FACTOR=0.9;
                stylesheet.putCellStyle("ROUNDED", style);

	
		
jGraph.setHtmlLabels(true);
		jGraph.setBorder(0);
		jGraph.setCellsEditable(false);
		jGraph.setCellsResizable(false);
		jGraph.setEdgeLabelsMovable(false);
		jGraph.setCellsCloneable(false);
		jGraph.setCellsDisconnectable(false);
		jGraph.setSplitEnabled(false);
		jGraph.setCellsDeletable(false);
                jGraph.setCellsBendable(false);
                jGraph.setAllowDanglingEdges(false);

                

            jGraph.getSelectionModel().addListener(mxEvent.CHANGE, new mxIEventListener() {

                @Override
                public void invoke(final Object sender, mxEventObject arg1) {
                    if (sender instanceof mxGraphSelectionModel) {

                        new Thread(new Runnable() {

                            public void run() {
                               
                                    Display.getDefault().asyncExec(new Runnable() {

                                        public void run() {
                                            for (Object cell : ((mxGraphSelectionModel) sender).getCells()) {

                                                if(!((com.mxgraph.model.mxCell) cell).isVertex()) return;
                                                if(!((((com.mxgraph.model.mxCell) cell).getValue()) instanceof PostgresModelGraphNode)) return;
                                                TreeItem ti = ((PostgresModelGraphNode) ((com.mxgraph.model.mxCell) cell).getValue()).getTreeitem();
                                                   if(ti==null)return;
                                                ModelgraphViewerTools.ts.setSelection(((PostgresModelGraphNode) ((com.mxgraph.model.mxCell) cell).getValue()).getTreeitem());
                                                int i = 3;


                                            }
                                        }
                                    });
                                
                            }
                        }).start();
                        

                    }
                }
            });

		

	   
	}

	
	public static void createTreeModel(ResultSet rs, mxGraph mxGraph, int i,Tree ts)
	{
		PostgresParser psqlp=new PostgresParser();
		//parse query
		psqlp.parseAndSetInputModelGraphIndex(rs,mxGraph,i,ts);

	
	}

	 public static String gernerateStyle(PostgresModelGraphNode pln) {


		return "ROUNDED;";
	}



	

	

	public static PostgresModelGraphNode createNodeFromString(String x) {
		System.out.println(x);
		PostgresModelGraphNode result=new PostgresModelGraphNode();
		int startidx=x.indexOf("label");
		if(startidx<0)
		{
			result.setSource("root");
			return result;
		}
		startidx+=7;
		int endidx=x.indexOf("}", startidx);
                String temp=x.substring(startidx, endidx);
                if(temp.contains("Agg"))
                    result.setSource("[Total]");
                else
                    result.setSource(temp.substring(temp.indexOf("|")+1));

//parse MginId
               startidx = x.indexOf("mginid") + 8;
                
                if(startidx-8>0)
                {
                    
                     int endix=x.indexOf("}", startidx);
                    result.setMginId(x.substring(startidx,endix));
                }

		ModelgraphViewerTools.createModelInfo(x,result);
		return result;
	}

        public static void createModelInfo(String x, PostgresModelGraphNode pln)
    {
            int startidx=x.indexOf("modelCount")+12;
            
            if(startidx<12)
                return;
            pln.isLeaf=true;
            int endix=x.indexOf("}", startidx);
            String modelCountS=x.substring(startidx,endix);
            int modelCount=Integer.parseInt(modelCountS);
            pln.setModelCount(modelCount);
            if(modelCount>0)
                pln.setHasModels(true);
            for(int i=0;i<modelCount;i++)
            {
                ModelInfo newModel=new ModelInfo();
                startidx=x.indexOf("t"+i)+4+String.valueOf(modelCount).length();
                endix=x.indexOf("}", startidx);
                newModel.setTime(x.substring(startidx, endix));
                startidx=x.indexOf("name"+i,startidx)+7+String.valueOf(modelCount).length();
                endix=x.indexOf("}", startidx);
                newModel.setName(x.substring(startidx,endix));
                pln.getModels().put(newModel.getName(), newModel);
                //read ParameterList
                startidx=x.indexOf("parameter"+i,startidx)+12+String.valueOf(modelCount).length();
                endix=x.indexOf("}", startidx);
                String paraList=x.substring(startidx, endix);
                String[] paras=paraList.split(";");
                for(String a: paras)
                {
                    //a=  TrainingData=select s_time, sum(s_amount) amt from sales where s_product='asd' group by s_time order by s_time;
                    if(a.equals("") || a.equals(" ")) continue;
                    startidx=a.indexOf("=");

                    newModel.getParameter().put(a.substring(0, startidx).trim(), a.substring(startidx+1).trim());
                }
                startidx=x.indexOf("m"+i,startidx)+4+String.valueOf(modelCount).length();
                endix=x.indexOf("}", startidx);
                String m2=x.substring(startidx, endix);
                if(m2.equals("sum")||m2.equals("avg")||m2.equals("max") || m2.equals("min"))
                {
                    String trstmt=newModel.getParameter().get("training data");
                    String[] columns=ModelgraphViewerTools.getColums(trstmt);
                    if(columns[0].toLowerCase().equals(newModel.getTime().toLowerCase()))
                        newModel.setMeassuere(columns[1]);
                    else
                        newModel.setMeassuere(columns[0]);
                }
                else
                    newModel.setMeassuere(x.substring(startidx, endix));
   


            }

            return;
        }


    public static String[] getColums(String stmt)
    {
        ArrayList<String> result=new ArrayList<String>();
        String textValue = stmt.toUpperCase();
        try{
        textValue = textValue.substring(7);
        }
        catch(Exception e)
        {
            return null; //deleted to fast ;-)
        }
        String[] pos = textValue.split("[,]");
        boolean breaker = false;
        for (String s : pos) {
            if (breaker) {
                return null;
            }
            if (s.contains("FROM")) {
                int fromInd = s.indexOf("FROM");
                s = s.substring(0, fromInd);
                breaker = true;
            }
            String s2 = s.toUpperCase().trim();
            if (s2.contains(" ") || s2.contains("AS"))//Alias
            {
                String[] pos2 = s2.split("[\\s]");
                result.add(pos2[pos2.length - 1].trim());
                continue;
            }
            if (s.trim().length() < 1) {
                continue;
            }
            result.add(s.trim());

        }
        return result.toArray(new String[2]);


    }


	public static void doLayout() {
		Object[] a=ModelgraphViewerTools.jGraph.getChildVertices(ModelgraphViewerTools.jGraph.getDefaultParent());
		jGraph.setCellsMovable(true);
		double maxw=0;
                double maxh=0;
		double w;
                double h;
		for(Object ao: a)
		{
			w=ModelgraphViewerTools.jGraph.getCellGeometry(((com.mxgraph.model.mxCell)(ao))).getWidth();
                        h=ModelgraphViewerTools.jGraph.getCellGeometry(((com.mxgraph.model.mxCell)(ao))).getHeight();
			if(maxw<w)
				maxw=w;
                        if(maxh<h)
				maxh=h;
		}
                if(equalsize)
                    PostgresModelGraphNode.cellwidth=(int) maxw;
                PostgresModelGraphNode.cellheight=(int) maxh;

		for(Object ao: a)
		{
			ModelgraphViewerTools.jGraph.updateCellSize(((com.mxgraph.model.mxCell)(ao)));
                        PostgresModelGraphNode node= (PostgresModelGraphNode)((com.mxgraph.model.mxCell)ao).getValue();
                        if(node.isLeaf && node.hasModels)
                            ((com.mxgraph.model.mxCell)ao).getGeometry().setHeight(PostgresModelGraphNode.cellheight);

		}
		
		ModelgraphViewerTools.layout.setHorizontal(false);


		ModelgraphViewerTools.layout.execute(jGraph.getDefaultParent());
                        jGraph.getView().setScale(jGraph.getView().getScale()/2);

	}


	
}
