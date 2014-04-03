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
 * TravelPackage
 *
 * Author  : Bryan Guillemette
 * Created : 1 May, 2005
 * Description: 
 *  
 * This class stores a travel package for a customer. 
 * TravelPackage is a package that consists of arrival date, departure date, 
 * hotel type, and events planned for some of the nights. 
 */

package edu.brown.cs.tacds;

import props.Misc;
import edu.brown.cs.tac.Constants;

public class TravelPackage {
	public static int BASIC_UTILITY = 1000;
	
	// arrival date - value between 0 and 3
	// can also take value -1 - it means that client does not get to go at all - null package
	private int inflightDayIndex;
	private int outflightDayIndex;	// departure date - value between 0 and 3
	private int hotelType;  // 0 - bad hotel, 1 - good hotel
	
	//  0 - no event, 1 - event1, 2- event2, 3 - event3
	private int eventDayIndex0;
	private int eventDayIndex1;
	private int eventDayIndex2;
	private int eventDayIndex3;
    
	private int utility;

	private boolean isNull = false;
	
	public int getNumEventUsed () {
		int ret = 0;
		if (eventDayIndex0 != 0) ret++;
		if (eventDayIndex1 != 0) ret++;
		if (eventDayIndex2 != 0) ret++;
		if (eventDayIndex3 != 0) ret++;
		return ret;
	}
	
    public TravelPackage(int _inflight,
			 int _outflight,
			 int _hotelType,
			 int _eventIndex1,
			 int _eventIndex2,
			 int _eventIndex3) {
    	inflightDayIndex = _inflight;
    	outflightDayIndex = _outflight;
    	hotelType = _hotelType;
    	
    	eventDayIndex0 = 0;
    	eventDayIndex1 = 0;
    	eventDayIndex2 = 0;
    	eventDayIndex3 = 0;
    	
    	switch (_eventIndex1) {
    	case 1: eventDayIndex0 = 1; break;
    	case 2: eventDayIndex1 = 1; break;
    	case 3: eventDayIndex2 = 1; break;
    	case 4: eventDayIndex3 = 1; break;
    	}
    	switch (_eventIndex2) {
    	case 1: eventDayIndex0 = 2; break;
    	case 2: eventDayIndex1 = 2; break;
    	case 3: eventDayIndex2 = 2; break;
    	case 4: eventDayIndex3 = 2; break;
    	}
    	switch (_eventIndex3) {
    	case 1: eventDayIndex0 = 3; break;
    	case 2: eventDayIndex1 = 3; break;
    	case 3: eventDayIndex2 = 3; break;
    	case 4: eventDayIndex3 = 3; break;
    	}
		
		utility = 0;
		
		Misc.myassert(isValid());
    }
	
    public TravelPackage(int _inflight,
    					 int _outflight,
    					 int _hotelType,
    					 int _eventDayIndex0,
    					 int _eventDayIndex1,
    					 int _eventDayIndex2,
    					 int _eventDayIndex3) {
    	inflightDayIndex = _inflight;
    	outflightDayIndex = _outflight;
    	hotelType = _hotelType;
    	eventDayIndex0 = _eventDayIndex0;
		eventDayIndex1 = _eventDayIndex1;
		eventDayIndex2 = _eventDayIndex2;
		eventDayIndex3 = _eventDayIndex3;
		
		utility = 0;

		Misc.myassert(isValid());
    }
    
    public TravelPackage(TravelPackage _tp) {
    	if (_tp != null) {
			utility = _tp.getUtility();
			inflightDayIndex = _tp.getInflightDayIndex();
			outflightDayIndex = _tp.getOutflightDayIndex();
			hotelType = _tp.getHotelType();
			eventDayIndex0 = _tp.getEventDayIndex0();
			eventDayIndex1 = _tp.getEventDayIndex1();
			eventDayIndex2 = _tp.getEventDayIndex2();
			eventDayIndex3 = _tp.getEventDayIndex3();
			
			isNull = _tp.isNull();
    	} else {
    		isNull = true;
    	}

		Misc.myassert(isValid());
    }
    
