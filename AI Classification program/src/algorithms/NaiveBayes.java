/*
 * Copyright (C) 2016 matthewrohrlach, nwmoore, emmanuel-armstrong
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package algorithms;

import data_components.DataPoint;
import data_components.DataSet;
import data_components.ProcessData;
import java.util.ArrayList;
import java.util.Arrays;

/**
 *
 * @author matthewrohrlach & Emmanuel Armstrong
 */
public class NaiveBayes {
    
    // Global variable that stores a parent DataSet
    protected final DataSet dataSet;
    
    // Global variable that stores a testing DataSet
    protected DataSet testingDataSet;
    
    // Global variable that stores list of training DataSets
    protected DataSet trainingDataSet;
    
    // Global variable that stores a DataSet file name
    protected final String dataSetName;
    
    // Global variable that stores an arraylist that stores lists of processed datapoints
    protected ArrayList<ArrayList<DataPoint>> processedPointsList;
    
    // Global variable that stores an arraylist that stores lists of separated training points
    protected ArrayList<ArrayList<DataPoint>> separatedPointsList;
    
    // Global variable that stores the index of DataPoints' actual class values
    protected final int classIndex;
    
    // Global variable that stores the number of unique classes in the DataSet
    protected int classCounter;
    
    // Global variable that stores the names of classes that correspond to lists in array
    protected ArrayList<String> observedClasses;
    
