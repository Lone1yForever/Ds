public final class Message {
    public final MessageType type;
    public final String from;
    public final String to;
    public final long ProposalNumber;
    public final String Candidate;
    public final long AcceptID;

public Message (MessageType type,String from,String to,long ProposalNumber,String Candidate,long AcceptID){
    this.type = type;
    this.from = from;
    this.to = to ;
    this.ProposalNumber = ProposalNumber;
    this.Candidate = Candidate;
    this.AcceptID = AcceptID;
}
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
public Message decode(String line){
    String [] p = line.trim().split("\\|",-1);
    MessageType t = MessageType.valueOf(p[0]);
    String from = p[1];
    String to = p[2];
    long ProposalNumber = Long.parseLong(p[3]);
    String Candidate = p[4].isEmpty() ? null :p[4];
    long AcceptID = Long.parseLong(p[5]);
    return new Message(t,from,to,ProposalNumber,Candidate,AcceptID);
}
    @Override
    public String toString() {
    return encode().trim();
}
}