    public static TravelPackage nullPackage() {
    	return new TravelPackage(null);
    }

    public boolean isValid(){
    	boolean ret = true;
    	
    	// the null package is valid
    	if (isNull) {
    		return true;
    	}

    	// make sure inflight is valid day
    	if (inflightDayIndex < 0 || inflightDayIndex > 3) {
    		ret = false;
    	}
    	
    	// make sure outflight is valid day
    	if (outflightDayIndex < 0 || outflightDayIndex > 3) {
    		ret = false;
    	}
    	
		// make sure outflight is at least 1 day later than inflight
		if (outflightDayIndex < inflightDayIndex) {
			ret = false;
		}
		
		// make sure only one of each type of event ticket is used
		int event1Sum = 0;
		if (eventDayIndex0 == 1)
			event1Sum++;
		if (eventDayIndex1 == 1)
			event1Sum++;
		if (eventDayIndex2 == 1)
			event1Sum++;
		if (eventDayIndex3 == 1)
			event1Sum++;
		if (event1Sum > 1)
			ret = false;

		int event2Sum = 0;
		if (eventDayIndex0 == 2)
			event2Sum++;
		if (eventDayIndex1 == 2)
			event2Sum++;
		if (eventDayIndex2 == 2)
			event2Sum++;
		if (eventDayIndex3 == 2)
			event2Sum++;
		if (event2Sum > 1)
			ret = false;

		int event3Sum = 0;
		if (eventDayIndex0 == 3)
			event3Sum++;
		if (eventDayIndex1 == 3)
			event3Sum++;
		if (eventDayIndex2 == 3)
			event3Sum++;
		if (eventDayIndex3 == 3)
			event3Sum++;
		if (event3Sum > 1)
			ret = false;
		
		// make sure events fall on a day when we're in town
		if ((inflightDayIndex > 0 && eventDayIndex0 != 0) ||
			(inflightDayIndex > 1 && eventDayIndex1 != 0) ||
			(inflightDayIndex > 2 && eventDayIndex2 != 0) ||
			(outflightDayIndex < 3 && eventDayIndex3 != 0) ||
			(outflightDayIndex < 2 && eventDayIndex2 != 0) ||
			(outflightDayIndex < 1 && eventDayIndex1 != 0))
			ret = false;
		
		return ret;
    }
	
    public int getUtility() {
		if (isNull)
			return 0;
    	else
    		return utility;
    }

    public float calculateCost (Priceline prices) {
    	if (!isValid() || isNull) {
    		if(!isValid()) System.out.println("isValid has a problem?, travelpackage");
    		return 0;
    	}
    	
    	float cost = 0;
    	
    	for (int i = inflightDayIndex; i <= outflightDayIndex; i++) {
    		if (hotelType == 0)
    			cost += prices.currBuyPrice[Constants.AUCTION_BAD_HOTEL + i];
    		else
    			cost += prices.currBuyPrice[Constants.AUCTION_GOOD_HOTEL + i];
    	}
    	
    	cost += prices.currBuyPrice[Constants.AUCTION_INFLIGHT  + inflightDayIndex];
    	cost += prices.currBuyPrice[Constants.AUCTION_OUTFLIGHT + outflightDayIndex];
    	
    	if (eventDayIndex0 != 0) {
    		cost += prices.currBuyPrice[12 + eventDayIndex0 * 4];
    	}
    	
    	if (eventDayIndex1 != 0) {
    		cost += prices.currBuyPrice[13 + eventDayIndex0 * 4];
    	}
    	
    	if (eventDayIndex2 != 0) {
    		cost += prices.currBuyPrice[14 + eventDayIndex0 * 4];
    	}
    	
    	if (eventDayIndex3 != 0) {
    		cost += prices.currBuyPrice[15 + eventDayIndex0 * 4];
    	}
    	
    	if (cost == 0) System.exit(0);
    	assert(cost != 0);
    	return cost;
    }
    
