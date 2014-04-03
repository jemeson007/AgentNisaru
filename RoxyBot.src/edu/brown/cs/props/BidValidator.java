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
 * BidValidator
 *
 * Author  : Victor Naroditskiy
 * Created : 1 January, 2007
 * Description:
 * 
 */

package edu.brown.cs.props;

import java.util.ArrayList;



import props.Misc;
import se.sics.tac.aw.Bid;
import se.sics.tac.aw.Quote;
import edu.brown.cs.tac.*;
import edu.brown.cs.tacds.*;

public class BidValidator {
	/*
	 Price quotes are only generated once per minute, on the minute.
	 Let ASK be the current ask quote (16th highest price). Any new bid b must satisfy the following conditions to be admitted to the auction:
	 1. b must offer to buy at least one unit at a price of ASK+1 or greater. (the smallest acceptable bid is 1)
	 2. If the agent's current bid b' would have resulted in a purchase of q units in the current state, 
	 	then the new bid b must offer to buy at least q units at ASK+1 or greater.
	 	
	 in other words we must bid (ask+1) on max(1,hqw). 
	 bidding less than ask+1 is pointless but when we bid ask+1 we expect to pay ask. so 
	 	remove bid points below ask (vn)
	 	if ask < bid < ask+1, bid (ask+1) (vn)
	 if there was b' but now we don't want any of the hotels, we can lower b' to (ask+1)
	 */
	public static ArrayList<Bid> getConsistentBids(ArrayList<Bid> newBids, Repository dr, Priceline flightPrices) {
	
		ArrayList<Bid> consistentBids = new ArrayList<Bid>();
	 	
		for (int auc = 0; auc < Constants.AUCTION_MAX; auc++) {
			// find a new bid for this auction
			Bid newBid = null;
			for (int i = 0; i< newBids.size(); i++) {
				Bid bid = (Bid) newBids.get(i); 
				if (bid.getAuction() == auc) {
					newBid = bid;
					break;
				}
			}
			
			//copy flight and en-nt bids - they are always consistent.
			if (Constants.auctionType(auc) != Constants.TYPE_HOTEL) {
				if (newBid != null) {
					consistentBids.add(newBid);
				}
				continue;
			}
			
			//take care of hotel bids
			if (newBid == null) {
				newBid = new Bid(auc);
			}
			
			Quote currentQuote = dr.getQuote(auc);
			final int hqw = (currentQuote == null) ? 0 : Math.max(0, currentQuote.getHQW());
			final float askPrice = (currentQuote == null) ? 0 : currentQuote.getAskPrice();
			final float minBidPrice = askPrice + 1;
			
			Bid currentBid = dr.getBid(auc);
			
			if (currentBid != null && currentQuote == null) {
				Misc.error("BidValidator : " +
						"currentQuote is null but currentBid is not. " +
						"auction " + auc + ", currentBid " + currentBid);
				Misc.myassert(false);
			}
			
			checkDescending(newBid);
			
			Bid consistentBid = new Bid(auc);
			
			// we have to bid at least minAskPrice but can choose to bid higher.
			for (int i=0; i<hqw; i++) {
				// Misc.myassert(currentBid != null);
				// if you start in the middle of the game, currentBid can be null,
				// while hqw is not 0. sjlee.
				if (currentBid == null) {
					Misc.warn("BidValidator : currentBid is null.");
					currentBid = new Bid(auc);
					for (int j = 0; j < hqw; j++) currentBid.addBidPoint(1, minBidPrice);
				}
				
				float newPrice = (i < newBid.getNoBidPoints()) ? newBid.getPrice(i) : 0;
				float bidPrice = Math.max(minBidPrice, newPrice);
				
				Misc.println("BidValidator : " +
						"adjusting bid for hqw " + hqw + 
						", auction " + auc + 
						", bid point "+ i + 
						", new price " + bidPrice + 
						", old price " + currentBid.getPrice(i) + 
						", new price " + askPrice);
				
				// we may end up chasing ourselves - we raise ask price by 1 every time and the ask price increases by one because of that. this happens when we our bid is the 16th bid. i don't see any way to update bids and not cause this. vn
				consistentBid.addBidPoint(1, bidPrice);
			}
			
			// if we want to bid higher than ask price on more than hqw, we do it here
			for(int i = hqw; i < newBid.getNoBidPoints(); i++) {
				float newPrice = newBid.getPrice(i);
				
				if (newPrice >= askPrice) {
					Misc.myassert(1 == newBid.getQuantity(i));
					
					newPrice = getBidAboveAsk(newPrice, askPrice);
					//Misc.println("BidValidator.getConsistentBids : submitting bids above currentPrice " + askPrice + " after hqw. bid point " + i + " bid " + newPrice);
					consistentBid.addBidPoint(1, newPrice);
				}
			}
			
			if (compareBids(consistentBid, currentBid)) {
				Misc.println("BidValidator : new bid " + consistentBid + " is the same as old bid " + currentBid + ". not submitting");
				//Misc.myassert(!Constants.SIMULATOR || Constants.SIMULATOR_CANNOT_WITHDRAW_BIDS);
				continue;
			}

			if(consistentBid.getNoBidPoints() > 0) {
				consistentBids.add(consistentBid);
			}
		}
		
		Misc.println("BidValidator " +
				": consistent bids " + newBids.size() + 
				", " + consistentBids);
		//Misc.myassert(consistentBids.size() >= newBids.size());
		
		return fixMUFlightBids(consistentBids, flightPrices);
	}
	
