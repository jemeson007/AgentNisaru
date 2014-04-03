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
 * Algorithm
 *
 * Author  : Victor Naroditskiy
 * Created : 29 October, 2005
 * 
 */

package edu.brown.cs.algorithm;

import java.util.*;
import edu.brown.cs.tacds.*;
import edu.brown.cs.tac.*;
import edu.brown.cs.completer.PackageCompleter;
import edu.brown.cs.props.GoodsChooser;
import props.*;
import se.sics.tac.aw.Bid;

public abstract class Algorithm {
	protected Repository repository; //need to create a bidder
	protected GoodsChooser goodsChooser;
	protected PackageCompleter completer;
	
	protected int numScenarios;
	protected int numPolicies;
	protected int numEvaluations;
	
	//in simulator data respository needed to create sg is not available when we create algorithms - we'll set sg and completer later
	public Algorithm(int s, int p, int e) {
		numScenarios = s;
		numPolicies = p;
		numEvaluations = e;

		completer = new PackageCompleter();
		repository = null;
		goodsChooser = null;
	}
	
	//used in Roxybot200X agent
	public void setFields(Repository dr, GoodsChooser c) {
		repository = dr;
		goodsChooser = c;
	}
	
	//SAA overrides this method
	//here we remove scnenarios as we use them to make sure each policy is generated using its own scenarios.
	//so we call getIdealBids which uses m_numScenarios most recent scenarios and then remove them so the next call to getIdealBids gets different scenarios
	public ArrayList<Bid> getBids() {
		Misc.myassert(!(numPolicies == 1 && numEvaluations != 0));
		
		ArrayList<Bid>[] policies = new ArrayList[numPolicies]; 
		for (int i = 0; i < numPolicies; i++) {
			policies[i] = getIdealBids();
			repository.removeXMostRecent(numScenarios);
		}
		
		if (numPolicies == 1) return policies[0];
		
		Misc.println("Algorithm.getBids : evaluating more than 1 policies...");
		
		// evaluation scenarios
		Priceline [] evaluationScenarios = repository.removeXMostRecent(numEvaluations);
		
		// evaluate policies
		int bestPolicy = -1;
		float bestEvaluation = 0;
		for (int i = 0; i < numPolicies; i++) {

			float evaluation = evaluate(policies[i], evaluationScenarios);
			if (bestPolicy == -1 || evaluation > bestEvaluation) {
				bestPolicy = i;
				bestEvaluation = evaluation;
			}
		}

		// this is to make sure we do not generate extra scenarios. 
		// also see the comment at the top of the method: we need to generate enough scenarios otherwise we won't have enough for all policies - vn 
		Misc.myassert(repository.getAvailableScenarios().length == 0);
		
		Misc.println("Algorithm.getBids : best policy = " + bestPolicy);
		
		return policies[bestPolicy];
	}
	
	// evaluates a policy (an ArrayList of Bids)
	public float evaluate (ArrayList<Bid> bids, Priceline[] evaluationScenarios) {
		float ret = 0;
		for (int i = 0; i < numEvaluations; i++) {
			Misc.println("Algorithm.evaluate : scenario " + i);
			//evaluationScenarios[i].priceline.printHotel();
			
			float bank = 0;
			float cost = 0;
			TACCollection hown = new TACCollection(repository.getOwnCollection());
			
			for (int j = 0; j < bids.size(); j++) {
				Bid b = bids.get(j);
				int auction = b.getAuction();
				
				switch (Constants.auctionType(auction)) {
				case Constants.TYPE_FLIGHT :
					for (int k = 0; k < b.getNoBidPoints(); k++) {
						//float currentPrice = m_dr.getQuote(auction).getAskPrice();
						float currentPrice = evaluationScenarios[i].currBuyPrice[auction];//expected min price - lower than current price
						if (b.getPrice(k) >= currentPrice) {
							int q = b.getQuantity(k);
							Misc.myassert(1 == q);//comment out if causes problems - vn
							hown.setOwn(auction, hown.getOwn(auction) + q);
							bank -= q*currentPrice;
							cost += q*currentPrice;
						}
					}
					break;
				
				case Constants.TYPE_HOTEL :
					// It can be happen if the calculation takes more than 1 minute.
					// In that case, we just ignore the bid. sjlee.
					// Because it is not a serious bug and it can happen frequently, 
					// it would be innocuous to continue this algorithm.
					if (repository.isAuctionClosed(auction)) {
						Misc.error("we are bidding on a closed hotel. a " + auction + " we own " + hown.getOwn(auction));
						break;
						// Misc.myassert(false);
					}
					
					for (int k = 0; k < b.getNoBidPoints(); k++) {
						float closingPrice = evaluationScenarios[i].currBuyPrice[auction];
						if (b.getPrice(k) >= closingPrice) {
							int q = b.getQuantity(k);
							Misc.myassert(1 == q);//comment out if causes problems - vn
							hown.setOwn(auction, hown.getOwn(auction) + q);
							bank -= q*closingPrice;
							cost += q*closingPrice;
						}
					}
					break;
				
				case Constants.TYPE_EVENT :
					//ignore!!
					break;
				}
			}
			
			TACCollection forsale = Constants.forSale();
			for (int j = 0; j < Constants.AUCTION_MAX; j++) {
   				forsale.setOwn(j,0);
			}				
			
			Completion c = completer.computeCompletion(repository.getClientPrefs(), 
						hown, forsale, evaluationScenarios[i]);
			
			
			for(int a=0; a<Constants.AUCTION_MAX; a++) {
				Misc.myassert(0 == c.getBidPolicy().getQuantity(a));
				Misc.myassert(0 == c.getAskPolicy().getQuantity(a));
			}
			
			
			bank += c.objectiveValue;
			ret += bank;
		}
		
		return (ret / (float) numEvaluations);
	}
	
