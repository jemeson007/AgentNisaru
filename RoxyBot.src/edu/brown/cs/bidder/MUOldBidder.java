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
 * MUOldBidder
 *
 * Author  : Victor Naroditskiy
 * Created : 1 January, 2007
 * 
 * Author  : Seong Jae Lee
 * Updated : 15 May, 2007
 * 
 */

package edu.brown.cs.bidder;

import java.util.*;
import se.sics.tac.aw.*;
import edu.brown.cs.tac.Constants;
import edu.brown.cs.tacds.*;
import props.Misc;

public class MUOldBidder extends MUBidder {
	
	public MUOldBidder(Repository dr) {
		super(dr);
		init(); //yes. is it ok to do this here instead of roxybot? this is old. don't really need it.
	}

	/**
	 * 1. Construct hotel bids with MU for each hotels and each qth good.
	 * 2. Construct event bids as completion says. Event price is set to 80, so don't worry.
	 *
	 * @param hotelsToBidOn :
	 * @param price : predicted price, we use prices from avg or the 1st scenario.
	 * @return an ArrayList of se.sics.tac.aw.Bid objects.
	 */
	public ArrayList<Bid> decideHotelPolicy (
			int[] hotelsAndFlightsToBidOn, 
			Priceline pp, 
			boolean setOtherPricesToInfinity) {
		
		Misc.println(getClass().getSimpleName() + ".decideHotelPolicy : price " + pp);
		ArrayList<Bid> ret = new ArrayList<Bid>();

		// basic state information
		Preference[] cprefs = m_dr.getClientPrefs();
		
		// construct hotel bids with MU for each hotels and each qth good.
		for (int a = 8; a < 16; a++) {
			if (!bidInAuction(a, m_dr)) continue;
			
			TACCollection own = m_dr.getOwnCollection();
			TACCollection available = Constants.forSale();
			
			// can't buy closed hotel auctions
			for (int h = 8; h < 16; h++) {
				if (m_dr.isAuctionClosed(h)) available.setOwn(h, 0);
			}
			
			if (setOtherPricesToInfinity) {
				for (int i = 0; i < 16; i++) {
					// This error can happen when a minute passed during computation.
					// Misc.myassert(hotelsAndFlightsToBidOn[i] == 0 || !m_dr.isAuctionClosed(i));
					if (hotelsAndFlightsToBidOn[i] != 0 && m_dr.isAuctionClosed(i)) {
						Misc.error(getClass().getSimpleName() + ".decideHotelPolicy " +
								": bid on closed auction " + i);
					}
					available.setOwn(i, hotelsAndFlightsToBidOn[i]);
				}
			}
			
			// assume we have n good - cost(n-1)
			// assume we cannot have more than n good
			available.setOwn(a, 0); // we cannot buy a anymore
			
			// without this 'if' we'll compute completion once for q=0 
			// even though we don't want to buy any hotels. that wastes time.
			if (hotelsAndFlightsToBidOn[a] == 0) continue;
			
			int max_q = hotelsAndFlightsToBidOn[a]+1;
			Bid bid = new Bid(a);
			float[] value = new float[max_q];
			float[] old_mu = new float[max_q];
			
			for (int q = 0; q < max_q; q++) {
				
				own.setOwn(a, q);
				// available.setOwn(a, 8);
				Priceline pcopy = new Priceline(pp);
				
				if (Constants.SIMULATOR && Constants.SIMULATOR_CANNOT_WITHDRAW_BIDS) {
					if (q < m_dr.getQuote(a).getHQW())
						pcopy.currBuyPrice[a] = 0;
				}
				
				value[q] = m_completer.computeCompletion(cprefs, own, available, pcopy).objectiveValue;
			}

			for (int q = 1; q < max_q; q++) {
				
				old_mu[q] = value[q] - value[q-1];
				
				float ask = m_dr.getQuote(a).getAskPrice();
				
				if (Constants.SIMULATOR && !Constants.SIMULATOR_CANNOT_WITHDRAW_BIDS)
					Misc.myassert(ask == 0);
				
				if (stopBidding(old_mu[q], ask)) {
					Misc.println(getClass().getSimpleName() + ".decideHotelPolicy " +
							": stopping " + a + "th hotel mu calculation" +
							", difference in utilities for " + q + "th copy smaller than current price " + 
							", " + (value[q] - pp.currBuyPrice[a] * (q-1)) + " - " + value[0] + " <= " + ask);
					break;
				}
				
				bid.addBidPoint (1, old_mu[q]);
				
			}
			
			if (bid.getNoBidPoints() > 0) ret.add(bid);
		}
		
		for (int i = 0; i < ret.size(); i++) {
			Bid b = (Bid) ret.get(i);
			Misc.println(getClass().getSimpleName() + ".decideHotelPolicy " +
					": auction " + b.getAuction() + 
					", " + b.getBidString());
		}
		
		return ret;
	}

