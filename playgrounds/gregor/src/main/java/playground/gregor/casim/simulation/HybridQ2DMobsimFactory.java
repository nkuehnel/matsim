/* *********************************************************************** *
 * project: org.matsim.*
 * HybridQ2DMobsimFactory.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2012 by the members listed in the COPYING,        *
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

package playground.gregor.casim.simulation;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.groups.QSimConfigGroup;
import org.matsim.core.mobsim.framework.Mobsim;
import org.matsim.core.mobsim.framework.MobsimFactory;
import org.matsim.core.mobsim.qsim.ActivityEngine;
import org.matsim.core.mobsim.qsim.QSim;
import org.matsim.core.mobsim.qsim.TeleportationEngine;
import org.matsim.core.mobsim.qsim.agents.AgentFactory;
import org.matsim.core.mobsim.qsim.agents.DefaultAgentFactory;
import org.matsim.core.mobsim.qsim.agents.PopulationAgentSource;
import org.matsim.core.mobsim.qsim.changeeventsengine.NetworkChangeEventsEngine;
import org.matsim.core.mobsim.qsim.qnetsimengine.HybridQSimCANetworkFactory;
import org.matsim.core.mobsim.qsim.qnetsimengine.QNetsimEngine;

import playground.gregor.sim2d_v4.scenario.Sim2DScenario;

public class HybridQ2DMobsimFactory implements MobsimFactory {

	private final static Logger log = Logger.getLogger(HybridQ2DMobsimFactory.class);
	
	CAEngine sim2DEngine = null;


	@Override
	public Mobsim createMobsim(Scenario sc, EventsManager eventsManager) {

		if (!sc.getConfig().controler().getMobsim().equals("hybridQ2D")) {
			throw new RuntimeException("This factory does not make sense for " + sc.getConfig().controler().getMobsim()  );
		}
		
		QSimConfigGroup conf = sc.getConfig().qsim();
		if (conf == null) {
			throw new NullPointerException("There is no configuration set for the QSim. Please add the module 'qsim' to your config file.");
		}

		// Get number of parallel Threads
		int numOfThreads = conf.getNumberOfThreads();

//		QNetsimEngineFactory netsimEngFactory;
//		if (numOfThreads > 1) {
//			eventsManager = new SynchronizedEventsManagerImpl(eventsManager);
//			netsimEngFactory = new ParallelQNetsimEngineFactory();
//			log.info("Using parallel QSim with " + numOfThreads + " threads.");
//		} else {
//			netsimEngFactory = new DefaultQSimEngineFactory();
//		}


		QSim qSim = new QSim(sc, eventsManager);

		TeleportationEngine teleportationEngine = new TeleportationEngine();
		qSim.addMobsimEngine(teleportationEngine);
		
		CAEngine e = new CAEngine(qSim);
		
		
		this.sim2DEngine = e;
		qSim.addMobsimEngine(e);
		
		ActivityEngine activityEngine = new ActivityEngine();
		qSim.addMobsimEngine(activityEngine);
		qSim.addActivityHandler(activityEngine);
		
		Sim2DScenario sc2d = (Sim2DScenario) sc.getScenarioElement(Sim2DScenario.ELEMENT_NAME);
		CAAgentFactory aBuilder = new CAAgentFactory(sc2d.getSim2DConfig(),sc);
//		Sim2DAgentFactory aBuilder = new ORCAAgentFactory(sc2d.getSim2DConfig(),sc);
		
		HybridQSimCANetworkFactory networkFactory = new HybridQSimCANetworkFactory(e,sc, aBuilder);
	
		QNetsimEngine netsimEngine = new QNetsimEngine( qSim, networkFactory ) ;
		qSim.addMobsimEngine(netsimEngine);
		qSim.addDepartureHandler(netsimEngine.getDepartureHandler());

		AgentFactory agentFactory = new DefaultAgentFactory(qSim);
		PopulationAgentSource agentSource = new PopulationAgentSource(sc.getPopulation(), agentFactory, qSim);
		qSim.addAgentSource(agentSource);

		if (sc.getConfig().network().isTimeVariantNetwork()) {
			qSim.addMobsimEngine(new NetworkChangeEventsEngine());		
		}
		
		return qSim;
	}

	public CAEngine getSim2DEngine() {
		return this.sim2DEngine;
	}

}