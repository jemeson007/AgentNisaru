package edu.brown.cs.tacds;

import edu.brown.cs.tac.Constants;
import edu.brown.cs.tac.TacRandom;

import java.util.*;

import props.Misc;

public class Client {
	public Preference preference;
	public int[] collection;
	
	// This is used for Walverine tatonnement
	// OLD_EVENT_VALUE is used to calculate Walverine Constant (Wellman 2002, price prediction).
	// It is also used to calculate EVPP (Wellman 2002, price prediction).
	public static double[][] OLD_EVENT_VALUE = {{74.7, 101.5, 106.9, 112.7}, {0, 66.2, 93.0, 106.9}, {0, 0, 66.2, 101.5}, {0, 0, 0, 74.7}}; // old value
	// all the other Walverine tatonnement uses EVENT_VALUE.
	private static double[][] EVENT_VALUE = {{78.01348244984501, 112.05646811961518, 119.85582720283928, 120.85583957807923}, {0, 76.64329298341406, 110.68627865318422, 119.85582720283928},  {0, 0, 76.64329298341406, 112.05646811961518}, {0, 0, 0, 78.01348244984501}};
	private static float EVENT_PRICE_14 = 72.867539f;
	private static float EVENT_PRICE_23 = 74.309567f;
	
	public static final int EVENT_OLD_CONST_MARGIN = 0;
	public static final int EVENT_NEW_CONST_MARGIN = 1;
	public static final int EVENT_NO_MARGIN = 2;
	public static final int EVENT_WITH_CONST_PRICE = 3;
	
	// random client generation
	public Client () {
		preference = new Preference();
		collection = new int[28];
	}

	public Client (Client c) {
		preference = new Preference(c.preference);
		collection = new int[28];
		for (int i = 0; i < collection.length; i++) {
			collection[i] = c.collection[i];
		}
	}
	
	public Client (Preference p) {
		preference = p;
		collection = new int[28];
	}
	
	// This is used for Walverine tatonnement on Walverine paper 2002
	// make sure that inflight : 1 ~ 4, outflight 2 ~ 5, hotel 0 : bad
	public float getValue (final MiniTravelPackage p, final Priceline prices, boolean addHotelBonus, int eventOption) {
		if (p.in == 0 && p.out ==0) return 0; // null package
		float travelPenalty = 100 * (Math.abs(p.in - preference.getArrivalDate()) + Math.abs(p.out - preference.getDepartureDate()));
		float hotelBonus = (addHotelBonus && p.hotel == 1) ? preference.getHotelValue() : 0;
		float eventMargin = getEventMargin(p, eventOption);
		float value = TravelPackage.BASIC_UTILITY - travelPenalty + hotelBonus + eventMargin - getCost(p, prices);
		return value;
	}
	
	private float getEventMargin (final MiniTravelPackage p, int option) {
		switch (option) {
		case EVENT_OLD_CONST_MARGIN: return (float) OLD_EVENT_VALUE[p.in - 1][p.out - 2];
		case EVENT_NEW_CONST_MARGIN: 	return (float) EVENT_VALUE[p.in - 1][p.out - 2];
		case EVENT_NO_MARGIN: return 0;
		case EVENT_WITH_CONST_PRICE: return getBestEventMargin(p.in, p.out);
		}
		
		Misc.myassert(false);
		return 0;
	}
	
	private float getCost(final MiniTravelPackage p, final Priceline prices) {
		float ret = 0;
		int auctionNo;
		
		auctionNo = Constants.AUCTION_INFLIGHT  + p.in  - 1;
		ret += (1 - collection[auctionNo]) * prices.currBuyPrice[auctionNo];
		
		auctionNo = Constants.AUCTION_OUTFLIGHT + p.out - 2;
		ret += (1 - collection[auctionNo]) * prices.currBuyPrice[auctionNo];
		
		for (int i = p.in; i < p.out; i++) {
			auctionNo = Constants.AUCTION_BAD_HOTEL + 4 * p.hotel + i - 1;
			ret += (1 - collection[auctionNo]) * prices.currBuyPrice[auctionNo];
		}
		
		return ret;
	}
	
