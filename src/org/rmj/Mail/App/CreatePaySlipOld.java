/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.rmj.mail.App;

import com.itextpdf.text.DocumentException;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Calendar;
import java.util.Date;
import java.util.logging.Level;
import java.util.logging.Logger;
import net.sf.jasperreports.engine.JRException;
import net.sf.jasperreports.engine.JasperFillManager;
import net.sf.jasperreports.engine.JasperPrint;
import net.sf.jasperreports.engine.data.JsonDataSource;
import net.sf.jasperreports.view.JasperViewer;
import net.sf.jasperreports.engine.JasperExportManager;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.rmj.appdriver.MiscUtil;
import org.rmj.appdriver.SQLUtil;
import org.rmj.appdriver.agent.GRiderX;
import org.rmj.lib.mail.SendMail;
import org.rmj.lib.net.LogWrapper;

/**
 *
 * @author sayso
 */
public class CreatePaySlipOld {
    private static GRiderX instance = null;
    private static SendMail pomail;
    private static LogWrapper logwrapr;
    private static Date period;
    private static String filter;
    
    public static void main(String args[]){
        if(args.length == 0){
            //Make sure that the utility will be executed during the 1st and 16th day of the month only
//            int day = java.time.LocalDate.now().getDayOfMonth();
//            if(!(day == 6 || day == 21)){
//                System.out.println(java.time.LocalDate.now());
//                System.exit(0);
//            }
//            
//            //Get the beginning day of the payroll period
//            if(day == 6){
//                Date prev = SQLUtil.toDate(java.time.LocalDate.now().minusDays(6).toString(), SQLUtil.FORMAT_SHORT_DATE);
//                Calendar cal = Calendar.getInstance();
//                cal.setTime(prev);
//                cal.set(Calendar.DAY_OF_MONTH, 16);
//                period = cal.getTime();
//            }
//            else{
//                Date prev = SQLUtil.toDate(java.time.LocalDate.now().minusDays(6).toString(), SQLUtil.FORMAT_SHORT_DATE);
//                Calendar cal = Calendar.getInstance();
//                cal.setTime(prev);
//                cal.set(Calendar.DAY_OF_MONTH, 1);
//                period = cal.getTime();
//            }
            
            period = SQLUtil.toDate("2019-12-16", SQLUtil.FORMAT_SHORT_DATE);
//            System.out.println(period);
//            System.exit(0);

            filter = "b.sEmployID = 'M00110013556'";

//            filter = "";
        }
        else if(args.length <= 2){
            period = SQLUtil.toDate(args[0], SQLUtil.FORMAT_SHORT_DATE);

            if(period == null){
                System.out.println("Please set the correct date for the start of payroll period with this format: YYYY-MM-DD");
                System.exit(0);
            }
            
            if(args.length == 1){
                filter = "";
            }
            else{
                //by division id
                String xfilter[] = args[1].split("=");
                if(xfilter[0].equalsIgnoreCase("d")){
                    filter = "";
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
                    System.out.println("Please set the correct filter...");
                    System.exit(0);
                }
            }
        }
        else{
            System.out.println("CreatePaySlip <date_from> [d=div_id | e=emp_id | b=branc_cd | t=type_id]");
            System.exit(0);
        }
        
        //set the config and temp path
        String path;
        if(System.getProperty("os.name").toLowerCase().contains("win")){
            path = "D:/GGC_Java_Systems";
        }
        else{
            path = "/srv/GGC_Java_Systems";
        }
        System.setProperty("sys.default.path.temp", path + "/temp");
        System.setProperty("sys.default.path.config", path);

        //set the log file
        logwrapr = new LogWrapper("RMJMail.CreatePaySlip", System.getProperty("sys.default.path.temp") + "/CreatePaySlip.log");

        //initialize driver
        instance = new GRiderX("gRider");
        instance.setOnline(true);
        
        String lsSQL;
        lsSQL = getMainQuery(period, filter, true);
        System.out.println(lsSQL);
        
        ResultSet loRS = instance.executeQuery(lsSQL);
        JSONArray xrows = new JSONArray();

        try {
            
            int row_ctr =0;
            instance.beginTrans();
            while(loRS.next()){
                System.out.println("Processing " + loRS.getString("sBranchNm") + ": "+ loRS.getString("sEmployNm"));
                JSONObject xrow = new JSONObject();
                xrow.put("sField01", loRS.getString("sEmployNm") + (loRS.getString("cPostedxx").equalsIgnoreCase("2") ? "" : "»NOT POSTED"));
                xrow.put("sField02", SQLUtil.dateFormat(loRS.getDate("dPeriodFr"), "MMM dd, yyyy") + " - " + SQLUtil.dateFormat(loRS.getDate("dPeriodTo"), "MMM dd, yyyy"));
                xrow.put("sField03", loRS.getString("sBranchNm") + " - " + loRS.getString("sDeptName"));
                xrow.put("sField04", loRS.getString("sPositnNm"));
                xrow.put("sField21", SQLUtil.dateFormat(loRS.getDate("dCovergFm"), "MMM dd, yyyy") + " - " + SQLUtil.dateFormat(loRS.getDate("dCovergTo"), "MMM dd, yyyy"));
                
                xrow.put("nField01", loRS.getDouble("nLveCredt"));
                xrow.put("nField02", loRS.getDouble("nAttendnc"));

                xrow.put("lField01", loRS.getDouble("nBasicSal"));
                
                //get daily pay
                double lnDailyPay;
                //is employee an probationary and not a monarch employee
                if(loRS.getString("sEmpTypID").equalsIgnoreCase("T") && !loRS.getString("cDivision").equalsIgnoreCase("3")){
                    lnDailyPay = (double) Math.round(loRS.getDouble("nBasicSal") / (loRS.getDouble("nAttendnc") + loRS.getDouble("nAbsencex")) * 100) / 100;
                }    
                else{
                    lnDailyPay = (double) Math.round(loRS.getDouble("nBasicSal") / 13 * 100) / 100;
                }
                
                xrow.put("lField02", loRS.getDouble("nOvertime"));
                xrow.put("lField03", loRS.getDouble("nHolidayx"));
                xrow.put("lField07", loRS.getDouble("nTardines"));
                xrow.put("lField08", loRS.getDouble("nUndrTime"));
                xrow.put("lField09", (double) Math.round(loRS.getDouble("nAbsencex") * lnDailyPay * 100) / 100);
         
                //System.out.println(loRS.getDouble("nTardines"));
                
                if(!ExtractBenefits(loRS.getString("sPayPerID"), loRS.getString("sEmployID"), xrow)){
                    System.exit(0);
                }
                
                if(!ExtractGovtDeductions(loRS.getString("sPayPerID"), loRS.getString("sEmployID"), xrow)){
                    System.exit(0);
                }
                
                if(!ExtractDeductions(loRS.getString("sPayPerID"), loRS.getString("sEmployID"), xrow)){
                    System.exit(0);
                }
                
                if(!ExtractLoans(loRS.getString("sPayPerID"), loRS.getString("sEmployID"), xrow)){
                    System.exit(0);
                }
                
                if(!ExtractAdjustments(loRS.getString("sPayPerID"), loRS.getString("sEmployID"), xrow, loRS)){
                    System.exit(0);
                }
                
                //System.out.println(xrow.toJSONString());
                
                xrows = new JSONArray();
                xrows.add(xrow);
                
                //row_ctr++;
                //if(row_ctr >= 2){
                    JsonDataSource jrjson; 
                    JasperPrint print;
                    
                    //prepare stream input for the creation of JASPER REPORT
                    InputStream stream = new ByteArrayInputStream(xrows.toJSONString().getBytes("UTF-8"));
                    jrjson = new JsonDataSource(stream); 
                    //System.out.println(xrows.toJSONString());

                    //create jasper report
                    print = JasperFillManager.fillReport(path + "/reports/Payslip.jasper", null, jrjson);
                    String filename = loRS.getString("sEmployID") + loRS.getString("sPayPerID");
                    //System.out.println(System.getProperty("sys.default.path.temp") + "/payslip/" + filename + "X.pdf");

                    //export jasper report to PDF    
                    JasperExportManager.exportReportToPdfFile(print, System.getProperty("sys.default.path.temp") + "/payslip/" + filename + "X.pdf");                

                    //set password in our jasper report
                    //02645850
                    String user = loRS.getString("sPassword").equalsIgnoreCase("****") || loRS.getString("sPassword").isEmpty() ? "1234" : loRS.getString("sPassword");
                    PDFPassword.encrypt_pdf(System.getProperty("sys.default.path.temp") + "/payslip/" + filename + "X.pdf", System.getProperty("sys.default.path.temp") + "/payslip/" + filename + ".pdf", user.getBytes(), "02645850".getBytes());
                    
                    //Delete file
                    File file = new File(System.getProperty("sys.default.path.temp") + "/payslip/" + filename + "X.pdf");
                    file.delete();
                    
                    //J;asperViewer jv = new JasperViewer(print, false);     
                    //jv.setVisible(true);

                    lsSQL = "UPDATE Payroll_Summary_New" +
                           " SET cMailSent = '1'" +
                           " WHERE sPayPerID = " + SQLUtil.toSQL(loRS.getString("sPayPerID")) +
                             " AND sEmployID = " + SQLUtil.toSQL(loRS.getString("sEmployID"));
                    instance.executeUpdate(lsSQL);
                    //loRS.last();
                    //System.exit(0);
                //}
            }
            instance.commitTrans();
        } catch (SQLException ex) {
            ex.printStackTrace();
            instance.rollbackTrans();
        } catch (UnsupportedEncodingException ex) {
            ex.printStackTrace();
            instance.rollbackTrans();
        } catch (JRException ex) {
            ex.printStackTrace();
            instance.rollbackTrans();
        } catch (IOException ex) {
            ex.printStackTrace();
            instance.rollbackTrans();
        } catch (DocumentException ex) {
            ex.printStackTrace();
            instance.rollbackTrans();
        }
    }

