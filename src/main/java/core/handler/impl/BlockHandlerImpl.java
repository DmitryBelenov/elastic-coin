package core.handler.impl;

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
import core.handler.interfaces.TransactionHandler;
import core.network.Flags;
import core.system.RewardManager;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.text.SimpleDateFormat;
import java.util.*;

import org.apache.commons.io.FileUtils;
import org.codehaus.jackson.map.ObjectMapper;

public class BlockHandlerImpl implements BlockHandler {

    public static double blockReward;

    private final String nonce = "0000000"; //optimal 7 zero
    private String coinHome;
    private ElasticCoinManager ecm;
    private String userId;

    public BlockHandlerImpl (ElasticCoinManager ecm, String userId) throws Exception {
        this.ecm = ecm;
        this.userId = userId;

        coinHome = this.ecm.getProps().getProperty("elcoin.home");
        blockReward = RewardManager.get();
        ecm.getLogger().log("Reward for block creating set to: " + blockReward);
    }

    @Override
    public void genesisGenerate (String publicKeyBase64, String privateKeyBase64) throws Exception {
        final String GENESIS_BLOCK = "genesis_block";
        Block genesis = new BlockImpl();

        TransactionHandler th = ecm.getTransactionHandler();
        Hash hash = ecm.getHash();
        Sign sign = ecm.getSign();

        String genesisJson = null;
        for (int i = 0; i < TransactionHandlerImpl.blockMaxSize; i++) {
            sign.initKeyPair();
            PrivateKey privateKey = sign.getPrivateKey();
            PublicKey publicKey = sign.getPublicKey();

            publicKeyBase64 = Base64.encode(publicKey.getEncoded());
            privateKeyBase64 = Base64.encode(privateKey.getEncoded());

            String transaction = th.createTransaction(publicKeyBase64, privateKeyBase64, null, null, 0.0);
            String ownerId = hash.getHash(publicKeyBase64.getBytes());
            genesisJson = th.appendTransactionToBlock(transaction, genesis, ownerId);
        }

        ObjectMapper mapper = new ObjectMapper();
        genesis = mapper.readValue(genesisJson, BlockImpl.class);

        String concatenatedIds = String.join("", genesis.getTransactions());
        String blockHash = hash.getHash((concatenatedIds + GENESIS_BLOCK).getBytes()).toLowerCase();

        genesis.setHash(blockHash);
        genesis.setPreviousHash(GENESIS_BLOCK);
        SimpleDateFormat formatter = new SimpleDateFormat("dd-MMM-yyyy HH:mm:ss.SSS");
        genesis.setCreateDate(formatter.format(new Date()));

        genesisJson = mapper.writeValueAsString(genesis);

        FileUtils.writeStringToFile(new File(coinHome + "\\" + blockHash + ".glc"), genesisJson, StandardCharsets.UTF_8);
    }

    @Override
    public boolean blockValidation (Block block) throws Exception {
        ObjectMapper mapper = new ObjectMapper();

        //validate proof of work
        if (proofOfWorkValidation(block)) {

            //validate integrity of transactions chain
            List<String> transports = block.getTransactions();
            if (transports.size() != TransactionHandlerImpl.blockMaxSize) {
                ecm.getLogger().log("Block size is not valid = " + transports.size());
                return false;
            }

            Hash hash = ecm.getHash();
            for (int i = transports.size() - 1; i >= 0; i--) {
                if (i == 0) {
                    String first = transports.get(i);
                    TransactionTransport firstTt = mapper.readValue(first, TransactionTransport.class);
                    if (!firstTt.getPreviousHash().equals("genesis_transaction")) {
                        ecm.getLogger().log("No genesis transaction in block");
                        return false;
                    }

                    String firstJsonBase64 = firstTt.getTransaction();
                    String firstJson = new String(Base64.decode(firstJsonBase64.getBytes()));

                    String hs = hash.getHash(firstJson.getBytes());

                    if (!firstTt.getHash().equals(hs)){
                        ecm.getLogger().log("Transaction hash: " + firstTt.getHash() + " in block: " + block.getHash() + " is NOT VALID. Expected: " + hs);
                        return false;
                    }

                    break;
                }

                String cur = transports.get(i);
                TransactionTransport curTt = mapper.readValue(cur, TransactionTransport.class);
                String pre = transports.get(i - 1);
                TransactionTransport preTt = mapper.readValue(pre, TransactionTransport.class);

                if (!curTt.getPreviousHash().equals(preTt.getHash())) {
                    ecm.getLogger().log("No chain integrity in block");
                    return false;
                }

                String curJsonBase64 = curTt.getTransaction();
                String curJson = new String(Base64.decode(curJsonBase64.getBytes()));

                String hs = hash.getHash((hash.getHash(curJson.getBytes()) + preTt.getHash()).getBytes());

                if (!curTt.getHash().equals(hs)){
                    ecm.getLogger().log("Transaction hash: " + curTt.getHash() + " in block: " + block.getHash() + " is NOT VALID. Expected: " + hs);
                    return false;
                }
            }

            //validate all transactions in block
            List<String> transactions = block.getTransactions();
            Block tmpBlock = new BlockImpl();
            tmpBlock.setTransactions(Collections.synchronizedList(new ArrayList<>()));

            for (String tr : transactions) {
                TransactionTransport tTransport = mapper.readValue(tr, TransactionTransport.class);
                byte[] transactionDecodedBytes = Base64.decode(tTransport.getTransaction());

                String transactionJson = new String(transactionDecodedBytes);
                Transaction transaction = mapper.readValue(transactionJson, TransactionImpl.class);

                String publicKeyBase64 = transaction.getPublicKeyBase64();
                byte[] publicKey = Base64.decode(publicKeyBase64);

                if (!ecm.getTransactionHandler().validateTransaction(tr, publicKey, tmpBlock)) {
                    ecm.getLogger().log("Transaction: " + tTransport.getHash() + " in block: " + block.getHash() + " is NOT VALID");
                    return false;
                }
                tmpBlock.getTransactions().add(tr);
            }

            SimpleDateFormat formatter = new SimpleDateFormat("dd-MMM-yyyy HH:mm:ss.SSS");
            block.setCreateDate(formatter.format(new Date()));
            String blockJson = mapper.writeValueAsString(block);
            FileUtils.writeStringToFile(new File(coinHome + "\\" + block.getHash() + ".elc"), blockJson, StandardCharsets.UTF_8);

            Flags.lastBlockHash = block.getHash();
            handledTransactionsProcess(block, mapper);

            return true;
        } else {
            ecm.getLogger().log("One of block transactions Proof Of Work is not valid");
        }
        return false;
    }

