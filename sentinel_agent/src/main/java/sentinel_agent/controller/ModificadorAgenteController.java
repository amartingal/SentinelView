package sentinel_agent.controller;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import sentinel_agent.model.UsuarioDAO;
import sentinel_agent.utils.AlertaUtil;
import sentinel_agent.utils.ValidacionUtil;
import sentinel_agent.view.App;

public class ModificadorAgenteController implements Initializable {
    @FXML
    private Button btnVolver;
    @FXML
    private ListView<String> listProcesos;
    @FXML
    private TextField txtNuevoProceso;
    @FXML
    private ListView<Integer> listPuertos;
    @FXML
    private TextField txtNuevoPuerto;

    private UsuarioDAO usuarioDAO;
    private ObservableList<String> procesosObservable;

    // Lista observable para sincronizar de forma automatica la coleccion de puertos
    // con la UI
    private ObservableList<Integer> puertosObservable;

    // Metodo de inicializacion que carga la configuracion activa del usuario al
    // abrir la pantalla
    @Override
    public void initialize(URL location, ResourceBundle resources) {
        usuarioDAO = new UsuarioDAO();
        String usuarioLogueado = App.getUsuarioLogueado();

        if (usuarioLogueado != null && !usuarioLogueado.isEmpty()) {
            List<String> procesos = usuarioDAO.obtenerProcesosPermitidos(usuarioLogueado);
            List<Integer> puertos = usuarioDAO.obtenerPuertosPermitidos(usuarioLogueado);

            procesosObservable = FXCollections
                    .observableArrayList(procesos != null ? procesos : new ArrayList<String>());
            puertosObservable = FXCollections.observableArrayList(puertos != null ? puertos : new ArrayList<Integer>());

            listProcesos.setItems(procesosObservable);
            listPuertos.setItems(puertosObservable);
        } else {
            procesosObservable = FXCollections.observableArrayList();
            puertosObservable = FXCollections.observableArrayList();
            listProcesos.setItems(procesosObservable);
            listPuertos.setItems(puertosObservable);
            AlertaUtil.mostrarAdvertencia("Sin sesión", "No hay usuario logueado.");
        }

        // VALIDACIÓN EN TIEMPO REAL: Nombre de proceso al escribirse
        txtNuevoProceso.textProperty().addListener((observable, oldValue, newValue) -> {
            String texto = newValue != null ? newValue.trim() : "";
            if (!texto.isEmpty()) {
                if (ValidacionUtil.esProcesoValido(texto)) {
                    txtNuevoProceso.setStyle("-fx-border-color: green; -fx-border-width: 2; -fx-border-radius: 3;");
                } else {
                    txtNuevoProceso.setStyle("-fx-border-color: red; -fx-border-width: 2; -fx-border-radius: 3;");
                }
            } else {
                txtNuevoProceso.setStyle("");
            }
        });

        // VALIDACIÓN EN TIEMPO REAL: Número de puerto al escribirse
        txtNuevoPuerto.textProperty().addListener((observable, oldValue, newValue) -> {
            String texto = newValue != null ? newValue.trim() : "";
            if (!texto.isEmpty()) {
                try {
                    int puerto = Integer.parseInt(texto);
                    if (puerto >= 1 && puerto <= 65535) {
                        txtNuevoPuerto.setStyle("-fx-border-color: green; -fx-border-width: 2; -fx-border-radius: 3;");
                    } else {
                        txtNuevoPuerto.setStyle("-fx-border-color: red; -fx-border-width: 2; -fx-border-radius: 3;");
                    }
                } catch (NumberFormatException e) {
                    txtNuevoPuerto.setStyle("-fx-border-color: red; -fx-border-width: 2; -fx-border-radius: 3;");
                }
            } else {
                txtNuevoPuerto.setStyle("");
            }
        });
    }

