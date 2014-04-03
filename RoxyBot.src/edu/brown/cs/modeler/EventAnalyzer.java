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
 * EventAnalyzer
 *
 * Author  : Seong Jae Lee
 * Created : 5 May, 2007
 * 
 * This is a subclass of EventPredictor. It predicts the prices of entertainment
 * tickets stochastically from recent 40 games. The default 40 games are from
 * TAC 2006 final. At the end of the game, downloadFromHost method is called,
 * and then readLog method is called to read the game history.
 * 
 * wget, gzip, mv and rm methods are needed.
 * 
 * Each time you make a prediction, call setNext method.
 * 
 */

package edu.brown.cs.modeler;

import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.LineNumberReader;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Random;

import props.Misc;
import edu.brown.cs.tac.Constants;

public class EventAnalyzer extends EventPredictor {
	public static final DecimalFormat formatter = new DecimalFormat("000.00");
	public static final String defaultDirectory = "./history/default/";
	public static final int numHistory = 40;
	
	private ArrayList<double[][]> trsHistory;
	private ArrayList<double[][]> askHistory;
	private ArrayList<double[][]> bidHistory;
	private int historyIndex;
	
	public void downloadFromHost (final String host, final int gameID) {
		try {
			Runtime rt = Runtime.getRuntime();
			Process process = null;
			String exec = "";
			int exitVal;
			
			// example : wget http://tac1.sics.se:8080/history/423/applet.log.gz;
			exec = "wget http://" + host + ":8080/history/" + gameID + "/applet.log.gz";
			process = rt.exec(exec);
			exitVal = process.waitFor();
			Misc.println("EventAnalyzer.downloadFromHost : " + exec);
			Misc.println("EventAnalyzer.downloadFromHost : wget, exitVal " + exitVal);
			
			// example : gzip -d applet.log.gz;
			exec = "gzip -d applet.log.gz";
			process = rt.exec(exec);
			exitVal = process.waitFor();
			Misc.println("EventAnalyzer.downloadFromHost : gzip, exitVal " + exitVal);
			
			// example : mv applet.log ./history/applet423.log;
			exec = "mv applet.log ./history/applet" + gameID + ".log -f";
			process = rt.exec(exec);
			exitVal = process.waitFor();
			Misc.println("EventAnalyzer.downloadFromHost : change name, exitVal " + exitVal);
			
			// example : rm ./history/applet23.log;
			exec = "rm ./history/applet" + (gameID - numHistory) + ".log -rf";
			process = rt.exec(exec);
			exitVal = process.waitFor();
			Misc.println("EventAnalyzer.downloadFromHost : remove old, exitVal " + exitVal);
			
		} catch (Throwable t) {
			t.printStackTrace();
		}
	}
	
