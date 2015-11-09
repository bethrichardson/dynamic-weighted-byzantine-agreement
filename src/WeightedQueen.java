/**
 * Created by neelshah on 10/31/15.
 */
public class WeightedQueen {

    // Process id
    private int i;

    //number of nodes
    private int N;

    private double rho = 1/4;

    // Weights
    private double[] w;

    // Proposed value
    private Value V;

    // Received values
    private Value[] values;

    // My value
    private Value myValue;

    // My weight
    private double myWeight;

    // Queen value
    private Value queenValue;

    //Message Handler for broadcasting control messages
    private MsgHandler msg;

    public WeightedQueen(int i, int n, Value V, MsgHandler msg) {
        this.i = i;
        this.w = new double[n];
        this.N = n;
        this.V = V;
        this.msg = msg;
    }

    public int calculateAlpha(){
        // Need this
        return 0;
    }

    public void run(int alpha) {
        for (int q = 1; q <= alpha; q++) {
            double s0 = 0.0, s1 = 0.0;

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

            if (s1 > 0.5) {
                myValue = Value.TRUE;
                myWeight = s1;
            } else {
                myValue = Value.FALSE;
                myWeight = s0;
            }

            // Phase Two
            if (q == i) {
                broadcastQueenValue(myValue);
                V = queenValue = myValue;
            } else {
                queenValue = receiveQueenValue();

                if (myWeight > 0.75) {
                    V = myValue;
                } else {
                    V = queenValue;
                }
            }
        }
    }

    private void broadcastNormalValue(Value V) {
        msg.broadcastMsg("controlNormalValue," + V);
    }

    private void broadcastQueenValue(Value V) {
        msg.broadcastMsg("controlQueenValue," + V);
    }

    private Value receiveQueenValue() {
        return Value.TRUE;
    }
}