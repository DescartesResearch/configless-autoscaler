package tools.descartes.autoscaling.dummy.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * A property class containing rules for scaling a.k.a. upper and lower bounds for deployed instances
 */
@Component
@ConfigurationProperties("constraints")
public class RulesProperties {
    /**
     * A list of rules a.k.a. upper and lower bounds for deployed instances per app
     */
    private List<ScalingExpression> rules;

    public List<ScalingExpression> getRules() {
        return rules;
    }

    public void setRules(List<ScalingExpression> rules) {
        this.rules = rules;
    }

    @Override
    public String toString() {
        return toStringWithSeparator("\n");
    }

    public String toStringWithSeparator(String separator) {
        StringBuilder builder = new StringBuilder();
        for (ScalingExpression se : rules) {
            builder.append(se.toString());
            builder.append(separator);
        }
        return builder.toString();
    }

    /**
     * Represents a scaling rule (an upper or lower bound for number of deployed instances for one app)
     */
    public static class ScalingExpression {
        private String app;
        private Relation relation;
        private int value;

        public String getApp() {
            return app;
        }

        public void setApp(String app) {
            this.app = app;
        }

        public Relation getRelation() {
            return relation;
        }

        public void setRelation(Relation relation) {
            this.relation = relation;
        }

        public int getValue() {
            return value;
        }

        public void setValue(int value) {
            this.value = value;
        }

        public enum Relation {
            SMALLER, SMALLER_EQUAL, GREATER, GREATER_EQUAL;

            @Override
            public String toString() {
                switch (this) {
                    case SMALLER:
                        return " < ";
                    case SMALLER_EQUAL:
                        return " <= ";
                    case GREATER:
                        return " > ";
                }
                return " >= ";
            }
        }

        /**
         * Checks whether the value fulfills the rule
         * @param checkVal the value to check
         * @return true if rule fulfilled, false if not
         */
        public boolean evaluate(double checkVal) {
            if (relation == null) return false;
            switch(relation) {
                case GREATER:
                    return checkVal > value;
                case SMALLER:
                    return checkVal < value;
                case GREATER_EQUAL:
                    return checkVal >= value;
            }
            return checkVal <= value;
        }

        /**
         * Returns the highest number (in case of an upper bound) or lowest number (in case of a lower bound) of
         * instances which would fulfill this rule
         * @return the highest number (in case of an upper bound) or lowest number (in case of a lower bound) of
         *          instances which would fulfill this rule
         */
        public int getClosestValue() {
            switch(relation) {
                case GREATER:
                    return value + 1;
                case SMALLER:
                    return value - 1;
            }
            return value;
        }

        @Override
        public String toString() {
            return app + " " + relation.toString() + " " + value;
        }
    }
}
