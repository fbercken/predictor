package org.bluedata.iterators;


import java.util.List;
import java.util.ArrayList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.bluedata.nn.core.Pair;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.dataset.DataSet;
import org.nd4j.linalg.dataset.api.DataSetPreProcessor;
import org.nd4j.linalg.dataset.api.iterator.DataSetIterator;
import org.nd4j.linalg.factory.Nd4j;
import org.nd4j.linalg.indexing.INDArrayIndex;
import org.nd4j.linalg.indexing.NDArrayIndex;



public class SequenceDataSetIterator implements DataSetIterator {

    private static final Logger log = LoggerFactory.getLogger(SequenceDataSetIterator.class);

    private int position = 0;
    private int miniBatchSize;
    private int INPUT_VECTOR_SIZE;
    private int OUTPUT_VECTOR_SIZE;
    private SequencePairReader reader;
    private AlignMode alignMode;
    private DataSetPreProcessor dataSetPreProcessor;



    public SequenceDataSetIterator(SequencePairReader reader, AlignMode alignMode, int miniBatchSize) {

        this.reader = reader;
        this.alignMode = alignMode;
        this.miniBatchSize = miniBatchSize;
        this.INPUT_VECTOR_SIZE  = reader.getInputColumns();
        this.OUTPUT_VECTOR_SIZE = reader.getOutputColumns();
    }


    public SequenceDataSetIterator(SequencePairReaderImpl reader, int miniBatchSize) {
        this( reader, AlignMode.ALIGN_END, miniBatchSize);
    }


    @Override
    public boolean hasNext() {
        return reader.hasNext();
    }


    @Override
    public DataSet next() {
        return next(miniBatchSize);
    }


