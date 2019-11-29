package core.handler.interfaces;

/**
 * Обработчик локального 'чейна'
 *
 * @author Belenov Dmitry
 * */

public interface ChainHandler {

    /**
     * Проверяет локальный 'чейн' на целостность
     *
     * @return признак целостности локальной цепи блоков true/false
     */
    boolean checkChainIntegrity() throws Exception;
}
