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
    public static Networkconfig load(File f) throws IOException {
        Networkconfig config = new Networkconfig();
        try(BufferedReader br = new BufferedReader(new FileReader(f))){
            String line;
            while((line = br.readLine()) != null){
                line = line.trim();
                if(line.isEmpty() || line.startsWith("#"))
                    continue;
                String[] p = line.split(",");
                config.map.put(p[0],new Entry(p[1],p[2],Integer.parseInt(p[3])));
            }
        }
        return config;
    }
    public Entry get(String id){return map.get(id);}
    public Collection <Entry> all(){return map.values();}
    public int size(){return map.size();}
}
