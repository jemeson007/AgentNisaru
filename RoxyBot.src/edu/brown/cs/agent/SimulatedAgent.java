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
 * SimulatedAgent
 *
 * Author  : Seong Jae Lee
 * Created : 15 May, 2007
 * 
 */

package edu.brown.cs.agent;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;

import props.Misc;
import se.sics.tac.aw.Bid;
import se.sics.tac.aw.Transaction;
import edu.brown.cs.algorithm.SAA;
import edu.brown.cs.modeler.FlightPredictor;
import edu.brown.cs.modeler.FlightPredictorAttac;
import edu.brown.cs.tac.Constants;
import edu.brown.cs.tacds.Priceline;
import edu.brown.cs.tacds.TACCollection;

// for the first minute, we should predict hotels after 'quote updated' are finished,
// because we need all the flight prices.
// for the other minutes, we should predict hotels after 'quote updated' are finished,
public class SimulatedAgent extends Agent {
	private ArrayList<Bid> bids;
	private ArrayList<Bid> flight_bids;
	private ArrayList<Bid> hotel_bids;
	private Priceline[] scenario;
	public final int agent_id;
	
	public SimulatedAgent(int no, String file, HashMap<String, String> map) {
		super();
		
		agent_id = no;
		
		AgentFactory f = new AgentFactory();
		f.setMap(map);
		f.parse(file);
		f.create(this);
	}
	
	public void gameStarted() {
		super.gameStarted();
		
		bids = null;
		flight_bids = null;
		hotel_bids = null;
	}
	
	public void calculateBids () {
		repository.hotel.predictX(algorithm.getNumScenariosToGenerate());
		scenario = repository.getAvailableScenarios();
		if (algorithm instanceof SAA) repository.flight.predict(repository, FlightPredictor.PREDICT_SAA);
		else repository.flight.predict(repository, FlightPredictor.PREDICT_MU);
		if (Constants.EVENTS_ON) repository.event.predict(repository);
		bids = algorithm.getBids();
		
		Misc.myassert(hotel_bids==null);
		flight_bids = new ArrayList<Bid>();
		hotel_bids = new ArrayList<Bid>();
		
		Iterator<Bid> iter = this.bids.iterator();
		while(iter.hasNext()) {
			Bid bid = iter.next();
			Misc.myassert(bid.getAuction()<16);
			if(bid.getAuction()<8) flight_bids.add(bid);
			else if(bid.getAuction()<16) hotel_bids.add(bid);
		}
		
		bids = null;
	}
	
	public ArrayList<Bid> getHotelBids () {
		Misc.println(getClass().getSimpleName() + ".getHotelBids " +
				": size " + hotel_bids.size());
		ArrayList<Bid> ret = hotel_bids;
		hotel_bids = null;
		
		return ret;
	}
	
	public ArrayList<Bid> getFlightBids () {
		Misc.println(getClass().getSimpleName() + ".getFlightBids " + 
				": option " + flight_bidding + 
				", time " + repository.getGameDecisec());
		
		ArrayList<Bid> ret = null;
		
		switch (flight_bidding) {
		case FLIGHT_NO_MODIFICATION:
			ret = flight_bids;
			flight_bids = null;
			
			break;
		
		case FLIGHT_BESTTIME:
			for (int i=flight_bids.size()-1; i>=0; i--) {
				Bid bid = flight_bids.get(i);
				if (repository.flight.buyNow(bid.getAuction(), repository.getGameDecisec())) {
					flight_bids.remove(i);
					if (ret == null) ret = new ArrayList<Bid>();
					ret.add(bid);
				}
			}
			
			break;
			
		case FLIGHT_FIRSTTIME:
			if (repository.getGameDecisec() == 5) {
				ret = flight_bids;
				flight_bids = null;
			}
			
			if (repository.getGameMinute() >= 1) {
				flight_bids = null;
			}
			
			break;
			
		case FLIGHT_LASTTIME:
			if (repository.getGameDecisec() == 53) {
				ret = flight_bids;
				flight_bids = null;
			}
			
			if (repository.getGameMinute() <= 7) {
				flight_bids = null;
			}
			
			break;
			
		case FLIGHT_ATTAC:
			
			if (repository.getGameDecisec() % 6 != 0) break;
			Misc.myassert(scenario != null);
			
			AttacAlgorithm attac = new AttacAlgorithm();
			attac.agent = this;
			attac.scenario = scenario;
			attac.update(flight_bids);
			ret = attac.run();
			
			flight_bids = null;
			
			// in case of SAA, we should set future flight prediction to infinity
			Misc.myassert(repository.flight instanceof FlightPredictorAttac);
			
			break;
		}
		
		return ret;
	}

	
	public void placeBids (ArrayList<Bid> bids) {
		this.bids = bids;
	}
	
	public void transaction(Transaction t) {
		Misc.println(getClass().getSimpleName() + ".transaction " +
				": auction " + t.getAuction() + 
				", " + repository.getTimeAsString());
		
		TACCollection col = new TACCollection(repository.getOwnCollection());
		int auctionNo = t.getAuction();
		col.setOwn (auctionNo, repository.getOwn(auctionNo) + t.getQuantity());
		
		repository.setOwnCollection(col);
		repository.account -= t.getPrice() * t.getQuantity();
	}

	public void setNextClosingAuction (int a) {
		
	}
}
