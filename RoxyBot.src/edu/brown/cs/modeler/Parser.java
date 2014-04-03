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
 * Parser
 *
 * Author  : Seong Jae Lee
 * Created : 16 June, 2006
 * Description:
 * 
 * This is used in EventAnalyzer
 * 
 */

package edu.brown.cs.modeler;

import edu.brown.cs.tac.Constants;
import edu.brown.cs.tacds.*;
import java.io.FileReader;
import java.io.IOException;
import java.io.LineNumberReader;
import java.text.DecimalFormat;
import java.util.Arrays;

import props.Misc;

public class Parser {
	protected static double EPSILON = 0.001;
	protected static DecimalFormat formatter = new DecimalFormat("0.0");
	
	protected String 			url;
	protected long			startTime;
	protected String[]		auctionId; 		// [28]
	public  String[]				agentID;			// [8]
	public  String[]				agentName;		// [8]
	
	// basic information
	public double[]				score, utility;
	public Preference[][]	preferences;	// [agent][client]
	public TACCollection[][]		collections;	// [agent][minute]
	public Allocation[]				allocations;		// [agent]
	
	// history
	public double[][]			flightHistory;			// [auction][decisecond]
	public double[]			flightMinimum;		// [auction]
	public double[][]		  	hotelHistory;			// [auction][minute]
	public int[]					hotelClosingTime;	// [auction]
	public double[]			hotelClosingPrice;	// [auction]
	public double[][]			eventBidHistory;		// [auction][decisecond] // Ask > Bid
	public double[][]			eventAskHistory;	// [auction][decisecond]
	public double[][]			eventTrsHistory;
	
	// bid and transaction
	public double[][]			bidPrice, askPrice, buyPrice, sellPrice; // [agent][auction]
	public int[][]				bidQuantity, askQuantity, buyQuantity, sellQuantity; // [agent][auction]
	
	// options. if you want to save time, change some of this variable to false.
	public boolean readScore = true;
	public boolean readPreference = true;
	public boolean readCollection = true;
	public boolean readAllocation = true;
	public boolean readBid = true;
	public boolean readTransaction = true;
	public boolean readQuote = true;
	
	public double getMinFlight (int i) {
		double ret = 1000;
		for (int t = 0; t < flightHistory[i].length; t++) {
			ret = Math.min(flightHistory[i][t], ret);
		}
		return ret;
	}
	
	/*
	public double getHotelClosingPrice(int i) {
		if (i >= 8) i -= 8;
		return hotelHistory[i][hotelClosingTime[i]];
	}*/
	
	private void init() {
		collections  			= new TACCollection[8][9];
		preferences			= new Preference[8][8];
		allocations				= new Allocation[8];
		flightHistory			= new double[8][55];
		flightMinimum			= new double[8];
		hotelHistory	 		= new double[8][9];
		hotelClosingTime 	= new int[8];
		hotelClosingPrice	= new double[8];
		bidQuantity 			= new int[8][28];
		askQuantity			= new int[8][28];
		buyQuantity			= new int[8][28];
		sellQuantity			= new int[8][28];
		bidPrice					= new double[8][28];
		askPrice				= new double[8][28];
		buyPrice				= new double[8][28];
		sellPrice					= new double[8][28];
		eventBidHistory		= new double[12][55];
		eventAskHistory  	= new double[12][55];
		eventTrsHistory		= new double[12][55];
		score						= new double[8];
		utility   					= new double[8];
		
		for (int i = 0; i < 12; i++) Arrays.fill(eventAskHistory[i], 200);
		for (int i = 0; i < 12; i++) Arrays.fill(eventTrsHistory[i], 999);
		for (int i = 0; i < 8; i++) for (int j = 1; j < 9; j++) hotelHistory[i][j] = Constants.INFINITY;
		
		auctionId = new String[28];
		agentID   = new String[8];
		agentName = new String[8];
	}
	
