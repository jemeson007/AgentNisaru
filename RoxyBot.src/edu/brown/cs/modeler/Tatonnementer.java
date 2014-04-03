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
 * Tatonnemeter
 *
 * Author  : Seong Jae Lee
 * Created : 5 May, 2007
 * 
 * This is used in HotelPredictorCE.
 * 
 * PERSPECTIVE_WALVERINE is our rough implementation of Walverine's tatonnement 
 * method. For more information, google the paper "Walverine: A Walrasian 
 * Trading Agent".
 *
 */

package edu.brown.cs.modeler;

import java.util.ArrayList;
import java.util.List;

import edu.brown.cs.tac.Constants;
import edu.brown.cs.tac.TacRandom;
import edu.brown.cs.tacds.Client;
import edu.brown.cs.tacds.Preference;
import edu.brown.cs.tacds.MiniTravelPackage;
import edu.brown.cs.tacds.Priceline;
import props.*;

public class Tatonnementer {
	// Because we are distributing (16 - our collection) for already closed hotels,
	// tatonnement for distribution is different from normal tatonnement.
	public static final int PERSPECTIVE_SAMPLED_CLIENT	= 1;
	public static final int PERSPECTIVE_WALVERINE = 2;
	public static final int PERSPECTIVE_EXPECTED_CLIENT = 3;
	public static final int PERSPECTIVE_DISTRIBUTION = 4; // perspective client
	public static final int PERSPECTIVE_BINOMIAL_AGENT = 5;
	
	public static final int UPDATE_TAT = 0;
	public static final int UPDATE_SAA = 1;
	
	// Walverine related-constants
	private static float[] INITIAL_PRICE = {50, 50, 50, 50, 100, 100, 100, 100};
	
	private int perspective = PERSPECTIVE_DISTRIBUTION;
	private int update = UPDATE_SAA;
	private int max_iteration = 5000;
	private double alpha = 1.0/24.0;
	private int num_agent = 8;
	private int num_client = 8; // per agent
	
	public void setPerspective (int a) { perspective = a; }
	public void setUpdate (int a) { update = a; }
	public void setMaxIteration (int a) { max_iteration = a; }
	public void setAlpha (double a) { alpha = a; }
	public void setAgent (int a) { num_agent = a; }
	public void setClient (int a) { num_client = a; }
	
	private int getMaxLoop () {
		if (perspective == PERSPECTIVE_WALVERINE) 
			return 100;
		return max_iteration;
	}
	
	private double getAlpha (int iteration) {
		if (perspective == PERSPECTIVE_WALVERINE)
			return (99.0/16.0 * Math.pow(0.95, iteration)) / 16.0;
		return this.alpha;
	}
	
	/**
	 * This is Walverine's tatonnement algorithm. It returns exactly same result to 
	 * the Walverine code, which is in /pro/roxybot/2005/sjlee/data/predict_new/.
	 *
	 * @param prefs: if it's null, then we 64 expected demand.
	 * @param prices: array size 16
	 * @return array size 8
	 */
	public float[] predictWalverine (Preference[] prefs, float[] bound) {
		Misc.myassert(bound.length >= 16);
		Misc.myassert(perspective == PERSPECTIVE_WALVERINE);
		Misc.myassert(update == UPDATE_TAT);
		
		float[] prices = new float[16];
		
		if (prefs == null) { // 64 expected clients
			for (int i = 0; i < 8; i++) prices[i+8] = INITIAL_PRICE[i];
			
			for (int t = 0; t < getMaxLoop(); t++) {
				float[]	otherDemand = getExpectedDemands(new Priceline(prices), Client.EVENT_OLD_CONST_MARGIN);
				double alpha = getAlpha(t);
				for (int i = 0; i < 8; i++) {
					prices[i+8] = (float) Math.max(0, alpha * (otherDemand[i] / 5.6f * 6.4f - 16.0f) + prices[i+8]);
				}
			}
		} else {
			for (int i = 0; i < 8; i++) prices[i+8] = INITIAL_PRICE[i];
			
			List<Client> clients = new ArrayList<Client>();
			for (int i = 0; i < 8; i++) clients.add(new Client(prefs[i]));
			
			for (int t = 0; t < getMaxLoop(); t++) {
				float[] otherDemand = getExpectedDemands(new Priceline(prices), Client.EVENT_NEW_CONST_MARGIN);
				int[] ownDemand = getOwnDemand(clients, new Priceline(prices));
				double alpha = getAlpha(t);
				for (int i = 0; i < 8; i++) {
					prices[i+8] = (float) Math.max(bound[i+8], prices[i+8] + alpha * (otherDemand[i] + ownDemand[i] - 16));
				}
			}
		}
		
		float[] ret = new float[8];
		for (int i = 0; i < 8; i++) ret[i] = prices[i+8];
		return ret;
	}
	
