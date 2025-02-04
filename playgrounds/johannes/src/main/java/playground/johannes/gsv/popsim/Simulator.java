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

package playground.johannes.gsv.popsim;

import org.apache.commons.math.FunctionEvaluationException;
import org.apache.commons.math.analysis.UnivariateRealFunction;
import org.apache.log4j.Logger;
import org.matsim.contrib.common.stats.Discretizer;
import org.matsim.contrib.common.stats.FixedSampleSizeDiscretizer;
import org.matsim.contrib.common.stats.LinearDiscretizer;
import org.matsim.contrib.common.util.XORShiftRandom;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import playground.johannes.gsv.popsim.analysis.*;
import playground.johannes.gsv.popsim.config.MatrixAnalyzerConfigurator;
import playground.johannes.gsv.synPop.mid.Route2GeoDistance;
import playground.johannes.gsv.synPop.sim3.ReplaceActTypes;
import playground.johannes.synpop.data.*;
import playground.johannes.synpop.data.io.PopulationIO;
import playground.johannes.synpop.gis.DataPool;
import playground.johannes.synpop.gis.FacilityData;
import playground.johannes.synpop.gis.FacilityDataLoader;
import playground.johannes.synpop.gis.ZoneDataLoader;
import playground.johannes.synpop.processing.CalculateGeoDistance;
import playground.johannes.synpop.processing.GuessMissingActTypes;
import playground.johannes.synpop.processing.LegAttributeRemover;
import playground.johannes.synpop.processing.TaskRunner;
import playground.johannes.synpop.sim.*;
import playground.johannes.synpop.sim.data.CachedPerson;
import playground.johannes.synpop.sim.data.Converters;
import playground.johannes.synpop.sim.data.DoubleConverter;
import playground.johannes.synpop.source.mid2008.MiDKeys;

import java.io.IOException;
import java.util.*;

/**
 * @author johannes
 */
public class Simulator {

    private static final Logger logger = Logger.getLogger(Simulator.class);

    private static final String MODULE_NAME = "synPopSim";

    /**
     * @param args
     * @throws IOException
     */
    public static void main(String[] args) throws IOException {
        final Config config = new Config();
        ConfigUtils.loadConfig(config, args[0]);

        logger.info("Loading persons...");
        Set<PlainPerson> refPersons = (Set<PlainPerson>) PopulationIO.loadFromXML(config.findParam(MODULE_NAME,
                "popInputFile"), new PlainFactory());
        logger.info(String.format("Loaded %s persons.", refPersons.size()));

        Random random = new XORShiftRandom(Long.parseLong(config.getParam("global", "randomSeed")));
        /*
		Prepare population for simulation.
		 */
        logger.info("Preparing reference simulation...");
        TaskRunner.run(new ReplaceActTypes(), refPersons);
        new GuessMissingActTypes(random).apply(refPersons);
        TaskRunner.run(new Route2GeoDistance(new Route2GeoDistFunction()), refPersons);
		/*
		Setting up data loaders.
		 */
        logger.info("Registering data loaders...");
        DataPool dataPool = new DataPool();
        dataPool.register(new FacilityDataLoader(config.getParam(MODULE_NAME, "facilities"), random), FacilityDataLoader.KEY);
        dataPool.register(new ZoneDataLoader(config.getModule(MODULE_NAME)), ZoneDataLoader.KEY);
		/*
		Setup the simulation population.
		 */
        logger.info("Cloning persons...");
        int size = (int) Double.parseDouble(config.getParam(MODULE_NAME, "populationSize"));
        Set<PlainPerson> simPersons = (Set<PlainPerson>) PersonUtils.weightedCopy(refPersons, new PlainFactory(), size,
                random);
        logger.info(String.format("Generated %s persons.", simPersons.size()));
        logger.info("Assigning home locations...");
        new SetHomeFacilities(dataPool, "modena", random).apply(simPersons);
        logger.info("Assigning random activity locations...");
        TaskRunner.run(new SetActivityFacilities((FacilityData) dataPool.get(FacilityDataLoader.KEY)), simPersons);
        logger.info("Recalculate geo distances...");
        TaskRunner.run(new LegAttributeRemover(CommonKeys.LEG_GEO_DISTANCE), simPersons);
        TaskRunner.run(new CalculateGeoDistance((FacilityData) dataPool.get(FacilityDataLoader.KEY)), simPersons);
		/*
		Setup analyzer and analyze reference population
		 */
        final String output = config.getParam(MODULE_NAME, "output");
        FileIOContext ioContext = new FileIOContext(output);
        final ConcurrentAnalyzerTask<Collection<? extends Person>> task = new ConcurrentAnalyzerTask<>();
        task.addComponent(new GeoDistanceBuilder(ioContext).build());
        task.addComponent(new GeoDistLau2ClassTask(ioContext));
        logger.info("Analyzing reference population...");
        ioContext.append("ref");
        AnalyzerTaskRunner.run(refPersons, task, ioContext);

        task.addComponent((AnalyzerTask<Collection<? extends Person>>) new MatrixAnalyzerConfigurator(config
                .getModule("matrixAnalyzer"), dataPool).load());
		/*
		Setup hamiltonian
		 */
        final HamiltonianComposite hamiltonian = new HamiltonianComposite();
		/*
		Setup distance distribution hamiltonian.
		 */
        UnivariatFrequency distDistrTerm = buildDistDistrTerm(refPersons, simPersons);
        hamiltonian.addComponent(distDistrTerm, Double.parseDouble(config.getParam(MODULE_NAME, "theta_distDistr")));
		/*
		Setup mean distance LAU2 hamiltonian.
		 */
        BivariatMean meanDistLau2Term = buildMeanDistLau2Term(refPersons, simPersons);
        hamiltonian.addComponent(meanDistLau2Term, Double.parseDouble(config.getParam(MODULE_NAME, "theta_distLau2")));
		/*
		Setup listeners for changes on geo distance.
		 */
        AttributeChangeListenerComposite geoDistListeners = new AttributeChangeListenerComposite();
        geoDistListeners.addComponent(distDistrTerm);
        geoDistListeners.addComponent(meanDistLau2Term);
		/*
		Setup the facility mutator.
		 */
        FacilityMutatorBuilder mutatorBuilder = new FacilityMutatorBuilder(dataPool, random);
        mutatorBuilder.addToBlacklist(ActivityTypes.HOME);
        mutatorBuilder.setListener(new GeoDistanceUpdater(geoDistListeners));
        Mutator<? extends Attributable> mutator = mutatorBuilder.build();
		/*
		Setup the sampler.
		 */
        MarkovEngine sampler = new MarkovEngine(simPersons, hamiltonian, mutator, random);

        MarkovEngineListenerComposite engineListeners = new MarkovEngineListenerComposite();

        long dumpInterval = (long) Double.parseDouble(config.getParam(MODULE_NAME, "dumpInterval"));
        engineListeners.addComponent(new AnalyzerListener(task, ioContext, dumpInterval));

//        engineListeners.addComponent(new MarkovEngineListener() {
//            AnalyzerListener l = new AnalyzerListener(task, String.format("%s/sim/", output), ));
//
//            @Override
//            public void afterStep(Collection<CachedPerson> population, Collection<? extends Attributable> mutations, boolean accepted) {
//                l.afterStep(population, null, accepted);
//            }
//        });

        engineListeners.addComponent(new MarkovEngineListener() {
            HamiltonianLogger l = new HamiltonianLogger(hamiltonian, (int) Double.parseDouble(config.getParam
                    (MODULE_NAME,
                            "logInterval")));

            @Override
            public void afterStep(Collection<CachedPerson> population, Collection<? extends Attributable> mutations, boolean accepted) {
                l.afterStep(population, null, accepted);
            }
        });

        sampler.setListener(engineListeners);

        sampler.run((long) Double.parseDouble(config.getParam(MODULE_NAME, "iterations")));

        Executor.shutdown();
    }

