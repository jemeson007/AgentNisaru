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
 * SellPolicy
 *
 * Author  : Bryan Guillemette
 * Created : 1 May, 2005
 * Description: 
 *  
 * Data Structure for keeping count of goods (such as owned goods).
 * 
 */

package edu.brown.cs.tacds;

import props.Misc;
import edu.brown.cs.tac.Constants;

public class TACCollection {
    
    // indexed by Constants.AUCTION...
	public int [] owned;
	
    public TACCollection() {
        owned = new int [Constants.AUCTION_MAX];
    }

    public TACCollection(TACCollection col) {
        owned = new int [Constants.AUCTION_MAX];
		for (int auction = 0; auction < Constants.AUCTION_MAX; auction++) {
			owned[auction] = col.getOwn(auction);
		}
    }

	// setter 
    public void setOwn(int auction, int quantity) {
    	if (quantity < 0)
    		Misc.warn ("TACCollection.setOwn: OH MY GOSH! The quantity is negative! + auctionNo: " + auction);
    	
		if (auction >= 0 && auction < Constants.AUCTION_MAX)
			owned[auction] = quantity;
	}
	
	// getter (return -1 if error)
	public int getOwn(int auction) {
		if (auction >= 0 && auction < Constants.AUCTION_MAX)
			return owned[auction];
		return -1;
	}
	
	public String toString() {
		String ret = "";
		
		for (int auction = 0; auction < Constants.AUCTION_MAX; auction++) {
			ret += owned[auction] + " ";
		}
		
		return ret;
	}

}
