package core.network.interfaces;

import net.tomp2p.dht.PeerDHT;

/**
 * Менеджер работы с TomP2P сетью
 *
 * @author Belenov Dmitry
 * */

public interface P2pManager {

    /**
     * Идентифицирует текущий нод (узел)
     * в общей сети
     *
     * @param peerDHT - объект нода
     * */
    void setPeer(PeerDHT peerDHT);

    /**
     * Отправляет в сеть запись key/value
     *
     * @param key - ключ
     * @param value - значение
     * */
    void putToNetwork(String key, String value) throws Exception;

    /**
     * Получает из сети значение по ключу
     *
     * @param key - ключ
     *
     * @return значение по ключу
     * */
    String getFromNetwork(String key);

    /**
     * Удаляет из сети значение по ключу
     *
     * @param key - ключ
     * */
    void removeFromNetwork(String key);

    /**
     * Инициализирует слушатель и обработчик новых блоков
     * */
    void startBlockListener();

    /**
     * Инициализирует слушатель и обработчик новых
     * нулевых транзакций (тр. подкачки)
     *
     * Добавляет в локальную очередь
     * */
    void startZeroTransactionListener();

    /**
     * Инициализирует слушатель и обработчик новых транзакций
     *
     * Добавляет в локальную очередь
     * */
    void startCoinTransactionListener();
}
