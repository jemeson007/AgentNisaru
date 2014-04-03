/**
 * tacds
 *
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
 * MiniTravelPackage
 *
 * Author  : Seong Jae Lee
 * Created : 5 May, 2007
 * Description: 
 *    
 * This class is lot different from TravelPackage. 
 * For example, inflight range is 0~4 here, while in TravelPackage, it's -1~3.
 * This is used for Tatonnementer, for simulating Walverine's Tatonnement.
 * It represents 'trip' structure in Walverine c code.
 */

package edu.brown.cs.tacds;

public class MiniTravelPackage {
	public int in, out;	// in = [1~4, 0 = null], out = [2~5]
	public int hotel;		// bad hotel = 0, good hotel = 1
	public double value = 0;
	
	public MiniTravelPackage(MiniTravelPackage toCopy) {
		in = toCopy.in;
		out = toCopy.out;
		hotel = toCopy.hotel;
		value = toCopy.value;
	}
	
	public MiniTravelPackage(int i, int o, int h, double v) {
		in = i; out = o; hotel = h; value = v;
	}
	
	public String toString() {
		String ret = "";
		ret += in + "," + out + "," + hotel + "," + value;
		return ret;
	}
}