    private static boolean ExtractBenefits(String fsPeriodID, String fsEmployID, JSONObject row){
        String lsSQL;
        
        lsSQL = "SELECT" +
                       "  b.sBeneftNm" +
                       ", CAST(REVERSE(DECODE(UNHEX(a.nAmountxx), 'PETMGR')) AS DECIMAL) nAmountxx" +
               " FROM Payroll_Benefits a" +
                    " LEFT JOIN Benefit b ON a.sBeneftID = b.sBeneftID" +
               " WHERE a.sPayPerID = " + SQLUtil.toSQL(fsPeriodID) +
                 " AND a.sEmployID = " + SQLUtil.toSQL(fsEmployID);
        ResultSet loRS = instance.executeQuery(lsSQL);
        
        //set label name
        row.put("sField05", "");
        row.put("sField06", "");
        row.put("sField07", "");

        row.put("lField04", 0.00);
        row.put("lField05", 0.00);
        row.put("lField06", 0.00);
        
        try {
            int ctr=0;
            double other=0;
            double last=0;
            while(loRS.next()){
                if(ctr <= 2){
                    row.put("sField" + String.format("%02d", ctr+5), loRS.getString("sBeneftNm").toUpperCase());
                    row.put("lField" + String.format("%02d", ctr+4), loRS.getDouble("nAmountxx"));
                    last = loRS.getDouble("nAmountxx");
                }
                else{
                    other += loRS.getDouble("nAmountxx");
                }
                ctr++;
            }
            
            if(other > 0){
                row.put("sField07", "MISC BENFTS");
                row.put("lField06", other + last);
            }
            
            return true;
            
        } catch (SQLException ex) {
            ex.printStackTrace();
            return false;
        }
    }
            
