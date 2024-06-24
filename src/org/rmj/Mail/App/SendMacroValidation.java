/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.rmj.mail.App;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;
import net.sf.jasperreports.engine.JRException;
import net.sf.jasperreports.engine.JasperExportManager;
import net.sf.jasperreports.engine.JasperFillManager;
import net.sf.jasperreports.engine.JasperPrint;
import net.sf.jasperreports.engine.data.JsonDataSource;
import org.apache.commons.codec.binary.Base64;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.rmj.appdriver.GProperty;
import org.rmj.appdriver.SQLUtil;
import org.rmj.appdriver.agent.GRiderX;
import org.rmj.lib.net.WebClient;
import org.rmj.xapitoken.util.RequestAccess;

/**
 *
 * @author sayso
 */
public class SendMacroValidation {
    private static GRiderX instance = null;
    
    //PayrollValidation_Macro [config] [paydiv] [div] [emptype] [period] [requestedby]
    public static void main(String args[]) throws SQLException, UnsupportedEncodingException, JRException, Exception{
        String path = "";
        String sTransNox = "";
        
        if(args.length == 0){
            path = "D:/GGC_Java_Systems";
            sTransNox = "M00122000004";
        }
        else if(args.length == 2){
            path = args[0];
            sTransNox = args[1];
        }
        else{
            System.out.println("Invalid parameters detected...");
            System.exit(0);
        }

        System.setProperty("sys.default.path.temp", path + "/temp");
        System.setProperty("sys.default.path.config", path);
        System.setProperty("sys.default.LogFile", path + "/temp/SendMacroValidation.log");
        System.setProperty("sys.default.Signature", path + "/images/180492153_794111184853448_4384690834553115402_n.png");
        
        writeLog("++++++++++++++++++++++++++++++++++++++++++++++");
        writeLog("SendMacroValidation Initiated: " + Calendar.getInstance().getTime());
        
        instance = new GRiderX("gRider");
        instance.setOnline(true);

        String sql = "SELECT b.sDivsnDsc, b.sPreAudtr, a.*" + 
                    " FROM Payroll_Validation_Macro_Master a" + 
                        " LEFT JOIN Division b ON a.cDivision = b.sDivsnCde" +
                    " WHERE a.sTransNox = " + SQLUtil.toSQL(sTransNox) ; 

        ResultSet rsMstr = instance.executeQuery(sql);
        //rs = instance.executeQuery(sql);
        
        String dPeriodFr;
        String dPeriodTr;
        String cDivision;
        String sEmpTypID;
        
        if(!rsMstr.next()){
            writeLog("Send Failed! Macro Validation with TransNo " + sTransNox + "is not existing!");
            writeLog("SendMacroValidation Ended: " + Calendar.getInstance().getTime());
            System.exit(1);
        }

        dPeriodFr = SQLUtil.dateFormat(rsMstr.getDate("dPeriodFr"), SQLUtil.FORMAT_SHORT_DATE);
        dPeriodTr = SQLUtil.dateFormat(rsMstr.getDate("dPeriodTo"), SQLUtil.FORMAT_SHORT_DATE);
        cDivision = rsMstr.getString("cDivision");
        sEmpTypID = rsMstr.getString("sEmpTypID");
        
        Map<String, Object> params = new HashMap<>();
        params.put("sCompnyNm", "Guanzon Group of Companies");
        params.put("sBranchNm", "Guanzon Central Office");
        params.put("sAddressx", "Guanzon Bldg., Perez Blvd., Dagupan City");
        params.put("sReportNm", "Macro Payroll Validation - " + (sEmpTypID.equalsIgnoreCase("r") ? "Regular" : "Probationary"));
        params.put("sReportDt", "For Payroll Period: " + rsMstr.getString("sValidDsc"));
        params.put("sPrintdBy", "SYSTEM GENERATED");
        
        JSONArray xrows = new JSONArray();

        sql = "SELECT *" + 
             " FROM Payroll_Validation_Macro_Detail" + 
             " WHERE sTransNox = " + SQLUtil.toSQL(sTransNox) + 
             " ORDER BY dPeriodFr DESC" ;   
        ResultSet rsDetl = instance.executeQuery(sql);
        
        while(rsDetl.next()){
            JSONObject xrow = new JSONObject() ;
            xrow.put("sField01", rsMstr.getString("sDivsnDsc"));
            xrow.put("sField02", SQLUtil.dateFormat(rsDetl.getDate("dPeriodFr"), "MM/dd") + " - " + SQLUtil.dateFormat(rsDetl.getDate("dPeriodTo"), "MM/dd/YYYY"));
            xrow.put("nField01", rsDetl.getInt("nEmployee"));
            xrow.put("lField01", rsDetl.getDouble("xBasicSal"));
            xrow.put("lField02", Math.round(rsDetl.getDouble("xBasicSal") / rsDetl.getInt("nEmployee") * 100.0) / 100.0);
            xrows.add(xrow);
            System.out.println(rsDetl.getDate("dPeriodFr"));
        }

       //prepare stream input for the creation of JASPER REPORT
        InputStream stream = new ByteArrayInputStream(xrows.toJSONString().getBytes("UTF-8"));
        JsonDataSource jrjson = new JsonDataSource(stream);
        //System.out.println(xrows.toJSONString());
        //create jasper report
        JasperPrint print = JasperFillManager.fillReport(path + "/reports/PayrollMacro.jasper", params, jrjson);
        String filename = "Macro-" + cDivision + "-" + sEmpTypID + "-" + dPeriodFr;

        //System.out.println(System.getProperty("sys.default.path.temp") + "/payslip/" + filename + "X.pdf");
        //export jasper report to PDF
        JasperExportManager.exportReportToPdfFile(print, System.getProperty("sys.default.path.temp") + "/payslip/" + filename + "X.pdf");
        
        JSONObject param = new JSONObject();
        File filex;
        //param.put("to", "masayson@guanzongroup.com.ph");
        param.put("from", "Payroll Validation Utility");
        param.put("to", rsMstr.getString("sPreAudtr"));
        param.put("subject", rsMstr.getString("sDivsnDsc") + " - Macro Payroll Validation for " + (sEmpTypID.equalsIgnoreCase("r") ? "Regular" : "Probationary") + "- " + rsMstr.getString("sValidDsc"));
        param.put("body", "Please check attached file.");
        filex =  new File(System.getProperty("sys.default.path.temp") + "/payslip/" + filename + "X.pdf");
        param.put("data1", encodeFileToBase64Binary(filex));
        param.put("filename1", filex.getName());
        
        if(!sendMail(param)){
            writeLog("Unable to send " + filename + "X.pdf");
        }

        Calendar cal = Calendar.getInstance();
        sql = "UPDATE Payroll_Validation_Macro_Master" +
             " SET cMailSent = '1'" +   
                ", dMailSent = " + SQLUtil.toSQL(cal) +                  
             " WHERE sTransNox = " + SQLUtil.toSQL(sTransNox); 
        instance.executeQuery(sql, "Payroll_Validation_Macro_Master", instance.getBranchCode(), "" );
        
        writeLog("Sent successfully " + filename + "X.pdf");
        writeLog("PayrollValidation_Macro Ended: " + Calendar.getInstance().getTime());
        
        //System.out.println(xrows.toJSONString());
        //send the 
        
    }

