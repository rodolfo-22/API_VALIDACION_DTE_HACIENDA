package sv.example.factura.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.google.zxing.WriterException;

import sv.example.factura.dto.FacturaRequest;
import sv.example.factura.dto.FacturaResponse;
import sv.example.factura.dto.MinisterioResponse;
import sv.example.factura.service.FacturaPdfService;
import sv.example.factura.util.HashUtil;

import java.util.Base64;

@RestController
@RequestMapping("/api/factura")
@CrossOrigin(origins = "*")
public class FacturaController {

    private final FacturaPdfService pdfService = new FacturaPdfService();

    @PostMapping(value = "/generar", produces = "application/json")
    public ResponseEntity<FacturaResponse> generar(@RequestBody FacturaRequest request) {
        try {
            byte[] pdf = pdfService.generateFacturaPdf(request);
            String b64 = Base64.getEncoder().encodeToString(pdf);

            // Simulación de “ministerio”: sello = hash( numeroControl|codigoGeneracion|fechaHora )
            String sello = HashUtil.sha256Base64(
                    (request.numeroControl == null ? "" : request.numeroControl) + "|" +
                            (request.codigoGeneracion == null ? "" : request.codigoGeneracion) + "|" +
                            (request.fechaHora == null ? "" : request.fechaHora)
            );

            MinisterioResponse min = new MinisterioResponse(
                    "APROBADO",
                    request.numeroControl,
                    request.codigoGeneracion,
                    request.fechaHora,
                    sello,
                    "Documento validado correctamente."
            );

            return ResponseEntity.ok(FacturaResponse.ok(b64, min));
        } catch (WriterException we) {
            return ResponseEntity.status(500).body(FacturaResponse.error("Error generando código de barras: " + we.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(FacturaResponse.error("Error generando PDF: " + e.getMessage()));
        }
    }

    // Demo con todos los campos dinámicos (útil para probar rápido sin Postman)
    @GetMapping(value = "/demo", produces = "application/json")
    public ResponseEntity<FacturaResponse> demo() {
        FacturaRequest r = new FacturaRequest();
        r.fechaHora = "27/09/2025 - 12:22";
        r.numeroControl = "DTE-01-S033P007-000000000047577";
        r.codigoGeneracion = "03E26D11-A392-4C58-9C5B-7BFAA9E60C65";
        r.totalEnLetras = "DIEZ 05/100";
        r.total = "10.05";

        r.emisor = new FacturaRequest.Emisor();
        r.emisor.nombreRazonSocial = "ALMACENES VIDRI S.A. DE C.V.";
        r.emisor.nit = "02101911710016";
        r.emisor.nrc = "27";
        r.emisor.actividadEconomica = "Venta al por menor de artículos de ferretería";
        r.emisor.direccion = "KILOMETRO 66 CARRETERA A ACAJUTLA, FRENTE A BYPASS. Sonsonate Centro, Sonsonate";
        r.emisor.telefono = "2450-4033";
        r.emisor.correo = "dtes.sv@vidri.com.sv";
        r.emisor.nombreComercial = "ALMACENES VIDRI S.A. DE C.V.";
        r.emisor.establecimiento = "Sucursal Sonsonate";

        r.receptor = new FacturaRequest.Receptor();
        r.receptor.nombreRazonSocial = "RODOLFO RAFAEL GARCIA CASTILLO";
        r.receptor.tipoDocumento = "DUI";
        r.receptor.numeroDocumento = "06212737-1";
        r.receptor.direccion = "KILOMETRO 66 CARRETERA A ACAJUTLA, FRENTE A BYPASS, San Salvador Centro, San Salvador";
        r.receptor.correo = "rodof2017@gmail.com";
        r.receptor.telefono = "00000000";

        FacturaRequest.Item it1 = new FacturaRequest.Item();
        it1.posicion = 1; it1.codigo = "132838"; it1.cantidad = 1; it1.unidad = "Unidad";
        it1.descripcion = "CUBO 6P LARGO 1/2\" (12.7 MM) X 17 MM TACTIX 361230";
        it1.precioUnitario = "2.9500"; it1.descuentoItem = "0.0000";
        it1.ventasNoSujetas = "0.0000"; it1.ventasExentas = "0.0000"; it1.ventasGravadas = "2.9500";
        it1.totalItem = "2.95";

        FacturaRequest.Item it2 = new FacturaRequest.Item();
        it2.posicion = 2; it2.codigo = "26074"; it2.cantidad = 1; it2.unidad = "Unidad";
        it2.descripcion = "LUBRICANTE WD-40 BOTE 382 ML 52011";
        it2.precioUnitario = "7.1000"; it2.descuentoItem = "0.0000";
        it2.ventasNoSujetas = "0.0000"; it2.ventasExentas = "0.0000"; it2.ventasGravadas = "7.1000";
        it2.totalItem = "7.10";

        r.items = java.util.List.of(it1, it2);
        return generar(r);
    }
}
