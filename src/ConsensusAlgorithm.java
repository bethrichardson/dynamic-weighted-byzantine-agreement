/**
 * Created by neelshah on 10/31/15.
 */
public class ConsensusAlgorithm {

    // Process id
    public int i;

    //number of nodes
    public int N;

    public double rho;

    // Weights
    public double[] w;

    // Proposed value
    public Value V;

    // Received values
    public Value[] values;

    // Value.TRUE for all nodes recognized as faulty
    public Value[] faultySet;

    // My value
    public Value myValue;

    // My weight
    public double myWeight;

    // Queen/King value
    public Value leaderValue;

    //Message Handler for broadcasting control messages
    public MsgHandler msg;

    public ConsensusAlgorithm(int i, int n, Value V, MsgHandler msg, double[] weights) {
        this.i = i;
        this.N = n;
        this.V = V;
        this.w = weights;
        this.msg = msg;
    }


    public void setNodeFaultyState(int j, Value faultyState){
        faultySet[j] = faultyState;
    }


    public Value checkForFaultyNode(Value receivedValue){
        if (myWeight > 3/4 && receivedValue != myValue){ //checks that matches myValue
            return Value.TRUE;
        }
        else{
            return Value.FALSE;
        }
    }

    public void runFaultyNodePhase() {
            //Check for faulty nodes
            for (int j = 0; j < values.length; j++) {
                setNodeFaultyState(j, checkForFaultyNode(values[j]));
            }
    }

    public int calculateAlpha(){
        // Need this
        return 0;
    }

    public void run(int alpha){}

    public void broadcastNormalValue(Value V) {
        msg.broadcastMsg("controlNormalValue," + V);
    }

    public void broadcastLeaderValue(Value V) {
        msg.broadcastMsg("controlLeaderValue," + V);
    }


    public Value receiveLeaderValue() {
        return Value.TRUE;
    }
}