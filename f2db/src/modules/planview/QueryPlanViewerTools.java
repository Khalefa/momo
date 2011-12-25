package modules.planview;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;


import com.mxgraph.canvas.mxGraphics2DCanvas;
import com.mxgraph.layout.mxCompactTreeLayout;
import com.mxgraph.layout.mxGraphLayout;
import com.mxgraph.layout.hierarchical.mxHierarchicalLayout;
import com.mxgraph.layout.mxEdgeLabelLayout;
import com.mxgraph.shape.mxMarkerRegistry;
import com.mxgraph.util.mxCellRenderer;
import com.mxgraph.util.mxConstants;
import com.mxgraph.util.mxEvent;
import com.mxgraph.util.mxEventObject;
import com.mxgraph.util.mxRectangle;
import com.mxgraph.util.mxUtils;
import com.mxgraph.util.mxEventSource.mxIEventListener;
import com.mxgraph.view.mxGraph;
import com.mxgraph.view.mxGraphSelectionModel;
import com.mxgraph.view.mxStylesheet;
import components.tabs.QueryPlanViewerTab;


import components.widgets.JGraphWidget;
import java.util.Collections;
import modules.modelgraphview.ModelInfo;
import org.apache.derby.impl.sql.compile.ForecastGranularityNode;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableItem;

/**
 * @author Christopher Schildt
 * @date   03.09.2011
 *
 */
public class QueryPlanViewerTools {
	
	static mxStylesheet stylesheet;
	static HashMap<String, Object> style;
	static mxGraph jGraph;
	static mxCompactTreeLayout layout;
	static double mid;
	public static boolean equalsize=true;
	
	public static void initializeStyles(mxGraph jGraph, double mid) {
		PostgresPlanNode.cellwidth=0;
		QueryPlanViewerTools.jGraph=jGraph;
		stylesheet = jGraph.getStylesheet();
		QueryPlanViewerTools.layout = new mxCompactTreeLayout(jGraph);
		QueryPlanViewerTools.mid=mid;
		QueryPlanViewerTools.layout.setHorizontal(false);
		
		QueryPlanViewerTools.style = new HashMap<String, Object>();
		mxGraphics2DCanvas.putShape("test", new Rect2());
		style.put(mxConstants.STYLE_SHAPE, "test");
		style.put(mxConstants.STYLE_AUTOSIZE, true);
		style.put(mxConstants.STYLE_OPACITY, 50);
		style.put(mxConstants.STYLE_ROUNDED, true);
		style.put(mxConstants.STYLE_ENDARROW, mxConstants.ARROW_DIAMOND);
		style.put(mxConstants.STYLE_STARTARROW, mxConstants.ARROW_CLASSIC);
		style.put(mxConstants.STYLE_FOLDABLE, true);
		style.put(mxConstants.STYLE_FONTCOLOR, "#000000");
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
		jGraph.getSelectionModel().addListener(mxEvent.CHANGE, new mxIEventListener() {
			@Override
			public void invoke(final Object sender, mxEventObject arg1) {
				if (sender instanceof mxGraphSelectionModel) {
	                for (final Object cell : ((mxGraphSelectionModel)sender).getCells()) {
	                	PostgresPlanNode node;
          
                                if(((com.mxgraph.model.mxCell)cell).getValue() instanceof ModelInfo)
                                {
                                     new Thread(new Runnable() {

                            public void run() {

                                    Display.getDefault().asyncExec(new Runnable() {

                                        public void run() {
                                    Table modelInfos=QueryPlanViewerTab.modelInfos;
                                    modelInfos.removeAll();
                                    ModelInfo mi=(ModelInfo)((com.mxgraph.model.mxCell)cell).getValue();

                                    Map<String, String> paras = mi.getParameter();
                                    TableItem test = new TableItem(modelInfos, SWT.None);
                                        test.setText(0, "model name");
                                        test.setText(1, paras.get("model name").toLowerCase());
                                    test = new TableItem(modelInfos, SWT.None);
                                        test.setText(0, "model type");
                                        test.setText(1, paras.get("model type").toLowerCase());
                                        if(paras.get("where expr")!=null){
                                     test = new TableItem(modelInfos, SWT.None);
                                        test.setText(0, "where expr");
                                        String temp=paras.get("where expr").replaceAll(" AND", "\nAND");
                                        test.setText(1, temp.toLowerCase());
                                            }
                                       if (paras.get("error") != null) {
                    test = new TableItem(modelInfos, SWT.None);
                    test.setText(0, "error");
                    test.setText(1, paras.get("error").toLowerCase());
                }
                if (paras.get("disag key") != null) {
                    test = new TableItem(modelInfos, SWT.None);
                    test.setText(0, "disag key");
                    test.setText(1, paras.get("disag key"));
                }
                                        List<String> para1= new ArrayList<String>(paras.keySet());
                                        Collections.sort(para1);
                                        for (String key : para1) {
                                        if(key.equals("model name") || key.equals("model type")|| key.equals("where expr")|| key.equals("disag key")|| key.equals("error")) continue;
                                        test = new TableItem(modelInfos, SWT.None);
                                        test.setText(0, key);
                                        test.setText(1, paras.get(key).toLowerCase());
                                    }
                                     for (int i = 0; i < 2; i++) {
                                         modelInfos.getColumn(i).pack();
                                    }
                                        }});}}).start();
                                    continue;
                                }
                                if(((com.mxgraph.model.mxCell) cell).getValue() instanceof PostgresPlanNode)
	                		node= (PostgresPlanNode)((com.mxgraph.model.mxCell)cell).getValue();
	                	else
	                		continue;

	                	QueryPlanViewerTools.jGraph.getModel().beginUpdate();
	                	node.setColl(!node.isColl());
	                	
	                	QueryPlanViewerTools.jGraph.updateCellSize(cell);

	                	for (Object cell2 :QueryPlanViewerTools.jGraph.getChildVertices(QueryPlanViewerTools.jGraph.getDefaultParent())){
	                		QueryPlanViewerTools.jGraph.getCellGeometry(cell2).setX(QueryPlanViewerTools.mid);
		                	QueryPlanViewerTools.jGraph.getCellGeometry(cell2).setY(0);
	                	}
	                	
	                	QueryPlanViewerTools.jGraph.getModel().endUpdate();
                                layout.setResetEdges(true);
                                layout.setMoveTree(true);
	                	QueryPlanViewerTools.jGraph.setSelectionCells((Object[])null);
	                	QueryPlanViewerTools.layout.execute((QueryPlanViewerTools.jGraph.getDefaultParent()));
			}
				}
	    }});
		

	   
	}

	
	public static void createTreeModel(ResultSet rs, mxGraph mxGraph, int i)
	{
		PostgresParser psqlp=new PostgresParser();
		//parse query
		psqlp.parseAndSetInputQueryPlan(rs,mxGraph,i);

	
	}

