package org.bluedata.iterators;

import java.util.Iterator;

import org.bluedata.nn.core.Pair;
import org.nd4j.linalg.api.ndarray.INDArray;

public interface SequencePairReader extends Iterator<Pair<INDArray,INDArray>>{

	 public int getInputColumns();
	 
	 public int getOutputColumns();
	    
	 public void reset();
	 
}
