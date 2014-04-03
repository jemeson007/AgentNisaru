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
 * SAACompleter
 *
 * Author  : Seong Jae Lee
 * Created : 5 May, 2007
 */

package edu.brown.cs.completer;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import props.Misc;
import ilog.concert.IloIntVar;
import ilog.concert.IloLinearNumExpr;
import ilog.cplex.IloCplex;
import edu.brown.cs.tac.Constants;
import edu.brown.cs.tacds.Allocation;
import edu.brown.cs.tacds.Completion;
import edu.brown.cs.tacds.Repository;
import edu.brown.cs.tacds.Priceline;
import edu.brown.cs.tacds.BidPolicy;
import edu.brown.cs.tacds.TravelPackage;

public class SAACompleter extends Completer {
	protected static final TravelPackage[] m_packages = Constants.makePackages();
	public Repository m_repository;
	
	// When numbers are less than 2000, the difference of 0.0001 is detected.
	public static final float EPSILON = 0.0001f;
	
	// There are infinite number of solutions of maximizing the objective.
	// BID_LOWEST : Bid lowest as possible for hotels. For flights, we bid 1000. 
	// BID_MAXMV  : Bid highest as possible for hotels, but the price should be smaller than its maximum MV. 
	//              It is used in Seong Jae Lee's thesis, and other papers.
	//              It's hard to expend, when we use stochastic flights and entertainments.
	// BID_MAXUTIL: Bid highest as possible for hotels, but the price should be smaller than its maximum utility.
	//              For SS, it's 1000 + 600 (fun bonus).
	//              For TT, it's 1000 + 150 (hotel bonus) + 600 (fun bonus).
	public static final int BID_LOWEST = 0; // SAA Bottom = SAA0, SAAX
	public static final int BID_MAXMV = 1;  // SAA Top in Seong Jae Lee's Thesis
	public static final int BID_MAXUTIL = 2;// 
	
	private int biddingPolicy = BID_LOWEST;
	public void setBidPolicy (int i) { biddingPolicy = i; }
	public int getBidPolicy () { return biddingPolicy; }
	
	// If it's an online game, time restriction should be added.
	private boolean tacGame = true;
	public void setTAC (boolean s) { tacGame = s; }
	public boolean isTAC () { return tacGame; }
	
	// hotel : stochastic, flight & entertainment : deterministic
	protected int numAuction = 28;
	protected int numClient = 8;
	protected int numScenario = 50;
	protected int numPackage = m_packages.length;
	
	public void setNumScenario (int s) { numScenario = s; }
	
	private float[][] constWB, constAB, constWS, constAS;
	private List<Float>[] constWBO, constWSO;
	private boolean[] constClosed;
	private float[] constProb;
	private float[][] constUtility;
	private int[] constOwn, constHQW;
	private boolean[][] constPackageContainGood;
	private float constAccount;
	
	private IloIntVar [][][] varClientPackageInScenario;
	private IloIntVar[][][] varWBidForQth;
	private IloIntVar[][][] varWAskForQth;
	private IloIntVar[][] varABid;
	private IloIntVar[][] varAAsk;
	
	IloLinearNumExpr objective;
	
	public SAACompleter (Repository r) {
		super();
		m_repository = r;
	}
	
	public Completion run () { try {
		m_cplex.clearModel();
		m_cplex.setParam(IloCplex.IntParam.ClockType, 2);
		m_cplex.setOut(null);
		
		if (tacGame) {
			if (m_repository.getGameSec() % 60 > 50) {
				Misc.warn(getClass().getSimpleName() + ".run : too little time left");
				return null;
			}
			
			m_cplex.setParam(IloCplex.DoubleParam.TiLim, 55 - m_repository.getGameSec() % 60);
		}
		
		return super.run();
	} catch (Exception e) { e.printStackTrace(); return null; } }
	
