package Component;

import org.json.JSONArray;
import org.json.JSONObject;

import Contabilidad.ContaHook;
import Servisofts.SPGConect;
import Servisofts.SUtil;
import Servisofts.Contabilidad.AsientoContable;
import Servisofts.Contabilidad.AsientoContableDetalle;
import Servisofts.Contabilidad.AsientoContableTipo;
import Servisofts.Server.SSSAbstract.SSSessionAbstract;

public class CuotaAmortizacion {
    public static final String COMPONENT = "cuota_amortizacion";

    public static void onMessage(JSONObject obj, SSSessionAbstract session) {
        switch (obj.getString("type")) {
            case "getAll":
                getAll(obj, session);
                break;
            case "getByKey":
                getByKey(obj, session);
                break;
            case "registro":
                registro(obj, session);
                break;
            case "editar":
                editar(obj, session);
                break;
        }
    }

    public static void getAll(JSONObject obj, SSSessionAbstract session) {
        try {
            String consulta = "select get_all('" + COMPONENT + "', 'key_cuota', '"+obj.getString("key_cuota")+"') as json";
            JSONObject data = SPGConect.ejecutarConsultaObject(consulta);
            obj.put("data", data);
            obj.put("estado", "exito");
        } catch (Exception e) {
            obj.put("estado", "error");
            obj.put("error", e.getMessage());
            e.printStackTrace();
        }
    }

