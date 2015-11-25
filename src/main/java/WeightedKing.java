import java.util.List;

/**
 * Created by neelshah on 10/31/15.
 */
public class WeightedKing extends ConsensusAlgorithm {

    public WeightedKing(int i, int n, Value V, MsgHandler msg, MessageType valueType, List<Double> weights, boolean actFaulty) {
        super(i, n, V, msg, valueType, weights, actFaulty);
        rho = 1.0/3;
    }

    @Override
    public void runPhaseOne(int k){
        readyRound(k);
        MsgHandler.debug("Node " + i + " is entering phase one of King algorithm for round " + k + ".");
        double s0 = 0.0, s1 = 0.0;
        resetValues();

        // Phase One
        if (weights.get(i) > 0) {
            broadcast(V);
        }

        waitForValues();

        for (int j = 0; j < weights.size(); j++) {
            if (weights.get(j) > 0) {
                if (values[j] == Value.TRUE) {
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
        MsgHandler.debug("Node " + i + " is entering phase two of King algorithm for round " + k + ".");
        double s0 = 0.0, s1 = 0.0, su = 0.0;

        if (weights.get(i) > 0) {
            broadcast(V);
        }

        waitForValues();

        for (int j = 0; j < weights.size(); j++) {
            if (weights.get(j) > 0) {
                if (values[j] == Value.TRUE) {
                    s1 += weights.get(j);
                } else if (values[j] == Value.FALSE) {
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
        MsgHandler.debug("Node " + i + " is entering leader phase of King algorithm for round " + k + ".");
        if (k == i) {
            broadcastLeaderValue(V);
        } else {
            waitForValues();

            leaderValue = receiveLeaderValue();

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