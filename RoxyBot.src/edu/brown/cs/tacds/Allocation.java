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
 * Allocation
 *
 * Author  : Bryan Guillemette
 * Created : 1 May, 2005
 * Description: 
 *    
 * Data Structure that associates a set of travel packages with client
 * indexes.
 * 
 */

package edu.brown.cs.tacds;

import edu.brown.cs.tac.Constants;

public class Allocation {
    private TravelPackage[] travelPackages;

    public Allocation() { 
    	travelPackages = new TravelPackage [Constants.NUM_CLIENTS_PER_ALLOCATION];
    	for (int i = 0; i < Constants.NUM_CLIENTS_PER_ALLOCATION; i++) {
    		travelPackages[i] = TravelPackage.nullPackage();
    	}
    }
    
    public int getNumOfGood(int auctionNo) {
    	int ret = 0;
    	for (int i = 0; i < travelPackages.length; i++) {
    		if ( travelPackages[i].containsGood(auctionNo) )
    			ret++;
    	}
    	return ret;
    }
    
    public void setTravelPackage(int clientIndex, TravelPackage tp) {
		travelPackages[clientIndex] = tp;
    }

    public TravelPackage getTravelPackage(int clientIndex){
		return travelPackages[clientIndex];
    }
    
    public int getTotalUtility() {
		int totalUtility = 0;
		for (int i = 0; i < 8; i++) {
			if (travelPackages[i] != null)
				totalUtility += travelPackages[i].getUtility();
		}
    	return totalUtility;
    }
    
    public int getNumClients() {
    	return Constants.NUM_CLIENTS_PER_ALLOCATION;
    }

    public String toString() {
    	String ret = "";
    	for (TravelPackage p : travelPackages) {
			if (p == null) continue;
			ret += "Allocation : " + p.toString() + "\n";
		}
    	return ret;
    }
    
    public boolean equals(Allocation a) {
    	return (this.toString().equals(a.toString()));
    }
}
