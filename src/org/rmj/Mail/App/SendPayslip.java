/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.rmj.mail.App;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;
import javax.mail.MessagingException;
import org.rmj.appdriver.SQLUtil;
import org.rmj.lib.mail.SendMail;
import org.rmj.lib.net.LogWrapper;
import org.rmj.appdriver.agent.GRiderX;
import org.rmj.lib.mail.MessageInfo;
import org.rmj.appdriver.agent.MsgBox;
import javax.net.ssl.SSLSocketFactory;
import org.apache.commons.codec.binary.Base64;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.rmj.lib.net.WebClient;

/**
 *
 * @author kalyptus
 */
public class SendPayslip {
   private static final String jar_path = new File(SendPayslip.class.getProtectionDomain().getCodeSource().getLocation().getPath()).getParentFile().getPath().replace("%20", " "); 
   private static GRiderX instance = null;
   private static SendMail pomail;
   private static final LogWrapper logwrapr = new LogWrapper("RMJMail.SendPaySlip", "temp/SendPaySlip.log");
//   private static final LogWrapper logwrapr = new LogWrapper("RMJMail.SendPaySlip", "D:/GGC_Java_Systems/temp/SendPaySlip.log");

   public static void main(String[] args) {
        String sender;

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
      
      ResultSet rsToSend;
   
      //set guanzon SMTP configuration as default
      if(args.length == 0)
         sender = "GMail";
      else
         sender = args[0];

      //load the configuration
      pomail = new SendMail(path, sender);
         
      //try to connect to the SMTP server
      if(pomail.connect(true)){
      
         //extract records to send
         rsToSend = extract2send(sender);
         
         //try to send email
         try {
            while(rsToSend.next()){
               try {
  
                   String lsEmail;
                   if(!rsToSend.getString("sEmailAdd").contains("gmail")){
                       lsEmail = rsToSend.getString("sEmailAdd");
                   }
                   else if(!rsToSend.getString("sEmpLevID").equalsIgnoreCase("0")  ){
                       lsEmail = rsToSend.getString("sEmailAdd");
                   }
                   else if("01".contains(rsToSend.getString("cDivision"))){
                       if(rsToSend.getString("cMainOffc").contains("1")){
                           if(rsToSend.getString("sDeptIDxx").equalsIgnoreCase("015")){
                                lsEmail = rsToSend.getString("sBranchMl");
                           }
                           else{
                                lsEmail = rsToSend.getString("sDeptMail");
                           }
                       }   
                       else{
                           lsEmail = rsToSend.getString("sBranchMl");
                       }
                   }
                   else{
                       lsEmail = rsToSend.getString("sEmailAdd");
                   }
                   
//                   if(!rsToSend.getString("sEmailAdd").contains("gmail")){
//                       lsEmail = rsToSend.getString("sEmailAdd");
//                   }
//                   else{
//                       if(rsToSend.getString("cMainOffc").contains("1")){
//                           if(rsToSend.getString("sBranchCD").equalsIgnoreCase("M029") && rsToSend.getString("sDeptIDxx").equalsIgnoreCase("015")){
//                                lsEmail = rsToSend.getString("sBranchMl");
//                           }
//                           else{
//                                lsEmail = rsToSend.getString("sDeptMail");
//                           }
//                       }   
//                       else{
//                           lsEmail = rsToSend.getString("sBranchMl");
//                       }
//                   }
                   
                   //byte email[] = rsToSend.getString("sEmailAdd").getBytes("UTF-8");
                  
                  //System.out.println("utf8 " + java.net.IDN.toASCII(new String(encode(email))));
                  //System.out.println("utf8 " + java.net.IDN.toUnicode(new String(encode(email))));
                  //System.out.println("latin1 " + new String(email, "windows-1252"));
                  //System.out.println("latin1 " + new String(email, "ISO-8859-1"));
                  
                  if(!lsEmail.isEmpty()){
                    MessageInfo msginfo = new MessageInfo();

                    //msginfo.addTo(java.net.IDN.toUnicode(new String(encode(email))));
                    //msginfo.addTo(rsToSend.getString("sEmailAdd"));
                    msginfo.addTo(lsEmail);
                    msginfo.setSubject("PAYSLIP (" + rsToSend.getString("dPeriodFr") + " - " + rsToSend.getString("dPeriodTo") + ")");
                    msginfo.setBody(rsToSend.getString("sEmployNm") + ".\nPlease download and delete your payslip.");
                    msginfo.addAttachment(System.getProperty("sys.default.path.temp") + "/payslip/" + rsToSend.getString("sEmployID") + rsToSend.getString("sPayPerID") + ".pdf");
                    //kalyptus - 2017.05.19 01:44pm
                    //set the no reply account as the sender instead of the petmgr
                    msginfo.setFrom("No Reply <no-reply@guanzongroup.com.ph>");
                    pomail.sendMessage(msginfo);

//                  pomail.initmsg();
//                  // pomail.setFrom("NO REPLY <petmgr@guanzongroup.com.ph>");
//                  //pomail.setRecipients(Message.RecipientType.CC, "stargaze_75@yahoo.com");
//                  pomail.setRecipients(Message.RecipientType.TO, rsToSend.getString("sEmailAdd"));               
//                  pomail.setSubject("PAYSLIP (" + rsToSend.getString("dPeriodFr") + " - " + rsToSend.getString("dPeriodTo") + ")");
//                  pomail.setBody(rsToSend.getString("sEmployNm"));
//                  pomail.addAttachment("C:/GGC_Systems/Temp/" + rsToSend.getString("sEmployID") + rsToSend.getString("sPayPerID") + ".pdf");
//                  pomail.sendMessage();
                  
                  }
                  
                  if(!rsToSend.getString("sProdctID").isEmpty()){
                      String message;
                      message = "Good day! \n\n Attached is your payslip for the payroll period " + rsToSend.getString("dPeriodFr") + " - " + rsToSend.getString("dPeriodTo");
                      message += ".\n\n [http://gts1.guanzongroup.com.ph:2007/repl/misc/download_ps.php?period="+ toBase64(rsToSend.getString("sPayPerID")) + "&client=" + toBase64(rsToSend.getString("sEmployID")) + "]";
                      sendNotification( 
                          rsToSend.getString("sProdctID"), 
                          rsToSend.getString("sUserIDxx"), 
                          "PAYSLIP (" + rsToSend.getString("dPeriodFr") + " - " + rsToSend.getString("dPeriodTo") + ")", 
                          message);
                  }
                  
                  
                  String lsSQL;
                  lsSQL = "UPDATE Payroll_Summary_New" +
                         " SET cMailSent = '2'" +
                         " WHERE sPayPerID = " + SQLUtil.toSQL(rsToSend.getString("sPayPerID")) +
                           " AND sEmployID = " + SQLUtil.toSQL(rsToSend.getString("sEmployID"));
                  instance.getConnection().createStatement().executeUpdate(lsSQL);
                  System.out.println(Calendar.getInstance().getTime());
               }   
               catch (MessagingException ex) {
                  logwrapr.severe("main: MessagingException error detected.", ex);
               } catch (IOException ex) {
                  logwrapr.severe("main: IOException error detected.", ex);         
               }
            }
         } catch (SQLException ex) {
            logwrapr.severe("main: SQLException error detected.", ex);
         } 
      }
      else{
          System.out.println("not connected");
      }
   }
   
