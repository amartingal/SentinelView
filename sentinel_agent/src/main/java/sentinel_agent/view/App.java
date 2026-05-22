package sentinel_agent.view;

import java.io.IOException;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class App extends Application {

    private static Scene scene;
    private static Stage splashStage;
    private static Stage mainStage;

    public static final int MAIN_WIDTH = 1300;
    public static final int MAIN_HEIGHT = 750;
    private static String usuarioLogueado;

    // Metodo que inicia la aplicacion
    @Override
    public void start(Stage stage) throws IOException {
        splashStage = stage;
        splashStage.initStyle(javafx.stage.StageStyle.UNDECORATED);
        scene = new Scene(loadFXML("SplashScreen"), 640, 480);
        scene.getStylesheets().add(App.class.getResource("/sentinel_agent/style.css").toExternalForm());
        splashStage.setScene(scene);

        splashStage.getIcons().add(new javafx.scene.image.Image(
                App.class.getResourceAsStream("/sentinel_agent/img/logoSinFondo.png")));
        splashStage.centerOnScreen();
        splashStage.show();

        Runtime.getRuntime().addShutdownHook(new Thread(sentinel_agent.model.ConexionMongo::cerrarConexion));
    }

    // Metodo que abre la pantalla de inicio de sesion
    public static void abrirInicioSesion() throws IOException {
        if (splashStage != null) {
            splashStage.close();
        }
        mainStage = new Stage();
        scene = new Scene(loadFXML("InicioSesion"), MAIN_WIDTH, MAIN_HEIGHT);
        scene.getStylesheets().add(App.class.getResource("/sentinel_agent/style.css").toExternalForm());
        mainStage.setScene(scene);
        mainStage.setTitle("Sentinel Agent");
        mainStage.getIcons().add(new javafx.scene.image.Image(
                App.class.getResourceAsStream("/sentinel_agent/img/logoSinFondo.png")));
        mainStage.setResizable(false);
        mainStage.setMinWidth(MAIN_WIDTH);
        mainStage.setMinHeight(MAIN_HEIGHT);
        mainStage.setMaxWidth(MAIN_WIDTH);
        mainStage.setMaxHeight(MAIN_HEIGHT);
        mainStage.show();
    }

    // Metodo que retorna el stage principal
    public static Stage getMainStage() {
        return mainStage;
    }

    // Metodo que cambia la pantalla principal
    public static void setRoot(String fxml) throws IOException {
        scene.setRoot(loadFXML(fxml));
        if (mainStage != null) {
            mainStage.setWidth(MAIN_WIDTH);
            mainStage.setHeight(MAIN_HEIGHT);
            mainStage.centerOnScreen();
        }
    }

    // Metodo que retorna el usuario logueado
    public static String getUsuarioLogueado() {
        return usuarioLogueado;
    }

    public static void setUsuarioLogueado(String usuario) {
        usuarioLogueado = usuario;
    }

    // Metodo que carga un archivo FXML
    private static Parent loadFXML(String fxml) throws IOException {
        FXMLLoader fxmlLoader = new FXMLLoader(App.class.getResource("/sentinel_agent/" + fxml + ".fxml"));
        return fxmlLoader.load();
    }

    // Metodo que inicia la aplicacion
    public static void main(String[] args) {
        launch();
    }
}
