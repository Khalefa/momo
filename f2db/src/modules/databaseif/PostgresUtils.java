package modules.databaseif;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;

import data.type.BooleanValue;
import data.type.DateValue;
import data.type.DoubleValue;
import data.type.GenericTuple;
import data.type.IntegerValue;
import data.type.LongValue;
import data.type.ShortValue;
import data.type.StringValue;
import data.type.Tuple;
import data.type.Value;

/*
 * @author Christopher Schildt
 * @date   03.09.2011
 *
 */
public class PostgresUtils {
	
	public static Tuple createTuple(ResultSet rs)
	{
		Tuple result = new GenericTuple();
		int i=0;
		try {
			for(i=0;i<rs.getMetaData().getColumnCount();i++)
			{
				int type= rs.getMetaData().getColumnType(i+1);
				Value resultValue=null;
				switch(type)
				{
				case Types.CHAR:
				case Types.LONGNVARCHAR:
				case Types.LONGVARCHAR:
				case Types.NVARCHAR:
				case Types.VARCHAR:
				case Types.SQLXML:
				{
					resultValue=new StringValue(rs.getString(i+1));
					break;
				}
				case Types.DATE:
				{
					resultValue=new DateValue(rs.getDate(i+1));
					break;
				}
				case Types.BOOLEAN:
				case Types.BIT:
				{
					resultValue=new BooleanValue(rs.getBoolean(i+1));
					break;
				}
				case Types.DOUBLE:
				{
					resultValue=new DoubleValue(rs.getDouble(i+1));
					break;
				}
				case Types.INTEGER:
				{
					resultValue=new IntegerValue(rs.getInt(i+1));
					break;
				}
				case Types.BIGINT:
				{
					resultValue=new LongValue(rs.getLong(i+1));
					break;
				}
				case Types.SMALLINT:
				{
					resultValue=new ShortValue(rs.getShort(i+1));
					break;
				}
				default:
				{
					resultValue=new StringValue(rs.getString(i+1));
				}
				
					
				}

				result.addValue(resultValue);
			}
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return result;
		

	}

}
