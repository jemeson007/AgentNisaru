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
 * HotelEngineOnlineSimultaneous
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
import edu.brown.cs.tac.Constants;

public class HotelEngineOnlineSimultaneous extends HotelEngine {
	
	public HotelEngineOnlineSimultaneous (SimulatedAgent[] a) { 
		super(a); 
	}
	
	public void gameInit(GameInfo info) {
		super.gameInit(info);
	}
	
	public void setHotelQuote(GameInfo info) { }
	
	public void setHotelTransaction(GameInfo info) {
		for (int auction_id : info.hotel_open) {
			Misc.myassert(auction_id >= 8 && auction_id < 16);
			
			/** decide closing price **/
			double price = findPrice(info, auction_id);
			Misc.println(getClass().getSimpleName() + ".setHotelTransaction " +
					": auction_id " + auction_id + 
					", size " + info.hotel_bid[auction_id].size() +
					", closing_price " + price);
			
			/** distribute goods **/
			int transaction_no = 0;
			for (BidInfo bid : info.hotel_bid[auction_id]) {
				transaction_no++;
				
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
				
				if(transaction_no == 16) break;
			}
			
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
