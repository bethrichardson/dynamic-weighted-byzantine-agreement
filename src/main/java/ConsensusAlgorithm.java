import java.util.Arrays;
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

    // My weight
    public double myWeight;

    // Queen/King value
    public Value leaderValue;

    //Message Handler for broadcasting control messages
    public MsgHandler msg;

    public MessageType valueType;

    public boolean actFaulty;

    public ConsensusAlgorithm(int i, int n, Value V, MsgHandler msg, MessageType valueType, List<Double> weights, boolean actFaulty) {
        this.i = i;
        this.N = n;
        this.V = V;
        this.values = new Value[N];
        this.weights = weights;
        this.faultySet = new Value[N];
        for (int j = 0; j < N; j++) setNodeFaultyState(j, Value.FALSE);

        this.msg = msg;
        this.valueType = valueType;
        this.actFaulty = actFaulty;

        this.suspectWeight = new Double[N];
        for (int j = 0; j < N; j++) suspectWeight[j] = 0.0;
    }

    public void setNodeSuspectWeight(int j, Double weight){
        suspectWeight[j] += weight;
    }


    public void setNodeFaultyState(int j, Value faultyState){
        faultySet[j] = faultyState;
    }

    public void resetValues() {
        values = new Value[N];
    }


    // TODO Override this for Queen and King since myWeight will be different
    public Value checkForFaultyNode(Value receivedValue, int j, int round, Boolean queenAlgorithm){
        if (faultySet[j].equals(Value.TRUE)) {
            return Value.TRUE;
        } else if (myWeight > 3/4 && receivedValue != V && round == j){
            MsgHandler.debug("Node " + i + " accuses node " + j + " in round " + round + " with myWeight " + myWeight + " and received value " + receivedValue);
            return Value.TRUE;
        }
        else if (queenAlgorithm && values[j] == Value.UNDECIDED){
            MsgHandler.debug("Node " + i + " accuses node " + j + " in round " + round + " with myWeight " + myWeight + " and received value " + receivedValue);
            return Value.TRUE;
        }
        else if (values[j] == null){
            MsgHandler.debug("Node " + i + " accuses node " + j + " in round " + round + " with myWeight " + myWeight + " and received value " + receivedValue);
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

    public void broadcastFaultySet() {
        msg.broadcastMsg(MessageType.FAULTY_SET, Arrays.asList(faultySet).toString(), false);

        setNodeValue(i, Value.TRUE);
    }

    public void addSuspectWeights(List<String> faultySet, int reporter){
        for (int j = 0; j < faultySet.size(); j++){
            if (Value.valueOf(faultySet.get(j)).equals(Value.TRUE)) {
                setNodeSuspectWeight(j, weights.get(reporter));
            }
        }

        setNodeValue(reporter, Value.TRUE);
    }

    // TODO Override for Queen and King since the weight threshold will be different
    public void gatherFaultyNodes() {
        resetValues();

        broadcastFaultySet();

        waitForValues();

        for (int j = 0; j < weights.size(); j++) {
            if (suspectWeight[j] >= 1.0/4) {
                setNodeFaultyState(j, Value.TRUE);
            }
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
    			break;
    		}
    	}

        return anchor;
    }

    public void runPhaseOne(){}

    public void runPhaseTwo(){}

    public void runLeaderPhase(int round){}

    public void broadcast(Value V) {
        if (!actFaulty) {
            msg.broadcastMsg(valueType, V.toString(), false);
            setNodeValue(i, V);
        } else {
            msg.broadcastMsg(valueType, Value.UNDECIDED.toString(), false);
            setNodeValue(i, Value.UNDECIDED);
        }
    }

    public Value receiveLeaderValue(int currentRound) {
        return values[currentRound];
    }

    public int countNullValues(){
        int counter = 0;

        for (Value value : this.values) {
            if (value == null) {
                counter++;
            }
        }

        return counter;
    }

    public synchronized void waitForValues(){
        long deadline = System.currentTimeMillis() + Constants.VALUE_TIMEOUT;

        try {
            wait(Constants.VALUE_TIMEOUT);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public synchronized void setNodeValue(int nodeId, Value nodeValue){
        values[nodeId] = nodeValue;

        if (countNullValues() == 0) {
            notify();
        }
    }
}