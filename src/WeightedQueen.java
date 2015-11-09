/**
 * Created by neelshah on 10/31/15.
 */
public class WeightedQueen extends ConsensusAlgorithm {

    //Message Handler for broadcasting control messages
    private MsgHandler msg;

    public WeightedQueen(int i, int n, Value V, MsgHandler msg, double[] weights) {
        super(i, n, V, msg, weights);
        rho = 1/4;
    }

    @Override
    public int calculateAlpha(){
        // Need this
        return 0;
    }

    @Override
    public void run(int alpha) {
        for (int q = 1; q <= alpha; q++) {
            double s0 = 0.0, s1 = 0.0;

            // Phase One
            if (w[i] > 0) { //TODO: Must Broadcast value even if 0 weight to allow to redistributed weight when correct
                broadcastNormalValue(V);
                values[i] = V;
            }

            for (int j = 0; j < w.length; j++) {
                if (w[j] > 0) {  //TODO: Weights must be given to the nodes. So this will work.
                    if (values[j] == Value.TRUE) {
                        s1 += w[j];
                    } else {
                        s0 += w[j];
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