/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package org.rmj.Mail.Lib;

import java.util.*;
import java.io.*;
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;
import javax.mail.*;
import javax.mail.internet.*;
import javax.activation.*;
import javax.mail.Message.RecipientType;

import org.apache.commons.lang3.StringUtils;
import org.rmj.appdriver.GCrypt;
import org.rmj.appdriver.agent.MsgBox;

/**
 *
 * @author kalyptus
 */
public class SendMail {
   String SIGNATURE = "08220326";
   Properties po_props = new Properties();
   String ps_path;
   Boolean pb_init;
   Session po_session;
   Transport po_trans;
   MimeMessage po_msg;
   Multipart po_mprt;

   //Inititializes the mail object using the path and configuration file
   //Usage:
   //    p_omail = new SendMail("D:/GGC_Java_Systems", "GMail")
   public SendMail(String app_path, String propfile){
      ps_path = app_path;

      System.setProperty("javax.net.ssl.trustStore", app_path + "/lib/cacerts");
      System.setProperty("javax.net.ssl.trustStorePassword","changeit");
      
      try {
         po_props.load(new FileInputStream(ps_path + "/config/" + propfile + ".properties"));
         pb_init = true;
      } 
      catch (IOException ex) {
         //ex.printStackTrace();
         pb_init = false;
      }
   }   
   
   //Connect to the server
   public boolean connect(boolean isdebug){
      //Don't allow connection if not properly initiaze.
      if(!pb_init) return false;

      GCrypt loEnc = new GCrypt(SIGNATURE);
      
      final String user = po_props.getProperty("mail.user.id");
      final String pass = loEnc.decrypt(po_props.getProperty("mail.user.auth"));
      System.out.println(user);
      System.out.println(pass);
      //MsgBox.showOk(pass);Taig2PMapua
      po_session = Session.getDefaultInstance(po_props,
          new javax.mail.Authenticator() {
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(user, pass);
          }
        });
      
      po_session.setDebug(isdebug);

      boolean isOk = false;
      
      //Initialize connection here....
      try {
         po_trans = po_session.getTransport("smtp");
         po_trans.connect();
         isOk = true;
      } catch (NoSuchProviderException ex) {
         //Logger.getLogger(SendMail.class.getName()).log(Level.SEVERE, null, ex);
      } catch (MessagingException ex) {
         //Logger.getLogger(SendMail.class.getName()).log(Level.SEVERE, null, ex);
      }
     
      return isOk;
   }   
   
   // initialize the message
   public boolean initmsg(){
      boolean bOk = true;
      
      //Don't allow initialization of message if not properly initiaze.
      if(!pb_init) return bOk;
      
      po_msg = new MimeMessage(po_session);
      po_mprt = new MimeMultipart();
      
      try {
         po_msg.setFrom(new InternetAddress(po_props.getProperty("mail.user.id")));
      } catch (MessagingException ex) {
         bOk = false;
      }
      
      return bOk;
   }
   
   // set sender
   public void setFrom(String from) throws MessagingException{
      po_msg.setFrom(new InternetAddress(from));
   }
   
   // set recepients
   public void setRecipients(RecipientType type, String to) throws AddressException, MessagingException{
         InternetAddress[] address = {new InternetAddress(to)};
         po_msg.setRecipients(type, address);
   }   
   
   // add additional recipient
   public void addRecipients(RecipientType type, String to) throws AddressException, MessagingException{
      InternetAddress[] address = {new InternetAddress(to)};
      po_msg.addRecipients(type, address);
   }   
   
   //set subject
   public void setSubject(String subject) throws MessagingException{
      po_msg.setSubject(subject);
   }
   
   //set email message
   public void setBody(String message) throws MessagingException{
      MimeBodyPart mbp1 = new MimeBodyPart();
      mbp1.setText(message);      
      po_mprt.addBodyPart(mbp1);
   }
   
   //set attachement
   public void addAttachment(String filename) throws IOException, MessagingException{
      MimeBodyPart mbp1 = new MimeBodyPart();
      mbp1.attachFile(filename);      
      po_mprt.addBodyPart(mbp1);
   }

   // send message
   public void sendMessage() throws MessagingException{
      // add the Multipart to the message
      po_msg.setContent(po_mprt);
      // set the Date: header
      po_msg.setSentDate(new Date());
      // don't forget to save
      po_msg.saveChanges();  
      // send the message
      po_trans.sendMessage(po_msg, po_msg.getAllRecipients());
   }
   
   // send message
   public void sendMessage(MessageInfo msginfo) throws MessagingException, IOException{
      //initialize message
      initmsg();

      //set from
      if(!msginfo.getFrom().isEmpty()){
         setFrom(msginfo.getFrom());
      }

      //set to
      if(msginfo.Size_To() > 0 ){
         StringBuffer lsrcpt = new StringBuffer();
         for(int n=0;n<=msginfo.Size_To()-1;n++){
            addRecipients(Message.RecipientType.TO, msginfo.getTo(n));               
            //lsrcpt.append(";").append(msginfo.getTo(n));
         }
         //setRecipients(Message.RecipientType.TO, lsrcpt.substring(1));               
      }

      //set cc
      if(msginfo.Size_CC() > 0 ){
         StringBuffer lsrcpt = new StringBuffer();
         for(int n=0;n<=msginfo.Size_CC()-1;n++){
            addRecipients(Message.RecipientType.CC, msginfo.getCC(n));               
            //lsrcpt.append(";").append(msginfo.getCC(n));
         }
         //setRecipients(Message.RecipientType.CC, lsrcpt.substring(1));               
      }
      
      //set bcc
      if(msginfo.Size_BCC() > 0 ){
         StringBuffer lsrcpt = new StringBuffer();
         for(int n=0;n<=msginfo.Size_BCC()-1;n++){
            addRecipients(Message.RecipientType.BCC, msginfo.getBCC(n));
            //lsrcpt.append(";").append(msginfo.getBCC(n));
         }
         //setRecipients(Message.RecipientType.BCC, lsrcpt.substring(1));               
      }

      setSubject(msginfo.getSubject());
      setBody(msginfo.getBody());

      //set attachment
      if(msginfo.Size_Attachment()> 0 ){
         for(int n=0;n<=msginfo.Size_Attachment()-1;n++){
            addAttachment(msginfo.getAttachment(n));
         }
      }
      
      // add the Multipart to the message
      po_msg.setContent(po_mprt);
      // set the Date: header
      po_msg.setSentDate(new Date());
      // don't forget to save
      po_msg.saveChanges();  
      // send the message
      po_trans.sendMessage(po_msg, po_msg.getAllRecipients());
   }
   
   // disconnect from server...
   public void disconnect() throws MessagingException{
      po_trans.close();
   }
}   
