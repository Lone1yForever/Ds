
public final class Message {
    public final MessageType type;
    public final String from;
    public final String to;
    public final long ProposalNumber;
    public final String Candidate;
    public final long AcceptID;
    /**
     * Creat a Message object
     * @param type             Message type
     * @param from             Sender ID
     * @param to               Receiver ID
     * @param ProposalNumber   Proposition
     * @param Candidate        M1-M9
     * @param AcceptID 。      Max ProposalNumber
     */
public Message (MessageType type,String from,String to,long ProposalNumber,String Candidate,long AcceptID){
    this.type = type;
    this.from = from;
    this.to = to ;
    this.ProposalNumber = ProposalNumber;
    this.Candidate = Candidate;
    this.AcceptID = AcceptID;
}
    /**
     *
     * @param from Sender ID
     * @param to   Sender ID
     * @param Candidate M1-M9
     * @return ADMIN_PROPOSE type Message
     */
    public static Message adminPropose(String from,String to,String Candidate){
    return new Message(MessageType.ADMIN_PROPOSE, from, to, -1, Candidate, -1);
}

    /**
     * Encode the message into text, format like: <code>｜</code>
     * @return String can write in Socket
     */

public String encode(){
    return String.join("|",
            type.name(),
            from,
            to,
            Long.toString(ProposalNumber),
            Candidate == null ? "" :Candidate,
            Long.toString(AcceptID)
            )+ "\n";
}
/**
 * @param line Text separated by '｜'
 * @return decode Message
 */
public static Message decode(String line){
    String [] p = line.trim().split("\\|",-1);
    MessageType t = MessageType.valueOf(p[0]);
    String from = p[1];
    String to = p[2];
    long ProposalNumber = Long.parseLong(p[3]);
    String Candidate = p[4].isEmpty() ? null :p[4];
    long AcceptID = Long.parseLong(p[5]);
    return new Message(t,from,to,ProposalNumber,Candidate,AcceptID);
}

    /**
     * Convenient for display
     * @return String
     */
    @Override
    public String toString() {
    return encode().trim();
}
}

