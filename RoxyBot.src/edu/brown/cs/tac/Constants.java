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
 * Constants
 *
 * Author  : Bryan Guillemette
 * Created : 1 May, 2005
 * Description: 
 *    
 * Constants and static methods.
 * 
 * Author  : Seong Jae Lee
 * Updated : 15 May, 2007
 * 
 * Obsolete variables are removed.
 * 
 */

package edu.brown.cs.tac;

import java.util.Arrays;
import java.util.Random;

import edu.brown.cs.tacds.Preference;
import edu.brown.cs.tacds.TACCollection;
import edu.brown.cs.tacds.TravelPackage;
import se.sics.tac.aw.*;

public class Constants {
	
	/*
	 * There are 28 auctions in the game, covering 7 items each associated with 4 seperate days.
	 * The problem with this paradigm, is that the typical item is associated with days 1-4 and one
	 * of the items (outflight) is associated with days 2-5.  This is annoying.  We can not get around
	 * having to translate from actual days to indices.  We will use the following convention: "day"
	 * will be the variable name used exclusively with real days (1-5), and "dayIndex" will be the
	 * variable used for indexing into arrays and auctions (0-3).  (0-3 even for outflight... even
	 * though outflights occur on days 2-5, like all other items, there are 4 outflight-days, so
	 * index using 0-3) 
	 */

    // offset for each auction
    public static final int AUCTION_INFLIGHT=0;
    public static final int AUCTION_OUTFLIGHT=4;
    public static final int AUCTION_BAD_HOTEL=8;
    public static final int AUCTION_GOOD_HOTEL=12;
    public static final int AUCTION_ALLIGATOR=16;
    public static final int AUCTION_AMUSEMENT=20;
    public static final int AUCTION_MUSEUM=24;
    // number of auctions
    public static final int AUCTION_MAX=28;
    public static final int NUM_FLIGHT_AUCTIONS=8;
    public static final int NUM_HOTEL_AUCTIONS=8;
    public static final int NUM_EVENT_AUCTIONS=12;

    public static final int TYPE_FLIGHT = TACAgent.CAT_FLIGHT;
    public static final int TYPE_HOTEL = TACAgent.CAT_HOTEL;
    public static final int TYPE_EVENT = TACAgent.CAT_ENTERTAINMENT;
    
    // Event offset.
    public static final int EVENT_OFFSET=4;
    public static boolean EVENTS_ON = false; // do we have events in the experiments?
    
	// For when you need to distinguish between GOOD and BAD hotels,
	// BAD is always 0, GOOD is always 1 - don't name them like this to avoid confusion with AUCTION_BAD_HOTEL and AUCTION_GOOD_HOTEL. they are not used anywhere anyway vn
	//public static final int BAD_HOTEL = 0;
	//public static final int GOOD_HOTEL = 1;

    // Max number of copies of a good we could ever use (not considering
    // obtaining goods with the purpose of denying other agents)
    public static int MAX_QUANTITY = 8;
    
	// Biggest number ever
    public static final float INFINITY = 10000;
    
    // Max price for even tickets
    public static final float MAX_PRICE_FOR_EVENT = 200;
    public static final float MAX_PRICE_FOR_FLIGHT = 800;
    
	// Number of Agents and clients in a game
	public static final int NUM_AGENTS = 8;
	public static final int NUM_CLIENTS_PER_AGENT = 8;
    public static final int NUM_DAYS = 5;
    public static final int NUM_DAY_INDICES = 4;
    
    // For Allocation
    public static final int NUM_CLIENTS_PER_ALLOCATION = 8;
    
    // number of unique travel packages (392 + the null package)
    public static final int NUM_PACKAGES = 393;
    public static final int NUM_PACKAGES_WITHOUT_FLIGHTS = 21;
    
    public static final int UNION = 1;//do we want to bid on the union of the goods from multiple completions
    public static final int ALL = 2;//do we want to bid on all goods
    public static final int INTERSECTION = 3;//do we want to bid on all goods
    
    public static final int GLOBAL_BEST_PRICE = 1;
    public static final int NOW_LATER_BEST_PRICES = 2;
    
    //for price prediction evaluator
    public static boolean PREDICTION_EVALUATOR = false;
    
