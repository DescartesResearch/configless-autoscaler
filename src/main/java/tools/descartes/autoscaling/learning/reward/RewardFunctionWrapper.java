package tools.descartes.autoscaling.learning.reward;

import org.springframework.stereotype.Component;

/**
 * A wrapper class which instantiates a concrete instance of IRewardFunction
 */
@Component
public class RewardFunctionWrapper {
    private final IRewardFunction rewardFunction;

    public RewardFunctionWrapper() {
        this.rewardFunction = new ImprovedRewardFunction();
    }

    /**
     * @return the wrapped IRewardFunction instance
     */
    public IRewardFunction getRewardFunction() {
        return rewardFunction;
    }
}
