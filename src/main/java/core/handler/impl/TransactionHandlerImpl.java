package core.handler.impl;

import com.sun.org.apache.xml.internal.security.exceptions.Base64DecodingException;
import com.sun.org.apache.xml.internal.security.utils.Base64;
import core.ElasticCoinManager;
import core.crypto.interfaces.Hash;
import core.crypto.interfaces.Sign;
import core.element.impl.BlockImpl;
import core.element.impl.TransactionImpl;
import core.element.impl.TransactionTransport;
import core.element.interfaces.Block;
import core.element.interfaces.Transaction;
import core.handler.interfaces.BlockHandler;
import core.handler.interfaces.ChainHandler;
import core.handler.interfaces.TransactionHandler;
import core.network.Flags;
import core.network.interfaces.P2pManager;
import org.codehaus.jackson.map.ObjectMapper;

import java.io.File;
import java.io.IOException;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class TransactionHandlerImpl implements TransactionHandler {

    public final static int blockMaxSize = 100;
    public static Block block;

    public static List<TransactionTransport> transactionsQueue;
    public static List<Transaction> handledTransactionsCache;
    static List<String> senderUidCache;
    private String coinHome;
    private ElasticCoinManager ecm;
    private String userId;
    private String userPublicKeyBase64;
    private String userPrivateKeyBase64;

    public TransactionHandlerImpl(ElasticCoinManager ecm, String userId, String userPublicKeyBase64, String userPrivateKeyBase64) throws Exception {
        this.ecm = ecm;
        this.userId = userId;
        this.userPublicKeyBase64 = userPublicKeyBase64;
        this.userPrivateKeyBase64 = userPrivateKeyBase64;

        // synchronized thread-safe list (need to be manually synchronize on the returned list when traversing it via Iterator, Spliterator or Stream)
        transactionsQueue = Collections.synchronizedList(new LinkedList<>());
        handledTransactionsCache = Collections.synchronizedList(new ArrayList<>());
        senderUidCache = Collections.synchronizedList(new ArrayList<>());

        coinHome = this.ecm.getProps().getProperty("elcoin.home");
    }

    public String createTransaction(String publicKeyBase64, String privateKeyBase64, String receiver, String senderUid, double operation) throws Exception {
        Hash hash = ecm.getHash();
        String hashPubKey = hash.getHash(publicKeyBase64.getBytes());

        SimpleDateFormat formatter = new SimpleDateFormat("dd-MMM-yyyy HH:mm:ss.SSS");
        Transaction transaction = new TransactionImpl(hashPubKey, publicKeyBase64, receiver, senderUid, operation, formatter.format(new Date()));

        ObjectMapper mapper = new ObjectMapper();
        String transactionJson = mapper.writeValueAsString(transaction);

        Sign sign = ecm.getSign();
        byte[] privateKeyBytes = Base64.decode(privateKeyBase64);
        PrivateKey privateKey = KeyFactory.getInstance("DSA").generatePrivate(new PKCS8EncodedKeySpec(privateKeyBytes));

        byte[] transactionSign = sign.setByteSignature(transactionJson, privateKey);

        String transactionBase64 = Base64.encode(transactionJson.getBytes());
        String signBase64 = Base64.encode(transactionSign);

        TransactionTransport transport = new TransactionTransport();
        // no hash and previousHash, on create it's empty while not appended to block
        transport.setOwnerId(hashPubKey);
        transport.setTransaction(transactionBase64);
        transport.setSign(signBase64);

        return mapper.writeValueAsString(transport);
    }

    @Override
    public String appendTransactionToBlock(String transactionTransport, Block block, String ownerId) throws Exception {
        Hash hash = ecm.getHash();
        ObjectMapper mapper = new ObjectMapper();

        int transactionsAmount = block.getTransactions().size();

        TransactionTransport tTransport = mapper.readValue(transactionTransport, TransactionTransport.class);
        byte[] transactionDecodedBytes = Base64.decode(tTransport.getTransaction());

        String transactionJson = new String(transactionDecodedBytes);
        Transaction transaction = mapper.readValue(transactionJson, TransactionImpl.class);

        String publicKeyBase64 = transaction.getPublicKeyBase64();
        byte[] publicKey = Base64.decode(publicKeyBase64);

        String transactionHash = hash.getHash(transactionJson.getBytes());

        if (transactionsAmount == 0) {
            if (validateTransaction(transactionTransport, publicKey, block)) {
                tTransport.setHash(transactionHash);
                tTransport.setPreviousHash("genesis_transaction");

                String tTransportWithHash = mapper.writeValueAsString(tTransport);
                block.getTransactions().add(tTransportWithHash);
                block.getTransactionsIdList().add(tTransport.getOwnerId());

                if (transaction.getReceiverId() != null && transaction.getReceiverId().equals(userId) && transaction.getOperation() < 0 && !transaction.getOwnerId().equals(userId)) {
                    sendTransactionAsResponse(transaction, true);
                }
            } else {
                ecm.getLogger().log("Transaction is NOT VALID");
                return null;
            }
        } else {
            if (validateTransaction(transactionTransport, publicKey, block)) {
                List<String> transactions = block.getTransactions();
                String lastTransactionJson = transactions.get(transactions.size() - 1);

                TransactionTransport lastTransaction = mapper.readValue(lastTransactionJson, TransactionTransport.class);
                String lastTransactionHash = lastTransaction.getHash();

                String concatenatedHash = hash.getHash((transactionHash + lastTransactionHash).getBytes());
                tTransport.setHash(concatenatedHash);
                tTransport.setPreviousHash(lastTransactionHash);

                String tTransportWithHashes = mapper.writeValueAsString(tTransport);
                block.getTransactions().add(tTransportWithHashes);
                block.getTransactionsIdList().add(tTransport.getOwnerId());

                if (transaction.getReceiverId() != null && transaction.getReceiverId().equals(userId) && transaction.getOperation() < 0 && !transaction.getOwnerId().equals(userId)) {
                    sendTransactionAsResponse(transaction, true);
                }
            } else {
                ecm.getLogger().log("Transaction is NOT VALID");
                return null;
            }
        }
        return mapper.writeValueAsString(block);
    }

    public void sendTransactionAsResponse(Transaction transaction, boolean cached) throws Exception {
        String senderUid = transaction.getSenderUid();

        String coinTransport = createTransaction(
                userPublicKeyBase64,
                userPrivateKeyBase64,
                null,
                senderUid,
                Math.abs(transaction.getOperation()));

        //sent to network
        P2pManager network = ecm.getP2pManager();
        boolean transactionSent = false;
        long increment = Flags.coinTransactionIncrement;
        while (!transactionSent) {
            String key = "coin_transaction" + increment;
            if (network.getFromNetwork(key) == null) {
                network.putToNetwork(key, coinTransport);
                transactionSent = true;
                ecm.getLogger().log("Coin response transaction: " + Math.abs(transaction.getOperation()) + " with key '" + key + "' sent to network by: " + userId);
            } else {
                increment++;
            }
        }

        if (cached) senderUidCache.add(senderUid);
    }

    @Override
    public boolean validateTransaction(String transaction, byte[] publicKey, Block block) throws Exception {
        File data = new File(coinHome);
        if (!data.exists())
            ecm.getLogger().log("[ERROR] Elastic Coin home path is not exists");

        String[] chain = data.list();

        if (chain != null) {
            ObjectMapper mapper = new ObjectMapper();

            ChainHandler ch = ecm.getChainHandler();
            //before validation check chain integrity
            if (!ch.checkChainIntegrity()) {
                ecm.getLogger().log("[ERROR] Chain integrity invalid");
                return false;
            }

            TransactionTransport tTransport = mapper.readValue(transaction, TransactionTransport.class);

            String transactionToValidateBase64 = tTransport.getTransaction();
            byte[] transactionToValidateJsonBytes = Base64.decode(transactionToValidateBase64.getBytes());

            String transactionToValidateJson = new String(transactionToValidateJsonBytes);

            Transaction transactionToValidate = mapper.readValue(transactionToValidateJson, TransactionImpl.class);

            if (transactionToValidate.getSenderUid() != null
                    && transactionToValidate.getSenderUid().equals("Elastic Coin System")
                    && transactionToValidate.getReceiverId() != null
                    && transactionToValidate.getOperation() < 0) {
                ecm.getLogger().log("Elastic Coin System reward operation: " + transactionToValidate.getOperation() + " is valid");
                return true;
            }

            double operationToValidate = transactionToValidate.getOperation();

            if (operationToValidate >= 0) {
                boolean signValid = signValidationProcess(tTransport.getSign(), publicKey, transactionToValidateJson);

                if (operationToValidate == 0) {
                    ecm.getLogger().log("Sign validation " + (signValid ? "ОК" : "ERROR"));
                    return signValid;
                } else {
                    if (signValid) {
                        return senderFound(chain, transactionToValidate, block);
                    } else {
                        ecm.getLogger().log("[ERROR] Invalid sign");
                    }
                }
            }

            double ownerBalance = 0.0;

            for (String f : chain) {
                if (f.endsWith(".elc")) {
                    //block hash suppose to be in lower case
                    String blockHash = f.substring(0, f.length() - 4); // escape .elc

                    Block blockValue = mapper.readValue(new File(coinHome + "\\" + f), BlockImpl.class);

                    boolean hashValid = blockHash.equals(blockValue.getHash());

                    if (hashValid) {
                        List<String> ids = blockValue.getTransactionsIdList();

                        final String ownerId = tTransport.getOwnerId();
                        if (ids.contains(ownerId)) {
                            for (String tJson : blockValue.getTransactions()) {
                                TransactionTransport tValue = mapper.readValue(tJson, TransactionTransport.class);
                                if (tValue.getOwnerId().equals(ownerId)) {
                                    String ownersTransactionBase64 = tValue.getTransaction();
                                    byte[] decodedBytes = Base64.decode(ownersTransactionBase64);

                                    String transactionJson = new String(decodedBytes);

                                    Transaction ownerTransaction = mapper.readValue(transactionJson, TransactionImpl.class);

                                    Hash hash = ecm.getHash();
                                    String hashPubKey = hash.getHash(ownerTransaction.getPublicKeyBase64().getBytes()).toUpperCase();

                                    if (hashPubKey.equals(tValue.getOwnerId()) && hashPubKey.equals(ownerTransaction.getOwnerId())) {
                                        boolean signValid = signValidationProcess(tValue.getSign(), publicKey, transactionJson);

                                        if (signValid) {
                                            double operation = ownerTransaction.getOperation();

                                            if (operation < 0 && ownerTransaction.getReceiverId() == null)
                                                ecm.getLogger().log("[ERROR] Invalid transaction detected: Receiver id not found, this transaction can't be valid");

                                            if (operation >= 0) {
                                                if (ownerTransaction.getReceiverId() != null)
                                                    ecm.getLogger().log("[ERROR] Invalid transaction detected: Receiver id is not null, this transaction can't be valid cause it addition");
                                                ownerBalance += operation;
                                            } else {
                                                ownerBalance -= Math.abs(operation);
                                            }
                                        } else
                                            ecm.getLogger().log("[ERROR] Invalid sign");
                                    } else
                                        ecm.getLogger().log("[ERROR] Invalid public key / owner id");
                                }
                            }
                        }
                    } else
                        ecm.getLogger().log("[ERROR] Block hash invalid: " + f);
                }
            }

            List<String> transports = block.getTransactions();

            if (transports.size() > 0) {
                TransactionTransport currentTransport = mapper.readValue(transaction, TransactionTransport.class);

                String currentTransactionBase64 = currentTransport.getTransaction();
                byte[] currentTransactionBytes = Base64.decode(currentTransactionBase64.getBytes());
                String currentTransactionJson = new String(currentTransactionBytes);

                Transaction currentTransaction = mapper.readValue(currentTransactionJson, TransactionImpl.class);
                String owner = currentTransaction.getOwnerId();

                for (String t : transports) {
                    TransactionTransport transportValue = mapper.readValue(t, TransactionTransport.class);
                    String transportValueBase64 = transportValue.getTransaction();
                    byte[] transportValueBytes = Base64.decode(transportValueBase64.getBytes());
                    String transportValueJson = new String(transportValueBytes);

                    Transaction transactionValue = mapper.readValue(transportValueJson, TransactionImpl.class);

                    if (transactionValue.getOwnerId().equals(owner) && transactionValue.getOperation() < 0) {
                        ownerBalance -= Math.abs(transactionValue.getOperation());
                    }
                }
            }

            boolean result = (ownerBalance - Math.abs(operationToValidate)) >= 0;
            ecm.getLogger().log("Operation: " + ownerBalance + " - " + Math.abs(operationToValidate) + " is " + result);
            return result;

        } else {
            ecm.getLogger().log("[ERROR] Unable to read coin data base from " + coinHome);
            return false;
        }
    }

    @Override
    public void blockGenerator() {
        BlockHandler bh = ecm.getBlockHandler();

        ObjectMapper mapper = new ObjectMapper();

        block = new BlockImpl();
        ScheduledExecutorService ses = Executors.newSingleThreadScheduledExecutor();
        ses.scheduleWithFixedDelay(() -> {
            CompletableFuture<Void> newTransaction = CompletableFuture.runAsync(() -> {
                if (transactionsQueue.size() > 0) {
                    if (Flags.generate) {
                        ecm.getLogger().log("Transaction handler started " + new Date());
                        if (block.getTransactions().size() == blockMaxSize - 1) {
                            Flags.listen = false;
                            try {
                                bh.sealTheBlock(block);
                            } catch (Exception e) {
                                ecm.getLogger().log("Block seal ERROR:\n" + e);
                            }

                            ecm.getLogger().log("Block sealed " + new Date());
                            block = new BlockImpl();
                            Flags.listen = true;
                        } else {
                            TransactionTransport transaction = transactionsQueue.get(0);
                            try {
                                String tTransportJson = mapper.writeValueAsString(transaction);

                                if (Flags.listen && block.getTransactions().size() < blockMaxSize - 1) {
                                    if (appendTransactionToBlock(tTransportJson, block, userId) != null) {
                                        transactionsQueue.remove(transaction);
                                        ecm.getLogger().log("Transaction " + transaction.getOwnerId() + " handled " + new Date());
                                    } else {
                                        ecm.getLogger().log("Transaction can't be append to block");
                                        transactionsQueue.remove(transaction);
                                    }
                                } else {
                                    ecm.getLogger().log("Transaction listen flag is false (or block size >= "+(blockMaxSize - 1)+")");
                                }
                            } catch (Exception e) {
                                ecm.getLogger().log("Timer task ERROR: " + e);
                            }
                        }
                        ecm.getLogger().log("Transaction handler finished " + new Date());
                    } else
                        ecm.getLogger().log("Generation temporary stopped");
                } else
                    ecm.getLogger().log("No transactions in queue..");
            });
            try {
                newTransaction.get();
            } catch (Exception e) {
                ecm.getLogger().log("Unable to get new transaction:" + e);
            }
        }, 0, 3, TimeUnit.SECONDS);
    }

    private boolean signValidationProcess(String signBase64, byte[] publicKey, String transactionJson) throws Exception {
        Sign sign = ecm.getSign();

        byte[] decodedSign = Base64.decode(signBase64);
        PublicKey pubKey = KeyFactory.getInstance("DSA").generatePublic(new X509EncodedKeySpec(publicKey));

        return sign.verifySignature(transactionJson, decodedSign, pubKey);
    }

    private boolean senderFound(String[] chain, Transaction transaction, Block work) throws IOException, Base64DecodingException {
        final String senderUid = transaction.getSenderUid();

        ObjectMapper mapper = new ObjectMapper();
        for (String file : chain) {
            if (file.endsWith(".elc")) {
                String blockHash = file.substring(0, file.length() - 4);

                Block block = mapper.readValue(new File(coinHome + "\\" + file), BlockImpl.class);

                boolean hashValid = blockHash.equals(block.getHash());

                if (hashValid) {
                    for (String transportJson : block.getTransactions()) {
                        TransactionTransport transport = mapper.readValue(transportJson, TransactionTransport.class);
                        String transactionToValidateBase64 = transport.getTransaction();

                        byte[] decodedBytes = Base64.decode(transactionToValidateBase64.getBytes());

                        String transactionToValidateJson = new String(decodedBytes);

                        Transaction transactionToValidate = mapper.readValue(transactionToValidateJson, TransactionImpl.class);

                        String uid = transactionToValidate.getSenderUid();
                        if (uid != null) {
                            if (uid.equals(senderUid)) {
                                double senderOperation = transactionToValidate.getOperation();
                                if (senderOperation < 0) {
                                    boolean result = transaction.getOperation() == Math.abs(senderOperation);
                                    ecm.getLogger().log("Sender of: " + transaction.getOperation() + "elc. found in chain: " + result);
                                    return result;
                                }
                            }
                        }
                    }
                }
            }
        }
        ecm.getLogger().log("No sender of " + transaction.getOperation() + "elc. found in chain");


        // if not found in cache try to found in queue (alien nodes)
        List<TransactionTransport> queue = TransactionHandlerImpl.transactionsQueue;
        for (TransactionTransport tt : queue) {
            byte[] transactionDecodedBytes = Base64.decode(tt.getTransaction());
            String tr = new String(transactionDecodedBytes);

            Transaction t = mapper.readValue(tr, TransactionImpl.class);

            String uid = t.getSenderUid();
            if (uid != null) {
                if (uid.equals(senderUid)) {
                    double senderOperation = t.getOperation();
                    if (senderOperation < 0) {
                        boolean result = transaction.getOperation() == Math.abs(senderOperation);
                        ecm.getLogger().log("Sender of: " + transaction.getOperation() + "elc. found  in queue: " + result);
                        return result;
                    }
                }
            }
        }
        ecm.getLogger().log("No sender of " + transaction.getOperation() + "elc. found in queue");

        // if not found in queue try to found in work block
        List<String> workBlock = work.getTransactions();
        for (String wb : workBlock) {
            TransactionTransport tt = mapper.readValue(wb, TransactionTransport.class);

            byte[] transactionDecodedBytes = Base64.decode(tt.getTransaction());
            String tr = new String(transactionDecodedBytes);

            Transaction t = mapper.readValue(tr, TransactionImpl.class);

            String uid = t.getSenderUid();
            if (uid != null) {
                if (uid.equals(senderUid)) {
                    double senderOperation = t.getOperation();
                    if (senderOperation < 0) {
                        boolean result = transaction.getOperation() == Math.abs(senderOperation);
                        ecm.getLogger().log("Sender of: " + transaction.getOperation() + "elc. found  in work block: " + result);
                        return result;
                    }
                }
            }
        }
        ecm.getLogger().log("No sender of " + transaction.getOperation() + "elc. found in work block");

        ecm.getLogger().log("No sender found for uid: " + senderUid + ", sum: " + transaction.getOperation());
        return false;
    }
}