    private boolean proofOfWorkValidation (Block block) {
        long nnc = block.getNonce();

        Hash hash = ecm.getHash();
        String concatenated = String.join("", block.getTransactions());
        String blockHash = hash.getHash(concatenated.getBytes()).toLowerCase();
        String readyToPowHash = hash.getHash((blockHash + block.getPreviousHash()).getBytes());

        String pow = readyToPowHash + nnc;
        String powHash = hash.getHash(pow.getBytes()).toLowerCase();

        return powHash.substring(0, nonce.length()).equals(nonce);
    }

    private void handledTransactionsProcess(Block blockWithHandledTransactions, ObjectMapper mapper) {
        TransactionHandler th = ecm.getTransactionHandler();

        Flags.listen = false;
        try {
            List<TransactionTransport> toRemoveFromQueue = new ArrayList<>();
            for (String handledTransportJson : blockWithHandledTransactions.getTransactions()) {
                TransactionTransport handledTransport = mapper.readValue(handledTransportJson, TransactionTransport.class);
                String handledTransactionJson = new String(Base64.decode(handledTransport.getTransaction()));
                Transaction handledTransaction = mapper.readValue(handledTransactionJson, TransactionImpl.class);

                if (handledTransaction.getReceiverId() != null
                        && handledTransaction.getReceiverId().equals(userId)
                        && handledTransaction.getOperation() < 0
                        && !TransactionHandlerImpl.senderUidCache.contains(handledTransaction.getSenderUid())
                        && !handledTransaction.getOwnerId().equals(userId)
                        && !handledTransaction.getSenderUid().equals("Elastic Coin System")) {

                    th.sendTransactionAsResponse(handledTransaction, false);
                    ecm.getLogger().log("In block was found transaction " + Math.abs(handledTransaction.getOperation()) + "elc. for me and I sent response");
                }

                for (TransactionTransport ttQueue : TransactionHandlerImpl.transactionsQueue) {
                    String ttQueueJson = new String(Base64.decode(ttQueue.getTransaction()));
                    Transaction tQueue = mapper.readValue(ttQueueJson, TransactionImpl.class);

                    if (tQueue.equals(handledTransaction))
                        toRemoveFromQueue.add(ttQueue);
                }

                TransactionHandlerImpl.handledTransactionsCache.add(handledTransaction);
            }

            for (TransactionTransport tt : toRemoveFromQueue)
                TransactionHandlerImpl.transactionsQueue.remove(tt);

            TransactionHandlerImpl.block = new BlockImpl();

        } catch (Exception e) {
            ecm.getLogger().log("Handled transaction caching ERROR: " + e);
        }

        Flags.listen = true;
    }

