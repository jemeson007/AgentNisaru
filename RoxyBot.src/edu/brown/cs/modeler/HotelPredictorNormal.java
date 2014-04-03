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
 * FlightPredictorNormal
 *
 * Author  : Seong Jae Lee
 * Created : 5 May, 2007
 * 
 * There are various options, but I only implemented NORMAL prediction.
 * 
 */

package edu.brown.cs.modeler;

import java.util.ArrayList;

import props.Misc;

import edu.brown.cs.tac.Constants;
import edu.brown.cs.tac.TacRandom;
import edu.brown.cs.tacds.Priceline;
import edu.brown.cs.tacds.Repository;

public class HotelPredictorNormal extends HotelPredictorCE {
	public static final int NORMAL=0;
	public static final int UNIFORM=1;
	public static final int BIMODAL=2;
	public static final int GEOMETRIC_HIGH=3;
	public static final int GEOMETRIC_LOW=4;
	public static final int POISSON=5;
	protected int distribution_option = NORMAL;
	
	protected DistributionOption closing_option;
	protected DistributionOption prediction_option;
	protected DistributionOption option;
	
	public class DistributionOption {
		double interval;
		double sigma;
		double shift;
		public void setAll(double i, double s, double sh) {interval=i; sigma=s; shift=sh;}
	}
	
	protected double[] MEAN = {10,50,50,10,40,110,110,40};
	
	public void setMean (double[] mean) { 
		Misc.myassert(mean.length == 8);
		for (int i = 0; i < 8; i++) {
			Misc.myassert(mean[i] >= 0);
			MEAN[i] = mean[i];
		}
	}
	
	public void setDistributionOption (int o) { distribution_option = o; }
	
	public void setClosingOption (double interval, double sigma, double shift) { 
		closing_option.setAll(interval, sigma, shift); 
	}
	
	public void setPredictionOption (double interval, double sigma, double shift) {
		prediction_option.setAll(interval, sigma, shift);
		option = prediction_option;
	}
	
	public HotelPredictorNormal (Repository r) {
		super(r);
		closing_option = new DistributionOption();
		prediction_option = new DistributionOption();
		option = new DistributionOption();
	}
	
	public void addExtremeScenarios () {
		ArrayList<Priceline> extremes = new ArrayList<Priceline>();
		
		for (int auction_id = 8; auction_id < 16; auction_id++) {
			double max = Math.max(
					MEAN[auction_id-8] * ( 1 + closing_option.interval + closing_option.shift),
					MEAN[auction_id-8] * ( 1 + prediction_option.interval + prediction_option.shift));
			double min = Math.min(
					MEAN[auction_id-8] * ( 1 - closing_option.interval + closing_option.shift),
					MEAN[auction_id-8] * ( 1 - prediction_option.interval + prediction_option.shift));
			min = Math.max(0.01, min);
			
			if (extremeMax && repository.getAvailableScenarios().length >= 16) {
				Priceline p = predict();
				for (int i = 8; i < 16; i++) p.currBuyPrice[i] = (float) MEAN[i-8];
				p.currBuyPrice[auction_id] = (float) max;
				p.probability = 0.1;
				Misc.println(getClass().getSimpleName() + ".addExtremeScenarios " +
						": max " + p.getHotelString());
				extremes.add(p);
			}

			if (extremeMin) {
				Priceline p = predict();
				for (int i = 8; i < 16; i++) p.currBuyPrice[i] = (float) MEAN[i-8];
				p.currBuyPrice[auction_id] = (float) min;
				p.probability = 0.1;
				Misc.println(getClass().getSimpleName() + ".addExtremeScenarios " +
						": min " + p.getHotelString());
				extremes.add(p);
			}
		}
		
		repository.removeXMostRecent(extremes.size());
		
		for (Priceline p : extremes) {
			repository.addScenario(p);
		}
	}
	
	public void changeOptionToClosing() { option = closing_option; }
	public void changeOptionToPrediction() { option = prediction_option; }
	
	public Priceline predict() {
		Priceline ret = new Priceline();
		ret = repository.flight.predict(repository, FlightPredictor.PREDICT_TAT, ret);
		ret = repository.event.predict(repository, ret);
   		/*ret = repository.updateFlightAndEvent(
   				ret, 
   				FlightPredictor.AFTER_MIN, 
   				FlightPredictor.AFTER_1MIN);
		*/
		
		for(int auction_id=8; auction_id<16; auction_id++) {
			if (repository.isAuctionClosed(auction_id)) {
				ret.currBuyPrice[auction_id] = Constants.INFINITY;
				continue;
			}
			
			double mean = MEAN[auction_id-8];
			double res = 0;
			double interval = mean * option.interval;
			double sigma = option.sigma;
			double shift = option.shift;
			if (shift < 0) Misc.myassert(interval - shift >= mean);
			
			switch(distribution_option) {
			case NORMAL: // [0, 2*mean] in N(mean, sigma)
				//res = Math.max(0, (mean + shift + rand.nextGaussian() * sigma));
				do {
					res = (mean + shift + TacRandom.nextGaussian(this) * sigma);
				} while (res <= 0);
				break;
				
				/*
				do {
					res = (mean + rand.nextGaussian() * sigma);
				} while (Math.abs(res - mean) >= interval);
				res += shift;
				break;
				*/
				
			case UNIFORM: // [-interval/2+mean, interval/2+mean]
				Misc.myassert(false);
				Misc.myassert(option.interval < 2);
				res = (float) (mean - interval/2.0 + TacRandom.nextDouble(this) * interval);
				break;
				
			case POISSON:
				Misc.myassert(false);
				break;
				
			case BIMODAL: // http://en.wikipedia.org/wiki/Bimodal_distribution
				if (TacRandom.nextBoolean(this)) {
					res = mean - interval;
				} else {
					res = mean + interval;
				}
				
				/*
				do {
					res = mean + rand.nextGaussian() * sigma;
				} while (Math.abs(res - mean) >= interval);
				
				res = (res > mean) ? res - interval : res + interval;
				res += shift;
				*/
				
				break;
				
			case GEOMETRIC_HIGH:
				Misc.myassert(false);
				break;
				
			case GEOMETRIC_LOW:
				Misc.myassert(false);
				break;
			}
			
			ret.currBuyPrice[auction_id] = (float) res;
		}
		
		ret.printHotel();
		
		return ret;
	}
}
