package sentinel_agent.controller;

import java.io.IOException;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.TextField;
import sentinel_agent.view.App;
import sentinel_agent.model.UsuarioDAO;
import sentinel_agent.utils.AlertaUtil;
import sentinel_agent.utils.ValidacionUtil;
import sentinel_agent.utils.EmailUtil;

public class RecuperarContrasenaController {
    @FXML
    private TextField txtUsuario;
    @FXML
    private TextField txtEmail;

    @FXML
    public void initialize() {
        String estiloBase = "-fx-background-color: white; -fx-padding: 10; -fx-font-size: 14px; -fx-alignment: center; -fx-text-fill: black;";

        // Validacion en tiempo real: nombre de usuario al perder el foco
        txtUsuario.focusedProperty().addListener((observable, oldValue, newValue) -> {
            if (!newValue) {
                String usuario = txtUsuario.getText() != null ? txtUsuario.getText().trim() : "";
                if (!usuario.isEmpty()) {
                    if (ValidacionUtil.esUsuarioValido(usuario)) {
                        txtUsuario.setStyle(estiloBase + "-fx-border-color: green; -fx-border-width: 2;");
                    } else {
                        txtUsuario.setStyle(estiloBase + "-fx-border-color: red; -fx-border-width: 2;");
                    }
                } else {
                    txtUsuario.setStyle(estiloBase);
                }
            }
        });

        // Validacion en tiempo real: correo electronico al perder el foco
        txtEmail.focusedProperty().addListener((observable, oldValue, newValue) -> {
            if (!newValue) {
                String email = txtEmail.getText() != null ? txtEmail.getText().trim() : "";
                if (!email.isEmpty()) {
                    if (ValidacionUtil.esEmailValido(email)) {
                        txtEmail.setStyle(estiloBase + "-fx-border-color: green; -fx-border-width: 2;");
                    } else {
                        txtEmail.setStyle(estiloBase + "-fx-border-color: red; -fx-border-width: 2;");
                    }
                } else {
                    txtEmail.setStyle(estiloBase);
                }
            }
        });
    }

    // Procesa la solicitud cuando el usuario presiona "Recuperar"
    @FXML
    private void handleRecuperar(ActionEvent event) {
        String usuario = txtUsuario.getText() != null ? txtUsuario.getText().trim() : "";
        String email = txtEmail.getText() != null ? txtEmail.getText().trim() : "";

        if (usuario.isEmpty() || email.isEmpty()) {
            AlertaUtil.mostrarAdvertencia("Campos incompletos", "Por favor, rellena todos los campos.");
            return;
        }

        if (!ValidacionUtil.esUsuarioValido(usuario)) {
            AlertaUtil.mostrarAdvertencia("Usuario inválido",
                    "El nombre de usuario debe tener mínimo 3 caracteres, estar en minúsculas, sin números y sin espacios.");
            return;
        }

        if (!ValidacionUtil.esEmailValido(email)) {
            AlertaUtil.mostrarAdvertencia("Email inválido",
                    "El correo debe tener un formato válido y terminar en .com o .es.");
            return;
        }

        UsuarioDAO dao = new UsuarioDAO();

        if (!dao.verificarUsuarioYEmail(usuario, email)) {
            AlertaUtil.mostrarError("Datos incorrectos",
                    "El nombre de usuario o el correo electrónico no coinciden con nuestros registros.");
            return;
        }

        String passwordAleatoria = ValidacionUtil.generarPasswordAleatoria();

        boolean exitoUpdate = dao.actualizarPassword(usuario, passwordAleatoria);

        if (exitoUpdate) {
            boolean emailEnviado = EmailUtil.enviarPasswordRecuperacion(email, passwordAleatoria);

            if (emailEnviado) {
                AlertaUtil.mostrarInfo("Contraseña Restablecida",
                        "Se ha enviado una nueva contraseña temporal a su correo electrónico.");
                volverLogin(null);
            } else {
                AlertaUtil.mostrarError("Error de Envío", "No se pudo enviar el correo. Contacte con soporte.");
            }
        } else {
            AlertaUtil.mostrarError("Error", "Hubo un problema al actualizar la contraseña. Inténtalo de nuevo.");
        }
    }

    // Regresa a la pantalla de inicio de sesion (Login) al presionar el enlace o
    // boton
    @FXML
    private void volverLogin(ActionEvent event) {
        try {
            App.setRoot("InicioSesion");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