	//the flight prices may have gone up. but MUs are calculated with predicted prices.
	//for example, out1 is 100, out2 is 120, mu for out1 is 110. real out1 is 120 real out2 is 140. we'd still want out1 when we recompute MUs with new prices.
	//bid to win if our bid is above the predicted price (amu average bids so some of its bids may be lower)
	public static ArrayList<Bid> fixMUFlightBids(ArrayList<Bid> consistentBids, Priceline flightPrices) {
		Misc.println("BidValidator.fixMUFligihtBids : fixing flight bids");
		
		ArrayList<Bid> fixedBids = new ArrayList<Bid>();
		for (int i=0; i<consistentBids.size(); i++) {
			Bid b = (Bid)consistentBids.get(i);
			int a = b.getAuction();
			if (Constants.auctionType(a) == Constants.TYPE_FLIGHT) {
				Bid fixedBid = new Bid(a);
				for(int bp=0; bp<b.getNoBidPoints(); bp++) {
					if (b.getPrice(bp) >= flightPrices.currBuyPrice[a]) {
						fixedBid.addBidPoint(b.getQuantity(bp), 1000);
					}else{
						fixedBid.addBidPoint(b.getQuantity(bp), b.getPrice(bp));
					}
				}
				fixedBids.add(fixedBid);
			} else {
				fixedBids.add(b);
			}
		}
		Misc.myassert(fixedBids.size() == consistentBids.size());
		
		for (int i=0; i<fixedBids.size(); i++) {
			if (Constants.auctionType(fixedBids.get(i).getAuction()) == Constants.TYPE_FLIGHT) {
				Misc.println("BidValidator.fixMUFligihtBids : fixed bid: " + fixedBids.get(i));
			}
		}
		
		return fixedBids;
	}
	
	//make sure the bid points are in the descending order
	public static void checkDescending(Bid bid) {
		if (bid.getNoBidPoints() > 1) {
			float bidValue = bid.getPrice(0);
			for(int i=1; i<bid.getNoBidPoints(); i++) {
				//Misc.myassert((int)bidValue >= (int)(bid.getPrice(i)));
				if(Math.round(bidValue) < Math.round(bid.getPrice(i))) {
					Misc.warn("BidValidator.fixMUFligihtBids : descending check failed: " + bid);
				}
				bidValue = bid.getPrice(i);
			}
		}
	}
	
	public static boolean compareBids(Bid a, Bid b) {
		if(a==null && b==null) {
			return true;
		}
		if(a==null) {
			return false;
		}
		if(b==null) {
			return false;
		}
		//not nulls
		if(a.getAuction() == b.getAuction() && a.getBidString().equals(b.getBidString())) {
			return true;
		}
		
		return false;
	}
	
	public static boolean bidCancelled(Bid oldBid) {
		return (oldBid == null || oldBid.getBidString().equals("((1 0.0))") || oldBid.getBidString().equals("()"));
	}
	
	//when we decide to spend 'ask' on something we can bid 'ask+1' because we'll most likely pay ask anyway.
	//the server only accepts bids of (ask+1) or higher. so we have to bid 'ask+1' even when we want to spend at most ask.
	public static float getBidAboveAsk(float bid, float ask) {
		Misc.myassert(bid >= ask);
		return Math.max(bid, ask+1f);
	}

}
