import java.util.ArrayList;
import java.util.Collections;

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
    public ArrayList<Double> weights;
 
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

    public ConsensusAlgorithm(int i, int n, Value V, MsgHandler msg, ArrayList<Double> weights) {
        this.i = i;
        this.N = n;
        this.V = V;
        this.values = new Value[N];
        this.weights = weights;
        this.faultySet = new Value[N];
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

    public int calculateAnchor() {
        	
    	double p = rho;
    	double sum = 0;
    	int anchor = 0;
    	
    	// sorting array in ascending order
    	Collections.sort(weights);
    	// reverse array so highest weights are at lower index
    	Collections.reverse(weights);
    	
    	for(int f = 0; f < weights.size(); f++) {
    		sum = sum + weights.get(f);
    		
    		if(sum > p){
    			anchor = f + 1;
    			MsgHandler.debug("Rho: %f " + rho);

    			MsgHandler.debug("Sum of minimum number of alphas > Rho: %f " + sum);
    			break;
    		}
    	}
        	
        return anchor;
    }

    public void run(int anchor){}

    public void broadcastValue(Value V) {
        msg.broadcastMsg("controlSetValue," + V);
    }

    public Value receiveLeaderValue(int currentRound) {
        return values[currentRound];
    }

    public void setNodeValue(int nodeId, Value nodeValue){
        values[nodeId] = nodeValue;
    }
}