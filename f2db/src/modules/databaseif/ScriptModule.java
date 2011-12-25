package modules.databaseif;

import java.io.File;
import java.io.FileFilter;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import modules.Module;
import modules.misc.Constants;
import modules.misc.DirectoryScanner;
import modules.misc.ModuleRegistry;

/**
 * This class provides a module dealing with SQL Script functionality.
 * 
 * @author Ulrike Fischer, Felix Beyer
 */
public class ScriptModule implements Module {

    // the singleton instance
    private static ScriptModule singleton;
    // an array of the files scanned
    private File[] scriptFiles;
    private Map<String, File[]> scriptFiles2;
 private Map<String, File[]> scriptFiles2_fcpart;
    private File[] timeseriesFiles;

    /**
     * private constructor
     */
    private ScriptModule() {
    }

    /**
     * @return Instance of a ScriptModule
     */
    public static ScriptModule getInstance() {
        if (singleton == null) {
            singleton = new ScriptModule();
            ModuleRegistry.addModule(singleton);
        }
        return singleton;
    }

    /**
     * Scans the script directory and stores all script files
     * in an array.
     */
    /*public void init() {
    System.out.println("loading Script module...");
    System.out.print("check for script files...");
    scriptFiles = new DirectoryScanner(Constants.script_directory,Constants.singleStmtWildcardFilter).scan();
    if (scriptFiles == null)
    scriptFiles = new File[0];
    timeseriesFiles = new DirectoryScanner(Constants.timeseries_directory,"*").scan();
    if (timeseriesFiles == null)
    timeseriesFiles = new File[0];
    System.out.println("done");
    System.out.println("Script module ready");
    }*/
    public void init() {
        System.out.println("loading script module...");
        System.out.print("check for script files...");
        if (scriptFiles == null) {
            scriptFiles2 = new HashMap<String, File[]>();
	    scriptFiles2_fcpart= new HashMap<String, File[]>();
        }
        scriptFiles2.put("General", new DirectoryScanner(Constants.script_directory, Constants.singleStmtWildcardFilter).scan());
        scriptFiles2_fcpart.put("General", new DirectoryScanner(Constants.script_directory, Constants.singleFCStmtWildcardFilter).scan());

        File dir = new File(Constants.script_directory);
        FileFilter fileFilter = new FileFilter() {

            public boolean accept(File file) {
            	if (file.getName().equals(".svn"))
            		return false;
                return file.isDirectory();
            }
        };
        System.out.println(dir.getAbsolutePath());
        for (File a : dir.listFiles(fileFilter)) {
            scriptFiles2.put(a.getName(), new DirectoryScanner(Constants.script_directory + a.getName() + "/", Constants.singleStmtWildcardFilter).scan());
            scriptFiles2_fcpart.put(a.getName(), new DirectoryScanner(Constants.script_directory + a.getName() + "/",Constants.singleFCStmtWildcardFilter).scan());
        }

        timeseriesFiles = new DirectoryScanner(Constants.timeseries_directory, "*").scan();
        if (timeseriesFiles == null) {
            timeseriesFiles = new File[0];
        }
        System.out.println("done");
        System.out.println("script module ready");
    }

    public void shutdown() {
        //TODO Auto-generated method stub
    }

    public void reset() {
        //TODO Auto-generated method stub
    }

    public Map<String, File[]> getScriptFiles() {
        return scriptFiles2;
    }

 public Map<String, File[]> getFCScriptFiles() {
        return scriptFiles2_fcpart;
    }

    public File[] getTimeseriesFiles() {
        return timeseriesFiles;
    }

    /**
     * saves a script under a certain filename
     * @param name the filename
     * @param content the content of the file (the sql script)
     */
    public void saveScriptFile(String name, String content) {
        File f = new File(name);
        int idx;
            idx=name.lastIndexOf("/");
        if(idx<0)
            idx=name.lastIndexOf("\\");
        String dir = name.substring(name.indexOf("sql-scripts")+11, idx);
        if(dir.equals("")) dir="General";
        try {
            FileOutputStream out = new FileOutputStream(f);
            byte[] b = content.getBytes();
            out.write(b);
            out.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        if (scriptFiles2.containsKey(dir)) {
            int size = scriptFiles2.get(dir).length;
            File[] scriptFiles3 = new File[size + 1];
            scriptFiles3[0] = f;
            for (int i = 0; i < size; i++) {
                scriptFiles3[i + 1] = scriptFiles2.get(dir)[i];
            }
            this.scriptFiles2.put(dir, scriptFiles3);
        } else {
            File[] scriptFiles3 = new File[1];
            scriptFiles3[0] = f;
        this.scriptFiles2.put(dir, scriptFiles3);
        }
        
    }
}
