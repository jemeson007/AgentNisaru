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
 * FlightPredictorBaysiznSampled
 *
 * Author  : Seong Jae Lee
 * Created : 5 May, 2007
 * 
 */

package edu.brown.cs.modeler;

import java.util.ArrayList;

import props.*;
import edu.brown.cs.tac.*;
import edu.brown.cs.tacds.Priceline;
import edu.brown.cs.tacds.Repository;

public class FlightPredictorBaysianSampled extends FlightPredictor {
	private int[][] priceHistory;	// [auction][time]
	private int[] latestPrice;
	private XDistribution[] dist;	// [auction]
	
    private static final int TIME_WITHIN_1MIN = 0;
    private static final int TIME_AFTER_1MIN  = 1;
    
	public class XDistribution {
		public final int stateNum = 101;
		public double[] point;
		public double[] distribution;
		
		public XDistribution() {
			point			= new double[stateNum];
			distribution 	= new double[stateNum];
			double interval = 40.0/(stateNum-1);	
			
			for (int i=0; i<stateNum; i++) {
				point[i] 		= -10.0 + i * interval;
				distribution[i] = 1.0/stateNum;
			}
		}
		
		public XDistribution (XDistribution x) {
	        distribution = new double[x.distribution.length];
	        point = new double[x.point.length];
	        for (int i = 0; i < x.distribution.length; i++) {
	            distribution[i] = x.distribution[i];
	        }
	        for (int i = 0; i < x.point.length; i++) {
	            point[i] = x.point[i];
	        }
		}
	}
    
	public FlightPredictorBaysianSampled() {
		init();
	}

	@Override
	public void init() {
		priceHistory = new int[8][54];
		latestPrice = new int[8];
		
		dist = new XDistribution[8];
		for (int i = 0; i < 8; i++) {
			dist[i] = new XDistribution();
		}		
	}

	@Override
	public boolean buyNow (int auction, int time) {
		if(time==0) return false;
		if(time==53) return true;
		
		double expected_perturbation = 0;
		
		for (int i = 0; i < dist[auction].stateNum; i++) {
			double x = dist[auction].point[i];
			double p = dist[auction].distribution[i];
			expected_perturbation += p * getExpectedPerturbation(this.latestPrice[auction], x, time);
		}
		
		if (expected_perturbation > 0) return true;
		else return false;
	}
    
	@Override
	protected float getPrice (int auction, int option, int decisec, int curPrice) {
		switch (option) {
		case WITHIN_1MIN:
			return getRandMinPrice (auction, TIME_WITHIN_1MIN, curPrice, decisec);
		case AFTER_1MIN:
			return getRandMinPrice (auction, TIME_AFTER_1MIN, curPrice, decisec);
		case AFTER_MIN:
			return Math.min(
					getRandMinPrice (auction, TIME_WITHIN_1MIN, curPrice, decisec), 
					getRandMinPrice (auction, TIME_AFTER_1MIN, curPrice, decisec));
		case HIST_MIN:
			return getHistoryMinPrice(auction, decisec);
		case TOTAL_MIN:
			return Math.min(
					getPrice(auction, FlightPredictor.AFTER_MIN, decisec, curPrice),
					getPrice(auction, FlightPredictor.HIST_MIN, decisec, curPrice));
		default:
			Misc.myassert(false);	
		}
		return 0;
	}

	@Override
	public void setPrice (int auctionNo, int time, int price) {
		priceHistory[auctionNo][time] = price;
		latestPrice[auctionNo] = price;
		
		if (time == 0) return;
		if (priceHistory[auctionNo][time-1] == 0) return;
			
		updateXDist(auctionNo, time);
	}
	
	private void updateXDist (int auctionNo, int time) {
		for (int i=0; i<dist[auctionNo].stateNum; i++) {
			double x = dist[auctionNo].point[i];
			int diff = priceHistory[auctionNo][time] - priceHistory[auctionNo][time-1];
			dist[auctionNo].distribution[i] *= probabilityY(x, diff, time);
		}
            
		double sum = 0;
		for (int i=0; i<dist[auctionNo].stateNum; i++) 
			sum += dist[auctionNo].distribution[i];
        
		if (sum == 0) {
            dist[auctionNo] = new XDistribution();
		}
		else {
			for (int i = 0; i < dist[auctionNo].stateNum; i++) {
				dist[auctionNo].distribution[i] /= sum;
			}
		}
	}

