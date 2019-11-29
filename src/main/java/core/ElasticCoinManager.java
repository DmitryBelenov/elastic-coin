package core;

import core.crypto.interfaces.Hash;
import core.crypto.interfaces.Sign;
import core.handler.interfaces.BlockHandler;
import core.handler.interfaces.ChainHandler;
import core.handler.interfaces.TransactionHandler;
import core.network.interfaces.P2pManager;
import core.system.Logger;
import core.system.PropertiesManager;
import core.system.Support;

/**
 * Основной API-менеджер для работы с монетой elastic coin.
 * Инициализируется созданием экземпляра имплементации
 * и вызовом метода init()
 *
 * @author Belenov Dmitry
 * */

public interface ElasticCoinManager {

    /**
     * Инициализирует рабочие объекты, входные параметры,
     * запускает таймеры и подключает текущий нод к P2P сети
     * */
    void init (String userId, String publicKeyBase64, String privateKeyBase64, PropertiesManager props) throws Exception;

    /**
     * Логгер системы
     * пишет лог в AppData\Local\ElasticCoinLog
     * при logging=true в параметрах запуска
     * */
    Logger getLogger ();

    /**
     * Менеджер конфигурации запуска
     * */
    PropertiesManager getProps ();

    /**
     * Менеджер работы с TomP2P.
     * Подключает нод к общей сети.
     *
     * Запускает таймеры мониторинга:
     * - блоков
     * - нулевых транзакций (тр.подкачки)
     * - не нулевых транзакций (основных)
     * - health check (защита от прерываний соединения)
     * */
    P2pManager getP2pManager ();

    /**
     * Обработчик блоков
     * */
    BlockHandler getBlockHandler ();

    /**
     * Обработчик локального 'чейна'
     * */
    ChainHandler getChainHandler ();

    /**
     * Менеджер вычисления хэш функции
     * */
    Hash getHash ();

    /**
     * Менеджер работы с эл. подписью
     * */
    Sign getSign ();

    /**
     * Обработчик транзакций
     * */
    TransactionHandler getTransactionHandler ();

    /**
     * Менеджер отправки сообщений в тех. поддержку
     * */
    Support getSupport();
}
