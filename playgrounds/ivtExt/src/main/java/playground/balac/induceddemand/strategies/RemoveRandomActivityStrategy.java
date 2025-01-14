package playground.balac.induceddemand.strategies;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.HasPlansAndId;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.core.replanning.PlanStrategy;
import org.matsim.core.replanning.PlanStrategyImpl;
import org.matsim.core.replanning.ReplanningContext;
import org.matsim.core.replanning.modules.ReRoute;
import org.matsim.core.replanning.selectors.RandomPlanSelector;
import org.matsim.core.utils.collections.QuadTree;

import com.google.inject.Inject;
import com.google.inject.name.Named;

public class RemoveRandomActivityStrategy implements PlanStrategy {
	private final PlanStrategy planStrategyDelegate;

		
	@Inject
	public  RemoveRandomActivityStrategy(final Scenario scenario) {
		
	    PlanStrategyImpl.Builder builder = new PlanStrategyImpl.Builder(new RandomPlanSelector<Plan, Person>() );
	    RemoveRandomActivity ira = new RemoveRandomActivity(scenario);
	    
		builder.addStrategyModule(ira);
		builder.addStrategyModule(new ReRoute(scenario));
		
		planStrategyDelegate = builder.build();		
	}	
	
	@Override
	public void run(HasPlansAndId<Plan, Person> person) {
		planStrategyDelegate.run(person);
		
	}

	@Override
	public void init(ReplanningContext replanningContext) {
		planStrategyDelegate.init(replanningContext);
		
	}

	@Override
	public void finish() {
		planStrategyDelegate.finish();
		
	}

}
