/* *********************************************************************** *
 * project: org.matsim.*
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
package playground.thibautd.maxess.prepareforbiogeme.tripbased;

import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.router.TripRouter;
import org.matsim.facilities.ActivityFacility;
import playground.thibautd.maxess.prepareforbiogeme.framework.ChoiceSet;
import playground.thibautd.maxess.prepareforbiogeme.framework.ChoiceSetSampler;

import java.util.*;

/**
 * @author thibautd
 */
public class RoutingChoiceSetSampler implements ChoiceSetSampler<Trip,TripChoiceSituation> {
	private final TripRouter router;
	private final Set<String> modes;
	private final DestinationSampler destinationSampler;

	public RoutingChoiceSetSampler(
			final TripRouter router,
			final Set<String> modes,
			final DestinationSampler destinationSampler) {
		this.router = router;
		this.modes = modes;
		this.destinationSampler = destinationSampler;
	}

	@Override
	public ChoiceSet<Trip> sampleChoiceSet(final Person decisionMaker, final TripChoiceSituation choice) {
		final Map<String, Trip> namedAlternatives = new LinkedHashMap<>();

		final List<ActivityFacility> destinations = new ArrayList<>();
		destinations.add( choice.getChoice().getDestination() );
		destinations.addAll( destinationSampler.sampleDestinations(decisionMaker, choice));

		final ActivityFacility origin = choice.getChoice().getOrigin();

		for ( int i = 0; i < destinations.size(); i++ ) {
			final ActivityFacility dest = destinations.get( i );
			for ( String mode : modes ) {
				final List<? extends PlanElement> tripelements =
						router.calcRoute(
							mode,
							origin,
							dest,
							12 * 3600,
							decisionMaker );
				namedAlternatives.put(
						i+"_"+mode,
						new Trip( origin , tripelements , dest ) );
			}
		}

		final String chosenMode = router.getMainModeIdentifier().identifyMainMode( choice.getChoice().getTrip() );
		return new ChoiceSet<>(
				decisionMaker,
				"0_"+chosenMode,
				namedAlternatives );
	}

	public interface DestinationSampler {
		Collection<ActivityFacility> sampleDestinations( Person decisionMaker , TripChoiceSituation choice );
	}
}
