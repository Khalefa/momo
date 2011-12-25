package components.listeners;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.LineStyleEvent;
import org.eclipse.swt.custom.StyleRange;

/**
 * This class is responsible for the SQL language keyword formatting. It can be applied to
 * a StyledText widget and provides the functionality, whenever a SQL92 keyword
 * occurs, this listener will recognize it and formats this keyword with a provided
 * or a default style.
 * 
 * @author Felix Beyer
 * @date   08.05.2006
 *
 */
public class SQLKeywordLineStyleListener extends DecoratedLineStyleListener 
                                         implements Keywords {

	private StyleRange keyword_SR; // the prototype stylerange
	private Map<String,Boolean> keywords_Map; // a map containing all the keywords
	private final String delimiter = "; ()\r\n\t~"; // the delimiter pattern
	private List<Integer> ranges; /** the ranges is used to determine areas, which should <b>not!!</b> get styled */
	private boolean cutEndChar = false; /** this one is necessary to determine if to cut off one end char of the style ranges*/ 
	
	// ******************************************************************************
	// Constructors
	// ******************************************************************************
	
	
	public SQLKeywordLineStyleListener(AbstractLineStyleListener listener){
		super(listener);
        
		// default stylerange
		keyword_SR = new StyleRange();
		keyword_SR.fontStyle = SWT.BOLD; // bold text
		keyword_SR.underline = true; // underlined
        // blue color for sql keywords
        // keyword_SR.foreground = ResourceRegistry.getInstance().getColor(0,0,255);
        // keyword_SR.background = ResourceRegistry.getInstance().getColor(Display.getDefault(),0,0,0);

        // create Hashmap to store the keywords
		keywords_Map = new HashMap<String,Boolean>();
		
        // parse in keywords and store them as keys into a map
		int size = keywords.length;
		Boolean trueValue = Boolean.valueOf(true);
		for(int i=0;i<size;i++){
			keywords_Map.put(keywords[i],trueValue);
		}
		
        // create ranges array for exclusive style formatting
        ranges = new ArrayList<Integer>();
		
	}
	
	public SQLKeywordLineStyleListener(AbstractLineStyleListener listener, StyleRange format){
	    super(listener);
        
        // save stylerange prototype 
		keyword_SR = format;
		
        // create Hashmap
		keywords_Map = new HashMap<String,Boolean>();
		
        // parse in keywords and store them as keys into a map
		int size = keywords.length;
		Boolean trueValue = Boolean.valueOf(true);
		for(int i=0;i<size;i++){
			keywords_Map.put(keywords[i],trueValue);
		}
		
        // create ranges array
        ranges = new ArrayList<Integer>();
		
	}
	
    /**
	 * resets the ranges list, which is kept to keep track of non-style areas
	 */
	public void reset(){ranges.clear();}
	
    /** this method switches the cutoff behaviour of the listener */
    public void switchCutOffChar(){cutEndChar = !cutEndChar;}
    
	/**
	 * adds an interval from start char offset with to start+length char offset, which is
	 * responsible for a different bg color
	 * @param start the start offset of the text 
	 * @param length the length in chars of the interval (line delimiters are counted too)
	 */
	public void addRange(int start, int length){
		ranges.add(start); ranges.add(length);
		//System.out.println("exclusive range from "+start+" to "+(start+length));
	}

	/**
	 * returns true if offset is in ranges list
	 * @param offset to check if in range
	 * @return true if the offset is in, otherwise false
	 */
	public boolean inRange(int offset){
		boolean inRange=false;
		int i = 0;
		while(i<ranges.size()){
			int start = ranges.get(i);
			i++;
			int length = ranges.get(i);
			i++;
			if(offset>=start && offset <=start+length){
				return true;
			}
		}
		return inRange;
	}
	
    public void decorate(LineStyleEvent event) {
        // first call the inner decorator, before we get to work
        listener.decorate(event);
        //System.out.println(event.lineOffset);
          
        // if we are not in the exclusive ranges array, start 
        if (!inRange(event.lineOffset)){
        
            int start   = event.lineOffset;
            String text = event.lineText;
            
            // parse in the String
            StringTokenizer st = new StringTokenizer(text,delimiter,true);
            String oword,word;
            boolean delimSwitch; // is true when token is found in the delimiter String
        
        Boolean value; // is true if we found a keyword 
        List<StyleRange> ranges = new ArrayList<StyleRange>(); // temporary holder for the ranges
        StyleRange sr;
        int startOfWord=start;
        int lengthOfWord;
        // repeat until all tokens are consumed
        while (st.hasMoreTokens()){
            delimSwitch=false;
            // fetch next token
            oword = st.nextToken();
            if(delimiter.indexOf(oword)>-1){
                delimSwitch = true;
            }
            if (!delimSwitch){
                // transform into upper case for lookup
                word  = oword.trim().toUpperCase();
                // save start and length of word
                startOfWord = start;
                lengthOfWord = oword.length();
                // do the lookup in the keywords map
                value = (Boolean)keywords_Map.get(word);
                // if we found a hit
                if(value!=null){
                    // add a new stylerange to the temp array
                    sr = (StyleRange)keyword_SR.clone();
                    sr.start  = startOfWord;
                    sr.length = lengthOfWord;
                    if(cutEndChar) sr.length--;
                    ranges.add(sr);
                }
                start += lengthOfWord;
            } else {
                
                start += oword.length();
            }
        }
        // now postprocess the found new StyleRanges and add them to the existing ones
        // of the event, we are listening to
        StyleRange[] srs = null; // start with null
        // if any keywords occured, renew the whole ranges array (append the new ones) 
        if (ranges.size()>0){
            if (event.styles!=null){
                int numberOfStyles = event.styles.length;
                srs = new StyleRange[ranges.size()+numberOfStyles];
            
                // copy old styles
                for(int i=0;i<numberOfStyles;i++){
                    srs[i] = event.styles[i];
                }
            
                // add new styles
                for(int i=numberOfStyles;i<ranges.size()+numberOfStyles;i++){
                    srs[i] = (StyleRange)ranges.get(i-numberOfStyles);
                }
            } else{
                srs = new StyleRange[ranges.size()];

                // only add new styles
                for(int i=0;i<ranges.size();i++){
                    srs[i] = (StyleRange)ranges.get(i);
                }
            }
            // store styles in event
            event.styles = srs;
        }
        
    }
    
	}
}
