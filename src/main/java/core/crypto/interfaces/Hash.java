package core.crypto.interfaces;

/**
 * Менеджер вычисления хэш функции
 *
 * @author Belenov Dmitry
 * */

public interface Hash {

    /**
     * Возвращает хэш
     *
     * @param data - набор байт для хэширования
     *
     * @return хэш
     * */
    String getHash(byte[] data);
}
