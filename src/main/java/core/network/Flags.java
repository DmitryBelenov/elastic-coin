package core.network;

import java.util.ArrayList;
import java.util.List;

/**
 * Рабочие состояния и флаги системы
 *
 * @author Belenov Dmitry
 * */

abstract public class Flags {

    /**
     * Хэш последнего сохраненного блока
     * */
    public static String lastBlockHash = null;

    /**
     * Счетчик нулевых транзакций (тр. подкачки)
     * */
    public static long zeroTransactionIncrement = 0;

    /**
     * Счетчик основных транзакций
     * */
    public static long coinTransactionIncrement = 0;

    /**
     * Коллектор ключей обработанных нулевых транзакций
     * */
    public static List<String> zeroTransactionKeys = new ArrayList<>();

    /**
     * Коллектор ключей обработанных основных транзакций
     * */
    public static List<String> coinTransactionKeys = new ArrayList<>();

    /**
     * Флаг - слушатель
     * */
    public static boolean listen = true;

    /**
     * Флаг - генератор
     * */
    public static boolean generate = true;

    /**
     * Флаг - логгер
     * */
    public static boolean logging = false;

    /**
     * Счетчик сгенерированных блоков
     * */
    public static int blocksCreated = 0;
}
