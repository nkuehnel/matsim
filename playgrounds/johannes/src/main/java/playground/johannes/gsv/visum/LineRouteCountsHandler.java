/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2014 by the members listed in the COPYING,        *
 *                   LICENSE and WARRANTY file.                            *
 * email           : info at matsim dot org                                *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 *   This program is free software; you can redistribute it and/or modify  *
 *   it under the terms of the GNU General Public License as published by  *
 *   the Free Software Foundation; either version 2 of the License, or     *
 *   (at your option) any later version.                                   *
 *   See also COPYING, LICENSE and WARRANTY file                           *
 *                                                                         *
 * *********************************************************************** */

/**
 * 
 */
package playground.johannes.gsv.visum;

import gnu.trove.TObjectDoubleHashMap;

import java.util.Map;

import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.network.NetworkUtils;

import playground.johannes.gsv.visum.NetFileReader.TableHandler;

/**
 * @author johannes
 *
 */
public class LineRouteCountsHandler extends TableHandler {

	public static final String NODE_KEY = "KNOTNR";
	
	public static final String COUNTS_KEY = "SUMAKTIVE:BENUTZENDEFZPELEMENTE\\SUMAKTIVE:FAHRPLANFAHRTELEMENTE\\BESETZ";
	
	public static final String LINE_KEY = "LINNAME";
	
	public static final String ROUTE_KEY = "LINROUTENAME";
	
	public static final String DCODE_KEY = "RICHTUNGCODE";
	
	public static final String INDEX_KEY = "INDEX";
	
	private TObjectDoubleHashMap<Link> counts;
	
	private String lastLine;
	
	private String lastRoute;
	
	private String lastDCode;
	
	private int lastIndex;
	
	private Node fromNode;
	
	private Network network;
	
	private IdGenerator idGenerator;
	
	/* (non-Javadoc)
	 * @see playground.johannes.gsv.visum.NetFileReader.TableHandler#handleRow(java.util.Map)
	 */
	@Override
	public void handleRow(Map<String, String> record) {
		int index = Integer.parseInt(record.get(INDEX_KEY));
		
		if(index - 1 == lastIndex) {
			lastIndex = index;
			
			String line = record.get(LINE_KEY);
			if(!line.equals(lastLine))
				throw new RuntimeException("Line name does not match.");
			
			String route = record.get(ROUTE_KEY);
			if(!route.equals(lastRoute))
				throw new RuntimeException("Route name does not match.");
			
			String dcode = record.get(DCODE_KEY);
			if(!dcode.equals(lastDCode))
				throw new RuntimeException("Direction does not match.");
			
			Node toNode = network.getNodes().get(idGenerator.generateId(record.get(NODE_KEY)));
			Link link = NetworkUtils.getConnectingLink(fromNode, toNode);
			
			if(link == null) {
				throw new RuntimeException("Link not found.");
			}
			
		} else {
			//new line
		}
		

	}

}