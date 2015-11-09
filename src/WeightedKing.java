/**
 * Created by neelshah on 10/31/15.
 */
public class WeightedKing extends ConsensusAlgorithm {

    public WeightedKing(int i, int n, Value V, MsgHandler msg, double[] weights) {
        super(i, n, V, msg, weights);
    }

    @Override
    public int calculateAlpha(){
        // Need this
        return 0;
    }

    @Override
    public void run(int alpha) {
        for (int k = 1; k <= alpha; k++) {
            double s0 = 0.0, s1 = 0.0, su = 0.0;

            // Phase One
            if (w[i] > 0) {
                broadcastNormalValue(V);
                values[i] = V;
            }

            for (int j = 0; j < w.length; j++) {
                if (w[j] > 0) {
                    if (values[j] == Value.TRUE) {
                        s1 += w[j];
                    } else {
                        s0 += w[j];
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

            // Phase Two
            s0 = 0.0;
            s1 = 0.0;
            su = 0.0;

            if (w[i] > 0) {
                broadcastNormalValue(V);
                values[i] = V;
            }

            for (int j = 0; j < w.length; j++) {
                if (w[j] > 0) {
                    if (values[j] == Value.TRUE) {
                        s1 += w[j];
                    } else if (values[j] == Value.FALSE) {
                        s0 += w[j];
                    } else {
                        su += w[j];
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

            // Phase Three
            if (k == i) {
                broadcastLeaderValue(myValue);
                V = leaderValue = myValue;
            } else {
                leaderValue = receiveLeaderValue();

                if (V == Value.UNDECIDED || myWeight < 0.667) {
                    if (leaderValue == Value.UNDECIDED) {
                        V = Value.TRUE;
                    } else {
                        V = leaderValue;
                    }
                }
            }

            //Check for faulty nodes
            super.runFaultyNodePhase();
        }
    }

}
