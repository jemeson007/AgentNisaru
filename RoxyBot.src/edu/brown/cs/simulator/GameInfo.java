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
 * GameInfo
 *
 * Author  : Seong Jae Lee
 * Created : 15 May, 2007
 * Description:
 * 
 */

package edu.brown.cs.simulator;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import edu.brown.cs.simulator.BidInfo;
import edu.brown.cs.tacds.Client;
import edu.brown.cs.tacds.Preference;
import edu.brown.cs.tacds.TACCollection;

public class GameInfo {
	public int current_minute;
	public int current_decisec;
	
	public List<Client> clients;
	public Preference[][] preferences;          // per agent, per client
	public TACCollection[] collections;         // per agent
	public double[] closing_prices;             // per auction		
	public double[] costs, utilities, scores;   // per agent
	
	public int[][]                flight_price; // per decisec, per auction
	public ArrayList<BidInfo>[][] flight_bid;   // per decisec, per auction
	
	public ArrayList<BidInfo>[] hotel_bid;      // per auction
	public LinkedList<Double>[]	hotel_price;    // per auction
	public ArrayList<Integer>	hotel_open;
}