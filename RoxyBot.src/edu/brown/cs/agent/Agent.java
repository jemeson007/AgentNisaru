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
 * Agent
 *
 * Author  : Seong Jae Lee
 * Created : 15 May, 2007
 * 
 * Agent
 * --RealAgent
 * --SimulatedAgent
 * 
 * AttacAlgorithm is an algorithm used no-event simulation game.
 * 
 */

package edu.brown.cs.agent;

import java.util.ArrayList;

import edu.brown.cs.props.GoodsChooser;
import edu.brown.cs.tac.Constants;
import edu.brown.cs.tacds.*;
import edu.brown.cs.algorithm.*;
import se.sics.tac.aw.AgentImpl;
import se.sics.tac.aw.Bid;
import se.sics.tac.aw.Quote;
import se.sics.tac.aw.Transaction;
import se.sics.tac.util.ArgEnumerator;
import props.*;

public class Agent extends AgentImpl {
	protected Repository repository;
	protected Algorithm algorithm;
	public int flight_bidding = FLIGHT_NO_MODIFICATION;
	public static final int FLIGHT_NO_MODIFICATION = 0; // no modification
	public static final int FLIGHT_BESTTIME = 1;        // bid when curr < exp. future min price
	public static final int FLIGHT_LASTTIME = 2;        // bid at the end of the game
	public static final int FLIGHT_ATTAC = 3;           // use ATTAC algorithm
	public static final int FLIGHT_FIRSTTIME = 4;       // bid at the first of the game
	
	public Agent() {
		algorithm = null;
		repository = new Repository();
	}
	
	public Repository getRepository () {return repository; }
	public Algorithm getAlgorithm() { return algorithm; }
	public int getFlightBidding() { return flight_bidding; }
	public void setAlgorithm (Algorithm a) { algorithm = a; }
	public void setRepository (Repository r) { repository = r; }
	public void setFlightBidding (int o) { flight_bidding = o; }
	
	public void bidRejected(Bid bid) {
		Misc.error(getClass().getSimpleName() + ".bidRejected");
		Misc.error("\t auction = " + Constants.auctionNoToString(bid.getAuction()));
		Misc.error("\t askPrice = " + repository.getQuote(bid.getAuction()).getAskPrice());
		Misc.error("\t hqw = " + repository.getQuote(bid.getAuction()).getHQW());
		Misc.error("\t rejected reason = " + bid.getRejectReasonAsString());
		Misc.error("\t rejected bid = " + bid.getBidString());
		Misc.error("\t repository bid = " + repository.getBid(bid.getAuction()).getBidString());
	}

	public void bidError(Bid bid, int error) {
		Misc.error(getClass().getSimpleName() + ".bidError");
		Misc.error("\t auction = " + Constants.auctionNoToString(bid.getAuction()));
		Misc.error("\t askPrice = " + repository.getQuote(bid.getAuction()).getAskPrice());
		Misc.error("\t hqw = " + repository.getQuote(bid.getAuction()).getHQW());
		Misc.error("\t error = " + error);
		Misc.error("\t errored bid = " + bid.getBidString());
		Misc.error("\t repository bid = " + repository.getBid(bid.getAuction()).getBidString());
	}

	protected void init (ArgEnumerator args) { }

	public void gameStarted() {
		Misc.println(getClass().getSimpleName() + ".gameStarted");
		
		repository.init();
		algorithm.setFields(repository, new GoodsChooser(repository, algorithm));
	}

	public void gameStopped() {
		Misc.println(getClass().getSimpleName() + ".gameStopped " +
				": " + repository.getTimeAsString());
	}
	
	public void quoteUpdated (Quote quote) {
		// Misc.println(getClass().getSimpleName() + ".quoteUpdated " +
		//		": " + quote.getAuction());
		
		repository.setQuote(quote);
	}
	
	public void quoteUpdated (int category) { 
		Misc.println(getClass().getSimpleName() + ".quoteUpdated " +
				": category " + category + 
				", " + repository.getTimeAsString());
	}
	
	public void bidUpdated (Bid bid) {
		// Misc.println("bidUpdated" + bid);
		repository.setBid(bid); 
	}

	public void auctionClosed(int auctionNo) {
		Misc.println(getClass().getSimpleName() + ".auctionClosed " +
				": " + auctionNo + 
				", " + repository.getTimeAsString());
		
		// sometimes same auctionClosed is called more than once
		if (!repository.isAuctionClosed(auctionNo))
			repository.closeAuction(auctionNo);
	}
	
	public void transaction (Transaction trans) {
		Misc.println(getClass().getSimpleName() + ".transaction " +
				": " + repository.getTimeAsString() + 
				", " + Constants.transactionToString(trans));
		
		TACCollection col = new TACCollection();
		for (int auctionNo = 0; auctionNo < Constants.AUCTION_MAX; auctionNo++) {
			col.setOwn (auctionNo, agent.getOwn(auctionNo));
		}
		
		repository.setOwnCollection(col);
	}
	
	public void placeBids (ArrayList<Bid> bids) {
	}
	
	public String toString () {
		return algorithm.toString();
	}
}
