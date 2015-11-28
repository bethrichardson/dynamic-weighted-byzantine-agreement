import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class ConsensusAlgorithm {

    // Process id
    public final int i;

    // number of nodes
    public final int N;

    public double rho;

    // Weights
    public List<Double> weights;

    // Proposed value
    public Value V;

    final Semaphore valuesSemaphore;

    final Semaphore leaderSemaphore;

    final Semaphore finishReadySemaphore;

    final Semaphore startReadySemaphore;

    // Received values
    final Value[] values;

    // Received finishReady to change round values
    final Integer[] finishReady;

    // Received startReady to begin round values
    final Integer[] startReady;

    // Value.TRUE for all nodes recognized as faulty
    final Value[] faultySet;

    // 0.0 for all nodes and raises if suspected faulty
    final Double[] suspectWeight;

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
        this.valuesSemaphore = new Semaphore(0);
        this.finishReadySemaphore = new Semaphore(0);
        this.startReadySemaphore = new Semaphore(0);
        this.leaderSemaphore = new Semaphore(0);
        this.values = new Value[N];
        this.finishReady = new Integer[N];
        this.startReady = new Integer[N];
        this.weights = weights;
        this.faultySet = new Value[N];
        Arrays.fill(faultySet, Value.FALSE);

        this.msg = msg;
        this.valueType = valueType;
        this.actFaulty = actFaulty;

        this.suspectWeight = new Double[N];
        Arrays.fill(suspectWeight, 0.0);
    }

    public void setNodeSuspectWeight(int j, Double weight){
        suspectWeight[j] += weight;
    }


    public void setNodeFaultyState(int j, Value faultyState){
        faultySet[j] = faultyState;
    }

    public void resetValues() {
        Arrays.fill(values, null);
        leaderValue = null;
        valuesSemaphore.drainPermits();
        leaderSemaphore.drainPermits();
    }

    public void resetReady() {
        Arrays.fill(finishReady, null);
        finishReadySemaphore.drainPermits();

        Arrays.fill(startReady, null);
        startReadySemaphore.drainPermits();
    }


    public Value checkForFaultyNode(Value receivedValue, int j, int round, Boolean queenAlgorithm){
        if (faultySet[j].equals(Value.TRUE)) {
            return Value.TRUE;
        } else if (myWeight > N - rho && receivedValue != V && round == j){
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
            if (suspectWeight[j] >= 0.25) {
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

    public void runPhaseOne(int round){}

    public void runPhaseTwo(int round){}

    public void runLeaderPhase(int round){}

    public void runFinalizeRound(int round){
        resetValues();
        msg.broadcastMsg(MessageType.FINALIZE_ROUND, Integer.toString(round), true);

        waitForAcks();
        resetReady();
    }

    public void readyRound(int round){
        resetValues();
        try {
            Thread.sleep(150);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        msg.broadcastMsg(MessageType.START_ROUND, Integer.toString(round), true);

        waitForStartAcks();
        resetReady();
    }

    public void broadcast(Value V) {
        if (!actFaulty) {
            msg.broadcastMsg(valueType, V.toString(), true);
            setNodeValue(i, V);
        } else {
            msg.broadcastMsg(valueType, Value.UNDECIDED.toString(), true);
            setNodeValue(i, Value.UNDECIDED);
        }
    }

    public void broadcastLeaderValue(Value V) {
        if (!actFaulty) {
            msg.broadcastMsg(MessageType.LEADER_VALUE, V.toString(), true);
            setLeaderValue(i, V);
        } else {
            msg.broadcastMsg(MessageType.LEADER_VALUE, Value.UNDECIDED.toString(), true);
            setLeaderValue(i, Value.UNDECIDED);
        }
    }

    public Value receiveLeaderValue() {
        return leaderValue;
    }

    public void setValue(Value value){
        V = value;
    }

    public Value getValue(){
        return V;
    }

    public final boolean waitForValues() {
        try {
            if(!valuesSemaphore.tryAcquire(3 * N/4, Constants.VALUE_TIMEOUT, TimeUnit.MILLISECONDS)) {
                for (int j = 0; j < this.values.length; j ++) {
                    if (this.values[j] == null) {
                        MsgHandler.debug("Node " + i + " is missing value for node " + j);
                    }
                }
//                throw new RuntimeException(new TimeoutException("Value timeout on node " + i));
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

    public final boolean waitForAcks() {
        try {
            if(!finishReadySemaphore.tryAcquire(N - 1, Constants.VALUE_TIMEOUT, TimeUnit.MILLISECONDS)) {
//                throw new RuntimeException(new TimeoutException("Ready for finish round timeout on node " + i));
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

    public final boolean waitForStartAcks() {
        try {
            if(!startReadySemaphore.tryAcquire(N - 1, Constants.VALUE_TIMEOUT, TimeUnit.MILLISECONDS)) {
//                throw new RuntimeException(new TimeoutException("Ready for new round timeout on node " + i));
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

    public final void waitForLeaderValue() {
        try {
            if(!leaderSemaphore.tryAcquire(1, Constants.VALUE_TIMEOUT, TimeUnit.MILLISECONDS)) {
//                throw new RuntimeException(new TimeoutException("Leader value timeout on node " + i));
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public final void setNodeValue(int nodeId, Value nodeValue) {
        MsgHandler.debug("Algo:Node " + i + " is setting value to " + nodeValue + " on node " + nodeId);
        if (values[nodeId] != null) {
//            throw new IllegalStateException("Attempting to overwrite value " + values[nodeId] + " with value "
//                    + nodeValue + " on node " + nodeId);
        }
        values[nodeId] = nodeValue;
        valuesSemaphore.release();
    }

    public final void setFinishReady(int nodeId, String nodeValue) {
        if (finishReady[nodeId] != null) {
//            throw new IllegalStateException("Attempting to overwrite value " + finishReady[nodeId] + " with finishReady value "
//                    + nodeValue + " on node " + nodeId);
        }
        finishReady[nodeId] = Integer.parseInt(nodeValue);
        finishReadySemaphore.release();
    }

    public final void setStartReady(int nodeId, String nodeValue) {
        MsgHandler.debug("Algo:Node " + i + " is setting startReady value for round " + nodeValue + " on node " + nodeId);
        if (startReady[nodeId] != null) {
//            throw new IllegalStateException("Attempting to overwrite value " + startReady[nodeId] + " with startReady value "
//                    + nodeValue + " on node " + nodeId);
        }
        startReady[nodeId] = Integer.parseInt(nodeValue);
        startReadySemaphore.release();
    }

    public final void setLeaderValue(int nodeId, Value nodeValue) {
        MsgHandler.debug("Algo:Node " + i + " is setting leader value to " + nodeValue + " on node " + nodeId);
        if (leaderValue != null) {
//            throw new IllegalStateException("Attempting to overwrite leader value " + values[nodeId] + " with value "
//                    + nodeValue + " on node " + nodeId);
        }
        leaderValue = nodeValue;
        leaderSemaphore.release();
    }
}