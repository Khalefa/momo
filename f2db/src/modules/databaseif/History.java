package modules.databaseif;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.List;

import modules.Module;
import modules.generic.DemoEvents;
import modules.generic.GenericModel;
import modules.generic.GenericModelChangeEvent;
import modules.generic.GenericModelChangeListener;
import modules.misc.ModuleRegistry;

/**
 * This class provides a history for statements. 
 * 
 * @author Felix Beyer, Rainer Gemulla
 * @date   10.04.2006
 *
 */
public class History implements Module, GenericModel{

	// -- private variables -----------------------------------------------------------------------
	
	private static History theHistory;

	private List<HistoryEntry> entries; // the statement info history 
	
	private int currentEntry; // current position in the history

	private List<GenericModelChangeListener> listeners; // an array for the different listeners
	

	// -- constructors ----------------------------------------------------------------------------
	
	private History() {
		entries = new ArrayList<HistoryEntry>();
		currentEntry = -1;// list is empty
		listeners = new ArrayList<GenericModelChangeListener>();

	}
	
	/**
	 * @return one instance of the StatementHistory
	 */
	public static History getInstance(){
		if(theHistory == null) {
			theHistory = new History();
			ModuleRegistry.addModule(theHistory);
		}
		return theHistory;
	}
	
	
	// -- business logic --------------------------------------------------------------------------
	
	/**
	 * adds a StatementInfo Object to the history 
	 * @param info the StatementInfo Object to add
	 */
	public void add(HistoryEntry entry){
        // deactivate old entry and store it after adding a new one
        if (currentEntry >= 0 && currentEntry < entries.size()) {
            entries.get(currentEntry).store();
        }

        // add new entry
		entries.add(entry);
		getLastEntry(); // jump to the new entry
	}

	/**
	 * this method returns the last history entry 
	 * @return the last entry
	 */
	public HistoryEntry getLastEntry() {
		if(entries.size()<=0)
			return null;
		return getEntry(entries.size()-1);
	}
	
	/** @returns IndexOutOfBoundsException if invalid index */
	public HistoryEntry getEntry(int index) {
		if (index < 0 || index >= entries.size()) {
			throw new IndexOutOfBoundsException();
		}
		/*
		// deactivate old entry
		if (currentEntry >= 0 && currentEntry < entries.size()) {
			entries.get(currentEntry).store();
		}*/
		
		// jump to new entry
		HistoryEntry entry = entries.get(index); 
		currentEntry = index;
		return entry;
	}
	
	public int size() {
		return entries.size();
	}
	
	// -- GenericModule ---------------------------------------------------------------------------
	
	/**
	 * this method resets the history
	 */
	public void reset() {
		entries.clear();
		currentEntry = -1;		
		
		// fire a load_history Event (HACK)
        GenericModelChangeEvent event = new GenericModelChangeEvent();
        event.detail = DemoEvents.LOAD_HISTORY;
        event.source = this.toString();
        fireModelChangeEvent(event);
	}
	
	/**
	 * this method initializes the history module component
	 */
	public void init() {
		System.out.println("loading History module...");
		System.out.println("History module ready");
	}
	
	/**
	 * this method shuts down the history
	 */
	public void shutdown() {
	}
	
	
	// -- materialization -------------------------------------------------------------------------
	
	public void save(String filename) throws FileNotFoundException, IOException {
		ObjectOutputStream out = new ObjectOutputStream(new FileOutputStream(filename));
		out.writeInt(entries.size());
		for (int i=0; i<entries.size(); i++) {
			HistoryEntry e = getEntry(i);
			e.restore();
			out.writeObject(e);
			e.store();
		}
		out.close();
	}
	
	public void load(String filename) throws FileNotFoundException, IOException, ClassNotFoundException {
		ObjectInputStream in = new ObjectInputStream(new FileInputStream(filename));
		entries.clear();
		int size = in.readInt();
		for (int i=0; i<size; i++) {
			HistoryEntry e = (HistoryEntry)in.readObject();
			add(e);			
		}
		entries.get(currentEntry).store();
		in.close();
        
		// fire a load_history Event
        GenericModelChangeEvent event = new GenericModelChangeEvent();
        event.detail = DemoEvents.LOAD_HISTORY;
        event.source = this.toString();
        fireModelChangeEvent(event);
        //System.out.println("history load event fired");

	}
	// ************************************************
	// GenericModel Stuff
	
	public void addModelChangeListener(GenericModelChangeListener listener) {
		this.listeners.add(listener);
	}

	public void removeModelChangeListener(GenericModelChangeListener listener) {
		this.listeners.remove(listener);
	}

	public void fireModelChangeEvent(GenericModelChangeEvent event) {
		for(GenericModelChangeListener listener:this.listeners){
			listener.modelChanged(event);
		}
	}

}
