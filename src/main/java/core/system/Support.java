package core.system;

import com.sun.org.apache.xml.internal.security.utils.Base64;
import core.ElasticCoinManager;
import java.util.Properties;
import javax.mail.*;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

public class Support {
    private ElasticCoinManager ecm;
    private final String from = "elcoin.sprt@gmail.com";
    private String pass;

    public Support (ElasticCoinManager ecm){
        this.ecm = ecm;
    }

    private Properties getProperties() {
        Properties props = System.getProperties();
        props.put("mail.smtp.host", "smtp.gmail.com");
        props.put("mail.smtp.socketFactory.port", "465");
        props.put("mail.smtp.socketFactory.class", "javax.net.ssl.SSLSocketFactory");
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.starttls.enable","true");
        props.put("mail.smtp.port", "465");
        props.put("mail.smtp.ssl.enable", "true");
        props.put("mail.smtp.localhost", "localhost");
//        props.put("mail.debug", "true");
        return props;
    }

    public boolean send (String to, String title, String msg) throws Exception {
        pass = new String(Base64.decode(
                "ssd==Ws11D21MDAw34d=FFgGhJhHytJuDYZnJlZTAwMDAwMA==loDFsgD34F56H+=678789s64dGHJ=-GFSgGkHGJhaDsFa235==g54@^46H^"
                .substring(index(10),index(11)-5)));
        boolean sent;
        Session session = Session.getDefaultInstance(getProperties(),
                new javax.mail.Authenticator(){
                    protected PasswordAuthentication getPasswordAuthentication() {
                        return new PasswordAuthentication(
                                from, pass);
                    }
                });
        try {
            MimeMessage message = new MimeMessage(session);
            message.setFrom(new InternetAddress(from));
            message.addRecipient(Message.RecipientType.TO, new InternetAddress(to));
            message.setSubject(title, "UTF-8");
            message.setText(msg, "UTF-8");

            Transport.send(message);
            ecm.getLogger().log("Message to tech support sent successful:\n" + msg);
            sent = true;
        } catch (Exception e) {
            ecm.getLogger().log("Message for tech support was not sent, cause: " + e);
            sent = false;
        }
        return sent;
    }

    private static int index(int i){
        int[] k = new int[i];
        k[0] = 0;
        k[1] = 1;
        for (int j=2; j<i; j++) {
            k[j] = k[j-2] + k[j-1];
        }
        return k[i-1];
    }
}