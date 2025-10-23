package sv.example.factura.dto;

public class FacturaResponse {
    public String base64Pdf;   // PDF en base64
    public String mime;        // "application/pdf"
    public String filename;    // "factura.pdf"
    public MinisterioResponse ministerio; // JSON de simulación

    public String status;      // "ok" | "error"
    public String message;     // detalles en error

    public static FacturaResponse ok(String base64, MinisterioResponse min) {
        FacturaResponse r = new FacturaResponse();
        r.base64Pdf = base64;
        r.mime = "application/pdf";
        r.filename = "factura.pdf";
        r.ministerio = min;
        r.status = "ok";
        return r;
    }

    public static FacturaResponse error(String msg) {
        FacturaResponse r = new FacturaResponse();
        r.status = "error";
        r.message = msg;
        return r;
    }
}
