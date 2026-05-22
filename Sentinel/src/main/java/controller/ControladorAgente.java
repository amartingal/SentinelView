package controller;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Optional;
import java.util.Properties;

import org.bson.Document;

import dao.ConexionMongo;
import io.GestorArchivos;
import scanner.ScannerProcesos;
import scanner.ScannerPuertos;

public class ControladorAgente {
	
	private List<String> procesosDelDia = new ArrayList<>();
    private List<Integer> puertosDelDia = new ArrayList<>();
    
    private int ultimoDiaSubido = -1; // Para evitar subir dos veces el mismo día
    private int[] diaDeLosDatos = {-1}; // Array de 1 posición que actua como puntero para saber de que dia son los datos en cache
    private String nombreGrupo = "GRUPO_DESCONOCIDO";
    
    private GestorArchivos gestorArchivos = new GestorArchivos();

    
    // Se llama solo 1 vez al encender el programa
    public void arrancarAgente() {
        gestorArchivos.cargarAlertas(procesosDelDia, puertosDelDia, diaDeLosDatos);
        ultimoDiaSubido = gestorArchivos.cargarUltimoDiaSubido();
        
        // Miramos si ya lo teniamos guardado en la carpeta oculta
        this.nombreGrupo = gestorArchivos.cargarGrupo();
        
        // Si es nulo (primera instalacion), buscamos el fichero .properties
        if (this.nombreGrupo == null || this.nombreGrupo.isEmpty()) {
            
            Properties propiedades = new Properties();
            try (FileInputStream fis = new FileInputStream("sentinel.properties")) {
                // Cargamos el fichero
                propiedades.load(fis);
                // Extraemos la variable "id_grupo"
                this.nombreGrupo = propiedades.getProperty("id_grupo");
                
                System.out.println("[INFO] Propiedades leídas. Registrado al grupo: " + this.nombreGrupo);
                
                // Lo guardamos AppData para no depender mas de este archivo
                gestorArchivos.guardarGrupo(this.nombreGrupo);
                
            } catch (Exception e) {
                // Si el usuario borró el archivo properties o no se descargó bien
                System.out.println("[ERROR] No se ha encontrado el archivo sentinel.properties junto al ejecutable.");
                this.nombreGrupo = "GRUPO_DESCONOCIDO";
            }
        }
    }

    
    // Este metodo lo llama el hilo del main
    public void ejecutarCicloDiario() {
        Calendar ahora = Calendar.getInstance();
        int diaActual = ahora.get(Calendar.DAY_OF_YEAR);
        int horaActual = ahora.get(Calendar.HOUR_OF_DAY);

        // Si las listas de reporte estan vacias, actualizamos la etiqueta del dia a hoy
        if (procesosDelDia.isEmpty() && puertosDelDia.isEmpty()) {
            diaDeLosDatos[0] = diaActual;
        }

        System.out.println("\n[INFO] Escaneando el sistema...");
        realizarEscaneo();
        
        boolean tocaSubir = true;
        
        /* Variante de funcionamiento para que se haga una UNICA subida y a hora determinada, no upset actual
        // Subida normal a las 21h
        if (horaActual >= 21 && ultimoDiaSubido != diaActual) {
            tocaSubir = true;
        }
        */
        // Peligro inminente
        if (procesosDelDia.size() >= 500 || puertosDelDia.size() >= 50) {
            System.out.println("[ALERTA] Límite masivo alcanzado. Forzando subida de emergencia.");
            tocaSubir = true;
        }

        // Subida de datos atrasados. Se queda activa siempre
        if (diaDeLosDatos[0] != -1 && diaDeLosDatos[0] != diaActual) {
            if (!procesosDelDia.isEmpty() || !puertosDelDia.isEmpty()) tocaSubir = true;
        }
        
        
        if (tocaSubir) {
            boolean exito = intentarSubidaMongo();
            if (exito) {
                ultimoDiaSubido = diaDeLosDatos[0];
                diaDeLosDatos[0] = diaActual; 
                // Escribimos en el disco duro que hoy esta cumplido
                gestorArchivos.guardarUltimoDiaSubido(ultimoDiaSubido); 
            }
        }
    }

    
    private void realizarEscaneo() {
        try {
            ConexionMongo conexion = new ConexionMongo();
            Document docWin = conexion.descargarConfiguracion("windows_conf", null);
            Document docAdmin = conexion.descargarConfiguracion("admin_conf", this.nombreGrupo);

            List<String> pWin = new ArrayList<>();
            List<String> pAdmin = new ArrayList<>();
            List<Integer> portWin = new ArrayList<>();
            List<Integer> portAdmin = new ArrayList<>();

            // Si Mongo ha devuelto datos
            if (docWin != null && docAdmin != null) {
                pWin = docWin.getList("procesos_permitidos", String.class);
                portWin = docWin.getList("puertos_permitidos", Integer.class);
                pAdmin = docAdmin.getList("procesos_permitidos", String.class);
                portAdmin = docAdmin.getList("puertos_permitidos", Integer.class);
                
                // Si hay red, actualizamos la copia de seguridad local
                gestorArchivos.guardarConfiguracionLocal(pWin, portWin, pAdmin, portAdmin);
            } else {
            	// Internet KO o Mongo KO
                System.out.println("[AVISO] Sin red. Cargando políticas locales...");
                File archivoConfig = new File(System.getenv("APPDATA") + File.separator + "SentinelAgent" + File.separator + "config_backup.dat");
                
                if (archivoConfig.exists()) {
                	// Lee el backup que hicimos en el paso anterior
                    ObjectInputStream ois = new ObjectInputStream(new FileInputStream(archivoConfig));
                    pWin = (List<String>) ois.readObject();
                    portWin = (List<Integer>) ois.readObject();
                    pAdmin = (List<String>) ois.readObject();
                    portAdmin = (List<Integer>) ois.readObject();
                    
                    ois.close();
                } else {
                    // Politicas extremas, no hay ni BBDD ni Backup
                    pWin.add("svchost.exe");
                    pWin.add("explorer.exe");
                    portWin.add(135);
                    portWin.add(445);
                }
            }

            ScannerProcesos scProc = new ScannerProcesos();
            ScannerPuertos scPort = new ScannerPuertos();

            // Compara lo que hay en el PC contra las listas que hemos bajado
            List<String> encontradosProc = scProc.obtenerProcesosSospechosos(pWin, pAdmin);
            for (int i = 0; i < encontradosProc.size(); i++) {
                String proceso = encontradosProc.get(i);
                // Si es un reporte nuevo, lo guarda en la RAM
                if (procesosDelDia.size() < 150 && !procesosDelDia.contains(proceso)) {
                	procesosDelDia.add(proceso);
                }
            }
            
            // Mismo con los puertos
            List<Integer> encontradosPort = scPort.obtenerPuertosSospechosos(portWin, portAdmin);
            for (int i = 0; i < encontradosPort.size(); i++) {
                Integer puerto = encontradosPort.get(i);
                if (puertosDelDia.size() < 50 && !puertosDelDia.contains(puerto)) {
                	puertosDelDia.add(puerto);
                }
            }
            
            // Guardamos el estado actual por si falla
            gestorArchivos.guardarAlertas(procesosDelDia, puertosDelDia, diaDeLosDatos[0]);
            
        } catch (Exception e) {
            System.out.println("Error en la lectura del PC.");
        }
    }
    
    
    private String obtenerMac() {
        try {
            InetAddress ip = InetAddress.getLocalHost(); // IP
            NetworkInterface network = NetworkInterface.getByInetAddress(ip); // Tarjeta de red
            byte[] mac = network.getHardwareAddress(); // MAC en byte

            if (mac == null) {
            	return "00-00-00-00-00-00";
            }

            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < mac.length; i++) {
                sb.append(String.format("%02X%s", mac[i], (i < mac.length - 1) ? "-" : ""));
            }
            return sb.toString();
            
        } catch (Exception e) {
            return "MAC-NO-DISPONIBLE";
        }
    }
    
    public static String getNombreCompletoUsuarioActivo() {
        String usuarioInterno = "";

        // Quien esta usando el PC buscando el proceso del escritorio
        Optional<String> usuarioExplorer = ProcessHandle.allProcesses()
                .filter(p -> p.info().command().orElse("").toLowerCase().endsWith("explorer.exe"))
                .findFirst()
                .flatMap(p -> p.info().user());

        if (usuarioExplorer.isPresent()) {
            String usuarioCompleto = usuarioExplorer.get(); // Suele venir como "DESKTOP\\usuario"
            if (usuarioCompleto.contains("\\")) {
                usuarioInterno = usuarioCompleto.substring(usuarioCompleto.indexOf("\\") + 1); // Nos quedamos con el usuario
            } else {
                usuarioInterno = usuarioCompleto;
            }
        } else {
            // Si por algun motivo no hay interfaz grafica, el del sistema (En ajustes)
            usuarioInterno = System.getProperty("user.name");
        }

        // Si el programa lo ejecuta NSSM y no hay nadie logueado, devolvera SYSTEM
        if (usuarioInterno.equalsIgnoreCase("SYSTEM")) {
            return "Sistema (Sin sesión activa)";
        }

        // Preguntar a Windows el "Display Name" de ese usuario interno
        try {
            // Hacemos una consulta a la clave exacta donde Windows guarda el nombre de la pantalla de bloqueo
        	ProcessBuilder builder = new ProcessBuilder("cmd.exe", "/c", 
                    "chcp 65001 > nul && reg query \"HKLM\\SOFTWARE\\Microsoft\\Windows\\CurrentVersion\\Authentication\\LogonUI\" /v LastLoggedOnDisplayName");
            
            Process process = builder.start();
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream(), java.nio.charset.StandardCharsets.UTF_8));
            
            String linea;
            String nombreCompleto = "";
            
            while ((linea = reader.readLine()) != null) {
                // Windows devuelve una línea con este formato:
                // "    LastLoggedOnDisplayName    REG_SZ    user"
                if (linea.contains("LastLoggedOnDisplayName")) {
                    // Partimos la frase usando "REG_SZ" como tijera
                    String[] partes = linea.split("REG_SZ");
                    if (partes.length > 1) {
                        nombreCompleto = partes[1].trim(); // Cogemos la parte derecha y le quitamos los espacios
                    }
                    break;
                }
            }

            // Si por algun motivo falla, devuelve el nombre del sistema de archivos de windows
            return nombreCompleto.isEmpty() ? usuarioInterno : nombreCompleto;

        } catch (Exception e) {
            System.err.println("Error al consultar el registro: " + e.getMessage());
            return usuarioInterno;
        }
    }

    
    private boolean intentarSubidaMongo() {
    	// Metodo para sacar el nombre de la sesion
    	String nombrePc = getNombreCompletoUsuarioActivo();
        String macPc = obtenerMac();
        
        //"SEGURO" o "ALERTA"
        String estado = "";
        if (procesosDelDia.isEmpty() && puertosDelDia.isEmpty()){
        	estado = "SEGURO";
        } else {
        	estado = "ALERTA";
        }

        // Transformamos el dia de la memoria en una fecha
        int diaParaSubir;
        if (diaDeLosDatos[0] != -1) { // Si hay datos en memoria, usamos ese dia
            diaParaSubir = diaDeLosDatos[0];
        } else { // Si es la primera vez o está vacío, usamos el día de hoy
            diaParaSubir = LocalDate.now().getDayOfYear();
        }
        
        int anioActual = LocalDate.now().getYear();
        String fechaDia = LocalDate.ofYearDay(anioActual, diaParaSubir).toString();

        
        ConexionMongo conexion = new ConexionMongo();
        boolean exito = conexion.subirReporte(this.nombreGrupo, macPc, nombrePc, estado, fechaDia, procesosDelDia, puertosDelDia);

        // Si la BBDD lo recibe bien
        if (exito) {
            procesosDelDia.clear(); // Limpia la RAM
            puertosDelDia.clear();
            // Borra el archivo temporal del disco duro para no duplicar datos
            gestorArchivos.borrarAlertas();
            System.out.println("[OK] Subida correcta. Memoria limpia.");
            return true;
        } else {
            System.out.println("[ERROR] No se pudo subir el reporte. Se intentará más tarde.");
            return false;
        }
    }
}