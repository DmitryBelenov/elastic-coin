package core.handler.interfaces;

import core.element.interfaces.Block;
import core.element.interfaces.Transaction;

/**
 * Обработчик транзакций
 *
 * @author Belenov Dmitry
 * */

public interface TransactionHandler {

    /**
     * Создание транзакции
     *
     * @param publicKeyBase64 - публичный ключ в Base64
     * @param privateKeyBase64 - приватный ключ в Base64
     * @param receiver - идентификатор нода получателя
     * @param senderUid - UUID получателя
     * @param operation - сумма операции
     *
     * @return json транзакции
     * */
    String createTransaction(String publicKeyBase64, String privateKeyBase64, String receiver, String senderUid, double operation) throws Exception;

    /**
     * Добавляет транзакцию в блок
     *
     * @param transaction - json транзакции
     * @param block - объект блока
     * @param ownerId - идентификатор текущего нода
     *
     * @return json дополненного блока
     * */
    String appendTransactionToBlock(String transaction, Block block, String ownerId) throws Exception;

    /**
     * Валидация транзакции
     *
     * @param transactionTransport - json транспортного объекта транзакции
     * @param publicKey - набор байт публичного ключа
     * @param block - объект блока
     *
     * @return признак валидности транзакции true/false
     * */
    boolean validateTransaction(String transactionTransport, byte[] publicKey, Block block) throws Exception;

    /**
     * Генератор блоков
     *
     * Опрашивает локальную очередь на наличие новых транзакций
     * и обрабатывает их локально добавляя в текущий блок
     * */
    void blockGenerator();

    /**
     * Отправляет ответную транзакцию
     *
     * При поступлении монет от стороннего нода,
     * отправляет в сеть ответную транзакцию для пополнения собственного баланса
     * (объязательное соглашение системы)
     *
     * Использует UUID отправителя
     *
     * @param transaction - объект транзакции
     * @param cached - признак кэширования UUID отправителя true/false
     * */
    void sendTransactionAsResponse(Transaction transaction, boolean cached) throws Exception;
}
