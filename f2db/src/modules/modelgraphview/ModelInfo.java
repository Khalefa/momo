/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package modules.modelgraphview;

import java.util.HashMap;
import java.util.Map;

/**
 *
 * @author schildt
 */
public class ModelInfo {
    String name;
    String time;
    String meassuere;
    Map<String,String> parameter=new HashMap<String,String>();

    public String getName() {
        return name;
    }

    public boolean isDisagg(){
        return (this.parameter.get("disag key")!=null && this.parameter.get("disag key").equals("1.000000"));
    }
    public boolean isNothing()
    {
        return (this.parameter.get("disag key")!=null && this.parameter.get("disag key").equals("0.000000"));
    }
    public void setName(String name) {
        this.name = name;
    }

    public String getMeassuere() {
        return meassuere;
    }



    public void setMeassuere(String meassuere) {
        this.meassuere = meassuere;
    }

    public String getTime() {
        return time;
    }

    public void setTime(String time) {
        this.time = time;
    }

    public Map<String, String> getParameter() {
        return parameter;
    }

    public void setParameter(Map<String, String> parameter) {
        this.parameter = parameter;
    }

    	public String toString()
	{
            if(this.parameter.get("model name").equals("(null)"))
                this.parameter.put("model name", "Model");
            if((this.parameter.get("disag key")!=null && this.parameter.get("disag key").equals("1.000000")))
		return "<table style=\"\" width=\"100%\" border=\"0\" cellpadding=\"4\" class=\"title\">" +
		"<tr><<th><b><center><font size=\"+0\" color=\"#000000\"><b>"+this.parameter.get("model name")+"</b></font></center></b></th></tr>" +
		"</table>";

            else
                if(this.parameter.get("disag key")!=null && this.parameter.get("disag key").equals("0.000000"))
                {
                    return "<table style=\"\" width=\"100%\" border=\"0\" cellpadding=\"4\" class=\"title\">" +
		"<tr><<th><b><center><font size=\"+0\" color=\"#000000\"><b>"+this.parameter.get("model name")+"</b></font></center></b></th></tr>" +
		"</table>";
                }
                else
                return "<table style=\"\" width=\"100%\" border=\"0\" cellpadding=\"4\" class=\"title\">" +
		"<tr><<th><b><center><font size=\"+0\" color=\"#000000\"><b>Disag <font size=\"+1\">&#8594;</font> <br>"+this.parameter.get("model name")+"</b></font></center></b></th></tr>" +
		"</table>";
	}



}
