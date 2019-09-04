package org.bluedata.serializers;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import org.deeplearning4j.nn.multilayer.MultiLayerNetwork;
import org.deeplearning4j.util.ModelSerializer;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.factory.Nd4j;
import org.bluedata.nn.core.Model;
import org.bluedata.nn.core.ModelFactory;



public class DPL4Model implements Model {

	private MultiLayerNetwork model;
	
	
	public DPL4Model setModel(MultiLayerNetwork model) {
		this.model = model;
		return this;
	}
	
	@Override
	public Object score(Object value) {	
		INDArray data = Nd4j.create( ((double[]) value) );			
		return model.output(data).data().asDouble();
	}

	
	
	public static ModelFactory Factory = new ModelFactory() {

		@Override
		public void writeModel( Model model, OutputStream outputStream) throws IOException {
			ModelSerializer.writeModel( ((DPL4Model) model).model, outputStream, true);
		}
		
		@Override
		public Model readModel(InputStream input) throws IOException {
			MultiLayerNetwork model = ModelSerializer.restoreMultiLayerNetwork(input);
			return (new DPL4Model()).setModel(model);
		}
		
	};

	
}