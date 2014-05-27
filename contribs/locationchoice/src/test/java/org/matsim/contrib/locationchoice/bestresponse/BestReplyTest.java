package org.matsim.contrib.locationchoice.bestresponse;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Scenario;
import org.matsim.contrib.locationchoice.DCControler;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.testcases.MatsimTestCase;


public class BestReplyTest extends MatsimTestCase {
	
	private Scenario scenario;
	private DestinationChoiceBestResponseContext context;
	
	private static final Logger log = Logger.getLogger(BestReplyTest.class);
	
	@Override
	protected void setUp() throws Exception {
		super.setUp();
		this.init();
	}
	
	public void testSampler() {
		DestinationSampler sampler = new DestinationSampler(
				context.getPersonsKValuesArray(), context.getFacilitiesKValuesArray(), scenario.getConfig().locationchoice());
		assertTrue(sampler.sample(context.getFacilityIndex(new IdImpl(1)), context.getPersonIndex(new IdImpl(1))));
		assertTrue(!sampler.sample(context.getFacilityIndex(new IdImpl(1)), context.getPersonIndex(new IdImpl(2))));
//		assertTrue(sampler.sample(new IdImpl(1), new IdImpl(1)));
//		assertTrue(!sampler.sample(new IdImpl(1), new IdImpl(2)));
	}
	
	public void init() {
		String configFile = this.getPackageInputDirectory() + "/config.xml";
		this.scenario = (ScenarioImpl) ScenarioUtils.loadScenario(ConfigUtils.loadConfig(configFile));
		this.context = new DestinationChoiceBestResponseContext(this.scenario);
		this.context.init();
	}
	
	public void testRunControler() {
		String args [] = {this.getPackageInputDirectory() + "/config.xml"};
		DCControler.main(args);		
	}
}