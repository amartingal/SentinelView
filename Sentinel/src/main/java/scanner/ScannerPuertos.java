package scanner;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

public class ScannerPuertos {

public List<Integer> obtenerPuertosSospechosos(List<Integer> permitidosWin, List<Integer> permitidosAdmin) {
        
        List<Integer> puertosRaros = new ArrayList<>();

        try {
        	// Abre una consola CMD invisible de Windows y ejecuta el comando 'netstat -ano' (lista todas las conexiones de red activas)
            Process proceso = Runtime.getRuntime().exec("netstat -ano");
            BufferedReader lector = new BufferedReader(new InputStreamReader(proceso.getInputStream()));
            String linea;

            
            while ((linea = lector.readLine()) != null) {
                
                // Filtramos solo las conexiones TCP que esten abiertas
                if (linea.contains("TCP") && (linea.contains("LISTENING") || linea.contains("ESCUCHANDO"))) {
                    
                    // TCP    0.0.0.0:135    0.0.0.0:0    LISTENING   992
                    // La limpiamos de espacios extra
                    String[] partes = linea.trim().split("\\s+");
                    
                    if (partes.length >= 2) {
                        String direccionLocal = partes[1]; // Coge algo como "0.0.0.0:135"
                        
                        // Separamos la IP del puerto usando los dos puntos
                        String[] ipYPuerto = direccionLocal.split(":");
                        // El puerto siempre es lo que hay detras de los últimos dos puntos
                        String puertoStr = ipYPuerto[ipYPuerto.length - 1]; 
                        
                        try {
                            int puertoDetectado = Integer.parseInt(puertoStr);
                            boolean esLegal = false;
                            
                            // Comprobamos si el puerto detectado esta en las listas blancas
                            if (permitidosWin != null && permitidosWin.contains(puertoDetectado)) {
                            	esLegal = true;
                            }
                            
                            if (permitidosAdmin != null && permitidosAdmin.contains(puertoDetectado)) {
                            	esLegal = true;
                            }
                            
                            
                            // Se comprueba si es ilegal y no estaba ya en la lista
                            if (!esLegal && !puertosRaros.contains(puertoDetectado)) {
                                puertosRaros.add(puertoDetectado);
                            }
                        } catch (Exception e) {}
                    }
                }
            }
        } catch (Exception e) {
            System.out.println("Error al leer netstat: " + e.getMessage());
        }
        
        return puertosRaros;
    }
}