package core.system;

import core.ElasticCoinManager;
import core.handler.impl.TransactionHandlerImpl;
import core.network.Flags;
import core.network.interfaces.P2pManager;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Очистка сети от обработанных транзакций
 *
 * Накапливает в локальный пул ключи транзакций
 * и по достижении max значения очищает сеть от них,
 * если это уже небыло сделано сторонним нодом
 *
 * @author Belenov Dmitry
 */

public class TransactionPullCleaner {

    private ElasticCoinManager ecm;
    private int maxPullSize;

    public TransactionPullCleaner(ElasticCoinManager ecm) {
        this.ecm = ecm;
        maxPullSize = TransactionHandlerImpl.blockMaxSize * 10;
    }

    public void start() {
        ScheduledExecutorService cleaner = Executors.newSingleThreadScheduledExecutor();
        cleaner.scheduleWithFixedDelay(() -> {
            CompletableFuture<Void> checkNClean = CompletableFuture.runAsync(() -> {

                if (Flags.zeroTransactionKeys.size() > maxPullSize) {
                    Flags.listen = false;

                    P2pManager p2pManager = ecm.getP2pManager();

                    List<String> zeroTransactionKeys = Flags.zeroTransactionKeys;
                    for (int i = 0; i < Flags.zeroTransactionKeys.size(); i++) {
                        String key = zeroTransactionKeys.get(i);
                        if (p2pManager.getFromNetwork(key) != null) {
                            p2pManager.removeFromNetwork(key);
                        }
                    }

                    Flags.zeroTransactionKeys = new ArrayList<>();

                    Flags.listen = true;
                }

                if (Flags.coinTransactionKeys.size() > maxPullSize) {
                    Flags.listen = false;

                    P2pManager p2pManager = ecm.getP2pManager();

                    List<String> coinTransactionKeys = Flags.coinTransactionKeys;
                    for (int i = 0; i < Flags.coinTransactionKeys.size(); i++) {
                        String key = coinTransactionKeys.get(i);
                        if (p2pManager.getFromNetwork(key) != null) {
                            p2pManager.removeFromNetwork(key);
                        }
                    }

                    Flags.coinTransactionKeys = new ArrayList<>();

                    Flags.listen = true;
                }
            });

            try {
                checkNClean.get();
            } catch (Exception e) {
                ecm.getLogger().log("Unable to clean transactions pull:" + e);
            }
        }, 0, 1, TimeUnit.HOURS);
    }
}
