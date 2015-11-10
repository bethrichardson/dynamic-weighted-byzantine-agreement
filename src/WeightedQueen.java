import java.util.ArrayList;

/**
 * Created by neelshah on 10/31/15.
 */
public class WeightedQueen extends ConsensusAlgorithm {

    //Message Handler for broadcasting control messages
    private MsgHandler msg;

    public WeightedQueen(int i, int n, Value V, MsgHandler msg, ArrayList<Double> weights) {
        super(i, n, V, msg, weights);
        rho = 1.0/4;
    }

    @Override
    public void run(int anchor) {
        for (int q = 1; q <= anchor; q++) {
            double s0 = 0.0, s1 = 0.0;

            // Phase One
            if (weights.get(i) > 0) { //TODO: Must Broadcast value even if 0 weight to allow to redistributed weight when correct
                broadcastNormalValue(V);
                values[i] = V;
            }

            for (int j = 0; j < weights.size(); j++) {
                if (weights.get(j) > 0) {  //TODO: Weights must be given to the nodes. So this will work.
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

            // Phase Two
            if (q == i) {
                broadcastLeaderValue(myValue);
                V = leaderValue = myValue;
            } else {
                leaderValue = receiveLeaderValue();

                if (myWeight > 0.75) {
                    V = myValue;
                } else {
                    V = leaderValue;
                }
            }

            //Check for faulty nodes
            super.runFaultyNodePhase();
        }
    }
}