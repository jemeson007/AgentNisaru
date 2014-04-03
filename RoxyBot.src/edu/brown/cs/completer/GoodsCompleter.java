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
 * GoodsCompleter
 *
 * Author  : Bryan Guillemette
 * Created : 4 April, 2005
 */

package edu.brown.cs.completer;

import ilog.concert.*;
import ilog.cplex.IloCplex;
import edu.brown.cs.tac.Constants;
import edu.brown.cs.tacds.Allocation;
import edu.brown.cs.tacds.Completion;
import edu.brown.cs.tacds.Preference;
import edu.brown.cs.tacds.Priceline;
import edu.brown.cs.tacds.TACCollection;
import edu.brown.cs.tacds.TravelPackage;

/**
 * @author gilmet
 *
 * To change the template for this generated type comment go to
 * Window&gt;Preferences&gt;Java&gt;Code Generation&gt;Code and Comments
 */
public class GoodsCompleter extends Completer {
	
	/*
 	* Neither event nor flight prices change over each of the scenarios,
 	* so whatever we bid for a particular flight or event auction, we will
 	* either win it in every scenario, or lose it in every scenario.  Once
 	* we start modeling events and flights stochastically, change the
 	* increments from Constatns.INFINITY.  Having the increment be infinity
 	* allows us to reduce the number of boolean variables per auction to 2.
 	*/
	
