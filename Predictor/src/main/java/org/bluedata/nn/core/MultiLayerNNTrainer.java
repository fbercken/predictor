package org.bluedata.nn.core;


import org.deeplearning4j.eval.Evaluation;
import org.deeplearning4j.nn.api.Layer;
import org.deeplearning4j.nn.multilayer.MultiLayerNetwork;
import org.deeplearning4j.optimize.api.TrainingListener;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.dataset.api.iterator.DataSetIterator;
import org.nd4j.linalg.dataset.api.preprocessor.DataNormalization;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.nd4j.linalg.dataset.DataSet;
import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;


public class MultiLayerNNTrainer {

    private static final Logger log = LoggerFactory.getLogger(MultiLayerNNTrainer.class);

    private int epochs;
    private boolean useRNN;
    private String modelPath;
    private String normalizerPath;
    private Evaluation evaluation;
    private MultiLayerNetwork model;
    private DataNormalization normalizer;
    private Collection<TrainingListener> listeners;



    public void init(Map<String,Object> data) {

        modelPath = (String) data.get(Utils.ATTR_MODEL_PATH);
        normalizerPath = (String) data.get(Utils.ATTR_NORMALIZER_PATH);
        epochs = (Integer) data.getOrDefault(Utils.ATTR_NUM_EPOCHS, DEFAULT_NUM_EPOCH);


        evaluation = new Evaluation();
        model = (MultiLayerNetwork) data.get(Utils.ATTR_MULTILAYER);
        normalizer = (DataNormalization) data.get(Utils.ATTR_NORMILIZER);
        listeners = (Collection<TrainingListener>)  data.getOrDefault(Utils.ATTR_LISTENERS, Collections.EMPTY_LIST);

        useRNN = isRNNModel();
        model.setListeners(listeners);
    }


    public void normalize(DataSetIterator iterator) {

        if ( normalizer != null) {
            normalizer.fit(iterator);
            iterator.setPreProcessor(normalizer);
            iterator.reset();
        }
    }


    public void train( DataSetIterator iterator) {

        log.info("Train Model");
        for( int epoch=0; epoch<epochs; epoch++) {
            while ( iterator.hasNext() ) {
                DataSet dataset = iterator.next();
                model.fit(dataset);
                if ( evaluation != null ) evaluate(dataset);
            }
            iterator.reset();
            model.rnnClearPreviousState();
        }
    }


    public void evaluate(DataSetIterator iterator) {

        log.info("Evaluate Model");
        iterator.setPreProcessor(normalizer);
        Evaluation eval = model.evaluate(iterator);
        log.info(eval.stats());
        iterator.reset();
    }


    public void evaluate(DataSet dataset) {
        INDArray features = dataset.getFeatures();
        INDArray labels = dataset.getLabels();
        INDArray featuresMask = dataset.getFeaturesMaskArray();
        INDArray labelsMask = dataset.getLabelsMaskArray();
        INDArray output =  model.output(features, true, featuresMask, labelsMask);

        if (useRNN) {
            evaluation.evalTimeSeries(labels, output, labelsMask);
        } else {
            evaluation.eval(labels,output,labelsMask);
        }
    }


    public void save( ) {

        log.info("Save Model and Normalizer");
        try {
            if ( modelPath != null ) Utils.saveModel( model, modelPath);
            if ( normalizer != null && normalizerPath != null) Utils.saveNormalizer(normalizer, normalizerPath);
        } catch( IOException e) {
            log.error("Unable to save NN model");
        }
    }

    public boolean isRNNModel() {
        Layer[] layers = model.getLayers();
        for( Layer layer : layers) {
            if ( layer.type() == Layer.Type.RECURRENT ) return true;
        }
        return false;
    }

    public final static int DEFAULT_NUM_EPOCH = 1;

}
