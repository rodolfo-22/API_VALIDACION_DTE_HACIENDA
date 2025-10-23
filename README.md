# factura-api

API en Spring Boot que genera una factura PDF (tamaño Carta) con posiciones absolutas, fuente monoespaciada y código de barras. Devuelve:
- `base64Pdf` para abrir en navegador con `data:application/pdf;base64,...`
- `ministerio` con JSON de simulación de aprobación.

## Requisitos
- Java 17+
- Maven 3.9+

## Ejecutar
```bash
mvn spring-boot:run
```
La API queda en: `http://localhost:8080`

### Salud
```
GET http://localhost:8080/api/health
```

### Demo (sin payload)
```
GET http://localhost:8080/api/factura/demo
```

### Generar con payload propio
```
POST http://localhost:8080/api/factura/generar
Content-Type: application/json
```
Body ejemplo:
```json
{
  "nombreEmisor":"ALMACENES VIDRI S.A. DE C.V.",
  "nit":"02101911710016",
  "direccionEmisor":"KILOMETRO 66 CARRETERA A ACAJUTLA, FRENTE A BYPASS. Sonsonate",
  "nombreReceptor":"RODOLFO RAFAEL GARCIA CASTILLO",
  "tipoDocumentoReceptor":"DUI",
  "noDocReceptor":"06212737-1",
  "fechaHora":"27/09/2025 - 12:22",
  "numeroControl":"DTE-01-S033P007-000000000047577",
  "codigoGeneracion":"03E26D11-A392-4C58-9C5B-7BFAA9E60C65",
  "totalEnLetras":"DIEZ 05/100",
  "total":"10.05",
  "items":[
    {"posicion":1,"codigo":"132838","cantidad":1,"unidad":"Unidad","descripcion":"CUBO 6P LARGO 1/2\" (12.7 MM) X 17 MM TACTIX 361230","precioUnitario":"2.9500","totalItem":"2.95"},
    {"posicion":2,"codigo":"26074","cantidad":1,"unidad":"Unidad","descripcion":"LUBRICANTE WD-40 BOTE 382 ML 52011","precioUnitario":"7.1000","totalItem":"7.10"}
  ]
}
```

La respuesta incluye:
- `base64Pdf` → usar como `src` en `<embed>` o `href` en `<a>`:
```html
<embed id="viewer" type="application/pdf" width="100%" height="800px">
<script>
  fetch('/api/factura/demo')
    .then(r => r.json())
    .then(data => {
      document.getElementById('viewer').src = 'data:application/pdf;base64,' + data.base64Pdf;
    });
</script>
```
- `ministerio` → JSON con `estado`, `numeroControl`, `codigoGeneracion`, `fechaHora`, `selloRecibido`, `observaciones`.

## Notas de formato
- Fuente monoespaciada (Courier) para respetar exactamente los espacios y columnas.
- Posicionamiento absoluto (coordenadas x,y) ajustable para calzar con tu plantilla.
- Código de barras Code128 (ZXing) insertado como imagen.
- Si necesitas soporte completo UTF-8 (acentos avanzados), carga una TTF (p. ej. Noto Sans Mono) con `PDType0Font.load(...)`.
