package Component;

import org.json.JSONArray;
import org.json.JSONObject;
import Servisofts.SPGConect;
import Servisofts.SUtil;
import Server.SSSAbstract.SSSessionAbstract;

public class Cuota {
    public static final String COMPONENT = "cuota";

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
            case "getPendientes":
                getPendientes(obj, session);
                break;
            case "registroAll":
                registroAll(obj, session);
                break;
            case "editar":
                editar(obj, session);
                break;
        }
    }

    public static void getAll(JSONObject obj, SSSessionAbstract session) {
        try {
            String consulta = "";
            if(obj.has("key_compra_venta")) consulta = "select get_all('" + COMPONENT + "', 'key_compra_venta', '"+obj.getString("key_compra_venta")+"') as json";
            if(obj.has("key_cliente")) consulta = "select get_cuotas('"+obj.getString("key_cliente")+"','"+obj.getString("key_empresa")+"') as json";
            if(obj.has("key_proveedor")) consulta = "select get_cuotas_proveedor('"+obj.getString("key_proveedor")+"', '"+obj.getString("key_empresa")+"') as json";
            JSONObject data = SPGConect.ejecutarConsultaObject(consulta);
            obj.put("data", data);
            obj.put("estado", "exito");
        } catch (Exception e) {
            obj.put("estado", "error");
            obj.put("error", e.getMessage());
            e.printStackTrace();
        }
    }

    public static void getPendientes(JSONObject obj, SSSessionAbstract session) {
            try {
                String consulta = "select get_cobranzas() as json";
                if(obj.has("key_empresa")){
                    consulta = "select get_cobranzas('"+obj.get("key_empresa")+"') as json";
                }
                JSONObject data = SPGConect.ejecutarConsultaObject(consulta);
                obj.put("data", data);
                obj.put("estado", "exito");
            } catch (Exception e) {
                obj.put("estado", "error");
                obj.put("error", e.getMessage());
                e.printStackTrace();
            }
        }

    public static JSONObject getAll(String key_compra_venta) {
        try {
            String consulta = "select get_all('" + COMPONENT + "', 'key_compra_venta', '"+key_compra_venta+"') as json";
            return SPGConect.ejecutarConsultaObject(consulta);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
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
            data.put("state", "cotizacion");
            data.put("fecha_on", SUtil.now());
            data.put("key_usuario", obj.getString("key_usuario"));
            data.put("key_servicio", obj.getJSONObject("servicio").getString("key"));

            if(obj.has("key_sucursal")){
                data.put("key_sucursal", obj.getString("key_sucursal"));    
            }
            SPGConect.insertArray(COMPONENT, new JSONArray().put(data));
            obj.put("data", data);
            obj.put("estado", "exito");
        } catch (Exception e) {
            obj.put("estado", "error");
            obj.put("error", e.getMessage());
            e.printStackTrace();
        }
    } 

    public static void registroAll(JSONObject obj, SSSessionAbstract session) {
        try {

            String consulta = "select anular('" + COMPONENT + "', 'key_compra_venta', '" + obj.getString("key_compra_venta") + "') as json";
            SPGConect.ejecutar(consulta);

            JSONArray data = obj.getJSONArray("data");
            JSONObject cuota;
            for (int i = 0; i < data.length(); i++) {
                cuota = data.getJSONObject(i);
                cuota.put("key", SUtil.uuid());
                cuota.put("estado", 1);
                cuota.put("key_usuario", obj.getString("key_usuario"));
                cuota.put("key_compra_venta", obj.getString("key_compra_venta"));
                cuota.put("fecha_on", SUtil.now());
            }
            
            SPGConect.insertArray(COMPONENT, data);
            
            JSONObject compra_venta = new JSONObject();
            compra_venta.put("key", obj.getString("key_compra_venta"));
            compra_venta.put("periodicidad_medida", obj.getString("periodicidad_medida"));
            compra_venta.put("periodicidad_valor", obj.getInt("periodicidad_valor"));
            compra_venta.put("porcentaje_interes", obj.getInt("porcentaje_interes"));
            compra_venta.put("tipo_pago", obj.getString("tipo_pago"));
            SPGConect.editObject("compra_venta", compra_venta);


/* 
            if(obj.getString("tipo_pago").equals("contado")){
                compra_venta.getJSONObject("data").getString("key_cuenta_contable_contado");
            }else{
                compra_venta.getJSONObject("data").getString("key_cuenta_contable_credito");
            }
*/
            obj.put("data", data);
            obj.put("estado", "exito");
            obj.put("sendAll", true);
        } catch (Exception e) {
            obj.put("estado", "error");
            obj.put("error", e.getMessage());
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
