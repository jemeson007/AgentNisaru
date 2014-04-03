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
 * TacRandom
 *
 * Author  : Seong Jae Lee
 * Updated : 15 May, 2007
 * 
 * All random variables are created with this class.
 * This class is necessary because
 * 1) creating java.util.Random class takes too much time, and
 * 2) one can fix seed value to generate the same result as before.
 * 
 */

package edu.brown.cs.tac;

import java.util.Random;

public class TacRandom {
	private static Random random;
	private static long seed;
	
	public static void init() { 
		seed = System.currentTimeMillis();
		random = new Random(seed);
	}
	
	public static void setSeed (long s) { 
		seed = s; 
		random = new Random(seed);
	}
	
	/**
	 * Returns a pseudorandom, uniformly distributed int value between 0 (inclusive) and the specified value (exclusive), drawn from this random number generator's sequence.
	 * For example, nextInt(null, 3) returns 0, 1 or 2.
	 * object argument is just for checking.
	 */
	public static int nextInt (Object object, int i) {
		if (random == null) init();
		return random.nextInt(i);
	}
	
	public static boolean nextBoolean (Object object) {
		if (random == null) init();
		return random.nextBoolean();
	}
	
	public static double nextDouble (Object object) {
		if (random == null) init();
		return random.nextDouble();		
	}
	
	public static double nextGaussian (Object object) {
		if (random == null) init();
		return random.nextGaussian();	
	}
}
