/* *********************************************************************** *
 * project: org.matsim.*
 * WeakSelectorFactory.java
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
package playground.thibautd.socnetsim.replanning.strategies;

import playground.thibautd.socnetsim.controller.ControllerRegistry;
import playground.thibautd.socnetsim.replanning.GroupLevelPlanSelectorFactory;
import playground.thibautd.socnetsim.replanning.selectors.GroupLevelPlanSelector;
import playground.thibautd.socnetsim.replanning.selectors.WeakSelector;

/**
 * @author thibautd
 */
public class WeakSelectorFactory implements GroupLevelPlanSelectorFactory {
	private final GroupLevelPlanSelectorFactory delegateFactory;

	public WeakSelectorFactory(
			final GroupLevelPlanSelectorFactory delegateFactory ) {
		this.delegateFactory = delegateFactory;
	}

	@Override
	public GroupLevelPlanSelector createSelector(final ControllerRegistry registry) {
		return new WeakSelector(
				registry.getWeakPlanLinkIdentifier() ,
				delegateFactory.createSelector( registry ) );
	}
}
