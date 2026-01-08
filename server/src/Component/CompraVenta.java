package Component;

import java.math.BigDecimal;
import java.sql.SQLException;
import org.json.JSONArray;
import org.json.JSONObject;
import Component.CompraVenta_Components.AmortizarCuotas;
import Component.CompraVenta_Components.AnularVenta;
import Component.CompraVenta_Components.CompraVentaCaja;
import Contabilidad.ContaHook;
import Servisofts.Contabilidad.AsientoContable;
import Servisofts.Contabilidad.AsientoContableDetalle;
import Servisofts.Contabilidad.AsientoContableTipo;
import Servisofts.Contabilidad.Contabilidad;
import Servisofts.SConsole;
import Servisofts.SPGConect;
import Servisofts.SUtil;
import Servisofts.SocketCliente.SocketCliente;
import Util.ConectInstance;
import Util.InventarioHook;
import Servisofts.Server.SSSAbstract.SSSessionAbstract;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.Random;

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
            case "getJson":
                getJson(obj, session);
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
            case "reporte":
                reporte(obj, session);
                break;
            case "ventaRapida":
                ventaRapida(obj, session);
                break;
            case "compraRapida":
                compraRapida(obj, session);
                break;

            // InComponents
            case "amortizarCuotaCompra":
                new AmortizarCuotas(obj, session, "compra");
                break;
            case "anularVenta":
                new AnularVenta(obj, session);
                break;
            case "compraCaja":
                new CompraVentaCaja(obj, session, "compra");
                break;
            case "ventaCaja":
                new CompraVentaCaja(obj, session, "venta");
                break;
            case "factura":
                emitirFacturaVenta(obj, session);
                break;

        }
    }

    public static JSONObject api_tipo_producto(JSONArray arr) {
        JSONObject send = new JSONObject();
        send.put("component", "reporte");
        send.put("type", "execute_function");
        send.put("func", "_get_tipo_producto");
        send.put("params", new JSONArray().put("'" + arr.toString() + "'"));
        JSONObject resp = SocketCliente.sendSinc("inventario", send);
        return resp;
    }

    public static String generar_leyenda(String key_empresa, String key_usuario) throws Exception {

        JSONObject send = new JSONObject();
        send.put("ambiente", "2");
        send.put("component", "siat");
        send.put("estado", "cargando");
        send.put("key_empresa", key_empresa);
        send.put("key_usuario", key_usuario);
        send.put("parametrica", "leyendasFactura");
        send.put("type", "getParametrica");

        JSONObject resp = SocketCliente.sendSinc("facturacion", send);

        // Validar respuesta
        if (!resp.has("data")) {
            throw new Exception("No se encontraron leyendas en la respuesta SIAT");
        }

        JSONArray data = resp.getJSONArray("data");
        if (data.length() == 0) {
            throw new Exception("Lista de leyendas vacía");
        }

        // Elegir leyenda aleatoria
        Random random = new Random();
        JSONObject leyendaObj = data.getJSONObject(random.nextInt(data.length()));

        return leyendaObj.getString("descripcionLeyenda");
    }

    public static void emitirFacturaVenta(JSONObject venta, SSSessionAbstract session) {
        try {
            // ===============================================================
            // 1. OBTENER DATOS PRINCIPALES
            // ===============================================================
            String keyVenta = venta.getString("key");

            JSONObject compraVenta = getByKey(keyVenta);
            JSONObject compraVentaDetalle = CompraVentaDetalle.getAll(keyVenta);

            String LEYENDA = generar_leyenda(compraVenta.getString("key_empresa"), compraVenta.getString("key_usuario"));
 
            if (compraVenta == null || compraVenta.length() == 0)
                throw new Exception("No se encontró la venta");

            if (compraVentaDetalle == null || compraVentaDetalle.length() == 0)
                throw new Exception("La factura no tiene detalles de productos");

            String key_caja = compraVenta.optString("key_caja", "").trim();

            if (key_caja.isEmpty())
                throw new Exception("No se encontró la key_caja");

            JSONObject caja = getCaja(key_caja);
            if (caja == null || caja.length() == 0)
                throw new Exception("No se encontró la caja");

            // ===============================================================
            // 2. VALIDAR PUNTO DE VENTA
            // ===============================================================
            String keyPuntoVenta = caja.optString("key_punto_venta", "").trim();

            if (keyPuntoVenta.isEmpty())
                throw new Exception("La caja no tiene punto de venta asignado");

            JSONObject puntoVenta = getPuntoVenta(keyPuntoVenta);
            if (puntoVenta == null || puntoVenta.length() == 0)
                throw new Exception("El punto de venta no existe");

            // ===============================================================
            String puntoVenta_codigo_facturacion = puntoVenta.optString("codigo_facturacion", "").trim();

            if (puntoVenta_codigo_facturacion.isEmpty())
                throw new Exception("puntoVenta no hay codigo_facturacion");

            // puntoVenta.get("codigo_facturacion")

            // ===============================================================
            // 3. VALIDAR SUCURSAL
            // ===============================================================
            String keySucursal = compraVenta.optString("key_sucursal", "").trim();

            if (keySucursal.isEmpty())
                throw new Exception("keySucursal");

            JSONObject sucursal = getSucursal(keySucursal);
            if (sucursal == null || sucursal.length() == 0)
                throw new Exception("La sucursal no existe");

            if (sucursal.optString("codigo_facturacion", "").trim().isEmpty())
                throw new Exception("La sucursal no tiene código de facturación");

            if (sucursal.optString("municipio", "").trim().isEmpty())
                throw new Exception("La sucursal no tiene municipio");

            if (sucursal.optString("direccion", "").trim().isEmpty())
                throw new Exception("La sucursal no tiene dirección");

            String telefonoSucursal = sucursal.optString("telefono", "").trim();
            if (telefonoSucursal.length() <= 5)
                throw new Exception("El teléfono de la sucursal no es válido");

            // ===============================================================
            // 4. VALIDAR EMPRESA
            // ===============================================================
            String keyEmpresa = compraVenta.optString("key_empresa", "").trim();

            if (keyEmpresa.isEmpty())
                throw new Exception("La venta no tiene empresa asignada");

            JSONObject empresa = getEmpresa(keyEmpresa);

            if (empresa == null || empresa.length() == 0)
                throw new Exception("No se encontró la empresa");

            if (empresa.optString("nit", "").trim().isEmpty())
                throw new Exception("La empresa no tiene NIT");

            if (empresa.optString("razon_social", "").trim().isEmpty())
                throw new Exception("La empresa no tiene razón social");

            // ===============================================================
            // 5. VALIDAR USUARIO
            // ===============================================================
            if (compraVenta.optString("key_usuario", "").trim().isEmpty())
                throw new Exception("No se encontró el usuario que registra la factura");

            // ===============================================================
            // 6. VALIDAR CLIENTE
            // ===============================================================
            JSONObject cliente = compraVenta.optJSONObject("cliente");

            if (cliente == null || cliente.length() == 0)
                throw new Exception("No se encontró información del cliente");

            String numeroDocumento = cliente.optString("nit", "").trim();
            String razonSocialCliente = cliente.optString("razon_social", "").trim();

            if (numeroDocumento.isEmpty() || razonSocialCliente.isEmpty())
                throw new Exception("Cliente inválido: faltan NIT o razón social");

            // ===============================================================
            // 7. VALIDAR DETALLES DE PRODUCTOS
            // ===============================================================
            for (String keyDetalle : compraVentaDetalle.keySet()) {
                JSONObject d = compraVentaDetalle.getJSONObject(keyDetalle);

                if (d.optDouble("precio_unitario", 0) <= 0)
                    throw new Exception("Producto sin precio válido: " + keyDetalle);

                if (d.optInt("cantidad", 0) <= 0)
                    throw new Exception("Producto sin cantidad válida: " + keyDetalle);

                if (d.optString("key_modelo", "").trim().isEmpty())
                    throw new Exception("Producto sin key_modelo: " + keyDetalle);
            }

            // ===============================================================
            // 8. CONFIGURACIONES FIJAS
            // ===============================================================
            // final String LEYENDA = "Ley N° 453: Tienes derecho a recibir información sobre las características y contenidos de los productos que consumes.";
            final String ACTIVIDAD_ECONOMICA = "475200";
            final String TIPO_DOC_CLIENTE = "1";
            final String NUMERO_FACTURA_DEFAULT = "0";

            String fechaEmision = LocalDateTime.now()
                    .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));

            JSONArray detalleFactura = new JSONArray();
            JSONArray keysModelos = new JSONArray();
            double montoTotalFactura = 0;

            // ===============================================================
            // 9. ARMAR DETALLE FACTURA
            // ===============================================================
            for (String keyDetalle : compraVentaDetalle.keySet()) {
                JSONObject d = compraVentaDetalle.getJSONObject(keyDetalle);

                double precio = d.getDouble("precio_unitario");
                int cantidad = d.getInt("cantidad");
                String keyModelo = d.getString("key_modelo");

                double subtotal = Math.round(precio * cantidad * 100.0) / 100.0;
                montoTotalFactura += subtotal;

                JSONObject item = new JSONObject();
                item.put("descripcion", d.optString("descripcion"));
                item.put("precioUnitario", precio);
                item.put("cantidad", cantidad);
                item.put("subTotal", subtotal);
                item.put("montoDescuento", 0);
                item.put("unidadMedida", "");
                item.put("codigoProducto", "");
                item.put("codigoProductoSin", "");
                item.put("actividadEconomica", ACTIVIDAD_ECONOMICA);
                item.put("numeroImei", "");
                item.put("numeroSerie", "");
                item.put("key_modelo", keyModelo);

                detalleFactura.put(item);
                keysModelos.put(keyModelo);
            }

            montoTotalFactura = Math.round(montoTotalFactura * 100.0) / 100.0;

            // ===============================================================
            // 10. COMPLETAR CÓDIGOS DE FACTURACIÓN
            // ===============================================================
            JSONObject respTipoProducto = api_tipo_producto(keysModelos);
            JSONArray tiposProducto = respTipoProducto.getJSONArray("data");

            for (int i = 0; i < detalleFactura.length(); i++) {
                JSONObject item = detalleFactura.getJSONObject(i);
                String modelo = item.getString("key_modelo");

                for (int j = 0; j < tiposProducto.length(); j++) {
                    JSONObject info = tiposProducto.getJSONObject(j);
                    if (info.getString("key").equals(modelo)) {
                        item.put("codigoProducto", info.getString("codigo_facturacion"));
                        item.put("codigoProductoSin", info.getString("codigo_facturacion"));
                        item.put("unidadMedida", info.getString("unidad_medida_facturacion"));
                        break;
                    }
                }
                item.remove("key_modelo");
            }

            // ===============================================================
            // 11. ARMAR FACTURA
            // ===============================================================
            JSONObject factura = new JSONObject();
            factura.put("nitEmisor", empresa.get("nit"));
            factura.put("razonSocialEmisor", empresa.get("razon_social"));
            factura.put("numeroFactura", NUMERO_FACTURA_DEFAULT);
            factura.put("cuf", "");

            factura.put("codigoSucursal", sucursal.get("codigo_facturacion"));
            factura.put("codigoPuntoVenta", puntoVenta.get("codigo_facturacion"));
            factura.put("municipio", sucursal.get("municipio"));
            factura.put("direccion", sucursal.get("direccion"));
            factura.put("telefono", telefonoSucursal);

            factura.put("fechaEmision", fechaEmision);

            factura.put("numeroDocumento", numeroDocumento);
            factura.put("nombreRazonSocial", razonSocialCliente);
            factura.put("codigoTipoDocumentoIdentidad", TIPO_DOC_CLIENTE);

            factura.put("complemento", "");
            factura.put("codigoCliente", "0");
            factura.put("numeroTarjeta", "");
            factura.put("montoGiftCard", 0);
            factura.put("codigoExcepcion", "1");
            factura.put("cafc", "");

            factura.put("codigoMetodoPago", "1");
            factura.put("montoTotal", montoTotalFactura);
            factura.put("montoTotalSujetoIva", montoTotalFactura);
            factura.put("codigoMoneda", "1");
            factura.put("tipoCambio", compraVenta.get("tipo_cambio"));
            factura.put("montoTotalMoneda", montoTotalFactura);
            factura.put("descuentoAdicional", 0);
            factura.put("leyenda", LEYENDA);
            factura.put("usuario", compraVenta.get("key_usuario"));
            factura.put("codigoDocumentoSector", "1");
            factura.put("detalle", detalleFactura);

            // ===============================================================
            // 12. ENVÍO
            // ===============================================================
            JSONObject request = new JSONObject();
            request.put("component", "factura");
            request.put("type", "emitir");
            request.put("data", factura);
            request.put("ambiente", 2);
            request.put("estado", "cargando");

            request.put("enviar_siat", true);
            request.put("key_usuario", compraVenta.get("key_usuario"));
            request.put("key_empresa", compraVenta.get("key_empresa"));

            JSONObject response = SocketCliente.sendSinc("facturacion", request);

            if (!"exito".equals(response.optString("estado"))) {
                SConsole.error(response.toString());
                throw new Exception("Error al emitir factura: " + response.toString());
            } else {
                JSONObject dataResponse = response.getJSONObject("data");
                String cuf = dataResponse.getString("cuf");
                String numeroFactura = dataResponse.getString("numeroFactura");
                String keyFactura = dataResponse.getString("key");

                JSONObject ventaUpdate = new JSONObject();
                ventaUpdate.put("key", keyVenta);

                JSONObject facturaSave = new JSONObject();
                facturaSave.put("cuf", cuf);
                facturaSave.put("numero", numeroFactura);
                facturaSave.put("key_factura", keyFactura);

                ventaUpdate.put("factura", facturaSave);
                SPGConect.editObject("compra_venta", ventaUpdate);
            }

            venta.put("data", response);
            venta.put("estado", "exito");

        } catch (Exception e) {
            venta.put("estado", "error");
            venta.put("error", e.getMessage());
            e.printStackTrace();
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
            String consulta = "select get_clientes('" + obj.getString("key_empresa") + "') as json";
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

    public static void getJson(JSONObject obj, SSSessionAbstract session) {
        try {
            String consulta = "select get_compra_venta_json('" + obj.getString("key") + "') as json";
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

            System.out.println(data);
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
                // vigentes = CompraVenta.verificarProductosVigentes(data.getString("key"));
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

            System.out.println("raaaaaaaaaaaaaaaaaaaaaaaaaaaa " + obj.getString("key_compra_venta"));
            new PDF().generarCompraVenta(obj.getString("key_compra_venta"));
            obj.put("data", obj.getString("key_compra_venta"));
            obj.put("estado", "exito");
        } catch (Exception e) {
            obj.put("estado", "error");
            obj.put("error", e.getMessage());
            e.printStackTrace();
        }
    }

    public static JSONObject generarComprobanteCompra(JSONObject compraVenta) {
        JSONObject compraVentaDetalle = CompraVentaDetalle.getAll(compraVenta.getString("key"));
        JSONObject send = new JSONObject();
        send.put("component", "asiento_contable");
        send.put("type", "set");
        send.put("key_usuario", "set");
        send.put("key_empresa", compraVenta.getString("key_empresa"));

        JSONObject comprobante = new JSONObject();
        comprobante.put("tipo", "traspaso");
        comprobante.put("fecha", compraVenta.getString("fecha_on").substring(0, 10));
        comprobante.put("descripcion", compraVenta.getString("tipo") + ": " + compraVenta.getString("descripcion"));
        comprobante.put("observacion", compraVenta.getString("observacion"));

        JSONArray detalle = new JSONArray();
        double suma = 0;

        JSONObject contabilidadEnviroment = Contabilidad.getEnviroment(compraVenta.getString("key_empresa"), "IVA");

        double iva = 1;
        if (contabilidadEnviroment.has("observacion")) {
            iva = Double.parseDouble(contabilidadEnviroment.getString("observacion"));
        }

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
            if (cvd.has("precio_facturado") && cvd.get("precio_facturado") != null
                    && !(cvd.get("precio_facturado") + "").equals("null")) {
                monto_iva = Double.parseDouble(cvd.get("precio_facturado") + "") * iva;
            }

            det.put("debe", total - monto_iva);
            suma += total;
            detalle.put(det);

            if (monto_iva > 0) {
                JSONObject ajuste = Contabilidad.getAjusteEmpresa(compraVenta.getString("key_empresa"), "credito_iva");

                det = new JSONObject();
                det.put("codigo", ajuste.getString("codigo"));
                det.put("glosa", cvd.getString("descripcion"));
                det.put("debe", monto_iva);
                detalle.put(det);
            }

        }

        JSONObject ajusteEmpresa = Contabilidad.getAjusteEmpresa(compraVenta.getString("key_empresa"),
                "cuentas_por_pagar");

        JSONObject det = new JSONObject();
        det.put("codigo", ajusteEmpresa.getString("codigo"));
        det.put("glosa", compraVenta.getString("descripcion"));
        det.put("haber", suma);
        detalle.put(det);

        comprobante.put("detalle", detalle);
        send.put("data", comprobante);

        send = SocketCliente.sendSinc("contabilidad", send);
        send.put("estado", "exito");
        if (send.getString("estado").equals("error")) {
            send.put("estado", "error");
            send.put("error", send.getString("error"));
        }
        return send;
    }

    public static JSONObject generarComprobanteVenta(JSONObject compraVenta) {
        JSONObject compraVentaDetalle = CompraVentaDetalle.getAll(compraVenta.getString("key"));
        JSONObject send = new JSONObject();
        send.put("component", "asiento_contable");
        send.put("type", "set");
        send.put("key_usuario", "set");
        send.put("key_empresa", compraVenta.getString("key_empresa"));

        JSONObject comprobante = new JSONObject();
        comprobante.put("tipo", "traspaso");
        comprobante.put("fecha", compraVenta.getString("fecha_on").substring(0, 10));
        comprobante.put("descripcion", compraVenta.getString("tipo") + ": " + compraVenta.getString("descripcion"));
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

            JSONObject ajusteEmpresa = Contabilidad.getAjusteEmpresa(compraVenta.getString("key_empresa"),
                    "cuentas_por_cobrar");

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

            if (cvd.has("precio_facturado") && !cvd.isNull("precio_facturado")) {
                try {
                    precio_facturado = cvd.getDouble("precio_facturado");
                    if (precio_facturado > 0)
                        isFacturado = true;
                } catch (Exception e) {
                }

            }

            monto_iva = precio_facturado * iva;

            det.put("haber", total - monto_iva);
            detalle.put(det);

            if (monto_iva > 0) {
                JSONObject ajuste = Contabilidad.getAjusteEmpresa(compraVenta.getString("key_empresa"), "debito_iva");

                det = new JSONObject();
                det.put("codigo", ajuste.getString("codigo"));
                det.put("glosa", cvd.getString("descripcion"));
                det.put("haber", monto_iva);
                detalle.put(det);
            }

            double totalCompra = 0;

            if (cvd.getJSONObject("data").has("precio_compra")) {
                totalCompra = cvd.getJSONObject("data").getDouble("precio_compra") * cvd.getDouble("cantidad");

                if (totalCompra > 0) {
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
            sumaFacturado += precio_facturado;
        }

        if (isFacturado) {
            // Credito IT

            JSONObject ajusteEmpresa = Contabilidad.getAjusteEmpresa(compraVenta.getString("key_empresa"),
                    "credito_it");
            JSONObject det = new JSONObject();
            det.put("codigo", ajusteEmpresa.getString("codigo"));
            det.put("glosa", "Impuesto a la transacción");
            det.put("debe", sumaFacturado * it);
            detalle.put(det);

            // Debito IT
            ajusteEmpresa = Contabilidad.getAjusteEmpresa(compraVenta.getString("key_empresa"), "debito_it");
            det = new JSONObject();
            det.put("codigo", ajusteEmpresa.getString("codigo"));
            det.put("glosa", "Impuesto a la transacción");
            det.put("haber", sumaFacturado * it);
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

            if (compraVenta.getString("tipo").equals("venta")) {
                // venta
                compraVenta = generarComprobanteVenta(compraVenta);
            } else {
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

    public static void reporte(JSONObject obj, SSSessionAbstract session) {
        try {
            String key_empresa = obj.getString("key_empresa");
            String where_tipo;
            try {
                where_tipo = "    AND compra_venta.tipo = '" + obj.getString("tipo") + "'\n";
            } catch (Exception e) {
                where_tipo = "";
            }
            String where_tipo_pago;
            try {
                where_tipo_pago = "    AND compra_venta.tipo_pago = '" + obj.getString("tipo_pago") + "'\n";
            } catch (Exception e) {
                where_tipo_pago = "";
            }
            String where_state;
            try {
                where_state = "    AND compra_venta.state = '" + obj.getString("state") + "'\n";
            } catch (Exception e) {
                where_state = "";
            }
            String where_fecha;
            try {
                where_fecha = "    AND compra_venta.fecha_on::date BETWEEN '" + obj.getString("fecha_inicio")
                        + "'::date AND '" + obj.getString("fecha_fin") + "'::date\n";
            } catch (Exception e) {
                where_fecha = "";
            }

            String consulta = "SELECT jsonb_object_agg(sq.key, to_json(sq.*))::json as json \n" +
                    "FROM (\n" +
                    "  SELECT compra_venta.*,\n" +
                    "    SUM(compra_venta_detalle.cantidad) AS cantidad,\n" +
                    "    SUM((compra_venta_detalle.cantidad * compra_venta_detalle.precio_unitario) - compra_venta_detalle.descuento) AS precio,\n"
                    +
                    "    SUM(compra_venta_detalle.precio_facturado) AS precio_facturado\n" +
                    "  FROM compra_venta\n" +
                    "    LEFT JOIN compra_venta_detalle ON compra_venta_detalle.key_compra_venta = compra_venta.key AND compra_venta_detalle.estado > 0\n"
                    +
                    "  WHERE compra_venta.key_empresa = '" + key_empresa + "'\n" +
                    "    AND compra_venta.estado > 0\n" +
                    where_tipo +
                    where_tipo_pago +
                    where_state +
                    where_fecha +
                    "  GROUP BY compra_venta.key\n" +
                    ") sq";
            JSONObject data = SPGConect.ejecutarConsultaObject(consulta);
            obj.put("data", data);
            obj.put("estado", "exito");
        } catch (Exception e) {
            obj.put("estado", "error");
            obj.put("error", e.getMessage());
            e.printStackTrace();
        }
    }

    public static void ventaRapidaOld(JSONObject obj, SSSessionAbstract session) {
        try {
            JSONObject data = obj.getJSONObject("data");

            JSONObject venta = registroVenta(obj);
            registroParticipantesVenta(venta);
            registroCompraVentaDetalle(venta, data);
            registroCuotaContado(venta);

            venta.put("state", "vendido");
            SPGConect.editObject(COMPONENT, venta);

            obj.put("data", venta);
            obj.put("estado", "exito");
        } catch (Exception e) {
            obj.put("estado", "error");
            obj.put("error", e.getMessage());
            e.printStackTrace();
        }
    }

    private static JSONObject registroVenta(JSONObject obj) throws SQLException {

        JSONObject data = obj.getJSONObject("data");

        // Registro la compra_venta
        JSONObject venta = new JSONObject();
        venta.put("key", SUtil.uuid());
        venta.put("estado", 1);
        venta.put("state", "cotizacion");
        venta.put("fecha_on", SUtil.now());
        venta.put("descripcion", "Venta Rápida");
        venta.put("key_usuario", obj.getString("key_usuario"));
        venta.put("key_servicio", obj.getJSONObject("servicio").getString("key"));
        venta.put("tipo_pago", "contado");
        venta.put("tipo", "venta");
        venta.put("key_empresa", obj.getString("key_empresa"));

        if (data.has("key_sucursal") && !data.isNull("key_sucursal")) {
            venta.put("key_sucursal", data.getString("key_sucursal"));
        }

        if (data.has("cliente") && !data.isNull("cliente")) {
            venta.put("cliente", data.getJSONObject("cliente"));
        }

        SPGConect.insertArray(COMPONENT, new JSONArray().put(venta));

        return venta;
    }

    private static JSONObject registroParticipantesVenta(JSONObject venta) throws SQLException {
        JSONObject compra_venta_participante = new JSONObject();
        compra_venta_participante.put("key", SUtil.uuid());
        compra_venta_participante.put("estado", 1);
        compra_venta_participante.put("fecha_on", SUtil.now());
        compra_venta_participante.put("key_usuario", venta.getString("key_usuario"));
        compra_venta_participante.put("key_compra_venta", venta.getString("key"));
        compra_venta_participante.put("key_usuario_participante", venta.getString("key_usuario"));
        compra_venta_participante.put("tipo", "admin");
        SPGConect.insertArray(CompraVentaParticipante.COMPONENT, new JSONArray().put(compra_venta_participante));
        return compra_venta_participante;
    }

    private static JSONObject registroCompraVentaDetalle(JSONObject venta, JSONObject data) throws Exception {
        JSONArray productos = data.getJSONArray("productos");

        JSONObject send = new JSONObject();
        send.put("component", "producto");
        send.put("type", "getAllByKeys");
        send.put("key_usuario", venta.getString("key_usuario"));
        send.put("key_empresa", venta.getString("key_empresa"));
        send.put("data", productos);

        // JSONObject response =
        // SSServerAbstract.getSessionByNombreServicio("inventario").sendSync(send);
        JSONObject response = SocketCliente.sendSinc("inventario", send);
        // send.put("estado", "exito");
        if (response.getString("estado").equals("error")) {
            response.put("estado", "error");
            response.put("error", send.getString("error"));
            throw new Exception("Error al traer los productos");
        }

        productos = response.getJSONArray("data");

        JSONObject productoIn;
        JSONObject compra_venta_detalle;
        JSONObject compra_venta_detalle_producto;
        BigDecimal total_venta = BigDecimal.ZERO;
        for (int i = 0; i < productos.length(); i++) {
            productoIn = productos.getJSONObject(i);

            // registrar compra_venta_detalle
            compra_venta_detalle = new JSONObject();
            compra_venta_detalle.put("key", SUtil.uuid());
            compra_venta_detalle.put("estado", 1);
            compra_venta_detalle.put("fecha_on", SUtil.now());
            compra_venta_detalle.put("key_usuario", venta.getString("key_usuario"));
            compra_venta_detalle.put("key_compra_venta", venta.getString("key"));
            compra_venta_detalle.put("descripcion", productoIn.getString("nombre_producto")); // sacar de producto
                                                                                              // nombre + observacion +
                                                                                              // descripcion
            compra_venta_detalle.put("cantidad", productoIn.getInt("cantidad_solicitada"));
            compra_venta_detalle.put("precio_unitario", productoIn.getInt("precio_solicitado"));
            compra_venta_detalle.put("descuento", 0);
            compra_venta_detalle.put("tipo", "producto"); // productio | activo_fijo

            SPGConect.insertArray(CompraVentaDetalle.COMPONENT, new JSONArray().put(compra_venta_detalle));

            // registrar compra_venta_detalle_producto
            compra_venta_detalle_producto = new JSONObject();
            compra_venta_detalle_producto.put("key", SUtil.uuid());
            compra_venta_detalle_producto.put("estado", 1);
            compra_venta_detalle_producto.put("fecha_on", SUtil.now());
            compra_venta_detalle_producto.put("key_usuario", venta.getString("key_usuario"));
            compra_venta_detalle_producto.put("key_compra_venta_detalle", compra_venta_detalle.getString("key"));
            compra_venta_detalle_producto.put("key_producto", productoIn.getString("key"));

            SPGConect.insertArray(CompraVentaDetalleProducto.COMPONENT,
                    new JSONArray().put(compra_venta_detalle_producto));

            total_venta = total_venta.add(BigDecimal.valueOf(productoIn.getDouble("precio_total_solicitado")));

        }

        venta.put("total_venta", total_venta.doubleValue());

        return null;
    }

    private static JSONObject registroCuotaContado(JSONObject venta) throws SQLException {
        JSONObject cuota = new JSONObject();
        cuota.put("key", SUtil.uuid());
        cuota.put("estado", 1);
        cuota.put("fecha_on", SUtil.now());
        cuota.put("key_usuario", venta.getString("key_usuario"));
        cuota.put("key_compra_venta", venta.getString("key"));
        cuota.put("codigo", "0");
        cuota.put("descripcion", "Inicial");
        cuota.put("fecha", new SimpleDateFormat("yyyy-MM-dd").format(new Date()));
        cuota.put("monto", venta.getDouble("total_venta"));
        cuota.put("capital", venta.getDouble("total_venta"));
        cuota.put("interes", 0);

        SPGConect.insertArray(Cuota.COMPONENT, new JSONArray().put(cuota));
        return cuota;
    }

    public static void compraRapida(JSONObject obj, SSSessionAbstract session) {
        ConectInstance conectInstance = null;
        try {
            JSONObject data = obj.getJSONObject("data");

            if (!data.has("key_proveedor") || data.isNull("key_proveedor")) {
                throw new Exception("Debe seleccionar un proveedor");
            }

            // key_usuario
            if (!data.has("key_usuario") || data.isNull("key_usuario")) {
                throw new Exception("Debe seleccionar un usuario");
            }
            if (!data.has("detalle") || data.isNull("detalle")) {
                throw new Exception("El campo detalle[] es requerido");
            }

            JSONObject caja = getCaja(data.getString("key_caja"));
            JSONObject punto_venta = getPuntoVenta(caja.getString("key_punto_venta"));
            JSONObject sucursal = getSucursal(punto_venta.getString("key_sucursal"));

            String keyEmpresa = caja.getString("key_empresa");
            String keyMoneda = obj.getJSONObject("data").getString("key_moneda");

            JSONObject monedas = ContaHook.getMonedas(keyEmpresa);

            JSONObject moneda = monedas.getJSONObject(keyMoneda);
            double tipo_cambio = moneda.getDouble("tipo_cambio");

            JSONObject puntosVentaTipoPago = ContaHook.puntosVentaTipoPago(punto_venta.getString("key"));
            JSONObject tiposPagoOriginal = ContaHook.tiposPago();

            JSONArray tiposPago = new JSONArray();
            double totalPago = 0;

            for (int i = 0; i < JSONObject.getNames(data.getJSONObject("tipos_pago")).length; i++) {
                String key = JSONObject.getNames(data.getJSONObject("tipos_pago"))[i];
                JSONObject value = data.getJSONObject("tipos_pago").getJSONObject(key);
                // Si el tipo de pago es mayor a 0
                totalPago += value.getDouble("monto_nacional");
                JSONObject tipoPago = puntosVentaTipoPago.getJSONObject(key);
                // Verificar si la moneda del tipo de pago es diferente a la moneda de la
                // compra_venta
                tipoPago.put("key", key);
                tipoPago.put("monto_nacional", value.optDouble("monto_nacional"));
                tipoPago.put("monto_extranjera", value.optDouble("monto_extranjera"));
                tiposPago.put(tipoPago);
            }

            String key_empresa = sucursal.getString("key_empresa");
            JSONArray detalle = data.getJSONArray("detalle");

            if (detalle.length() == 0) {
                throw new Exception("El campo detalle[] no puede estar vacío");
            }

            data.put("key_empresa", key_empresa);
            data.put("key_sucursal", sucursal.getString("key"));
            data.put("tipo", "compra");
            data.put("state", "comprado");
            data.put("key_moneda", keyMoneda);
            data.put("tipo_cambio", tipo_cambio);
            data.put("tipo_pago", "contado");
            data.put("key", SUtil.uuid());
            data.put("fecha_on", SUtil.now());
            data.put("estado", 1);

            double total_compra = 0;
            // Validamos el detalle

            for (int i = 0; i < detalle.length(); i++) {
                JSONObject item = detalle.getJSONObject(i);
                if (!item.has("key_modelo") || item.isNull("key_modelo")) {
                    throw new Exception("El campo key_modelo es requerido en el detalle");
                }
                if (!item.has("cantidad") || item.isNull("cantidad")) {
                    throw new Exception("El campo cantidad es requerido en el detalle");
                }
                if (!item.has("precio_unitario") || item.isNull("precio_unitario")) {
                    throw new Exception("El campo precio_unitario es requerido en el detalle");
                }

                total_compra += item.getDouble("cantidad") * item.getDouble("precio_unitario")
                        - item.optDouble("descuento", 0);
                item.put("key", SUtil.uuid());
                item.put("estado", 1);
                item.put("fecha_on", SUtil.now());
                item.put("key_compra_venta", data.getString("key"));
            }

            total_compra = Math.round(total_compra * 100.0) / 100.0; // Redondear a dos decimales

            if (totalPago != total_compra) {
                throw new Exception("El total pagado no coincide con el total de la compra (" + totalPago + " - "
                        + total_compra + ")");
            }

            JSONObject cuota_inicial = new JSONObject();
            cuota_inicial.put("key", SUtil.uuid());
            cuota_inicial.put("estado", 1);
            cuota_inicial.put("fecha_on", SUtil.now());
            cuota_inicial.put("fecha", SUtil.now());
            cuota_inicial.put("key_usuario", data.getString("key_usuario"));
            cuota_inicial.put("key_compra_venta", data.getString("key"));
            cuota_inicial.put("codigo", "0");
            cuota_inicial.put("descripcion", "Inicial");
            cuota_inicial.put("monto", total_compra);
            cuota_inicial.put("capital", total_compra);
            cuota_inicial.put("interes", 0);
            data.put("cuotas", new JSONArray().put(cuota_inicial));

            conectInstance = new ConectInstance();
            conectInstance.Transacction();

            conectInstance.insertObject("compra_venta", data);
            conectInstance.insertArray("compra_venta_detalle", detalle);
            conectInstance.insertObject("cuota", cuota_inicial);

            AsientoContable asiento = new AsientoContable(AsientoContableTipo.egreso);
            asiento.descripcion = "Compra Iva";
            asiento.observacion = "Compra iva Iva";
            asiento.key_empresa = key_empresa;
            asiento.key_usuario = data.getString("key_usuario");

            JSONObject tags = new JSONObject()
                    .put("key_usuario", data.getString("key_usuario"))
                    .put("key_caja", caja.getString("key"))
                    .put("key_punto_venta", punto_venta.getString("key"))
                    .put("key_sucursal", sucursal.getString("key"))
                    .put("key_compra_venta", data.getString("key"));

            JSONObject tipoPagoOriginal = null;
            for (int i = 0; i < tiposPago.length(); i++) {
                JSONObject tipoPago = tiposPago.getJSONObject(i);

                tipoPagoOriginal = tiposPagoOriginal.getJSONObject(tipoPago.getString("key_tipo_pago"));

                double montoTc = tipoPago.getDouble("monto_nacional");
                double montoTcExt = tipoPago.getDouble("monto_extranjera");
                montoTc = Math.round(montoTc * 100.0) / 100.0; // Redondear a dos decimales
                montoTcExt = Math.round(montoTcExt * 100.0) / 100.0; // Redondear a dos decimales

                if (tipoPago.optBoolean("pasa_por_caja", false)) {
                    asiento.setDetalle(new AsientoContableDetalle(
                            caja.getString("key_cuenta_contable"),
                            "Compra Rapida",
                            "haber",
                            montoTc,
                            montoTcExt,
                            tags));
                } else {
                    asiento.setDetalle(new AsientoContableDetalle(
                            tipoPago.getString("key_cuenta_contable"),
                            "Compra Rapida",
                            "haber",
                            montoTc,
                            montoTcExt,
                            tags));
                }
            }

            if (data.optBoolean("facturar", false)) {

                double porc_iva = Contabilidad.getEnviroment(key_empresa, "IVA").optDouble("observacion", 0);
                double iva = total_compra - (total_compra / ((porc_iva / 100) + 1)); // de las env
                iva = Math.round(iva * 100.0) / 100.0;

                if (data.optBoolean("facturar_luego", false)) {
                    JSONObject CuentaDeIva = Contabilidad.getAjusteEmpresa(key_empresa, "credito_iva_por_cobrar");

                    asiento.setDetalle(new AsientoContableDetalle(
                            CuentaDeIva.getString("key"),
                            "Compra Iva por cobrar",
                            "debe",
                            iva,
                            iva,
                            tags));
                } else {

                    JSONObject CuentaDeIva = Contabilidad.getAjusteEmpresa(key_empresa, "credito_iva");

                    asiento.setDetalle(new AsientoContableDetalle(
                            CuentaDeIva.getString("key"),
                            "Compra Iva",
                            "debe",
                            iva,
                            iva,
                            tags));
                }
                data.put("precio_facturado", total_compra);

                conectInstance.editObject("compra_venta", data);
            }

            // Registramos los productos
            JSONObject inventarioRequest = new JSONObject();
            inventarioRequest.put("component", "modelo");
            inventarioRequest.put("type", "compraRapida");
            inventarioRequest.put("compra", data);
            inventarioRequest.put("tags", tags);

            inventarioRequest.put("asiento_contable", asiento.toJSON());

            JSONObject responseInventario = SocketCliente.sendSinc("inventario", inventarioRequest, 15000);
            if (!responseInventario.getString("estado").equals("exito")) {
                SConsole.warning("Registrando la compra con error en el inventario: ",
                        responseInventario.getString("error"));
                throw new Exception(responseInventario.getString("error"));
            }

            JSONObject asientoContable = responseInventario.getJSONObject("asiento_contable");

            JSONObject info = new JSONObject();
            info.put("key_compra_venta", data.optString("key"));

            double amortizar = 0;

            for (int i = 0; i < tiposPago.length(); i++) {
                JSONObject tipoPago = tiposPago.getJSONObject(i);
                // moneda= monedas.getJSONObject(tipoPago.getString("key_moneda"));

                tipoPagoOriginal = tiposPagoOriginal.getJSONObject(tipoPago.getString("key_tipo_pago"));

                if (!tipoPagoOriginal.optBoolean("is_credito", false)) {

                    amortizar += tipoPago.getDouble("monto_nacional");
                }

                double dmonto = tipoPago.optDouble("monto_extranjera");
                dmonto = Math.round(dmonto * 100.0) / 100.0; // Redondear a dos decimales

                setDetalleCaja(caja.getString("key"), tipoPago.getString("key"), dmonto * -1, "Compra Rapida",
                        "compra_rapida", asientoContable.getString("key"), asientoContable.getString("codigo"),
                        data.getString("key_usuario"), info, moneda.getString("key"),
                        moneda.optDouble("tipo_cambio", 1), tipoPago.getString("key_tipo_pago"));

            }
            conectInstance.commit();

            JSONObject send = new JSONObject();
            send.put("tipos_pago", tiposPago);
            send.put("key_usuario", data.getString("key_usuario"));
            send.put("key_caja", caja.getString("key"));
            send.put("monto", amortizar);
            send.put("fecha", SUtil.now());
            CuotaAmortizacion.intentarAmortizar(cuota_inicial.getString("key"), send);

            data.put("tipo", "compra");
            obj.put("data", data);
            obj.put("estado", "exito");
        } catch (Exception e) {
            obj.put("estado", "error");
            obj.put("error", e.getMessage());
            e.printStackTrace();
            if (conectInstance != null) {
                conectInstance.rollback();
            }
        } finally {
            if (conectInstance != null) {
                conectInstance.close();
            }
        }
    }

    public static void ventaRapida(JSONObject obj, SSSessionAbstract session) {
        ConectInstance conectInstance = null;
        try {
            JSONObject data = obj.getJSONObject("data");
            conectInstance = new ConectInstance();
            conectInstance.Transacction();

            JSONObject caja = getCaja(data.getString("key_caja"));
            JSONObject punto_venta = getPuntoVenta(caja.getString("key_punto_venta"));
            JSONObject sucursal = getSucursal(punto_venta.getString("key_sucursal"));
            JSONObject monedas = ContaHook.getMonedas(sucursal.getString("key_empresa"));
            // alvaro
            String keyMoneda = data.getString("key_moneda");
            JSONObject monedaCaja = monedas.getJSONObject(keyMoneda);
            double tipo_cambioCaja = monedaCaja.getDouble("tipo_cambio");

            JSONObject puntoVentaTiposPago = ContaHook.puntosVentaTipoPago(caja.getString("key_punto_venta"));
            // JSONObject tiposPago = ContaHook.tiposPago();
            double totalPago = 0;

            for (int i = 0; i < JSONObject.getNames(data.getJSONObject("tipos_pago")).length; i++) {
                String key = JSONObject.getNames(data.getJSONObject("tipos_pago"))[i];
                JSONObject value = data.getJSONObject("tipos_pago").getJSONObject(key);
                totalPago += value.optDouble("monto_nacional", 0);
                // Si el tipo de pago es mayor a 0
                JSONObject tipoPago = puntoVentaTiposPago.getJSONObject(key);

                tipoPago.put("monto_extranjera", value.optDouble("monto_extranjera", 0));
                tipoPago.put("monto_nacional", value.optDouble("monto_nacional", 0));

            }

            JSONObject venta = new JSONObject();
            venta.put("key", SUtil.uuid());
            venta.put("tipo", "venta");
            venta.put("state", "vendido");
            venta.put("tipo_pago", "contado");
            venta.put("key", SUtil.uuid());
            venta.put("key_moneda", keyMoneda);
            venta.put("tipo_cambio", tipo_cambioCaja);
            venta.put("fecha_on", SUtil.now());
            venta.put("estado", 1);
            venta.put("descripcion", "Venta Rápida");
            if (data.has("key_cliente") && !data.isNull("key_cliente")) {
                venta.put("key_cliente", data.getString("key_cliente"));
            }
            venta.put("key_usuario", data.getString("key_usuario"));
            venta.put("key_sucursal", punto_venta.getString("key_sucursal"));
            venta.put("key_empresa", sucursal.getString("key_empresa"));

            double total_venta = 0;


            JSONArray detalle = data.getJSONArray("detalle");
            for (int i = 0; i < detalle.length(); i++) {
                JSONObject item = detalle.getJSONObject(i);
                if (!item.has("key_modelo") || item.isNull("key_modelo")) {
                    throw new Exception("El campo key_modelo es requerido en el detalle");
                }
                if (!item.has("cantidad") || item.isNull("cantidad")) {
                    throw new Exception("El campo cantidad es requerido en el detalle");
                }
                if (!item.has("precio_unitario") || item.isNull("precio_unitario")) {
                    throw new Exception("El campo precio_unitario es requerido en el detalle");
                }
                total_venta += item.getDouble("cantidad") * item.getDouble("precio_unitario")
                        - item.optDouble("descuento", 0);
                item.put("key", SUtil.uuid());
                item.put("estado", 1);
                item.put("fecha_on", SUtil.now());
                item.put("key_compra_venta", venta.getString("key"));
                
            }

            total_venta = Math.round(total_venta * 100.0) / 100.0; // Redondear a dos decimales

            if (totalPago != total_venta) {
                throw new Exception("El total pagado no coincide con el total de la venta");
            }

            JSONObject cuota_inicial = new JSONObject();
            cuota_inicial.put("key", SUtil.uuid());
            cuota_inicial.put("estado", 1);
            cuota_inicial.put("fecha_on", SUtil.now());
            cuota_inicial.put("fecha", SUtil.now());
            cuota_inicial.put("key_usuario", venta.getString("key_usuario"));
            cuota_inicial.put("key_compra_venta", venta.getString("key"));
            cuota_inicial.put("codigo", "0");
            cuota_inicial.put("descripcion", "Inicial");
            cuota_inicial.put("monto", total_venta);
            cuota_inicial.put("capital", total_venta);
            cuota_inicial.put("interes", 0);

            conectInstance.insertObject("compra_venta", venta);
            conectInstance.insertArray("compra_venta_detalle", detalle);
            conectInstance.insertObject("cuota", cuota_inicial);
            venta.put("cuotas", new JSONArray().put(cuota_inicial));
            venta.put("detalle", detalle);

            // JSONObject CuentaDeGanancias =
            // Contabilidad.getAjusteEmpresa(sucursal.getString("key_empresa"), "ganancia");

            AsientoContable asiento = new AsientoContable(AsientoContableTipo.ingreso);
            asiento.descripcion = "Venta Rápida";
            asiento.observacion = "Venta rapida obs";
            asiento.fecha = SUtil.now();
            asiento.key_empresa = sucursal.getString("key_empresa");
            asiento.key_usuario = data.getString("key_usuario");

            JSONObject tags = new JSONObject()
                    .put("key_usuario", data.getString("key_usuario"))
                    .put("key_caja", caja.getString("key"))
                    .put("key_punto_venta", punto_venta.getString("key"))
                    .put("key_sucursal", sucursal.getString("key"))
                    .put("key_compra_venta", venta.getString("key"));

            Double totalVentaGeneral = total_venta;
            double porcImp = 0;
            if (data.optBoolean("facturar", false)) {
                double porc_iva = Contabilidad.getEnviroment(sucursal.getString("key_empresa"), "IVA")
                        .optDouble("observacion", 0);
                double porc_it = Contabilidad.getEnviroment(sucursal.getString("key_empresa"), "IT")
                        .optDouble("observacion", 0);

                JSONObject CuentaDeIva = Contabilidad.getAjusteEmpresa(sucursal.getString("key_empresa"), "debito_iva");
                JSONObject CuentaDeIt = Contabilidad.getAjusteEmpresa(sucursal.getString("key_empresa"), "debito_it");
                double iva = total_venta * (porc_iva / 100); // de las env
                double it = total_venta * (porc_it / 100); // de las env
                total_venta -= iva;
                total_venta -= it;

                porcImp += porc_iva;
                porcImp += porc_it;

                asiento.setDetalle(new AsientoContableDetalle(
                        CuentaDeIva.getString("key"),
                        "Venta Rapida Iva",
                        "haber",
                        iva,
                        iva,
                        tags));

                asiento.setDetalle(new AsientoContableDetalle(
                        CuentaDeIt.getString("key"),
                        "Venta Rapida IT",
                        "haber",
                        it,
                        it,
                        tags));

                totalVentaGeneral = Math.round(totalVentaGeneral * 100.0) / 100.0;
                venta.put("precio_facturado", totalVentaGeneral);
                conectInstance.editObject("compra_venta", venta);

            }

            // total_venta = Math.round(total_venta * 100.0) / 100.0;
            // asiento.setDetalle(new
            // AsientoContableDetalle(CuentaDeGanancias.getString("key"), "Ventas")
            // .setHaber(total_venta));

            JSONObject tipoPagoOriginal = null;
            double amortizar = 0;
            JSONObject moneda;
            JSONArray tiposPago = new JSONArray();
            for (int i = 0; i < JSONObject.getNames(data.getJSONObject("tipos_pago")).length; i++) {
                JSONObject tipoPago = puntoVentaTiposPago
                        .getJSONObject(JSONObject.getNames(data.getJSONObject("tipos_pago"))[i]);
                tiposPago.put(tipoPago);

                // moneda = monedas.getJSONObject(tipoPago.getString("key_moneda"));

                tipoPagoOriginal = tipoPago;

                if (!tipoPagoOriginal.optBoolean("is_credito", false)) {
                    amortizar += tipoPago.getDouble("monto_nacional");
                }

                tags.put("key_tipo_pago", tipoPago.getString("key_tipo_pago"));

                String keyCuenta = tipoPago.getString("key_cuenta_contable");
                if (tipoPago.optBoolean("pasa_por_caja", false)) {
                    keyCuenta = caja.getString("key_cuenta_contable");
                }

                // double montoBase =
                // tipoPago.getDouble("monto")/moneda.optDouble("tipo_cambio",1);
                double montoTc = tipoPago.getDouble("monto_nacional");
                montoTc = Math.round(montoTc * 100.0) / 100.0; // Redondear a dos decimales

                double montoTcExt = tipoPago.getDouble("monto_extranjera");
                montoTcExt = Math.round(montoTcExt * 100.0) / 100.0; // Redondear a dos decimales
                asiento.setDetalle(new AsientoContableDetalle(
                        keyCuenta,
                        "Venta Rapida Caja",
                        "debe",
                        montoTc,
                        montoTcExt,
                        tags));

            }

            tags.remove("key_tipo_pago");

            JSONObject inventarioRequest = new JSONObject();
            inventarioRequest.put("component", "modelo");
            inventarioRequest.put("type", "ventaRapida");
            inventarioRequest.put("venta", venta);
            inventarioRequest.put("monedaCaja", monedaCaja);
            inventarioRequest.put("porcentaje_impuesto", porcImp);

            inventarioRequest.put("asiento_contable", asiento.toJSON());

            JSONObject responseInventario = SocketCliente.sendSinc("inventario", inventarioRequest);
            if (!responseInventario.getString("estado").equals("exito")) {
                SConsole.warning("Registrando la venta con error en el inventario: ",
                        responseInventario.getString("error"));
                throw new Exception(responseInventario.getString("error"));
            }

            JSONObject asientoContable = responseInventario.getJSONObject("asiento_contable");

            JSONObject info = new JSONObject();
            info.put("key_compra_venta", venta.optString("key"));

            for (int i = 0; i < JSONObject.getNames(data.getJSONObject("tipos_pago")).length; i++) {

                JSONObject puntoVentaTipoPago = puntoVentaTiposPago
                        .getJSONObject(JSONObject.getNames(data.getJSONObject("tipos_pago"))[i]);

                // moneda= monedas.getJSONObject(puntoVentaTipoPago.getString("key_moneda"));

                double dmonto = puntoVentaTipoPago.getDouble("monto_extranjera");
                dmonto = Math.round(dmonto * 100.0) / 100.0; // Redondear a dos decimales

                setDetalleCaja(caja.getString("key"), puntoVentaTipoPago.getString("key_tipo_pago"), dmonto,
                        "Venta Rapida", "venta_rapida", asientoContable.getString("key"),
                        asientoContable.getString("codigo"), data.getString("key_usuario"), info, keyMoneda,
                        tipo_cambioCaja, puntoVentaTipoPago.getString("key_tipo_pago"));
            }
            // asiento.enviar();
            obj.put("data", venta);
            obj.put("estado", "exito");
            conectInstance.commit();

            JSONObject send = new JSONObject();
            send.put("key_usuario", data.getString("key_usuario"));
            send.put("key_caja", caja.getString("key"));
            send.put("monto", amortizar);
            send.put("fecha", SUtil.now());
            send.put("tipos_pago", tiposPago);

            CuotaAmortizacion.intentarAmortizar(cuota_inicial.getString("key"), send);

        } catch (Exception e) {
            obj.put("estado", "error");
            obj.put("error", e.getMessage());
            e.printStackTrace();
            conectInstance.rollback();
        } finally {
            if (conectInstance != null) {
                conectInstance.close();
            }
        }
    }

    public static JSONObject getCaja(String key_caja) {
        JSONObject send = new JSONObject();
        send.put("component", "caja");
        send.put("type", "getByKey");
        send.put("key", key_caja);
        JSONObject resp = SocketCliente.sendSinc("caja", send);
        return resp.getJSONObject("data");
    }

    public static JSONObject getCajaDatalle(String key_caja) {
        JSONObject send = new JSONObject();
        send.put("component", "caja_detalle");
        send.put("type", "getByKey");
        send.put("key_caja", key_caja);
        JSONObject resp = SocketCliente.sendSinc("caja", send);
        return resp.getJSONObject("data");
    }

    // public static JSONObject generarLeyenda() {
    // JSONObject send = new JSONObject();
    // send.put("component", "caja");
    // send.put("type", "getByKey");
    // send.put("key", key_caja);
    // JSONObject resp = SocketCliente.sendSinc("caja", send);
    // return resp.getJSONObject("data");
    // }

    public static JSONObject getPuntoVenta(String key_punto_venta) {
        JSONObject send = new JSONObject();
        send.put("component", "punto_venta");
        send.put("type", "getByKey");
        send.put("key", key_punto_venta);
        JSONObject resp = SocketCliente.sendSinc("empresa", send);
        return resp.getJSONObject("data");
    }

    public static JSONObject getSucursal(String key_sucursal) {
        JSONObject send = new JSONObject();
        send.put("component", "sucursal");
        send.put("type", "getByKey");
        send.put("key", key_sucursal);
        JSONObject resp = SocketCliente.sendSinc("empresa", send);
        return resp.getJSONObject("data");
    }

    public static JSONObject getEmpresa(String key_empresa) {
        JSONObject send = new JSONObject();
        send.put("component", "empresa");
        send.put("type", "getByKey");
        send.put("key", key_empresa);
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

    public static JSONObject setDetalleCaja(String key_caja, String key_punto_venta_tipo_pago, double monto,
            String desc, String tipo, String key_comprobante, String codigo_comprobante, String key_usuario,
            JSONObject info, String key_moneda, double tipo_cambio, String key_tipo_pago) {
        JSONObject send = new JSONObject();
        send.put("component", "caja_detalle");
        send.put("type", "registro");
        send.put("key_usuario", key_usuario);

        JSONObject det = new JSONObject();
        det.put("key_caja", key_caja);
        det.put("key_punto_venta_tipo_pago", key_punto_venta_tipo_pago);
        det.put("key_tipo_pago", key_tipo_pago);
        det.put("monto", monto);
        det.put("key_moneda", key_moneda);
        det.put("tipo_cambio", tipo_cambio);
        det.put("descripcion", desc);
        det.put("tipo", tipo);
        det.put("fecha", SUtil.now());
        det.put("key_comprobante", key_comprobante);
        det.put("codigo_comprobante", codigo_comprobante);
        det.put("data", info);

        send.put("data", det);

        JSONObject resp = SocketCliente.sendSinc("caja", send);
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

}