    //for offline simulations
    public static boolean SIMULATOR = false;
    public static boolean SEQUENTIAL_SIMULATOR = false;
    public static boolean SEQUENTIAL_SIMULATOR128 = false;
    public static boolean SIMULTANEOUS_SIMULATOR = false;    
    public static boolean SIMULATOR_CANNOT_WITHDRAW_BIDS = false;//synonimous to unknown order of closing
    public static boolean SIMULATOR_CLOSE_FLIGHTS = false;
    
    public static int auctionType (int auctionNo) {
    	if (auctionNo < AUCTION_BAD_HOTEL) return TYPE_FLIGHT;
    	if (auctionNo >= AUCTION_ALLIGATOR)	return TYPE_EVENT;
    	
    	return TYPE_HOTEL;
    }
    
    public static String auctionNoToString(int auctionNo) {
    	if (auctionNo >= 0 && auctionNo < 4) {
    		return " In Day" + (auctionNo - AUCTION_INFLIGHT  + 1) + " ";
    	} else if (auctionNo < 8) {
			return "Out Day" + (auctionNo - AUCTION_OUTFLIGHT + 2) + " ";
    	} else if (auctionNo < 12) {
			return " BH Day" + (auctionNo - AUCTION_BAD_HOTEL + 1) + " ";
    	} else if (auctionNo < 16) {
			return " GH Day" + (auctionNo - AUCTION_GOOD_HOTEL+ 1) + " ";
    	} else if (auctionNo < 20) {
			return "Ev1 Day" + (auctionNo - AUCTION_ALLIGATOR + 1) + " ";
    	} else if (auctionNo < 24) {
			return "Ev2 Day" + (auctionNo - AUCTION_AMUSEMENT + 1) + " ";
    	} else if (auctionNo < 28) {
			return "Ev3 Day" + (auctionNo - AUCTION_MUSEUM    + 1) + " ";
    	} else {
    		return "";
    	}
    }
    
    /*
     * For Sale Collection
     */
    public static TACCollection forSale() {
        TACCollection ret = new TACCollection();
        ret.owned = new int [Constants.AUCTION_MAX];
        Arrays.fill(ret.owned, MAX_QUANTITY);
        //do  not need this for ec06 experiments - vn
        //for (int a = AUCTION_BAD_HOTEL; a < AUCTION_ALLIGATOR; a++) {
        //    ret.owned[a] = 4;
        //}
        return ret;
    }
    
	public static String bidToString (Bid b) {
		String ret = "Bid:\n";
		ret += "\tAuction ............... = " + auctionNoToString(b.getAuction()) + "\n";
		ret += "\tBid Hash .............. = " + b.getBidHash() + "\n";
		ret += "\tBig String ............ = " + b.getBidString() + "\n";
		ret += "\tBid ID ................ = " + b.getID() + "\n";
		ret += "\tNumber Bid Points ..... = " + b.getNoBidPoints() + "\n";
		ret += "\tProcessing State ...... = " + b.getProcessingState() + "\n";
		ret += "\tPS String ............. = " + b.getProcessingStateAsString() + "\n";
		ret += "\tQuantity .............. = " + b.getQuantity() + "\n";
		for (int i = b.getNoBidPoints() - 1; i >= 0; i--) {
			//ret += "\tBid Point " + i + " ........... = (" + b.getQuantity(i) + "," + b.getPrice(i) + ")\n";
		}
		ret += "\tReject Reason ......... = " + b.getRejectReason() + "\n";
		ret += "\tRR String ............. = " + b.getRejectReasonAsString() + "\n";
		ret += "\tTime Close ............ = " + b.getTimeClosed() + "\n";
		ret += "\tTime Processed ........ = " + b.getTimeProcessed() + "\n";
		ret += "\tIs Awaiting Transaction = " + b.isAwaitingTransactions() + "\n";
		ret += "\tIs Preliminary ........ = " + b.isPreliminary() + "\n";
		ret += "\tIs Rejected ........... = " + b.isRejected() + "\n";
		return ret;
	}
	
