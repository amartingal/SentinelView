package sentinel_agent.controller;

import java.io.IOException;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import sentinel_agent.view.App;
import sentinel_agent.utils.AlertaUtil;
import sentinel_agent.utils.ValidacionUtil;

public class RegistroController {
    @FXML
    private TextField txtUsuario;
    @FXML
    private TextField txtEmail;
    @FXML
    private PasswordField txtPassword;
    @FXML
    private PasswordField txtRepetirPassword;
    @FXML
    private VBox panelInfo;
    @FXML
    private Button btnInfo;

    @FXML
    public void initialize() {
        panelInfo.setVisible(false);
        panelInfo.setManaged(false);

        // VALIDACIÓN EN TIEMPO REAL: Nombre de usuario
        txtUsuario.focusedProperty().addListener((observable, oldValue, newValue) -> {
            // Si el campo de texto pierde el foco de la pantalla
            if (!newValue) {
                // Obtiene la entrada de texto cruda sin pre-limpiar
                String usuario = txtUsuario.getText() != null ? txtUsuario.getText() : "";
                // Estilo CSS base para restaurar el borde o feedback
                String estiloBase = "-fx-background-color: white; -fx-padding: 10; -fx-font-size: 14px; -fx-alignment: center; -fx-text-fill: black;";
                if (!usuario.isEmpty()) {
                    if (ValidacionUtil.esUsuarioValido(usuario)) {
                        sentinel_agent.model.UsuarioDAO dao = new sentinel_agent.model.UsuarioDAO();
                        if (dao.existeUsuario(usuario)) {
                            System.out.println("Aviso: El nombre de usuario '" + usuario + "' ya está en uso.");
                            txtUsuario.setStyle(estiloBase + "-fx-border-color: red; -fx-border-width: 2;");
                        } else {
                            txtUsuario.setStyle(estiloBase + "-fx-border-color: green; -fx-border-width: 2;");
                        }
                    } else {
                        txtUsuario.setStyle(estiloBase + "-fx-border-color: red; -fx-border-width: 2;");
                    }
                } else {
                    txtUsuario.setStyle(
                            "-fx-background-color: white; -fx-padding: 10; -fx-font-size: 14px; -fx-alignment: center; -fx-text-fill: black;");
                }
            }
        });

        // VALIDACIÓN EN TIEMPO REAL: Correo electrónico
        txtEmail.focusedProperty().addListener((observable, oldValue, newValue) -> {
            if (!newValue) {
                String email = txtEmail.getText() != null ? txtEmail.getText().trim() : "";
                String estiloBase = "-fx-background-color: white; -fx-padding: 10; -fx-font-size: 14px; -fx-alignment: center; -fx-text-fill: black;";
                if (!email.isEmpty()) {
                    if (ValidacionUtil.esEmailValido(email)) {
                        sentinel_agent.model.UsuarioDAO dao = new sentinel_agent.model.UsuarioDAO();
                        if (dao.existeEmail(email)) {
                            System.out.println("Aviso: El correo '" + email + "' ya está en uso.");
                            txtEmail.setStyle(estiloBase + "-fx-border-color: red; -fx-border-width: 2;");
                        } else {
                            txtEmail.setStyle(estiloBase + "-fx-border-color: green; -fx-border-width: 2;");
                        }
                    } else {
                        txtEmail.setStyle(estiloBase + "-fx-border-color: red; -fx-border-width: 2;");
                    }
                } else {
                    txtEmail.setStyle(
                            "-fx-background-color: white; -fx-padding: 10; -fx-font-size: 14px; -fx-alignment: center; -fx-text-fill: black;");
                }
            }
        });

        // VALIDACIÓN EN TIEMPO REAL: Contraseña principal
        txtPassword.focusedProperty().addListener((observable, oldValue, newValue) -> {
            // Si el campo de contraseña pierde el foco
            if (!newValue) {
                String password = txtPassword.getText();
                String estiloBase = "-fx-background-color: white; -fx-padding: 10; -fx-font-size: 14px; -fx-alignment: center; -fx-text-fill: black;";
                if (password != null && !password.isEmpty()) {
                    if (ValidacionUtil.esPasswordValida(password)) {
                        txtPassword.setStyle(estiloBase + "-fx-border-color: green; -fx-border-width: 2;");
                    } else {
                        txtPassword.setStyle(estiloBase + "-fx-border-color: red; -fx-border-width: 2;");
                    }

                    String repetirPassword = txtRepetirPassword.getText();
                    if (repetirPassword != null && !repetirPassword.isEmpty()) {
                        if (password.equals(repetirPassword)) {
                            txtRepetirPassword.setStyle(estiloBase + "-fx-border-color: green; -fx-border-width: 2;");
                        } else {
                            txtRepetirPassword.setStyle(estiloBase + "-fx-border-color: red; -fx-border-width: 2;");
                        }
                    }
                } else {
                    txtPassword.setStyle(
                            "-fx-background-color: white; -fx-padding: 10; -fx-font-size: 14px; -fx-alignment: center; -fx-text-fill: black;");
                }
            }
        });

        // VALIDACIÓN EN TIEMPO REAL: Confirmar contraseña
        txtRepetirPassword.focusedProperty().addListener((observable, oldValue, newValue) -> {
            if (!newValue) {
                String password = txtPassword.getText();
                String repetirPassword = txtRepetirPassword.getText();
                String estiloBase = "-fx-background-color: white; -fx-padding: 10; -fx-font-size: 14px; -fx-alignment: center; -fx-text-fill: black;";
                if (repetirPassword != null && !repetirPassword.isEmpty()) {
                    if (repetirPassword.equals(password)) {
                        txtRepetirPassword.setStyle(estiloBase + "-fx-border-color: green; -fx-border-width: 2;");
                    } else {
                        txtRepetirPassword.setStyle(estiloBase + "-fx-border-color: red; -fx-border-width: 2;");
                    }
                } else {
                    txtRepetirPassword.setStyle(
                            "-fx-background-color: white; -fx-padding: 10; -fx-font-size: 14px; -fx-alignment: center; -fx-text-fill: black;");
                }
            }
        });
    }

