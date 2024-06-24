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
 /*** @author kalyptus
 * FTP_Upload "M00115000001" "D:/GGC_Java_Systems/temp/upload/M00115000001" "Manager's Assembly.xlsx"
 * 
   Private Sub Command1_Click()
      Dim lsShell As String
      lsShell = "D:/GGC_Java_systems/ftp_upload.bat" + " " _
                  + "M00115000001" + " " _
                  + "D:/GGC_Java_Systems/temp/upload/M00115000001" + " " _
                  + """" + "Manager's Assembly.xlsx" + """"
      Debug.Print lsShell
      Shell lsShell
   End Sub
 * 
 * 
 */

public class FTP_Upload {
   //kalyptus - 2015.11.04 03:14pm
   //get the path of this java file once it was executed...
   private static final String SIGNATURE = "08220326";
   private static String GUANZON_SITE = null; 
   private static LogWrapper logwrapr;
   private static String host_dir = null;
   private static SFTP_DU sftp;

    public static void main(String[] args) {       
        String path;
        if(System.getProperty("os.name").toLowerCase().contains("win")){
            path = "D:/GGC_Java_Systems";
        }
        else{
            path = "/srv/GGC_Java_Systems";
        }
        System.setProperty("sys.default.path.config", path);
        
        logwrapr = new LogWrapper("RMJMail.FTP_Upload", System.getProperty("sys.default.path.config") + "/temp/FTP_Upload.log");
       
      // this utility should always have a 3 parameter 
      // source, client_dir, file
      System.out.println(args.length);
      if(args.length != 3){
         System.exit(1);
      }
      
      System.out.println("Loading config...");
      //load configuration
      loadconfig();
      
      // /srv1/automail/upload/
      //      for sending mail
      
      String srvr = host_dir + "upload/" + args[0];
      String clnt = args[1];
      String file = args[2];
      
      System.out.println("Creating destination...");
      sftp.mkdir(srvr);
            
    try {
        System.out.println("Uploading file - " + file);
        sftp.Upload(clnt, srvr, file);
        logwrapr.info("File uploaded successfully - " + file);
        System.out.println("File uploaded successfully - " + file);
        System.exit(0);
    } catch (Exception ex) {
        logwrapr.severe("Exception error detected.", ex);
        ex.printStackTrace();
        System.exit(1);
    }
}  
   
   private static void loadconfig(){
      GCrypt loEnc = new GCrypt(SIGNATURE);
      GProperty loProp = new GProperty("/config/AutoReader");
        
      sftp = new SFTP_DU();

      sftp.setPort(Integer.valueOf(loProp.getConfig("mail.sftp.port")));
      sftp.setUser(loEnc.decrypt(loProp.getConfig("mail.sftp.user")));
      //System.out.println(loEnc.decrypt(loProp.getConfig("mail.sftp.user")));
      //logwrapr.info(loEnc.decrypt(loProp.getConfig("mail.sftp.user")));
      //greplusr
      sftp.setPassword(loEnc.decrypt(loProp.getConfig("mail.sftp.pass")));
      //System.out.println(loEnc.decrypt(loProp.getConfig("mail.sftp.pass")));
      //logwrapr.info(loEnc.decrypt(loProp.getConfig("mail.sftp.pass")));
      //2t4tsotc!Aiotrt?
      String sFTPHost[] = loProp.getConfig("mail.sftp.host").split(";");
        
      for(int x=0;x < sFTPHost.length;x++){
         if(sftp.xConnect(sFTPHost[x])){
            System.out.println("Connected to " + sFTPHost[x]);
            x = sFTPHost.length;
         }
      }
      
      host_dir = loProp.getConfig("mail.sftp.fldr");
      
      System.err.println(loEnc.decrypt(loProp.getConfig("mail.sftp.user")));
      System.err.println(loEnc.decrypt(loProp.getConfig("mail.sftp.pass")));
      
   }            
}
