package components.listeners;

import java.util.HashMap;
import java.util.Map;
import java.util.StringTokenizer;

import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.custom.VerifyKeyListener;
import org.eclipse.swt.events.VerifyEvent;
import org.eclipse.swt.graphics.Point;

/**
 * This class is a special Listener, which can be applied to a StyledText
 * Widget. It transforms SQL92 Keywords into their upper case equivalent.
 * @author Felix Beyer
 * @date   08.05.2006
 */
public class UpperKeyListener implements VerifyKeyListener, Keywords {

	private StyledText st;
	private Map<String,Boolean> keywords_Map;
	private boolean toUpperCase = false;
	private final String delimiter = "; ()\r\n\t~";
	private boolean checkOnlyAfterSpaceChars = false;
	
	public UpperKeyListener(StyledText st){
		this.st = st;
		resetStyle();
		
		// create Hashmap
		keywords_Map = new HashMap<String,Boolean>();
		// parse in keywords and store them as keys into a map
		int size = keywords.length;
		Boolean trueValue = Boolean.valueOf(true);
		for(int i=0;i<size;i++){
			keywords_Map.put(keywords[i],trueValue);
		}
	}

    public UpperKeyListener(StyledText st, boolean spaceChars){
        this(st);
        checkOnlyAfterSpaceChars = spaceChars;
    }

    
    /**
	 * this resets the listener and toggles uppercase replacement on
	 *
	 */
	public void resetStyle(){
		toUpperCase = true;
	}
	
	/**
	 * this is the hook method which gets called by the StyledText Widget.
	 * it only gets working if a space character occured.(it could also be adjusted
	 * to check after every character, but due to the replacement of a whole string,
	 * this method is resource-saving)
	 */
	public void verifyKey(VerifyEvent event) {
		if (event.character==' ' || !checkOnlyAfterSpaceChars)
				if(toUpperCase){
			  		// save important StyledText state information
                    // before the text replacing starts
                    int offset = st.getCaretOffset(); // the offset of the caret
					String text = st.getText(); // the original text
					Point selection = st.getSelection(); // the selection before replacing
                    
					String word,oword;
					Boolean value;
					boolean delimSwitch; // is true when token is found in the delimiter String

					int start = 0;
					String newText = text;
					StringTokenizer stoken = new StringTokenizer(text,delimiter,true);

					while(stoken.hasMoreTokens()){
							
							delimSwitch=false;
							// get new word token
							oword = stoken.nextToken();
							if(delimiter.indexOf(oword)>-1)delimSwitch = true;
							if (!delimSwitch){
							// trim it to upper case
							word  = oword.trim().toUpperCase();
							// lookup if its in the map
							value = (Boolean)keywords_Map.get(word);
							// if it`s in there, transform into uppercase
							if(value!=null){
								// get start index in whole text 
								int wordStart = start; 
						
								newText = newText.substring(0,wordStart) + 
										  oword.toUpperCase() + 
										  newText.substring(wordStart+oword.length());
					
							}
							start += word.length();
							} else {
								start += oword.length();
							}
							
					}
					
					st.setText(newText);
					st.setCaretOffset(offset);
					st.setSelection(selection.x,selection.y);					
					
				}
	}

}
