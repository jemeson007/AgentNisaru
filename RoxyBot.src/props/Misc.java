/**
 * <legal>
 * Copyright (c) 2005 Brown University.
 *
 * This file is part of Roxybot.
 *
 * Roxybot is free software; you can redistribute it and/or modify it under 
 * the terms of the GNU Lesser General Public License as published by the Free 
 * Software Foundation; either version 2.1 of the License, or (at your option) 
 * any later version.
 *
 * Roxybot is distributed in the hope that it will be useful, but WITHOUT 
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or 
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License 
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License along 
 * with Foobar; if not, write to the Free Software Foundation, Inc., 
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA 
 * </legal>
 *
 * Constants
 *
 * Author  : Victor Naroditskiy
 * Created : 1 January, 2007
 *
 * Author  : Seong Jae Lee
 * Updated : 15 May, 2007
 * 
 * 
 */

package props;

import java.io.FileReader;
import java.io.LineNumberReader;
import java.io.PrintStream;
import java.lang.reflect.Array;
import java.util.*;

public class Misc
{
	public static boolean verbose = true;
	public static ArrayList<String> ignoreList = new ArrayList<String>();
	public static boolean shutup = false;
	
	public static void init() { try {
		FileReader reader = new FileReader("config/ignorelist.txt");
		LineNumberReader lineReader = new LineNumberReader(reader);
		String line;
		
		while (null != (line = lineReader.readLine())) {
			if (line.length() == 0) continue;
			if (line.startsWith("#")) continue;
			ignoreList.add(line);
		}
		
		lineReader.close();
		reader.close();
	} catch (Exception e) { e.printStackTrace(); }}
	
	public static void error(String s) {
		System.out.println("Error: *** " + s);
	}

	public static void warn(String s) {
		System.out.println("Warn: ### " + s);
	}

	public static void warn(Object s) {
		System.out.println("Warn: ### " + s);
	}

	public static void print(Object s) {
		if (!verbose || shutup) return;
		
		for (int i = 0; i < ignoreList.size(); i++) {
			if (s.toString().startsWith(ignoreList.get(i))) {
				shutup = true;
				return;
			}
		}
		
		System.out.print(s);
	}

	public static void println(Object s) {
		for (int i = 0; i < ignoreList.size(); i++) {
			if (s.toString().startsWith(ignoreList.get(i))) {
				shutup = true;
				return;
			}
		}
		
		if (verbose) {
			System.out.println(s);
		}
	}

	public static void println() {
		if (verbose) {
			System.out.println();
		}
	}
	
	public static void myassert(boolean test) {
		if(!test) {
			Misc.error("Misc.myassert failed!");
			try {
				throw new Error();
			}
			catch(Error e){
				e.printStackTrace(System.err);
				e.printStackTrace(System.out);
				Misc.println("assert. exiting");
				System.exit(0);
			}
		}
	}
	
	public static void printSeparator(int currentIndex, int totalCount, PrintStream ps) {
		if (currentIndex < totalCount-1) {
			ps.print(",\t");
		}else if(currentIndex == totalCount-1){
			ps.println();
		}else{
			myassert(false);
		}
	}
	
	public static void printSpaces(int numSpaces) {
		for (int i=0; i<numSpaces; i++) {
			Misc.print(" ");
		}
	}
	
	public static void printPadded(Object o, int len) {
		Misc.print(o);
		for(int i=0; i<len-o.toString().length(); i++) {
			Misc.print(" ");
		}
	}
	
    public static long getTime() {
    	return System.currentTimeMillis()/1000;
    }

    public static boolean floatsEqual(float a, float b) {
    	if(Math.abs(a-b) < .001) {
    		return true;
    	}
    	return false;
    }

    public static boolean doublesEqual(double a, double b) {
    	if(Math.abs(a-b) < .001) {
    		return true;
    	}
    	return false;
    }

    
    public static boolean compareArrays(Object a1, Object a2) {
	    int len = Array.getLength(a1); 	    
	    int dim = getDim(a1);           

	    int len2 = Array.getLength(a2); 	    
	    int dim2 = getDim(a2);
	    
	    Misc.myassert(len == len2);
	    Misc.myassert(dim == dim2);
	    
	    for(int i=0; i<len; i++) {
	    	Object e1 = Array.get(a1, i);
	    	Object e2 = Array.get(a2, i);
	    	if(1 == dim) {
	    		if(!e1.equals(e2)) {
	    			return false;
	    		}
	    	}else{
	    		return compareArrays(e1, e2);
	    	}
		}
		return true;    	
    }
    
    // If `array' is an array object returns its dimensions; otherwise returns 0
    public static int getDim(Object array) {
        int dim = 0;
        Class cls = array.getClass();
        while (cls.isArray()) {
            dim++;
            cls = cls.getComponentType();
        }
        return dim;
    }

	
	public static String printsArray(Object array) {
		String s = "";
	    int len = Array.getLength(array); 	    
	    int dim = getDim(array);           
	    
	    for(int i=0; i<len; i++) {
	    	Object element = Array.get(array, i);
	    	if(1 == dim) { 
	    		s += element + " ";
	    	}else{
	    		s += printsArray(element);
	    	}
		}
	    if(dim > 1) {
	    	s += "\n";
	    }
		
		return s;
	}
	
	public static void printArray(Object array) {
		Misc.println(printsArray(array));
	}
		
	
	public static void printList(List list) {
		Iterator i = list.iterator();
		while(i.hasNext()) {
			Misc.print(i.next() + " ");
		}
		Misc.println();
	}
	
	public static void main (String[] args) {
		int[] a1 = {1,2,3};
		int[] a2 = {1,3,3};
		Misc.println("f. arrays equal " + compareArrays(a1, a2));
		
		a2[1] = 2;
		Misc.println("t. arrays equal " + compareArrays(a1, a2));
		
		double b1[][] = new double[2][3];
		b1[1][2] = 3;
		
		double b2[][] = new double[2][3];
		b1[1][2] = 3;
		
		Misc.println("t. arrays equal " + compareArrays(b1, b2));
		
		b1[0][2] = 3;
		Misc.println("f. arrays equal " + compareArrays(b1, b2));

	}
}
