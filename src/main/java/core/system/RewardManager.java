package core.system;

import core.handler.impl.BlockHandlerImpl;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Менеджер наград
 *
 * Устанавливает значение награды за
 * созданный блок, в зависимости от времени.
 * Уменьшает награду ежегодно на 0.2 elc.
 * Рассчитан на ~100 лет
 *
 * @author Belenov Dmitry
 */
public class RewardManager {

    /**
     * Дата старта работы монеты
     * */
    private static final String start = "09-09-2019 09:09:09";

    private static final int totalYears = 100;
    private static Logger logger = new Logger();

    public RewardManager () {
    }

    public static double get () {
        Date startDate = null;
        try {
            startDate = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss").parse(start);
        } catch (ParseException e) {
            logger.log("[ERROR] Can't parse system start date\n" + e);
        }
        Date currentDate = new Date();

        double reward = 20.0;
        for (int i=0; i<totalYears + 1; i++) {
            Calendar calendarStart = Calendar.getInstance();
            if (startDate != null) {
                calendarStart.setTime(startDate);
                if (i != 0)
                    calendarStart.add(Calendar.YEAR, i);

                if (currentDate.after(calendarStart.getTime())) {
                    if (i == totalYears)
                        break;
                    reward -= 0.2;
                }
                else break;
            }
        }
        return reward;
    }

    public static void run(){
        ScheduledExecutorService rewardHandler = Executors.newSingleThreadScheduledExecutor();
        rewardHandler.scheduleWithFixedDelay(() -> {
            double newReward = get();
            if (BlockHandlerImpl.blockReward > newReward) {
                BlockHandlerImpl.blockReward = newReward;
                logger.log("Reward for block creating set to: " + newReward + ". Date: " + new Date());
            }
        }, 0, 5, TimeUnit.SECONDS);
    }
}
