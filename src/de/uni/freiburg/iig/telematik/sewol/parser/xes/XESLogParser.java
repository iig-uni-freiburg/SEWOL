package de.uni.freiburg.iig.telematik.sewol.parser.xes;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

import org.deckfour.xes.extension.XExtension;
import org.deckfour.xes.in.XParser;
import org.deckfour.xes.in.XParserRegistry;
import org.deckfour.xes.model.XAttribute;
import org.deckfour.xes.model.XEvent;
import org.deckfour.xes.model.XLog;
import org.deckfour.xes.model.XTrace;

import de.invation.code.toval.parser.ParserException;
import de.invation.code.toval.types.DataUsage;
import de.invation.code.toval.validate.ParameterException;
import de.invation.code.toval.validate.Validate;
import de.uni.freiburg.iig.telematik.sewol.log.DULogEntry;
import de.uni.freiburg.iig.telematik.sewol.log.DataAttribute;
import de.uni.freiburg.iig.telematik.sewol.log.EventType;
import de.uni.freiburg.iig.telematik.sewol.log.LockingException;
import de.uni.freiburg.iig.telematik.sewol.log.LogEntry;
import de.uni.freiburg.iig.telematik.sewol.log.LogSummary;
import de.uni.freiburg.iig.telematik.sewol.log.LogTrace;
import de.uni.freiburg.iig.telematik.sewol.parser.AbstractLogParser;
import de.uni.freiburg.iig.telematik.sewol.parser.ParserDateFormat;
import de.uni.freiburg.iig.telematik.sewol.parser.ParserFileFormat;
import de.uni.freiburg.iig.telematik.sewol.parser.ParsingMode;
import java.io.FileNotFoundException;

/**
 * <p>
 * A parser class for MXML and XES files for the SEWOL log classes.
 * </p>
 * <p>
 * The {@link XParserRegistry} from OpenXES is used, as it helps choosing the right parser. Because of the transformation of the files to an OpenXES log format and the subsequent transformation to the SEWOL log format, the complexity in time and space ends up in O(2n). An own implementation without the OpenXES classes could result in O(n).
 * </p>
 * 
 * @author Adrian Lange
 */
public class XESLogParser extends AbstractLogParser {

