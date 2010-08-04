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
 *  Creation date: 25.11.2009
 */ 
package amuse.nodes.validator.metrics.correlation;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;

import amuse.interfaces.nodes.NodeException;
import amuse.nodes.classifier.interfaces.ClassifiedSongPartitionsDescription;
import amuse.nodes.validator.interfaces.ClassificationQualityMetricCalculator;
import amuse.nodes.validator.interfaces.ValidationMetricDouble;

/**
 * Spearman's rank correlation coefficient is a special case of Pearson product-moment correlation coefficient.
 *  
 * @author Igor Vatolkin
 * @version $Id: $
 */
public class SpearmansRhoRankCorrelation extends ClassificationQualityMetricCalculator {

	/**
	 * @see amuse.nodes.validator.interfaces.ClassificationQualityMetricCalculatorInterface#setParameters(java.lang.String)
	 */
	public void setParameters(String parameterString) throws NodeException {
		// Does nothing
	}
	
	/**
	 * @see amuse.nodes.validator.interfaces.ClassificationQualityMetricCalculatorInterface#calculateMetric(java.util.ArrayList, java.util.ArrayList)
	 */
	public ValidationMetricDouble[] calculateMetric(ArrayList<Double> groundTruthRelationships, ArrayList<ClassifiedSongPartitionsDescription> predictedRelationships) throws NodeException {
		if(groundTruthRelationships.size() != predictedRelationships.size()) {
			throw new NodeException("The number of labeled instances must be equal to the number of predicted instances!");
		}
		
		ValidationMetricDouble[] metricOnSongLev = null;
		ValidationMetricDouble[] metricOnPartLev = null;
		
		if(groundTruthRelationships.get(0) instanceof Double) {
			if(this.getSongLevel()) {
				metricOnSongLev = calculateFuzzyMetricOnSongLevel(groundTruthRelationships, predictedRelationships);
			} 
			if(this.getPartitionLevel()) {
				metricOnPartLev = calculateFuzzyMetricOnPartitionLevel(groundTruthRelationships, predictedRelationships);
			}
		} else {
			return null;
		}
		
		// Return the corresponding number of metric values
		if(this.getSongLevel() && !this.getPartitionLevel()) {
			return metricOnSongLev;
		} else if(!this.getSongLevel() && this.getPartitionLevel()) {
			return metricOnPartLev;
		} else if(this.getSongLevel() && this.getPartitionLevel()) {
			ValidationMetricDouble[] metrics = new ValidationMetricDouble[2];
			metrics[0] = metricOnSongLev[0];
			metrics[1] = metricOnPartLev[0];
			return metrics;
		} else {
			return null;
		}
	}

	/**
	 * @see amuse.nodes.validator.interfaces.ClassificationQualityMetricCalculatorInterface#calculateBinaryMetricOnSongLevel(java.util.ArrayList, java.util.ArrayList)
	 */
	public ValidationMetricDouble[] calculateBinaryMetricOnSongLevel(ArrayList<Boolean> groundTruthRelationships, ArrayList<ClassifiedSongPartitionsDescription> predictedRelationships) throws NodeException {
		return null;
	}

	/**
	 * @see amuse.nodes.validator.interfaces.ClassificationQualityMetricCalculatorInterface#calculateBinaryMetricOnPartitionLevel(java.util.ArrayList, java.util.ArrayList)
	 */
	public ValidationMetricDouble[] calculateBinaryMetricOnPartitionLevel(ArrayList<Boolean> groundTruthRelationships, ArrayList<ClassifiedSongPartitionsDescription> predictedRelationships) throws NodeException {
		return null;
	}