	private void readAgentID() throws IOException {
		LineNumberReader lineReader = new LineNumberReader(new FileReader(url));
		String line = lineReader.readLine();
		int id = 0;
		while (id < 8 && line != null) {
			if (line.contains(",a,")) {
				String[] fragment = line.split(",");
				agentID[id] = fragment[3];
				agentName[id] = fragment[2];
				id++;
			}
			
			line = lineReader.readLine();
		}
		if (line == null) Misc.warn("readAgentID : id " + id);
	}
	
	private void readAuctionID() throws IOException {
		LineNumberReader lineReader = new LineNumberReader(new FileReader(url));
		String line = lineReader.readLine();
		int id = 0;
		while (id < 28 && line != null) {
			if (line.contains(",u,")) {
				String[] fragment = line.split(",");
				auctionId[id] = fragment[2];
				id++;
			}
			line = lineReader.readLine();
		}
		if (line == null) Misc.warn("readAuctionID");
		
		// Good hotel and bad hotel are reversed.
		for (int i = 0; i < 4; i++) {
			String temp = auctionId[8+i];
			auctionId[8+i] = auctionId[12+i];
			auctionId[12+i] = temp;
		}
	}
	
	private void readPreference() throws IOException {
		LineNumberReader lineReader = new LineNumberReader(new FileReader(url));
		for (String line = lineReader.readLine(); line != null; line = lineReader.readLine()) {
			String[] fragment = line.split(",");
			if (!fragment[1].equals("c")) continue;
			int agent = this.getAgentID(fragment[3]);
			
			for (int c = 0; c < 8; c++) {
				int index = 4 + c * 6;
				int in  = Integer.valueOf(fragment[index]);
				int out = Integer.valueOf(fragment[index+1]);
				int hp  = Integer.valueOf(fragment[index+2]);
				int e1p = Integer.valueOf(fragment[index+3]);
				int e2p = Integer.valueOf(fragment[index+4]);
				int e3p = Integer.valueOf(fragment[index+5]);
				
				preferences[agent][c] = new Preference(in, out, hp, e1p, e2p, e3p);
			}
		}
	}
	
	private void readCollection() throws IOException {
		for (int agent = 0; agent < 8; agent++) {
			for (int i = 0; i < 9; i++) {
				collections[agent][i] = new TACCollection();
			}
		}
		
		LineNumberReader lineReader = new LineNumberReader(new FileReader(url));
		
		int oldMinute = 0;
		for (String line = lineReader.readLine(); line != null; line = lineReader.readLine()) {
			String[] fragment = line.split(",");
			if (!fragment[1].equals("t")) continue;
			
			int decisec = ((int)(Long.valueOf(fragment[0]) - startTime)) / 10;
			int minute = decisec / 6;
			int buyer = this.getAgentID(fragment[2]);
			int seller = this.getAgentID(fragment[3]);
			int auction = this.getAuctionID(fragment[4]);
			int quantity = Integer.valueOf(fragment[5]);
			
			if (oldMinute != minute) {
				for (int agent = 0; agent < 8; agent++) {
					for (int m = oldMinute + 1; m < 9; m++) {
						collections[agent][m] = new TACCollection(collections[agent][oldMinute]);
					}
				}
				oldMinute = minute;
			}
			
			/*
			if (auction == 23 && (buyer == 0 || seller == 0)) {
				if (seller == -1) {
					System.out.println("Parser transaction a" + auction + 
							", from the server\t" + 
							", to " + this.agentName[buyer] + "\t" +
							", q " + quantity);
				} else {
					System.out.println("Parser transaction a" + auction + 
							", from " + this.agentName[seller] + "\t" + 
							", to " + this.agentName[buyer] + "\t" + 
							", decisec " + decisec + "\t" +
							", q" + quantity);
				}
			} /**/
			
			int own = collections[buyer][minute].getOwn(auction) + quantity;
			collections[buyer][minute].setOwn(auction, own);
			
			if (seller != -1) {
				own = collections[seller][minute].getOwn(auction) - quantity;
				collections[seller][minute].setOwn(auction, own);
			}
		}
		
		for (int m = oldMinute + 1; m < 9; m++) {
			for (int agent = 0; agent < 8; agent++) {
				collections[agent][m] = new TACCollection(collections[agent][oldMinute]);
			}
		}
		
		for (int agent = 0; agent < 8; agent++) {
			for (int a = 16; a < 28; a++) {
				collections[agent][8].setOwn(a, Math.max(0, collections[agent][8].getOwn(a)));
			}
		}
	}

