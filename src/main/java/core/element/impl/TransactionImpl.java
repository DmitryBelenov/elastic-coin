package core.element.impl;
import core.element.interfaces.Transaction;

public class TransactionImpl implements Transaction {

    private String ownerId; // hash of public key
    private String publicKeyBase64;
    private String receiverId;
    private String senderUid;
    private double operation;
    private String date;

    public TransactionImpl(){}

    public TransactionImpl (String ownerId,
                            String publicKeyBase64,
                            String receiverId,
                            String senderUid,
                            double operation,
                            String date) {
        this.ownerId = ownerId;
        this.publicKeyBase64 = publicKeyBase64;
        this.receiverId = receiverId;
        this.senderUid = senderUid;
        this.operation = operation;
        this.date = date;
    }

    public String getSenderUid() {
        return senderUid;
    }

    public void setSenderUid(String senderUid) {
        this.senderUid = senderUid;
    }

    public String getOwnerId() {
        return ownerId;
    }

    public String getPublicKeyBase64() {
        return publicKeyBase64;
    }

    public String getReceiverId() {
        return receiverId;
    }

    public void setReceiverId(String receiver) {
        this.receiverId = receiver;
    }

    public double getOperation () {
        return operation;
    }

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }
}
