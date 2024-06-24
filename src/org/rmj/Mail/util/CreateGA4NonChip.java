/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.rmj.mail.util;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Calendar;
import java.util.Date;
import org.apache.poi.EncryptedDocumentException;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.apache.poi.xssf.usermodel.XSSFCell;
import org.rmj.appdriver.SQLUtil;
import org.rmj.appdriver.agent.GRiderX;
import org.json.simple.JSONObject;
import org.rmj.appdriver.MiscUtil;
import org.rmj.lib.net.MiscReplUtil;

/**
 *
 * @author sayso
 */
public class CreateGA4NonChip{
   private static GRiderX instance = null;
   public static void main(String[] args){
        String path;
        String file;
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
       
        if(args.length == 0){
            file = "D:/GGC_Java_Systems/temp/non-chip/M116.xlsx";
        }
        else{
            file = args[0];
        }
        
       try {

            Workbook wb = WorkbookFactory.create(new File(file)); // Or .xlsx
            Sheet s = wb.getSheet("sheet1");

            instance.beginTrans();
            for(int ctr1 = 1; s.getRow(ctr1) != null; ctr1++){
                Row r1 = s.getRow(ctr1);
                String cltnme = "";
                if(!(r1 == null || r1.getCell(0) == null || r1.getCell(0).getStringCellValue().equalsIgnoreCase("xxxx"))){
                    String branch = r1.getCell(0).getStringCellValue();
                    String drnoxx = "";
                    Date trdate = r1.getCell(4).getDateCellValue();

                    cltnme = r1.getCell(2).getStringCellValue();
                    
                    if (r1.getCell(3).getCellType() == CellType.STRING){
                        drnoxx = r1.getCell(3).getStringCellValue();
                    }
                    else{
                        drnoxx = Integer.toString((int) r1.getCell(3).getNumericCellValue());
                    }
                    
                    System.out.println("Processing: " + cltnme);
                    String sql = "SELECT a.sTransNox, a.dTransact, COUNT(*) nRecdCtrx" +
                                " FROM MC_SO_Master a" +
                                    " LEFT JOIN MC_SO_GiveAways b ON a.sTransNox = b.sTransNox" +
                                " WHERE a.sTransNox LIKE " + SQLUtil.toSQL(branch + "%") + 
                                  " AND a.sDRNOxxxx = " + SQLUtil.toSQL(drnoxx) + 
                                  " AND a.dTransact = " + SQLUtil.toSQL(SQLUtil.dateFormat(trdate, SQLUtil.FORMAT_TIMESTAMP)) + 
                                  " AND a.cTranStat <> '3'";
                    //System.out.println(sql);
                    ResultSet rs = instance.getConnection().createStatement().executeQuery(sql);
                    
                    rs.next();
                    
                    int ctr = rs.getInt("nRecdCtrx"); 
                    
                    r1.createCell(6).setCellValue(ctr);
                    
                    if(ctr > 0){
                        String sqlx = "SELECT a.sTransNox xTransNox, a.dTransact, b.*" +
                                     " FROM MC_SO_Master a" +
                                        " LEFT JOIN MC_SO_GiveAways b ON a.sTransNox = b.sTransNox AND b.sPartsIDx = 'GMO120000001'" +
                                     " WHERE a.sTransNox LIKE " + SQLUtil.toSQL(branch + "%") + 
                                       " AND a.sDRNOxxxx = " + SQLUtil.toSQL(drnoxx) + 
                                       " AND a.dTransact = " + SQLUtil.toSQL(SQLUtil.dateFormat(trdate, SQLUtil.FORMAT_TIMESTAMP)) + 
                                       " AND a.cTranStat <> '3'";
                        rs = instance.getConnection().createStatement().executeQuery(sqlx);
                        rs.next();

                        System.out.println(rs.getString("sTransNox"));
                        if(rs.getString("sTransNox") == null){
                            Calendar cal = Calendar.getInstance();
                            sql = "INSERT INTO MC_SO_GiveAways" +
                                 " SET sTransNox = " + SQLUtil.toSQL(rs.getString("xTransNox")) +
                                    ", nEntryNox = " + String.valueOf(ctr + 1) +
                                    ", sPartsIDx = 'GMO120000001'" +
                                    ", nQuantity = 1" +
                                    ", nGivenxxx = 0" +
                                    ", cGAwyStat = '3'" +
                                    ", dModified = " + SQLUtil.toSQL(cal);
                            instance.executeQuery(sql, "MC_SO_GiveAways", branch, "");
                            System.out.println(sql);
                        }
                        else{
                            r1.getCell(6).setCellValue(-1);
                        }
                    }
                }
            
                System.out.println(cltnme);
            }
            
           FileOutputStream outputStream = new FileOutputStream(file.replace(".xlsx", "-update.xlsx")); 
           wb.write(outputStream);
            
            instance.commitTrans();
       } catch (IOException ex) {
           instance.commitTrans();
       } catch (EncryptedDocumentException ex) {
           instance.rollbackTrans();
           ex.printStackTrace();
       } catch (SQLException ex) {
           instance.rollbackTrans();
           ex.printStackTrace();
       }
   }   
    
}