	private Preference[] getPrefs (Client[] clients) {
		Preference[] ret = new Preference[clients.length];
		for  (int i = 0; i < ret.length; i++) ret[i] = clients[i].preference;
		return ret;
	}
	
	private boolean isNoHotelClosed (Priceline p) {
		for (int a = 8; a < 16; a++) if (p.currBuyPrice[a] == Constants.INFINITY) return false;
		return true;
	}
	
	public Priceline predictHotel (Client[] ownClient, Priceline p) {
		ArrayList<Client> clients = new ArrayList<Client>();
		for (int i = 0; i < ownClient.length; i++) clients.add(ownClient[i]);
		
		switch(perspective) {
		case PERSPECTIVE_SAMPLED_CLIENT:
			for (int i = 1; i < num_agent; i++) for (int j = 0; j < num_client; j++) clients.add(new Client()); // random client
			return predictHotel (clients, p);
			
		case PERSPECTIVE_EXPECTED_CLIENT:
			Misc.myassert(num_agent == 8);
			Misc.myassert(num_client == 8);
			return predictHotel (clients, p);
			
		case PERSPECTIVE_BINOMIAL_AGENT:
			int counter = 0;
			do {
				for (int i = 0; i < 32; i++) if (TacRandom.nextBoolean(this)) counter++;
			} while (counter <= 1);
			
			for (int i = 0; i < counter; i++) for (int k = 0; k < num_client; k++) clients.add(new Client());
			return predictHotel (clients, p);
			
		case PERSPECTIVE_WALVERINE:
			Misc.myassert(num_agent == 8);
			Misc.myassert(num_client == 8);
			float[] hotels = predictWalverine(getPrefs(ownClient), p.currBuyPrice);
			for (int i = 0; i < 8; i++) p.currBuyPrice[i+8] = hotels[i];
			return p;
			
		case PERSPECTIVE_DISTRIBUTION:
			for (int i = 1; i < num_agent; i++) for (int j = 0; j < num_client; j++) clients.add(new Client()); // random client
			Misc.myassert(clients.size() == num_agent * num_client);

			if (isNoHotelClosed(p)) return predictHotel (clients, p);
			
			Priceline tmpP = new Priceline(p); // to copy flight / entertainment prices.
			for (int a = 8; a < 16; a++) tmpP.currBuyPrice[a] = 0.01f;
			tmpP = predictHotel(clients, tmpP);
			
			/** DISTRIBUTION START **/
			int[] spentGood = new int[8];
			for (int a = 8; a < 16; a++) spentGood[a-8] = 0;
			
			ArrayList<float[]> demands = new ArrayList<float[]>();
			for (Client c : clients) demands.add(c.getDemand(tmpP));
			
			for (int a = 8; a < 16; a++) {
				if (p.currBuyPrice[a] != Constants.INFINITY) continue;
				
				for (int i = 0; i < clients.size(); i++) {
					clients.get(i).collection[a] += demands.get(i)[a];
					Misc.myassert(clients.get(i).collection[a] <= 1);
					Misc.myassert(demands.get(i)[a] == 0 || demands.get(i)[a] == 1);
					spentGood[a-8] += demands.get(i)[a];
					
					if (spentGood[a-8] == 16) break; // there can be a case, due to small max.iter.no.
				}
				
				Misc.myassert(spentGood[a-8] <= 16);
				
				while (spentGood[a-8] < 16) {
					Client c = clients.get(TacRandom.nextInt(this, clients.size()));
					if (c.collection[a] == 1) continue;
					c.collection[a] = 1;
					spentGood[a-8]++;
				}
				
				Misc.myassert(spentGood[a-8] == 16);
			}
			/** DISTRIBUTION END **/
			
			return predictHotel(clients, p);
			
		default:
			Misc.myassert(false);
		}
		
		return null;
	}

