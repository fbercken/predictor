package org.bluedata.serializers;


import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.io.ByteArrayOutputStream;
import org.tensorflow.Graph;
import org.tensorflow.Operation;
import org.tensorflow.Session;
import org.tensorflow.Tensor;
import org.bluedata.nn.core.Model;
import org.bluedata.nn.core.ModelFactory;



public class TENSORFLOWModel_1_13 implements Model {

	private Graph graph;
	private Session session;
	private Operation inputOperation;
	private Operation outputOperation;
	
	private TENSORFLOWModel_1_13(Graph graph) {
		this.graph = graph;
		this.session = new Session(graph);	
		Iterator<Operation> iter = this.graph.operations();
		
		// Get first operation - input
		if ( iter.hasNext() ) inputOperation = iter.next();
		// Get last operation - output
		while (iter.hasNext()) outputOperation = iter.next();
		
		System.out.println( "ze: "+ outputOperation.numOutputs()  + "  " );
	
	}
	
	@Override
	public Object score(Object value) {
		
		Tensor<?> inputs = getInputs(value);
				
		Tensor<?> result = session.runner()
				.feed(inputOperation.name(), inputs)
				.fetch(outputOperation.name())
				.run()
				.get(0);
		
	/*	float[][] mat = new float[1][9];
		result.copyTo(mat);
		
		for(int i=0; i<9; i++)  System.out.println(mat[0][i] );*/
		
		return getOutputs(result);
	}

	
	public Tensor<?> getInputs(Object data) {		
		
		Map<String,Double> entry = (Map<String,Double>) data;
		
		
		// need to generalize 
		float[][] arr = new float[1][11]; 
		arr[0][0] =	entry.get("fixed acidity").floatValue();
		arr[0][1] = entry.get("volatile acidity").floatValue();
		arr[0][2] = entry.get("citric acid").floatValue();
		arr[0][3] = entry.get("residual sugar").floatValue();
		arr[0][4] = entry.get("chlorides").floatValue();
		arr[0][5] = entry.get("free sulfur dioxide").floatValue();
		arr[0][6] = entry.get("total sulfur dioxide").floatValue();
		arr[0][7] = entry.get("density").floatValue();
		arr[0][8] = entry.get("pH").floatValue();
		arr[0][9] = entry.get("sulphates").floatValue();
		arr[0][10] = entry.get("alcohol").floatValue();
		
		//return Tensor.create( new long[]{1}, inputs);
		System.out.println( Tensor.create(arr).numElements() );
		
		return Tensor.create(arr);
	}
	
	
	public Object getOutputs(Tensor<?> data) {	
		
		long[] shape = data.shape();
		float[][] mat = new float[1][9];
		data.copyTo(mat);
		
		System.out.println( "elts: "  +  data.numElements() );
		
		Map<String,Object> result = new HashMap<>();	
		for(int i=0; i<shape[1]; i++) {		
			result.put( String.valueOf(i), mat[0][i]);
		}	
		
		return result;
	}
	
	
	public static ModelFactory Factory = new ModelFactory() {

		@Override
		public void writeModel( Model model, OutputStream outputStream) throws IOException {
			outputStream.write( ((TENSORFLOWModel_1_13) model).graph.toGraphDef() );
		}
		
		@Override
		public Model readModel(InputStream input) throws IOException {
			Graph graph = new Graph();
			graph.importGraphDef(toByteArray(input));
			return new TENSORFLOWModel_1_13(graph);
		}
		
		
		private byte[] toByteArray(InputStream is) throws IOException {
			int nRead;
			byte[] data = new byte[16384];
			ByteArrayOutputStream buffer = new ByteArrayOutputStream();
			while ((nRead = is.read(data, 0, data.length)) != -1) buffer.write(data, 0, nRead);		
			return buffer.toByteArray();
		}
		
	};
}
