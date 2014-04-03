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
 * Created : 29 October, 2005
 * Description:
 * 
 */

package edu.brown.cs.props;

import java.util.Arrays;
import edu.brown.cs.tac.Constants;
import edu.brown.cs.tacds.*;
import edu.brown.cs.algorithm.Algorithm;
import edu.brown.cs.completer.PackageCompleter;
import props.Misc;
import se.sics.tac.aw.Bid;

public class GoodsChooser {
	Repository m_dr;
	Algorithm m_alg;

	public GoodsChooser(Repository dr, Algorithm alg) {
		m_dr = dr;
		m_alg = alg;
	}

	/**
	 * @param option
	 * 		1. Constants.ALL : return max quantity for each goods.
	 * 		2. Constants.UNION
	 * 		3. Constants.INTERSECTION
	 *
	 * @return quantity to bid on for each goods.
	 * 		i.e. union or intersection of best allocations for each scenarios.
	 */
	public int[] goodsToBidOn (final int option, Priceline[] scenarios) {
		// Misc.println("GoodChooser.goodsToBidOn : " + m_dr.scenariosAvailable() + " scenarios");

		if (Constants.ALL == option) {
			// a scenario has been generate above
			int[] ret = allGoods();
			for (int a = 8; a < 16; a++) if (m_dr.isAuctionClosed(a)) ret[a] = 0;
			return ret;
			
		}
		
		Misc.myassert(scenarios != null);
		Misc.myassert(scenarios.length != 0);

		/** building forSale **/
		// quantities of goods for sale
		TACCollection forSale = Constants.forSale();

		// can't buy closed hotel or flight auctions
		for (int a = 0; a < 16; a++) {
			if (m_dr.isAuctionClosed(a)) forSale.setOwn(a, 0);
		}

		// only consider buying 1 of each type of ticket per minute
		//for (int e = Constants.AUCTION_ALLIGATOR; e < Constants.AUCTION_MAX; e++) {
		//	forSale.setOwn(e, 1);
		//}

		/** building ideal completions **/
		Misc.println("GoodsChooser : computing \"ideal\" completions");
		PackageCompleter completer  = new PackageCompleter();
		Completion []	idealComps = new Completion[scenarios.length];

		// build completions for each scenario
		// pp : scenario price
		// howned : own collection or expected hqw
		for (int i = 0; i < idealComps.length; i++) {
			//Priceline pp = m_dr.updateFlightAndEvent(new Priceline(scenarios[i].priceline), true);
			Priceline pp = scenarios[i];

			// hypothetically owned collection
			TACCollection howned = m_dr.getOwnCollection();

			for (int a = Constants.AUCTION_BAD_HOTEL; a < Constants.AUCTION_BAD_HOTEL + 8; a++) {
				if (m_dr.isAuctionClosed(a)) continue;

				howned.setOwn(a, hqw(a, pp));
			}

			/*
			for (int a = 0; a < Constants.AUCTION_MAX; a++) {
				Misc.println(Constants.auctionNoToString(a) +
						":  owned   = " + howned.getOwn(a) +
						"\t, for sale = " + forSale.getOwn(a) +
						"\t,   buy	= " + pp.buyPrices[a] +
						"\t,   sell   = " + pp.sellPrices[a]);
			}
			*/

			Preference[] cprefs = m_dr.getClientPrefs();
			idealComps[i] = completer.computeCompletion(cprefs, howned, forSale, pp);

			if (idealComps[i] == null) {
				for (int j = 0; j < cprefs.length; j++) {
					Misc.println("GoodsChooser.goodsToBidOn : " + cprefs[j]);
				}
				Misc.println("GoodsChooser : " + howned);
				Misc.println("GoodsChooser : " + forSale);
				Misc.println("GoodsChooser : " + pp);
			}
		} // end of for, building completions for each scenarios.

		for (int i = 0; i < idealComps.length; i++) {
			if (idealComps[i] == null) {
				Misc.error("GoodsChooser : idealComps[" + i + "] is null.");
				Misc.myassert(false);
			}
		}

		/** Calculating intersection and union for each 'goods' **/
		Misc.println("GoodsChooser : intersecting allocations from IDEAL completions.");
		// for now, intersect the allocated goods across all scenarios
		// in the future consider:
		// find out which flights/events we want across all scenarios
		// and find out which hotels we want in any scenarios
		int [] intersection = new int[Constants.AUCTION_MAX];
		int [] union = new int[Constants.AUCTION_MAX];
		int [] goods = new int[Constants.AUCTION_MAX];

		// initialize intersection
		Allocation alloc = idealComps[0].getAllocation();
		for (int i = 0; i < alloc.getNumClients(); i++) {
			TravelPackage tp = alloc.getTravelPackage(i);
			for (int j = 0; j < Constants.AUCTION_MAX; j++) {
				intersection[j] += tp.containsGood(j) ? 1 : 0;
			}
		}

		// for each scenario's completion
		for (int i = 0; i < idealComps.length; i++) {
			Arrays.fill(goods, 0);
			alloc = idealComps[i].getAllocation();

			// find which goods are allocated in this completion
			for (int j = 0; j < alloc.getNumClients(); j++) {
				TravelPackage tp = alloc.getTravelPackage(j);
				for (int k = 0; k < Constants.AUCTION_MAX; k++) {
					goods[k] += tp.containsGood(k) ? 1 : 0;
				}
			}

			for (int a = 0; a < Constants.AUCTION_MAX; a++) {
				intersection[a] = Math.min(intersection[a], goods[a]);
				union[a] = Math.max(union[a], goods[a]);
			}
		} // end of for, each scenario's completion

		Misc.println("GoodsChooser : goods wanted in all scenarios (intersection)");
		printGoods(intersection);
		for(int g=0; g<intersection.length; g++) {
			Misc.myassert(intersection[g] <= Constants.NUM_CLIENTS_PER_AGENT);
			Misc.myassert(union[g] <= Constants.NUM_CLIENTS_PER_AGENT);
		}
		
		//Misc.println("Goods wanted in any scenario:");
		//printGoods(union);

		// remove this when you run bboy
		//can't check after we subtract what we own b/c transaction (ex flight purchase)
		//may occur after we substract from union but before we substract from intersection 
		for (int a = 0; a < Constants.AUCTION_MAX; a++) {
			Misc.myassert(scenarios.length == 1);
			Misc.myassert(union[a] == intersection[a]);
		}
		
		intersection = removeOwn(intersection);
		//Misc.println("Goods wanted in all scenarios minus what we own:");
		//printGoods(intersection);
		
		union = removeOwn(union);
		//Misc.println("Goods wanted in any scenario minus what we own:");
		//printGoods(union);

		if (Constants.UNION == option) {
			return union;
		}

		if (Constants.INTERSECTION == option) {
			return intersection;
		}

		Misc.error("GoodsChooser : unionIntersectionAll " + option);
		return null;
	}

