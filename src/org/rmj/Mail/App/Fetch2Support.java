/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.rmj.mail.App;

import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import javax.mail.Folder;
import javax.mail.Message;
import javax.mail.MessagingException;
import org.apache.poi.EncryptedDocumentException;
import org.apache.poi.openxml4j.exceptions.InvalidFormatException;

import org.rmj.appdriver.MiscUtil;
import org.rmj.appdriver.SQLUtil;
import org.rmj.lib.mail.MessageInfo;
import org.rmj.lib.mail.ReadMail;
import org.rmj.appdriver.agent.GRiderX;

import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;

//import jxl.*; 
//import jxl.read.biff.BiffException;
import org.rmj.appdriver.GProperty;
import org.rmj.lib.net.LogWrapper;
import org.rmj.appdriver.agent.MsgBox;

/**
 *
 * @author kalyptus
 */
public class Fetch2Support {
   //kalyptus - 2015.11.04 03:14pm
   //get the path of this hava file once it was executed...
   private static final String jar_path = new File(Fetch2Support.class.getProtectionDomain().getCodeSource().getLocation().getPath()).getParentFile().getPath().replace("%20", " "); 
   private static final LogWrapper logwrapr = new LogWrapper("RMJMail.Fetch2Support", jar_path + "/temp/Fetch2Support.log");

   private static ReadMail pomail;
   private static GRiderX instance = null;
   private static String dTransact = null;
   private static String sBranchCD = null;
   private static String sModulexx = null;
   private static String sReferNox = null;
   private static String sOldValue = null;
   private static String sNewValue = null;
   private static String sOthrInfo = null;
   private static GProperty loProp = null;
      
