package local.jk.kj;


import java.io.File;
import java.io.IOException;
import java.util.List;

import org.apache.commons.configuration.CompositeConfiguration;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.PropertiesConfiguration;
import org.apache.commons.configuration.SystemConfiguration;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

/**
 * Hello world!
 *
 */
public class App{
	public static final String PROPERTIES_FILE = "ui.properties";
	public static final String PROPERTY_PROCESS = "ui.process";
	public static final String PROPERTY_PROCESS_WORK_TIME_LIMIT = "ui.process.work.time.limit";
	public static final String PROPERTY_PROCESS_CHECK_TIME_INTERVAL = "ui.process.check.interval";

	public static CompositeConfiguration config;
	static final Logger log = LogManager.getLogger(App.class.getName());

	private static int processWorkTimeLimit = 60*20;
	private static int processCheckInterval = 10;
	
	static {
		log.info("********** Uninstall Install *************");
		config = new CompositeConfiguration();
		
		config.addConfiguration(new SystemConfiguration());
		
		File f = null;
		String programDir = System.getProperty("user.dir");
		if (programDir != null) {
			f = new File(programDir+File.separator+PROPERTIES_FILE);
			if (f.canRead()) {
				try {
					config.addConfiguration(new PropertiesConfiguration(f.getAbsoluteFile()));
					log.debug("Added configuration from: " + f.getAbsoluteFile());
				} catch (ConfigurationException e) {
					e.printStackTrace();
				}

			} else {
				log.error("Configuration file: "+ f.getAbsoluteFile()+ " doesn't exist");
			}
		}
		
		processWorkTimeLimit = config.getInt(PROPERTY_PROCESS_WORK_TIME_LIMIT, 60*20);
		processCheckInterval = config.getInt(PROPERTY_PROCESS_CHECK_TIME_INTERVAL, 10);
	}
	
    public static void main( String[] args ) {
    	try {
			runAllProcesses(false);
		} catch (IOException e) {
			log.error(e);
			log.error("Please check if path to program specified in properties file is correct");
		}
    }
    
    public static void runAllProcesses(boolean terminateExistingProcesses) throws IOException{
    	
    	List<String> processesName = config.getList(PROPERTY_PROCESS);

    	for (String processName : processesName) {
        	String pathToProcess = processName;
        	
    		if (processName.contains("\\")) {
    			File f = new File(pathToProcess);
    			processName = f.getName();
    		}
    		
    		if (terminateExistingProcesses) {
		    	int numberOfProcesses = WindowsUtils.numberOfRunningProcesses(processName);
		    	if (numberOfProcesses > 0 ) {
		    		if (numberOfProcesses == 1){
		    			log.warn("find already running process "+ processName + " process will be terminated");
		    		} else {
		        		log.warn("find already running "+numberOfProcesses+" \""+ processName + "\" processes, all of them will be terminated");
		        		
		    		}
		        	WindowsUtils.killProcess(processName);
		    	}    			
    		}

			WindowsUtils.startProcess(pathToProcess, processWorkTimeLimit , processCheckInterval);
    	}	
    }
}
