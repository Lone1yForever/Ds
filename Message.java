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
}
