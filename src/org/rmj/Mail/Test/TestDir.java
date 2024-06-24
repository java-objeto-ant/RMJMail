/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.rmj.mail.Test;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.rmj.mail.App.FTP_Download;

/**
 *
 * @author kalyptus
 */
public class TestDir {
//   private static String jar_path = new File(TestDir.class.getProtectionDomain().getCodeSource().getLocation().toURI().getPath()).getParentFile().getPath(); 
   private static String jar_path = new File(TestDir.class.getProtectionDomain().getCodeSource().getLocation().getPath()).getParentFile().getPath().replace("%20", " "); 
   public static void main(String[] args){
      String program = "java -Xmx1g -cp D:/GGC_Java_Systems/RMJMail.jar org.rmj.Mail.App.FTP_Download";
      String transno = "M00115000004";
      String local = "D:/GGC_Java_Systems/temp/";
      String file = "SUZUKI DEAL1.xlsx";

      try {
//           Runtime.getRuntime().exec( program  + " " + transno + " \"" + local + "\" \"" + file + "\"");         
         Process proc =  Runtime.getRuntime().exec( program  + " " + transno + " \"" + local + "\" \"" + file + "\"");
         proc.waitFor();
      
         InputStream in = proc.getInputStream();
         InputStream err = proc.getErrorStream();

         byte b[]=new byte[in.available()];
         in.read(b,0,b.length);
         System.out.println(new String(b));

         byte c[]=new byte[err.available()];
         err.read(c,0,c.length);
         System.out.println(new String(c));         
      } catch (IOException ex) {
         System.out.println(ex.getMessage());
      } catch (InterruptedException ex) {
         System.out.println(ex.getMessage());
      }
        
   }   
}
