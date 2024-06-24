
import java.io.IOException;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.rmj.appdriver.SQLUtil;
import org.rmj.lib.net.WebClient;

public class testSendNotification {
    public static void main(String [] args){
        try {
            if (sendNotification("gRider", "GAP0190004", "test notification", "this is a test.")){
                System.out.println("Done sending!");
            } else {
                System.out.println("Unable to send!");
            }
        } catch (IOException ex) {
            Logger.getLogger(testSendNotification.class.getName()).log(Level.SEVERE, null, ex);
        }
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
}
