package DictionarySearch;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
 
/**
 *
 * @author Emmanuel Armstrong
 */


//program to take a dictionary of potential passwords, encrypt them using typical md5 encryption and decrypt an  already encrypted password
public class DictionarySearch {
	
	static File dict0to3 = new File("C:\Users\mount\Documents\School\ComputerSecurity\Assignment2\Dict0to3.txt");
	static FileWriter f0to3 = null;
	static File dict4to7 = new File("C:\Users\mount\Documents\School\ComputerSecurity\Assignment2\Dict4to7.txt");
	static FileWriter f4to7 = null;
	static File dict8toB = new File("C:\Users\mount\Documents\School\ComputerSecurity\Assignment2\/Dict8toB.txt");
	static FileWriter f8toB = null;
	static File dictCtoF = new File("C:\Users\mount\Documents\School\ComputerSecurity\Assignment2\DictCtoF.txt");
	static FileWriter fCtoF = null;
	static File passwords = new File("C:\Users\mount\Downloads\xaa");
	static long startTime;
	static MessageDigest md5 = null;
	static BufferedReader in = null;
	static String[] hashValueArray = {"6f047ccaa1ed3e8e05cde1c7ebc7d958",
			"275a5602cd91a468a0e10c226a03a39c",
			"b4ba93170358df216e8648734ac2d539",
			"dc1c6ca00763a1821c5af993e0b6f60a",
			"8cd9f1b962128bd3d3ede2f5f101f4fc",
			"554532464e066aba23aee72b95f18ba2"};
	
	public static void main(String args[]) {
		//createLookUpDict();
		for (String hashValue : hashValueArray) {
			startTime = System.nanoTime();
			try {
				if (hashValue.matches("^[0-3].*")) {
					in = new BufferedReader(new FileReader(dict0to3));
				} else if (hashValue.matches("^[4-7].*")) {
					in = new BufferedReader(new FileReader(dict4to7));
				} else if (hashValue.matches("^[89ab].*")) {
					in = new BufferedReader(new FileReader(dict8toB));
				} else {
					in = new BufferedReader(new FileReader(dictCtoF));

				}
				String psswd = "";
				while ((psswd = in.readLine()) != null) {
					matchHashValue(hashValue, psswd);
				}
			} catch (IOException e) {
				e.printStackTrace();
				System.out.println("Assignment2: main: Error opening File");
			}
		}
	}
	
	private static void matchHashValue(String value, String line) {
		Pattern pattern = Pattern.compile("([a-z0-9]{32}):(.*)");
		Matcher match = pattern.matcher(line);
		if (match.find()) {
			if (value.equals(match.group(1))) {
				long endTime = System.nanoTime() - startTime;
				System.out.println("Hash:");
				System.out.println(match.group(1));
				System.out.println("Password:");
				System.out.println(match.group(2));
				System.out.println("Time:");
				System.out.println(endTime / 1000000);
			}
		}
	}
	
	private static void createLookUpDict() {
		try {
			//input file of passwords
			md5 =  MessageDigest.getInstance("MD5");
			byte[] passwordBytes = new byte[(int)passwords.length()];
			in = new BufferedReader(new FileReader(passwords));
			//out files with hash matched to passwords
			f0to3 = new FileWriter(dict0to3);
			f4to7 = new FileWriter(dict4to7);
			f8toB = new FileWriter(dict8toB);
			fCtoF = new FileWriter(dictCtoF);
			//read, digest, then create the dictionary
			String psswd = "";
			while ((psswd = in.readLine()) != null) {
				md5.update(psswd.getBytes());
				byte[] md5Bytes = md5.digest();
				
				if (String.format("%02x", md5Bytes[0] & 0xff).matches("^[0-3].*")) {
					for (byte b : md5Bytes) {
						f0to3.append(String.format("%02x", b & 0xff));
					}
					f0to3.write(":" + psswd + "\n");
				}
				else if (String.format("%02x", md5Bytes[0] & 0xff).matches("^[4-7].*")) {
					for (byte b : md5Bytes) {
						f4to7.append(String.format("%02x", b & 0xff));
					}
					f4to7.write(":" + psswd + "\n");
				}
				else if (String.format("%02x", md5Bytes[0] & 0xff).matches("^[89ab].*")) {
					for (byte b : md5Bytes) {
						f8toB.append(String.format("%02x", b & 0xff));
					}
					f8toB.write(":" + psswd + "\n");
				} else {
					for (byte b : md5Bytes) {
						fCtoF.append(String.format("%02x", b & 0xff));
					}
					fCtoF.write(":" + psswd + "\n");
				}
			}
			f0to3.close();
			f4to7.close();
			f8toB.close();
			fCtoF.close();
		} catch (NoSuchAlgorithmException|IOException e) {
			e.printStackTrace();
			System.out.println("Assignment2: createLookUpDict: Error opening File");
}
	}
}
