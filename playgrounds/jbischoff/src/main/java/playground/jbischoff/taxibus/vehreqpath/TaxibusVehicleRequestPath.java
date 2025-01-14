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

package playground.jbischoff.taxibus.vehreqpath;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.TreeSet;

import org.matsim.api.core.v01.network.Link;
import org.matsim.contrib.dvrp.data.Requests;
import org.matsim.contrib.dvrp.data.Vehicle;
import org.matsim.contrib.dvrp.path.VrpPathWithTravelData;

import playground.jbischoff.taxibus.passenger.TaxibusRequest;
import playground.jbischoff.taxibus.passenger.TaxibusRequest.TaxibusRequestStatus;


public class TaxibusVehicleRequestPath implements Comparable<TaxibusVehicleRequestPath>
{
    public final Vehicle vehicle;
    public final Set<TaxibusRequest> requests;
    public final ArrayList<VrpPathWithTravelData> path;
    private Double t0 = null;
    private Double arrivalTime = null;
    private Link initialDestination = null;
    
    public TaxibusVehicleRequestPath(Vehicle vehicle, TaxibusRequest request, VrpPathWithTravelData path)
    {
    	this.requests = new LinkedHashSet<>();
    	this.path = new ArrayList<>();
    	this.requests.add(request);
        this.vehicle = vehicle;
        this.path.add(path);
        this.t0 = request.getT0();
        this.arrivalTime = path.getArrivalTime();
        this.initialDestination = request.getToLink();
    }
    public TaxibusVehicleRequestPath(Vehicle vehicle, Set<TaxibusRequest> requests, ArrayList<VrpPathWithTravelData> path)
    {
    	this.requests = requests;
    	this.path = path;
        this.vehicle = vehicle;
        
    
    }
	public Double getT0() {
		if (t0 == null){
			throw new IllegalStateException("Only initial requests paths have a distinct t0.");
		}
		return t0;
	}
	public Double getArrivalTime() {
		if (arrivalTime == null){
			throw new IllegalStateException("Only initial requests paths have a distinct arrivalTime.");
		}

		return arrivalTime;
	}
	public Link getInitialDestination() {
		if (initialDestination== null){
			throw new IllegalStateException("Only initial requests paths have a distinct first request destination.");
		}
		return initialDestination;
	}
	
	public void failIfAnyRequestNotUnplanned(){
		for (TaxibusRequest request : this.requests){
			if (request.getStatus() != TaxibusRequestStatus.UNPLANNED) {
	            throw new IllegalStateException();
	        }
		}
	}
	
	
	@Override
	public int compareTo(TaxibusVehicleRequestPath arg0) {

		return t0.compareTo(arg0.getT0());
	}
	
    public TreeSet<TaxibusRequest> getPickUpsForLink(Link link){
    	TreeSet<TaxibusRequest> beginningRequests = new TreeSet<>(Requests.ABSOLUTE_COMPARATOR);
    	for (TaxibusRequest req : this.requests){
    		if (req.getFromLink().equals(link)){
    			beginningRequests.add(req);
    		}
    	}
    	
    	return beginningRequests.isEmpty() ? null : beginningRequests ;
    }
    
    public TreeSet<TaxibusRequest> getDropOffsForLink(Link link){
    	TreeSet<TaxibusRequest> endingRequests = new TreeSet<>(Requests.ABSOLUTE_COMPARATOR);
    	for (TaxibusRequest req : this.requests){
    		if (req.getToLink().equals(link)){
    			
    			endingRequests.add(req);
    		}
    	}
    	
    	return endingRequests.isEmpty() ? null : endingRequests ;
    }
    
    
    
    
    
    
    
    
    
    
    
}