    private static boolean ExtractGovtDeductions(String fsPeriodID, String fsEmployID, JSONObject row){
        String lsSQL;
        lsSQL = "SELECT" +
                       "  a.sDeductID" +
                       ", c.sDeductNm" +
                       ", a.nAmountxx" +
                       ", a.cSubsidzd" +
               " FROM Payroll_Govt_Deductions a" +
                    " LEFT JOIN Deduction c ON a.sDeductID = c.sDeductID" +
               " WHERE a.sPayPerID = " + SQLUtil.toSQL(fsPeriodID) +
                 " AND a.sEmployID = " + SQLUtil.toSQL(fsEmployID);
        ResultSet loRS = instance.executeQuery(lsSQL);
        
        //set label name
        row.put("sField08", "");

        row.put("lField10", 0.00);
        row.put("lField11", 0.00);
        row.put("lField12", 0.00);
        row.put("lField13", 0.00);

        try {
            while(loRS.next()){
                if(loRS.getString("cSubsidzd").equalsIgnoreCase("0")){
                    //Is SSS?
                    if(loRS.getString("sDeductID").equalsIgnoreCase("11003")){
                        row.put("lField10", loRS.getDouble("nAmountxx"));
                    }
                    //Is HDMF
                    else if(loRS.getString("sDeductID").equalsIgnoreCase("11002")){
                        row.put("lField11", loRS.getDouble("nAmountxx"));
                    }
                    //Is PHILHEALTH
                    else if(loRS.getString("sDeductID").equalsIgnoreCase("11001")){
                        row.put("lField12", loRS.getDouble("nAmountxx"));
                    }
                    //Is WTAX
                    else if(loRS.getString("sDeductID").equalsIgnoreCase("11010")){
                        row.put("lField13", loRS.getDouble("nAmountxx"));
                        row.put("sField08", "WTAX");
                    }
                }
            }
            
            return true;
            
        } catch (SQLException ex) {
            ex.printStackTrace();
            return false;
        }
    }
    
