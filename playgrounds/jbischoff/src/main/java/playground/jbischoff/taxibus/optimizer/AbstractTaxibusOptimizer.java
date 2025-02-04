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

package playground.jbischoff.taxibus.optimizer;

import java.util.Collection;
import java.util.List;
import java.util.TreeSet;

import org.matsim.contrib.dvrp.data.Request;
import org.matsim.contrib.dvrp.data.Requests;
import org.matsim.contrib.dvrp.data.Vehicle;
import org.matsim.contrib.dvrp.schedule.DriveTask;
import org.matsim.contrib.dvrp.schedule.Schedule;
import org.matsim.contrib.dvrp.schedule.Task;
import org.matsim.core.mobsim.framework.events.MobsimBeforeSimStepEvent;

import playground.jbischoff.taxibus.passenger.TaxibusRequest;
import playground.jbischoff.taxibus.scheduler.TaxibusTask;
import playground.jbischoff.taxibus.scheduler.TaxibusTask.TaxibusTaskType;


public abstract class AbstractTaxibusOptimizer
    implements TaxibusOptimizer
{
    protected final TaxibusOptimizerConfiguration optimConfig;
    protected final Collection<TaxibusRequest> unplannedRequests;

    private final boolean doUnscheduleAwaitingRequests;//PLANNED or TAXI_DISPATCHED
    private final boolean destinationKnown;
    private final boolean vehicleDiversion;

    protected boolean requiresReoptimization = false;


    public AbstractTaxibusOptimizer(TaxibusOptimizerConfiguration optimConfig,
             boolean doUnscheduleAwaitingRequests)
    {
        this.optimConfig = optimConfig;
        this.unplannedRequests = new TreeSet<TaxibusRequest>(Requests.ABSOLUTE_COMPARATOR);
       	this.doUnscheduleAwaitingRequests=doUnscheduleAwaitingRequests;

        destinationKnown = optimConfig.scheduler.getParams().destinationKnown;
        vehicleDiversion = optimConfig.scheduler.getParams().vehicleDiversion;
    }


    @Override
    public void notifyMobsimBeforeSimStep(@SuppressWarnings("rawtypes") MobsimBeforeSimStepEvent e)
    {
    	if (!unplannedRequests.isEmpty()) { requiresReoptimization = true;
    	}
    	
        if (requiresReoptimization) {
            if (doUnscheduleAwaitingRequests) {
                unscheduleAwaitingRequests();
            }

            for (Vehicle v : optimConfig.context.getVrpData().getVehicles().values()) {
                optimConfig.scheduler.updateTimeline((Schedule<TaxibusTask>) v.getSchedule());
            }

            scheduleUnplannedRequests();
            
            if (doUnscheduleAwaitingRequests && vehicleDiversion) {
                handleAimlessDriveTasks();
            }
            
            requiresReoptimization = false;
        }
    }


    protected void unscheduleAwaitingRequests()
    {
        List<TaxibusRequest> removedRequests = optimConfig.scheduler
                .removeAwaitingRequestsFromAllSchedules();
        unplannedRequests.addAll(removedRequests);
    }


    protected abstract void scheduleUnplannedRequests();

    
    protected void handleAimlessDriveTasks()
    {
        optimConfig.scheduler.stopAllAimlessDriveTasks();
    }
    

    @Override
    public void requestSubmitted(Request request)
    {
        unplannedRequests.add((TaxibusRequest)request);
        requiresReoptimization = true;
    }


    @Override
    public void nextTask(Schedule<? extends Task> schedule)
    {
        Schedule<TaxibusTask> taxiSchedule = (Schedule<TaxibusTask>) schedule;
        optimConfig.scheduler.updateBeforeNextTask(taxiSchedule);

        TaxibusTask newCurrentTask = taxiSchedule.nextTask();

        if (!requiresReoptimization && newCurrentTask != null) {// schedule != COMPLETED
            requiresReoptimization = doReoptimizeAfterNextTask(newCurrentTask);
        }
    }


    protected boolean doReoptimizeAfterNextTask(TaxibusTask newCurrentTask)
    {
        return !destinationKnown
                && newCurrentTask.getTaxibusTaskType() == TaxibusTaskType.DRIVE_WITH_PASSENGER;
    }


    @Override
    public void nextLinkEntered(DriveTask driveTask)
    {
        optimConfig.scheduler.updateTimeline((Schedule<TaxibusTask>) driveTask.getSchedule());

        //TODO we may here possibly decide whether or not to reoptimize
        //if (delays/speedups encountered) {requiresReoptimization = true;}
    }
}
