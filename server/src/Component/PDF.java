package Component;


import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import javax.imageio.ImageIO;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType0Font;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.json.JSONArray;
import org.json.JSONObject;

import Server.SSSAbstract.SSServerAbstract;
import Servisofts.SConfig;
import Servisofts.SUtil;

public class PDF {
    
    private PDDocument document;
    private PDPage page;
    private PDPageContentStream contentStream;
    private PDType0Font font;
    private float y;
    private int fontSize;
    private int espacios[];
    private float ANCHO = 612;
    private float ALTO = 792;
    private int ANCHO_MARGEN = 572;
    private int ALTO_MARGEN = 752;

    private NumeroLiteral numero_literal;
    private String pattern = "###,###,###.##";
    private String pattern3 = "###,###,###.###";
    private String rutaFont = "./font/dejavu-sans/DejaVuSans.ttf";
    private String rutaFontBold = "./font/dejavu-sans/DejaVuSans-Bold.ttf";


    private DecimalFormat myFormatter = new DecimalFormat(pattern,DecimalFormatSymbols.getInstance(Locale.US));
    private DecimalFormat myFormatter3 = new DecimalFormat(pattern3,DecimalFormatSymbols.getInstance(Locale.US));
    
  
    public PDF(){
        try{
            document = new PDDocument();
            page = new PDPage(PDRectangle.LETTER);
            contentStream = new PDPageContentStream(document, page, PDPageContentStream.AppendMode.APPEND, true);
        }catch(Exception e){
            e.printStackTrace();
        }
        
    }


    public void perfilSucursal(JSONObject usuario){
        try{
            
            String text = usuario.getString("razon_social");
            setBold();
            write(text);
            
            setNormal();
            //writeImg("./img/tierra.jpg");
            
            //writeImgUrl("https://empresa.servisofts.com/http/sucursal/"+usuario.getString("key_sucursal")+"?time="+new Date().getTime(), ANCHO/2);
            y-=fontSize+2;

            if(!usuario.isNull("nit")){
                text = "Nit. "+usuario.getString("nit");
                write(text);
            }

            if(!usuario.isNull("telefono")){
                text = "Tel. "+usuario.getString("telefono");
                write(text);
            }
            
            if(!usuario.isNull("direccion")){
                text = usuario.getString("direccion");
                write(text);
            }
            
        }catch(Exception e){
            e.printStackTrace();
        }
    }

    public void perfilUsuario(JSONObject usuario){
        try{
            
            String text = usuario.getString("razon_social");
            setBold();
            write(text);
            
            setNormal();
            //writeImg("./img/tierra.jpg");
            
            writeImgUrl("https://usuario.servisofts.com/http/usuario/"+usuario.getString("key_usuario")+"?time="+new Date().getTime(), ANCHO/2);
            y-=fontSize+2;

            if(!usuario.isNull("nit")){
                write("Nit. "+usuario.getString("nit"));
            }

            if(!usuario.isNull("telefono")){
                write("Tel. "+usuario.getString("telefono"));
            }
            
            if(!usuario.isNull("correo")){
                write(usuario.getString("correo"));
            }
            
        }catch(Exception e){
            e.printStackTrace();
        }
    }

