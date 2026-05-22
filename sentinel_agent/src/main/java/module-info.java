module sentinel_agent {
    requires javafx.controls;
    requires javafx.fxml;
    requires org.mongodb.driver.sync.client;
    requires org.mongodb.driver.core;
    requires org.mongodb.bson;
    requires jbcrypt;
    requires java.mail;
    requires java.desktop;
    requires java.xml;
    requires com.github.librepdf.openpdf;
    requires java.net.http;

    // Abrir paquetes de controladores a FXML para la inyección de dependencias
    opens sentinel_agent.controller to javafx.fxml;

    // Abrir el paquete de vista si hay FXMLs que dependen de él
    opens sentinel_agent.view to javafx.fxml;

    // Exportar paquetes para que JavaFX pueda acceder a la clase principal App
    exports sentinel_agent.view;
    exports sentinel_agent.controller;
    exports sentinel_agent.model;
    exports sentinel_agent.utils;
}