    // Global variable that stores the number of unique values per attribute index
    protected int[] potentialAttributeValues;
    protected ArrayList<ArrayList<String>> observedAttributeValues;
  
    
    // Constructor that takes a ProcessData object
    public NaiveBayes (ProcessData processData) {
        
        // Set dataSet object using processData
        dataSet = processData.getDataSet();
        
        // Set dataSet name using processData
        dataSetName = processData.getDataSetName();
        
        // Set classIndex using processData
        classIndex = processData.getClassIndex();
        
        // Count the number of unique classes
        classCounter = 0;
        observedClasses = new ArrayList<>();
        String observedClass;
        for (DataPoint observedDataPoint : dataSet.getDataSet()) {
            
            // Test for a unique class
            observedClass = observedDataPoint.getFeatures()[classIndex];
            if (!observedClasses.contains(observedClass)) {
                classCounter++;
                observedClasses.add(observedClass);
            }
        }
        
        // Calculate the number of potential values for every attribute (and class)
        int numAttributes = dataSet.getDataPoint(0).getFeatures().length;
        observedAttributeValues = new ArrayList<>();
        
        // Initialize observed attribute value lists
        for (int addedLists = 0; addedLists < numAttributes; addedLists++) {
            ArrayList<String> listToAdd = new ArrayList<>();
            observedAttributeValues.add(listToAdd);
        }
        
        potentialAttributeValues = new int[numAttributes];
        for (int attributeIterator = 0; attributeIterator < numAttributes; attributeIterator++) {
            
            potentialAttributeValues[attributeIterator] = this.numberOfFeatureValues(attributeIterator);
        }
        
        // Constructor calls final method to classify dataset
        processedPointsList = new ArrayList<>();
        separatedPointsList = new ArrayList<>();
        
        // Classify with tenFoldCrossValidation
        for (int testIndexIterator = 0; testIndexIterator < 10; testIndexIterator++) {

            // Set the appropriate testing dataSet
            testingDataSet = this.dataSet.getSubsetAt(testIndexIterator);

            // Initialize processed datapoint arraylist to size of number of unique classes
            processedPointsList.clear();
            for (int addedLists = 0; addedLists < classCounter; addedLists++) {
                ArrayList<DataPoint> listToAdd = new ArrayList<>();
                processedPointsList.add(listToAdd);
            }

            // Rebuild the training dataSet
            trainingDataSet = new DataSet();
            for (int setIterator = 0; setIterator < 10; setIterator++) {

                if (setIterator != testIndexIterator) {
                    
                    for (DataPoint trainPoint : this.dataSet.getSubsetAt(setIterator).getDataSet()) {
                        
                        trainingDataSet.addToSet(trainPoint);
                    }
                }
            }

            // Run the classification algorithm
            classifyDataSet();
        }
    }
    
    
    // Method that processes dataset (no arguments, uses globally-stored sets)
    protected final void classifyDataSet() {
        
        // Run algorithm
        
        // Data points are added to their respective lists
        //================================================
        
        // ========= TRAINING ============================
        
        // Separate training points into lists and calculate prior probabilities
        separateTrainingSet();
        double[] priorProbabilities = new double[separatedPointsList.size()];
        for (int listIterator = 0; listIterator < separatedPointsList.size(); listIterator++) {
            
            priorProbabilities[listIterator] = ((double) separatedPointsList.get(listIterator).size()
                    / (double) trainingDataSet.size());
        }
        
        // Calculate the probability of every feature with regards to class
        int featureSize = (dataSet.getDataPoint(0).getFeatures().length - 1);
        int thisPotentialAttributeValues;
        
        // Calculate max of potential values for each attribute
        int greatestNumberOfFeatures = -1;
        int thisNumberOfFeatures;
        for (ArrayList<String> observedAttributeValueList : observedAttributeValues) {
            
            thisNumberOfFeatures = observedAttributeValueList.size();
            if (thisNumberOfFeatures > greatestNumberOfFeatures) {
                greatestNumberOfFeatures = thisNumberOfFeatures;
            }
        }
        double[][][] featureProbabilities = new double[featureSize][greatestNumberOfFeatures][separatedPointsList.size()+1];
        String featureInQuestion;
        String thisFeature;
        int featureCount;
        int featureCountOfAllClasses;
        
        // For every feature
        for (int featureIterator = 0; featureIterator < featureSize; featureIterator++) {
            
            if (featureIterator != classIndex) {
                thisPotentialAttributeValues = potentialAttributeValues[featureIterator];
                // For every potential value of that feature
                for (int valueIterator = 0; valueIterator < thisPotentialAttributeValues; valueIterator++) {

                    // For every class to which a potential value may belong
                    featureCountOfAllClasses = 0;
                    for (int classListIterator = 0; classListIterator < separatedPointsList.size(); classListIterator++) {

                        // Calculate the number of occurences of this value within this class list
                        featureInQuestion = observedAttributeValues.get(featureIterator).get(valueIterator);
                        featureCount = 0;
                        for (DataPoint point : separatedPointsList.get(classListIterator)) {

                            thisFeature = point.getFeatures()[valueIterator];
                            if (thisFeature.equals(featureInQuestion)) {

                                featureCount++;
                            }
                        }

                        // Track this feature count as part of a sum
                        featureCountOfAllClasses += featureCount;

                        // Probability of (this feature value) of (this feature) of (this specific class) is stored
                        featureProbabilities[featureIterator][valueIterator][classListIterator] = ((double) featureCount / (double) separatedPointsList.get(classListIterator).size());
                    }
                    // Probability of (this feature value) over all classes is stored
                    featureProbabilities[featureIterator][valueIterator][separatedPointsList.size()] = ((double) featureCountOfAllClasses / (double) trainingDataSet.size());
                }
            }
        }
        
        // ========= TESTING ============================
        
        double numeratorProduct;
        double denominatorProduct;
        
        double greatestProbability;
        double thisProbability;
        double thisEvidenceProbability;
        double thisLikelihoodProbability;
        int classIndexOfGreatestProbability = 0;
        
        String[] currentFeatures;
        String existingValue;
        int existingValueIndex;
        
        // Run on testing set
        for (int pointIterator = 0; pointIterator < testingDataSet.size(); pointIterator++) {
            
            greatestProbability = -1;
            denominatorProduct = 1.0;
            currentFeatures = testingDataSet.getDataPoint(pointIterator).getFeatures();
            
            // Build denominator product for all classes
            for (int featureIterator = 0; featureIterator < featureSize; featureIterator++) {
                
                if (featureIterator != classIndex) {
                    // Evidence has already been stored
                    existingValue = currentFeatures[featureIterator];
                    existingValueIndex = observedAttributeValues.get(featureIterator).indexOf(existingValue);
                    
                    thisEvidenceProbability = featureProbabilities[featureIterator][existingValueIndex][0];
                    if (thisEvidenceProbability > 0) {
                        denominatorProduct *= thisEvidenceProbability;
                    }
                    else {
                        denominatorProduct *= .01;
                    }
                }
            }
            
            // Build numerator product to create probability
            for (int classIterator = 0; classIterator < observedClasses.size(); classIterator++) {
                
                numeratorProduct = 1.0;
                
                // For every feature of the current point
                for (int featureIterator = 0; featureIterator < featureSize; featureIterator++) {
                    
                    if (featureIterator != classIndex) {
                        // Likelihood has already been stored
                        existingValue = currentFeatures[featureIterator];
                        existingValueIndex = observedAttributeValues.get(featureIterator).indexOf(existingValue);
                        
                        thisLikelihoodProbability = featureProbabilities[featureIterator][existingValueIndex][classIterator];
                        if (thisLikelihoodProbability > 0) {
                            numeratorProduct *= thisLikelihoodProbability;
                        }
                        else {
                            numeratorProduct *= .00001;
                        }
                    }
                }
                
                // Factor in prior probability
                numeratorProduct *= priorProbabilities[classIterator];
                
                // Final calculation
                thisProbability = numeratorProduct / denominatorProduct;
                
                // Compare class probability against other classes
                if ((thisProbability) > greatestProbability) {
                    
                    greatestProbability = thisProbability;
                    classIndexOfGreatestProbability = classIterator;
                }
            }
            
            // Assign dataPoint to proper class
            processedPointsList.get(classIndexOfGreatestProbability).add(testingDataSet.getDataPoint(pointIterator));
        }
        
        //=============================================
        
        // Run method to print metrics
        printMetrics();
    }
    
