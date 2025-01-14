/* *********************************************************************** *
 * project: org.matsim.*
 * EventsToExperiencedPlans.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2013 by the members listed in the COPYING,        *
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
package playground.thibautd.scripts;

import java.util.Collection;
import java.util.Iterator;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PopulationWriter;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.MatsimEventsReader;
import org.matsim.core.population.ActivityImpl;
import org.matsim.core.population.MatsimPopulationReader;
import org.matsim.core.router.EmptyStageActivityTypes;
import org.matsim.core.router.TripStructureUtils;
import org.matsim.core.scenario.ScenarioUtils;

import playground.thibautd.utils.EventsToPlans;

/**
 * @author thibautd
 */
public class EventsToExperiencedPlans {
	public static void main(final String[] args) {
		final String eventsFile = args[ 0 ];
		final String inPopFile = args[ 1 ];
		final String outputPlansFile = args[ 2 ];

		final Scenario inputSc = ScenarioUtils.createScenario( ConfigUtils.createConfig() );
		new MatsimPopulationReader( inputSc ).parse( inPopFile );

		final EventsManager events = EventsUtils.createEventsManager();
		final EventsToPlans eventsToPlans =
			new EventsToPlans(
					new EventsToPlans.IdFilter() {
						@Override
						public boolean accept(final Id id) {
							return inputSc.getPopulation().getPersons().containsKey( id );
						}
					});
		events.addHandler( eventsToPlans );

		new MatsimEventsReader( events ).readFile( eventsFile );

		final Scenario sc = ScenarioUtils.createScenario( ConfigUtils.createConfig() );

		for ( final Plan plan : eventsToPlans.getPlans().values() ) {
			plan.getPerson().addPlan( plan );
			transmitCoordinates( inputSc.getPopulation().getPersons().get( plan.getPerson().getId() ) , plan );
			sc.getPopulation().addPerson( plan.getPerson() );
		}

		new PopulationWriter( sc.getPopulation() , sc.getNetwork() ).write( outputPlansFile );
	}

	private static void transmitCoordinates(
			final Person person,
			final Plan plan) {
		final Collection<Activity> originalActivities = TripStructureUtils.getActivities( person.getSelectedPlan() , EmptyStageActivityTypes.INSTANCE );
		final Collection<Activity> newActivities = TripStructureUtils.getActivities( plan , EmptyStageActivityTypes.INSTANCE );

		assert newActivities.size() <= originalActivities.size();

		final Iterator<Activity> origIterator = originalActivities.iterator();
		final Iterator<Activity> newIterator = newActivities.iterator();

		while ( newIterator.hasNext() ) {
			final Activity origAct = origIterator.next();
			final Activity newAct = newIterator.next();

			((ActivityImpl) newAct).setCoord( origAct.getCoord() );
		}
	}
}

