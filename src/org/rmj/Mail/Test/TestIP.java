/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.rmj.mail.Test;

import java.net.*;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

public class TestIP
{
  public void getIPAddresses() 
  {
     try {
        Enumeration interfaces = NetworkInterface.getNetworkInterfaces();
        while ( interfaces.hasMoreElements() )
        {
           NetworkInterface intf = (NetworkInterface)interfaces.nextElement();
           
           Enumeration addresses = intf.getInetAddresses();
           while ( addresses.hasMoreElements() )
           {
              InetAddress address = (InetAddress) addresses.nextElement();
              if(address.toString().contains("172")){
                 System.out.println("Net Name: " + intf.getDisplayName() + "»IP address: " + address.toString());
              }
           }
        }
     } catch (SocketException ex) {
        Logger.getLogger(TestIP.class.getName()).log(Level.SEVERE, null, ex);
     }
   }

  public static String getHostName(){
     String hostname = "";
     try {
        InetAddress addr = InetAddress.getLocalHost();
        hostname = addr.getHostName();
     } catch (UnknownHostException ex) {
        Logger.getLogger(TestIP.class.getName()).log(Level.SEVERE, null, ex);
     }
     
     return hostname;
  }
  
   public static void main(String[] args) 
   {
      System.out.println("Hostname: " + getHostName());
      TestIP client = new TestIP();
      client.getIPAddresses();
   }
}