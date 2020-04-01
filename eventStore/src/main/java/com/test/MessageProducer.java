package com.test;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import java.util.concurrent.ExecutionException;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import org.apache.kafka.clients.producer.Callback;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;



public class MessageProducer<K,V> {
	
	private static final Logger log = LoggerFactory.getLogger(MessageProducer.class);
	
	private final String TOPIC;
	private final boolean ASYNC;
	private final Function<Object,K> buildKey;
	private final Function<Object,V> buildValue;
	
	
	public MessageProducer(String topicName, Function<Object,K> keyFunction, Function<Object,V> valueFunction, boolean async) {

		this.ASYNC = async;
	    this.TOPIC = topicName;
	    this.buildKey = keyFunction;
	    this.buildValue = valueFunction;
	}
	
	
	public void run(Stream<? extends Object> stream) {
		
		BiConsumer<K,V> consumer;
		KafkaProducer<K,V> producer = null;
		
			
		try (InputStream props = ClassLoader.class.getResourceAsStream("/producer.props")) {
			
			Properties properties = new Properties();
			properties.load(props);
			
			producer = new KafkaProducer<>(properties);
			consumer = selectSender(producer);
						
			stream.forEach( v -> consumer.accept( buildKey.apply(v), buildValue.apply(v)) );
			
		} catch(IOException e) {
			log.error("unable to read properties file%s", e);
			
		} finally {
			if ( producer != null) {
				producer.flush();
				producer.close();
			}
		}
	}
	

	private BiConsumer<K,V> selectSender(final KafkaProducer<K,V> producer) {
		
		return (ASYNC) ?
			
			(key,value) -> {
				long startTime = System.currentTimeMillis();
				producer.send(new ProducerRecord<>(this.TOPIC,key, value), new MyCallback<K,V>(key,value,startTime));
			}
			
		:
			
			(key,value) -> {
				try {
					producer.send(new ProducerRecord<>(this.TOPIC,key,value)).get();		
				} catch (InterruptedException | ExecutionException e) {
					e.printStackTrace();
				}
			}
		;
		
	}
	

	public static void main(String[] args) throws InterruptedException {
		
		MessageProducer<String,String> producer = new MessageProducer<>(
			"/eventstream:eventtopic",
			v -> String.valueOf(v),
			v -> String.format("message %s", v),
			true
		);			

		producer.run( IntStream.range(0,10).boxed() );
			
		Thread.sleep(10000);
		
		
	}
	
	
	
	class MyCallback<K,V> implements Callback {
		
		K key;
		V message;
		long startTime;
		
		MyCallback(K key, V message, long startTime) {
			this.key = key;
			this.message = message;
			this.startTime = startTime;
		}

		@Override
		public void onCompletion(RecordMetadata metadata, Exception exception) {
			System.out.println(String.format("Submitted [%s]: %s", key,message));		
		}		
	}

}
