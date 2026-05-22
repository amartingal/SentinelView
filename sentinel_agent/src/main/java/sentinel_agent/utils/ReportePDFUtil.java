package sentinel_agent.utils;

import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import java.awt.Color;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

public class ReportePDFUtil {

    // Metodo que genera el PDF con los reportes
    public static boolean generarPDFReportes(List<org.bson.Document> reportes, String usuario, String fecha,
            File destino) {
        // Crear documento con medidas
        Document principal = new Document(PageSize.A4.rotate(), 30, 30, 30, 30);
        // Metodo que crea el PDF
        try {
            PdfWriter.getInstance(principal, new FileOutputStream(destino));
            principal.open();

            // Titulo del PDF
            Font fontTitulo = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 20, Font.NORMAL);
            Paragraph titulo = new Paragraph("Reporte Detallado de Seguridad - SentinelAgent", fontTitulo);
            titulo.setAlignment(Element.ALIGN_CENTER);
            principal.add(titulo);

            // Subtitulo del PDF
            Font fontSubtitulo = FontFactory.getFont(FontFactory.HELVETICA, 12, Font.NORMAL);
            Paragraph meta = new Paragraph();
            meta.setSpacingBefore(10f);
            meta.add(new Phrase("Usuario/Grupo: " + usuario + "\n", fontSubtitulo));
            meta.add(new Phrase("Fecha de los datos: " + fecha + "\n", fontSubtitulo));
            meta.add(new Phrase(
                    "Fecha de generación: " + java.time.LocalDateTime.now().toString().replace("T", " ") + "\n",
                    fontSubtitulo));
            meta.add(new Phrase("Total de equipos analizados: " + reportes.size(), fontSubtitulo));
            principal.add(meta);

            // Tabla con los reportes
            float[] columnWidths = { 15, 15, 10, 30, 30 };
            PdfPTable tabla = new PdfPTable(columnWidths);
            tabla.setWidthPercentage(100);
            tabla.setSpacingBefore(15f);
            tabla.setSpacingAfter(10f);
            tabla.setSplitLate(false);

            // Cabeceras de la tabla
            Font fontCabecera = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 11, Font.NORMAL);
            String[] cabeceras = { "Equipo ID", "Dirección MAC", "Estado", "Procesos Sospechosos",
                    "Puertos Sospechosos" };

            for (String cabecera : cabeceras) {
                PdfPCell cell = new PdfPCell(new Phrase(cabecera, fontCabecera));
                cell.setBackgroundColor(Color.LIGHT_GRAY);
                cell.setHorizontalAlignment(Element.ALIGN_CENTER);
                cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
                cell.setPadding(8);
                tabla.addCell(cell);
            }
            tabla.setHeaderRows(1);

            // Datos de la tabla
            Font fontDatos = FontFactory.getFont(FontFactory.HELVETICA, 9, Font.NORMAL);
            Font fontDetalle = FontFactory.getFont(FontFactory.HELVETICA, 8, Font.ITALIC);

            // Recorrer los reportes
            for (org.bson.Document doc : reportes) {
                tabla.addCell(
                        new Phrase(doc.getString("equipo_id") != null ? doc.getString("equipo_id") : "N/A", fontDatos));
                tabla.addCell(new Phrase(doc.getString("mac_address") != null ? doc.getString("mac_address") : "N/A",
                        fontDatos));

                // Estado del equipo
                String estado = doc.getString("estado") != null ? doc.getString("estado") : "Desconocido";
                PdfPCell cellEstado = new PdfPCell(new Phrase(estado, fontDatos));
                cellEstado.setHorizontalAlignment(Element.ALIGN_CENTER);
                if (estado.equalsIgnoreCase("alerta") || estado.equalsIgnoreCase("critico")) {
                    cellEstado.setBackgroundColor(new Color(255, 200, 200));
                }
                tabla.addCell(cellEstado);

                // Procesos sospechosos
                List<?> procs = doc.getList("procesos_sospechosos", Object.class);
                String listadoProcesos = "-";
                if (procs != null && !procs.isEmpty()) {
                    listadoProcesos = procs.stream()
                            .map(p -> {
                                if (p instanceof org.bson.Document)
                                    return ((org.bson.Document) p).getString("nombre");
                                return p.toString();
                            })
                            .filter(n -> n != null && !n.isEmpty())
                            .collect(Collectors.joining("\n• ", "• ", ""));
                }
                // Añadir procesos sospechosos a la tabla
                PdfPCell cellProcs = new PdfPCell(new Phrase(listadoProcesos, fontDetalle));
                cellProcs.setPadding(5);
                tabla.addCell(cellProcs);

                // Puertos sospechosos
                List<?> puertos = doc.getList("puertos_sospechosos", Object.class);
                String listadoPuertos = "-";
                if (puertos != null && !puertos.isEmpty()) {
                    listadoPuertos = puertos.stream()
                            .map(obj -> {
                                if (obj instanceof org.bson.Document) {
                                    Integer num = ((org.bson.Document) obj).getInteger("puerto");
                                    return num != null ? String.valueOf(num) : "Desconocido";
                                }
                                return obj.toString();
                            })
                            .collect(Collectors.joining(", "));
                }
                // Añadir puertos sospechosos a la tabla
                PdfPCell cellPuertos = new PdfPCell(new Phrase(listadoPuertos, fontDetalle));
                cellPuertos.setPadding(5);
                tabla.addCell(cellPuertos);
            }

            // Añadir tabla al documento
            principal.add(tabla);
            principal.add(new Paragraph(" "));

            // Pie de página
            Paragraph pie = new Paragraph("SentinelAgent - Informe de seguridad generado automáticamente.",
                    FontFactory.getFont(FontFactory.HELVETICA_OBLIQUE, 8, Font.NORMAL));
            pie.setAlignment(Element.ALIGN_RIGHT);
            principal.add(pie);
            principal.close();
            return true;

        } catch (DocumentException | IOException e) {
            // Muestra en consola el error que impidio escribir el archivo
            System.err.println("Error al generar PDF detallado: " + e.getMessage());
            // Imprime la traza completa de error
            e.printStackTrace();
            // Retorna falso ante excepciones
            return false;
        }
    }
}
