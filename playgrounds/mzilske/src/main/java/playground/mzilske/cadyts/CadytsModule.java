/*
 *  *********************************************************************** *
 *  * project: org.matsim.*
 *  * CadytsModule.java
 *  *                                                                         *
 *  * *********************************************************************** *
 *  *                                                                         *
 *  * copyright       : (C) 2014 by the members listed in the COPYING, *
 *  *                   LICENSE and WARRANTY file.                            *
 *  * email           : info at matsim dot org                                *
 *  *                                                                         *
 *  * *********************************************************************** *
 *  *                                                                         *
 *  *   This program is free software; you can redistribute it and/or modify  *
 *  *   it under the terms of the GNU General Public License as published by  *
 *  *   the Free Software Foundation; either version 2 of the License, or     *
 *  *   (at your option) any later version.                                   *
 *  *   See also COPYING, LICENSE and WARRANTY file                           *
 *  *                                                                         *
 *  * ***********************************************************************
 */

package playground.mzilske.cadyts;

import cadyts.calibrators.analytical.AnalyticalCalibrator;
import com.google.common.collect.ImmutableMap;
import com.google.inject.AbstractModule;
import com.google.inject.TypeLiteral;
import com.google.inject.multibindings.MapBinder;
import com.google.inject.multibindings.Multibinder;
import org.matsim.analysis.VolumesAnalyzer;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.contrib.cadyts.general.CadytsBuilder;
import org.matsim.contrib.cadyts.general.PlansTranslator;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.controler.events.IterationEndsEvent;
import org.matsim.core.controler.events.StartupEvent;
import org.matsim.core.controler.listener.ControlerListener;
import org.matsim.core.replanning.PlanStrategy;
import org.matsim.core.replanning.PlanStrategyFactory;
import org.matsim.core.replanning.PlanStrategyImpl;
import org.matsim.core.scoring.ScoringFunctionFactory;
import org.matsim.core.scoring.functions.CharyparNagelScoringFunctionFactory;
import org.matsim.counts.CountSimComparison;
import org.matsim.counts.Counts;
import org.matsim.counts.algorithms.CountsComparisonAlgorithm;
import playground.mzilske.cdranalysis.StreamingOutput;
import playground.mzilske.util.IterationSummaryFileControlerListener;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;

public class CadytsModule extends AbstractModule {

    @Override
    protected void configure() {
        Multibinder <ControlerListener> controlerListenerBinder = Multibinder.newSetBinder(binder(), ControlerListener.class);
        controlerListenerBinder.addBinding().to(CadytsControlerListener.class);
        controlerListenerBinder.addBinding().toProvider(MyControlerListenerProvider.class);
        MapBinder<String, PlanStrategyFactory> planStrategyFactoryBinder
                = MapBinder.newMapBinder(binder(), String.class, PlanStrategyFactory.class);
        planStrategyFactoryBinder.addBinding("ccc").toProvider(MyPlanStrategyFactoryProvider.class);
        bind(ScoringFunctionFactory.class).to(MyScoringFunctionFactory.class);
        bind(new TypeLiteral<AnalyticalCalibrator<Link>>(){}).toProvider(CalibratorProvider.class).in(Singleton.class);
        bind(new TypeLiteral<PlansTranslator<Link>>(){}).to(PlanToPlanStepBasedOnEvents.class).in(Singleton.class);
    }

    static class CalibratorProvider implements Provider<AnalyticalCalibrator<Link>> {

        @Inject Scenario scenario;
        @Inject @Named("calibrationCounts") Counts counts;

        @Override
        public AnalyticalCalibrator get() {
            return CadytsBuilder.buildCalibrator(scenario.getConfig(), this.counts, new LinkLookUp(scenario));
        }
    }

    static class MyPlanStrategyFactoryProvider implements Provider<PlanStrategyFactory> {
        @Inject PlansTranslator<Link> plansTranslator;
        @Inject AnalyticalCalibrator<Link> calibrator;

        @Override
        public PlanStrategyFactory get() {
            return new PlanStrategyFactory() {
                @Override
                public PlanStrategy createPlanStrategy(Scenario scenario, EventsManager events) {
                    ExpBetaPlanChangerWithCadytsPlanRegistration<Link> planSelector = new ExpBetaPlanChangerWithCadytsPlanRegistration<Link>(1.0, plansTranslator, calibrator);
                    return new PlanStrategyImpl(planSelector);
                }
            };
        }
    }

    static class MyControlerListenerProvider implements Provider<ControlerListener> {
        @Inject @Named("allCounts") Counts counts;
        @Inject Scenario scenario;
        @Inject OutputDirectoryHierarchy controlerIO;
        @Inject VolumesAnalyzer volumesAnalyzer;
        @Override
        public ControlerListener get() {
            return new IterationSummaryFileControlerListener(controlerIO,
                    ImmutableMap.<String, IterationSummaryFileControlerListener.Writer>of(
                    "linkstats.txt",
                    new IterationSummaryFileControlerListener.Writer() {
                        @Override
                        public StreamingOutput notifyStartup(StartupEvent event) {
                            return new StreamingOutput() {
                                @Override
                                public void write(PrintWriter pw) throws IOException {
                                    pw.printf("%s\t%s\t%s\t%s\t%s\n",
                                            "iteration",
                                            "link",
                                            "hour",
                                            "sim.volume",
                                            "count.volume");
                                }
                            };
                        }

                        @Override
                        public StreamingOutput notifyIterationEnds(final IterationEndsEvent event) {
                            CountsComparisonAlgorithm countsComparisonAlgorithm = new CountsComparisonAlgorithm(volumesAnalyzer, counts, scenario.getNetwork(), 1.0);
                            countsComparisonAlgorithm.run();
                            final List<CountSimComparison> comparison = countsComparisonAlgorithm.getComparison();
                            return new StreamingOutput() {
                                @Override
                                public void write(PrintWriter pw) throws IOException {
                                    for (CountSimComparison countLink : comparison) {
                                        pw.printf("%d\t%s\t%d\t%f\t%f\n",
                                                event.getIteration(),
                                                countLink.getId().toString(),
                                                countLink.getHour(),
                                                countLink.getSimulationValue(),
                                                countLink.getCountValue());
                                    }
                                }
                            };
                        }
                    }
            ));
        }
    }

}