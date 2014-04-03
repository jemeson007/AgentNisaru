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
 * FlightPredictorAttac
 *
 * Author  : Seong Jae Lee
 * Created : 5 May, 2007
 * 
 */

package edu.brown.cs.modeler;

import props.Misc;
import edu.brown.cs.tac.Constants;

public class FlightPredictorAttac extends FlightPredictor {
	private FlightPredictor internal_predictor;
	public float[] predicted_within_minute;
	public float[] predicted_after_minute;
	
	public FlightPredictorAttac(FlightPredictor f) { 
		internal_predictor = f; 
		predicted_within_minute = new float[8];
		predicted_after_minute = new float[8];	}
	
	@Override
	public void init() {
		internal_predictor.init();
		for (int i = 0; i < 8; i++) predicted_within_minute[i] = 0;
		for (int i = 0; i < 8; i++) predicted_after_minute[i] = 0;
	}
	
	@Override
	public void setPrice (int auction, int decisec, int price) { 
		internal_predictor.setPrice(auction, decisec, price);
		
		if (predicted_within_minute[auction] == 0) {
			predicted_within_minute[auction] = 
				internal_predictor.getPrice(auction, WITHIN_1MIN, decisec, price);
			predicted_after_minute[auction] = 
				internal_predictor.getPrice(auction, AFTER_1MIN, decisec, price);
		}
		
		if (decisec % 6 == 0) {
			predicted_within_minute[auction] = 
				internal_predictor.getPrice(auction, WITHIN_1MIN, decisec, price);
			predicted_after_minute[auction] = 
				internal_predictor.getPrice(auction, AFTER_1MIN, decisec, price);
		}
	}
	
	@Override
	protected float getPrice (int auction, int option, int decisec, int curprice) { 
		switch (option) {
		case AFTER_MIN: // from MU agents
			return Math.min(
					predicted_within_minute[auction],
					predicted_after_minute[auction]);
		case WITHIN_1MIN: // from SAA agents
			return Math.min(
					predicted_within_minute[auction],
					predicted_after_minute[auction]);
		case AFTER_1MIN: // from SAA agents
			return Constants.INFINITY;
		default: Misc.myassert(false);
		}
	
		Misc.myassert(false);
		return 0; 
	}
	
	@Override
	// called from Agent
	public boolean buyNow (int auction, int decisec) { 
		return predicted_within_minute[auction] < predicted_after_minute[auction];
	}
	
	@Override
	public FlightPredictor clone() {
		FlightPredictorAttac ret = new FlightPredictorAttac(internal_predictor.clone());
		for (int a = 0; a < 8; a++) {
			ret.predicted_after_minute[a] = predicted_after_minute[a];
			ret.predicted_within_minute[a] = predicted_within_minute[a];
		}
		return ret;
	}
}
