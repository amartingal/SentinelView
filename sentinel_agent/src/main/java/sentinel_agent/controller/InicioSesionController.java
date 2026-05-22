package sentinel_agent.controller;

import java.io.IOException;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import sentinel_agent.utils.AlertaUtil;
import sentinel_agent.view.App;

public class InicioSesionController {
    @FXML
    private TextField txtUsuario;
    @FXML
    private PasswordField txtPassword;

    private int intentosFallidos = 0;

    // Metodo que maneja el inicio de sesion
    @FXML
    private void handleLogin(ActionEvent event) {
        String usuario = txtUsuario.getText() != null ? txtUsuario.getText().trim().toLowerCase() : "";
        String password = txtPassword.getText();

        if (usuario.isEmpty() || password == null || password.isEmpty()) {
            AlertaUtil.mostrarAdvertencia("Advertencia", "Por favor, ingrese su Nombre de Usuario y Contraseña.");
            return;
        }

        // Instancia la clase de acceso a datos para MongoDB
        sentinel_agent.model.UsuarioDAO dao = new sentinel_agent.model.UsuarioDAO();
        // Valida las credenciales del usuario
        boolean credencialesValidas = dao.validarLogin(usuario, password);

        if (credencialesValidas) {
            intentosFallidos = 0;
            App.setUsuarioLogueado(usuario);
            System.out.println("Inicio de sesión exitoso. Bienvenido " + usuario + ".");
            try {
                App.setRoot("Principal");
            } catch (Exception e) {
                e.printStackTrace();
                AlertaUtil.mostrarError("Error", "Error al redirigir a la pantalla principal.");
            }
        } else {
            intentosFallidos++;
            System.out.println("Fallo al iniciar sesión. Intento: " + intentosFallidos);
            if (intentosFallidos >= 3) {
                AlertaUtil.mostrarInfo("¿Problemas de acceso?",
                        "Si has olvidado tu contraseña, puedes restablecerla en el enlace debajo de '¿Has olvidado tu contraseña?'.");
            } else {
                AlertaUtil.mostrarError("Error de Autenticación",
                        "El usuario o la contraseña son incorrectos.");
            }
        }
    }

    // Metodo que maneja el olvido de la contraseña
    @FXML
    private void olvidePassword(ActionEvent event) {
        try {
            App.setRoot("RecuperarContrasena");
        } catch (IOException e) {
            e.printStackTrace();
            AlertaUtil.mostrarError("Error", "No se pudo cargar la pantalla de recuperación.");
        }
    }

    // Redirige a la pantalla de creacion de una nueva cuenta
    @FXML
    private void irRegistro(ActionEvent event) throws IOException {
        try {
            App.setRoot("Registro");
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("Error al cargar la vista registro.fxml");
        }
    }
}
