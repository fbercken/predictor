package org.bluedata.serializers;


import java.util.Map;
import java.util.List;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import org.dmg.pmml.FieldName;
import org.jpmml.evaluator.Evaluator;
import org.jpmml.evaluator.FieldValue;
import org.jpmml.evaluator.InputField;
import org.jpmml.evaluator.TargetField;
import org.jpmml.evaluator.neural_network.NeuronProbabilityDistribution;
import org.jpmml.evaluator.ModelEvaluatorBuilder;
import org.jpmml.model.PMMLUtil;
import org.xml.sax.SAXException;
import org.bluedata.nn.core.Model;
import org.bluedata.nn.core.ModelFactory;

import javax.xml.bind.JAXBException;


public class PMMLModel implements Model {
	

	private final Evaluator evaluator;
	private final ModelEvaluatorBuilder evaluatorBuilder;
	
	private List<InputField> inputFields;
	private List<TargetField> targetFields;	
	//private List<OutputField> outputFields;
	
	
	public PMMLModel(ModelEvaluatorBuilder evaluatorBuilder) {
		
		this.evaluatorBuilder = evaluatorBuilder;
		evaluator = evaluatorBuilder.build();
		
		evaluator.verify();

		inputFields  = evaluator.getInputFields();
		targetFields = evaluator.getTargetFields();
		//outputFields = evaluator.getOutputFields();
		
	}

	@SuppressWarnings("unchecked")
	@Override
	public Object score(Object value) {
		
		Object rawValue, targetValue;
		FieldName inputName, targetName;
		
		Map<String,?> inputRecord = getInputs(value);
		
		Map<FieldName,FieldValue> arguments = new LinkedHashMap<>();
		for(InputField inputField : inputFields){			
			inputName = inputField.getName();
			rawValue = inputRecord.get(inputName.getValue());
			arguments.put(inputName, inputField.prepare(rawValue));
		}

		Map<FieldName,?> results = evaluator.evaluate(arguments);
		
		Map<String,Object> resultRecord = new HashMap<>();		
		for(TargetField targetField : targetFields) {
			targetName = targetField.getName();
			targetValue = results.get(targetName);
			resultRecord.put( targetName.getValue(), targetValue);
		}
		
		return getOutputs(resultRecord);
	}
	

	
	public Map<String,?> getInputs(Object data) {	
	
		return (Map<String,?>) data;
	}
	
	
	
	@SuppressWarnings("unchecked")
	public Object getOutputs(Map<String,?> data) {	
		
		NeuronProbabilityDistribution<Number> probabilities;		
		probabilities = (NeuronProbabilityDistribution<Number>) data.get("quality");
			
		Map<String,Object> result = new HashMap<>();	
		for( String category : probabilities.getCategories()) {
			result.put( category, probabilities.getProbability(category).floatValue() );
		}	
		return result;
	}
	
	
	
	
	public static ModelFactory Factory = new ModelFactory() {
			
		public void writeModel(Model model, OutputStream outputStream) throws IOException {
			try {
				PMMLUtil.marshal( ((PMMLModel) model).evaluatorBuilder.getPMML(), outputStream);
			} catch (JAXBException e) {
				throw new IOException("Unable to marshall PMML model");
			}
		}
		
		public Model readModel(InputStream input) throws IOException {
			try {
				return new PMMLModel( new ModelEvaluatorBuilder( PMMLUtil.unmarshal(input)) );
			} catch (SAXException | JAXBException e) {
				throw new IOException( "Unable to unmarshall PMML model");
			} 
		}
	
	};

}