   public static void main(String[] args) {
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

       String sender;
      
      if(args.length == 0)
         sender = "AutoReader-Win7";
      else
         sender = args[0];
      
      loProp = new GProperty(jar_path + "/config/" + sender);
        
      //Load the driver to use in saving the email to the database
      instance = new GRiderX("gRider");
      instance.setOnline(true);
      
      if(instance.getConnection() == null){
         System.out.println("Unable to connect to the database server...");
         return;
      }
         
      //load the configuration
      System.out.println("Loading configuration file...");
      pomail = new ReadMail(jar_path, sender);
      
      //connect to the server
      System.out.println("Connecting to the email server...");
      if (pomail.connect(true)){
         try {
            //open folder to fetch
            pomail.openFolder("INBOX", Folder.READ_WRITE);

            //Fetch Unread Messages
            pomail.fetchMesage(false);
            
            //read each fetch mail
            for(int lnctr = 0; lnctr <= pomail.getFetchMsgCount() - 1 ; lnctr++){
               Message message = pomail.getFetchMessage(lnctr);
               
               sBranchCD = "";
               sModulexx = "";
               sReferNox = "";
               sOldValue = "";
               sNewValue = "";
               sOthrInfo = "";
         
               //Create transaction no for this fetch mail
               Connection loCon = instance.getConnection();
               
               String lsNo = MiscUtil.getNextCode("Support_Request_Master", "sTransNox", true, loCon, instance.getBranchCode());

               //transfer mail to MessageInfo object
               MessageInfo msginfo = pomail.getMessageInfo(message, lsNo);
               System.out.println("xSUBJECT: " + msginfo.getSubject());
               System.out.println("xDATE   : " + msginfo.getDate());
               System.out.println("xFROM   : " + msginfo.getFrom());

               //extract [to]
               StringBuilder to = new StringBuilder();
               if(msginfo.Size_To() > 0){
                  for(int n=0;n<=msginfo.Size_To()-1;n++){
                     to.append(";").append(msginfo.getTo(n));
                  }
                  System.out.println("xTO     : " + to.substring(1));               
               }

               //extract [cc]
               StringBuilder cc = new StringBuilder();
               if(msginfo.Size_CC() > 0){
                  for(int n=0;n<=msginfo.Size_CC()-1;n++){
                     cc.append(";").append(msginfo.getCC(n));
                  }
                  System.out.println("xCC     : " + cc.substring(1));               
               }

               //extract [bcc]
               StringBuilder bcc = new StringBuilder();
               if(msginfo.Size_BCC() > 0){
                  for(int n=0;n<=msginfo.Size_BCC()-1;n++){
                     bcc.append(";").append(msginfo.getBCC(n));
                  }
                  System.out.println("xBCC    : " + bcc.substring(1));               
               }

               System.out.println("xCONTENT: " + msginfo.getBody());

               //download and get filename of attachment
               StringBuilder attach = new StringBuilder();
               if(msginfo.Size_Attachment()> 0){
                  for(int n=0;n<=msginfo.Size_Attachment()-1;n++){
                     attach.append(";").append(msginfo.getAttachment(n));
                     
                     //read possible SSRF.XLS attachement send by the branch...
                     if (msginfo.getAttachment(n).toLowerCase().contains(".xls") 
                     && msginfo.getAttachment(n).toLowerCase().contains("ssrf")){
                        readEXL(lsNo, msginfo.getAttachment(n));
                     }
                  }
                  System.out.println("xATTACHED: " + attach.substring(1));
               }
               
               //TODO: detemine the associate assigned for this support request
               
               System.out.println(SQLUtil.toSQL(lsNo));
               System.out.println(SQLUtil.toSQL(msginfo.getDate()));
               System.out.println(SQLUtil.toSQL(to.toString()));
               System.out.println(SQLUtil.toSQL(cc.toString()));
               System.out.println(SQLUtil.toSQL(bcc.toString()));
               System.out.println(SQLUtil.toSQL(msginfo.getBody()));
               System.out.println(SQLUtil.toSQL(msginfo.getSubject()));
               System.out.println(SQLUtil.toSQL(attach.toString()));
               System.out.println("Branch is: " + instance.getBranchCode());
//               MsgBox.showOk("Isnull:" + (sModulexx == null ? "true" : "false"));
//               MsgBox.showOk(SQLUtil.toSQL((sModulexx == null ? "" : sModulexx)));

               //Initiate ACID
               instance.beginTrans();
               
               //write gathered info to the Support_Request_Master table
               String lsSQL = "INSERT INTO Support_Request_Master" + 
                             " SET sTransNox = " + SQLUtil.toSQL(lsNo) + 
                                ", sBranchCD = " + SQLUtil.toSQL((sBranchCD == null ? "" : sBranchCD.length() > 4 ? sBranchCD.substring(0, 3) : sBranchCD)) +  
                                ", dTransact = " + SQLUtil.toSQL(msginfo.getDate()) + 
                                ", sMailFrom = " + SQLUtil.toSQL(msginfo.getFrom()) +  
                                ", sMailToxx = " + SQLUtil.toSQL(to.toString().substring(1)) +  
                                ", sMailCCxx = " + SQLUtil.toSQL(cc.toString().length() > 0 ? cc.toString().substring(1) : cc.toString()) +  
                                ", sMailBCCx = " + SQLUtil.toSQL(bcc.toString().length() > 0 ? bcc.toString().substring(1) : bcc.toString()) +  
                                ", sSubjectx = " + SQLUtil.toSQL(msginfo.getSubject()) +  
                                ", sMailBody = " + SQLUtil.toSQL(msginfo.getBody().length() > 2048 ? msginfo.getBody().substring(0, 2047) :  msginfo.getBody()) +  
                                ", sAttached = " + SQLUtil.toSQL(attach.toString().length() > 0 ? attach.toString().substring(1) : attach.toString()) +  
                                ", sModuleID = " + SQLUtil.toSQL((sModulexx == null ? "" : sModulexx)) +  
                                ", sOldValue = " + SQLUtil.toSQL((sOldValue == null ? "" : sOldValue)) +  
                                ", sNewValue = " + SQLUtil.toSQL((sNewValue == null ? "" : sNewValue)) +  
                                ", sReferNox = " + SQLUtil.toSQL((sReferNox == null ? "" : sReferNox)) +  
                                ", sOthrInfo = " + SQLUtil.toSQL((sOthrInfo == null ? "" : sOthrInfo)) +  
                                ", dAutoPick = " + SQLUtil.toSQL(instance.getServerDate()) +  
                                ", sTrackrID = " + SQLUtil.toSQL("") +  
                                ", cStatusxx = " + SQLUtil.toSQL("0") + 
                                ", dModified = " + SQLUtil.toSQL(instance.getServerDate());  
               instance.executeQuery(lsSQL, "Support_Request_Master", "", "");

//               MsgBox.showOk("We will delete the message after this");

              System.out.println("We will delete the message after this");

            //commit update
            instance.commitTrans();
              
//
//            pomail.deleteMessage(message);
//            pomail.closeFolder();
//            pomail.disconnect();
            }
            
         } catch (MessagingException ex) {
            logwrapr.severe("main: MessagingException error detected.", ex);
         } catch (IOException ex) {
            logwrapr.severe("main: IOException error detected.", ex);
         } 
         
      } //if (pomail.connect(true))
   }   
   
