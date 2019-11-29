package core.element.interfaces;

import java.util.List;

/**
 * Блок
 *
 * @author Belenov Dmitry
 */

public interface Block {

    /**
     * Возвращает хэш блока
     */
    String getHash();

    /**
     * Инициализирует хэш блока
     */
    void setHash(String hash);

    /**
     * Возвращает хэш предыдущего блока
     */
    String getPreviousHash();

    /**
     * Инициализирует хэш предыдущего блока
     */
    void setPreviousHash(String previousHash);

    /**
     * Возвращает дату/время создания блока
     */
    String getCreateDate();

    /**
     * Инициализирует дату/время создания блока
     */
    void setCreateDate(String createDate);

    /**
     * Возвращает значение 'nonce' для Proof Of Work проверок
     */
    long getNonce();

    /**
     * Инициализирует значение 'nonce' для Proof Of Work проверок
     */
    void setNonce(long nonce);

    /**
     * Возвращает список идентификаторов транзакций в блоке
     */
    List<String> getTransactionsIdList();

    /**
     * Инициализирует список идентификаторов транзакций в блоке
     */
    void setTransactionsIdList(List<String> transactionsHashList);

    /**
     * Возвращает список транзакций в блоке
     */
    List<String> getTransactions();

    /**
     * Инициализирует список транзакций в блоке
     */
    void setTransactions(List<String> transactions);
}