	/**
	 * @see amuse.nodes.validator.interfaces.ClassificationQualityMetricCalculatorInterface#calculateFuzzyMetricOnSongLevel(java.util.ArrayList, java.util.ArrayList)
	 */
	public ValidationMetricDouble[] calculateFuzzyMetricOnSongLevel(ArrayList<Double> groundTruthRelationships, ArrayList<ClassifiedSongPartitionsDescription> predictedRelationships) throws NodeException {
		
		// Sort labeled and predicted values
		ArrayList<Double> sortedPredictedRelationships = new ArrayList<Double>(groundTruthRelationships.size());
		ArrayList<Double> sortedLabeledRelationships = new ArrayList<Double>(groundTruthRelationships.size());
		for(int i=0;i<groundTruthRelationships.size();i++) {
			
			// Calculate the predicted value for this song (averaging among all partitions)
			Double currentPredictedValue = 0.0d;
			for(int j=0;j<predictedRelationships.get(i).getRelationships().length;j++) {
				currentPredictedValue += predictedRelationships.get(i).getRelationships()[j];
			}
			currentPredictedValue /= predictedRelationships.get(i).getRelationships().length;
			
			sortedPredictedRelationships.add(currentPredictedValue);
			sortedLabeledRelationships.add(new Double(groundTruthRelationships.get(i)));
		}
		Collections.sort(sortedPredictedRelationships);
		Collections.sort(sortedLabeledRelationships);
		
		// Calculate ranks for predicted values
		HashMap<Double, Double> predictedValueToRang = new HashMap<Double, Double>();
		for(int i=sortedPredictedRelationships.size()-1;i>=0;i--) {
			
			// Calculate the number of the equal values
			int equalValuesNumber = 1;
			double rank = sortedPredictedRelationships.size() - i; // First position in descending order
			for(int j=i-1;j>=0;j--) {
				if(sortedPredictedRelationships.get(j).equals(sortedPredictedRelationships.get(i))) {
					equalValuesNumber++;
					rank += sortedPredictedRelationships.size() - j; // Add the next position in descending order
				} else {
					break;
				}
			}
			rank /= equalValuesNumber;
			predictedValueToRang.put(sortedPredictedRelationships.get(i), rank);
			
			// Go to the next position with different value
			i -= (equalValuesNumber-1);
		}
		
		// Calculate ranks for labeled values
		HashMap<Double, Double> labeledValueToRang = new HashMap<Double, Double>();
		for(int i=sortedLabeledRelationships.size()-1;i>=0;i--) {
			
			// Calculate the number of the equal values
			int equalValuesNumber = 1;
			double rank = sortedLabeledRelationships.size() - i; // First position in descending order
			for(int j=i-1;j>=0;j--) {
				if(sortedLabeledRelationships.get(j).equals(sortedLabeledRelationships.get(i))) {
					equalValuesNumber++;
					rank += sortedLabeledRelationships.size() - j; // Add the next position in descending order
				} else {
					break;
				}
			}
			rank /= equalValuesNumber;
			labeledValueToRang.put(sortedLabeledRelationships.get(i), rank);
			
			// Go to the next position with different value
			i -= (equalValuesNumber-1);
		}
		
		// Calculate the Spearman's rank correlation
		double sumOfRankMultiplications = 0.0d;
		double sumOfSquaredPredictedRanks = 0.0d;
		double sumOfSquaredLabeledRanks = 0.0d;
		double p = groundTruthRelationships.size() * Math.pow((groundTruthRelationships.size()+1d)/2d, 2);
		for(int i=0;i<groundTruthRelationships.size();i++) {
			
			// Calculate the predicted value for this song (averaging among all partitions)
			Double currentPredictedValue = 0.0d;
			for(int j=0;j<predictedRelationships.get(i).getRelationships().length;j++) {
				currentPredictedValue += predictedRelationships.get(i).getRelationships()[j];
			}
			currentPredictedValue /= predictedRelationships.get(i).getRelationships().length;
			
			sumOfRankMultiplications += predictedValueToRang.get(currentPredictedValue) * labeledValueToRang.get(groundTruthRelationships.get(i));
			sumOfSquaredPredictedRanks += Math.pow(predictedValueToRang.get(currentPredictedValue), 2);
			sumOfSquaredLabeledRanks += Math.pow(labeledValueToRang.get(groundTruthRelationships.get(i)), 2);
			
		}	
		
		// Calculate the correlation coefficient
		double corrCoef = (sumOfRankMultiplications - p) / 
			(Math.sqrt(sumOfSquaredPredictedRanks - p) * Math.sqrt(sumOfSquaredLabeledRanks - p));
		
		// Prepare the result
		ValidationMetricDouble[] correlationMetric = new ValidationMetricDouble[1];
		correlationMetric[0] = new ValidationMetricDouble();
		correlationMetric[0].setId(301);
		correlationMetric[0].setName("Speraman's rank correlation coefficient on song level");
		correlationMetric[0].setValue(corrCoef);
		return correlationMetric;
	}
	