	protected void setConstants () {
		constPackageContainGood = new boolean[numPackage][numAuction];
		constOwn     = new int[numAuction];
		constHQW     = new int[numAuction];
		constUtility = new float[numClient][numPackage];
		constProb    = new float[numScenario];
		constAccount = (float) m_repository.account;
		constClosed  = new boolean[numAuction];
		Arrays.fill(constClosed, false);
		
		constWB = new float[numAuction][numScenario];
		constAB = new float[numAuction][numScenario];
		constWS = new float[numAuction][numScenario];
		constAS = new float[numAuction][numScenario];

		constWBO = new List[numAuction];
		constWSO = new List[numAuction];
		for (int a = 0; a < numAuction; a++) {
			constWBO[a] = new ArrayList<Float>();
			constWSO[a] = new ArrayList<Float>();
		}
		
		for (int p = 0; p < numPackage; p++) {
			for (int a = 0; a < numAuction; a++) {
				constPackageContainGood[p][a] = m_packages[p].containsGood(a);
			}
			
			for (int c = 0; c < numClient; c++) {
				constUtility[c][p] = m_packages[p].calculateUtility(m_repository.getClientPrefs()[c], true, true, true);
			}
		}

		for (int a = 0; a < numAuction; a++) {
			constOwn[a] = m_repository.getOwn(a);
			if (Constants.auctionType(a) == Constants.TYPE_HOTEL) {
				if (m_repository.isAuctionClosed(a)) {
					constClosed[a] = true;
				} else {
					constClosed[a] = false;
				}
				
				constHQW[a] = m_repository.getQuote(a).getHQW();
			}
		}
		
		Priceline[] samples = m_repository.removeXMostRecent(numScenario);
		
		for (int s = 0; s < numScenario; s++) constProb[s] = (float) samples[s].probability;
		
		for (int a = 0; a < numAuction; a++) {
			switch (Constants.auctionType(a)) {
			case Constants.TYPE_FLIGHT:
				for (int i = 0; i < numScenario; i++) {
					constWB[a][i] = samples[i].currBuyPrice[a];
					constAB[a][i] = samples[i].nextBuyPrice[a];
					
					if (!constWBO[a].contains(samples[i].currBuyPrice[a])) {
						constWBO[a].add(samples[i].currBuyPrice[a]);
					}
				}
				
				break;
				
			case Constants.TYPE_HOTEL:
				if (constClosed[a]) break;
				
				for (int i = 0; i < numScenario; i++) {
					float priceToBid = samples[i].currBuyPrice[a];
					constWB[a][i] = priceToBid;
					
					// TAC convention: must offer at least q units at ASK+1 or greater if HQW > 0.
					// However, bids are rejected even when HQW is negative.
					// Therefore, we bid at least ASK+1 no matter what the value of HQW is.
					if (tacGame) {
						constWB[a][i] = Math.max(1.00f, constWB[a][i]);
						float priceOnAuction = m_repository.getQuote(a).getAskPrice();
						priceToBid = Math.max(priceOnAuction + 1.00f + 2 * EPSILON, priceToBid);
					}
					
					if (biddingPolicy == BID_LOWEST) {
						priceToBid += EPSILON;
					} else {
						priceToBid -= EPSILON;
					}
					
					if (!constWBO[a].contains(priceToBid)) {
						constWBO[a].add(priceToBid);
					}
				}
				
				if (biddingPolicy == BID_MAXMV) {
					if (a >= 12) constWBO[a].add(500f);
					if (a <  12) constWBO[a].add(350f);
				}
				
				if (biddingPolicy == BID_MAXUTIL) {
					if (a >= 12 && numAuction == 28) constWBO[a].add(1750f);
					if (a >= 12 && numAuction == 16) constWBO[a].add(1150f);
					if (a <  12 && numAuction == 28) constWBO[a].add(1600f);
					if (a <  12 && numAuction == 16) constWBO[a].add(1000f);
				}
				
				break;
				
			case Constants.TYPE_EVENT:
				// We plus/minus epsilon so that one does not bid/ask at the same time.
				for (int i = 0; i < numScenario; i++) {
					constWB[a][i] = samples[i].currBuyPrice[a];
					constAB[a][i] = samples[i].nextBuyPrice[a];
					constWS[a][i] = samples[i].currSellPrice[a];
					constAS[a][i] = samples[i].nextSellPrice[a];
					
					if (!constWBO[a].contains(samples[i].currBuyPrice[a] + EPSILON)) {
						constWBO[a].add(samples[i].currBuyPrice[a] + EPSILON);
					}
					if (!constWSO[a].contains(samples[i].currSellPrice[a] - EPSILON)) {
						constWSO[a].add(samples[i].currSellPrice[a] - EPSILON);
					}
				}
				
				// If we accidently sold more than we have, we repurchase the ticket by any cost.
				// If we assume we can sell it later with price 200, he will purchase the ticket by any cost.
				if (constOwn[a] < 0) {
					/*
					for (int i = 0; i < numScenario; i++) {
						constAB[a][i] = 200.0f + EPSILON;
						constWS[a][i] = 0.0f;
						constAS[a][i] = 200.0f + EPSILON;
					}
					*/
					
					constWBO[a].clear();
					constWBO[a].add(200.0f);
				}
				
				break;
			}
		}
		
		String toPrint = getClass().getSimpleName() + ".setConstants : own ";
		for (int a = 0; a < numAuction; a++) toPrint += constOwn[a] + " ";
		Misc.println(toPrint);
		
		toPrint = getClass().getSimpleName() + ".setConstants : hqw ";
		for (int a = 8; a < 16; a++) toPrint += constHQW[a] + " ";
		Misc.println(toPrint);
	}
	