    public String generarCompraVenta(String key_compra_venta )    throws IOException{

        JSONObject compra_venta = CompraVenta.getByKey(key_compra_venta);
        
        document = new PDDocument();

        initOffSet();
      
        
        y = page.getMediaBox().getHeight();
        y-=20;

        setFontSize(8);

        setBold();
        write(compra_venta.getString("descripcion"));
        setNormal();
        write(compra_venta.getString("observacion"));
        drawLine();
        y-=fontSize+2;

        setBold();
        write(compra_venta.getString("state"));

        drawLine();
        y-=fontSize+2;

        JSONObject proveedor = null;

        if(compra_venta.has("proveedor") && !compra_venta.isNull("proveedor")) proveedor = compra_venta.getJSONObject("proveedor");
        y-=fontSize+2;
        if(proveedor!=null) perfilSucursal(proveedor);
        else write("Seleccione un proveedor");

        y-=fontSize+2;
        drawLine();
        y-=fontSize+2;

        JSONObject cliente = null;
        
        if(compra_venta.has("cliente") && !compra_venta.isNull("cliente"))  cliente = compra_venta.getJSONObject("cliente");
        y-=fontSize+2;
        if(cliente!=null) perfilUsuario(cliente);
        else write("Seleccione un cliente");

        
        drawLine();
        y-=fontSize+2;
        if(!compra_venta.isNull("tipo_pago")){
            switch(compra_venta.getString("tipo_pago")){
                case "pp_discrecional":
                    write("Credito");
                break;
                case "pp_financiero":
                    write("Credito");
                break;
                default: write("Contado");
            }
        }else{
            write("Contado");
        }
        
        drawLine();

        setBold();
        y-=fontSize+2;
        write("DETALLE");

        JSONObject compra_venta_detalles = CompraVentaDetalle.getAll(key_compra_venta);

        setNormal();
        JSONObject compra_venta_detalle;
        double subtotal=0;
        if(compra_venta_detalles!=null && !compra_venta_detalles.isEmpty()){
            for (int i = 0; i < JSONObject.getNames(compra_venta_detalles).length; i++) {
                y-=fontSize+2;
                compra_venta_detalle = compra_venta_detalles.getJSONObject(JSONObject.getNames(compra_venta_detalles)[i]);
                writeLeft(compra_venta_detalle.getString("descripcion"));
                subtotal+=(compra_venta_detalle.getDouble("precio_unitario")*compra_venta_detalle.getInt("cantidad"));
                writeLeftRigth(compra_venta_detalle.getDouble("precio_unitario")+" x "+compra_venta_detalle.getInt("cantidad"), (compra_venta_detalle.getDouble("precio_unitario")*compra_venta_detalle.getInt("cantidad"))+"");
                writeLeft(compra_venta_detalle.getString("tipo"));
    
            }
        }else{
            write("Ingrese algun producto");
        }
        
        drawLine();
        y-=fontSize+2;

        writeLeftRigth("SUBTOTAL Bs.", subtotal+"");
        writeLeftRigth("DESCUENTO Bs.", "0.00");
        writeLeftRigth("MONTO GIFCARD Bs.", "0.00");
        writeLeftRigth("TOTAL A PAGAR Bs.", "0.00");
        writeLeftRigth("IMPORTE BASE CREDITO FISCAL", subtotal+"");

        y-=fontSize+2;

        write("SON: "+new NumeroLiteral().Convertir(subtotal+"", false));
        


        contentStream.setLineWidth(1f);   
        contentStream.stroke();  
        contentStream.close();
        document.save(SConfig.getJSON("files").getString("url")+"pdf/"+key_compra_venta);
        document.close();
        return key_compra_venta;
    }
    

    private float textCenter(String text,int x) throws IOException{
        float tamanoFontX = font.getStringWidth(text) / 1000 * fontSize;
        float startX = (x - (tamanoFontX/2));
        return startX;
    }

 

    public float getTextCenterX(String text) throws IOException{
        float tamanoFontX = font.getStringWidth(text) / 1000 * fontSize;
        float startX = (page.getMediaBox().getWidth() - tamanoFontX) / 2;
        return startX;
    }
    public float getTextCenterY(String text) throws IOException{
        float tamanoFontY = font.getFontDescriptor().getFontBoundingBox().getHeight() / 1000 * fontSize;
        float startY = (page.getMediaBox().getHeight() - tamanoFontY) / 2;
        return startY;
    }

    private float textCenter(String text) throws IOException{
        float tamanoFontX = font.getStringWidth(text) / 1000 * fontSize;
        float startX = (120 - (tamanoFontX/2));
        return startX;
    }

