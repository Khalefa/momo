package modules.misc;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Properties;

import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Device;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Widget;
/**
 * This class stores all resource objects for the application.<br> 
 * It provides convenient methods to get access to System Colors, 
 * user defined colors, user defined images, system images, fonts 
 * and other resources. <br>
 * 
 * @author Felix Beyer
 * @date   10.04.2006
 *
 */
public class ResourceRegistry {
	
	// default image mapping file
	private static String data = "./properties/images.map.properties";
	
    // the singleton instance
	private static ResourceRegistry singleton;
	
    // the different maps, storing the resources
	private Map<String,Image> imageMap; // the image map
	private Map<String,Font> fontMap; // the font map
	private Map<String,Color> colorMap; // the color map
    
	/** private constructor */ 
	private ResourceRegistry(){
	    // new image Map
		this.imageMap = new HashMap<String,Image>(); 
		this.fontMap  = new HashMap<String,Font>();
		this.colorMap = new HashMap<String,Color>();
		
        // create app images
        createAppImages();
	}
	
    /**
	 * @return the ResourceRegistry singleton instance
	 */
	public static ResourceRegistry getInstance(){
		if(singleton == null) {
			singleton = new ResourceRegistry();
            System.out.println("ResourceRegistry created...");
		}
		return singleton;
	}
	
    /** load the images from the image properties file */
	public void loadImages(Display dis){
	    
		// load properties file
		Properties props = new Properties();
	    try {
			props.load(new FileInputStream(data));
		} catch (FileNotFoundException e) {
			System.out.println("Sorry, didn`t found file: "+data);
			e.printStackTrace();
		} catch (IOException e) {
			System.out.println("An IOException occured during loading the Image Map File.");
			e.printStackTrace();
		}
		
		// load images from image map properties file right into imageMap
		Iterator iter = props.keySet().iterator();
		String name;
		String value;
		Image img;
		System.out.println("loading images from "+data+" and cache them");
		while (iter.hasNext()){
			name  = (String)iter.next();
			value = props.getProperty(name);
			img = new Image(dis,value);
			imageMap.put(name,img);
			System.out.print(".");
		}
		System.out.println("done");
	}
	
	/** get image from unique name */
	public Image getImage(String name){
		return (Image)imageMap.get(name);
	}
	
    /** get an image, filled with one color of size width x height*/
    public Image getImage (Widget widget, Color color, int width, int height){
        String key = color.toString()+"|"+width+"|"+height;
        if (imageMap.containsKey(key)){
            return (Image)imageMap.get(key);
        } else {
            
            Image i = new Image(Display.getDefault(),15,10);
            GC gc = new GC(i);
            gc.setForeground(getColor(0,0,0));
            gc.setBackground(color);
            gc.fillRectangle(i.getBounds());
            gc.dispose();
            imageMap.put(key,i);
            return i;
            
        }
    }
    /** this method creates app images, which aren`t parsed in as a resource*/
    private void createAppImages(){

        // create a null color image, looks like this:
        // ---
        // |\|
        // ---
        
        String key = "nullColor";
        Image i = new Image(Display.getDefault(),15,10);
        GC gc = new GC(i);
        gc.setForeground(getColor(0,0,0));
        gc.setBackground(getColor(255,255,255));
        gc.fillRectangle(i.getBounds());
        gc.setForeground(getColor(255,0,0));
        gc.drawLine(0,0,i.getBounds().width,i.getBounds().height);
        gc.dispose();
        imageMap.put(key,i);

    }
    
    /** get a font for preview on the button */
    public Font getPreviewFont10pt(String identifier){
        Font font = (Font)fontMap.get(identifier);
        // get font data from font
        FontData[] data = font.getFontData();
        // set font size to 10
        data[0].setHeight(10);        
        // create new font from fontdata
        Font font2 = new Font(Display.getDefault(),data);
        // put it into the map to dispose it at the end
        fontMap.put(font2.toString(),font2);
        
        return font2;
        
    }
    
    /** get a font from an identifier string*/
    public Font getFont(String identifier){
        if(fontMap.containsKey(identifier)){
            return (Font)fontMap.get(identifier);
        } else {
            if (identifier!=null){
                Font font = new Font(Display.getDefault(),
                                     new FontData(identifier));
                fontMap.put(identifier,font);
                return font;
            } else {
                return null;
            }
        }
    }
    
    /** get a font */
    public Font getFont(Device device, String identifier){
        if(fontMap.containsKey(identifier)){
            return (Font)fontMap.get(identifier);
        } else {
            Font font = new Font(device,new FontData(identifier));
            fontMap.put(identifier,font);
            return font;
        }
    }
    
	/** get a font */
	public Font getFont(Device device, String name, int height, int style){
		String key = name+height+"-"+style;
		if(this.fontMap.containsKey(key)){
			return (Font)this.fontMap.get(key);
		} else {
			Font font = new Font(device,name,height,style);
            this.fontMap.put(key,font);
			return font;
		}
	}
    
