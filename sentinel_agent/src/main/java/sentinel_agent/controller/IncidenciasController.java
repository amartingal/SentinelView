package sentinel_agent.controller;

import java.io.IOException;
import org.bson.Document;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import sentinel_agent.view.App;

public class IncidenciasController {
    @FXML
    private TextField txtFecha;
    @FXML
    private TextField txtMac;
    @FXML
    private TextField txtIdUsuario;
    @FXML
    private TextArea lblPuertos;
    @FXML
    private TextArea lblProcesos;
    @FXML
    private Button btnAtras;

    private static Document reporteActual;

    public static void setReporteActual(Document reporte) {
        reporteActual = reporte;
    }

    // Inicializa de forma automatica la vista cargando y formateando los datos
    @FXML
    public void initialize() {
        if (reporteActual != null) {
            cargarDatosReporte();
        }
    }

    // Extrae y distribuye la informacion del reporte MongoDB en la interfaz de
    // usuario
    private void cargarDatosReporte() {
        String mac = reporteActual.getString("mac_address");
        Object fechaRaw = reporteActual.get("fecha_escaneo");
        String fechaStr = (fechaRaw != null) ? fechaRaw.toString() : "Sin fecha";
        if (fechaStr.length() >= 10) {
            fechaStr = fechaStr.substring(0, 10);
            String[] partes = fechaStr.split("-");
            if (partes.length == 3) {
                fechaStr = partes[2] + "/" + partes[1] + "/" + partes[0];
            }
        }
        String equipoId = reporteActual.getString("equipo_id");
        txtFecha.setText(fechaStr);
        txtMac.setText(mac != null ? mac : "Desconocida");
        txtIdUsuario.setText(equipoId != null ? equipoId : "Desconocido");
        StringBuilder sbProcesos = new StringBuilder();
        Object procesosRaw = reporteActual.get("procesos_sospechosos");

        if (procesosRaw instanceof Iterable) {
            for (Object obj : (Iterable<?>) procesosRaw) {
                if (obj instanceof Document) {
                    Document proc = (Document) obj;
                    String nombre = proc.getString("nombre");
                    sbProcesos.append(nombre != null ? nombre : "Proceso desconocido").append("\n");
                } else if (obj != null) {
                    sbProcesos.append(obj.toString()).append("\n");
                }
            }
        }

        if (sbProcesos.length() == 0) {
            lblProcesos.setText("No se encontraron procesos sospechosos.");
        } else {
            lblProcesos.setText(sbProcesos.toString().trim());
        }

        StringBuilder sbPuertos = new StringBuilder();
        Object puertosRaw = reporteActual.get("puertos_sospechosos");
        if (puertosRaw instanceof Iterable) {
            for (Object obj : (Iterable<?>) puertosRaw) {
                if (obj instanceof Document) {
                    Document pDoc = (Document) obj;
                    Integer num = pDoc.getInteger("puerto");
                    if (num != null) {
                        sbPuertos.append(num).append("\n");
                    }
                } else if (obj != null) {
                    sbPuertos.append(obj.toString()).append("\n");
                }
            }
        }

        if (sbPuertos.length() == 0) {
            lblPuertos.setText("No se encontraron puertos sospechosos.");
        } else {
            lblPuertos.setText(sbPuertos.toString().trim());
        }
    }

    // Regresa al panel Dashboard principal al presionar el boton Volver
    @FXML
    private void volverAtras(ActionEvent event) {
        try {
            App.setRoot("Principal");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