	private void readAllocation() throws IOException {
		for (int agent = 0; agent < 8; agent++) {
			allocations[agent] = new Allocation();
		}
		
		// l, gameID, agentID, in, out, hotel, e1, e2, e3 [, more client allocations]
		LineNumberReader lineReader = new LineNumberReader(new FileReader(url));
		for (String line = lineReader.readLine(); line != null; line = lineReader.readLine()) {
			String[] fragment = line.split(",");
			if (!fragment[1].equals("l")) continue;
			int agent = this.getAgentID(fragment[3]);
			
			for (int c = 0; c < 8; c++) {
				int index = 4 + c * 6;
				int in  = Integer.valueOf(fragment[index]) - 1;
				int out = Integer.valueOf(fragment[index+1]) - 2;
				int hp  = Integer.valueOf(fragment[index+2]);
				int e1p = Integer.valueOf(fragment[index+3]);
				int e2p = Integer.valueOf(fragment[index+4]);
				int e3p = Integer.valueOf(fragment[index+5]);
				
				Misc.myassert(in >= -1 && in <= 3);
				Misc.myassert(out >= -2 && out <= 3);
				if (in == -1)
					allocations[agent].setTravelPackage(c, TravelPackage.nullPackage());
				else
					allocations[agent].setTravelPackage(c, new TravelPackage(in, out, hp, e1p, e2p, e3p));
			}
		}
	}

	private void readScore() throws IOException {
		LineNumberReader lineReader = new LineNumberReader(new FileReader(url));
		for (String line = lineReader.readLine(); line != null; line = lineReader.readLine()) {
			String[] fragment = line.split(",");
			
			if (fragment[1].equals("s")) {
				int agentId = getAgentID(fragment[3]);
				score[agentId] = Double.valueOf(fragment[4]);
				utility[agentId] = Double.valueOf(fragment[6]);
			}
		}
	}
	
	private void readQuote() throws IOException {
		for (int a = 0; a < 8; a++) {
			flightMinimum[a] = Constants.MAX_PRICE_FOR_FLIGHT;
		}
		
		LineNumberReader lineReader = new LineNumberReader(new FileReader(url));
		for (String line = lineReader.readLine(); line != null; line = lineReader.readLine()) {
			String[] fragment = line.split(",");
			if (!fragment[1].equals("q")) continue;

			int sec =  ((int)(Long.valueOf(fragment[0]) - startTime));
			int decisec = sec / 10;
			int minute = decisec / 6;
			int auction = this.getAuctionID(fragment[2]);
			double askPrice = Double.valueOf(fragment[3]);	// buy with this price
			double bidPrice = Double.valueOf(fragment[4]); // sell with this price, only for event

			switch(Constants.auctionType(auction)) {
			case Constants.TYPE_FLIGHT:
				flightHistory[auction][decisec] = (int) askPrice;
				flightMinimum[auction] = Math.min(flightMinimum[auction], askPrice);
				Misc.myassert(flightHistory[auction][decisec] > 100);
				break;
			
			case Constants.TYPE_HOTEL:
				if (hotelHistory[auction-8][minute] > 0 && 
						hotelHistory[auction-8][minute] < Constants.INFINITY) {
					hotelHistory[auction-8][minute] = Constants.INFINITY;
					hotelClosingPrice[auction-8] = askPrice;
					hotelClosingTime[auction-8] = minute;
				} else {
					hotelHistory[auction-8][minute] = askPrice;
				}
				break;
			
			case Constants.TYPE_EVENT:
				eventAskHistory[auction-16][decisec] = askPrice;
				eventBidHistory[auction-16][decisec] = bidPrice;
				
				if (eventAskHistory[auction-16][decisec] == 0 || eventAskHistory[auction-16][decisec] > 200) 
					eventAskHistory[auction-16][decisec] = 200;

				if (eventBidHistory[auction-16][decisec] > 200)
					eventBidHistory[auction-16][decisec] = 200;
				
				for (int i = decisec + 1; i <= 54; i++) {
					eventAskHistory[auction-16][i] = eventAskHistory[auction-16][decisec];
					eventBidHistory[auction-16][i] = eventBidHistory[auction-16][decisec];
				}
				
				break;
			}
		}
	}
	
