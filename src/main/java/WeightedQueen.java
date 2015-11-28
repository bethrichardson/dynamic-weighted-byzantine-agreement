import java.util.Arrays;
import java.util.List;

/**
 * Created by neelshah on 10/31/15.
 */
public class WeightedQueen extends ConsensusAlgorithm {

    public WeightedQueen(int i, int n, Value V, MsgHandler msg, List<Double> weights, boolean actFaulty) {
        super(i, n, V, msg, weights, actFaulty);
        rho = 1.0/4.0;
        calculateAnchor();
    }

    @Override
     public void runPhaseOne(int q){
        double s0 = 0.0, s1 = 0.0;

        // Phase One
        if (weights.get(i) > 0) {
            broadcastPhaseOneValue(V, q);
        }

        waitForPhaseOneValues(q);

        for (int j = 0; j < weights.size(); j++) {
            if (weights.get(j) > 0) {
                if (p1Values[j] == Value.TRUE) {
                    s1 += weights.get(j);
                } else {
                    s0 += weights.get(j);
                }
            }
        }

        if (s1 > 0.5) {
            V = Value.TRUE;
            myWeight = s1;
        } else {
            V = Value.FALSE;
            myWeight = s0;
        }
    }

    @Override
    public void runLeaderPhase(int q) {
        // Phase Two
        if (q == i) {
            MsgHandler.debug("Node " + i + " is leader in round " + q + " and has value: " + V);
            broadcastLeaderValue(V, q);
        } else {
            waitForLeaderValue(q);

            if (myWeight <= 0.75) {
                V = leaderValue;
            }
        }
    }

    @Override
    public void runFaultyNodePhase(int round) {
        super.runFaultyNodePhase(round, true);
    }
}