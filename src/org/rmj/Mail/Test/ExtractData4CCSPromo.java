/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.rmj.mail.Test;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Calendar;
import java.util.Date;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.poi.EncryptedDocumentException;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.rmj.appdriver.MiscUtil;
import org.rmj.appdriver.SQLUtil;
import org.rmj.appdriver.agent.GRiderX;
import org.rmj.lib.net.MiscReplUtil;

/**
 *
 * @author sayso
 */
public class ExtractData4CCSPromo {
   private static GRiderX instance = null;
   public static void main(String[] args){
        String path;
        if(System.getProperty("os.name").toLowerCase().contains("win")){
            path = "D:/GGC_Java_Systems";
            System.setProperty("sys.default.path.temp", path + "/temp");
        }
        else{
            path = "/srv/GGC_Java_Systems";
            System.setProperty("sys.default.path.temp", path + "/temp");
        }
        System.setProperty("sys.default.path.config", path);
      
        instance = new GRiderX("gRider");
        instance.setOnline(true);
       
       try {

            String lsSQL = "SELECT * FROM Branch";
            ResultSet rs = instance.getConnection().createStatement().executeQuery(lsSQL);

//            while(rs.next()){
//                System.out.println(rs.getString("sBranchNm"));
//            }
            
            Workbook wb = WorkbookFactory.create(new File("D:/GGC_Java_Systems/temp/2020_07PromoRebate.xlsX")); // Or .xlsx
            Sheet s = wb.getSheet("Detailed");

            for(int ctr1 = 2; s.getRow(ctr1) != null; ctr1++){
                Row r1 = s.getRow(ctr1);
                if(r1 != null){
                    Date date = r1.getCell(1).getDateCellValue();
                    
                    //r1.getCell(1).setCellValue(clndr);
                    
                    if(!(r1.getCell(4) == null || r1.getCell(4).getStringCellValue().isEmpty())){
                       //System.out.println(r1.getCell(4).getStringCellValue() + ":" + MiscReplUtil.format(date, "YYYY-MM-d") + ":" + r1.getCell(5).getStringCellValue());
                       String sql = "INSERT INTO CCS_Promo_Detail" + 
                                   " SET sTransNox = 'M0W120000001'" +
                                      ", nEntryNox = " + SQLUtil.toSQL(ctr1 - 1) +
                                      ", sMainInfo = " + SQLUtil.toSQL(r1.getCell(4).getStringCellValue()) + 
                                      ", sOthrInfo = " + SQLUtil.toSQL(date) + 
                                      ", dModified = " + SQLUtil.toSQL(Calendar.getInstance());
                       instance.executeQuery(sql, "xxxTableAll", "M001", "");
                       System.out.println(sql);
                    }
                }
           }
           
           //FileOutputStream outputStream = new FileOutputStream("D:/GGC_Java_Systems/temp/2020_07PromoRebate.xlsX"); 
           //wb.write(outputStream);
           
       } catch (IOException ex) {
           System.out.print("IOException");
           ex.printStackTrace();
       } catch (EncryptedDocumentException ex) {
           System.out.print("EncryptedDocumentException");
           ex.printStackTrace();
       } catch (SQLException ex) {
           System.out.print("SQLException");
           ex.printStackTrace();
       }
   }   
    
}
