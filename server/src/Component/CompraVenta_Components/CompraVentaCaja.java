package Component.CompraVenta_Components;

import org.json.JSONArray;
import org.json.JSONObject;

import Component.Descuento;
import Contabilidad.ContaHook;
import Servisofts.SUtil;
import Servisofts.SocketCliente.SocketCliente;
import Util.ConectInstance;
import Servisofts.Server.SSSAbstract.SSSessionAbstract;

public class CompraVentaCaja {

    public CompraVentaCaja(JSONObject obj, SSSessionAbstract session, String tipo) {
        ConectInstance conectInstance = null;
        try {
            conectInstance = new ConectInstance();
            conectInstance.Transacction();

            JSONObject data = obj.getJSONObject("data");
            JSONObject caja = obj.getJSONObject("data").getJSONObject("caja");
            JSONArray detalle = obj.getJSONObject("data").getJSONArray("detalle");

            JSONObject moneda = ContaHook.getMoneda(caja.getString("key_empresa"), data.getString("key_moneda"));

            double totalDescuento = 0;

            JSONArray descuentosObj = data.optJSONArray("descuentos");
            if (descuentosObj != null && descuentosObj.length() > 0) {
                JSONObject descuentos = Descuento.getAll(caja.getString("key_empresa"));
                for (int i = 0; i < descuentosObj.length(); i++) {
                    String key_descuento = descuentosObj.getJSONObject(i).optString("key");
                    if (!descuentos.has(key_descuento)) {
                        throw new Exception("El descuento con key " + key_descuento + " no existe");
                    }
                    double porcentaje = descuentos.getJSONObject(key_descuento).getDouble("porcentaje");
                    String key_cuenta_contable = descuentos.getJSONObject(key_descuento)
                            .getString("key_cuenta_contable");
                    totalDescuento += porcentaje;

                    descuentosObj.getJSONObject(i).put("key_cuenta_contable", key_cuenta_contable);
                    descuentosObj.getJSONObject(i).put("porcentaje", porcentaje);
                }
            }

            if (totalDescuento > 100) {
                throw new Exception("El descuento no puede ser mayor al 100%");
            }

            JSONObject compraVenta = new JSONObject();
            compraVenta.put("key", data.getString("key_compra_venta"));
            compraVenta.put("estado", 1);
            compraVenta.put("fecha_on", SUtil.now());
            compraVenta.put("key_usuario", data.getString("key_usuario"));
            compraVenta.put("descripcion", data.getString("descripcion"));
            compraVenta.put("observacion", data.optString("observacion"));
            compraVenta.put("state", tipo == "compra" ? "comprado" : "vendido");
            compraVenta.put("tipo", tipo);
            compraVenta.put("tipo_pago", "contado");
            compraVenta.put("key_proveedor", data.optString("key_proveedor"));
            compraVenta.put("key_cliente", data.optString("key_cliente"));
            compraVenta.put("key_empresa", caja.getString("key_empresa"));
            compraVenta.put("key_sucursal", caja.getString("key_sucursal"));
            compraVenta.put("key_caja", caja.getString("key"));
            compraVenta.put("key_moneda", moneda.getString("key"));
            compraVenta.put("tipo_cambio", moneda.getDouble("tipo_cambio"));
            compraVenta.put("facturar", data.getBoolean("facturar"));

            JSONObject data_extra = new JSONObject();
            data_extra.put("razon_social", data.optJSONObject("cliente").optString("razon_social"));
            data_extra.put("nit", data.optJSONObject("cliente").optString("nit"));
            compraVenta.put("data", data_extra);

            conectInstance.insertObject("compra_venta", compraVenta);

            data.put("compra_venta", compraVenta);

            JSONArray compraVentaDescuentos = new JSONArray();

            if (descuentosObj != null) {
                for (int i = 0; i < descuentosObj.length(); i++) {
                    String key_descuento = descuentosObj.getJSONObject(i).optString("key");
                    JSONObject descuento = new JSONObject();
                    descuento.put("key", SUtil.uuid());
                    descuento.put("monto", descuentosObj.getJSONObject(i).getDouble("monto"));
                    descuento.put("estado", 1);
                    descuento.put("fecha_on", SUtil.now());
                    descuento.put("key_usuario", data.getString("key_usuario"));
                    descuento.put("key_compra_venta", compraVenta.getString("key"));
                    descuento.put("key_descuento", key_descuento);
                    compraVentaDescuentos.put(descuento);
                }
            }

            compraVenta.put("descuentos", descuentosObj);
            conectInstance.insertArray("compra_venta_descuento", compraVentaDescuentos);

            String obs = data.optString("observacion", null);
            if (obs == null) {
                obs = (tipo == "compra" ? "Compra" : "Venta");
            }
            data.put("observacion", obs);

            double total_compra_venta = 0;

            for (int i = 0; i < detalle.length(); i++) {
                JSONObject item = detalle.getJSONObject(i);
                if (!item.has("key_modelo") || item.isNull("key_modelo")) {
                    throw new Exception("El campo key_modelo es requerido en el detalle");
                }
                if (!item.has("cantidad") || item.isNull("cantidad")) {
                    throw new Exception("El campo cantidad es requerido en el detalle");
                }
                if (!item.has("precio_unitario") || item.isNull("precio_unitario")) {
                    throw new Exception("El campo precio_unitario es requerido en el detalle");
                }

                total_compra_venta += item.getDouble("cantidad") * item.getDouble("precio_unitario_base")
                        - item.optDouble("descuento", 0);

                item.put("key", SUtil.uuid());
                item.put("estado", 1);
                item.put("fecha_on", SUtil.now());
                item.put("key_compra_venta", compraVenta.getString("key"));

            }

            total_compra_venta = Math.round(total_compra_venta * 100.0) / 100.0; // Redondear a dos decimales
            // total_compra_venta -= totalDescuento;
            double descuento = total_compra_venta * (totalDescuento);
            descuento = Math.round(descuento * 100.0) / 100.0;
            // Redondear a dos decimales
            compraVenta.put("descuento", descuento);
            conectInstance.editObject("compra_venta", compraVenta);

            total_compra_venta -= descuento;

            total_compra_venta = Math.round(total_compra_venta * 100.0) / 100.0;

            JSONObject tiposPago = data.getJSONObject("tipos_pago");
            double montoBase = 0.0;

            JSONArray cuotas = new JSONArray();

            for (int i = 0; i < JSONObject.getNames(tiposPago).length; i++) {
                JSONObject tipoPago = tiposPago.getJSONObject(JSONObject.getNames(tiposPago)[i]);

                JSONObject cuota = new JSONObject();
                cuota.put("key", SUtil.uuid());
                cuota.put("estado", 1);
                cuota.put("fecha_on", SUtil.now());
                cuota.put("fecha", SUtil.now().substring(0, 10));
                cuota.put("key_usuario", data.getString("key_usuario"));
                cuota.put("key_compra_venta", compraVenta.getString("key"));
                cuota.put("key_moneda", tipoPago.getJSONObject("empresa_tipo_pago").getString("key_moneda"));
                cuota.put("codigo", cuotas.length() + "");
                cuota.put("monto_base", tipoPago.getDouble("monto_nacional"));
                montoBase += tipoPago.getDouble("monto_nacional");
                cuota.put("monto", tipoPago.getDouble("monto_extranjera"));
                cuota.put("capital", 0);
                cuota.put("interes", 0);
                cuota.put("estado", 1);

                if (tipoPago.getJSONObject("empresa_tipo_pago").getString("key_tipo_pago").equals("credito")) {

                    cuota.put("descripcion", "Cuota Credito");
                    conectInstance.insertObject("cuota", cuota);

                } else {
                    cuota.put("descripcion", "Cuota Contado");
                    conectInstance.insertObject("cuota", cuota);

                    JSONObject cuotaAmortizacion = new JSONObject();
                    cuotaAmortizacion.put("key", SUtil.uuid());
                    cuotaAmortizacion.put("key_cuota", cuota.getString("key"));
                    cuotaAmortizacion.put("estado", 1);
                    cuotaAmortizacion.put("fecha_on", SUtil.now());
                    cuotaAmortizacion.put("fecha", SUtil.now());
                    cuotaAmortizacion.put("key_usuario", data.getString("key_usuario"));
                    cuotaAmortizacion.put("key_compra_venta", compraVenta.getString("key"));
                    cuotaAmortizacion.put("descripcion", "Cuota Amortizacion");
                    cuotaAmortizacion.put("key_moneda",
                            tipoPago.getJSONObject("empresa_tipo_pago").getString("key_moneda"));
                    cuotaAmortizacion.put("key_empresa_tipo_pago",
                            tipoPago.getJSONObject("empresa_tipo_pago").optString("key"));
                    cuotaAmortizacion.put("monto", tipoPago.getDouble("monto_extranjera"));
                    cuotaAmortizacion.put("monto_base", tipoPago.getDouble("monto_nacional"));
                    cuotaAmortizacion.put("capital", 0);
                    cuotaAmortizacion.put("interes", 0);
                    conectInstance.insertObject("cuota_amortizacion", cuotaAmortizacion);

                }
                cuotas.put(cuota);
            }
            montoBase = Math.round(montoBase * 100.0) / 100.0;
            if (total_compra_venta != montoBase) {
                throw new Exception("El total de la " + tipo + " no coincide con el total pagado, " + total_compra_venta
                        + " != " + montoBase);
            }

            data.put("cuotas", cuotas);

            JSONObject send = new JSONObject();
            send.put("component", "modelo");
            if (tipo.equals("compra")) {
                send.put("type", "compraCaja");
            } else if (tipo.equals("venta")) {
                send.put("type", "ventaCaja");
            }
            send.put("data", data);

            JSONObject response = SocketCliente.sendSinc("inventario", send);
            // System.out.println(response);

            if (!response.getString("estado").equals("exito")) {
                throw new Exception(response.optString("error", "Error al registrar la compra"));
            }

            data = response.getJSONObject("data");

            conectInstance.insertArray("compra_venta_detalle", detalle);
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
