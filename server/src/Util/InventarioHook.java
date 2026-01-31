package Util;

import org.json.JSONObject;
import Servisofts.SocketCliente.SocketCliente;

public class InventarioHook {

    public static JSONObject getModeloCliente(String key){
        JSONObject send = new JSONObject();
        send.put("component", "modelo_cliente");
        send.put("type", "getByKey");
        send.put("key", key);
        JSONObject resp = SocketCliente.sendSinc("inventario", send);
        resp = resp.getJSONObject("data");

        return resp;
    }

    public static JSONObject getKeyCuentaContableModeloCliente(String key_costo){
        JSONObject send = new JSONObject();
        send.put("component", "modelo_cliente");
        send.put("type", "getByKey");
        send.put("key", key_costo);
        JSONObject resp = SocketCliente.sendSinc("inventario", send);
        resp = resp.getJSONObject("data");

        return resp;
    }

}