    public float calculateUtility(Preference prefs, boolean flightsOn, boolean hotelsOn, boolean eventsOn) {
		if (!isValid() || isNull){
		    return 0;
		}
        
        int inflightDayIndexPref = prefs.getArrivalDate() - 1;
        int outflightDayIndexPref = prefs.getDepartureDate() - 2;
        int hotelPref = prefs.getHotelValue();
        int event1Pref = prefs.getEvent1Value();
        int event2Pref = prefs.getEvent2Value();
        int event3Pref = prefs.getEvent3Value();

		int travelPenalty = 100 * (Math.abs(inflightDayIndexPref - getInflightDayIndex()) + Math.abs(outflightDayIndexPref - getOutflightDayIndex()));
        
		int hotelBonus = getHotelType() * hotelPref;

		int funBonus = 0;

		switch(eventDayIndex0) {
			case 1 : funBonus += event1Pref; break;
			case 2 : funBonus += event2Pref; break;
			case 3 : funBonus += event3Pref; break;
			default : break;
		}

		switch(eventDayIndex1) {
			case 1 : funBonus += event1Pref; break;
			case 2 : funBonus += event2Pref; break;
			case 3 : funBonus += event3Pref; break;
			default : break;
		}

		switch(eventDayIndex2) {
			case 1 : funBonus += event1Pref; break;
			case 2 : funBonus += event2Pref; break;
			case 3 : funBonus += event3Pref; break;
			default : break;
		}

		switch(eventDayIndex3) {
			case 1 : funBonus += event1Pref; break;
			case 2 : funBonus += event2Pref; break;
			case 3 : funBonus += event3Pref; break;
			default : break;
		}
	
		utility = BASIC_UTILITY - (flightsOn ? travelPenalty : 0) + (hotelsOn ? hotelBonus : 0) + (eventsOn ? funBonus : 0);
		
		return (float) utility; 
    }
    
	public String toString() {
		if (isNull) return "NULL TRAVEL PACKAGE";
		
		String ret = "";
		ret += "in: " + (inflightDayIndex + 1) + ", out: " + (outflightDayIndex + 2) + ", hotel: ";
		
		if (hotelType == 0)
			ret += "BAD ";
		else
			ret += "GOOD";
		
		ret += ", events: " + eventDayIndex0 + "" + eventDayIndex1 + "" + eventDayIndex2 + "" + eventDayIndex3;
        ret += ", utility: " + utility;
		
        return ret;
	}

	/*
	 * Getters and Setters
	 */

	public int getEvent(int dayIndex) {
		if (isNull)
			return 0;
		switch(dayIndex) {
			case 0 : return eventDayIndex0;
			case 1 : return eventDayIndex1;
			case 2 : return eventDayIndex2;
			case 3 : return eventDayIndex3;
			default: return -1; 
		}
	}

    public int getEventDayIndex0() {
		if (isNull)
			return -1;
        return eventDayIndex0;
    }

    public int getEventDayIndex1() {
		if (isNull)
			return -1;
        return eventDayIndex1;
    }

    public int getEventDayIndex2() {
		if (isNull)
			return -1;
        return eventDayIndex2;
    }

    public int getEventDayIndex3() {
		if (isNull)
			return -1;
        return eventDayIndex3;
    }

    public int getHotelType() {
		if (isNull)
			return -1;
        return hotelType;
    }

    public int getInflightDayIndex() {
		if (isNull)
			return -1;
        return inflightDayIndex;
    }

    public int getOutflightDayIndex() {
		if (isNull)
			return -1;
        return outflightDayIndex;
    }

    public void setEventDayIndex0(int i) {
        eventDayIndex0 = i;
    }

    public void setEventDayIndex1(int i) {
        eventDayIndex1 = i;
    }

