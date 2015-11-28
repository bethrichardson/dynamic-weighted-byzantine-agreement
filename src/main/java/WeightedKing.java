import java.util.List;

/**
 * Created by neelshah on 10/31/15.
 */
public class WeightedKing extends ConsensusAlgorithm {

    public WeightedKing(int i, int n, Value V, MsgHandler msg, List<Double> weights, boolean actFaulty) {
        super(i, n, V, msg, weights, actFaulty);
        rho = 1.0/3.0;
        calculateAnchor();
    }

    @Override
    public void runPhaseOne(int k){
        double s0 = 0.0, s1 = 0.0;

        // Phase One
        if (weights.get(i) > 0) {
            broadcastPhaseOneValue(V, k);
        }

        waitForPhaseOneValues(k);

        for (int j = 0; j < weights.size(); j++) {
            if (weights.get(j) > 0) {
                if (p1Values[j] == Value.TRUE) {
                    s1 += weights.get(j);
                } else {
                    s0 += weights.get(j);
                }
            }
        }

        if (s0 >= (2.0/3.0)) {
            V = Value.FALSE;
        } else if (s1 >= (2.0/3.0)) {
            V = Value.TRUE;
        } else {
            V = Value.UNDECIDED;
        }
    }

    @Override
    public void runPhaseTwo(int k){
        double s0 = 0.0, s1 = 0.0, su = 0.0;

        if (weights.get(i) > 0) {
            broadcastPhaseTwoValue(V, k);
        }

        waitForPhaseTwoValues(k);

        for (int j = 0; j < weights.size(); j++) {
            if (weights.get(j) > 0) {
                if (p2Values[j] == Value.TRUE) {
                    s1 += weights.get(j);
                } else if (p2Values[j] == Value.FALSE) {
                    s0 += weights.get(j);
                } else {
                    su += weights.get(j);
                }
            }
        }

        if (s0 > (1.0/3.0)) {
            V = Value.FALSE;
            myWeight = s0;
        } else if (s1 > (1.0/3.0)) {
            V = Value.TRUE;
            myWeight = s1;
        } else {
            V = Value.UNDECIDED;
            myWeight = su;
        }
    }

    @Override
    public void runLeaderPhase(int k) {
        if (k == i) {
            broadcastLeaderValue(V, k);
        } else {
            waitForLeaderValue(k);

            if (V == Value.UNDECIDED || myWeight < (2.0/3.0)) {
                if (leaderValue == Value.UNDECIDED) {
                    V = Value.TRUE;
                } else {
                    V = leaderValue;
                }
            }
        }
    }

    @Override
    public void runFaultyNodePhase(int round) {
        runFaultyNodePhase(round, false);
    }
}