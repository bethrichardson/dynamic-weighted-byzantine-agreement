import java.util.ArrayList;

/**
 * Created by neelshah on 10/31/15.
 */


public class WeightedKing extends ConsensusAlgorithm {

    public WeightedKing(int i, int n, Value V, MsgHandler msg, ArrayList<Double> weights) {
        super(i, n, V, msg, weights, (2 * n)/3);
        rho = 1.0/3;
    }

    @Override
    public void runPhaseOne(){
        double s0 = 0.0, s1 = 0.0;

        // Phase One
        if (weights.get(i) > 0) {
            broadcastValue(V);
            values[i] = V;
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

        if (s0 >= 0.667) {
            V = Value.FALSE;
        } else if (s1 >= 0.667) {
            V = Value.TRUE;
        } else {
            V = Value.UNDECIDED;
        }
    }

    @Override
    public void runPhaseTwo(){
        double s0 = 0.0, s1 = 0.0, su = 0.0;

        if (weights.get(i) > 0) {
            broadcastValue(V);
            values[i] = V;
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

        if (s0 > 0.333) {
            V = Value.FALSE;
            myWeight = s0;
        } else if (s1 > 0.333) {
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
                broadcastValue(myValue);
                V = leaderValue = myValue;
            } else {
                waitForValues();

                leaderValue = receiveLeaderValue(k);

                if (V == Value.UNDECIDED || myWeight < 0.667) {
                    if (leaderValue == Value.UNDECIDED) {
                        V = Value.TRUE;
                    } else {
                        V = leaderValue;
                    }
                }
            }

            //Check for faulty nodes
            super.runFaultyNodePhase(k, false);
    }

}