	/**
	 * The completer assumes that it can buy /sell entertainment tickets as much
	 * as possible, but we actually bid / ask at most one good. Thus, we calculate
	 * MUs two times for each auction. sjlee.
	 *
	 * This methods is only called from SMU and AMU. It doesn't have to 'set other
	 * prices to infinity'. Thus, it doesn't have such a variable as an input.
	 * (Compared to the 'decideHotelPolicy' method)
	 * 
	 * 'useMUForBidPrice' is true in AverageMU. It is false in StraightMU.
	 * RoxyBot 2000 and RoxyBot 2002 are using decideEventPolicyWithCompletion.
	 */
	public ArrayList<Bid> decideEventPolicyWithMU (Priceline prices, boolean useMUForBidPrice) {
		ArrayList<Bid> ret = new ArrayList<Bid>();
		if (!Constants.EVENTS_ON) return ret;
		
		for (int a = 16; a < 28; a++) {
			TACCollection availableGood = Constants.forSale();
			availableGood.setOwn(a, 0);
			for (int h = 8; h < 16; h++) {
				if (m_dr.isAuctionClosed(h)) availableGood.setOwn(h, 0);
			}
			TACCollection holdingGood = m_dr.getOwnCollection();
			
			Bid bid = new Bid(a);
			
			Completion comp = m_completer.computeCompletion(
					m_dr.getClientPrefs(), holdingGood, availableGood, prices);
			double currentUtility = comp.objectiveValue;
			
			// Sometimes our entertainment is negative. It should not happen,
			// but the agent shouldn't crash for that reason. Moreover, it should 
			// try to earn the good as soon as possible, because entertainment 
			// oversell penalty is immense. It would be much better if we can
			// put this formula into Completer, but I don't know how to deal it
			// with ILP. sjlee.
			if (m_dr.getOwn(a) < 0) currentUtility += 200 * (m_dr.getOwn(a));
			
			double sellMU = 200; // If there's no good, you loose 200 for ent. panelty.
			if (m_dr.getOwn(a) != 0) {
				holdingGood.setOwn(a, m_dr.getOwn(a) - 1);
				comp = m_completer.computeCompletion(
						m_dr.getClientPrefs(), holdingGood, availableGood, prices);
				sellMU = currentUtility - comp.objectiveValue;
				
				// Entertainment oversell penalty. See above for a detailed comment.
				if (m_dr.getOwn(a) - 1 < 0) sellMU += 200 * (m_dr.getOwn(a) - 1);
			}
			
			holdingGood.setOwn(a, m_dr.getOwn(a) + 1);
			comp = m_completer.computeCompletion(
					m_dr.getClientPrefs(), holdingGood, availableGood, prices);
			double buyMU = comp.objectiveValue - currentUtility;

			if (useMUForBidPrice) { // This is for AverageMU.
				bid.addBidPoint(1, (float) buyMU);
				bid.addBidPoint(-1, (float) sellMU);
			} else { // This is for StraightMU.
				Misc.println(getClass().getSimpleName() + ".decideEventPolicyWithMU : " +
						"auction " + a + 
						", buyPrice " + formatter.format(prices.currBuyPrice[a]) + 
						", buyMU " + formatter.format(buyMU) + 
						", sellMU " + formatter.format(sellMU) + 
						", sellPrice " + formatter.format(prices.currSellPrice[a]));
				
				// This part is always tricky, since computer's float/double system is
				// a little bit different from mathematics. MU is exactly same to the
				// prediction, when the good is in the union of optimal allocation, but 
				// not in the intersection. But due to the fault of the system, we put
				// an epsilon value 1 (Do you think it's too big?). sjlee.
				// Since StraightMU tends to get entertainment as much as she can,
				// we promote selling by adding 1 to ask price, and dissuade buying 
				// by adding 1 to buy price. In other words, if the good is only in the 
				// union, the agent tends to remove the good from her holding. sjlee.
				if (buyMU > prices.currBuyPrice[a] + 1) bid.addBidPoint(1, prices.currBuyPrice[a]);
				if (sellMU < prices.currSellPrice[a] + 1) bid.addBidPoint(-1, prices.currSellPrice[a]);
			}

			ret.add(bid);
		}

		return ret;
	}
	
	public ArrayList<Bid> decideFlightPolicyWithMU(Priceline pp) {
		Misc.println(getClass().getSimpleName() + ".decideFlightPolicyWithMU");

		ArrayList<Bid> ret = new ArrayList<Bid>();
			
		for (int a = 0; a < 8; a++) {
			if (!bidInAuction(a, m_dr)) continue;
			// Misc.println("MUBidder : computing utilities for " + Constants.auctionNoToString(a));
			
			float[] utilities = new float[9];
			TACCollection forsale = Constants.forSale();
			for (int h = Constants.AUCTION_BAD_HOTEL; h < Constants.AUCTION_ALLIGATOR; h++) {
				if (m_dr.isAuctionClosed(h))
					forsale.setOwn(h, 0);
			}
			forsale.setOwn(a,0);
			TACCollection howned = m_dr.getOwnCollection();

			// construct a bid
			Bid b = new Bid(a);
			for (int q = m_dr.getOwn(a); q < utilities.length; q++) {
				// Misc.println("MUBidder : own set to " + q + " calling completer");
				howned.setOwn(a,q);
				
				//we can want up to 8 copies
				Completion comp = m_completer.computeCompletion(m_dr.getClientPrefs(), howned, forsale, pp);
				utilities[q] = comp.objectiveValue;
				
				if (q > m_dr.getOwn(a)) {
					//Quote quote = m_dr.getQuote(a);
					//can't use quote.getAskPrice because it may be newer and different from the prices in the priceline that we use in completions
					//float ask = pp.currBuyPrice[a];
					float ask = 0;//AMU is going to average bids. it needs bids even if they are below the current price
					float bid = utilities[q] - utilities[q-1];
					if(stopBidding(bid, ask)) {
						// Misc.println("stopping flight mu calculation. difference in utilities for " + q + "th copy smaller than current price. " + utilities[q] + " - " + utilities[q-1] + " <= " + ask);
						break;
					}
					b.addBidPoint(1, bid);
				}
			}
			
			if (b.getQuantity() > 0) {
				Misc.println("MUBidder.decideFlightPolicyWithMU : flights : Bid for auction " + b.getAuction() + ": " + b.getBidString());
				ret.add(b);
			}
		} 
		
		return ret;
	}
}


