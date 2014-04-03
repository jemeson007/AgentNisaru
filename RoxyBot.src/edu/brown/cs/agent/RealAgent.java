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
 * RealAgent
 *
 * Author  : Seong Jae Lee
 * Created : 15 May, 2007
 * 
 */

package edu.brown.cs.agent;

import java.util.ArrayList;
import java.util.List;

import props.Misc;
import se.sics.tac.aw.Bid;
import se.sics.tac.aw.TACAgent;
import se.sics.tac.aw.Transaction;
import edu.brown.cs.algorithm.SAA;
import edu.brown.cs.modeler.EventAnalyzer;
import edu.brown.cs.modeler.FlightPredictor;
import edu.brown.cs.modeler.FlightPredictorAttac;
import edu.brown.cs.props.BidValidator;
import edu.brown.cs.tac.Constants;
import edu.brown.cs.tacds.Preference;
import edu.brown.cs.tacds.Priceline;
import edu.brown.cs.tacds.TACCollection;

// for the first minute, we should predict hotels after 'quote updated' are finished,
// because we need all the flight prices.
// for the other minutes, we should predict hotels after 'quote updated' are finished,
public class RealAgent extends Agent {
	protected Scheduler scheduler;
	protected boolean[] categoryUpdated;
	protected double account;
	protected RealAgent theAgent;
	private int gameID;
	
	public RealAgent() {
		super();
		
		AgentFactory f = new AgentFactory();
		f.parse();
		f.create(this);
		
		theAgent = this;
		
		account = 0;
	}
	
	public void gameStarted() {
		super.gameStarted();
		
		Misc.println(getClass().getName() + ".gameStarted");

		scheduler = null;
		categoryUpdated = new boolean[3];
		for (int i = 0; i < 3; i++) categoryUpdated[i] = false;
		
		// Init Time
		repository.setStartTime(System.currentTimeMillis() - agent.getGameTime());
		
		// Init Prefs
		Preference[] cprefs = new Preference[8];
		Misc.println(getClass().getName() + ".gameStarted : clients");
		for (int client = 0; client < 8; client++) {
			cprefs[client] = new Preference(
					agent.getClientPreference(client, TACAgent.ARRIVAL),
					agent.getClientPreference(client, TACAgent.DEPARTURE),
					agent.getClientPreference(client, TACAgent.HOTEL_VALUE),
					agent.getClientPreference(client, TACAgent.E1),
					agent.getClientPreference(client, TACAgent.E2),
					agent.getClientPreference(client, TACAgent.E3));
			Misc.println("\t" + cprefs[client]);
		}
		repository.setClientPrefs(cprefs);
		
		// Init Collection
		TACCollection col = new TACCollection();
		for (int auctionNo = 0; auctionNo < Constants.AUCTION_MAX; auctionNo++) {
			col.setOwn(auctionNo, Math.max(0, agent.getOwn(auctionNo)));
		}
		repository.setOwnCollection(col);
		
		// If we started this agent in the middle of the game,
		// trasaction functions are called before gameStarted function.
		repository.account = account;
		
		this.gameID = agent.getGameID();
	}
	
	public void auctionClosed(int a) {
		super.auctionClosed(a);
		
		if (Constants.auctionType(a) != Constants.TYPE_HOTEL) return;
		if (repository.getNumClosedAuction() != repository.getGameSec() / 60) {
			Misc.warn(getClass().getSimpleName() + ".auctionClosed : " +
					"started in the middle " +
					", #closed " + repository.getNumClosedAuction() + 
					", minute " + repository.getGameSec() / 60);
			return;
		}
		if (repository.getGameDecisec() % 6 > 3) return;
		if (scheduler != null) {
			Misc.warn(getClass().getSimpleName() + ".auctionClosed : scheduler is still running");
			return;
		}
		
		scheduler = new Scheduler();
		scheduler.start();
	}
	
	public void transaction(Transaction t) {
		super.transaction(t);

		// If we started this agent in the middle of the game,
		// trasaction functions are called before gameStarted function.
		repository.account -= t.getPrice() * t.getQuantity();
		account -= t.getPrice() * t.getQuantity();
	}
	
	public void quoteUpdated (int category) {
		super.quoteUpdated(category);
		
		// this part is hacky. because we do want to run it between 10 ~ 30 sec
		// after all the quotes are updated.
		if (repository.getGameDecisec() > 3) return;
		
		int count = 0;
		categoryUpdated[category] = true;
		for (int i = 0; i < 3; i++) if (categoryUpdated[i]) count++;
		
		if (scheduler == null && count == 3) {
			scheduler = new Scheduler();
			scheduler.start();
			for (int i = 0; i < 3; i++) categoryUpdated[i] = false;
		}
	}
	
