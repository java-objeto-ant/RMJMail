/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.rmj.mail.App;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Calendar;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.mail.MessagingException;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.rmj.appdriver.GProperty;
import org.rmj.appdriver.MiscUtil;
import org.rmj.appdriver.SQLUtil;
import org.rmj.lib.mail.MessageInfo;
import org.rmj.lib.mail.SendMail;
import org.rmj.lib.net.LogWrapper;
import org.rmj.appdriver.agent.GRiderX;

/**
 * FSECStatus 
 * @author kalyptus
 *  Usage #1: java -Xmx1g -cp RMJMail.jar org.rmj.Mail.App.FSECStatus <config> <from> <thru> 
 *        Ex: java -Xmx1g -cp RMJMail.jar org.rmj.Mail.App.FSECStatus AutoReader-Win7 2017-01-01 2017-01-31    
 *
 *  Usage #2: java -Xmx1g -cp RMJMail.jar org.rmj.Mail.App.FSECStatus <config> <from> <thru> <areacode>
 *        Ex: java -Xmx1g -cp RMJMail.jar org.rmj.Mail.App.FSECStatus AutoReader-Win7 2017-01-01 2017-01-31 M001   
 */
public class FSECStatus {
   private static final String jar_path = new File(Fetch2Support.class.getProtectionDomain().getCodeSource().getLocation().getPath()).getParentFile().getPath().replace("%20", " "); 
   private static final LogWrapper logwrapr = new LogWrapper("RMJMail.FSECStatus", jar_path + "/temp/FSECStatus.log");
   private static GRiderX instance = null;
   private static GProperty loProp = null;
   private static SendMail pomail;
   
   public static void main(String[] args) {
      String sPropName;
      Calendar dFrom = Calendar.getInstance();
      Calendar dThru = Calendar.getInstance();
      // this utility should always have a 3 parameter 
      // source, client_dir, file
      
//      System.out.println("Args Length: " + Integer.toString(args.length));
//      for(int n=0;n < args.length;n++){
//         System.out.println("Args " + Integer.toString(n) + " :" + args[n]);
//      }
//      System.exit(1);

        String path;
        if(System.getProperty("os.name").toLowerCase().contains("win")){
            path = "D:/GGC_Java_Systems";
            System.setProperty("sys.default.path.temp", path + "/temp");
        }
        else{
            path = "/srv/GGC_Java_Systems";
            System.setProperty("sys.default.path.temp", path + "/temp");
        }
        System.setProperty("sys.default.path.config", path);

      
      if(args.length < 3){
         System.out.println("Invalid number of arguments...");
         System.exit(1);
      }

      sPropName = args[0]; 
      dFrom.setTime(SQLUtil.toDate(args[1], "yyyy-MM-dd"));
      dThru.setTime(SQLUtil.toDate(args[2], "yyyy-MM-dd"));
      
      //Load the driver to use in saving the email to the database
      instance = new GRiderX("gRider");
      instance.setOnline(true);

      if(instance.getConnection() == null){
         System.out.println("Unable to connect to the database server...");
         System.exit(1);
      }
      
      ResultSet loRSArea = null;
      
      if(args.length == 3)
         loRSArea= getAreaList();
      else if(args.length == 4)
         loRSArea= getAreaList(args[3]);
      
      ResultSet loRSFSEC = null;
      if(loRSArea == null){
         System.out.println("No MC Area was extracted from the database!");
         System.exit(1);
      }

      loProp = new GProperty(jar_path + "/config/" + sPropName);
      pomail = new SendMail(jar_path, sPropName);
      
      try{
         if(pomail.connect(true)){
            while(loRSArea.next()){
               loRSFSEC = getFSECStat(loRSArea.getString("sAreaCode"), args[1], args[2]);

               if(loRSFSEC != null){
                  System.out.println("creating for " + loRSArea.getString("sAreaDesc"));

                  if(createExcel(loRSArea.getString("sAreaCode"), loRSArea.getString("sAreaDesc"), dFrom, dThru, loRSFSEC)){
                     sendExcel(loRSArea.getString("sAreaCode"), loRSArea.getString("sAreaDesc"), dFrom, dThru, loRSArea.getString("sEmailAdd"));
                  } 
               } //if(loRSFSEC != null){
            } //while(loRSArea.next()){
         }//if(pomail.connect(true)){
         
      }
      catch (SQLException ex) {
         logwrapr.severe("cleanCancelledMCSales: error in SQL statement", ex);
         instance.setErrMsg(ex.getMessage());
         instance.rollbackTrans();
      }//catch (SQLException ex)         
      finally{
         MiscUtil.close(loRSArea);
         MiscUtil.close(loRSFSEC);
      }        
      return;
   }
        