	/*
	 * The hqw contained in hotel bids represents the quantity
	 * you would have won had the auction ended in the last minute.
	 * This hqw is meant to represent the quantity your current
	 * bid would win, assuming the prices passed in priceline are
	 * accurate.
	 *
	 * ## As of June 6, 2005 this is only works for hotels##
	 */
	private int hqw (int auctionNo, Priceline prices) {
		Bid b = m_dr.getBid(auctionNo);

		if (m_dr.isAuctionClosed(auctionNo)) {
			return 0;
		}

		if (b == null) {
			return 0;
		}

		int hqw = 0;
		for (int bidNo = 0; bidNo < b.getNoBidPoints(); bidNo++) {
			if (b.getPrice(bidNo) > prices.currBuyPrice[auctionNo]) {
				hqw += b.getQuantity(bidNo);
			}
		}

		if (Constants.SIMULATOR && !Constants.SIMULATOR_CANNOT_WITHDRAW_BIDS) {
			Misc.myassert(hqw == 0);
		}

		return hqw > Constants.MAX_QUANTITY ? Constants.MAX_QUANTITY : hqw;
	}

	private void printGoods(int[] goods) {
		String ret;
		
		ret = "GoodsChooser : flights ";
		for (int auction_id=0; auction_id<8; auction_id++) 
			ret += " " + goods[auction_id];
		Misc.println(ret);
		
		ret = "GoodsChooser : hotels  ";
		for (int auction_id=8; auction_id<16; auction_id++) 
			ret += " " + goods[auction_id];
		Misc.println(ret);
		
		if (Constants.EVENTS_ON) {
			ret = "GoodsChooser : events  ";
			for (int auction_id=16; auction_id<24; auction_id++) 
				ret += " " + goods[auction_id];
			Misc.println(ret);
		}
	}

	private int[] allGoods() {
		int[] ret = new int[Constants.AUCTION_MAX];
		Arrays.fill(ret, Constants.MAX_QUANTITY);
		return ret;
	}

	//public int[] noMoreThanMaxQuantity (int[] bidOn) {
	//	for (int i=0; i<bidOn.length; i++) {
	//		bidOn[i] = Math.min(bidOn[i], Constants.MAX_QUANTITY);
	//	}
	//	return bidOn;
	//}

	//do not need to bid on what we already have
	public int[] removeOwn (int[] bidOn) {
		for (int i=0; i<bidOn.length; i++) {
			bidOn[i] = Math.max(0, bidOn[i] - m_dr.getOwn(i));
		}
		return bidOn;
	}

}