	 static String gernerateStyle(PostgresPlanNode pln) {
		String type=pln.getType(false);
		if(type.equals("Sort"))
			return "ROUNDED;";
		if(type.equals("Hash Join"))
			return "ROUNDED;";
		if(type.equals("Hash"))
			return "ROUNDED;";
		if(type.equals("SeqScan"))
			return "ROUNDED;";
		if(type.equals("GroupAggregate"))
			return "ROUNDED;";
		if(type.equals("Input"))
			return "shape=none;targetPerimeterSpacing=0.0;perimeterSpacing=0.0;spacing=0.0;spacingTop=0.0;";
                if(type.equals("Output"))
			return "shape=none;targetPerimeterSpacing=0.0;perimeterSpacing=0.0;spacing=0.0;spacingTop=0.0;";
		return "ROUNDED;";
	}



	

	

	public static PostgresPlanNode createNodeFromString(String x) {
		PostgresPlanNode result=new PostgresPlanNode();
		result.setSourceText(x);
		System.out.println(x);
		result.setType(QueryPlanViewerTools.determineType(x));
		result.setCost(QueryPlanViewerTools.determineCost(x));
		result.setRows(QueryPlanViewerTools.determineRows(x));
		result.setWidth(QueryPlanViewerTools.determineWidth(x));
		if(result.getType(false).contains("Scan"))
		{	if(!result.getType(false).contains("Index"))
				result.setSource(QueryPlanViewerTools.determineSource(x));
			result.setQual(QueryPlanViewerTools.determineScanQual(x));
		}
		else if(result.getType(false).contains("Nested Loop"))
		{
			result.setQual(QueryPlanViewerTools.determineNLJoinQual(x));
		}
		else if(result.getType(false).contains("Hash Join"))
		{
			result.setQual(QueryPlanViewerTools.determineHJoinQual(x));
		}
		else if(result.getType(false).contains("Merge Join"))
		{
			result.setQual(QueryPlanViewerTools.determineMJoinQual(x));
		}
		else if(result.getType(false).contains("Group"))
		{
			result.setQual(QueryPlanViewerTools.determineScanQual(x));
		}
		
		else if(result.getType(false).contains("Agg"))
		{
			result.setQual(QueryPlanViewerTools.determineScanQual(x));
		}
		else if(result.getType(false).contains("Result"))
		{
			result.setQual(QueryPlanViewerTools.determineScanQual(x));
		}
		else if(result.getType(false).contains("Sort"))
		{
			result.setQual(QueryPlanViewerTools.determineSortQual(x));
		}
                else if(result.getType(false).contains("Create ForecastModel"))
		{
			result.setOutput(QueryPlanViewerTools.determineOutput(x));
                        determineParameters(result,x);
		}

		else if(result.getType(false).contains("Forecast"))
		{
                        result.setBuild(QueryPlanViewerTools.determineBuild(x));
                        result.setSource(QueryPlanViewerTools.determineInput(x));
                    
                        determineParameters2(result,x);
		}
		return result;
	}


