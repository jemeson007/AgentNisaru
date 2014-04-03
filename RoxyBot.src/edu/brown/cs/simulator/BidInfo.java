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
 * BidInfo
 *
 * Author  : Seong Jae Lee
 * Created : 15 May, 2007
 * Description:
 * 
 */

package edu.brown.cs.simulator;

import edu.brown.cs.tac.TacRandom;

public class BidInfo {
	public int auction_id;
	public int agent_id;
	public int time;

	public double price;
	public double sorting_price;

	/**
	 * sorting_price is price + epsilon, which is used to sort bids. since we 
	 * have to sort randomly if there are more than one bid which have same 
	 * prices, this is necessary.
	 */
	public BidInfo(int n, float p) { 
		agent_id = n;
		price   = p;
		sorting_price = TacRandom.nextDouble(this) * 0.0001 + (double) p;
	}
}
