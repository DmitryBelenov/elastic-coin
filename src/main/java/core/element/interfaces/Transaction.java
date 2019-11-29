package core.element.interfaces;

/**
 * Транзакция
 *
 * @author Belenov Dmitry
 */

public interface Transaction {

    /**
     * Возвращает UUID отправителя
     * */
    String getSenderUid();

    /**
     * Инициализирует UUID отправителя
     * */
    void setSenderUid(String senderUid);

    /**
     * Возвращает идентификатор нода (хэш публичного ключа)
     * */
    String getOwnerId();

    /**
     * Возвращает публичный ключ в Base64
     * */
    String getPublicKeyBase64();

    /**
     * Возвращает идентификатор нода получателя (хэш публичного ключа)
     * */
    String getReceiverId();

    /**
     * Инициализирует идентификатор нода (хэш публичного ключа)
     * */
    void setReceiverId(String receiver);

    /**
     * Возвращает сумму операции
     * */
    double getOperation();

    /**
     * Возвращает дату создания транзакции
     * */
    String getDate();

    /**
     * Инициализирует дату создания транзакции
     * */
    void setDate(String date);
}
