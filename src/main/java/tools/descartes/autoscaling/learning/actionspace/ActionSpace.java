package tools.descartes.autoscaling.learning.actionspace;

/**
 * Class representing a certain action space for a reinforcement learner.
 */
public class ActionSpace {

	public static ActionSpace getDefaultActionSpace() {
		return new ActionSpace(new int[] { -1, 0, 1 });
	}

	private int[] actions;

	public ActionSpace(int[] actions) {
		super();
		this.actions = actions;
	}

	/**
	 * @return the actions
	 */
	public int[] getActions() {
		return actions;
	}

	/**
	 * @param actions the actions to set
	 */
	public void setActions(int[] actions) {
		this.actions = actions;
	}

}
