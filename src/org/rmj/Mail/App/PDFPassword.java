/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.rmj.mail.App;

import com.itextpdf.text.DocumentException;
import com.itextpdf.text.pdf.PdfReader;
import com.itextpdf.text.pdf.PdfStamper;
import com.itextpdf.text.pdf.PdfWriter;
import java.io.FileOutputStream;
import java.io.IOException;

/**
 *
 * @author kalyptus
 */
public class PDFPassword {
   public static void main(String[] args) throws IOException, DocumentException {
      String source;
      String dest;
      String user;
      String owner;

      if(args.length != 4){
         System.out.println("PDFPassword [source_path] [dest_path] [user_password] [owner_password]");
         System.exit(1);
      }
      
      source = args[0];
      dest = args[1];
      user = args[2];
      owner = args[3];
      
      /** User password */
      byte[] USER = user.getBytes();
      /** Owner password. */
      byte[] OWNER = owner.getBytes();      
      
      encrypt_pdf(source, dest, USER, OWNER);
      
      System.exit(0);
   }
   
   public static void encrypt_pdf(String src, String dest, byte[] user, byte[] owner) throws IOException, DocumentException {
       PdfReader reader = new PdfReader(src);
       PdfStamper stamper = new PdfStamper(reader, new FileOutputStream(dest));
       stamper.setEncryption(user, owner,
           PdfWriter.ALLOW_PRINTING, PdfWriter.ENCRYPTION_AES_128 | PdfWriter.DO_NOT_ENCRYPT_METADATA);
       stamper.close();
       reader.close();
   }   
}
