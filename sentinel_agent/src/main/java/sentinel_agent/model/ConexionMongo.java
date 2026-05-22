package sentinel_agent.model;

import java.util.Base64;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoDatabase;

public class ConexionMongo {
    private static final String URI_SECRETA = "bW9uZ29kYitzcnY6Ly9hZG1pbjoxMjM0QGNsdXN0ZXIwLndzanl2bGEubW9uZ29kYi5uZXQvP2FwcE5hbWU9Q2x1c3RlcjA=";
    private static final String URI_MONGO = new String(Base64.getDecoder().decode(URI_SECRETA));
    private static final String DATABASE_NAME = "SentinelDB";
    private static MongoClient clienteMongo = null;

    // Patron Singleton para la conexion de MongoDB
    public static MongoDatabase getDatabase() {
        if (clienteMongo == null) {
            synchronized (ConexionMongo.class) {
                if (clienteMongo == null) {
                    System.out.println("--> Conectando con MongoDB Atlas...");
                    clienteMongo = MongoClients.create(URI_MONGO);
                }
            }
        }
        return clienteMongo.getDatabase(DATABASE_NAME);
    }

    // Metodo que cierra la conexion con MongoDB
    public static void cerrarConexion() {
        if (clienteMongo != null) {
            clienteMongo.close();
            clienteMongo = null;
            System.out.println("--> Conexion con MongoDB Atlas cerrada.");
        }
    }
}
