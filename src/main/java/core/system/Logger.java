package core.system;

import core.network.Flags;
import java.io.*;
import java.text.SimpleDateFormat;
import java.util.Date;

public class Logger {
    private SimpleDateFormat sf = new SimpleDateFormat("dd.MM.yyyy");
    private SimpleDateFormat dateFormat = new SimpleDateFormat("dd.MM.yyyy HH:mm:ss");
    private String filePath = System.getProperty("user.home")+"\\AppData\\Local\\ElasticCoinLog\\"+sf.format(new Date())+"\\log.txt";

    public Logger(){
        File logFile = new File(filePath);
        if (!logFile.exists()) {
            logFile.getParentFile().mkdirs();
        }
    }

    public void log (Exception e) {
        if (Flags.logging) {
            try {
                StringWriter sw = new StringWriter();
                PrintWriter pw = new PrintWriter(sw);
                e.printStackTrace(pw);

                FileWriter fw = new FileWriter(filePath, true);
                BufferedWriter bw = new BufferedWriter(fw);

                bw.write(dateFormat.format(new Date()) + " " + sw.toString() + "\n");
                bw.close();
            } catch (IOException e1) {
                System.out.println(e1);
            }
        }
    }

    public void log (String loggingText) {
        if (Flags.logging) {
            try {
                FileWriter fw = new FileWriter(filePath, true);
                BufferedWriter bw = new BufferedWriter(fw);
                bw.write(dateFormat.format(new Date()) + " " + loggingText + "\n");
                bw.close();
            } catch (IOException e1) {
                System.out.println(e1);
            }
        }
    }
}
