package org.bluedata.serializers;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import ml.dmlc.xgboost4j.java.Booster;
import ml.dmlc.xgboost4j.java.DMatrix;
import ml.dmlc.xgboost4j.java.XGBoost;
import ml.dmlc.xgboost4j.java.XGBoostError;
import org.bluedata.nn.core.Model;
import org.bluedata.nn.core.ModelFactory;

public class XGBoost4JModel implements Model {
	
	private Booster predictor;
	
	public XGBoost4JModel(Booster predictor) {
		this.predictor = predictor;
	}

	@Override
	public Object score(Object value) {
		float[][] result = null;
		try {
			DMatrix matrix = getInputs(value);
			result =  predictor.predict(matrix);
		} catch (XGBoostError e) {
			e.printStackTrace();
		}
		
		return getOutputs(result);
	}
	
	public DMatrix getInputs(Object data) throws XGBoostError {	
		int rowsNb = 1, columnsNb = 1, size = 0;
		float[] values = new float[size];
		DMatrix matrix = new DMatrix(values, rowsNb, columnsNb);
		return matrix;
	}
	
	public Object getOutputs(float[][] data) {		
		return data.getClass();
	}
	
	
	
	public static ModelFactory Factory = new ModelFactory() {

		@Override
		public void writeModel( Model model, OutputStream outputStream) throws IOException {
			//ModelSerializer.writeModel( ((DPL4JModel) model).model, outputStream, true);
			throw new IOException("Not implemented");
		}
		
		@Override
		public Model readModel(InputStream input) throws IOException {			
			try {
				return new XGBoost4JModel( XGBoost.loadModel(input));
			} catch (XGBoostError e) {
				throw new IOException( "Unable to unmarshall PMML model");
			}
		}
		
	};

}

