package modules.planview;

import java.awt.Color;

/**
 * @author Christopher Schildt
 * @date   03.09.2011
 *
 */
public class NodeStyles {

	public static Color getBodyColor(String x) {
		if (x.contains("Result"))
			return new Color(230, 200, 200);
		if (x.equals("Append"))
			return new Color(230, 200, 200);
		if (x.equals("Recursive Union"))
			return new Color(230, 200, 200);
		if (x.equals("BitmapAnd"))
			return new Color(230, 200, 200);
		if (x.equals("BitmapOr"))
			return new Color(230, 200, 200);
		if (x.equals("SeqScan"))
			return new Color(100, 220, 120);
		if (x.equals("Index Scan"))
			return new Color(230, 200, 200);
		if (x.equals("Bitmap Index Scan"))
			return new Color(230, 200, 200);
		if (x.equals("Bitmap Heap Scan"))
			return new Color(230, 200, 200);
		if (x.equals("Tid Scan"))
			return new Color(230, 200, 200);
		if (x.equals("Subquery Scan"))
			return new Color(230, 200, 200);
		if (x.equals("Function Scan"))
			return new Color(230, 200, 200);
		if (x.equals("Values Scan"))
			return new Color(230, 200, 200);
		if (x.equals("Cte Scan"))
			return new Color(230, 200, 200);
		if (x.equals("WorkTable Scan"))
			return new Color(230, 200, 200);
		if (x.equals("Nested Loop Left Join"))
			return new Color(230, 200, 200);
		if (x.equals("Nested Loop Right Join"))
			return new Color(230, 200, 200);
		if (x.equals("Nested Loop Semi Join"))
			return new Color(230, 200, 200);
		if (x.equals("Nested Loop Anti Join"))
			return new Color(230, 200, 200);
		if (x.equals("Nested Loop ??? Join"))
			return new Color(230, 200, 200);
		if (x.equals("Nested Loop Full Join"))
			return new Color(230, 200, 200);
		if (x.equals("Nested Loop"))
			return new Color(230, 200, 200);
		if (x.equals("Merge Left Join"))
			return new Color(230, 200, 200);
		if (x.equals("Merge Right Join"))
			return new Color(230, 200, 200);
		if (x.equals("Merge Full Join"))
			return new Color(230, 200, 200);
		if (x.equals("Merge Semi Join"))
			return new Color(230, 200, 200);
		if (x.equals("Merge Anti Join"))
			return new Color(230, 200, 200);
		if (x.equals("Merge ??? Join"))
			return new Color(230, 200, 200);
		if (x.equals("Merge Join"))
			return new Color(230, 200, 200);
		if (x.equals("Hash Left Join"))
			return new Color(230, 200, 200);
		if (x.equals("Hash Full Join"))
			return new Color(230, 200, 200);
		if (x.equals("Hash Right Join"))
			return new Color(230, 200, 200);
		if (x.equals("Hash Semi Join"))
			return new Color(230, 200, 200);
		if (x.equals("Hash Anti Join"))
			return new Color(230, 200, 200);
		if (x.equals("Hash ??? Join"))
			return new Color(230, 200, 200);
		if (x.equals("Hash Join"))
			return new Color(230, 200, 200);
		if (x.equals("Materialize"))
			return new Color(230, 200, 200);
		if (x.equals("Sort"))
			return new Color(230, 200, 200);
		if (x.equals("GroupAggregate"))
			return new Color(230, 200, 200);
		if (x.equals("Group"))
			return new Color(230, 200, 200);
		if (x.equals("HashAggregate"))
			return new Color(230, 200, 200);
		if (x.equals("Aggregate ???"))
			return new Color(230, 200, 200);
		if (x.equals("Aggregate"))
			return new Color(230, 200, 200);
		if (x.equals("WindowAgg"))
			return new Color(230, 200, 200);
		if (x.equals("Unique"))
			return new Color(230, 200, 200);
		if (x.equals("Hash"))
			return new Color(230, 200, 200);
		if (x.equals("HashSetOp Intersect All"))
			return new Color(230, 200, 200);
		if (x.equals("HashSetOp Intersect"))
			return new Color(230, 200, 200);
		if (x.equals("HashSetOp Except All"))
			return new Color(230, 200, 200);
		if (x.equals("HashSetOp Except"))
			return new Color(230, 200, 200);
		if (x.equals("HashSetOp ???"))
			return new Color(230, 200, 200);
		if (x.equals("SetOp Intersect All"))
			return new Color(230, 200, 200);
		if (x.equals("SetOp Intersect"))
			return new Color(230, 200, 200);
		if (x.equals("SetOp Except All"))
			return new Color(230, 200, 200);
		if (x.equals("SetOp Except"))
			return new Color(230, 200, 200);
		if (x.equals("SetOp ???"))
			return new Color(230, 200, 200);
		if (x.equals("Limit"))
			return new Color(230, 200, 200);
		if (x.equals("CreateModel + Forecast"))
			return new Color(230, 200, 200);
		if (x.equals("Forecast"))
			return new Color(230, 200, 200);
                if (x.equals("Modelinfo"))
			return new Color(230, 200, 200);
		if (x.equals("Decompose Additiv"))
			return new Color(230, 200, 200);
		if (x.equals("Create ForecastModel"))
			return new Color(230, 200, 200);
		if (x.equals("Scantarget"))
                    return new  Color(100, 220, 120);
		if(x.equals("ModelGraphNodeFilledModel"))
                     return new Color(0, 255, 0);
                return new Color(230, 0, 0);
	}
	public static Color getHeadColor(String x) {
		if (x.contains("Result"))
			return new Color(230, 220, 220);
		if (x.equals("Append"))
			return new Color(230, 220, 220);
		if (x.equals("Recursive Union"))
			return new Color(230, 220, 220);
		if (x.equals("BitmapAnd"))
			return new Color(230, 220, 220);
		if (x.equals("BitmapOr"))
			return new Color(230, 220, 220);
		if (x.equals("SeqScan"))
			return new Color(100, 200, 100);
		if (x.equals("Index Scan"))
			return new Color(230, 220, 220);
		if (x.equals("Bitmap Index Scan"))
			return new Color(230, 220, 220);
		if (x.equals("Bitmap Heap Scan"))
			return new Color(230, 220, 220);
		if (x.equals("Tid Scan"))
			return new Color(230, 220, 220);
		if (x.equals("Subquery Scan"))
			return new Color(230, 220, 220);
		if (x.equals("Function Scan"))
			return new Color(230, 220, 220);
		if (x.equals("Values Scan"))
			return new Color(230, 220, 220);
		if (x.equals("Cte Scan"))
			return new Color(230, 220, 220);
		if (x.equals("WorkTable Scan"))
			return new Color(230, 220, 220);
		if (x.equals("Nested Loop Left Join"))
			return new Color(230, 220, 220);
		if (x.equals("Nested Loop Right Join"))
			return new Color(230, 220, 220);
		if (x.equals("Nested Loop Semi Join"))
			return new Color(230, 220, 220);
		if (x.equals("Nested Loop Anti Join"))
			return new Color(230, 220, 220);
		if (x.equals("Nested Loop ??? Join"))
			return new Color(230, 220, 220);
		if (x.equals("Nested Loop Full Join"))
			return new Color(230, 220, 220);
		if (x.equals("Nested Loop"))
			return new Color(230, 220, 220);
		if (x.equals("Merge Left Join"))
			return new Color(230, 220, 220);
		if (x.equals("Merge Right Join"))
			return new Color(230, 220, 220);
		if (x.equals("Merge Full Join"))
			return new Color(230, 220, 220);
		if (x.equals("Merge Semi Join"))
			return new Color(230, 220, 220);
		if (x.equals("Merge Anti Join"))
			return new Color(230, 220, 220);
		if (x.equals("Merge ??? Join"))
			return new Color(230, 220, 220);
		if (x.equals("Merge Join"))
			return new Color(230, 220, 220);
		if (x.equals("Hash Left Join"))
			return new Color(230, 220, 220);
		if (x.equals("Hash Full Join"))
			return new Color(230, 220, 220);
		if (x.equals("Hash Right Join"))
			return new Color(230, 220, 220);
		if (x.equals("Hash Semi Join"))
			return new Color(230, 220, 220);
		if (x.equals("Hash Anti Join"))
			return new Color(230, 220, 220);
		if (x.equals("Hash ??? Join"))
			return new Color(230, 220, 220);
		if (x.equals("Hash Join"))
			return new Color(230, 220, 220);
		if (x.equals("Materialize"))
			return new Color(230, 220, 220);
		if (x.equals("Sort"))
			return new Color(230, 220, 220);
		if (x.equals("GroupAggregate"))
			return new Color(230, 220, 220);
		if (x.equals("Group"))
			return new Color(230, 220, 220);
		if (x.equals("HashAggregate"))
			return new Color(230, 220, 220);
		if (x.equals("Aggregate ???"))
			return new Color(230, 220, 220);
		if (x.equals("Aggregate"))
			return new Color(230, 220, 220);
		if (x.equals("WindowAgg"))
			return new Color(230, 220, 220);
		if (x.equals("Unique"))
			return new Color(230, 220, 220);
		if (x.equals("Hash"))
			return new Color(230, 220, 220);
		if (x.equals("HashSetOp Intersect All"))
			return new Color(230, 220, 220);
		if (x.equals("HashSetOp Intersect"))
			return new Color(230, 220, 220);
		if (x.equals("HashSetOp Except All"))
			return new Color(230, 220, 220);
		if (x.equals("HashSetOp Except"))
			return new Color(230, 220, 220);
		if (x.equals("HashSetOp ???"))
			return new Color(230, 220, 220);
		if (x.equals("SetOp Intersect All"))
			return new Color(230, 220, 220);
		if (x.equals("SetOp Intersect"))
			return new Color(230, 220, 220);
		if (x.equals("SetOp Except All"))
			return new Color(230, 220, 220);
		if (x.equals("SetOp Except"))
			return new Color(230, 220, 220);
		if (x.equals("SetOp ???"))
			return new Color(230, 220, 220);
		if (x.equals("Limit"))
			return new Color(230, 220, 220);
		if (x.equals("CreateModel + Forecast"))
			return new Color(230, 220, 220);
                if (x.equals("Modelinfo"))
			return new Color(240, 230, 230);
		if (x.equals("Forecast"))
			return new Color(230, 220, 220);
		if (x.equals("Decompose Additiv"))
			return new Color(230, 220, 220);
		if (x.equals("Create ForecastModel"))
			return new Color(230, 220, 220);
		if (x.equals("Scantarget"))
			return new Color(100, 200, 100);
		if(x.equals("ModelGraphNodeFilledModel"))
                     return new Color(103,186, 84);
                if(x.equals("ModelGraphNodeFilledDisagg"))
                     return new Color(94,110, 191);
                if(x.equals("ModelGraphNodeEmpty"))
                     return new Color(150,150, 150);
                if(x.equals("ModelGraphNodeEmptyLeaf"))
                     return new Color(0,0, 0,0);
                return new Color(230, 0, 0);
	}

}
