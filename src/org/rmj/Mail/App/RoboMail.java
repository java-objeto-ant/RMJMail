/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.rmj.mail.App;

import java.io.File;
import java.io.IOException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Calendar;
import javax.mail.MessagingException;
import org.rmj.appdriver.GProperty;
import org.rmj.appdriver.SQLUtil;
import org.rmj.lib.mail.MessageInfo;
import org.rmj.lib.mail.SendMail;
import org.rmj.lib.net.LogWrapper;
import org.rmj.lib.net.SFTP_DU;
import org.rmj.appdriver.agent.GRiderX;

/**
 *
 * @author kalyptus
 */
public class RoboMail {
   private static final String jar_path = new File(RoboMail.class.getProtectionDomain().getCodeSource().getLocation().getPath()).getParentFile().getPath().replace("%20", " "); 
   private static GRiderX instance = null;
   private static SendMail pomail;
   private static final LogWrapper logwrapr = new LogWrapper("RMJMail.RoboMail", "temp/RoboMail.log");
   private static String host_dir = null;
   private static SFTP_DU sftp;
   
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
        System.setProperty("sys.default.path.config", path );

        String sender;
      
      instance = new GRiderX("gRider");
      instance.setOnline(true);
      
      if(instance.getConnection() == null){
         System.out.println("Unable to connect to the database server...");
         return;
      }
      
      ResultSet rsToSend;
   
      //set guanzon SMTP configuration as default
      if(args.length == 0)
         sender = "AutoReader-Win7";
      else
         sender = args[0];
      
      //load configuration
      loadconfig(sender);
      //System.exit(0);
      
      //load the configuration
      //pomail = new SendMail("D:/GGC_Java_Systems", sender);
      pomail = new SendMail(jar_path, sender);
   
      //try to connect to the SMTP server
      if(pomail.connect(true)){
      
         //extract records to send
         rsToSend = extract2send();
         
         //try to send email
         try {
            while(rsToSend.next()){
               try {
                  MessageInfo msginfo = new MessageInfo();
                  msginfo.setFrom(rsToSend.getString("sMailFrom"));
                  
                  //set mailto
                  if(!rsToSend.getString("sMailToxx").isEmpty()){ 
                     String mailto[] = rsToSend.getString("sMailToxx").split(";");
                     for (String mailto1 : mailto) {
                        if (!mailto1.isEmpty()) 
                           msginfo.addTo(mailto1);
                     }
                  }
                  
                  //set mailcc
                  if(!rsToSend.getString("sMailCCxx").isEmpty()){ 
                     String mailto[] = rsToSend.getString("sMailCCxx").split(";");
                     for (String mailto1 : mailto) {
                        if (!mailto1.isEmpty()) 
                           msginfo.addCC(mailto1);
                     }
                  }
                  
                  //set mailbcc
                  if(!rsToSend.getString("sMailBCCx").isEmpty()){ 
                     System.out.println("BCC:" + rsToSend.getString("sMailBCCx"));
                     String mailto[] = rsToSend.getString("sMailBCCx").split(";");
                     for (String mailto1 : mailto) {
                        if (!mailto1.isEmpty()){ 
                           msginfo.addBCC(mailto1);
                           System.out.println(mailto1);
                        }
                     }
                  }
                  
                  msginfo.setSubject(rsToSend.getString("sSubjectx"));
                  msginfo.setBody(rsToSend.getString("sMailBody"));
                  
                  //set attachment
                  if(!rsToSend.getString("sAttached").isEmpty()){ 
                     String mailto[] = rsToSend.getString("sAttached").split(";");
                     for (String mailto1 : mailto) {
                        if (!mailto1.isEmpty()) 
                           msginfo.addAttachment(host_dir + "/upload/" + rsToSend.getString("sTransNox") + "/" + mailto1);
                     }
                  }                  
                  
                  pomail.sendMessage(msginfo);
                  
                  String lsSQL;
                  lsSQL = "UPDATE Send_Mail_Master" +
                         " SET cStatusxx = '2'" +
                         " WHERE sTransNox = " + SQLUtil.toSQL(rsToSend.getString("sTransNox"));
                  instance.getConnection().createStatement().executeUpdate(lsSQL);
                  System.out.println(Calendar.getInstance().getTime());
               }   
               catch (MessagingException ex) {
                  logwrapr.severe("main: MessagingException error detected.", ex);
               } catch (IOException ex) {
                  logwrapr.severe("main: IOException error detected.", ex);         
               }
            }
         } catch (SQLException ex) {
            logwrapr.severe("main: SQLException error detected.", ex);
         } 
      }
   }
   
    private static ResultSet extract2send(){
        ResultSet rs = null;
        try {
            String lsSQL = "SELECT" +
                                "  sTransNox" + 
                                  ", dTransact" +
                                  ", sMailFrom" +
                                  ", sMailToxx" +
                                  ", sMailCCxx" +
                                  ", sMailBCCx" +
                                  ", sSubjectx" +
                                  ", sMailBody" +
                                  ", sAttached" +
                                  ", sSourceCD" +
                                  ", sSourceNo" +
                                  ", cStatusxx" +
                                  ", dPostedxx" +
                                  ", sModified" +
                                  ", dModified" +                          
                          " FROM Send_Mail_Master" +
                          " WHERE cStatusxx = '1'" + 
                          " ORDER BY dPostedxx DESC";
            
            //create statement to be use in executing queries
            rs = instance.getConnection().createStatement().executeQuery(lsSQL);
        } 
        catch (SQLException ex) {
         logwrapr.severe("extract2send: SQLException error detected.", ex);
        }
        
        return rs;
   }       
    
   private static void loadconfig(String prop){
      GProperty loProp = new GProperty(prop);
        
      sftp = new SFTP_DU();
      host_dir = loProp.getConfig("mail.sftp.fldr");
      System.out.println(instance.Decrypt(loProp.getConfig("mail.user.auth")));
      //System.out.println(loProp.getConfig("mail.user.id"));
      //System.out.println(instance.Encrypt("gu9nz0n@1946"));
      
      
      loProp.setConfig("mail.user.auth", instance.Encrypt("gu9nz0n@@@1946"));
      loProp.save(System.getProperty("sys.default.path.config") + "/config/" + prop);
      
//      loProp.save("D:/GGC_Java_Systems/AutoReader-Win7.properties");
   }    
}
