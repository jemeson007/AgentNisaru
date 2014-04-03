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
 * PackageCompleter
 *
 * Author  : Bryan Guillemette
 * Created : 4 April, 2005
 */

package edu.brown.cs.completer;

import ilog.concert.*;
import edu.brown.cs.tac.Constants;
import edu.brown.cs.tacds.Allocation;
import edu.brown.cs.tacds.Completion;
import edu.brown.cs.tacds.Preference;
import edu.brown.cs.tacds.Priceline;
import edu.brown.cs.tacds.TACCollection;
import edu.brown.cs.tacds.TravelPackage;
import props.Misc;
import java.util.*;

public class PackageCompleter extends Completer {
	private TravelPackage[] m_packages = Constants.makePackages();;
	private int NUM_PACKAGES = m_packages.length;;
	
	/*
 	* Neither event nor flight prices change over each of the scenarios,
 	* so whatever we bid for a particular flight or event auction, we will
 	* either win it in every scenario, or lose it in every scenario.  Once
 	* we start modeling events and flights stochastically, change the
 	* increments from Constatns.INFINITY.  Having the increment be infinity
 	* allows us to reduce the number of boolean variables per auction to 2.
 	*/
	
	public PackageCompleter() {
		super();
	}
		
	/* -------------------------------------------------------------------
 	* FUNC: findBestCompletion() 
 	*
 	* DESCR: The method that gets called to allocate the goods to all
 	* clients and determine which goods (if any) should be purchased
 	* given the prices passed in the PriceLine. 
 	*
 	* RETURNS: This method returns an instance of the
 	* Completion class, it wraps around a TotalAllocation, a
 	* PurchasePolicy, and a SellPolicy.
 	* ------------------------------------------------------------------*/
	public Completion computeCompletion(Preference[] cprefs, 
										TACCollection own,
										TACCollection forSale,
										Priceline pp) {
		// Don't pass zero prices. they cause problems in different places. 
		// For example, mubidder expects prices to be positive because it doesn't bid 
		// when MU is zero. vn
		for(int a=8; a<16; a++) {
			if (pp.currBuyPrice[a] == 0) pp.currBuyPrice[a] = 0.01f;
			Misc.myassert(pp.currBuyPrice[a] > 0);
		}
		
		int numClients = cprefs.length;
		
		 //Misc.println("PackageCompleter :\n\town\t" + own + "\n\tsale\t" + forSale + "\n\tprice\t" + pp);
		try {
			m_cplex.clearModel();
			m_cplex.setOut(null);
			//m_cplex.setParam(IloCplex.IntParam.ClockType, 2);
			//m_cplex.setParam(IloCplex.DoubleParam.TiLim, 3.0);
			
			/******************
			 * Set up Constants
			 */
			 // we can buy any number of goods up to forSale for the same price! how is forSale chosen?
			float [] buyPrices = pp.currBuyPrice;
			float [] sellPrices = pp.currSellPrice;
			
			/* Because we are using predicted entertainment price, sell price can be 
			 * higher than buy price. In this case the completer would expect an 
			 * arbitrage. sjlee.
			for (int a=0; a<Constants.lastAuction(); a++) {
				if (buyPrices[a] < sellPrices[a]) {
					Misc.error("PackageCompleter : buy<sell : auction " + a + " buy " + buyPrices[a] + " sell " + sellPrices[a]);
					Misc.myassert(false);
				}
				
				if (Constants.auctionType(a) == Constants.TYPE_EVENT) {
					// the price should not be the same o/w there is nothing preventing the ilp from deciding to 
					// sell the tickets and then buy them back. this does not affect the objective value
					// but tac server does not allow this
					if (buyPrices[a] == sellPrices[a]) {
						sellPrices[a]--;
						buyPrices[a]++;
					}
				}
			} /* */
			
			int [] owned = own.owned;
			int [] fs = forSale.owned;
			
			
			/****************************
			 * Set up Decistion Variables
			 */
			IloIntVar [][] allocatePackageToClient = new IloIntVar[NUM_PACKAGES][numClients];
			IloIntVar [] buyGoods = new IloIntVar[Constants.lastAuction()];
			IloIntVar [] sellGoods = new IloIntVar[Constants.lastAuction()];
			
			for (int pack = 0; pack < NUM_PACKAGES; pack++) {
				for (int client = 0; client < numClients; client++) {
					allocatePackageToClient[pack][client] = m_cplex.boolVar("c" + client + "_gets_package_" + pack);
				}
			}
			
			// buying and selling goods with constraint
			// Q : Can we sell hotels?
			// A : No! If we can sell hotels, then we must own hotels, 
			//	 which means that hotel auction is closed, thus fs[auction] = 0.
			for (int auc = 0; auc < Constants.lastAuction(); auc++) {
				if (Constants.auctionType(auc) == Constants.TYPE_EVENT) {
					buyGoods[auc] = m_cplex.intVar(0, Math.max(0, fs[auc]-own.owned[auc]), "buy_from_a" + auc);
					sellGoods[auc] = m_cplex.intVar(0, Math.max(0, own.owned[auc]), "sell_to_a"  + auc); // sometimes own good is negative.
				} else {
					buyGoods[auc]  = m_cplex.intVar(0, fs[auc], "buy_from_a" + auc);
				}				
			}
			
			
			/***************************
			 * Set up Objective Function
			 */
			IloLinearNumExpr objective = m_cplex.linearNumExpr();
			
			// utility
			for (int client = 0; client < numClients; client++) {
				for (int pack = 0; pack < NUM_PACKAGES; pack++) {
					objective.addTerm(m_packages[pack].calculateUtility(
							cprefs[client], true, true, true), 
							allocatePackageToClient[pack][client]);
				}
			}
			
			// cost / revenue
			for (int auc = 0; auc < Constants.lastAuction(); auc++) {
				objective.addTerm(-buyPrices[auc], buyGoods[auc]);
				if (Constants.auctionType(auc) == Constants.TYPE_EVENT) {
					objective.addTerm(sellPrices[auc], sellGoods[auc]);
				}
			}
			
			// add maximize objective to model
			m_cplex.addMaximize(objective);
			
			
			/********************
			 * Set up constraints
			 */
			// each client gets one package
			for (int c = 0; c < numClients; c++) {
				IloLinearNumExpr constraint = m_cplex.linearNumExpr();
				constraint.setConstant(0);
				for (int pack = 0; pack < NUM_PACKAGES; pack++) {
					constraint.addTerm(1, allocatePackageToClient[pack][c]);
				}
				m_cplex.addEq(1.0, constraint, "each_client_gets_1_pkg_or_null_c_" + c);
			}
			
			/*
			 * constraint supply -- bought <= forSale
			 * this was already taken care of when the vars were declared
			for (int auction = 0; auction < Constants.lastAuction(); auction++) {
				m_cplex.addLe(buyGoods[auction], forSale.getOwn(auction));
			}
			 */
			
			// allocated <= bought + owned - sold
			for (int auction = 0; auction < Constants.lastAuction(); auction++) {
				IloLinearNumExpr allocation = m_cplex.linearNumExpr();
				IloLinearNumExpr hypotheticalOwn = m_cplex.linearNumExpr();
				hypotheticalOwn.setConstant(owned[auction]);
				
				for (int client = 0; client < numClients; client++) {
					for (int pack = 0; pack < NUM_PACKAGES; pack++) {
						if (m_packages[pack].containsGood(auction)) {
							allocation.addTerm(1, allocatePackageToClient[pack][client]);
						}
					}
				}
				
				hypotheticalOwn.addTerm(1,buyGoods[auction]);
				if (Constants.auctionType(auction) == Constants.TYPE_EVENT) {
					hypotheticalOwn.addTerm(-1, sellGoods[auction]);
				}
				
				m_cplex.addLe(allocation, hypotheticalOwn, "allocation_a_ " + auction);
			}
			
			/*************
			 * Solve model
			 */
			//m_cplex.exportModel("allocModel.lp");
			
			long sTime = System.currentTimeMillis();
			boolean solved = m_cplex.solve();
			long fTime = System.currentTimeMillis();
			Misc.myassert(solved);
			
			if (fTime - sTime > 3000) {
				Misc.warn("PackageCompleter : completion took too long. " + (fTime - sTime) + " milliseconds.");
			}
			
			/******************
			 * Retrieve results
			 */
			Completion ret = new Completion();
			
			// retrieve allocation
			Allocation allocation = ret.getAllocation();
			for (int client = 0; client < numClients; client++) {
				allocation.setTravelPackage(client, TravelPackage.nullPackage());
				for (int pack = 0; pack < NUM_PACKAGES; pack++) {
					int v = (int) Math.round(m_cplex.getValue(allocatePackageToClient[pack][client]));
					
					if (v == 1) {
						TravelPackage tp = new TravelPackage(m_packages[pack]);
						tp.calculateUtility(cprefs[client], true, true, true);
						allocation.setTravelPackage(client, tp);
					}
				}
			}
			
			// put quantities and prices in policies
			for (int auctionNo = 0; auctionNo < Constants.lastAuction(); auctionNo++) {
				int numToBuy = (int) Math.round(m_cplex.getValue(buyGoods[auctionNo]));
				Misc.myassert(numToBuy <= fs[auctionNo]);
				ret.getBidPolicy().addPrice(auctionNo, numToBuy, buyPrices[auctionNo]);
				
				if (Constants.auctionType(auctionNo) == Constants.TYPE_EVENT) {
					ret.getAskPolicy().addPrice(auctionNo, (int) Math.round(m_cplex.getValue(sellGoods[auctionNo])), sellPrices[auctionNo]);
				}
			}
			
			//ret.bestObjectiveValue = (float) m_cplex.getBestObjValue();//!! why bestobj and not objvalue
			ret.objectiveValue = (float) m_cplex.getObjValue();//!! why bestobj and not objvalue
			
			if (m_cplex.getBestObjValue() != m_cplex.getObjValue()) {
				Misc.warn("PackageCompleter : obj value different from best obj. bestObj "+ m_cplex.getBestObjValue() + " obj " + m_cplex.getObjValue());
				//Misc.myassert(false);
			}
			
			m_cplex.clearModel();
			
			return ret;
			
		} catch (Exception e) {
			e.printStackTrace();
			Misc.myassert(false); //find out why this happens
			return null;
		}
	}
		