	/**
	 * @see amuse.nodes.validator.interfaces.ClassificationQualityMetricCalculatorInterface#calculateFuzzyMetricOnPartitionLevel(java.util.ArrayList, java.util.ArrayList)
	 */
	public ValidationMetricDouble[] calculateFuzzyMetricOnPartitionLevel(ArrayList<Double> groundTruthRelationships, ArrayList<ClassifiedSongPartitionsDescription> predictedRelationships) throws NodeException {
		
		// Calculate the number of all partitions
		int overallPartitionNumber = 0;
		for(int i=0;i<groundTruthRelationships.size();i++) {
			overallPartitionNumber += predictedRelationships.get(i).getRelationships().length;
		}
		
		// Sort labeled and predicted values
		ArrayList<Double> sortedPredictedRelationships = new ArrayList<Double>(groundTruthRelationships.size());
		ArrayList<Double> sortedLabeledRelationships = new ArrayList<Double>(groundTruthRelationships.size());
		for(int i=0;i<groundTruthRelationships.size();i++) {
			for(int j=0;j<predictedRelationships.get(i).getRelationships().length;j++) {
				sortedPredictedRelationships.add(predictedRelationships.get(i).getRelationships()[j]);
				sortedLabeledRelationships.add(new Double(groundTruthRelationships.get(i)));
			}
		}
		Collections.sort(sortedPredictedRelationships);
		Collections.sort(sortedLabeledRelationships);
		
		
		// Calculate ranks for predicted values
		HashMap<Double, Double> predictedValueToRang = new HashMap<Double, Double>();
		for(int i=sortedPredictedRelationships.size()-1;i>=0;i--) {
			
			// Calculate the number of the equal values
			int equalValuesNumber = 1;
			double rank = sortedPredictedRelationships.size() - i; // First position in descending order
			for(int j=i-1;j>=0;j--) {
				if(sortedPredictedRelationships.get(j).equals(sortedPredictedRelationships.get(i))) {
					equalValuesNumber++;
					rank += sortedPredictedRelationships.size() - j; // Add the next position in descending order
				} else {
					break;
				}
			}
			rank /= equalValuesNumber;
			predictedValueToRang.put(sortedPredictedRelationships.get(i), rank);
			
			// Go to the next position with different value
			i -= (equalValuesNumber-1);
		}
		
		// Calculate ranks for labeled values
		HashMap<Double, Double> labeledValueToRang = new HashMap<Double, Double>();
		for(int i=sortedLabeledRelationships.size()-1;i>=0;i--) {
			
			// Calculate the number of the equal values
			int equalValuesNumber = 1;
			double rank = sortedLabeledRelationships.size() - i; // First position in descending order
			for(int j=i-1;j>=0;j--) {
				if(sortedLabeledRelationships.get(j).equals(sortedLabeledRelationships.get(i))) {
					equalValuesNumber++;
					rank += sortedLabeledRelationships.size() - j; // Add the next position in descending order
				} else {
					break;
				}
			}
			rank /= equalValuesNumber;
			labeledValueToRang.put(sortedLabeledRelationships.get(i), rank);
			
			// Go to the next position with different value
			i -= (equalValuesNumber-1);
		}
		
		// Calculate the Spearman's rank correlation
		double sumOfRankMultiplications = 0.0d;
		double sumOfSquaredPredictedRanks = 0.0d;
		double sumOfSquaredLabeledRanks = 0.0d;
		double p = overallPartitionNumber * Math.pow((overallPartitionNumber+1d)/2d, 2);
		for(int i=0;i<groundTruthRelationships.size();i++) {
			for(int j=0;j<predictedRelationships.get(i).getRelationships().length;j++) {
				sumOfRankMultiplications += predictedValueToRang.get(predictedRelationships.get(i).getRelationships()[j]) * labeledValueToRang.get(groundTruthRelationships.get(i));
				sumOfSquaredPredictedRanks += Math.pow(predictedValueToRang.get(predictedRelationships.get(i).getRelationships()[j]), 2);
				sumOfSquaredLabeledRanks += Math.pow(labeledValueToRang.get(groundTruthRelationships.get(i)), 2);
			}
		}
		
		// Calculate the correlation coefficient
		double corrCoef = (sumOfRankMultiplications - p) / 
			(Math.sqrt(sumOfSquaredPredictedRanks - p) * Math.sqrt(sumOfSquaredLabeledRanks - p));
		
		// Prepare the result
		ValidationMetricDouble[] correlationMetric = new ValidationMetricDouble[1];
		correlationMetric[0] = new ValidationMetricDouble();
		correlationMetric[0].setId(301);
		correlationMetric[0].setName("Speraman's rank correlation coefficient on partition level");
		correlationMetric[0].setValue(corrCoef);
		return correlationMetric;
	}

	/**
	 * @see amuse.nodes.validator.interfaces.ClassificationQualityMetricCalculatorInterface#calculateMulticlassMetricOnSongLevel(java.util.ArrayList, java.util.ArrayList)
	 */
	public ValidationMetricDouble[] calculateMulticlassMetricOnSongLevel(ArrayList<ArrayList<Double>> groundTruthRelationships, ArrayList<ArrayList<ClassifiedSongPartitionsDescription>> predictedRelationships) throws NodeException {
		return null;
	}

	/**
	 * @see amuse.nodes.validator.interfaces.ClassificationQualityMetricCalculatorInterface#calculateMulticlassMetricOnPartitionLevel(java.util.ArrayList, java.util.ArrayList)
	 */
	public ValidationMetricDouble[] calculateMulticlassMetricOnPartitionLevel(ArrayList<ArrayList<Double>> groundTruthRelationships, ArrayList<ArrayList<ClassifiedSongPartitionsDescription>> predictedRelationships) throws NodeException {
		return null;
	}


}
