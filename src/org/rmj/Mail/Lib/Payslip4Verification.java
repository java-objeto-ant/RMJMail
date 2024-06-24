/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.rmj.mail.lib;

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
import javax.mail.MessagingException;
import net.sf.jasperreports.engine.JRException;
import net.sf.jasperreports.engine.JasperExportManager;
import net.sf.jasperreports.engine.JasperFillManager;
import net.sf.jasperreports.engine.JasperPrint;
import net.sf.jasperreports.engine.data.JsonDataSource;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.rmj.mail.App.CreatePayslip;
import org.rmj.mail.App.PDFPassword;
import org.rmj.appdriver.GRider;
import org.rmj.appdriver.MiscUtil;
import org.rmj.appdriver.SQLUtil;
import org.rmj.lib.mail.MessageInfo;
import org.rmj.lib.mail.SendMail;

/**
 *
 * @author sayso
 */
public class Payslip4Verification {
    private GRider poRider;
    private boolean pbWithReglr = true;
    private boolean pbWithProbi = true;
    private int pnCount = 10;
    private String psCluster;
    private Date pdPerioFrom;
    private String psWarning;
    private int pnWarnCode;
    
    public Payslip4Verification(GRider rider, String cluster, Date payfrom){
        this.poRider = rider;
        this.pdPerioFrom = payfrom;
        this.psCluster = cluster;
        System.out.println(payfrom);
    }

    public boolean CreateVerification() throws SQLException{
        //Check if verification for a certain period of the cluster was created
        String lsSQL = "SELECT *" +
                     " FROM Payroll_Discrepancy_Checker_Master" +
                     " WHERE dPeriodFr = " + SQLUtil.toSQL(this.pdPerioFrom) +
                          " AND sClustrCD = " + SQLUtil.toSQL(this.psCluster) +
                          " AND cTranStat NOT IN ('3')";
        ResultSet loRS = this.poRider.executeQuery(lsSQL);
        System.out.println(lsSQL);
        
        if(loRS.next()){
            System.out.println("Already created!");
            return false;
        }

        String lsPeriod = "";

        poRider.beginTrans();

        String lsTransNox = MiscUtil.getNextCode("Payroll_Discrepancy_Checker_Master", "sTransNox", true, poRider.getConnection(), poRider.getBranchCode());
        lsSQL = "INSERT INTO Payroll_Discrepancy_Checker_Master" + 
               " SET sTransNox = " + SQLUtil.toSQL(lsTransNox) + 
                  ", dCreatedx = " + SQLUtil.toSQL(Calendar.getInstance()) + 
                  ", dPeriodFr = " + SQLUtil.toSQL(this.pdPerioFrom) + 
                  ", sClustrCD = " + SQLUtil.toSQL(this.psCluster) + 
                  ", cMailSent = '0'" + 
                  ", cTranStat = '0'";
        System.out.println(lsSQL);
        poRider.executeQuery(lsSQL, "Payroll_Discrepancy_Checker_Master", "", "");

        lsSQL = "SELECT a.sPayPerID, a.sEmployID, a.sBranchCd" + 
               " FROM Payroll_Summary_New a" +
                    " LEFT JOIN Branch_Others b ON a.sBranchCd = b.sBranchCd" +
                    " LEFT JOIN Payroll_Period c ON a.sPayPerID = c.sPayPerID" +
               " WHERE c.dPeriodFr = " + SQLUtil.toSQL(this.pdPerioFrom) +
                 " AND c.cPostedxx = '2'" +
                 " AND b.cPayDivCd = " + SQLUtil.toSQL(this.psCluster);

        System.out.println(lsSQL);
        loRS = this.poRider.executeQuery(lsSQL);

        int count = countRows(loRS);

        int[] random = createRandom(this.pnCount, 1, count);

        for(int x=0;x<this.pnCount;x++){
            loRS.absolute(random[x]);
            lsSQL = "INSERT INTO Payroll_Discrepancy_Checker_Detail" + 
                   " SET sTransNox = " + SQLUtil.toSQL(lsTransNox) + 
                      ", nEntryNox = " + SQLUtil.toSQL(x+1) + 
                      ", sPayPerID = " + SQLUtil.toSQL(loRS.getString("sPayPerID")) + 
                      ", sEmployID = " + SQLUtil.toSQL(loRS.getString("sEmployID")) +
                      ", sBranchCD = " + SQLUtil.toSQL(loRS.getString("sBranchCD")) +
                      ", sCheckrID = ''" + 
                      ", cChckStat = '0'" + 
                      ", sRemarksx = ''" + 
                      ", sPostedBy = ''";
            poRider.executeQuery(lsSQL, "Payroll_Discrepancy_Checker_Detail", "", "");
        }    

        poRider.commitTrans();

        return true;
    }
    