   private static ResultSet extract2send(String sender){
      ResultSet rs = null;
      try {
          
         String lsSQL =" SELECT a.sBranchCD, a.sPayPerID, a.sEmployID, IFNULL(b.sEmailAdd, '') sEmailAdd, c.dPeriodFr, c.dPeriodTo, CONCAT(b.sLastName, ', ', b.sFrstname) sEmployNm, IFNULL(j.sUserIDxx, '') sUserIDxx, IFNULL(j.sProdctID, '') sProdctID" +
                              ", IFNULL(e.sEmailAdd, '') sBranchMl" +
                              ", IFNULL(d.sEmailAdd, '') sDeptMail" +
                              ", IFNULL(i.sEmpLevID, '0') sEmpLevID" +
                              ", f.cDivision" +
                              ", a.sDeptIDxx" +
                              ", i.sBranchCD" +
                              ", e.cMainOffc" +
                       " FROM Payroll_Summary_New a" +
                            " LEFT JOIN Client_Master b ON a.sEmployID = b.sClientID" +
                            " LEFT JOIN Payroll_Period c ON a.sPayPerID = c.sPayPerID" +
                            " LEFT JOIN App_User_Master j ON  a.sEmployID = j.sEmployNo and j.cActivatd = '1' AND j.sProdctID = 'gRider'" +
                            " LEFT JOIN Employee_Master001 i ON a.sEmployID = i.sEmployID" + 
                            " LEFT JOIN Department d ON i.sDeptIDxx = d.sDeptIDxx" + 
                            " LEFT JOIN Branch e ON i.sBranchCD = e.sBranchCD" + 
                            " LEFT JOIN Branch_Others f ON i.sBranchCD = f.sBranchCD" + 
                       " WHERE a.cMailSent = '1'" +                  
                       " ORDER BY e.sBranchCD DESC, sEmployNm" + 
                       (sender.compareToIgnoreCase("ymail") == 0 ? " LIMIT 150" : " LIMIT 350");

//                            " AND b.sEmailAdd" +  (sender.compareToIgnoreCase("ymail") == 0 ? " " : " NOT ") + "LIKE '%gmail.com%'" + 

         //create statement to be use in executing queries
         System.out.println(lsSQL);
         rs = instance.getConnection().createStatement().executeQuery(lsSQL);
      } 
      catch (SQLException ex) {
       logwrapr.severe("extract2send: SQLException error detected.", ex);
      }

      return rs;
   }    
   
