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
 * AverageMU
 *
 * Author  : Victor Naroditskiy
 * Created : 1 January, 2007
 * 
 * Author  : Seong Jae Lee
 * Created : 5 May, 2007
 * 
 */

package edu.brown.cs.algorithm;
 
/*
 * what to bid on: all goods
 * what to bid: run MU multiple times with different scenario prices and average the bids across MUs
 * 
 */


import java.util.ArrayList;
import java.util.Iterator;

import edu.brown.cs.bidder.MUBidder;
import edu.brown.cs.bidder.MUOldBidder;
import edu.brown.cs.modeler.FlightPredictor;
import edu.brown.cs.props.*;
import edu.brown.cs.tac.Constants;
import edu.brown.cs.tacds.*;
import se.sics.tac.aw.*;
import props.*;

public class AverageMU extends Algorithm {
	private boolean use_old_mu = false;
	
	public void useOldMU () { use_old_mu = true; }
	public void useNewMU () { use_old_mu = false; }
	
	public AverageMU(int s) {
		super(s, 1, 0);
	}
	
	public AverageMU(int s, int p, int e) {
		super(s, p, e);
	}
	
	public ArrayList<Bid> getIdealBids() {
		Priceline[] scenarios = repository.getXMostRecent(numScenarios);

		// Because it's bidding on all available goods *constantly*, we don't have to
		// pass on scenarios. sjlee.
		int[] goodsToBidOn = goodsChooser.goodsToBidOn(Constants.ALL, null);
		
		float[][] totalBids = new float[Constants.AUCTION_MAX][Constants.MAX_QUANTITY];
		float[][] totalSells = new float[Constants.AUCTION_MAX][Constants.MAX_QUANTITY];
		
		MUBidder bidder;
		if (use_old_mu) bidder = new MUOldBidder(repository);
		else bidder = new MUBidder(repository);
		
		for (int i = 0; i < scenarios.length; i++) {
			//Priceline prices = m_dr.updateFlightAndEvent(scenarios[i].priceline, true);
			Priceline prices = scenarios[i];
			ArrayList<Bid> currentBids = bidder.decideHotelPolicy(goodsToBidOn, prices, false);
			currentBids.addAll(bidder.decideFlightPolicyWithMU(prices));
			currentBids.addAll(bidder.decideEventPolicyWithMU(prices, true));
			
			//add bids
			Iterator<Bid> iter = currentBids.iterator();
			while (iter.hasNext()) {
				Bid bid = iter.next();
				for (int q = 0; q < bid.getNoBidPoints(); q++) {
					Misc.myassert(bid.getQuantity(q) == 1 || bid.getQuantity(q) == -1);
					if (bid.getQuantity(q) == 1) {
						totalBids[bid.getAuction()][q] += bid.getPrice(q);
						Misc.println("AverageMU.getIdealBids : " +
								"auction " + bid.getAuction() + 
								", adding bid " + bid.getPrice(q) + 
								", q " + q + 
								", total " + totalBids[bid.getAuction()][q]);
					}
					if (bid.getQuantity(q) == -1) {
						totalSells[bid.getAuction()][q] += bid.getPrice(q);
						Misc.println("AverageMU.getIdealBids : " +
								"auction " + bid.getAuction() + 
								", adding sell bid " + bid.getPrice(q) + 
								", bp " + q + 
								", total " + totalSells[bid.getAuction()][q]);
					}	
				} // end of for quantitieth
			} // end of while bids
		} // end of for scenarios
		
		ArrayList<Bid> bids = new ArrayList<Bid>();
		
		Priceline priceline = new Priceline();
		priceline = repository.flight.predict(repository, FlightPredictor.PREDICT_MU, priceline);
		if (Constants.EVENTS_ON) priceline = repository.event.predict(repository, priceline);
		Priceline prices = priceline;
		//Priceline prices = this.repository.updateFlightAndEvent(new Priceline(), 
		//		FlightPredictor.AFTER_MIN, FlightPredictor.AFTER_MIN);
		
		for (int a=0; a < totalBids.length; a++) {
			if (totalBids[a][0] == 0 && totalSells[a][0] == 0) continue;
			
			Bid bid = new Bid(a);
			for (int q = 0; q < Constants.MAX_QUANTITY; q++) {
				if (totalBids[a][q] > 0) {
					float mu = totalBids[a][q]/scenarios.length;
					Misc.myassert(mu >= 0);
					if (Constants.auctionType(a) == Constants.TYPE_EVENT) {
						if (mu < prices.currBuyPrice[a]) continue;
						bid.addBidPoint(1, prices.currBuyPrice[a]);
					} else {
						bid.addBidPoint(1, mu);
					}
				}
				
				if (totalSells[a][q] > 0) {
					Misc.myassert(Constants.auctionType(a) == Constants.TYPE_EVENT);
					float mu = totalSells[a][q]/scenarios.length;
					if (mu < prices.currSellPrice[a]) continue;
					if (totalBids[a][q] > 0 && mu <= prices.currBuyPrice[a]) continue; // self transaction
					bid.addBidPoint(-1, prices.currSellPrice[a]);
				}
				
				// We only bid one goods for entertainment. sjlee.
				if (Constants.auctionType(a) == Constants.TYPE_EVENT) break;
			}
			
			if (bid.getQuantity() != 0)
				Misc.println("AverageMU.getIdealBids : adding bid " + bid);
			
			bids.add(bid);
		}
		
		return BidValidator.getConsistentBids(bids, repository, scenarios[0]);
	}

	public String toString() {
		String ret = "";
		ret += "AMU";
		ret += this.use_old_mu ? "O" : "N" ;
		ret += "-";
		ret += super.toString();
		return ret;
	}
}
