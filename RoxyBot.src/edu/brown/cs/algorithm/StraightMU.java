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
 * StraightMU
 *
 * Author  : Victor Naroditskiy
 * Created : 29 October, 2005
 */

package edu.brown.cs.algorithm;

import java.util.*;

import props.Misc;

import edu.brown.cs.tacds.*;
import edu.brown.cs.tac.*;
import edu.brown.cs.bidder.*;
import edu.brown.cs.props.BidValidator;
import se.sics.tac.aw.Bid;

public class StraightMU extends Algorithm{
	private boolean use_old_mu = false;
	
	public void useOldMU () { use_old_mu = true; }
	public void useNewMU () { use_old_mu = false; }
	
	public StraightMU (int s) {
		super(s, 1, 0);
	}
	
	public ArrayList<Bid> getIdealBids() {
		Priceline[] scenarios = repository.getXMostRecent(numScenarios);
		Priceline average = averageScenarios(scenarios);
		Misc.println("StraightMU.getIdealBids : avg scenario " + average);
		
		Priceline[] averageArray = new Priceline[1];
		averageArray[0] = average;
		
		int[] goodsToBidOn = goodsChooser.goodsToBidOn(Constants.ALL, averageArray);//all goods	
		
		MUBidder bidder;
		if (use_old_mu) bidder = new MUOldBidder(repository);
		else bidder = new MUBidder(repository);
		
		ArrayList<Bid> ret = new ArrayList<Bid>();
		ret.addAll(bidder.decideHotelPolicy(goodsToBidOn, average, false));
		ret.addAll(bidder.decideFlightPolicyWithMU(average));
		ret.addAll(bidder.decideEventPolicyWithMU(average, false));
		
		return BidValidator.getConsistentBids(ret, repository, average);
	}
	
	public boolean usesAvgScenario() {
		return true;
	}

	public String toString() {
		String ret = "";
		ret += "SMU";
		ret += this.use_old_mu ? "O" : "N" ;
		ret += "-";
		ret += super.toString();
		return ret;
	}
}

