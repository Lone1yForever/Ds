
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class PaxosState {
    private long promisedId= -1;
    private long acceptedId= -1;
    private String acceptedValue= null;

    /**@return Max proposal number A value less than this will be rejected */
    public synchronized long getPromisedId() {return promisedId;}

    /**@return The proposal number that has been accepted*/
    public synchronized long getAcceptedId() {return acceptedId;}

    /**@return The candidate that has been accepted*/
    public synchronized String getAcceptedValue() {return acceptedValue;}

    /**
     * @param n Proposition
     * @return true = promise
     */
    public synchronized boolean onPrepare(long n ){
        if (n>promisedId){
            promisedId = n;
            return true;
        }
        return false;
    }

    /**
     *Handle ACCEPT_REQUEST(n, v) : If n ≥ promisedId, accept and update acceptedId/acceptedValue
     * @param n proposition
     * @param v candidate
     * @return true = promise
     */
    public synchronized boolean onAcceptedRequest(long n,String v){
        if (n>=promisedId){
            promisedId = n;
            acceptedId = n;
            acceptedValue= v;
            return true;
        }
        return false;
    }
    private final Map<String,Integer> acceptCount = new ConcurrentHashMap<>();

    /**
     * @param v Candidate
     * @return The number of times this candidate has been elected
     */
    public int addAccepted(String v){
        return acceptCount.merge(v,1,Integer::sum);
    }
}
