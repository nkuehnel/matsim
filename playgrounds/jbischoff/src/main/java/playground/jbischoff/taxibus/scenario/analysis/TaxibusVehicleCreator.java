/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2015 by the members listed in the COPYING,        *
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

package playground.jbischoff.taxibus.scenario.analysis;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.contrib.dvrp.data.Vehicle;
import org.matsim.contrib.dvrp.data.VehicleImpl;
import org.matsim.contrib.dvrp.data.file.VehicleWriter;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.network.NetworkImpl;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.geotools.MGC;

import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.Point;

import playground.jbischoff.taxi.berlin.demand.TaxiDemandWriter;

/**
 * @author  jbischoff
 *
 */
public class TaxibusVehicleCreator
	{


	private String networkFile = "C:/Users/Joschka/Documents/shared-svn/projects/vw_rufbus/scenario/network/network_nopt.xml";
	private String vehiclesFilePrefix = "C:/Users/Joschka/Documents/shared-svn/projects/vw_rufbus/scenario/input/taxibus_vehicles_";
	
	private Scenario scenario ;
	private Random random = MatsimRandom.getRandom();
    private List<Vehicle> vehicles = new ArrayList<>();

	
	public static void main(String[] args) {
		TaxibusVehicleCreator tvc = new TaxibusVehicleCreator();
		for (int i = 100; i<1501 ; i=i+200 ){
			System.out.println(i);
			tvc.run(i);
		}
}

	public TaxibusVehicleCreator() {
				
		this.scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		new MatsimNetworkReader(scenario).readFile(networkFile);
	}
	private void run(int amount) {
	    
		for (int i = 0 ; i< amount; i++){
		double x = 593084 + random.nextDouble()*(629810-593084);
		double y = 5785583 + random.nextDouble()*(5817600-5785583);
		Coord c = new Coord(x,y);
		Link link = ((NetworkImpl) scenario.getNetwork()).getNearestLinkExactly(c);
        Vehicle v = new VehicleImpl(Id.create("tb"+i, Vehicle.class), link, 8, Math.round(1), Math.round(48*3600));
        vehicles.add(v);

		}
		new VehicleWriter(vehicles).write(vehiclesFilePrefix+amount+".xml.gz");
	}

	
}