	private void readBid() throws IOException {
		// 0       1  2         3             4                5             6
		// time, b, bidID, agentID, auctionID, bidType, processStatus [, bidList]
		// bidType
		//		s for bid submission,
		// 		r for bid replacement,
		// 		t for transaction,
		// 		w for bid withdrawal,
		// 		c for rejected bid
		// bidList is the bid list in the form quantity1, unitPrice1, quantity2, unitPrice2, etc
		LineNumberReader lineReader = new LineNumberReader(new FileReader(url));
		for (String line = lineReader.readLine(); line != null; line = lineReader.readLine()) {
			String[] fragment = line.split(",");
			if (!fragment[1].equals("b")) continue;
			
			int sec = ((int)(Long.valueOf(fragment[0]) - startTime));
			int agentID = getAgentID(fragment[3]);
			int auctionID = getAuctionID(fragment[4]);
			String bidType = fragment[5];
			
			if (bidType.equals("s")) {
				Misc.myassert (fragment.length >= 7 && fragment.length % 2 == 1);
				
				for (int i = 0; i < (fragment.length - 7)/2; i++) {
					double price = Double.valueOf(fragment[8 + 2 * i]);
					int quantity =Integer.valueOf(fragment[7 + 2 * i]);

					if (price * quantity > -EPSILON) {
						bidPrice[agentID][auctionID] += price;
						bidQuantity[agentID][auctionID] += quantity;
					} else {
						askPrice[agentID][auctionID] += price;
						askQuantity[agentID][auctionID] += quantity;
					}
				} // end of for
			} // end of if
		} // end of for
	}

	private void readTransaction() throws IOException {
		// 0       1 2             3            4                5             6
		// time, t,buyerID, sellerID, auctionID, quantity, price [, transactionID]
		LineNumberReader lineReader = new LineNumberReader(new FileReader(url));
		for (String line = lineReader.readLine(); line != null; line = lineReader.readLine()) {
			String[] fragment = line.split(",");
			if (!fragment[1].equals("t")) continue;
			
			
			int decisec = ((int)(Long.valueOf(fragment[0]) - startTime)) / 10;
			int buyer = this.getAgentID(fragment[2]);
			int seller = this.getAgentID(fragment[3]);
			int auction = this.getAuctionID(fragment[4]);
			int quantity = Integer.valueOf(fragment[5]);
			double price = Double.valueOf(fragment[6]);
			
			// We don't count initially distributed entertainment tickets.
			if (!fragment[3].equals("auction") && 
					Constants.auctionType(auction) ==  Constants.TYPE_EVENT) {
				buyQuantity[buyer][auction] += quantity;
				buyPrice[buyer][auction] += quantity * price;
			}
			
			if (Constants.auctionType(auction) !=  Constants.TYPE_EVENT) {
				buyQuantity[buyer][auction] += quantity;
				buyPrice[buyer][auction] += quantity * price;
			}
			
			if (seller != -1) {
				sellQuantity[seller][auction] += quantity;
				sellPrice[seller][auction] += quantity * price;
			}
			
			if (Constants.auctionType(auction) ==  Constants.TYPE_EVENT) {
				eventTrsHistory[auction-16][decisec] = price;
			}
		}
		
		for (int a = 16; a < 28; a++) {
			double tmp = eventTrsHistory[a-16][54];
			for (int t = 53; t > 0; t--) {
				if (eventTrsHistory[a-16][t] == 999) {
					eventTrsHistory[a-16][t] = tmp;
				} else {
					tmp = eventTrsHistory[a-16][t];
				}
			}
			eventTrsHistory[a-16][0] = tmp;
		}
	}
	
