package Component;

import java.util.UUID;

import org.json.JSONArray;
import org.json.JSONObject;

import com.google.gson.JsonObject;

import Servisofts.SPGConect;
import Servisofts.SUtil;
import Server.SSSAbstract.SSServerAbstract;
import Server.SSSAbstract.SSSessionAbstract;

public class CompraVenta {
    public static final String COMPONENT = "compra_venta";

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
            String consulta = "select get_all_compra_venta('"+obj.getString("key_usuario")+"') as json";
            JSONObject data = SPGConect.ejecutarConsultaObject(consulta);
            obj.put("data", data);
            obj.put("estado", "exito");
        } catch (Exception e) {
            obj.put("estado", "error");
            obj.put("error", e.getMessage());
            e.printStackTrace();
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

            JSONObject compra_venta_participante = new JSONObject();
            compra_venta_participante.put("key", SUtil.uuid());
            compra_venta_participante.put("estado", 1);
            compra_venta_participante.put("fecha_on", SUtil.now());
            compra_venta_participante.put("key_usuario", obj.getString("key_usuario"));
            compra_venta_participante.put("key_compra_venta", data.getString("key"));
            compra_venta_participante.put("key_usuario_participante", obj.getString("key_usuario"));
            compra_venta_participante.put("tipo", "admin");
            SPGConect.insertArray("compra_venta_participante", new JSONArray().put(compra_venta_participante));

            obj.put("data", data);
            obj.put("estado", "exito");
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

            if(data.getString("state").equals("comprado")){
                obj.put("sendAll", true);
            }else{
                JSONObject compraVentaParticipantes = CompraVentaParticipante.getAll(data.getString("key"));
                JSONArray key_usuarios = new JSONArray();
                for (int i = 0; i < JSONObject.getNames(compraVentaParticipantes).length; i++) {
                    key_usuarios.put(compraVentaParticipantes.getJSONObject(JSONObject.getNames(compraVentaParticipantes)[i]).getString("key_usuario_participante"));
                } 

                obj.put("sendUsers", key_usuarios);
            }


        } catch (Exception e) {
            obj.put("estado", "error");
            obj.put("error", e.getMessage());
            e.printStackTrace();
        }
    }


}