	// This is used for client perspective tatonnement.
	// It also uses constant event bonus to save time.
	public float[] getDemand (Priceline prices) {
		float[] ret = new float[28];
		
		ArrayList<MiniTravelPackage> optPackages = new ArrayList<MiniTravelPackage>();
		optPackages.add(new MiniTravelPackage(0, 0, 0, -1.0));
		
		for (int in = 1; in <= 4; in++) {
			for (int out = in+1; out <= 5; out++) {
				for (int h = 0; h <= 1; h++) {
					MiniTravelPackage p = new MiniTravelPackage(in, out, h, 0);
					int eventOption = Constants.EVENTS_ON ? EVENT_NEW_CONST_MARGIN : EVENT_NO_MARGIN; 
					p.value = getValue(p, prices, true, eventOption);
					
					if (p.value > optPackages.get(0).value) {
						optPackages.clear();
						optPackages.add(p);
					} else if (p.value == optPackages.get(0).value) {
						optPackages.add(p);
					}
				}
			}
		}
		
		MiniTravelPackage optPackage = optPackages.get(TacRandom.nextInt(this, optPackages.size()));
		
		if (optPackage.value > 0) {
			int auctionNo;
			
			auctionNo = Constants.AUCTION_INFLIGHT  + optPackage.in - 1;
			ret[auctionNo] = 1 - collection[auctionNo];
			Misc.myassert(collection[auctionNo] <= 1);
			
			auctionNo = Constants.AUCTION_OUTFLIGHT + optPackage.out - 2;
			ret[auctionNo] = 1 - collection[auctionNo];
			Misc.myassert(collection[auctionNo] <= 1);
			
			for (int i = optPackage.in; i < optPackage.out; i++) {
				if (optPackage.hotel == 0) auctionNo = Constants.AUCTION_BAD_HOTEL + i - 1;
				if (optPackage.hotel == 1) auctionNo = Constants.AUCTION_GOOD_HOTEL+ i - 1;
				ret[auctionNo] = 1 - collection[auctionNo];
				Misc.myassert(collection[auctionNo] <= 1);
			}
		}
		
		return ret;
	}
	
	// in : 1 ~ 4, out : 2 ~ 5
	// This is still hacky, because we are using expected event price.
	private float getBestEventMargin (int inflight, int outflight) {
		ArrayList<Integer> availableDays = new ArrayList<Integer>();
		for (int i = inflight; i < outflight; i++) availableDays.add(new Integer(i));
		return getBestEventMargin(availableDays, 0);
	}
	
	// index : 0 ~ 2
	private float getBestEventMargin (final ArrayList<Integer> available, int index) {
		float ret = 0;
		for (int i = 0; i < available.size(); i++) {
			int day = available.get(i); // buy event[index] ticket, 1 ~ 4
			int bonus = preference.getEventValue(index + 1);
			float pay = (Math.abs(day - 2.5) < 1) ? EVENT_PRICE_23 : EVENT_PRICE_14;
			if (bonus - pay < 0) continue;
			
			float margin = bonus - pay;
			if (index != 2) {
				ArrayList<Integer> nextAvailable = new ArrayList<Integer>();
				for (int j = 0; j < available.size(); j++) {
					if (i == j) continue;
					nextAvailable.add(available.get(j));
				}
				margin += getBestEventMargin(nextAvailable, index + 1);
			}
			
			if (margin > ret) ret = margin;
		}
		
		if (index != 2) {
			float margin = getBestEventMargin(available, index + 1);
			if (margin > ret) ret = margin;
		}
		
		return ret;
	}
	
	// in : 1 ~ 4, out : 2 ~ 5
	// it is also used in edu.brown.cs.predictionEvaluation.Statistics
	public static float getConstBestEventMargin (int inflight, int outflight) {
		return (float) EVENT_VALUE[inflight-1][outflight-2];
	}
	
	public String toString() {
		String ret="client:";
		ret += preference.toString();
		for(int i=0; i<collection.length; i++) {
			if(collection[i] > 0) {
				ret += " " + collection[i] + " of " + i;
			}
		}
		return ret;
	}
}