    /**
     * Separates training dataset into lists by class value
     */
    protected void separateTrainingSet() {
        
        // Initialize processed datapoint arraylist to size of number of unique classes
        separatedPointsList.clear();
        for (int addedLists = 0; addedLists < classCounter; addedLists++) {
            ArrayList<DataPoint> listToAdd = new ArrayList<>();
            separatedPointsList.add(listToAdd);
        }
        
        String thisPointClass;
        DataPoint thisDataPoint;
        int totalPointsToExamine = this.trainingDataSet.getDataSet().size();
        
        // Put each point where it belongs (cheating for testing purposes)
        for (int pointIterator = 0; pointIterator < totalPointsToExamine; pointIterator++) {
            
            thisDataPoint = trainingDataSet.getDataPoint(pointIterator);
            thisPointClass = thisDataPoint.getFeatures()[classIndex];
            separatedPointsList.get(observedClasses.indexOf(thisPointClass)).add(thisDataPoint);
        }
    }
    
    /**
     * Find the number of possible unique values that an attribute may have at an index
     * @param attributeIndex
     * @return 
     */
    protected final int numberOfFeatureValues(int attributeIndex) {
        
        ArrayList<String> theseObservedAttributeValues = new ArrayList<>();
        
        for (DataPoint point : dataSet.getDataSet()) {
            
            if (!theseObservedAttributeValues.contains(point.getFeatures()[attributeIndex])) {
                
                theseObservedAttributeValues.add(point.getFeatures()[attributeIndex]);
            }
        }
        
        observedAttributeValues.set(attributeIndex, theseObservedAttributeValues);
        return theseObservedAttributeValues.size();
    }
    
    /**
     * Hacky method to compile metrics without printing
     * @return array of (numCorrectPoints, numTotalPoints, numCorrectPointsClass 0 .. numCorrectPointsClass n)
     */
    protected int[] calculateMetrics() {
        
        // Track the total percentage of correct points 
        int correctPointCounter = 0;
        int totalPointCounter = 0;
        
        // We'll also track the percentage of correct points by class
        int[] metricArray = new int[classCounter+2];
        
        // For every list of classified data points
        int classListLength;
        for (int listIterator = 0; listIterator < processedPointsList.size(); listIterator++) {
            
            // Take the size of that list of classified data points
            classListLength = processedPointsList.get(listIterator).size();
            
            // Iterate through this list of classified data points
            for (int pointIterator = 0; pointIterator < classListLength; pointIterator++) {
                
                // Track the number of points, correctly-classified points, and correctly-classified points per class
                totalPointCounter++;
                String thisPointClass = processedPointsList.get(listIterator).get(pointIterator).getFeatures()[classIndex];
                if (thisPointClass.equals(observedClasses.get(listIterator))) {
                    
                    correctPointCounter++;
                    metricArray[listIterator+2] = (metricArray[listIterator+2] + 1);
                }
            }
        }
        
        metricArray[0] = correctPointCounter;
        metricArray[1] = totalPointCounter;
        
        return metricArray;
    }
    