	public static void main(String[] args) {
		PackageCompleter c = new PackageCompleter();
		
		long seed = System.currentTimeMillis();
		//seed = 1113494438527l;
		Random rand = new Random(seed);
		
		while(true) {
			Preference[] cprefs = new Preference[1];
			Misc.println("PREFS");
			for(int i=0; i<cprefs.length; i++) {
				cprefs[i] = Constants.genRandClientPrefs(rand);	
				Misc.println(cprefs[i]);
			}
			
			for (int a=8; a<16; a++) {
				TACCollection own = new TACCollection();
				TACCollection forSale = Constants.forSale();
				Priceline pp = new Priceline();
				for(int i=0; i<8; i++) pp.currBuyPrice[i] = 300;
				for(int i=8; i<12; i++) pp.currBuyPrice[i] = 10000;
				for(int i=12; i<16; i++) pp.currBuyPrice[i] = 10000;
				//for(int i=8; i<16; i++) own.setOwn(i,rand.nextInt(Math.max(1,rand.nextInt(12)/4)));
				for(int i=8; i<16; i++) own.setOwn(i,rand.nextInt(2));
				float[] util = new float[cprefs.length+1];
				for(int i=0; i<cprefs.length+1; i++) {
					own.setOwn(a, i);
					forSale.setOwn(a,0);
					util[i] = (c.computeCompletion(cprefs, own, forSale, pp)).objectiveValue;
					if(i>1) {
						float diff1;
						float diff2;
						diff1 = util[i-1] - util[i-2];
						diff2 = util[i] - util[i-1];
						Misc.println(diff1 + " " + diff2);
						if(diff2 > diff1) {
							Misc.myassert(false);
						}
					}
				}
				float diff1;
				float diff2;
				for(int i=2; i<cprefs.length+1; i++) {
					diff1 = util[i-1] - util[i-2];
					diff2 = util[i] - util[i-1];
					Misc.println(diff1 + " " + diff2);
					if(diff2 > diff1) {
						Misc.myassert(false);
					}
				}				
			}
		}
		
		
		/*
		GoodsCompleter c1 = new GoodsCompleter();
		PackageCompleter c2 = new PackageCompleter();
		
		int count = 0;
		
		long seed = System.currentTimeMillis();
		//seed = 1113494438527l;
		Random rand = new Random(seed);
		
		CustomerPrefs[] cprefs = new CustomerPrefs[ConstantsSAA.NUM_CLIENTS_PER_AGENT];
		TACCollection own = new TACCollection();
		TACCollection forSale = ConstantsSAA.forSale();
		PredictedPrices pp = new PredictedPrices();

		long t1, t2, t3;
		
		int numTrials = 100;
		int time1Sum = 0;
		int time2Sum = 0;
		
		for (int client = 0; client < ConstantsSAA.NUM_CLIENTS_PER_AGENT; client++) {
			cprefs[client] = ConstantsSAA.genRandCustPrefs(rand);
		}
		
		// flights
		for (int a = 0; a < ConstantsSAA.AUCTION_BAD_HOTEL; a++) {
			pp.buyPrices[a] = rand.nextInt(651) + 150;
			pp.sellPrices[a] = 0;
		}
		
		// hotels
		for (int a = ConstantsSAA.AUCTION_BAD_HOTEL; a < ConstantsSAA.AUCTION_ALLIGATOR; a++) {
			pp.buyPrices[a] = rand.nextInt(201) + 1;
			pp.sellPrices[a] = 0;
		}
		
		// events
		for (int a = ConstantsSAA.AUCTION_ALLIGATOR; a < ConstantsSAA.AUCTION_MAX; a++) {
			pp.buyPrices[a] = rand.nextInt(201);
			pp.sellPrices[a] = rand.nextInt((int) pp.buyPrices[a] + 1);
		}
		
		Completion comp1 = c1.computeCompletion(cprefs, own, forSale, pp);
		Completion comp2 = c2.computeCompletion(cprefs, own, forSale, pp);
		
		System.out.println("GoodsCompleter Allocation:");
		System.out.println(comp1.allocation);
		System.out.println("Objective = " + comp1.bestObjectiveValue);
		
		System.out.println("PackageCompleter Allocation:");
		System.out.println(comp2.allocation);
		System.out.println("Objective = " + comp2.bestObjectiveValue);
		*/
		/*
		for (int i = 0; i < numTrials; i++) {
			
			for (int client = 0; client < ConstantsSAA.NUM_CLIENTS_PER_AGENT; client++) {
				cprefs[client] = ConstantsSAA.genRandCustPrefs(rand);
			}
			
			for (int auction = 0; auction < 28; auction++) {
				for (int quantity = 1; quantity <= 16; quantity++) {
					pl.setPrice(auction, quantity, pp.buyPrices[auction]);
					if (auction >= 16) {
						pl.setSellPrice(auction, quantity, pp.sellPrices[auction]);
					}
				}
			}
			
			ConstantsSAA.reflectOwnedGoodsInPriceline(col, pl);
			
			t1 = System.currentTimeMillis();
			Completion comp1 = c1.computeCompletion(cprefs, col, pp, true);
			t2 = System.currentTimeMillis();
			Completion comp2 = c2.computeCompletion(cprefs, pl, true);
			t3 = System.currentTimeMillis();
			
			time1Sum += t2 - t1;
			time2Sum += t3 - t2;
			
			System.out.println("trial = " + i +
							   "\taverage time1 = " + (time1Sum / (i+1)) + 
							   "\taverage time2 = " + (time2Sum / (i+1)));
		}
		
		System.out.println("average time1 = " + (time1Sum / numTrials) + 
						   "\taverage time2 = " + (time2Sum / numTrials));
	   
		
		float [] costs1 = new float[8];
		float [] costs2 = new float[8];
		TravelPackage temp = null;
		for (int client = 0; client < 8; client++) {
			temp = comp1.getAllocation().getTravelPackage(client);
			for (int auction = 0; auction < 28; auction++) {
				costs1[client] += temp.containsGood(auction) ? pp.buyPrices[auction] : 0;
			}
			temp = comp2.getAllocation().getTravelPackage(client);
			for (int auction = 0; auction < 28; auction++) {
				costs2[client] += temp.containsGood(auction) ? pp.buyPrices[auction] : 0;
			}
		}
		
		for (int i = 0; i < 8; i++) {
			System.out.println(cprefs[i]);
		}
		
		System.out.println(pp);
		System.out.println(pl);
		
		System.out.println();
		
		System.out.println(comp1.getAllocation());
		for (int client = 0; client < 8; client++) {
			temp = comp1.getAllocation().getTravelPackage(client);
			System.out.println("client " + client + ":" + 
							   "  utility = " + temp.getUtility() + 
							   "  cost = " + costs1[client] + 
							   "  profit = " + (temp.getUtility() - costs1[client]));
		}
		float tu = comp1.getAllocation().getTotalUtility();
		System.out.println("comp1: Totaly Utility = " + tu);
		float cost = 0;
		for (int auction = 0; auction < 28; auction++) {
			cost += comp1.getBuyPolicy().getQuantity(auction) * pp.buyPrices[auction];
		}
		System.out.println("comp1: Cost = " + cost);
		float revenue = 0;
		for (int auction = 0; auction < 28; auction++) {
			revenue += comp1.getSellPolicy().getQuantity(auction) * pp.sellPrices[auction];
		}
		System.out.println("comp1: Revenue = " + revenue);
		System.out.println("comp1: Total Utility - Costs + Revenue = " + (tu - cost + revenue));
		System.out.println("comp1: best objective = " + comp1.bestObjectiveValue);
		System.out.println("comp1: time to compute = " + (t2 - t1));
		
		System.out.println();
		
		System.out.println(comp2.getAllocation());
		for (int client = 0; client < 8; client++) {
			temp = comp2.getAllocation().getTravelPackage(client);
			System.out.println("client " + client + ":" + 
							   "  utility = " + temp.getUtility() + 
							   "  cost = " + costs2[client] + 
							   "  profit = " + (temp.getUtility() - costs2[client]));
		}
		tu = comp2.getAllocation().getTotalUtility();
		System.out.println("comp2: Totaly Utility = " + tu);
		cost = 0;
		for (int auction = 0; auction < 28; auction++) {
			cost += comp2.getBuyPolicy().getQuantity(auction) * pp.buyPrices[auction];
		}
		System.out.println("comp2: Cost = " + cost);
		revenue = 0;
		for (int auction = 0; auction < 28; auction++) {
			revenue += comp2.getSellPolicy().getQuantity(auction) * pp.sellPrices[auction];
		}
		System.out.println("comp2: Revenue = " + revenue);
		System.out.println("comp2: Total Utility - Costs + Revenue = " + (tu - cost + revenue));
		System.out.println("comp2: best objective = " + comp2.bestObjectiveValue);
		System.out.println("comp2: time to compute = " + (t3 - t2));
		
		System.out.println("seed = " + seed);
		*/
	}
	
}

