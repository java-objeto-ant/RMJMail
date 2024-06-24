/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.rmj.xapitoken.util;

import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.rmj.appdriver.SQLUtil;
import org.rmj.lib.net.WebClient;

/**
 *
 * @author sayso
 */
public class RequestAccess {
    private static String url = "https://restgk.guanzongroup.com.ph/x-api/v1.0/auth/access_request.php";
    private static String token = "eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9.eyJpc3MiOiJndWFuem9uZ3JvdXAuY29tLnBoIiwiYXVkIjoiQ2VidWFuYSBMaHVpbGxlciIsImlhdCI6IjEyODg2MjcyMDAiLCJuYmYiOiIxNTkwODE1MzgwIiwiZXhwIjpudWxsLCJkYXRhIjoiYjk1YmNmODZjNGU2MmJmY2Y0ZjM4NzJmOWM0MjdlNTQ1ZTU1YjdkMWZlYTg1MzM0ZWFmYWVlMDliMjllNWIxYmRhNjcyN2E2Y2ZhZWE1MzVjYWFiOGRkM2M2YzFlMWJiMmZhOThlNmMwY2NiODRkYTg0MTdiYTFiYzI4N2RmMWYzYzdhZDRlNjliYmZjYTMyMTZlOTc0ODFjY2ZlZTNlZGMwNTM0NDdkYTYwZDgzMzNjZmZmM2M3Y2VmZDc4OTYwIn0.7zktyIhn7RjkF1A6zHlRT0xFWnmzMltewWg7FoAhmH4";
    private static String FORMAT_TIMESTAMP  = "yyyy-MM-dd HH:mm:ss";

    public static void main(String[] args) throws IOException{
        String path;
        if(System.getProperty("os.name").toLowerCase().contains("win")){
            path = "D:/GGC_Java_Systems";
        }
        else{
            path = "/srv/GGC_Java_Systems";
        }
        System.setProperty("sys.default.path.temp", path + "/temp");
        System.setProperty("sys.default.path.config", path);
        
        //Sample code snippet of executing a program in java
        //String xa[] = new String[] {"IntegSys", "GGC_BM001", "GAP0190001"};
        //RequestToken.main(xa);
        
        Map<String, String> headers = 
                        new HashMap<String, String>();
        headers.put("Accept", "application/json");
        headers.put("Content-Type", "application/json");
        String args_0 = "";
        String args_1 = "";
        JSONParser oParser = new JSONParser();
        switch(args.length){
            case 0:
                args_0 = System.getProperty("sys.default.path.config") + "/mac1975.token";
                args_1 = System.getProperty("sys.default.path.config") + "/access.token";
                break;
            case 1:
                args_0 = args[0];
                args_1 = System.getProperty("sys.default.path.config") + "/access.token";
                break;
            case 2:   
                args_0 = args[0];
                args_1 = args[1];
                break;
            default:
                System.out.println("RequestAccess <client_token> <access_token>");
                System.exit(1);
                
        }

        try {
            JSONObject oJson = (JSONObject)oParser.parse(new FileReader(args_0));
            System.out.println(args_0);
            System.out.println("Hello");
            System.out.println((String)oJson.get("client_key"));
            headers.put("g-client-key", (String)oJson.get("client_key"));
        } catch (ParseException ex) {
            Logger.getLogger(RequestAccess.class.getName()).log(Level.SEVERE, null, ex);
        }

        //JSONParser oParser = new JSONParser();
        //JSONObject json_obj = null;

        //String response = WebClient.httpPostJSon(sURL, param.toJSONString(), (HashMap<String, String>) headers);
        //System.out.println("Try...");
        String response = WebClient.httpsPostJSon(url, null, (HashMap<String, String>) headers);
        //System.out.println("After try...");
        if(response == null){
            System.out.println("HTTP Error detected: " + System.getProperty("store.error.info"));
            //return null;
            System.exit(1);
        }

        try {
            
            JSONObject oJson = (JSONObject) oParser.parse(response);
            
            if(((String)oJson.get("result")).equalsIgnoreCase("success")){
                JSONObject oData = new JSONObject();
                
                oData.put("access_key", (String)((JSONObject)oJson.get("payload")).get("token"));
                //oData.put("client_key", (String)((JSONObject) oParser.parse((String)oJson.get("payload"))).get("token"));
                oData.put("created", dateFormat(Calendar.getInstance().getTime(), FORMAT_TIMESTAMP));
                oData.put("parent", args_0);

                OutputStreamWriter out = new OutputStreamWriter(new FileOutputStream(args_1),"UTF-8");
                out.write(oData.toJSONString());
                out.flush();
                out.close();
            }
            
        } catch (ParseException ex) {
            //Logger.getLogger(RequestToken.class.getName()).log(Level.SEVERE, null, ex);
            ex.printStackTrace();
        }

        //json_obj = (JSONObject) oParser.parse(response);
        //System.out.println(json_obj.toJSONString());
        //System.out.println(response);
    }
    
   private static String dateFormat(Object date, String format){
      SimpleDateFormat sf = new SimpleDateFormat(format);
      String ret;
      if ( date instanceof Timestamp )
         ret = sf.format((Date)date);
      else if ( date instanceof Date )
         ret = sf.format(date);
      else if ( date instanceof Calendar ){
         Calendar loDate = (Calendar) date;
         ret = sf.format(loDate.getTime());
         loDate = null;
      }
      else
         ret = null;

      sf = null;
      return ret;
    }    
    
}
