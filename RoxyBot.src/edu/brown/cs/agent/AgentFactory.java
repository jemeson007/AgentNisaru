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
 * AgentFactory
 *
 * Author  : Seong Jae Lee
 * Created : 15 May, 2007
 * 
 */

package edu.brown.cs.agent;

import java.io.FileReader;
import java.io.LineNumberReader;
import java.util.HashMap;

import edu.brown.cs.algorithm.Algorithm;
import edu.brown.cs.algorithm.AverageMU;
import edu.brown.cs.algorithm.BidEvaluator;
import edu.brown.cs.algorithm.SAA;
import edu.brown.cs.algorithm.StraightMU;
import edu.brown.cs.bidder.SAABidder;
import edu.brown.cs.completer.SAACompleter;
import edu.brown.cs.modeler.EventAnalyzer;
import edu.brown.cs.modeler.EventPredictor;
import edu.brown.cs.modeler.FlightPredictorAttac;
import edu.brown.cs.modeler.FlightPredictorBaysianExpected;
import edu.brown.cs.modeler.FlightPredictorBaysianSampled;
import edu.brown.cs.modeler.FlightPredictorConst;
import edu.brown.cs.modeler.HotelPredictorCE;
import edu.brown.cs.modeler.HotelPredictorNormal;
import edu.brown.cs.modeler.Tatonnementer;
import edu.brown.cs.tac.Constants;
import edu.brown.cs.tacds.Repository;
import edu.brown.cs.tacds.RepositoryReal;
import edu.brown.cs.tacds.RepositorySimulator;

import props.Misc;

public class AgentFactory {

	private HashMap<String, String> map;
	
	public AgentFactory () { map = new HashMap<String, String>(); }
	
	public void setMap (HashMap<String, String> m) { map = m; }
	
	public void parse () { parse("agent"); }
	
	public void parse (String url) { try {
		FileReader reader = new FileReader(url);
		LineNumberReader lineReader = new LineNumberReader(reader);
		String line;
		
		while (null != (line = lineReader.readLine())) {
			if (line.length() == 0) continue;
			if (line.startsWith("%")) continue;
			
			Misc.myassert(line.split(" = ").length == 2);
			String name = line.split(" = ")[0];
			String option = line.split(" = ")[1];
			
			if (!map.containsKey(name)) map.put(name, option);
		}
		
		lineReader.close();
		reader.close();
	} catch (Exception e) { e.printStackTrace(); }}
	
	public void create (Agent agent) {
		agent.repository = getRepository();
		agent.algorithm  = getAlgorithm(agent.repository);
		
		String token;
		if ((token = map.get("flight_bidding")) != null) {
			if (token.equals("no_modification"))agent.setFlightBidding(Agent.FLIGHT_NO_MODIFICATION);
			else if (token.equals("attac"))     agent.setFlightBidding(Agent.FLIGHT_ATTAC);
			else if (token.equals("best_time")) agent.setFlightBidding(Agent.FLIGHT_BESTTIME);
			else if (token.equals("last_time")) agent.setFlightBidding(Agent.FLIGHT_LASTTIME);
			else if (token.equals("first_time")) agent.setFlightBidding(Agent.FLIGHT_FIRSTTIME);
		} else agent.setFlightBidding(Agent.FLIGHT_NO_MODIFICATION);
	}
	
