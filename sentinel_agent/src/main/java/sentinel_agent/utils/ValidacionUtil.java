package sentinel_agent.utils;

import java.util.regex.Pattern;

// Clase que contiene metodos de validacion
public class ValidacionUtil {
    private static final String PASSWORD_PATTERN = "^(?=.*[a-zA-Z])(?=.*\\d)(?=.*[@#$%^&+=!._-]).{4,}$";

    // Metodo que valida el nombre de usuario
    public static boolean esUsuarioValido(String usuario) {
        if (usuario == null)
            return false;
        String userPattern = "^[a-zñáéíóúü]{3,}$";
        return Pattern.matches(userPattern, usuario);
    }

    // Metodo que valida la contraseña
    public static boolean esPasswordValida(String password) {
        if (password == null)
            return false;
        return Pattern.matches(PASSWORD_PATTERN, password);
    }

    // Metodo que valida el correo electronico
    public static boolean esEmailValido(String email) {
        if (email == null)
            return false;
        String emailPattern = "^[A-Za-z0-9+_.-]+@[A-Za-z0-9-]+(\\.[A-Za-z0-9-]+)*\\.(?i)(com|es)$";
        return Pattern.matches(emailPattern, email);
    }

    // Metodo que valida el proceso
    public static boolean esProcesoValido(String proceso) {
        if (proceso == null)
            return false;
        String procesoPattern = "^[a-zA-Z0-9_\\-\\.\\s]+\\.[eE][xX][eE]$";
        return Pattern.matches(procesoPattern, proceso);
    }

    // Metodo que genera una contraseña aleatoria
    public static String generarPasswordAleatoria() {
        String letras = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ";
        String numeros = "0123456789";
        String simbolos = "@#$%^&+=!._-";
        java.security.SecureRandom random = new java.security.SecureRandom();
        StringBuilder sb = new StringBuilder();

        sb.append(letras.charAt(random.nextInt(letras.length())));
        sb.append(numeros.charAt(random.nextInt(numeros.length())));
        sb.append(simbolos.charAt(random.nextInt(simbolos.length())));

        // Genera el resto de la contraseña
        String todosLosCaracteres = letras + numeros + simbolos;
        for (int i = 3; i < 8; i++) {
            sb.append(todosLosCaracteres.charAt(random.nextInt(todosLosCaracteres.length())));
        }

        // Baraja la contraseña
        char[] caracteres = sb.toString().toCharArray();
        for (int i = caracteres.length - 1; i > 0; i--) {
            int j = random.nextInt(i + 1);
            char temp = caracteres[i];
            caracteres[i] = caracteres[j];
            caracteres[j] = temp;
        }
        return new String(caracteres);
    }
}
