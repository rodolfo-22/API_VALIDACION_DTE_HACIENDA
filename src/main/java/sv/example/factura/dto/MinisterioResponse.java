package sv.example.factura.dto;

/**
 * Simulación de la respuesta del "ministerio".
 * Ajusta los nombres/estructura a lo que necesites “literalmente”.
 */
public class MinisterioResponse {
    public String estado;             // "APROBADO" | "RECHAZADO"
    public String numeroControl;      // igual al request
    public String codigoGeneracion;   // igual al request
    public String fechaHora;          // igual al request o generado
    public String selloRecibido;      // hash/huella simulada
    public String observaciones;      // texto libre si aplica

    public MinisterioResponse() {}

    public MinisterioResponse(String estado, String numeroControl, String codigoGeneracion,
                              String fechaHora, String selloRecibido, String observaciones) {
        this.estado = estado;
        this.numeroControl = numeroControl;
        this.codigoGeneracion = codigoGeneracion;
        this.fechaHora = fechaHora;
        this.selloRecibido = selloRecibido;
        this.observaciones = observaciones;
    }
}
