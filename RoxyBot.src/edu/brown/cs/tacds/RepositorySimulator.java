/*
 * Created on Jun 2, 2005
 */
package edu.brown.cs.tacds;

import props.Misc;
import edu.brown.cs.tac.Constants;

public class RepositorySimulator extends Repository {

	public void setStartTime (long t) { Misc.myassert(false); }
	public void setGameTime (int t) { time = t; }
	public int getGameSec() { return time; }

	/*
	public Priceline updateFlightAndEvent (Priceline p, int currOption, int nextOption) {
		Priceline ret = new Priceline(p);
		
		for (int a = 0; a < 8; a++) {
			float currPrice = flight.getPrice(a, currOption, this.getGameDecisec(), (int)this.getQuote(a).getAskPrice());
			float nextPrice = flight.getPrice(a, nextOption, this.getGameDecisec(), (int)this.getQuote(a).getAskPrice());
			
			ret.currBuyPrice[a] = currPrice;
			ret.nextBuyPrice[a] = nextPrice;
		}

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
			
			ret.currBuyPrice[a] = (float) Math.min(currAskPrice, predAskPrice);
			ret.currSellPrice[a] = (float) Math.max(currBidPrice, predBidPrice);
		}
		
		return ret;
	}
	*/
}
