package com.ptc.integrity.services.utilities.updater;


import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.sql.Timestamp;
import java.util.Scanner;

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
	public static final String PROPERTIES_FILE = "rict.properties";
	public static final String PROPERTY_NEW_CLIENT_DIR = "NEW_CLIENT_INSTALL_DIR";
	public static final String PROPERTY_OLD_CLIENT_DIR = "OLD_CLIENT_DIR";
	public static final String PROPERTY_OLD_CLIENT_UNINSTALLATOR = "OLD_CLIENT_UNINSTALL_APP";
	public static final String PROPERTY_NEW_CLIENT_INSTALLATOR_DIR = "NEW_CLIENT_INSTALL_APP";
	public static final String PROPERTY_PROCESS_WORK_TIME_LIMIT = "WORKING_TIME_LIMIT_OF_PROCESS";
	public static final String PROPERTY_PROCESS_CHECK_TIME_INTERVAL = "rict.process.check.interval";
	
	public static String oldClientDir, installAppDir, appDir, userHome, mksDir;
	
	

	public static CompositeConfiguration config;
	static final Logger log = LogManager.getLogger(App.class.getName());

	private static int processWorkTimeLimit = 60*20;
	private static int processCheckInterval = 10;
	
	static {
		log.info("********** Rollout Integrity Client Tool **********");
		System.out.println("********** Rollout Integrity Client Tool **********");
		config = new CompositeConfiguration();
		
		config.addConfiguration(new SystemConfiguration());
		
		File f = null;
		appDir = System.getProperty("user.dir");
		userHome = System.getProperty("user.home");
		mksDir = userHome + File.separator + ".mks";
		
		if (appDir != null) {
			f = new File(appDir+File.separator+PROPERTIES_FILE);
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
		oldClientDir = config.getString(PROPERTY_OLD_CLIENT_DIR);
		installAppDir = config.getString(PROPERTY_NEW_CLIENT_INSTALLATOR_DIR);
		
	
	}
	
    public static void main( String[] args ) {    	
    	Timestamp mainStart, start, stop, mainStop;
    	mainStart = new Timestamp(new java.util.Date().getTime());
    	
    	if (WindowsUtils.ifProcessRunning("IntegrityClient.exe")) {
    		WindowsUtils.killProcess("IntegrityClient.exe");
    		log.info("Find running IntegrityClient, process will be terminated");
    	}
    	
    	backUpMksDir();
    	
    	// UNINSTALL CLIENT
    	try {
        	start = new Timestamp(new java.util.Date().getTime());
        	System.out.println("[" + start + "] Uninstalling old Integrity Client...");
    		int exitCode = uninstallIntegrityClient();
        	stop = new Timestamp(new java.util.Date().getTime());
    		if (exitCode == 0){
    			String message = "[" + stop + "] Old Integrity Client was successful uninstalled. Time duration: " +Utils.timeDuration(start, stop);
    			WindowsUtils.removeDirectory(new File(config.getString(PROPERTY_OLD_CLIENT_DIR)));
    			System.out.println(message);
    			log.info(message);
    			
    		}else {
    			String message = "[" + stop + "] Error occurred while uninstalling Integrity Client. Time duration: " +Utils.timeDuration(start, stop);
    			System.out.println(message);
    			log.error(message);
    			return;
    		}
		} catch (IOException e) {
			log.error(e);
			log.error("Please check if \""+PROPERTY_OLD_CLIENT_UNINSTALLATOR+"\" property is properly set and points to OLD IntegrityClient dir");
		}
    	
    	File oldClientDir = new File(config.getString(PROPERTY_OLD_CLIENT_DIR));
    	if (oldClientDir!= null && oldClientDir.isDirectory()) {
    		if (WindowsUtils.deleteFolder(oldClientDir)) {
    			log.info("Old client directory ["+oldClientDir.getAbsolutePath()+"] was removed");
    		} else {
    			log.warn("Cannot delete Old Client directory ["+oldClientDir.getAbsolutePath()+"]" );
    		}
    	}

    	
    	// INSTALL NEW CLIENT
    	try {
        	start = new Timestamp(new java.util.Date().getTime());
        	System.out.println("[" + start + "] Installing new Integrity Client...");
    		int exitCode = installNewIntegrityClient();
        	stop = new Timestamp(new java.util.Date().getTime());
    		if (exitCode == 0){
    			String message = "[" + stop + "] New Integrity Client was successful installed. Time duration: " +Utils.timeDuration(start, stop);
    			System.out.println(message);
    			log.info(message);
    			
    		}else {
    			String message = "[" + stop + "] Error occurred while installing Integrity Client. Time duration: " +Utils.timeDuration(start, stop);
    			System.out.println(message);
    			log.error(message);
    			return;
    		}
		} catch (IOException e) {
			log.error(e);
			log.error("Please check if \""+PROPERTY_NEW_CLIENT_INSTALLATOR_DIR+"\" property is properly set");
		}
    	
    	mainStop = new Timestamp(new java.util.Date().getTime());
    	System.out.println("Total time duration: " + Utils.timeDuration(mainStart, mainStop)+"\nAll roll-out steps run successfully. Press enter to exit RICT");
    	log.info("Total time duration: " + Utils.timeDuration(mainStart, mainStop));
    	try {
			System.in.read();
		} catch (IOException e) {
			e.printStackTrace();
		}
    	

    	// pasteviewsets
    	// update viewsets
    	replacePortsAndSettings(new File(mksDir + File.separator + "viewset" + File.separator+"user") , "localhost", "7001");
    	
    	
    		
    }
    
    private static int uninstallIntegrityClient() throws IOException {
    	String oldClientDir = config.getString(PROPERTY_OLD_CLIENT_UNINSTALLATOR);
    	String pathToProcess = oldClientDir;
    	
		if (oldClientDir.contains("\\")) {
			File f = new File(pathToProcess);
			oldClientDir = f.getName();
		}
		
	    int numberOfProcesses = WindowsUtils.numberOfRunningProcesses(oldClientDir);
	    if (numberOfProcesses > 0 ) {
	    	if (numberOfProcesses == 1){
	    		log.warn("find already running process "+ oldClientDir + " process will be terminated");
	    	} else {
	       		log.warn("find already running "+numberOfProcesses+" \""+ oldClientDir + "\" processes, all of them will be terminated");
	       		
	    	}
	       	WindowsUtils.killProcess(oldClientDir);
	    }    			
		
	    WindowsUtils.startProcess(pathToProcess, null, processWorkTimeLimit , processCheckInterval);
	    return WindowsUtils.checkExitCode();
    }
    
    private static int installNewIntegrityClient() throws IOException {
    	//String integrityInstallatorDir = config.getString(PROPERTY_NEW_CLIENT_INSTALLATOR_DIR);
    	String integrityInstallator = config.getString(PROPERTY_NEW_CLIENT_INSTALLATOR_DIR) + File.separator + "mksclient.exe";
    	
    	String propertiesFileName= "mksclient.properties";
    	
    	/* Properties prop = new Properties();
    	prop.setProperty("INSTALLER_UI", "silent");
    	prop.setProperty("MKS_LICENSE_AGREEMENT", "true");
    	prop.setProperty("MKS_CREATE_INTEGRITY_CLIENT_SHORTCUT", "true");
    	prop.setProperty("MKS_CREATE_ADMIN_CLIENT_SHORTCUT", "true");
    	prop.setProperty("USER_INSTALL_DIR", config.getString(PROPERTY_NEW_CLIENT_DIR));
    	prop.setProperty("INSTALL_OVERRIDE", "true");
    	prop.setProperty("MKS_USE_SAME_SERVER", config.getString("MKS_USE_SAME_SERVER"));
    	prop.setProperty("MKS_COMMON_HOST", config.getString("MKS_COMMON_HOST"));
    	prop.setProperty("MKS_COMMON_PORT", config.getString("MKS_COMMON_PORT"));
    	prop.setProperty("MKS_IM_HOST", config.getString("MKS_IM_HOST"));
    	prop.setProperty("MKS_IM_PORT", config.getString("MKS_IM_PORT"));
    	prop.setProperty("MKS_SI_HOST", config.getString("MKS_SI_HOST"));
    	prop.setProperty("MKS_SI_PORT", config.getString("MKS_SI_PORT"));
    	prop.setProperty("MKS_PRODUCT_LANGUAGE", config.getString("MKS_PRODUCT_LANGUAGE"));

    	File propFile = new File(integrityInstallatorDir+File.separator+propertiesFileName);
    	OutputStream fos = new FileOutputStream(propFile);
    	prop.store(fos, "# Integrity Client silent installer properties file.");
    	
    	/*
    	String host, port, imHost, imPort, siHost, siPort;
    	host = config.getString("MKS_COMMON_HOST");
    	port = config.getString("MKS_COMMON_PORT");
    	imHost = config.getString("MKS_IM_HOST");
    	imPort = config.getString("MKS_IM_PORT");
    	siHost = config.getString("MKS_SI_HOST");
    	siPort = config.getString("MKS_SI_PORT");
    	
    	if (config.getString("MKS_USE_SAME_SERVER").isEmpty()){
    		prop.setProperty("MKS_USE_SAME_SERVER", "true");
    	} else {
    		prop.setProperty("MKS_USE_SAME_SERVER", config.getString("MKS_USE_SAME_SERVER"));
    	}
    	
    	if (!host.isEmpty()) {
    	
    	}
    	
    	*/

    
    	String pathToProcess = integrityInstallator;
    	
		if (integrityInstallator.contains("\\")) {
			File f = new File(pathToProcess);
			integrityInstallator = f.getName();
		}
		
	    int numberOfProcesses = WindowsUtils.numberOfRunningProcesses(integrityInstallator);
	    if (numberOfProcesses > 0 ) {
	    	if (numberOfProcesses == 1){
	    		log.warn("find already running process "+ integrityInstallator + " process will be terminated");
	    	} else {
	       		log.warn("find already running "+numberOfProcesses+" \""+ integrityInstallator + "\" processes, all of them will be terminated");
	       		
	    	}
	       	WindowsUtils.killProcess(integrityInstallator);
	    }    			
		
	    WindowsUtils.startProcess(pathToProcess, "-f "+propertiesFileName, processWorkTimeLimit , processCheckInterval);
	    return WindowsUtils.checkExitCode();    	
    }
    
    private static void backUpMksDir(){
    	File fViewSets = new File(mksDir);
    	if(fViewSets.isDirectory()){
    		try {
				Utils.copyFolder(fViewSets, new File(appDir+File.separator+"mks_backup"+ File.separator+".mks"));
			} catch (IOException e) {
				log.error(e);
				e.printStackTrace();
			}
    	} else {
    		log.error("Cannot find "+fViewSets.getAbsolutePath() + " viewset directory");
    	}
    }
    
    private static void replacePortsAndSettings(File dir, String hostname, String port) {
    	if (!dir.isDirectory()) {
    		log.error("Error while replacing hostnames and ports in viewsets directory " + dir.getAbsolutePath() + " doesn't exists");
    		return;
    	}
    	File[] arrayOfViewsets = dir.listFiles();
    	
    	for (File viewset : arrayOfViewsets) {

    			Scanner sc = null;
				try {
					sc = new Scanner(viewset);
				} catch (FileNotFoundException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
    			
    			while (sc.hasNextLine()) {
    				String line = sc.nextLine();
    				//line.replaceAll(regex, replacement)
    			}
     		
    	}
    	
    	
    	
    }
    
    /*
    private void checkProperties(){
		File f1, f2, f3;
		
		f1 = new File(mksDir);
		f2 = new File(oldClientDir);
		f3 = new File(installAppDir);
		if (!f1.isDirectory()){
			
		}

    }
    */
    
}