	public void writeDefaultToFile() {
		Misc.println("EventAnalyzer.write");
		
		String transaction = "";
		String ask = "";
		String bid = "";
		for (int i = 0; i < trsHistory.size(); i++) {
			for (int a = 0; a < 12; a++) {
				for (int t = 0; t < 18; t++) {
					transaction += formatter.format(trsHistory.get(i)[a][t]) + " ";
					ask += formatter.format(askHistory.get(i)[a][t]) + " ";
					bid += formatter.format(bidHistory.get(i)[a][t]) + " ";
				}
				transaction += "\n";
				ask += "\n";
				bid += "\n";
			}
		}
		Misc.println("EventAnalyzer.write : built strings");

		BufferedWriter out;
		
		try {
			String filename;
			filename = defaultDirectory + "event.trs";
			out = new BufferedWriter(new FileWriter(filename));
			out.write(transaction);
			out.close();
			Misc.println("EventAnalyzer.write : wrote trs");
			
			filename = defaultDirectory + "event.bid";
			out = new BufferedWriter(new FileWriter(filename));
			out.write(bid);
			out.close();
			Misc.println("EventAnalyzer.write : wrote bid");

			filename = defaultDirectory + "event.ask";
			out = new BufferedWriter(new FileWriter(filename));
			out.write(ask);
			out.close();
			Misc.println("EventAnalyzer.write : wrote ask");

		} catch (IOException e) {
			e.printStackTrace();
		}
		
	}
	
	
	/**
 	* Read a "./history/applet'gameID'.log" file and put it into our history lists.
 	* If there are more than 'numHistory' histories in our lists, we remove the oldest one.
 	* Note that you should have a log file before you call this method.
 	* To download the log file, call 'downloadFromHost' method.
 	* Also note that this method uses parser, which takes a lot of time,
 	* so you should not read several log files at one time during the game.
 	* 
 	* @param gameID
 	*/
	public void readLog (final int gameID) {
		Misc.println("EventAnalyzer.readLog");
		String filename = "./history/applet" + gameID + ".log";
		
		try {
			Parser p = new Parser();
			p.parse(filename);
			
			trsHistory.add(p.eventTrsHistory);
			askHistory.add(p.eventAskHistory);
			bidHistory.add(p.eventBidHistory);
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		if (trsHistory.size() > numHistory) {
			Misc.println("EventAnalyzer.readLog : history size " + trsHistory.size() + ">" + numHistory + ", remove the oldest");
			trsHistory.remove(0);
			askHistory.remove(0);
			bidHistory.remove(0);
		}
	}
	
	public void readDefaultFromLog() {
		Misc.println("EventAnalyzer : readDefault");
		
		try {
			for (int i = 1; i <= numHistory; i++) {
				System.out.println("readDefault : " + i);

				Parser p = new Parser();
				p.parse(defaultDirectory + "applet" + i + ".log");
			
				trsHistory.add(p.eventTrsHistory);
				askHistory.add(p.eventAskHistory);
				bidHistory.add(p.eventBidHistory);
				Misc.println(p.eventAskHistory[5][5]);
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		Misc.myassert(trsHistory.size() == numHistory);
	}
	
	public void readDefaultFromFile() {
		try {
		String filename = defaultDirectory + "event.trs";
		FileReader reader = new FileReader(filename);
		LineNumberReader lineReader = new LineNumberReader(reader);
		String line = "";

		while (true) {
			double[][] transaction = new double[12][18];
			
			for (int a = 0; a < 12; a++) {
				line = lineReader.readLine();
				if (line == null) break;
				if (line.length() == 0) break; // The last line is empty.
				
				String[] frag = line.split(" ");
				for (int t = 0; t < 18; t++) {
					transaction[a][t] = Double.valueOf(frag[t]);
				}
			}
			
			if (line == null) break;
			if (line.length() == 0) break; // The last line is empty.
			trsHistory.add(transaction);
		}
		
		Misc.myassert(trsHistory.size() == numHistory);
		
		filename = defaultDirectory + "event.ask";
		reader = new FileReader(filename);
		lineReader = new LineNumberReader(reader);
		line = "";

		while (true) {
			double[][] ask = new double[12][18];
			
			for (int a = 0; a < 12; a++) {
				line = lineReader.readLine();
				if (line == null) break;
				if (line.length() == 0) break; // The last line is empty.

				String[] frag = line.split(" ");
				for (int t = 0; t < 18; t++) {
					ask[a][t] = Double.valueOf(frag[t]);
				}
			}
			
			if (line == null) break;
			if (line.length() == 0) break; // The last line is empty.
			askHistory.add(ask);
		}

		filename = defaultDirectory + "event.bid";
		reader = new FileReader(filename);
		lineReader = new LineNumberReader(reader);
		line = "";

		while (true) {
			double[][] bid = new double[12][18];
			
			for (int a = 0; a < 12; a++) {
				line = lineReader.readLine();
				if (line == null) break;
				if (line.length() == 0) break; // The last line is empty.

				String[] frag = line.split(" ");
				for (int t = 0; t < 18; t++) {
					bid[a][t] = Double.valueOf(frag[t]);
				}
			}
			
			if (line == null) break;
			if (line.length() == 0) break; // The last line is empty.
			bidHistory.add(bid);
		}
		
		Misc.myassert(bidHistory.size() == askHistory.size());
		Misc.myassert(trsHistory.size() == askHistory.size());
	} catch (Exception e) { e.printStackTrace(); } }
	
	public EventAnalyzer() {
		trsHistory = new ArrayList<double[][]>();
		askHistory = new ArrayList<double[][]>();
		bidHistory = new ArrayList<double[][]>();
		
		this.readDefaultFromFile();
	}
	
	public void printAuction(int a) {
		System.out.println("auction " + a);
		
		for (int i = 0; i < trsHistory.size(); i++) {
			for (int j = 0; j <= 18; j ++) {
				System.out.print(trsHistory.get(i)[a][j] + " ");
			}
			System.out.println();
		}
	}
	
	public void setNext() {
		Random rand = new Random();
		historyIndex = rand.nextInt(trsHistory.size()-1);
	}
	
	public double getExpBidPrice (int a, int sec) {
		if (sec == 0) sec = 1;
		Misc.myassert(bidHistory.size() == trsHistory.size());

		a -= 16;
		double ret = 0;
		for (int i = 0; i < bidHistory.size(); i++) {
			double max = 0;
			for (int t = sec / 30; t < 18; t++) {
				double bid = bidHistory.get(i)[a][t];
				double trs = (trsHistory.get(i)[a][t] == 999) ? 0 : trsHistory.get(i)[a][t] - 1;
				max = Math.max(max, Math.max(bid, trs));
			}
			ret += max / bidHistory.size();
		}
		return ret;
	}
	
	// we are going to buy with this price.
	public double getExpAskPrice (int a, int sec) {
		if (sec == 0) sec = 1;
		Misc.myassert(askHistory.size() == trsHistory.size());
		
		a -= 16;
		double ret = 0;
		for (int i = 0; i < askHistory.size(); i++) {
			double min = 200;
			for (int t = sec / 30; t < 18; t++) {
				double ask = askHistory.get(i)[a][t];
				double trs = (trsHistory.get(i)[a][t] == 999) ? 200 : trsHistory.get(i)[a][t] + 1;
				min = Math.min(min, Math.min(ask, trs));
			}
			ret += min / askHistory.size();
		}
		return ret;
	}
	
	public double getPredAskPrice (int a, int sec) {
		if (sec == 0) sec = 1;
		Misc.myassert(sec > 0 && sec < 540);
		Misc.myassert(Constants.auctionType(a) == Constants.TYPE_EVENT);
		int t = (sec - (sec % 30)) / 30;
		int index = historyIndex;
		double trs = (trsHistory.get(index)[a-16][t] == 999) ? 200 : trsHistory.get(index)[a-16][t] + 1;
		
		return Math.min(200, Math.min(askHistory.get(index)[a-16][t], trs));
	}

	public double getPredBidPrice (int a, int sec) {
		if (sec == 0) sec = 1;
		Misc.myassert(sec > 0 && sec < 540);
		Misc.myassert(Constants.auctionType(a) == Constants.TYPE_EVENT);
		int t = (sec - (sec % 30)) / 30;
		int index = historyIndex;
		double trs = (trsHistory.get(index)[a-16][t] == 999) ? 0 : trsHistory.get(index)[a-16][t] - 1;
		
		return Math.max(0, Math.max(bidHistory.get(index)[a-16][t], trs));
	}
	
	// We can sell at this price in the future. From 30 seconds ~ end.
	public double getMaxBidPrice (int a, int sec) {
		if (sec == 0) sec = 1;
		Misc.myassert(sec > 0 && sec < 540);
		Misc.myassert(Constants.auctionType(a) == Constants.TYPE_EVENT);
		
		int t = (sec - (sec % 30)) / 30;
		int index = historyIndex;
		double max = 0;
		
		// If there was a transaction, we could have sold the good with the transaction price.
		for (int i = t + 1; i < 18; i++) {
			if (trsHistory.get(index)[a-16][i] == 999) break;
			max = Math.max(max, trsHistory.get(index)[a-16][i] - 1);
		}

		// There can be a case that 1) ask price is null, and 2) bid price is not null, for example, 130.
		// In this case, we could have sold the good with 130, although no transaction has been made.
		for (int i = t + 1; i < 18; i++) {
			max = Math.max(max, bidHistory.get(index)[a-16][i]);
		}
		
		return Math.min(200, Math.max(0, max));
	}
	
	// We can buy at this price in the future. After 30 seconds ~ end.
	public double getMinAskPrice(int a, int sec) {
		if (sec == 0) sec = 1;
		Misc.myassert(sec > 0 && sec < 540);
		Misc.myassert(Constants.auctionType(a) == Constants.TYPE_EVENT);
		
		int t = (sec - (sec % 30)) / 30;
		int index = historyIndex;
		double min = 200;
		
		// If there was a transaction, we could have bought the good with the transaction price.
		for (int i = t+1; i < 18; i++) {
			if (trsHistory.get(index)[a-16][i] == 999) break;
			min = Math.min(min, trsHistory.get(index)[a-16][i] + 1);
		}
		
		// There can be a case that 1) bid price is null, and 2) ask price is not null, for example, 130.
		// In this case, we could have bought the good with 130, although no transaction has been made.
		for (int i = t + 1; i < 18; i++) {
			min = Math.min(min, askHistory.get(index)[a-16][i]);
		}
		
		return Math.min(200, Math.max(min, 0));
	}

	public static void main(String[] args) {
		EventAnalyzer analyzer = new EventAnalyzer();
		
		// This two lines are for building a defualt file.
		// Since reading logs with parser takes too much time,
		// we preparse it, save it to the default file.
		/*
		analyzer.readDefaultFromLog();
		analyzer.writeDefaultToFile();
		
		// Now you read default file when you create the class
		long tic = System.currentTimeMillis();
		analyzer.readDefaultFromFile();
		long toc = System.currentTimeMillis();
		Misc.println("## Reading from the default file took : " + (toc-tic) + " miliseconds.");
		*/
		
		// When a game ends, you download the history and put it in our recent stack.
		analyzer.downloadFromHost("tac1.sics.se", 41600);
		analyzer.readLog(41600);
		
		// Note that you should call setRandomHistory before you call get*Price series,
		// else you will get all the same output, since we draw prices from one history.
		analyzer.setNext();
		for (int i = 1; i < 540; i += 30) {
			Misc.println("## predBidPrice " + analyzer.getPredBidPrice(20, i));
			Misc.println("## predAskPrice " + analyzer.getPredAskPrice(20, i));
			Misc.println("## maxBidPrice (future sell price) " + analyzer.getMaxBidPrice(20, i));
			Misc.println("## minAskPrice (future buy price) " + analyzer.getMinAskPrice(20, i));
			Misc.println("## expBidPrice (future sell price) " + analyzer.getExpBidPrice(20, i));
			Misc.println("## expAskPrice (future buy price) " + analyzer.getExpAskPrice(20, i));
		}  /* */
	}
}
