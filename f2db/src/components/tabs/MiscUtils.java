package components.tabs;

import modules.config.Configuration;
import modules.misc.ResourceRegistry;

import org.eclipse.swt.custom.StyleRange;

public class MiscUtils {
    public static  StyleRange loadStyleRangeFromConfiguration(String key){
        
        StyleRange sr = new StyleRange();
        
        sr.foreground = ResourceRegistry.getInstance().getColor(
                Configuration.getInstance().getProperty(key+".fgcolor"));
        sr.background = ResourceRegistry.getInstance().getColor(
                Configuration.getInstance().getProperty(key+".bgcolor"));
        sr.font = ResourceRegistry.getInstance().getFont(
                Configuration.getInstance().getProperty(key+".font"));
        return sr;
    }
}
