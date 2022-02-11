package tools.descartes.autoscaling.learning;

import java.util.List;
import java.util.Map;

import tools.descartes.autoscaling.metrics.Metric;
import tools.descartes.autoscaling.learning.actionspace.ActionSpace;

/**
 * Model for evaluating a given state for a specified action space.
 *
 */
public interface IRLModel {

	/**
	 * Predict the best action for the given state and the given actionspace. Action
	 * is denoted by an integer index for the {@link ActionSpace} reference.
	 * 
	 * @param state The relevant state.
	 * @param space The corresponding {@link ActionSpace}.
	 * @return The best action of the action space
	 */
	default int predictBestAction(List<Metric> state, ActionSpace space) {
		Map<Integer, Double> values = this.predictValuesPerAction(state, space);
		int argMaxAction = Integer.MAX_VALUE;
		double argMax = Double.NEGATIVE_INFINITY;
		for (Integer action : values.keySet()) {
			if (values.get(action) > argMax) {
				argMax = values.get(action);
				argMaxAction = action;
			} else if (values.get(action) == argMax) {
				// if multiple actions have same expected reward then select minimum
				if (argMaxAction > action) {
					argMaxAction = action;
				}
			}
		}
		if (argMax != Double.NEGATIVE_INFINITY) return argMaxAction;
		else throw new IllegalStateException("The given action space did not contain any valid actions...");
	}

	/**
	 * Returns the expected reward for each action of the given action space.
	 * 
	 * @param state The relevant state.
	 * @param space The corresponding {@link ActionSpace}.
	 * @return A map containing expected rewards for all actions in ActionSpace
	 */
	Map<Integer, Double> predictValuesPerAction(List<Metric> state, ActionSpace space);

}
