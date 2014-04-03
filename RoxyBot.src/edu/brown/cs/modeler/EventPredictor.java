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
 * EventPredictor
 *
 * Author  : Seong Jae Lee
 * Created : 5 May, 2007
 * 
 * This predictor is built for no-entertainment option.
 * 
 */

package edu.brown.cs.modeler;

import edu.brown.cs.tacds.Priceline;
import edu.brown.cs.tacds.Repository;

public class EventPredictor {
	public void init() {};
	public double getExpBidPrisce(int a, int sec) {return 200; }; // curr. exp. buy price
	public double getExpAskPrisce(int a, int sec) {return 0; }; // curr. exp. sell price
	public double getPredBidPrice(int a, int sec) {return 200; }; // curr. rand. buy price
	public double getPredAskPrice(int a, int sec) {return 0; }; // curr. rand. sell price
	public double getMaxBidPrice(int a, int sec) {return 200; }; // future rand. sell price
	public double getMinAskPrice(int a, int sec) {return 0; }; // future rand. buy price
	public void setNext() { ; }; // used for stochastic predictor
	
	public Priceline predict (Repository r, Priceline sample) {
		int sec = r.getGameDecisec() * 10;
		
		for (int a = 16; a < 28; a++) {
			if (r.getQuote(a) == null) continue;
			
			double currAskPrice = (r.getQuote(a).getAskPrice() == 0) ? 200 : r.getQuote(a).getAskPrice();
			double currBidPrice = r.getQuote(a).getBidPrice();
			
			sample.currBuyPrice[a] = (float) Math.min(currAskPrice, getPredBidPrice(a, sec));
			sample.nextBuyPrice[a] = (float) getMinAskPrice(a, sec);
			sample.currSellPrice[a] = (float) Math.max(currBidPrice, getPredAskPrice(a, sec));
			sample.nextSellPrice[a] = (float) getMaxBidPrice(a, sec);
		}
		
		return sample;
	}
	
	public void predict(Repository r) {
		Priceline[] samples = r.getXMostRecent(r.getAvailableScenarios().length);
		r.clearScenarios();
		int sec = r.getGameDecisec() * 10;

		for (int i = 0; i < samples.length; i++) {
			for (int a = 16; a < 28; a++) {
				samples[i].currBuyPrice[a] = (float) getPredBidPrice(a, sec);
				samples[i].nextBuyPrice[a] = (float) getMinAskPrice(a, sec);
				samples[i].currSellPrice[a] = (float) getPredAskPrice(a, sec);
				samples[i].nextSellPrice[a] = (float) getMaxBidPrice(a, sec);
			}
			r.addScenario(samples[i]);
			setNext();
		}
	};
}
