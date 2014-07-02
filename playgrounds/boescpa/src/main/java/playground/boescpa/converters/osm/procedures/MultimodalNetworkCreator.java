/*
 * *********************************************************************** *
 * project: org.matsim.*                                                   *
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
 * *********************************************************************** *
 */

package playground.boescpa.converters.osm.procedures;

import org.matsim.api.core.v01.network.Network;

/**
 * Creates a multimodal network from an osmFile.
 * Hereby "multimodal" is understood in the sense of
 * 	"private and public transport modes that use the street network".
 *
 * @author boescpa
 */
public abstract class MultimodalNetworkCreator {

	protected final Network network;

	protected MultimodalNetworkCreator(Network network) {
		this.network = network;
	}

	public final void createMultimodalNetwork(String osmFile) {
		Network carNetwork = createStreetNetwork(osmFile);
		Network ptNetwork = createPTNetwork(osmFile);
		mergePTwithCarNetwork(ptNetwork, carNetwork);
	}

	/**
	 * Create a standard car network from the given osmFile based on established OSM converters.
	 *
	 * @return standard car network
	 */
	protected abstract Network createStreetNetwork(String osmFile);

	/**
	 * Create a standard pt network from the given osmFile based on experimental OSM converters.
	 *
	 * @return standard pt network
	 */
	protected abstract Network createPTNetwork(String osmFile);

	/**
	 * Merge the two special networks to a new network. Thereby makes sure that the allowed modes
	 * for each link are merged and not replaced or substituted.
	 *
	 * The resulting, merged network is written into this.network.
	 *
	 * @param ptNetwork
	 * @param carNetwork
	 */
	protected abstract void mergePTwithCarNetwork(Network ptNetwork, Network carNetwork);

}