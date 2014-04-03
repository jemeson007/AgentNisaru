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
 * SAA
 *
 * Author  : Seong Jae Lee
 * Created : 5 May, 2007
 */

package edu.brown.cs.algorithm;

import java.util.ArrayList;

import props.Misc;
import se.sics.tac.aw.Bid;
import edu.brown.cs.bidder.*;
import edu.brown.cs.completer.SAACompleter;

public class SAA extends Algorithm {
	private SAABidder m_bidder;
	
	public void setBidder (SAABidder b) { m_bidder = b; }
	public SAABidder getBidder () {return m_bidder;}
	
	public SAA(int s) {
		super(s, 1, 0);
		m_bidder = new SAABidder(repository);
	}

	public ArrayList<Bid> getBids() {
		long start = System.currentTimeMillis();
		ArrayList<Bid> bids = m_bidder.decidePolicy(null); 
		long end = System.currentTimeMillis();

		Misc.println("SAA.getBids : took " + (int)((end-start)/1000.0) + " seconds.");
		
		return bids;
	}
	
	public String toString() {
		String ret = "SAA";
		
		int bidPolicy = ((SAACompleter)m_bidder.getCompleter()).getBidPolicy();
		
		if (bidPolicy == SAACompleter.BID_LOWEST) ret += "B";
		else ret += "T";
		
		if ( repository.hotel.isExtremeMax() && !repository.hotel.isExtremeMin()) ret += "M-";
		else if (!repository.hotel.isExtremeMax() &&  repository.hotel.isExtremeMin()) ret += "m-";
		else if ( repository.hotel.isExtremeMax() &&  repository.hotel.isExtremeMin()) ret += "X-";
		else ret += "-";
		
		ret += super.toString();
		return ret;
	}

	protected ArrayList<Bid> getIdealBids() {
		return null;
	}
}
