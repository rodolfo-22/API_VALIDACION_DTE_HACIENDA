package sv.example.factura.service;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.apache.pdfbox.pdmodel.graphics.image.LosslessFactory;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.qrcode.QRCodeWriter;

import sv.example.factura.dto.FacturaRequest;
import sv.example.factura.dto.FacturaRequest.Emisor;
import sv.example.factura.dto.FacturaRequest.Receptor;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;

public class FacturaPdfService {

    public byte[] generateFacturaPdf(FacturaRequest req) throws IOException, WriterException {
        try (PDDocument doc = new PDDocument()) {
            // Carta: 612 x 792 puntos
            PDPage page = new PDPage(PDRectangle.LETTER);
            doc.addPage(page);

            PDRectangle box = page.getMediaBox();
            float ml = 36f;                         // margen izquierdo
            float mr = 36f;                         // margen derecho
            float mt = 32f;                         // margen superior
            float yTop = box.getUpperRightY() - mt; // y inicial
            float contentWidth = box.getWidth() - ml - mr;

            // Fuentes (PDFBox 3)
            PDType1Font fontBold = new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD);
            PDType1Font fontReg  = new PDType1Font(Standard14Fonts.FontName.HELVETICA);
            PDType1Font monoBold = new PDType1Font(Standard14Fonts.FontName.COURIER_BOLD);
            PDType1Font monoReg  = new PDType1Font(Standard14Fonts.FontName.COURIER);

            Emisor em = req.emisor != null ? req.emisor : new Emisor();
            Receptor rc = req.receptor != null ? req.receptor : new Receptor();

            try (PDPageContentStream cs = new PDPageContentStream(doc, page)) {

                // ====== Encabezado Superior ======
                // Reserva de logo (solo rectángulo vacío)
                float logoW = 150f, logoH = 40f;
                drawRectStroke(cs, ml, yTop - logoH + 8f, logoW, logoH, new Color(0,0,0)); // placeholder
// Título centrado (un poco más pequeño)
                cs.setFont(fontBold, 14); // antes 14
                drawCentered(cs, ml + logoW + 10f, box.getUpperRightX() - mr, yTop + 6f, "DOCUMENTO TRIBUTARIO ELECTRÓNICO");
                cs.setFont(fontReg, 13);  // antes 12
                drawCentered(cs, ml + logoW + 10f, box.getUpperRightX() - mr, yTop - 8f, "FACTURA");

// Geometría de la banda (debajo del título)
                 contentWidth = box.getWidth() - ml - mr;
                float bandTopY    = yTop - 60f;   // más abajo para no tocar el título
                float bandHeight  = 56f;
                float bandBottomY = bandTopY - bandHeight;
                float midX        = ml + contentWidth/2f;

// Dimensiones/posición del QR (centrado vertical en la banda, a la derecha)
                final float qrSize = 85f;   // tamaño del QR que cabe en la banda
                final float qrPad  = 4f;    // margen entre QR y líneas
                float qrXpos = box.getUpperRightX() - mr - qrSize;
                float qrYpos = bandBottomY + (bandHeight - qrSize) / 2f;

// Líneas de la banda: llegan hasta antes del QR (no lo atraviesan)
                float rightLineStop = qrXpos - qrPad; // detén la línea un poco antes del QR
                drawLine(cs, ml, bandTopY,    rightLineStop, bandTopY,    1.2f, Color.BLACK);
                drawLine(cs, ml, bandBottomY, rightLineStop, bandBottomY, 1.2f, Color.BLACK);

// Separador vertical al centro (queda antes del QR)
                drawLine(cs, midX, bandTopY, midX, bandBottomY, 2f, Color.BLACK);

// Textos dentro de la banda (fuente más pequeña para que quepan)
                cs.setFont(fontReg, 7f);
                float leftX  = ml + 8f;
                float rightX = midX + 8f;
                float yL = bandTopY - 16f;
                drawText(cs, leftX, yL,   "Código de Generación: " + safe(req.codigoGeneracion)); yL -= 12f;
                drawText(cs, leftX, yL,   "Número de Control: " + safe(req.numeroControl));       yL -= 12f;
                drawText(cs, leftX, yL,   "Sello de recepción: (se genera en respuesta ministerio)");

                float yR = bandTopY - 16f;
                drawText(cs, rightX, yR,  "Modelo de Facturación: Modelo Facturación Previo"); yR -= 12f;
                drawText(cs, rightX, yR,  "Tipo de Transmisión: Transmisión normal");          yR -= 12f;
                drawText(cs, rightX, yR,  "Fecha y Hora de Generación: " + safe(req.fechaHora));

// (¡AL FINAL!) dibuja el QR para que no tape textos ni líneas
                String qrText = "CG: " + safe(req.codigoGeneracion) + " | NC: " + safe(req.numeroControl) + " | FH: " + safe(req.fechaHora);
                BufferedImage qrImg = qr(qrText, (int)qrSize, (int)qrSize);
                PDImageXObject qrX = LosslessFactory.createFromImage(doc, qrImg);
                cs.drawImage(qrX, qrXpos, qrYpos, qrSize, qrSize);




                // ====== Secciones EMISOR / RECEPTOR en dos columnas ======
                float sectionTop = bandBottomY - 16f;
                float sectionW = contentWidth;
                float headerH = 16f;
                // barra encabezado gris
                drawHeaderBar(cs, ml, sectionTop, sectionW, headerH, new Color(235,235,235));
                cs.setFont(fontBold, 9.5f);
                drawText(cs, ml + 8f, sectionTop + 4f, "EMISOR");
                drawText(cs, ml + sectionW/2f + 8f, sectionTop + 4f, "RECEPTOR");

                // vertical separador
                float infoTopY = sectionTop - 16f;
                 midX = ml + contentWidth/2f; // Asegúrate de tener esta var disponible
                drawLine(cs, midX, infoTopY + 2f, midX, infoTopY - 100f, 2f, Color.BLACK); // línea larga (luego no importa)

                // ---- CONTENIDO EMISOR/RECEPTOR ----
                cs.setFont(fontReg, 7f);
                float colPad = 8f;
                float lx = ml + colPad;
                float rx2 = midX + colPad;
                float ly = infoTopY - 4f;

                // Emisor
                ly = drawField(cs, lx, ly, "Nombre o Razón Social:", em.nombreRazonSocial);
                ly = drawField(cs, lx, ly, "NIT:", em.nit);
                ly = drawField(cs, lx, ly, "NRC:", em.nrc);
                ly = drawField(cs, lx, ly, "Actividad Económica:", em.actividadEconomica);
                ly = drawField(cs, lx, ly, "Dirección:", em.direccion);
                ly = drawField(cs, lx, ly, "Número de Teléfono:", em.telefono);
                ly = drawField(cs, lx, ly, "Correo Electrónico:", em.correo);
                ly = drawField(cs, lx, ly, "Nombre Comercial:", em.nombreComercial);
                ly = drawField(cs, lx, ly, "Establecimiento:", em.establecimiento);
                float endLeft = ly; // <-- y final lado izquierdo

                // Receptor
                float ry2 = infoTopY - 4f;
                ry2 = drawField(cs, rx2, ry2, "Nombre o Razón social:", rc.nombreRazonSocial);
                ry2 = drawField(cs, rx2, ry2, "Tipo de Documento:", rc.tipoDocumento);
                ry2 = drawField(cs, rx2, ry2, "No. de doc. de Identificación:", rc.numeroDocumento);
                ry2 = drawField(cs, rx2, ry2, "Dirección:", rc.direccion);
                ry2 = drawField(cs, rx2, ry2, "Correo Electrónico:", rc.correo);
                ry2 = drawField(cs, rx2, ry2, "Número de teléfono:", rc.telefono);
                float endRight = ry2; // <-- y final lado derecho

                // AHORA sí: base del bloque calculada por el contenido más largo
                float infoBottomY = Math.min(endLeft, endRight) - 6f; // pequeño padding
                drawLine(cs, ml, infoBottomY, box.getUpperRightX() - mr, infoBottomY, 2f, Color.BLACK);

                // ====== Tabla de Items ======
                float gapToTable = 18f;
                float tblTop = infoBottomY - gapToTable;

                drawHeaderBar(cs, ml, tblTop, contentWidth, 18f, new Color(212,60,60));
                cs.setFont(fontBold, 8.2f);
                cs.setNonStrokingColor(Color.WHITE);
                drawText(cs, ml + 6f, tblTop + 5f,
                        String.format("%-4s %-8s %-5s %-8s %-46s %11s %11s %11s %11s",
                                "N°", "Código", "Cant.", "Unidad", "Descripción",
                                "Precio Unit.", "Desc. Item", "No Suj.", "Exentas", "Gravadas"));
                cs.setNonStrokingColor(Color.BLACK);
                cs.setFont(monoReg, 6.5f);

                float yi = tblTop - 18f - 6f;
                List<FacturaRequest.Item> items = req.items != null ? req.items : List.of();
                for (FacturaRequest.Item it : items) {
                    String desc = trunc(safe(it.descripcion), 45); // <-- antes 56
                    String linea = String.format("%-4d %-8s %-5d %-8s %-46s %11s %11s %11s %11s",
                            it.posicion,
                            safe(it.codigo),
                            it.cantidad,
                            safe(it.unidad),
                            desc,
                            safe(it.precioUnitario),
                            safe(def0(it.descuentoItem)),
                            safe(def0(it.ventasNoSujetas)),
                            safe(def0(it.ventasExentas)),
                            safe(def0(it.ventasGravadas))
                    );
                    drawText(cs, ml + 6f, yi, linea); // se mantiene pegado al margen izquierdo
                    yi -= 12f;
                }


                // ====== Panel inferior: Detalles (izq) y Suma Total (der) ======
                float leftW  = contentWidth * 0.60f;
                float rightW = contentWidth - leftW;
                float blockTop = yi - 12f;

// más alto para que todo quepa
                final float boxH = 140f;

// ---------- Caja izquierda ("Detalles") ----------
                drawBox(cs, ml, blockTop, leftW, boxH, new Color(212,60,60), 1.2f);
                cs.setFont(fontBold, 7.6f);
                cs.setNonStrokingColor(Color.WHITE);
                drawText(cs, ml + 8f, blockTop + 5f, "Detalles");
                cs.setNonStrokingColor(Color.BLACK);
                cs.setFont(fontReg, 6.3f);

                float dPadX = ml + 8f;
                float dY = blockTop - 16f;

// fila: total en letras (fondo gris claro como en maqueta)
                rowBg(cs, ml + 1f, dY + 2f, leftW - 2f, 12f, new Color(238,238,238));
                dY = drawField(cs, dPadX, dY, "Total en letras:", safe(req.totalEnLetras));

// fila: condición (gris)
                rowBg(cs, ml + 1f, dY + 2f, leftW - 2f, 12f, new Color(238,238,238));
                dY = drawField(cs, dPadX, dY, "Condicion de la Operacion:", "CONTADO - N1");

// separador sutil
                drawLine(cs, dPadX, dY - 2f, ml + leftW - 8f, dY - 2f, .6f, Color.LIGHT_GRAY);
                dY -= 10f;

// grid “Responsable / N° Documento”
                float detMid = ml + leftW/2f;
                rowBg(cs, ml + 1f, dY + 2f, leftW - 2f, 12f, new Color(238,238,238));
                cs.setFont(fontBold, 6.3f);
                drawText(cs, dPadX, dY, "Responsable por parte del EMISOR:");
                drawRight(cs, detMid + 6f, ml + leftW - 8f, dY, "N° Documento:");
                cs.setFont(fontReg, 6.3f);
                dY -= 14f;

                rowBg(cs, ml + 1f, dY + 2f, leftW - 2f, 12f, new Color(238,238,238));
                cs.setFont(fontBold, 6.3f);
                drawText(cs, dPadX, dY, "Responsable por parte del RECEPTOR:");
                drawRight(cs, detMid + 6f, ml + leftW - 8f, dY, "N° Documento:");
                cs.setFont(fontReg, 6.3f);
                dY -= 14f;

// línea vertical central del panel izquierdo
                drawLine(cs, detMid, blockTop - 16f, detMid, blockTop - boxH + 4f, 1f, Color.BLACK);

// resto de filas (alternando grises)
                rowBg(cs, ml + 1f, dY + 2f, leftW - 2f, 12f, new Color(238,238,238));
                dY = drawField(cs, dPadX, dY, "Numero de control Vidri:", safe(req.numeroControl).replace("DTE-01-",""));

                rowBg(cs, ml + 1f, dY + 2f, leftW - 2f, 12f, new Color(238,238,238));
                dY = drawField(cs, dPadX, dY, "Vendedor:", "0000S63");

                rowBg(cs, ml + 1f, dY + 2f, leftW - 2f, 12f, new Color(238,238,238));
                dY = drawField(cs, dPadX, dY, "Codigo interno:", "C00080925");

// borde base del panel (ya lo deja drawBox, no hace falta extra)

// ---------- Caja derecha ("Suma Total de Operaciones") ----------
                 rightX = ml + leftW;
                drawBox(cs, rightX, blockTop, rightW, boxH, new Color(212,60,60), 1.2f);
                cs.setFont(fontBold, 7.6f);
                cs.setNonStrokingColor(Color.WHITE);
                drawText(cs, rightX + 8f, blockTop + 5f, "Suma Total de Operaciones");
                cs.setNonStrokingColor(Color.BLACK);

// tipografía más chica para que nada se salga
                cs.setFont(fontReg, 6.3f);

                float ty = blockTop - 16f;
                final float rightLimit = rightX + rightW - 10f;

// columna separadora (valores a la derecha)
                float valueColX = rightX + rightW - 90f; // separador vertical como en tu ejemplo
                drawLine(cs, valueColX, blockTop - 16f, valueColX, blockTop - boxH + 4f, 1f, Color.BLACK);

// helper para filas con alternado gris
                ty = kvRow(cs, rightX + 8f, ty, rightLimit, "Ventas no Sujetas:", "$0.00", true);
                ty = kvRow(cs, rightX + 8f, ty, rightLimit, "Total Gravada:", "$" + safe(req.total), false);
                ty = kvRow(cs, rightX + 8f, ty, rightLimit, "Monto Global Descuento y Otros a Ventas Gravadas:", "$0.00", true);
                ty = kvRow(cs, rightX + 8f, ty, rightLimit, "Sumatoria de Ventas:", "$" + safe(req.total), false);
                ty = kvRow(cs, rightX + 8f, ty, rightLimit, "Sub-Total:", "$" + safe(req.total), true);
                ty = kvRow(cs, rightX + 8f, ty, rightLimit, "IVA Percibido:", "$0.00", false);
                ty = kvRow(cs, rightX + 8f, ty, rightLimit, "IVA Retenido:", "$0.00", true);
                ty = kvRow(cs, rightX + 8f, ty, rightLimit, "Retencion Renta:", "$0.00", false);
                ty = kvRow(cs, rightX + 8f, ty, rightLimit, "Monto Total de la Operacion:", "$" + safe(req.total), true);
                ty = kvRow(cs, rightX + 8f, ty, rightLimit, "Total otros Montos no Afectos:", "$0.00", false);

// Barra "Total a pagar" siempre dentro
                float payBarH = 14f;
                float bottomY = blockTop - boxH;
                float payY = Math.max(bottomY + 4f, ty - payBarH); // nunca por debajo del borde

                cs.setNonStrokingColor(new Color(212,60,60));
                cs.addRect(rightX, payY, rightW, payBarH);
                cs.fill();

                cs.setNonStrokingColor(Color.WHITE);
                cs.setFont(fontBold, 8.3f);
                drawRight(cs, rightX + 8f, rightX + rightW - 12f, payY + 3.5f, "Total a pagar: $" + safe(req.total));

            }

            try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
                doc.save(baos);
                return baos.toByteArray();
            }
        }
    }

    // ==== helpers ====
    private static void drawText(PDPageContentStream cs, float x, float y, String text) throws IOException {
        cs.beginText();
        cs.newLineAtOffset(x, y);
        cs.showText(text);
        cs.endText();
    }

    private static void drawCentered(PDPageContentStream cs, float xLeft, float xRight, float y, String text) throws IOException {
        float width = xRight - xLeft;
        float x = xLeft + width / 2f - (text.length() * 3.0f);
        drawText(cs, Math.max(x, xLeft), y, text);
    }

    private static void drawRight(PDPageContentStream cs, float xLeft, float xRight, float y, String text) throws IOException {
        float approx = text.length() * 3.5f;
        float x = Math.max(xLeft, xRight - approx);
        drawText(cs, x, y, text);
    }

    private static void drawHeaderBar(PDPageContentStream cs, float x, float y, float w, float h, Color color) throws IOException {
        cs.setNonStrokingColor(color);
        cs.addRect(x, y, w, h);
        cs.fill();
        cs.setNonStrokingColor(Color.BLACK);
    }

    private static void drawBox(PDPageContentStream cs, float x, float y, float w, float h, Color headerColor, float stroke) throws IOException {
        drawRectStroke(cs, x, y - h, w, h, Color.BLACK, 1f);
        cs.setNonStrokingColor(headerColor);
        cs.addRect(x, y - 16f, w, 16f);
        cs.fill();
        cs.setNonStrokingColor(Color.BLACK);
    }

    private static void drawRectStroke(PDPageContentStream cs, float x, float y, float w, float h, Color stroke) throws IOException {
        drawRectStroke(cs, x, y, w, h, stroke, 1f);
    }
    private static void drawRectStroke(PDPageContentStream cs, float x, float y, float w, float h, Color stroke, float lw) throws IOException {
        cs.setStrokingColor(stroke);
        cs.setLineWidth(lw);
        cs.addRect(x, y, w, h);
        cs.stroke();
        cs.setStrokingColor(Color.BLACK);
        cs.setLineWidth(1f);
    }

    private static void drawLine(PDPageContentStream cs, float x1, float y1, float x2, float y2, float lw, Color c) throws IOException {
        cs.setStrokingColor(c);
        cs.setLineWidth(lw);
        cs.moveTo(x1, y1);
        cs.lineTo(x2, y2);
        cs.stroke();
        cs.setStrokingColor(Color.BLACK);
        cs.setLineWidth(1f);
    }

    private static String safe(String s) { return s == null ? "" : s; }

    private static String trunc(String s, int len) {
        if (s == null) return "";
        return s.length() <= len ? s : s.substring(0, len - 3) + "...";
    }

    private static String def0(String s) { return (s == null || s.isBlank()) ? "0.0000" : s; }
    private static boolean nz(String s)   { return s != null && !s.isBlank(); }

    private static float drawField(PDPageContentStream cs, float x, float y, String label, String value) throws IOException {
        String l = label == null ? "" : label;
        String v = value == null ? "" : value;
        drawText(cs, x, y, l + " " + v);
        return y - 12f;
    }

    private static BufferedImage qr(String data, int width, int height) throws WriterException {
        QRCodeWriter writer = new QRCodeWriter();
        BitMatrix bm = writer.encode(data, BarcodeFormat.QR_CODE, width, height);
        return MatrixToImageWriter.toBufferedImage(bm);
    }
    // fondo de fila (rectángulo)
    private static void rowBg(PDPageContentStream cs, float x, float y, float w, float h, Color c) throws IOException {
        cs.setNonStrokingColor(c);
        cs.addRect(x, y - h + 2f, w, h); // y viene “en baseline”; ajustamos
        cs.fill();
        cs.setNonStrokingColor(Color.BLACK);
    }

    // fila clave: valor con fondo alterno
    private static float kvRow(PDPageContentStream cs, float x, float y, float xRight, String k, String v, boolean gray) throws IOException {
        if (gray) rowBg(cs, x - 7f, y + 2f, (xRight - (x - 7f)), 12f, new Color(238,238,238));
        drawText(cs, x, y, k);
        drawRight(cs, x, xRight, y, v);
        return y - 12f;
    }

    // drawKV con límite derecho configurable (por si lo usas en otro lado)
    private static float drawKV(PDPageContentStream cs, float x, float y, String k, String v, float xRight) throws IOException {
        drawText(cs, x, y, k);
        drawRight(cs, x, xRight, y, v);
        return y - 11f;
    }

}