   private static boolean sendMail(JSONObject param) throws IOException, ParseException{
        String sURL = "https://restgk.guanzongroup.com.ph/x-api/v1.0/mail/sendrawmail.php";        
        JSONObject oJson;
        JSONParser oParser = new JSONParser();
       
        oJson = (JSONObject)oParser.parse(new FileReader(System.getProperty("sys.default.path.config") + "/" + "access" + ".token"));
        Calendar current_date = Calendar.getInstance();
        current_date.add(Calendar.MINUTE, -25);
        Calendar date_created = Calendar.getInstance();
        date_created.setTime(SQLUtil.toDate((String) oJson.get("created") , SQLUtil.FORMAT_TIMESTAMP));

        //Check if token is still valid within the time frame
        //Request new access token if not in the current period range
        if(current_date.after(date_created)){
            String[] xargs = new String[] {(String) oJson.get("parent")};
            RequestAccess.main(xargs);
            oJson = (JSONObject)oParser.parse(new FileReader(System.getProperty("sys.default.path.config") + "/" + "access" + ".token"));
        }

        //Set the access_key as the header of the URL Request
        JSONObject headers = new JSONObject();
        headers.put("g-access-token", (String)oJson.get("access_key"));
        //System.out.println("printing param:");
        //System.out.println(headers.toJSONString());
        //System.out.println(param.toJSONString());
        //System.out.println("param printed:");
        //System.out.println((String)oJson.get("access_key"));
        
        //System.out.println(param.toJSONString());
        //System.out.println(headers.toJSONString());
        //String response = null;
        String response = WebClient.httpPostJSon(sURL, param.toJSONString(), (HashMap<String, String>) headers);
        //System.out.println(param.toJSONString());
        //System.out.println(headers.toJSONString());
        writeLog(response);
        if(response == null){
            writeLog("HTTP Error detected: " + System.getProperty("store.error.info"));
            return false;
        }
        JSONObject result = (JSONObject) oParser.parse(response);
        
        if(!((String)result.get("result")).equalsIgnoreCase("success")) 
            return false;
        
       return true;
   } 
    
    public static String encodeFileToBase64Binary(File file) throws Exception{
        FileInputStream fileInputStreamReader = new FileInputStream(file);
        byte[] bytes = new byte[(int)file.length()];
        fileInputStreamReader.read(bytes);
        return new String(Base64.encodeBase64(bytes), "UTF-8");
    }
   
   private static GProperty loadConfig(String sprop){
         return(new GProperty(sprop));
   }    

   private static void writeLog(String value){
        try {
            FileWriter fw = new FileWriter(System.getProperty("sys.default.LogFile"), true);
            fw.write(value + "\n");
            fw.close();
        } catch (IOException ex) {
            ex.printStackTrace();
        }
   }   
    
}