	protected void setDecisionVariables () { try {
		varClientPackageInScenario = new IloIntVar[numClient][numPackage][numScenario];
		
		varWBidForQth = new IloIntVar[numAuction][][];
		varWAskForQth = new IloIntVar[numAuction][][];
		varABid = new IloIntVar[numAuction][numScenario];
		varAAsk = new IloIntVar[numAuction][numScenario];
		
		for (int a = 0; a < numAuction; a++) {
			varWAskForQth[a] = new IloIntVar[0][];
			varWBidForQth[a] = new IloIntVar[0][];
			
			switch(Constants.auctionType(a)) {
			case Constants.TYPE_FLIGHT:
				
				varWBidForQth[a] = new IloIntVar[Constants.MAX_QUANTITY][];
				for (int q = 0; q < Constants.MAX_QUANTITY; q++) {
					varWBidForQth[a][q] = new IloIntVar[constWBO[a].size()];
					for (int i = 0; i < constWBO[a].size(); i++) {
						varWBidForQth[a][q][i] = m_cplex.boolVar();
					}
				}

				for (int i = 0; i < numScenario; i++) {
					varABid[a][i] = m_cplex.intVar(0, 8);
				}

				break;
				
			case Constants.TYPE_HOTEL:
				if (constClosed[a]) break;
				
				varWBidForQth[a] = new IloIntVar[Constants.MAX_QUANTITY][];
				for (int q = 0; q < Constants.MAX_QUANTITY; q++) {
					varWBidForQth[a][q] = new IloIntVar[constWBO[a].size()];
					for (int i = 0; i < constWBO[a].size(); i++) {
						varWBidForQth[a][q][i] = m_cplex.boolVar();
					}
				}
				
				break;
				
			case Constants.TYPE_EVENT:
				varWBidForQth[a] = new IloIntVar[1][];
				varWBidForQth[a][0] = new IloIntVar[constWBO[a].size()];
				for (int i = 0; i < varWBidForQth[a][0].length; i++) {
					varWBidForQth[a][0][i] = m_cplex.boolVar();
				}
				
				if (constOwn[a] > 0) {
				varWAskForQth[a] = new IloIntVar[1][];
				varWAskForQth[a][0] = new IloIntVar[constWSO[a].size()];					
				for (int i = 0; i < varWAskForQth[a][0].length; i++) {
					varWAskForQth[a][0][i] = m_cplex.boolVar();
				}
				}
				
				for (int s = 0; s < numScenario; s++) {
					varABid[a][s] = m_cplex.intVar(0, 8);
					varAAsk[a][s] = m_cplex.intVar(0, constOwn[a]);
				}
				
				
				
				break;
			}
		}
		
		for (int a = 0; a < numAuction; a++) {
			for (int q = 0; q < varWBidForQth[a].length; q++) {
				for (int i = 0; i < varWBidForQth[a][q].length; i++) {
					Misc.myassert(varWBidForQth[a][q][i] != null);
				}
			}
			
			for (int q = 0; q < varWAskForQth[a].length; q++) {
				for (int i = 0; i < varWAskForQth[a][q].length; i++) {
					Misc.myassert(varWAskForQth[a][q][i] != null);
				}
			}
		}
		
		for (int c = 0; c < numClient; c++) {
			for (int p = 0; p < numPackage; p++) {
				for (int s = 0; s < numScenario; s++) {
					varClientPackageInScenario[c][p][s] = 
						m_cplex.boolVar("clientGetsPack_c_" + c + "_p_" + p + "_s_" + s);
				}
			}
		}
	} catch (Exception e) { e.printStackTrace(); }}
	
