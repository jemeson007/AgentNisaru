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
 * GameEngine
 *
 * Author  : Seong Jae Lee
 * Created : 15 May, 2007
 * Description:
 *
 * This is a general simulator game engine.
 * 
 * The class hierarchy is:
 *      GameEngine
 *      --GameEngineSimultaneous
 *      --GameEngineOffline
 *        --GameEngineOfflineSimultaneous
 * 
 * It is not perfectly designed: the followings should be modified at the same time.
 * (GameEngineSimultaneous, GameEngineOfflineSimultaneous)
 * 
 */

package edu.brown.cs.simulator;

import java.util.ArrayList;

import props.Misc;
import se.sics.tac.aw.Quote;
import edu.brown.cs.agent.SimulatedAgent;
import edu.brown.cs.completer.PackageCompleter;
import edu.brown.cs.tacds.Client;
import edu.brown.cs.tacds.Completion;
import edu.brown.cs.tacds.Preference;
import edu.brown.cs.tacds.Priceline;
import edu.brown.cs.tacds.TACCollection;

public class GameEngine {
	
	protected int game_no;
	protected SimulatedAgent[] agents;
	public FlightEngine flight_engine;
	public HotelEngine hotel_engine;
	protected int game_bgn = 0;
	
	public GameEngine (SimulatedAgent[] a, int g) { 
		agents = a;
		game_no = g;
	}
	
	/* 
	 * Changes the index of the starting game from 0 to the specified number.
	 * This is used in random online game simulation.
	 */
	public void setGameBgn (int i) { game_bgn = i; }
	
	/*
	 * This code is overwritten by GameEngineSimultaneous. 
	 * This code is overwritten by GameEngineOfflineSimultaneous.
	 */
	public void run() {
		GameInfo info = new GameInfo();
		
		for (int i = game_bgn; i < game_bgn + game_no; i++) {
			Misc.println("%,bgn,"+i);
			long bgn = System.currentTimeMillis();
			
			gameInit(info);
			gameStarted(info);
			for (int t = 0; t < 9; t++) {
				gameTic(info);
			}
			gameStopped(info);
			
			long end = System.currentTimeMillis();
			Misc.println("%,end,"+i+","+((end-bgn)/1000));
		}
	}
	
	public void gameInit(GameInfo info) {
		int agent_no = agents.length;
		
		info.preferences = new Preference[agent_no][8];
		info.clients = new ArrayList<Client>();
		for (int i = 0; i < agent_no; i++) for (int j = 0; j < 8; j++) {
			info.preferences[i][j] = new Preference();
			info.clients.add(new Client(info.preferences[i][j]));
		}
		
		info.current_minute = 0;
		info.current_decisec = 0;
		
		info.collections = new TACCollection[agent_no];
		for (int i = 0; i < agent_no; i++) {
			info.collections[i] = new TACCollection();
		}
		
		info.costs     = new double[agent_no];
		info.utilities = new double[agent_no];
		info.scores    = new double[agent_no];
		
		flight_engine.gameInit(info);
		hotel_engine.gameInit(info);
	}
	
	public void gameStarted(GameInfo info) {
		Misc.println(getClass().getSimpleName() + ".gameStarted");
		
		for (int i = 0; i < agents.length; i++) {
			agents[i].gameStarted();
			agents[i].getRepository().setGameTime(0);
			agents[i].getRepository().setClientPrefs(info.preferences[i]);
			agents[i].getRepository().setOwnCollection(info.collections[i]);
			
			Misc.println("%,agt,"+i+","+agents[i]);
			
			String str="%,cln," + i + ",";
			for (int j=0; j<8; j++) {
				str += info.preferences[i][j];
				if (j!=7) str += ",";
			}
			Misc.println(str);
			
			
			for (int a = 0; a < 8; a++) {
				Quote quote = new Quote(a);
				quote.setAskPrice((float)info.flight_price[a][0]);
				quote.setBidPrice(0);
				quote.setHQW(0);
				agents[i].quoteUpdated(quote);
			}
			
			for (int a = 8; a < 16; a++) {
				Quote quote = new Quote(a);
				quote.setAskPrice(0);
				quote.setBidPrice(0);
				quote.setHQW(0);
				agents[i].quoteUpdated(quote);
			}
		}
		
		flight_engine.gameStarted(info);
		hotel_engine.gameStarted(info);
	}
	