    private static boolean ExtractDeductions(String fsPeriodID, String fsEmployID, JSONObject row){
        String lsSQL;
        lsSQL = "SELECT" +
                       "  c.sDeductNm" +
                       ", a.nAmountxx" +
               " FROM Payroll_Deductions a" +
                    " LEFT JOIN Employee_Deduction b ON a.sReferNox = b.sTransNox" +
                    " LEFT JOIN Deduction c ON b.sDeductID = c.sDeductID" +
               " WHERE a.sPayPerID = " + SQLUtil.toSQL(fsPeriodID) +
                 " AND a.sEmployID = " + SQLUtil.toSQL(fsEmployID);

        ResultSet loRS = instance.executeQuery(lsSQL);

        int ctr;
        if((Double)row.get("lField13") == 0){
            ctr = 8;
        }
        else{
            ctr = 9;
        }
        
        //set label name
        row.put("sField09", "");
        row.put("sField10", "");

        row.put("lField14", 0.00);
        row.put("lField15", 0.00);
        
        try {
            double other=0;
            double last=0;
            while(loRS.next()){
                if(ctr <= 10){
                    row.put("sField" + String.format("%02d", ctr), loRS.getString("sDeductNm").toUpperCase());
                    row.put("lField" + String.format("%02d", ctr+5), loRS.getDouble("nAmountxx"));
                    last = loRS.getDouble("nAmountxx");
                }
                else{
                    other += loRS.getDouble("nAmountxx");
                }
                ctr++;
            }
            
            if(other > 0){
                row.put("sField10", "MISC DED(s)");
                row.put("lField15", other + last);
            }
            
            return true;
            
        } catch (SQLException ex) {
            ex.printStackTrace();
            return false;
        }
    }

    private static boolean ExtractLoans(String fsPeriodID, String fsEmployID, JSONObject row){
        String lsSQL;
        lsSQL = "SELECT" +
                       "  a.sReferNox" +
                       ", c.sLoanName" +
                       ", a.nAmountxx" +
               " FROM Payroll_Loans a" +
                   ", Employee_Loan_Master b" +
                   ", Loans c" +
               " WHERE a.sReferNox = b.sTransNox" +
                 " AND b.sLoanIDxx = c.sLoanIDxx" +
                 " AND a.sPayPerID = " + SQLUtil.toSQL(fsPeriodID) +
                 " AND a.sEmployID = " + SQLUtil.toSQL(fsEmployID) +
                 " AND a.cIntegSys = '0'" + 
               " UNION " +
               "SELECT" +
                       "  a.sReferNox" +
                       ", CASE b.cLoanType WHEN '0' THEN 'MC Sales' WHEN '1' THEN 'Sidecar' ELSE 'Others' END sLoanName" +
                       ", a.nAmountxx" +
               " FROM Payroll_Loans a" +
                   ", MC_AR_Master b" +
               " WHERE a.sReferNox = b.sAcctNmbr" +
                 " AND a.sPayPerID = " + SQLUtil.toSQL(fsPeriodID) +
                 " AND a.sEmployID = " + SQLUtil.toSQL(fsEmployID) +
                 " AND a.cIntegSys = '1'";
        ResultSet loRS = instance.executeQuery(lsSQL);
        
        //set label name
        row.put("sField11", "");
        row.put("sField12", "");
        row.put("sField13", "");
        row.put("sField14", "");
        row.put("sField15", "");

        row.put("lField16", 0.00);
        row.put("lField17", 0.00);
        row.put("lField18", 0.00);
        row.put("lField19", 0.00);
        row.put("lField20", 0.00);
        
        try {
            int ctr=0;
            double other=0;
            double last=0;
            while(loRS.next()){
                if(ctr <= 4){
                    row.put("sField" + String.format("%02d", ctr+11), loRS.getString("sLoanName").toUpperCase());
                    row.put("lField" + String.format("%02d", ctr+16), loRS.getDouble("nAmountxx"));
                    last = loRS.getDouble("nAmountxx");
                }
                else{
                    other += loRS.getDouble("nAmountxx");
                }
                ctr++;
            }
            
            if(other > 0){
                row.put("sField15", "MISC LOANS");
                row.put("lField20", other + last);
            }
            
            return true;
            
        } catch (SQLException ex) {
            ex.printStackTrace();
            return false;
        }
    }

