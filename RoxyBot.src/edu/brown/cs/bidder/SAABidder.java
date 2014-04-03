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
 * SAABidder
 *
 * Author  : Victor Naroditskiy
 * Created : 1 January, 2007
 * 
 * Author  : Seong Jae Lee
 * Updated : 15 May, 2007
 * 
 */

package edu.brown.cs.bidder;

import java.util.ArrayList;
import se.sics.tac.aw.Bid;
import edu.brown.cs.completer.*;
import edu.brown.cs.tacds.*;
import edu.brown.cs.tac.*;
import props.Misc;

public class SAABidder {
	private Repository repository;
	private Completer completer;
	
	public SAABidder(Repository r) {
		repository = r;
		completer = null;
	}
	
	public void setCompleter (Completer c) { completer = c; }
	public Completer getCompleter () { return completer; }
	
	public void init() { }

	public ArrayList<Bid> decidePolicy(Priceline[] scenarios) {
		Misc.println(getClass().getSimpleName() + ".decidePolicy");
		
		Completion completion = completer.run();
		
		if (completion == null) {
			Misc.error(getClass().getSimpleName() + ".decidePolicy : null completion, returning.");
			return null;
		}
		
		ArrayList<Bid> ret = new ArrayList<Bid>();
		
		for (int auctionNo = 0; auctionNo < Constants.AUCTION_MAX; auctionNo++) {
			Bid b = new Bid(auctionNo);
			int totalQuantity;
			
			switch (Constants.auctionType(auctionNo)) {
			case Constants.TYPE_FLIGHT:
				totalQuantity = completion.getBidPolicy().getQuantity(auctionNo);
				if (totalQuantity == 0) break;
				
				for (int q = 0; q < totalQuantity; q++) {
					b.addBidPoint(1, 1000.0f);
				}
				
				Misc.println(getClass().getSimpleName() + 
						"\t" + Constants.auctionNoToString(auctionNo) + ":" + 
						b.getBidString() + 
						", curr " + repository.getQuote(auctionNo).getAskPrice());
				
				break;

			case Constants.TYPE_EVENT:
				int totalBidQuantity = completion.getBidPolicy().getQuantity(auctionNo);
				int totalAskQuantity = completion.getAskPolicy().getQuantity(auctionNo);   			
				if (totalBidQuantity == 0 && totalAskQuantity == 0) continue;
				
				for (int q = 0; q < totalBidQuantity; q++) {
					b.addBidPoint( 1, completion.getBidPolicy().getPrice(auctionNo, q));
				}
				
				for (int q = 0; q < totalAskQuantity; q++) {
					b.addBidPoint(-1, completion.getAskPolicy().getPrice(auctionNo, q));
				}
				
				Misc.println(getClass().getSimpleName() + 
						"\t" + Constants.auctionNoToString(auctionNo) + ":" +
						b.getBidString());
				
				break;
				
			case Constants.TYPE_HOTEL:
				totalQuantity = completion.getBidPolicy().getQuantity(auctionNo);
				if (totalQuantity == 0) continue;
				
				for (int q = 0; q < totalQuantity; q++) {
					float price = completion.getBidPolicy().getPrice(auctionNo, q);
					b.addBidPoint(1, price);
				}
				
				Misc.println(getClass().getSimpleName() +
						"\t" + Constants.auctionNoToString(auctionNo) + ":" +
						b.getBidString());
		   		
		   		break;
			}
			
			if (b.getQuantity() != 0) {
				ret.add(b);
			}
		}
		
		return ret;
	}
}
