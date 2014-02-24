package de.uni.freiburg.iig.telematik.jawl.parser.petrify;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.StringTokenizer;

import de.invation.code.toval.file.FileReader;
import de.invation.code.toval.parser.ParserException;
import de.invation.code.toval.validate.ParameterException;
import de.uni.freiburg.iig.telematik.jawl.log.LogEntry;
import de.uni.freiburg.iig.telematik.jawl.log.LogTrace;
import de.uni.freiburg.iig.telematik.jawl.parser.LogParserInterface;

public class PetrifyParser implements LogParserInterface {

	@Override
	public List<List<LogTrace<LogEntry>>> parse(File file, boolean onlyDistinctTraces) throws IOException, ParserException, ParameterException {
		List<List<LogTrace<LogEntry>>> result = new ArrayList<List<LogTrace<LogEntry>>>();
		List<LogTrace<LogEntry>> traceList = new ArrayList<LogTrace<LogEntry>>();
		Set<LogTrace<LogEntry>> traceSet = new HashSet<LogTrace<LogEntry>>();
		result.add(traceList);
		FileReader reader = new FileReader(file.getAbsolutePath());
		String nextLine = null;
		int traceCount = 0;
		while ((nextLine = reader.readLine()) != null) {
			LogTrace<LogEntry> newTrace = new LogTrace<LogEntry>(++traceCount);
			StringTokenizer tokenizer = new StringTokenizer(nextLine);
			while(tokenizer.hasMoreTokens()){
				String nextToken = tokenizer.nextToken();
				if(nextToken != null && !nextToken.isEmpty()){
					newTrace.addEntry(new LogEntry(nextToken));
				}
			}
			if(!onlyDistinctTraces){
				traceList.add(newTrace);
			} else {
				if(traceSet.add(newTrace)){
					traceList.add(newTrace);
				}
			}
		}
		return result;
	}

	

}