    // Añade un nuevo proceso a la configuracion al pulsar el boton Añadir
    @FXML
    private void anadirProceso(ActionEvent event) {
        String nuevoProceso = txtNuevoProceso.getText().trim();
        if (nuevoProceso.isEmpty()) {
            AlertaUtil.mostrarAdvertencia("Campo vacío", "Debe ingresar el nombre de un proceso.");
            return;
        }

        // Valida el formato del proceso (debe tener formato proceso.exe)
        if (!ValidacionUtil.esProcesoValido(nuevoProceso)) {
            AlertaUtil.mostrarAdvertencia("Formato inválido",
                    "El proceso debe tener un formato válido (ej. proceso.exe) y terminar en .exe.");
            return;
        }

        // Comprueba si el proceso ya fue ingresado con anterioridad
        if (procesosObservable.contains(nuevoProceso)) {
            AlertaUtil.mostrarAdvertencia("Proceso duplicado", "El proceso ya se encuentra en la lista.");
            return;
        }

        procesosObservable.add(nuevoProceso);
        guardarProcesos();
        txtNuevoProceso.clear();
    }

    // Quita el proceso seleccionado en la lista al pulsar el boton Quitar
    @FXML
    private void quitarProceso(ActionEvent event) {
        String seleccionado = listProcesos.getSelectionModel().getSelectedItem();
        if (seleccionado == null) {
            AlertaUtil.mostrarAdvertencia("Selección requerida", "Seleccione un proceso para quitarlo.");
            return;
        }

        procesosObservable.remove(seleccionado);
        guardarProcesos();
    }

    // Incorpora un nuevo puerto de red a la configuracion
    @FXML
    private void anadirPuerto(ActionEvent event) {
        String nuevoPuertoStr = txtNuevoPuerto.getText().trim();
        if (nuevoPuertoStr.isEmpty()) {
            AlertaUtil.mostrarAdvertencia("Campo vacío", "Debe ingresar un número de puerto.");
            return;
        }

        try {
            Integer puerto = Integer.parseInt(nuevoPuertoStr);
            if (puerto < 1 || puerto > 65535) {
                AlertaUtil.mostrarAdvertencia("Puerto inválido", "El puerto debe estar entre 1 y 65535.");
                return;
            }

            if (puertosObservable.contains(puerto)) {
                AlertaUtil.mostrarAdvertencia("Puerto duplicado", "El puerto ya se encuentra en la lista.");
                return;
            }

            puertosObservable.add(puerto);
            guardarPuertos();
            txtNuevoPuerto.clear();
        } catch (NumberFormatException e) {
            AlertaUtil.mostrarError("Error de formato", "Debe ingresar un número válido.");
        }
    }

    // Elimina el puerto de red seleccionado de la lista al pulsar Quitar
    @FXML
    private void quitarPuerto(ActionEvent event) {
        Integer seleccionado = listPuertos.getSelectionModel().getSelectedItem();
        if (seleccionado == null) {
            AlertaUtil.mostrarAdvertencia("Selección requerida", "Seleccione un puerto para quitarlo.");
            return;
        }

        puertosObservable.remove(seleccionado);
        guardarPuertos();
    }

    // Escribe la lista completa de procesos permitidos en MongoDB Atlas
    private void guardarProcesos() {
        String usuario = App.getUsuarioLogueado();
        if (usuario != null) {
            boolean exito = usuarioDAO.actualizarProcesosPermitidos(usuario, new ArrayList<>(procesosObservable));
            if (!exito) {
                AlertaUtil.mostrarError("Error", "No se pudieron guardar los procesos en la base de datos.");
            }
        }
    }

    // Escribe la lista completa de puertos permitidos en MongoDB Atlas
    private void guardarPuertos() {
        String usuario = App.getUsuarioLogueado();
        if (usuario != null) {
            boolean exito = usuarioDAO.actualizarPuertosPermitidos(usuario, new ArrayList<>(puertosObservable));
            if (!exito) {
                AlertaUtil.mostrarError("Error", "No se pudieron guardar los puertos en la base de datos.");
            }
        }
    }

    // Regresa a la vista anterior de Ajustes
    @FXML
    private void volverAtras(ActionEvent event) {
        try {
            App.setRoot("Ajustes");
        } catch (IOException e) {
            e.printStackTrace();
            AlertaUtil.mostrarError("Error", "No se pudo volver a Ajustes.");
        }
    }
}