	public void gameStopped() {
		super.gameStopped();
		account = 0;
		
		if (repository.event instanceof EventAnalyzer) {
			long end = System.currentTimeMillis() + 10000;
			while (System.currentTimeMillis() < end);
			
			Misc.println(getClass().getSimpleName() + ".gameStopped : download bidding history for entertainment prediction.");
			Misc.println(getClass().getSimpleName() + ".gameStopped : host " + agent.getHost() + " , gameID " + gameID);
			((EventAnalyzer) repository.event).downloadFromHost(agent.getHost(), gameID);
			((EventAnalyzer) repository.event).readLog(gameID);			
		}
	}
	
	public class Scheduler extends Thread {
		private Priceline[] predictions;
		
		public void run () {
			Misc.println(getClass().getSimpleName() + ".run : " + repository.getTimeAsString());
			
			repository.hotel.predictX(algorithm.getNumScenariosToGenerate()); // hotel predicted
			if (algorithm instanceof SAA) repository.flight.predict(repository, FlightPredictor.PREDICT_SAA);
			else repository.flight.predict(repository, FlightPredictor.PREDICT_MU);
			if (Constants.EVENTS_ON) repository.event.predict(repository);
			
			predictions = new Priceline[repository.getAvailableScenarios().length];
			for (int i = 0; i < predictions.length; i++) {
				predictions[i] = new Priceline(repository.getAvailableScenarios()[i]);
			}
			
			placeBids(algorithm.getBids());
			
			scheduler = null;
		}
		
		public void wait(int milliseconds) {
			long end = System.currentTimeMillis() + milliseconds;
			while (System.currentTimeMillis() < end);
		}
		
		public void placeBids(ArrayList<Bid> bids) {
			if (bids == null) return;

			// BID CANCELLATION
			for (int a = 0; a < 28; a++) {
				if (Constants.auctionType(a) == Constants.TYPE_HOTEL) continue;
				if (Constants.auctionType(a) == Constants.TYPE_EVENT && !Constants.EVENTS_ON) continue;
				Bid b = new Bid(a);
				b.addBidPoint(1,0);
				agent.submitBid(b);
				
			}
			
			long tic = System.currentTimeMillis();
			boolean areCanceled = false;
			while (!areCanceled) {
				wait(1000);
				areCanceled = true;
				for (int a = 0; a < 28; a++) {
					if (Constants.auctionType(a) == Constants.TYPE_HOTEL) continue;
					if (Constants.auctionType(a) == Constants.TYPE_EVENT && !Constants.EVENTS_ON) continue;
					if (!BidValidator.bidCancelled(repository.getBid(a))) areCanceled = false;
				}
			}
			long toc = System.currentTimeMillis();
			Misc.println(getClass().getSimpleName() + ".run : bid cancellation took " + (toc - tic) + " milliseconds.");
			
			// FLIGHT BIDDING POLICY
			if (flight_bidding == FLIGHT_ATTAC) {
				Misc.myassert(repository.flight instanceof FlightPredictorAttac);

				List<Bid> flight_bids = new ArrayList<Bid>();
				for (Bid bid : bids) {
					if (Constants.auctionType(bid.getAuction()) != Constants.TYPE_FLIGHT) continue;
					bids.remove(bid);
					flight_bids.add(bid);
				}
				
				AttacAlgorithm attac = new AttacAlgorithm();
				attac.agent = theAgent;
				attac.scenario = predictions;
				attac.update(flight_bids);
				flight_bids = attac.run();
				
				for (Bid bid : flight_bids) {
					bids.add(bid);
				}
			}
			
			for (Bid bid : bids) {
				if (Constants.auctionType(bid.getAuction()) != Constants.TYPE_FLIGHT) continue;
				
				switch (flight_bidding) {
				case FLIGHT_NO_MODIFICATION: break;
				case FLIGHT_FIRSTTIME:
					if (repository.getGameMinute() >= 1) bids.remove(bid);
					break;
				case FLIGHT_BESTTIME:
					if (!repository.flight.buyNow(bid.getAuction(), repository.getGameDecisec())) bids.remove(bid);
					break;
				case FLIGHT_LASTTIME:
					if (repository.getGameMinute() <= 7) bids.remove(bid);
					break;
				case FLIGHT_ATTAC:					
					break;
				default: Misc.myassert(false);
				}
			}
			
			for (int i = 0; i < bids.size(); i++) {
				agent.submitBid(bids.get(i));
			}
		}
		
	}
}
