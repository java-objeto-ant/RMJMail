/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.rmj.mail.Test;

import java.io.File;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import jxl.CellType;
import org.apache.poi.EncryptedDocumentException;
import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import static org.apache.poi.ss.formula.DataValidationEvaluator.isType;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.apache.poi.ss.util.CellReference;

/**
 *
 * @author kalyptus
 */
public class POIExcelTest {
   public static void main(String[] args){
       try {
           Workbook wb = WorkbookFactory.create(new File("D:/GGC_Java_Systems/temp/2020_07PromoRebate.xlsX")); // Or .xlsx
           Sheet s = wb.getSheet("Detailed");

           Row r1 = s.getRow(1);
           
           if(r1 != null){
               if(r1.getCell(1) != null){
                  System.out.println(r1.getCell(1).getStringCellValue());
               }
           }
       } catch (IOException ex) {
           System.out.print("IOException");
           ex.printStackTrace();
       } catch (EncryptedDocumentException ex) {
           System.out.print("EncryptedDocumentException");
           ex.printStackTrace();
       }
   }   
}
