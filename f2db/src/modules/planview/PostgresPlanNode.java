package modules.planview;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import modules.misc.PlanNode;

/**
 * @author Christopher Schildt
 * @date   03.09.2011
 *
 */
public class PostgresPlanNode implements Serializable,PlanNode{
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	public static boolean collapsedDefault=true;
	static int cellwidth=0;
	String label;
	String type;
	List<String> content;
	String sourceText;
	String cost;

    public boolean getBuild() {
        return build;
    }

    public void setBuild(boolean build) {
        this.build = build;
    }
	String rows;
	boolean coll;
	String width;
        int count;
        boolean build;
        Map<Integer,Map<String,String>> forecastModels;

    public int getCount() {
        return count;
    }

    public void setCount(int count) {
        this.count = count;
    }
        Map<String,String> parameters=new HashMap<String,String>();

    public String getOutput() {
        return output;
    }

    public void addParameter(String key,String value)
    {
        if(parameters==null)
            parameters=new HashMap<String, String>();
        parameters.put(key, value);
    }
    public void addfMPar(int number,String key,String value)
    {
        if(forecastModels==null)
            forecastModels=new HashMap<Integer,Map<String, String>>();
        if(!forecastModels.containsKey(number))
        {
            forecastModels.put(number, new HashMap<String, String>());
        }
        forecastModels.get(number).put(key, value);
        return;
    }

    public Map<String,String> getParameter()
    {
        return parameters;
    }

    public void setOutput(String output) {
        this.output = output;
    }
        String output;
	
	//Only for Non-Index Scans
	String source;
	//for Scans, Joins, Aggs, Groups,Results
	String qual;
	
	public String getQual() {
		return qual;
	}

	public void setQual(String qual) {
		this.qual = qual;
	}

	public String getSourceText() {
		return sourceText;
	}

	public void setSourceText(String sourceText) {
		this.sourceText = sourceText;
	}

	public String getLabel() {
		return label;
	}

	public void setLabel(String label) {
		this.label = label;
	}

	public String getType(boolean b) {
		if(!b)
		return type;
		else
			return type;
	}

	public String getCost() {
		return cost;
	}

	public void setCost(String cost) {
		this.cost = cost;
	}

	public String getRows() {
		return rows;
	}

	public void setRows(String rows) {
		this.rows = rows;
	}

	public String getWidth() {
		return width;
	}

	public void setWidth(String width) {
		this.width = width;
	}

	public void setType(String type) {
		this.type = type;
	}

	public List<String> getContent() {
		return content;
	}

	public void setContent(List<String> content) {
		this.content = content;
	}

	public String getSource() {
		return source;
	}

	public void setSource(String source) {
		this.source = source;
	}

	public PostgresPlanNode()
	{
		content=new ArrayList<String>();
		coll=collapsedDefault;
	}

