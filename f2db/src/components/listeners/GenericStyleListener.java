package components.listeners;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.LineStyleEvent;
import org.eclipse.swt.custom.StyleRange;
/**
 * This is a generic Style Listener to use with a StyledText Widget. It can format the
 * bg color of lines, the fg color + style according to different ranges set by the user.
 * 
 * @author Felix Beyer
 * @date   18.11.2006
 *
 */
public class GenericStyleListener extends DecoratedLineStyleListener{

    private StyleRange[] sr; // the Stylerange to use for formatting
    private List<Integer> ranges; // the ranges, which will be formatted with the given color


    /**
     * The default niladic constructor 
     * @param sr the prototype StyleRange to use
     * @param bg the bg color to format 
     */
    public GenericStyleListener(AbstractLineStyleListener listener){
        super(listener);
        
        // default stylerange
        this.sr = new StyleRange[1];
        StyleRange sr1 = new StyleRange();
        sr1.fontStyle = SWT.BOLD;
        sr1.underline = true;
        sr[0] = sr1;
        
        // initialize the ranges array 
        this.ranges = new ArrayList<Integer>();
    }
    
    
    /**
     * The constructor 
     * @param sr the prototype StyleRange to use
     * @param bg the bg color to format 
     */
    public GenericStyleListener(AbstractLineStyleListener listener, StyleRange[] sr){
        super(listener);
        this.sr = sr;
        this.ranges = new ArrayList<Integer>();
    }

    /** resets the ranges list */
    public void reset(){ranges.clear();}
    
    /**
     * the hook method which gets called to determine 
     * the FGColor and the Style to format the input
     */
    public void decorate(LineStyleEvent event) {
        listener.decorate(event);
        
        int srToUse = inRange(event.lineOffset);  
        if (srToUse>-1){
            StyleRange newSR = new StyleRange();

            newSR = (StyleRange)sr[srToUse].clone();
            newSR.start  = event.lineOffset;
            newSR.length = event.lineText.length();
            StyleRange[] styles = null;
            
            if (event.styles!=null){
                styles = new StyleRange[event.styles.length+1];
                for(int i=0;i<event.styles.length;i++){
                    styles[i] = event.styles[i];
                }
                styles[styles.length-1] = newSR;
            } else {
                styles = new StyleRange[1];
                styles[0] = newSR;
            }
            event.styles = styles;
        }
    }

    /**
     * adds an interval from start char offset to start+length char offset, which is
     * responsible for a different bg color
     * @param start the start offset of the text 
     * @param length the length in chars of the interval (line delimiters are counted too)
     */
    public void addRange(int offset, int start, int length){
        ranges.add(start);
        ranges.add(length);
        ranges.add(offset);
        //System.out.println("new range from "+start+" to "+(start+length));
    }
    
    /**
     * returns StyleRange offset to use if offset is in ranges list, otherwise -1
     * @param offset to check if in range
     * @return StyleRange offset
     */
    public int inRange(int offset){
        int inRange=-1;
        int i = 0;
        while(i<ranges.size()){
            int start = ranges.get(i);
            i++;
            int length = ranges.get(i);
            i++;
            int sr = ranges.get(i);
            i++;
            
            if(offset>=start && offset <=start+length){
                return sr;
            }
        }
        return inRange;
    }
    
    // ---------------------------------------------------------
    // Getter & Setter 

    public void setStyleRangePrototypes(StyleRange[] sr) {
        this.sr = sr;
    }
    
    public void setStyleRangePrototype(int offset, StyleRange sr) {
        if(offset >=0 && offset<sr.length){
            this.sr[offset] = sr;
        } else {
        }
    }

    public StyleRange[] getStyleRangePrototypes() {
        return this.sr;
    }
    
    
    public StyleRange getStyleRangePrototype(int offset) {
            if(offset >=0 && offset<sr.length){
                return sr[offset];
            } else {
                return null;
            }
        }
    
    
    
}
