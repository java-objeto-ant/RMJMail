/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.rmj.mail.App;

import java.io.File;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.mail.MessagingException;
import org.rmj.appdriver.agent.GRiderX;
import org.rmj.lib.mail.MessageInfo;
import org.rmj.lib.mail.SendMail;
import org.rmj.lib.net.LogWrapper;

/**
 *
 * @author sayso
 */
public class SendCebuana {
   private static final String jar_path = new File(SendPayslip.class.getProtectionDomain().getCodeSource().getLocation().getPath()).getParentFile().getPath().replace("%20", " "); 
   private static GRiderX instance = null;
   private static SendMail pomail;
   private static final LogWrapper logwrapr = new LogWrapper("RMJMail.SendCebuana", "temp/SendCebuana.log");

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

      //set guanzon SMTP configuration as default
      String sender;
      if(args.length == 0)
         sender = "GMail";
      else
         sender = args[0];
        
      //load the configuration
      pomail = new SendMail(path, sender);
         
      //try to connect to the SMTP server
      if(pomail.connect(true)){
            try {
                MessageInfo msginfo = new MessageInfo();

                Calendar cal = Calendar.getInstance();
                cal.add(Calendar.DATE, -1);
                DateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd");
                DateFormat dateFormat2 = new SimpleDateFormat("MMddyyyy");
                
                //msginfo.addTo(java.net.IDN.toUnicode(new String(encode(email))));
                msginfo.addTo("llbuclares@guanzongroup.com.ph");
                msginfo.addCC("jcsayson@guanzongroup.com.ph");
                msginfo.addCC("maestrada@guanzongroup.com.ph");
                msginfo.setSubject("Cebuana - " + dateFormat.format(cal.getTime()));

                msginfo.setBody("As stated");
                msginfo.addAttachment("/home/guanzon/cebuana/" + dateFormat2.format(cal.getTime()) + "_Cebuana.xls");
                //kalyptus - 2017.05.19 01:44pm
                //set the no reply account as the sender instead of the petmgr
                msginfo.setFrom("No Reply <no-reply@guanzongroup.com.ph>");
                pomail.sendMessage(msginfo);
            } catch (MessagingException ex) {
                  logwrapr.severe("main: MessagingException error detected.", ex);
            } catch (IOException ex) {
                  logwrapr.severe("main: IOException error detected.", ex);
            }
      }
   }   
}
