package modules.timeseries;

import java.io.BufferedReader;
import java.io.FileReader;
import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.sql.Time;
import java.sql.Timestamp;
import java.util.StringTokenizer;

import modules.databaseif.JDBCInterface;

/**
 * @author Sebastian Seifert
 */
public class TimeseriesImporter {

	public static void importTimeseries(Connection con, String file, String tablename) throws Exception {
		Statement s = con.createStatement();

		boolean tableExists;
		ResultSet rs;
		if(!JDBCInterface.getInstance().isPostgres_connected())
			rs = s.executeQuery("SELECT COUNT(tablename) FROM sys.systables WHERE tablename='" + tablename.toUpperCase() + "'");
		else
			rs = s.executeQuery("SELECT COUNT(tablename) FROM pg_tables WHERE tablename='" + tablename.toLowerCase() + "'");
		rs.next();
		tableExists = rs.getInt(1) != 0;

		BufferedReader reader = new BufferedReader(new FileReader(file));

		String line = reader.readLine();
		
		String datatype = null;
		int dt = 0;
		final String derbyF_datatype_pattern = "# DerbyF TimeseriesImporter: ";
		if (line.startsWith(derbyF_datatype_pattern)) {
			datatype = line.substring(derbyF_datatype_pattern.length()).trim();

			if (datatype.equalsIgnoreCase("int"))
				dt = 0;
			else if (datatype.equalsIgnoreCase("long"))
				dt = 1;
			else if (datatype.equalsIgnoreCase("time"))
				dt = 2;
			else if (datatype.equalsIgnoreCase("date"))
				dt = 3;
//			else if (datatype.equalsIgnoreCase("timestamp"))
//				dt = 4;
			else
				throw new RuntimeException("Impot failed. Datatype not supported!");

			if (!tableExists)
				if(!JDBCInterface.getInstance().isPostgres_connected())
					s.execute("CREATE TABLE " + tablename + " (t " + datatype + ", x double)");
				else
					s.execute("CREATE TABLE " + tablename + " (t " + datatype + ", x double precision)");
			line = reader.readLine();
		} else
			if (!tableExists)
				if(!JDBCInterface.getInstance().isPostgres_connected())
					s.execute("CREATE TABLE " + tablename + " (t int, x double)");
				else
					s.execute("CREATE TABLE " + tablename + " (t int, x double precision )");

		
		PreparedStatement pstmt = con.prepareStatement("INSERT INTO " + tablename + " VALUES (?, ?)");
		if (datatype == null) {
			int time = 0;
			while (line != null) {
				if (line.startsWith("#")) {
					line = reader.readLine();
					continue;
				}

				StringTokenizer st = new StringTokenizer(line);

				while (st.hasMoreTokens()) {
					String t = st.nextToken();

					pstmt.setInt(1, time);
					pstmt.setDouble(2, Double.parseDouble(t));
					pstmt.execute();
					time++;
				}

				line = reader.readLine();
			}
		} else {
			
			while (line != null) {
				if (line.startsWith("#")) {
					line = reader.readLine();
					continue;
				}

				StringTokenizer st = new StringTokenizer(line);

				String time = st.nextToken();
				String x = st.nextToken();
				
				if (st.hasMoreTokens())
					throw new RuntimeException("Impot failed. More elements available as needed!");

				switch (dt) {
				case 0:
					pstmt.setInt(1, Integer.valueOf(time));
					break;
				case 1:
					pstmt.setLong(1, Long.valueOf(time));
					break;
				case 2:
					pstmt.setTime(1, Time.valueOf(time));
					break;
				case 3:
					pstmt.setDate(1, Date.valueOf(time));
					break;
				case 4:
					pstmt.setTimestamp(1, Timestamp.valueOf(time));
					break;
				}
				pstmt.setDouble(2, Double.parseDouble(x));
				pstmt.execute();

				line = reader.readLine();
			}
		}
	}
}
