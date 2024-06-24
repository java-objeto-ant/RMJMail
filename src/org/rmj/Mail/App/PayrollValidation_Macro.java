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
public class PayrollValidation_Macro {
    private static GRiderX instance = null;
    
    //PayrollValidation_Macro [config] [paydiv] [div] [emptype] [period] [requestedby]
    public static void main(String args[]) throws SQLException, UnsupportedEncodingException, JRException, Exception{
        String path = "";
        String cDivision = "";
        String sEmpTypID = "";
        String dPeriodFr = "";
        String sRqstdByx = "";
        String sTransNox = "";
        
        if(args.length == 0){
            path = "D:/GGC_Java_Systems";
            cDivision = "0";    //Mobile Phone
            sEmpTypID = "R";    //Regular
            dPeriodFr = "2022-05-16";
            sRqstdByx = "M033070005";
        }
        else if(args.length == 5){
            path = args[0];
            cDivision = args[1];
            sEmpTypID = args[2];
            dPeriodFr = args[3];
            sRqstdByx = args[4];
        }
        else{
            System.out.println("Invalid parameters detected...");
            System.exit(0);
        }

        System.setProperty("sys.default.path.temp", path + "/temp");
        System.setProperty("sys.default.path.config", path);
        System.setProperty("sys.default.LogFile", path + "/temp/PayrollValidation_Macro.log");
        System.setProperty("sys.default.Signature", path + "/images/180492153_794111184853448_4384690834553115402_n.png");
        
        writeLog("++++++++++++++++++++++++++++++++++++++++++++++");
        writeLog("PayrollValidation_Macro Initiated: " + Calendar.getInstance().getTime());
        
        
        instance = new GRiderX("gRider");
        instance.setOnline(true);
        
        String sql = "SELECT sTransNox" + 
                    " FROM Payroll_Validation_Macro_Master" + 
                    " WHERE cDivision = " + SQLUtil.toSQL(cDivision) + 
                      " AND sEmpTypID = " + SQLUtil.toSQL(sEmpTypID) + 
                      " AND dPeriodFr = " + SQLUtil.toSQL(dPeriodFr) + 
                      " AND sRqstdByx = " + SQLUtil.toSQL(sRqstdByx); 
//        ResultSet rs = instance.executeQuery(sql);
        
//        if(rs.next()){
//            sTransNox = rs.getString("sTransNox");
//        }
        
        sql = "SELECT cDivision" +
                   ", sDivsnDsc" + 
                   ", dPeriodFr" +
                   ", dPeriodTo" +
                   ", COUNT(*) nRecdCtrx" +
                   ", SUM(xBasicSal) xBasicSal" +
                   ", SUM(nAbsencex) nAbsencex" +
                   ", SUM(nHolidayx) nHolidayx" +
                   ", SUM(nTardines) nTardines" +
                   ", SUM(nUndrTime) nUndrTime" +
                   ", SUM(nOverTime) nOverTime" +
                   ", SUM(nBenefits) nBenefits" +
                   ", SUM(nNightDif) nNightDif" +
                   ", SUM(nOTNightD) nOTNightD" +
              " FROM" +
                " (SELECT c.cDivision" +
                       ", d.sDivsnDsc" +
                       ", a.dPeriodFr" +
                       ", a.dPeriodTo" +
                       ", @xBasicSal := CAST(REVERSE(DECODE(UNHEX(b.nBasicSal), 'PETMGR')) AS DECIMAL) xBasicSal" +
                       ", @nDailyRt1 := ROUND(@xBasicSal / IF(c.cPayDivCd = '3' OR a.sEmpTypID = 'R', 13, b.nAttendnc + b.nAbsencex), 2) AS nDailyRt1" +
                       ", @nAbsencex := ROUND(b.nAbsencex * @nDailyRt1, 2) nAbsencex" +
                       ", CAST(REVERSE(DECODE(UNHEX(b.nHolidayx), 'PETMGR')) AS DECIMAL) + CAST(REVERSE(DECODE(UNHEX(b.nAdjHldyx), 'PETMGR')) AS DECIMAL) nHolidayx" +
                       ", CAST(REVERSE(DECODE(UNHEX(b.nTardines), 'PETMGR')) AS DECIMAL) + CAST(REVERSE(DECODE(UNHEX(b.nAdjTardi), 'PETMGR')) AS DECIMAL) nTardines" +
                       ", CAST(REVERSE(DECODE(UNHEX(b.nUndrTime), 'PETMGR')) AS DECIMAL) + CAST(REVERSE(DECODE(UNHEX(b.nAdjUndrT), 'PETMGR')) AS DECIMAL) nUndrTime" +
                       ", CAST(REVERSE(DECODE(UNHEX(b.nOverTime), 'PETMGR')) AS DECIMAL) + CAST(REVERSE(DECODE(UNHEX(b.nAdjOverT), 'PETMGR')) AS DECIMAL) nOverTime" +
                       ", CAST(REVERSE(DECODE(UNHEX(b.nBenefits), 'PETMGR')) AS DECIMAL) + CAST(REVERSE(DECODE(UNHEX(b.nAdjBenft), 'PETMGR')) AS DECIMAL) nBenefits" +
                       ", CAST(REVERSE(DECODE(UNHEX(b.nNightDif), 'PETMGR')) AS DECIMAL) + CAST(REVERSE(DECODE(UNHEX(b.nAdjNghtD), 'PETMGR')) AS DECIMAL) nNightDif" +
                       ", CAST(REVERSE(DECODE(UNHEX(b.nOTNightD), 'PETMGR')) AS DECIMAL) + CAST(REVERSE(DECODE(UNHEX(b.nAdjOTNht), 'PETMGR')) AS DECIMAL) nOTNightD" +
                       ", b.nAdjustxx" +
                  " FROM Payroll_Period a" +
                        " LEFT JOIN Payroll_Summary_New b ON a.sPayPerID = b.sPayPerID" +
                        " LEFT JOIN Branch_Others c ON b.sBranchCd = c.sBranchCD" +
                        " LEFT JOIN Division d ON c.cDivision = d.sDivsnCde" +
                  " WHERE a.dPeriodFr BETWEEN DATE_ADD(" + SQLUtil.toSQL(dPeriodFr) + ", INTERVAL -3 MONTH) AND " + SQLUtil.toSQL(dPeriodFr) + 
                    " AND a.sEmpTypID = " + SQLUtil.toSQL(sEmpTypID) +
                    " AND c.cDivision = " + SQLUtil.toSQL(cDivision) +
              " GROUP BY cDivision, dPeriodTo" +
              " ORDER BY dPeriodTo DESC LIMIT 6";
        
        ResultSet rs = instance.executeQuery(sql);
        //rs = instance.executeQuery(sql);
        
        
        Map<String, Object> params = new HashMap<>();
        params.put("sCompnyNm", "Guanzon Group of Companies");
        params.put("sBranchNm", "Guanzon Central Office");
        params.put("sAddressx", "Perez Blvd., Dagupan City");
        params.put("sReportNm", "Macro Payroll Validation");
        params.put("sReportDt", "For Payroll Period: " + dPeriodFr);
        
        JSONArray xrows = new JSONArray();
        
        while(rs.next()){
            JSONObject xrow = new JSONObject() ;
            xrow.put("sField01", rs.getString("sDivsnDsc"));
            xrow.put("sField02", rs.getDate("dPeriodFr") + " to " + rs.getDate("dPeriodTo"));
            xrow.put("nField01", rs.getInt("nRecdCtrx"));
            xrow.put("lField01", rs.getDouble("xBasicSal"));
            xrow.put("nField02", Math.round(rs.getDouble("xBasicSal") / rs.getInt("nRecdCtrx") * 100.0) / 100.0);
            xrows.add(xrow);
            System.out.println(rs.getDate("dPeriodFr"));
        }

       //prepare stream input for the creation of JASPER REPORT
        InputStream stream = new ByteArrayInputStream(xrows.toJSONString().getBytes("UTF-8"));
        JsonDataSource jrjson = new JsonDataSource(stream);
        //System.out.println(xrows.toJSONString());
        //create jasper report
        JasperPrint print = JasperFillManager.fillReport(path + "/reports/PayrollMacro.jasper", params, jrjson);
        String filename = cDivision + "-" + sEmpTypID + "-" + dPeriodFr;

        //System.out.println(System.getProperty("sys.default.path.temp") + "/payslip/" + filename + "X.pdf");
        //export jasper report to PDF
        JasperExportManager.exportReportToPdfFile(print, System.getProperty("sys.default.path.temp") + "/payslip/" + filename + "X.pdf");
        

        JSONObject param = new JSONObject();
        File filex;
        param.put("to", "masayson@guanzongroup.com.ph");
        param.put("subject", "Macro Payroll Validation for " + dPeriodFr);
        param.put("body", "Please check attached file.");
        filex =  new File(System.getProperty("sys.default.path.temp") + "/payslip/" + filename + "X.pdf");
        param.put("data1", encodeFileToBase64Binary(filex));
        param.put("filename1", filex.getName());
        
        if(!sendMail(param)){
            writeLog("Unable to send " + filename + "X.pdf");
        }

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
