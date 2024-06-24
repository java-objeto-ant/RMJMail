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
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.poi.EncryptedDocumentException;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.rmj.appdriver.SQLUtil;
import org.rmj.appdriver.agent.GRiderX;

/**
 *
 * @author sayso
 */
public class UpdateCRLocationThruRelease {
   private static GRiderX instance = null;
   public static void main(String[] args){
        String path;
        String branch;
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
        //instance.setOnline(true);
       
        branch = instance.getBranchCode();

//        if(args.length == 0){
//            branch = "M001";
//        }
//        else{
//            branch = args[0];
//        }
        
        String sql = "SELECT a.*, b.*, c.sTransNox xTransNox, c.sLocatnCR, d.sSerialID, d.sLocatnCR xLocatnCR" +
                    " FROM MC_Reg_Release_Master a" +
                        " LEFT JOIN MC_Reg_Release_Detail b ON a.sTransNox = b.sTransNox" +
                        " LEFT JOIN MC_Registration c ON a.sReferNox = c.sTransNox" +
                        " LEFT JOIN MC_Serial_Registration d ON c.sSerialID = d.sSerialID" +
                    " WHERE a.sTransNox LIKE " + SQLUtil.toSQL(branch + "%") + 
                      " AND b.sDocuIDxx = 'M001130001'" +
                      " AND b.cOriginal = '1'" +
                      " AND NOT (c.sLocatnCR = 'CLT' AND d.sLocatnCR = 'CLT')";
       try {
           ResultSet rs = instance.getConnection().createStatement().executeQuery(sql);
           instance.beginTrans();
           //int ctr;
           while(rs.next()){
               if(!rs.getString("sLocatnCR").equalsIgnoreCase("clt")){
                  sql = "UPDATE MC_Registration" + 
                       " SET sLocatnCR = 'CLT'" + 
                       " WHERE sTransNox = " + SQLUtil.toSQL(rs.getString("xTransNox"));
                  instance.executeQuery(sql, "MC_Registration", branch, "");
                  //System.out.println(Integer.toString(ctr) + ":" + sql);
                  System.out.println(sql);
               }
               if(!rs.getString("xLocatnCR").equalsIgnoreCase("clt")){
                  sql = "UPDATE MC_Serial_Registration" + 
                       " SET sLocatnCR = 'CLT'" + 
                       " WHERE sSerialID = " + SQLUtil.toSQL(rs.getString("sSerialID"));
                  instance.executeQuery(sql, "MC_Serial_Registration", branch, "");
                  System.out.println(sql);
               }
           }
           instance.commitTrans();
       } catch (SQLException ex) {
           ex.printStackTrace();
       }
   }   
}
