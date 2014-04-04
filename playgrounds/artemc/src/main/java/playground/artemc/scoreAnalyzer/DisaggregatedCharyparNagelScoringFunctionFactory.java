/* *********************************************************************** *
 * project: org.matsim.*
 * CharyparNagelOpenTimesScoringFunctionFactory.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2007 by the members listed in the COPYING,        *
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

package playground.artemc.scoreAnalyzer;

import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup;
import org.matsim.core.scoring.ScoringFunction;
import org.matsim.core.scoring.ScoringFunctionFactory;
import org.matsim.core.scoring.functions.CharyparNagelActivityScoring;
import org.matsim.core.scoring.functions.CharyparNagelAgentStuckScoring;
import org.matsim.core.scoring.functions.CharyparNagelLegScoring;
import org.matsim.core.scoring.functions.CharyparNagelMoneyScoring;
import org.matsim.core.scoring.functions.CharyparNagelScoringParameters;

/**
 * A factory to create scoring functions as described by D. Charypar and K. Nagel.
 * 
 * <blockquote>
 *  <p>Charypar, D. und K. Nagel (2005) <br>
 *  Generating complete all-day activity plans with genetic algorithms,<br>
 *  Transportation, 32 (4) 369-397.</p>
 * </blockquote>
 * 
 * @author rashid_waraich
 */
public class DisaggregatedCharyparNagelScoringFunctionFactory implements ScoringFunctionFactory {

	protected Network network;
	private final PlanCalcScoreConfigGroup config;
	private CharyparNagelScoringParameters params = null;

	public DisaggregatedCharyparNagelScoringFunctionFactory(final PlanCalcScoreConfigGroup config, Network network) {
		this.config = config;
		this.network = network;
	}

	/**
	 * puts the scoring functions together, which form the
	 * CharyparScoringFunction
	 * <p/>
	 * This creational method gets the plan as an argument.  Since it is possible to get the person from the plan, it is thus
	 * possible to make the scoring function person-specific.
	 * <p/>  
	 * Notes:<ul>
	 * <li>If I understand this correctly, this creational method is 
	 * called in every iteration. kai, apr'11
	 * <li>The fact that you have a person-specific scoring function does not mean that the "creative" modules
	 * (such as route choice) are person-specific.  This is not a bug but a deliberate design concept in order 
	 * to reduce the consistency burden.  Instead, the creative modules should generate a diversity of possible
	 * solutions.  In order to do a better job, they may (or may not) use person-specific info.  kai, apr'11
	 * </ul>
	 * 
	 * @param plan
	 * @return new ScoringFunction
	 */
	@Override
	public ScoringFunction createNewScoringFunction(Plan plan) {
		if (this.params == null) {
			/* lazy initialization of params. not strictly thread safe, as different threads could
			 * end up with different params-object, although all objects will have the same
			 * values in them due to using the same config. Still much better from a memory performance
			 * point of view than giving each ScoringFunction its own copy of the params.
			 */
			this.params = new CharyparNagelScoringParameters(this.config);
		}

		DisaggregatedSumScoringFunction sumScoringFunction = new DisaggregatedSumScoringFunction();
		sumScoringFunction.addScoringFunction(new CharyparNagelActivityScoring(this.params));		
		for(String mode:config.getModes().keySet()){
			sumScoringFunction.addLegScoringFunction(mode, new CharyparNagelLegScoring(this.params, this.network));
		}
		sumScoringFunction.addLegScoringFunction("transit_walk", new CharyparNagelLegScoring(this.params, this.network));
		
		//sumScoringFunction.addLegScoringFunction("car", new CharyparNagelLegScoring(this.params, this.network));
		//sumScoringFunction.addLegScoringFunction("pt", new CharyparNagelLegScoring(this.params, this.network));
		
		
		sumScoringFunction.addScoringFunction(new CharyparNagelMoneyScoring(this.params));
		sumScoringFunction.addScoringFunction(new CharyparNagelAgentStuckScoring(this.params));
			
		
//		ScoringFunctionAccumulator sumScoringFunction = new ScoringFunctionAccumulator();
//		sumScoringFunction.addScoringFunction(new CharyparNagelActivityScoring(this.params));
//		sumScoringFunction.addScoringFunction(new CharyparNagelLegScoring(this.params, this.network));
//		sumScoringFunction.addScoringFunction(new CharyparNagelMoneyScoring(this.params));
//		sumScoringFunction.addScoringFunction(new CharyparNagelAgentStuckScoring(this.params));
		
		
		return sumScoringFunction;
	}
	
}