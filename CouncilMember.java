
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.*;

public class CouncilMember {
    private final String id;
    private final Networkconfig netconf;
    private final Profile profile;
    private final PaxosState state = new PaxosState();
    private final Messenger messenger;
    private final int quorum;
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();

    private static final class Promise {long acceptedId;String acceptedValue;}
    private volatile long activePid= -1 ;
    private volatile String proposedValue = null;
    private final Map<String,Promise> promises = new ConcurrentHashMap<>();
    private final Set <String> Confirmation= ConcurrentHashMap.newKeySet();
    private volatile boolean decided = false;
    public CouncilMember(String id, Networkconfig netconf, Profile profile){
        this.id = id;
        this.netconf = netconf;
        this.profile = profile;
        this.messenger = new Messenger(netconf,profile,id);
        this.quorum = netconf.size()/2+1;

    }
    /**
     * @param port Listening interface
     * @throws IOException Port is occupied.
     */
    private void startServer(int port) throws IOException {
        ServerSocket serverSocket = new ServerSocket(port);
        System.out.println("[" + id + "] LISTENING on " + port);
        Executors.newSingleThreadExecutor().submit(()->{
            while(true){
                try(Socket socket = serverSocket.accept();
                    BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8))){
                    String line = in.readLine();
                    if(line!=null)
                        handle (Message.decode(line));
                }catch(Exception e){
                    System.err.println("[" + id + "] recv error: " + e.getMessage());
                }
            }
        });
    }

    /**
     *
     * @param message Decoded message
     */
    private void handle(Message message){
        switch (message.type){
            case ADMIN_PROPOSE : onAdminPropose(message.Candidate);
            break;
            case PREPARE : onPrepare (message);
            break;
            case PROMISE : onPromise(message);
            break;
            case NACK:onNack(message);
            break;
            case ACCEPT_REQUEST: onAcceptRequest(message);
            break;
            case ACCEPTED: onAccepted(message);
            break;
            default:
            break;
        }
    }

    /**
     * create new proposal number
     * @return New proposal number
     */
    private long newProposalId(){
        int num  = Integer.parseInt(id.substring(1));
        return Instant.now().toEpochMilli() *100L+num;
    }

    /**
     * @param candidate M1-M9
     */
    private void onAdminPropose(String candidate){
        if(candidate == null || candidate.isEmpty())
            return;
        beginProposal(candidate);
    }

    /**
     *Broadcast and creat a new vote
     * Timeout : creat a new proposal number and retry
     * @param candidate M1-M9
     */
    private void beginProposal(String candidate){
        this.activePid = newProposalId();
        this.proposedValue = candidate;
        this.promises.clear();
        this.Confirmation.clear();
        broadcast(new Message(MessageType.PREPARE,id,"ALL",activePid,null,-1));
        System.out.println("[" + id + "] PROPOSER: sent PREPARE pid=" + activePid + " value=" + candidate);

        scheduler.schedule(()->{
            if (!decided && Confirmation.size() < quorum){
                System.out.println("[" + id + "] TIMEOUT: retry with higher proposal id");
                beginProposal(candidate);
            }
        }, 1500, TimeUnit.MILLISECONDS);
    }

    /**
     * @param message PROMISE message
     */
    private  void onPromise(Message message){
        if(message.ProposalNumber != activePid)
            return;
        Promise p = new Promise();
        p.acceptedId = message.AcceptID;
        p.acceptedValue = message.Candidate;
        promises.put(message.from,p);
        if(promises.size() >= quorum){
            long maxAid = -1;
            String v = proposedValue;
            for(Promise pr : promises.values()){
                if(pr.acceptedValue != null && pr.acceptedId > maxAid){
                    maxAid = pr.acceptedId;
                    v=pr.acceptedValue;
                }
            }
            broadcast(new Message(MessageType.ACCEPT_REQUEST, id, "ALL", activePid, v, -1L));
            System.out.println("[" + id + "] PROPOSER: sent ACCEPT_REQUEST pid=" + activePid + " value=" + v);
        }
    }
    private void onNack(Message message) {
        if (message.ProposalNumber == activePid)
            System.out.println("[" + id + "] NACK from " + message.from);
    }

    /**
     * @param message ACCEPTED message
     */
    private void onAccepted(Message message){
        if(message.ProposalNumber != activePid)
            return;
        Confirmation.add(message.from);
        if(Confirmation.size() >= quorum){
            broadcast(new Message(MessageType.DECIDE, id, "ALL", activePid, message.Candidate, -1L));
            onDecide(message.Candidate);
        }
    }

    /**
     * @param message PREPARE message
     */
    private void onPrepare(Message message){
        if(profile.shouldDrop())
            return;
        profile.Delay();
        boolean ok  = state.onPrepare(message.ProposalNumber ) ;
        if(ok){
            Message reply = new Message(MessageType.PROMISE,
                    id,
                    message.from,
                    message.ProposalNumber,
                    state.getAcceptedValue(),
                    state.getAcceptedId());
messenger.send(reply);
            System.out.println("[" + id + "] ACCEPTOR: PROMISE to " + message.from
                    + " pid=" + message.ProposalNumber
                    + " (acceptedId=" + state.getAcceptedId()
                    + ", value=" + state.getAcceptedValue() + ")");
        }else{
            Message nack = new Message(MessageType.NACK, id, message.from, state.getPromisedId(), null, -1L);
            messenger.send(nack);
        }
}

    /**
     * As an acceptor: Handle the ACCEPT_REQUEST and return it as ACCEPTED or NACK.
     * @param message ACCEPT_REQUEST message
     */
    private void onAcceptRequest(Message message){
        if(profile.shouldDrop())
        return;
        profile.Delay();
        boolean ok  = state.onAcceptedRequest(message.ProposalNumber,message.Candidate) ;
        if(ok){
            Message ack = new Message(MessageType.ACCEPTED, id, message.from, message.ProposalNumber, message.Candidate, -1L);
            messenger.send(ack);
            int c = state.addAccepted(message.Candidate);
            System.out.println("[" + id + "] ACCEPTOR: ACCEPTED pid=" + message.ProposalNumber
                    + " value=" + message.Candidate + " local count=" + c);
        }else{
            Message nack = new Message(MessageType.NACK, id, message.from, state.getPromisedId(),null, -1L);
            messenger.send(nack);
        }
}

    /**
     * As a learner: Learn the final decision
     * @param Candidate winner
     */
    private synchronized void onDecide(String Candidate){
        if(decided)
            return;
        decided = true;
        System.out.printf("CONSENSUS: "+ Candidate +" has been elected Council President!");
        System.out.flush();
    }

    private void broadcast(Message message){ messenger.send(message); }
    public static void main (String[] args) throws Exception{
        if(args.length <2 ){
            System.err.println("java paxos.CouncilMember <MID> --profile <reliable|standard|latent|failure> [--config network.config]");
            System.exit(1);
        }
        String mid = args[0];
        String profileName = "standard";
        String configPath = "network.config";
        for(int i =1; i < args.length; i++){
            if("--profile".equals(args[i])&& i+1 < args.length)
                profileName = args[++i];
            else if ("--config".equals(args[i])&& i+1 < args.length)
                configPath = args[++i];
        }
        Profile profile;
        switch (profileName){
            case "reliable":
                profile = Profile.reliable();
                break;
                case "latent":
                    profile = Profile.latent();
                    break;
                    case "failure":
                        profile = Profile.failure();
                        break;
                        default:
                            profile = Profile.standard();
                            break;
        }
        Networkconfig netconf = Networkconfig.load(new File(configPath));
        Networkconfig.Entry self = netconf.get(mid);
        if(self == null)
            throw new IllegalArgumentException("Unknown member id " + mid);
        CouncilMember node  = new CouncilMember(mid,netconf,profile);
        node.startServer(self.port );
        BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
        String line;
        while((line = br.readLine()) != null){
            line = line.trim();
            if(line.startsWith("propose ")){
                String candidate =line.substring("propose ".length()).trim();
                node.beginProposal(candidate);
            }
        }
    }
 }
