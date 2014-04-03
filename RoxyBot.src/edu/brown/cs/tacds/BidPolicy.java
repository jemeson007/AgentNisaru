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
 * BuyPolicy
 *
 * Author  : Bryan Guillemette
 * Created : 1 May, 2005
 * Description: 
 *    
 * Data Structure keeping count of quantity and price that an agent
 * should buy each type of auction.
 * 
 * Author  : Seong Jae Lee
 * Updated : 13 June, 2005
 * Description:
 * 
 * - removed setQuantity
 * - edited  toString
 * - changed setPrice -> addPrice
 * 
 * setPrice(auctionNo, quantity, price) changed to addPrice(auctionNo, price).
 * Whenever this is called, auction's quantity increases by 1 automatically.
 * So we don't have to use setQuantity(quantity).
 * 
 */
package edu.brown.cs.tacds;

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.Arrays;
import edu.brown.cs.tac.Constants;

public class BidPolicy {
	// Indexed by auction number
    private int[]     m_quantity;  // m_quantity[auctionNo]
    private float[][] m_price;     // m_price[auctionNo][quantity]
    
    public BidPolicy() {
    	m_quantity = new int[Constants.AUCTION_MAX];
    	Arrays.fill(m_quantity, 0);
    	
    	m_price = new float[Constants.AUCTION_MAX][Constants.MAX_QUANTITY];
    	for (int auctionNo = 0; auctionNo < Constants.AUCTION_MAX; auctionNo++) {
    		Arrays.fill(m_price[auctionNo], 0);
    	}
	}
    
    // Return the quantitieth price
    public float getPrice(int auctionNo, int quantitieth) { return m_price[auctionNo][quantitieth]; }
  
    // Return total quantity for specified auction
    public int getQuantity(int auctionNo) { return m_quantity[auctionNo];}
    
    // Quantity increases automatically
    public void addPrice(int auctionNo, float price) {
    	m_quantity[auctionNo]++;
    	m_price[auctionNo][m_quantity[auctionNo]-1] = price;
    }
    
    // buy quantity goods at price
    public void addPrice(int auctionNo, int quantity, float price) {
        for (int i = 0; i < quantity; i++) {
            m_quantity[auctionNo]++;
            m_price[auctionNo][m_quantity[auctionNo]-1] = price;
        }
    }
    
	/*
     *  Sorts price arrays in descending order
	 */
	public void sort() {
		for (int auctionNo = 0; auctionNo < Constants.AUCTION_MAX; auctionNo++) {
			for (int i = 0; i < m_price[auctionNo].length; i++) {
				m_price[auctionNo][i] *= -1;
			}
			Arrays.sort(m_price[auctionNo]);
			for (int i = 0; i < m_price[auctionNo].length; i++) {
				m_price[auctionNo][i] *= -1;
			}
		}
	}

    /*
     *  Returns a string representation of this BuyPolicy. 
     */
    public String toString(){
    	String ret = "";
        NumberFormat formatter = new DecimalFormat("000");
        
		for (int auctionNo = 0; auctionNo < Constants.AUCTION_MAX; auctionNo++){
			if (m_quantity[auctionNo] == 0) continue;
			ret += getClass().getSimpleName() + " : " + Constants.auctionNoToString(auctionNo) + " : ";
			for (int quantity = 0; quantity < m_quantity[auctionNo]; quantity++) {
				ret += formatter.format(m_price[auctionNo][quantity]) + "  ";
			}
			ret += "\n";
		}
		
		return ret;
    }
}