    public boolean CreateReport(){
        String lsSQL = "SELECT a.sTransNox, a.sClustrCd, a.dPeriodFr, b.sDivsnDsc, c.sLastName, c.sFrstName, c.sMiddName, c.sSuffixNm, c.cGenderCD, c.sEmailAdd, d.sPassword" +
                      " FROM Payroll_Discrepancy_Checker_Master a" +
                            " LEFT JOIN PetMgr_Division b ON a.sClustrCD = b.sDivsnCde" +
                            " LEFT JOIN Client_Master c ON b.sCheckrID = c.sClientID" + 
                            " LEFT JOIN Employee_Master001 d ON b.sCheckrID = d.sEmployID" +
                            " WHERE cMailSent = '0'";
        ResultSet loRSMaster = this.poRider.executeQuery(lsSQL);
        System.out.println(lsSQL);
        
        try {
            while(loRSMaster.next()){
                this.poRider.beginTrans();
                String xFilter = "SELECT sEmployID" + 
                                " FROM Payroll_Discrepancy_Checker_Detail" + 
                                " WHERE sTransNox = " + SQLUtil.toSQL(loRSMaster.getString("sTransNox")) ;
                JsonDataSourcePayslip oPay2Ver = new JsonDataSourcePayslip(poRider, pdPerioFrom, "(b.sEmployID IN (" + xFilter + "))");
                JSONArray json_array = oPay2Ver.CreateJson();
                
                if(json_array != null){
                    String path = System.getProperty("sys.default.path.config");

                    JsonDataSource jrjson;
                    JasperPrint print;
                    InputStream stream = null;
                    
                    //prepare stream input for the creation of JASPER REPORT
                    stream = new ByteArrayInputStream(json_array.toJSONString().getBytes("UTF-8"));
                    jrjson = new JsonDataSource(stream);
                    //System.out.println(xrows.toJSONString());
                    //create jasper report
                    print = JasperFillManager.fillReport(path + "/reports/Payslip.jasper", null, jrjson);
                    String filename = loRSMaster.getString("sClustrCd") + SQLUtil.dateFormat(loRSMaster.getDate("dPeriodFr"), "yyyyMMdd");

                    //System.out.println(System.getProperty("sys.default.path.temp") + "/payslip/" + filename + "X.pdf");
                    //export jasper report to PDF
                    JasperExportManager.exportReportToPdfFile(print, System.getProperty("sys.default.path.temp") + "/payslip/" + filename + "X.pdf");
                    //set password in our jasper report
                    //02645850
                    String user = loRSMaster.getString("sPassword").equalsIgnoreCase("****") || loRSMaster.getString("sPassword").isEmpty() ? "1234" : loRSMaster.getString("sPassword");
                    PDFPassword.encrypt_pdf(System.getProperty("sys.default.path.temp") + "/payslip/" + filename + "X.pdf", System.getProperty("sys.default.path.temp") + "/payslip/" + filename + ".pdf", user.getBytes(), "02645850".getBytes());
                    //Delete file
                    File file = new File(System.getProperty("sys.default.path.temp") + "/payslip/" + filename + "X.pdf");
                    file.delete();

                    Calendar cal = Calendar.getInstance();
                    lsSQL = "UPDATE Payroll_Discrepancy_Checker_Master" +
                           " SET cMailSent = '1'" +
                              ", dMailSent = " + SQLUtil.toSQL(cal) +
                           " WHERE sTransNox = " + SQLUtil.toSQL(loRSMaster.getString("sTransNox"));
                    this.poRider.executeUpdate(lsSQL);
                }
                this.poRider.commitTrans();
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
            this.poRider.rollbackTrans();
            return false;
        } catch (UnsupportedEncodingException ex) {
            ex.printStackTrace();
            this.poRider.rollbackTrans();
            return false;
        } catch (JRException ex) {
            ex.printStackTrace();
            this.poRider.rollbackTrans();
            return false;
        } catch (IOException ex) {
            ex.printStackTrace();
            this.poRider.rollbackTrans();
            return false;
        } catch (DocumentException ex) {
            ex.printStackTrace();
            this.poRider.rollbackTrans();
            return false;
        }
        
        return true;
    }
    
    public boolean SendReport(){
        String lsSQL = "SELECT a.sTransNox, a.sClustrCd, a.dPeriodFr, b.sDivsnDsc, c.sLastName, c.sFrstName, c.sMiddName, c.sSuffixNm, c.cGenderCD, c.sEmailAdd, d.sPassword, c.sCompnyNm" +
                      " FROM Payroll_Discrepancy_Checker_Master a" +
                            " LEFT JOIN PetMgr_Division b ON a.sClustrCD = b.sDivsnCde" +
                            " LEFT JOIN Client_Master c ON b.sCheckrID = c.sClientID" + 
                            " LEFT JOIN Employee_Master001 d ON b.sCheckrID = d.sEmployID" +
                            " WHERE cMailSent = '1'";
        ResultSet loRSMaster = this.poRider.executeQuery(lsSQL);
        System.out.println(lsSQL);
        
        String sender = "GMail";
        String path = System.getProperty("sys.default.path.config");
        System.out.println(path);
        //load the configuration
        SendMail pomail = new SendMail(path, sender);

        try {
            if(pomail.connect(true)){
                while(loRSMaster.next()){
                    this.poRider.beginTrans();

                    MessageInfo msginfo = new MessageInfo();
                    String filename = loRSMaster.getString("sClustrCd") + SQLUtil.dateFormat(loRSMaster.getDate("dPeriodFr"), "yyyyMMdd");

                    //msginfo.addTo(java.net.IDN.toUnicode(new String(encode(email))));
                    msginfo.addTo(loRSMaster.getString("sEmailAdd"));
                    msginfo.setSubject("Payroll verification for " + loRSMaster.getString("dPeriodFr"));
                    msginfo.setBody(loRSMaster.getString("sDivsnDsc"));
                    msginfo.addAttachment(System.getProperty("sys.default.path.temp") + "/payslip/" + filename + ".pdf");
                    //kalyptus - 2017.05.19 01:44pm
                    //set the no reply account as the sender instead of the petmgr
                    msginfo.setFrom("No Reply <no-reply@guanzongroup.com.ph>");
                    pomail.sendMessage(msginfo);
                    
                    Calendar cal = Calendar.getInstance();
                    lsSQL = "UPDATE Payroll_Discrepancy_Checker_Master" +
                           " SET cMailSent = '2'" +
                              ", dMailSent = " + SQLUtil.toSQL(cal) +
                           " WHERE sTransNox = " + SQLUtil.toSQL(loRSMaster.getString("sTransNox"));
                    this.poRider.executeUpdate(lsSQL);
                    this.poRider.commitTrans();
                }
            }

        } catch (SQLException ex) {
            ex.printStackTrace();
            this.poRider.rollbackTrans();
            return false;
        } catch (MessagingException ex) {
            Logger.getLogger(Payslip4Verification.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IOException ex) {
            Logger.getLogger(Payslip4Verification.class.getName()).log(Level.SEVERE, null, ex);
        }

        return true;
    }
    
    public void includeProbi(boolean include){
        this.pbWithProbi = include;
    }
    public boolean includeProbi(){
        return this.pbWithProbi;
    }
    
    public void includeRegular(boolean include){
        this.pbWithReglr = include;
    }
    public boolean includeRegular(){
        return this.pbWithReglr;
    }

    public void verifyCount(int count){
        if(count > 0){
            this.pnCount = count;
        }
    }
    public int verifyCount(){
        return this.pnCount;
    }
    
    private int countRows(ResultSet rs){
        int size = 0;
        try {
            rs.last();
            size = rs.getRow();
            rs.beforeFirst();
        }
        catch(SQLException ex) {
            ex.printStackTrace();
            return 0;
        }
        return size;        
    }
    
    private int[] createRandom(int size, int min, int max){
        int[] lnRand = new int[size];
        
        int x=0;
        while(x<size){
            int value = randomInRange(min, max);
            System.out.printf("Ctr = %d: Value = %d\n", x, value);
            if(!contains(lnRand, value)){
                lnRand[x] = value;
                x++;
            }
        }
        
        return lnRand;
    }
    
    private int randomInRange(int min, int max) {
        if (min >= max) {
            throw new IllegalArgumentException("max must be greater than min");
        }

        return (int)(Math.random() * ((max - min) + 1)) + min;
    }
    
    private boolean contains(final int[] array, final int v) {
        boolean result = false;
        for(int i : array){
            if(i == v){
                result = true;
                break;
            }
        }
        return result;
    }    
}
