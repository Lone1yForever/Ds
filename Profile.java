import java.util.concurrent.ThreadLocalRandom;

/**
 * Simulate different network conditions
 * Members have 4 different situation : reliable/standard/latent/failure
 */
public interface Profile {
    default void Delay(){}

    /**
     * @return true = loss
     */
    default boolean shouldDrop(){return false;}
    static Profile reliable(){return new Profile(){};} // no loss
    static Profile standard(){   // no loss
        return new Profile(){
            @Override public void Delay(){
                sleepRand(5,30);
            }
    };
    }
    static Profile latent(){ //high latency
        return new Profile(){
            @Override public void Delay(){sleepRand(200,600);}
            @Override public boolean shouldDrop() { return ThreadLocalRandom.current().nextDouble() < 0.05; }
        };
    }
    static Profile failure(){ // Mid  latency . High packet loss probability
        return new Profile(){
            @Override
            public void Delay(){sleepRand(50, 200);}
            @Override
            public boolean shouldDrop() { return ThreadLocalRandom.current().nextDouble() < 0.15; }
        };
    }
    private static void sleepRand(int minMs, int maxMs) {
        try { Thread.sleep(ThreadLocalRandom.current().nextInt(minMs, maxMs + 1));
        } catch (InterruptedException ignored) {}
    }
}
