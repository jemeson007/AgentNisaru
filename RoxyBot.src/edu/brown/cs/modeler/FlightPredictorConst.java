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
 * FlightPredictorConst
 *
 * Author  : Seong Jae Lee
 * Created : 5 May, 2007
 * 
 * We want to make sure the flight prediction and bidding is not biased. 
 * Thus, flight prediction should be made right after the first quote update of each minute is called. 
 * And, each agent should bid right after the last quote update of each minute is called. 
 * In case of SAA, current flight prediction should be the expected (i.e. deterministic) minimum price between current second to the end. 
 * We do not have to change SAA completer.
 * 
 */


package edu.brown.cs.modeler;

import props.Misc;
import edu.brown.cs.tac.Constants;
import edu.brown.cs.tacds.Priceline;
import edu.brown.cs.tacds.Repository;

public class FlightPredictorConst extends FlightPredictor {
	private float[] prices;
	private int bidding_option;
	public static final int BID_LASTTIME = 1;
	public static final int BID_FIRSTTIME = 2;
	public static final int BID_NOMODIFICATION = 3;
	
	public FlightPredictorConst(int price) {
		prices = new float[8]; 
		for (int a = 0; a < 8; a++) prices[a] = price;
	}
	
	public void setBiddingOption (int o) { bidding_option = o; }
	public int getBiddingOption () { return bidding_option; }
	
	@Override
	public void init() {
		for (int i = 0; i < 8; i++) prices[i] = 0;
	}
	
	@Override
	public void setPrice (int auction, int decisec, int price) { 
		prices[auction] = price;
	}
	
    public Priceline predict (Repository r, int option, Priceline sample) {
		for (int a = 0; a < 8; a++) {
			if (option == PREDICT_MU) {
				if (bidding_option == BID_FIRSTTIME) {
					if (r.getGameMinute() == 0) sample.currBuyPrice[a] = prices[a];
					else sample.currBuyPrice[a] = Constants.INFINITY;
				} else {
					sample.currBuyPrice[a] = prices[a];
				}
			}
			
			if (option == PREDICT_TAT) {
				sample.currBuyPrice[a] = prices[a];
	    	}
			
			if (option == PREDICT_SAA) {
				if (bidding_option == BID_LASTTIME) {
					if (r.getGameMinute() < 8) {
						sample.currBuyPrice[a] = Constants.INFINITY;
						sample.nextBuyPrice[a] = prices[a];
					} else {
						sample.currBuyPrice[a] = prices[a];
						sample.nextBuyPrice[a] = Constants.INFINITY;
					}
				}
				
				if (bidding_option == BID_FIRSTTIME) {
					if (r.getGameMinute() == 0) sample.currBuyPrice[a] = prices[a];
					else sample.currBuyPrice[a] = Constants.INFINITY;
					
					sample.nextBuyPrice[a] = Constants.INFINITY;
				}
				
				if (bidding_option == BID_NOMODIFICATION) {
					sample.currBuyPrice[a] = prices[a];
					
					if (r.getGameMinute() < 8) sample.nextBuyPrice[a] = prices[a];
					else sample.nextBuyPrice[a] = Constants.INFINITY;
				}
			}
		}
		
		return sample;
    }
	
	@Override
	protected float getPrice (int auction, int option, int decisec, int curprice) { 
		Misc.myassert(false);
		return 0; 
	}
	
	@Override
	// called from Agent
	public boolean buyNow (int auction, int decisec) { 
		switch (bidding_option) {
		case BID_NOMODIFICATION:
			return true;
		case BID_LASTTIME:
			if (decisec < 48) return false;
			else return true;
		case BID_FIRSTTIME:
			if (decisec < 6) return true;
			else return false;
		default:
			Misc.myassert(false);
			return true;
		}
	}
		
	@Override
	public FlightPredictor clone() {
		FlightPredictorConst ret = new FlightPredictorConst(0);
		
		for (int a = 0; a < 8; a++)
			for (int ds = 0; ds < 54; ds++) 
				ret.setPrice(a, ds, (int) getPrice(a, AFTER_MIN, ds, 0));
		
		ret.setBiddingOption(this.getBiddingOption());
		
		return ret;
	}
}