	private static String determineSortQual(String x) {
		int costind= x.indexOf("Sort Key");
		int rowind=x.lastIndexOf(",",x.length()-1);
		String temp=x.substring(costind,rowind);
		String[] temp2=temp.split("[:,]");
		String result="";
		for(String k : temp2)
			result+=k+"\n";
		return result;
	}


	private static String determineMJoinQual(String x) {
		int costind,rowind;
		String result="";
		costind=x.indexOf("Merge Cond");

		rowind=x.indexOf(")",costind);
		result=x.concat(x.substring(costind,rowind));
		costind=x.indexOf("Join Filter",rowind);
		if(costind>0)
		{
			rowind=x.indexOf(")",costind);
			result=result.concat(x.substring(costind,rowind));
			costind=x.indexOf("Filter",rowind);
			if(costind>0)
			{
				rowind=x.indexOf(")",costind);
				result=result.concat(x.substring(costind,rowind));
				costind=x.indexOf("Filter",rowind);
			}
		}
		else
		{
			costind=x.indexOf("Filter",rowind);
			if(costind>0)
			{
				rowind=x.indexOf(")",costind);
				result=result.concat(x.substring(costind,rowind));
				costind=x.indexOf("Filter",rowind);
			}
		}

		return result;
	}


	private static String determineHJoinQual(String x) {
		int costind,rowind;
		String result="";
		costind=x.indexOf("Hash Cond");

		rowind=x.indexOf(")",costind)+1;
		String temp=x.substring(costind,rowind);
		String[] temp2=temp.split("[:,]");
		String result2="";
		for(String k : temp2)
			result2+=k+"\n";
		result=result.concat(result2);
		costind=x.indexOf("Join Filter",rowind);
		if(costind>0)
		{
			rowind=x.indexOf(")",costind);
			result=result.concat(x.substring(costind,rowind));
			costind=x.indexOf("Filter",rowind);
			if(costind>0)
			{
				rowind=x.indexOf(")",costind);
				result=result.concat(x.substring(costind,rowind));
				costind=x.indexOf("Filter",rowind);
			}
		}
		else
		{
			costind=x.indexOf("Filter",rowind);
			if(costind>0)
			{
				rowind=x.indexOf(")",costind);
				result=result.concat(x.substring(costind,rowind));
				costind=x.indexOf("Filter",rowind);
			}
		}

		return result;
	}


	private static String determineNLJoinQual(String x) {
		int costind,rowind;
		String result="";
		costind=x.indexOf("Join Filter");
		if(costind>0)
		{
			rowind=x.indexOf(")",costind);
			result=x.concat(x.substring(costind,rowind));
			costind=x.indexOf("Filter",rowind);
			if(costind>0)
			{
				rowind=x.indexOf(")",costind);
				result=result.concat(x.substring(costind,rowind));
			}
		}
		else
		costind=x.indexOf("Filter");
		if(costind>0)
		{
			rowind=x.indexOf(")",costind)+1;
			result=result.concat(x.substring(costind,rowind));
		}
		return result;
	}


