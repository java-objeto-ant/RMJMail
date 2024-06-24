/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.rmj.mail.Test;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

/**
 *
 * @author kalyptus
 */
public class TestVB {
   private static String jar_path = new File(TestDir.class.getProtectionDomain().getCodeSource().getLocation().getPath()).getParentFile().getPath().replace("%20", " "); 
   public static void main(String[] args){
      String program = "C:/GGC_Systems/uWatchBin.exe";
      try {
//           Runtime.getRuntime().exec( program  + " " + transno + " \"" + local + "\" \"" + file + "\"");         
         Process proc =  Runtime.getRuntime().exec(program);
         System.out.println("Andito na ba #1");
         proc.waitFor();
         System.out.println("Andito na ba #2");
      
         InputStream in = proc.getInputStream();
         InputStream err = proc.getErrorStream();
         System.out.println("Andito na ba #3");

         byte b[]=new byte[in.available()];
         in.read(b,0,b.length);
         System.out.println(new String(b));
         System.out.println("Andito na ba #4");

         byte c[]=new byte[err.available()];
         err.read(c,0,c.length);
         System.out.println(new String(c));         
         
         System.out.println("Andito na ba#5");
      } catch (IOException ex) {
         System.out.println(ex.getMessage());
      } catch (InterruptedException ex) {
         System.out.println(ex.getMessage());
      }
        
   }      
}
