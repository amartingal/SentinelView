package sentinel_agent.controller;

import java.net.URL;
import java.util.ResourceBundle;
import javafx.animation.Interpolator;
import javafx.animation.RotateTransition;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.shape.Arc;
import javafx.util.Duration;
import sentinel_agent.model.ConexionMongo;
import sentinel_agent.view.App;

public class SplashScreenController implements Initializable {
    @FXML
    private ImageView logoView;
    @FXML
    private Arc spinner;
    @FXML
    private Label statusLabel;

    // Metodo que se ejecuta al iniciar el SplashScreen
    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        try {
            URL logoUrl = getClass().getResource("/sentinel_agent/img/logoSinFondo.png");
            if (logoUrl != null) {
                Image logo = new Image(logoUrl.toString());
                logoView.setImage(logo);
            } else {
                System.out.println("Aviso: No se encontró logoSinFondo.png en src/main/resources/sentinel_agent/img/");
            }
        } catch (Exception e) {
            // Controla cualquier fallo en la lectura de imagenes sin detener la app
            System.out.println("Aviso: Error al cargar el logoSinFondo.png: " + e.getMessage());
        }

        // Configura una transicion de rotacion para el arco que dura 2 segundos por
        // ciclo
        RotateTransition rt = new RotateTransition(Duration.seconds(2), spinner);

        rt.setByAngle(360);
        rt.setCycleCount(RotateTransition.INDEFINITE);
        rt.setInterpolator(Interpolator.LINEAR);
        rt.play();

        // Tarea concurrente de inicializacion en segundo plano para no congelar la UI
        Task<Void> initTask = new Task<Void>() {
            @Override
            protected Void call() throws Exception {
                // Notifica al usuario del inicio de la conexion con el cluster Atlas
                updateMessage("Estableciendo conexión con MongoDB Atlas...");
                // Dispara el Singleton de ConexionMongo para instanciar el MongoClient
                ConexionMongo.getDatabase();

                // Actualiza el mensaje a exito de conexion
                updateMessage("Sistema configurado correctamente.");
                // Pequeña pausa estetica de 800 milisegundos para legibilidad
                Thread.sleep(800);
                // Fin de la tarea
                return null;
            }
        };

        // Escucha y vincula los cambios de mensaje de la tarea al texto de statusLabel
        initTask.messageProperty().addListener((obs, oldVal, newVal) -> {
            // Sincroniza la actualizacion con el hilo principal de renderizado de la UI
            Platform.runLater(() -> statusLabel.setText(newVal));
        });

        // Evento que se dispara cuando la tarea en segundo plano finaliza con éxito
        initTask.setOnSucceeded(event -> {
            try {
                App.abrirInicioSesion();
            } catch (Exception e) {
                // Imprime traza de fallo de transicion
                e.printStackTrace();
            }
        });

        initTask.setOnFailed(event -> {
            Platform.runLater(() -> {
                statusLabel.setText("Error de conexión. Verifique su internet.");
                statusLabel.setStyle("-fx-text-fill: #ff6b6b; -fx-font-weight: bold;");
            });
            System.err.println("Error en la inicialización: " + initTask.getException());
        });

        Thread thread = new Thread(initTask);
        thread.setDaemon(true);
        // Arranca fisicamente la ejecucion del hilo secundario
        thread.start();
    }
}
