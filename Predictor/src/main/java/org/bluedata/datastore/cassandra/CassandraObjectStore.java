package org.bluedata.datastore.cassandra;


import java.io.*;
import java.nio.ByteBuffer;

import org.bluedata.datastore.ObjectStore;

import com.datastax.driver.core.BoundStatement;
import com.datastax.driver.core.PreparedStatement;
import com.datastax.driver.core.Row;
import com.datastax.driver.dse.DseCluster;
import com.datastax.driver.dse.DseSession;


public class CassandraObjectStore extends ObjectStore  {


    private String tableName;
    private DseCluster cluster;
    private DseSession session;
    private PreparedStatement writeStatement;
    private PreparedStatement readStatement;


    protected CassandraObjectStore(Builder builder) {

        this.cluster = DseCluster.builder()
                .addContactPoint( (String) builder.get("addresses"))
                .withPort( (Integer) builder.get("port"))
                .build();

        this.session = cluster.connect();
        this.tableName = (String) builder.get("tableName");

        session.execute(String.format(CREATE_STATEMENT,tableName));
        this.readStatement = session.prepare(String.format(READ_STATEMENT,tableName));
        this.writeStatement = session.prepare(String.format(WRITE_STATEMENT,tableName));
    }



    public <V> void put(String id, V value) throws Exception {

        ByteArrayOutputStream data = new ByteArrayOutputStream();
        ObjectOutputStream out = new ObjectOutputStream(data);
        out.writeObject(value);
        BoundStatement bound = writeStatement.bind(id,ByteBuffer.wrap(data.toByteArray()));
        session.execute(bound);
    }


    public <V> V get(String id) throws Exception {

        BoundStatement bound = readStatement.bind(id);
        Row row = session.execute(bound).one();
        if ( row == null ) {
            return null;
        } else {
            ByteArrayInputStream data = new ByteArrayInputStream(row.getBytes(0).array());
            ObjectInputStream in = new ObjectInputStream(data);
            return (V) in.readObject();
        }
    }


    public static class Builder extends ObjectStore.Builder {
        
        
        public Builder() {
        	this.put("port", 9042);
        	this.put("addresses", "127.0.0.1");
        	this.put("tablename", "objectStore");
        }


        public ObjectStore build() {
            return new CassandraObjectStore(this);
        }

    }


    public static String CREATE_STATEMENT = "CREATE TABLE IF NOT EXISTS %s (id text, data blob, PRIMARY KEY(id))";
    public static String READ_STATEMENT = "SELECT data FROM %s where id = ?";
    public static String WRITE_STATEMENT = "INSERT INTO %s (id, data) VALUES (?,?)";

}
