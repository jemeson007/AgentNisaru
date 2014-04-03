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
 * CPLEXUser
 *
 * Author  : Victor Naroditskiy
 * Created : 1 January, 2007
 * Description:
 * 
 */

package edu.brown.cs.props;

import props.Misc;
import ilog.cplex.IloCplex;

//do not create a new cplex object every time you need it as that may take some time
//extend this class if you want to use cplex
public class CPLEXUser {
	protected static IloCplex m_cplex;
	private static boolean m_cplexInitialized=false;
		
	public CPLEXUser() {
		if (!m_cplexInitialized) {
			try{
				Misc.warn("IloCplex created");
				m_cplex = new IloCplex();
				m_cplexInitialized=true;//helps us avoid memory leaks
			}catch(Exception e) {
				Misc.error("could not initialize cplex");
				e.printStackTrace();
				Misc.myassert(false);
			}
		}
	}

}
