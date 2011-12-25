package modules.planview;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import modules.misc.ResourceRegistry;
import modules.modelgraphview.ModelgraphViewerTools;
import modules.modelgraphview.PostgresModelGraphNode;


import com.mxgraph.model.mxCell;
import com.mxgraph.util.mxPoint;
import com.mxgraph.view.mxGraph;
import modules.modelgraphview.ModelInfo;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeItem;


/**
 * @author Christopher Schildt
 * @date   03.09.2011
 *
 */
public class PostgresParser {

    List<String> plannodes = new ArrayList<String>();
    List<Integer> plannodesWSC = new ArrayList<Integer>();

    public void parseAndSetInputQueryPlan(ResultSet rs, mxGraph mxGraph, int i) {
        mxGraph.setHtmlLabels(true);
        try {
            List<String> plannodes = new ArrayList<String>();
            List<Integer> plannodesWSC = new ArrayList<Integer>();
            StringBuffer str = new StringBuffer();
            int cnt;
            if(rs==null) return;
            while (rs.next()) {
                String rowString = rs.getString(1);
                if (rowString.contains("->")) {
                    plannodes.add(str.toString());
                    cnt = 0;
                    while (str.toString().startsWith(" ", cnt++));
                    plannodesWSC.add(cnt);

                    str = new StringBuffer();
                }
                str.append(rowString + ",");
            }
            plannodes.add(str.toString());
            cnt = 0;
            while (str.toString().startsWith(" ", cnt++));
            plannodesWSC.add(cnt);
            String x = plannodes.get(0);
            PostgresPlanNode pln = QueryPlanViewerTools.createNodeFromString(x);
            Object newNode = mxGraph.insertVertex(mxGraph.getDefaultParent(), null, pln, i, 0, 100, 30, QueryPlanViewerTools.gernerateStyle(pln));
            mxGraph.updateCellSize(newNode);
            this.addModelNodes(pln, mxGraph,newNode);

            this.addChildren(newNode, 0, 0, plannodes, plannodesWSC, mxGraph, i);

        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    private void addChildren(Object newNode, int parentcnt, int parentindex,
            List<String> plannodes, List<Integer> plannodesWSC, mxGraph mxGraph, double w) {

        if (parentindex + 1 >= plannodes.size()) {
            return;
        }
        String x = plannodes.get(parentindex + 1);
        PostgresPlanNode pln = QueryPlanViewerTools.createNodeFromString(x);
        int currentcnt = plannodesWSC.get(parentindex + 1);
        if (currentcnt <= parentcnt) {
            return;
        }
        Object xNode = mxGraph.insertVertex(mxGraph.getDefaultParent(), null, pln, w, 0, 100, 30, QueryPlanViewerTools.gernerateStyle(pln));
        mxGraph.updateCellSize(xNode);
        this.addModelNodes(pln, mxGraph,xNode);
        Object xArc = mxGraph.insertEdge(mxGraph.getDefaultParent(), null, "", newNode, xNode, "startArrow=classic;endArrow=none;strokeWidth=2;"); //
        
        mxGraph.updateCellSize(xArc);
        this.addChildren(xNode, currentcnt, parentindex + 1, plannodes, plannodesWSC, mxGraph, w);
        checkandUpdateArcData((mxCell)xArc,(mxCell)newNode,(mxCell)xNode);

        for (int i = parentindex + 2; i < plannodes.size(); i++) {
            x = plannodes.get(i);
            if (plannodesWSC.get(i) < currentcnt) {
                return;
            }
            if (plannodesWSC.get(i) > currentcnt) {
                continue;
            }
            pln = QueryPlanViewerTools.createNodeFromString(x);
            xNode = mxGraph.insertVertex(mxGraph.getDefaultParent(), null, pln, w, 0, 100, 30, QueryPlanViewerTools.gernerateStyle(pln));
            mxGraph.updateCellSize(xNode);
            xArc=mxGraph.insertEdge(mxGraph.getDefaultParent(), null, "", newNode, xNode, "startArrow=classic;endArrow=none;strokeWidth=2;");
            
            this.addModelNodes(pln, mxGraph,xNode);
            this.addChildren(xNode, currentcnt, i, plannodes, plannodesWSC, mxGraph, w);
            checkandUpdateArcData((mxCell)xArc,(mxCell)newNode,(mxCell)xNode);


        }

    }

    public void parseAndSetInputModelGraphIndex(ResultSet rs, mxGraph mxGraph,
            int i, Tree ts) {
        Map<String, Object> nodeMap = new HashMap<String, Object>();
        Map<String, TreeItem> nodeToTreeMap = new HashMap<String, TreeItem>();
        PostgresModelGraphNode pln;

        try {
            rs.next();
            String rowString = rs.getString(1);
            mxGraph.setHtmlLabels(true);
            for (String attname : rowString.split(";")) {
                if (attname.trim().equals("")) {
                    continue;
                } else {
                    String[] temp = attname.trim().split(":");
                    ModelgraphViewerTools.addAtt(temp[0], temp[1].equals("true"));
                }
            }

            rs.next();
            rowString = rs.getString(1);
            rs.next();
            rowString = rs.getString(1);
             ModelgraphViewerTools.trainingdata=extractTrainingString(rowString);
            pln = ModelgraphViewerTools.createNodeFromString(rowString);
            Object o = mxGraph.insertVertex(mxGraph.getDefaultParent(), null, pln, i, 0, 100, 30, ModelgraphViewerTools.gernerateStyle(pln));

            nodeMap.put("root", o);
            TreeItem item = new TreeItem(ts, SWT.NONE);
            item.setData(o);
            item.setText("Root");
            nodeToTreeMap.put("root", item);
           
            while (rs.next()) {
                rowString = rs.getString(1);
                if (rowString.contains("->")) //Edge
                {
                    String[] sourceDest = rowString.split("->");
                    String node1 = sourceDest[1].split(";")[0].trim();
                    TreeItem item2 = new TreeItem(nodeToTreeMap.get(sourceDest[0].trim()), SWT.NONE);
                    item2.setData(nodeMap.get(sourceDest[1].split(";")[0].trim()));
                    PostgresModelGraphNode a=((PostgresModelGraphNode) ((com.mxgraph.model.mxCell) nodeMap.get(node1)).getValue());
                    item2.setText(((PostgresModelGraphNode) ((com.mxgraph.model.mxCell) nodeMap.get(node1)).getValue()).getSource());
                    nodeToTreeMap.put(sourceDest[1].split(";")[0].trim(), item2);
                    ((PostgresModelGraphNode) ((com.mxgraph.model.mxCell) nodeMap.get(node1)).getValue()).setTreeitem(item2);
                    createTreeChilds(((PostgresModelGraphNode) ((com.mxgraph.model.mxCell) nodeMap.get(node1)).getValue()), item2);
                    mxGraph.insertEdge(mxGraph.getDefaultParent(), null, "", nodeMap.get(sourceDest[0].trim()), nodeMap.get(node1), "strokeWidth=2;strokeColor=#000000");
                    if (((PostgresModelGraphNode) ((com.mxgraph.model.mxCell) nodeMap.get(node1)).getValue()).isHasModel()) {
                        ((com.mxgraph.model.mxCell) nodeMap.get(node1)).setStyle(ModelgraphViewerTools.gernerateStyle(((PostgresModelGraphNode) ((com.mxgraph.model.mxCell) nodeMap.get(node1)).getValue())));
                    }
                    continue;
                } else //Vertex
                {
                    pln = ModelgraphViewerTools.createNodeFromString(rowString);

                    o = mxGraph.insertVertex(mxGraph.getDefaultParent(), null, pln, i, 0, 100, 30, ModelgraphViewerTools.gernerateStyle(pln));
                    String[] name = rowString.split("[\\s]");
                    mxGraph.updateCellSize(o);
                    nodeMap.put(name[0], o);
                }
            }

        } catch (SQLException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    public void createTreeChilds(PostgresModelGraphNode pln, TreeItem item2) {
        Map<String, TreeItem> tmmap = new HashMap<String, TreeItem>();
        if (!pln.isHasModels()) {
            return;
        } else {
            for (String name : pln.getModels().keySet()) {
                ModelInfo temp = pln.getModels().get(name);
                String time = temp.getTime();
                
                String meassure = temp.getMeassuere();
                if (!tmmap.containsKey(time + meassure)) {
                    TreeItem newItem = new TreeItem(item2, SWT.None);
                    newItem.setText(meassure + " on " + time);

                    tmmap.put(time + meassure, newItem);

                }

                TreeItem newItem = new TreeItem(tmmap.get(time + meassure), SWT.None);
                newItem.setText(temp.getName());
                newItem.setData(temp);
                if (temp.getParameter().get("type").equals("Model")) {
                    pln.setHasModel(true);
                    newItem.setImage(ResourceRegistry.getInstance().getImage("model"));
                } else {
                    newItem.setImage(ResourceRegistry.getInstance().getImage("disag"));
                }
                //add models
            }
        }

        return;

    }

    private void addModelNodes(PostgresPlanNode node, mxGraph mxGraph,Object parent) {

        if (node.forecastModels == null)
            return;
        {
            for (int i : node.forecastModels.keySet()) {

                ModelInfo model=new ModelInfo();
                model.setParameter(node.forecastModels.get(i));
                if(model.getParameter().get("buildModel").equals("1"))
                {
                    if(parent==null) continue;
                    Object[] edges=mxGraph.getEdges(parent);
                    for(Object a: edges)
                    {
                        mxCell temp=(mxCell)a;
                        temp.setValue(model);
                    }
                    continue;
                }
                //create SourceNode
                PostgresPlanNode newNode=new PostgresPlanNode();
		newNode.setType("Input");
		newNode.setLabel(node.getSource());
		Object newGraphNode=mxGraph.insertVertex(QueryPlanViewerTools.jGraph.getDefaultParent(), null, newNode, 0, 0,100 , 30, QueryPlanViewerTools.gernerateStyle(newNode));

                //create new ModelNode
        


                //create Edge with ModelNode as Label
                com.mxgraph.model.mxCell edge= (com.mxgraph.model.mxCell)mxGraph.insertEdge(mxGraph.getDefaultParent(), null, model, parent, newGraphNode, "startArrow=classic;strokeWidth=2;endArrow=none;strokeWidth=2;rounded=true;");
               mxPoint source=edge.getGeometry().getSourcePoint();
               mxPoint target=edge.getGeometry().getTargetPoint();
               int y=0;
               if(target!=null && source!=null && target.getX()-source.getX()>50)
                   y=40;
                edge.getGeometry().setOffset(new mxPoint(37,y));
            }
            node.forecastModels = null;
            node.setSource(null);
        }

    }

    private void checkandUpdateArcData(mxCell arc, mxCell newNode, mxCell xNode) {
           PostgresPlanNode source=(PostgresPlanNode) newNode.getValue();
           PostgresPlanNode target=(PostgresPlanNode) xNode.getValue();
            if (!source.type.equals("Forecast") || !target.type.equals("Create ForecastModel"))
                return;
                ModelInfo model=new ModelInfo();
                model.setParameter(target.parameters);
                        arc.setValue(model);
                        arc.getGeometry().setOffset(new mxPoint(37,0));

                


    }

    private String extractTrainingString(String rowString) {
        int startidx=rowString.indexOf("[")+1;
        int endidx=rowString.lastIndexOf("]");
        return rowString.substring(startidx,endidx);
    }
}
