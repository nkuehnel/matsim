/* *********************************************************************** *
 * project: org.matsim.*
 * ModelIterator.java
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
package playground.thibautd.initialdemandgeneration.socnetgensimulated.framework;

import java.util.ArrayList;
import java.util.List;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import org.apache.commons.math3.analysis.MultivariateFunction;
import org.apache.commons.math3.optim.ConvergenceChecker;
import org.apache.commons.math3.optim.InitialGuess;
import org.apache.commons.math3.optim.MaxEval;
import org.apache.commons.math3.optim.PointValuePair;
import org.apache.commons.math3.optim.SimpleBounds;
import org.apache.commons.math3.optim.nonlinear.scalar.GoalType;
import org.apache.commons.math3.optim.nonlinear.scalar.MultivariateOptimizer;
import org.apache.commons.math3.optim.nonlinear.scalar.ObjectiveFunction;
import org.apache.commons.math3.optim.nonlinear.scalar.noderiv.CMAESOptimizer;
import org.apache.commons.math3.random.MersenneTwister;
import org.apache.log4j.Logger;

import playground.thibautd.initialdemandgeneration.socnetgen.framework.SnaUtils;
import org.matsim.contrib.socnetsim.framework.population.SocialNetwork;

/**
 * @author thibautd
 */
@Singleton
public class ModelIterator {
	private static final Logger log =
		Logger.getLogger(ModelIterator.class);

	// get the provider to be sure all preprocessed data is there when getting value
	// not the nicest design...
	private final Provider<Thresholds> initialValue;

	private final double targetClustering;
	private final double targetDegree;

	private final double precisionClustering;
	private final double precisionDegree;

	private final int maxIterations;

	private final double powellMinAbsoluteChange;
	private final double powellMinRelativeChange;

	private final int nThreads;

	private final List<EvolutionListener> listeners = new ArrayList< >();

	private final ModelRunner runner;

	@Inject
	public ModelIterator(
			final SocialNetworkGenerationConfigGroup config,
			final ModelRunner runner,
			@Named( "initialValue" )
			final Provider<Thresholds> initialValue) {
		this.nThreads = config.getNThreads();
		this.initialValue = initialValue;
		this.runner = runner;
		this.targetClustering = config.getTargetClustering();
		this.targetDegree = config.getTargetDegree();

		listeners.add( new EvolutionLogger() );

		this.precisionClustering = config.getPrecisionClustering();
		this.precisionDegree = config.getPrecisionDegree();

		this.powellMinAbsoluteChange = config.getPowellMinAbsoluteChange();
		this.powellMinRelativeChange = config.getPowellMinRelativeChange();

		this.maxIterations = config.getMaxIterations();
	}

	private SocialNetwork generate( final ModelRunner runner , final Thresholds thresholds ) {
		log.info( "generate network for "+thresholds );
		final long start = System.currentTimeMillis();
		final SocialNetwork sn = runner.runModel( thresholds );

		thresholds.setResultingAverageDegree( SnaUtils.calcAveragePersonalNetworkSize( sn ) );
		thresholds.setResultingClustering( estimateClustering( sn ) );

		log.info( "generation took "+(System.currentTimeMillis() - start)+" ms" );
		return sn;
	}


	public SocialNetwork iterateModelToTarget() {
		final Thresholds initialThresholds = initialValue.get();
		final MultivariateOptimizer optimizer =
			new CMAESOptimizer(
					maxIterations,
					1E-9,
					true,
					3,
					50,
					new MersenneTwister( 42 ),
					false,
					new Convergence() );

		final double x = initialThresholds.getPrimaryThreshold();
		final double y = initialThresholds.getSecondaryReduction();

		final PointValuePair result =
			optimizer.optimize(
					GoalType.MINIMIZE,
					new MaxEval( maxIterations ),
					new InitialGuess( new double[]{ x , y } ),
					new ObjectiveFunction( new Function( 1 , runner ) ),
					new CMAESOptimizer.Sigma( new double[]{ 5 , 500 } ),
					new CMAESOptimizer.PopulationSize( 7 ),
					new SimpleBounds(
						new double[]{ Double.NEGATIVE_INFINITY , 0 }, // lower bounds: constrain secondary reduction to be positive
						new double[]{ Double.POSITIVE_INFINITY , Double.POSITIVE_INFINITY } ) // upper bounds
					);

		final Thresholds bestThresholds = new Thresholds( result.getPoint()[ 0 ] , result.getPoint()[ 1 ] );
		final SocialNetwork bestSn = generate( runner , bestThresholds );

		log.info( "best social network found for thresholds: "+bestThresholds );

		return bestSn;
	}

	private double estimateClustering( final SocialNetwork sn ) {
		//final double estimate = SnaUtils.estimateClusteringCoefficient( samplingRateClustering , sn );

		//return Math.abs( targetClustering - estimate ) > 10 * precisionClustering ? estimate : SnaUtils.calcClusteringCoefficient( sn );
		return SnaUtils.estimateClusteringCoefficient( 1900 , nThreads , precisionClustering , 0.95 , sn );
	}

	public void addListener( final EvolutionListener l ) {
		listeners.add( l );
	}

	private double distClustering( final Thresholds thresholds ) {
		return Math.abs( targetClustering -  thresholds.getResultingClustering() );
	}

	private double distDegree( final Thresholds thresholds ) {
		return Math.abs( targetDegree -  thresholds.getResultingAverageDegree() );
	}

	public interface EvolutionListener {
		public void handleMove( Thresholds m , double fitness );
	}

	private static class EvolutionLogger implements EvolutionListener {
		@Override
		public void handleMove( final Thresholds m , final double fitness ) {
			log.info( "generated network for "+m+" -> fitness="+fitness );
		}
	}

	private class Function implements MultivariateFunction {
		private final double nDistance;
		private final ModelRunner runner;

		public Function(
				final int nDistance,
				final ModelRunner runner ) {
			this.nDistance = nDistance;
			this.runner = runner;
		}

		@Override
		public double value( final double[] args ) {
			final Thresholds thr = new Thresholds( args[ 0 ] , args[ 1 ] );
	 		generate( runner , thr );

			// use the "distance n" (distance 1 is manhatan, distance 2 is euclidean distance)
			final double fitness =
					Math.pow(
						Math.pow( distDegree( thr ) / precisionDegree , nDistance ) +
							Math.pow( distClustering( thr ) / precisionClustering , nDistance ),
						1 / nDistance );

			for ( EvolutionListener l : listeners ) l.handleMove( thr , fitness );

			return fitness;
		}
	}

	private class Convergence implements ConvergenceChecker<PointValuePair> {
		@Override
		public boolean converged( final int i , final PointValuePair prev , final PointValuePair curr ) {
			// not really satisfying...
			final boolean conv = curr.getValue().doubleValue() < 1 || Math.abs( prev.getValue() - curr.getValue() ) < 1E-8;

			if ( conv ) {
				log.info( "convergence checker considers convergenced is reached." );
			}
			else {
				final double prevVal = prev.getValue();
				final double currVal = curr.getValue();

				final double abs = Math.abs( currVal - prevVal );
				final double rel = Math.abs( (currVal - prevVal) / prevVal );

				if ( abs <= powellMinAbsoluteChange || rel <= powellMinRelativeChange ) {
					// never printed: optimizer calls this class after line search,
					// if and only if the optimizer does not itself considers it converged...
					log.warn( "considered non converged, but too flat: Powell will abort!" );
					log.warn( "absolute change: "+abs );
					log.warn( "relative change: "+rel );
				}
			}

			return conv;
		}
	}
}

