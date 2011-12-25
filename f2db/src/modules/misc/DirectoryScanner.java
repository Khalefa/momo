package modules.misc;

import java.io.File;
import java.io.FilenameFilter;
import java.util.StringTokenizer;

/**
 * This class provides a scanner for directories.
 * 
 * @author Felix Beyer, Ulrike Fischer
 * @date 14.05.2006
 * 
 */
public class DirectoryScanner {

	private String wildCard; // the wildcard pattern

	private String directory; // the dir to scan
	
	private WildcardFilter filter; // the pattern filter
	
	
	public DirectoryScanner(String directory, String wildCard) {
		this.wildCard = wildCard;
		this.directory = directory;
		filter = new WildcardFilter();
	}

	/**
	 * Scans the specified directory and returns all files which
	 * match the wildcard pattern.
	 */
	public File[] scan() {				
		return new File(directory).listFiles(filter);
	}
	
	// filters filenames with the wildcard pattern
	private class WildcardFilter implements FilenameFilter {

		public boolean accept(File directory, String fileName) {

			if (fileName.equals(".svn"))
				return false;
			
			StringTokenizer parts = new StringTokenizer(wildCard,"*");
			int index = 0;
			String first = null, last = null;
			
			while (parts.hasMoreTokens()) {

				String part = parts.nextToken();

				if (first == null) {
					first = part;
				}
				last = part;

				if ((index = fileName.indexOf(part, index)) != -1) {
					index++;
				} else {
					return false;
				}
			}

			if ((!wildCard.startsWith("*") && !fileName.startsWith(first))
					|| (!wildCard.endsWith("*") && !fileName.endsWith(last))) {
				return false;
			}
			return true;
		}
	}
	
	// **************************************************
	// Getter & Setter methods

	public String getDirectory() {
		return directory;
	}

	public void setDirectory(String directory) {
		this.directory = directory;
	}

	public String getWildCard() {
		return wildCard;
	}

	public void setWildCard(String wildCard) {
		this.wildCard = wildCard;
	}

	//**************************************************
	// just a quick test
	public static void main(String[] args) {
		DirectoryScanner test = new DirectoryScanner(System.getProperty("user.dir") + "/build/images/","Co*.png");
		File[] files = test.scan();
		for (int i=0; i < files.length; i++) {
			System.out.println(files[i].getName());
		}
	}

}
