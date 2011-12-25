package modules.modelgraphview;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import modules.misc.PlanNode;
import org.eclipse.swt.widgets.TreeItem;

/*
 * @author Christopher Schildt
 * @date   03.09.2011
 *
 */
public class PostgresModelGraphNode implements Serializable, PlanNode {

    /**
     *
     */
    private static final long serialVersionUID = 1L;
    public static int cellwidth;
    public static int cellheight;
    private static boolean collapsedDefault;
    String source;
    int mginId = 0;
    boolean collapsed;
    TreeItem treeitem;
    boolean hasModels; //Disaggs or Models
    boolean isLeaf = false;
    boolean hasModel = false; //real models, no disaggs
    Map<String, ModelInfo> models = new HashMap<String, ModelInfo>();
    int modelCount;

    public boolean isHasModels() {
        return hasModels;
    }

    public boolean isHasModel() {
        return hasModel;
    }

    public void setHasModel(boolean hasModel) {
        this.hasModel = hasModel;
    }

    public boolean isIsLeaf() {
        return isLeaf;
    }

    public void setIsLeaf(boolean isLeaf) {
        this.isLeaf = isLeaf;
    }

    public int getModelCount() {
        return modelCount;
    }

    public void setModelCount(int modelCount) {
        this.modelCount = modelCount;
    }

    public void setHasModels(boolean hasModels) {
        this.hasModels = hasModels;
    }

    public Map<String, ModelInfo> getModels() {
        return models;
    }

    public void setModels(Map<String, ModelInfo> models) {
        this.models = models;
    }

    public TreeItem getTreeitem() {
        return treeitem;
    }

    public void setTreeitem(TreeItem treeitem) {
        this.treeitem = treeitem;
    }

    public static void setCollapsedDefault(boolean collapsedDefault) {
        PostgresModelGraphNode.collapsedDefault = collapsedDefault;
    }

    public static boolean isCollapsedDefault() {
        return collapsedDefault;
    }

    public void setSource(String substring) {
        // TODO Auto-generated method stub
        this.source = substring;
    }

    public String getSource() {
        return this.source;
    }

    public String toString() {
        String result = "";
        if (PostgresModelGraphNode.cellwidth == 0) {
            result += "<table style=\"\" width=\"100%\" border=\"0\" cellpadding=\"4\" class=\"title\">";
        } else {
            result += "<table style=\"\" width=\"" + PostgresModelGraphNode.cellwidth + "px\" border=\"0\" cellpadding=\"4\" class=\"title\">";
        }
        if (!this.models.isEmpty()) {
            result += "<tr><th colspan=\"3\"><center><font size=\"+1\">" + this.mginId + "</font></center></th></tr><tr><th colspan=\"3\"><b><center><font size=\"+2\">" + this.source + "</font></center></b></th></tr>";
            Iterator iter = this.models.keySet().iterator();
            while (iter.hasNext()) {
                ModelInfo a = this.models.get(iter.next());
                if (a.parameter.get("disag key") != null) {
                    result += "<tr><td><center><font size=\"+1\">DisagScheme</font></center></td><td><font size=\"+1\"><center>&#8594;</center></font></td><td><center><font size=\"+1\">" + a.name + "</font></center></td></tr>";
                } else {
                    result += "<tr><td><center><font size=\"+1\">Model</font></center></td><td><font size=\"+1\"><center></center></font></td><td><center><font size=\"+1\">" + a.name + "</font></center></td></tr>";
                }

            }
        } else {
            result += "<tr><th colspan=\"3\"><b><center><font size=\"+2\">" + this.source + "</font></center></b></th></tr>";
        }

        result += "</table>";
        return result;
    }

    @Override
    public String getType(boolean b) {
        if (!this.isLeaf) {
            return "ModelGraphNodeEmpty";
        }
        if (this.modelCount <= 0) {
            return "ModelGraphNodeEmptyLeaf";
        }
        if (this.hasModel) {
            return "ModelGraphNodeFilledModel";
        } else {
            return "ModelGraphNodeFilledDisagg";
        }
    }

    public PostgresModelGraphNode() {
        collapsed = collapsedDefault;
    }

    @Override
    public boolean isColl() {
        return this.collapsed;
    }

    void setMginId(String substring) {
        this.mginId = Integer.parseInt(substring);
    }
}