	public Priceline predictHotel (List<Client> clients, Priceline pricesReal) {
		Misc.myassert (perspective != PERSPECTIVE_WALVERINE);
		
		Priceline pricesCopy = new Priceline(pricesReal);
		
		int maxLoop = getMaxLoop();
		for (int t = 0; t < maxLoop; t++) {
			float[] demand = new float[8];
			if (perspective == PERSPECTIVE_SAMPLED_CLIENT || perspective == PERSPECTIVE_DISTRIBUTION || perspective == PERSPECTIVE_BINOMIAL_AGENT) {
				demand = getDemands(clients, pricesCopy);
			} else if (perspective == PERSPECTIVE_EXPECTED_CLIENT) {
				int[] ownDemand = getOwnDemand(clients.subList(0, num_client-1), pricesCopy);
				float[] expDemand = getExpectedDemands(pricesCopy, Client.EVENT_NEW_CONST_MARGIN);
				for (int i = 0; i < 8; i++) {
					demand[i] = ownDemand[i] + expDemand[i];
				}
			}
			Misc.myassert(demand.length == 8);
			
			final float alpha = (float) getAlpha(t);
			boolean isConverged = true;
			for (int a = 8; a < 16; a++) {
				float diff = demand[a-8] - 16;
				
				if (update == UPDATE_SAA && diff > 0) {
					pricesCopy.currBuyPrice[a] += alpha * diff;
					isConverged = false;
				}
				
				if (update == UPDATE_TAT) {
					float price = pricesCopy.currBuyPrice[a] + alpha * diff;
					pricesCopy.currBuyPrice[a] = Math.max(pricesReal.currBuyPrice[a], price);
				}
			}
			
			if (update == UPDATE_SAA && isConverged) break;
		}

		return pricesCopy;
	}

	// Client Perspective
	private float[] getDemands (List<Client> clients, Priceline prices) {
		float[] ret = new float[8];

		for (Client c : clients) {
			float[] demands = c.getDemand(prices);
			for (int a = 8; a < 16; a++) ret[a-8] += demands[a];
		}

		return ret;
	}
	
	// Own demand for 8 clients
	// Used for Walverine Tatonnement
	private int[] getOwnDemand (List<Client> clients, Priceline prices) {
		int[] ret = new int[8];
		
		for (Client c : clients) {
			MiniTravelPackage optPackage = new MiniTravelPackage(0, 0, 0, 0.0); // in, out, hotel, value
			
			for (int in = 1; in < 5; in++) for (int out = in + 1; out < 6; out++) for (int h = 0; h < 2; h++) {
				MiniTravelPackage p = new MiniTravelPackage(in, out, h, 0);
				
				/* Before 2007.2.23, the code was the following. it is obviously wrong.
				 * But there are several results came from this code, so I preserve it. 
				 * p.value = c.getValue(p, prices, true, Client.EVENT_WITH_CONST_PRICE);
				 */
				p.value = Constants.EVENTS_ON ? 
					c.getValue(p, prices, true, Client.EVENT_WITH_CONST_PRICE) :
					c.getValue(p, prices, true, Client.EVENT_NO_MARGIN);
				if (p.value > optPackage.value) optPackage = p;
			}
			
			for (int i = optPackage.in; i < optPackage.out; i++) {
				ret[i - 1 + 4 * optPackage.hotel] += 1;
			}
		}
		
		return ret;
	}
	
	// Expected demands for other 56 clients
	// Used for Walverine Tatonnement
	private float[] getExpectedDemands (Priceline prices, int eventOption) {
		float[] ret = new float[8];
		
		Client[] clients = new Client[10];
		int counter = 0;
		for (int in = 1; in < 5; in++) for (int out = in + 1; out < 6; out++) {
			clients[counter] = new Client(new Preference(in, out, 0, 0, 0, 0));
			counter++;
		}
		Misc.myassert(counter == 10);
		
		for (int c = 0; c < clients.length; c++) {
			MiniTravelPackage bPackage = new MiniTravelPackage(0, 0, 0, 0.0); // in, out, hotel, value
			MiniTravelPackage gPackage = new MiniTravelPackage(0, 0, 1, 0.0);
			
			for (int in = 1; in < 5; in++) for (int out = in + 1; out < 6; out++) {
				MiniTravelPackage p = new MiniTravelPackage(in, out, 0, 0);
				p.value = clients[c].getValue(p, prices, false, eventOption);
				if (p.value > bPackage.value) bPackage = new MiniTravelPackage(p);
				
				p.hotel = 1;
				p.value = clients[c].getValue(p, prices, false, eventOption);
				if (p.value > gPackage.value) gPackage = new MiniTravelPackage(p);
			}
			
			// If weight goes to 1, you more want to get a bad hotel.
			double weight = Math.min(1, Math.max(0, (bPackage.value - gPackage.value - 50.0)/100.0));
			
			for (int i = bPackage.in; i < bPackage.out; i++) ret[i - 1] += 5.6 * weight;
			for (int i = gPackage.in; i < gPackage.out; i++) ret[i - 1 + 4] += 5.6 * (1.0 - weight);
		}
		
		return ret;
	}

}
