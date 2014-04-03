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
 * HotelEngineOnlineKnownSequential
 *
 * Author  : Seong Jae Lee
 * Created : 15 May, 2007
 * Description:
 * 
 */

package edu.brown.cs.simulator;

import props.Misc;
import se.sics.tac.aw.Quote;
import edu.brown.cs.agent.SimulatedAgent;

public class HotelEngineOnlineKnownSequential extends HotelEngine {
	
	public HotelEngineOnlineKnownSequential (SimulatedAgent[] a) { super(a); }
	
	//@ override
	public void setHotelQuote(GameInfo info) {
		for (int auction : info.hotel_open) {
			String str = "%,hqw,"+info.current_decisec+","+auction;
			for (int i = 0; i < agents.length; i++) {
				Quote quote = new Quote(auction);
				quote.setAskPrice(0);
				quote.setHQW(0);
				agents[i].quoteUpdated(quote);
				str += "," + 0;
			}
			Misc.println(str);
		}
	}
}
