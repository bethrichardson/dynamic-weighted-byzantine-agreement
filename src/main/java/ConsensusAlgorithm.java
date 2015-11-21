import java.util.Collections;
import java.util.List;

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
    public List<Double> weights;
 
    // Proposed value
    public Value V;

    // Received values
    public Value[] values;

    // Value.TRUE for all nodes recognized as faulty
    public Value[] faultySet;

    // 0.0 for all nodes and raises if suspected faulty
    public Double[] suspectWeight;

    // My value
    public Value myValue;

    // My weight
    public double myWeight;

    // Queen/King value
    public Value leaderValue;

    //Message Handler for broadcasting control messages
    public MsgHandler msg;

    public int numNodesToWaitFor;

    public ConsensusAlgorithm(int i, int n, Value V, MsgHandler msg, List<Double> weights, int numNodesToWaitFor) {
        this.i = i;
        this.N = n;
        this.V = V;
        this.values = new Value[N];
        this.weights = weights;
        this.faultySet = new Value[N];
        this.msg = msg;
        this.numNodesToWaitFor = numNodesToWaitFor;
        this.suspectWeight = new Double[N];
        for (int j = 0; j < N; j++) suspectWeight[j] = 0.0;
    }

    public void setNodeSuspectWeight(int j, Double weight){
        suspectWeight[j] += weight;
    }


    public void setNodeFaultyState(int j, Value faultyState){
        faultySet[j] = faultyState;
    }


    public Value checkForFaultyNode(Value receivedValue, int j, int round, Boolean queenAlgorithm){
        MsgHandler.debug("Node " + i + "is checking for faulty node " + j + " in round " + round + " with myWeight " + myWeight + " and received value " + receivedValue);

        if (myWeight > 3/4 && receivedValue != myValue && round == j){
            return Value.TRUE;
        }
        else if (queenAlgorithm && values[j] == Value.UNDECIDED){
            return Value.TRUE;
        }
        else if (values[j] == null){
            return Value.TRUE;
        }
        else {
            return Value.FALSE;
        }
    }

    public void runFaultyNodePhase(int round, Boolean queenAlgorithm) {
        //Check for faulty nodes
        for (int j = 0; j < values.length; j++) {
            setNodeFaultyState(j, checkForFaultyNode(values[j], j, round, queenAlgorithm));
        }
    }

    public void runFaultyNodePhase(int round) {
        runFaultyNodePhase(round, false);
    }

    public void broadcastFaulty(int j) {
        msg.broadcastMsg(MessageType.FaultyNode, Integer.toString(j), false);
    }

    public void gatherFaultyNodes() {
        for (int j = 0; j < weights.size(); j++) {
            if (faultySet[j] == Value.TRUE) {
                broadcastFaulty(j);
            }
        }

        waitForValues();

        for (int j = 0; j < weights.size(); j++) {
            if (suspectWeight[j] >= 1.0/4) {
                faultySet[j] = Value.TRUE;
            }
        }
    }

    public synchronized int calculateAnchor() {
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
    			break;
    		}
    	}

        return anchor;
    }

    public void runPhaseOne(){}

    public void runPhaseTwo(){}

    public void runLeaderPhase(int round){}

    public void broadcastValue(Value V) {
        msg.broadcastMsg(MessageType.SetValue, V.toString(), false);
    }

    public Value receiveLeaderValue(int currentRound) {
        return values[currentRound];
    }

    public int countNonNullValues(){
        int counter = 0;
        for (int i = 0; i < this.values.length; i ++)
            if (this.values[i] != null)
                counter ++;
        return counter;
    }

    public void waitForValues(){
        Utils.timedWait(100, "Node " + Integer.toString(i) + " currently has " +
            Integer.toString(countNonNullValues()) + " non-null values." );
    }

    public void setNodeValue(int nodeId, Value nodeValue){
        values[nodeId] = nodeValue;
    }
}