    private static boolean ExtractAdjustments(String fsPeriodID, String fsEmployID, JSONObject row, ResultSet source){
        double lnAdjBasic;
        double lnAdjBenft;
        double lnOtherAdj;
        double lnMiscxAdj;

        row.put("sField16", "");
        row.put("sField17", "");
        row.put("sField18", "");
        row.put("sField19", "");
        row.put("sField20", "");
        
        row.put("lField21", 0.00);
        row.put("lField22", 0.00);
        row.put("lField23", 0.00);
        row.put("lField24", 0.00);
        row.put("lField25", 0.00);
        
        
        try {
            //get daily benefits/pay of employee from previous payroll period
            //these will be use in the computing of possible adjustments of employee
            lnAdjBasic = source.getDouble("nAdjBasic");
            lnAdjBenft = source.getDouble("nAdjBenft");

            //Check if employee has salary adjustments
            lnMiscxAdj = ExtractOtherAdj(fsPeriodID, fsEmployID, "SalA");
            
            int ctr=0;
            
            //check if there are possible SALARY/HOLIDAY ADJUSTMENTS
            if(lnAdjBenft > 0 || !(lnMiscxAdj == 0 && source.getDouble("nAdjPremx") == 0 && source.getDouble("nTmePremx") == 0)){
                row.put("sField" + String.format("%02d", ctr+16), "SLRY/HLDY");
                row.put("lField" + String.format("%02d", ctr+21), source.getDouble("nAdjPremx") + source.getDouble("nTmePremx") + source.getDouble("nAdjHldyx") + lnMiscxAdj);
                ctr++;
            }

            //Check if employee has benefit adjustments
            lnMiscxAdj = ExtractOtherAdj(fsPeriodID, fsEmployID, "BenA");
            if(lnAdjBenft > 0 || lnMiscxAdj != 0){
                row.put("sField" + String.format("%02d", ctr+16), "BENEFITS");
                //row.put("lField" + String.format("%02d", ctr+21), lnAdjBenft + lnMiscxAdj);
                row.put("lField" + String.format("%02d", ctr+21), lnMiscxAdj);
                ctr++;
            }

            //Check if employee has overtime adjustments
            if(source.getDouble("nAdjOverT") != 0){
                row.put("sField" + String.format("%02d", ctr+16), "OVERTIME");
                row.put("lField" + String.format("%02d", ctr+21), source.getDouble("nAdjOverT"));
                ctr++;
            }

            //check if employee has attendance adjustments
            lnMiscxAdj = ExtractOtherAdj(fsPeriodID, fsEmployID, "Atnd");
            if(source.getDouble("nAdjAttnd") - source.getDouble("nAdjAbsnt") != 0 || lnMiscxAdj != 0){
                row.put("sField" + String.format("%02d", ctr+16), "ATTENDANCE");
                row.put("lField" + String.format("%02d", ctr+21), lnMiscxAdj + (lnAdjBasic * (source.getDouble("nAdjAttnd") - source.getDouble("nAdjAbsnt"))));
                ctr++;
            }

            lnOtherAdj = 0;
            
            //check if employee has tardiness adjustments
            if(source.getDouble("nAdjTardi") != 0){
                row.put("sField" + String.format("%02d", ctr+16), "ATTENDANCE");
                row.put("lField" + String.format("%02d", ctr+21), source.getDouble("nAdjTardi"));
                if(ctr >= 4){
                    lnOtherAdj += source.getDouble("nAdjTardi");
                }
                ctr++;
            }

            //check if employee has undertime adjustments
            if(source.getDouble("nAdjUndrT") != 0){
                int xctr = ctr <= 4 ? ctr : 4;
                row.put("sField" + String.format("%02d", xctr+16), "UNDERTIME");
                row.put("lField" + String.format("%02d", xctr+21), source.getDouble("nAdjUndrT"));
                if(ctr >= 4){
                    lnOtherAdj += source.getDouble("nAdjUndrT");
                }
                ctr++;
            }
            
            //check if employee has NIGHT DIFFERENTIAL
            lnMiscxAdj = source.getDouble("nNightDif") + 
                         source.getDouble("nAdjNghtD") + 
                         source.getDouble("nOTNightD") + 
                         source.getDouble("nAdjOTNht");
            if(lnMiscxAdj != 0){
                int xctr = ctr <= 4 ? ctr : 4;
                row.put("sField" + String.format("%02d", xctr+16), "NIGHT DIFF");
                row.put("lField" + String.format("%02d", xctr+21), lnMiscxAdj);
                if(ctr >= 4){
                    lnOtherAdj += lnMiscxAdj;
                }
                ctr++;
            }
            
            //check for a manual adjustment
            lnMiscxAdj = ExtractOtherAdj(fsPeriodID, fsEmployID, "PyAd");
            if(lnMiscxAdj != 0){
                int xctr = ctr <= 4 ? ctr : 4;
                row.put("sField" + String.format("%02d", xctr+16), "OTHERS");
                row.put("lField" + String.format("%02d", xctr+21), lnMiscxAdj);
                if(ctr >= 4){
                    lnOtherAdj += lnMiscxAdj;
                }
                ctr++;
            }

            //if number of adjustments is more than 5, then all adjustments from 5 up 
            //is transferred to MISC ADJ
            if(ctr > 4){
                ctr = 4;
                row.put("sField" + String.format("%02d", ctr+16), "MISC ADJ");
                row.put("lField" + String.format("%02d", ctr+21), lnOtherAdj);
            }
            
            return true;
        } catch (SQLException ex) {
            ex.printStackTrace();
            return false;
        }
    }
    
