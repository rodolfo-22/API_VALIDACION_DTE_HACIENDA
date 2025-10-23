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
                // Título centrado
                cs.setFont(fontBold, 14);
                drawCentered(cs, ml + logoW + 10f, box.getUpperRightX() - mr, yTop + 4f, "DOCUMENTO TRIBUTARIO ELECTRÓNICO");
                cs.setFont(fontReg, 12);
                drawCentered(cs, ml + logoW + 10f, box.getUpperRightX() - mr, yTop - 12f, "FACTURA");

                // QR en esquina superior derecha
                String qrText = "CG: " + safe(req.codigoGeneracion) + " | NC: " + safe(req.numeroControl) + " | FH: " + safe(req.fechaHora);
                BufferedImage qrImg = qr(qrText, 110, 110);
                PDImageXObject qrX = LosslessFactory.createFromImage(doc, qrImg);
                float qrXpos = box.getUpperRightX() - mr - 110f;
                float qrYpos = yTop - 110f + 8f;
                cs.drawImage(qrX, qrXpos, qrYpos, 110f, 110f);

                // Banda info bajo título (dos columnas, línea superior e inferior)
                float bandTopY = yTop - 52f;
                float bandHeight = 50f;
                drawLine(cs, ml, bandTopY, box.getUpperRightX() - mr, bandTopY, 1.2f, Color.BLACK);
                float bandBottomY = bandTopY - bandHeight;
                drawLine(cs, ml, bandBottomY, box.getUpperRightX() - mr, bandBottomY, 1.2f, Color.BLACK);

                // línea vertical al centro
                float midX = ml + contentWidth/2f;
                drawLine(cs, midX, bandTopY, midX, bandBottomY, 2f, Color.BLACK);

                cs.setFont(fontReg, 8.5f);
                float leftX = ml + 6f;
                float rightX = midX + 6f;
                float y = bandTopY - 14f;
                drawText(cs, leftX, y, "Código de Generación: " + safe(req.codigoGeneracion)); y -= 12f;
                drawText(cs, leftX, y, "Número de Control: " + safe(req.numeroControl)); y -= 12f;
                String sello = "Sello de recepción: (se genera en respuesta ministerio)";
                drawText(cs, leftX, y, sello);

                float yr = bandTopY - 14f;
                drawText(cs, rightX, yr, "Modelo de facturación: Modelo Facturación Previo"); yr -= 12f;
                drawText(cs, rightX, yr, "Tipo de Transmisión: Transmisión normal"); yr -= 12f;
                drawText(cs, rightX, yr, "Fecha y Hora de Generación: " + safe(req.fechaHora));

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
                float infoBottomY = infoTopY - 86f;
                drawLine(cs, midX, infoTopY + 2f, midX, infoBottomY - 2f, 2f, Color.BLACK);
                drawLine(cs, ml, infoBottomY, box.getUpperRightX() - mr, infoBottomY, 2f, Color.BLACK);

                cs.setFont(fontReg, 8.5f);
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

                // Receptor
                float ry2 = infoTopY - 4f;
                ry2 = drawField(cs, rx2, ry2, "Nombre o Razón social:", rc.nombreRazonSocial);
                ry2 = drawField(cs, rx2, ry2, "Tipo de Documento:", rc.tipoDocumento);
                ry2 = drawField(cs, rx2, ry2, "No. de doc. de Identificación:", rc.numeroDocumento);
                ry2 = drawField(cs, rx2, ry2, "Dirección:", rc.direccion);
                ry2 = drawField(cs, rx2, ry2, "Correo Electrónico:", rc.correo);
                ry2 = drawField(cs, rx2, ry2, "Número de teléfono:", rc.telefono);

                // ====== Tabla de Items ======
                float tblTop = infoBottomY - 20f;
                drawHeaderBar(cs, ml, tblTop, contentWidth, 18f, new Color(212,60,60));
                cs.setFont(fontBold, 8.5f);
                cs.setNonStrokingColor(Color.WHITE);
                drawText(cs, ml + 6f, tblTop + 5f,
                        String.format("%-4s %-8s %-5s %-7s %-56s %12s %12s %12s %12s",
                                "N°","Código","Cant.","Unidad","Descripción","Precio Unitario","Desc. Item","No Suj.","Exentas","Gravadas"));
                cs.setNonStrokingColor(Color.BLACK);
                cs.setFont(monoReg, 8.2f);

                float yi = tblTop - 18f - 6f;
                List<FacturaRequest.Item> items = req.items != null ? req.items : List.of();
                for (FacturaRequest.Item it : items) {
                    String desc = trunc(safe(it.descripcion), 56);
                    String linea = String.format("%-4d %-8s %-5d %-7s %-56s %12s %12s %12s %12s",
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
                    drawText(cs, ml + 6f, yi, linea);
                    yi -= 12f;
                }

                // ====== Panel inferior: Detalles (izq) y Suma Total (der) ======
                float contentBottom = 72f;
                float leftW = contentWidth * 0.60f;
                float rightW = contentWidth - leftW;
                float blockTop = yi - 12f;

                // Caja izquierda
                drawBox(cs, ml, blockTop, leftW, 120f, new Color(212,60,60), 1.2f);
                cs.setFont(fontBold, 9f);
                cs.setNonStrokingColor(Color.WHITE);
                drawText(cs, ml + 8f, blockTop + 5f, "Detalles");
                cs.setNonStrokingColor(Color.BLACK);
                cs.setFont(fontReg, 8.5f);

                float dY = blockTop - 16f;
                dY = drawField(cs, ml + 8f, dY, "Total en letras:", req.totalEnLetras);
                dY = drawField(cs, ml + 8f, dY, "Condición de la Operación:", "CONTADO - N1");
                drawLine(cs, ml + 8f, dY - 2f, ml + leftW - 8f, dY - 2f, .6f, Color.LIGHT_GRAY); dY -= 10f;
                dY = drawField(cs, ml + 8f, dY, "Responsable por parte del EMISOR:", "");
                dY = drawField(cs, ml + 8f + (leftW/2f - 16f), dY + 12f, "N° Documento:", ""); dY -= 22f;
                dY = drawField(cs, ml + 8f, dY, "Responsable por parte del RECEPTOR:", "");
                dY = drawField(cs, ml + 8f + (leftW/2f - 16f), dY + 12f, "N° Documento:", ""); dY -= 22f;
                dY = drawField(cs, ml + 8f, dY, "Número de control Vidri:", safe(req.numeroControl).replace("DTE-01-", ""));
                dY = drawField(cs, ml + 8f, dY, "Vendedor:", "");
                dY = drawField(cs, ml + 8f, dY, "Código interno:", "");

                // Caja derecha
                 rightX = ml + leftW;
                drawBox(cs, rightX, blockTop, rightW, 120f, new Color(212,60,60), 1.2f);
                cs.setFont(fontBold, 9f);
                cs.setNonStrokingColor(Color.WHITE);
                drawText(cs, rightX + 8f, blockTop + 5f, "Suma Total de Operaciones");
                cs.setNonStrokingColor(Color.BLACK);
                cs.setFont(fontReg, 8.5f);

                float ty = blockTop - 16f;
                ty = drawKV(cs, rightX + 8f, ty, "Ventas no Sujetas:", "$0.00");
                ty = drawKV(cs, rightX + 8f, ty, "Total Gravada:", "$" + safe(req.total));
                ty = drawKV(cs, rightX + 8f, ty, "Monto Global Descuento, Bonificación, Rebajas y Otros a Ventas Gravadas:", "$0.00");
                ty = drawKV(cs, rightX + 8f, ty, "Sumatoria de Ventas:", "$" + safe(req.total));
                ty = drawKV(cs, rightX + 8f, ty, "Sub-Total:", "$" + safe(req.total));
                ty = drawKV(cs, rightX + 8f, ty, "IVA Percibido:", "$0.00");
                ty = drawKV(cs, rightX + 8f, ty, "IVA Retenido:", "$0.00");
                ty = drawKV(cs, rightX + 8f, ty, "Retención Renta:", "$0.00");
                ty = drawKV(cs, rightX + 8f, ty, "Monto Total de la Operación:", "$" + safe(req.total));
                ty = drawKV(cs, rightX + 8f, ty, "Total otros Montos no Afectos:", "$0.00");

                // Banda final total a pagar
                float payBarH = 14f;
                cs.setNonStrokingColor(new Color(212,60,60));
                cs.addRect(rightX, ty - payBarH, rightW, payBarH);
                cs.fill();
                cs.setNonStrokingColor(Color.WHITE);
                cs.setFont(fontBold, 9.5f);
                drawRight(cs, rightX + 8f, rightX + rightW - 8f, ty - payBarH + 3.5f, "Total a pagar: $" + safe(req.total));
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

    private static float drawKV(PDPageContentStream cs, float x, float y, String k, String v) throws IOException {
        drawText(cs, x, y, k);
        drawRight(cs, x, x + 240f, y, v);
        return y - 12f;
    }

    private static BufferedImage qr(String data, int width, int height) throws WriterException {
        QRCodeWriter writer = new QRCodeWriter();
        BitMatrix bm = writer.encode(data, BarcodeFormat.QR_CODE, width, height);
        return MatrixToImageWriter.toBufferedImage(bm);
    }
}
