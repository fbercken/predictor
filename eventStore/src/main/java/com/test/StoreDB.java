package com.test;


import org.ojai.Document;
import org.ojai.DocumentStream;
import org.ojai.store.Connection;
import org.ojai.store.DocumentStore;
import org.ojai.store.DriverManager;
import org.ojai.store.Query;



public class StoreDB {
	
	private final String storeName;
	private final DocumentStore store;
	private final Connection connection;
	
	
	public StoreDB(String storeName) {
		
		this.storeName = storeName;
		this.connection = DriverManager.getConnection("ojai:mapr:");

		if ( connection.storeExists(storeName) ) {
			this.store = connection.getStore(this.storeName);
		} else {
			this.store = connection.createStore(this.storeName);
		}
	}

	
	public void store(String key, String json) {		
		Document doc = connection.newDocument(json);
		this.store.insertOrReplace(key,doc);
	}
	
	public String findById(String key) {
		return this.store.findById(key).asJsonString();
	}
	
	public DocumentStream query(String criteria) {
		Query query = connection.newQuery(criteria);
		return this.store.find(query);
	}
	
	
	public static void main(String[] args) {
		
		StoreDB storedDB = new StoreDB("/user/mapr/events");
			
		
		storedDB.store("1",  "{ \"name\": \"fred\"}" );;
		System.out.println(storedDB.findById("1"));
		
		System.out.println("end");

	}

}
