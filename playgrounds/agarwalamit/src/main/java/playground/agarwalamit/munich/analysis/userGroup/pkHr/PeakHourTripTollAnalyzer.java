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
package playground.agarwalamit.munich.analysis.userGroup.pkHr;

import java.io.BufferedWriter;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.MatsimEventsReader;
import org.matsim.core.utils.collections.Tuple;
import org.matsim.core.utils.io.IOUtils;

import playground.agarwalamit.analysis.trip.TripTollHandler;
import playground.agarwalamit.munich.utils.ExtendedPersonFilter;
import playground.agarwalamit.utils.ListUitls;
import playground.agarwalamit.utils.LoadMyScenarios;
import playground.benjamin.scenarios.munich.analysis.filter.UserGroup;

/**
 * @author amit
 */

public class PeakHourTripTollAnalyzer {

	public PeakHourTripTollAnalyzer(double simulationEndTime, int noOfTimeBins) {
		log.warn("Peak hours are assumed as 07:00-10:00 and 15:00-18:00 by looking on the travel demand for BAU scenario.");
		this.tollHandler = new TripTollHandler( simulationEndTime, noOfTimeBins );
	} 

	private static final Logger log = Logger.getLogger(PeakHourTripTollAnalyzer.class);
	private TripTollHandler tollHandler ;

	private final List<Double> pkHrs = new ArrayList<>(Arrays.asList(new Double []{8., 9., 10., 16., 17., 18.,})); // => 7-10 and 15-18
	private final ExtendedPersonFilter pf = new ExtendedPersonFilter();
	private Map<Id<Person>,List<Double>> person2Tolls_pkHr = new HashMap<>();
	private Map<Id<Person>,List<Double>> person2Tolls_offPkHr = new HashMap<>();
	private Map<Id<Person>,Integer> person2TripCounts_pkHr = new HashMap<>();
	private Map<Id<Person>,Integer> person2TripCounts_offPkHr = new HashMap<>();
	private SortedMap<String, Tuple<Double,Double>> usrGrp2Tolls = new TreeMap<>();
	private SortedMap<String, Tuple<Integer,Integer>> usrGrp2TripCounts = new TreeMap<>();

	public static void main(String[] args) {
		String [] pricingSchemes = new String [] {"ei","ci","eci"};
		for (String str :pricingSchemes) {
			String dir = "../../../../repos/runs-svn/detEval/emissionCongestionInternalization/iatbr/output/";
			String eventsFile = dir+str+"/ITERS/it.1500/1500.events.xml.gz";
			String configFile = dir+str+"/output_config.xml.gz";

			PeakHourTripTollAnalyzer tda = new PeakHourTripTollAnalyzer(LoadMyScenarios.getSimulationEndTime(configFile), 30);
			tda.run(eventsFile);
			tda.writeTripData(dir+"/analysis/", str);
			tda.writeRBoxPlotData(dir+"/analysis/", str);
		}
	}

	public void run(String eventsFile) {
		EventsManager events = EventsUtils.createEventsManager();
		MatsimEventsReader reader = new MatsimEventsReader(events);
		events.addHandler(this.tollHandler);
		reader.readFile(eventsFile);
		splitDataInPeakOffPeakHours();
		storeUserGroupData();
	}

	public void writeRBoxPlotData(String outputFolder, String pricingScheme) {
		if( ! new File(outputFolder+"/boxPlot/").exists()) new File(outputFolder+"/boxPlot/").mkdirs();

		BufferedWriter writer = IOUtils.getBufferedWriter(outputFolder+"/boxPlot/toll_"+pricingScheme+"_pkHr"+".txt");
		try {
			for(Id<Person> p : person2Tolls_pkHr.keySet()){
				String ug = pf.getMyUserGroupFromPersonId(p);
				for(double d: person2Tolls_pkHr.get(p)){
					writer.write(pricingScheme.toUpperCase()+"\t"+ ug+"\t"+d+"\n");
				}
			}
			writer.close();
		} catch (Exception e) {
			throw new RuntimeException("Data is not written in file. Reason: " + e);
		}

		//write off peak hour toll/trip
		writer = IOUtils.getBufferedWriter(outputFolder+"/boxPlot/toll_"+pricingScheme+"_offPkHr"+".txt");
		try {
			for(Id<Person> p : person2Tolls_offPkHr.keySet()){
				String ug = pf.getMyUserGroupFromPersonId(p);
				for(double d: person2Tolls_offPkHr.get(p)){
					writer.write(pricingScheme.toUpperCase()+"\t"+ ug+"\t"+d+"\n");
				}
			}
			writer.close();
		} catch (Exception e) {
			throw new RuntimeException("Data is not written in file. Reason: " + e);
		}
	}

