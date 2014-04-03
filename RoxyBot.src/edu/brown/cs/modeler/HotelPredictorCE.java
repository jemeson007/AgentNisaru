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
 * HotelPredictorCE
 *
 * Author  : Seong Jae Lee
 * Created : 15 May, 2007
 * Description:
 * 
 */

package edu.brown.cs.modeler;

import java.util.ArrayList;
import java.util.Iterator;

import props.Misc;
import edu.brown.cs.tac.Constants;
import edu.brown.cs.tacds.Client;
import edu.brown.cs.tacds.Priceline;
import edu.brown.cs.tacds.Repository;

public class HotelPredictorCE {
	Repository repository;
	
	Tatonnementer tatonnementer;
	boolean extremeMax;
	boolean extremeMin;
	
	public void init () {}
	
	public HotelPredictorCE (Repository r) {
		repository = r;
		tatonnementer = new Tatonnementer();
		extremeMax = false;
		extremeMin = false;
	}
	
	public void setTatonnementer (Tatonnementer t) { this.tatonnementer = t; }
	public void setExtremeMax (boolean e) { extremeMax = e; }
	public void setExtremeMin (boolean e) { extremeMin = e; }
	public Tatonnementer getTatonnementer () { return this.tatonnementer; }
	public boolean isExtremeMax () { return extremeMax; }
	public boolean isExtremeMin () { return extremeMin; }
	
	public void predictX (int x) {
		for (int i = 0; i < x; i++) repository.addScenario(predict());
		if (extremeMax || extremeMin) addExtremeScenarios();
	}
	
	public void addExtremeScenarios () {
		Priceline average = new Priceline();
   		Priceline[] scenarios = repository.getAvailableScenarios();
   		
   		for (int a = 8; a < 16; a++) average.currBuyPrice[a] = 0;
   		for (int s = 0; s < scenarios.length; s++) {
			for (int a = 8; a < 16; a++) {
				average.currBuyPrice[a] += scenarios[s].currBuyPrice[a] / scenarios.length;
			}
		}
		
		// average scenario with extreme price
		ArrayList<Priceline> extremes = new ArrayList<Priceline>();
		for (int a = 0; a < 8; a++) {
			if (repository.isAuctionClosed(a+8)) continue;
			
			Priceline minScenario = new Priceline(average);
			Priceline maxScenario = new Priceline(average);
			
			if (extremeMax) {
				float max = average.currBuyPrice[a+8];
				final float[] maxConstants = {1.5f, 1.75f, 2f, 3f};
				
				for (int i = 0; i < 4; i++) {
					if (repository.getNumClosedAuction() == 0 && (i == 0 || i == 2)) continue;
					if (repository.getNumClosedAuction() == 1 && (i == 0 || i == 2)) continue;
					maxScenario.currBuyPrice[a+8] = Math.max (max + (i+1) * 10, max * maxConstants[i]);
					
					Priceline tmp = new Priceline(maxScenario);
					tmp.probability = 1;
					extremes.add(tmp);
					
					Misc.println(getClass().getSimpleName() + ".addExtremeScenarios " +
							": max " + tmp.getHotelString());
				}
			}
			
			if (extremeMin) {
				minScenario.currBuyPrice[a+8] = repository.getQuote(a+8).getAskPrice() + 0.1f;
				minScenario.probability = 1;
				extremes.add(minScenario);
				
				Misc.println(getClass().getSimpleName() + ".addExtremeScenarios " +
						": max " + minScenario.getHotelString());
			}
		}
		
		// remove extremes.size scenarios
		repository.removeXMostRecent(extremes.size());
		
		// add open auction scenarios
		Iterator<Priceline> iter = extremes.iterator();
		while (iter.hasNext()) {
			Priceline s = iter.next();
			repository.addScenario(s);
		}
	}
	
	public Priceline predict() {
		Priceline priceline = new Priceline();
		priceline = repository.flight.predict(repository, FlightPredictor.PREDICT_TAT, priceline);
		if (Constants.EVENTS_ON) priceline = repository.event.predict(repository, priceline);
		// priceline = repository.updateFlightAndEvent(priceline, FlightPredictor.AFTER_MIN, FlightPredictor.AFTER_1MIN);
		
		for (int auctionNo = 8; auctionNo < 16; auctionNo++) {
			if (repository.isAuctionClosed(auctionNo)) priceline.currBuyPrice[auctionNo] = Constants.INFINITY;
			else priceline.currBuyPrice[auctionNo] = Math.max(0.001f, repository.getQuote(auctionNo).getAskPrice() + 0.01f);
		}
		
		int numClosed = 0;
		for (int a = 8; a < 16; a++) if (repository.isAuctionClosed(a)) numClosed++;
		if (numClosed == 8) return priceline; // all hotels are closed.
		
		Client[] clients = new Client[8];
		for (int j = 0; j < 8; j++) clients[j] = new Client(repository.getClientPrefs()[j]);
		priceline = tatonnementer.predictHotel(clients, priceline);
		
		for (int i = 0; i < 8; i++) {
			priceline.currBuyPrice[i+8] = Math.max(priceline.currBuyPrice[i+8], repository.getQuote(i+8).getAskPrice() + 0.001f);
		}
		
		priceline.printHotel();
		
		return priceline;
	}
}
