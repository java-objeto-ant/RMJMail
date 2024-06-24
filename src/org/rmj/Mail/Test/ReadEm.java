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
import org.apache.commons.lang3.StringUtils;

/**
 *
 * @author kalyptus
 */
public class ReadEm {
   public static void main(String[] args) {
      String certloc = "D:/GGC_Java_Systems/lib/cacerts";
      
      System.out.println(certloc);
      System.setProperty("javax.net.ssl.trustStore", certloc);
      System.setProperty("javax.net.ssl.trustStorePassword","changeit");
      
      Properties props = new Properties();

      try {
         props.load(new FileInputStream("D:/GGC_Java_Systems/config/AutoReader.properties"));
      } 
      catch (IOException ex) {
         ex.printStackTrace();
      }
      
      Session session = Session.getDefaultInstance(props,
         new javax.mail.Authenticator() {
            protected PasswordAuthentication getPasswordAuthentication() {
               return new PasswordAuthentication("support@guanzongroup.com.ph", "it@auditacctg");
          }
      });      
      //session.setDebug(true);
      
      try {
   
            Store store = session.getStore("imap");
            store.connect();

            Folder inbox = store.getFolder("INBOX");
            inbox.open(Folder.READ_WRITE);
            Message msg = inbox.getMessage(inbox.getMessageCount() - 2);
            
            Multipart mp = (Multipart) msg.getContent();
            System.out.println("BODY PART COUNT: " + mp.getCount());
            
            Address[] in = msg.getFrom();
            for (Address address : in) {
                System.out.println("FROM:" + address.toString());
            }
            
            Address[] to = msg.getRecipients(Message.RecipientType.TO);
            for (Address address : to) {
                System.out.println("TO:" + address.toString());
            }
            
            Address[] cc = msg.getRecipients(Message.RecipientType.CC);
            if(cc != null){
               for (Address address : cc) {
                   System.out.println("CC:" + address.toString());
               }
            }
            
            Address[] bcc = msg.getRecipients(Message.RecipientType.BCC);
            if(bcc != null){
               for (Address address : bcc) {
                   System.out.println("BCC:" + address.toString());
               }
            }
            
            System.out.println("SENT DATE:" + msg.getSentDate());
            System.out.println("SUBJECT:" + msg.getSubject());
            //System.out.println("CONTENT:" + msg.getContent().toString());   
            
            processMessageBody(msg);
            
            if(true) return;
      } catch (Exception mex) {
            mex.printStackTrace();
      }
   }  
   
    public static void processMessageBody(Message message) {
        try {
            Object content = message.getContent();
            // check for string
            // then check for multipart
            if (content instanceof String) {
                //System.out.println("MESSAGE BODY IS A STRING!");                
                System.out.println("BODY: " + content);
            } else if (content instanceof Multipart) {
                //System.out.println("MESSAGE BODY IS A MULTIPART!");                
                Multipart multiPart = (Multipart) content;
                procesMultiPart(multiPart);
            } else if (content instanceof InputStream) {
                //System.out.println("MESSAGE BODY IS AN INPUT STREAM!");                
                InputStream inStream = (InputStream) content;
                int ch;
                while ((ch = inStream.read()) != -1) {
                    System.out.write(ch);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        } catch (MessagingException e) {
            e.printStackTrace();
        }
    }

    public static void procesMultiPart(Multipart content) {
        //System.out.println("MULTIPART IS HERE!");
        try {
            int multiPartCount = content.getCount();
            for (int i = 0; i < multiPartCount; i++) {
                BodyPart bodyPart = content.getBodyPart(i);
                //System.out.println("Count: " + i);
                //System.out.println("Content Type: " + bodyPart.getContentType());
                Object o;
                o = bodyPart.getContent();
                if(Part.ATTACHMENT.equalsIgnoreCase(bodyPart.getDisposition())){
                  System.out.println("Downloading attachement: " + bodyPart.getFileName());    
                  InputStream is = bodyPart.getInputStream();
                  File f = new File("D:/GGC_Java_Systems/temp/" + bodyPart.getFileName());
                  FileOutputStream fos = new FileOutputStream(f);
                  byte[] buf = new byte[4096];
                  int bytesRead;
                  while((bytesRead = is.read(buf))!=-1) {
                      fos.write(buf, 0, bytesRead);
                  }
                  fos.close();
                  //attachments.add(f);                   
                }else if(bodyPart.isMimeType("text/plain; charset=UTF-8")) {
                    //System.out.println("MULTIPART HAS A MIME TYPE OF " + bodyPart.getContentType());                    
                    System.out.println("BODY: " + o);                   
//                } 
//                else if (o instanceof String) {
//                    System.out.println("MULTIPART IS A STRING!");                    
//                    System.out.println(o);
                } else if (o instanceof Multipart) {
                    //System.out.println("CALL MULTIPART AGAIN!");
                    procesMultiPart((Multipart) o);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        } catch (MessagingException e) {
            e.printStackTrace();
        }
    }    
}
