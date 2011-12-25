package modules.misc;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;

import data.type.Tuple;

/**
 * Helper class, which provides methods to easily format strings.
 * @author Felix Beyer
 * @date   18.06.2006
 *
 */
public class StringUtils {

	/** Formats a string to a given width. The result is either left- or right-aligned. If the
	 * given string exceeds the given width, it is truncated and its last character replaced by
	 * a "~" sign.
     * 
     * @param text the string which should be formatted
     * @param width the desired length of the output
     * @param alignRight if <code>true</code> the result is right-aligned
     * @return the formatted string of length <code>width</code>
     */
	public static String format(String text, int width, boolean alignRight) {
		if(text==null) return "";
		String result = text;
	       
	       // fill with spaces
	       while (result.length() < width) {
	           if (alignRight) {
	               result = " " + result;
	           } else {
	               result = result + " ";
	           }
	       }
	       
	       // truncate
	       if (result.length() > width) {
	           result = result.substring(0, width-1);
	           result = result + "~";
	       }
	       
	    return result;
	}
	
	/**
	 * Replaces a string containing escape chars \r and \n, with empty char
	 * @param text the string to format
	 * @return a string containing no \r and \n chars anymore
	 */
	public static String replaceDelChars(String text){
		if (text == null)
			return text;
		
		String newString = text;

		if(newString.indexOf("\n\r") > -1) newString = newString.replaceAll("\n\r"," ");
		if(newString.indexOf("\f") > -1) newString = newString.replaceAll("\f"," ");
		if(newString.indexOf("\r") > -1) newString = newString.replaceAll("\r"," ");
		if(newString.indexOf("\n") > -1) newString = newString.replaceAll("\n"," ");
		if(newString.indexOf("\t") > -1) newString = newString.replaceAll("\t"," ");
		
		return newString;
	}
	
    /**
	 * this method extract the first n values from the tuple and creates a group name
	 * @param t the Tuple to use
	 * @return a String describing the group of the Tuple
	 */
	public static String getGroupNameFromTuple(int n, Tuple t){
		if(n > t.size())
			return "";
		StringBuilder name = new StringBuilder();
		for(int i = 0; i < n - 1; i++){
			name.append(t.getValue(i).toString()).append(";");
		}
		if (n > 0)
			name.append(t.getValue(n - 1).toString());
		return name.toString();
	}
	
    /** formats a Double value, with the specified number of Decimals */
	public static String formatDouble(double value,int noDecimals){
		StringBuilder pattern = new StringBuilder("###,###,###,###,##0.");
		for (int i = 0; i < noDecimals; i++)
			pattern.append("0");
		DecimalFormat form = new DecimalFormat(pattern.toString(), new DecimalFormatSymbols(Locale.ENGLISH));
		return form.format(value);
	}
	
	/**
	 * cuts the package name and returns only the last class name without the package
	 * @param classname the classname (with package)
	 * @return a shortened version of classname :-)
	 */
	public static String cutPackageName(String classname){
		int index = classname.lastIndexOf(".");
		if(index>0){
			return classname.substring(index+1);
		} else return classname;
	}
	
    /** resolve other abbreviations */
    public static String resolveAbbreviation(String abb){
        String ret = abb; 
        // resolve stmt type if appropriate
        ret = resolveStatementType(abb);

        if(abb.equalsIgnoreCase("Y")){
            ret = "YES";
        } else 
        if(abb.equalsIgnoreCase("N")){
            ret = "NO";
        } else 
        if(abb.equalsIgnoreCase("F")){
            ret = "FULL EXPLAIN";
        } else 
        if(abb.equalsIgnoreCase("O")){
            ret = "EXPLAIN ONLY";
        } else 
        if(abb.equalsIgnoreCase("T")){
            ret = "TABLE";
        } else 
        if(abb.equalsIgnoreCase("R")){
            ret = "ROW";
        } else 
        if(abb.equalsIgnoreCase("I")){
            ret = "INDEX";
        } else 
        if(abb.equalsIgnoreCase("C")){
            ret = "CONSTRAINT";
        } else 
        if(abb.equalsIgnoreCase("SH")){
            ret = "SHARE";
        } else 
        if(abb.equalsIgnoreCase("IS")){
            ret = "INSTANTENAOUS SHARE";
        } else 
        if(abb.equalsIgnoreCase("EX")){
            ret = "EXCLUSIVE";
        } else 
        if(abb.equalsIgnoreCase("IX")){
            ret = "INSTANTENEOUS EXCLUSIVE";
        } else 
        if(abb.equalsIgnoreCase("RU")){
            ret = "READ UNCOMMITED";
        } else 
        if(abb.equalsIgnoreCase("RC")){
            ret = "READ COMMITED";
        } else 
        if(abb.equalsIgnoreCase("RR")){
            ret = "REPEATABLE READ";
        } else 
        if(abb.equalsIgnoreCase("SE")){
            ret = "SERIALIZABLE";
        } else 
        if(abb.equalsIgnoreCase("EX")){
            ret = "EXTERNAL";
        } else 
        if(abb.equalsIgnoreCase("IN")){
            ret = "INTERNAL";
        }
        
        return ret;
    }
    
    /** resolve statement type from abbreviation */
    public static String resolveStatementType(String type){
        String ret = type;
        if (type.equalsIgnoreCase("C")){
            ret = "CALL Statement";
        } else 
        if (type.equalsIgnoreCase("S")){
            ret = "SELECT Statement";
        } else
        if (type.equalsIgnoreCase("SA")){
            ret = "SELECT APPROXIMATE Statement";
        } else
        if (type.equalsIgnoreCase("D")){
            ret = "DELETE Statement";
        } else
        if (type.equalsIgnoreCase("I")){
            ret = "INSERT Statement";
        } else
        if (type.equalsIgnoreCase("U")){
            ret = "UPDATEE Statement";
        } else
        if (type.equalsIgnoreCase("DDL")){
            ret = "DDL Statement";
        }
        return ret;
    }
    
	/** test main method to run seperately */
	public static void main(String[] args) {
		String s = "This\nis\n\ra\n\rtest";
		System.out.println(s);
		System.out.println(replaceDelChars(s));
		double d = 23343443433.432422554;
		System.out.println(formatDouble(d,2));
	}
}