   private static ResultSet getAreaList(){
      try {
         StringBuilder lsSQL = new StringBuilder();
         lsSQL.append("SELECT a.sAreaCode, a.sAreaDesc, a.sAreaMngr, b.sCompnyNm, b.sEmailAdd")
             .append(" FROM Branch_Area a") 
               .append(" LEFT JOIN Client_Master b ON a.sAreaMngr = b.sClientID") 
             .append(" WHERE a.cDivision = '1' AND IFNULL(sAreaMngr, '') <> ''"); 
         ResultSet loRS = instance.getConnection().createStatement().executeQuery(lsSQL.toString());
         return loRS;
        } catch (SQLException ex) {
            logwrapr.severe("getAreaList: SQLException error detected.", ex);
            return null;
        }
   }
   private static ResultSet getAreaList(String sAreaCode){
      try {
         StringBuilder lsSQL = new StringBuilder();
         lsSQL.append("SELECT a.sAreaCode, a.sAreaDesc, a.sAreaMngr, b.sCompnyNm, b.sEmailAdd")
             .append(" FROM Branch_Area a") 
               .append(" LEFT JOIN Client_Master b ON a.sAreaMngr = b.sClientID") 
             .append(" WHERE a.cDivision = '1' AND IFNULL(sAreaMngr, '') <> ''")
               .append(" AND a.sAreaCode = " + SQLUtil.toSQL(sAreaCode));  
         ResultSet loRS = instance.getConnection().createStatement().executeQuery(lsSQL.toString());
         return loRS;
        } catch (SQLException ex) {
            logwrapr.severe("getAreaList: SQLException error detected.", ex);
            return null;
        }
   }
   
   private static ResultSet getFSECStat(String fsAreaCode, String fsDateFrom, String fsDateThru){
      try {
         StringBuilder lsSQL = new StringBuilder();
         lsSQL.append("SELECT e.sBranchNm, a.dTransact, a.sDRNoxxxx, h.sCompnyNm, h.sMobileNo, c.sEngineNo,")
                .append(" IF(d.cTranStat = '0', 'Open',") 
                   .append(" IF(d.cTranStat = '1', 'Sent',") 
                      .append(" IF(d.cTranStat = '2', 'Confirmed',") 
                         .append(" IF(d.cTranStat = '3', 'Cancelled', 'Activated')))) cStatusxx") 
             .append(" FROM MC_SO_Master a") 
               .append(" LEFT JOIN MC_SO_Detail b ON a.sTransNox = b.sTransNox") 
               .append(" LEFT JOIN MC_Serial c ON b.sSerialID = c.sSerialID") 
               .append(" LEFT JOIN FSEC_Confirmation d ON c.sSerialID = d.sSerialID") 
               .append(" LEFT JOIN Branch e ON LEFT(a.sTransNox, 4) = e.sBranchCD") 
               .append(" LEFT JOIN Branch_Others f ON e.sBranchCD = f.sBranchCD") 
               .append(" LEFT JOIN Client_Master h ON a.sClientID = h.sClientID") 
             .append(" WHERE a.dTransact BETWEEN " + SQLUtil.toSQL(fsDateFrom) +   " AND " + SQLUtil.toSQL(fsDateThru)) 
               .append(" AND f.sAreaCode = " + SQLUtil.toSQL(fsAreaCode)) 
               .append(" AND a.cTranStat <> '3'") 
               .append(" AND b.cMotorNew = '1'")
               .append(" AND d.cTranStat IN ('0', '1')")  
             .append("ORDER BY sBranchNm, dTransact, sDRNoxxxx");    
         ResultSet loRS = instance.getConnection().createStatement().executeQuery(lsSQL.toString());
         return loRS;
        } catch (SQLException ex) {
            logwrapr.severe("getAreaList: SQLException error detected.", ex);
            return null;
        }
   }

