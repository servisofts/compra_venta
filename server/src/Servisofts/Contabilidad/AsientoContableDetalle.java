package Servisofts.Contabilidad;

import org.json.JSONObject;

public class AsientoContableDetalle {

    public static AsientoContableDetalle fromJSON(JSONObject json) {
        AsientoContableDetalle detalle = new AsientoContableDetalle(json.optString("key_cuenta_contable"),
                json.optString("glosa"));
        detalle.setDebe(json.optDouble("debe", 0));
        detalle.setHaber(json.optDouble("haber", 0));
        if(json.has("key_moneda")) {
            detalle.key_moneda = json.optString("key_moneda");
            detalle.tipo_cambio = json.optDouble("tipo_cambio", 1);
            detalle.debe_me = json.optDouble("debe_me", 0);
            detalle.haber_me = json.optDouble("haber_me", 0);
        }
        return detalle;
    }

    public String key_cuenta_contable;
    public String glosa;
    public String key_moneda;
    public double tipo_cambio = 1;
    public double debe;
    public double haber;
    public double debe_me;
    public double haber_me;

    public AsientoContableDetalle(String key_cuenta_contable, String glosa) {
        this.glosa = glosa;
        this.key_cuenta_contable = key_cuenta_contable;
    }

    public AsientoContableDetalle(String key_cuenta_contable, String glosa, String key_moneda, double tipo_cambio) {
        this.glosa = glosa;
        this.key_moneda = key_moneda;
        this.key_cuenta_contable = key_cuenta_contable;
        this.tipo_cambio = tipo_cambio;
    }

    public AsientoContableDetalle setDebe(double debe) {
        this.debe = debe;
        double debe_me_ = debe / tipo_cambio;
        debe_me_ = Math.round(debe_me_ * 100.0) / 100.0;
        if(tipo_cambio != 1) this.debe_me = debe_me_;
        return this;
    }

    public AsientoContableDetalle setHaber(double haber) {
        this.haber = haber;
        double haber_me_ = haber / tipo_cambio;
        haber_me_ = Math.round(haber_me_ * 100.0) / 100.0;
        if(tipo_cambio != 1) this.haber_me = haber_me_;
        return this;
    }

    public JSONObject toJSON() {
        JSONObject json = new JSONObject();
        json.put("key_cuenta_contable", key_cuenta_contable);
        json.put("glosa", glosa);
        json.put("debe", debe);
        json.put("debe_me", debe_me);
        json.put("haber", haber);
        json.put("haber_me", haber_me);
        json.put("key_moneda", key_moneda);
        return json;
    }

}
