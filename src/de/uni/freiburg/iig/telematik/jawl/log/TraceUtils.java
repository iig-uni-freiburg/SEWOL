package de.uni.freiburg.iig.telematik.jawl.log;


public class TraceUtils {
	
	public static CommonLogEntries getCommonActivities(LogTrace... traces){
		return new CommonLogEntries(traces);
	}
	
	public static void main(String[] args){
		LogTrace trace1 = new LogTrace(1);
		LogTrace trace2 = new LogTrace(2);
		LogTrace trace3 = new LogTrace(3);
		LogEntry entry1A = new LogEntry("A");
		LogEntry entry1B = new LogEntry("B");
		LogEntry entry1C = new LogEntry("C");
		LogEntry entry1D = new LogEntry("D");
		LogEntry entry2A = new LogEntry("A");
		LogEntry entry2B = new LogEntry("B");
		LogEntry entry2C = new LogEntry("C");
		LogEntry entry2D = new LogEntry("D");
		LogEntry entry3A = new LogEntry("A");
		LogEntry entry3B = new LogEntry("B");
		LogEntry entry3C = new LogEntry("C");
		LogEntry entry3D = new LogEntry("D");
		trace1.addEntry(entry1A);
		trace1.addEntry(entry1B);
		trace1.addEntry(entry1C);
		trace2.addEntry(entry2B);
		trace2.addEntry(entry2C);
		trace3.addEntry(entry3A);
		trace3.addEntry(entry3B);
		
		System.out.println(getCommonActivities(trace1,trace2,trace3).getCommonEntries());
		
	}
	
	

}