package Component;


import org.json.JSONArray;
import org.json.JSONObject;
import Servisofts.SPGConect;
import Servisofts.SUtil;
import Server.SSSAbstract.SSSessionAbstract;

public class CompraVentaDetalle {
    public static final String COMPONENT = "compra_venta_detalle";

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
            case "comprasSinRecepcionar":
                comprasSinRecepcionar(obj, session);
                break;
        }
    }

    public static void getAll(JSONObject obj, SSSessionAbstract session) {
        try {
            String consulta = "select get_all('" + COMPONENT + "', 'key_compra_venta', '"+obj.getString("key_compra_venta")+"') as json";
            JSONObject data = SPGConect.ejecutarConsultaObject(consulta);
            obj.put("data", data);
            obj.put("estado", "exito");
        } catch (Exception e) {
            obj.put("estado", "error");
            obj.put("error", e.getMessage());
            e.printStackTrace();
        }
    }

    

    public static int getCantidadCompraProductosDisponibles(String key_compra_venta_detalle) {
        try {
            String consulta = "select compra_venta_detalle_pendentes('" + key_compra_venta_detalle + "') as json";
            return SPGConect.ejecutarConsultaInt(consulta);
            
        } catch (Exception e) {
            e.printStackTrace();
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
            JSONObject compraVentaParticipantes = CompraVentaParticipante.getAll(data.getString("key_compra_venta"));
            JSONArray key_usuarios = new JSONArray();
            for (int i = 0; i < JSONObject.getNames(compraVentaParticipantes).length; i++) {
                key_usuarios.put(compraVentaParticipantes.getJSONObject(JSONObject.getNames(compraVentaParticipantes)[i]).getString("key_usuario_participante"));
            } 

            obj.put("sendUsers", key_usuarios);
            
        } catch (Exception e) {
            obj.put("estado", "error");
            obj.put("error", e.getMessage());
            e.printStackTrace();
        }
    }

    public static void comprasSinRecepcionar(JSONObject obj, SSSessionAbstract session) {
        try {
            String consulta = "select compras_sin_recepcionar('" + obj.getString("key_sucursal") + "') as json";
            JSONObject data = SPGConect.ejecutarConsultaObject(consulta);
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

            /*JSONObject aux = getByKey(obj.getJSONObject("data").getString("key"));
            aux.put("key_compra_venta", JSONObject.getNames(aux)[0]);
            aux.put("key", SUtil.uuid());
            SPGConect.insertArray(COMPONENT+"_historico", new JSONArray().put(aux));*/

            JSONObject data = obj.getJSONObject("data");
            SPGConect.editObject(COMPONENT, data);
            obj.put("data", data);

            obj.put("estado", "exito");
            JSONObject compraVentaParticipantes = CompraVentaParticipante.getAll(data.getString("key_compra_venta"));
            JSONArray key_usuarios = new JSONArray();
            for (int i = 0; i < JSONObject.getNames(compraVentaParticipantes).length; i++) {
                key_usuarios.put(compraVentaParticipantes.getJSONObject(JSONObject.getNames(compraVentaParticipantes)[i]).getString("key_usuario_participante"));
            } 

            obj.put("sendUsers", key_usuarios);
        } catch (Exception e) {
            obj.put("estado", "error");
            obj.put("error", e.getMessage());
            e.printStackTrace();
        }
    }

}