    public void setEventDayIndex2(int i) {
        eventDayIndex2 = i;
    }

    public void setEventDayIndex3(int i) {
        eventDayIndex3 = i;
    }

    public void setHotelType(int i) {
        hotelType = i;
    }

    public void setInflight(int i) {
        inflightDayIndex = i;
    }

    public void setOutflight(int i) {
        outflightDayIndex = i;
    }

	public void setIsNull(boolean b) {
		isNull = b;
	}
	
	public boolean isNull() {
		return isNull;
	}

	public boolean containsGood(int auctionNo) {
		if (isNull)
			return false;
		
		if (auctionNo >= Constants.AUCTION_INFLIGHT &&
			auctionNo <= Constants.AUCTION_INFLIGHT + 3) {
			if ((auctionNo - Constants.AUCTION_INFLIGHT) == inflightDayIndex)
				return true;
			else
				return false;
		} else if (auctionNo >= Constants.AUCTION_OUTFLIGHT &&
				   auctionNo <= Constants.AUCTION_OUTFLIGHT + 3) {
			if ((auctionNo - Constants.AUCTION_OUTFLIGHT) == outflightDayIndex)
				return true;
			else
				return false;	
		} else if (auctionNo >= Constants.AUCTION_BAD_HOTEL &&
				   auctionNo <= Constants.AUCTION_BAD_HOTEL + 3) {
			if (((auctionNo - Constants.AUCTION_BAD_HOTEL) >= inflightDayIndex) &&
				((auctionNo - Constants.AUCTION_BAD_HOTEL) <= outflightDayIndex) &&
				(hotelType == 0))
				return true;
			else
				return false;
		} else if (auctionNo >= Constants.AUCTION_GOOD_HOTEL &&
				   auctionNo <= Constants.AUCTION_GOOD_HOTEL + 3) {
			if (((auctionNo - Constants.AUCTION_GOOD_HOTEL) >= inflightDayIndex) &&
				((auctionNo - Constants.AUCTION_GOOD_HOTEL) <= outflightDayIndex) &&
				(hotelType == 1))
				return true;
			else
				return false;
		} else if (auctionNo >= Constants.AUCTION_ALLIGATOR &&
				   auctionNo <= Constants.AUCTION_ALLIGATOR + 3) {
			switch (auctionNo - Constants.AUCTION_ALLIGATOR) {
				case 0 :
					return ((eventDayIndex0 == 1) ? true : false);
				case 1 :
					return ((eventDayIndex1 == 1) ? true : false);
				case 2 :
					return ((eventDayIndex2 == 1) ? true : false);
				case 3 :
					return ((eventDayIndex3 == 1) ? true : false);
				default :
					return false;
			}
		} else if (auctionNo >= Constants.AUCTION_AMUSEMENT &&
				   auctionNo <= Constants.AUCTION_AMUSEMENT + 3) {
			switch (auctionNo - Constants.AUCTION_AMUSEMENT) {
				case 0 :
					return ((eventDayIndex0 == 2) ? true : false);
				case 1 :
					return ((eventDayIndex1 == 2) ? true : false);
				case 2 :
					return ((eventDayIndex2 == 2) ? true : false);
				case 3 :
					return ((eventDayIndex3 == 2) ? true : false);
				default :
					return false;
			}
		} else if (auctionNo >= Constants.AUCTION_MUSEUM &&
				   auctionNo <= Constants.AUCTION_MUSEUM + 3) {
			switch (auctionNo - Constants.AUCTION_MUSEUM) {
				case 0 :
					return ((eventDayIndex0 == 3) ? true : false);
				case 1 :
					return ((eventDayIndex1 == 3) ? true : false);
				case 2 :
					return ((eventDayIndex2 == 3) ? true : false);
				case 3 :
					return ((eventDayIndex3 == 3) ? true : false);
				default :
					return false;
			}
		} else {
			return false;
		}
	}
}	