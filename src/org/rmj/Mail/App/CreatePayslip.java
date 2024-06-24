/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.rmj.mail.App;

import com.itextpdf.text.DocumentException;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
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
import java.util.logging.Level;
import java.util.logging.Logger;
import net.sf.jasperreports.engine.JRException;
import net.sf.jasperreports.engine.JasperExportManager;
import net.sf.jasperreports.engine.JasperFillManager;
import net.sf.jasperreports.engine.JasperPrint;
import net.sf.jasperreports.engine.data.JsonDataSource;
import org.apache.commons.codec.binary.Base64;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.rmj.appdriver.SQLUtil;
import org.rmj.appdriver.agent.GRiderX;
import org.rmj.lib.mail.SendMail;
import org.rmj.lib.net.LogWrapper;
import org.rmj.mail.lib.JsonDataSourcePayslip;

/**
 *
 * @author sayso
 */
public class CreatePayslip {
    private static GRiderX instance = null;
    private static SendMail pomail;
    private static Date period;
    private static String filter;

    public static void main(String args[]) throws SQLException{
        String path;
        if(System.getProperty("os.name").toLowerCase().contains("win")){
            path = "D:/GGC_Java_Systems";
        }
        else{
            path = "/srv/GGC_Java_Systems";
        }
        System.setProperty("sys.default.path.temp", path + "/temp");
        System.setProperty("sys.default.path.config", path);
        System.setProperty("sys.default.LogFile", path + "/temp/CreatePayslip.log");
        System.setProperty("sys.default.Signature", path + "/images/180492153_794111184853448_4384690834553115402_n.png");
        
        writeLog("++++++++++++++++++++++++++++++++++++++++++++++");
        writeLog("CreatePayslip Initiated: " + Calendar.getInstance().getTime());

        //initialize driver
        instance = new GRiderX("gRider");
        instance.setOnline(true);
        
        if(args.length == 0){
            period = getPeriod();
            filter = "";
        }
        else if(args.length <= 2){
            period = SQLUtil.toDate(args[0], SQLUtil.FORMAT_SHORT_DATE);

            if(period == null){
                System.out.println("Please set the correct date for the start of payroll period with this format: YYYY-MM-DD");
                writeLog("Please set the correct date for the start of payroll period with this format: YYYY-MM-DD" );
                writeLog("CreatePaySlip Stopped: " + Calendar.getInstance().getTime());
                System.exit(0);
            }
            
            if(args.length == 1){
                filter = "";
            }
            else{
                
                //by division id
                String xfilter[] = args[1].split("=");
                if(xfilter[0].equalsIgnoreCase("d")){
                    filter = "i.cPayDivCd = " + SQLUtil.toSQL(xfilter[1]);
                }
                //by branch code
                else if(xfilter[0].equalsIgnoreCase("b")){
                    filter = "b.sBranchCD = " + SQLUtil.toSQL(xfilter[1]);
                }
                //by employee id
                else if(xfilter[0].equalsIgnoreCase("e")){
                    filter = "b.sEmployID = " + SQLUtil.toSQL(xfilter[1]);
                }
                //by employee type
                else if(xfilter[0].equalsIgnoreCase("t")){
                    filter = "";
                }
                //not inside any of the filter
                else{
                    writeLog("Please set the correct filter...");
                    System.out.println("Please set the correct filter...");
                    writeLog("CreatePaySlip Stopped: " + Calendar.getInstance().getTime());
                    System.exit(0);
                }
            }
        }
        else{
            writeLog("CreatePaySlip <date_from> [d=div_id | e=emp_id | b=branc_cd | t=type_id]");
            writeLog("CreatePaySlip Stopped: " + Calendar.getInstance().getTime());
            System.out.println("CreatePaySlip <date_from> [d=div_id | e=emp_id | b=branc_cd | t=type_id]");
            System.exit(0);
        }
        
        if(filter.isEmpty()){
            filter = "(IFNULL(g.sEmailAdd, '') <> '' OR IFNULL(j.sUserIDxx, '') <> '')";
        }
        else{
            filter = filter + " AND (IFNULL(g.sEmailAdd, '') <> '' OR IFNULL(j.sUserIDxx, '') <> '')";
        }

        //filter = filter + " AND (IFNULL(g.sEmailAdd, '') NOT LIKE '%gmail.com%')";
        
        JsonDataSourcePayslip oPay = new JsonDataSourcePayslip(instance, period, filter);
        JSONArray json_array = oPay.CreateJson();
        
        if(json_array != null){
            instance.beginTrans();
            InputStream stream = null;
            
            try {

                File filex =  new File(System.getProperty("sys.default.Signature"));
                String sImage = encodeFileToBase64Binary(filex);
                Map<String, Object> params = new HashMap<>();
                params.put("psSignature", sImage);

                JSONArray xrows = new JSONArray();

                for (Object json : json_array) {
                    String lsSQL;
                    JSONObject xrow = (JSONObject) json;
                    xrows = new JSONArray();
                    xrows.add(json);
                    JsonDataSource jrjson;
                    JasperPrint print;
                    //prepare stream input for the creation of JASPER REPORT
                    stream = new ByteArrayInputStream(xrows.toJSONString().getBytes("UTF-8"));
                    jrjson = new JsonDataSource(stream);
                    //System.out.println(xrows.toJSONString());
                    //create jasper report
                    print = JasperFillManager.fillReport(path + "/reports/Payslip.jasper", params, jrjson);
                    String filename = ((String)xrow.get("sEmployID")) + ((String)xrow.get("sPayPerID"));

                    //System.out.println(System.getProperty("sys.default.path.temp") + "/payslip/" + filename + "X.pdf");
                    //export jasper report to PDF
                    JasperExportManager.exportReportToPdfFile(print, System.getProperty("sys.default.path.temp") + "/payslip/" + filename + "X.pdf");
                    //System.out.println(System.getProperty("sys.default.path.temp") + "/payslip/" + filename + "X.pdf");
                    //set password in our jasper report
                    //02645850
                    String user = ((String)xrow.get("sPassword")).equalsIgnoreCase("****") || ((String)xrow.get("sPassword")).isEmpty() ? "1234" : ((String)xrow.get("sPassword"));
                    PDFPassword.encrypt_pdf(System.getProperty("sys.default.path.temp") + "/payslip/" + filename + "X.pdf", System.getProperty("sys.default.path.temp") + "/payslip/" + filename + ".pdf", user.getBytes(), "02645850".getBytes());
                    //Delete file
                    File file = new File(System.getProperty("sys.default.path.temp") + "/payslip/" + filename + "X.pdf");
                    file.delete();

                    lsSQL = "UPDATE Payroll_Summary_New" +
                           " SET cMailSent = '1'" +
                           " WHERE sPayPerID = " + SQLUtil.toSQL((String)xrow.get("sPayPerID")) +
                             " AND sEmployID = " + SQLUtil.toSQL((String)xrow.get("sEmployID"));
                    instance.executeUpdate(lsSQL);

                }
            } catch (UnsupportedEncodingException ex) {
                writeLog(ex.getMessage());
                instance.rollbackTrans();
            } catch (JRException | DocumentException | IOException ex) {
                writeLog(ex.getMessage());
                instance.rollbackTrans();
            } catch (Exception ex) {
                writeLog(ex.getMessage());
                instance.rollbackTrans();
            } finally {
                try {
                    stream.close();
                } catch (IOException ex) {
                    writeLog(ex.getMessage());
                    ex.printStackTrace();
                }
            }
            
            instance.commitTrans();
        }
        writeLog("CreatePaySlip Stopped: " + Calendar.getInstance().getTime());
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
   
    private static String encodeFileToBase64Binary(File file) throws Exception{
         FileInputStream fileInputStreamReader = new FileInputStream(file);
         byte[] bytes = new byte[(int)file.length()];
         fileInputStreamReader.read(bytes);
         return new String(Base64.encodeBase64(bytes), "UTF-8");
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
}