	/*
	 * This code is overwritten by GameEngineSimultaneous.
	 * This code is overwritten by GameEngineOffline.
	 * This code is overwritten by GameEngineOfflineSimultaneous.
	 */
	public void gameTic(GameInfo info) {
		Misc.println(getClass().getSimpleName() + ".gameTic " +
				": " + info.current_decisec/6 + ":" + info.current_decisec%6 + "0");
		Misc.myassert(info.current_decisec%6==0);
		
		for (int i = 0; i < 6; i++) {
			flight_engine.setFlightQuote(info);
			
			if (i==0) {
				hotel_engine.setHotelQuote(info);
				
				for (int agent_id=0; agent_id<agents.length; agent_id++) {
					long tic = System.currentTimeMillis();
					if (!info.hotel_open.isEmpty()) agents[agent_id].setNextClosingAuction(info.hotel_open.get(0));
					agents[agent_id].calculateBids();
					long toc = System.currentTimeMillis();
					Misc.println("%,run," + info.current_decisec + "," + agent_id + "," + (toc-tic));
				}
			}
			
			flight_engine.getFlightBid(info);
			flight_engine.setFlightTransaction(info);
			
			if (i==5 && !info.hotel_open.isEmpty()) {
				hotel_engine.getHotelBid(info);
			}
			
			tic(info);
			
			if (i==5 && !info.hotel_open.isEmpty()) {
				hotel_engine.setHotelTransaction(info);
			}
		}
	}
	
	public void gameStopped(GameInfo info) {
		int agent_no = agents.length;
		PackageCompleter completer = new PackageCompleter();
		String str = "";
		
		for (int agent_id = 0; agent_id < agent_no; agent_id++) {
			Completion completion = completer.computeCompletion(
					info.preferences[agent_id], 
					info.collections[agent_id], 
					new TACCollection(), 
					new Priceline());
			
			info.utilities[agent_id] = completion.getAllocation().getTotalUtility();
			info.scores[agent_id] = info.utilities[agent_id] - info.costs[agent_id];
			
			int penalty = 0;
			int null_package = 0;
			int bonus = 0;
			for (int c=0; c<8; c++) {
				if (completion.getAllocation().getTravelPackage(c).isNull()) {
					null_package++;
					continue;
				}
				
				if (1 == completion.getAllocation().getTravelPackage(c).getHotelType()) {
					bonus += info.preferences[agent_id][c].getHotelValue();
				}
				
				int in = info.preferences[agent_id][c].getArrivalDate() - 1;
				int out = info.preferences[agent_id][c].getDepartureDate() - 2;
				penalty += 100*Math.abs(in - completion.getAllocation().getTravelPackage(c).getInflightDayIndex());
				penalty += 100*Math.abs(out - completion.getAllocation().getTravelPackage(c).getOutflightDayIndex());
			}
			
			//%,scr,agent,score,utility,cost,penalty,hotelbonus,nullpackage
			Misc.println("%,scr,"+agent_id+","+info.scores[agent_id]+","+
					(int)info.utilities[agent_id]+","+info.costs[agent_id]+","+
					penalty+","+bonus+","+null_package);
			
			//%,alc,agent,...
			str = "%,all," + agent_id;
			for (int a = 0; a < 16; a++) {
				str += "," + completion.getAllocation().getNumOfGood(a);
			}
			Misc.println(str);
			
			//%,cll,agent,...
			str ="%,cll," + agent_id;
			for (int a = 0; a < 16; a++) {
				str += "," + info.collections[agent_id].owned[a];
			}
			Misc.println(str);
		}
		
		hotel_engine.gameStopped(info);
		flight_engine.gameStopped(info);
	}
	
	protected void tic(GameInfo info) {
		info.current_decisec++;
		if (info.current_decisec%6==0) info.current_minute++;
		for (int agent_id = 0; agent_id < agents.length; agent_id++) {
			agents[agent_id].getRepository().setGameTime(10 * info.current_decisec);
		}
		
		Misc.println(getClass().getSimpleName() + ".tic " +
				": " + info.current_decisec/6 + ":" + info.current_decisec%6 + "0");
	}
}