    // Fuerza el despliegue del panel informativo de requisitos
    private void forzarMostrarInfo() {
        if (!panelInfo.isVisible()) {
            panelInfo.setVisible(true);
            panelInfo.setManaged(true);
        }
    }

    // Alterna dinamicamente la visibilidad de la guía informativa de requisitos
    @FXML
    private void mostrarOcultarInfo(ActionEvent event) {
        if (panelInfo.isVisible()) {
            panelInfo.setVisible(false);
            panelInfo.setManaged(false);
        } else {
            panelInfo.setVisible(true);
            panelInfo.setManaged(true);
        }
    }

    // Procesa e inserta la nueva cuenta de usuario al pulsar el boton Registrar
    @FXML
    private void handleRegistro(ActionEvent event) {
        String usuario = txtUsuario.getText() != null ? txtUsuario.getText() : "";
        String email = txtEmail.getText() != null ? txtEmail.getText().trim() : "";
        String password = txtPassword.getText();
        String repetirPassword = txtRepetirPassword.getText();

        if (usuario.isEmpty() || email.isEmpty() || password == null || password.isEmpty() || repetirPassword == null
                || repetirPassword.isEmpty()) {
            AlertaUtil.mostrarAdvertencia("Advertencia",
                    "Por favor, rellena todos los campos (el correo es obligatorio).");
            return;
        }

        boolean usuarioValido = ValidacionUtil.esUsuarioValido(usuario);
        boolean emailValido = ValidacionUtil.esEmailValido(email);
        boolean passwordValida = ValidacionUtil.esPasswordValida(password);

        if (!usuarioValido) {
            AlertaUtil.mostrarAdvertencia("Usuario inválido",
                    "El nombre de usuario debe tener mínimo 3 caracteres, estar en minúsculas, sin números y sin espacios.");
            forzarMostrarInfo();
            return;
        } else if (!emailValido) {
            AlertaUtil.mostrarAdvertencia("Email inválido",
                    "El correo debe tener un formato válido y terminar en .com o .es.");
            forzarMostrarInfo();
            return;
        } else if (!passwordValida) {
            AlertaUtil.mostrarAdvertencia("Contraseña débil",
                    "Debe tener mínimo 4 caracteres, en ella debe haber por lo menos una letra, un número y un símbolo.");
            forzarMostrarInfo();
            return;
        }

        if (!password.equals(repetirPassword)) {
            AlertaUtil.mostrarAdvertencia("Error", "Las contraseñas no coinciden.");
            return;
        }

        // Instancia el DAO para persistir en base de datos
        sentinel_agent.model.UsuarioDAO dao = new sentinel_agent.model.UsuarioDAO();

        if (dao.existeEmail(email)) {
            AlertaUtil.mostrarAdvertencia("Email en uso", "El correo electrónico ya está registrado por otro usuario.");
            return;
        }

        boolean exito = dao.registrarUsuario(usuario, password, email);

        if (exito) {
            AlertaUtil.mostrarInfo("Registro Exitoso", "¡Cuenta creada! Ya puedes iniciar sesión.");
            try {
                App.setRoot("InicioSesion");
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else {
            AlertaUtil.mostrarAdvertencia("Advertencia", "El nombre de usuario ya está en uso.");
        }
    }

    // Regresa a la pantalla de inicio de sesion (Login)
    @FXML
    private void volverInicioSesion(ActionEvent event) {
        try {
            App.setRoot("InicioSesion");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
