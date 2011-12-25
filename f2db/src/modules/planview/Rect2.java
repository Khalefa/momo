 
package modules.planview;

import java.awt.Rectangle;
import java.util.Map;

import modules.misc.PlanNode;

import com.mxgraph.canvas.mxGraphics2DCanvas;
import com.mxgraph.shape.mxBasicShape;
import com.mxgraph.util.mxConstants;
import com.mxgraph.util.mxUtils;
import com.mxgraph.view.mxCellState;
import modules.modelgraphview.PostgresModelGraphNode;

/**
 * @author Christopher Schildt
 * @date   03.09.2011
 *
 */
public class Rect2 extends mxBasicShape
{

    /**
     * 
     */
    public void paintShape(mxGraphics2DCanvas canvas, mxCellState state)
    {
        Map<String, Object> style = state.getStyle();

        if (mxUtils.isTrue(style, mxConstants.STYLE_ROUNDED, false))
        {
            Rectangle tmp = state.getRectangle();
            Object k=((com.mxgraph.model.mxCell)state.getCell()).getValue();
            PlanNode node;
            node= (PlanNode)((com.mxgraph.model.mxCell)state.getCell()).getValue();
           
 
            int x = tmp.x;
            int y = tmp.y;
            int w = tmp.width;
            int h = tmp.height;
            int radius;
            if(node instanceof PostgresModelGraphNode)
            {
                if((((PostgresModelGraphNode)node).isIsLeaf() &&(((PostgresModelGraphNode)node).isHasModel())))
                    radius=0;
                else
                   radius = getArcSize(w, h);
            }
            else
                 radius = getArcSize(w, h);

            boolean shadow = hasShadow(canvas, state);
            int shadowOffsetX = (shadow) ? mxConstants.SHADOW_OFFSETX : 0;
            int shadowOffsetY = (shadow) ? mxConstants.SHADOW_OFFSETY : 0;

            if (canvas.getGraphics().hitClip(x, y, w + shadowOffsetX,
                    h + shadowOffsetY))
            {
                // Paints the optional shadow
                if (shadow)
                {
                    canvas.getGraphics().setColor(mxConstants.SHADOW_COLOR);
                    canvas.getGraphics().fillRoundRect(
                            x + mxConstants.SHADOW_OFFSETX,
                            y + mxConstants.SHADOW_OFFSETY, w, h, radius,
                            radius);
                }

                // Paints the background
                if (configureGraphics(canvas, state, true))
                {
                	if(!node.isColl()){
                    canvas.getGraphics().setColor(NodeStyles.getHeadColor(node.getType(false)));
                    canvas.getGraphics().fillRoundRect(x, y, w, h, radius,
                            radius);

                	}
                	else
                	{
                		 canvas.getGraphics().setColor(NodeStyles.getHeadColor(node.getType(false)));
                         canvas.getGraphics().fillRoundRect(x, y, w, h, radius,
                                 radius);
                	}

                }

                // Paints the foreground
                if (configureGraphics(canvas, state, false))
                {
                    canvas.getGraphics().drawRoundRect(x, y, w, h, radius,
                            radius);
                }
            }
        }
        else
        {
            Rectangle rect = state.getRectangle();

            // Paints the background
            if (configureGraphics(canvas, state, true))
            {
                canvas.fillShape(rect, hasShadow(canvas, state));
            }

            // Paints the foreground
            if (configureGraphics(canvas, state, false))
            {
                canvas.getGraphics().drawRect(rect.x, rect.y, rect.width,
                        rect.height);
            }
        }
    }

    /**
     * Computes the arc size for the given dimension.
     * 
     * @param w Width of the rectangle.
     * @param h Height of the rectangle.
     * @return Returns the arc size for the given dimension.
     */
    public int getArcSize(int w, int h)
    {
        int arcSize;

        if (w <= h)
        {
            arcSize = (int) Math.round(h
                    * mxConstants.RECTANGLE_ROUNDING_FACTOR);

            if (arcSize > (w / 2))
            {
                arcSize = w / 2;
            }
        }
        else
        {
            arcSize = (int) Math.round(w
                    * mxConstants.RECTANGLE_ROUNDING_FACTOR);

            if (arcSize > (h / 2))
            {
                arcSize = h / 2;
            }
        }
        return arcSize;
    }

}