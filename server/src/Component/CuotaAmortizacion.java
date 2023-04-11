package Component;

import org.json.JSONArray;
import org.json.JSONObject;
import Servisofts.SPGConect;
import Servisofts.SUtil;
import Server.SSSAbstract.SSSessionAbstract;

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
            data.put("key", SUtil.uuid());
            data.put("estado", 1);
            data.put("fecha_on", SUtil.now());
            data.put("key_usuario", obj.getString("key_usuario"));

            SPGConect.insertArray(COMPONENT, new JSONArray().put(data));
            
            JSONArray cuentas = new JSONArray();
            for (int i = 0; i < data.getJSONArray("key_cuotas").length(); i++) {
                intentarAmortizar(data.getJSONArray("key_cuotas").getString(i), cuentas);
            }

            
            obj.put("cuentas", cuentas);
           // obj.put("data", data);
            obj.put("estado", "exito");
        } catch (Exception e) {
            obj.put("estado", "error");
            obj.put("error", e.getMessage());
            e.printStackTrace();
        }
    } 

    public static void intentarAmortizar(String key_cuota, JSONArray cuentas){
        try {

            JSONObject cuota = Cuota.getByKey(key_cuota);
            double amortizaciones = CuotaAmortizacion.getAmortizacioens(key_cuota);


            if(cuota.getDouble("monto")>=amortizaciones){
                System.out.println("amortizar");

                JSONObject compra_venta = CompraVenta.getByKey(cuota.getString("key_compra_venta"));
                JSONObject compra_venta_detalles = CompraVentaDetalle.getPorcentajes(cuota.getString("key_compra_venta"));
                JSONObject compra_venta_detalle;
                double monto_a_pagar;
                String cuenta_contable;

                
                JSONObject _send;
                for (int i = 0; i < JSONObject.getNames(compra_venta_detalles).length; i++) {
                    compra_venta_detalle = compra_venta_detalles.getJSONObject(JSONObject.getNames(compra_venta_detalles)[i]);
                    monto_a_pagar = cuota.getDouble("monto")*(compra_venta_detalle.getDouble("porcentaje")/100);
                    
                    if(compra_venta.getString("tipo_pago").equals("contado")){
                        cuenta_contable = compra_venta_detalle.getString("key_cuenta_contable_contado");
                    }else{
                        cuenta_contable = compra_venta_detalle.getString("key_cuenta_contable_credito");
                    }  
                    _send = new JSONObject();
                    _send.put("key_cuenta_contable", cuenta_contable);
                    _send.put("monto", monto_a_pagar);
                    _send.put("key_compra_venta_detalle", compra_venta_detalle.getString("key"));
                    cuentas.put(_send);
                }
                

                cuota.put("estado", 2);
                SPGConect.editObject("cuota", cuota);

            }else{
                System.out.println("no amortizar");
            }
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