	private static String determineScanQual(String x) {
		int costind,rowind;
		String result="";
		costind=x.indexOf("Index Cond");
		if(costind>0)
		{
			rowind=x.indexOf(")",costind);
			result=result.concat(x.substring(costind,rowind));
		}
		costind=x.indexOf("Filter");
		if(costind>0)
		{
			rowind=x.indexOf(")",costind);
			result=result.concat(x.substring(costind,rowind));
		}
		costind=x.indexOf("RecheckCond");
		if(costind>0)
		{
			rowind=x.indexOf(")",costind);
			result=result.concat(x.substring(costind,rowind));		
		}
		costind=x.indexOf("Tid Cont");
		if(costind>0)
		{
			
			rowind=x.indexOf(")",costind);
			result=result.concat(x.substring(costind,rowind));
		}
		
		return result;
	}


	private static String determineSource(String x) {
		int costind= x.indexOf("on")+3;
		int rowind=x.indexOf(" ",costind);
		return x.substring(costind,rowind);
	}


	private static String determineWidth(String x) {
		int costind= x.indexOf("width")+6;
		int rowind=x.indexOf(")",costind);
		return x.substring(costind,rowind);
	}


	private static String determineRows(String x) {
		int costind= x.indexOf("rows")+5;
		int rowind=x.indexOf("width",costind);
		return x.substring(costind,rowind);
	}


	private static String determineCost(String x) {
		int costind= x.indexOf("cost")+5;
		int rowind=x.indexOf("rows",costind);
		return x.substring(costind,rowind);
	}


	private static String determineType(String x) {
		if (x.contains("Result"))
			return "Result";
		if (x.contains("Append"))
			return "Append";
		if (x.contains("Recursive Union"))
			return "Recursive Union";
		if (x.contains("BitmapAnd"))
			return "BitmapAnd";
		if (x.contains("BitmapOr"))
			return "BitmapOr";
		if (x.contains("Seq Scan"))
			return "SeqScan";
		if (x.contains("Index Scan"))
			return "Index Scan";
		if (x.contains("Bitmap Index Scan"))
			return "Bitmap Index Scan";
		if (x.contains("Bitmap Heap Scan"))
			return "Bitmap Heap Scan";
		if (x.contains("Tid Scan"))
			return "Tid Scan";
		if (x.contains("Subquery Scan"))
			return "Subquery Scan";
		if (x.contains("Function Scan"))
			return "Function Scan";
		if (x.contains("Values Scan"))
			return "Values Scan";
		if (x.contains("Cte Scan"))
			return "Cte Scan";
		if (x.contains("WorkTable Scan"))
			return "WorkTable Scan";
		if (x.contains("Nested Loop Left Join"))
			return "Nested Loop Left Join";
		if (x.contains("Nested Loop Right Join"))
			return "Nested Loop Right Join";
		if (x.contains("Nested Loop Semi Join"))
			return "Nested Loop Semi Join";
		if (x.contains("Nested Loop Anti Join"))
			return "Nested Loop Anti Join";
		if (x.contains("Nested Loop ??? Join"))
			return "Nested Loop ??? Join";
		if (x.contains("Nested Loop Full Join"))
			return "Nested Loop Full Join";
		if (x.contains("Nested Loop"))
			return "Nested Loop";
		if (x.contains("Merge Left Join"))
			return "Merge Left Join";
		if (x.contains("Merge Right Join"))
			return "Merge Right Join";
		if (x.contains("Merge Full Join"))
			return "Merge Full Join";
		if (x.contains("Merge Semi Join"))
			return "Merge Semi Join";
		if (x.contains("Merge Anti Join"))
			return "Merge Anti Join";
		if (x.contains("Merge ??? Join"))
			return "Merge ??? Join";
		if (x.contains("Merge Join"))
			return "Merge Join";
		if (x.contains("Hash Left Join"))
			return "Hash Left Join";
		if (x.contains("Hash Full Join"))
			return "Hash Full Join";
		if (x.contains("Hash Right Join"))
			return "Hash Right Join";
		if (x.contains("Hash Semi Join"))
			return "Hash Semi Join";
		if (x.contains("Hash Anti Join"))
			return "Hash Anti Join";
		if (x.contains("Hash ??? Join"))
			return "Hash ??? Join";
		if (x.contains("Hash Join"))
			return "Hash Join";
		if (x.contains("Materialize"))
			return "Materialize";
		if (x.contains("Sort"))
			return "Sort";
		if (x.contains("GroupAggregate"))
			return "GroupAggregate";
		if (x.contains("Group"))
			return "Group";
		if (x.contains("HashAggregate"))
			return "HashAggregate";
		if (x.contains("Aggregate ???"))
			return "Aggregate ???";
		if (x.contains("Aggregate"))
			return "Aggregate";
		if (x.contains("WindowAgg"))
			return "WindowAgg";
		if (x.contains("Unique"))
			return "Unique";
		if (x.contains("HashSetOp Intersect All"))
			return "HashSetOp Intersect All";
		if (x.contains("HashSetOp Intersect"))
			return "HashSetOp Intersect";
		if (x.contains("HashSetOp Except All"))
			return "HashSetOp Except All";
		if (x.contains("HashSetOp Except"))
			return "HashSetOp Except";
		if (x.contains("HashSetOp ???"))
			return "HashSetOp ???";
		if (x.contains("SetOp Intersect All"))
			return "SetOp Intersect All";
		if (x.contains("SetOp Intersect"))
			return "SetOp Intersect";
		if (x.contains("SetOp Except All"))
			return "SetOp Except All";
		if (x.contains("SetOp Except"))
			return "SetOp Except";
		if (x.contains("SetOp ???"))
			return "SetOp ???";
		if (x.contains("Limit"))
			return "Limit";
		if (x.contains("Hash"))
			return "Hash";
		if (x.contains("Decompose Additiv"))
			return "Decompose Additiv";
		if (x.contains("Create ForecastModel"))
			return "Create ForecastModel";
                if (x.contains("Forecast"))
			return "Forecast";
		if (x.contains("ScanTarget"))
			return "ScanTarget";
		return "UNKNOWN";
	}


