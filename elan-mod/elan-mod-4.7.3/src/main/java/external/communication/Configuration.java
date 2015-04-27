package external.communication;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *  This class loads and save all properties which are important for the 
 * communication.
 * 
 * @author jeeickme
 */
public class Configuration {

    private Properties prop = new Properties();
    private String path;
   
    public String remoteIP = "localhost";
    public int remotePort = 4241;
    public String browserIP = "localhost";
    public int browserPort = 4243;
    public int syncFreq = 200;
    
    /**
     * init class, set the path to an external config.properties file and load the properties.
     * @param pathToConfig 
     */
    public Configuration(String pathToConfig) {
        this.path = pathToConfig;
        loadProperties();
    }

    /**
     * loading the properties of a file and save them as variables of this class.
     */
    private void loadProperties() {

        System.out.println("LOADING CONFIGFILE: "+this.path);
        
        try {
            this.prop.load(new FileInputStream(this.path));
            System.out.println("Found file");
            remoteIP = prop.getProperty("remoteIP");
            System.out.println("got remote IP " + remoteIP);
            browserIP = prop.getProperty("browserIP");
            System.out.println("got browserIP " + browserIP);
            remotePort = Integer.parseInt(prop.getProperty("remotePort"));
            System.out.println("got remote port " + remotePort);
            browserPort = Integer.parseInt(prop.getProperty("browserPort"));
            System.out.println("got browser port " + browserPort);
            syncFreq = Integer.parseInt(prop.getProperty("syncFreq"));
            System.out.println("got remote frequency " + syncFreq);
        } catch (IOException ex) {
            Logger.getLogger(EventHook.class.getName()).log(Level.SEVERE, null, ex);
            System.out.println("Failed to load properties file, will use default");
        } catch (NumberFormatException ex) {
            System.out.println("Error while reading configuration file");
        }

        System.out.println(this.remoteIP);
        System.out.println(this.remotePort);

    }
}
