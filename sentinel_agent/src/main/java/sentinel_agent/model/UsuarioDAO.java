package sentinel_agent.model;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.TreeSet;
import org.bson.Document;
import org.mindrot.jbcrypt.BCrypt;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Updates;
import com.mongodb.client.result.UpdateResult;

// Clase que interactua con la coleccion Usuarios de MongoDB
public class UsuarioDAO {
    // Referencia a la coleccion de Usuarios en MongoDB
    private MongoCollection<Document> coleccion;

    // Constructor que obtiene la coleccion Usuarios de MongoDB
    public UsuarioDAO() {
        MongoDatabase baseDatos = ConexionMongo.getDatabase();
        coleccion = baseDatos.getCollection("Usuarios");
    }

    // Metodo que comprueba si existe un usuario con el nombre de usuario
    // proporcionado
    public boolean existeUsuario(String nombreUsuario) {
        Document usuario = coleccion.find(Filters.eq("usuario", nombreUsuario)).first();
        return usuario != null;
    }

    // Metodo que comprueba si existe un usuario con el correo electronico
    // proporcionado
    public boolean existeEmail(String email) {
        Document usuario = coleccion.find(Filters.eq("email", email)).first();
        return usuario != null;
    }

    // Metodo que comprueba si existe un usuario con el nombre de usuario y correo
    // electronico proporcionados
    public boolean verificarUsuarioYEmail(String usuario, String email) {
        Document userDoc = coleccion.find(Filters.and(
                Filters.eq("usuario", usuario),
                Filters.eq("email", email))).first();
        return userDoc != null;
    }

    // Metodo que actualiza la contraseña de un usuario
    public boolean actualizarPassword(String usuario, String nuevaPassword) {
        try {
            String passwordHasheada = BCrypt.hashpw(nuevaPassword, BCrypt.gensalt());
            UpdateResult resultado = coleccion.updateOne(
                    Filters.eq("usuario", usuario),
                    Updates.set("password", passwordHasheada));
            return resultado.getModifiedCount() > 0;
        } catch (Exception e) {
            System.err.println("Error al actualizar contraseña: " + e.getMessage());
            return false;
        }
    }

    // Metodo que registra un nuevo usuario
    public boolean registrarUsuario(String nombreUsuario, String password, String email) {
        if (existeUsuario(nombreUsuario)) {
            return false;
        }
        String passwordHasheada = BCrypt.hashpw(password, BCrypt.gensalt());
        Document nuevoUsuario = new Document("usuario", nombreUsuario)
                .append("password", passwordHasheada)
                .append("email", email);
        coleccion.insertOne(nuevoUsuario);

        MongoDatabase baseDatos = ConexionMongo.getDatabase();
        MongoCollection<Document> configColeccion = baseDatos.getCollection("Configuraciones");
        Document configInicial = new Document("tipo", "admin_conf")
                .append("grupo", nombreUsuario)
                .append("puertos_permitidos", new ArrayList<Integer>())
                .append("procesos_permitidos", new ArrayList<String>());
        configColeccion.insertOne(configInicial);

        return true;
    }

    // Compara el hash guardado de la base de datos con la password ingresada
    public boolean validarLogin(String nombreUsuario, String password) {
        Document usuarioDoc = coleccion.find(Filters.eq("usuario", nombreUsuario)).first();

        if (usuarioDoc != null) {
            String hashAlmacenado = usuarioDoc.getString("password");
            try {
                return BCrypt.checkpw(password, hashAlmacenado);
            } catch (Exception e) {
                System.out.println("Error de seguridad en validación: " + e.getMessage());
                return false;
            }
        }

        return false;
    }

    // Elimina el usuario y su configuracion del agente de forma permanente
    public boolean borrarUsuario(String nombreUsuario) {
        try {
            coleccion.deleteOne(Filters.eq("usuario", nombreUsuario));

            MongoDatabase baseDatos = ConexionMongo.getDatabase();
            MongoCollection<Document> configColeccion = baseDatos.getCollection("Configuraciones");
            configColeccion.deleteMany(Filters.eq("grupo", nombreUsuario));

            return true;
        } catch (Exception e) {
            System.err.println("Error al borrar usuario: " + e.getMessage());
            return false;
        }
    }

    // Obtiene la lista de nombres de procesos autorizados para el usuario
    public List<String> obtenerProcesosPermitidos(String usuario) {
        MongoDatabase baseDatos = ConexionMongo.getDatabase();
        MongoCollection<Document> configColeccion = baseDatos.getCollection("Configuraciones");

        Document userDoc = configColeccion
                .find(Filters.and(Filters.eq("tipo", "admin_conf"), Filters.eq("grupo", usuario))).first();
        if (userDoc != null && userDoc.containsKey("procesos_permitidos")) {
            return userDoc.getList("procesos_permitidos", String.class);
        }
        return new ArrayList<>();
    }

