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
 * GameEngineOffline
 *
 * Author  : Seong Jae Lee
 * Created : 15 May, 2007
 * Description:
 *
 * This is a simulator game engine for offline experiments. 
 * To make debug easier, each player is assigned the same set of clients.
 * 
 */

package edu.brown.cs.simulator;

import java.util.ArrayList;

import props.Misc;

import edu.brown.cs.agent.SimulatedAgent;
import edu.brown.cs.tac.TacRandom;
import edu.brown.cs.tacds.Client;
import edu.brown.cs.tacds.Preference;

public class GameEngineOffline extends GameEngine {

	protected long seed;
	
	public GameEngineOffline (SimulatedAgent[] a, int g) {
		super(a, g);
	}
	
	public void gameInit(GameInfo info) {
		super.gameInit(info);
		
		seed = System.currentTimeMillis();
		
		info.clients = new ArrayList<Client>();
		for (int i = 0; i < agents.length; i++) {
			TacRandom.setSeed(seed);
			for (int j = 0; j < 8; j++) {
				info.preferences[i][j] = new Preference();
				info.clients.add(new Client(info.preferences[i][j]));
			}
		}
	}
	
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
					TacRandom.setSeed(seed);
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
}
