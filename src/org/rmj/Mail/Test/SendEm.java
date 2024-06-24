/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package org.rmj.mail.Test;

import java.util.*;
import java.io.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.mail.*;
import javax.mail.internet.*;
import javax.activation.*;

/**
 *
 * @author kalyptus
 */
public class SendEm {
   public static void main(String[] args) {
      // create some properties and get the default Session
      //String certloc = System.getProperty("java.home") + "\\lib\\security\\cacerts";
      String certloc = "D:/GGC_Java_Systems/lib/cacerts";
      
      System.out.println(certloc);
      System.setProperty("javax.net.ssl.trustStore", certloc);
      System.setProperty("javax.net.ssl.trustStorePassword","changeit");
     
      //Properties props = System.getProperties();
      Properties props = new Properties();

//      
//      try {
//         props.load(new FileInputStream("D:/GGC_Java_Systems/config/GMail.properties"));
//      } 
//      catch (IOException ex) {
//         ex.printStackTrace();
//      }
            
      props.put("mail.smtp.host", "192.168.10.220");
      props.put("mail.smtp.port", "587");
      props.put("mail.smtp.starttls.required", "true");
      props.put("mail.smtp.localhost", "192.168.10.220");
      props.put("mail.smtp.auth", "true");
      
      props.put("mail.store.protocol", "imap");
      props.put("mail.imap.host", "192.168.10.220");
      props.put("mail.imap.port", "143");
      props.put("mail.imap.starttls.enable", "true");      
         
      props.put("mail.user.id", "masayson@guanzongroup.com.ph");
      props.put("mail.user.auth", "Iwewwaipae");
      
//      try {
//          props.store(new FileOutputStream("D:/GGC_Java_Systems/config/GMail.properties"), null);
//      } catch (IOException ex) {
//          ex.printStackTrace();
//      }
        
      //kalyptus
      final String user = props.getProperty("mail.user.id");
      final String pass = props.getProperty("mail.user.auth");
      
      Session session = Session.getInstance(props,
          new javax.mail.Authenticator() {
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(user, pass);
          }
        });
      session.setDebug(true);
      
      String from = "No Reply <lgk_guanzon@yahoo.com>";
      String to = "Marlon A. Sayson <stargaze_75@yahoo.com>";      
      String msgText1 = "Hope this will work.\n";
      String subject = "Sending Managers Salary";
      String filename = "D:\\GGC_Java_Systems\\temp\\Managers Salary.xlsx"; 

      //String javahome = System.getProperty("java.home");
      //System.out.println("JAVA_HOME:" + javahome);
      
      try {
         Transport tr = session.getTransport("smtp");
         tr.connect();
      
         System.out.println("Is connection already established?");
         // create a message
         MimeMessage msg = new MimeMessage(session);
         msg.setFrom(new InternetAddress(from));
         
         InternetAddress[] address = {new InternetAddress(to)};
         msg.setRecipients(Message.RecipientType.TO, address);

         InternetAddress[] addressCC = {new InternetAddress("masayson@guanzongroup.com.ph")};
         msg.setRecipients(Message.RecipientType.CC, addressCC);
         
         msg.setSubject(subject);
         
         // create and fill the first message part
         MimeBodyPart mbp1 = new MimeBodyPart();
         mbp1.setText(msgText1);

         // create the second message part usually the attachement
         MimeBodyPart mbp2 = new MimeBodyPart();

         // attach the file to the message
         if(!filename.isEmpty()){
            mbp2.attachFile(filename);
         } 
         
         /*
          * Use the following approach instead of the above line if
          * you want to control the MIME type of the attached file.
          * Normally you should never need to do this.
          *
         FileDataSource fds = new FileDataSource(filename) {
            public String getContentType() {
                return "application/octet-stream";
            }
         };
         mbp2.setDataHandler(new DataHandler(fds));
         mbp2.setFileName(fds.getName());
         */

         // create the Multipart and add its parts to it
         Multipart mp = new MimeMultipart();
         mp.addBodyPart(mbp1);
         
         if(!filename.isEmpty()){ 
            mp.addBodyPart(mbp2);
         }   

         // add the Multipart to the message
         msg.setContent(mp);

         // set the Date: header
         msg.setSentDate(new Date());

         // send the message
         msg.saveChanges();        
         
         tr.sendMessage(msg, msg.getAllRecipients());
         
      } catch (MessagingException mex) {
         mex.printStackTrace();
         Exception ex = null;
         if ((ex = mex.getNextException()) != null) {
            ex.printStackTrace();
         }
      } catch (IOException mex) {
         mex.printStackTrace();
      }
   }   
}
