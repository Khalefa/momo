package modules.misc;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import modules.Module;
import modules.config.Configuration;
import modules.databaseif.History;
import modules.databaseif.JDBCInterface;
import modules.databaseif.ScriptModule;
/**
 * This is the Module Registry. It is the repository for all the different
 * application modules. When a module gets requested by calling the getInstance()
 * method of the module and if the module gets first requested, the constructor 
 * will be called and this should lookup the Module Registry and register itself there. 
 * After this the application can just call a global shutdown or a global reset by just 
 * calling shutdown or reset of the Module Registry. 
 * Another handy application of this class is the following. creating an instance
 * of this class and calling init of the module registry, triggers a set of getInstance() 
 * methods for different system modules. The developer can then be sure, that these
 * modules have been created and initialized.
 * @author Felix Beyer
 * @date   05.10.2006
 *
 */
public class ModuleRegistry {

	/** hold the list of modules */
	private List<Module> modules;
	
	/** the singleton reference */
	private static ModuleRegistry singleton;
	
	/** a simple private constructor */
	private ModuleRegistry(){
		modules = new ArrayList<Module>();
	}
	/** returns the single instance of this class */
	public static ModuleRegistry getInstance(){
		if(singleton==null){
			singleton = new ModuleRegistry();
		}
		return singleton;
	}
	/** this method inits several modules */
	public void init(){
		System.out.println("initialize modules:");
		JDBCInterface.getInstance().init();
		Configuration.getInstance().init();
		ScriptModule.getInstance().init();
        History.getInstance().init();
		System.out.println("all modules loaded!");
	}
	
	/** this is the method, every single module has to call to register itself */
	public static void addModule(Module mod){
	   getInstance().modules.add(mod);
	}
	
	/** this method shutsdown all registered modules */
	public void shutdown(){
	    System.out.println("shutting down modules");
        Iterator iter = modules.iterator();
        while(iter.hasNext()){
			((Module)iter.next()).shutdown();
		}
        System.out.println("all modules successfully shut down");
	}
	
	/** this method resets all registered modules */
	public void reset(){
		Iterator iter = modules.iterator();
		while(iter.hasNext()){
			((Module)iter.next()).reset();
		}
		
	}
}
