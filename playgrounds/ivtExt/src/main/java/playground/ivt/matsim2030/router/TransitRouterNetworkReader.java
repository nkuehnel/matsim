/* *********************************************************************** *
 * project: org.matsim.*
 * TransitRouterNetworkReader.java
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
package playground.ivt.matsim2030.router;

import java.util.Map;
import java.util.Stack;

import org.matsim.api.core.v01.Id;
import org.matsim.core.api.experimental.IdFactory;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.utils.io.MatsimXmlParser;
import org.matsim.core.utils.misc.Counter;
import org.matsim.pt.router.TransitRouterNetwork;
import org.matsim.pt.router.TransitRouterNetwork.TransitRouterNetworkLink;
import org.matsim.pt.router.TransitRouterNetwork.TransitRouterNetworkNode;
import org.matsim.pt.transitSchedule.api.TransitLine;
import org.matsim.pt.transitSchedule.api.TransitRoute;
import org.matsim.pt.transitSchedule.api.TransitRouteStop;
import org.matsim.pt.transitSchedule.api.TransitSchedule;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;

import org.xml.sax.Attributes;

/**
 * This is copied from ph's playground, to avoid depending on it
 * and have some control over it, in particular put the dtd in jar
 * @author thibautd
 */
public class TransitRouterNetworkReader extends MatsimXmlParser {

	private final static String NETWORK = "transitRouterNetwork";
	private final static String LINKS = "links";
	private final static String NODE = "node";
	private final static String NODES = "nodes";
	private final static String LINK = "link";

	private final IdFactory idFactory;
	private final TransitRouterNetwork network;
	private final TransitSchedule transitSchedule;

	private final Counter nodesCounter = new Counter("# read nodes: ");
	private final Counter linksCounter = new Counter("# read links: ");
	
	public TransitRouterNetworkReader(
			final IdFactory idFactory,
			final TransitSchedule schedule,
			final TransitRouterNetwork network) {
		this.idFactory = idFactory;
		this.network = network;
		this.transitSchedule = schedule;
	}

	@Override
	public void startTag(final String name, final Attributes atts, final Stack<String> context) {
		
		if (NODE.equals(name)) {
			startNode(atts);
		} else if (LINK.equals(name)) {
			startLink(atts);
		} else if (NETWORK.equals(name)) {
			startNetwork();
		}
	}

	@Override
	public void endTag(final String name, final String content, final Stack<String> context) {

		// If the entire network is read, we have to call finishInit().
		if (NETWORK.equals(name)) {
			network.finishInit();
		} else if (NODE.equals(name)) {
			nodesCounter.incCounter();
		} else if (NODES.equals(name)) {
			nodesCounter.printCounter();
		} else if (LINK.equals(name)) {
			linksCounter.incCounter();
		} else if (LINKS.equals(name)) {
			linksCounter.printCounter();
		}
	}

	private void startNetwork() {
		nodesCounter.reset();
		linksCounter.reset();
	}

	@SuppressWarnings("unchecked")
	private void startNode(final Attributes atts) {	
		
		Id nodeId = this.idFactory.createId(atts.getValue("id"));
		Id stopId = this.idFactory.createId(atts.getValue("stopfacility"));
		Id routeId = this.idFactory.createId(atts.getValue("route"));
		Id lineId = this.idFactory.createId(atts.getValue("line"));
		
		TransitLine line = this.transitSchedule.getTransitLines().get(lineId);
		TransitRoute route = line.getRoutes().get(routeId);
		TransitStopFacility stopFacility = this.transitSchedule.getFacilities().get(stopId);
		TransitRouteStop stop = route.getStop(stopFacility);
		
		TransitRouterNetworkNode node = new TransitRouterNetworkNode(nodeId, stop, route, line);
		((Map<Id, TransitRouterNetworkNode>) network.getNodes()).put(nodeId, node);
	}
	
	@SuppressWarnings("unchecked")
	private void startLink(final Attributes atts) {
		
//		Id linkId = this.scenario.createId(atts.getValue("id"));
//		Id fromId = this.scenario.createId(atts.getValue("from"));
//		Id toId = this.scenario.createId(atts.getValue("to"));
		Id linkId = new IdImpl(atts.getValue("id"));
		Id fromId = new IdImpl(atts.getValue("from"));
		Id toId = new IdImpl(atts.getValue("to"));
		
		String string = null;
		Id routeId = null;
		Id lineId = null;
		
		string = atts.getValue("route");
//		if (string != null) routeId = this.scenario.createId(string);
		if (string != null) routeId = new IdImpl(string);
		
		string = atts.getValue("line");
//		if (string != null) lineId = this.scenario.createId(string);
		if (string != null) lineId = new IdImpl(string);
		
		TransitLine line = null;
		TransitRoute route = null;
		if (lineId != null) {
			line = this.transitSchedule.getTransitLines().get(lineId);
			route = null;
			if (line != null) route = line.getRoutes().get(routeId);			
		}
		
		TransitRouterNetworkNode fromNode = this.network.getNodes().get(fromId);
		TransitRouterNetworkNode toNode = this.network.getNodes().get(toId);
		if (fromNode == null) {
			throw new RuntimeException("FromNode " + fromId + " was not found!");			
		} else if (toNode == null) {
			throw new RuntimeException("ToNode " + fromId + " was not found!");
		}
		
		double length = Double.parseDouble(atts.getValue("length"));
		TransitRouterNetworkLink link = new TransitRouterNetworkLink(linkId, fromNode, toNode, route, line, length);
		
		((Map<Id, TransitRouterNetworkLink>) network.getLinks()).put(linkId, link);
		((Map<Id, TransitRouterNetworkLink>) fromNode.getOutLinks()).put(link.getId(), link);
		((Map<Id, TransitRouterNetworkLink>) toNode.getInLinks()).put(link.getId(), link);
	}
}