package com.test;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.JsonNode;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Properties;
import java.util.function.Consumer;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;



public class MessageConsumer<K,V> {

	
private static final Logger log = LoggerFactory.getLogger(MessageConsumer.class);
	
	private final String TOPIC;
	private final int POLLING;
	private final Consumer<ConsumerRecord<K,V>> RECORDCONSUMER;
	
	
	public MessageConsumer(String topicName, int polling, Consumer<ConsumerRecord<K,V>> recordConsumer) throws IOException {
		 
    	this.TOPIC = topicName;
    	this.POLLING = polling;
    	this.RECORDCONSUMER = recordConsumer;
	}
	
	
	public void run() {
		
		KafkaConsumer<K,V> consumer = null;
		
		try (InputStream props = ClassLoader.class.getResourceAsStream("/consumer.props")) {
			
			Properties properties = new Properties();
            properties.load(props);
			consumer = new KafkaConsumer<>(properties);
			consumer.subscribe( Arrays.asList(TOPIC));
			ConsumerRecords<K,V> records;
		
			while (true) {
	
	           records = consumer.poll(POLLING);
	
	           for (ConsumerRecord<K,V> record : records) {
	           		this.RECORDCONSUMER.accept(record);
	           }
			}
           
		} catch( IOException e) {
			e.printStackTrace();
		} finally {
			if ( consumer != null) {
				consumer.close();
			}
		}
		
		
	}
	
	public static void main(String[] args) throws IOException {
		
		MessageConsumer<String,String> consumer =  new MessageConsumer<>(
			"/eventstream:eventtopic",
			100,
			record -> System.out.println( String.format("%s", record.value()) )
		);
		
		consumer.run();
	}

}
