/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.rmj.mail.lib;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Date;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.rmj.appdriver.GRider;
import org.rmj.appdriver.SQLUtil;

/**
 * @author sayso
 * 
 * Returns an Array of JSon Data for the creation of Payslip report... 
 */
public class JsonDataSourcePayslip {
    private GRider poRider;
    private Date pdPeriod;
    private String psFilter;
            
    public JsonDataSourcePayslip(GRider rider, Date period, String filter){
        this.poRider = rider;
        this.pdPeriod = period;
        this.psFilter = filter;
    }
    
    public JSONArray CreateJson(){
        String lsSQL;
        lsSQL = getMainQuery(this.pdPeriod, this.psFilter);
        System.out.println(lsSQL);
        
        ResultSet loRS = this.poRider.executeQuery(lsSQL);
        JSONArray xrows = new JSONArray();

        try {
            
            while(loRS.next()){
                System.out.println("Processing " + loRS.getString("sBranchNm") + ": "+ loRS.getString("sEmployNm"));
                JSONObject xrow = new JSONObject();
                xrow.put("sField01", loRS.getString("sEmployNm") + (loRS.getString("cPostedxx").equalsIgnoreCase("2") ? "" : "»NOT POSTED"));
                xrow.put("sField02", SQLUtil.dateFormat(loRS.getDate("dPeriodFr"), "MMM dd, yyyy") + " - " + SQLUtil.dateFormat(loRS.getDate("dPeriodTo"), "MMM dd, yyyy"));
                xrow.put("sField03", loRS.getString("sBranchNm") + " - " + loRS.getString("sDeptName"));
                xrow.put("sField04", loRS.getString("sPositnNm"));
                xrow.put("sField21", SQLUtil.dateFormat(loRS.getDate("dCovergFm"), "MMM dd, yyyy") + " - " + SQLUtil.dateFormat(loRS.getDate("dCovergTo"), "MMM dd, yyyy"));
                
                xrow.put("nField01", loRS.getDouble("nLveCredt"));
                xrow.put("nField02", loRS.getDouble("nAttendnc"));

                xrow.put("lField01", loRS.getDouble("nBasicSal"));
                
                //get daily pay
                double lnDailyPay;
                //is employee an probationary and not a monarch employee
                if(loRS.getString("sEmpTypID").equalsIgnoreCase("T") && !loRS.getString("cDivision").equalsIgnoreCase("3")){
                    lnDailyPay = (double) Math.round(loRS.getDouble("nBasicSal") / (loRS.getDouble("nAttendnc") + loRS.getDouble("nAbsencex")) * 100) / 100;
                }    
                else{
                    lnDailyPay = (double) Math.round(loRS.getDouble("nBasicSal") / 13 * 100) / 100;
                }
                
                xrow.put("lField02", loRS.getDouble("nOvertime"));
                xrow.put("lField03", loRS.getDouble("nHolidayx"));
                xrow.put("lField07", loRS.getDouble("nTardines"));
                xrow.put("lField08", loRS.getDouble("nUndrTime"));
                xrow.put("lField09", (double) Math.round(loRS.getDouble("nAbsencex") * lnDailyPay * 100) / 100);
         
                //System.out.println(loRS.getDouble("nTardines"));
                
                if(!ExtractBenefits(loRS.getString("sPayPerID"), loRS.getString("sEmployID"), xrow)){
                    System.exit(0);
                }
                
                if(!ExtractGovtDeductions(loRS.getString("sPayPerID"), loRS.getString("sEmployID"), xrow)){
                    System.exit(0);
                }
                
                if(!ExtractDeductions(loRS.getString("sPayPerID"), loRS.getString("sEmployID"), loRS.getString("sBranchCD"), xrow)){
                    System.exit(0);
                }
                
                if(!ExtractLoans(loRS.getString("sPayPerID"), loRS.getString("sEmployID"), loRS.getString("sBranchCD"), xrow)){
                    System.exit(0);
                }
                
                if(!ExtractAdjustments(loRS.getString("sPayPerID"), loRS.getString("sEmployID"), xrow, loRS)){
                    System.exit(0);
                }
                
                xrow.put("sPayPerID", loRS.getString("sPayPerID"));
                xrow.put("sEmployID", loRS.getString("sEmployID"));
                xrow.put("sPassword", loRS.getString("sPassword"));
                
                xrows.add(xrow);
            }
            return xrows;
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
        
        return null;
    }

    private boolean ExtractBenefits(String fsPeriodID, String fsEmployID, JSONObject row){
        String lsSQL;
        
        lsSQL = "SELECT" +
                       "  IFNULL(b.sBeneftNm, '') sBeneftNm" +
                       ", IFNULL(CAST(REVERSE(DECODE(UNHEX(a.nAmountxx), 'PETMGR')) AS DECIMAL), 0) nAmountxx" +
               " FROM Payroll_Benefits a" +
                    " LEFT JOIN Benefit b ON a.sBeneftID = b.sBeneftID" +
               " WHERE a.sPayPerID = " + SQLUtil.toSQL(fsPeriodID) +
                 " AND a.sEmployID = " + SQLUtil.toSQL(fsEmployID);
        ResultSet loRS = this.poRider.executeQuery(lsSQL);
        
        //set label name
        row.put("sField05", "");
        row.put("sField06", "");
        row.put("sField07", "");

        row.put("lField04", 0.00);
        row.put("lField05", 0.00);
        row.put("lField06", 0.00);
        
        try {
            int ctr=0;
            double other=0;
            double last=0;
            while(loRS.next()){
                if(ctr <= 2){
                    row.put("sField" + String.format("%02d", ctr+5), loRS.getString("sBeneftNm").toUpperCase());
                    row.put("lField" + String.format("%02d", ctr+4), loRS.getDouble("nAmountxx"));
                    last = loRS.getDouble("nAmountxx");
                }
                else{
                    other += loRS.getDouble("nAmountxx");
                }
                ctr++;
            }
            
            if(other > 0){
                row.put("sField07", "MISC BENFTS");
                row.put("lField06", other + last);
            }
            
            return true;
            
        } catch (SQLException ex) {
            ex.printStackTrace();
            return false;
        }
    }
            
    private boolean ExtractGovtDeductions(String fsPeriodID, String fsEmployID, JSONObject row){
        String lsSQL;
        lsSQL = "SELECT" +
                       "  a.sDeductID" +
                       ", c.sDeductNm" +
                       ", a.nAmountxx" +
                       ", a.cSubsidzd" +
               " FROM Payroll_Govt_Deductions a" +
                    " LEFT JOIN Deduction c ON a.sDeductID = c.sDeductID" +
               " WHERE a.sPayPerID = " + SQLUtil.toSQL(fsPeriodID) +
                 " AND a.sEmployID = " + SQLUtil.toSQL(fsEmployID);
        ResultSet loRS = this.poRider.executeQuery(lsSQL);
        
        //set label name
        row.put("sField08", "");

        row.put("lField10", 0.00);
        row.put("lField11", 0.00);
        row.put("lField12", 0.00);
        row.put("lField13", 0.00);

        try {
            while(loRS.next()){
                if(loRS.getString("cSubsidzd").equalsIgnoreCase("0")){
                    //Is SSS?
                    if(loRS.getString("sDeductID").equalsIgnoreCase("11003")){
                        row.put("lField10", loRS.getDouble("nAmountxx"));
                    }
                    //Is HDMF
                    else if(loRS.getString("sDeductID").equalsIgnoreCase("11002")){
                        row.put("lField11", loRS.getDouble("nAmountxx"));
                    }
                    //Is PHILHEALTH
                    else if(loRS.getString("sDeductID").equalsIgnoreCase("11001")){
                        row.put("lField12", loRS.getDouble("nAmountxx"));
                    }
                    //Is WTAX
                    else if(loRS.getString("sDeductID").equalsIgnoreCase("11010")){
                        row.put("lField13", loRS.getDouble("nAmountxx"));
                        row.put("sField08", "WTAX");
                    }
                }
            }
            
            return true;
            
        } catch (SQLException ex) {
            ex.printStackTrace();
            return false;
        }
    }
    
    private boolean ExtractDeductions(String fsPeriodID, String fsEmployID, String fsBranchCD, JSONObject row){
        String lsSQL;
        lsSQL = "SELECT" +
                       "  IFNULL(c.sDeductNm, '') sDeductNm" +
                       ", IFNULL(a.nAmountxx, 0) nAmountxx" +
                       ", c.cOnPayslp" +
               " FROM Payroll_Deductions a" +
                    " LEFT JOIN Employee_Deduction b ON a.sReferNox = b.sTransNox" +
                    " LEFT JOIN Deduction c ON b.sDeductID = c.sDeductID" +
               " WHERE a.sPayPerID = " + SQLUtil.toSQL(fsPeriodID) +
                 " AND a.sEmployID = " + SQLUtil.toSQL(fsEmployID);

        ResultSet loRS = this.poRider.executeQuery(lsSQL);

        int ctr;
        if((Double)row.get("lField13") == 0){
            ctr = 8;
        }
        else{
            ctr = 9;
        }
        
        //set label name
        row.put("sField09", "");
        row.put("sField10", "");

        row.put("lField14", 0.00);
        row.put("lField15", 0.00);
        
        try {
            double other=0;
            double last=0;
            while(loRS.next()){
                //do not include deductions that are tagged as not payslip 
                if(fsBranchCD.equalsIgnoreCase("M114")){
                    if(loRS.getString("cOnPayslp").equalsIgnoreCase("1")){
                        if(ctr <= 10){
                            row.put("sField" + String.format("%02d", ctr), loRS.getString("sDeductNm").toUpperCase());
                            row.put("lField" + String.format("%02d", ctr+5), loRS.getDouble("nAmountxx"));
                            last = loRS.getDouble("nAmountxx");
                        }
                        else{
                            other += loRS.getDouble("nAmountxx");
                        }
                        ctr++;
                    }
                }
                else{
                    if(ctr <= 10){
                        row.put("sField" + String.format("%02d", ctr), loRS.getString("sDeductNm").toUpperCase());
                        row.put("lField" + String.format("%02d", ctr+5), loRS.getDouble("nAmountxx"));
                        last = loRS.getDouble("nAmountxx");
                    }
                    else{
                        other += loRS.getDouble("nAmountxx");
                    }
                    ctr++;
                }
            }
            
            if(other > 0){
                row.put("sField10", "MISC DED(s)");
                row.put("lField15", other + last);
            }
            
            return true;
            
        } catch (SQLException ex) {
            ex.printStackTrace();
            return false;
        }
    }

    private boolean ExtractLoans(String fsPeriodID, String fsEmployID, String fsBranchCD, JSONObject row){
        String lsSQL;
        lsSQL = "SELECT" +
                       "  a.sReferNox" +
                       ", c.sLoanName" +
                       ", a.nAmountxx" +
                       ", c.cOnPayslp" +
               " FROM Payroll_Loans a" +
                   ", Employee_Loan_Master b" +
                   ", Loans c" +
               " WHERE a.sReferNox = b.sTransNox" +
                 " AND b.sLoanIDxx = c.sLoanIDxx" +
                 " AND a.sPayPerID = " + SQLUtil.toSQL(fsPeriodID) +
                 " AND a.sEmployID = " + SQLUtil.toSQL(fsEmployID) +
                 " AND a.cIntegSys = '0'" + 
               " UNION " +
               "SELECT" +
                       "  a.sReferNox" +
                       ", CASE b.cLoanType WHEN '0' THEN 'MC Sales' WHEN '1' THEN 'Sidecar' ELSE 'Others' END sLoanName" +
                       ", a.nAmountxx" +
                       ", '0' cOnPayslp" +
               " FROM Payroll_Loans a" +
                   ", MC_AR_Master b" +
               " WHERE a.sReferNox = b.sAcctNmbr" +
                 " AND a.sPayPerID = " + SQLUtil.toSQL(fsPeriodID) +
                 " AND a.sEmployID = " + SQLUtil.toSQL(fsEmployID) +
                 " AND a.cIntegSys = '1'";
        ResultSet loRS = this.poRider.executeQuery(lsSQL);
        
        //set label name
        row.put("sField11", "");
        row.put("sField12", "");
        row.put("sField13", "");
        row.put("sField14", "");
        row.put("sField15", "");

        row.put("lField16", 0.00);
        row.put("lField17", 0.00);
        row.put("lField18", 0.00);
        row.put("lField19", 0.00);
        row.put("lField20", 0.00);
        
        try {
            int ctr=0;
            double other=0;
            double last=0;
            while(loRS.next()){
                if(fsBranchCD.equalsIgnoreCase("M114")){
                    if(loRS.getString("cOnPayslp").equalsIgnoreCase("1")){
                        if(ctr <= 4){
                            row.put("sField" + String.format("%02d", ctr+11), loRS.getString("sLoanName").toUpperCase());
                            row.put("lField" + String.format("%02d", ctr+16), loRS.getDouble("nAmountxx"));
                            last = loRS.getDouble("nAmountxx");
                        }
                        else{
                            other += loRS.getDouble("nAmountxx");
                        }
                        ctr++;
                    }
                }else{
                    if(ctr <= 4){
                        row.put("sField" + String.format("%02d", ctr+11), loRS.getString("sLoanName").toUpperCase());
                        row.put("lField" + String.format("%02d", ctr+16), loRS.getDouble("nAmountxx"));
                        last = loRS.getDouble("nAmountxx");
                    }
                    else{
                        other += loRS.getDouble("nAmountxx");
                    }
                    ctr++;
                }    
            }
            
            if(other > 0){
                row.put("sField15", "MISC LOANS");
                row.put("lField20", other + last);
            }
            
            return true;
            
        } catch (SQLException ex) {
            ex.printStackTrace();
            return false;
        }
    }

    private boolean ExtractAdjustments(String fsPeriodID, String fsEmployID, JSONObject row, ResultSet source){
        double lnAdjBasic;
        double lnAdjBenft;
        double lnOtherAdj;
        double lnMiscxAdj;

        row.put("sField16", "");
        row.put("sField17", "");
        row.put("sField18", "");
        row.put("sField19", "");
        row.put("sField20", "");
        
        row.put("lField21", 0.00);
        row.put("lField22", 0.00);
        row.put("lField23", 0.00);
        row.put("lField24", 0.00);
        row.put("lField25", 0.00);
        
        
        try {
            //get daily benefits/pay of employee from previous payroll period
            //these will be use in the computing of possible adjustments of employee
            lnAdjBasic = source.getDouble("nAdjBasic");
            lnAdjBenft = source.getDouble("nAdjBenft");

            //Check if employee has salary adjustments
            lnMiscxAdj = ExtractOtherAdj(fsPeriodID, fsEmployID, "SalA");
            
            int ctr=0;
            
            //check if there are possible SALARY/HOLIDAY ADJUSTMENTS
            if(lnAdjBenft > 0 || !(lnMiscxAdj == 0 && source.getDouble("nAdjPremx") == 0 && source.getDouble("nTmePremx") == 0)){
                row.put("sField" + String.format("%02d", ctr+16), "SLRY/HLDY");
                row.put("lField" + String.format("%02d", ctr+21), source.getDouble("nAdjPremx") + source.getDouble("nTmePremx") + source.getDouble("nAdjHldyx") + lnMiscxAdj);
                ctr++;
            }

            //Check if employee has benefit adjustments
            lnMiscxAdj = ExtractOtherAdj(fsPeriodID, fsEmployID, "BenA");
            if(lnAdjBenft > 0 || lnMiscxAdj != 0){
                row.put("sField" + String.format("%02d", ctr+16), "BENEFITS");
                //row.put("lField" + String.format("%02d", ctr+21), lnAdjBenft + lnMiscxAdj);
                row.put("lField" + String.format("%02d", ctr+21), lnMiscxAdj);
                ctr++;
            }

            //Check if employee has overtime adjustments
            if(source.getDouble("nAdjOverT") != 0){
                row.put("sField" + String.format("%02d", ctr+16), "OVERTIME");
                row.put("lField" + String.format("%02d", ctr+21), source.getDouble("nAdjOverT"));
                ctr++;
            }

            //check if employee has attendance adjustments
            lnMiscxAdj = ExtractOtherAdj(fsPeriodID, fsEmployID, "Atnd");
            if(source.getDouble("nAdjAttnd") - source.getDouble("nAdjAbsnt") != 0 || lnMiscxAdj != 0){
                row.put("sField" + String.format("%02d", ctr+16), "ATTENDANCE");
                row.put("lField" + String.format("%02d", ctr+21), lnMiscxAdj + (lnAdjBasic * (source.getDouble("nAdjAttnd") - source.getDouble("nAdjAbsnt"))));
                ctr++;
            }

            lnOtherAdj = 0;
            
            //check if employee has tardiness adjustments
            if(source.getDouble("nAdjTardi") != 0){
                row.put("sField" + String.format("%02d", ctr+16), "ATTENDANCE");
                row.put("lField" + String.format("%02d", ctr+21), source.getDouble("nAdjTardi"));
                if(ctr >= 4){
                    lnOtherAdj += source.getDouble("nAdjTardi");
                }
                ctr++;
            }

            //check if employee has undertime adjustments
            if(source.getDouble("nAdjUndrT") != 0){
                int xctr = ctr <= 4 ? ctr : 4;
                row.put("sField" + String.format("%02d", xctr+16), "UNDERTIME");
                row.put("lField" + String.format("%02d", xctr+21), source.getDouble("nAdjUndrT"));
                if(ctr >= 4){
                    lnOtherAdj += source.getDouble("nAdjUndrT");
                }
                ctr++;
            }
            
            //check if employee has NIGHT DIFFERENTIAL
            lnMiscxAdj = source.getDouble("nNightDif") + 
                         source.getDouble("nAdjNghtD") + 
                         source.getDouble("nOTNightD") + 
                         source.getDouble("nAdjOTNht");
            if(lnMiscxAdj != 0){
                int xctr = ctr <= 4 ? ctr : 4;
                row.put("sField" + String.format("%02d", xctr+16), "NIGHT DIFF");
                row.put("lField" + String.format("%02d", xctr+21), lnMiscxAdj);
                if(ctr >= 4){
                    lnOtherAdj += lnMiscxAdj;
                }
                ctr++;
            }
            
            //check for a manual adjustment
            lnMiscxAdj = ExtractOtherAdj(fsPeriodID, fsEmployID, "PyAd");
            if(lnMiscxAdj != 0){
                int xctr = ctr <= 4 ? ctr : 4;
                row.put("sField" + String.format("%02d", xctr+16), "OTHERS");
                row.put("lField" + String.format("%02d", xctr+21), lnMiscxAdj);
                if(ctr >= 4){
                    lnOtherAdj += lnMiscxAdj;
                }
                ctr++;
            }

            //if number of adjustments is more than 5, then all adjustments from 5 up 
            //is transferred to MISC ADJ
            if(ctr > 4){
                ctr = 4;
                row.put("sField" + String.format("%02d", ctr+16), "MISC ADJ");
                row.put("lField" + String.format("%02d", ctr+21), lnOtherAdj);
            }
            
            return true;
        } catch (SQLException ex) {
            ex.printStackTrace();
            return false;
        }
    }
    
    private double ExtractOtherAdj(String fsPeriodID, String fsEmployID, String fsSourceCD){
        String lsSQL;
        lsSQL = "SELECT SUM(nAmountxx) nAmountxx" +
               " FROM Payroll_Adjustments" +
               " WHERE sPayPerID = " + SQLUtil.toSQL(fsPeriodID) +
                 " AND sEmployID = " + SQLUtil.toSQL(fsEmployID) +
                 " AND sSourceCD = " + SQLUtil.toSQL(fsSourceCD);
        ResultSet loRS = this.poRider.executeQuery(lsSQL);
        
        try {
            double sum=0;
            if(loRS.next()){
                sum = loRS.getDouble("nAmountxx");
            }
            return sum;
        } catch (SQLException ex) {
            ex.printStackTrace();
            return 0;
        }
    }
    
    private String getMainQuery(Date period, String xfilter){
        String lsSQL;
        lsSQL = "SELECT DISTINCT" +
                    "  a.sPayPerID" +
                    ", b.sEmployID" +
                    ", a.sEmpTypID" +
                    ", a.dPeriodFr" +
                    ", a.dPeriodTo" +
                    ", a.dCovergFm" +
                    ", a.dCovergTo" +
                    ", CONCAT(g.sLastName, ', ', g.sFrstName, IF(TRIM(IFNULL(g.sSuffixNm, '')) = '', ' ', CONCAT(' ', g.sSuffixNm, ' ')), g.sMiddName) sEmployNm" +
                    ", d.sBranchNm" +
                    ", f.sDeptName" +
                    ", e.sPositnNm" +
                    ", (IFNULL(h.nNoDaysxx, 0) - IFNULL(h.nCredtUse, 0)) nLveCredt" +
                    ", CAST(REVERSE(DECODE(UNHEX(b.nBasicSal), 'PETMGR')) AS CHAR) nBasicSal" +
                    ", CAST(REVERSE(DECODE(UNHEX(b.nOverTime), 'PETMGR')) AS CHAR) nOverTime" +
                    ", CAST(REVERSE(DECODE(UNHEX(b.nHolidayx), 'PETMGR')) AS CHAR) nHolidayx" +
                    ", CAST(REVERSE(DECODE(UNHEX(b.nAdjHldyx), 'PETMGR')) AS CHAR) nAdjHldyx" +
                    ", b.nAbsencex" +
                    ", CAST(REVERSE(DECODE(UNHEX(b.nTardines), 'PETMGR')) AS CHAR) nTardines" +
                    ", CAST(REVERSE(DECODE(UNHEX(b.nUndrTime), 'PETMGR')) AS CHAR) nUndrTime" +
                    ", CAST(REVERSE(DECODE(UNHEX(b.nNightDif), 'PETMGR')) AS CHAR) nNightDif" +
                    ", CAST(REVERSE(DECODE(UNHEX(b.nAdjNghtD), 'PETMGR')) AS CHAR) nAdjNghtD" +
                    ", CAST(REVERSE(DECODE(UNHEX(b.nOTNightD), 'PETMGR')) AS CHAR) nOTNightD" +
                    ", CAST(REVERSE(DECODE(UNHEX(b.nAdjOTNht), 'PETMGR')) AS CHAR) nAdjOTNht" +
                    ", CAST(REVERSE(DECODE(UNHEX(b.nAdjBasic), 'PETMGR')) AS CHAR) nAdjBasic" +
                    ", CAST(REVERSE(DECODE(UNHEX(b.nAdjBenft), 'PETMGR')) AS CHAR) nAdjBenft" +
                    ", b.nAdjAttnd" +
                    ", b.nAdjAbsnt" +
                    ", CAST(REVERSE(DECODE(UNHEX(b.nAdjOverT), 'PETMGR')) AS CHAR) nAdjOverT" +
                    ", CAST(REVERSE(DECODE(UNHEX(b.nAdjTardi), 'PETMGR')) AS CHAR) nAdjTardi" +
                    ", CAST(REVERSE(DECODE(UNHEX(b.nAdjUndrT), 'PETMGR')) AS CHAR) nAdjUndrT" +
                    ", CAST(REVERSE(DECODE(UNHEX(b.nAdjPremx), 'PETMGR')) AS CHAR) nAdjPremx" +
                    ", CAST(REVERSE(DECODE(UNHEX(b.nTmePremx), 'PETMGR')) AS CHAR) nTmePremx" +
                    ", b.nAttendnc" +
                    ", b.nAbsencex" +
                    ", a.cPostedxx" +
                    ", c.sPassword" +
                    ", i.cDivision" +
                    ", i.sPyClustr" +
                    ", b.sBranchCD" +
                " FROM Payroll_Period a" +
                     " LEFT JOIN Payroll_Summary_New b ON a.sPayPerID = b.sPayPerID" +
                     " LEFT JOIN Employee_Master001 c ON b.sEmployID = c.sEmployID" +
                     " LEFT JOIN Branch d ON b.sBranchCD = d.sBranchCD" +
                     " LEFT JOIN Position e ON c.sPositnID = e.sPositnID" +
                     " LEFT JOIN Department f ON c.sDeptIDxx = f.sDeptIDxx" +
                     " LEFT JOIN Client_Master g ON c.sEmployID = g.sClientID" +
                     " LEFT JOIN Employee_Leave_Credit h ON c.sEmployID = h.sEmployID AND h.cLeaveTyp = '0' AND a.dPeriodFr BETWEEN h.dValdFrom AND h.dValdThru AND (IFNULL(h.nNoDaysxx, 0) - IFNULL(h.nCredtUse, 0) > 0)" +
                     " LEFT JOIN Branch_Others i on b.sBranchCD = i.sBranchCD" +
                     " LEFT JOIN App_User_Master j ON  c.sEmployID = j.sEmployNo and j.cActivatd = '1'" +
                " WHERE a.sEmpTypID IN ('R', 'T')" +
                  " AND a.cPostedxx = '2'" +
                  " AND a.dPeriodFr = " + SQLUtil.toSQL(period) +
                     (xfilter.isEmpty() ? "" : " AND " + xfilter)  +
                  " AND b.cMailSent = '0'" +
                " ORDER BY d.sBranchNm, f.sDeptName, g.sLastName, g.sFrstName, g.sSuffixNm, g.sMiddName";
        
        System.out.println(lsSQL);
        
        return lsSQL;
    }
}
