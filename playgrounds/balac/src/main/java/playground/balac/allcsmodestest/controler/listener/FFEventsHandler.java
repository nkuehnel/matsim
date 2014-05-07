package playground.balac.allcsmodestest.controler.listener;

import java.util.ArrayList;
import java.util.HashMap;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.LinkLeaveEvent;
import org.matsim.api.core.v01.events.PersonArrivalEvent;
import org.matsim.api.core.v01.events.PersonDepartureEvent;
import org.matsim.api.core.v01.events.handler.LinkLeaveEventHandler;
import org.matsim.api.core.v01.events.handler.PersonArrivalEventHandler;
import org.matsim.api.core.v01.events.handler.PersonDepartureEventHandler;
import org.matsim.api.core.v01.network.Network;

public class FFEventsHandler implements PersonArrivalEventHandler, PersonDepartureEventHandler, LinkLeaveEventHandler{
	HashMap<Id, ArrayList<RentalInfoFF>> ffRentalsStats = new HashMap<Id, ArrayList<RentalInfoFF>>();
	HashMap<Id, String> arrivals = new HashMap<Id, String>();
	ArrayList<RentalInfoFF> arr = new ArrayList<RentalInfoFF>();
	HashMap<Id, Boolean> inVehicle = new HashMap<Id, Boolean>();
	
	Network network;
	public FFEventsHandler(Network network) {
		
		this.network = network;
	}
	
	@Override
	public void reset(int iteration) {
		// TODO Auto-generated method stub
		ffRentalsStats = new HashMap<Id, ArrayList<RentalInfoFF>>();
		arrivals = new HashMap<Id, String>();
		arr = new ArrayList<RentalInfoFF>();
		inVehicle = new HashMap<Id, Boolean>();
		
	}

	@Override
	public void handleEvent(LinkLeaveEvent event) {
		Id perid = event.getVehicleId();
		if(inVehicle.get(perid) != null && inVehicle.get(perid)) {
			
			RentalInfoFF info = ffRentalsStats.get(perid).get(ffRentalsStats.get(perid).size() - 1);
			info.distance += network.getLinks().get(event.getLinkId()).getLength();
			
		}
		
	}

	@Override
	public void handleEvent(PersonDepartureEvent event) {
		// TODO Auto-generated method stub
		inVehicle.put(event.getPersonId(), false);
		if (event.getLegMode().equals("walk_ff")) {
			
			RentalInfoFF info = new RentalInfoFF();
			info.accessStartTime = event.getTime();
			info.personId = event.getPersonId();
			ArrayList<RentalInfoFF> temp1 = new ArrayList<RentalInfoFF>();
			if (ffRentalsStats.get(event.getPersonId()) == null) {
				
				temp1.add(info);
				ffRentalsStats.put(event.getPersonId(), temp1);

			}
			else {
				ffRentalsStats.get(event.getPersonId()).add(info);
				
			}
			
		}
		else if (event.getLegMode().equals("freefloating")) {
			inVehicle.put(event.getPersonId(), true);

			RentalInfoFF info = ffRentalsStats.get(event.getPersonId()).get(ffRentalsStats.get(event.getPersonId()).size() - 1);
			
			info.startTime = event.getTime();
			info.startLinkId = event.getLinkId();
			info.accessEndTime = event.getTime();
		}
		
	}

	@Override
	public void handleEvent(PersonArrivalEvent event) {
		// TODO Auto-generated method stub
		
		if (event.getLegMode().equals("freefloating")) {
			RentalInfoFF info = ffRentalsStats.get(event.getPersonId()).get(ffRentalsStats.get(event.getPersonId()).size() - 1);
			info.endTime = event.getTime();
			info.endLinkId = event.getLinkId();
			arr.add(info);

			
			
		}
		
	}
	
	public ArrayList<RentalInfoFF> rentals() {
		
		return arr;
	}
	
	public class RentalInfoFF {
		private Id personId = null;
		private double startTime = 0.0;
		private double endTime = 0.0;
		private Id startLinkId = null;
		private Id endLinkId = null;
		private double distance = 0.0;
		private double accessStartTime = 0.0;
		private double accessEndTime = 0.0;
		private double egressStartTime = 0.0;
		private double egressEndTime = 0.0;
		public String toString() {
			
			return personId + " " + Double.toString(startTime) + " " + Double.toString(endTime) + " " +
			startLinkId.toString() + " " +	endLinkId.toString()+ " " + Double.toString(distance)+ " " + Double.toString(accessEndTime - accessStartTime)+ " " + Double.toString(egressEndTime - egressStartTime);
		}
	}

}