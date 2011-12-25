package modules.databaseif;

import java.io.Serializable;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import modules.misc.Constants;
import modules.misc.StringUtils;
import data.type.Tuple;
import data.type.Value;

/**
 * This class encapsulates a JDBC ResultSet. It provides functions to iterate
 * over this ResultSet. This class will be heavily used by application components.
 * 
 * @author Felix Beyer
 * @date   10.04.2006
 *
 */
public class ResultSetModel implements Serializable {
	
	private static final long serialVersionUID = 3209520959052456855L;

	private List<String> columnNames; // the column names
	private List<Tuple> tuples; // the Tuples
	private int[] columnWidth; // an array containing the column width for each column
	
	private boolean createSB; // a flag, which indicates if a stringbuffer during parse in should be created for speedup the string creation
	private StringBuffer stringBuffer; // the stringbuffer to fill

	/**
	 * This is the constructor of the ResultSetModel 
	 * @param createSB if true a string buffer will be created during filling the tuples array
	 */
	public ResultSetModel(boolean createSB){
		columnNames = new ArrayList<String>();
		tuples = new ArrayList<Tuple>();
		this.createSB = createSB;
		
		if(createSB) stringBuffer = new StringBuffer();
	}

    /**
     * extracts the column headers for display  
     * @param rsmd the resultsetmetadata object of the resultset
	 */
	public void extractColumnHeaders(ResultSetMetaData rsmd){
        try {
            // number of columns
            int columnCount = rsmd.getColumnCount();
            // create collection which stores the column headers as Strings
            List<String> columnHeaders = new ArrayList<String>(columnCount);
            
            // number of printed characters
            int[] columnPrintWidth = new int[columnCount]; 
            // numeric column type?
            boolean[] columnIsNumeric = new boolean[columnCount];
                
            // parse in column headers from metadata
            for (int column=0; column < columnCount; column++) {
                // retrieve information about the column
                String columnName = rsmd.getColumnName(column+1);
                int columnDisplaySize = rsmd.getColumnDisplaySize(column+1);
                if (columnDisplaySize <= 0) {
                    columnDisplaySize = Constants.DEFAULT_PRINT_WIDTH;
                }
                if (columnDisplaySize > Constants.MAX_PRINT_WIDTH) {
                    columnDisplaySize = Constants.MAX_PRINT_WIDTH;
                }
                
                columnIsNumeric[column] = rsmd.isSigned(column+1); // hack for isNumeric 
                columnPrintWidth[column] = Math.max(columnDisplaySize, columnName.length());

                // add column name to collection
                columnHeaders.add(StringUtils.format(columnName, columnPrintWidth[column]+1, false));
            }
            
            // save columnheaders 
            setColumnNames(columnHeaders);
        } catch (SQLException e) {
            System.out.println("SQLException occured during extraction of column headers from metadata for ResultSetModel.");
            e.printStackTrace();
        }
    }
    
    /**
	 * this method sets the column headers of this result set
	 * @param columnNames the coilumn headers
	 */
	public void setColumnNames(List<String> columnNames){
		this.columnNames = columnNames;
		columnWidth = new int[columnNames.size()];
		// initialize column width array
		int i=0;
		for (String s:columnNames){
			s=s.trim();
			columnWidth[i] = s.length()+2;
			i++;
		}
	}
	
	/**
	 * this method adds a tuple
	 * @param t the tuple to add
	 */
	public void addTuple(Tuple t){
			// add the tuple to the list of tuples
            tuples.add(t);
			
			int i=0;
			String s;
			// iterate over columns to determine maximum columnwidth
            for(Value v : t){
				s = v.toString();
				if (s == null) {
                    s = Constants.PRINT_NULL;
                } else {
                    // update column width if value not null
                    columnWidth[i] = Math.max(columnWidth[i],s.length());
                }
				
				// if stringbuffer creation is toggled on
				if(createSB) {
					stringBuffer.append(StringUtils.format(s,columnNames.get(i).length(),v.isNumeric())+" ");
				}
				i++;
			}
			if(createSB) stringBuffer.append("\r\n");
	}
	
	/**
	 * this method returns a StringBuffer containing the contents of this ResultSetModel
	 * @param newLine the LineDelimiter for a newline character
	 * @return a StringBuffer of the contents
	 */
	public StringBuffer getStringBufferFromRSM(String newLine){
		int columnCount = columnNames.size(); // number of columns

		if(!createSB){
			stringBuffer = new StringBuffer();
			// print data
			Iterator<Tuple> iter = tuples.iterator();
			Tuple tuple;
			String valueFreeOfSpecialChars;
			while (iter.hasNext()) { // for each row
				tuple = iter.next();
				for (int column=0; column < columnCount; column++) { // for each column
					// print value
					Value value = tuple.getValue(column);
					if(!value.isNull()){
						valueFreeOfSpecialChars = StringUtils.replaceDelChars(value.toString());
						stringBuffer.append(StringUtils.format(valueFreeOfSpecialChars, columnWidth[column], value.isNumeric()));
					} else {
						stringBuffer.append(StringUtils.format(Constants.PRINT_NULL, columnWidth[column], value.isNumeric()));
					}
					if (column < columnCount) {
						stringBuffer.append(' ');                			
					}
				}
				stringBuffer.append(newLine);
			}
			stringBuffer.append(newLine);
			// toggle flag to get into other path, when next request for this model 
			createSB = true;

		}

		StringBuffer sb = new StringBuffer(); // create new StringBuffer

		// print column headers
		for (int column=0; column < columnCount; column++) {
			// retrieve information about the column
			String columnName = columnNames.get(column);
			// print column name
			sb.append(StringUtils.format(columnName,columnWidth[column],false)).append(" ");
		}
		sb.append(newLine);

		// print separator line
		for (int column=0; column < columnCount; column++) {
			for (int i=0; i<columnWidth[column]; i++) {
				sb.append('-');
			}
			sb.append(' ');
		}
		sb.append(newLine);

		// use already created stringbuffer to print out results
		sb.append(stringBuffer);

		// finally print tuple count
		sb.append(newLine + tuples.size() + " row(s) selected.");
		return sb;
	}
	
	/**
	 * 
	 * @return the list of tuples
	 */
	public List<Tuple> getTuples() {
		return tuples;
	}
	
	/**
	 * set the current tuples
	 */
	public void setTuples(List<Tuple> tupleList){
		tuples.clear();
		for(Tuple t:tupleList){
			addTuple(t);
		}
	}
	
	/**
	 * 
	 * @return a list of the column header names
	 */
	public List<String> getColumnNames() {
		return columnNames;
	}
	
	/**
	 * return a single column header name
	 * @param index the column index to get the name from
	 * @return the appropriate column header name
	 */
	public String getColumnName(int index) {
		return columnNames.get(index);
	}
	
	/**
	 * 
	 * @return an array of column widths for each column
	 */
	public int[] getColumnWidth() {
		return columnWidth;
	}
	
	/**
	 * 
	 * @return the number of columns
	 */
	public int getColumnCount() {
		return columnNames.size();
	}
}
