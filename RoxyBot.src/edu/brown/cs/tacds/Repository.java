/**
 * tacds
 *
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
 * Repository
 *
 * Author  : Seong Jae Lee
 * Created : 5 May, 2007
 * Description: 
 *    
 * This class contains all the predictors and game informations.
 * 
 */

package edu.brown.cs.tacds;

import java.util.Vector;

import props.Misc;

import edu.brown.cs.modeler.EventPredictor;
import edu.brown.cs.modeler.FlightPredictor;
import edu.brown.cs.modeler.HotelPredictorCE;
import edu.brown.cs.tac.Constants;

import se.sics.tac.aw.Bid;
import se.sics.tac.aw.Quote;

public class Repository {
	public FlightPredictor flight;
	public HotelPredictorCE hotel;
	public EventPredictor event;
	public double account;
	
	protected TACCollection collection;
	protected Preference[] preferences;
	protected Vector<Priceline> scenarios;
	protected boolean[] auctionClosed;
	protected Bid [] bids;
	protected Quote [] quotes;
	
	protected long starttime;
	protected int time;

	public Repository () { }
	
	public void init () {
		Misc.println("Repository.init");
		
		Misc.myassert(flight != null);
		Misc.myassert(hotel != null);
		Misc.myassert(event != null);
		
		flight.init();
		hotel.init();
		event.init();
		
		collection = new TACCollection();
		preferences = new Preference[8];
		
		auctionClosed = new boolean[28];
		bids = new Bid[28];
		quotes = new Quote[28];
		
		scenarios = new Vector<Priceline>();

		account = 0;
	}
	
	public boolean isAuctionClosed(int a) { return auctionClosed[a]; }
	public void closeAuction(int a) { auctionClosed[a] = true; }
	
	public TACCollection getOwnCollection() { return new TACCollection(collection); }
	public void setOwnCollection(TACCollection col) { collection = new TACCollection(col); }
	public int getOwn (int a) { return collection.getOwn(a); }
	
	public Preference[] getClientPrefs() {
		Preference[] ret = new Preference[preferences.length];
		for (int i = 0; i < preferences.length; i++) {
			ret[i] = new Preference(preferences[i]);
		}
		return ret;
	}
	
	public void setClientPrefs(Preference [] _cprefs) {
		preferences = new Preference[_cprefs.length];
		for (int i = 0; i < preferences.length; i++) {
			preferences[i] = new Preference(_cprefs[i]);
		}
	}
	public Bid getBid (int a) { return bids[a]; }
	public void setBid (Bid b) {
		Misc.myassert(b != null);
		
		Bid toStore = new Bid(b.getAuction());
		
		for (int i = 0; i < b.getNoBidPoints(); i++) {
			toStore.addBidPoint(b.getQuantity(i), b.getPrice(i));
		}
		
		bids[b.getAuction()] = toStore;			
	}
	public Quote getQuote (int auctionNo) { return quotes[auctionNo]; }
	public void setQuote (Quote q) {
		Misc.myassert(q != null);
		quotes[q.getAuction()] = q;
		
		switch (Constants.auctionType(q.getAuction())) {
		case Constants.TYPE_FLIGHT:
			flight.setPrice(q.getAuction(), getGameDecisec(), (int) q.getAskPrice());
			break;
		case Constants.TYPE_HOTEL:
			break;
		case Constants.TYPE_EVENT:
			break;
		}
	}
	
	public int getNumClosedAuction() {
		int ret = 0;
		for (int a = 8; a < 16; a++) {
			if(this.auctionClosed[a]) ret++;
		}
		return ret;
	}

	public void setStartTime (long t) { starttime = t; }
	public void setGameTime (int t) { Misc.myassert(false); }
	public int getGameSec() { return (int) (System.currentTimeMillis() - starttime) / 1000; }
	public int getGameDecisec() { return getGameSec() / 10; }
	public int getGameMinute() {return getGameSec() / 60; }
	public String getTimeAsString () {
		long timeInSeconds = getGameSec();
		int sec = (int) (timeInSeconds % 60);
		StringBuffer sb = new StringBuffer();
		sb.append(timeInSeconds / 60).append(':');
		if (sec < 10) sb.append('0');
		return sb.append(sec).toString();
	}
	
	/*
	public Priceline uspdateFlightAndEvent (Priceline priceline, int currOption, int nextOption) {
		for (int a = 0; a < 8; a++) {
			priceline.currBuyPrice[a] = flight.getPrice(a, currOption, this.getGameDecisec(), (int)this.getQuote(a).getAskPrice());
			priceline.nextBuyPrice[a] = flight.getPrice(a, nextOption, this.getGameDecisec(), (int)this.getQuote(a).getAskPrice());
		}

		for (int a = 16; a < 28; a++) {
			priceline.currBuyPrice[a] = 200;
			priceline.currSellPrice[a] = 0;
		}
		
		return priceline;
	}
	*/
	
	public void removeXScenarios(int x) { ; }
	public int scenariosAvailable() { return scenarios.size(); }
	public void addScenario(Priceline s) { scenarios.add(new Priceline(s)); }
	public Priceline[] getXMostRecent(int x) {
		Priceline[] ret = new Priceline[x];
		for (int i = 0; i < x; i++) {
			ret[i] = new Priceline(scenarios.get(scenarios.size() - 1 - i));
		}
		return ret;
	}
	public Priceline[] removeXMostRecent(int x) {
		Misc.myassert(scenarios.size() >= x);
		Priceline [] ret = new Priceline[x];
		for (int i = 0; i < x; i++) {
			ret[i] = scenarios.remove(scenarios.size() - 1);
		}
		return ret;
	}
	
	public Priceline[] getAvailableScenarios() {
		int x = scenarios.size();
		Priceline [] ret = new Priceline[x];
		for (int i = 0; i < x; i++) {
			ret[i] = new Priceline((Priceline) scenarios.get(scenarios.size() - 1 - i));
		}
		return ret;
	}
	public void clearScenarios() {
		scenarios = new Vector<Priceline>();
	}

}
