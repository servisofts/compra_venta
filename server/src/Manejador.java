import Component.*;
import Servisofts.SConsole;
import org.json.JSONObject;
import Server.SSSAbstract.SSSessionAbstract;

public class Manejador {
    public static void onMessage(JSONObject obj, SSSessionAbstract session) {
        if (session != null) {
            SConsole.log(session.getIdSession(), "\t|\t", obj.getString("component"), obj.getString("type"));
        } else {
            SConsole.log("http-server", "-->", obj.getString("component"), obj.getString("type"));
        }
        if (obj.isNull("component")) {
            return;
        }
        switch (obj.getString("component")) {
            case CompraVenta.COMPONENT:
                CompraVenta.onMessage(obj, session);
                break;
            case CompraVentaDetalle.COMPONENT:
                CompraVentaDetalle.onMessage(obj, session);
                break;
            case CompraVentaDetalleProducto.COMPONENT:
                CompraVentaDetalleProducto.onMessage(obj, session); 
                break;
            case Cuota.COMPONENT:
                Cuota.onMessage(obj, session);
                break;
            case CompraVentaParticipante.COMPONENT:
                CompraVentaParticipante.onMessage(obj, session);
                break;
            case CompraVentaComentario.COMPONENT:
                CompraVentaComentario.onMessage(obj, session);
                break;
            case CompraVentaHistorico.COMPONENT:
                CompraVentaHistorico.onMessage(obj, session);
                break;
            case CuotaAmortizacion.COMPONENT:
                CuotaAmortizacion.onMessage(obj, session);
                break;
        }
    }
}