	public void writeTripData(String outputFolder, String pricingScheme){
		BufferedWriter writer = IOUtils.getBufferedWriter(outputFolder+"/userGrp_tripToll_"+pricingScheme+".txt");
		try {
			writer.write("userGroup \t peakHrTotalTollPerTrip \t offPeakHrTotalTollPerTrip \t peakHrAvgTollPerTrip \t offPkHrAvgTollPerTrip \t peakHrTripCount \t offPeakHrTripCount \n");
			for(String ug:this.usrGrp2Tolls.keySet()){
				writer.write(ug+"\t"+this.usrGrp2Tolls.get(ug).getFirst()+"\t"+this.usrGrp2Tolls.get(ug).getSecond()+"\t"
						+( this.usrGrp2Tolls.get(ug).getFirst()/this.usrGrp2TripCounts.get(ug).getFirst() )+"\t"+(this.usrGrp2Tolls.get(ug).getSecond()/this.usrGrp2TripCounts.get(ug).getSecond())+"\t"
						+this.usrGrp2TripCounts.get(ug).getFirst()+"\t"+this.usrGrp2TripCounts.get(ug).getSecond()+"\n");
			}
			writer.close();
		} catch (Exception e) {
			throw new RuntimeException("Data is not written. Reason "+e);
		}
	}

	private void storeUserGroupData(){
		for(UserGroup ug : UserGroup.values()){
			usrGrp2Tolls.put(pf.getMyUserGroup(ug), new Tuple<Double, Double>(0., 0.));
			usrGrp2TripCounts.put(pf.getMyUserGroup(ug), new Tuple<Integer, Integer>(0, 0));
		}
		//first store peak hour data
		for (Id<Person> personId : this.person2Tolls_pkHr.keySet()) {
			String ug = pf.getMyUserGroupFromPersonId(personId);
			double pkToll = usrGrp2Tolls.get(ug).getFirst() + ListUitls.doubleSum(this.person2Tolls_pkHr.get(personId));
			int pkTripCount = usrGrp2TripCounts.get(ug).getFirst() + this.person2TripCounts_pkHr.get(personId);
			usrGrp2Tolls.put(ug, new Tuple<Double, Double>(pkToll, 0.));
			usrGrp2TripCounts.put(ug, new Tuple<Integer,Integer>(pkTripCount,0) );
		}

		//now store off-peak hour data
		for (Id<Person> personId : this.person2Tolls_offPkHr.keySet()) {
			String ug = pf.getMyUserGroupFromPersonId(personId);
			double offpkToll = usrGrp2Tolls.get(ug).getSecond() + ListUitls.doubleSum(this.person2Tolls_offPkHr.get(personId));
			int offpkTripCount = usrGrp2TripCounts.get(ug).getSecond() + this.person2TripCounts_offPkHr.get(personId);
			usrGrp2Tolls.put(ug, new Tuple<Double, Double>(usrGrp2Tolls.get(ug).getFirst(), offpkToll));
			usrGrp2TripCounts.put(ug, new Tuple<Integer,Integer>(usrGrp2TripCounts.get(ug).getFirst(),offpkTripCount) );
		}
	}

	private void splitDataInPeakOffPeakHours() {
		SortedMap<Double, Map<Id<Person>, List<Double>>> timebin2person2tripToll = this.tollHandler.getTimeBin2Person2TripToll();
		SortedMap<Double, Map<Id<Person>, Integer>> timebin2person2tripCounts = this.tollHandler.getTimeBin2Person2TripsCount();

		for(double d :timebin2person2tripToll.keySet()) {
			for (Id<Person> person : timebin2person2tripToll.get(d).keySet()) {
				if(pkHrs.contains(d)) {
					if (person2Tolls_pkHr.containsKey(person) ) {
						List<Double> tolls = person2Tolls_pkHr.get(person);
						tolls.addAll(timebin2person2tripToll.get(d).get(person));
						person2TripCounts_pkHr.put(person, timebin2person2tripCounts.get(d).get(person) + person2TripCounts_pkHr.get(person));
					} else {
						person2Tolls_pkHr.put(person,  timebin2person2tripToll.get(d).get(person));
						person2TripCounts_pkHr.put(person, timebin2person2tripCounts.get(d).get(person));
					}
				} else {
					if (person2Tolls_offPkHr.containsKey(person) ) {
						List<Double> tolls =  person2Tolls_offPkHr.get(person);
						tolls.addAll(timebin2person2tripToll.get(d).get(person));
						person2TripCounts_offPkHr.put(person, timebin2person2tripCounts.get(d).get(person) + person2TripCounts_offPkHr.get(person));
					} else {
						person2Tolls_offPkHr.put(person,  timebin2person2tripToll.get(d).get(person));
						person2TripCounts_offPkHr.put(person, timebin2person2tripCounts.get(d).get(person));
					}
				}
			}
		}
	}
}
