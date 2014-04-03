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
 * HotelEngineOfflineSimultaneous
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
import edu.brown.cs.modeler.HotelPredictorCE;
import edu.brown.cs.modeler.HotelPredictorNormal;
import edu.brown.cs.tac.Constants;
import edu.brown.cs.tacds.Client;
import edu.brown.cs.tacds.Priceline;

public class HotelEngineOfflineSimultaneous extends HotelEngine {
	
	protected HotelPredictorCE predictor;
	
	public HotelEngineOfflineSimultaneous (SimulatedAgent[] a) { 
		super(a); 
		predictor = agents[0].getRepository().hotel;
	}
		
	//@ override
	protected double findPrice(GameInfo info, int auction_id, int minute) {
		Misc.myassert(false);
		return 0;
	}
	
	//@ override
	public void setHotelQuote(GameInfo info) { }
	
	//@ override
	public void setHotelTransaction(GameInfo info) {
		int minute = info.current_minute; // [1-8]
		
		if (minute != 8) return;
		
		// CLOSING PRICE
		Priceline closingPrice = null;
		if (predictor instanceof HotelPredictorNormal) {
			((HotelPredictorNormal)predictor).changeOptionToClosing();
			closingPrice = predictor.predict();
			((HotelPredictorNormal)predictor).changeOptionToPrediction();
		} else if (predictor instanceof HotelPredictorCE) {
			closingPrice = new Priceline();
			for (int a = 0; a < 8; a++)	closingPrice.currBuyPrice[a] = info.flight_price[0][a];
			for (int a = 8; a < 16; a++) closingPrice.currBuyPrice[a] = 0.0001f;
			Client[] clients = new Client[8];
			for (int i = 0; i < 8; i++) clients[i] = info.clients.get(i);
			closingPrice = predictor.getTatonnementer().predictHotel(clients, closingPrice);
		} else {
			Misc.myassert(false);
		}
		
		for (int auction_id : info.hotel_open) {
			Misc.myassert(auction_id >= 8 && auction_id < 16);
			// if (info.hotel_price[auction_id].size() != minute + 1) continue;
			
			/** decide closing price **/
			// double price = findPrice(info, auction_id, info.current_minute);
			double price = closingPrice.currBuyPrice[auction_id];
			Misc.println(getClass().getSimpleName() + ".setHotelTransaction " +
					": auction_id " + auction_id + 
					", size " + info.hotel_bid[auction_id].size() +
					", closing_price " + price);
			
			/** distribute goods **/
			// int transaction_no = 0;
			for (BidInfo bid : info.hotel_bid[auction_id]) {
				// transaction_no++;
				
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
				
				// if(transaction_no == 16) break;
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
}