	private Repository getRepository () {
		Repository repository = null;
		String token;
		
		if ((token = map.get("game_type")) != null) {
			if (token.equals("simulation")) repository = new RepositorySimulator();
			else if (token.equals("real")) repository = new RepositoryReal();
			else Misc.error("game_type : " + token);
		} else repository = new RepositoryReal();
		
		if ((token = map.get("entertainment")) != null) {
			if (token.equals("exist")) Constants.EVENTS_ON= true;
			else if (token.equals("none")) Constants.EVENTS_ON= false;
			else Misc.error("entertainment : " + token);
		} else Constants.EVENTS_ON = true;
		
		if ((token = map.get("entertainment_prediction")) != null) {
			if (token.equals("constant")) repository.event = new EventPredictor();
			else if (token.equals("history")) repository.event = new EventAnalyzer();
			else Misc.error("entertainment_prediction : " + token);
		} else repository.event = new EventAnalyzer();
		
		if ((token = map.get("flight_prediction")) != null) {
			if (token.equals("expected")) repository.flight = new FlightPredictorBaysianExpected();
			else if (token.equals("sampled")) repository.flight = new FlightPredictorBaysianSampled();
			else if (token.equals("constant")) {
				String token2;
				Misc.myassert((token2 = map.get("flight_price")) != null);
				repository.flight = new FlightPredictorConst(Integer.valueOf(token2));
				FlightPredictorConst flight = (FlightPredictorConst) repository.flight;
				
				String token3;
				if ((token3 = map.get("flight_bidding")) != null) {
					if (token3.equals("no_modification"))flight.setBiddingOption(FlightPredictorConst.BID_NOMODIFICATION);
					else if (token3.equals("last_time")) flight.setBiddingOption(FlightPredictorConst.BID_LASTTIME);
					else if (token3.equals("first_time")) flight.setBiddingOption(FlightPredictorConst.BID_FIRSTTIME);
					else Misc.myassert(false);
				} else flight.setBiddingOption(FlightPredictorConst.BID_NOMODIFICATION);
				
				repository.flight = flight;
				
			} else Misc.error("flight_prediction : " + token);
		} else repository.flight = new FlightPredictorBaysianExpected();
		
		if ((token = map.get("flight_bidding")) != null) {
			if (token.equals("attac")) repository.flight = new FlightPredictorAttac(repository.flight);
		}

		if ((token = map.get("closing_type")) != null) {
			if (token.equals("ce")) repository.hotel = new HotelPredictorCE(repository);
			else if (token.equals("normal")) repository.hotel = new HotelPredictorNormal(repository);
			else Misc.error("closing_type : " + token);
		} else repository.hotel = new HotelPredictorCE(repository);
		
		if (repository.hotel instanceof HotelPredictorCE) {
			Tatonnementer t = ((HotelPredictorCE) repository.hotel).getTatonnementer();
			
			if ((token = map.get("agent_number")) != null) {
				t.setAgent(Integer.valueOf(token));
			}
			
			if ((token = map.get("ce_update")) != null) {
				if (token.equals("simultaneousAA")) t.setPerspective(Tatonnementer.UPDATE_SAA);
				else if (token.equals("tatonnement")) t.setPerspective(Tatonnementer.UPDATE_TAT);
				else Misc.error("ce_update : " + token);
			}
			
			if ((token = map.get("ce_perspective")) != null) {
				if (token.equals("expected")) t.setPerspective(Tatonnementer.PERSPECTIVE_EXPECTED_CLIENT);
				else if (token.equals("sampled")) t.setPerspective(Tatonnementer.PERSPECTIVE_SAMPLED_CLIENT);
				else if (token.equals("distribution")) t.setPerspective(Tatonnementer.PERSPECTIVE_DISTRIBUTION);
				else if (token.equals("Walverine")) t.setPerspective(Tatonnementer.PERSPECTIVE_WALVERINE);
				else Misc.error("ce_perspective : " + token);
			}

			if ((token = map.get("ce_max_iteration")) != null) {
				t.setMaxIteration(Integer.valueOf(token));
			}

			if ((token = map.get("ce_alpha")) != null) {
				t.setAlpha(Double.valueOf(token));
			}
			
			if ((token = map.get("random_agent")) != null && Boolean.valueOf(token)) {
				((HotelPredictorCE) repository.hotel).getTatonnementer().setPerspective(Tatonnementer.PERSPECTIVE_BINOMIAL_AGENT);
			}
		}
		
		if (repository.hotel instanceof HotelPredictorNormal) {
			double sigma = 0.5;
			double shift = 0;
			double interval = 0;
			double[] mean = new double[8];
			
			if ((token = map.get("normal_closing_sigma")) != null) sigma = Double.valueOf(token);
			if ((token = map.get("normal_closing_shift")) != null) shift = Double.valueOf(token);
			if ((token = map.get("normal_closing_interval")) != null) interval = Double.valueOf(token);
		
			((HotelPredictorNormal) repository.hotel).setClosingOption(interval, sigma, shift);
			
			if ((token = map.get("normal_prediction_sigma")) != null) sigma = Double.valueOf(token);
			if ((token = map.get("normal_prediction_shift")) != null) shift = Double.valueOf(token);
			if ((token = map.get("normal_prediction_interval")) != null) interval = Double.valueOf(token);

			((HotelPredictorNormal) repository.hotel).setPredictionOption(interval, sigma, shift);
			
			if ((token = map.get("normal_mean")) != null) {
				String[] tokens = token.split(", ");
				Misc.myassert(tokens.length == 8);
				for (int i = 0; i < 8; i++) mean[i] = Double.valueOf(tokens[i]);
			}
			
			((HotelPredictorNormal) repository.hotel).setMean(mean);			
		}
		
		Misc.myassert(repository.hotel != null);
		Misc.myassert(repository.event != null);
		Misc.myassert(repository.flight != null);
		
		Misc.println(getClass().getSimpleName() + " " +
				": " + repository.flight.getClass().getSimpleName() +
				", " + repository.hotel.getClass().getSimpleName() +
				", " + repository.event.getClass().getSimpleName());
		
		return repository;
	}
	