	/**
	 * Checks whether the given file can be parsed by the file extension.
         * @param file
         * @return 
	 */
	public boolean canParse(File file) {
		for (XParser parser : XParserRegistry.instance().getAvailable()) {
			if (parser.canParse(file)) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Parses the specified log file path and returns a collection of processes.
	 * 
	 * @param filePath
	 *            Path to file to parse
         * @param parsingMode
	 * @return Collection of processes, which consist of a collection of instances, which again consist of a collection of {@link LogTrace} objects.
	 * @throws ParameterException
	 *             Gets thrown if there's a discrepancy in how the file should be interpreted.
	 * @throws ParserException
	 *             Gets thrown if the file under the given path can't be read, is a directory, or doesn't exist.
	 */
        @Override
	public List<List<LogTrace<LogEntry>>> parse(String filePath, ParsingMode parsingMode) throws ParameterException, ParserException {
		Validate.notNull(filePath);
		return parse(new File(filePath), parsingMode);
	}

	/**
	 * Parses the specified log file and returns a collection of processes.
	 * 
	 * @param inputStream
	 *            {@link InputStream} to parse
         * @param parsingMode
	 * @return Collection of processes, which consist of a collection of instances, which again consist of a collection of {@link LogTrace} objects.
	 * @throws ParameterException
	 *             Gets thrown if there's a discrepancy in how the file should be interpreted.
	 * @throws ParserException
	 *             Gets thrown if the given file can't be read, is a directory, or doesn't exist.
	 */
        @Override
	public List<List<LogTrace<LogEntry>>> parse(InputStream inputStream, ParsingMode parsingMode) throws ParameterException, ParserException {
		try {
			inputStream.available();
		} catch (IOException e) {
			throw new ParameterException("Unable to read input file: " + e.getMessage());
		}

		Collection<XLog> logs = null;
		XParser parser = ParserFileFormat.XES.getParser();
		try {
			logs = parser.parse(inputStream);
		} catch (Exception e) {
			throw new ParserException("Exception while parsing with OpenXES: " + e.getMessage());
		}
		if (logs == null)
			throw new ParserException("No suitable parser could have been found!");

		parsedLogFiles = new ArrayList<>(logs.size());
		Set<List<String>> activitySequencesSet = new HashSet<>();
		Set<LogTrace<LogEntry>> traceSet = new HashSet<>();
		for (XLog log : logs) {
			activitySequencesSet.clear();
			traceSet.clear();
			Class<?> logEntryClass = null;
			List<LogTrace<LogEntry>> logTraces = new ArrayList<>();
			if (containsDataUsageExtension(log)) {
				logEntryClass = DULogEntry.class;
			} else {
				logEntryClass = LogEntry.class;
			}
			for (XTrace trace : log) {
				Integer traceID = null;

				// Extract trace ID
				for (Map.Entry<String, XAttribute> attribute : trace.getAttributes().entrySet()) {
					String key = attribute.getKey();
					String value = attribute.getValue().toString();
					if (key.equals("concept:name")) {
						try {
							traceID = Integer.parseInt(value);
						} catch (NumberFormatException e) {
							// if NAN, take the hash
							traceID = value.hashCode();
						}
						if (traceID < 0) {
							traceID *= Integer.signum(traceID);
						}
					}
				}
				if (traceID == null)
					throw new ParserException("Cannot extract case-id");

				// Build new log trace
				LogTrace<LogEntry> logTrace = new LogTrace<>(traceID);

				// Check for similar instances
				Collection<Long> similarInstances = getSimilarInstances(trace);
				if (similarInstances != null) {
					logTrace.setSimilarInstances(similarInstances);
				}

				for (XEvent event : trace) {
					// Add events to log trace
					logTrace.addEntry(buildLogEntry(event, logEntryClass));
				}
				
				switch(parsingMode){
				case DISTINCT_ACTIVITY_SEQUENCES:
					if(!activitySequencesSet.add(logTrace.getActivities()))
						break;
					logTrace.reduceToActivities();
//				case DISTINCT_TRACES:
//					if(!traceSet.add(logTrace))
//						break;
				case COMPLETE:
					logTraces.add(logTrace);
				}
				
			}
			parsedLogFiles.add(logTraces);
			summaries.add(new LogSummary<>(logTraces));
		}

		return parsedLogFiles;
	}

	/**
	 * Parses the specified log file and returns a collection of processes.
	 * 
	 * @param file
	 *            File to parse
         * @param parsingMode
	 * @return Collection of processes, which consist of a collection of instances, which again consist of a collection of {@link LogTrace} objects.
	 * @throws ParameterException
	 *             Gets thrown if there's a discrepancy in how the file should be interpreted.
	 * @throws ParserException
	 *             Gets thrown if the given file can't be read, is a directory, or doesn't exist.
	 */
	@Override
	public List<List<LogTrace<LogEntry>>> parse(File file, ParsingMode parsingMode) throws ParameterException, ParserException {
		Validate.noDirectory(file);
		if (!file.canRead())
			throw new ParameterException("Unable to read input file!");

		try {
			try {
				InputStream is = new FileInputStream(file);
				return parse(is, parsingMode);
			} catch (FileNotFoundException | ParameterException | ParserException e) {
				throw new ParserException("Exception while parsing with OpenXES: " + e.getMessage());
			}
		} catch (Exception e) {
			throw new ParserException("Error while parsing log with OpenXES-Parser: " + e.getMessage());
		}
	}

	/**
	 * Checks if the extension list contains the {@link XExtension} with the name <i>AttributeDataUsage</i>.
	 */
	private boolean containsDataUsageExtension(XLog log) {
		for (XExtension extension : log.getExtensions()) {
			if (extension.getName().equals("AttributeDataUsage"))
				return true;
		}
		return false;
	}

	private LogEntry buildLogEntry(XEvent xesEvent, Class<?> logEntryClass) throws ParserException, ParameterException {
		LogEntry logEntry;
		try {
			logEntry = (LogEntry) logEntryClass.newInstance();
		} catch (InstantiationException | IllegalAccessException e) {
			throw new ParameterException("Cannot instantiate log entry class: " + e.getMessage());
		}
		for (Map.Entry<String, XAttribute> attribute : xesEvent.getAttributes().entrySet()) {
			String key = attribute.getKey();
                        switch (key) {
                                case "concept:name":
                                        addName(logEntry, attribute.getValue().toString());
                                        break;
                                case "org:resource":
                                        addOriginator(logEntry, attribute.getValue().toString());
                                        break;
                                case "Role":
                                        addRole(logEntry, attribute.getValue().toString());
                                        break;
                                case "lifecycle:transition":
                                        addEventType(logEntry, attribute.getValue().toString());
                                        break;
                                case "time:timestamp":
                                        addTimestamp(logEntry, attribute.getValue().toString());
                                        break;
                                default:
                                        // If the key is unknown, a meta attribute or a data attribute with the key/value pair is added
                                        if (attribute.getValue().getAttributes().containsKey("dataUsage:usage")) {
                                                addDataUsage(logEntry, attribute);
                                        } else {
                                                addMetaInformation(logEntry, attribute);
                                        }       break;
                        }
		}
		return logEntry;
	}

	private void addName(LogEntry entry, String value) throws ParserException {
		if (value == null || value.isEmpty())
			throw new ParserException("No value for concept:name");
		try {
			entry.setActivity(value);
		} catch (Exception e) {
			throw new ParserException("Cannot set activity of log entry: " + e.getMessage());
		}
	}

	private void addOriginator(LogEntry entry, String value) throws ParserException {
		if (value == null || value.isEmpty())
			throw new ParserException("No value for org:resource");
		try {
			if (entry.getOriginator() == null || !entry.getOriginator().equals(value))
				entry.setOriginator(value);
			entry.setOriginator(value);
		} catch (Exception e) {
			throw new ParserException("Cannot set originator of log entry: " + e.getMessage());
		}
	}
	
	private void addRole(LogEntry entry, String value) throws ParserException {
		if (value == null || value.isEmpty())
			throw new ParserException("No value for Role");
		try {
			if (entry.getRole() == null || !entry.getRole().equals(value))
				entry.setRole(value);
			entry.setRole(value);
		} catch (LockingException e) {
			throw new ParserException("Cannot set role of log entry: " + e.getMessage());
		}
	}

	private void addEventType(LogEntry entry, String value) throws ParserException {
		if (value == null || value.isEmpty())
			throw new ParserException("No value for lifecycle:transition");
		EventType eventType = EventType.parse(value);
		if (eventType == null)
			throw new ParserException("Cannot parse event type: " + eventType);
		try {
			entry.setEventType(eventType);
		} catch (Exception e) {
			throw new ParserException("Cannot set event type of log entry: " + e.getMessage());
		}
	}

	private void addTimestamp(LogEntry entry, String value) throws ParserException {
		if (value == null || value.isEmpty())
			throw new ParserException("No value for time:timestamp");
		Date date = null;
		String sanitizedDateString = value.replaceAll(":(\\d\\d)$", "$1");
		for (ParserDateFormat pdf : ParserDateFormat.values()) {
			if (date == null) {
				try {
					date = ParserDateFormat.getDateFormat(pdf).parse(sanitizedDateString);
				} catch (ParseException e) {
					// is allowed to happen
				} catch (ParameterException e) {
					// cannot happen.
					throw new RuntimeException(e);
				}
			}
		}
		if (date == null)
			throw new ParserException("Cannot read timestamp.");

		try {
			entry.setTimestamp(date);
		} catch (Exception e) {
			throw new ParserException("Cannot set log entry timestamp: " + e.getMessage());
		}
	}

	private void addDataUsage(LogEntry entry, Map.Entry<String, XAttribute> attribute) throws ParserException, ParameterException {
		if (!(entry instanceof DULogEntry))
			throw new ParameterException("Cannot add data usage to log entry of type " + entry.getClass().getSimpleName());
		
		String dataAttributeKey = attribute.getKey();
		Object dataAttributeValue = parseAttributeValue(attribute.getValue());
		DataAttribute dataAttribute = new DataAttribute(dataAttributeKey, dataAttributeValue);

		// Get sub-attributes
		for (Map.Entry<String, XAttribute> subattribute : attribute.getValue().getAttributes().entrySet()) {
			String dataAttributeDataUsageString = null;
			if (subattribute.getKey().equals("dataUsage:usage")) {
				dataAttributeDataUsageString = subattribute.getValue().toString();
				List<DataUsage> dataUsageList = parseDataUsageString(dataAttributeDataUsageString);
				for (DataUsage dataUsage : dataUsageList) {
					try {
						((DULogEntry) entry).addDataUsage(dataAttribute, dataUsage);
					} catch (ParameterException | LockingException e) {
						throw new ParserException("Cannot add data usage information to log entry: " + e.getMessage());
					}
				}
			}
		}
	}

	private void addMetaInformation(LogEntry entry, Map.Entry<String, XAttribute> attribute) throws ParserException {
		entry.addMetaAttribute(new DataAttribute(attribute.getKey(), attribute.getValue()));
	}

	private Collection<Long> getSimilarInstances(XTrace trace) throws ParserException {
		// Check for similar instances
		Integer numSimilarInstances = null;
		String groupedIdentifiers = null;
		for (Entry<String, XAttribute> v : trace.getAttributes().entrySet()) {
			if (v.getKey().toLowerCase().equals("numSimilarInstances".toLowerCase())) {
				try {
					numSimilarInstances = Integer.parseInt(v.getValue().toString().trim());
				} catch (NumberFormatException e) {
					throw new ParserException("The value of \"numSimilarInstances\" is not of the type integer: " + v.getValue().toString() + ": " + e.getMessage());
				}
			}
			if (v.getKey().toLowerCase().equals("GroupedIdentifiers".toLowerCase())) {
				groupedIdentifiers = v.getValue().toString();
			}
		}
		if (numSimilarInstances != null && groupedIdentifiers != null) {
			String[] groupedIdentifiersSplitted = groupedIdentifiers.trim().split("\\s*,\\s*");

			if (groupedIdentifiersSplitted.length != numSimilarInstances)
				System.err.println("The amount of similar instances differ in \"numSimilarInstances\" and \"GroupedIdentifiers\".");

			Collection<Long> groupedIdentifiersArray = new ArrayList<>(groupedIdentifiersSplitted.length);
                        for (String groupedIdentifiersSplitted1 : groupedIdentifiersSplitted) {
                                try {
                                        groupedIdentifiersArray.add(Long.parseLong(groupedIdentifiersSplitted1.trim()));
                                } catch (NumberFormatException e) {
                                        throw new ParserException("The given identifier \"" + groupedIdentifiersSplitted1 + "\" is not of the integer type: " + e.getMessage());
                                }
                        }
			if (groupedIdentifiersArray.size() > 0)
				return groupedIdentifiersArray;
		}
		return null;
	}

	/**
	 * Tries to parse the value of a {@link XAttribute} to a numeric, boolean, or string value.
	 */
	private Object parseAttributeValue(XAttribute xAttribute) {
		String attributeString = xAttribute.toString();

		// TODO better solution?

		// All numeric values as double
		try {
			double a = Double.parseDouble(attributeString);
			return a;
		} catch (NumberFormatException e) {
			// Ignore
		}
		// Boolean
		if (attributeString.trim().toLowerCase().equals("true"))
			return true;
		if (attributeString.trim().toLowerCase().equals("false"))
			return false;
		// String
		return attributeString;
	}

	/**
	 * Takes a String containing {@link DataUsage} identifier separated by commas, removes every leading and training whitespace, and parses them into a {@link List}. <br>
	 * TODO move to TOVAL into enum {@link DataUsage}?
	 */
	private static List<DataUsage> parseDataUsageString(String dataUsageString) throws ParameterException {
		List<String> dataUsageStrings = Arrays.asList(dataUsageString.split("\\s*,\\s*"));
		List<DataUsage> dataUsageList = new ArrayList<>(dataUsageStrings.size());
		for (String d : dataUsageStrings) {
			DataUsage dataUsage = DataUsage.parse(d);
			if (!dataUsageList.contains(dataUsage))
				dataUsageList.add(dataUsage);
		}
		return dataUsageList;
	}

	public static void main(String[] args) throws ParameterException, ParserException {
                XESLogParser p = new XESLogParser();
		List<List<LogTrace<LogEntry>>> l = p.parse("/home/alange/P2P-log-v6-anonymized.xes", ParsingMode.COMPLETE);
	}
}