   private static byte[] encode(byte[] arr){
        Charset utf8charset = Charset.forName("UTF-8");
        Charset iso88591charset = Charset.forName("ISO-8859-15");

        ByteBuffer inputBuffer = ByteBuffer.wrap(arr);

        // decode UTF-8
        CharBuffer data = utf8charset.decode(inputBuffer);

        // encode ISO-8559-1
        ByteBuffer outputBuffer = iso88591charset.encode(data);
        byte[] outputData = outputBuffer.array();

        return outputData;
    }
   
   private static boolean sendNotification(String apps, String user, String title, String message) throws IOException{
        //String sURL = "https://restgk.guanzongroup.com.ph/notification/send_request.php";        
        String sURL = "https://restgk.guanzongroup.com.ph/notification/send_request_system.php";        
        Calendar calendar = Calendar.getInstance();
        //Create the header section needed by the API
        Map<String, String> headers = 
                        new HashMap<String, String>();
        headers.put("Accept", "application/json");
        headers.put("Content-Type", "application/json");
        headers.put("g-api-id", "IntegSys");
        headers.put("g-api-imei", "356060072281722");
        headers.put("g-api-key", SQLUtil.dateFormat(calendar.getTime(), "yyyyMMddHHmmss"));    
        headers.put("g-api-hash", org.apache.commons.codec.digest.DigestUtils.md5Hex((String)headers.get("g-api-imei") + (String)headers.get("g-api-key")));    
        headers.put("g-api-user", "GAP0190001");   
        headers.put("g-api-mobile", "09178048085");
        //headers.put("g-api-token", "cPYKpB-pPYM:APA91bE82C4lKZduL9B2WA1Ygd0znWEUl9rM7pflSlpYLQJq4Nl9l5W4tWinyy5RCLNTSs3bX3JjOVhYnmCpe7zM98cENXt5tIHwW_2P8Q3BXI7gYtEMTJN5JxirOjNTzxWHkWDEafza");    
        headers.put("g-api-token", "fFg2vKxLR-6VJmLA1f8ZbX:APA91bF-pCydHARkxMoj5JeyhHM9WyHo8WhES--609t5-vD9wEfR5PcgHCCRpPqsZHHDmD3CySSSKhvB7Lud_jOLYTcmDk--PDry4darnlQGdsB-9tgPDmfnAHXnf1k7NJpPh0Vu2xFA");    

        JSONArray rcpts = new JSONArray();
        JSONObject rcpt = new JSONObject();
        rcpt.put("app", apps);
        rcpt.put("user", user);
        rcpts.add(rcpt);
        
        //Create the parameters needed by the API
        JSONObject param = new JSONObject();
        param.put("type", "00000");
        param.put("parent", null);
        param.put("title", title);
        param.put("message", message);
        param.put("rcpt", rcpts);

        JSONParser oParser = new JSONParser();
        JSONObject json_obj = null;

        String response = WebClient.httpsPostJSon(sURL, param.toJSONString(), (HashMap<String, String>) headers);
        if(response == null){
            System.out.println("HTTP Error detected: " + System.getProperty("store.error.info"));
            System.exit(1);
        }
        //json_obj = (JSONObject) oParser.parse(response);
        //System.out.println(json_obj.toJSONString());
        
        return true;
   }
   
    private static String toBase64(String val){
        Base64 base64 = new Base64();
        String encodedString1 = new String(base64.encode(val.getBytes()));
        return (encodedString1);
    }
}