   private static boolean createExcel(String sAreaCode, String sAreaDesc, Calendar dDateFrom, Calendar dDateThru, ResultSet loRSFSEC){
      try{
         FileInputStream excelFile = new FileInputStream(new File(jar_path + "/config/FSEPStatusReport-temp.xlsx"));
         XSSFWorkbook workbook = new XSSFWorkbook(excelFile);
         XSSFSheet sheet = workbook.getSheetAt(0);
         Row row = sheet.getRow(0);
         row.getCell(0).setCellValue("FSEC Confirmation Status - " + sAreaDesc);

         row = sheet.getRow(1);
         row.getCell(0).setCellValue("For the Period " + SQLUtil.dateFormat(dDateFrom, "MMM dd, yyyy") + " TO " + SQLUtil.dateFormat(dDateThru, "MMM dd, yyyy"));
         row = sheet.getRow(2);
         row.getCell(0).setCellValue("As of " + SQLUtil.dateFormat(instance.getServerDate(), "MMM dd, yyyy"));
         
         int lnRow=5;
         while(loRSFSEC.next()){
            row = sheet.createRow(lnRow);
            System.out.println(loRSFSEC.getString("sBranchNm"));
            row.createCell(0).setCellValue(loRSFSEC.getString("sBranchNm"));
            row.createCell(1).setCellValue(SQLUtil.dateFormat(loRSFSEC.getDate("dTransact"), "yyyy/MM/dd"));
            row.createCell(2).setCellValue(loRSFSEC.getString("sDRNoxxxx"));
            row.createCell(3).setCellValue(loRSFSEC.getString("sCompnyNm"));
            row.createCell(4).setCellValue(loRSFSEC.getString("sMobileNo"));
            row.createCell(5).setCellValue(loRSFSEC.getString("sEngineNo"));
            row.createCell(6).setCellValue(loRSFSEC.getString("cStatusxx"));
            lnRow++;
         }
         
         if(lnRow>5){
            String lsCode = SQLUtil.dateFormat(dDateFrom, "yyyyMMdd") + SQLUtil.dateFormat(dDateThru, "yyyyMMdd");
            FileOutputStream outputStream = new FileOutputStream(jar_path + "/temp/FSEPStat" + sAreaCode + lsCode + ".xlsx");
            workbook.write(outputStream);
            workbook.close();            
         }
         else
            return false;
         
      } catch (IOException ex) {
         logwrapr.severe("createExcel: IOException encountered... " , ex);
      } catch (SQLException ex) {
         logwrapr.severe("createExcel: Error creating excel for " + sAreaDesc , ex);
         return false;
      }

      return true;
   }
   
   private static void sendExcel(String sAreaCode, String sAreaDesc, Calendar dDateFrom, Calendar dDateThru, String sEmailAdd){
      try {
         MessageInfo msginfo = new MessageInfo();
         msginfo.setFrom("no-reply@guanzongroup.com.ph");
         msginfo.addTo(sEmailAdd);
         //msginfo.addTo("masayson@guanzongroup.com.ph");
         msginfo.setSubject("List of Unconfirmed FSEP " +
                            "For the Period " + SQLUtil.dateFormat(dDateFrom, "MMM dd, yyyy") + " TO " + SQLUtil.dateFormat(dDateThru, "MMM dd, yyyy"));
         String lsMessage = "\nREMINDER: HINDI ipinahihintulot na ang BRANCH mismo ang mag-send ng SMS Confirmation para i-activate ang FSEP.\n\nPlease see attached file...\n";
         msginfo.setBody(lsMessage);
         String lsCode = SQLUtil.dateFormat(dDateFrom, "yyyyMMdd") + SQLUtil.dateFormat(dDateThru, "yyyyMMdd");
         msginfo.addAttachment(jar_path + "/temp/FSEPStat" + sAreaCode + lsCode + ".xlsx");
         pomail.sendMessage(msginfo);
      } catch (MessagingException ex) {
          logwrapr.severe("MessagingException: IOException encountered... " , ex);
      } catch (IOException ex) {
          logwrapr.severe("sendExcel: IOException encountered... " , ex);
      }
   }
}