	public String toString()
	{
			if(PostgresPlanNode.cellwidth==0) //No exakt size given, therefore make our label as big as needed
			{
				String result="";
                                if(!this.coll)result+="<br>";
				if(this.getType(false).contains("Input")||this.getType(false).contains("Output"))
					return "<font size=\"+1\">"+this.getLabel()+"</font>";
				if(this.getType(false).contains("Scantarget"))
				{
					result+= "<table style=\"\" width=\"100%\" border=\"0\" cellpadding=\"4\" class=\"title\">" +
					"<tr><th colspan=\"2\"><b><center><font size=\"+1\">"+this.getLabel()+"</font size=\"+1\"></center></b></th></tr>" +
					"</table>";
					return result;
				}
				
					if(this.qual==null || this.qual.equals(""))
						result+= "<table style=\"\" width=\"100%\" border=\"0\" cellpadding=\"4\" class=\"title\">" +
							"<tr><th colspan=\"2\"><b><center><font size=\"+1\">"+this.getType(true)+"</font size=\"+1\"></center></b></th></tr>" +
							"</table>";
					else
						result+= "<table style=\"\" width=\"100%\" border=\0\" cellpadding=\"4\" class=\"title\">" +
						"<tr><th colspan=\"2\"><b><center><font size=\"+1\">"+this.getType(false)+"</font size=\"+1\"></center></b> with "+this.getQual()+"</th></tr>" +
						"</table>";
				
			if(this.coll)
				return result;

			
			String rgb=Integer.toHexString(NodeStyles.getBodyColor(this.getType(false)).getRGB());
                        if(!this.type.equals("Modelinfo"))
                        {
                            result += ""
                                    +
			"<table width=\"90%\" style=\"background-color:#"+rgb.substring(2, rgb.length())+ "\" border=\"1\" cellpadding=\"0\">" +
			"<tr><td>Cost: </td><td>"+this.getCost()+"</td></tr>" +
			"<tr><td>Rows: </td><td>"+this.getRows()+"</td></tr>" +
			"<tr><td>Width: </td><td>"+this.getWidth()+"</td></tr>";

                        result +=
			"</table>";}
                        if(this.type.equals("Modelinfo"))
                        {
                            result+=""+
                            "<br><table width=\"90%\" border=\"0\" cellpadding=\"3\">";
                            if(this.parameters.containsKey("Key"))
                            {
                                float diskey=Float.parseFloat(this.parameters.get("Key"));
                                if(diskey!=0)
                                    result += "<tr><th colspan=\"2\"><center>"+diskey+"x</center></th></tr>";
                                else
                                    result += "<tr><th colspan=\"2\"><center>1x</center></th></tr>";
                            }
                            for(String key:this.parameters.keySet())
                            {
                                if(key.equals("Key")) continue;
                                result+="<tr><td>"+key+"</td><td>"+this.parameters.get(key)+"</td></tr>";
                            }
                            result+="<tr></tr></table>";
                        }
                       

			return result+"<br>";
			}

			//Getting to thís line means, that we want all vertices the same size!
			String result="";
			if(this.getType(false).contains("Input")||this.getType(false).contains("Output"))
				return "<font size=\"+1\">"+this.getLabel()+"</font>";
			if(this.getType(false).contains("Scantarget"))
			{
				result+= "<table style=\"\" width=\""+PostgresPlanNode.cellwidth+"px\" border=\"0\" cellpadding=\"4\" class=\"title\">" +
				"<tr><th colspan=\"2\"><b><center><font size=\"+1\">"+this.getLabel()+"</font size=\"+1\"></center></b></th></tr>" +
				"</table>";
				return result;
			}
					if(this.qual==null || this.qual.equals(""))
						result+= "<table style=\"\" width=\""+PostgresPlanNode.cellwidth+"px\" border=\"0\" cellpadding=\"4\" class=\"title\">" +
							"<tr><th colspan=\"2\"><b><center><font size=\"+1\">"+this.getType(true)+"</font size=\"+1\"></center></b></th></tr>" +
							"</table>";
					else
						result+= "<table style=\"\" width=\""+PostgresPlanNode.cellwidth+"px\" border=\"0\" cellpadding=\"4\" class=\"title\">" +
						"<tr><th colspan=\"2\"><b><center><font size=\"+1\">"+this.getType(false)+"</font size=\"+1\"></center></b> with "+this.getQual()+"</th></tr>" +
						"</table>";
			if(this.coll)
				return result;

                       
			String rgb=Integer.toHexString(NodeStyles.getBodyColor(this.getType(false)).getRGB());
                         if(!this.type.equals("Modelinfo"))
                        {
			result+=""+
			"<table width=\"90%\" style=\"background-color:#"+rgb.substring(2, rgb.length())+ "\" border=\"1\" cellpadding=\"0\">" +
			"<tr><td>Cost: </td><td>"+this.getCost()+"</td></tr>" +
			"<tr><td>Rows: </td><td>"+this.getRows()+"</td></tr>" +
			"<tr><td>Width: </td><td>"+this.getWidth()+"</td></tr>";
                        result +=
			"</table>";
            }
                         if(this.type.equals("Modelinfo"))
                        {
                            result+=""+
                            "<br><table width=\"90%\" border=\"0\" cellpadding=\"3\">";
                            if(this.parameters.containsKey("Key"))
                            {
                                float diskey=Float.parseFloat(this.parameters.get("Key"));
                                if(diskey!=0)
                                    result += "<tr><th colspan=\"2\"><center>"+diskey+"x</center></th></tr>";
                                else
                                    result += "<tr><th colspan=\"2\"><center>1x</center></th></tr>";
                            }
                            for(String key:this.parameters.keySet())
                            {
                                if(key.equals("Key")) continue;
                                result+="<tr><td>"+key+"</td><td>"+this.parameters.get(key)+"</td></tr>";
                            }
                            result+="<tr></tr></table><br>";
                        }
                        

			return result;

		}

	public boolean isColl() {
		return coll;
	}

	public void setColl(boolean coll) {
		this.coll = coll;
	}
	}

