package org.bluedata.iterators;


import org.bluedata.nn.core.Pair;
import org.datavec.api.records.reader.RecordReader;
import org.datavec.api.writable.Writable;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.factory.Nd4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Iterator;
import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;


public class SequencePairReaderImpl implements SequencePairReader {

    private static final Logger log = LoggerFactory.getLogger(SequencePairReaderImpl.class);
    private int pastLength;
    private int predictLength;
    private int window;
    private int[] inputIndexes;
    private int[] outputIndexes;
    private RecordReader reader;
    private List<INDArray> tmpFeatures;
    private List<INDArray> tmpLabels;



    public SequencePairReaderImpl(RecordReader reader, int[] inputIndexes, int pastLength, int[] outputIndexes, int predictLength) {

        this.pastLength = pastLength;
        this.predictLength = predictLength;
        this.inputIndexes = inputIndexes;
        this.outputIndexes = outputIndexes;
        this.reader = reader;

        this.window = pastLength + predictLength;
        this.tmpFeatures = new ArrayList<>();
        this.tmpLabels = new ArrayList<>();

        reset();
    }


    private void initialize() {

        for(int i=0; i<( window ); i++) {
            if ( reader.hasNext() ) {
                List<Writable> current = reader.next();
                tmpFeatures.add(transform( current, inputIndexes));
                tmpLabels.add(transform( current, outputIndexes));
            }
        }
    }

    private INDArray transform(List<Writable> data, int[] indexes) {

        int i = 0;
        double[] input = new double[indexes.length];
        for (int j : indexes) {
            input[i++] = data.get(j).toDouble();
        }
        return Nd4j.create(input, new int[]{ indexes.length, 1}, 'f');
    }

    public int getInputColumns() {

        return inputIndexes.length;
    }

    public int getOutputColumns() {

        return outputIndexes.length;
    }

    public boolean hasNext() {

        return reader.hasNext();
    }

    public Pair<INDArray,INDArray> next() {

        if ( !reader.hasNext() ) throw new NoSuchElementException();

        List<Writable> current = reader.next();

        //if ( tmpFeatures.size() >= window )
        tmpFeatures.remove(0);
        tmpFeatures.add(transform(current, inputIndexes));

        tmpLabels.remove(0);
        tmpLabels.add(transform(current, outputIndexes));


        return new Pair<>(
                Nd4j.hstack(tmpFeatures.subList(0, pastLength) ),
                Nd4j.hstack(tmpLabels.subList(pastLength, pastLength + predictLength))
        );
    }


    public void reset() {
        reader.reset();
        initialize();
    }


}
