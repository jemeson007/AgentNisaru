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
 * MUBidder
 *
 * Author  : Seong Jae Lee
 * Created : 15 May, 2007
 * 
 */

package edu.brown.cs.bidder;

import java.text.DecimalFormat;
import java.util.*;

import se.sics.tac.aw.*;
import edu.brown.cs.tac.Constants;
import edu.brown.cs.tacds.*;
import edu.brown.cs.completer.*;
import props.Misc;

public class MUBidder{
	protected Repository m_dr;
	protected PackageCompleter m_completer;
	protected float [] eventMaxReward;
	protected float [] eventMinReward;
	
	protected static DecimalFormat formatter = new DecimalFormat("000.0");
	
	public MUBidder(Repository dr) {
		m_dr = dr;
		m_completer = new PackageCompleter();
		eventMaxReward = new float[Constants.AUCTION_MAX];
		eventMinReward = new float[Constants.AUCTION_MAX];
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
			int[] good_to_bid, 
			Priceline price, 
			boolean set_infinity) {
		
		Misc.println(getClass().getSimpleName() + ".decideHotelPolicy " +
				": price " + price + 
				", good_to_bid "+ good_to_bid[8]);
		ArrayList<Bid> ret = new ArrayList<Bid>();
		Preference[] preference = m_dr.getClientPrefs();
		
		for (int a = 8; a < 16; a++) {
			if (good_to_bid[a] != 0 && m_dr.isAuctionClosed(a)) {
				Misc.error(getClass().getSimpleName() + ".decideHotelPolicy " +
						": bid on closed auction " + a);
				Misc.myassert(false);
			}
		}
		
		// construct hotel bids with MU for each hotels and each qth good.
		for (int a = 8; a < 16; a++) {
			if (!bidInAuction(a, m_dr)) continue;
			if (good_to_bid[a] == 0) continue;
			
			Bid bid = new Bid(a);
			int max_q = good_to_bid[a]+1;
			TACCollection own = m_dr.getOwnCollection();
			TACCollection available = Constants.forSale();			
			
			for (int h = 8; h < 16; h++) {
				if (m_dr.isAuctionClosed(h)) available.setOwn(h, 0);
				if (set_infinity) available.setOwn(h, good_to_bid[h]);
			}
			
			// this is the new definition of MU. April 2, 2007, sjlee.
			for (int q = 1; q < max_q; q++) {
				
				float mu = 0;
				
				own.setOwn(a, q);
				available.setOwn(a, 8);
				mu += m_completer.computeCompletion(preference, own, available, price).objectiveValue;
				mu -= (q-1) * price.currBuyPrice[a];
				
				own.setOwn(a, 0);
				available.setOwn(a, q-1);
				mu -= m_completer.computeCompletion(preference, own, available, price).objectiveValue;
				
				if (stopBidding(mu, m_dr.getQuote(a).getAskPrice())) break;
				
				bid.addBidPoint(1, mu);
				
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
		Misc.println("This part is not implemented yet.");
		Misc.myassert(false);
		
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
				Misc.println("MUBidder.decideEventPolicyWithMU : " +
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

	/**
	 * Calculate a completion, and bid as it says. We proved that if a good is in our
	 * optimal solution, its MU is higher than the price. So we don't have to
	 * calculate all MUs. sjlee.
	 */
	public ArrayList<Bid> decideEventPolicyWithCompletion (Priceline prices) {
		ArrayList<Bid> ret = new ArrayList<Bid>();
		if (!Constants.EVENTS_ON) return ret;
		
		TACCollection holdingGood = m_dr.getOwnCollection();
		TACCollection availableGood = Constants.forSale();
		for (int h = 8; h < 16; h++) {
			if (m_dr.isAuctionClosed(h)) availableGood.setOwn(h, 0);
		}

		Completion comp = m_completer.computeCompletion(
				m_dr.getClientPrefs(),
				holdingGood, availableGood, prices);

		for (int a = 16; a < 28; a++) {
			Bid b	= new Bid(a);

			// We bid for entertainment only one quantity per each time. sjlee.
			if (comp.getBidPolicy().getQuantity(a) > 0) {
				b.addBidPoint(1, prices.currBuyPrice[a]);
				Misc.println("MUBidder.decideHotelPolicy : " +
						"adding event  buy-bid: a " + b.getAuction() +
						" string " + b.getBidString());
			}

			if (comp.getAskPolicy().getQuantity(a) > 0) {
				Misc.myassert(1 <= holdingGood.getOwn(a));
				b.addBidPoint(-1, prices.currSellPrice[a]);
				Misc.println("MUBidder.decideHotelPolicy : " +
						"adding event sell-bid: a " + b.getAuction() +
						", string " + b.getBidString() +
						", holding " + holdingGood.getOwn(a));
			}

			ret.add(b);
		}

		return ret;
	}

	public ArrayList<Bid> decideFlightPolicyWithCompletion (int[] flightsToBidOn, Priceline pp) {
		Misc.println("MUBidder.decideFlightPolicyWithCompletion");
		
		ArrayList<Bid> ret = new ArrayList<Bid>();
		
		for (int a = 0; a < 8; a++) {
			if (!bidInAuction(a, m_dr)) continue;
			
			//!!document
			int buyQuantity = flightsToBidOn[a];//subtracted in GoodsChooser
			if (buyQuantity > 0) {
				Bid b = new Bid(a);
				for (int q = 0; q < buyQuantity; q++) {
					b.addBidPoint(1, 1000);
				}
				ret.add(b);
				Misc.println("MUBidder.decideFlightPolicyWithCompletion : added flight bid: a " + b.getAuction() + " string " + b.getBidString());
			}
		}

		return ret;
	}

	public ArrayList<Bid> decideFlightPolicyWithMU(Priceline pp) {
		Misc.println(getClass().getSimpleName() + ".decideFlightPolicyWithMU");

		ArrayList<Bid> ret = new ArrayList<Bid>();
			
		for (int a = 0; a < 8; a++) {
			if (!bidInAuction(a, m_dr)) continue;
			
			Bid bid = new Bid(a);
			int max_q = 8 - m_dr.getOwn(a);
			TACCollection own = m_dr.getOwnCollection();
			TACCollection available = Constants.forSale();
			
			for (int h = 8; h < 16; h++) {
				if (m_dr.isAuctionClosed(h)) available.setOwn(h, 0);
			}
			
			for (int q = m_dr.getOwn(a) + 1; q <= max_q; q++) {
				
				float mu = 0;
				
				own.setOwn(a, q);
				available.setOwn(a, max_q);
				mu += m_completer.computeCompletion(m_dr.getClientPrefs(), own, available, pp).objectiveValue;
				mu -= (q-1-m_dr.getOwn(a)) * pp.currBuyPrice[a];
				
				own.setOwn(a, m_dr.getOwn(a));
				available.setOwn(a, q-1);
				mu -= m_completer.computeCompletion(m_dr.getClientPrefs(), own, available, pp).objectiveValue;
				
				if (stopBidding(mu, 0)) break;
				
				bid.addBidPoint(1, mu);
			}
			
			if (bid.getNoBidPoints() > 0) ret.add(bid);
			
		}

		return ret;
	}

	// Run at the end of a game
	public void cleanUp() { }

	/*
	 * Initialize the Bidder at the beginning of every game
	 */
	public void init() {
		Arrays.fill(eventMaxReward, 0);
		Arrays.fill(eventMinReward, 200);

		// get our customer prefs for the clients' event values
		Preference[] cprefs = m_dr.getClientPrefs();

		// find the max and min rewards we could possibly receive for owning
		// each type of event ticket -- for each type of event ticket, this is
		// the maximum/minimum value of that event ticket among our 8 clients.
		for (int c = 0; c < cprefs.length; c++) {
			for (int a = Constants.AUCTION_ALLIGATOR; a < Constants.AUCTION_MAX; a++) {
				if (a < Constants.AUCTION_AMUSEMENT) {
					float event1Value = cprefs[c].getEvent1Value();
					if (event1Value > eventMaxReward[a])
						eventMaxReward[a] = event1Value;
					if (event1Value < eventMinReward[a])
						eventMinReward[a] = event1Value;
				} else if (a < Constants.AUCTION_MUSEUM) {
					float event2Value = cprefs[c].getEvent2Value();
					if (event2Value > eventMaxReward[a])
						eventMaxReward[a] = event2Value;
					if (event2Value < eventMinReward[a])
						eventMinReward[a] = event2Value;
				} else {
					float event3Value = cprefs[c].getEvent3Value();
					if (event3Value > eventMaxReward[a])
						eventMaxReward[a] = event3Value;
					if (event3Value < eventMinReward[a])
						eventMinReward[a] = event3Value;
				}
			}
		}
	}

	//stop bidding if ask is higher than bid or mu is zero
	public boolean stopBidding(float bid, float ask) {
		//add .009 because comparing floats seems to be unreliable when they are equal. we want to bid when bid == ask. vn
		if(bid +.009 < ask || bid <= 0) {
			return true;
		}
		return false;
	}

	/**
	 *  Returns if we can bid for the auction or not. In the real game, we just
	 *  check if the auction is closed or not (in case of hotel). But for simulation,
	 *  we should consider carefully.
	 */ 
	public static boolean bidInAuction(int a, Repository dr) {
		if (dr.isAuctionClosed(a)) return false;
		
		// Misc.println("MUBidder.bidInAuction : bidding.  auc " + a);
		return true;
	}

}


