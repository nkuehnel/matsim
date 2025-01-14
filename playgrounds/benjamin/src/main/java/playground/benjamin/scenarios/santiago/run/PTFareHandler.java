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

package playground.benjamin.scenarios.santiago.run;

import java.util.HashSet;
import java.util.Set;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.events.ActivityStartEvent;
import org.matsim.api.core.v01.events.PersonArrivalEvent;
import org.matsim.api.core.v01.events.PersonDepartureEvent;
import org.matsim.api.core.v01.events.PersonEntersVehicleEvent;
import org.matsim.api.core.v01.events.PersonMoneyEvent;
import org.matsim.api.core.v01.events.handler.ActivityStartEventHandler;
import org.matsim.api.core.v01.events.handler.PersonArrivalEventHandler;
import org.matsim.api.core.v01.events.handler.PersonDepartureEventHandler;
import org.matsim.api.core.v01.events.handler.PersonEntersVehicleEventHandler;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.controler.Controler;
import org.matsim.vehicles.Vehicle;
import org.matsim.vehicles.Vehicles;

import playground.benjamin.scenarios.santiago.SantiagoScenarioConstants;
import playground.benjamin.scenarios.santiago.population.SantiagoScenarioBuilder;

public class PTFareHandler implements PersonDepartureEventHandler, PersonEntersVehicleEventHandler {
	private static final Logger log = Logger.getLogger(PersonDepartureEventHandler.class);

	private final Controler controler;
	
	/*
	 * The full fare scheme is available under http://www.transantiago.cl/tarifas-y-pagos/conoce-las-tarifas.
	 * TODO: Keep track of interchanges so a reduced/no additional fare is charged.
	 * TODO: Integrate studentSeniorFare?
	 */
	private final double peakFare = -720.;
	private final double intermediateFare = -660.;
	private final double offPeakFare = -640.;
	
	private final double studentSeniorFare = -210.;
	
	private final double startIntermediateTimeMorning = 6.5 * 3600;
//	private final double endIntermediateTimeMorning = (7.0 * 3600) - 1;
//	private final double startIntermediateTimeDay = 9.0 * 3600;
//	private final double endIntermediateTimeDay = (18.0 * 3600) - 1;
//	private final double startIntermediateTimeEvening = 20.0 * 3600;
	private final double endIntermediateTimeEvening = (20.75 * 3600) - 1;
	
	private final double startPeakTimeMorning = 7.0 * 3600;
	private final double endPeakTimeMorning = (9.0 * 3600) - 1;
	private final double startPeakTimeEvening = 18.0 * 3600;
	private final double endPeakTimeEvening = (20.0 * 3600) - 1;

	private Set<String> ptModes;
	private boolean doModeChoice;
	private Vehicles transitVehicles;
	private Population population;
	
	
	public PTFareHandler(final Controler controler, boolean doModeChoice, Population population){
		this.controler = controler;
		this.doModeChoice = doModeChoice;
		if(this.doModeChoice){
			this.transitVehicles = controler.getScenario().getTransitVehicles();
			this.population = population;
		} else {
			this.ptModes = definePtModes();
		}
	}

	@Override
	public void reset(int iteration) {
		//nothing to do
	}
	
	//only possible if mode choice is on (i.e. transit vehicles are there)
	@Override
	public void handleEvent(PersonEntersVehicleEvent event) {
		if(this.doModeChoice){ //only if mode choice is on (i.e. transit vehicles are there)
			double time = event.getTime();
			Id<Person> pid = event.getPersonId();
			Id<Vehicle> vid = event.getVehicleId();
			if(vid == null || vid.equals("")){
				log.warn("Vehicle is not properly defined; cannot prove if it is a PT vehicle. Hence, no fare is collected from person " + pid);
			}
			if(this.transitVehicles.getVehicles().get(vid) == null){
				// person is entering a non-transit vehicle; no fare to pay.
			} else {
				if(population.getPersons().get(pid) == null){
					//this is a TransitDriverAgent who should not pay fare!
				} else {
					double fare = getTimedependentFare(time);
					this.controler.getEvents().processEvent(new PersonMoneyEvent(time, pid, fare));
				}
			}
		}
	}
	
	@Override
	public void handleEvent(PersonDepartureEvent event) {
		if(!this.doModeChoice){ //only if mode choice is off (i.e. teleported pt is on)
			double time = event.getTime();
			Id<Person> id = event.getPersonId();
			String legMode = event.getLegMode();
			if(this.ptModes.contains(legMode)){
				double fare = getTimedependentFare(time);
				this.controler.getEvents().processEvent(new PersonMoneyEvent(time, id, fare));
			}
		}
	}
	
	private double getTimedependentFare(double time) {
		double fare = offPeakFare;
		if(time >= startPeakTimeMorning && time <= endPeakTimeMorning){
			fare = peakFare;
		} else if(time >= startPeakTimeEvening && time <= endPeakTimeEvening){
			fare = peakFare;
		} else if(time >= startIntermediateTimeMorning && time <= endIntermediateTimeEvening){
			fare = intermediateFare;
		}
		return fare;
	}

	private Set<String> definePtModes() {
		Set<String> ptModes = new HashSet<String>();
//		ptModes.add(TransportMode.pt);
		ptModes.add(SantiagoScenarioConstants.Modes.bus.toString());
		ptModes.add(SantiagoScenarioConstants.Modes.metro.toString());
		ptModes.add(SantiagoScenarioConstants.Modes.train.toString());
		return ptModes;
	}
}