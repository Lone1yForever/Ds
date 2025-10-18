import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public class Networkconfig {
    public static final class Entry{
        public final String id;
        public final String host;
        public final int port;
        public Entry(String id, String host, int port){
            this.id = id;
            this.host = host;
            this.port = port;
        }
    }
    private final Map<String,Entry> map = new HashMap<String,Entry>();

    /**
     *
     * @param f Host & Post
     * @return Analysis result
     * @throws IOException fail or file not exist
     */
    public static Networkconfig load(File f) throws IOException {
        Networkconfig config = new Networkconfig();
        try(BufferedReader br = new BufferedReader(new FileReader(f))){
            String line;
            while((line = br.readLine()) != null){
                line = line.trim();
                if(line.isEmpty() || line.startsWith("#"))
                    continue;
                String[] p = line.split(",", -1);

                if (p.length < 3) {
                    throw new IOException("Bad config line (need id,host,port): " + line);
                }
                String id   = p[0].trim();
                String host = p[1].trim();
                String portStr = p[2].trim();
                int port;
                try {
                    port = Integer.parseInt(portStr);
                } catch (NumberFormatException e) {
                    throw new IOException("Bad port: " + portStr);
                }
                config.map.put(id, new Entry(id, host, port));
            }
        }
        return config;
    }

    /**
     * @param id Member ID
     * @return results
     */
    public Entry get(String id){return map.get(id);}

    /**
     * @return All results
     */
    public Collection <Entry> all(){return map.values();}

    /**
     * @return Member size
     */
    public int size(){return map.size();}
}
