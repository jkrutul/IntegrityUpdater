package local.jk.kj;

import java.sql.Timestamp;

public class Utils {

	
	public static String timeDuration (Timestamp start, Timestamp stop) {
		long diff = stop.getTime() - start.getTime();
		long diffSeconds = diff / 1000 % 60;
		long diffMinutes = diff / (60 * 1000) % 60;
		long diffHours = diff / (60 * 60 * 1000) % 24;
		long diffDays = diff / (24 * 60 * 60 * 1000);
		
		if (diffDays > 0){
			return diffDays +"days " + String.format("%02d", diffHours)+":"+String.format("%02d", diffMinutes)+":"+ String.format("%02d", diffSeconds)+"s ";
		} else {
			return String.format("%02d", diffHours)+":"+String.format("%02d", diffMinutes)+":"+ String.format("%02d", diffSeconds)+"s ";
		}
	
		
	}
}
