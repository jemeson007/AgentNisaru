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
 * SimulatorFactory
 *
 * Author  : Seong Jae Lee
 * Created : 15 May, 2007
 * Description:
 * 
 */

package edu.brown.cs.simulator;

import java.io.FileReader;
import java.io.LineNumberReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Random;

import edu.brown.cs.agent.SimulatedAgent;
import edu.brown.cs.tac.TacRandom;
import edu.brown.cs.tacds.Preference;
import edu.brown.cs.tacds.TravelPackage;

import props.Misc;

public class SimulatorFactory {
	private HashMap<String, String> map;
	private SimulationSetting setting;
	
	private class SimulationSetting {
		public int agent_number = 8;
		public int game_number = 1;
		public boolean binomial = false;
		public boolean random_agent = false; // only for online
		public String simulation_type = "decision_theoretic";
		public String auction_type = "unknown_sequential";
		public List<String> agent_setting;
		public String flight_bidding = "no_modification";
	}
	
	public SimulatorFactory () {
		map = new HashMap<String, String>();
		setting = new SimulationSetting();
		Misc.init();
	}
	
	public void parse (String url) { try {
		FileReader reader = new FileReader(url);
		LineNumberReader lineReader = new LineNumberReader(reader);
		String line, token;
		
		while (null != (line = lineReader.readLine())) {
			if (line.length() == 0) continue;
			if (line.startsWith("%")) continue;
			Misc.myassert(line.split(" = ").length == 2);
			map.put(line.split(" = ")[0], line.split(" = ")[1]);
		}
		
		lineReader.close();
		reader.close();
		
		if ((token = map.get("agent_number")) != null)
			setting.agent_number = Integer.valueOf(token);
		
		if ((token = map.get("game_number")) != null)
			setting.game_number = Integer.valueOf(token);

		if ((token = map.get("simulation_type")) != null) {
			setting.simulation_type = token;
			Misc.myassert(token.equals("decision_theoretic") || token.equals("game_theoretic"));
			}
		
		if ((token = map.get("auction_type")) != null) {
			setting.auction_type = token;
			Misc.myassert(token.equals("unknown_sequential") || 
					token.equals("known_sequential") ||
					token.equals("simultaneous"));
		}
		
		if ((token = map.get("random_agent")) != null) {
			setting.random_agent = Boolean.valueOf(token);
		}
		
		if ((token = map.get("flight_bidding")) != null) {
			setting.flight_bidding  = token;
			Misc.myassert(token.equals("no_modification") || 
					token.equals("best_time") ||
					token.equals("last_time") ||
					token.equals("first_time") ||
					token.equals("attac"));
		}
		
		setting.agent_setting = new ArrayList<String>();
		for (int i = 0; i <= 100; i++) {
			if (!map.containsKey("agent" + i)) continue;
			token = map.get("agent" + i);
			setting.agent_setting.add(token);
		}
	} catch (Exception e) { e.printStackTrace(); }}
	