	public static String quoteToString (Quote q) {
		String ret = "Quote:\n";
		ret += "\tAsk Price ............. = " + q.getAskPrice() + "\n";
		ret += "\tAuction ............... = " + auctionNoToString(q.getAuction()) + "\n";
		ret += "\tAuction Status ........ = " + q.getAuctionStatus() + "\n";
		ret += "\tAuction Status String . = " + q.getAuctionStatusAsString() + "\n";
		ret += "\tBid Price ............. = " + q.getBidPrice() + "\n";
		ret += "\tHQW ................... = " + q.getHQW() + "\n";
		ret += "\tLast Quote Time ....... = " + q.getLastQuoteTime() + "\n";
		ret += "\tNext Quote Time ....... = " + q.getNextQuoteTime() + "\n";
		return ret;
	}
	
	public static String transactionToString (Transaction t) {
		String ret = "Transaction:\n";
		ret += "\tAuction ............... = " + auctionNoToString(t.getAuction()) + "\n";
		ret += "\tPrice ................. = " + t.getPrice() + "\n";
		ret += "\tQuantity .............. = " + t.getQuantity() + "\n";
		return ret;
	}
		
	public static TravelPackage[] makePackages() {
		if (EVENTS_ON) return makeAllPackages();
		else return makePackagesWithoutEvents();
	}

	public static TravelPackage[] makePackagesWithoutEvents() {
		TravelPackage [] ret = new TravelPackage[NUM_PACKAGES_WITHOUT_FLIGHTS];
		ret[0] = TravelPackage.nullPackage();
		int count = 1;
		for(int arrivalDayIndex = 0; arrivalDayIndex <= 3; arrivalDayIndex++) {
			for (int departureDayIndex = arrivalDayIndex; departureDayIndex <= 3; departureDayIndex++) {
				for (int hotelType = 0; hotelType <= 1; hotelType++) {
					ret[count] = new TravelPackage(arrivalDayIndex,
							departureDayIndex,
							hotelType,
							0,
							0,
							0,
							0);
					count++;
				}
			}
		}
		return ret;
	}

	/**
	 * Calculates all 393 possible travel packages (including the null package)
	 */
	public static TravelPackage[] makeAllPackages() {

		TravelPackage [] ret = new TravelPackage[NUM_PACKAGES];
		ret[0] = TravelPackage.nullPackage();
		int count = 1;

		for(int arrivalDayIndex = 0; arrivalDayIndex <= 3; arrivalDayIndex++) {
			for (int departureDayIndex = arrivalDayIndex; departureDayIndex <= 3; departureDayIndex++) {
				for (int hotelType = 0; hotelType <= 1; hotelType++) {
					for (int numEventTickets = 0; numEventTickets <= (departureDayIndex - arrivalDayIndex + 1) && numEventTickets <= 3; numEventTickets++) {
						// whichEvents[permutation][numEvent]
						int [][] whichEventsAndWhatOrder = permuteEventsAndOrders(numEventTickets);
						// which days[permutation][numEvent]
						int [][] whichDays = permuteDays(arrivalDayIndex, departureDayIndex, numEventTickets);
						for (int perm1 = 0; perm1 < whichEventsAndWhatOrder.length; perm1++) {
							for (int perm2 = 0; perm2 < whichDays.length; perm2++) {
								int eventDayIndex0 = 0;
								int eventDayIndex1 = 0;
								int eventDayIndex2 = 0;
								int eventDayIndex3 = 0;
								
								for (int numEvent = 0; numEvent < numEventTickets; numEvent++) {
									switch (whichDays[perm2][numEvent]) {
										case 0 :
											eventDayIndex0 = whichEventsAndWhatOrder[perm1][numEvent]; break;
										case 1 :
											eventDayIndex1 = whichEventsAndWhatOrder[perm1][numEvent]; break;
										case 2 :
											eventDayIndex2 = whichEventsAndWhatOrder[perm1][numEvent]; break;
										case 3 :
											eventDayIndex3 = whichEventsAndWhatOrder[perm1][numEvent]; break;
									}
								}
								
								ret[count] = new TravelPackage(arrivalDayIndex,
																departureDayIndex,
																hotelType,
																eventDayIndex0,
																eventDayIndex1,
																eventDayIndex2,
																eventDayIndex3);
								count++;
							}
						}
					}
				}
			}
		}
		return ret;
	}
    