	private Algorithm getAlgorithm (Repository rep) {
		Algorithm algorithm = null;
		String token;
		
		Misc.myassert(map.containsKey("algorithm"));
		if (map.get("algorithm").equals("SAA")) {
			SAABidder b = new SAABidder(rep);
			SAACompleter c = new SAACompleter(rep);
			
			algorithm = new SAA(50);
			if ((token = map.get("algorithm_numScenario")) != null) {
				algorithm = new SAA(Integer.valueOf(token));
				c.setNumScenario(Integer.valueOf(token));
			}
			
			if ((token = map.get("algorithm_saa_extreme_max")) != null) {
				rep.hotel.setExtremeMax(Boolean.valueOf(token));
			}
			
			if ((token = map.get("algorithm_saa_extreme_min")) != null) {
				rep.hotel.setExtremeMin(Boolean.valueOf(token));
			}
			
			if ((token = map.get("algorithm_saa_type")) != null) {
				if (token.equals("bottom")) c.setBidPolicy(SAACompleter.BID_LOWEST);
				else if (token.equals("top")) c.setBidPolicy(SAACompleter.BID_MAXMV);
				else if (token.equals("top_util")) c.setBidPolicy(SAACompleter.BID_MAXUTIL);
				else Misc.error("algorithm_saa_type : " + token);
			} else c.setBidPolicy(SAACompleter.BID_LOWEST);
			
			if ((token = map.get("game_type")) != null) {
				if (token.equals("simulation")) c.setTAC(false); 
				else if (token.equals("real")) c.setTAC(true);
				else Misc.error("game_type : " + token);
			} else c.setTAC(true);
			
			b.setCompleter(c);
			((SAA)algorithm).setBidder(b);
		}
		
		if (map.get("algorithm").equals("SMU")) {
			algorithm = new StraightMU(40);
			if ((token = map.get("algorithm_numScenario")) != null) {
				algorithm = new StraightMU(Integer.valueOf(token));
			}
			
			if ((token = map.get("algorithm_mu_useOldMU")) != null) {
				if (!Boolean.valueOf(token)) ((StraightMU)algorithm).useNewMU();
				else ((StraightMU)algorithm).useOldMU();
			} else ((StraightMU)algorithm).useOldMU();
		}
		
		if (map.get("algorithm").equals("AMU")) {
			algorithm = new AverageMU(15, 1, 0);
			if ((token = map.get("algorithm_numScenario")) != null) {
				algorithm = new AverageMU(Integer.valueOf(token));
			}
			
			if ((token = map.get("algorithm_mu_useOldMU")) != null) {
				if (!Boolean.valueOf(token)) ((AverageMU)algorithm).useNewMU();
				else ((AverageMU)algorithm).useOldMU();
			} else ((AverageMU)algorithm).useOldMU();
		}
		
		if (map.get("algorithm").equals("TMU")) {
			algorithm = new BidEvaluator(40, 1, 0);
			if ((token = map.get("algorithm_numScenario")) != null) {
				algorithm = new BidEvaluator(Integer.valueOf(token), 1, 0);				
			}
			
			if ((token = map.get("algorithm_tmu_extreme")) != null) 
				((BidEvaluator) algorithm).setExtreme(Boolean.valueOf(token));
			else 
				((BidEvaluator) algorithm).setExtreme(false);
			
			if ((token = map.get("algorithm_mu_useOldMU")) != null) {
				if (!Boolean.valueOf(token)) ((BidEvaluator)algorithm).useNewMU();
				else ((BidEvaluator)algorithm).useOldMU();
			} else ((BidEvaluator)algorithm).useOldMU();
		}

		if (map.get("algorithm").equals("BE")) {
			int p = 25; int e = 15;
			
			if ((token = map.get("algorithm_numPolicy")) != null) {
				p = Integer.valueOf(token);
			}
			
			if ((token = map.get("algorithm_numEvaluation")) != null) {
				e = Integer.valueOf(token);
			}
			algorithm = new BidEvaluator(1, p, e);
			
			if ((token = map.get("algorithm_be_extreme")) != null) 
				((BidEvaluator) algorithm).setExtreme(Boolean.valueOf(token));
			else 
				((BidEvaluator) algorithm).setExtreme(false);
			
			if ((token = map.get("algorithm_mu_useOldMU")) != null) {
				if (!Boolean.valueOf(token)) ((BidEvaluator)algorithm).useNewMU();
				else ((BidEvaluator)algorithm).useOldMU();
			} else ((BidEvaluator)algorithm).useOldMU();
		}
		
		return algorithm;
	}
	
	public static void main(String[] args) {
		(new AgentFactory()).parse();
	}

}