    /** get a font using the default display*/
    public Font getFont(String name, int height, int style){
        String key = name+height+"-"+style;
        if(this.fontMap.containsKey(key)){
            return (Font)this.fontMap.get(key);
        } else {
            Font font = new Font(Display.getDefault() , name, height, style);
            this.fontMap.put(key,font);
            return font;
        }
    }
    
    /** get a color, specified through the values of R, G and B using the default display */
    public Color getColor(int red, int green, int blue){
        String key = red+"-"+green+"-"+blue;
        if(colorMap.containsKey(key)){
            return (Color)this.colorMap.get(key);
        } else {
            Color color = new Color(Display.getDefault(),red,green,blue);
            colorMap.put(key,color);
            return color;
        }
    }

    /** get a color, specified through a color string of the form RRR,GGG,BBB*/
    public Color getColor(String colorString){
        if (colorString==null || colorString.equalsIgnoreCase("null")) return null;
        int[] rgb = new int[3];
        
        rgb[0] = Integer.parseInt(colorString.substring(0,3));
        rgb[1] = Integer.parseInt(colorString.substring(4,7));
        rgb[2] = Integer.parseInt(colorString.substring(8,11));
        
        String key = rgb[0]+"-"+rgb[1]+"-"+rgb[2];
        if(this.colorMap.containsKey(key)){
            return (Color)colorMap.get(key);
        } else {
            Color color = new Color(Display.getDefault(),rgb[0],rgb[1],rgb[2]);
            colorMap.put(key,color);
            return color;
        }
    }
    
    /** get a color, specified through an triple int array holding the values of R, G and B and using the default display. */
    public Color getColor(int[] rgb){
        String key = rgb[0]+"-"+rgb[1]+"-"+rgb[2];
        if(this.colorMap.containsKey(key)){
            return (Color)colorMap.get(key);
        } else {
            Color color = new Color(Display.getDefault(),rgb[0],rgb[1],rgb[2]);
            colorMap.put(key,color);
            return color;
        }
    }
    
    /** get a color, specified through the values of R, G and B. */
	public Color getColor(Device device, int red, int green, int blue){
		String key = red+"-"+green+"-"+blue;
		if(colorMap.containsKey(key)){
			return (Color)this.colorMap.get(key);
		} else {
			Color color = new Color(device,red,green,blue);
			colorMap.put(key,color);
			return color;
		}
	}

    /** get a color, specified through an triple int array holding the values of R, G and B. */
	public Color getColor(Device device, int[] rgb){
		String key = rgb[0]+"-"+rgb[1]+"-"+rgb[2];
		if(colorMap.containsKey(key)){
			return (Color)this.colorMap.get(key);
		} else {
			Color color = new Color(device,rgb[0],rgb[1],rgb[2]);
			colorMap.put(key,color);
			return color;
		}
	}
	/** help function to fill up Strings with leading 0`s */
    private String triplet(String code){
        if (code!=null){
            int length = code.length();
            if (length==3) {
                return code;
            } else {
                String newCode="";
                    switch(length){
                        case 0 : newCode="000"; break;
                        case 1 : newCode="00"+code; break;
                        case 2 : newCode="0"+code;break;
                    }
                return newCode;
            }
        } 
        return null;
    }
    /** convert an rgb object to a color code, namely: "RRR,GGG,BBB" */ 
    public String convertRGBtoColorCode(RGB rgb){
        String red   = Integer.toString(rgb.red);
        String green = Integer.toString(rgb.green);
        String blue  = Integer.toString(rgb.blue);
        red   = triplet(red);
        green = triplet(green);
        blue  = triplet(blue);
        
        return red+","+green+","+blue;
    }
    
    /** dispose all kept resources */
	public void disposeAll(){
		System.out.print("Disposing system resources kept by the ResourceRegistry...");
        disposeImages();
		disposeColors();
		disposeFonts();
        System.out.println("done");
	}
	
    /** dispose all created colors */
	private void disposeColors(){
		Iterator iter = colorMap.keySet().iterator();
		String name;
		Color value;
		while (iter.hasNext()){
			name  = (String)iter.next();
			value = (Color)colorMap.get(name); 
			value.dispose();
		}
	}
    
    /** dispose all created fonts */
	private void disposeFonts(){
		Iterator iter = fontMap.keySet().iterator();
		String name;
		Font value;
		while (iter.hasNext()){
			name  = (String)iter.next();
			value = (Font)fontMap.get(name); 
			value.dispose();
		}
	}

    /** dispose all created images */
	private void disposeImages(){
		Iterator iter = imageMap.keySet().iterator();
		String name;
		Image value;
		while (iter.hasNext()){
			name  = (String)iter.next();
			value = (Image)imageMap.get(name); 
			value.dispose();
		}

	}
	
}
