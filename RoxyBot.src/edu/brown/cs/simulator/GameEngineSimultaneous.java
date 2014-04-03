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
 * GameEngineSimultaneous
 *
 * Author  : Seong Jae Lee
 * Created : 15 May, 2007
 * Description:
 *
 * This is a simulator game engine for online simultaneous experiments.
 * Make sure if you change this class, change GameEngineOfflineSimultaneous too.
 * 
 */

package edu.brown.cs.simulator;

import props.Misc;
import edu.brown.cs.agent.SimulatedAgent;

public class GameEngineSimultaneous extends GameEngine {

	public GameEngineSimultaneous (SimulatedAgent[] a, int g) { 
		super(a, g);
	}
	
	public void run() {
		GameInfo info = new GameInfo();
		for (int i = game_bgn; i < game_bgn + game_no; i++) {
			Misc.println("%,bgn,"+i);
			long bgn = System.currentTimeMillis();
			
			this.gameInit(info);
			this.gameStarted(info);
			this.gameTic(info);
			this.gameStopped(info);
			
			long end = System.currentTimeMillis();
			Misc.println("%,end,"+i+","+((end-bgn)/1000));
		}
	}
	
	public void gameTic(GameInfo info) {
		for(int i=0; i<53; i++) super.tic(info);
		
		flight_engine.setFlightQuote(info);

		for(int agent_id=0; agent_id<agents.length; agent_id++) {
			long tic = System.currentTimeMillis();
			agents[agent_id].calculateBids();
			long toc = System.currentTimeMillis();
			Misc.println("%,run," + info.current_decisec + "," + agent_id + "," + (toc-tic));
		}
		
		flight_engine.getFlightBid(info);
		hotel_engine.getHotelBid(info);
		
		flight_engine.setFlightTransaction(info);
		hotel_engine.setHotelTransaction(info);
		
		hotel_engine.setHotelQuote(info);
	}
}
