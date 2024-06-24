/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.rmj.mail.App;

import java.sql.SQLException;
import java.util.Calendar;
import java.util.Date;
import org.rmj.appdriver.SQLUtil;
import org.rmj.appdriver.agent.GRiderX;
import org.rmj.lib.net.LogWrapper;
import org.rmj.mail.lib.Payslip4Verification;

/**
 *
 * @author sayso
 */
public class CreatePayslip4Verfication {
    private static GRiderX instance = null;
    private static LogWrapper logwrapr;

    public static void main(String args[]) throws SQLException{
        String path;
        if(System.getProperty("os.name").toLowerCase().contains("win")){
            path = "D:/GGC_Java_Systems";
        }
        else{
            path = "/srv/GGC_Java_Systems";
        }
        System.setProperty("sys.default.path.temp", path + "/temp");
        System.setProperty("sys.default.path.config", path);

        //set the log file
        logwrapr = new LogWrapper("RMJMail.CreatePayslip4Verfication", System.getProperty("sys.default.path.temp") + "/CreatePayslip4Verfication.log");

        //initialize driver
        instance = new GRiderX("gRider");
        instance.setOnline(true);

        //get payroll period start date
        //+++++++++++++++++++++++++++++
        int day = java.time.LocalDate.now().getDayOfMonth();
        Date prev;
        Calendar cal = Calendar.getInstance();
        
        if(day <= 15){
            prev = SQLUtil.toDate(java.time.LocalDate.now().minusDays(day - 1).toString(), SQLUtil.FORMAT_SHORT_DATE);
            cal.set(Calendar.DAY_OF_MONTH, 16);
            
        }
        else{
            cal.set(Calendar.DAY_OF_MONTH, 1);
        }
        cal.set(Calendar.HOUR, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        //+++++++++++++++++++++++++++++

        Payslip4Verification oPay4Ver = new Payslip4Verification(instance, "1", cal.getTime());
        //oPay4Ver.CreateVerification();
        oPay4Ver.CreateReport();
        oPay4Ver.SendReport();
    }
}

