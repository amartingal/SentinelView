package main;

import java.util.logging.Level;
import java.util.logging.Logger;

import controller.ControladorAgente;

public class _main {
	
public static void main(String[] args) {
		
		Logger.getLogger("org.mongodb.driver").setLevel(Level.SEVERE);
		System.out.println("--- AGENTE SENTINEL ACTIVO ---");
		
		// Instanciamos el Agente y preparamos los datos
		ControladorAgente controlador = new ControladorAgente();
		controlador.arrancarAgente();
		
		// Hilo infinito
		Thread hiloAgente = new Thread(new Runnable() {
			@Override
			public void run() {
				while (true) {
				    try {
				        controlador.ejecutarCicloDiario();
				    } catch (Exception e) {
				        System.out.println("[ERROR CRITICO] " + e.getMessage());
				    } finally {
				        try {
				            System.out.println("[INFO] Hilo en espera. Próximo ciclo en 2 minutos...");
				            Thread.sleep(60000); //2min
				        } catch (Exception ex) {}
				    }
				}
			}
		});
		
		hiloAgente.start();
	}
}