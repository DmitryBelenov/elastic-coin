package core.element.impl;

/**
 * Транспортный объект транзакции
 *
 * @author Belenov Dmitry
 * */

public class TransactionTransport {

    /**
     * Хэш транзакции
     * */
    private String hash;

    /**
     * Хэш предыдущей транзакции
     * */
    private String previousHash;

    /**
     * Идентификатор текущего нода (хэш публичного ключа)
     * */
    private String ownerId;

    /**
     * Json транзакции
     * */
    private String transaction;

    /**
     * Эл. подпись
     * */
    private String sign;

    public String getHash() {
        return hash;
    }

    public void setHash(String hash) {
        this.hash = hash;
    }
    
    public String getPreviousHash() {
        return previousHash;
    }

    public void setPreviousHash(String previousHash) {
        this.previousHash = previousHash;
    }

    public String getOwnerId() {
        return ownerId;
    }

    public void setOwnerId(String ownerId) {
        this.ownerId = ownerId;
    }

    public String getTransaction() {
        return transaction;
    }

    public void setTransaction(String transaction) {
        this.transaction = transaction;
    }

    public String getSign() {
        return sign;
    }

    public void setSign(String sign) {
        this.sign = sign;
    }
}
