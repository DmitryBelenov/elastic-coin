package core.network.impl;

import com.sun.org.apache.xml.internal.security.utils.Base64;
import core.ElasticCoinManager;
import core.element.impl.BlockImpl;
import core.element.impl.TransactionImpl;
import core.element.impl.TransactionTransport;
import core.element.interfaces.Block;
import core.element.interfaces.Transaction;
import core.handler.impl.TransactionHandlerImpl;
import core.network.Flags;
import core.network.interfaces.P2pManager;
import core.system.Logger;
import core.system.PropertiesManager;
import net.tomp2p.dht.*;
import net.tomp2p.futures.FutureBootstrap;
import net.tomp2p.p2p.PeerBuilder;
import net.tomp2p.peers.Number160;
import net.tomp2p.storage.Data;
import org.codehaus.jackson.map.ObjectMapper;

import java.net.InetAddress;
import java.util.Random;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class P2pManagerImpl implements P2pManager {

    private PeerDHT peerDHT;
    private ElasticCoinManager ecm;

    private Logger logger;
    private PropertiesManager props;

    public P2pManagerImpl (ElasticCoinManager ecm) throws Exception {
        this.ecm = ecm;
        logger = ecm.getLogger();
        props = ecm.getProps();

        peerDHT = getPeerDHT();
        keepAliveBootstrapConnection();
    }

    @Override
    public void setPeer (PeerDHT peerDHT) {
        this.peerDHT = peerDHT;
    }

    @Override
    public void putToNetwork (String key, String value) throws Exception {
        FuturePut futurePut = peerDHT.put(Number160.createHash(key)).data(new Data(value)).start().awaitUninterruptibly();
        if (futurePut.isSuccess())
            ecm.getLogger().log(key + " key send to network");
    }

    @Override
    public String getFromNetwork (String key) {
        try {
            FutureGet futureGet = peerDHT.get(Number160.createHash(key)).start();
            futureGet.awaitUninterruptibly();
            if (futureGet.isSuccess()) {
                return futureGet.dataMap().values().iterator().next().object().toString();
            }
        } catch (Exception e) {
            ecm.getLogger().log("No value by key '" + key + "' in network");
        }
        return null;
    }

    @Override
    public void removeFromNetwork (String key) {
        FutureRemove futureRemove = peerDHT.remove(Number160.createHash(key)).start();
        futureRemove.awaitUninterruptibly();
        if (futureRemove.isRemoved())
            ecm.getLogger().log(key + " removed from network");
    }

    @Override
    public void startBlockListener () {
        ObjectMapper mapper = new ObjectMapper();

        ScheduledExecutorService blockListener = Executors.newSingleThreadScheduledExecutor();
        blockListener.scheduleWithFixedDelay(() -> {
            if (Flags.listen) {
                CompletableFuture<Void> blockJson = CompletableFuture.runAsync(() ->
                {
                    String newBlockKey = Flags.lastBlockHash;
                    if (newBlockKey != null) {
                        try {
                            String newBlockJson = getFromNetwork(newBlockKey);

                            Flags.generate = false;

                            Block newBlock = mapper.readValue(newBlockJson, BlockImpl.class);

                            String blockHash = newBlock.getHash();
                            ecm.getLogger().log("Got new block " + blockHash);

                            if (ecm.getBlockHandler().blockValidation(newBlock)) {
                                ecm.getLogger().log("Block " + blockHash + " valid");
                            } else {
                                ecm.getLogger().log("[WARNING] Block " + blockHash + " NOT valid");
                                if (getFromNetwork(newBlockKey) != null) removeFromNetwork(newBlockKey);
                            }
                        } catch (Exception e) {
                            ecm.getLogger().log("No new blocks in system");
                        }
                        Flags.generate = true;
                    }
                });
                try {
                    blockJson.get();
                } catch (Exception e) {
                    ecm.getLogger().log("Unable to get new block:" + e);
                }
            }
        }, 0, 3, TimeUnit.SECONDS);
    }

    @Override
    public void startZeroTransactionListener () {
        ObjectMapper mapper = new ObjectMapper();

        ScheduledExecutorService zeroTransactionListener = Executors.newSingleThreadScheduledExecutor();
        zeroTransactionListener.scheduleWithFixedDelay(() -> {
            if (Flags.listen) {
                CompletableFuture<Void> transactionJson = CompletableFuture.runAsync(() ->
                {
                    try {
                        String key = "zero_transaction" + Flags.zeroTransactionIncrement;
                        String newTransaction = getFromNetwork(key);
                        if (newTransaction != null) {
                            Flags.zeroTransactionKeys.add(key);

                            TransactionTransport newTransport = mapper.readValue(newTransaction, TransactionTransport.class);

                            byte[] transactionDecodedBytes = Base64.decode(newTransport.getTransaction());
                            String tr = new String(transactionDecodedBytes);

                            Transaction transaction = mapper.readValue(tr, TransactionImpl.class);

                            if (!TransactionHandlerImpl.transactionsQueue.contains(newTransport)) {
                                if (!TransactionHandlerImpl.handledTransactionsCache.contains(transaction)) {

                                    TransactionHandlerImpl.transactionsQueue.add(newTransport);

                                    ecm.getLogger().log("New zero transaction got form network by key '" + key + "'");
                                } else {
                                    TransactionHandlerImpl.handledTransactionsCache.remove(transaction);
                                }
                            }
                            Flags.zeroTransactionIncrement++;
                        }
                    } catch (Exception e) {
                        ecm.getLogger().log("Read new zero transaction from network ERROR:" + e);
                    }
                });
                try {
                    transactionJson.get();
                } catch (Exception e) {
                    ecm.getLogger().log("Unable to get new transaction:" + e);
                }
            }
        }, 0, 3, TimeUnit.SECONDS);
    }

    @Override
    public void startCoinTransactionListener () {
        ObjectMapper mapper = new ObjectMapper();

        ScheduledExecutorService coinTransactionListener = Executors.newSingleThreadScheduledExecutor();
        coinTransactionListener.scheduleWithFixedDelay(() -> {
            if (Flags.listen) {
                CompletableFuture<Void> transactionJson = CompletableFuture.runAsync(() ->
                {
                    try {
                        String key = "coin_transaction" + Flags.coinTransactionIncrement;
                        String newTransaction = getFromNetwork(key);
                        if (newTransaction != null) {
                            Flags.coinTransactionKeys.add(key);

                            TransactionTransport newTransport = mapper.readValue(newTransaction, TransactionTransport.class);

                            byte[] transactionDecodedBytes = Base64.decode(newTransport.getTransaction());
                            String tr = new String(transactionDecodedBytes);

                            Transaction transaction = mapper.readValue(tr, TransactionImpl.class);

                            if (!TransactionHandlerImpl.transactionsQueue.contains(newTransport)) {
                                if (!TransactionHandlerImpl.handledTransactionsCache.contains(transaction)) {

                                    TransactionHandlerImpl.transactionsQueue.add(newTransport);

                                    ecm.getLogger().log("New coin transaction got form network by key '" + key + "'");

                                } else {
                                    TransactionHandlerImpl.handledTransactionsCache.remove(transaction);
                                }
                            }

                            Flags.coinTransactionIncrement++;
                        }
                    } catch (Exception e) {
                        ecm.getLogger().log("Read new coin transaction from network ERROR:" + e);
                    }
                });
                try {
                    transactionJson.get();
                } catch (Exception e) {
                    ecm.getLogger().log("Unable to get new reward transaction:" + e);
                }
            }
        }, 0, 3, TimeUnit.SECONDS);
    }

    private void keepAliveBootstrapConnection () {
        final ScheduledExecutorService ses = Executors.newSingleThreadScheduledExecutor();
        ses.scheduleWithFixedDelay(() -> {
            String bootstrapPeer = getFromNetwork("bootstrapPeer");
            if (bootstrapPeer == null) {
                Flags.generate = false;
                try {
                    PeerDHT newPeer = getPeerDHT();
                    setPeer(newPeer);
                    logger.log("Bootstrap peer RECONNECT attempted");
                } catch (Exception e) {
                    logger.log("Bootstrap peer NOT CONNECT: " + e);
                }
                Flags.generate = true;
            } else
                logger.log("Bootstrap peer status: " + bootstrapPeer);
        }, 0, 5, TimeUnit.SECONDS);
    }

    private PeerDHT getPeerDHT () throws Exception {
        String bootstrapIp = props.getProperty("bootstrap.peer.ip");
        int workPort = Integer.parseInt(props.getProperty("work.port"));

        Random random = new Random();
        int rndIdOfPeer = random.nextInt(100000000);

        PeerDHT peerDHT = new PeerBuilderDHT(new PeerBuilder(Number160.createHash(rndIdOfPeer)).ports(workPort).start()).start();
        logger.log("Peer created");

        FutureBootstrap fb = peerDHT.peer().bootstrap().inetAddress(InetAddress.getByName(bootstrapIp)).ports(workPort).start();
        fb.awaitUninterruptibly();

        if (fb.isSuccess()) {
            logger.log("Bootstrap partner loaded");
            peerDHT.peer().discover().peerAddress(fb.bootstrapTo().iterator().next()).start().awaitUninterruptibly();
            String myIP = peerDHT.peerAddress().inetAddress().getHostAddress();
            logger.log("My outside IP address: " + myIP);

            if (myIP.equals(bootstrapIp)) {
                String key = "bootstrapPeer";
                String value = "alive";

                peerDHT.put(Number160.createHash(key)).data(new Data(value)).start().awaitUninterruptibly();
                logger.log(key + " key send to network");
            }
        }
        return peerDHT;
    }
}
