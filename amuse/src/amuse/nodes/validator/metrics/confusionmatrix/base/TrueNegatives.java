/** 
 * This file is part of AMUSE framework (Advanced MUsic Explorer).
 * 
 * Copyright 2006-2010 by code authors
 * 
 * Created at TU Dortmund, Chair of Algorithm Engineering
 * (Contact: <http://ls11-www.cs.tu-dortmund.de>) 
 *
 * AMUSE is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * AMUSE is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with AMUSE. If not, see <http://www.gnu.org/licenses/>.
 * 
 *  Creation date: 23.01.2009
 */ 
package amuse.nodes.validator.metrics.confusionmatrix.base;

import java.util.ArrayList;

import amuse.interfaces.nodes.NodeException;
import amuse.nodes.classifier.interfaces.ClassifiedSongPartitions;
import amuse.nodes.validator.interfaces.ClassificationQualityDoubleMetricCalculator;
import amuse.nodes.validator.interfaces.ValidationMetricDouble;

/**
 *  Number of true negatives
 *  
 * @author Igor Vatolkin
 * @version $Id$
 */
public class TrueNegatives extends ClassificationQualityDoubleMetricCalculator {

	/**
	 * @see amuse.nodes.validator.interfaces.ClassificationQualityMetricCalculatorInterface#setParameters(java.lang.String)
	 */
	public void setParameters(String parameterString) throws NodeException {
		// Does nothing
	}
	
	/**
	 * @see amuse.nodes.validator.interfaces.ClassificationQualityMetricCalculatorInterface#calculateOneClassMetricOnSongLevel(java.util.ArrayList, java.util.ArrayList)
	 */
	public ValidationMetricDouble[] calculateOneClassMetricOnSongLevel(ArrayList<Double> groundTruthRelationships, ArrayList<ClassifiedSongPartitions> predictedRelationships) throws NodeException {
		int numberOfTrueNegatives = 0;
		for(int i=0;i<groundTruthRelationships.size();i++) {
			
			// Calculate the predicted value for this song (averaging among all partitions)
			Double currentPredictedValue = 0.0d;
			for(int j=0;j<predictedRelationships.get(i).getRelationships().length;j++) {
				currentPredictedValue += predictedRelationships.get(i).getRelationships()[j];
			}
			currentPredictedValue /= predictedRelationships.get(i).getRelationships().length;
			
			if(currentPredictedValue >= 0.5) {
				currentPredictedValue = 1.0d;
			} else {
				currentPredictedValue = 0.0d;
			}
			
			// Round the ground truth value to a binary value
			Double currentGroundTruthValue = groundTruthRelationships.get(i);
			if(currentGroundTruthValue >= 0.5) {
				currentGroundTruthValue = 1.0d;
			} else {
				currentGroundTruthValue = 0.0d;
			}
			
			if(currentGroundTruthValue.doubleValue() == 0.0 && currentPredictedValue.doubleValue() == 0.0) {
				numberOfTrueNegatives++;
			}
			
		}
		
		// Prepare the result
		ValidationMetricDouble[] trueNegativesMetric = new ValidationMetricDouble[1];
		trueNegativesMetric[0] = new ValidationMetricDouble();
		trueNegativesMetric[0].setId(101);
		trueNegativesMetric[0].setName("Number of true negatives on song level");
		trueNegativesMetric[0].setValue(new Double(numberOfTrueNegatives));
		return trueNegativesMetric;
	}

	/**
	 * @see amuse.nodes.validator.interfaces.ClassificationQualityMetricCalculatorInterface#calculateOneClassMetricOnPartitionLevel(java.util.ArrayList, java.util.ArrayList)
	 */
	public ValidationMetricDouble[] calculateOneClassMetricOnPartitionLevel(ArrayList<Double> groundTruthRelationships, ArrayList<ClassifiedSongPartitions> predictedRelationships) throws NodeException {
		int numberOfTrueNegatives = 0;
		for(int i=0;i<groundTruthRelationships.size();i++) {
			for(int j=0;j<predictedRelationships.get(i).getRelationships().length;j++) {
				if(groundTruthRelationships.get(i).doubleValue() == 0.0 && 
						predictedRelationships.get(i).getRelationships()[j].doubleValue() == 0.0) {
					numberOfTrueNegatives++;
				}
			}
		}
		
		// Prepare the result
		ValidationMetricDouble[] trueNegativesMetric = new ValidationMetricDouble[1];
		trueNegativesMetric[0] = new ValidationMetricDouble();
		trueNegativesMetric[0].setId(101);
		trueNegativesMetric[0].setName("Number of true negatives on partition level");
		trueNegativesMetric[0].setValue(new Double(numberOfTrueNegatives));
		return trueNegativesMetric;
	}

	/**
	 * @see amuse.nodes.validator.interfaces.ClassificationQualityMetricCalculatorInterface#calculateMulticlassMetricOnSongLevel(java.util.ArrayList, java.util.ArrayList)
	 */
	public ValidationMetricDouble[] calculateMultiClassMetricOnSongLevel(ArrayList<ClassifiedSongPartitions> groundTruthRelationships, ArrayList<ClassifiedSongPartitions> predictedRelationships) throws NodeException {
		throw new NodeException(this.getClass().getName() + " can be calculated only for binary classification tasks");
	}


	/**
	 * @see amuse.nodes.validator.interfaces.ClassificationQualityMetricCalculatorInterface#calculateMulticlassMetricOnPartitionLevel(java.util.ArrayList, java.util.ArrayList)
	 */
	public ValidationMetricDouble[] calculateMultiClassMetricOnPartitionLevel(ArrayList<ClassifiedSongPartitions> groundTruthRelationships, ArrayList<ClassifiedSongPartitions> predictedRelationships) throws NodeException {
		throw new NodeException(this.getClass().getName() + " can be calculated only for binary classification tasks");
	}


}

