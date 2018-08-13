package DumpReader;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 *
 * @author Emmanuel Armstrong
 */
 
//program to read a simulated memory dump from a credit card reader and pull the card information out of the dump.
public class DumpReader {
	
	static File dumpFile = new File("C:\Users\mount\Documents\School\ComputerSecurity\Assignment1\memorydump.dmp");
	static FileInputStream fis = null;
	static byte[] charsOfFile = new byte[(int)dumpFile.length()]; 
	
	public static void main(String args[]){	
		
		try {
			byte[] bytesOfFile = new byte[(int)dumpFile.length()];
			fis = new FileInputStream(dumpFile);
			fis.read(bytesOfFile);
			int j = 0;
			
			for (int i = 0; i < bytesOfFile.length; i++) {
				int b = bytesOfFile[i];
				if (b >= 37 && b <= 123) {
					charsOfFile[j] = bytesOfFile[i];
					j++;
				}
			}
			fis.close();
			validateTracks();
			
		} catch (IOException e) {
			System.out.println("Assignment1: main: Error opening file");
		}
	}
	
	private static void validateTracks() {
		int currentByte;

		boolean start1 = false;
		String track1 = "";

		boolean start2 = false;
		String track2 = "";

		Pattern pattern1 = Pattern.compile("^%B([\\d]{13,19})\\^([a-zA-Z]{2,26})/([a-zA-Z]{2,26})\\^([\\d]{5,})\\?");
		Pattern pattern2 = Pattern.compile("^;([\\d]{13,19})=([\\d]{12,})\\?");

		for (int i = 0; i < charsOfFile.length; i++) {
			currentByte = charsOfFile[i];
			// track 1
			if (start1 || currentByte == 37) {

				start1 = true;
				track1 += (char) currentByte;

				if (currentByte == 63) {
					start1 = false;
				} else if (currentByte == 37 && track1.length() > 2) {
					track1 = "";
					start1 = true;
					track1 += (char) currentByte;
				}
			}

			// track 2
			if (start2 || currentByte == 59) {

				start2 = true;
				track2 += (char) currentByte;

				if (currentByte == 63) {
					start2 = false;
				} else if (currentByte == 59 && track2.length() > 2) {
					track2 = "";
					start2 = true;
					track2 += (char) currentByte;
				}
			}
			// final tracks
			if (track1.length() > 12 || track2.length() > 12) {
				Matcher matcher1 = pattern1.matcher(track1);
				Matcher matcher2 = pattern2.matcher(track2);
				if (matcher1.find() && matcher2.find()) {
					System.out.println(matcher1.group(1));
					System.out.println(matcher1.group(2));
					System.out.println(matcher1.group(3));
					System.out.println(matcher1.group(4));
					System.out.println(matcher2.group(1));
					System.out.println(matcher2.group(2));
					track1 = "";
					start1 = false;
					track2 = "";
					start2 = false;
				}
			}
		}

	}
}