    // Obtiene la lista de puertos TCP/UDP autorizados para el usuario
    public List<Integer> obtenerPuertosPermitidos(String usuario) {
        MongoDatabase baseDatos = ConexionMongo.getDatabase();
        MongoCollection<Document> configColeccion = baseDatos.getCollection("Configuraciones");

        Document userDoc = configColeccion
                .find(Filters.and(Filters.eq("tipo", "admin_conf"), Filters.eq("grupo", usuario))).first();
        if (userDoc != null && userDoc.containsKey("puertos_permitidos")) {
            List<?> rawList = userDoc.getList("puertos_permitidos", Object.class);
            List<Integer> puertos = new ArrayList<>();
            for (Object obj : rawList) {
                if (obj instanceof Number) {
                    puertos.add(((Number) obj).intValue());
                } else if (obj instanceof String) {
                    try {
                        puertos.add(Integer.parseInt((String) obj));
                    } catch (Exception ignored) {
                    }
                }
            }
            return puertos;
        }
        return new ArrayList<>();
    }

    // Actualiza la lista de procesos permitidos para el agente de monitoreo
    public boolean actualizarProcesosPermitidos(String usuario, List<String> procesos) {
        try {
            MongoDatabase baseDatos = ConexionMongo.getDatabase();
            MongoCollection<Document> configColeccion = baseDatos.getCollection("Configuraciones");

            UpdateResult resultado = configColeccion.updateOne(
                    Filters.and(Filters.eq("tipo", "admin_conf"), Filters.eq("grupo", usuario)),
                    Updates.set("procesos_permitidos", procesos));
            return resultado.getModifiedCount() > 0 || resultado.getMatchedCount() > 0;
        } catch (Exception e) {
            System.err.println("Error al actualizar procesos permitidos: " + e.getMessage());
            return false;
        }
    }

    // Actualiza la lista de puertos permitidos para el agente de red
    public boolean actualizarPuertosPermitidos(String usuario, List<Integer> puertos) {
        try {
            MongoDatabase baseDatos = ConexionMongo.getDatabase();
            MongoCollection<Document> configColeccion = baseDatos.getCollection("Configuraciones");

            UpdateResult resultado = configColeccion.updateOne(
                    Filters.and(Filters.eq("tipo", "admin_conf"), Filters.eq("grupo", usuario)),
                    Updates.set("puertos_permitidos", puertos));
            return resultado.getModifiedCount() > 0 || resultado.getMatchedCount() > 0;
        } catch (Exception e) {
            System.err.println("Error al actualizar puertos permitidos: " + e.getMessage());
            return false;
        }
    }

    // Lee e interpreta el valor de fecha_escaneo controlando tipos String e ISODate
    // de MongoDB
    private String leerFechaEscaneo(Document doc) {
        Object raw = doc.get("fecha_escaneo");

        if (raw == null) {
            System.err.println("[FECHA] campo 'fecha_escaneo' es null en documento: " + doc.getObjectId("_id"));
            return "Sin fecha";
        }
        System.out.println("[FECHA] tipo=" + raw.getClass().getSimpleName() + " | valor=" + raw);

        if (raw instanceof String) {
            String s = (String) raw;
            return s.isEmpty() ? "Sin fecha" : s;
        }

        if (raw instanceof Date) {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
            sdf.setTimeZone(java.util.TimeZone.getTimeZone("UTC"));
            return sdf.format((Date) raw);
        }

        System.err.println("[FECHA] tipo inesperado " + raw.getClass().getName() + ", usando toString()");
        return raw.toString();
    }

    // Formatea un documento de reporte a una linea de texto legible para el
    // ListView
    public String formatearDocumentoReporte(Document doc) {
        String macAddress = doc.getString("mac_address") != null ? doc.getString("mac_address") : "Sin MAC";
        String equipoId = doc.getString("equipo_id") != null ? doc.getString("equipo_id") : "Sin equipo";
        String estado = doc.getString("estado") != null ? doc.getString("estado") : "Desconocido";

        return "MAC: " + macAddress
                + " | Equipo: " + equipoId
                + " | Estado: " + estado;
    }