   private static void readEXL(String sTransNox, String sFileName){
      try {

         //dTransact = "";
         sBranchCD = "";
         sModulexx = "";
         sReferNox = "";
         sOldValue = "";
         sNewValue = "";
         sOthrInfo = "";
         
         Workbook wb = WorkbookFactory.create(new File(loProp.getConfig("mail.sftp.fldr") + "download/" + sTransNox + "/" + sFileName)); // Or .xlsx
         //Sheet s = wb.getSheet("Sheet1");
         Sheet s = wb.getSheetAt(0);
         
         if(s == null) 
            return;

         //Access B7
         //dTransact = s.getRow(5).getCell(2).getStringCellValue();
         sBranchCD = s.getRow(3).getCell(2).getStringCellValue();
         sModulexx = s.getRow(7).getCell(1).getStringCellValue();
         sReferNox = s.getRow(7).getCell(2).getStringCellValue();
         sOldValue = s.getRow(7).getCell(4).getStringCellValue();
         sNewValue = s.getRow(7).getCell(5).getStringCellValue();
         sOthrInfo = s.getRow(7).getCell(6).getStringCellValue();

//         MsgBox.showOk(sModulexx.concat(""));
      }  catch (NullPointerException ex) {
         logwrapr.severe("main: NullPointerException error detected.", ex);
      }  catch (IOException ex) {
         logwrapr.severe("main: IOException error detected.", ex);
      } catch (EncryptedDocumentException ex) {
         logwrapr.severe("main: EncryptedDocumentException error detected.", ex);
      }  catch (Exception ex) {
         logwrapr.severe("main: Exception error detected.", ex);
      }
   }
   
//   private static void readEXL(String sTransNox, String sFileName){
//      try {
//
//         dTransact = "";
//         sBranchCD = "";
//         sModulexx = "";
//         sReferNox = "";
//         sOldValue = "";
//         sNewValue = "";
//         sOthrInfo = "";
//         
//         Workbook workbook = Workbook.getWorkbook(new File(loProp.getConfig("mail.sftp.fldr") + "download/" + sTransNox + "/" + sFileName));
//         Sheet sheet = workbook.getSheet(0);
//
//         dTransact = sheet.getCell(2,5).getContents();
//         sBranchCD = sheet.getCell(2,3).getContents();
//         sModulexx = sheet.getCell(1,7).getContents();
//         sReferNox = sheet.getCell(2,7).getContents();
//         sOldValue = sheet.getCell(4,7).getContents();
//         sNewValue = sheet.getCell(5,7).getContents();
//         sOthrInfo = sheet.getCell(6,7).getContents();
//         
//      } catch (IOException ex) {
//         logwrapr.severe("main: IOException error detected.", ex);
//      } catch (BiffException ex) {
//         logwrapr.severe("main: BiffException error detected.", ex);
//      }
//   }   
}
