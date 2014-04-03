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
 * HotelEngineOfflineSequential
 *
 * Author  : Seong Jae Lee
 * Created : 15 May, 2007
 * Description:
 * 
 */

package edu.brown.cs.simulator;

import props.Misc;
import se.sics.tac.aw.Quote;
import se.sics.tac.aw.Transaction;
import edu.brown.cs.agent.SimulatedAgent;
import edu.brown.cs.modeler.EventPredictor;
import edu.brown.cs.modeler.FlightPredictor;
import edu.brown.cs.modeler.FlightPredictorConst;
import edu.brown.cs.modeler.HotelPredictorCE;
import edu.brown.cs.tac.Constants;
import edu.brown.cs.tacds.Client;
import edu.brown.cs.tacds.Priceline;
import edu.brown.cs.tacds.RepositorySimulator;

public class HotelEngineOfflineSequential extends HotelEngine {
	
	protected FlightPredictor flight_predictor;
	protected HotelPredictorCE hotel_predictor;
	
	public HotelEngineOfflineSequential (SimulatedAgent[] a) { 
		super(a); 
		
		flight_predictor = agents[0].getRepository().flight;
		hotel_predictor = agents[0].getRepository().hotel;
	}
	
	//@ override
	protected double findPrice(GameInfo info, int auction_id, int minute) {
		Misc.myassert(false);
		return 0;
	}
	
	private void calculateClosingPrice(GameInfo info) {
		Priceline closing_price = new Priceline();

		RepositorySimulator r = new RepositorySimulator();
		r.setGameTime(info.current_decisec);
		r.flight = new FlightPredictorConst(0);
		r.hotel = new HotelPredictorCE(r);
		r.event = new EventPredictor();
		r.init();
		
		for (int a = 0; a < 8; a++) {
			Quote q = new Quote(a);
			q.setAskPrice(info.flight_price[info.current_decisec][a]);
			r.setQuote(q);
		}
		
		closing_price = flight_predictor.predict(r, FlightPredictor.PREDICT_TAT, closing_price);
		
		for (int a = 8; a < 16; a++) {
			if (!info.hotel_open.contains(new Integer(a))) closing_price.currBuyPrice[a] = Constants.INFINITY;
			else closing_price.currBuyPrice[a] = Math.max(0.001f, info.hotel_price[a].getLast().floatValue());
		}
		
		int numClosed = 0;
		for (int a = 8; a < 16; a++) if (!info.hotel_open.contains(new Integer(a))) numClosed++;
		if (numClosed == 8) return; // all hotels are closed.
		
		Client[] clients = new Client[8];
		for (int i = 0; i < 8; i++) clients[i] = info.clients.get(i);
		closing_price = hotel_predictor.getTatonnementer().predictHotel(clients, closing_price);
		
		for (int a = 8; a < 16; a++) {
			if (!info.hotel_open.contains(new Integer(a))) continue;
			info.hotel_price[a].add((double) closing_price.currBuyPrice[a]);
		}		
	}
	
	public void setHotelQuote(GameInfo info) {
		for (int auction : info.hotel_open) {
			/** find price **/
			double ask_price = info.hotel_price[auction].getLast().floatValue();
			Misc.println("%,htl,"+info.current_decisec+","+auction+","+
					info.hotel_bid[auction].size()+","+ask_price);
			
			/** find hqw **/
			int hqw_no = 0;
			int hqw[] = new int[agents.length];
			for (BidInfo bid : info.hotel_bid[auction]) {
				if (ask_price >= bid.price - 0.001) continue;
				
				hqw_no++;
				hqw[bid.agent_id]++;
				if (hqw_no == 16) break;
			}
			
			/** update hqw **/
			String str = "%,hqw,"+info.current_decisec+","+auction;
			for (int i = 0; i < agents.length; i++) {
				Quote quote = new Quote(auction);
				quote.setAskPrice((float)ask_price);
				quote.setHQW(hqw[i]);
				agents[i].quoteUpdated(quote);
				str += "," + hqw[i];
			}
			Misc.println(str);
		}
	}
	
	public void setHotelTransaction(GameInfo info) {
		if (info.hotel_open.isEmpty()) return;
		
		int auction_id = info.hotel_open.remove(0);
		Misc.myassert(auction_id >= 8 && auction_id < 16);
		
		/** decide closing price **/
		calculateClosingPrice(info);
		double price = info.hotel_price[auction_id].getLast();
		Misc.println(getClass().getSimpleName() + ".setHotelTransaction " +
				": auction_id " + auction_id + 
				", size " + info.hotel_bid[auction_id].size() +
				", closing_price " + price);
		
		
		/** distribute goods **/
		for (BidInfo bid : info.hotel_bid[auction_id]) {
			if (bid.price + 0.0001 <= price) continue;
			
			Transaction trans = new Transaction(auction_id, 1, (float) price);
			agents[bid.agent_id].transaction(trans);
			info.costs[bid.agent_id] += price;
			info.collections[bid.agent_id].owned[auction_id]++;
			
			Misc.println("%,trs" +
					","+info.current_decisec+
					","+bid.agent_id+
					","+"-1"+
					","+auction_id+
					","+"1" +
					","+price);
		}
		
		info.hotel_price[auction_id].add(price);
		
		/** update agents **/
		Quote quote = new Quote(auction_id);
		quote.setAskPrice(Constants.INFINITY);
		quote.setHQW(0);
		for (int agent_id = 0; agent_id < agents.length; agent_id++) {
			agents[agent_id].quoteUpdated(quote);
			agents[agent_id].auctionClosed(auction_id);
		}
	}
}