	private static int [][] permuteEventsAndOrders(int numEvents) {
		switch (numEvents) {
			case 0 :
				return new int [][] {{}};
			case 1 :
				return new int [][] {{1}, {2}, {3}};
			case 2 :
				return new int [][] {{1,2}, {1,3}, {2,1}, {2,3}, {3,1}, {3,2}};
			case 3 :
				return new int [][] {{1,2,3}, {1,3,2}, {2,1,3}, {2,3,1}, {3,1,2}, {3,2,1}};
			default :
				return new int [][] {{}};
		}
	}

	private static int [][] permuteDays(int arrival, int departure, int numEvents) {
		int numDays = departure - arrival + 1;
		switch (numDays) {
			case 1 :
				switch (numEvents) {
					case 0 :
						return new int [][] {{}};
					case 1 :
						return new int [][] {{arrival}};
				}
				break;
			case 2 :
				switch (numEvents) {
					case 0 :
						return new int [][] {{}};
					case 1 :
						return new int [][] {{arrival}, 
											 {arrival + 1}}; 
					case 2 :
						return new int [][] {{arrival, arrival + 1}};
				}
				break;
			case 3 :
				switch (numEvents) {
					case 0 :
						return new int [][] {{}};
					case 1 :
						return new int [][] {{arrival}, 
											 {arrival + 1}, 
											 {arrival + 2}};
					case 2 :
						return new int [][] {{arrival, arrival + 1},
											 {arrival, arrival + 2},
											 {arrival + 1, arrival + 2}};
					case 3 :
						return new int [][] {{arrival, arrival + 1, arrival + 2}};
				}
				break;
			case 4 :
				switch (numEvents) {
					case 0 :
						return new int [][] {{}};
					case 1 :
						return new int [][] {{arrival},
											 {arrival + 1},
											 {arrival + 2},
											 {arrival + 3}};
					case 2 :
						return new int [][] {{arrival, arrival + 1},
											 {arrival, arrival + 2},
											 {arrival, arrival + 3},
											 {arrival + 1, arrival + 2},
											 {arrival + 1, arrival + 3},
											 {arrival + 2, arrival + 3}};
					case 3 :
						return new int [][] {{arrival, arrival + 1, arrival + 2},
											 {arrival, arrival + 1, arrival + 3},
											 {arrival, arrival + 2, arrival + 3},
											 {arrival + 1, arrival + 2, arrival + 3}};
				}
				break;
			default :
				return new int [][] {{}};
		}
		return new int [][] {{}};
	}

    /*
     * Generates random customer preferences for one client
     */
    public static Preference genRandClientPrefs(Random rand) {
    	
    	//there are 10 possible arrival/departure combinations, call them iteneraries
    	int itenerary = rand.nextInt(10)+1;
    	int arrival = 0;
    	int departure = 0;
    	switch(itenerary) {
    		case 1	:	arrival = 1; departure = 2; break;
    		case 2	:	arrival = 1; departure = 3; break;
    		case 3	:	arrival = 1; departure = 4; break;
    		case 4	:	arrival = 1; departure = 5; break;
    		case 5	:	arrival = 2; departure = 3; break;
    		case 6	:	arrival = 2; departure = 4; break;
    		case 7	:	arrival = 2; departure = 5; break;
    		case 8	:	arrival = 3; departure = 4; break;
    		case 9	:	arrival = 3; departure = 5; break;
    		case 10	:	arrival = 4; departure = 5; break;
    		default	:	System.out.println("Random isn't working as I expect."); break;
    	}
    	int hotelBonus = rand.nextInt(101)+50;
    	int event1Bonus = rand.nextInt(201);
    	int event2Bonus = rand.nextInt(201);
    	int event3Bonus = rand.nextInt(201);
    	return new Preference(arrival,
    							  departure,
    							  hotelBonus,
    							  event1Bonus,
    							  event2Bonus,
    							  event3Bonus);
    }

    public static int lastAuction() {
    	if (EVENTS_ON) return Constants.AUCTION_MAX;
    	else return Constants.AUCTION_ALLIGATOR;
    }


}
