package orego.shape;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.Scanner;
import java.util.StringTokenizer;

/**
 * Extracts patterns from SGF files.
 */
public class TestEvaluator {

	public static void main(String[] args) {
//		new TestEvaluator().run(orego.experiment.Debug.OREGO_ROOT_DIRECTORY + "SgfTestFiles" + File.separator);
		new TestEvaluator().run("/Network/Servers/maccsserver.lclark.edu/Users/lvonessen/Desktop/cgtc-2.0.1");
	}

	/**
	 * Takes a directory of index#.html files and walks through them, looking
	 * for the number of tests that passed
	 */
	public void run(String directory) {
		for (int i = 0; i < 70; i++) {
			try {
				File file = new File(directory + File.separator + "index" + i
						+ ".html");
				int passes = lookForPasses(file);
				System.out.println(i + " " + passes);
			} catch (Exception e) {
				System.out.println(i + " is missing");
			}
		}
	}

	/**
	 * Check for the patterns in a particular file.
	 */
	public int lookForPasses(File file) throws FileNotFoundException {
		Scanner s=null;
		s = new Scanner (file);
		String string="";
		while (s.hasNextLine()) {
			string = s.nextLine();
			if (string.contains("542")){
				break;
			}
		}
		for (int i=0; i<4; i++){
			s.nextLine();
		}
		StringTokenizer stoken = new StringTokenizer(s.nextLine(),"<>td/");
		return Integer.parseInt(stoken.nextToken());
	}
}