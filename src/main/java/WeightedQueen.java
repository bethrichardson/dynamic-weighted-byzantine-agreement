import java.util.ArrayList;

/**
 * Created by neelshah on 10/31/15.
 */
public class WeightedQueen extends ConsensusAlgorithm {

    public WeightedQueen(int i, int n, Value V, MsgHandler msg, ArrayList<Double> weights) {
        super(i, n, V, msg, weights, (3 * n)/4);
        rho = 1.0/4;
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

        if (s1 > 0.5) {
            myValue = Value.TRUE;
            myWeight = s1;
        } else {
            myValue = Value.FALSE;
            myWeight = s0;
        }

    }


    @Override
    public void runLeaderPhase(int q) {
            // Phase Two
            if (q == i) {
                broadcastValue(myValue);
                V = leaderValue = myValue;
            } else {
                waitForValues();
                leaderValue = receiveLeaderValue(q);

                if (myWeight > 0.75) {
                    V = myValue;
                } else {
                    V = leaderValue;
                }
            }

            //Check for faulty nodes
            super.runFaultyNodePhase(q, true);
    }
}