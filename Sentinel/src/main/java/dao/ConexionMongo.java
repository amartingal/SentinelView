package dao;

import java.util.Base64;
import java.util.List;

import org.bson.Document;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.UpdateOptions;

public class ConexionMongo {
	
	private final String URI_SECRETA = "bW9uZ29kYitzcnY6Ly9hZG1pbjoxMjM0QGNsdXN0ZXIwLndzanl2bGEubW9uZ29kYi5uZXQvP2FwcE5hbWU9Q2x1c3RlcjA=";
	private final String URI_MONGO = new String(Base64.getDecoder().decode(URI_SECRETA));
	
	// Bajar las listas blancas de procesos y puertos
    public Document descargarConfiguracion(String tipoConf, String grupo) {
        try {
        	//Crea el cliente que establece la conexion con el servidor de Mongo
            MongoClient clienteMongo = MongoClients.create(URI_MONGO);
            // Selecciona la base de datos
            MongoDatabase baseDatos = clienteMongo.getDatabase("SentinelDB");
            // Selecciona la coleccion
            MongoCollection<Document> coleccion = baseDatos.getCollection("Configuraciones");

            // Filtro (=WHERE...)
            Document filtro = new Document("tipo", tipoConf);
            // Si nos pasan un grupo, añadimos la condición al WHERE
            if (grupo != null) {
                filtro.append("grupo", grupo);
            }
            Document configuracion = coleccion.find(filtro).first();
            
            clienteMongo.close();
            return configuracion;
        } catch (Exception e) {
            return null; 
        }
    }

    // Sube el reporte con los resultados obtenidos
    public boolean subirReporte(String grupo, String mac, String equipoId, String estado, String fechaDia, List<String> procesos, List<Integer> puertos) {
        System.out.println("--> Conectando con MongoDB Atlas...");
        
        try (MongoClient clienteMongo = MongoClients.create(URI_MONGO)) {
            MongoDatabase baseDatos = clienteMongo.getDatabase("SentinelDB");
            MongoCollection<Document> coleccion = baseDatos.getCollection("Reportes");

            // Buscamos si ya existe el reporte de este PC para el dia en cuestion
            Document filtro = new Document("mac_address", mac)
                    .append("fecha_escaneo", fechaDia);
            
            // Se construye BSON (JSON de Mongo)
            Document reporte = new Document("$set", new Document("grupo", grupo)
                    .append("mac_address", mac)
                    .append("equipo_id", equipoId)
                    .append("fecha_escaneo", fechaDia)
                    .append("estado", estado))
            	.append("$addToSet", new Document("procesos_sospechosos", new Document("$each", procesos))
                    .append("puertos_sospechosos", new Document("$each", puertos)));

            //Si no existe, lo crea con todo lo del $set. Si existe, no duplica, solo inyecta en las listas
            UpdateOptions opciones = new UpdateOptions().upsert(true);
            coleccion.updateOne(filtro, reporte, opciones);
            
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}