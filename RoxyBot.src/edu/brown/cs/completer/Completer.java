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
 * Completer
 *
 * Author  : Victor Naroditskiy
 * Created : 1 January, 2007
 */

package edu.brown.cs.completer;

import props.Misc;
import edu.brown.cs.props.CPLEXUser;
import edu.brown.cs.tacds.Completion;

public class Completer extends CPLEXUser {
	public Completer () {
		super();
	}
	
	public Completion run () {
		setConstants();
		setDecisionVariables();
		setObjectiveFunction();
		setConstraints();
		calculate();
		return writeOutput();
	}
	
	protected void setConstants () {
		
	}
	
	protected void setDecisionVariables () {
		
	}
	
	protected void setObjectiveFunction () {
		
	}
	
	protected void setConstraints () {
		
	}
	
	protected void calculate () { try {
		long startTime = System.currentTimeMillis();
		Misc.myassert(m_cplex.solve());
		long finishTime = System.currentTimeMillis();
		
		Misc.println("Completer.calculate :" +
				" sec " + (finishTime - startTime) / 1000.0);
	} catch (Exception e) { Misc.error(e.getMessage()); }
	}
	
	protected Completion writeOutput () {
		return null;
	}
}
