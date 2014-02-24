package de.uni.freiburg.iig.telematik.jawl.log;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import de.invation.code.toval.validate.ParameterException;
import de.invation.code.toval.validate.Validate;

public class LogTrace<E extends LogEntry> {
	
	private List<E> logEntries = new ArrayList<E>();
	private int caseNumber = -1;
	private Set<Integer> similarInstances = new HashSet<Integer>();
	
	public LogTrace(){}
	
	public LogTrace(Integer caseNumber) throws ParameterException{
		Validate.notNegative(caseNumber);
		this.caseNumber = caseNumber;
	}
	
	public boolean addEntry(E entry){
		if(entry != null){
			return logEntries.add(entry);
		}
		return false;
	}
	
	public List<E> getEntries(){
		return Collections.unmodifiableList(logEntries);
	}
	
	public List<E> getEntriesForActivity(String activity) throws ParameterException{
		Validate.notNull(activity);
		List<E> result = new ArrayList<E>();
		for(E entry: logEntries){
			if(entry.getActivity().equals(activity)){
				result.add(entry);
			}
		}
		return result;
	}
	
	/**
	 * Returns all log entries of this trace whose activities are in the given activity set.
	 * @param activities
	 * @return
	 * @throws ParameterException 
	 */
	public List<E> getEntriesForActivities(Set<String> activities) throws ParameterException{
		Validate.noNullElements(activities);
		List<E> result = new ArrayList<E>();
		for(E entry: logEntries)
			if(activities.contains(entry.getActivity()))
				result.add(entry);
		return result;
	}
	
	public List<E> getEntriesForGroup(String groupID) throws ParameterException{
		Validate.notNull(groupID);
		List<E> result = new ArrayList<E>();
		for(E entry: logEntries)
			if(entry.getGroup().equals(groupID))
				result.add(entry);
		return result;
	}
	
	public List<E> getFirstKEntries(Integer k) throws ParameterException{
		Validate.notNegative(k);
		if(k>size())
			throw new ParameterException("Trace does only contain "+size()+" entries!");
		List<E> result = new ArrayList<E>();
		if(k==0)
			return result;
		for(int i=0; i<k; i++)
			result.add(logEntries.get(i));
		return result;
	}
	
	public List<E> getSucceedingEntries(E entry) throws ParameterException{
		Validate.notNull(entry);
		List<E> result = new ArrayList<E>();
		Integer index = null;
		for(E traceEntry: logEntries){
			if(traceEntry == entry){
				index = logEntries.indexOf(traceEntry);
				break;
			}
		}
		if(index != null && index < logEntries.size()-1){
			for(int i=index+1; i<logEntries.size(); i++){
				result.add(logEntries.get(i));
			}
		}
		return result;
	}
	
	public E getDirectSuccessor(E entry) throws ParameterException{
		Validate.notNull(entry);
		Integer index = null;
		for(E traceEntry: logEntries){
			if(traceEntry == entry){
				index = logEntries.indexOf(traceEntry);
				break;
			}
		}
		if(index != null && index < logEntries.size()-1){
			return logEntries.get(index + 1);
		}
		return null;
	}
	
	public List<E> getPreceedingEntries(E entry) throws ParameterException{
		Validate.notNull(entry);
		List<E> result = new ArrayList<E>();
		Integer index = null;
		for(E traceEntry: logEntries){
			if(traceEntry == entry){
				index = logEntries.indexOf(traceEntry);
				break;
			}
		}
		if(index != null && index > 0){
			for(int i=0; i<index; i++){
				result.add(logEntries.get(i));
			}
		}
		return result;
	}
	
	public E getDirectPredecessor(E entry) throws ParameterException{
		Validate.notNull(entry);
		Integer index = null;
		for(E traceEntry: logEntries){
			if(traceEntry == entry){
				index = logEntries.indexOf(traceEntry);
				break;
			}
		}
		if(index != null && index > 0){
			return logEntries.get(index - 1);
		}
		return null;
	}
	
	public boolean removeEntry(E entry){
		return logEntries.remove(entry);
	}
	
	public boolean removeAllEntries(Collection<E> entries) throws ParameterException{
		Validate.noNullElements(entries);
		boolean entriesChanged = false;
		for(E entry: entries){
			if(removeEntry(entry))
				entriesChanged = true;
		}
		return entriesChanged;
	}
	
	public void setCaseNumber(int caseNumber) throws ParameterException{
		Validate.notNull(caseNumber);
		this.caseNumber = caseNumber;
	}
	
	public int getCaseNumber(){
		return caseNumber;
	}
	
	public int size(){
		return logEntries.size();
	}
	
	public Set<Integer> getSimilarInstances() {
		return Collections.unmodifiableSet(similarInstances);
	}
	
	public int getNumberOfSimilarInstances(){
		return similarInstances.size();
	}
	
	public void addSimilarInstance(Integer similarInstance) throws ParameterException {
		Validate.notNull(similarInstance);
		this.similarInstances.add(similarInstance);
	}

	public void setSimilarInstances(Collection<Integer> similarInstances) throws ParameterException {
		Validate.notNull(similarInstances);
		this.similarInstances.clear();
		this.similarInstances.addAll(similarInstances);
	}

	public boolean containsActivity(String activity){
		return getDistinctActivities().contains(activity);
	}
	
	public List<String> getActivities(){
		List<String> result = new ArrayList<String>();
		for(E entry: logEntries){
			result.add(entry.getActivity());
		}
		return result;
	}
	
	public Set<String> getDistinctActivities(){
		Set<String> result = new HashSet<String>();
		for(E entry: logEntries)
			result.add(entry.getActivity());
		return result;
	}
	
	public Set<String> getDistinctOriginators(){
		Set<String> result = new HashSet<String>();
		for(E entry: logEntries)
			result.add(entry.getOriginator());
		return result;
	}
	
	public void sort(){
		Collections.sort(logEntries);
	}
	
	@Override
	public String toString(){
		//TODO: Format
		return logEntries.toString();
	}
}
