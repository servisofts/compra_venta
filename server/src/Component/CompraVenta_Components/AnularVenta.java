package Component.CompraVenta_Components;

import org.json.JSONArray;
import org.json.JSONObject;

import Component.Descuento;
import Contabilidad.ContaHook;
import Servisofts.SPGConect;
import Servisofts.SUtil;
import Servisofts.SocketCliente.SocketCliente;
import Util.ConectInstance;
import Servisofts.Server.SSSAbstract.SSSessionAbstract;

public class AnularVenta {

    public AnularVenta(JSONObject obj, SSSessionAbstract session) {
         ConectInstance conectInstance = null;
        try {
            conectInstance = new ConectInstance();
            conectInstance.Transacction();

            String key_compra_venta = obj.getString("key_compra_venta");
            String query = """
                    SELECT to_json(sq1.*) AS json
                    FROM (
                            SELECT
                                compra_venta.*,
                                (
                                    SELECT
                                        array_to_json(array_agg(descuento.*)) AS total_descuento
                                    FROM compra_venta_descuento,
                                    descuento
                                    WHERE compra_venta_descuento.key_compra_venta = compra_venta.key
                                    AND compra_venta_descuento.estado > 0
                                    AND descuento.key = compra_venta_descuento.key_descuento
                                    AND descuento.estado > 0
                                ) as descuentos,
                                array_to_json(array_agg(cvd)) AS detalles
                            FROM compra_venta
                            LEFT JOIN (
                                SELECT
                                    compra_venta_detalle.*,
                                    array_to_json(array_agg(compra_venta_detalle_producto.*)) AS compra_venta_detalle_producto
                                FROM compra_venta_detalle
                                LEFT JOIN compra_venta_detalle_producto
                                ON compra_venta_detalle_producto.key_compra_venta_detalle = compra_venta_detalle.key AND compra_venta_detalle_producto.estado > 0
                                WHERE compra_venta_detalle.key_compra_venta = '%s' AND compra_venta_detalle.estado > 0
                                GROUP BY compra_venta_detalle.key

                            ) cvd ON compra_venta.key = cvd.key_compra_venta
                            where compra_venta.key = '%s'
                            GROUP BY compra_venta.key
                        ) sq1
                            """
                    .formatted(key_compra_venta, key_compra_venta);
            JSONObject compraVenta = SPGConect.ejecutarConsultaObject(query);
            compraVenta.put("estado", 0);
            conectInstance.editObject("compra_venta", compraVenta);
            // compraVenta.put("detalles", detalles);

            JSONObject empresa_tipo_pago = obj.getJSONObject("empresa_tipo_pago");
            String consultaGetCuotas = """
                    SELECT array_to_json(array_agg(sq1.*)) AS json
                    FROM (
                        SELECT
                            cuota.*,
                            array_to_json(
                                array_agg(cuota_amortizacion.*)
                                FILTER (WHERE cuota_amortizacion.key_cuota IS NOT NULL)
                            ) AS cuota_amortizacion
                        FROM cuota
                        LEFT JOIN cuota_amortizacion
                            ON cuota.key = cuota_amortizacion.key_cuota
                           AND cuota_amortizacion.estado > 0
                        WHERE cuota.key_compra_venta = '%s'
                          AND cuota.estado > 0
                        GROUP BY cuota.key
                    ) sq1;
                                            """
                    .formatted(key_compra_venta);

            JSONArray cuotas = conectInstance.ejecutarConsultaArray(consultaGetCuotas);

            JSONArray detallesm = new JSONArray();

            JSONArray descuentos = compraVenta.optJSONArray("descuentos");
            if (descuentos != null && descuentos.length() > 0) {

                for (int i = 0; i < descuentos.length(); i++) {
                    JSONObject descuento = descuentos.getJSONObject(i);
                    String key_cuenta_contable = descuento.getString("key_cuenta_contable");
                    detallesm.put(new JSONObject()
                            .put("key_cuenta_contable", key_cuenta_contable)
                            .put("tipo", "haber")
                            .put("monto", compraVenta.getDouble("descuento"))
                            .put("monto_me", 0)
                            .put("glosa", "Revertir descuento aplicado"));
                }
            }

            for (int i = 0; i < cuotas.length(); i++) {
                JSONObject cuota = cuotas.getJSONObject(i);
                double monto_sin_pagar = cuota.optDouble("monto_base", 0) - cuota.optDouble("total_amortizado_base", 0);
                if (monto_sin_pagar > 0) {
                    double tipo_cambio = cuota.optDouble("monto_base", 0) / cuota.optDouble("monto_base", 0);
                    double monto_sin_pagar_me = 0;
                    if (tipo_cambio != 1) {
                        monto_sin_pagar_me = Math.round(monto_sin_pagar * tipo_cambio * 100.0) / 100.0;
                    }

                    JSONObject etp = empresa_tipo_pago.getJSONObject(cuota.getString("key_empresa_tipo_pago"));
                    detallesm.put(new JSONObject()
                            .put("key_cuenta_contable", etp.getString("key_cuenta_contable"))
                            .put("tipo", "haber")
                            .put("monto", monto_sin_pagar)
                            .put("monto_me", monto_sin_pagar_me)
                            .put("glosa", "Revertir cuota pendiente por pagar"));
                    // Revertir cuota;
                }

                cuota.put("estado", 0);
                conectInstance.editObject("cuota", cuota);

                JSONArray cuota_amortizacion = cuota.optJSONArray("cuota_amortizacion");
                if (cuota_amortizacion != null) {
                    for (int j = 0; j < cuota_amortizacion.length(); j++) {
                        JSONObject amortizacion = cuota_amortizacion.getJSONObject(j);
                        double tipo_cambio = amortizacion.optDouble("monto", 0)
                                / amortizacion.optDouble("monto_base", 0);
                        double monto_me = 0;
                        if (tipo_cambio != 1) {
                            monto_me = amortizacion.optDouble("monto", 0);
                        }
                        JSONObject etp = empresa_tipo_pago
                                .getJSONObject(amortizacion.getString("key_empresa_tipo_pago"));
                        detallesm.put(new JSONObject()
                                .put("key_cuenta_contable", etp.getString("key_cuenta_contable"))
                                .put("tipo", "haber")
                                .put("monto", amortizacion.getDouble("monto_base"))
                                .put("monto_me", monto_me)
                                .put("glosa", "Revertir amortizacion pagada"));

                        amortizacion.put("estado", 0);
                        conectInstance.editObject("cuota_amortizacion", amortizacion);
                    }
                }
            }

            System.out.println(detallesm);
            obj.put("detalle_cuotas", detallesm);
            obj.put("compra_venta", compraVenta);
            obj.put("component", "modelo");
            // JSONObject send = new JSONObject();
            // send.put("component", "compra_venta");
            // send.put("type", "anularCompraVenta");
            // send.put("key_usuario", obj.getString("key_usuario"));
            // send.put("key_compra_venta", key_compra_venta);

            JSONObject data = SocketCliente.sendSinc("inventario", obj);
            if (!data.getString("estado").equals("exito")) {
                throw new Exception(data.optString("error", "Error al anular la venta en inventario"));
            }
            obj.put("data", compraVenta);
            obj.put("estado", "exito");
            conectInstance.commit();

            obj.put("data", data);
            obj.put("estado", "exito");
        } catch (Exception e) {
            e.printStackTrace();
            obj.put("estado", "error");
            obj.put("error", e.getMessage());
            conectInstance.rollback();
        } finally {
            if (conectInstance != null) {
                conectInstance.close();
            }
        }
    }

}
