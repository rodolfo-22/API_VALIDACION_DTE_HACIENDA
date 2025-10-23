package sv.example.factura.dto;

import java.util.List;

/**
 * Payload principal que recibirá la API para generar el PDF.
 * Contiene emisor, receptor, metadatos y detalle de items.
 */
public class FacturaRequest {

    public String fechaHora;         // p.ej. "27/09/2025 - 12:22"
    public String numeroControl;     // p.ej. "DTE-01-S033P007-000000000047577"
    public String codigoGeneracion;  // GUID

    public Emisor emisor;
    public Receptor receptor;

    public String totalEnLetras;     // p.ej. "DIEZ 05/100"
    public String total;             // p.ej. "10.05"

    public List<Item> items;

    // --- subtipos ----
    public static class Emisor {
        public String nombreRazonSocial;
        public String nit;
        public String nrc;                 // opcional
        public String actividadEconomica;  // opcional
        public String direccion;
        public String telefono;            // opcional
        public String correo;              // opcional
        public String nombreComercial;     // opcional
        public String establecimiento;     // opcional
    }

    public static class Receptor {
        public String nombreRazonSocial;
        public String tipoDocumento;   // p.ej. "DUI", "NIT"
        public String numeroDocumento;
        public String direccion;       // opcional
        public String correo;          // opcional
        public String telefono;        // opcional
    }

    public static class Item {
        public int    posicion;
        public String codigo;
        public int    cantidad;
        public String unidad;
        public String descripcion;
        public String precioUnitario;     // texto para imprimir tal cual (formateado)
        public String descuentoItem;      // opcional, "0.0000" por defecto
        public String ventasNoSujetas;    // opcional
        public String ventasExentas;      // opcional
        public String ventasGravadas;     // opcional
        public String totalItem;          // opcional si quieres imprimir un total por renglón
    }
}
