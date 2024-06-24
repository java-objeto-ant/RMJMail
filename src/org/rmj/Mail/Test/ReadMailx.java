/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.rmj.mail.Test;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.mail.Folder;
import javax.mail.Message;
import javax.mail.MessagingException;
import org.rmj.lib.mail.MessageInfo;
import org.rmj.lib.mail.ReadMail;
import org.rmj.appdriver.agent.MsgBox;

/**
 *
 * @author kalyptus
 */
public class ReadMailx {
   private static ReadMail pomail;
   public static void main(String[] args) {
      String sender;
      
      if(args.length == 0)
         sender = "AutoReader";
      else
         sender = args[0];
      
      //load the configuration
      pomail = new ReadMail("D:/GGC_Java_Systems", sender);
      //connect to the server
      if (pomail.connect(false)){
         try {
            //open folder to fetch
            pomail.openFolder("INBOX", Folder.READ_WRITE);
            Message message = pomail.getMessage(pomail.getMessageCount()-3);
            MessageInfo msginfo = pomail.getMessageInfo(message, "M00115000002");
           
            System.out.println("xSUBJECT: " + msginfo.getSubject());
            System.out.println("xDATE   : " + msginfo.getDate());
            System.out.println("xFROM   : " + msginfo.getFrom());
            if(msginfo.Size_To() > 0){
               StringBuilder to = new StringBuilder();
               for(int n=0;n<=msginfo.Size_To()-1;n++){
                  to.append(";").append(msginfo.getTo(n));
               }
               System.out.println("xTO     : " + to.substring(1));               
            }

            if(msginfo.Size_CC() > 0){
               StringBuilder to = new StringBuilder();
               for(int n=0;n<=msginfo.Size_To()-1;n++){
                  to.append(";").append(msginfo.getCC(n));
               }
               System.out.println("xCC     : " + to.substring(1));               
            }
            
            if(msginfo.Size_BCC() > 0){
               StringBuilder to = new StringBuilder();
               for(int n=0;n<=msginfo.Size_To()-1;n++){
                  to.append(";").append(msginfo.getBCC(n));
               }
               System.out.println("xBCC    : " + to.substring(1));               
            }
            
            System.out.println("xCONTENT: " + msginfo.getBody());

            if(msginfo.Size_Attachment()> 0){
               StringBuilder to = new StringBuilder();
               for(int n=0;n<=msginfo.Size_To()-1;n++){
                  to.append(";").append(msginfo.getAttachment(n));
               }
               System.out.println("xATTACHED: " + to.substring(1));               
            }
            
//            MsgBox.showOk("We will delete the message after this");
//
//            pomail.deleteMessage(message);
//            pomail.closeFolder();
//            pomail.disconnect();
            
         } catch (MessagingException ex) {
            Logger.getLogger(ReadMailx.class.getName()).log(Level.SEVERE, null, ex);
         } catch (IOException ex) {
            Logger.getLogger(ReadMailx.class.getName()).log(Level.SEVERE, null, ex);
         }
      }
   }   
}