    private void writeImg(String url){
        try{
            PDImageXObject pdImage = null;
            try{
                pdImage = PDImageXObject.createFromFile(url, document);
            }catch(Exception e){
                System.out.println("Imagen registrada");
            }
            y-=100;
            contentStream.drawImage(pdImage, 25, y, 100, 100);
            if(y<=100){
                newPage();
                y = page.getMediaBox().getHeight()-50;
            }
        }catch(Exception e){
            e.printStackTrace();
        }
        
    }
    
    private void writeImgUrl(String url){
        try{
            URL url_ = new URL(url);
            PDImageXObject pdImage = null;
            BufferedImage image_ = ImageIO.read(url_);
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ImageIO.write(image_, "PNG", baos);
            byte[] bytes = baos.toByteArray();
            pdImage = PDImageXObject.createFromByteArray(document, bytes, "prueba");
            y-=100;
            contentStream.drawImage(pdImage, 25, y, 100, 100);
            if(y<=100){
                newPage();
                y = page.getMediaBox().getHeight()-50;
            }
        }catch(Exception e){
            System.out.println("Imagen registrada");
        }
    }

    private void writeImgUrl(String url, float center){
        try{
            URL url_ = new URL(url);
            PDImageXObject pdImage = null;
            BufferedImage image_ = ImageIO.read(url_);
            

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ImageIO.write(image_, "PNG", baos);
            byte[] bytes = baos.toByteArray();
            pdImage = PDImageXObject.createFromByteArray(document, bytes, "prueba");
            y-=100;
            contentStream.drawImage(pdImage, center-(100/2), y, 100, 100);
            if(y<0){
                newPage();
                y = page.getMediaBox().getHeight()-50;
            }   
        }catch(Exception e){
            System.out.println(e.getLocalizedMessage());
        }
    }
    
    private void writeText(String contenido){
        try{    
            String txt = "";
            for (int i = 0; i < 1; i++) {
                y-=fontSize+2;
                txt = contenido;
                        
                while((txt.length()*6)>ANCHO){
                    write(10, y, txt.substring(0, ((int)ANCHO/6)));
                    txt = txt.substring(((int)ANCHO/6), txt.length());
                    y-=fontSize+2;
                }            
                write(25, y, txt);
            }
            y-=fontSize+2;
            if(y<=0){
                newPage();
                y = page.getMediaBox().getHeight()-50;
            }
        }catch(Exception e){
            e.printStackTrace();
        }
        
    }

    private void write(String text) throws IOException {
        //String line = new String(text.getBytes("ISO-8859-1"), "UTF-8");.
        String []text_ = text.split("\n");
        String text__;
        String text_1;
        for (int i = 0; i < text_.length; i++) {
            text__ = text_[i];
            List<String> lista = truncarText(text__, ((int)ANCHO)-5);
            
            for (int j = 0; j < lista.size(); j++) {
                text_1 = lista.get(j);
                write(getTextCenterX(text_1), y, text_1);
                y-=fontSize+2;
            }
        }

    }

    private void writeLeft(String text) throws IOException {
        //String line = new String(text.getBytes("ISO-8859-1"), "UTF-8");.
        String []text_ = text.split("\n");
        String text__;
        String text_1;
        for (int i = 0; i < text_.length; i++) {
            text__ = text_[i];
            List<String> lista = truncarText(text__, ((int)ANCHO)-5);
            
            for (int j = 0; j < lista.size(); j++) {
                text_1 = lista.get(j);
                write(5, y, text_1);
                y-=fontSize+2;
            }
        }

    }
    private void writeLeftRigth(String text, String text1) throws IOException {
        //String line = new String(text.getBytes("ISO-8859-1"), "UTF-8");.
        String []text_ = text.split("\n");
        String text__;
        String text_1;
        double w1;
        for (int i = 0; i < text_.length; i++) {
            text__ = text_[i];
            List<String> lista = truncarText(text__, ((int)ANCHO)-5);
            
            for (int j = 0; j < lista.size(); j++) {
                w1 = font.getStringWidth(text1)/100;

                write((float)(ANCHO-2-w1), y, text1);

                text_1 = lista.get(j);
                write(5, y, text_1);
                y-=fontSize+2;
            }
        }

    }