	// randomly generated minimum price from (currTime+1min) to the end of the game.
	public ArrayList<Integer> getRandHistory (int auctionNo, float currPrice, int currTime) {
        double rand = TacRandom.nextDouble(this);
		double x = 0;
		
		double temp = 0;
		for (int i = 0; i< dist[auctionNo].stateNum; i++) {
			temp += dist[auctionNo].distribution[i];
			if (rand <= temp) {
				x = dist[auctionNo].point[i];
				break;
			}
		}
		
		int simPrice = (int) currPrice;
		ArrayList<Integer> ret = new ArrayList<Integer>();
		
		for (int t = currTime; t < 54; t++) {
			float priceChange = 0;
			
			temp = x(currTime, x);
			if (temp == 0) {
				priceChange = (int) (TacRandom.nextDouble(this) * 20 - 10);
			} else if (temp < 0) {
				priceChange = (int) (TacRandom.nextDouble(this) * -1 * ((int) (temp+1) + 10) + 10);		
			} else if (temp > 0) {
				priceChange = (int) (TacRandom.nextDouble(this) * ((int) temp + 10) - 10);
			}
			
			simPrice += priceChange;
			simPrice = Math.max(150, simPrice);
			simPrice = Math.min(800, simPrice);
			
			ret.add(simPrice);
		}
		
		return ret;
	}

	
	// randomly generated minimum price from (currTime+1min) to the end of the game.
	public float getRandMinPrice (int auctionNo, int option, float currPrice, int currTime) {
        if (option == TIME_AFTER_1MIN && currTime >= 48) return 1000;

        double rand = TacRandom.nextDouble(this);
		double x = 0;
		
		double temp = 0;
		for (int i = 0; i< dist[auctionNo].stateNum; i++) {
			temp += dist[auctionNo].distribution[i];
			if (rand <= temp) {
				x = dist[auctionNo].point[i];
				break;
			}
		}
		
		float minPrice = 800;
		float simPrice = currPrice;
		
		for (int t = currTime; t < 54; t++) {
			float priceChange = 0;
			
			temp = x(currTime, x);
			if (temp == 0) {
				priceChange = (int) (TacRandom.nextDouble(this) * 20 - 10);
			} else if (temp < 0) {
				priceChange = (int) (TacRandom.nextDouble(this) * -1 * ((int) (temp+1) + 10) + 10);		
			} else if (temp > 0) {
				priceChange = (int) (TacRandom.nextDouble(this) * ((int) temp + 10) - 10);
			}
			
			simPrice += priceChange;
			simPrice = Math.max(150, simPrice);
			simPrice = Math.min(800, simPrice);
			
			if (option == TIME_WITHIN_1MIN) {
				if (t < currTime + 6) {
					minPrice = Math.min(simPrice, minPrice);
				}
			} else {
				if (t >= currTime + 6) {
					minPrice = Math.min(simPrice, minPrice);
				}
			}
		}
		
		return minPrice;
	}
    
    public float getHistoryMinPrice (int auctionNo, int time) {
    	float ret = Constants.MAX_PRICE_FOR_FLIGHT;
    	
    	for (int i = 0; i <= time; i++) {
    		if (priceHistory[auctionNo][i] == 0) break;
    		ret = Math.min(ret, priceHistory[auctionNo][i]);
    	}
    	
    	return ret;
    }

    public float getExpectedMinPrice (int auctionNo, int timeOption, int price, int time) {
    	if (time >= 8*6 && timeOption == TIME_AFTER_1MIN)
			return 999;
		
		double ret = 0;
		
		for (int i = 0; i < dist[auctionNo].stateNum; i++) {
			double x = dist[auctionNo].point[i];
			double p = dist[auctionNo].distribution[i];
			double expPrice = price + getExpectedMinPriceChange(price, x, time, timeOption);
			
			ret += expPrice * p;
		}
		
		return (float) ret;
    }
    
	private double getExpectedMinPriceChange (int price, double x, int time, int timeOption) {
		double expectedChange = 0;
		double expectedMinChange = Constants.INFINITY;
		int borderPoint = (time / 6 + 1) * 6;
		
		for (int t = time; t < 54; t++) {
			expectedChange += getExpectedPerturbation(price, x, t);
			
			if (timeOption == TIME_AFTER_1MIN && t <= borderPoint) continue;
			if (timeOption == TIME_WITHIN_1MIN && t > borderPoint) break;
			
			expectedMinChange = Math.min(expectedChange, expectedMinChange);
		}

		return expectedMinChange;
	}

	private double getExpectedPerturbation(int price, double x, int time) {
		double maxChange = 10;
		double minChange = -10;
		
		int change = (int) Math.abs(x(time, x));
		
		if (change > 0) maxChange = change;
		if (change < 0)	minChange = change;
		
		minChange = Math.max(250.0 - price, minChange);
		maxChange = Math.min(600.0 - price, maxChange);
		
		return (maxChange + minChange) / 2.0;
	}
	
	private float probabilityY (double x, int y, double t) {
		if (y > x(t,x) && x(t,x) > 0) return 0;
		if (y < x(t,x) && x(t,x) < 0) return 0;
		if (y > 10 && x(t,x) < 0) return 0;
		
		if (x(t,x) != 0) return 1.0f / (float)( 11.0 + Math.abs((int)x(t,x)) );
		return 1.0f/21.0f;
	}
	
	private double x (double t, double x) { return 10.0+t*(x-10.0)/54.0; }

	@Override
	public FlightPredictor clone() {
		FlightPredictor ret = new FlightPredictorBaysianSampled();
		return ret;
	}
}
