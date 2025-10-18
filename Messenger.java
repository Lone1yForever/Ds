import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.net.Socket;


public final class Messenger {
    private final Networkconfig netconf;
    private final Profile profile;
    private final String myId;

    /**
     *
     * @param netconf host / post
     * @param profile delay / loss
     * @param myId    Personal ID
     */
    public Messenger(Networkconfig netconf, Profile profile, String myId){
        this.netconf = netconf;
        this.profile = profile;
        this.myId = myId;
    }

    /**
     * Send a message to the member
     * @param msg Message want to send
     */
    public void send(Message msg){
        if (profile.shouldDrop())
            return;
        profile.Delay();
        if("ALL".equals(msg.to)){
            for(Networkconfig.Entry e :netconf.all()){
                if(e.id.equals(myId))
                    continue;
                sendTo(e,msg);
            }
        }else{
            sendTo(netconf.get(msg.to), msg);
        }
    }

    /**
     *
     * @param e   Network address
     * @param msg Message want to send
     */
    private void sendTo(Networkconfig.Entry e, Message msg){
        if (e== null)
            return;
        try(Socket socket = new Socket(e.host, e.port);
            BufferedWriter w = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()))){
            w.write(msg.encode());
            w.flush();
        }catch(IOException ignored) { }
    }
}
