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
 * AttacAlgorithm
 *
 * Author  : Seong Jae Lee
 * Created : 15 May, 2007
 * Description :
 * 
 * This is our rough implementation of ATTAC's flight bidding algorithm. For 
 * more information, google the paper "ATTac-2001: A Learning, Autonomous 
 * Bidding Agent". Our implementation is designed for non-entertainment ticket 
 * games in our simulator. In online, time-scheduling problem might happen, 
 * so we do not recommend using our implementation.
 * 
 */

// vn: why set entertainment own/avail to zero? it does not matter when there is no en-nt but in general i'm not sure what's better.!!
// sjlee: I assumed it only for non-event game. if there are event tickets, more consideration should be needed.

/**
 * Note that this will be a thread implementation in the online game,
 * because we are going to scan through each scenarios till we have enough time.
 * After the bidder decides which flights to bid on, it calls 'run' method.
 * For each scenario, it calculates benefit from postponing.
 * When we have reasonable amount of time left, we call 'interrupt' method.
 * Then the run method will stop and will send the number of goods to bid
 * to RoxybotAgent.
 */
package edu.brown.cs.agent;

import java.util.ArrayList;
import java.util.List;

import props.Misc;
import se.sics.tac.aw.Bid;
import edu.brown.cs.completer.PackageCompleter;
import edu.brown.cs.tac.Constants;
import edu.brown.cs.tacds.Preference;
import edu.brown.cs.tacds.Completion;
import edu.brown.cs.tacds.Priceline;
import edu.brown.cs.tacds.TACCollection;
import edu.brown.cs.modeler.FlightPredictor;
import edu.brown.cs.modeler.FlightPredictorAttac;

public class AttacAlgorithm {
	private static final int NOW = 0;
	private static final int LATER = 1;
	
	public Agent agent;
	public Priceline[] scenario;
	
	private PackageCompleter completer;
	
	private Preference[] pref;
	private int[] own;
	private int[] set;
	private double[][] hotel;
	private double[][] flight;
	
	public AttacAlgorithm () {
		completer = new PackageCompleter();
	}
	
	public void update (List<Bid> list) {
		pref = agent.repository.getClientPrefs();
		
		own = new int[16];
		for (int a = 0; a < 16; a++) this.own[a] = agent.repository.getOwn(a);
		
		hotel = new double[scenario.length][8];
		Misc.myassert(scenario.length != 0);
		for (int i = 0; i < scenario.length; i++) {
			for (int a = 0; a < 8; a++) {
				hotel[i][a] = scenario[i].currBuyPrice[8+a];
			}
		}
		
		flight = new double[2][8];
		
		Misc.myassert(agent.repository.flight instanceof FlightPredictorAttac);
		FlightPredictorAttac flight_predictor = (FlightPredictorAttac) agent.repository.flight;
		for (int a = 0; a < 8; a++) {
			flight[NOW][a] = flight_predictor.predicted_within_minute[a];
			flight[LATER][a] = flight_predictor.predicted_after_minute[a];
		}

		set = new int[8];
		for (int i = 0; i < list.size(); i++) {
			if (Constants.auctionType(list.get(i).getAuction()) != Constants.TYPE_FLIGHT) continue;
			int auctionNo = list.get(i).getAuction();
			for (int j = 0; j < list.get(i).getNoBidPoints(); j++) {
				if (list.get(i).getPrice(j) > 
					Math.min(this.flight[NOW][auctionNo], this.flight[LATER][auctionNo])) {
					this.set[auctionNo] += list.get(i).getQuantity(j);
				}
			}
		}
		
	}
		
