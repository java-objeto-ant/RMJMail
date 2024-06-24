/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.rmj.mail.App;

/**
 *
 * @author sayso
 */
public class SendNow {
   public static void main(String[] args) {
       //set guanzon SMTP configuration as default
      String sender;       
      if(args.length == 0)
         sender = "GMail";
      else
         sender = args[0];      
   }    
}
