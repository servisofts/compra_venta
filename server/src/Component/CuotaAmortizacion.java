package Component;

import org.json.JSONArray;
import org.json.JSONObject;
import Servisofts.SPGConect;
import Servisofts.SUtil;
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

            if(data.getString("estado").equals("error")){
                obj.put("estado", "error");
                obj.put("error", data.getString("error"));
            }

            
            
           
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
                cuota.put("estado", 2);               
                SPGConect.editObject("cuota", cuota);   
            }

            

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