	protected void setObjectiveFunction () { try {
		objective = m_cplex.linearNumExpr();
		
		// this is just for checking how much this algorithm expect to earn. 
		// adding a constant value to objective function doesn't harm. - sjlee
		float totalProb = 0;
		for (int s = 0; s < numScenario; s++) totalProb += constProb[s];
		objective.setConstant(constAccount * totalProb);
		
		for (int s = 0; s < numScenario; s++) {
			IloLinearNumExpr value = m_cplex.linearNumExpr();
			double prob = constProb[s];
			
			// utility
			for (int c = 0; c < numClient; c++) for (int p = 0; p < numPackage; p++) {
				value.addTerm(varClientPackageInScenario[c][p][s], prob * constUtility[c][p]);
			}
			
			// cost and revenue
			for (int a = 0; a < numAuction; a++) {
				switch (Constants.auctionType(a)) {
				case Constants.TYPE_FLIGHT:
					
					for (int q = 0; q < varWBidForQth[a].length; q++) {
						for (int i = 0; i < varWBidForQth[a][q].length; i++) {
							float simPrice = constWB[a][s];
							float bidPrice = constWBO[a].get(i);
							
							if (simPrice - EPSILON <= bidPrice) {
								value.addTerm(varWBidForQth[a][q][i], prob * -1.0 * simPrice);
							}
						}
					}
					
					value.addTerm(varABid[a][s], prob * -1.0 * constAB[a][s]);
					
					break;
					
				case Constants.TYPE_HOTEL:
					if (constClosed[a]) break;
					
					for (int q = 0; q < varWBidForQth[a].length; q++) {
						for (int i = 0; i < varWBidForQth[a][q].length; i++) {
							float simPrice = constWB[a][s];
							float bidPrice = constWBO[a].get(i);
							
							// Bidding too much is always discouraged.
							value.addTerm(varWBidForQth[a][q][i], -EPSILON);
							
							if (simPrice <= bidPrice) {
								value.addTerm(varWBidForQth[a][q][i], prob * -1.0 * simPrice);
								
								if (biddingPolicy == BID_LOWEST) { // discourage bidding high
									value.addTerm(varWBidForQth[a][q][i], prob * (-EPSILON) * bidPrice);									
								} else { // encourage bidding high
									value.addTerm(varWBidForQth[a][q][i], prob * EPSILON * bidPrice);									
								}							
							}
						}
					}
					
					break;
					
				case Constants.TYPE_EVENT:
					
					for (int q = 0; q < varWBidForQth[a].length; q++) {
						for (int i = 0; i < varWBidForQth[a][q].length; i++) {
							float simPrice = constWB[a][s];
							float bidPrice = constWBO[a].get(i);
							
							if (simPrice <= bidPrice) {
								value.addTerm(varWBidForQth[a][q][i], prob * (-1) * (bidPrice));
							}
						}
					}
					
					for (int q = 0; q < varWAskForQth[a].length; q++) {
						for (int i = 0; i < varWAskForQth[a][q].length; i++) {
							float simPrice = constWS[a][s];
							float askPrice = constWSO[a].get(i);
							
							if (simPrice >= askPrice) {
								value.addTerm(varWAskForQth[a][q][i], prob * 1 * (askPrice));
							}
						}
					}
					
					value.addTerm(varABid[a][s], prob * -1.0 * constAB[a][s]);
					value.addTerm(varAAsk[a][s], prob * -1.0 * constAS[a][s]);
					break;
				}
			}
			
			objective.add(value);
		}
		
		// add maximize objective to model
		m_cplex.addMaximize(objective);
	} catch (Exception e) { e.printStackTrace(); System.exit(0);}}
	
