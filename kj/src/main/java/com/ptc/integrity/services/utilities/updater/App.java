package com.ptc.integrity.services.utilities.updater;


import java.io.BufferedReader;
import java.io.Console;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.sql.Timestamp;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.regex.Pattern;

import org.apache.commons.configuration.CompositeConfiguration;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.PropertiesConfiguration;
import org.apache.commons.configuration.SystemConfiguration;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

import com.mks.api.CmdRunner;
import com.mks.api.Command;
import com.mks.api.IntegrationPoint;
import com.mks.api.IntegrationPointFactory;
import com.mks.api.Session;
import com.mks.api.response.APIException;
import com.mks.api.response.Field;
import com.mks.api.response.Response;
import com.mks.api.response.WorkItem;
import com.mks.api.response.WorkItemIterator;
import com.mks.api.util.APIVersion;
import com.ptc.integrity.services.utilities.models.Sandbox;

/**
 * Hello world!
 *
 */
public class App{
	public static final String PROPERTIES_FILE = "rict.properties";
	public static final String PROPERTY_OLD_CLIENT_DIR = "OLD_CLIENT_DIR";
	public static final String PROPERTY_NEW_CLIENT_INSTALLATOR_DIR = "NEW_CLIENT_INSTALL_APP_DIR";
	public static final String PROPERTY_PROCESS_WORK_TIME_LIMIT = "WORKING_TIME_LIMIT_OF_PROCESS";
	public static final String PROPERTY_PROCESS_CHECK_TIME_INTERVAL = "PROCESS_CHECK_INTERVAL";
	public static final String PROPERTY_SERVER_HOSTNAME = "SERVER_HOSTNAME";
	public static final String PROPERTY_SERVER_PORT = "SERVER_PORT";
	
	public static String oldClientDir, installAppDir, appDir, userHome, mksDir, serverHostname, serverPort;
	
	public static CompositeConfiguration config;
	static final Logger log = LogManager.getLogger(App.class.getName());

	private static int processWorkTimeLimit = 60*20;
	private static int processCheckInterval = 10;
	
