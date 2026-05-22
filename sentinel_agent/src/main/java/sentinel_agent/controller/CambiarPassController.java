package sentinel_agent.controller;

import java.io.IOException;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.PasswordField;
import sentinel_agent.view.App;
import sentinel_agent.model.UsuarioDAO;
import sentinel_agent.utils.AlertaUtil;
import sentinel_agent.utils.ValidacionUtil;

public class CambiarPassController {
    @FXML
    private PasswordField txtPass;
    @FXML
    private PasswordField txtRepetirPass;

    @FXML
    public void initialize() {
        String estiloBase = "-fx-background-color: white; -fx-background-radius: 10; -fx-border-color: black; -fx-border-radius: 10; -fx-font-size: 15px;";

        // Valida en tiempo real la nueva contraseña
        txtPass.focusedProperty().addListener((observable, oldValue, newValue) -> {
            if (!newValue) {
                String pass = txtPass.getText();
                if (pass != null && !pass.isEmpty()) {
                    if (ValidacionUtil.esPasswordValida(pass)) {
                        txtPass.setStyle(estiloBase + "-fx-border-color: green;");
                    } else {
                        txtPass.setStyle(estiloBase + "-fx-border-color: red;");
                    }
                    String repetirPass = txtRepetirPass.getText();
                    if (repetirPass != null && !repetirPass.isEmpty()) {
                        if (pass.equals(repetirPass)) {
                            txtRepetirPass.setStyle(estiloBase + "-fx-border-color: green;");
                        } else {
                            txtRepetirPass.setStyle(estiloBase + "-fx-border-color: red;");
                        }
                    }
                } else {
                    txtPass.setStyle(estiloBase);
                }
            }
        });

        // Valida en tiempo real la confirmacion de la contraseña
        txtRepetirPass.focusedProperty().addListener((observable, oldValue, newValue) -> {
            if (!newValue) {
                String pass = txtPass.getText();
                String repetirPass = txtRepetirPass.getText();
                if (repetirPass != null && !repetirPass.isEmpty()) {
                    if (repetirPass.equals(pass)) {
                        txtRepetirPass.setStyle(estiloBase + "-fx-border-color: green;");
                    } else {
                        txtRepetirPass.setStyle(estiloBase + "-fx-border-color: red;");
                    }
                } else {
                    txtRepetirPass.setStyle(estiloBase);
                }
            }
        });
    }

    // Accion para cancelar la operacion y retornar a la vista anterior
    @FXML
    private void volverAtras(ActionEvent event) {
        try {
            App.setRoot("Ajustes");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // Procesa y guarda la nueva contraseña del usuario logueado en la sesion
    @FXML
    private void procesarCambio(ActionEvent event) {
        String pass = txtPass.getText();
        String repetirPass = txtRepetirPass.getText();
        String usuario = App.getUsuarioLogueado();

        if (pass == null || pass.isEmpty() || repetirPass == null || repetirPass.isEmpty()) {
            AlertaUtil.mostrarAdvertencia("Campos incompletos", "Por favor, rellena ambos campos.");
            return;
        }

        if (!pass.equals(repetirPass)) {
            AlertaUtil.mostrarError("Error de coincidencia", "Las contraseñas ingresadas no coinciden.");
            return;
        }

        if (!ValidacionUtil.esPasswordValida(pass)) {
            AlertaUtil.mostrarAdvertencia("Contraseña débil",
                    "La nueva contraseña debe tener mín. 4 caracteres, una letra, un número y un símbolo.");
            return;
        }
        UsuarioDAO dao = new UsuarioDAO();
        boolean exito = dao.actualizarPassword(usuario, pass);
        if (exito) {
            AlertaUtil.mostrarInfo("Éxito",
                    "Tu contraseña ha sido actualizada correctamente. Por seguridad, debes iniciar sesión de nuevo.");
            try {
                App.setUsuarioLogueado(null);
                App.setRoot("InicioSesion");
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else {
            AlertaUtil.mostrarError("Error", "No se pudo actualizar la contraseña. Revisa tu conexión.");
        }
    }
}
