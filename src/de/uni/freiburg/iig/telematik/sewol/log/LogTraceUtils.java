package de.uni.freiburg.iig.telematik.sewol.log;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import de.invation.code.toval.validate.ParameterException;
import de.invation.code.toval.validate.Validate;

public class LogTraceUtils {

        private static final String traceFormat = "trace%s: %s";

        public static LogTrace<LogEntry> createTraceFromActivities(long caseNumber, String... activities) {
                return createTraceFromActivities(caseNumber, LogEntry.class, activities);
        }

        public static <E extends LogEntry> LogTrace<E> createTraceFromActivities(long caseNumber, Class<E> entryClass, String... activities) {
                return createTraceFromActivities(caseNumber, entryClass, Arrays.asList(activities));
        }

        public static LogTrace<LogEntry> createTraceFromActivities(long caseNumber, List<String> activities) {
                return createTraceFromActivities(caseNumber, LogEntry.class, activities);
        }

        public static <E extends LogEntry> LogTrace<E> createTraceFromActivities(long caseNumber, Class<E> entryClass, List<String> activities) {
                Validate.notNegative(caseNumber);
                Validate.notNull(entryClass);
                try {
                        entryClass.newInstance();
                } catch (InstantiationException | IllegalAccessException e) {
                        throw new ParameterException("Cannot instantiate entry class: " + entryClass.getSimpleName());
                }
                Validate.notNull(activities);
                Validate.notEmpty(activities);
                Validate.noNullElements(activities);

                LogTrace<E> result = new LogTrace<>(caseNumber);
                for (String activity : activities) {
                        E newEntry = null;
                        try {
                                newEntry = entryClass.newInstance();
                                newEntry.setActivity(activity);
                        } catch (IllegalAccessException | InstantiationException | LockingException e) {
				// Cannot happen, since we validated this before.
                                // Cannot happen, since the field activity is not locked.
                                throw new RuntimeException(e);
                        }

                        result.addEntry(newEntry);
                }

                return result;
        }

        public static <E extends LogEntry> List<LogTrace<E>> createTraceList(LogTrace<E>... traces) {
                return Arrays.asList(traces);
        }

        public static <E extends LogEntry> List<List<String>> createStringRepresentation(Collection<LogTrace<E>> traceList, boolean onlyDistinctSequences) {
                Set<List<String>> distinctSequences = new HashSet<>();
                List<List<String>> result = new ArrayList<>();
                for (LogTrace<E> trace : traceList) {
                        List<String> activityList = trace.getActivities();
                        if (!onlyDistinctSequences) {
                                result.add(activityList);
                        } else {
                                if (distinctSequences.add(activityList)) {
                                        result.add(activityList);
                                }
                        }
                }
                return result;
        }

        public static <E extends LogEntry> boolean containsTracesWithDuplicates(Collection<LogTrace<E>> traceList) {
                for (LogTrace<E> trace : traceList) {
                        if (trace.containsDuplicateActivities()) {
                                return true;
                        }
                }
                return false;
        }

        public static <E extends LogEntry> void print(Collection<LogTrace<E>> coll) {
                if (coll == null) {
                        throw new NullPointerException();
                }
                if (coll.isEmpty()) {
                        return;
                }
                for (LogTrace<E> t : coll) {
                        System.out.println(String.format(traceFormat, t.getCaseNumber(), t.getActivities()));
                }
        }
}
