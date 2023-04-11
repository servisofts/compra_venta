package Component;


import org.json.JSONArray;
import org.json.JSONObject;
import Servisofts.SPGConect;
import Servisofts.SUtil;
import Server.SSSAbstract.SSSessionAbstract;

public class CompraVentaDetalleProducto {
    public static final String COMPONENT = "compra_venta_detalle_producto";

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
            case "registroExcel":
                registroExcel(obj, session);
                break;
            case "editar":
                editar(obj, session);
                break;
        }
    }

    public static void getAll(JSONObject obj, SSSessionAbstract session) {
        try {

            String consulta = "select get_compra_venta_detalle_productos('" + obj.getString("key_compra_venta") + "') as json";
            JSONObject data =  SPGConect.ejecutarConsultaObject(consulta);

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

            if(CompraVentaDetalle.getCantidadCompraProductosDisponibles(obj.getJSONObject("data").getString("key_compra_venta_detalle"))<=0){
                obj.put("estado", "error");
                obj.put("error", "No existen productos pendientes de recepcionar");
                return ;
            }

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
            obj.put("sendAll", true );
        } catch (Exception e) {
            obj.put("estado", "error");
            obj.put("error", e.getMessage());
            e.printStackTrace();
        }
    }
    public static void registroExcel(JSONObject obj, SSSessionAbstract session) {
        try {
            JSONArray keys = obj.getJSONArray("keys");
            if(CompraVentaDetalle.getCantidadCompraProductosDisponibles(obj.getString("key_compra_venta_detalle"))<keys.length()){
                obj.put("estado", "error");
                obj.put("error", "No existen productos pendientes de recepcionar");
                return ;
            }

            JSONArray cvdp = new JSONArray();
            JSONObject data;
            for (int i = 0; i < keys.length(); i++) {
                data = new JSONObject();
                data.put("key", SUtil.uuid());
                data.put("estado", 1);
                data.put("fecha_on", SUtil.now());
                data.put("key_usuario", obj.getString("key_usuario"));
                data.put("key_compra_venta_detalle", obj.getString("key_compra_venta_detalle"));
                data.put("key_producto", keys.getString(i));
                cvdp.put(data);
            }
            

            SPGConect.insertArray(COMPONENT, cvdp);
            obj.put("data", cvdp);
            obj.put("estado", "exito");
            //obj.put("sendAll", true );
        } catch (Exception e) {
            obj.put("estado", "error");
            obj.put("error", e.getMessage());
            e.printStackTrace();
        }
    }

    public static JSONObject registro(String key_compra_venta_detalle, String key_usuario, String key_sucursal, String key_producto) {
        try {

            JSONObject compraVentaDetalle = CompraVentaDetalle.getByKey(key_compra_venta_detalle);
            JSONObject compraVenta = CompraVenta.getByKey(compraVentaDetalle.getString("key_compra_venta"));

            JSONObject data = new JSONObject();
            data.put("key", SUtil.uuid());
            data.put("estado", 1);
            data.put("fecha_on", SUtil.now());
            data.put("key_usuario", key_usuario);
            data.put("key_sucursal", key_sucursal);
            data.put("key_compra_venta_detalle", compraVentaDetalle.getString("key"));



            if(compraVenta.getString("tipo").equals("compra")){
                if(CompraVentaDetalle.getCantidadCompraProductosDisponibles(key_compra_venta_detalle)<=0){
                    return null;
                }
            }else{
                data.put("key_producto", key_producto);
            }

            SPGConect.insertArray(COMPONENT, new JSONArray().put(data));

            return data;
            
            
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
    

    public static void editar(JSONObject obj, SSSessionAbstract session) {
        try {
            JSONObject data = obj.getJSONObject("data");
            if(obj.has("fecha_off")){
                data.put("fecha_off", SUtil.now()); 
            }
            SPGConect.editObject(COMPONENT, data);
            obj.put("data", data);
            obj.put("estado", "exito");
        } catch (Exception e) {
            obj.put("estado", "error");
            obj.put("error", e.getMessage());
            e.printStackTrace();
        }
    }

}