	public GoodsCompleter() {
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
		
		int numClients = cprefs.length;
		
		try {
			
			m_cplex.clearModel();
			//m_cplex.setOut(null);
			m_cplex.setParam(IloCplex.IntParam.ClockType, 2);
			m_cplex.setParam(IloCplex.DoubleParam.TiLim, 3.0);
			
			/**
 			* Rename auction constants
 			*/
			
			int a = Constants.AUCTION_INFLIGHT;
			int d = Constants.AUCTION_OUTFLIGHT;
			int b = Constants.AUCTION_BAD_HOTEL;
			int g = Constants.AUCTION_GOOD_HOTEL;
			int r = Constants.AUCTION_ALLIGATOR;
			int s = Constants.AUCTION_AMUSEMENT;
			int t = Constants.AUCTION_MUSEUM;
			
			/**
 			* Set up Constants
 			*/
			
			float [] buyPrices = pp.currBuyPrice;
			float [] sellPrices = pp.currSellPrice;
			int [] owned = own.owned;
			int [] fs = forSale.owned;
			
			/****************************
 			* Set up Decistion Variables
 			*/
			
			IloIntVar [][] allocateGoodToClient = new IloIntVar[Constants.AUCTION_MAX][numClients];
			IloIntVar [] buyGoods = new IloIntVar[Constants.AUCTION_MAX];
			IloIntVar [] sellGoods = new IloIntVar[Constants.AUCTION_MAX];
			
			IloIntVar [] clientGetsNullPackage = new IloIntVar[numClients];
			
			IloIntVar [] stayingInBadHotels = new IloIntVar[numClients];
			IloIntVar [] stayingInGoodHotels = new IloIntVar[numClients];
			
			// initialize decision variables
			
			// allocateGoodToClient
			for (int auction = 0; auction < Constants.AUCTION_MAX; auction++) {
				for (int client = 0; client < numClients; client++) {
					allocateGoodToClient[auction][client] = m_cplex.boolVar("c" + client + "_gets_a" + auction);
				}
			}
			
			// buying and selling goods
			for (int auction = 0; auction < Constants.AUCTION_MAX; auction++) {
				buyGoods[auction] = m_cplex.intVar(0, fs[auction], "buy_from_a" + auction);
				sellGoods[auction] = m_cplex.intVar(0, fs[auction], "sell_to_a" + auction);
			}
			
			// client status variables
			for (int client = 0; client < numClients; client++) {
				clientGetsNullPackage[client] = m_cplex.boolVar("c" + client + "_alloc_null_package");
				stayingInBadHotels[client] = m_cplex.boolVar("c" + client + "_in_bad_hotels");
				stayingInGoodHotels[client] = m_cplex.boolVar("c" + client + "_in_good_hotels");
			}
			
			/***************************
 			* Set up Objective Function
 			*/
			
			IloLinearNumExpr objective = m_cplex.linearNumExpr();
			objective.setConstant(numClients * TravelPackage.BASIC_UTILITY);
			
			// utility
			for (int client = 0; client < numClients; client++) {
				// if not a valid package, lose the 1000
				objective.addTerm(-TravelPackage.BASIC_UTILITY, clientGetsNullPackage[client]);
				
				// travel penalties
				for (int dayIndex = 0; dayIndex < Constants.NUM_DAY_INDICES; dayIndex++) {
					int inPrefIndex = cprefs[client].getArrivalDate() - 1;
					int outPrefIndex = cprefs[client].getDepartureDate() - 2;
					int inPenalty = Math.abs(100 * (inPrefIndex - dayIndex));
					int outPenalty = Math.abs(100 * (outPrefIndex - dayIndex));
					objective.addTerm(-inPenalty, allocateGoodToClient[a + dayIndex][client]);
					objective.addTerm(-outPenalty, allocateGoodToClient[d + dayIndex][client]);
				}
				
				/**
				public TravelPakcage(
				allocateGoodToClient[auction][client]
				
				public TravelPackage(int _inflight,
   					 int _outflight,
   					 int _hotelType,
   					 int _eventIndex1,
   					 int _eventIndex2,
   					 int _eventIndex3);
				public float calculateUtility(Preference prefs, boolean flightsOn, boolean hotelsOn, boolean eventsOn) {
				**/
				
				// hotel bonus
				objective.addTerm(cprefs[client].getHotelValue(), stayingInGoodHotels[client]);
			
				// event bonus
				for (int auction = Constants.AUCTION_ALLIGATOR; auction < Constants.AUCTION_AMUSEMENT; auction++) {
					objective.addTerm(allocateGoodToClient[auction][client], cprefs[client].getEvent1Value());
				}
				for (int auction = Constants.AUCTION_AMUSEMENT; auction < Constants.AUCTION_MUSEUM; auction++) {
					objective.addTerm(allocateGoodToClient[auction][client], cprefs[client].getEvent2Value());
				}
				for (int auction = Constants.AUCTION_MUSEUM; auction < Constants.AUCTION_MAX; auction++) {
					objective.addTerm(allocateGoodToClient[auction][client], cprefs[client].getEvent3Value());
				}
			}
			// cost/revenue
			for (int auction = 0; auction < Constants.AUCTION_MAX; auction++) {
				objective.addTerm(-buyPrices[auction], buyGoods[auction]);
				objective.addTerm(sellPrices[auction], sellGoods[auction]);
			}
			
			// add maximize objective to model
			m_cplex.addMaximize(objective);
			
			/********************
 			* Set up constraints
 			*/
			
			// constraints per client
			for (int client = 0; client < numClients; client++) {
				/*
 				* contraint 1 -- each client gets one inflight, or a null package
 				*/
				IloLinearNumExpr c1 = m_cplex.linearNumExpr();
				c1.setConstant(0);
				
				c1.addTerm(1, clientGetsNullPackage[client]);
				for (int dayIndex = 0; dayIndex < Constants.NUM_DAY_INDICES; dayIndex++) {
					c1.addTerm(1,allocateGoodToClient[Constants.AUCTION_INFLIGHT + dayIndex][client]);
				}
				
				m_cplex.addEq(1.0, c1, "each_client_gets_1_inflight_or_null");
				
				/*
 				* constraint 2 -- can't leave before you arrive
 				*/
				IloLinearNumExpr [] c2RightSides = new IloLinearNumExpr[2*Constants.NUM_DAY_INDICES];
				for (int i = 0; i < Constants.NUM_DAY_INDICES; i++) {
					c2RightSides[i] = m_cplex.linearNumExpr();
					c2RightSides[i+Constants.NUM_DAY_INDICES] = m_cplex.linearNumExpr();
					for (int dayIndex = i; dayIndex < Constants.NUM_DAY_INDICES; dayIndex++) {
						c2RightSides[i].addTerm(1, allocateGoodToClient[d+dayIndex][client]);
					}
					for (int dayIndex = 0; dayIndex <= i; dayIndex++) {
						c2RightSides[i+Constants.NUM_DAY_INDICES].addTerm(1, allocateGoodToClient[a+dayIndex][client]);
					}
				}
				for (int dayIndex = 0; dayIndex < Constants.NUM_DAY_INDICES; dayIndex++) {
					m_cplex.addLe(allocateGoodToClient[a+dayIndex][client], c2RightSides[dayIndex]);
					m_cplex.addLe(allocateGoodToClient[d+dayIndex][client], c2RightSides[dayIndex+Constants.NUM_DAY_INDICES]);
				}
				
				/*
 				* constraint 3 -- don't allocate hotels before you arrive or after you leave
 				*/
				
				// a1 + b0 + g0 <= 1
				IloLinearNumExpr c3a = m_cplex.linearNumExpr();
				c3a.addTerm(1, allocateGoodToClient[a+1][client]);
				c3a.addTerm(1, allocateGoodToClient[b+0][client]);
				c3a.addTerm(1, allocateGoodToClient[g+0][client]);
				m_cplex.addLe(c3a,1);
				
				// a2 + b0 + g0, a2 + b1 + g1 <= 1
				IloLinearNumExpr c3b1 = m_cplex.linearNumExpr();
				c3b1.addTerm(1, allocateGoodToClient[a+2][client]);
				c3b1.addTerm(1, allocateGoodToClient[b+0][client]);
				c3b1.addTerm(1, allocateGoodToClient[g+0][client]);
				m_cplex.addLe(c3b1,1);
				
				IloLinearNumExpr c3b2 = m_cplex.linearNumExpr();
				c3b2.addTerm(1, allocateGoodToClient[a+2][client]);
				c3b2.addTerm(1, allocateGoodToClient[b+1][client]);
				c3b2.addTerm(1, allocateGoodToClient[g+1][client]);
				m_cplex.addLe(c3b2,1);

				// a3 + b0 + g0, a3 + b1 + g1, a3 + b2 + g2 <= 1
				IloLinearNumExpr c3c1 = m_cplex.linearNumExpr();
				c3c1.addTerm(1, allocateGoodToClient[a+3][client]);
				c3c1.addTerm(1, allocateGoodToClient[b+0][client]);
				c3c1.addTerm(1, allocateGoodToClient[g+0][client]);
				m_cplex.addLe(c3c1,1);

				IloLinearNumExpr c3c2 = m_cplex.linearNumExpr();
				c3c2.addTerm(1, allocateGoodToClient[a+3][client]);
				c3c2.addTerm(1, allocateGoodToClient[b+1][client]);
				c3c2.addTerm(1, allocateGoodToClient[g+1][client]);
				m_cplex.addLe(c3c2,1);

				IloLinearNumExpr c3c3 = m_cplex.linearNumExpr();
				c3c3.addTerm(1, allocateGoodToClient[a+3][client]);
				c3c3.addTerm(1, allocateGoodToClient[b+2][client]);
				c3c3.addTerm(1, allocateGoodToClient[g+2][client]);
				m_cplex.addLe(c3c3,1);
				
				// d2 + b3 + g3 <= 1
				IloLinearNumExpr c3d1 = m_cplex.linearNumExpr();
				c3d1.addTerm(1, allocateGoodToClient[d+2][client]);
				c3d1.addTerm(1, allocateGoodToClient[b+3][client]);
				c3d1.addTerm(1, allocateGoodToClient[g+3][client]);
				m_cplex.addLe(c3d1,1);
				
				// d1 + b2 + g2, d1 + b3 + g3 <= 1
				IloLinearNumExpr c3e1 = m_cplex.linearNumExpr();
				c3e1.addTerm(1, allocateGoodToClient[d+1][client]);
				c3e1.addTerm(1, allocateGoodToClient[b+2][client]);
				c3e1.addTerm(1, allocateGoodToClient[g+2][client]);
				m_cplex.addLe(c3e1,1);

				IloLinearNumExpr c3e2 = m_cplex.linearNumExpr();
				c3e2.addTerm(1, allocateGoodToClient[d+1][client]);
				c3e2.addTerm(1, allocateGoodToClient[b+3][client]);
				c3e2.addTerm(1, allocateGoodToClient[g+3][client]);
				m_cplex.addLe(c3e2,1);
				
				// d0 + b1 + g1, d0 + b2 + g2, d0 + b3 + g3 <= 1
				IloLinearNumExpr c3f1 = m_cplex.linearNumExpr();
				c3f1.addTerm(1, allocateGoodToClient[d+0][client]);
				c3f1.addTerm(1, allocateGoodToClient[b+1][client]);
				c3f1.addTerm(1, allocateGoodToClient[g+1][client]);
				m_cplex.addLe(c3f1,1);

				IloLinearNumExpr c3f2 = m_cplex.linearNumExpr();
				c3f2.addTerm(1, allocateGoodToClient[d+0][client]);
				c3f2.addTerm(1, allocateGoodToClient[b+2][client]);
				c3f2.addTerm(1, allocateGoodToClient[g+2][client]);
				m_cplex.addLe(c3f2,1);

				IloLinearNumExpr c3f3 = m_cplex.linearNumExpr();
				c3f3.addTerm(1, allocateGoodToClient[d+0][client]);
				c3f3.addTerm(1, allocateGoodToClient[b+3][client]);
				c3f3.addTerm(1, allocateGoodToClient[g+3][client]);
				m_cplex.addLe(c3f3,1);
				
				/*
 				* constraint 4 -- days in town = hotels allocated
 				*/
				IloLinearNumExpr c4a = m_cplex.linearNumExpr();
				IloLinearNumExpr c4b = m_cplex.linearNumExpr();
				c4a.addTerms(new double[] {4,3,2,1,-3,-2,-1,0},
 							new IloIntVar [] {allocateGoodToClient[d+3][client],
   											allocateGoodToClient[d+2][client],
   											allocateGoodToClient[d+1][client],
   											allocateGoodToClient[d+0][client],
   											allocateGoodToClient[a+3][client],
   											allocateGoodToClient[a+2][client],
   											allocateGoodToClient[a+1][client],
   											allocateGoodToClient[a+0][client]});
				c4b.addTerms(new double[] {1,1,1,1,1,1,1,1},
 							new IloIntVar [] {allocateGoodToClient[b+0][client],
   											allocateGoodToClient[b+1][client],
   											allocateGoodToClient[b+2][client],
   											allocateGoodToClient[b+3][client],
   											allocateGoodToClient[g+0][client],
   											allocateGoodToClient[g+1][client],
   											allocateGoodToClient[g+2][client],
   											allocateGoodToClient[g+3][client]});
				m_cplex.addEq(c4a, c4b);
				
				/*
 				* constraint 5 -- stay in one kind of hotel or the other (neither if null)
 				*/
				IloLinearNumExpr c5a = m_cplex.linearNumExpr();
				c5a.addTerm(1, clientGetsNullPackage[client]);
				c5a.addTerm(1, stayingInBadHotels[client]);
				c5a.addTerm(1, stayingInGoodHotels[client]);
				m_cplex.addEq(c5a, 1);
				for (int dayIndex = 0; dayIndex < Constants.NUM_DAY_INDICES; dayIndex++) {
					m_cplex.addGe(stayingInBadHotels[client], allocateGoodToClient[b+dayIndex][client]);
					m_cplex.addGe(stayingInGoodHotels[client], allocateGoodToClient[g+dayIndex][client]);
				}
				
				/*
 				* constraint 6 -- don't allocate events before you arrive or after you leave
 				*/
				
				// a1 + r0 + s0 + t0 <= 1
				IloLinearNumExpr c6a1 = m_cplex.linearNumExpr();
				c6a1.addTerm(1, allocateGoodToClient[a+1][client]);
				c6a1.addTerm(1, allocateGoodToClient[r+0][client]);
				c6a1.addTerm(1, allocateGoodToClient[s+0][client]);
				c6a1.addTerm(1, allocateGoodToClient[t+0][client]);
				m_cplex.addLe(c6a1,1);
				
				// a2 + r0 + s0 + t0, a2 + r1 + s1 + t1 <= 1
				IloLinearNumExpr c6b1 = m_cplex.linearNumExpr();
				c6b1.addTerm(1, allocateGoodToClient[a+2][client]);
				c6b1.addTerm(1, allocateGoodToClient[r+0][client]);
				c6b1.addTerm(1, allocateGoodToClient[s+0][client]);
				c6b1.addTerm(1, allocateGoodToClient[t+0][client]);
				m_cplex.addLe(c6b1,1);

				IloLinearNumExpr c6b2 = m_cplex.linearNumExpr();
				c6b2.addTerm(1, allocateGoodToClient[a+2][client]);
				c6b2.addTerm(1, allocateGoodToClient[r+1][client]);
				c6b2.addTerm(1, allocateGoodToClient[s+1][client]);
				c6b2.addTerm(1, allocateGoodToClient[t+1][client]);
				m_cplex.addLe(c6b2,1);

				// a3 + r0 + s0 + t0, a3 + r1 + s1 + t1, a3 + r2 + s2 + t2 <= 1
				IloLinearNumExpr c6c1 = m_cplex.linearNumExpr();
				c6c1.addTerm(1, allocateGoodToClient[a+3][client]);
				c6c1.addTerm(1, allocateGoodToClient[r+0][client]);
				c6c1.addTerm(1, allocateGoodToClient[s+0][client]);
				c6c1.addTerm(1, allocateGoodToClient[t+0][client]);
				m_cplex.addLe(c6c1,1);

				IloLinearNumExpr c6c2 = m_cplex.linearNumExpr();
				c6c2.addTerm(1, allocateGoodToClient[a+3][client]);
				c6c2.addTerm(1, allocateGoodToClient[r+1][client]);
				c6c2.addTerm(1, allocateGoodToClient[s+1][client]);
				c6c2.addTerm(1, allocateGoodToClient[t+1][client]);
				m_cplex.addLe(c6c2,1);

				IloLinearNumExpr c6c3 = m_cplex.linearNumExpr();
				c6c3.addTerm(1, allocateGoodToClient[a+3][client]);
				c6c3.addTerm(1, allocateGoodToClient[r+2][client]);
				c6c3.addTerm(1, allocateGoodToClient[s+2][client]);
				c6c3.addTerm(1, allocateGoodToClient[t+2][client]);
				m_cplex.addLe(c6c3,1);
				
				// d2 + r3 + s3 + t3 <= 1
				IloLinearNumExpr c6d1 = m_cplex.linearNumExpr();
				c6d1.addTerm(1, allocateGoodToClient[d+2][client]);
				c6d1.addTerm(1, allocateGoodToClient[r+3][client]);
				c6d1.addTerm(1, allocateGoodToClient[s+3][client]);
				c6d1.addTerm(1, allocateGoodToClient[t+3][client]);
				m_cplex.addLe(c6d1,1);
				
				// d1 + r2 + s2 + t2, d1 + r3 + s3 + t3 <= 1
				IloLinearNumExpr c6e1 = m_cplex.linearNumExpr();
				c6e1.addTerm(1, allocateGoodToClient[d+1][client]);
				c6e1.addTerm(1, allocateGoodToClient[r+2][client]);
				c6e1.addTerm(1, allocateGoodToClient[s+2][client]);
				c6e1.addTerm(1, allocateGoodToClient[t+2][client]);
				m_cplex.addLe(c6e1,1);

				IloLinearNumExpr c6e2 = m_cplex.linearNumExpr();
				c6e2.addTerm(1, allocateGoodToClient[d+1][client]);
				c6e2.addTerm(1, allocateGoodToClient[r+3][client]);
				c6e2.addTerm(1, allocateGoodToClient[s+3][client]);
				c6e2.addTerm(1, allocateGoodToClient[t+3][client]);
				m_cplex.addLe(c6e2,1);
				
				// d0 + r1 + s1 + t1, d0 + r2 + s2 + t2, d0 + r3 + s3 + t3 <= 1
				IloLinearNumExpr c6f1 = m_cplex.linearNumExpr();
				c6f1.addTerm(1, allocateGoodToClient[d+0][client]);
				c6f1.addTerm(1, allocateGoodToClient[r+1][client]);
				c6f1.addTerm(1, allocateGoodToClient[s+1][client]);
				c6f1.addTerm(1, allocateGoodToClient[t+1][client]);
				m_cplex.addLe(c6f1,1);

				IloLinearNumExpr c6f2 = m_cplex.linearNumExpr();
				c6f2.addTerm(1, allocateGoodToClient[d+0][client]);
				c6f2.addTerm(1, allocateGoodToClient[r+2][client]);
				c6f2.addTerm(1, allocateGoodToClient[s+2][client]);
				c6f2.addTerm(1, allocateGoodToClient[t+2][client]);
				m_cplex.addLe(c6f2,1);

				IloLinearNumExpr c6f3 = m_cplex.linearNumExpr();
				c6f3.addTerm(1, allocateGoodToClient[d+0][client]);
				c6f3.addTerm(1, allocateGoodToClient[r+3][client]);
				c6f3.addTerm(1, allocateGoodToClient[s+3][client]);
				c6f3.addTerm(1, allocateGoodToClient[t+3][client]);
				m_cplex.addLe(c6f3,1);
				
				/*
 				* constraint 7 -- don't allocate more than one of each type of event per client
 				*/
				IloLinearNumExpr c7a = m_cplex.linearNumExpr();
				c7a.addTerm(1, allocateGoodToClient[r+0][client]);
				c7a.addTerm(1, allocateGoodToClient[r+1][client]);
				c7a.addTerm(1, allocateGoodToClient[r+2][client]);
				c7a.addTerm(1, allocateGoodToClient[r+3][client]);
				m_cplex.addLe(c7a,1);

				IloLinearNumExpr c7b = m_cplex.linearNumExpr();
				c7b.addTerm(1, allocateGoodToClient[s+0][client]);
				c7b.addTerm(1, allocateGoodToClient[s+1][client]);
				c7b.addTerm(1, allocateGoodToClient[s+2][client]);
				c7b.addTerm(1, allocateGoodToClient[s+3][client]);
				m_cplex.addLe(c7b,1);

				IloLinearNumExpr c7c = m_cplex.linearNumExpr();
				c7c.addTerm(1, allocateGoodToClient[t+0][client]);
				c7c.addTerm(1, allocateGoodToClient[t+1][client]);
				c7c.addTerm(1, allocateGoodToClient[t+2][client]);
				c7c.addTerm(1, allocateGoodToClient[t+3][client]);
				m_cplex.addLe(c7c,1);
				
				/*
 				* constraint 8 -- no more than 1 event per day per client
 				*/
				IloLinearNumExpr c8a = m_cplex.linearNumExpr();
				c8a.addTerm(1, allocateGoodToClient[r+0][client]);
				c8a.addTerm(1, allocateGoodToClient[s+0][client]);
				c8a.addTerm(1, allocateGoodToClient[t+0][client]);
				m_cplex.addLe(c8a,1);

				IloLinearNumExpr c8b = m_cplex.linearNumExpr();
				c8b.addTerm(1, allocateGoodToClient[r+1][client]);
				c8b.addTerm(1, allocateGoodToClient[s+1][client]);
				c8b.addTerm(1, allocateGoodToClient[t+1][client]);
				m_cplex.addLe(c8b,1);

				IloLinearNumExpr c8c = m_cplex.linearNumExpr();
				c8c.addTerm(1, allocateGoodToClient[r+2][client]);
				c8c.addTerm(1, allocateGoodToClient[s+2][client]);
				c8c.addTerm(1, allocateGoodToClient[t+2][client]);
				m_cplex.addLe(c8c,1);

				IloLinearNumExpr c8d = m_cplex.linearNumExpr();
				c8d.addTerm(1, allocateGoodToClient[r+3][client]);
				c8d.addTerm(1, allocateGoodToClient[s+3][client]);
				c8d.addTerm(1, allocateGoodToClient[t+3][client]);
				m_cplex.addLe(c8d,1);
				
				/*
 				* constraint 9 -- don't allocate events if client gets null package
 				*/
				for (int event = r; event < Constants.AUCTION_MAX; event++) {
					IloLinearNumExpr c9 = m_cplex.linearNumExpr();
					c9.addTerm(1, clientGetsNullPackage[client]);
					c9.addTerm(1, allocateGoodToClient[event][client]);
					m_cplex.addLe(c9,1);
				}
			}
			
			/*
 			* constraint supply -- bought <= forSale
 			*/
			for (int auction = 0; auction < Constants.AUCTION_MAX; auction++) {
				m_cplex.addLe(buyGoods[auction], forSale.getOwn(auction));
			}
			
			/*
 			* constraint final -- allocated <= bought + owned - sold
 			*/
			for (int auction = 0; auction < Constants.AUCTION_MAX; auction++) {
				IloLinearNumExpr c6allocated = m_cplex.linearNumExpr();
				IloLinearNumExpr c6finalOwned = m_cplex.linearNumExpr();
				c6finalOwned.setConstant(owned[auction]);
				
				for (int client = 0; client < numClients; client++) {
					c6allocated.addTerm(1, allocateGoodToClient[auction][client]);
				}
				
				c6finalOwned.addTerm(1,buyGoods[auction]);
				c6finalOwned.addTerm(-1,sellGoods[auction]);
				
				m_cplex.addLe(c6allocated, c6finalOwned);
			}
			
			/**
 			* TODO: constraints for allocating events
 			*/
			
			/*************
 			* Solve model
 			*/
			
			//m_cplex.exportModel("allocModel.sav");
			//long startTime = System.currentTimeMillis();
			m_cplex.solve();
			//long finishTime = System.currentTimeMillis();
			//System.out.println("Solve Time = " + ((float) (finishTime - startTime) / 1000) + " seconds.");
			//System.out.println("Objective Value = " + m_cplex.getBestObjValue());
			
			
			/******************
			 * Retrieve results
			 */
	  
			Completion ret = new Completion();
			
			// retrieve allocation
			Allocation allocation = ret.getAllocation();
			for (int client = 0; client < numClients; client++) {
				if (m_cplex.getValue(clientGetsNullPackage[client]) == 1) {
					allocation.setTravelPackage(client, TravelPackage.nullPackage());
				} else {
					int inflight = -1;
					int outflight = -1;
					int hotel = 0;
					int eventDayIndex0 = 0;
					int eventDayIndex1 = 0;
					int eventDayIndex2 = 0;
					int eventDayIndex3 = 0;
					
					// inflight
					for (int dayIndex = 0; dayIndex < 4; dayIndex++) {
						if (Math.round(m_cplex.getValue(allocateGoodToClient[a+dayIndex][client])) == 1) {
							inflight = dayIndex;
							continue;
						}
					}
					
					// outflight
					for (int dayIndex = 0; dayIndex < 4; dayIndex++) {
						if (Math.round(m_cplex.getValue(allocateGoodToClient[d+dayIndex][client])) == 1) {
							outflight = dayIndex;
							continue;
						}
					}
					
					// hotel
					hotel = (int) Math.round(m_cplex.getValue(stayingInGoodHotels[client]));
					
					// events
					for (int auction = r; auction < Constants.AUCTION_MAX; auction += 4) {
						if (Math.round(m_cplex.getValue(allocateGoodToClient[auction][client])) == 1) {
							eventDayIndex0 = 1 + (auction - r) / 4;
							continue;
						}
					}
					for (int auction = r+1; auction < Constants.AUCTION_MAX; auction += 4) {
						if (Math.round(m_cplex.getValue(allocateGoodToClient[auction][client])) == 1) {
							eventDayIndex1 = 1 + (auction - (r+1)) / 4;
							continue;
						}
					}
					for (int auction = r+2; auction < Constants.AUCTION_MAX; auction += 4) {
						if (Math.round(m_cplex.getValue(allocateGoodToClient[auction][client])) == 1) {
							eventDayIndex2 = 1 + (auction - (r+2)) / 4;
							continue;
						}
					}
					for (int auction = r+3; auction < Constants.AUCTION_MAX; auction += 4) {
						if (Math.round(m_cplex.getValue(allocateGoodToClient[auction][client])) == 1) {
							eventDayIndex3 = 1 + (auction - (r+3)) / 4;
							continue;
						}
					}
					
					TravelPackage tp = new TravelPackage(inflight,
														 outflight,
														 hotel,
														 eventDayIndex0,
														 eventDayIndex1,
														 eventDayIndex2,
														 eventDayIndex3);
					
					tp.calculateUtility(cprefs[client], true, true, true);
					allocation.setTravelPackage(client, tp);
				}
			}
			
			// put quantities and prices in policies
			for (int auctionNo = 0; auctionNo < Constants.AUCTION_MAX; auctionNo++) {
				ret.getBidPolicy().addPrice(auctionNo, (int) Math.round(m_cplex.getValue(buyGoods[auctionNo])), buyPrices[auctionNo]);
				if (auctionNo >= Constants.AUCTION_ALLIGATOR) {
					ret.getAskPolicy().addPrice(auctionNo, (int) Math.round(m_cplex.getValue(sellGoods[auctionNo])), sellPrices[auctionNo]);
				}
			}
			
			ret.objectiveValue = (float) m_cplex.getObjValue();
			
			m_cplex.clearModel();
			
			return ret;
			
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}
	
	public static void main(String[] args) {
/*
		GreedyCompleter c1 = new GreedyCompleter();
		ILPCompleter2 c2 = new ILPCompleter2();
		
		int count = 0;
		
		long seed = System.currentTimeMillis();
		//seed = 1113494438527l;
		Random rand = new Random(seed);
		
		CustomerPrefs[] cprefs = new CustomerPrefs[ConstantsSAA.NUM_CLIENTS_PER_AGENT];
		TACCollection col = new TACCollection();
		PredictedPrices pp = new PredictedPrices();
		Priceline pl = new Priceline();

		long t1, t2, t3;
		
		int numTrials = 100;
		int time1Sum = 0;
		int time2Sum = 0;
		
		for (int i = 0; i < numTrials; i++) {
			
			for (int client = 0; client < ConstantsSAA.NUM_CLIENTS_PER_AGENT; client++) {
				cprefs[client] = ConstantsSAA.genRandCustPrefs(rand);
			}
*/			
			/*
			 cprefs[0] = new CustomerPrefs(2,3,150,0,0,0);
			 cprefs[1] = new CustomerPrefs(2,3,150,0,0,0);
			 cprefs[2] = new CustomerPrefs(2,3,150,0,0,0);
			 cprefs[3] = new CustomerPrefs(2,3,150,0,0,0);
			 cprefs[4] = new CustomerPrefs(2,3,150,0,0,0);
			 cprefs[5] = new CustomerPrefs(2,3,150,0,0,0);
			 cprefs[6] = new CustomerPrefs(2,3,150,0,0,0);
			 cprefs[7] = new CustomerPrefs(2,3,150,0,0,0);
			 */
/*			
			pp.buyPrices[0] = 250 + rand.nextInt(151);
			pp.buyPrices[1] = 250 + rand.nextInt(151);
			pp.buyPrices[2] = 250 + rand.nextInt(151);
			pp.buyPrices[3] = 250 + rand.nextInt(151);
			pp.buyPrices[4] = 250 + rand.nextInt(151);
			pp.buyPrices[5] = 250 + rand.nextInt(151);
			pp.buyPrices[6] = 250 + rand.nextInt(151);
			pp.buyPrices[7] = 250 + rand.nextInt(151);
			pp.buyPrices[8] = rand.nextInt(151);
			pp.buyPrices[9] = 50 + rand.nextInt(151);
			pp.buyPrices[10] = 50 + rand.nextInt(151);
			pp.buyPrices[11] = rand.nextInt(151);
			pp.buyPrices[12] = rand.nextInt(151);
			pp.buyPrices[13] = 50 + rand.nextInt(151);
			pp.buyPrices[14] = 50 + rand.nextInt(151);
			pp.buyPrices[15] = rand.nextInt(200);
			pp.buyPrices[16] = rand.nextInt(200);
			pp.buyPrices[17] = rand.nextInt(200);
			pp.buyPrices[18] = rand.nextInt(200);
			pp.buyPrices[19] = rand.nextInt(200);
			pp.buyPrices[20] = rand.nextInt(200);
			pp.buyPrices[21] = rand.nextInt(200);
			pp.buyPrices[22] = rand.nextInt(200);
			pp.buyPrices[23] = rand.nextInt(200);
			pp.buyPrices[24] = rand.nextInt(200);
			pp.buyPrices[25] = rand.nextInt(200);
			pp.buyPrices[26] = rand.nextInt(200);
			pp.buyPrices[27] = rand.nextInt(200);
			pp.sellPrices[16] = rand.nextInt((int) pp.buyPrices[16]+1);
			pp.sellPrices[17] = rand.nextInt((int) pp.buyPrices[17]+1);
			pp.sellPrices[18] = rand.nextInt((int) pp.buyPrices[18]+1);
			pp.sellPrices[19] = rand.nextInt((int) pp.buyPrices[19]+1);
			pp.sellPrices[20] = rand.nextInt((int) pp.buyPrices[20]+1);
			pp.sellPrices[21] = rand.nextInt((int) pp.buyPrices[21]+1);
			pp.sellPrices[22] = rand.nextInt((int) pp.buyPrices[22]+1);
			pp.sellPrices[23] = rand.nextInt((int) pp.buyPrices[23]+1);
			pp.sellPrices[24] = rand.nextInt((int) pp.buyPrices[24]+1);
			pp.sellPrices[25] = rand.nextInt((int) pp.buyPrices[25]+1);
			pp.sellPrices[26] = rand.nextInt((int) pp.buyPrices[26]+1);
			pp.sellPrices[27] = rand.nextInt((int) pp.buyPrices[27]+1);
			
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
	   
		/*
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