	private static Timestamp mainStart, mainStop;
	private static APIUtils api;
	
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
		serverHostname = config.getString(PROPERTY_SERVER_HOSTNAME);
		serverPort = config.getString(PROPERTY_SERVER_PORT);
		api = new APIUtils();
	
	}
	
    public static void main( String[] args ) throws InterruptedException {    	
    	
    	/*
    	Runtime runTime = Runtime.getRuntime();
    	Process process;
    	try {
    		process = runTime.exec("cmd /c C:\\MKS\\IntegrityClient\\uninstall\\IntegrityClientUninstall.exe");
    		Thread.sleep(10000);
    		if(WindowsUtils.ifProcessRunning("IntegrityClient.exe")){
    			System.out.println("Process running");
    			process.waitFor();
    		}

		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		
		*/
		
    	
        mainStart = new Timestamp(new java.util.Date().getTime());
        
        /*
    	if( !checkProperties()) {
    		abortApp();
    	}
    	
    	*/
         
    	Timestamp start, stop;

    	
    	if (WindowsUtils.ifProcessRunning("IntegrityClient.exe")) {
    		WindowsUtils.killProcess("IntegrityClient.exe");
    		log.info("Find running IntegrityClient, process will be terminated");
    	}
    	
    	backUpMksDir();
    	/*
    	// UNINSTALL CLIENT
    	try {
        	start = new Timestamp(new java.util.Date().getTime());
        	System.out.println("[" + start + "] Uninstalling old Integrity Client...");
    		int exitCode = uninstallIntegrityClient();
        	stop = new Timestamp(new java.util.Date().getTime());
    		if (exitCode == 0){
    			String message = "[" + stop + "] Old Integrity Client was successful uninstalled. Time duration: " +Utils.timeDuration(start, stop);
    			WindowsUtils.deleteFolder(new File(config.getString(PROPERTY_OLD_CLIENT_DIR)));
    			System.out.println(message);
    			log.info(message);
    			
    		}else {
    			String message = "[" + stop + "] Error occurred while uninstalling Integrity Client. Time duration: " +Utils.timeDuration(start, stop);
    			System.out.println(message);
    			log.error(message);
    			//abortApp(); //TODO uncomment
    		}
		} catch (IOException e) {
			log.error(e);
			log.error("Please check if \""+PROPERTY_OLD_CLIENT_DIR+"\" property is properly set and points to OLD IntegrityClient dir");
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
    	
    	

    
    	
    	// update viewsets
    	try {
			replacePortsAndSettings(new File(mksDir + File.separator + "viewset") , "localhost", "7001");
		} catch (IOException e) {
			log.error(e);
		}
    	
    
    	*/
    	
    		// update sandboxes
    	
    	
    	
    	String userName = readFromConsole("Enter User Name:");
    	String password = readFromConsole("Enter your password:");
    	api.connectToIntegrity(userName, password);
    	List<String> sandboxes = api.getSanboxesRegisteredTo("localhost");
    	/*for (String sandbox : sandboxes) {
    		log.info("Found Sandbox: " + sandbox);
    	}
    	api.dropSanboxes(sandboxes);
    	api.reImportSandboxes(sandboxes, "admin", "admin", "192.168.153.29", "7001");
		*/
    	api.setDefaultServerConnection("localhost");
    	
    	api.endSession();
    	exitAppSuccessfull();
    }
    
    private static String readFromConsole(String message){
    	 BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
         System.out.print(message);
         try {
			return br.readLine();
		} catch (IOException e) {
			log.error(e);
		}
		return null;

    }
                
    public static void exitAppSuccessfull(){
    	System.out.println("Total time duration: " + Utils.timeDuration(mainStart, mainStop)+"\nAll roll-out steps run successfully. Press enter to exit RICT");
    	log.info("Total time duration: " + Utils.timeDuration(mainStart, mainStop));
    	try {
			System.in.read();
		} catch (IOException e) {
			e.printStackTrace();
		}
    }
    
    public static void abortApp(){
    	System.out.println("Total time duration: " + Utils.timeDuration(mainStart, mainStop)+"\nError occurred. Please check log file. Press enter to exit RICT");
    	log.info("Total time duration: " + Utils.timeDuration(mainStart, mainStop));
    	try {
			System.in.read();
		} catch (IOException e) {
			e.printStackTrace();
		}
    }
    
    private static int uninstallIntegrityClient() throws IOException {
    	String oldClientDir = config.getString(PROPERTY_OLD_CLIENT_DIR) + File.separator + "uninstall" + File.separator + "IntegrityClientUninstall.exe";
    	String pathToProcess = oldClientDir;
		File f = new File(pathToProcess);
		if (!f.exists()){
			log.error(f.getAbsolutePath() + " doesn't exist, uninstall abort");
			return -1;
		}
		
		if (oldClientDir.contains("\\")) {
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
    	File viewSetsBackup = new File(appDir+File.separator+"mks_backup"+ File.separator+".mks");
    	
    	WindowsUtils.deleteFolder(viewSetsBackup);
    	viewSetsBackup.mkdirs();
    	
    	if(fViewSets.isDirectory()){
    		try {
				Utils.copyFolder(fViewSets, viewSetsBackup);
			} catch (IOException e) {
				log.error(e);
				e.printStackTrace();
			}
    	} else {
    		log.error("Cannot find "+fViewSets.getAbsolutePath() + " viewset directory");
    	}
    }
    
    private static void replacePortsAndSettings(File dir, String hostname, String port) throws IOException {
    	
	   	 String p1 = "<Setting name=\"server.port\">";
	   	 String p3 = "<Setting name=\"server.hostname\">";
	   	 String p2 = "</Setting>";
	   	 
	   	 String hostSetting = p3 + hostname + p2;
	   	 String portSetting = p1 + port +p2;
	   	 
	   	 String portRegex = Pattern.quote(p1) + "(.*?)"  + Pattern.quote(p2);
	   	 String hostRegex = Pattern.quote(p3) + "(.*?)"  + Pattern.quote(p2);

    	if (!dir.isDirectory()) {
    		log.error("Error while replacing hostnames and ports in viewsets directory " + dir.getAbsolutePath() + " doesn't exists");
    		return;
    	}
    	
    	File[] arrayOfDirs = dir.listFiles();
    	
    	for (File directory : arrayOfDirs) {
	    	File[] arrayOfViewsets = directory.listFiles();
	    	
	    	for (File viewset : arrayOfViewsets) {
	    		
	    		FileInputStream fs = new FileInputStream(viewset.getAbsoluteFile());
	    		BufferedReader br = new BufferedReader(new InputStreamReader(fs));
	    		FileWriter writer = new FileWriter(viewset.getAbsoluteFile());
	    		
	    		String line = br.readLine();
	    		while (line != null) {
	    			line = line.replaceAll(portRegex, portSetting);
	    			line = line.replaceAll(hostRegex, hostSetting);
	    			writer.write(line);
	    			writer.write(System.getProperty("line.separator"));
	    			line = br.readLine();
	    		}
	    		writer.flush();
	    		writer.close();	
	    		br.close();
	    		fs.close();
	    	}    		
    	}

    }
    
    private static boolean checkProperties(){
    	File oldClientFolder = new File(oldClientDir);
    	File installatorFolder = new File(installAppDir);
		File mksclient = new File(installatorFolder + File.separator + "mksclient.exe");
		File mksprop = new File(installatorFolder + File.separator + "mksclient.properties");
		
    	if (oldClientFolder.isDirectory()){
    		log.info("Found old Client app directory [" + oldClientFolder.getAbsolutePath()+"]");
    	} else {
    		log.error("Old Client app directory not found under [" + oldClientFolder.getAbsolutePath() +"]");
    		return false;
    	}
    	if (installatorFolder.isDirectory()) {
    		if (mksclient.exists()) {
    			log.info("Found mksclinet.exe");
    		} else {
    			log.error("Can't find mksclient.exe in the specified folder ["+mksclient.getAbsolutePath()+"]");
    			return false;
    		}
    		if (mksprop.exists()) {
    			log.info("Found mksclient.properties");
    		} else {
    			log.error("Can't find mksclient.properties in the specified folder ["+mksprop.getAbsolutePath()+"]");
    		}
    	} else {
    		log.error("Can't find the specified folder ["+installatorFolder.getAbsolutePath()+"] Please check property " + PROPERTY_NEW_CLIENT_INSTALLATOR_DIR);
    		return false;
    	}
    	return true;
    }

    private static void modifySandboxes(){
    	
    }
}