    @Override
    public void sealTheBlock (Block block) throws Exception {
        TransactionHandler th = ecm.getTransactionHandler();
        ObjectMapper mapper = new ObjectMapper();
        Hash hash = ecm.getHash();

        Sign sign = ecm.getSign();
        sign.initKeyPair();

        //sign reward by generated system keys*********************************
        PrivateKey privateKey = sign.getPrivateKey();
        PublicKey publicKey = sign.getPublicKey();

        String publicKeyBase64 = Base64.encode(publicKey.getEncoded());
        String privateKeyBase64 = Base64.encode(privateKey.getEncoded());
        //*********************************************************************

        double rew = Math.abs(blockReward) * -1;
        String rewardToCreatorJson = th.createTransaction(publicKeyBase64, privateKeyBase64, userId, "Elastic Coin System", rew);

        TransactionTransport rewardTransport = mapper.readValue(rewardToCreatorJson, TransactionTransport.class);
        byte[] transactionDecodedBytes = Base64.decode(rewardTransport.getTransaction());

        String transactionJson = new String(transactionDecodedBytes);
        String transactionHash = hash.getHash(transactionJson.getBytes());

        List<String> transactions = block.getTransactions();
        String lastTransactionJson = transactions.get(transactions.size() - 1);

        TransactionTransport lastTransaction = mapper.readValue(lastTransactionJson, TransactionTransport.class);
        String lastTransactionHash = lastTransaction.getHash();

        String concatenatedHash = hash.getHash((transactionHash + lastTransactionHash).getBytes());
        rewardTransport.setHash(concatenatedHash);
        rewardTransport.setPreviousHash(lastTransactionHash);

        String rewardTransportWithHashes = mapper.writeValueAsString(rewardTransport);
        block.getTransactions().add(rewardTransportWithHashes);
        block.getTransactionsIdList().add(rewardTransport.getOwnerId());

        String concatenated = String.join("", block.getTransactions());
        String blockHash = hash.getHash(concatenated.getBytes()).toLowerCase();

        String prevHash = getLastBlockHashFromChain();
        block.setPreviousHash(prevHash);

        String readyToPoWHash = hash.getHash((blockHash + prevHash).getBytes());

        try {
            String logMsg = "Block with pre hash :" + prevHash + " already append to network by another peer";

            // check is block exists
            String blockAlreadyExists = ecm.getP2pManager().getFromNetwork(prevHash);
            if (blockAlreadyExists == null) {
                ProofOfWorkData powData = proofOfWork(readyToPoWHash);
                String powHash = powData.getPoW();
                long nonce = powData.getNonce();

                block.setHash(powHash);
                block.setNonce(nonce);

                String blockJson = mapper.writeValueAsString(block);

                // check again is block exists
                blockAlreadyExists = ecm.getP2pManager().getFromNetwork(prevHash);
                if (blockAlreadyExists == null) {
                    ecm.getP2pManager().putToNetwork(prevHash, blockJson);
                    Flags.lastBlockHash = prevHash;
                    Flags.blocksCreated++;

                    boolean transactionSent = false;
                    long increment = Flags.coinTransactionIncrement;
                    while (!transactionSent) {
                        String key = "coin_transaction" + increment;
                        if (ecm.getP2pManager().getFromNetwork(key) == null) {
                            ecm.getP2pManager().putToNetwork(key, rewardTransportWithHashes);
                            transactionSent = true;
                            ecm.getLogger().log("Coin transaction with key '" + key + "' sent to network");
                        } else {
                            increment++;
                        }
                    }
                } else
                    ecm.getLogger().log(logMsg);
            } else
                ecm.getLogger().log(logMsg);
        } catch (Exception e) {
            ecm.getLogger().log("Finish block seal process:" + e);
        }
    }

    private String getLastBlockHashFromChain () throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        SimpleDateFormat formatter = new SimpleDateFormat("dd-MMM-yyyy HH:mm:ss.SSS");

        File[] home = (new File(coinHome)).listFiles();
        if (home != null) {
            int startSize = home.length;

            if (home.length == 1) {
                Block genesis = mapper.readValue(home[0], BlockImpl.class);
                return genesis.getHash();
            }

            Map<Date, String> blocks = new HashMap<>();
            for (File block : home) {
                Block current = mapper.readValue(block, BlockImpl.class);
                Date date = formatter.parse(current.getCreateDate());
                blocks.put(date, current.getHash());
            }

            Date maxDate = blocks.keySet().stream().max(Date::compareTo).get();

            int endSize;
            home = (new File(coinHome)).listFiles();
            if (home != null) {
                endSize = home.length;
                if (startSize == endSize)
                    return blocks.get(maxDate);
                else
                    getLastBlockHashFromChain();

            } else {
                return null;
            }
        } else
            ecm.getLogger().log("Unable to read coin home, cause it's empty");
        return null;
    }

    @Override
    @SuppressWarnings ("unchecked")
    public <T> T proofOfWork (String hashForTransform) {
        Hash hash = ecm.getHash();
        String pow = hashForTransform;

        boolean proven = false;
        long increment = 0;

        String tmp = hashForTransform;

        ecm.getLogger().log("Proof Of Work in process..");
        while (!proven) {
            String work = hash.getHash(hashForTransform.getBytes()).toLowerCase();
            String check = work.substring(0, nonce.length());
            if (check.equals(nonce)) {
                proven = true;
                pow = work;
                ecm.getLogger().log("Proof Of Work: " + work);
                ecm.getLogger().log("Result: " + hashForTransform);
                ecm.getLogger().log("Iterations: " + increment);
            } else {
                hashForTransform = tmp;
                increment++;
                hashForTransform += increment;
            }
        }
        return (T) new ProofOfWorkData(pow, increment);
    }

    public static class ProofOfWorkData {
        String poW;
        long nonce;

        ProofOfWorkData (String poW, long nonce) {
            this.poW = poW;
            this.nonce = nonce;
        }

        String getPoW () {
            return poW;
        }

        long getNonce () {
            return nonce;
        }
    }
}