	public GameEngine create () {
		String token;
		
		if ((token = map.get("utility")) != null)
			TravelPackage.BASIC_UTILITY = Integer.valueOf(token);
		
		if ((token = map.get("hotel_bonus_interval")) != null)
			Preference.HOTEL_INTERVAL = Integer.valueOf(token);
		
		SimulatedAgent[] agents = new SimulatedAgent[setting.agent_number];
		for (int i = 0; i < setting.agent_number; i++) {
			HashMap<String, String> copy = new HashMap<String, String>();
			copy.putAll(map);

			if (setting.random_agent) {
				String file = setting.agent_setting.get(TacRandom.nextInt(this, setting.agent_setting.size()));
				agents[i] = new SimulatedAgent(i, "config/agent/"+file, copy);
			} else {
				Misc.myassert(setting.agent_number == setting.agent_setting.size());
				String file = setting.agent_setting.get(i);
				agents[i] = new SimulatedAgent(i, "config/agent/"+file, copy);
			}
			
			/* TODO
			if (setting.auction_type.equals("known sequential") && 
					agents[i].getAlgorithm() instanceof SAA) {
				SAACompleterSequential completer = new SAACompleterSequential(agents[i].getRepository());
				completer.setNumScenario(agents[i].getAlgorithm().getNumScenarios());
				((SAA)agents[i].getAlgorithm()).getBidder().setCompleter(completer);
			}
			*/
		}
		
		GameEngine engine = null;
		if (setting.simulation_type.equals("game_theoretic") && 
				setting.auction_type.equals("unknown_sequential"))
			engine = new GameEngine(agents, setting.game_number);
		else if (setting.simulation_type.equals("decision_theoretic") && 
				setting.auction_type.equals("unknown_sequential"))
			engine = new GameEngineOffline(agents, setting.game_number);
		else if (setting.simulation_type.equals("game_theoretic") && 
				setting.auction_type.equals("simultaneous"))
			engine = new GameEngineSimultaneous(agents, setting.game_number);
		else if (setting.simulation_type.equals("decision_theoretic") && 
				setting.auction_type.equals("simultaneous"))
			engine = new GameEngineOfflineSimultaneous(agents, setting.game_number);
		else if (setting.simulation_type.equals("game_theoretic") && 
				setting.auction_type.equals("known_sequential"))
			engine = new GameEngine(agents, setting.game_number);
		else if (setting.simulation_type.equals("decision_theoretic") && 
				setting.auction_type.equals("known_sequential"))
			engine = new GameEngineOffline(agents, setting.game_number);
		else Misc.error("close setting is undefined : " + setting.auction_type);
		
		FlightEngine flight_engine = null;
		if ((token = map.get("flight_price")) != null)
			flight_engine = new FlightEngineConst(agents, Integer.valueOf(token));
		else
			flight_engine = new FlightEngine(agents);
		
		HotelEngine hotel_engine = null;
		if (setting.simulation_type.equals("game_theoretic") && 
				setting.auction_type.equals("unknown_sequential"))
			hotel_engine = new HotelEngine(agents);
		else if (setting.simulation_type.equals("decision_theoretic") && 
				setting.auction_type.equals("unknown_sequential"))
			hotel_engine = new HotelEngineOfflineSequential(agents);
		else if (setting.simulation_type.equals("game_theoretic") && 
				setting.auction_type.equals("simultaneous"))
			hotel_engine = new HotelEngineOnlineSimultaneous(agents);
		else if (setting.simulation_type.equals("decision_theoretic") &&
				setting.auction_type.equals("simultaneous"))
			hotel_engine = new HotelEngineOfflineSimultaneous(agents);
		else if (setting.simulation_type.equals("game_theoretic") &&
				setting.auction_type.equals("known_sequential"))
			hotel_engine = new HotelEngineOnlineKnownSequential(agents);
		else if (setting.simulation_type.equals("decision_theoretic") &&
				setting.auction_type.equals("known_sequential"))
			hotel_engine = new HotelEngineOfflineKnownSequential(agents);
		else Misc.error("close setting is undefined : " + setting.auction_type);
		
		engine.flight_engine = flight_engine;
		engine.hotel_engine = hotel_engine;
		
		return engine;
	}
	
	public static void main (String[] args) {
		SimulatorFactory f = new SimulatorFactory();
		
		if (args.length == 0) f.parse("./config/simset/default");
		else f.parse(args[0]);
		
		if (!f.setting.random_agent) {
			GameEngine simulator = f.create();
			simulator.run();
		} else {
			Misc.println("random");
			Random rand = new Random();
			
			int gameno = f.setting.game_number;
			int agentno = f.setting.agent_number;
			
			for (int g = 0; g < gameno; g++) {
				f.setting.agent_number = 0;
				while (f.setting.agent_number <= 1) {
					for (int i = 0; i < agentno; i++) {
						if (rand.nextBoolean()) {
							f.setting.agent_number++;
						}
					}
				}
				
				f.setting.game_number = 1;
				GameEngine simulator = f.create();
				simulator.setGameBgn(g);
				simulator.run();
			}
		}
	}
}
