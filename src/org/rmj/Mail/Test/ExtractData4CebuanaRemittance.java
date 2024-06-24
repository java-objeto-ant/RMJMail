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
import org.apache.poi.EncryptedDocumentException;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.rmj.appdriver.SQLUtil;
import org.rmj.appdriver.agent.GRiderX;
import org.json.simple.JSONObject;
import org.rmj.appdriver.MiscUtil;
import org.rmj.lib.net.MiscReplUtil;

/**
 *
 * @author sayso
 */
public class ExtractData4CebuanaRemittance {
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

//            String lsSQL = "SELECT * FROM Branch";
//            ResultSet rs = instance.getConnection().createStatement().executeQuery(lsSQL);

//            while(rs.next()){
//                System.out.println(rs.getString("sBranchNm"));
//            }

            String file = "D:/GGC_Java_Systems/temp/Cebuana Remittances (2021.02.18).xlsx";
            Workbook wb = WorkbookFactory.create(new File(file)); // Or .xlsx
            Sheet s = wb.getSheet("sheet1");

            instance.beginTrans();
            for(int ctr1 = 1; s.getRow(ctr1) != null; ctr1++){
                Row r1 = s.getRow(ctr1);
                if(r1 != null){
                    Date date = r1.getCell(0).getDateCellValue();
                    String refer = r1.getCell(6).getStringCellValue();
                    String lname = r1.getCell(3).getStringCellValue();
                    String fname = "";
                    double amount = r1.getCell(4).getNumericCellValue();
                    String acctnm = r1.getCell(2).getStringCellValue();
                    String mobile = "";
                    String address= "";
                            
                    String sql;
                    
                    if(refer.length() > 0){
                        sql = "SELECT *" + 
                             " FROM XAPITrans" + 
                             " WHERE sReferNox = " + SQLUtil.toSQL(refer) + 
                             " ORDER BY sTransNox DESC";
                        ResultSet rs = instance.getConnection().createStatement().executeQuery(sql);
                        if(rs.next()){
                            if(rs.getString("cTranStat").equalsIgnoreCase("2") || rs.getString("cTranStat").equalsIgnoreCase("1")){
                                if(rs.getString("sAcctNmbr").equalsIgnoreCase(acctnm)){
                                    r1.createCell(6).setCellValue("existing + posted");
                                    System.out.println(refer + " is existing");
                                }
                                else{
                                    r1.createCell(6).setCellValue("diff acctnmbr posted");
                                }
                                continue;
                            }
                            else{
                                if(rs.getString("sAcctNmbr").equalsIgnoreCase(acctnm)){
                                    r1.createCell(6).setCellValue("existing");
                                }
                                else{
                                    r1.createCell(6).setCellValue("diff acctnmbr");
                                }
                                
                                sql = "UPDATE XAPITrans" + 
                                     " SET cTranStat = '3'" + 
                                     " WHERE sTransNox = " + SQLUtil.toSQL(rs.getString("sTransNox"));
                                instance.executeQuery(sql, "XAPITrans", "GAP1", "");
                                System.out.println(sql);
                            }
                        }
                    }
                    
                    if(acctnm.length() > 4){
                        sql = "SELECT a.sAcctNmbr, b.sCompnyNm, b.sMobileNo, CONCAT(b.sAddressx, ', ', c.sTownName, ', ', d.sProvName) xAddressx" +
                             " FROM MC_AR_Master a" +
                                " LEFT JOIN Client_Master b ON a.sClientID = b.sClientID" +
                                " LEFT JOIN TownCity c ON b.sTownIDxx = c.sTownIDxx" + 
                                " LEFT JOIN Province d ON c.sProvIDxx = d.sProvIDxx" +
                             " WHERE a.sAcctNmbr = " + SQLUtil.toSQL(acctnm);
                    }
                    else{
                        sql = "SELECT a.sAcctNmbr, b.sCompnyNm, b.sMobileNo, CONCAT(b.sAddressx, ', ', c.sTownName, ', ', d.sProvName) xAddressx" +
                             " FROM MC_AR_Master a" +
                                " LEFT JOIN Client_Master b ON a.sClientID = b.sClientID" +
                                " LEFT JOIN TownCity c ON b.sTownIDxx = c.sTownIDxx" + 
                                " LEFT JOIN Province d ON c.sProvIDxx = d.sProvIDxx" +
                             " WHERE b.sCompnyNm LIKE " + SQLUtil.toSQL(lname + ", " + fname + "%");
                    }
                    ResultSet rs = instance.getConnection().createStatement().executeQuery(sql);

                    if(rs.next()){
                        mobile = rs.getString("sMobileNo");
                        address = rs.getString("xAddressx");
                        acctnm = rs.getString("sAcctNmbr");
                    }
                    
                    JSONObject json = new JSONObject();
                    json.put("branch", refer.substring(2, 8));
                    json.put("referno", refer);
                    json.put("datetime", MiscReplUtil.format(date, SQLUtil.FORMAT_TIMESTAMP));
                    json.put("account", acctnm);
                    json.put("name", lname + ", " + fname);
                    json.put("address", address);
                    json.put("mobile", mobile);
                    json.put("amount", amount);
        
                String transno = MiscUtil.getNextCode("XAPITrans", "sTransNox", true, instance.getConnection(), instance.getBranchCode());

                sql = "INSERT INTO XAPITrans" + 
                         " SET sTransNox = " + SQLUtil.toSQL(transno) +  
                            ", sReferNox = " + SQLUtil.toSQL(refer) + 
                            ", sAcctNmbr = " + SQLUtil.toSQL(acctnm) + 
                            ", sPayloadx = '" + json.toJSONString() + "'" + 
                            ", cTranStat = '0'" + 
                            ", dReceived = " + SQLUtil.toSQL(Calendar.getInstance());
                instance.executeQuery(sql, "XAPITrans", "GAP1", "");
                System.out.println(sql);
                    
                }
           }
 
           FileOutputStream outputStream = new FileOutputStream(file.replace(".xlsx", "-update.xlsx")); 
           wb.write(outputStream);
            
           instance.commitTrans();
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
