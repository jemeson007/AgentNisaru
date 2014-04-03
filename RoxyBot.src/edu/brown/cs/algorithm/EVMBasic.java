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
 * EVMBasic
 *
 * Author  : Victor Naroditskiy
 * Created : 29 October, 2005
 * 
 * what to bid on: completion with 1 AVERAGE scenario
 * what to bid: scenario's price
 */

package edu.brown.cs.algorithm;

import java.util.*;

import se.sics.tac.aw.Bid;
import edu.brown.cs.tacds.*;
import edu.brown.cs.tac.*;
import edu.brown.cs.bidder.*;
import edu.brown.cs.props.BidValidator;

public class EVMBasic extends Algorithm {

	public EVMBasic(int s, int p, int e) {
		super(s, p, e);
	}
	
	public ArrayList<Bid> getIdealBids() {
        Priceline average = averageScenarios(repository.getXMostRecent(numScenarios));
		MVBidder bidder = new MVBidder(repository);
		ArrayList<Bid> bids = new ArrayList<Bid>();
		
        // get an optimal allocation for the average scenario.
		Priceline[] averageScenarioAr = new Priceline[1];
		averageScenarioAr[0] = average;
        int[] goodsToBidOn = goodsChooser.goodsToBidOn (Constants.INTERSECTION, averageScenarioAr);
		
		//Priceline prices = m_dr.updateFlightAndEvent(average.priceline, true);

        // bid
        bids.addAll(bidder.getBid(goodsToBidOn, average));
		
		return BidValidator.getConsistentBids(bids, repository, average);
	}
	
	public boolean usesAvgScenario() {
		if(numPolicies == 1) {
			return true;
		}
		return false;
	}

	public String toString() {
		return " EVM " + super.toString();
	}
}
