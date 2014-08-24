package local.jk.kj;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

public class WindowsUtils {
	
	static final Logger log = LogManager.getLogger(App.class.getName());
	
	public static List<String> listRunningProcesses() {
	    List<String> processes = new ArrayList<String>();
	    try {
	      String line;
	      Process p = Runtime.getRuntime().exec("tasklist.exe /fo csv /nh");
	      BufferedReader input = new BufferedReader(new InputStreamReader(p.getInputStream()));
	      while ((line = input.readLine()) != null) {
	          if (!line.trim().equals("")) {
	              // keep only the process name
	              line = line.substring(1);
	              processes.add(line.substring(0, line.indexOf("\"")));
	          }

	      }
	      input.close();
	    }
	    catch (Exception err) {
	      err.printStackTrace();
	    }
	    return processes;
	  }
	
	public static void killProcess (String taskName){
		Process p;
		try {
			String line;
			p = Runtime.getRuntime().exec("taskkill.exe /IM "+ taskName);
			BufferedReader input = new BufferedReader(new InputStreamReader(p.getInputStream()));
			while ((line = input.readLine()) != null ){
				log.warn(line);
			}
		} catch (IOException e) {
			e.printStackTrace();
		}

	}
	/**
	 * Function to start process
	 * @param processName - name of The process
	 * @param interruptProcessWorkAfter - number of seconds after process work shoud be interrupted
	 * @param processCheckInterval - checking process interval
	 * @throws IOException 
	 */
	public static void startProcess (String processName, int interruptProcessWorkAfter, int processCheckInterval) throws IOException {
		Process process = null;
    	Runtime runTime = Runtime.getRuntime();
    	Timestamp start, stop;
    	
    	interruptProcessWorkAfter*=1000;
    	processCheckInterval*=1000;
    	start = new Timestamp(new java.util.Date().getTime());

    	process = runTime.exec(processName);
    	log.info("Process: "+ processName+ " was started at: " + start);

    	
    	int counter = 0;
    	int limit = ((interruptProcessWorkAfter) / processCheckInterval) ;
    	
    	while (true) {
    		try {
				Thread.sleep(processCheckInterval);
			} catch (InterruptedException e) {
				log.error(e);
			}
    		if (WindowsUtils.ifProcessRunning(processName)){
    			log.info("Process \"" + processName+"\" is still running");        		
    		} else {
        		stop = new Timestamp(new java.util.Date().getTime());
        		String timeDuraton = Utils.timeDuration(start, stop);
       			log.info("Process \"" + processName +"\"  sucessful end his work. Process time duration: " + timeDuraton);
        		
    			break;
    		}

    		if ( counter > limit ) {
        		stop = new Timestamp(new java.util.Date().getTime());
        		String timeDuraton = Utils.timeDuration(start, stop);
        		log.error("Process \"" + processName+"\" is exceed execution limit and will be terminated.");  
    			process.destroy();
    			log.error("Process \"" + processName+"\" has been terminated. Process time duration: " + timeDuraton);
    			break;
    		} 
    		
    		counter++;
    	}	
	}
	
	public static boolean ifProcessRunning (String processName){
		return listRunningProcesses().contains(processName);
	}
	
	public static int numberOfRunningProcesses(String processName){
		int counter = 0;
		List<String> processes = listRunningProcesses();
		for (String process : processes){
			if (process.equals(processName)) {
				counter++;
			}
		}
		
		return counter;
	}

}
