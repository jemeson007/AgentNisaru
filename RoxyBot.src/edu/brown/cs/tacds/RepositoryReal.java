/**
 * tacds
 *
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
 * Repository
 *
 * Author  : Seong Jae Lee
 * Created : 2 June, 2005
 * Description: 
 *    
 * This class contains all the predictors and game informations.
 * 
 */

package edu.brown.cs.tacds;

import edu.brown.cs.tac.Constants;

public class RepositoryReal extends Repository {
	/*
	//only SAA uses nextBuyPrices
	//MU's use this function with type set to globalbestprice
	//SAA uses this function with type set to nowlaterminprice
	public Priceline updateFlightAndEvent (Priceline p, int currOption, int nextOption) {
		Priceline ret = new Priceline(p);
		
		for (int a = 0; a < 8; a++) {
			float currPrice = flight.getPrice(a, currOption, this.getGameDecisec(), (int)this.getQuote(a).getAskPrice());
			float nextPrice = flight.getPrice(a, nextOption, this.getGameDecisec(), (int)this.getQuote(a).getAskPrice());
			
			ret.currBuyPrice[a] = currPrice;
			ret.nextBuyPrice[a] = nextPrice;
		}
		
		// At any point, tatonnementer doesn't use next event price information.
		// SAACompleter updates future's event price itself, so we don't have to
		// consider future's event price here. sjlee.
		// Therefore, the entertainment price updated here is only for MU agents. sjlee.
		for (int a = 16; a < 28; a++) {
			if (!Constants.EVENTS_ON) {
				ret.currBuyPrice[a] = 200;
				ret.currSellPrice[a] = 0;
				continue;
			}
			
			int currSecond = this.getGameSec();
			double currAskPrice = (getQuote(a).getAskPrice() == 0) ? 200 : getQuote(a).getAskPrice();
			double predAskPrice = event.getExpAskPrice(a, currSecond);
			double currBidPrice = getQuote(a).getBidPrice();
			double predBidPrice = event.getExpBidPrice(a, currSecond);
			
			// Althogh our prediction is out of current bid-ask price range, it expects
			// it can get the good someday. It can be a problem though, since MU
			// agents will expect buying lots of amount of entertainments with 
			// best expected prices, resulting predilection of long term trip assignments.
			ret.currBuyPrice[a] = (float) Math.min(currAskPrice, predAskPrice);
			ret.currSellPrice[a] = (float) Math.max(currBidPrice, predBidPrice);
			// ret.currBuyPrice[auc] = (float) Math.max(currBidPrice, Math.min(currAskPrice, predAskPrice));
			// ret.currSellPrice[auc] = (float) Math.min(currAskPrice, Math.max(currBidPrice, predBidPrice));
		}
		
		return ret;
	}
	*/
}
