package Component.CompraVenta_Components;

import org.json.JSONArray;
import org.json.JSONObject;

import Servisofts.Contabilidad.AsientoContable;
import Servisofts.Contabilidad.AsientoContableDetalle;
import Servisofts.Contabilidad.AsientoContableTipo;
import Util.ConectInstance;
import Servisofts.Server.SSSAbstract.SSSessionAbstract;

/**
 * Revierte UNA amortización puntual (no toda la venta/compra, para eso está Anular.java):
 * el "click" de amortizar puede haber creado varias filas de cuota_amortizacion (una por
 * cada combinación cuota x medio de pago), pero todas comparten el mismo key_caja_detalle
 * del medio de pago que las financió. Esa es la unidad que se revierte junta.
 */
public class AnularAmortizacion {

    public AnularAmortizacion(JSONObject obj, SSSessionAbstract session) {
        ConectInstance conectInstance = null;
        try {
            conectInstance = new ConectInstance();
            conectInstance.Transacction();

            JSONObject data = obj.getJSONObject("data");
            JSONObject empresaTipoPago = obj.getJSONObject("empresa_tipo_pago");

            // Se puede identificar el pago a anular por su key_caja_detalle directo (ej. desde el
            // listado de movimientos de caja) o por una de sus cuota_amortizacion (ej. desde el kardex).
            String key_caja_detalle = data.optString("key_caja_detalle", "");
            if (key_caja_detalle.isEmpty() && data.has("key_cuota_amortizacion")) {
                String queryResolver = """
                        SELECT to_json(sq.*) AS json
                        FROM (
                            SELECT cuota_amortizacion.key_caja_detalle
                            FROM cuota_amortizacion
                            WHERE cuota_amortizacion.key = '%s'
                              AND cuota_amortizacion.estado > 0
                        ) sq
                        """.formatted(data.getString("key_cuota_amortizacion"));

                JSONObject resuelto = conectInstance.ejecutarConsultaObject(queryResolver);
                if (resuelto == null || resuelto.isEmpty()) {
                    throw new Exception("La amortización no existe o ya fue anulada");
                }
                key_caja_detalle = resuelto.optString("key_caja_detalle", "");
            }

            if (key_caja_detalle.isEmpty()) {
                throw new Exception(
                        "Esta amortización es anterior a la actualización del sistema y no puede anularse automáticamente");
            }

            String queryOrigen = """
                    SELECT to_json(sq.*) AS json
                    FROM (
                        SELECT
                            compra_venta.tipo AS tipo_compra_venta,
                            compra_venta.key_cliente,
                            compra_venta.key_proveedor,
                            compra_venta.key_empresa
                        FROM cuota_amortizacion
                        JOIN cuota ON cuota.key = cuota_amortizacion.key_cuota
                        JOIN compra_venta ON compra_venta.key = cuota.key_compra_venta
                        WHERE cuota_amortizacion.key_caja_detalle = '%s'
                          AND cuota_amortizacion.estado > 0
                        LIMIT 1
                    ) sq
                    """.formatted(key_caja_detalle);

            JSONObject origen = conectInstance.ejecutarConsultaObject(queryOrigen);
            if (origen == null || origen.isEmpty()) {
                throw new Exception("No se encontraron amortizaciones activas para este pago");
            }

            String tipo = origen.getString("tipo_compra_venta");
            String key_empresa = origen.getString("key_empresa");
            String columnaClienteProveedor = tipo.equals("venta") ? "key_cliente" : "key_proveedor";
            String keyClienteProveedor = tipo.equals("venta") ? origen.optString("key_cliente")
                    : origen.optString("key_proveedor");

            // Todas las filas de cuota_amortizacion que este mismo pago (mismo caja_detalle) generó.
            String queryHermanas = """
                    SELECT array_to_json(array_agg(cuota_amortizacion.*)) AS json
                    FROM cuota_amortizacion
                    WHERE cuota_amortizacion.key_caja_detalle = '%s'
                      AND cuota_amortizacion.estado > 0
                    """.formatted(key_caja_detalle);

            JSONArray hermanas = conectInstance.ejecutarConsultaArray(queryHermanas);
            if (hermanas == null || hermanas.length() == 0) {
                throw new Exception("No se encontraron amortizaciones activas para este pago");
            }

            // Info de cuenta contable por cada hermana, para armar la reversión.
            String queryDetalleContable = """
                    SELECT array_to_json(array_agg(sq.*)) AS json
                    FROM (
                        SELECT
                            cuota_amortizacion.key,
                            cuota_amortizacion.key_cuota,
                            cuota_amortizacion.monto,
                            cuota_amortizacion.monto_base,
                            cuota_amortizacion.key_empresa_tipo_pago,
                            cuota.key_cuenta_contable,
                            cuota.codigo AS codigo_cuota
                        FROM cuota_amortizacion
                        JOIN cuota ON cuota.key = cuota_amortizacion.key_cuota
                        WHERE cuota_amortizacion.key_caja_detalle = '%s'
                          AND cuota_amortizacion.estado > 0
                    ) sq
                    """.formatted(key_caja_detalle);

            JSONArray detalleContable = conectInstance.ejecutarConsultaArray(queryDetalleContable);

            // Solo se puede anular el pago más reciente de este cliente/proveedor.
            String queryPosteriores = """
                    SELECT to_json(sq.*) AS json
                    FROM (
                        SELECT count(*) AS cnt
                        FROM cuota_amortizacion ca2
                        JOIN cuota c2 ON c2.key = ca2.key_cuota
                        JOIN compra_venta cv2 ON cv2.key = c2.key_compra_venta
                        WHERE cv2.%s = '%s'
                          AND cv2.key_empresa = '%s'
                          AND ca2.estado > 0
                          AND (ca2.key_caja_detalle IS NULL OR ca2.key_caja_detalle <> '%s')
                          AND ca2.fecha_on > (
                              SELECT max(fecha_on) FROM cuota_amortizacion
                              WHERE key_caja_detalle = '%s' AND estado > 0
                          )
                    ) sq
                    """.formatted(columnaClienteProveedor, keyClienteProveedor, key_empresa, key_caja_detalle,
                    key_caja_detalle);

            JSONObject posteriores = conectInstance.ejecutarConsultaObject(queryPosteriores);
            if (posteriores != null && posteriores.optInt("cnt", 0) > 0) {
                throw new Exception("Solo se puede anular la amortización más reciente de este " +
                        (tipo.equals("venta") ? "cliente" : "proveedor"));
            }

            AsientoContable asiento = new AsientoContable(AsientoContableTipo.traspaso);
            asiento.descripcion = "Anulación de amortización de cuotas";
            asiento.observacion = "Anulación de amortización de cuotas";
            asiento.key_empresa = key_empresa;
            asiento.key_usuario = obj.getString("key_usuario");

            double montoTotalRevertido = 0;
            double montoTotalRevertidoBase = 0;

            for (int i = 0; i < detalleContable.length(); i++) {
                JSONObject det = detalleContable.getJSONObject(i);

                JSONObject tags = new JSONObject()
                        .put("key_usuario", obj.getString("key_usuario"))
                        .put("key_empresa", key_empresa)
                        .put("key_cuota", det.getString("key_cuota"))
                        .put("key_cuota_amortizacion", det.getString("key"))
                        .put("key_empresa_tipo_pago", det.getString("key_empresa_tipo_pago"));

                JSONObject etp = empresaTipoPago.getJSONObject(det.getString("key_empresa_tipo_pago"));

                // Reversión del lado de la cuota: signo opuesto al que posteó AmortizarCuotas.
                asiento.setDetalle(new AsientoContableDetalle(
                        det.getString("key_cuenta_contable"),
                        "Anulación amortización de cuota " + det.optString("codigo_cuota", ""),
                        tipo.equals("venta") ? "debe" : "haber",
                        det.getDouble("monto_base"),
                        det.getDouble("monto"),
                        tags));

                // Reversión del lado del medio de pago: signo opuesto también.
                asiento.setDetalle(new AsientoContableDetalle(
                        etp.getString("key_cuenta_contable"),
                        "Anulación amortización por medio de " + etp.optString("descripcion", ""),
                        tipo.equals("venta") ? "haber" : "debe",
                        det.getDouble("monto_base"),
                        det.getDouble("monto"),
                        tags));

                montoTotalRevertido += det.getDouble("monto");
                montoTotalRevertidoBase += det.getDouble("monto_base");
            }

            JSONObject response = asiento.enviar();
            if (!"exito".equals(response.optString("estado"))) {
                throw new Exception(response.optString("error", "Error al generar el asiento de reversión"));
            }

            // Poner estado=0 en cada hermana: el trigger actualizar_cuota_amortizacion recalcula la cuota sola.
            for (int i = 0; i < hermanas.length(); i++) {
                JSONObject hermana = hermanas.getJSONObject(i);
                hermana.put("estado", 0);
                conectInstance.editObject("cuota_amortizacion", hermana);
            }

            conectInstance.commit();

            obj.put("data", new JSONObject()
                    .put("key_caja_detalle", key_caja_detalle)
                    .put("monto", montoTotalRevertido)
                    .put("monto_base", montoTotalRevertidoBase)
                    .put("tipo", tipo));
            obj.put("estado", "exito");
        } catch (Exception e) {
            e.printStackTrace();
            obj.put("estado", "error");
            obj.put("error", e.getMessage());
            if (conectInstance != null) {
                conectInstance.rollback();
            }
        } finally {
            if (conectInstance != null) {
                conectInstance.close();
            }
        }
    }
}
