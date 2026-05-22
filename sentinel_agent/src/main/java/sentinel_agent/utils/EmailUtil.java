package sentinel_agent.utils;

import java.util.Properties;
import javax.mail.Authenticator;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

public class EmailUtil {
    // Direccion de correo electronico del remitente
    private static final String REMITENTE = "[EMAIL_ADDRESS]";
    // Contraseña de aplicacion generada por Google para el envio de correos
    private static final String PASSWORD_APP = "duxk oyxp ggvn tltn";

    public static boolean enviarPasswordRecuperacion(String destinatario, String nuevaPassword) {
        Properties props = new Properties();
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.starttls.enable", "true");
        props.put("mail.smtp.starttls.required", "true");
        props.put("mail.smtp.host", "smtp.gmail.com");
        props.put("mail.smtp.port", "587");
        props.put("mail.smtp.ssl.protocols", "TLSv1.2");
        props.put("mail.smtp.ssl.trust", "smtp.gmail.com");
        props.put("mail.debug", "true");

        // Crear sesion de correo
        Session session = Session.getInstance(props, new Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(REMITENTE, PASSWORD_APP);
            }
        });

        try {
            // Instancia un nuevo mensaje de correo asociado a la sesion
            Message message = new MimeMessage(session);
            // Configura la direccion de origen (remitente) del correo
            message.setFrom(new InternetAddress(REMITENTE));
            // Añade al destinatario principal de la lista TO
            message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(destinatario));
            // Define la linea del asunto del correo electronico
            message.setSubject("Recuperacion de Contraseña - SentinelAgent");

            // Construye el cuerpo del correo en HTML estilizado
            String cuerpo = "<h3>Hola,</h3>"
                    + "<p>Has solicitado restablecer tu contraseña en <b>SentinelAgent</b>.</p>"
                    + "<p>Tu nueva contraseña temporal es: <code style='font-size: 16px; background: #eee; padding: 5px;'>"
                    + nuevaPassword + "</code></p>"
                    + "<p>Por favor, inicia sesión y cámbiala lo antes posible desde los ajustes de tu cuenta.</p>"
                    + "<br><p>Saludos,<br>El equipo de SentinelAgent</p>";

            // Integra el cuerpo HTML especificando codificacion UTF-8
            message.setContent(cuerpo, "text/html; charset=utf-8");

            // Ejecuta el envio fisico del mensaje a traves de la red
            Transport.send(message);

            // Imprime log de exito en consola para confirmacion del desarrollador
            System.out.println("Email enviado con éxito a: " + destinatario);
            // Retorna verdadero para indicar que el correo se envio con exito
            return true;

        } catch (MessagingException e) {
            // Imprime el mensaje de error si el servidor SMTP rechaza la conexion
            System.err.println("Error al enviar el email: " + e.getMessage());
            // Imprime la pila de llamadas completa para depuracion
            e.printStackTrace();
            // Retorna falso si fallo el envio de correo
            return false;
        }
    }
}
