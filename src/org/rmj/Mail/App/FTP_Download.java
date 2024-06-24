/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.rmj.mail.App;

import java.io.File;
import org.rmj.appdriver.GCrypt;
import org.rmj.appdriver.GProperty;
import org.rmj.lib.net.LogWrapper;
import org.rmj.lib.net.SFTP_DU;

/**
 *
 * @author kalyptus
 * FTP_Download "M00115000001" "D:/GGC_Java_Systems/temp/download/M00115000001" "Manager's Assembly.xlsx"
 */
public class FTP_Download {
   //kalyptus - 2015.11.04 03:14pm
   //get the path of this hava file once it was executed...
   private static final String jar_path = new File(FTP_Download.class.getProtectionDomain().getCodeSource().getLocation().getPath()).getParentFile().getPath().replace("%20", " "); 
   
   private static final String SIGNATURE = "08220326";
   private static String GUANZON_SITE = null; 
   private static final LogWrapper logwrapr = new LogWrapper("RMJMail.FTP_Download", jar_path + "/temp/FTP_Download.log");
   private static String host_dir = null;
   private static SFTP_DU sftp;
   
   public static void main(String[] args) {

       
       
      // this utility should always have a 3 parameter 
      // source, client_dir, file
      if(args.length != 3){
         System.exit(1);
      }
      
      //load configuration
      loadconfig();
      
      // /srv1/automail/download/
      //    for downloaded mail
      String srvr = host_dir + "download/" + args[0];
      String clnt = args[1];
      String file = args[2];
      
      File downpth = new File(clnt);
      if (!downpth.exists()) {
          downpth.mkdirs();
      }   
            
      try {
         sftp.Download(srvr, clnt, file);
         System.exit(0);
      } catch (Exception ex) {
        logwrapr.severe("Exception error detected.", ex);
        System.exit(1);
      }
   }  
   
   private static void loadconfig(){
      GCrypt loEnc = new GCrypt(SIGNATURE);
      GProperty loProp = new GProperty(jar_path + "/config/AutoReader");
        
      sftp = new SFTP_DU();

      sftp.setPort(Integer.valueOf(loProp.getConfig("mail.sftp.port")));
      sftp.setUser(loEnc.decrypt(loProp.getConfig("mail.sftp.user")));
      sftp.setPassword(loEnc.decrypt(loProp.getConfig("mail.sftp.pass")));
      
      String sFTPHost[] = loProp.getConfig("mail.sftp.host").split(";");
        
      for(int x=0;x < sFTPHost.length;x++){
         if(sftp.xConnect(sFTPHost[x]))
            x = sFTPHost.length;
      }

      host_dir = loProp.getConfig("mail.sftp.fldr");
   }        

   
}    
    

