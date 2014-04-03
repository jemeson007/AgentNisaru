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
 * MVBidder
 *
 * Author  : Victor Naroditskiy
 * Created : 1 January, 2007
 * 
 */

package edu.brown.cs.bidder;

import java.util.*;

import se.sics.tac.aw.*;
import edu.brown.cs.tacds.*;

public class MVBidder{
	private Repository repository;

	public MVBidder (Repository dr) {
		repository = dr;
	}
	
	public ArrayList<Bid> getBid(int[] goodsToBidOn, Priceline p) {
		ArrayList<Bid> ret = new ArrayList<Bid>();
		
		for (int a = 0; a < 16; a++) {
			if (goodsToBidOn[a] > 0 && MUBidder.bidInAuction(a, repository)) {
				Bid bid = new Bid(a);
				for (int c = 1; c <= goodsToBidOn[a]; c++) {
					if (a<8) {
						bid.addBidPoint(1, 1000);
					} else {
						bid.addBidPoint(1, p.currBuyPrice[a]);
					}
				}
				ret.add(bid);
			}
		}
		
		return ret;
	}
}