    private static UnivariatFrequency buildDistDistrTerm(Set<PlainPerson> refPersons, Set<PlainPerson>
            simPersons) {
        Set<Attributable> refLegs = getLegs(refPersons);
        Set<Attributable> simLegs = getLegs(simPersons);

        List<Double> values = new LegCollector(new NumericAttributeProvider(CommonKeys.LEG_GEO_DISTANCE)).collect(refPersons);
        double[] nativeValues = CollectionUtils.toNativeArray(values);
        Discretizer disc = FixedSampleSizeDiscretizer.create(nativeValues, 50, 100);

        UnivariatFrequency f = new UnivariatFrequency(refLegs, simLegs, CommonKeys.LEG_GEO_DISTANCE, disc);

        return f;
    }

    private static Set<Attributable> getLegs(Set<? extends Person> persons) {
        Set<Attributable> legs = new HashSet<>();
        for (Person p : persons) {
            Episode e = p.getEpisodes().get(0);
            legs.addAll(e.getLegs());
        }

        return legs;
    }

    private static BivariatMean buildMeanDistLau2Term(Set<PlainPerson> refPersons, Set<PlainPerson> simPersons) {
        copyLau2ClassAttribute(refPersons);
        copyLau2ClassAttribute(simPersons);

        Set<Attributable> refLegs = getLegs(refPersons);
        Set<Attributable> simLegs = getLegs(simPersons);

        Converters.register(MiDKeys.PERSON_LAU2_CLASS, DoubleConverter.getInstance());
        BivariatMean bm = new BivariatMean(refLegs, simLegs, MiDKeys.PERSON_LAU2_CLASS, CommonKeys.LEG_GEO_DISTANCE,
                new LinearDiscretizer(1.0));

        return bm;
    }

    private static void copyLau2ClassAttribute(Set<PlainPerson> persons) {
        for (Person p : persons) {
            String lau2Class = p.getAttribute(MiDKeys.PERSON_LAU2_CLASS);
            for (Episode e : p.getEpisodes()) {
                for (Segment leg : e.getLegs()) {
                    leg.setAttribute(MiDKeys.PERSON_LAU2_CLASS, lau2Class);
                }
            }
        }
    }

    public static class Route2GeoDistFunction implements UnivariateRealFunction {

        @Override
        public double value(double x) throws FunctionEvaluationException {
            double routDist = x / 1000.0;
            double factor = 0.77 - Math.exp(-0.17 * Math.max(20, routDist) - 1.48);
            return routDist * factor * 1000;
        }
    }

}
