import java.util.List;

/**
 * Created by neelshah on 11/19/15.
 */
public class WeightUpdate {
    public static void flat(Value[] faults, List<Double> weights) {
        for (int i = 0; i < faults.length; i++){
            if (faults[i].equals(Value.TRUE)) {
                weights.set(i, 0.0);
            }
        }

        normalizeWeights(weights);
    }

    public static void additive(Value[] faults, List<Double> weights, double alpha, double beta) {
        for (int i = 0; i < faults.length; i++){
            if (faults[i].equals(Value.TRUE)) {
                weights.set(i, weights.get(i) + alpha);
            } else if (faults[i].equals(Value.FALSE)) {
                weights.set(i, weights.get(i) - beta);
            }
        }

        normalizeWeights(weights);
    }

    public static void multiplicative(Value[] faults, List<Double> weights, double epsilon) {
        for (int i = 0; i < faults.length; i++){
            if (faults[i].equals(Value.TRUE)) {
                weights.set(i, weights.get(i) * (1 - epsilon));
            } else if (faults[i].equals(Value.FALSE)) {
                weights.set(i, weights.get(i) * (1 + epsilon));
            }
        }

        normalizeWeights(weights);
    }

    private static void normalizeWeights(List<Double> weights) {
        double sum = 0;

        for(Double weight : weights) {
            sum += weight;
        }

        for(int i = 0; i < weights.size(); i++) {
            weights.set(i, weights.get(i) / sum);

        }
    }
}