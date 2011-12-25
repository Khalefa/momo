package components.listeners;

import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
/**
 * This generic Resize Listener is a customizable Background Listener 
 * @author Felix Beyer
 * @date   19.11.2006
 */
public class GenericResizeListener implements Listener{
    
    public Composite parent;
    public String type;
    public String gradienttype;
    public Image oldImage;
    public Color bgColor;
    public Color one;
    public Color two;
    public boolean useImage = false;
    public Image decorateImage;
   
    public void setup(){
        handleEvent(null);
    }
    
    public void handleEvent(Event event) {
                if(type.equals("gradient")){
                    Rectangle rect = parent.getClientArea ();
                    Image newImage = new Image (Display.getDefault(), Math.max (1, rect.width), Math.max (1, rect.height));
                    GC gc = new GC (newImage);
                    if(gradienttype.equals("topdown")){
                        gc.setForeground (one);
                        gc.setBackground (two);
                        gc.fillGradientRectangle (rect.x, rect.y, rect.width, rect.height, true);
                    } else 
                    if(gradienttype.equals("bottomup")){
                        gc.setForeground (two);
                        gc.setBackground (one);
                        gc.fillGradientRectangle (rect.x, rect.y, rect.width, rect.height, true);
                    } else 
                    if(gradienttype.equals("leftright")){
                        gc.setForeground (one);
                        gc.setBackground (two);
                        gc.fillGradientRectangle (rect.x, rect.y, rect.width, rect.height, false);
                    } else 
                    if(gradienttype.equals("rightleft")){
                        gc.setForeground (two);
                        gc.setBackground (one);
                        gc.fillGradientRectangle (rect.x, rect.y, rect.width, rect.height, false);
                    } 
                    if (type.equals("solid")) {
                        gc.setForeground(bgColor);
                        gc.fillRectangle (rect.x, rect.y, rect.width, rect.height);
                    }
                    gc.dispose ();
                    parent.setBackgroundImage (newImage);
                    if (oldImage != null) oldImage.dispose ();
                    oldImage = newImage;
                } else 
                if(type.equalsIgnoreCase("solid")){
                    Rectangle rect = parent.getClientArea ();
                    Image newImage = new Image (Display.getDefault(), Math.max (1, rect.width), Math.max (1, rect.height));
                    GC gc = new GC (newImage);
                    gc.setBackground (bgColor);
                    gc.fillRectangle (rect.x, rect.y, rect.width, rect.height);
                    gc.dispose ();
                    parent.setBackgroundImage (newImage);
                    if (oldImage != null) oldImage.dispose ();
                    oldImage = newImage;
                } else 
                if(type.equalsIgnoreCase("blank")){   
                    if (oldImage != null) oldImage.dispose ();
                }
     }

}