    private static double ExtractOtherAdj(String fsPeriodID, String fsEmployID, String fsSourceCD){
        String lsSQL;
        lsSQL = "SELECT SUM(nAmountxx) nAmountxx" +
               " FROM Payroll_Adjustments" +
               " WHERE sPayPerID = " + SQLUtil.toSQL(fsPeriodID) +
                 " AND sEmployID = " + SQLUtil.toSQL(fsEmployID) +
                 " AND sSourceCD = " + SQLUtil.toSQL(fsSourceCD);
        ResultSet loRS = instance.executeQuery(lsSQL);
        
        try {
            double sum=0;
            if(loRS.next()){
                sum = loRS.getDouble("nAmountxx");
            }
            return sum;
        } catch (SQLException ex) {
            ex.printStackTrace();
            return 0;
        }
    }
    
    private static String getMainQuery(Date period, String xfilter, boolean withmail){
        String lsSQL;
        lsSQL = "SELECT" +
                    "  a.sPayPerID" +
                    ", b.sEmployID" +
                    ", a.sEmpTypID" +
                    ", a.dPeriodFr" +
                    ", a.dPeriodTo" +
                    ", a.dCovergFm" +
                    ", a.dCovergTo" +
                    ", CONCAT(g.sLastName, ', ', g.sFrstName, IF(TRIM(IFNULL(g.sSuffixNm, '')) = '', ' ', CONCAT(' ', g.sSuffixNm, ' ')), g.sMiddName) sEmployNm" +
                    ", d.sBranchNm" +
                    ", f.sDeptName" +
                    ", e.sPositnNm" +
                    ", (IFNULL(h.nNoDaysxx, 0) - IFNULL(h.nCredtUse, 0)) nLveCredt" +
                    ", CAST(REVERSE(DECODE(UNHEX(b.nBasicSal), 'PETMGR')) AS CHAR) nBasicSal" +
                    ", CAST(REVERSE(DECODE(UNHEX(b.nOverTime), 'PETMGR')) AS CHAR) nOverTime" +
                    ", CAST(REVERSE(DECODE(UNHEX(b.nHolidayx), 'PETMGR')) AS CHAR) nHolidayx" +
                    ", CAST(REVERSE(DECODE(UNHEX(b.nAdjHldyx), 'PETMGR')) AS CHAR) nAdjHldyx" +
                    ", b.nAbsencex" +
                    ", CAST(REVERSE(DECODE(UNHEX(b.nTardines), 'PETMGR')) AS CHAR) nTardines" +
                    ", CAST(REVERSE(DECODE(UNHEX(b.nUndrTime), 'PETMGR')) AS CHAR) nUndrTime" +
                    ", CAST(REVERSE(DECODE(UNHEX(b.nNightDif), 'PETMGR')) AS CHAR) nNightDif" +
                    ", CAST(REVERSE(DECODE(UNHEX(b.nAdjNghtD), 'PETMGR')) AS CHAR) nAdjNghtD" +
                    ", CAST(REVERSE(DECODE(UNHEX(b.nOTNightD), 'PETMGR')) AS CHAR) nOTNightD" +
                    ", CAST(REVERSE(DECODE(UNHEX(b.nAdjOTNht), 'PETMGR')) AS CHAR) nAdjOTNht" +
                    ", CAST(REVERSE(DECODE(UNHEX(b.nAdjBasic), 'PETMGR')) AS CHAR) nAdjBasic" +
                    ", CAST(REVERSE(DECODE(UNHEX(b.nAdjBenft), 'PETMGR')) AS CHAR) nAdjBenft" +
                    ", b.nAdjAttnd" +
                    ", b.nAdjAbsnt" +
                    ", CAST(REVERSE(DECODE(UNHEX(b.nAdjOverT), 'PETMGR')) AS CHAR) nAdjOverT" +
                    ", CAST(REVERSE(DECODE(UNHEX(b.nAdjTardi), 'PETMGR')) AS CHAR) nAdjTardi" +
                    ", CAST(REVERSE(DECODE(UNHEX(b.nAdjUndrT), 'PETMGR')) AS CHAR) nAdjUndrT" +
                    ", CAST(REVERSE(DECODE(UNHEX(b.nAdjPremx), 'PETMGR')) AS CHAR) nAdjPremx" +
                    ", CAST(REVERSE(DECODE(UNHEX(b.nTmePremx), 'PETMGR')) AS CHAR) nTmePremx" +
                    ", b.nAttendnc" +
                    ", b.nAbsencex" +
                    ", a.cPostedxx" +
                    ", c.sPassword" +
                    ", i.cDivision" +
                " FROM Payroll_Period a" +
                     " LEFT JOIN Payroll_Summary_New b ON a.sPayPerID = b.sPayPerID" +
                     " LEFT JOIN Employee_Master001 c ON b.sEmployID = c.sEmployID" +
                     " LEFT JOIN Branch d ON b.sBranchCD = d.sBranchCD" +
                     " LEFT JOIN Position e ON c.sPositnID = e.sPositnID" +
                     " LEFT JOIN Department f ON c.sDeptIDxx = f.sDeptIDxx" +
                     " LEFT JOIN Client_Master g ON c.sEmployID = g.sClientID" +
                     " LEFT JOIN Employee_Leave_Credit h ON c.sEmployID = h.sEmployID AND h.cLeaveTyp = '0' AND a.dPeriodFr BETWEEN h.dValdFrom AND h.dValdThru" +
                     " LEFT JOIN Branch_Others i on d.sBranchCD = i.sBranchCD" +
                " WHERE a.sEmpTypID IN ('R', 'T')" +
                  " AND a.dPeriodFr = " + SQLUtil.toSQL(period) +
                     (!withmail ? "" : " AND IFNULL(g.sEmailAdd, '') <> ''") + 
                     (xfilter.isEmpty() ? "" : " AND " + xfilter)  +
                " ORDER BY d.sBranchNm, f.sDeptName, g.sLastName, g.sFrstName, g.sSuffixNm, g.sMiddName";
        
        return lsSQL;
    }
}