	/**
	 * 
	 * @param pref [auction 0 ~ 15]
	 * @param own [auction 0 ~ 7]
	 * @param set [auction 0 ~ 7]
	 * @param hotel [scenario 0 ~ n][auction 0 ~ 7]
	 * @param flight [now=0, later=1][auction 0 ~ 7]
	 */
	public ArrayList<Bid> run () {
		
		double[][] total_postpone_benefit = new double[8][];
		for (int a =0; a < 8; a++) total_postpone_benefit[a] = new double[set[a]];
		double[][] postpone_benefit = new double[8][];
		for (int a =0; a < 8; a++) postpone_benefit[a] = new double[set[a]];
		int[] num_to_buy = new int[8];
		
		for (int i = 0; i < 8; i++) {
			Misc.println("AttacAlgorithm : flight " + flight[NOW][i] + " " + flight[LATER][i]);
		}
		
		for (int s = 0; s < hotel.length; s++) {
			Misc.myassert(hotel[s].length == 8);
			postpone_benefit = getPostponingBenefit(pref, own, set, hotel[s], flight);
			for (int a = 0; a < 8; a++) {
				// if (flight[NOW][a] > flight[LATER][a]) continue;
				for (int g = 0; g < set[a]; g++) {
					total_postpone_benefit[a][g] += (flight[NOW][a] - flight[LATER][a]) + postpone_benefit[a][g];
				}
			}
		}
		
		for (int a = 0; a < 8; a++) {
			if (set[a] == 0) continue;
			if (flight[NOW][a] > flight[LATER][a]) continue;
			String ret = "AttacAlgorithm : benefit_array[" + a + "] : ";
			for (int g = 0; g < set[a]; g++) ret += total_postpone_benefit[a][g] + ", ";
			Misc.println(ret);
		}
		
		for (int a = 0; a < 8; a++) {
			if (set[a] == 0) continue;
			if (flight[NOW][a] > flight[LATER][a]) continue;
			num_to_buy[a] = this.set[a];
			for (int g = 0; g < set[a]; g++) {
				if (total_postpone_benefit[a][g] > 0) {
					num_to_buy[a] = g;
					break;
				}
			}
		}
		
		ArrayList<Bid> list = new ArrayList<Bid>();
		for (int a = 0; a < 8; a++) {
			if (num_to_buy[a] == 0) continue;
			Bid bid = new Bid(a);
			for (int i=0; i<num_to_buy[a]; i++) {
				bid.addBidPoint(1, 1000.0f);
			}
			list.add(bid);
		}
		
		return list;
		
	}
	
	private double[][] getPostponingBenefit (Preference[] pref, int[] own, int[] set, double[] hotel, double[][] flight) {
		
		// Misc.println("AttacAlgorithm.getPostponingBenefit : " + pref + " " + own.length + " " + set.length + " " + hotel.length);
		
		double[][] ret = new double[8][];
		for (int a = 0; a < 8; a++) ret[a] = new double[set[a]];
		double[] price = new double[16];
		int[] tmp_own = new int[16];
		
		for (int a = 0; a < 8; a++) price[a] = Math.min(flight[NOW][a], flight[LATER][a]);
		for (int a = 8; a < 16; a++) price[a] = hotel[a-8];
		
		for (int a = 0; a < 8; a++) {
			
			if (set[a] == 0) continue;
			if (flight[NOW][a] > flight[LATER][a]) continue; // price is decreasing.
			
			// Misc.println("AttacAlgorithm.getPostponingBenefit : a " + a);			
			
			double prev_value;
			double next_value;
			
			// value of the completion when we are not forced to buy 'g' units of flight a.
			// Misc.println("AttacAlgorithm.getPostponingBenefit : compute completion 0");
			prev_value = computeCompletion(pref, own, price).objectiveValue;
			
			for (int tmp_a = 0; tmp_a < 16; tmp_a ++) tmp_own[tmp_a] = own[tmp_a];
			
			for (int g = 0; g < set[a]; g++) {
				
				tmp_own[a]++;
				// Misc.println("AttacAlgorithm.getPostponingBenefit : compute completion " + (g+1));
				next_value = computeCompletion(pref, tmp_own, price).objectiveValue - (g+1) * flight[NOW][a];
				
				ret[a][g] = prev_value - next_value;
				
				prev_value = next_value;
				
			}
		}
		
		return ret;
		
	}
	
	private Completion computeCompletion(Preference[] cprefs, int[] own, double[] price) {
		
		TACCollection ownCollection = new TACCollection();
		TACCollection availableCollection = new TACCollection();
		Priceline priceline = new Priceline();
		
		for (int a = 0; a < 16; a++) ownCollection.setOwn(a, own[a]);
		for (int a = 16; a < 28; a++) ownCollection.setOwn(a, 0);
		for (int a = 0; a < 16; a++) availableCollection.setOwn(a, 8);
		for (int a = 16; a < 28; a++) availableCollection.setOwn(a, 0);
		for (int a = 0; a < 16; a++) priceline.currBuyPrice[a] = (float) price[a];
		
		return completer.computeCompletion(cprefs, ownCollection, availableCollection, priceline);
		
	}

}
