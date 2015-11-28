import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

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

    // Anchor
    public int anchor;

    // Received values
    public Value[] p1Values;
    public Value[] p2Values;
    public Value[] faultySetsValues;
    public Value leaderValue;

    // Lock
    Lock p1Lock = new ReentrantLock();
    Condition[] p1ValuesArrived;
    Condition[] p1ValuesCleared;

    Lock p2Lock = new ReentrantLock();
    Condition[] p2ValuesArrived;
    Condition[] p2ValuesCleared;

    Lock leaderLock = new ReentrantLock();
    Condition[] leaderValueArrived;
    Condition[] leaderValueCleared;

    Lock faultySetsLock = new ReentrantLock();
    final Condition faultySetsArrived = faultySetsLock.newCondition();

    // Value.TRUE for all nodes recognized as faulty
    public Value[] faultySet;

    // 0.0 for all nodes and raises if suspected faulty
    public Double[] suspectWeight;

    // My weight
    public double myWeight;

    //Message Handler for broadcasting control messages
    public MsgHandler msg;

    public boolean actFaulty;

    public int faultNode;

    public ConsensusAlgorithm(int i, int n, Value V, MsgHandler msg, List<Double> weights, boolean actFaulty) {
        this.i = i;
        this.N = n;
        this.V = V;
        this.weights = weights;

        this.p1Values = new Value[N];
        this.p2Values = new Value[N];

        this.faultySet = new Value[N];
        for (int j = 0; j < N; j++) setNodeFaultyState(j, Value.FALSE);

        this.faultySetsValues = new Value[N];

        this.msg = msg;
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

    public void resetValuesForNewRound(int round) {
        p1Lock.lock();
        p2Lock.lock();
        leaderLock.lock();

        try {
            Arrays.fill(p1Values, null);
            Arrays.fill(p2Values, null);
            leaderValue = null;

            p1ValuesCleared[round].signalAll();
            p2ValuesCleared[round].signalAll();
            leaderValueCleared[round].signalAll();
            MsgHandler.debug("Node " + i + " leader value has cleared in round " + round);

        } finally {
            p1Lock.unlock();
            p2Lock.unlock();
            leaderLock.unlock();
        }

    }

    // TODO Override this for Queen and King since myWeight will be different
    public Value checkForFaultyNode(int j, int round, Boolean queenAlgorithm){
        if (faultySet[j].equals(Value.TRUE)) {
            return Value.TRUE;
        } else if (myWeight > 3/4 && leaderValue != V && round == j){
            MsgHandler.debug("Node " + i + " accuses node " + j + " in round " + round + " with myWeight " + myWeight + " and received value " + leaderValue);
            return Value.TRUE;
        }
        else if (queenAlgorithm && p1Values[j] == Value.UNDECIDED){
            MsgHandler.debug("Node " + i + " accuses node " + j + " in round " + round + " with myWeight " + myWeight + " and received value " + p1Values[j]);
            return Value.TRUE;
        }
        else if (p1Values[j] == null){
            MsgHandler.debug("Node " + i + " accuses node " + j + " in round " + round + " with myWeight " + myWeight + " and received value " + p1Values[j]);
            return Value.TRUE;
        }
        else {
            return Value.FALSE;
        }
    }

    public void runFaultyNodePhase(int round, Boolean queenAlgorithm) {
        //Check for faulty nodes
        for (int j = 0; j < N; j++) {
            setNodeFaultyState(j, checkForFaultyNode(j, round, queenAlgorithm));
        }
    }

    public void runFaultyNodePhase(int round) {
        runFaultyNodePhase(round, false);
    }

    public void broadcastFaultySet() {
        msg.broadcastMsg(MessageType.FAULTY_SET, Arrays.asList(faultySet).toString(), false);

        addSuspectWeights(Utils.interpretStringAsList(Arrays.asList(faultySet).toString()), i);
    }

    public void addSuspectWeights(List<String> faultySet, int reporter){
        faultySetsLock.lock();
        try {
            for (int j = 0; j < faultySet.size(); j++){
                if (Value.valueOf(faultySet.get(j)).equals(Value.TRUE)) {
                    setNodeSuspectWeight(j, weights.get(reporter));
                }
            }

            faultySetsValues[reporter] = Value.TRUE;

            if (countNullValues(faultySetsValues) == 0) {
                faultySetsArrived.signal();
            }
        } finally {
            faultySetsLock.unlock();
        }
    }

    // TODO Override for Queen and King since the weight threshold will be different
    public void gatherFaultyNodes() {
        broadcastFaultySet();

        waitForFaultySetsValues();

        for (int j = 0; j < weights.size(); j++) {
            if (suspectWeight[j] >= 1.0/4) {
                setNodeFaultyState(j, Value.TRUE);
            }
        }
    }

    public int calculateAnchor() {
    	double p = rho;
    	double sum = 0;
    	anchor = 0;

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

        this.p1ValuesArrived = new Condition[anchor];
        Arrays.fill(p1ValuesArrived, p1Lock.newCondition());

        this.p1ValuesCleared = new Condition[anchor];
        Arrays.fill(p1ValuesCleared, p1Lock.newCondition());

        this.p2ValuesArrived = new Condition[anchor];
        Arrays.fill(p2ValuesArrived, p2Lock.newCondition());

        this.p2ValuesCleared = new Condition[anchor];
        Arrays.fill(p2ValuesCleared, p2Lock.newCondition());

        this.leaderValueArrived = new Condition[anchor];
        Arrays.fill(leaderValueArrived, leaderLock.newCondition());

        this.leaderValueCleared = new Condition[anchor];
        Arrays.fill(leaderValueCleared, leaderLock.newCondition());

        return anchor;
    }

    public void runPhaseOne(int round){}

    public void runPhaseTwo(int round){}

    public void runLeaderPhase(int round){}

    public void broadcastPhaseOneValue(Value V, int round) {
        if (!actFaulty) {
            msg.broadcastMsg(MessageType.PHASE_ONE_VALUE, V.toString() + "," + round, false);
            setPhaseOneNodeValue(i, V, round);
        } else {
            msg.broadcastMsg(MessageType.PHASE_ONE_VALUE, Value.UNDECIDED.toString()+ "," + round, false);
            setPhaseOneNodeValue(i, Value.UNDECIDED, round);
        }
    }

    public void broadcastPhaseTwoValue(Value V, int round) {
        if (!actFaulty) {
            msg.broadcastMsg(MessageType.PHASE_TWO_VALUE, V.toString() + "," + round, false);
            setPhaseTwoNodeValue(i, V, round);
        } else {
            msg.broadcastMsg(MessageType.PHASE_TWO_VALUE, Value.UNDECIDED.toString()+ "," + round, false);
            setPhaseTwoNodeValue(i, Value.UNDECIDED, round);
        }
    }

    public void broadcastLeaderValue(Value V, int round) {
        if (!actFaulty) {
            msg.broadcastMsg(MessageType.LEADER_VALUE, V.toString() + "," + round, false);
            setLeaderValue(V, round);
        } else {
            msg.broadcastMsg(MessageType.LEADER_VALUE, Value.UNDECIDED.toString()+ "," + round, false);
            setLeaderValue(Value.UNDECIDED, round);
        }
    }

    public int countNullValues(Value[] values) {
        int counter = 0;

        for (Value value : values) {
            if (value == null) {
                counter++;
            }
        }

        return counter;
    }

    public void waitForPhaseOneValues(int round){
        p1Lock.lock();
        try {
            if (countNullValues(p1Values) > 0) {
                p1ValuesArrived[round].await(Constants.VALUE_TIMEOUT, TimeUnit.MILLISECONDS);
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        } finally {
            p1Lock.unlock();
        }
    }

    public void waitForPhaseTwoValues(int round){
        p2Lock.lock();
        try {
            if (countNullValues(p2Values) > 0) {
                p2ValuesArrived[round].await(Constants.VALUE_TIMEOUT, TimeUnit.MILLISECONDS);
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        } finally {
            p2Lock.unlock();
        }
    }

    public void waitForLeaderValue(int round){
        leaderLock.lock();
        try {
            if (leaderValue == null) {
                MsgHandler.debug("Node " + i + " is waiting on leader value to arrive in round " + round);
                leaderValueArrived[round].await(Constants.VALUE_TIMEOUT, TimeUnit.MILLISECONDS);
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        } finally {
            leaderLock.unlock();
        }
    }

    public void waitForFaultySetsValues(){
        faultySetsLock.lock();
        try {
            if (countNullValues(faultySetsValues) > 0) {
                faultySetsArrived.await(Constants.VALUE_TIMEOUT, TimeUnit.MILLISECONDS);
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        } finally {
            faultySetsLock.unlock();
        }
    }

    public void setPhaseOneNodeValue(int nodeId, Value value, int round){
        p1Lock.lock();
        try {
            if (p1Values[nodeId] == null) {
                p1Values[nodeId] = value;

                if (countNullValues(p1Values) == 0) {
                    MsgHandler.debug("Node " + i + " has received phase one values in round " + round + ": " + Arrays.toString(p1Values));
                    p1ValuesArrived[round].signal();
                }
            } else {
                MsgHandler.debug("Node " + i + " is waiting on phase one values to be cleared in round " + round);
                p1ValuesCleared[round].await();
            }
        } catch (InterruptedException e) {
            MsgHandler.debug("Node " + i + " was interrupted waiting on phase one values to be cleared in round " + round);
            e.printStackTrace();
        } finally {
            p1Lock.unlock();
        }
    }

    public void setPhaseTwoNodeValue(int nodeId, Value value, int round){
        p2Lock.lock();
        try {
            if (p2Values[nodeId] == null) {
                p2Values[nodeId] = value;

                if (countNullValues(p2Values) == 0) {
                    p2ValuesArrived[round].signal();
                }
            } else {
                p2ValuesCleared[round].await();
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        } finally {
            p2Lock.unlock();
        }
    }

    public void setLeaderValue(Value value, int round){
        leaderLock.lock();
        try {
            if (leaderValue == null) {
                leaderValue = value;
                MsgHandler.debug("Node " + i + " has set leader value in round " + round);
                leaderValueArrived[round].signal();
            } else {
                MsgHandler.debug("Node " + i + " is waiting on leader value to be cleared in round " + round);
                leaderValueCleared[round].await();
            }
        } catch (InterruptedException e) {
            MsgHandler.debug("Node " + i + " was interrupted waiting on leader value to be cleared in round " + round);
            e.printStackTrace();
        } finally {
            leaderLock.unlock();
        }
    }
}