    private void write(float x, float y, String text) throws IOException {
        //String line = new String(text.getBytes("ISO-8859-1"), "UTF-8");.
        String line = new String(text.getBytes("ISO-8859-1"), "UTF-8");


        contentStream.beginText();
        contentStream.newLineAtOffset(x, y);
        try {
            contentStream.showText(line);

        } catch (Exception e) {
            // line = new String(text.getBytes("ISO-8859-1"), "UTF-8");
            contentStream.showText(text);
        }
        contentStream.endText();
    }

    private float textRight(String text,int x) throws IOException{
        float tamanoFontX = font.getStringWidth(text) / 1000 * fontSize;
        float startX = (x - (tamanoFontX));
        return startX;
    }
    private float getCenterX(float ancho) throws IOException{
        float startX = (page.getMediaBox().getWidth() - ancho) / 2;
        return startX;
    }
    private float getCenterY(float alto) throws IOException{
        float startY = (page.getMediaBox().getHeight() - alto) / 2;
        return startY;
    }
    
    public static Color getColorHex(String colorStr) {
        return new Color(
                Integer.valueOf( colorStr.substring( 1, 3 ), 16 ),
                Integer.valueOf( colorStr.substring( 3, 5 ), 16 ),
                Integer.valueOf( colorStr.substring( 5, 7 ), 16 ) );
    }
    public void newPage() throws IOException{
        contentStream.setLineWidth(1f);   
        contentStream.stroke();  
        contentStream.close();
        
        initOffSet();
    }
    public void newPageOffSeT() throws IOException{
        contentStream.setLineWidth(1f);   
        contentStream.stroke();  
        contentStream.close();
        //page = new PDPage(new PDRectangle(ANCHO, ALTO));
        page = new PDPage(new PDRectangle());
        document.addPage(page);
        contentStream = new PDPageContentStream(document, page);
        setFontSize(fontSize);
    }
    
    public void setFontSize(int fontSize) throws IOException {
        this.fontSize = fontSize;
        contentStream.setFont(font, fontSize);
    }

    public void setColor(Color color) throws IOException{
        contentStream.setNonStrokingColor(color); 
    }
    public String getRutaFont() {
        return rutaFont;
    }

    public void setRutaFont(String rutaFont) {
        this.rutaFont = rutaFont;
    }

    public String getRutaFontBold() {
        return rutaFontBold;
    }

    public void setRutaFontBold(String rutaFontBold) {
        this.rutaFontBold = rutaFontBold;
    }

    public void setBold() throws IOException{
        font = PDType0Font.load(document, new File(getRutaFontBold()));
        contentStream.setFont(font, fontSize);
    }
    public void setNormal() throws IOException{
        font = PDType0Font.load(document, new File(getRutaFont()));
        contentStream.setFont(font, fontSize);
    }
    public int PositionMid(int x,int y){
        int resultado;
        resultado=y-x;
        resultado=y-(resultado/2);
        return resultado;
    }
    public void armarTabla(JSONArray array){
        JSONArray head = array.getJSONArray(0);
        JSONArray body = array.getJSONArray(0);
        int width=0;
        
        for(int i=0;i<head.length();i++){
            width+=head.getJSONObject(i).getInt("width");
        }
        int restar=configWidth(width);
        for(int i=0;i<head.length();i++){
            width+=head.getJSONObject(i).getInt("width");
        }
    }
    
    public int configWidth(int width){
        int restar =0;
        int aux=0;
        if(width>ALTO_MARGEN){
            aux=width-ALTO_MARGEN;
            restar=aux/espacios.length;
        }
        return restar;
    }
    