    // Obtiene las fechas de escaneo unicas de los ultimos 7 dias ordenadas de mas
    // reciente a mas antigua
    public List<String> obtenerFechasConReportes(String grupo) {
        TreeSet<String> fechasSet = new TreeSet<>(Collections.reverseOrder());
        try {
            MongoDatabase baseDatos = ConexionMongo.getDatabase();
            MongoCollection<Document> coleccionReportes = baseDatos.getCollection("Reportes");
            for (Document doc : coleccionReportes
                    .find(Filters.or(Filters.eq("grupo", grupo), Filters.eq("Grupo", grupo)))) {
                String fechaEscaneo = leerFechaEscaneo(doc);

                if (!"Sin fecha".equals(fechaEscaneo) && fechaEscaneo.length() >= 10) {

                    fechasSet.add(fechaEscaneo.substring(0, 10));
                }
            }

            System.out.println("---> Fechas distintas con reportes para '" + grupo + "': " + fechasSet.size());
        } catch (Exception e) {
            System.err.println("Error al obtener fechas con reportes: " + e.getMessage());
        }

        // Convierte el TreeSet a una estructura de lista indexable
        List<String> fechas = new ArrayList<>(fechasSet);
        return fechas.size() > 7 ? fechas.subList(0, 7) : fechas;
    }

    public List<Document> obtenerReportesPorGrupoYFecha(String grupo, String fecha) {
        List<Document> resultados = new ArrayList<>();
        try {
            MongoDatabase baseDatos = ConexionMongo.getDatabase();
            MongoCollection<Document> coleccionReportes = baseDatos.getCollection("Reportes");
            for (Document doc : coleccionReportes
                    .find(Filters.or(Filters.eq("grupo", grupo), Filters.eq("Grupo", grupo)))) {
                String fechaEscaneo = leerFechaEscaneo(doc);
                if (fechaEscaneo.startsWith(fecha)) {
                    resultados.add(doc);
                }
            }
            System.out.println("---> Reportes del " + fecha + " para grupo '" + grupo + "': " + resultados.size());
        } catch (Exception e) {
            System.err.println("Error al obtener reportes por fecha: " + e.getMessage());
        }
        return resultados;
    }

    public Document obtenerEstadisticasDelDia(String grupo, String fechaISO) {
        List<Document> reportes = obtenerReportesPorGrupoYFecha(grupo, fechaISO);

        int totalProcesos = 0;
        int totalPuertos = 0;
        int totalArchivos = 0;
        int alertas = 0;

        java.util.Map<String, Integer> frecPuerto = new java.util.HashMap<>();

        java.util.Map<String, Integer> frecProceso = new java.util.HashMap<>();

        for (Document doc : reportes) {

            List<?> procs = doc.getList("procesos_sospechosos", Object.class);
            if (procs != null) {
                totalProcesos += procs.size();
                for (Object p : procs) {
                    String nombre = "";
                    if (p instanceof Document) {
                        nombre = ((Document) p).getString("nombre");
                    } else if (p != null) {
                        nombre = p.toString();
                    }
                    if (nombre != null && !nombre.isEmpty()) {
                        frecProceso.merge(nombre, 1, Integer::sum);
                    }
                }
            }

            List<?> puertos = doc.getList("puertos_sospechosos", Object.class);
            if (puertos != null) {
                totalPuertos += puertos.size();
                for (Object obj : puertos) {
                    String clave = "";
                    if (obj instanceof Document) {
                        Integer num = ((Document) obj).getInteger("puerto");
                        clave = num != null ? String.valueOf(num) : "";
                    } else if (obj != null) {
                        clave = obj.toString();
                    }
                    if (!clave.isEmpty()) {
                        frecPuerto.merge(clave, 1, Integer::sum);
                    }
                }
            }

            List<?> archivos = doc.getList("archivos_sospechosos", Object.class);
            if (archivos != null) {
                totalArchivos += archivos.size();
            }

            String estado = doc.getString("estado");
            if (estado != null && estado.equalsIgnoreCase("alerta")) {
                alertas++;
            }
        }

        // Calcula cual fue el puerto sospechoso mas recurrente del dia (Moda)
        String puertoTop = frecPuerto.entrySet().stream()
                .max(java.util.Map.Entry.comparingByValue())
                .map(java.util.Map.Entry::getKey)
                .orElse("-");

        // Calcula cual fue el proceso sospechoso mas recurrente del dia (Moda)
        String procesoTop = frecProceso.entrySet().stream()
                .max(java.util.Map.Entry.comparingByValue())
                .map(java.util.Map.Entry::getKey)
                .orElse("-");

        // Suma el total acumulado de incidencias del dia (procesos + puertos +archivos)
        int totalIncidencias = totalProcesos + totalPuertos + totalArchivos;

        // Construye y devuelve un Documento con toda la analitica procesada
        return new Document("totalIncidencias", totalIncidencias)
                .append("puertoTop", puertoTop)
                .append("procesoTop", procesoTop)
                .append("equipos", reportes.size())
                .append("alertas", alertas)
                .append("archivos", totalArchivos);
    }
}
