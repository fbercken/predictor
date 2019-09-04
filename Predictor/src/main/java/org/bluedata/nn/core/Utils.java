package org.bluedata.nn.core;


import java.io.*;
import java.net.URL;
import java.net.URLConnection;
import java.net.MalformedURLException;
import org.deeplearning4j.nn.multilayer.MultiLayerNetwork;
import org.deeplearning4j.util.ModelSerializer;
import org.nd4j.linalg.dataset.api.preprocessor.DataNormalization;
import org.nd4j.linalg.dataset.api.preprocessor.serializer.NormalizerSerializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;



public class Utils {

    private static final Logger log = LoggerFactory.getLogger(Utils.class);


    public static void saveModel( MultiLayerNetwork model, String path) throws IOException {

        log.info("Save Model");
        boolean saveUpdater = true;

        URL url = new URL(path);
        URLConnection connection = url.openConnection();
        connection.setDoOutput(true);
        OutputStream out = connection.getOutputStream();

      //  OutputStream out = new FileOutputStream(new File(path));
        ModelSerializer.writeModel( model, out, saveUpdater);
        out.close();
    }


    public static MultiLayerNetwork loadModel(String path, boolean loadUpdater) throws Exception {

        try ( InputStream inputStream = (new URL(path)).openStream() ) {
            return ModelSerializer.restoreMultiLayerNetwork( inputStream, loadUpdater);

        } catch(MalformedURLException e) {
            log.error("Malformed deepLearning URL '{}'", path);
        } catch (IOException e) {
            log.error("Unable to read DeepLearning Model '{}'", path);
        }
        throw new Exception( "Unable to load Model " + path);
    }


    public static void saveNormalizer( DataNormalization normalizer, String path) throws IOException {

        log.info("Save Normalizer");
        
        URL url = new URL(path);
        URLConnection connection = url.openConnection();
        connection.setDoOutput(true);
        OutputStream out = connection.getOutputStream();
        
       // OutputStream out = new FileOutputStream(new File(path));
        NormalizerSerializer serializer = NormalizerSerializer.getDefault();
        serializer.write(normalizer, out);
    }


    public static DataNormalization loadNormalizer(String url) throws Exception {

        try ( InputStream inputStream = (new URL(url)).openStream() ) {
            NormalizerSerializer serializer = NormalizerSerializer.getDefault();
            return serializer.restore(inputStream);

        } catch(MalformedURLException e) {
            log.error("Malformed deepLearning URL '{}'", url);
        } catch (Exception e) {
            log.error("Unable to read DeepLearning Normalizer '{}'", url);
        }
        
        throw new Exception( "Unable to load normalizer " + url);
    }




    public final static String ATTR_MODEL_PATH = "modelUrl";
    public final static String ATTR_LOAD_UPDATER = "loadUpdater";
    public final static String ATTR_NORMALIZER_PATH = "normalizerUrl";
    public final static String ATTR_NORMILIZER = "normalizer";
    public final static String ATTR_NUM_EPOCHS = "numEpochs";
    public final static String ATTR_FRACTION_TRAIN = "fractionTrain";
    public final static String ATTR_LISTENERS = "listeners";
    public final static String ATTR_MULTILAYER = "multiLayerModel";
}
