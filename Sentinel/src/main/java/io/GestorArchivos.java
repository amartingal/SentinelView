package io;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

public class GestorArchivos {
	
    private final String RUTA_OCULTA = System.getenv("APPDATA") + File.separator + "SentinelAgent" + File.separator;
    private final String CACHE_DATOS = RUTA_OCULTA + "cache_datos.dat";
    private final String CACHE_CONFIG = RUTA_OCULTA + "config_backup.dat";
    private final String MEMORIA_GRUPO = RUTA_OCULTA + "grupo.dat";
    private final String MEMORIA_CALENDARIO = RUTA_OCULTA + "calendario.dat";

    
    public GestorArchivos() {
        // Al instanciar, comprobamos si la carpeta secreta existe. Si no, la creamos y la ocultamos.
        File carpeta = new File(System.getenv("APPDATA") + File.separator + "SentinelAgent");
        if (!carpeta.exists()) {
            carpeta.mkdirs();
            try {
                // Le forzamos a Windows el atributo de "Carpeta Oculta"
                Path ruta = Paths.get(carpeta.getAbsolutePath());
                Files.setAttribute(ruta, "dos:hidden", true);
            } catch (Exception e) {}
        }
    }
    
    
    public void guardarGrupo(String grupo) {
        try (FileOutputStream fos = new FileOutputStream(MEMORIA_GRUPO);
             DataOutputStream dos = new DataOutputStream(fos)) {
            dos.writeUTF(grupo);
        } catch (Exception e) {}
    }

    public String cargarGrupo() {
        File archivo = new File(MEMORIA_GRUPO);
        
        if (archivo.exists()) {
            try (FileInputStream fis = new FileInputStream(archivo);
                 DataInputStream dis = new DataInputStream(fis)) {
                return dis.readUTF();
            } catch (Exception e) {}
        } else {System.out.println("[ERROR] No se ha encontrado grupo");}
        
        return null;
    }

    
    public void guardarUltimoDiaSubido(int dia) {
        try (FileOutputStream fos = new FileOutputStream(MEMORIA_CALENDARIO);
             DataOutputStream dos = new DataOutputStream(fos)) {
            dos.writeInt(dia);
        } catch (Exception e) {}
    }

    public int cargarUltimoDiaSubido() {
        File archivo = new File(MEMORIA_CALENDARIO);
        
        if (archivo.exists()) {
            try (FileInputStream fis = new FileInputStream(archivo);
                 DataInputStream dis = new DataInputStream(fis)) {
                return dis.readInt();
            } catch (Exception e) {}
        }
        return -1;
    }

    
    // Memoria RAM a Disco local
    public void cargarAlertas(List<String> procesos, List<Integer> puertos, int[] diaGuardado) {
        File archivo = new File(CACHE_DATOS);
        
        if (archivo.exists()) {
            try (FileInputStream fis = new FileInputStream(archivo); ObjectInputStream ois = new ObjectInputStream(fis)) {
                procesos.addAll((List<String>) ois.readObject());
                puertos.addAll((List<Integer>) ois.readObject());
                diaGuardado[0] = ois.readInt(); 
                System.out.println("[INFO] Alertas antiguas recuperadas del disco.");
            } catch (Exception e) {}
        }
    }

    public void guardarAlertas(List<String> procesos, List<Integer> puertos, int dia) {
        try (FileOutputStream fos = new FileOutputStream(CACHE_DATOS); ObjectOutputStream oos = new ObjectOutputStream(fos)) {
            oos.writeObject(procesos);
            oos.writeObject(puertos);
            oos.writeInt(dia);
        } catch (Exception e) {}
    }

    public void borrarAlertas() {
        File archivo = new File(CACHE_DATOS);
        
        if (archivo.exists()) {
        	archivo.delete();
        }
    }

    
    // Evento de emergencia
    public void guardarConfiguracionLocal(List<String> pWin, List<Integer> portWin, List<String> pAdmin, List<Integer> portAdmin) {
        try (FileOutputStream fos = new FileOutputStream(CACHE_CONFIG); ObjectOutputStream oos = new ObjectOutputStream(fos)) {
            oos.writeObject(pWin);
            oos.writeObject(portWin);
            oos.writeObject(pAdmin);
            oos.writeObject(portAdmin);
        } catch (Exception e) {}
    }
}