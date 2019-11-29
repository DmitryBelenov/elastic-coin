package core.crypto.interfaces;

import java.io.IOException;
import java.security.*;

/**
 * Менеджер работы с эл. подписью
 *
 * @author Belenov Dmitry
 * */

public interface Sign {

    /**
     * Инициализирует приватный и публичный ключи
     * */
    void initKeyPair() throws NoSuchAlgorithmException, NoSuchProviderException;

    /**
     * Подписывает Object
     * @param data - данные для подписи
     * @param privateKey - приватный ключ подписи
     *
     * @return подписанный класс object
     * */
    SignedObject setSignature(String data, PrivateKey privateKey) throws NoSuchAlgorithmException, InvalidKeyException, IOException, SignatureException;

    /**
     * Подписывает String
     * @param data - данные для подписи
     * @param privateKey - приватный ключ подписи
     *
     * @return подписанный набор байт
     * */
    byte[] setByteSignature(String data, PrivateKey privateKey) throws NoSuchAlgorithmException, InvalidKeyException, SignatureException;

    /**
     * Проверяет подпись
     * @param data - данные с подписью
     * @param signatureBytes - подпись
     * @param publicKey - публичный ключ подписанта
     *
     * @return признак валидности подписи true/false
     * */
    boolean verifySignature(String data, byte[] signatureBytes, PublicKey publicKey) throws NoSuchAlgorithmException, InvalidKeyException, SignatureException;

    /**
     * Проверяет подпись
     * @param signedData - данные с подписью
     * @param publicKey - публичный ключ подписанта
     *
     * @return признак валидности подписи true/false
     * */
    boolean verifySignature(SignedObject signedData, PublicKey publicKey) throws NoSuchAlgorithmException, SignatureException, InvalidKeyException;

    /**
     * Удаляет подпись
     * @param signedData - данные с подписью
     *
     * @return данные без подписи
     * */
    String getUnsignedData(SignedObject signedData) throws IOException, ClassNotFoundException;

    /**
     * @return приватный ключ подписи
     * */
    PrivateKey getPrivateKey();

    /**
     * @return публичный ключ подписи
     * */
    PublicKey getPublicKey();

    /**
     * Сохраняет ключи на диск
     * @param filePath - полный путь для сохранения
     * @param key - ключ (приватный/публичный)
     * */
    void saveKey(final String filePath, Key key) throws IOException, KeyManagementException;

    /**
     * Читает ключи с диска
     * @param filePath - полный путь к ключу
     *
     * @return объект ключа подписи (приватный/публичный)
     * */
    <K> K readKey(final String filePath) throws IOException, ClassNotFoundException, KeyManagementException;
}
