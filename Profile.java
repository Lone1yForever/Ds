import java.util.concurrent.ThreadLocalRandom;

public interface Profile {
    default void Delay(){}
    default boolean shouldDrop(){return false;}
    static Profile reliable(){return new Profile(){};}
    static Profile standard(){
        return new Profile(){
            @Override public void Delay(){
                sleepRand(10,60);
            }
    };
    }
    static Profile latent(){
        return new Profile(){
            @Override public void Delay(){sleepRand(150,600);}
            @Override public boolean shouldDrop() { return ThreadLocalRandom.current().nextDouble() < 0.05; }
        };
    }
    static Profile failure(){
        return new Profile(){
            @Override public void Delay(){sleepRand(50, 200);}
            @Override public boolean shouldDrop() { return ThreadLocalRandom.current().nextDouble() < 0.20; }
        };
    }
    private static void sleepRand(int minMs, int maxMs) {
        try { Thread.sleep(ThreadLocalRandom.current().nextInt(minMs, maxMs + 1)); } catch (InterruptedException ignored) {}
    }
}