    /**
     * Call method to calculate metrics, then print them.
     */
    public void printMetrics() {
        
        int[] calculatedMetrics = calculateMetrics();
        
        // Get the total percentage of correct points 
        int correctPointCounter = calculatedMetrics[0];
        int totalPointCounter = calculatedMetrics[1];
        
        // Get the percentage of correct points by class
        int[] correctPointsByClass = Arrays.copyOfRange(calculatedMetrics, 2, calculatedMetrics.length);
        
        // Build results to output
        String resultString =
                    "Naive Bayes Classification of " + dataSetName + ":\n"
                        +  "   =========================\n"
                        +  "   Total points: " + totalPointCounter + "\n"
                        +  "   Correct points: " + correctPointCounter + "\n"
                        +  "   Percentage correct: " + ((double) correctPointCounter 
                                / (double) totalPointCounter * (double) 100.0) + "%\n"
                        +  "   Random chance?: " + ((double) 1.0 / (double) observedClasses.size() * (double) 100.0) + "%\n"
                        +  "   =========================\n";
        
        // Add results per each class
        for (int i = 0; i < classCounter; i++) {
            
            resultString += "   Total classified as " + translateClassName(i) + ": "
                    + processedPointsList.get(i).size() + "\n";
            if (!processedPointsList.get(i).isEmpty()) {
                resultString += "   Percentage correct (" + translateClassName(i) + "): " 
                        + ((double)correctPointsByClass[i] / (double)processedPointsList.get(i).size() * 100.0)
                        + "%\n";
            }
            else {
                resultString += "   Percentage correct (" + translateClassName(i) + "): 0%\n";
            }
        }
        resultString += "   =========================\n";
        
        // Print results
        System.out.println(resultString + "\n");
    }
    
    /**
     * Several of the targeted datasets use numerical values to represent class names.
     * This method translates these values into their relevant names.
     * @param classIndex
     * @return 
     */
    public String translateClassName(int classIndex) {
        
        String untranslatedName = observedClasses.get(classIndex);
        String translatedName;
        switch (dataSetName) {
            
            case "breast-cancer-wisconsin.data.txt":
                switch (untranslatedName) {
                    
                    case "2":
                        translatedName = "benign";
                        break;
                    case "4":
                        translatedName = "malignant";
                        break;
                    default:
                        translatedName = untranslatedName;
                        break;
                }
                break;
                
            case "house-votes-84.data.txt":
                translatedName = untranslatedName;
                break;
                
            case "soybean-small.data.txt":
                translatedName = untranslatedName;
                break;
                
            case "glass.data.txt":
                switch (untranslatedName) {
                    
                    case "1":
                        translatedName = "building_windows_float_processed";
                        break;
                    case "2":
                        translatedName = "building_windows_non_float_processed";
                        break;
                    case "3":
                        translatedName = "vehicle_windows_float_processed";
                        break;
                    case "4":
                        translatedName = "vehicle_windows_non_float_processed";
                        break;
                    case "5":
                        translatedName = "containers";
                        break;
                    case "6":
                        translatedName = "tableware";
                        break;
                    case "7":
                        translatedName = "headlamps";
                        break;
                    default:
                        translatedName = untranslatedName;
                        break;
                }
                break;
                
            case "iris.data.txt":
                translatedName = untranslatedName;
                break;
                
            default:
                translatedName = untranslatedName;
                break;
        }
        
        return translatedName;
    }
}
