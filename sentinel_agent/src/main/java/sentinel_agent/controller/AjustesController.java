package sentinel_agent.controller;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.stage.FileChooser;
import sentinel_agent.view.App;

// Controlador de la pantalla de ajustes
public class AjustesController {
    @FXML
    private Button btnVolver;
    @FXML
    private Button btnCambiarPass;
    @FXML
    private Button btnCerrarSesion;
    @FXML
    private Button btnDescargarInstalador;
    @FXML
    private Button btnModificadorAgente;
    @FXML
    private Button btnBorrarCuenta;

    // Direccion de descarga del instalador en GitHub
    private static final String URL_INSTALADOR = "https://github.com/amartingal/Sentinel-Releases/releases/download/v1.0.0/Instalador_Sentinel.exe";

    // Boton que retorna a la pantalla principal
    @FXML
    private void volverAtras(ActionEvent event) {
        System.out.println("[DEBUG] Click en Volver Atrás");
        try {
            App.setRoot("Principal");
        } catch (Exception e) {
            System.err.println("[ERROR] No se pudo volver a Principal: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // Boton que retorna a la pantalla de cambio de contraseña
    @FXML
    private void cambiarPassword(ActionEvent event) {
        try {
            App.setRoot("CambiarPass");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // Boton que retorna a la pantalla de modificacion de agente
    @FXML
    private void modificarAgente(ActionEvent event) {
        try {
            App.setRoot("ModificadorAgente");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // Boton que cierra la sesion actual
    @FXML
    private void cerrarSesion(ActionEvent event) {
        System.out.println("[DEBUG] Click en Cerrar Sesión");
        try {
            App.setUsuarioLogueado(null);
            PrincipalController.resetFechaSeleccionada();
            App.setRoot("InicioSesion");
        } catch (Exception e) {
            System.err.println("[ERROR] Error al cerrar sesión: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // Metodo que borra la cuenta del usuario
    @FXML
    private void borrarCuenta(ActionEvent event) {
        String usuarioActual = App.getUsuarioLogueado();
        if (usuarioActual == null)
            return;

        javafx.scene.control.Dialog<String> dialog = new javafx.scene.control.Dialog<>();
        dialog.setTitle("Borrar Cuenta");
        dialog.setHeaderText("¡ATENCIÓN! Vas a borrar tu cuenta de forma permanente.");

        javafx.scene.control.ButtonType btnConfirmar = new javafx.scene.control.ButtonType("Borrar Cuenta",
                javafx.scene.control.ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(btnConfirmar, javafx.scene.control.ButtonType.CANCEL);

        javafx.scene.control.PasswordField passwordField = new javafx.scene.control.PasswordField();
        passwordField.setPromptText("Introduce tu contraseña actual");

        javafx.scene.layout.VBox vbox = new javafx.scene.layout.VBox(10);
        vbox.getChildren().addAll(
                new javafx.scene.control.Label(
                        "Para continuar, confirma tu identidad introduciendo tu contraseña actual:"),
                passwordField);
        dialog.getDialogPane().setContent(vbox);

        Platform.runLater(() -> passwordField.requestFocus());

        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == btnConfirmar) {
                return passwordField.getText();
            }
            return null;
        });

        java.util.Optional<String> result = dialog.showAndWait();
        result.ifPresent(password -> {
            sentinel_agent.model.UsuarioDAO dao = new sentinel_agent.model.UsuarioDAO();
            if (dao.validarLogin(usuarioActual, password)) {
                if (dao.borrarUsuario(usuarioActual)) {
                    sentinel_agent.utils.AlertaUtil.mostrarInfo("Cuenta Borrada",
                            "Tu cuenta y todos tus datos han sido eliminados del sistema.");
                    cerrarSesion(event);
                } else {
                    sentinel_agent.utils.AlertaUtil.mostrarError("Error",
                            "Ocurrió un problema técnico al borrar la cuenta.");
                }
            } else {
                sentinel_agent.utils.AlertaUtil.mostrarError("Error", "Contraseña incorrecta. Operación cancelada.");
            }
        });
    }

    // Gestiona la descarga del agente y empaquetado del archivo ZIP
    @FXML
    private void descargarInstalador(ActionEvent event) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Guardar instalador Sentinel");
        fileChooser.setInitialFileName("Sentinel_Instalador.zip");
        fileChooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("Archivo ZIP", "*.zip"));
        File destino = fileChooser.showSaveDialog(App.getMainStage());

        if (destino == null) {
            return;
        }

        btnDescargarInstalador.setDisable(true);
        btnDescargarInstalador.setText("Descargando...");

        String usuario = App.getUsuarioLogueado() != null ? App.getUsuarioLogueado() : "desconocido";

        // Tarea asincrona concurrente para descargar y empaquetar sin bloquear la UI
        Task<Void> tareaDescarga = new Task<Void>() {
            @Override
            protected Void call() throws Exception {
                System.out.println("[INFO] Iniciando descarga del instalador...");

                byte[] exeBytes = descargarConRedirecciones(URL_INSTALADOR);
                System.out.println("[INFO] Descarga completada: " + exeBytes.length + " bytes");

                String propContent = "id_grupo=" + usuario;
                byte[] propBytes = propContent.getBytes(StandardCharsets.UTF_8);

                try (FileOutputStream fos = new FileOutputStream(destino);
                        ZipOutputStream zos = new ZipOutputStream(fos)) {

                    ZipEntry entryExe = new ZipEntry("Instalador_Sentinel.exe");
                    zos.putNextEntry(entryExe);
                    zos.write(exeBytes);
                    zos.closeEntry();

                    ZipEntry entryProp = new ZipEntry("sentinel.properties");
                    zos.putNextEntry(entryProp);
                    zos.write(propBytes);
                    zos.closeEntry();
                }
                System.out.println("[INFO] ZIP guardado en: " + destino.getAbsolutePath());
                return null;
            }

            // Metodo que se ejecuta en caso de exito
            @Override
            protected void succeeded() {
                Platform.runLater(() -> {
                    btnDescargarInstalador.setDisable(false);
                    btnDescargarInstalador.setText("Descargar instalador\ndel agente");

                    Alert alert = new Alert(AlertType.INFORMATION, null, ButtonType.OK);
                    alert.setTitle("Descarga completada");
                    alert.setHeaderText("¡Instalador listo!");
                    alert.setContentText("El ZIP se ha guardado correctamente en:\n" + destino.getAbsolutePath());
                    alert.showAndWait();
                });
            }

            // Metodo que se ejecuta en caso de fallo inesperado de red
            @Override
            protected void failed() {
                Platform.runLater(() -> {
                    btnDescargarInstalador.setDisable(false);
                    btnDescargarInstalador.setText("Descargar instalador\ndel agente");

                    Throwable error = getException();
                    System.err.println("[ERROR] Fallo en la descarga: " + error.getMessage());
                    error.printStackTrace();

                    Alert alert = new Alert(AlertType.ERROR, null, ButtonType.OK);
                    alert.setTitle("Error en la descarga");
                    alert.setHeaderText("No se pudo completar la descarga");
                    alert.setContentText("Error: " + error.getMessage());
                    alert.showAndWait();
                });
            }
        };

        Thread hilo = new Thread(tareaDescarga);
        hilo.setDaemon(true);

        hilo.start();
    }

    // Metodo para descargar el instalador binario manejando redirecciones HTTP 302
    private byte[] descargarConRedirecciones(String urlStr) throws IOException {
        String currentUrl = urlStr;
        for (int redirecciones = 0; redirecciones < 10; redirecciones++) {
            URL url = new URL(currentUrl);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setInstanceFollowRedirects(false);
            conn.setRequestProperty("User-Agent", "SentinelAgent/1.0");
            conn.connect();

            int status = conn.getResponseCode();
            System.out.println("[INFO] HTTP " + status + " -> " + currentUrl);

            if (status == HttpURLConnection.HTTP_OK) {
                try (InputStream is = new BufferedInputStream(conn.getInputStream());
                        ByteArrayOutputStream buffer = new ByteArrayOutputStream()) {
                    byte[] chunk = new byte[8192];
                    int leido;
                    while ((leido = is.read(chunk)) != -1) {
                        buffer.write(chunk, 0, leido);
                    }
                    return buffer.toByteArray();
                }
            } else if (status == HttpURLConnection.HTTP_MOVED_TEMP
                    || status == HttpURLConnection.HTTP_MOVED_PERM
                    || status == 307 || status == 308) {
                String location = conn.getHeaderField("Location");
                conn.disconnect();
                if (location == null || location.isEmpty()) {
                    throw new IOException("Redirección sin cabecera Location.");
                }
                currentUrl = location;
            } else {
                conn.disconnect();
                throw new IOException("Respuesta HTTP inesperada: " + status + " en " + currentUrl);
            }
        }
        throw new IOException("Demasiadas redirecciones al descargar el instalador.");
    }
}
