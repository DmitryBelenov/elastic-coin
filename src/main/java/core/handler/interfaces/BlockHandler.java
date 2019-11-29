package core.handler.interfaces;

import core.element.interfaces.Block;

/**
 * Обработчик блоков
 *
 * @author Belenov Dmitry
 */

public interface BlockHandler {

    /**
     * Генерирует genesis блок
     *
     * @param publicKeyBase64 - публичный ключ в Base64
     * @param privateKeyBase64 - приватный ключ в Base64
     */
    void genesisGenerate(String publicKeyBase64, String privateKeyBase64) throws Exception;

    /**
     * Валидация блока
     *
     * @param block - проверяемый блок
     *
     * @return признак валидности блока true/false
     */
    boolean blockValidation(Block block) throws Exception;

    /**
     * Запечатывает блок
     *
     * @param block - наполненный блок
     *
     * процедуры:
     * - назначение награды
     * - Proof Of Work
     * - попытка отправки в сеть
     */
    void sealTheBlock(Block block) throws Exception;

    /**
     * Proof Of Work
     *
     * @param hashForTransform - хэш блока
     *
     * @return объект PoW (хэш, nonce)
     */
    <T> T proofOfWork(String hashForTransform);
}
