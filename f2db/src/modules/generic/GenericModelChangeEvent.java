package modules.generic;

public class GenericModelChangeEvent {
	
	public int detail; // contains details about what changed
	public String source; // contains the source of the event (who generated that event)
	public boolean showResultInConsole = true;
	
	public GenericModelChangeEvent(){}

}
