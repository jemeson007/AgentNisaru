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
 * FlightPredictor
 *
 * Author  : Seong Jae Lee
 * Created : 5 May, 2007
 * 
 */

package edu.brown.cs.modeler;

import edu.brown.cs.tacds.Priceline;
import edu.brown.cs.tacds.Repository;

public abstract class FlightPredictor {
	
	protected final static int CURRENT = 0;         // [curr]
	protected final static int HIST_MIN = 1;        // min[0, curr]
	protected static final int WITHIN_1MIN = 2;     // min[curr, curr+1] 
	protected static final int AFTER_1MIN = 4;      // min[curr+1, end]
	protected static final int AFTER_MIN = 6;       // min[curr, end]
	protected static final int TOTAL_MIN = 8;       // min[0, end]
    
    public static final int PREDICT_TAT = 1;
    public static final int PREDICT_MU  = 2;
    public static final int PREDICT_SAA = 3;
    
    abstract public void init();
    abstract public void setPrice (int auction, int decisec, int price);
    abstract protected float getPrice (int auction, int option, int decisec, int curPrice);
    abstract public boolean buyNow (int auction, int time);
    abstract public FlightPredictor clone();
	
    public final void predict (Repository r, int option) {
		Priceline[] samples = r.getXMostRecent(r.getAvailableScenarios().length);
		r.clearScenarios();
		
		for (int i = 0; i < samples.length; i++) {
			samples[i] = predict(r, option, samples[i]);			
			r.addScenario(samples[i]);
		}
    }
    
    public Priceline predict (Repository r, int option, Priceline sample) {
		int decisec = r.getGameDecisec();
		
		for (int a = 0; a < 8; a++) {
			int curprice = (int) r.getQuote(a).getAskPrice();
			
			if (option == PREDICT_TAT || option == PREDICT_MU) {
				sample.currBuyPrice[a] = getPrice(a, AFTER_MIN, decisec, curprice);
	    	}
			
			if (option == PREDICT_SAA) {
				sample.currBuyPrice[a] = getPrice(a, WITHIN_1MIN, decisec, curprice);
				sample.nextBuyPrice[a] = getPrice(a, AFTER_1MIN, decisec, curprice);
			}
		}
		
		return sample;
    }
    
    /*
    // This is only used for prediction
	public void predisct (Repository r) {
		Priceline[] samples = r.getXMostRecent(r.getAvailableScenarios().length);
		int decisec = r.getGameDecisec();
		r.clearScenarios();

		for (int i = 0; i < samples.length; i++) {
			for (int a = 0; a < 8; a++) {
				int curprice = (int) r.getQuote(a).getAskPrice();
				samples[i].currBuyPrice[a] = getPrice(a, WITHIN_1MIN, decisec, curprice);
				samples[i].nextBuyPrice[a] = getPrice(a, AFTER_1MIN, decisec, curprice);
			}
			r.addScenario(samples[i]);
		}
	}
	
	public Priceline predict (Repository r, Priceline sample, int curr_option) {
		int decisec = r.getGameDecisec();
		
		for (int a = 0; a < 8; a++) {
			int curprice = (int) r.getQuote(a).getAskPrice();
			sample.currBuyPrice[a] = getPrice(a, curr_option, decisec, curprice);
			sample.nextBuyPrice[a] = getPrice(a, AFTER_1MIN, decisec, curprice);
		}
		
		return sample;
	}
	*/
}
