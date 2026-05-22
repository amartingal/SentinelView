package sentinel_agent.utils;

import javafx.scene.control.Alert;

// Utilidades para mostrar diálogos emergentes en JavaFX
public class AlertaUtil {
    // Metodo para mostrar alerta de advertencia
    public static void mostrarAdvertencia(String titulo, String mensaje) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle(titulo);
        alert.setHeaderText(null);
        alert.setContentText(mensaje);
        alert.initOwner(sentinel_agent.view.App.getMainStage());
        alert.showAndWait();
    }

    // Metodo para mostrar alerta de error
    public static void mostrarError(String titulo, String mensaje) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(titulo);
        alert.setHeaderText(null);
        alert.setContentText(mensaje);
        alert.initOwner(sentinel_agent.view.App.getMainStage());
        alert.showAndWait();
    }

    // Metodo para mostrar alerta de información
    public static void mostrarInfo(String titulo, String mensaje) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(titulo);
        alert.setHeaderText(null);
        alert.setContentText(mensaje);
        alert.initOwner(sentinel_agent.view.App.getMainStage());
        alert.showAndWait();
    }
}
