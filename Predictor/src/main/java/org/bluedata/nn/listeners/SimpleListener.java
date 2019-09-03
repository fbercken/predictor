package org.bluedata.nn.listeners;


import java.util.List;
import java.util.Map;
import org.deeplearning4j.nn.api.Model;
import org.deeplearning4j.optimize.api.TrainingListener;
import org.nd4j.linalg.api.ndarray.INDArray;


public class SimpleListener implements TrainingListener {

    @Override
    public void iterationDone(Model model, int iteration, int epoch) {
        System.out.printf( "gradient: %s\n", model.gradient() );
        System.out.printf( "score: %s\n" , model.score() );
        System.out.printf( "gardscore: %s\n" ,model.gradientAndScore() );
        System.out.printf( "i: %s, i1: %s\n" ,iteration, epoch);

    }

    @Override
    public void onEpochStart(Model model) {

    }

    @Override
    public void onEpochEnd(Model model) {

    }

    @Override
    public void onForwardPass(Model model, List<INDArray> list) {

    }

    @Override
    public void onForwardPass(Model model, Map<String, INDArray> map) {

    }

    @Override
    public void onGradientCalculation(Model model) {

    }

    @Override
    public void onBackwardPass(Model model) {

    }

}