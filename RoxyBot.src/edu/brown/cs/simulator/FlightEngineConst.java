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
 * FlightEngineConst
 *
 * Author  : Seong Jae Lee
 * Created : 15 May, 2007
 * Description:
 * 
 */

package edu.brown.cs.simulator;

import edu.brown.cs.agent.SimulatedAgent;

public class FlightEngineConst extends FlightEngine {

	public int const_price;
	
	public FlightEngineConst (SimulatedAgent[] a, int price) { 
		super(a); 
		
		const_price = price;
	}
	
	public void gameInit(GameInfo info) {
		super.gameInit(info);
		
		for(int auction_id = 0; auction_id<8; auction_id++) {
			for (int t = 0; t < 54; t++) {
				info.flight_price[t][auction_id] = const_price;
			}
		}
	}
	
}