	// averages the prices in an array of scenarios
	public Priceline averageScenarios(Priceline[] scenarios) {
		Priceline ret = new Priceline();
		if (scenarios.length == 0) return ret;
		if (scenarios.length == 1) return scenarios[0];
		
		ret.probability = scenarios[0].probability;
		
		// set prices to 0
		for (int a = 0; a < Constants.AUCTION_MAX; a++) {
			ret.currBuyPrice[a] = 0;
			ret.currSellPrice[a] = 0;
			ret.nextBuyPrice[a] = 0;
			ret.nextSellPrice[a] = 0;
		}
		
		// sum over scenario
		for (int i = 0; i < scenarios.length; i++) {
			for (int a = 0; a < Constants.AUCTION_MAX; a++) {
				ret.currBuyPrice[a] += scenarios[i].currBuyPrice[a];
				ret.currSellPrice[a] += scenarios[i].currSellPrice[a];
				ret.nextBuyPrice[a] += scenarios[i].nextBuyPrice[a];
				ret.nextSellPrice[a] += scenarios[i].nextSellPrice[a];
			}
		}
		
		// divide by number of scenarios
		for (int a = 0; a < Constants.AUCTION_MAX; a++) {
			ret.currBuyPrice[a] /= (float) scenarios.length;
			ret.currSellPrice[a] /= (float) scenarios.length;
			ret.nextBuyPrice[a] /= (float) scenarios.length;
			ret.nextSellPrice[a] /= (float) scenarios.length;
		}
		
		ret.roundPrices();
		return ret;
	}
	
	//overridden in SAA because of time issues
	public int getMinScenarios() {
		return getNumScenarios();
	}

	protected abstract ArrayList<Bid> getIdealBids();//ideal means the bids we would bid if there were not outstanding bids

	//these two methods are called from sg to generate the right scenarios
	public int getNumScenarios() {
		return numScenarios;
	}
	
	public void setNumScenarios(int n) {
		numScenarios = n;
	}
	
	public int getNumPolicies() {
		return numPolicies;
	}
	
	public void setNumPolicies(int n) {
		numPolicies = n;
	}
	
	public int getNumEvaluations() {
		if(getNumPolicies() < 2) {
			Misc.myassert(numEvaluations == 0);
		}
		return numEvaluations;
	}

	public void setNumEvaluations(int evaluations) {
		numEvaluations = evaluations;
	}
	
	public int getNumScenariosToGenerate() {
		return getNumScenarios()*getNumPolicies()+getNumEvaluations();
	}
	
	public String toString() {
		return numScenarios + "-" + numPolicies + "-" + numEvaluations;
	}

	//we share the avg scenario in the Simulator.
	public boolean usesAvgScenario() {
		return false;
	}
	
	//saa overrides it
	public int getScenarioType() {
		return Constants.GLOBAL_BEST_PRICE;
	}
	
}
