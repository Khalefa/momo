package components.listeners;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.swt.custom.LineBackgroundEvent;
import org.eclipse.swt.custom.LineBackgroundListener;
import org.eclipse.swt.graphics.Color;
/**
 * This class is the implementation of a LineBackgroundListener, which
 * formats a StyledText widget with determined background colors.
 * It simply checks its ranges array and returns a set BG Color for the specified slot, 
 * if the area is covered in the ranges array.
 * 
 * @author Felix Beyer
 * @date   08.05.2006
 *
 */
public class GenericBGColorListener implements LineBackgroundListener {
    
	private Color[] bgColors; // the bg color to use for formatting 
	private List<Integer> ranges; // the ranges, which will be formatted with the given color
	
	/**
	 * the constructor accepts two colors 
	 * @param bg the colors for the different range types
	 */
	public GenericBGColorListener(Color[] bg){
		// save color
        bgColors = bg;
		// and create new ArrayList to keep the formatting ranges
        ranges = new ArrayList<Integer>();
	}

    /**
	 * this method clears the ranges arrays
	 */
	public void reset(){ranges.clear();}
	
	/**
	 * adds an interval from start char offset to start+length char offset, which is
	 * responsible for a different bg color
	 * @param start the start offset of the text 
	 * @param length the length in chars of the interval (line delimiters are counted too)
	 */
	public void addRange(int colorNo, int start, int length){
		ranges.add(start);
		ranges.add(length);
        ranges.add(colorNo);
		//System.out.println("new range from "+start+" to "+(start+length));
	}
	
	/**
	 * returns the colorNo if offset is in ranges list, otherwise -1
	 * @param offset to check if in range
	 * @return true if the offset is in, otherwise false
	 */
	public int inRange(int offset){
		int i = 0;
		while(i<ranges.size()){
			int start = ranges.get(i);
			i++;
			int length = ranges.get(i);
			i++;
            int color = ranges.get(i);
            i++;
			if(offset>=start && offset <=start+length){
				return color;
			}
		}
		return -1;
	}
	
	/**
	 * hook method which gets called when a new line is requested to determine its bg color
	 */
	public void lineGetBackground(LineBackgroundEvent event) {
		//System.out.println("in OutputBGListener..."+event.lineText);
		int color = inRange(event.lineOffset);
        if(color>-1){
			//System.out.println("in Range, offset:"+event.lineOffset+"event.lineBackground:"+event.lineBackground);
            event.lineBackground = bgColors[color];
		}
	}
	
    // ---------------------------------------------------------
	// Get & Set Methods 

    public Color getBGColor(int no) {return bgColors[no];}
	public void setBGColor(int no, Color bgColor) {
	    if (no>=0 && no<bgColors.length){
	        bgColors[no] = bgColor;
        }
    }
	
}
