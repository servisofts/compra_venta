package Component;

import org.json.JSONArray;
import org.json.JSONObject;

import Contabilidad.Contabilidad;
import Servisofts.SPGConect;
import Servisofts.SUtil;
import SocketCliente.SocketCliente;
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
            case "getClientes":
                getClientes(obj, session);
                break;
            case "getClientesDeudores":
                getClientesDeudores(obj, session);
                break;
            case "getClientesMorosos":
                getClientesMorosos(obj, session);
                break;
            case "getDeudaProveedores":
                getDeudaProveedores(obj, session);
                break;
            case "getByKeyCliente":
                getByKeyCliente(obj, session);
                break;
            case "getStates":
                getStates(obj, session);
                break;
            case "registro":
                registro(obj, session);
                break;
            case "editar":
                editar(obj, session);
                break;
            case "pdf":
                pdf(obj, session);
                break;
            case "generarAsientoContable":
                generarAsientoContable(obj, session);
                break;
        }
    }

    public static void getAll(JSONObject obj, SSSessionAbstract session) {
        try {
            String consulta = "select get_all_compra_venta('" + obj.getString("key_empresa") + "') as json";
            JSONObject data = SPGConect.ejecutarConsultaObject(consulta);
            obj.put("data", data);
            obj.put("estado", "exito");
        } catch (Exception e) {
            obj.put("estado", "error");
            obj.put("error", e.getMessage());
            e.printStackTrace();
        }
    }

    public static void getStates(JSONObject obj, SSSessionAbstract session) {
        try {
            // String consulta = "select
            // get_compras_ventas('"+obj.getString("key_sucursal")+"') as json";
            String consulta = "select get_compras_ventas('" + obj.getString("key_sucursal") + "', '"
                    + obj.getString("fecha_inicio") + "', '" + obj.getString("fecha_fin") + "') as json";
            JSONObject data = SPGConect.ejecutarConsultaObject(consulta);
            obj.put("data", data);
            obj.put("estado", "exito");
        } catch (Exception e) {
            obj.put("estado", "error");
            obj.put("error", e.getMessage());
            e.printStackTrace();
        }
    }

    public static void getClientes(JSONObject obj, SSSessionAbstract session) {
        try {
            String consulta = "select get_clientes('"+obj.getString("key_empresa")+"') as json";
            JSONObject data = SPGConect.ejecutarConsultaObject(consulta);
            obj.put("data", data);
            obj.put("estado", "exito");
        } catch (Exception e) {
            obj.put("estado", "error");
            obj.put("error", e.getMessage());
            e.printStackTrace();
        }
    }

    public static void getClientesDeudores(JSONObject obj, SSSessionAbstract session) {
        try {
            String consulta = "select get_clientes_deudores() as json";
            if (obj.has("key_empresa")) {
                consulta = "select get_clientes_deudores('" + obj.get("key_empresa") + "') as json";
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

    public static void getClientesMorosos(JSONObject obj, SSSessionAbstract session) {
        try {
            String consulta = "select get_clientes_morosos() as json";
            JSONObject data = SPGConect.ejecutarConsultaObject(consulta);
            obj.put("data", data);
            obj.put("estado", "exito");
        } catch (Exception e) {
            obj.put("estado", "error");
            obj.put("error", e.getMessage());
            e.printStackTrace();
        }
    }

    public static void getDeudaProveedores(JSONObject obj, SSSessionAbstract session) {
        try {

            String consulta = "select get_deuda_proveedores() as json";
            if (obj.has("key_empresa")) {
                consulta = "select get_deuda_proveedores('" + obj.get("key_empresa") + "') as json";
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

    public static boolean verificarProductosVigentes(String key_compra_venta) {
        try {
            String consulta = "select get_compra_venta_detalle_productos('" + key_compra_venta + "') as json";
            JSONObject compra_venta_detalle_productos = SPGConect.ejecutarConsultaObject(consulta);

            JSONObject cpdp;

            JSONObject venta;

            boolean vigentes = true;

            for (int i = 0; i < JSONObject.getNames(compra_venta_detalle_productos).length; i++) {
                cpdp = compra_venta_detalle_productos
                        .getJSONObject(JSONObject.getNames(compra_venta_detalle_productos)[i]);
                consulta = "select is_producto_vendido('" + cpdp.getString("key_producto") + "') as json";
                venta = SPGConect.ejecutarConsultaObject(consulta);
                if (!venta.isEmpty()) {
                    vigentes = false;
                }
            }

            return vigentes;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
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

    public static void getByKeyCliente(JSONObject obj, SSSessionAbstract session) {
        try {
            String consulta = "select ventas_cliente('" + obj.getString("key_cliente") + "') as json";
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

            if (obj.has("key_sucursal")) {
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

            JSONObject data_ = new JSONObject();
            String tipo_registro_icon;
            if (data.getString("tipo").equals("compra")) {
                data_.put("url", "/compra/profile?pk=" + data.getString("key"));
                data_.put("tipo", "compra");
                tipo_registro_icon = "🛍️";
            } else {
                data_.put("url", "/venta/profile?pk=" + data.getString("key"));
                data_.put("tipo", "venta");
                tipo_registro_icon = "🏷️";
            }

            Notificar.send(tipo_registro_icon + " Registraste " + data.getString("descripcion"),
                    data.getString("observacion"), data_, obj.getJSONObject("servicio").getString("key"),
                    obj.getString("key_usuario"));

            obj.put("sendAll", true);

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

            JSONObject data = obj.getJSONObject("data");

            
            
            boolean vigentes = true;
            if (data.getString("state").equals("vendido")) {
             //   vigentes = CompraVenta.verificarProductosVigentes(data.getString("key"));
            }

            if (!vigentes) {
                obj.put("estado", "error");
                ;
                obj.put("error", "Algunos productos ya no estan vigentes");
                return;
            }


            SPGConect.editObject(COMPONENT, data);

            obj.put("data", data);
            obj.put("estado", "exito");

            obj.put("sendAll", true);

            /*
             * Para enviar solo a los participantes de la compraventa
             * if(data.getString("state").equals("comprado")){
             * obj.put("sendAll", true);
             * }else{
             * JSONObject compraVentaParticipantes =
             * CompraVentaParticipante.getAll(data.getString("key"));
             * JSONArray key_usuarios = new JSONArray();
             * for (int i = 0; i < JSONObject.getNames(compraVentaParticipantes).length;
             * i++) {
             * key_usuarios.put(compraVentaParticipantes.getJSONObject(JSONObject.getNames(
             * compraVentaParticipantes)[i]).getString("key_usuario_participante"));
             * }
             * 
             * obj.put("sendUsers", key_usuarios);
             * }
             */

        } catch (Exception e) {
            obj.put("estado", "error");
            obj.put("error", e.getMessage());
            e.printStackTrace();
        }
    }

    public static void pdf(JSONObject obj, SSSessionAbstract session) {
        try {

            new PDF().generarCompraVenta(obj.getString("key_compra_venta"));
            obj.put("data", obj.getString("key_compra_venta"));
            obj.put("estado", "exito");
        } catch (Exception e) {
            obj.put("estado", "error");
            obj.put("error", e.getMessage());
            e.printStackTrace();
        }
    }

    public static JSONObject generarComprobanteCompra(JSONObject compraVenta){
        JSONObject compraVentaDetalle = CompraVentaDetalle.getAll(compraVenta.getString("key"));
        JSONObject send = new JSONObject();
        send.put("component", "asiento_contable");
        send.put("type", "set");
        send.put("key_usuario", "set");
        send.put("key_empresa", compraVenta.getString("key_empresa"));

        JSONObject comprobante = new JSONObject();
        comprobante.put("tipo", "traspaso");
        comprobante.put("fecha", compraVenta.getString("fecha_on").substring(0, 10));
        comprobante.put("descripcion", compraVenta.getString("tipo")+": "+compraVenta.getString("descripcion"));
        comprobante.put("observacion", compraVenta.getString("observacion"));

        JSONArray detalle = new JSONArray();
        double suma = 0;

        JSONObject contabilidadEnviroment = Contabilidad.getEnviroment(compraVenta.getString("key_empresa"), "IVA");
        double iva = Double.parseDouble(contabilidadEnviroment.getString("observacion"));

        for (int i = 0; i < JSONObject.getNames(compraVentaDetalle).length; i++) {
            JSONObject cvd = compraVentaDetalle.getJSONObject(JSONObject.getNames(compraVentaDetalle)[i]);
            JSONObject det = new JSONObject();

            // Key_cuenta_detalle_tipo_producto
            det.put("key_cuenta_contable", cvd.getJSONObject("data").getString("key_cuenta_contable"));
            det.put("glosa", cvd.getString("descripcion"));
            double total = (cvd.getDouble("precio_unitario") * cvd.getDouble("cantidad"));
            if (cvd.has("descuento") && !cvd.isNull("descuento")) {
                total -= cvd.getDouble("descuento");
            }


            double monto_iva = 0;
            if(cvd.has("precio_facturado") && cvd.get("precio_facturado") != null && !(cvd.get("precio_facturado")+"").equals("null")){
                monto_iva = Double.parseDouble(cvd.get("precio_facturado")+"")*iva;
            }
            

            det.put("debe", total-monto_iva);
            suma += total;
            detalle.put(det);

            if(monto_iva>0){
                JSONObject ajuste = Contabilidad.getAjusteEmpresa(compraVenta.getString("key_empresa"), "credito_iva");
            
                det = new JSONObject();
                det.put("codigo", ajuste.getString("codigo"));
                det.put("glosa", cvd.getString("descripcion"));
                det.put("debe", monto_iva);
                detalle.put(det);
            }
            

        }


        JSONObject ajusteEmpresa = Contabilidad.getAjusteEmpresa(compraVenta.getString("key_empresa"), "cuentas_por_pagar");

        

        JSONObject det = new JSONObject();
        det.put("codigo", ajusteEmpresa.getString("codigo"));
        det.put("glosa", compraVenta.getString("descripcion"));
        det.put("haber", suma);
        detalle.put(det);

        comprobante.put("detalle", detalle);
        send.put("data", comprobante);

        send = SocketCliente.sendSinc("contabilidad", send);
        send.put("estado", "exito");
        if(send.getString("estado").equals("error")){
            send.put("estado", "error");
            send.put("error", send.getString("error"));
        }
        return send;
    }
    
    public static JSONObject generarComprobanteVenta(JSONObject compraVenta){
        JSONObject compraVentaDetalle = CompraVentaDetalle.getAll(compraVenta.getString("key"));
        JSONObject send = new JSONObject();
        send.put("component", "asiento_contable");
        send.put("type", "set");
        send.put("key_usuario", "set");
        send.put("key_empresa", compraVenta.getString("key_empresa"));

        JSONObject comprobante = new JSONObject();
        comprobante.put("tipo", "traspaso");
        comprobante.put("fecha", compraVenta.getString("fecha_on").substring(0, 10));
        comprobante.put("descripcion", compraVenta.getString("tipo")+": "+compraVenta.getString("descripcion"));
        comprobante.put("observacion", compraVenta.getString("observacion"));

        JSONObject contabilidadEnviroment = Contabilidad.getEnviroment(compraVenta.getString("key_empresa"), "IVA");
        double iva = Double.parseDouble(contabilidadEnviroment.getString("observacion"));

        JSONObject contabilidadEnviromentIt = Contabilidad.getEnviroment(compraVenta.getString("key_empresa"), "IT");
        double it = Double.parseDouble(contabilidadEnviromentIt.getString("observacion"));

        boolean isFacturado = false;

        JSONArray detalle = new JSONArray();
        double suma = 0;
        double sumaFacturado = 0;
        for (int i = 0; i < JSONObject.getNames(compraVentaDetalle).length; i++) {
            JSONObject cvd = compraVentaDetalle.getJSONObject(JSONObject.getNames(compraVentaDetalle)[i]);

            JSONObject ajusteEmpresa = Contabilidad.getAjusteEmpresa(compraVenta.getString("key_empresa"), "cuentas_por_cobrar");

            JSONObject det = new JSONObject();
            det.put("codigo", ajusteEmpresa.getString("codigo"));
            det.put("glosa", compraVenta.getString("descripcion"));
            double total = (cvd.getDouble("precio_unitario") * cvd.getDouble("cantidad"));
            if (cvd.has("descuento") && !cvd.isNull("descuento")) {
                total -= cvd.getDouble("descuento");
            } 
            det.put("debe", total);
            detalle.put(det);



            det = new JSONObject();
            det.put("key_cuenta_contable", cvd.getJSONObject("data").getString("key_cuenta_contable_ganancia"));
            det.put("glosa", cvd.getString("descripcion"));


            double monto_iva = 0;
            double precio_facturado = 0;

            if(cvd.has("precio_facturado") && !cvd.isNull("precio_facturado")){
                try{
                    precio_facturado = cvd.getDouble("precio_facturado");
                    if(precio_facturado>0) isFacturado = true;
                }catch(Exception e){}
                
            }
            
            monto_iva = precio_facturado*iva;
            

            det.put("haber", total-monto_iva);
            detalle.put(det);

            if(monto_iva>0){
                JSONObject ajuste = Contabilidad.getAjusteEmpresa(compraVenta.getString("key_empresa"), "debito_iva");
            
                det = new JSONObject();
                det.put("codigo", ajuste.getString("codigo"));
                det.put("glosa", cvd.getString("descripcion"));
                det.put("haber", monto_iva);
                detalle.put(det);
            }
            

            double totalCompra = 0;

            if(cvd.getJSONObject("data").has("precio_compra")){
                totalCompra = cvd.getJSONObject("data").getDouble("precio_compra")*cvd.getDouble("cantidad");

                if(totalCompra>0){
                    JSONObject det1 = new JSONObject();

                    det1 = new JSONObject();
                    // Sacar cuenta 5
                    det1.put("key_cuenta_contable", cvd.getJSONObject("data").getString("key_cuenta_contable_costo"));
                    det1.put("glosa", cvd.getString("descripcion"));
                    det1.put("debe", totalCompra);
                    detalle.put(det1);
                    
                    // Sacar cuenta 1
                    det1 = new JSONObject();
                    det1.put("key_cuenta_contable", cvd.getJSONObject("data").getString("key_cuenta_contable"));
                    det1.put("glosa", cvd.getString("descripcion"));
                    det1.put("haber", totalCompra);
                    detalle.put(det1);

                    
                }
            }
            suma += total;
            sumaFacturado+=precio_facturado;
        }

        
        if(isFacturado){        
        //Credito IT

            JSONObject ajusteEmpresa = Contabilidad.getAjusteEmpresa(compraVenta.getString("key_empresa"), "credito_it");
            JSONObject det = new JSONObject();
            det.put("codigo", ajusteEmpresa.getString("codigo"));
            det.put("glosa", "Impuesto a la transacción");
            det.put("debe", sumaFacturado*it);
            detalle.put(det);

            //Debito IT
            ajusteEmpresa = Contabilidad.getAjusteEmpresa(compraVenta.getString("key_empresa"), "debito_it");
            det = new JSONObject();
            det.put("codigo", ajusteEmpresa.getString("codigo"));
            det.put("glosa", "Impuesto a la transacción");
            det.put("haber", sumaFacturado*it);
            detalle.put(det);

        }

        comprobante.put("detalle", detalle);
        send.put("data", comprobante);

        send = SocketCliente.sendSinc("contabilidad", send);
        return send;
    }


    public static void generarAsientoContable(JSONObject obj, SSSessionAbstract session) {
        try {

            String keyCompraVenta = obj.getString("key_compra_venta");
            // new PDF().generarCompraVenta();
            // obj.put("data", obj.getString("key_compra_venta"));

            JSONObject compraVenta = CompraVenta.getByKey(keyCompraVenta);

            if(compraVenta.getString("tipo").equals("venta")){
                // venta
                compraVenta = generarComprobanteVenta(compraVenta);
            }else{
                // compra
                compraVenta = generarComprobanteCompra(compraVenta);
            }
            obj = compraVenta;
        } catch (Exception e) {
            obj.put("estado", "error");
            obj.put("error", e.getMessage());
            e.printStackTrace();
        }
    }

}
