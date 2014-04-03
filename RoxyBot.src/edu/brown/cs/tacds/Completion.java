/**
 * tacds
 *
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
 * Completion
 *
 * Author  : Bryan Guillemette
 * Created : 1 May, 2005
 * Description: 
 *    
 * This class represents a Completion from the Completer.  
 * It is ust a wrapper class for an Allocation, a
 * BuyPolicy, a SellPolicy, and the objective value obtained
 * for the optimal solution.
 * 
 */

package edu.brown.cs.tacds;

public class Completion {

    Allocation alloc;
    BidPolicy m_buy;
    BidPolicy m_sell;
	public float objectiveValue;

    public Completion() {
        alloc = new Allocation();
        m_buy = new BidPolicy();
        m_sell = new BidPolicy();
    }
    
    public Completion(Allocation _alloc, BidPolicy pp, BidPolicy sp){
		alloc = _alloc;
		m_buy = pp;
		m_sell = sp;
    }

    /* 
     * Returns the Allocation associated with this object. 
     */
    public Allocation getAllocation(){
		return alloc;
    }

    /* 
     * Returns the BuyPolicy associated with this object. 
     */
    public BidPolicy getBidPolicy(){
		return m_buy;
    }

    /* 
     * Returns the SellPolicy associated with this object. 
     */
    public BidPolicy getAskPolicy(){
		return m_sell;
    }
    
    /*
     * Returns a String representation of this object.
     */
    public String toString(){
		String ret = "Completion : \n";
		ret += alloc.toString();
		return ret;
    }
}
