package playground.yu.integration.cadyts.parameterCalibration.withCarCounts.scoring;

import org.matsim.core.controler.listener.IterationStartsListener;
import org.matsim.core.controler.listener.ScoringListener;
import org.matsim.core.controler.listener.StartupListener;

import playground.yu.integration.cadyts.parameterCalibration.withCarCounts.mnlValidation.CadytsChoice;
import playground.yu.scoring.PlansScoringI;

/**
 * Interface for all {@code PlanScoring4PC}
 * 
 * @author yu
 * 
 */
public interface PlansScoring4PC_I extends StartupListener, ScoringListener,
		IterationStartsListener, PlansScoringI {
	@Override
	public CadytsChoice getPlanScorer();
}
