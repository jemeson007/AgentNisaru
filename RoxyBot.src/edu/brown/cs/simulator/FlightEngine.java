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
 * FlightEngine
 *
 * Author  : Seong Jae Lee
 * Created : 15 May, 2007
 * Description:
 * 
 */

package edu.brown.cs.simulator;

import java.util.ArrayList;
import java.util.Iterator;

import props.Misc;
import se.sics.tac.aw.Bid;
import se.sics.tac.aw.Quote;
import se.sics.tac.aw.Transaction;
import edu.brown.cs.agent.SimulatedAgent;
import edu.brown.cs.tac.TacRandom;

public class FlightEngine {
	
	SimulatedAgent[] agents;
	
	public FlightEngine (SimulatedAgent[] a) { agents = a; }
	
	@SuppressWarnings("unchecked")
	public void gameInit(GameInfo info) {
		info.flight_price = new int[54][8];
		
		info.flight_bid = new ArrayList[54][8];
		for(int t=0; t<54; t++) for(int a=0; a<8; a++) {
			info.flight_bid[t][a] = new ArrayList<BidInfo>();
		}
		
		for(int auction_id = 0; auction_id<8; auction_id++) {
			double x = -10 + TacRandom.nextDouble(this) * 40; // [-10,30]
			info.flight_price[0][auction_id] = 250 + (int) (TacRandom.nextDouble(this) * 150);
			
			for (int t = 1; t < 54; t++) {
				double t_double = t;
				int t_x = (int) Math.round(10.0 + (t_double/54.0)*(x-10.0));
				int change = 0;
				if(t_x>0) change = -10 + (int) Math.floor(TacRandom.nextDouble(this) * (double)(t_x+11));
				if(t_x<0) change = t_x + (int) Math.floor(TacRandom.nextDouble(this) * (double)(-t_x+11));
				if(t_x==0) change = -10 + (int) Math.floor(TacRandom.nextDouble(this) * 21.0);
				
				info.flight_price[t][auction_id] = info.flight_price[t-1][auction_id] + change;
				if (info.flight_price[t][auction_id] < 150) info.flight_price[t][auction_id] = 150;
				if (info.flight_price[t][auction_id] > 800) info.flight_price[t][auction_id] = 800;
			}
		}
	}
	
	public void gameStarted(GameInfo info) {
		
	}
	
	public void setFlightQuote (GameInfo info) {
		int decisec = info.current_decisec; // [0 ~ 5] + [0 ~ 8] * 6
		int agent_no = agents.length;
		Misc.myassert(decisec>=0 && decisec<=53);
		
		String str ="%,flt,"+decisec;
		for (int auction_id=0; auction_id<8; auction_id++) {
			double ask_price = info.flight_price[decisec][auction_id];
			str += "," + (int)ask_price;
			Quote quote = new Quote(auction_id);
			quote.setAskPrice((float)ask_price);
			quote.setBidPrice(0);
			quote.setHQW(0);
			for (int agent_id=0; agent_id<agent_no; agent_id++) {
				agents[agent_id].quoteUpdated(quote);
			}
		}
		Misc.println(str);
	}

	public void setFlightTransaction (GameInfo info) {
		int decisec = info.current_decisec; // [0 ~ 5] + [0 ~ 8] * 6
		Misc.myassert(decisec>=0 && decisec<=53);
		
		for (int auction_id = 0; auction_id < 8; auction_id++) {
		for (int i = 0; i < info.flight_bid[decisec][auction_id].size(); i++) {
			if (info.flight_price[decisec][auction_id] > info.flight_bid[decisec][auction_id].get(i).price) continue;
			
			Transaction trans = new Transaction(auction_id, 1, (float) info.flight_price[decisec][auction_id]);
			int agent_id = info.flight_bid[decisec][auction_id].get(i).agent_id;
			
			info.costs[agent_id] += info.flight_price[decisec][auction_id];
			info.collections[agent_id].owned[auction_id]++;
			agents[agent_id].transaction(trans);
			
			//%,trs,time,buyer,seller,auction,quantity,price
			Misc.println("%,trs,"+decisec+","+agent_id+",-1,"+
					auction_id+",1,"+info.flight_price[decisec][auction_id]);
		}
		}
	}
	
	public void getFlightBid(GameInfo info) {
		int decisec = info.current_decisec;
		for(int agent_id=0; agent_id<agents.length; agent_id++) {
			ArrayList<Bid> list = agents[agent_id].getFlightBids();
			if(list==null) continue;
			Iterator<Bid> iter = list.iterator();
			
			while(iter.hasNext()) {
				Bid bid = iter.next();
				agents[agent_id].bidUpdated(bid);
				
				for(int i=0; i<bid.getNoBidPoints(); i++) {
					Misc.myassert(bid.getQuantity(i)==1);
					info.flight_bid[decisec][bid.getAuction()].add(new BidInfo(agent_id, bid.getPrice(i)));
					
					//%,bid,time,agent,auction,quantity,price
					Misc.println("%,bid" +
							","+decisec+
							","+agent_id+
							","+bid.getAuction()+
							","+bid.getQuantity(i)+
							","+bid.getPrice(i));
				}
			}
		}
	}
	
	public void gameStopped(GameInfo info) {
	}
}