    @Override
    public DataSet next(int num) {

        long requireLength = 0;
        long predictLength = 0;

        List<INDArray> features = new ArrayList<>();
        List<INDArray> labels = new ArrayList<>();

        for (int i = 0; i < num && hasNext(); i++) {

            Pair<INDArray,INDArray> pair = reader.next();

            INDArray feature = pair.getKey();
            requireLength = Math.max(requireLength,feature.size(1));
            features.add(feature);

            INDArray label = pair.getValue();
            predictLength =  Math.max(predictLength,label.size(1));
            labels.add(label);
        }

        long currentMiniBatchSize = features.size();
        long maxTimeSeriesLength = Math.max(requireLength,predictLength);

        INDArray featuresMask = Nd4j.zeros( currentMiniBatchSize, maxTimeSeriesLength, 'f');
        INDArray labelsMask = Nd4j.zeros( currentMiniBatchSize, maxTimeSeriesLength, 'f');

        INDArray dsInputs = Nd4j.zeros( new long[] { currentMiniBatchSize, INPUT_VECTOR_SIZE, maxTimeSeriesLength}, 'f');
        INDArray dsOutputs = Nd4j.zeros( new long[] { currentMiniBatchSize, OUTPUT_VECTOR_SIZE, maxTimeSeriesLength}, 'f');


        long startLabel, endLabel;
        long startFeature, endFeature;

        switch(alignMode) {

            case EQUAL_LENGTH:

                for (int i = 0; i < currentMiniBatchSize; i++) {

                    INDArray f = features.get(i);
                    INDArray l = labels.get(i);

                    dsInputs.tensorAlongDimension(i,1, 2).put(new INDArrayIndex[]{NDArrayIndex.interval(0, f.size(0)), NDArrayIndex.all() }, f);
                    dsOutputs.tensorAlongDimension(i,1, 2).put(new INDArrayIndex[]{NDArrayIndex.interval(0, l.size(0)), NDArrayIndex.all() }, l);

                    //System.out.println(dsOutputs.tensorAlongDimension(i,1, 2));
                    featuresMask = null;
                    labelsMask = null;
                    position++;
                }
                break;

            case ALIGN_START:

                startFeature = 0;
                startLabel = 0;

                for (int i = 0; i < currentMiniBatchSize; i++) {

                    INDArray f = features.get(i);
                    INDArray l = labels.get(i);

                    endFeature = f.size(1);
                    endLabel = l.size(1);

                    dsInputs.tensorAlongDimension(i,1, 2).put(new INDArrayIndex[]{NDArrayIndex.interval(0, f.size(0)), NDArrayIndex.interval( startFeature, endFeature) }, f);
                    dsOutputs.tensorAlongDimension(i,1, 2).put(new INDArrayIndex[]{NDArrayIndex.interval(0, l.size(0)), NDArrayIndex.interval( startLabel, endLabel) }, l);

                    featuresMask.get( new INDArrayIndex[] { NDArrayIndex.point(i), NDArrayIndex.interval(startFeature,endFeature)} ).assign(1);
                    labelsMask.get( new INDArrayIndex[] { NDArrayIndex.point(i), NDArrayIndex.interval(startLabel,endLabel)} ).assign(1);

                    position++;
                }
                break;

            case ALIGN_END:

                for (int i = 0; i < currentMiniBatchSize; i++) {

                    INDArray f = features.get(i);
                    INDArray l = labels.get(i);

                    long fLen = f.size(1);
                    long lLen = l.size(1);

                    if ( fLen >= lLen) {
                        startFeature = 0;
                        endFeature = fLen;
                        startLabel = fLen - lLen;
                        endLabel = fLen;
                    } else {
                        startFeature = lLen -fLen;
                        endFeature = lLen;
                        startLabel = 0;
                        endLabel = lLen;
                    }

                 //   System.out.println( startLabel + " " + endLabel + " " + l.size(1) + " " + l);
                 //   System.out.println("max: " + maxTimeSeriesLength);

                    dsInputs.tensorAlongDimension(i,1, 2).put(new INDArrayIndex[]{NDArrayIndex.interval(0, f.size(0)), NDArrayIndex.interval( startFeature, endFeature) }, f);
                    dsOutputs.tensorAlongDimension(i,1, 2).put(new INDArrayIndex[]{NDArrayIndex.interval(0, l.size(0)), NDArrayIndex.interval( startLabel, endLabel) }, l);

                    //System.out.println(dsOutputs.tensorAlongDimension(i,1, 2));

                    featuresMask.get( new INDArrayIndex[] { NDArrayIndex.point(i), NDArrayIndex.interval(startFeature,endFeature)} ).assign(1);
                    labelsMask.get( new INDArrayIndex[] { NDArrayIndex.point(i), NDArrayIndex.interval(startLabel,endLabel)} ).assign(1);

                    position++;
                }
                break;
            default:
                throw new RuntimeException();
        }

        // System.out.println(dsInputs);
        DataSet dataset = new DataSet( dsInputs, dsOutputs, featuresMask, labelsMask);
        if ( dataSetPreProcessor != null) dataSetPreProcessor.preProcess(dataset);
        return dataset;
    }


    @Override
    public int inputColumns() {
        return INPUT_VECTOR_SIZE;
    }

    @Override
    public int totalOutcomes() {
        return OUTPUT_VECTOR_SIZE;
    }

    @Override
    public boolean resetSupported() {
        return true;
    }

    @Override
    public boolean asyncSupported() {
        return false;
    }

    @Override
    public void reset() {
        position = 0;
        reader.reset();
    }

    @Override
    public int batch() {
        return miniBatchSize;
    }

    @Override
    public void setPreProcessor(DataSetPreProcessor dataSetPreProcessor) {
        this.dataSetPreProcessor = dataSetPreProcessor;
    }

    @Override
    public DataSetPreProcessor getPreProcessor() {
        return this.dataSetPreProcessor;
    }

    @Override
    public List<String> getLabels() {
        throw new UnsupportedOperationException("Not Implemented");
    }



    public enum AlignMode {
        EQUAL_LENGTH, ALIGN_START, ALIGN_END;
    }


}
