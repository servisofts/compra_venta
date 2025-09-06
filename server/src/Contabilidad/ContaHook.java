package Contabilidad;

import org.json.JSONArray;
import org.json.JSONObject;
import SocketCliente.SocketCliente;

public class ContaHook {

    public static JSONObject getMoneda(String key_empresa, String key_moneda){
        JSONObject send = new JSONObject();
        send.put("service", "empresa");
        send.put("component", "empresa_moneda");
        send.put("type", "getAll");
        send.put("key_empresa", key_empresa);
        JSONObject resp = SocketCliente.sendSinc("empresa", send);
        resp = resp.getJSONObject("data");

        for (String key : resp.keySet()) {
            JSONObject item = resp.getJSONObject(key);
            if (item.getString("key").equals(key_moneda)) {
                return item;
            }
        }
        return resp;
    }

    public static JSONObject getMonedas(String key_empresa){
        JSONObject send = new JSONObject();
        send.put("service", "empresa");
        send.put("component", "empresa_moneda");
        send.put("type", "getAll");
        send.put("key_empresa", key_empresa);
        JSONObject resp = SocketCliente.sendSinc("empresa", send);
        return resp.getJSONObject("data");

    }

    public static JSONObject getAjusteEmpresa(String key_empresa, String key_ajuste) {
        JSONObject send = new JSONObject();
        send.put("component", "ajuste_empresa");
        send.put("type", "getByKeyAjuste");
        send.put("key_empresa", key_empresa);
        send.put("key_ajuste", key_ajuste);
        JSONObject resp = SocketCliente.sendSinc("contabilidad", send);
        return resp.getJSONObject("data");
    }
    
    public static JSONObject getTipoPago(String key_tipo_pago) {
        JSONObject send = new JSONObject();
        send.put("component", "tipo_pago");
        send.put("type", "getByKey");
        send.put("key", key_tipo_pago);
        JSONObject resp = SocketCliente.sendSinc("empresa", send);
        return resp.getJSONObject("data");
    }
    

    public static JSONObject puntoVentaTipoPago(String key_punto_venta_tipo_pago) {
        JSONObject send = new JSONObject();
        send.put("component", "punto_venta_tipo_pago");
        send.put("type", "getByKey");
        send.put("key", key_punto_venta_tipo_pago);
        JSONObject resp = SocketCliente.sendSinc("empresa", send);
        return resp.getJSONObject("data");
    }

    public static JSONObject puntoVentaTipoPago(String key_punto_venta, String key_tipo_pago) {
        JSONObject send = new JSONObject();
        send.put("component", "punto_venta_tipo_pago");
        send.put("type", "getAll");
        send.put("key_punto_venta", key_punto_venta);
        JSONObject resp = SocketCliente.sendSinc("empresa", send);
        resp = resp.getJSONObject("data");

        for (String key : resp.keySet()) {
            JSONObject item = resp.getJSONObject(key);
            if (item.getString("key_tipo_pago").equals(key_tipo_pago)) {
                return item;
            }
        }
        return resp;
    }

    public static JSONObject puntosVentaTipoPago(String key_punto_venta) {
        JSONObject send = new JSONObject();
        send.put("component", "punto_venta_tipo_pago");
        send.put("type", "getAll");
        send.put("key_punto_venta", key_punto_venta);
        JSONObject resp = SocketCliente.sendSinc("empresa", send);
        return resp.getJSONObject("data");

    }

    public static JSONObject tiposPago() {
        JSONObject send = new JSONObject();
        send.put("component", "tipo_pago");
        send.put("type", "getAll");
        JSONObject resp = SocketCliente.sendSinc("empresa", send);
        return resp.getJSONObject("data");

    }
   
    public static JSONObject puntoVentaTipoPago(String key_punto_venta, String key_tipo_pago, String key_moneda) {
        JSONObject send = new JSONObject();
        send.put("component", "punto_venta_tipo_pago");
        send.put("type", "getAll");
        send.put("key_punto_venta", key_punto_venta);
        JSONObject resp = SocketCliente.sendSinc("empresa", send);
        resp = resp.getJSONObject("data");

        for (String key : resp.keySet()) {
            JSONObject item = resp.getJSONObject(key);
            if (item.getString("key_tipo_pago").equals(key_tipo_pago) && item.getString("key_moneda").equals(key_moneda)) {
                return item;
            }
        }
        return resp;
    }
}