	protected void setConstraints () { try {
		// clientPackageInScenario
		for (int c = 0; c < numClient; c++) {
			for (int s = 0; s < numScenario; s++) {
				IloLinearNumExpr constraint = m_cplex.linearNumExpr();
				for (int p = 0; p < numPackage; p++) {
					constraint.addTerm(varClientPackageInScenario[c][p][s], 1.0);
				}
				m_cplex.addEq(constraint, 1.0, "client1pkg_c_" + c + "_s_" + s);
			}
		}
		
		for (int a = 0; a < numAuction; a++) {
			if (constClosed[a]) continue;
			
			for (int q = 0; q < varWBidForQth[a].length; q++) {
				IloLinearNumExpr constraint = m_cplex.linearNumExpr();
				for (int i = 0; i < varWBidForQth[a][q].length; i++) {
					constraint.addTerm(varWBidForQth[a][q][i], 1.0);
				}
				m_cplex.addLe(constraint, 1.0);
			}
			
			if (Constants.auctionType(a) != Constants.TYPE_EVENT) continue;
			
			for (int q = 0; q < varWAskForQth[a].length; q++) {
				IloLinearNumExpr constraint = m_cplex.linearNumExpr();
				for (int i = 0; i < varWAskForQth[a][q].length; i++) {
					constraint.addTerm(varWAskForQth[a][q][i], 1.0);
				}
				m_cplex.addLe(constraint, 1.0);
			}
		}
		
		// Do not allocate more than we will have
		for (int s = 0; s < numScenario; s++)
		for (int a = 0; a < numAuction; a++) {
			IloLinearNumExpr allocNum = m_cplex.linearNumExpr();
			IloLinearNumExpr ownNum   = m_cplex.linearNumExpr();
			
			for (int c = 0; c < numClient; c++) {
				for (int p = 0; p < numPackage; p++) {
					if(constPackageContainGood[p][a]) {
						allocNum.addTerm(varClientPackageInScenario[c][p][s], 1);
					}
				}
			}
			
			ownNum.setConstant(constOwn[a]);
			
			switch (Constants.auctionType(a)) {
			case Constants.TYPE_FLIGHT: 
				
				for (int q = 0; q < varWBidForQth[a].length; q++) {
					for (int i = 0; i < varWBidForQth[a][q].length; i++) {
						float simPrice = constWB[a][s];
						float bidPrice = constWBO[a].get(i);
						if (bidPrice >= simPrice - EPSILON) {
							ownNum.addTerm(varWBidForQth[a][q][i], 1.0);
						}
					}
				}
				
				ownNum.addTerm(varABid[a][s], 1.0);
				
				m_cplex.addLe(allocNum, ownNum);
				break;
				
			case Constants.TYPE_HOTEL:
				if (constClosed[a]) {
					m_cplex.addLe(allocNum, ownNum);
					break;
				}
				
				for (int q = 0; q < varWBidForQth[a].length; q++) {
					for (int i = 0; i < varWBidForQth[a][q].length; i++) {
						float simPrice = constWB[a][s];
						float bidPrice = constWBO[a].get(i);
						if (bidPrice >= simPrice) {
							ownNum.addTerm(varWBidForQth[a][q][i], 1.0);
						}
					}
				}
				
				m_cplex.addLe(allocNum, ownNum);
				break;
				
			case Constants.TYPE_EVENT:
				IloLinearNumExpr sellNum = m_cplex.linearNumExpr();

				for (int q = 0; q < varWAskForQth[a].length; q++) {
					for (int i = 0; i < varWAskForQth[a][q].length; i++) {
						float simPrice = constWS[a][s];
						float bidPrice = constWSO[a].get(i);
						if (bidPrice <= simPrice) {
							sellNum.addTerm(varWAskForQth[a][q][i], 1.0);
							ownNum.addTerm(varWAskForQth[a][q][i], -1.0);
						}
					}
				}
				
				sellNum.addTerm(varAAsk[a][s], 1.0);
				ownNum.addTerm(varAAsk[a][s], -1.0);
				
				m_cplex.addLe(sellNum, Math.max(0, constOwn[a]));
				
				for (int q = 0; q < varWBidForQth[a].length; q++) {
					for (int i = 0; i < varWBidForQth[a][q].length; i++) {
						float simPrice = constWB[a][s];
						float bidPrice = constWBO[a].get(i);
						if (bidPrice >= simPrice) {
							ownNum.addTerm(varWBidForQth[a][q][i], 1.0);
						}
					}
				}
				
				ownNum.addTerm(varABid[a][s], 1.0);
				
				m_cplex.addLe(allocNum, ownNum);
				
				break;
			}
		}
		
		// hqw
		// if hwqNum != 0, hwqNum <= bidNum
		for (int a = 8; a < 16; a++) {
			if (constClosed[a] || constHQW[a]<=0) continue;
			
			IloLinearNumExpr bidNum = m_cplex.linearNumExpr();
			for (int q = 0; q < varWBidForQth[a].length; q++) {
				for (int i = 0; i < varWBidForQth[a][q].length; i++) {
					bidNum.addTerm(varWBidForQth[a][q][i], 1.0);
				}
			}
			
			m_cplex.addLe(constHQW[a], bidNum);
		}
	} catch (Exception e) { e.printStackTrace(); } }

