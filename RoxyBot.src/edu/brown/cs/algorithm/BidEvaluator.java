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
 * Algorithm
 *
 * Author  : Victor Naroditskiy
 * Created : 29 October, 2005
 * 
 * what to bid on: completion with 1 AVERAGE scenario
 * what to bid: MU
 * 
 * 1. Generate one average scenario.
 * 2. Get optimal allocation for that scenario.
 * Note. hotel : We consider we already own hqw goods.
 */

package edu.brown.cs.algorithm;

import java.util.*;
import props.*;
import se.sics.tac.aw.Bid;
import edu.brown.cs.tacds.*;
import edu.brown.cs.tac.*;
import edu.brown.cs.bidder.*;
import edu.brown.cs.props.BidValidator;

public class BidEvaluator extends Algorithm {
	// when true, the prices of goods that are not part of the completion are set to infinity.
	// this increases the MUs of the goods in the completion. vn
	private boolean m_setOtherPricesToInfinity = false;
	private boolean use_old_mu = false;
	
	public void useOldMU () { use_old_mu = true; }
	public void useNewMU () { use_old_mu = false; }
	public void setExtreme (boolean e) { m_setOtherPricesToInfinity = e; }
	
	public BidEvaluator(int s, int p, int e) {
		super(s, p, e);
		m_setOtherPricesToInfinity = false;
	}
	
	public BidEvaluator(int s, int p, int e, boolean setOtherPricesToInfinity) {
		super(s, p, e);
		m_setOtherPricesToInfinity = setOtherPricesToInfinity;
	}
	
	public ArrayList<Bid> getIdealBids() {
		Priceline[] averageScenarioAr = new Priceline[1];
		Priceline averageScenario = averageScenarios(repository.getXMostRecent(numScenarios));
		averageScenarioAr[0] = averageScenario;
		Misc.println(getClass().getSimpleName() + ".getIdealBids " +
				": avg " + averageScenario);
		
		int[] hotelsAndFlightsToBidOn = goodsChooser.goodsToBidOn (Constants.INTERSECTION, averageScenarioAr);
		
		MUBidder bidder;
		if (use_old_mu) bidder = new MUOldBidder(repository);
		else bidder = new MUBidder(repository);
		
		ArrayList<Bid> bids = new ArrayList<Bid>();
		
		bids.addAll(bidder.decideHotelPolicy(hotelsAndFlightsToBidOn, averageScenario, m_setOtherPricesToInfinity));
		bids.addAll(bidder.decideFlightPolicyWithCompletion(hotelsAndFlightsToBidOn, averageScenario));
		bids.addAll(bidder.decideEventPolicyWithCompletion(averageScenario));
		
		return BidValidator.getConsistentBids(bids, repository, averageScenario);
	}
	
	public boolean usesAvgScenario() {
		if(numPolicies == 1) { // roxy2000 uses the average scenario.
			return true;
		}
		return false;
	}
	
	public String toString() {
		String ret = "";
		ret += this.numEvaluations>0 ? "02" : "00";
		ret += this.m_setOtherPricesToInfinity ? "T" : "F" ;
		ret += this.use_old_mu ? "O" : "N" ;
		ret += "-";
		ret += super.toString();
		return ret;
	}
	
	
}
