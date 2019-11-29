package core;

import core.crypto.impl.HashImpl;
import core.crypto.impl.SignImpl;
import core.crypto.interfaces.Hash;
import core.crypto.interfaces.Sign;
import core.handler.impl.BlockHandlerImpl;
import core.handler.impl.ChainHandlerImpl;
import core.handler.impl.TransactionHandlerImpl;
import core.handler.interfaces.BlockHandler;
import core.handler.interfaces.ChainHandler;
import core.handler.interfaces.TransactionHandler;
import core.network.Flags;
import core.network.impl.P2pManagerImpl;
import core.network.interfaces.P2pManager;
import core.system.*;

public class ElasticCoinManagerImpl implements ElasticCoinManager {

    private P2pManager p2pManager;
    private TransactionHandler transactionHandler;
    private BlockHandler blockHandler;
    private ChainHandler chainHandler;
    private Hash hash;
    private Sign sign;
    private PropertiesManager props;
    private Logger logger;
    private Support support;

    public void init (String userId, String publicKeyBase64, String privateKeyBase64, PropertiesManager props) throws Exception {
        logger = new Logger();
        this.props = props;
        Flags.logging = Boolean.parseBoolean(this.props.getProperty("logging"));

        hash = new HashImpl();
        sign = new SignImpl();

        p2pManager = new P2pManagerImpl(this);

        transactionHandler = new TransactionHandlerImpl(this, userId, publicKeyBase64, privateKeyBase64);
        blockHandler = new BlockHandlerImpl(this, userId);
        chainHandler = new ChainHandlerImpl(this);

        Flags.lastBlockHash = this.props.getProperty("last.block.hash");
        Flags.zeroTransactionIncrement = Integer.parseInt(this.props.getProperty("zero.transaction.counter"));
        Flags.coinTransactionIncrement = Integer.parseInt(this.props.getProperty("coin.transaction.counter"));

        support = new Support(this);
        RewardManager.run();

        TransactionPullCleaner pullCleaner = new TransactionPullCleaner(this);
        pullCleaner.start();
    }

    public Logger getLogger () {
        return logger;
    }

    public PropertiesManager getProps () {
        return props;
    }

    public P2pManager getP2pManager () {
        return p2pManager;
    }

    public BlockHandler getBlockHandler () {
        return blockHandler;
    }

    public ChainHandler getChainHandler () {
        return chainHandler;
    }

    public Hash getHash () {
        return hash;
    }

    public Sign getSign () {
        return sign;
    }

    @Override
    public TransactionHandler getTransactionHandler () {
        return transactionHandler;
    }

    public Support getSupport () {
        return support;
    }
}
