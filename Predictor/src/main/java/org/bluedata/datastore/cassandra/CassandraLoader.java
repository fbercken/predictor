package org.bluedata.datastore.cassandra;


import com.datastax.driver.core.ProtocolVersion;
import com.datastax.driver.dse.DseCluster;
import com.datastax.driver.dse.DseSession;
import com.datastax.driver.mapping.Mapper;
import com.datastax.driver.mapping.MappingManager;


public class CassandraLoader<K,V> {

    private DseCluster cluster;
    private DseSession session;
    private MappingManager manager;
    private Mapper<V> mapper;


    protected <V>CassandraLoader(Builder builder) {

        this.cluster = DseCluster.builder()
                .addContactPoint(builder.addresses)
                .withClusterName(builder.name)
                .withPort(builder.port)
            //    .withProtocolVersion(builder.protocolVersion)
                .build();

        this.session = cluster.connect();
        this.manager = new MappingManager(session);
        this.mapper = manager.mapper( builder.clazz);
    }

    public V get(Object... key) {
        return mapper.get(key);
    }

    public void delete(Object... key) {
        mapper.delete(key);
    }

    public void save(V value) {
       mapper.save(value);
    }



    public static class Builder {

    	private Class clazz;
        private int port = 9042;
        private String name = "default";
        private String addresses;
        private ProtocolVersion protocolVersion = ProtocolVersion.V2;
        
  


        public Builder withName(String name) {
            this.name = name;
            return this;
        }

        public Builder withPort(int port) {
            this.port = port;
            return this;
        }

        public Builder withContactPoints(String addresses) {
            this.addresses = addresses;
            return this;
        }
        
        public Builder withProtocolVersion(ProtocolVersion protocolVersion) {
            this.protocolVersion = protocolVersion;
            return this;
        }

        public Builder withClass(Class<?> clazz) {
            this.clazz = clazz;
            return this;
        }

        @SuppressWarnings("rawtypes")
		public CassandraLoader build() {
            return new CassandraLoader(this);
        }

    }

}