	protected void calculate () { try {
		float totalProb = 0;
		for (int s = 0; s < numScenario; s++) totalProb += constProb[s];
		objective.setConstant(constAccount * totalProb); 
		
		super.calculate();
		Misc.println(getClass().getSimpleName() + ".calculate : expected score " + (int) (m_cplex.getObjValue()/totalProb));
	} catch (Exception e) { e.printStackTrace(); } }
	
	private float getMax (List<Float> list) {
		if (list.isEmpty()) return 0;
		float max = -Float.MAX_VALUE;
		for (Float i : list) {
			if (max < i.floatValue()) max = i.floatValue();
		}
		return max;
	}
	
	protected Completion writeOutput () { try {
		Allocation allocation = new Allocation();
		BidPolicy bidPolicy = new BidPolicy();
		BidPolicy askPolicy = new BidPolicy();
		
		for (int c = 0; c < numClient; c++) {
			for (int p = 0; p < numPackage; p++) {
				if (Math.round(m_cplex.getValue(varClientPackageInScenario[c][p][0])) == 1) {
					TravelPackage tp = new TravelPackage(m_packages[p]);
					tp.calculateUtility(m_repository.getClientPrefs()[c], true, true, true);
					allocation.setTravelPackage(c, tp);
				}
			}
		}
		
		for (int a = 0; a < numAuction; a++) {
			switch (Constants.auctionType(a)) {
			case Constants.TYPE_FLIGHT:
				for (int q = 0; q < varWBidForQth[a].length; q++) {
					for (int i = 0; i < varWBidForQth[a][q].length; i++) {
						int value = (int) Math.round(m_cplex.getValue(varWBidForQth[a][q][i]));
						Misc.myassert(value == 1 || value == 0);
						if (value == 0) continue;
						
						float bidPrice = constWBO[a].get(i);
						
						
						if (bidPrice >= getMax(constWBO[a]) - EPSILON) {
							bidPolicy.addPrice(a, 1000);
						} else {
							if (bidPrice > EPSILON) bidPolicy.addPrice(a, bidPrice); // it expects to lose sometimes
						}
					}
				}
				
				break;
				
			case Constants.TYPE_HOTEL:
				if (constClosed[a]) break;
				
				for (int q = 0; q < varWBidForQth[a].length; q++) {
					for (int i = 0; i < varWBidForQth[a][q].length; i++) {
						int value = (int) Math.round(m_cplex.getValue(varWBidForQth[a][q][i]));
						Misc.myassert(value == 1 || value == 0);
						if (value == 0) continue;
						
						float bidPrice = constWBO[a].get(i);
						if (bidPrice > EPSILON) bidPolicy.addPrice(a, bidPrice);
					}
				}
				
				break;
				
			case Constants.TYPE_EVENT:
				for (int q = 0; q < varWBidForQth[a].length; q++) {
					for (int i = 0; i < varWBidForQth[a][q].length; i++) {
						int value = (int) Math.round(m_cplex.getValue(varWBidForQth[a][q][i]));
						Misc.myassert(value == 1 || value == 0);
						if (value == 0) continue;
						
						float bidPrice = constWBO[a].get(i);
						if (bidPrice > EPSILON) bidPolicy.addPrice(a, bidPrice);
					}
				}
				
				for (int q = 0; q < varWAskForQth[a].length; q++) {
					for (int i = 0; i < varWAskForQth[a][q].length; i++) {
						int value = (int) Math.round(m_cplex.getValue(varWAskForQth[a][q][i]));
						Misc.myassert(value == 1 || value == 0);
						if (value == 0) continue;
						
						float askPrice = constWSO[a].get(i);
						if (askPrice > EPSILON) askPolicy.addPrice(a, askPrice);
					}
				}
				
				break;
			}
		}				
		
		bidPolicy.sort();
		askPolicy.sort();
		
		Completion ret = new Completion(allocation, bidPolicy, askPolicy);
		Misc.println(getClass().getSimpleName() + ".writeOutput : " + ret);
		return ret;
	} catch (Exception e) { e.printStackTrace(); return null; } }
}

