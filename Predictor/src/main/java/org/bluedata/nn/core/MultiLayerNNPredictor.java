package org.bluedata.nn.core;


import org.bluedata.datastore.cassandra.CassandraObjectStore;
import org.deeplearning4j.nn.api.Layer;

import org.deeplearning4j.nn.multilayer.MultiLayerNetwork;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.dataset.api.preprocessor.DataNormalization;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.deeplearning4j.nn.layers.recurrent.LSTM;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class MultiLayerNNPredictor {

    private static final Logger log = LoggerFactory.getLogger(MultiLayerNNPredictor.class);

    private String modelUrl;
    private String stateStore;
    private String stateTable;
    private Boolean loadUpdater;
    private String normalizerUrl;
    private Boolean isRevertLabel;
    private MultiLayerNetwork model;
    private DataNormalization normalizer;
    private List<Integer> rnnIndexLayers;
    private org.bluedata.datastore.ObjectStore stateStores;



    public void init(Map<String,Object> data) {

        try {
            this.modelUrl = (String) data.get(Utils.ATTR_MODEL_PATH);
            this.stateStore =  (String) data.get(Utils.ATTR_STATE_STORE);
            this.stateTable =  (String) data.get(Utils.ATTR_STATE_TABLE);
            this.normalizerUrl = (String) data.get(Utils.ATTR_NORMALIZER_PATH);
            this.loadUpdater = (Boolean) data.getOrDefault(Utils.ATTR_LOAD_UPDATER, DEFAULT_LOADUPDATER);
            

            this.model = Utils.loadModel(modelUrl, loadUpdater);
            this.normalizer = ( !"".equals(normalizerUrl) ) ? Utils.loadNormalizer(normalizerUrl) : null;
            this.isRevertLabel = (normalizer != null && normalizer.isFitLabel()) ? true : false;
            this.rnnIndexLayers = getRnnIndexLayers(model);
            
            this.stateStores = new CassandraObjectStore.Builder()
            	.with("ContactPoints", this.stateStore)
            	.with("TableName", this.stateTable)
                .build();

        } catch( Exception e) {
            log.error( "Predictor: %s" , e );
        }
    }


    public int[] predict(INDArray value) {
        if ( normalizer != null ) normalizer.transform(value);
        return model.predict(value);
    }


    public INDArray output(INDArray value) {

        if ( normalizer != null ) normalizer.transformLabel(value);
        INDArray result = model.output(value);
        if ( isRevertLabel ) normalizer.revertLabels(result);

        return result;
    }


    public INDArray rnnTimeStep(INDArray value) {

        if ( normalizer != null ) normalizer.transform(value);
        INDArray result = model.rnnTimeStep(value);
        if ( isRevertLabel ) normalizer.revertLabels(result);
        return result;
    }


    public INDArray rnnTimeStep(String uuid, INDArray value) throws Exception {

        if ( normalizer != null ) normalizer.transform(value);
        restoreState(uuid);
        INDArray result = model.rnnTimeStep(value);
        saveState(uuid);
        if ( isRevertLabel ) normalizer.revertLabels(result);
        return result;
    }


    private void restoreState(String uuid)  throws Exception {

        Map<Integer,Map<String,INDArray>> state = stateStores.get(uuid);

        if ( state == null ) {
            model.rnnClearPreviousState();
        } else {
            for( Map.Entry<Integer,Map<String,INDArray>> entry : state.entrySet()) {
                model.rnnSetPreviousState(entry.getKey(), entry.getValue());
            }
        }
    }


    private void saveState(String uuid) throws Exception {

        Map<Integer,Map<String,INDArray>> state = new HashMap<>();
        for(int index: rnnIndexLayers) state.put( index, model.rnnGetPreviousState(index));
        stateStores.put(uuid,state);
    }


    private List<Integer> getRnnIndexLayers(MultiLayerNetwork model) {
        List<Integer> result = new ArrayList<>();
        Layer[] layers = model.getLayers();
        for( Layer layer : layers) {
            if ( layer instanceof LSTM) {
                result.add(layer.getIndex());
            }
        }
        return result;
    }


    public final static Boolean DEFAULT_LOADUPDATER = true;

}
