
import org.rmj.mail.App.FTP_Upload;

public class sendEmail {
    public static void main(String [] args){
        args = new String[3];
        args[0] = "M0W123000001";
        args[1] = "c:/GGC_Systems/Temp/Upload/";
        args[2] = "M0W423000051.pdf";
        
        FTP_Upload.main(args);
    }
}
