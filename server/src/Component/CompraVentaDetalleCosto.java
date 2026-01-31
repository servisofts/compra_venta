package Component;


import org.json.JSONArray;
import org.json.JSONObject;

import Component.CompraVenta_Components.CompraVentaCaja;
import Contabilidad.ContaHook;
import Servisofts.SPGConect;
import Servisofts.SUtil;
import Servisofts.Server.SSSAbstract.SSSessionAbstract;
import Servisofts.SocketCliente.SocketCliente;

public class CompraVentaDetalleCosto {
    public static final String COMPONENT = "compra_venta_detalle_costo";

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
            case "generarCompra":
                generarCompra(obj, session);
                break;
        }
    }

    public static void getAll(JSONObject obj, SSSessionAbstract session) {
        try {
            String consulta = "select get_compra_venta_costos('" + obj.getString("key_empresa") + "') as json";
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
            JSONObject data = obj.getJSONObject("data");

            double monto = Double.parseDouble(data.getString("monto") + "");
            if(monto <= 0) {
                obj.put("estado", "error");
                obj.put("error", "El monto debe ser mayor o igual a 1");
                return ;
            }

            
            data.put("key", SUtil.uuid());
            data.put("estado", 1);
            data.put("state", "cotizacion");
            data.put("fecha_on", SUtil.now());
            data.put("key_usuario", obj.getString("key_usuario"));

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

    public static void editar(JSONObject obj, SSSessionAbstract session) {
        try {
            JSONObject data = obj.getJSONObject("data");
           
            SPGConect.editObject(COMPONENT, data);
            obj.put("data", data);
            obj.put("estado", "exito");
        } catch (Exception e) {
            obj.put("estado", "error");
            obj.put("error", e.getMessage());
            e.printStackTrace();
        }
    }

    public static void generarCompra(JSONObject obj, SSSessionAbstract session) {
        try {
            

            String key_costo = obj.optString("key_costo");
            JSONObject costo = getByKey(key_costo);


            JSONObject data = new JSONObject();

            JSONObject compraVentaDetalle = CompraVentaDetalle.getByKey(costo.getString("key_compra_venta_detalle"));
            JSONObject compraVenta = CompraVenta.getByKey(compraVentaDetalle.getString("key_compra_venta"));


            JSONObject send = new JSONObject();
            send.put("component", "caja");
            send.put("type", "getByKey");
            send.put("key", compraVenta.getString("key_caja"));
            JSONObject resp = SocketCliente.sendSinc("caja", send);
            JSONObject caja = resp.getJSONObject("data");

            send = new JSONObject();
            send.put("component", "modelo_cliente");
            send.put("type", "getByKey");
            send.put("key", costo.getString("key_costo"));
            resp = SocketCliente.sendSinc("inventario", send);
            JSONObject modeloCliente = resp.getJSONObject("data");

            send = new JSONObject();
            send.put("component", "tipo_costo");
            send.put("type", "getByKey");
            send.put("key", modeloCliente.getString("key_tipo_costo"));
            resp = SocketCliente.sendSinc("inventario", send);
            JSONObject tipoCosto = resp.getJSONObject("data");
            
            send = new JSONObject();
            send.put("component", "empresa_tipo_pago");
            send.put("type", "getAll");
            send.put("key_empresa", caja.getString("key_empresa"));
            resp = SocketCliente.sendSinc("caja", send);
            JSONObject empresaTipoPago = resp.getJSONObject("data");


            JSONObject moneda = ContaHook.getMonedaBase(caja.getString("key_empresa"));



            data.put("key_caja", caja.getString("key"));
            data.put("caja", caja);
            data.put("key_moneda", moneda.getString("key"));
            data.put("key_compra_venta", SUtil.uuid());
            data.put("key_usuario", caja.getString("key_usuario"));
            data.put("descripcion", costo.optString("descripcion"));
            data.put("observacion", "Generado desde costo");
            data.put("key_proveedor", modeloCliente.getString("key_cliente"));
            data.put("key_cliente", "");
            data.put("facturar", false);

            



            
            data.put("detalle", new JSONArray().put(new JSONObject()
                .put("key_modelo", modeloCliente.getString("key_modelo"))
                .put("cantidad", 1)
                .put("precio_unitario", costo.optDouble("monto"))
                .put("precio_unitario_base", costo.optDouble("monto"))
                .put("key_modelo_cliente", modeloCliente.getString("key"))
            ));

            JSONObject emptipoPago = new JSONObject();
            String keyEmpresaTipoPago = JSONObject.getNames(empresaTipoPago)[0];
            JSONObject empresaTP = empresaTipoPago.getJSONObject(keyEmpresaTipoPago);

            send = new JSONObject();
            send.put("component", "tipo_pago");
            send.put("type", "getByKey");
            send.put("key", empresaTP.getString("key_tipo_pago"));
            resp = SocketCliente.sendSinc("caja", send);
            JSONObject tipoPago = resp.getJSONObject("data");

            tipoPago.put("empresa_tipo_pago", empresaTP);
            tipoPago.put("monto_nacional", costo.optDouble("monto"));
            tipoPago.put("monto_extranjera", 0);

            emptipoPago.put(keyEmpresaTipoPago, tipoPago);

            data.put("tipos_pago", emptipoPago);

            JSONObject sendS = new JSONObject();
            sendS.put("data", data);

            new CompraVentaCaja(sendS,null,"compra");
            

            SPGConect.editObject(COMPONENT, costo);
            obj.put("data", costo);
            obj.put("estado", "exito");
        } catch (Exception e) {
            obj.put("estado", "error");
            obj.put("error", e.getMessage());
            e.printStackTrace();
        }
    }

    

}
