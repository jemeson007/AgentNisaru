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
 * Priceline
 *
 * Author  : Bryan Guillemette
 * Created : 1 May, 2005
 * Description: 
 *    
 * A class containing two 28x8 arrays, one for buy prices and one
 * for sell prices, of the 28 auctions and the 8 copies of each good
 * within that auction that we could conceivably want to buy.
 * 
 */

package edu.brown.cs.tacds;

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.*;

import props.Misc;
import edu.brown.cs.tac.Constants;

public class Priceline {    
    public float[] 	currBuyPrice;
    public float[] 	currSellPrice;
    public float[] 	nextBuyPrice; //used for future flight prices predictions in SAA
    public float[] 	nextSellPrice;
    public double	probability;
    
	public Priceline() {
		probability = 1;
		currBuyPrice  = new float[Constants.AUCTION_MAX];
		currSellPrice = new float[Constants.AUCTION_MAX];
		nextBuyPrice  = new float[Constants.AUCTION_MAX];
		nextSellPrice = new float[Constants.AUCTION_MAX];
		
		Arrays.fill(currBuyPrice, Constants.INFINITY); //assume we can't buy anything
	}
   
	public Priceline (float[] prices) {
		probability = 1;
		currBuyPrice  = new float[Constants.AUCTION_MAX];
		currSellPrice = new float[Constants.AUCTION_MAX];
		nextBuyPrice  = new float[Constants.AUCTION_MAX];
		nextSellPrice = new float[Constants.AUCTION_MAX];
		
		Arrays.fill(currBuyPrice, Constants.INFINITY);//assume we can't buy anything
		
		if (prices.length != 8) for (int i = 0; i < prices.length; i++) currBuyPrice[i] = prices[i]; // flight + hotel, or, all auctions
		else for (int i = 0; i < prices.length; i++) currBuyPrice[i + 8] = prices[i]; // hotel
	}
	
	public Priceline(Priceline pl) {
		probability = pl.probability;
		currBuyPrice  = new float[Constants.AUCTION_MAX];
		currSellPrice = new float[Constants.AUCTION_MAX];
		nextBuyPrice  = new float[Constants.AUCTION_MAX];
		nextSellPrice = new float[Constants.AUCTION_MAX];
		
		for (int i = 0; i < Constants.AUCTION_MAX; ++i) {
			currBuyPrice[i] = pl.currBuyPrice[i];
			nextBuyPrice[i] = pl.nextBuyPrice[i];
			currSellPrice[i] = pl.currSellPrice[i];
			nextSellPrice[i] = pl.nextSellPrice[i];
		}
	}

	public String toString() {
		NumberFormat formatter = new DecimalFormat("000");
		String ret = "";

		for (int a = 0; a < 8; a++) {
			ret += formatter.format(currBuyPrice[a]);
			ret += "(" + formatter.format(nextBuyPrice[a]) + ")" + ",";
		}
		
		for (int a = 8; a < 16; a++) {
			if (currBuyPrice[a] >= 10000) ret += "xxx";
			else ret += formatter.format(currBuyPrice[a]);

			if(a!=15) ret += ",";
		}
		
		if(!Constants.EVENTS_ON) return ret;
		
		ret += ", ";
		for (int a=16; a<28; a++) {
			if (currBuyPrice[a] >= 10000) ret += "xxx";
			else ret += formatter.format(currBuyPrice[a]);
			
			ret += "(" + formatter.format(currSellPrice[a]) + ")";
			if(a!=27) ret += ",";
		}
		
		return ret;
	}
	
	public String getHotelString() {
		String ret = "";
		for (int a = 8; a < 16; a++) {
			if (currBuyPrice[a] != Constants.INFINITY) 
				ret += (int) currBuyPrice[a];
			else
				ret += "xxx";
			if (a != 15) ret+=",";
		}
		return ret;
	}
	
	public void printHotel() {
		String ret = "Priceline : ";
		NumberFormat formatter = new DecimalFormat("000");

		for (int a = 8; a < 16; a ++) {
			if (currBuyPrice[a] < 10000)
				ret += formatter.format(currBuyPrice[a]);
			else
				ret += "xxx" ;
			
			if (a % 8 != 7) 
				ret += ", ";
		}
		
		Misc.println(ret);
	}
	
	// we get slightly different numbers after we average a bunch of them in Algorith.averageScenario. We do not need more precision than 2 decimal places. vn
	public void roundPrices() {
		for (int i=0; i<currBuyPrice.length; i++) {
			currBuyPrice[i] = (float)(Math.round(100*currBuyPrice[i]))/100f;
			if (i >= 8 && i < 16 && currBuyPrice[i] <= 0) {
				Misc.error("average price is zero" + currBuyPrice[i] + " i" + i);
				Misc.myassert(currBuyPrice[i] > 0); 
				//zero prices in scenarios cause problems in MUBidder when we bid only if prices are greater than zero. vn
			}
			currSellPrice[i] = (float)(Math.round(100*currSellPrice[i]))/100f;
			nextBuyPrice[i] = (float)(Math.round(100*nextBuyPrice[i]))/100f;
			nextSellPrice[i] = (float)(Math.round(100*nextSellPrice[i]))/100f;
		}
	}

	
	/*
	public String toString() {
		String ret = "";
		NumberFormat formatter = new DecimalFormat("000");

		for (int a = 0; a < 16; a ++) {
			if (currBuyPrice[a] < 1000)
				ret += formatter.format((int) currBuyPrice[a]) + ":";
			else
				ret += "xxx:" ;
			
			if (a % 8 == 7) 
				ret += ":";
		}
		
		for (int a = 0; a < 8; a ++) {
			ret += formatter.format((int) nextBuyPrice[a]) + ":";
		}
		
		return ret;
	}
	*/
	

}