	public static void doLayout() {
		Object[] a=QueryPlanViewerTools.jGraph.getChildVertices(QueryPlanViewerTools.jGraph.getDefaultParent());
		
		List<com.mxgraph.model.mxCell> inputLiust=QueryPlanViewerTools.rewriteGraph(a);
		a=QueryPlanViewerTools.jGraph.getChildVertices(QueryPlanViewerTools.jGraph.getDefaultParent());
		QueryPlanViewerTools.layout.setLevelDistance(50);
		
		for(com.mxgraph.model.mxCell ao: inputLiust)
		{       if(!PostgresPlanNode.collapsedDefault && PostgresPlanNode.cellwidth==0)
                            QueryPlanViewerTools.jGraph.updateCellSize(((com.mxgraph.model.mxCell)(ao)));
                        else
			QueryPlanViewerTools.jGraph.getCellGeometry(((com.mxgraph.model.mxCell)(ao))).setY(QueryPlanViewerTools.jGraph.getCellGeometry(((com.mxgraph.model.mxCell)(ao))).getY());
                        
                }
                
		double maxw=0;
		double w;
		for(Object ao: a)
		{
			w=QueryPlanViewerTools.jGraph.getCellGeometry(((com.mxgraph.model.mxCell)(ao))).getWidth();
			if(maxw<w)
				maxw=w;
                        PostgresPlanNode node=((PostgresPlanNode) ((com.mxgraph.model.mxCell)(ao)).getValue());
                        if(node.forecastModels!=null && node.forecastModels.keySet().size()>5);//count models change leveldistance
                            QueryPlanViewerTools.layout.setLevelDistance(100);
		}
                if(equalsize)
                    PostgresPlanNode.cellwidth=(int) maxw;

		for(Object ao: a)
		{
			QueryPlanViewerTools.jGraph.updateCellSize(((com.mxgraph.model.mxCell)(ao)));
		}
		
                
                QueryPlanViewerTools.layout.setNodeDistance(50);
                QueryPlanViewerTools.layout.execute(jGraph.getDefaultParent());
                        jGraph.getView().setScale(jGraph.getView().getScale()/1.5);
                
	}


