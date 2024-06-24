/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.rmj.mail.App;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import net.sf.jasperreports.engine.JRException;
import net.sf.jasperreports.engine.JasperExportManager;
import net.sf.jasperreports.engine.JasperFillManager;
import net.sf.jasperreports.engine.JasperPrint;
import net.sf.jasperreports.engine.data.JsonDataSource;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.rmj.appdriver.GProperty;
import org.rmj.appdriver.MiscUtil;
import org.rmj.appdriver.SQLUtil;
import org.rmj.appdriver.agent.GRiderX;
import org.rmj.lib.net.WebClient;
import static org.rmj.mail.App.SendMacroValidation.encodeFileToBase64Binary;
import org.rmj.xapitoken.util.RequestAccess;

/**
 *
 * @author sayso
 */
public class SendMicroValidation {
    private static GRiderX instance = null;
    private static String cDivision = "";
    private static String dPeriodFr = "";
    private static String dPeriodTo = "";
    private static String sRqstdByx = "";
    private static String sTransNox = "";
    private static String path = "";
    
    //PayrollValidation_Macro [config] [paydiv] [div] [emptype] [period] [requestedby]
    public static void main(String args[]) throws SQLException, UnsupportedEncodingException, JRException, Exception{
        if(args.length == 0){
            path = "D:/GGC_Java_Systems";
        }
        else{
            path = args[0];
        }

        System.setProperty("sys.default.path.temp", path + "/temp");
        System.setProperty("sys.default.path.config", path);
        System.setProperty("sys.default.LogFile", path + "/temp/SendMicroValidation.log");
        System.setProperty("sys.default.Signature", path + "/images/180492153_794111184853448_4384690834553115402_n.png");
        
        writeLog("++++++++++++++++++++++++++++++++++++++++++++++");
        writeLog("SendMicroValidation Initiated: " + Calendar.getInstance().getTime());
        
        instance = new GRiderX("gRider");
        instance.setOnline(true);

        switch(args.length){
            case 0:
                cDivision = "2";    //Mobile Phone
                dPeriodFr = "2023-01-16";
                sRqstdByx = "M033070005";
                break;
            case 2:
                cDivision = args[1];
                sRqstdByx = "M033070005";
                dPeriodFr = SQLUtil.dateFormat(getPeriod(),SQLUtil.FORMAT_SHORT_DATE);
                break;
            case 4:
                cDivision = args[1];
                dPeriodFr = args[2];
                sRqstdByx = args[3];
                break;
            default:
                System.out.println("Invalid parameters detected...");
                System.exit(0);
        }
        
        String sql = "SELECT sTransNox" + 
                    " FROM Payroll_Validation_Micro_Master" + 
                    " WHERE cDivision = " + SQLUtil.toSQL(cDivision) + 
                      " AND dPeriodFr = " + SQLUtil.toSQL(dPeriodFr) ;
        ResultSet rs = instance.executeQuery(sql);
        
        //Create the validation transaction
        if(rs.next()){
            sTransNox = rs.getString("sTransNox");
        }
        else{
            sTransNox = createValidation();
        }
        
        //check if creation of validation transaction failed
        if(sTransNox.isEmpty()){
            writeLog("Unable to create Micro Validation for the following request:" );
            writeLog("dPeriodFr = " + SQLUtil.toSQL(dPeriodFr));
            writeLog("cDivision = " + SQLUtil.toSQL(cDivision));
            writeLog("Sending failed!");
            writeLog("SendMicroValidation Ended: " + Calendar.getInstance().getTime());
            System.exit(1);
        }
        
        //open the validation transaction 
        sql = "SELECT b.sDivsnDsc, b.sPstAudtr, a.*" + 
             " FROM Payroll_Validation_Micro_Master a" + 
                 " LEFT JOIN Division b ON a.cDivision = b.sDivsnCde" +
             " WHERE a.sTransNox = " + SQLUtil.toSQL(sTransNox) ; 
        ResultSet rsMstr = instance.executeQuery(sql);
        if(!rsMstr.next()){
            writeLog("Send Failed! Micro Validation with TransNo " + sTransNox + "is not existing!");
            writeLog("SendMicroValidation Ended: " + Calendar.getInstance().getTime());
            System.exit(1);
        }
        
        sql = "SELECT b.sCompnyNm, a.*" + 
             " FROM Payroll_Validation_Micro_Employee a" +
                " LEFT JOIN Client_Master b ON a.sEmployID = b.sClientID" +
             " WHERE a.sTransNox = " + SQLUtil.toSQL(sTransNox) ; 
        ResultSet rsEmployee = instance.executeQuery(sql);
        if(!rsEmployee.next()){
            writeLog("Send Failed! Unable to open detail for Micro Validation with TransNo " + sTransNox + "is not existing!");
            writeLog("SendMicroValidation Ended: " + Calendar.getInstance().getTime());
            System.exit(1);
        }
        
        JSONArray xrows = new JSONArray();
        
        do{
             sql = "SELECT b.sEmployID" +
                       ", a.dPeriodFr" +
                       ", a.dPeriodTo" +
                       ", @xBasicSal := CAST(REVERSE(DECODE(UNHEX(b.nBasicSal), 'PETMGR')) AS DECIMAL) xBasicSal" +
                       ", @nDailyRt1 := ROUND(@xBasicSal / IF(c.cPayDivCd = '3' OR a.sEmpTypID = 'R', 13, b.nAttendnc + b.nAbsencex), 2) AS nDailyRt1" +
                       ", @nAbsencex := ROUND(b.nAbsencex * @nDailyRt1, 2) nAbsencex" +
                       ", CAST(REVERSE(DECODE(UNHEX(b.nHolidayx), 'PETMGR')) AS DECIMAL) nHolidayx" +
                       ", CAST(REVERSE(DECODE(UNHEX(b.nTardines), 'PETMGR')) AS DECIMAL) nTardines" +
                       ", CAST(REVERSE(DECODE(UNHEX(b.nUndrTime), 'PETMGR')) AS DECIMAL) nUndrTime" +
                       ", CAST(REVERSE(DECODE(UNHEX(b.nOverTime), 'PETMGR')) AS DECIMAL) nOverTime" +
                       ", CAST(REVERSE(DECODE(UNHEX(b.nBenefits), 'PETMGR')) AS DECIMAL) nBenefits" +
                       ", CAST(REVERSE(DECODE(UNHEX(b.nNightDif), 'PETMGR')) AS DECIMAL) nNightDif" +
                       ", CAST(REVERSE(DECODE(UNHEX(b.nOTNightD), 'PETMGR')) AS DECIMAL) nOTNightD" +
                       ", b.nAdjustxx" +
                       ", b.nAttendnc" + 
                       ", a.sEmpTypID" +
                       ", d.sBranchNm" +
                    " FROM Payroll_Period a" +
                          " LEFT JOIN Payroll_Summary_New b ON a.sPayPerID = b.sPayPerID" +
                          " LEFT JOIN Branch_Others c ON b.sBranchCD = c.sBranchCD" +
                          " LEFT JOIN Branch d ON c.sBranchCD = d.sBranchCD" +
                   " WHERE a.dPeriodFr BETWEEN DATE_ADD(" + SQLUtil.toSQL(dPeriodFr) + ", INTERVAL -3 MONTH) AND " + SQLUtil.toSQL(dPeriodFr) + 
                     " AND b.sEmployID = " + SQLUtil.toSQL(rsEmployee.getString("sEmployID")) +
                  " ORDER BY dPeriodTo DESC LIMIT 6";
            ResultSet rsx = instance.executeQuery(sql);
            
            String emptype = "";
            while(rsx.next()){
                JSONObject xrow = new JSONObject();
                if(rsx.getRow() == 1){
                    emptype = rsx.getString("sEmpTypID");
                }
                if(emptype.equalsIgnoreCase("R")){
                    xrow.put("sField01", rsEmployee.getString("sCompnyNm") + "(Regular) - " + rsx.getString("sBranchNm"));
                }
                else{
                    xrow.put("sField01", rsEmployee.getString("sCompnyNm") + "(Probationary) - " + rsx.getString("sBranchNm"));
                }
                System.out.println(xrow.get("sField01"));
                
                if(emptype.equalsIgnoreCase("R") & rsx.getString("sEmpTypID").equalsIgnoreCase("T")){
                    xrow.put("sField02", SQLUtil.dateFormat(rsx.getDate("dPeriodFr"), "MM/dd") + " - " + SQLUtil.dateFormat(rsx.getDate("dPeriodTo"), "MM/dd/YYYY") + "*" );
                }
                else{
                    xrow.put("sField02", SQLUtil.dateFormat(rsx.getDate("dPeriodFr"), "MM/dd") + " - " + SQLUtil.dateFormat(rsx.getDate("dPeriodTo"), "MM/dd/YYYY"));
                }
                System.out.println(xrow.get("sField02"));
                
                xrow.put("nField01", rsx.getInt("nAttendnc"));
                xrow.put("lField01", rsx.getDouble("xBasicSal"));
                xrow.put("lField02", rsx.getDouble("nBenefits"));
                xrow.put("lField03", rsx.getDouble("nOverTime"));
                xrow.put("lField04", rsx.getDouble("nHolidayx"));
                xrow.put("lField05", rsx.getDouble("nNightDif") + rsx.getDouble("nOTNightD"));
                xrow.put("lField06", rsx.getDouble("nAdjustxx"));
                xrow.put("lField07", rsx.getDouble("nAbsencex"));
                xrow.put("lField08", rsx.getDouble("nUndrTime"));
                xrow.put("lField09", rsx.getDouble("nTardines"));
                xrows.add(xrow);
            }
        }while(rsEmployee.next());
        
        Map<String, Object> params = new HashMap<>();
        params.put("sCompnyNm", "Guanzon Group of Companies");
        params.put("sBranchNm", "Guanzon Central Office");
        params.put("sAddressx", "Guanzon Bldg., Perez Blvd., Dagupan City");
        params.put("sReportNm", "Micro Payroll Validation - " + rsMstr.getString("sDivsnDsc")) ;
        params.put("sReportDt", "For Payroll Period: " + rsMstr.getString("sValidDsc"));
        params.put("sPrintdBy", "SYSTEM GENERATED");
        
       //prepare stream input for the creation of JASPER REPORT
        InputStream stream = new ByteArrayInputStream(xrows.toJSONString().getBytes("UTF-8"));
        JsonDataSource jrjson = new JsonDataSource(stream);
        //System.out.println(xrows.toJSONString());
        //create jasper report
        JasperPrint print = JasperFillManager.fillReport(path + "/reports/PayrollMicro1.jasper", params, jrjson);
        String filename = "Micro-" + cDivision + "-" + dPeriodFr;

        //System.out.println(System.getProperty("sys.default.path.temp") + "/payslip/" + filename + "X.pdf");
        //export jasper report to PDF
        JasperExportManager.exportReportToPdfFile(print, System.getProperty("sys.default.path.temp") + "/payslip/" + filename + "X.pdf");

        JSONObject param = new JSONObject();
        File filex;
        param.put("from", "Payroll Validation Utility");
        System.out.println(rsMstr.getString("sPstAudtr"));
        param.put("to", rsMstr.getString("sPstAudtr"));
        param.put("subject", rsMstr.getString("sDivsnDsc") + " - Micro Payroll Validation for - " + rsMstr.getString("sValidDsc") + "(Detail)");
        String body = "Good Day,\n\n" +
                      "You were selected by the company's CEO to perform a post-validation checking of salary!\n" + 
                      "Kindly see attached file for this cut off post salary validation. Ten (10) randomly selected employees were system generated for you to be able to check. " +
                      "When you noticed an unusual changes in figure, please do contact the Payroll Master on your division for rechecking. " +
                      "Your continued cooperation is much appreciated. Thank you and keep safe.\n\n\n" +
                      "THIS EMAIL IS SYSTEM GENERATED!!!";
        param.put("body", body);
        filex =  new File(System.getProperty("sys.default.path.temp") + "/payslip/" + filename + "X.pdf");
        param.put("data1", encodeFileToBase64Binary(filex));
        param.put("filename1", filex.getName());

        if(!sendMail(param)){
            writeLog("Unable to send " + filename + "X.pdf");
        }

        Calendar cal = Calendar.getInstance();
        sql = "UPDATE Payroll_Validation_Micro_Master" +
             " SET cMailSent = '1'" +   
                ", dMailSent = " + SQLUtil.toSQL(cal) +                  
             " WHERE sTransNox = " + SQLUtil.toSQL(sTransNox); 
        instance.executeQuery(sql, "Payroll_Validation_Micro_Master", instance.getBranchCode(), "" );
        
        writeLog("Sent successfully " + filename + "X.pdf");
        writeLog("SendMicroValidation Ended: " + Calendar.getInstance().getTime());
        
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
            System.out.println(oJson.get("parent"));
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

    private static ResultSet requestValidation(String emptype, int ctr){
        String sql;
        String transno="";
        
        sql = "SELECT a.sBranchCD, b.dPeriodFr, b.dPeriodTo, a.sEmployID, d.sCompnyNm, e.sDivsnDsc, e.sDivsnMgr" +
             " FROM Payroll_Summary_New a" +
                " LEFT JOIN Payroll_Period b ON a.sPayPerID =  b.sPayPerID" +
                " LEFT JOIN Branch_Others c ON a.sBranchCD = c.sBranchCD" +
                " LEFT JOIN Client_Master d ON a.sEmployID = d.sClientID" +
                " LEFT JOIN Division e ON c.cDivision = e.sDivsnCde" +
             " WHERE b.dPeriodFr = " + SQLUtil.toSQL(dPeriodFr) + 
               " AND b.sEmpTypID = " + SQLUtil.toSQL(emptype) +
               " AND TRIM(e.sClustrCD) = " + SQLUtil.toSQL(cDivision) +
               " AND (a.nAbsencex > 0 OR a.nTardines <> '847920CA7E84DD435E' OR a.nUndrTime <> '847920CA7E84DD435E' OR a.nAdjustxx <> 0)" +
             " ORDER BY RAND()" +
             " LIMIT " + Integer.toString(ctr);
//               " AND (a.nAbsencex > 0)" +

        System.out.println(sql);
        ResultSet rs = instance.executeQuery(sql);
        return(rs);
    }
   
    private static String createValidation() throws SQLException{
        String sql;
        String transno="";
        
        ResultSet rs = requestValidation("R", 15);
        //rs = instance.executeQuery(sql);
        
        if(!rs.next()){
            return "";
        }
        
        transno = MiscUtil.getNextCode("Payroll_Validation_Micro_Master", "sTransNox", true, instance.getConnection(), instance.getBranchCode());
        Calendar cal = Calendar.getInstance();
        String validsc = SQLUtil.dateFormat(rs.getDate("dPeriodFr"), "MMM dd") + " - " + SQLUtil.dateFormat(rs.getDate("dPeriodTo"), "MMM dd, YYYY");
        
        sql = "INSERT INTO Payroll_Validation_Micro_Master" + 
             " SET sTransNox = " + SQLUtil.toSQL(transno) + 
                ", sEmpTypID = ''" +  
                ", cPayDivCd = ''" +  
                ", cDivision = " + SQLUtil.toSQL(cDivision) + 
                ", dPeriodFr = " + SQLUtil.toSQL(rs.getDate("dPeriodFr")) + 
                ", dPeriodTo = " + SQLUtil.toSQL(rs.getDate("dPeriodTo")) + 
                ", sValidatr = " + SQLUtil.toSQL(rs.getString("sDivsnMgr")) + 
                ", dRequestd = " + SQLUtil.toSQL(cal) +
                ", cMailSent = '0'" +
                ", cTranStat = '0'" +
                ", sValidDsc = " + SQLUtil.toSQL(validsc);
        System.out.println(sql);
        instance.executeQuery(sql, "Payroll_Validation_Micro_Master", rs.getString("sBranchCD"), "");
        
        int lnctr =0;
        do{
            lnctr++;
            sql = "INSERT INTO Payroll_Validation_Micro_Employee" + 
                 " SET sTransNox = " + SQLUtil.toSQL(transno) + 
                    ", nEntryNox = " + SQLUtil.toSQL(lnctr) + 
                    ", sEmployID = " + SQLUtil.toSQL(rs.getString("sEmployID"));
            instance.executeQuery(sql, "Payroll_Validation_Micro_Employee", rs.getString("sBranchCD"), "");
            System.out.println(sql);
        } while(rs.next());

        rs = requestValidation("T", 5);
        //rs = instance.executeQuery(sql);
        
        while(rs.next()){
            lnctr++;
            sql = "INSERT INTO Payroll_Validation_Micro_Employee" + 
                 " SET sTransNox = " + SQLUtil.toSQL(transno) + 
                    ", nEntryNox = " + SQLUtil.toSQL(lnctr) + 
                    ", sEmployID = " + SQLUtil.toSQL(rs.getString("sEmployID"));
            instance.executeQuery(sql, "Payroll_Validation_Micro_Employee", rs.getString("sBranchCD"), "");
            System.out.println(sql);
        }
        
        return transno;
    }

    private static Date getPeriod() throws SQLException{
        String sql = "SELECT *" + 
                     " FROM Payroll_Period" +
                     " WHERE sPayPerID LIKE 'M001%' AND sEmpTypID = 'R' AND cPostedxx = '2'" +
                     " ORDER BY sPayPerID DESC LIMIT 1;";
        ResultSet rs = instance.executeQuery(sql);
        
        if(rs.next()){
            return rs.getDate("dPeriodFr");
        }
        
        return null;
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
