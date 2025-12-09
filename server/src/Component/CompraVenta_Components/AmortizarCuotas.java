package Component.CompraVenta_Components;

import org.json.JSONArray;
import org.json.JSONObject;

import Component.Descuento;
import Contabilidad.ContaHook;
import Servisofts.SConsole;
import Servisofts.SUtil;
import Servisofts.SocketCliente.SocketCliente;
import Util.ConectInstance;
import Servisofts.Server.SSSAbstract.SSSessionAbstract;

public class AmortizarCuotas {

    public AmortizarCuotas(JSONObject obj, SSSessionAbstract session, String tipo) {
        ConectInstance conectInstance = null;
        try {
            conectInstance = new ConectInstance();
            conectInstance.Transacction();

            // Capturar la data devuelta
            JSONObject data = obj.getJSONObject("data");
            JSONObject tiposPago = data.getJSONObject("tipos_pago");
            JSONArray key_cuotas = data.getJSONArray("cuotas");

            String query = """
                    select array_to_json(array_agg(cuota.*)) as json
                    from cuota
                    where cuota.key in (
                        select json_array_elements_text('%s')::text
                    )
                    """.formatted(key_cuotas.toString());

            JSONArray cuotas = conectInstance.ejecutarConsultaArray(query);

            // double total_monto_caja = 0;
            // for (String key : JSONObject.getNames(tiposPago)) {
            // JSONObject tipoPago = tiposPago.getJSONObject(key);
            // total_monto_caja += tipoPago.optDouble("monto_nacional", 0);
            // }
            // System.out.println(cuotas);

            JSONArray amortizaciones = new JSONArray();
            for (int i = 0; i < cuotas.length(); i++) {
                JSONObject cuota = cuotas.getJSONObject(i);
                double monto = cuota.optDouble("monto", 0);
                double monto_base = cuota.optDouble("monto_base", 0);
                double tipo_cambio = monto_base / monto;
                double total_amortizado = cuota.optDouble("total_amortizado", 0);
                double total_amortizado_base = cuota.optDouble("total_amortizado_base", 0);
                String key_moneda = cuota.optString("key_moneda");

                double deuda = monto - total_amortizado;
                double deuda_base = monto_base - total_amortizado_base;

                if (deuda <= 0) {
                    SConsole.log("La cuota ya esta pagada ", cuota.optString("key"));
                    continue;
                }
                for (String key_empresa_tipo_pago : JSONObject.getNames(tiposPago)) {
                    JSONObject tipoPago = tiposPago.getJSONObject(key_empresa_tipo_pago);
                    double monto_nacional = tipoPago.optDouble("monto_nacional", 0);
                    double monto_extranjera = tipoPago.optDouble("monto_extranjera", 0);
                    double tipo_cambio_tp = monto_nacional / monto_extranjera;
                    double monto_gastado_nacional = tipoPago.optDouble("monto_gastado_nacional", 0);
                    double monto_gastado_extranjera = tipoPago.optDouble("monto_gastado_extranjera", 0);

                    double monto_disponible = monto_nacional - monto_gastado_nacional;

                    if (monto_disponible <= 0) {
                        continue;
                    }

                    double monto_para_amortizar = 0;
                    if (monto_disponible >= deuda_base) {
                        monto_para_amortizar = deuda_base;
                    } else {
                        monto_para_amortizar = monto_disponible;
                    }
                    double monto_para_amortizar_extranjera = monto_para_amortizar * tipo_cambio;
                    // total_monto_caja -= monto_para_amortizar;

                    JSONObject amort = new JSONObject();
                    amort.put("key", SUtil.uuid());
                    amort.put("key_cuota", cuota.getString("key"));
                    amort.put("estado", 1);
                    amort.put("fecha_on", SUtil.now());
                    amort.put("fecha", SUtil.now());
                    amort.put("key_usuario", obj.getString("key_usuario"));
                    amort.put("descripcion", "Amortización");
                    amort.put("key_moneda", key_moneda);
                    amort.put("key_empresa_tipo_pago", key_empresa_tipo_pago);
                    amort.put("monto", monto_para_amortizar_extranjera);
                    amort.put("monto_base", monto_para_amortizar);
                    amort.put("capital", 0);
                    amort.put("interes", 0);

                    conectInstance.insertObject("cuota_amortizacion", amort);
                    total_amortizado -= monto_para_amortizar_extranjera;
                    total_amortizado_base -= monto_para_amortizar;

                    tipoPago.put("monto_gastado_nacional", monto_gastado_nacional + monto_para_amortizar);
                    tipoPago.put("monto_gastado_extranjera",
                            monto_gastado_extranjera + (monto_para_amortizar / tipo_cambio_tp));

                    amortizaciones.put(amort);
                }
                cuota.optDouble("total_amortizado", total_amortizado);
                cuota.optDouble("total_amortizado_base", total_amortizado_base);
                deuda = monto - total_amortizado;
                deuda_base = monto_base - total_amortizado_base;
                if (deuda_base <= 0) {
                    SConsole.log("La cuota se pago completa", cuota.getString("key"));
                } else {
                    SConsole.log("La cuota se pago parcial aun se debe MN:", deuda_base, "ME:", deuda,
                            cuota.getString("key"));
                }
            }

            double total_monto_restante = 0;
            double total_monto_extranjera_restante = 0;
            for (String key : JSONObject.getNames(tiposPago)) {
                JSONObject tipoPago = tiposPago.getJSONObject(key);
                total_monto_restante += (tipoPago.optDouble("monto_nacional", 0)
                        - tipoPago.optDouble("monto_gastado_nacional", 0));
                total_monto_extranjera_restante += (tipoPago.optDouble("monto_extranjera", 0)
                        - tipoPago.optDouble("monto_gastado_extranjera", 0));
                if (total_monto_restante > 0) {
                    SConsole.log("Sobro dinero en el tipo_pago", tipoPago, "total_monto_restante", total_monto_restante,
                            "total_monto_extranjera_restante", total_monto_extranjera_restante);
                }
            }
            // System.out.println("total_monto_restante",total_monto_restante);
            conectInstance.commit();

            obj.put("data", data);
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