	private static List<com.mxgraph.model.mxCell> rewriteGraph(Object[] a) {
		QueryPlanViewerTools.jGraph.getModel().beginUpdate();
		List<com.mxgraph.model.mxCell> inputLiust=new ArrayList<com.mxgraph.model.mxCell>();
		for(Object ao: a)
		{
			if(((com.mxgraph.model.mxCell)ao).getValue() instanceof PostgresPlanNode)
			{
				PostgresPlanNode node=(PostgresPlanNode)((com.mxgraph.model.mxCell)ao).getValue();
                                
				if(node.getSource()!=null) // got anything with input
				{
					PostgresPlanNode newNode=new PostgresPlanNode();
					newNode.setType("Input");
					newNode.setLabel(node.getSource());
					Object newGraphNode=QueryPlanViewerTools.jGraph.insertVertex(QueryPlanViewerTools.jGraph.getDefaultParent(), null, newNode, 0, 0,100 , 30, QueryPlanViewerTools.gernerateStyle(newNode));
					inputLiust.add(((com.mxgraph.model.mxCell)newGraphNode));
					QueryPlanViewerTools.jGraph.insertEdge(QueryPlanViewerTools.jGraph.getDefaultParent(),null,"",ao,newGraphNode, "startArrow=classic;strokeWidth=2;endArrow=none;");
				}
                                if(node.getOutput()!=null) // got anything with input
				{
					PostgresPlanNode newNode=new PostgresPlanNode();
					newNode.setType("Output");
					newNode.setLabel(node.getOutput());
					Object newGraphNode=QueryPlanViewerTools.jGraph.insertVertex(null, null, newNode, 0, 0,100 , 30, QueryPlanViewerTools.gernerateStyle(newNode));
					inputLiust.add(((com.mxgraph.model.mxCell)newGraphNode));
					QueryPlanViewerTools.jGraph.insertEdge(QueryPlanViewerTools.jGraph.getDefaultParent(),null,"",ao,newGraphNode, "startArrow=classic;endArrow=classic;strokeWidth=2;");
				}

			}
			else
			{
				continue;
			}
		}
		QueryPlanViewerTools.jGraph.getModel().endUpdate();
		return inputLiust;
	}

    private static String determineOutput(String x) {
        	int costind= x.indexOf("Output")+8;
	int rowind=x.indexOf(",",costind);
	String result=x.substring(costind,rowind);


                if(result.contains("OFF"))
                    return null;
                else
                    return result;
    }
    private static String determineInput(String x) {
        	int costind= x.indexOf("Input")+7;
	int rowind=x.indexOf(",",costind);
	String result=x.substring(costind,rowind);


                if(result.contains("OFF"))
                    return null;
                else
                    return result;
    }

    private static void determineParameters(PostgresPlanNode result, String x) {
        int costind= x.indexOf("Parameters")+13;
		int rowind=x.indexOf(",",costind)-2;

		String paraList=x.substring(costind,rowind);
                String[] paras=paraList.split(";");
               for(String a: paras)
                {
                    //a=  TrainingData=select s_time, sum(s_amount) amt from sales where s_product='asd' group by s_time order by s_time;
                    if(a.equals("") || a.equals(" ")) continue;
                    costind=a.indexOf("=");
                    String key=a.substring(0, costind).trim();
                    String value=a.substring(costind+1).trim();
                    result.getParameter().put(a.substring(0, costind).trim(), a.substring(costind+1).trim());
                }
        return;
	
    }

    private static boolean determineBuild(String x) {
       	int costind= x.indexOf("buildModel")+2;
	int rowind=x.indexOf(",",costind);
	String result=x.substring(costind,rowind);


                if(result.contains("0"))
                    return false;
                else
                    return true;
    }

    private static void determineParameters2(PostgresPlanNode result, String x) {
                int costind= x.indexOf("modelCount")+12;
		int rowind=x.indexOf(",",costind);
                String countString=x.substring(costind,rowind);
                int mc=Integer.parseInt(countString);
                result.setCount(mc);
                for(;mc>0;mc--)
                {
                  costind= x.indexOf("parameters"+mc)+13+String.valueOf(mc).length();
		 rowind=x.indexOf(",",costind)-2;

		String paraList=x.substring(costind,rowind);
                String[] paras=paraList.split(";");
                for(String a: paras)
                {
                    if(a.equals("")) continue;
                    costind=a.indexOf("=");
                    String key=a.substring(0, costind).trim();
                    String value=a.substring(costind+1).trim();
 
                    result.addfMPar(mc,a.substring(0, costind).trim(), a.substring(costind+1).trim());
                }

                }
        return;
    }





	
}