	public void parse (String url) throws IOException { 
		Misc.myassert(url.endsWith(".log"));
		
		init();
		
		this.url = url;
		LineNumberReader lineReader = new LineNumberReader(new FileReader(url));
		String line = lineReader.readLine();
		startTime = Long.valueOf(line.split(",")[0]);
		
		// Always, read these two method first.
		this.readAgentID();
		this.readAuctionID();
		
		if (readScore) this.readScore();
		if (readPreference) this.readPreference();
		if (readCollection) this.readCollection();
		if (readAllocation) this.readAllocation();
		if (readBid) this.readBid();
		if (readTransaction) this.readTransaction();
		if (readQuote) this.readQuote();
	}
	
	private int getAuctionID (String s) {
		for (int a = 0; a < 28; a++) {
			if (auctionId[a].equals(s)) {
				return a;
			}
		}
		
		return -1;
	}
	
	public int getAgentIDFromName (String s) {
		for (int a = 0; a < 8; a++) {
			if (agentName[a].equals(s)) {
				return a;
			}
		}
		
		return -1; // when 'auction'
	}
	
	public int getAgentID (String s) {
		for (int a = 0; a < 8; a++) {
			if (agentID[a].equals(s)) {
				return a;
			}
		}
		
		return -1; // when 'auction'
	}
		
	public String toString() {
		String ret = "";
		
		int maxAuction = 16;
		
		/*
		Misc.println("agentID\tagentName\tscore\tutility");
		for (int i = 0; i < 8; i++) {
			Misc.println(agentID[i] + "\t"
					+ agentName[i] + "\t" 
					+ formatter.format(score[i]) + "\t" 
					+ formatter.format(utility[i]));
		} /**/
		
		Misc.println("BidPrice");
		for (int i = 0; i < 8; i++) {
			Misc.print(getAgentWithTab(i));
			for (int j = 0; j < maxAuction; j++) {
				Misc.print(formatter.format(bidPrice[i][j]) + "\t");
			}
			Misc.println();
		} /**/
		
		Misc.println("BuyPrice");
		for (int i = 0; i < 8; i++) {
			Misc.print(getAgentWithTab(i));
			for (int j = 0; j < maxAuction; j++) {
				Misc.print(formatter.format(buyPrice[i][j]) + "\t");
			}
			Misc.println();
		} /**/
		
		Misc.println("BuyQuantity");
		for (int i = 0; i < 8; i++) {
			Misc.print(getAgentWithTab(i));
			for (int j = 0; j < maxAuction; j++) {
				Misc.print(buyQuantity[i][j] + "\t");
			}
			Misc.println();
		} /**/
		
		Misc.println("HotelHistory");
		for (int i = 0; i < 8; i++) {
			for (int j = 0; j < 9; j++) {
				Misc.print(formatter.format(hotelHistory[i][j]) + "\t");
			}
			Misc.println();
		} /**/
		
		Misc.println("HotelClosingTime");
		for (int i = 0; i < 8; i++) {
			Misc.print(hotelClosingTime[i] + "\t");
		}
		Misc.println();
		
		Misc.println("HotelClosingPrice");
		for (int i = 0; i < 8; i++) {
			Misc.print(formatter.format(this.hotelClosingPrice[i]) + "\t");
		}
		Misc.println(); /**/
		
		return ret;
	}
	
	public String getAgentWithTab(int i) {
		if (agentName[i].length() < 8) return agentName[i] + "\t\t";
		return agentName[i] + "\t";
	}
	
	public static void main (String[] args) {
		try {			
			Parser p = new Parser();
			p.parse("/pro/roxybot/2005/sjlee/data/history/final06/applet1.log");
			Misc.println(p);
		} catch (IOException e) {	e.printStackTrace(); }
	}
}