    public static ArrayList<String> limitText(String text,int tamano){
        ArrayList list = new ArrayList<>();
        String mesenge = "";
        String[] list_aux = text.split(" ");
        for (int i = 0; i < list_aux.length; i++) {
            mesenge += list_aux[i]+" ";
            if (tamano <= mesenge.length()) {
                list.add(mesenge);
                mesenge = "";
            } else {
                if (i == list_aux.length - 1) {
                    list.add(mesenge);
                }
            }
        }
        return list;
    }
    public static ArrayList<String> truncarText(String text,int tamano){
        ArrayList list = new ArrayList<>();
        String mesenge = "";
        for (int i = 0; i < text.length(); i++) {
            mesenge += text.charAt(i);
            if((mesenge.length()>=tamano)){
                list.add(mesenge);
                mesenge="";
            }
            if(i>=(text.length()-1)){
                list.add(mesenge);
            }
        }
        return list;
    }
    public static String getMesNum(int num) {
        String mes ="";
        switch (num) {
            case 1:
                mes = "ENERO";
                break;
            case 2:
                mes = "FEBRERO";
                break;
            case 3:
                mes = "MARZO";
                break;
            case 4:
                mes = "ABRIL";
                break;
            case 5:
                mes = "MAYO";
                break;
            case 6:
                mes = "JUNIO";
                break;
            case 7:
                mes = "JULIO";
                break;
            case 8:
                mes = "AGOSTO";
                break;
            case 9:
                mes = "SEPTIEMBRE";
                break;
            case 10:
                mes = "OCTUBRE";
                break;
            case 11:
                mes = "NOVIEMBRE";
                break;
            case 12:
                mes = "DICIEMBRE";
                break;    
        }
        return mes;
    }

    public static String  getFechaLiteral(String fecha_date){
        String fecha ="";
        //2022-03-29T19:08:24
        String aux[]=fecha_date.split("T");
        aux = aux[0].split("-");
        fecha = aux[2]+" DE "+getMesNum(Integer.parseInt(aux[1]))+" DE "+aux[0];
        return fecha;
    }
    public static String  getFechaFormat(String fecha_date){
        String fecha ="";
        //2022-03-29T19:08:24
        String aux[]=fecha_date.split("T");
        String hora[]=aux[1].split(":");
        aux = aux[0].split("-");
        String estado="AM";
        if(Integer.parseInt(hora[0])>12){
            estado="PM";
        }
        fecha = aux[2]+"/"+aux[1]+"/"+aux[0] +" "+hora[0]+":"+hora[1]+" "+estado;
        return fecha;
    }
    public String getNumFormat(double monto){
        String monto_total="";
        monto_total=myFormatter.format(monto);
        myFormatter.setMaximumFractionDigits(2);
        myFormatter.setMinimumFractionDigits(2);
       if (monto_total.equals("0")) {
            monto_total = monto_total + ".00";
        }
        String[] tamano = monto_total.split("\\.");
        if (tamano.length == 1) {
            monto_total = monto_total + ".00";
        } else {
            if (tamano[1].length() == 1) {
                monto_total = monto_total + "0";
            }
        }
        return monto_total;
    }
    
    public String getNumFormat3(double monto){
        String monto_total="";
        monto_total=myFormatter3.format(monto);
        myFormatter3.setMaximumFractionDigits(3);
        myFormatter3.setMinimumFractionDigits(3);
       if (monto_total.equals("0")) {
            monto_total = monto_total + ".000";
        }
        String[] tamano = monto_total.split("\\.");
        if (tamano.length == 1) {
            monto_total = monto_total + ".000";
        } else {
            if (tamano[1].length() == 1) {
                monto_total = monto_total + "0";
            }
        }
        return monto_total;
    }


    public void drawLine() throws IOException{        
        contentStream.moveTo(0, y);
        contentStream.lineTo(ANCHO,y);
        contentStream.stroke(); 
   }

   public void initOffSet() throws IOException{
        ALTO=671;
        ANCHO=241;
        ALTO_MARGEN = 874;
        numero_literal= new NumeroLiteral();
        //page = new PDPage(new PDRectangle(ANCHO, cal));
        page = new PDPage(new PDRectangle(ANCHO, ALTO));
        document.addPage(page);
        font = PDType0Font.load(document, new File(getRutaFont()));
        contentStream = new PDPageContentStream(document, page, PDPageContentStream.AppendMode.APPEND, true);
    }



    

}
