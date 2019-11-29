package core.element.impl;

import core.element.interfaces.Block;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class BlockImpl implements Block {

    private String hash;
    private String previousHash;
    private String createDate;
    private long nonce;

    private List<String> transactionsIdList;

    private List<String> transactions;

    public BlockImpl() {
        transactionsIdList = new ArrayList<>();
        transactions = Collections.synchronizedList(new ArrayList<>());
    }

    public String getPreviousHash() {
        return previousHash;
    }

    public void setPreviousHash(String previousHash) {
        this.previousHash = previousHash;
    }

    public String getHash() {
        return hash;
    }

    public void setHash(String hash) {
        this.hash = hash;
    }

    public String getCreateDate() {
        return createDate;
    }

    public void setCreateDate(String createDate) {
        this.createDate = createDate;
    }

    public long getNonce () {
        return nonce;
    }

    public void setNonce (long nonce) {
        this.nonce = nonce;
    }

    public List<String> getTransactionsIdList() {
        return transactionsIdList;
    }

    public void setTransactionsIdList(List<String> transactionsHashList) {
        this.transactionsIdList = transactionsHashList;
    }

    public List<String> getTransactions() {
        return transactions;
    }

    public void setTransactions (List<String> transactions) {
        this.transactions = transactions;
    }
}
