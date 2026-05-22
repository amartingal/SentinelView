package sentinel_agent.controller;

import java.io.IOException;
import java.util.List;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.VBox;
import sentinel_agent.view.App;
import sentinel_agent.utils.AlertaUtil;
import sentinel_agent.utils.ReportePDFUtil;
import javafx.stage.FileChooser;
import java.io.File;

public class PrincipalController {
    @FXML
    private DatePicker datePicker;
    @FXML
    private ComboBox<String> comboIdUsuario;
    @FXML
    private VBox vboxFechas;

    private static java.time.LocalDate fechaSeleccionada;

    @FXML
    private ListView<org.bson.Document> listaReportes;

    public static void resetFechaSeleccionada() {
        fechaSeleccionada = null;
    }

    @FXML
    private Button btnAjustes;
    @FXML
    private Button btnExportar;
    @FXML
    private Label lblTotalIncidencias;
    @FXML
    private Label lblPuertoTop;
    @FXML
    private Label lblProcesoTop;
    @FXML
    private Label lblEquipos;
    @FXML
    private Label lblAlertas;

    @FXML
    public void initialize() {
        btnAjustes.setTooltip(new Tooltip("Abrir pantalla de ajustes"));
        btnExportar.setTooltip(new Tooltip("Generar reporte en formato externo"));

        configurarEfectoHover(btnAjustes);
        configurarEfectoHover(btnExportar);

        String usuarioActual = App.getUsuarioLogueado();
        comboIdUsuario.setItems(FXCollections.observableArrayList(
                usuarioActual != null ? usuarioActual : "Sin sesión"));
        comboIdUsuario.getSelectionModel().selectFirst();
        comboIdUsuario.setDisable(true);

        if (fechaSeleccionada == null) {
            fechaSeleccionada = java.time.LocalDate.now();
        }
        datePicker.setValue(fechaSeleccionada);

        // Listener para filtrar reportes reactivamente al cambiar la fecha
        datePicker.valueProperty().addListener((ov, oldValue, newValue) -> {
            if (newValue == null)
                return;

            // Bloquear la selección de fechas futuras
            if (newValue.isAfter(java.time.LocalDate.now())) {
                javafx.application.Platform.runLater(() -> {
                    AlertaUtil.mostrarAdvertencia("Fecha no válida", "No se pueden seleccionar fechas futuras.");
                    datePicker.setValue(oldValue != null ? oldValue : java.time.LocalDate.now());
                });
                return;
            }

            fechaSeleccionada = newValue;
            filtrarPorFecha(newValue.toString());
        });

        filtrarPorFecha(fechaSeleccionada.toString());
        cargarFechasRecientes();

        // Doble clic sobre un reporte para abrir su desglose de incidencias
        listaReportes.setOnMouseClicked(event -> {
            if (event.getClickCount() == 2) {
                org.bson.Document seleccionado = listaReportes.getSelectionModel().getSelectedItem();
                if (seleccionado != null) {
                    try {
                        sentinel_agent.controller.IncidenciasController.setReporteActual(seleccionado);
                        App.setRoot("Incidencias");
                    } catch (Exception e) {
                        e.printStackTrace();
                        AlertaUtil.mostrarError("Error", "No se pudo abrir la vista de incidencias.");
                    }
                }
            }
        });

        // Formateador personalizado para las celdas de la lista de reportes
        listaReportes.setCellFactory(param -> new ListCell<org.bson.Document>() {
            @Override
            protected void updateItem(org.bson.Document item, boolean empty) {
                super.updateItem(item, empty);
                actualizarEstiloCelda(item, empty, isSelected());
            }

            @Override
            public void updateSelected(boolean selected) {
                super.updateSelected(selected);
                actualizarEstiloCelda(getItem(), isEmpty(), selected);
            }

            private void actualizarEstiloCelda(org.bson.Document item, boolean empty, boolean selected) {
                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                    setStyle("-fx-background-color: transparent;");
                } else {
                    sentinel_agent.model.UsuarioDAO dao = new sentinel_agent.model.UsuarioDAO();
                    String textoItem = dao.formatearDocumentoReporte(item);

                    String estado = item.getString("estado");
                    boolean isAlerta = estado != null && estado.equalsIgnoreCase("alerta");

                    // Círculo indicador de estado (Rojo si hay alerta, Verde si es seguro)
                    javafx.scene.shape.Circle indicador = new javafx.scene.shape.Circle(8);
                    indicador.setFill(isAlerta ? javafx.scene.paint.Color.RED : javafx.scene.paint.Color.GREEN);

                    javafx.scene.control.Label label = new javafx.scene.control.Label(textoItem);
                    label.setAlignment(Pos.CENTER);

                    javafx.scene.layout.HBox hbox = new javafx.scene.layout.HBox(15, indicador, label);
                    hbox.setAlignment(Pos.CENTER);

                    String estiloBox = "-fx-border-color: black; " +
                            "-fx-border-width: 1; " +
                            "-fx-padding: 15px; " +
                            "-fx-text-fill: black; " +
                            "-fx-alignment: center; ";

                    if (selected) {
                        estiloBox += "-fx-font-weight: bold; -fx-background-color: lightgrey;";
                    } else {
                        estiloBox += "-fx-font-weight: normal; -fx-background-color: white;";
                    }

                    hbox.setStyle(estiloBox);

                    // Restamos margen al binding de ancho para evitar barra de scroll horizontal
                    hbox.prefWidthProperty().bind(listaReportes.widthProperty().subtract(40));
                    hbox.setMaxWidth(javafx.scene.layout.Region.USE_PREF_SIZE);

                    javafx.scene.layout.VBox container = new javafx.scene.layout.VBox(hbox);
                    container.setAlignment(Pos.CENTER);
                    javafx.scene.layout.VBox.setMargin(hbox, new javafx.geometry.Insets(0, 0, 15, 0));

                    setGraphic(container);
                    setAlignment(Pos.CENTER);
                    setText(null);
                    setStyle("-fx-background-color: transparent; -fx-padding: 5px; -fx-alignment: center;");
                }
            }
        });
    }

    // Genera botones laterales dinámicos para acceder rápidamente a las últimas 7
    // fechas de escaneo
    private void cargarFechasRecientes() {
        String usuarioActual = App.getUsuarioLogueado();
        if (usuarioActual == null || usuarioActual.isEmpty())
            return;

        sentinel_agent.model.UsuarioDAO dao = new sentinel_agent.model.UsuarioDAO();
        List<String> fechas = dao.obtenerFechasConReportes(usuarioActual);

        vboxFechas.getChildren().clear();

        if (fechas.isEmpty()) {
            Label sinFechas = new Label("Sin reportes recientes");
            sinFechas.setStyle("-fx-text-fill: #555555; -fx-font-style: italic;");
            vboxFechas.getChildren().add(sinFechas);
            return;
        }

        for (String fechaISO : fechas) {
            String fechaDisplay = formatearFechaDisplay(fechaISO);
            Button btnFecha = new Button(fechaDisplay);
            btnFecha.setMaxWidth(240.0);
            btnFecha.setMinWidth(240.0);
            btnFecha.setPrefHeight(35.0);
            btnFecha.setMinHeight(35.0);
            btnFecha.setUserData(fechaISO);

            btnFecha.setStyle("-fx-background-color: white; -fx-border-color: gray;"
                    + " -fx-border-radius: 5; -fx-text-fill: #0b295c; -fx-font-size: 14px; -fx-cursor: hand;");

            btnFecha.setOnAction(e -> {
                try {
                    java.time.LocalDate nuevaFecha = java.time.LocalDate.parse(fechaISO);
                    datePicker.setValue(nuevaFecha);
                } catch (Exception ex) {
                    System.err.println("Error al actualizar DatePicker: " + ex.getMessage());
                }
            });

            vboxFechas.getChildren().add(btnFecha);
        }

        if (datePicker.getValue() != null) {
            actualizarBotonSeleccionado(datePicker.getValue().toString());
        }
    }

    private void resetearEstiloBotonesFechas() {
        for (Node node : vboxFechas.getChildren()) {
            if (node instanceof Button) {
                node.setStyle("-fx-background-color: white; -fx-border-color: gray;"
                        + " -fx-border-radius: 5; -fx-text-fill: #0b295c; -fx-font-size: 14px; -fx-cursor: hand;");
            }
        }
    }

    // Resalta visualmente el botón lateral de la fecha seleccionada
    private void actualizarBotonSeleccionado(String fechaISO) {
        resetearEstiloBotonesFechas();
        for (Node node : vboxFechas.getChildren()) {
            if (node instanceof Button) {
                Button btn = (Button) node;
                if (fechaISO.equals(btn.getUserData())) {
                    btn.setStyle("-fx-background-color: white; -fx-border-color: #0b295c;"
                            + " -fx-border-radius: 5; -fx-border-width: 2;"
                            + " -fx-text-fill: #0b295c; -fx-font-weight: bold; -fx-font-size: 14px; -fx-cursor: hand;");
                    break;
                }
            }
        }
    }

    // Convierte YYYY-MM-DD a formato legible DD/MM/YYYY
    private String formatearFechaDisplay(String fechaISO) {
        if (fechaISO == null || fechaISO.length() < 10)
            return fechaISO;
        String[] partes = fechaISO.split("-");
        if (partes.length < 3)
            return fechaISO;
        return partes[2] + "/" + partes[1] + "/" + partes[0];
    }

    // Filtra la lista de reportes según la fecha indicada
    private void filtrarPorFecha(String fechaISO) {
        String usuarioActual = App.getUsuarioLogueado();
        if (usuarioActual != null && !usuarioActual.isEmpty()) {
            sentinel_agent.model.UsuarioDAO dao = new sentinel_agent.model.UsuarioDAO();
            List<org.bson.Document> reportes = dao.obtenerReportesPorGrupoYFecha(usuarioActual, fechaISO);

            if (reportes.isEmpty()) {
                listaReportes.setItems(FXCollections.observableArrayList());
                resetearEstiloBotonesFechas();

                javafx.application.Platform.runLater(() -> {
                    AlertaUtil.mostrarAdvertencia(
                            "Sin informes",
                            "No se encontraron informes para la fecha: "
                                    + formatearFechaDisplay(fechaISO) + ".");
                });
            } else {
                listaReportes.setItems(FXCollections.observableArrayList(reportes));
                actualizarBotonSeleccionado(fechaISO);
            }

            cargarEstadisticas(fechaISO);
        }
    }

    @FXML
    private void abrirAjustes(ActionEvent event) {
        try {
            App.setRoot("Ajustes");
        } catch (IOException e) {
            e.printStackTrace();
            AlertaUtil.mostrarError("Error", "No se pudo abrir la pantalla de ajustes.");
        }
    }

    // Exporta la información actual de la tabla a formato PDF
    @FXML
    private void exportarTodo(ActionEvent event) {
        String usuarioActual = App.getUsuarioLogueado();
        ObservableList<org.bson.Document> reportes = listaReportes.getItems();

        if (reportes == null || reportes.isEmpty()) {
            AlertaUtil.mostrarAdvertencia("Sin datos", "No hay reportes para exportar en la fecha seleccionada.");
            return;
        }

        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Guardar Reporte PDF");
        fileChooser.setInitialFileName("Reporte_" + usuarioActual + "_" + datePicker.getValue().toString() + ".pdf");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Archivos PDF", "*.pdf"));

        File file = fileChooser.showSaveDialog(App.getMainStage());

        if (file != null) {
            boolean exito = ReportePDFUtil.generarPDFReportes(
                    reportes,
                    usuarioActual,
                    datePicker.getValue().toString(),
                    file);

            if (exito) {
                AlertaUtil.mostrarInfo("Exportación Exitosa",
                        "El reporte se ha guardado correctamente en: " + file.getAbsolutePath());
            } else {
                AlertaUtil.mostrarError("Error de Exportación", "No se pudo generar el archivo PDF.");
            }
        }
    }

    private void configurarEfectoHover(Button btn) {
        String estiloOriginal = btn.getStyle();
        String estiloHover = estiloOriginal + " -fx-background-color: #153c7a;";

        btn.setOnMouseEntered(e -> btn.setStyle(estiloHover));
        btn.setOnMouseExited(e -> btn.setStyle(estiloOriginal));
    }

    // Carga los contadores y estadísticas agregadas en la cabecera superior del
    // Dashboard
    private void cargarEstadisticas(String fechaISO) {
        String usuarioActual = App.getUsuarioLogueado();
        if (usuarioActual == null || usuarioActual.isEmpty())
            return;

        sentinel_agent.model.UsuarioDAO dao = new sentinel_agent.model.UsuarioDAO();
        org.bson.Document stats = dao.obtenerEstadisticasDelDia(usuarioActual, fechaISO);

        lblTotalIncidencias.setText(String.valueOf(stats.getInteger("totalIncidencias", 0)));
        lblPuertoTop.setText(stats.getString("puertoTop") != null ? stats.getString("puertoTop") : "-");
        lblProcesoTop.setText(stats.getString("procesoTop") != null ? stats.getString("procesoTop") : "-");
        lblEquipos.setText(String.valueOf(stats.getInteger("equipos", 0)));
        lblAlertas.setText(String.valueOf(stats.getInteger("alertas", 0)));
    }
}
