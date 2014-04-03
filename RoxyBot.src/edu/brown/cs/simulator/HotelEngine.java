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
 * HotelEngine
 *
 * Author  : Seong Jae Lee
 * Created : 15 May, 2007
 * Description:
 * 
 * HotelEngine                           : game-theoretic, unknown sequential
 * --HotelEngineOnlineKnownSequential    : game-theoretic, known sequential
 * --HotelEngineOnlineSimultaneous       : game-theoretic, simultaneous
 * --HotelEngineOfflineSequential        : decision-theoretic, unknown sequential
 *   --HotelEngineOfflineKnownSequential : decision-theoretic, known sequential
 * --HotelEngineOfflineSimultaneous      : decision-theoretic, simultaneous
 * 
 */

package edu.brown.cs.simulator;

import java.util.ArrayList;
import java.util.LinkedList;

import props.Misc;
import se.sics.tac.aw.Bid;
import se.sics.tac.aw.Quote;
import se.sics.tac.aw.Transaction;
import edu.brown.cs.agent.SimulatedAgent;
import edu.brown.cs.tac.Constants;
import edu.brown.cs.tac.TacRandom;

public class HotelEngine {
	
	SimulatedAgent[] agents;
	
	public HotelEngine (SimulatedAgent[] a) { agents = a; }
	
	public void gameInit(GameInfo info) {
		info.hotel_open = new ArrayList<Integer>();
		
		ArrayList<Integer> temp = new ArrayList<Integer>();
		for (int a=8; a<16; a++) temp.add(a);
		while (!temp.isEmpty()) {
			int index = TacRandom.nextInt(this, temp.size());
			info.hotel_open.add(temp.remove(index));
		}
		
		info.hotel_price = new LinkedList[16];
		for (int a=8; a<16; a++) info.hotel_price[a] = new LinkedList<Double>();
		for (int a=8; a<16; a++) info.hotel_price[a].add(0.0);
		
		info.hotel_bid = new ArrayList[16];
		for (int a=8; a<16; a++) info.hotel_bid[a] = new ArrayList<BidInfo>();
	}
	
	public void gameStarted(GameInfo info) {
		
	}
	
	public void gameStopped(GameInfo info) {
		for(int auction_id=8; auction_id<16; auction_id++) {
			Misc.println("%,cls" +
					"," + auction_id +
					"," + info.hotel_price[auction_id].getLast());
		}
	}
	
	protected double findPrice(GameInfo info, int auction_id) {
		double ret = info.hotel_price[auction_id].getLast();
		if (info.hotel_bid[auction_id].size() >= 16) {
			Misc.myassert(ret <= info.hotel_bid[auction_id].get(15).price + 0.0001);
			ret = Math.max(ret, info.hotel_bid[auction_id].get(15).price);
		}
		info.hotel_price[auction_id].add(ret);
		return ret;
	}
	
	public void setHotelQuote(GameInfo info) {
		for (int auction : info.hotel_open) {
			/** find price **/
			double ask_price = findPrice(info, auction);
			Misc.println("%,htl,"+info.current_decisec+","+auction+","+
					info.hotel_bid[auction].size()+","+ask_price);
			
			/** find hqw **/
			int hqw_no = 0;
			int hqw[] = new int[agents.length];
			for (BidInfo bid : info.hotel_bid[auction]) {
				Misc.myassert(ask_price <= bid.price + 0.001);
				
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
	
	
	public void getHotelBid(GameInfo info) {
		int agent_no = agents.length;
		
		ArrayList<Bid>[] curr_list = new ArrayList[agent_no];
		/** get new bids from agent **/
		for (int agent_id=0; agent_id<agent_no; agent_id++) {
			curr_list[agent_id] = agents[agent_id].getHotelBids();
			Misc.println(getClass().getSimpleName() + ".getHotelBid " +
					": agent_id " + agent_id +
					", list " + curr_list[agent_id]);
		}
		
		/** keep top 16 bids only **/
		for (int auction_id : info.hotel_open) {
			ArrayList<BidInfo> prev_list = info.hotel_bid[auction_id];
			for (int i = prev_list.size()-1; i >= 16 ; i--) prev_list.remove(i);
			Misc.myassert(prev_list.size() <= 16);
		}

		/** remove old bids if updated **/
		for (int auction_id : info.hotel_open) {
			ArrayList<BidInfo> prev_list = info.hotel_bid[auction_id];
			
			for (int agent_id=0; agent_id<agent_no; agent_id++) {
				int prev_no = 0; // this is hqw
				int curr_no = 0;
				
				for (Bid bid : curr_list[agent_id])
					if (bid.getAuction() == auction_id) curr_no += bid.getQuantity();
				
				for (BidInfo bid : prev_list)
					if (bid.agent_id == agent_id) prev_no++;
				
				Misc.myassert(curr_no >= prev_no || curr_no == 0 || prev_no < 16);
				
				if (curr_no != 0) {
					for (int i = Math.min(15, prev_list.size()-1); i >=0; i--) {
						if (prev_list.get(i).agent_id != agent_id) continue;
						prev_list.remove(i);
					}
				}
			}
		}
			
		/** update new bids **/
		for (int agent_id=0; agent_id<agent_no; agent_id++) {
			for (Bid bid : curr_list[agent_id]) {
				agents[agent_id].bidUpdated(bid);
				
				for (int i = 0; i < bid.getNoBidPoints(); i++) {
					Misc.myassert(bid.getQuantity(i) == 1);
					Misc.println("%,bid" +
							","+info.current_decisec+
							","+agent_id+
							","+bid.getAuction()+
							","+bid.getQuantity(i)+
							","+bid.getPrice(i));
					info.hotel_bid[bid.getAuction()].add(new BidInfo(agent_id, bid.getPrice(i)));
				}
			}
		}
		
		/** sort bids **/
		sortHotelBid(info);
	}
	
	public void setHotelTransaction(GameInfo info) {
		if (info.hotel_open.isEmpty()) return;
		
		int auction_id = info.hotel_open.remove(0);
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

	protected void sortHotelBid(GameInfo info) {
		for (int a : info.hotel_open) {
			int size = info.hotel_bid[a].size();
			for (int i = 0; i < size; i++) for (int j = i; j < size; j++) {
				double price_i = info.hotel_bid[a].get(i).sorting_price;
				double price_j = info.hotel_bid[a].get(j).sorting_price;
				if (price_j > price_i) {
					BidInfo bidinfo_j = info.hotel_bid[a].get(j);
					info.hotel_bid[a].set(j, info.hotel_bid[a].get(i));
					info.hotel_bid[a].set(i, bidinfo_j);
				}
			}
		}
	}	
}
