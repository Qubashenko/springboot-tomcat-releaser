package hello;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;
import org.springframework.context.event.ContextRefreshedEvent;
import org.apache.log4j.Logger;
import java.io.IOException;
import java.io.InputStream;

@Component
public class TomcatReleaser implements ApplicationListener<ContextRefreshedEvent>{


    private static String OS = System.getProperty("os.name").toLowerCase();
    private Runtime RT = Runtime.getRuntime();
    private static Logger log = Logger.getLogger(TomcatReleaser.class.getName());

    @Value("${tomcat_releaser.tomcat_port:8080}")
    private String tomcat_port;


    @Override
    public void onApplicationEvent(ContextRefreshedEvent contextRefreshedEvent) {
        log.info("Port found: " + this.tomcat_port);
        String pid = "";
        try {
            pid = getPidByPort(tomcat_port);
            log.info("Tomcat pid instance found: " + pid);
            if (!pid.isEmpty()) {

                killProcById(pid, RT);
                if (getPidByPort(tomcat_port).isEmpty()){
                    log.info("Tomcat instance has been killed");
                }
            }

        } catch (IOException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

    }


    private static String getPidByPort(String port) throws InterruptedException, IOException {

        ProcessBuilder builder = new ProcessBuilder("cmd.exe", "/C", "for /f \"tokens=5\" %a in ('netstat -aon ^| findstr " + port + "') do echo %a");
        Process process = null;
        try {
            process = builder.start();
        } catch (IOException e) {
            e.printStackTrace();
        }
        process.waitFor();
        return parseNetstatOutput(process.getInputStream());
    }

    private static String parseNetstatOutput(InputStream inputStream) throws IOException{
        int bytesRead = -1;
        byte[] bytes = new byte[1024];
        String output=  "";

        while((bytesRead = inputStream.read(bytes)) > -1){
            output =  new String(bytes, 0, bytesRead);
        }

        String[] last_line = output.split("[\\r\\n]+");
        String[] pid = last_line[last_line.length-1].split("\\s+");
        return pid[pid.length-1].trim();
    }


    public static void killProcById(String pid, Runtime rt){

        try{
            String cmd = new String();

            if(isWindows()){
                cmd ="taskkill /F /PID " + pid;
            }else if(isUnix()){
                cmd = "kill -9 " + pid;
            }
            rt.exec(cmd);

        }catch(Exception e){
            e.printStackTrace();
            log.error("Failed to kill" + pid + "process");
            System.exit(0);
        }
    }

    public static boolean isWindows(){
        return (OS.contains("win"));
    }

    public static boolean isUnix(){
        return ((OS.contains("nix"))||(OS.contains("nux"))||(OS.contains("aix")));
    }
}
