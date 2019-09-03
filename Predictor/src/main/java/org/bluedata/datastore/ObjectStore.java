package org.bluedata.datastore;

import java.util.Map;
import java.util.HashMap;


public abstract class ObjectStore {
	
	
	
	public abstract <V> void put(String id, V value) throws Exception;
	

	public abstract <V> V get(String id) throws Exception;
	
	
	
	 public abstract static class Builder {
	
		 protected Map<String,Object> dictionary = new HashMap<>();
		 
		 
		 public Object get(String key) {
			 return dictionary.get(key.toLowerCase());
		 }
		 
		 public Object put(String key, Object value) {
			 return dictionary.put(key,value);
		 }
		 
		 
		 public ObjectStore.Builder with(String key, Object value) {
			 this.dictionary.put(key.toLowerCase(),value);
			 return this;
		 }
		 
		 public abstract ObjectStore build();
	 } 
	 
}
