import java.util.List;

/**
 * Created by neelshah on 11/27/15.
 */
public class WeightedQueenFault extends WeightedQueen {

    public WeightedQueenFault(int i, int n, Value V, MsgHandler msg, List<Double> weights, boolean actFaulty) {
        super(i, n, V, msg, weights, actFaulty);
    }

    @Override
    public void broadcastPhaseOneValue(Value V, int round) {
        if (!actFaulty) {
            msg.broadcastMsg(MessageType.FAULT_PHASE_ONE_VALUE, V.toString() + "," + round + "," + faultNode, false);
            setPhaseOneNodeValue(i, V, round);
        } else {
            msg.broadcastMsg(MessageType.FAULT_PHASE_ONE_VALUE, Value.UNDECIDED.toString()+ "," + round + "," + faultNode, false);
            setPhaseOneNodeValue(i, Value.UNDECIDED, round);
        }
    }

    @Override
    public void broadcastPhaseTwoValue(Value V, int round) {
        if (!actFaulty) {
            msg.broadcastMsg(MessageType.FAULT_PHASE_TWO_VALUE, V.toString() + "," + round + "," + faultNode, false);
            setPhaseTwoNodeValue(i, V, round);
        } else {
            msg.broadcastMsg(MessageType.FAULT_PHASE_TWO_VALUE, Value.UNDECIDED.toString()+ "," + round + "," + faultNode, false);
            setPhaseTwoNodeValue(i, Value.UNDECIDED, round);
        }
    }

    @Override
    public void broadcastLeaderValue(Value V, int round) {
        if (!actFaulty) {
            msg.broadcastMsg(MessageType.FAULT_LEADER_VALUE, V.toString() + "," + round + "," + faultNode, false);
            setLeaderValue(V, round);
        } else {
            msg.broadcastMsg(MessageType.FAULT_LEADER_VALUE, Value.UNDECIDED.toString()+ "," + round + "," + faultNode, false);
            setLeaderValue(Value.UNDECIDED, round);
        }
    }
}