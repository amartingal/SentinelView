package scanner;

import java.io.File;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class ScannerProcesos {

public List<String> obtenerProcesosSospechosos(List<String> permitidosWin, List<String> permitidosAdmin) {
        
        List<String> procesosRaros = new ArrayList<>();
        // Para leer la RAM
        Iterator<ProcessHandle> listaProcesos = ProcessHandle.allProcesses().iterator();

        
        while (listaProcesos.hasNext()) {
            ProcessHandle proceso = listaProcesos.next();
            // Se intenta sacar la ruta del proceso
            String rutaAbsoluta = proceso.info().command().orElse("");

            if (!rutaAbsoluta.equals("")) {
            	// Nos quedamos con el nombre, ruta fuera
                String nombreProceso = new File(rutaAbsoluta).getName();
                String nombreMinusculas = nombreProceso.toLowerCase();

                boolean esLegal = false;

                // Comprobamos si esta en la lista de Windows
                if (permitidosWin != null) {
                    for (int i = 0; i < permitidosWin.size(); i++) {
                        if (permitidosWin.get(i).equalsIgnoreCase(nombreMinusculas)) {
                            esLegal = true;
                        }
                    }
                }

                // Comprobamos si esta en la lista del Administrador
                if (permitidosAdmin != null) {
                    for (int i = 0; i < permitidosAdmin.size(); i++) {
                        if (permitidosAdmin.get(i).equalsIgnoreCase(nombreMinusculas)) {
                            esLegal = true;
                        }
                    }
                }

                // Si despues de mirar las dos listas no es legal, salta aletra
                if (esLegal == false) {
                    // Se compruebasi esta en la lista
                    if (!procesosRaros.contains(nombreProceso)) {
                        procesosRaros.add(nombreProceso);
                    }
                }
            }
        }
        return procesosRaros;
    }
}