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
 * CustomerPrefs
 *
 * Author  : Bryan Guillemette
 * Created : 1 May, 2005
 * Description: 
 *    
 * Customer Preference Data for a single Customer.
 * 
 */

package edu.brown.cs.tacds;

import java.util.Arrays;

import edu.brown.cs.tac.TacRandom;

import props.Misc;

public class Preference {
    
	public static int HOTEL_INTERVAL = 100;
	
    private int arrivalDay;  // measure in actual days (possible values: 1-4)
    private int departureDay;  // measured in actual days (possible values: 2-5)
	private int hotelValue;
	private int event1Value;
	private int event2Value;
	private int event3Value;
	private int[] sortedEventValues;//sorted in ascending order
	
	public Preference() {
		switch(TacRandom.nextInt(this, 10)) {
    		case 0	:	arrivalDay = 1; departureDay = 2; break;
    		case 1	:	arrivalDay = 1; departureDay = 3; break;
    		case 2	:	arrivalDay = 1; departureDay = 4; break;
    		case 3	:	arrivalDay = 1; departureDay = 5; break;
    		case 4	:	arrivalDay = 2; departureDay = 3; break;
    		case 5	:	arrivalDay = 2; departureDay = 4; break;
    		case 6	:	arrivalDay = 2; departureDay = 5; break;
    		case 7	:	arrivalDay = 3; departureDay = 4; break;
    		case 8	:	arrivalDay = 3; departureDay = 5; break;
    		case 9	:	arrivalDay = 4; departureDay = 5; break;
    		default :	Misc.error("Random isn't working as I expect."); break;
    	}
		
    	hotelValue = 100 - HOTEL_INTERVAL/2 + TacRandom.nextInt(this, HOTEL_INTERVAL + 1);
		
		event1Value = TacRandom.nextInt(this, 201);
		event2Value = TacRandom.nextInt(this, 201);
		event3Value = TacRandom.nextInt(this, 201);
		
		sortedEventValues = new int[3];
		sortedEventValues[0] = event1Value;
		sortedEventValues[1] = event2Value;
		sortedEventValues[2] = event3Value;
		Arrays.sort(sortedEventValues);//ascending order
	}
	
    public Preference(int a, int d, int h, int e1, int e2, int e3) {
        arrivalDay = a;
        departureDay = d;
        hotelValue = h;
        event1Value = e1;
        event2Value = e2;
        event3Value = e3;
        
        sortedEventValues = new int[3];
		sortedEventValues[0] = e1;
		sortedEventValues[1] = e2;
		sortedEventValues[2] = e3;
		Arrays.sort(sortedEventValues);//ascending order
    }
    
    public Preference(Preference cprefs) {
    	arrivalDay = cprefs.arrivalDay;
    	departureDay = cprefs.departureDay;
    	hotelValue = cprefs.hotelValue;
    	event1Value = cprefs.event1Value;
    	event2Value = cprefs.event2Value;
    	event3Value = cprefs.event3Value;
    	sortedEventValues = new int[3];
    	for(int i=0; i<sortedEventValues.length; i++) {
    		sortedEventValues[i] = cprefs.getSortedEventValues()[i];
    	}
    }

    /*
     * Accessor Methods. Get called to return the values for various
     * customer preferences. 
     */
    public int getArrivalDate() {
        return arrivalDay;
    }
    public int getDepartureDate() {
        return departureDay;
    }
    public int getHotelValue() {
        return hotelValue;
    }
    public int getEvent1Value() {
        return event1Value;
    }
    public int getEvent2Value() {
        return event2Value;
    }
    public int getEvent3Value() {
        return event3Value;
    }

    public int getEventValue(int type) {
        switch (type) {
        	case 0 :
        		return 0;
            case 1 :
                return getEvent1Value();
            case 2 :
                return getEvent2Value();
            case 3 :
                return getEvent3Value();
        }

        return -1;
    }
    
    public int[] getSortedEventValues() {
    	return sortedEventValues;
    }
    
    public void incrementValue(int index, double value) {
    	switch(index) {
    	case 0:
    		arrivalDay += value;
    		break;
    	case 1:
    		departureDay += value;
    		break;
    	case 2:
    		hotelValue += value;
    		break;
    	case 3:
    		event1Value += value;
    		break;
    	case 4:
    		event2Value += value;
    		break;
    	case 5:
    		event3Value += value;
    		break;
    	}
    }
    
    public String toString() {
    	String ret = ""; 
    	ret += arrivalDay;
    	ret += "," + departureDay;
    	ret += "," + hotelValue;
    	ret += "," + event1Value;
    	ret += "," + event2Value;
    	ret += "," + event3Value;
    	return ret;
    }
}
