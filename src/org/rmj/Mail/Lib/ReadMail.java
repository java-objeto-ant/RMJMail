/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.rmj.Mail.Lib;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import javax.mail.Address;
import javax.mail.BodyPart;
import javax.mail.Flags;
import javax.mail.Folder;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Multipart;
import javax.mail.NoSuchProviderException;
import javax.mail.Part;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Store;
import javax.mail.Transport;
import javax.mail.internet.MimeMessage;
import javax.mail.search.FlagTerm;
import org.rmj.appdriver.GCrypt;
import org.rmj.appdriver.SQLUtil;
/**
 *
 * @author kalyptus
 */
public class ReadMail {
   String SIGNATURE = "08220326";
   Properties po_props = new Properties();
   String ps_path;
   Boolean pb_init;
   Session po_session;
   Store po_store;
   Folder po_folder;
   Message[] messages;
   
   //Inititializes the mail object using the path and configuration file
   //Usage:
   //    p_omail = new ReadMail("D:/GGC_Java_Systems", "GMail")
   public ReadMail(String app_path, String propfile){
      ps_path = app_path;

      System.setProperty("javax.net.ssl.trustStore", ps_path + "/lib/cacerts");
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
   
   public int getFetchMsgCount(){
      return messages.length;
   }
   
   public Message getFetchMessage(int item){
      return messages[item];
   }

   public int getMessageCount() throws MessagingException{
      return po_folder.getMessageCount();
   }
   
   public Message getMessage(int item) throws MessagingException{
      return po_folder.getMessage(item);
   }
   
   public boolean connect(boolean isdebug){
      //Don't allow connection if not properly initiaze.
      if(!pb_init) return false;
      
      GCrypt loEnc = new GCrypt(SIGNATURE);

      final String user = po_props.getProperty("mail.user.id");
      final String pass = loEnc.decrypt(po_props.getProperty("mail.user.auth"));
      
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
         po_store = po_session.getStore("imap");
         po_store.connect();
            
         isOk = true;
      } catch (NoSuchProviderException ex) {
         //Logger.getLogger(SendMail.class.getName()).log(Level.SEVERE, null, ex);
      } catch (MessagingException ex) {
         //Logger.getLogger(SendMail.class.getName()).log(Level.SEVERE, null, ex);
      }
     
      return isOk;
   }   

   // ex: openFolder("INBOX", Folder.READ_WRITE);
   public void openFolder(String folder, int stat) throws MessagingException{
      po_folder = po_store.getFolder(folder);
      po_folder.open(stat);
   }
   
   public void fetchMesage(boolean read) throws MessagingException{
      messages = po_folder.search(new FlagTerm(new Flags(Flags.Flag.SEEN), read));
   }
   
   public void fetchMesage() throws MessagingException{
      messages = po_folder.getMessages();
   }
   
   public void closeFolder() throws MessagingException{
      po_folder.close(true);
   }
   
   public MessageInfo getMessageInfo(Message message, String msgid) throws MessagingException, IOException{
      MessageInfo msginfo = new MessageInfo();
      
      //set from
      Address[] in = message.getFrom();
      for (Address address : in) {
          msginfo.setFrom(address.toString());
      }      

      //set to
      Address[] to = message.getRecipients(Message.RecipientType.TO);
      for (Address address : to) {
          msginfo.addTo(address.toString());
      }      

      //set cc
      Address[] cc = message.getRecipients(Message.RecipientType.CC);
      if(cc != null){
         for (Address address : cc) {
             msginfo.addCC(address.toString());
         }      
      }

      //set bcc
      Address[] bcc = message.getRecipients(Message.RecipientType.BCC);
      if(bcc != null){ 
         for (Address address : bcc) {
             msginfo.addBCC(address.toString());
         }      
      }
      
      //set subject
      msginfo.setSubject(message.getSubject());
      
      //set sent date
      msginfo.setDate(SQLUtil.dateFormat(message.getSentDate(), "yyyy-MM-dd HH:mm:ss"));
      
      processMessageBody(message, msginfo, msgid);      
      
      return msginfo;
   }

   private void processMessageBody(Message message, MessageInfo msginfo, String msgid) throws MessagingException, IOException {
      Object content = message.getContent();
      // check for string
      // then check for multipart
      if (content instanceof String) {
         //System.out.println("MESSAGE BODY IS A STRING!");                
         System.out.println("BODY: " + content);
         msginfo.setBody(content.toString());
      } else if (content instanceof Multipart) {
         //System.out.println("MESSAGE BODY IS A MULTIPART!");                
         Multipart multiPart = (Multipart) content;
         procesMultiPart(multiPart, msginfo, msgid);
      } else if (content instanceof InputStream) {
         //System.out.println("MESSAGE BODY IS AN INPUT STREAM!");                
         InputStream inStream = (InputStream) content;
         int ch;
         StringBuilder s = new StringBuilder();
         while ((ch = inStream.read()) != -1) {
            s.append(ch);
         }
         msginfo.setBody(s.toString());
      }
   }

   private void procesMultiPart(Multipart content, MessageInfo msginfo, String msgid) throws MessagingException, IOException {
      //System.out.println("MULTIPART IS HERE!");
      int multiPartCount = content.getCount();
      for (int i = 0; i < multiPartCount; i++) {
         BodyPart bodyPart = content.getBodyPart(i);
         Object o;
         o = bodyPart.getContent();
         if(Part.ATTACHMENT.equalsIgnoreCase(bodyPart.getDisposition())){
            //save the filename of the attachment to the msginfo object
            msginfo.addAttachment(bodyPart.getFileName());
                    
            //create the directory for the attachment based on the msgid
            String uploadpath = po_props.getProperty("mail.sftp.fldr") + "download/" + msgid + "/";
            File uploadpth = new File(uploadpath);
            if (!uploadpth.exists()) {
                uploadpth.mkdirs();
            }        
            
            //dowload the attachment to the created attachment folder
            InputStream is = bodyPart.getInputStream();
            File f = new File(uploadpath + bodyPart.getFileName());
            FileOutputStream fos = new FileOutputStream(f);
            byte[] buf = new byte[4096];
            int bytesRead;
            while((bytesRead = is.read(buf))!=-1) {
                fos.write(buf, 0, bytesRead);
            }
            fos.close();
            //attachments.add(f);                   
         }else if(bodyPart.isMimeType("text/plain")) {
            msginfo.setBody(msginfo.getBody().concat("\n").concat(bodyPart.getContentType()).concat("\n").concat(o.toString()));
            //System.out.println("MULTIPART HAS A MIME TYPE OF " + bodyPart.getContentType());                    
            //System.out.println("BODY: " + o);                   
         } else if (o instanceof Multipart) {
            //System.out.println("CALL MULTIPART AGAIN!");
            procesMultiPart((Multipart) o, msginfo, msgid);
         }else{
            msginfo.setBody(msginfo.getBody().concat("\n").concat(bodyPart.getContentType()).concat("\n").concat(o.toString()));
         }
      }
   }    
   
   public void deleteMessage(Message message) throws MessagingException {
      message.setFlag(Flags.Flag.DELETED, true);
   }   

    // disconnect from server...
   public void disconnect() throws MessagingException{
      po_store.close();
   }
}
