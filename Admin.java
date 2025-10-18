import java.io.*;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

public final class Admin {
    /**
     * Ask a member to initiate a proposal for a certain candidate as a Proposer.
     * @param args Command line parameter
     * @throws Exception Read failed, target member does not exist, network connection, write failed
     */
    public static void main (String[] args) throws Exception{
        if (args.length<3){
            System.err.println(" java -cp out Admin <fromMID> <toMID> <candidate> [--config <path/to/network.config>]");
            System.exit(1);
        }
        String from = args[0];
        String to = args[1];
        String candidate = args[2];
        String configPath = (args.length >= 5 && "--config".equals(args[3])) ? args[4] : "network.config";
        Networkconfig netconf = Networkconfig.load(new File(configPath));
        Networkconfig.Entry e = netconf.get(to);
        if (e == null)
            throw new IllegalArgumentException("Unknown toMid" + to);
        Message message = Message.adminPropose(from,to,candidate);
        try (Socket s = new Socket(e.host,e.port);
             BufferedWriter w = new BufferedWriter(new OutputStreamWriter(s.getOutputStream(), StandardCharsets.UTF_8 ))){
            w.write(message.encode());
            w.flush();
        }
    }
}