    public static JSONObject getAll(String key_cuota) {
        try {
            String consulta = "select get_all('" + COMPONENT + "', 'key_cuota', '"+key_cuota+"') as json";
            return SPGConect.ejecutarConsultaObject(consulta);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public static double getAmortizacioens(String key_cuota) {
        try {
            String consulta = "select get_amortizaciones('"+key_cuota+"') as monto";
            String a = SPGConect.ejecutarConsultaString(consulta);
            return Double.parseDouble(a);
        } catch (Exception e) {
            //e.printStackTrace();
            return 0;
        }
    }

    public static void getByKey(JSONObject obj, SSSessionAbstract session) {
        try {
            String consulta = "select get_by_key('" + COMPONENT + "', '" + obj.getString("key") + "') as json";
            JSONObject data = SPGConect.ejecutarConsultaObject(consulta);
            obj.put("data", data);
            obj.put("estado", "exito");
        } catch (Exception e) {
            obj.put("estado", "error");
            obj.put("error", e.getMessage());
            e.printStackTrace();
        }
    }

    public static JSONObject getByKey(String key) {
        try {
            String consulta = "select get_by_key('" + COMPONENT + "', '" + key + "') as json";
            return SPGConect.ejecutarConsultaObject(consulta);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public static void registro(JSONObject obj, SSSessionAbstract session) {
        try {
            JSONObject data = obj.getJSONObject("data");
            
            data.put("estado", 1);
            data.put("fecha_on", SUtil.now());
            data.put("key_usuario", obj.getString("key_usuario"));
            
            
            for (int i = 0; i < data.getJSONArray("key_cuotas").length(); i++) {
                intentarAmortizar(data.getJSONArray("key_cuotas").getString(i), data);
            }

            obj.put("estado", "exito");

            
            
           
        } catch (Exception e) {
            obj.put("estado", "error");
            obj.put("error", e.getMessage());
            e.printStackTrace();
        }
    } 

    public static void intentarAmortizar(String key_cuota, JSONObject data){
        try {

            // trae plata
            if(data.getDouble("monto")<=0){
                data.put("estado", "error");
                data.put("error", "Monto insuficiente");
                return;
            }

            JSONObject cuota = Cuota.getByKey(key_cuota);
            double amortizaciones = CuotaAmortizacion.getAmortizacioens(key_cuota);
            double monto_deuda = cuota.getDouble("monto")-amortizaciones;

            // ya no se debe
            if(monto_deuda<=0){
                data.put("estado", "error");
                data.put("error", "No tiene deuda");
                cuota.put("estado", 2);
                SPGConect.editObject("cuota", cuota);
                return;
            }


            // se debe plata monto_deuda
            
            data.put("key", SUtil.uuid());
            data.put("key_cuota", cuota.getString("key"));
            data.put("estado", 1);


            SPGConect.insertArray(COMPONENT, new JSONArray().put(data));
            System.out.println("amortizar");

            if(data.getDouble("monto")>=monto_deuda){   
                cuota.put("estado", 1);               
                SPGConect.editObject("cuota", cuota);   
            }


            JSONObject caja = CompraVenta.getCaja(data.getString("key_caja"));
            JSONObject punto_venta = CompraVenta.getPuntoVenta(caja.getString("key_punto_venta"));
            JSONObject sucursal = CompraVenta.getSucursal(punto_venta.getString("key_sucursal"));
            

            JSONObject monedas = ContaHook.getMonedas(sucursal.getString("key_empresa"));
            String keyMoneda = data.getString("key_moneda");
            JSONObject monedaCaja = monedas.getJSONObject(keyMoneda);
            double tipo_cambioCaja = monedaCaja.getDouble("tipo_cambio");

            JSONObject puntoVentaTiposPago = ContaHook.puntosVentaTipoPago(caja.getString("key_punto_venta"));
            JSONObject tiposPago = ContaHook.tiposPago();
            double totalPago = 0;

            AsientoContable asiento = new AsientoContable(AsientoContableTipo.ingreso);
            asiento.descripcion = "Amortizacion de cuota";
            asiento.observacion = "Amortizacion de cuota obs";
            asiento.fecha = SUtil.now();
            asiento.key_empresa = sucursal.getString("key_empresa");
            asiento.key_usuario = data.getString("key_usuario");

            


            JSONObject tags = new JSONObject()
                .put("key_usuario", data.getString("key_usuario"))
                .put("key_caja", caja.getString("key"))
                .put("key_punto_venta", punto_venta.getString("key"))
                .put("key_sucursal", sucursal.getString("key"))
                .put("key_cuota", cuota.getString("key"));


                
            double amortizar = 0;
            for (int i = 0; i < JSONObject.getNames(data.getJSONObject("tipos_pago")).length; i++) {
                JSONObject tipoPago =  puntoVentaTiposPago.getJSONObject(JSONObject.getNames(data.getJSONObject("tipos_pago"))[i]);

               
                JSONObject moneda = monedas.getJSONObject(tipoPago.getString("key_moneda"));

                JSONObject tipoPagoOriginal = tiposPago.getJSONObject(tipoPago.getString("key_tipo_pago"));

                if(!tipoPagoOriginal.optBoolean("is_credito",false)){
                    amortizar+=tipoPago.getDouble("monto_nacional");
                }
                
                tags.put("key_tipo_pago", tipoPago.getString("key_tipo_pago"));


                String keyCuenta=tipoPago.getString("key_cuenta_contable");
                if(tipoPago.optBoolean("pasa_por_caja",false)){
                    keyCuenta = caja.getString("key_cuenta_contable");
                }

                //double montoBase = tipoPago.getDouble("monto")/moneda.optDouble("tipo_cambio",1);
                double montoTc = tipoPago.getDouble("monto_nacional");
                montoTc = Math.round(montoTc * 100.0) / 100.0; // Redondear a dos decimales
                asiento.setDetalle(new AsientoContableDetalle(
                    keyCuenta,
                    "Amortizacion de cuota", 
                    "debe", 
                    tipoPago.optDouble("monto_nacional"),
                    tipoPago.optDouble("monto_extranjera"),
                    tags));
            }
            tags.remove("key_tipo_pago");

            JSONObject asientoContable = asiento.enviar();
            
            for (int i = 0; i < JSONObject.getNames(data.getJSONObject("tipos_pago")).length; i++) {

                JSONObject puntoVentaTipoPago =  puntoVentaTiposPago.getJSONObject(JSONObject.getNames(data.getJSONObject("tipos_pago"))[i]);

                JSONObject moneda= monedas.getJSONObject(puntoVentaTipoPago.getString("key_moneda"));

                double dmonto = puntoVentaTipoPago.getDouble("monto_extranjera");
                dmonto = Math.round(dmonto * 100.0) / 100.0; // Redondear a dos decimales

                CompraVenta.setDetalleCaja(data.getString("key_caja"), puntoVentaTipoPago.getString("key_tipo_pago"), dmonto, "Venta Rapida", "venta_rapida",asientoContable.getString("key"),asientoContable.getString("codigo"), data.getString("key_usuario"), tags, puntoVentaTipoPago.getString("key_moneda"), moneda.getDouble("tipo_cambio"), puntoVentaTipoPago.getString("key_tipo_pago"));
            }

            //CompraVenta.setDetalleCaja(data.getString("key_caja"), tipoPago.getString("key"), data.getDouble("monto")*-1, "Amotizar cuota", "amortizar_cuota", asientoContable.getString("key"), asientoContable.getString("codigo"), data.getString("key_usuario"), tags, moneda.getString("key"), moneda.optDouble("tipo_cambio",1), tipoPago.getString("key_tipo_pago"));
                

            

            data.put("estado", "exito");
        

        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    public static void editar(JSONObject obj, SSSessionAbstract session) {
        try {
            JSONObject aux = getByKey(obj.getJSONObject("data").getString("key"));
            aux.put("key_compra_venta", JSONObject.getNames(aux)[0]);
            aux.put("key", SUtil.uuid());
            aux.put("fecha_on", SUtil.now());
            SPGConect.insertArray(COMPONENT+"_historico", new JSONArray().put(aux));

            JSONObject data = obj.getJSONObject("data");
            SPGConect.editObject(COMPONENT, data);
            obj.put("data", data);
            obj.put("estado", "exito");
            obj.put("sendAll", true);


        } catch (Exception e) {
            obj.put("estado", "error");
            obj.put("error", e.getMessage());
            e.printStackTrace();
        }
    }

}
