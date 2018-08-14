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
import data_components.knn.Result;

import java.util.ArrayList;

/**
 *
 * @author Emmanuel Armstrong
 */
public class KNearestNeighbors {

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

    // Global variable that stores the index of DataPoints' actual class values
    protected final int classIndex;

    // Global variable that stores the number of unique classes in the DataSet
    protected int classCounter;

    // Global variable that stores the names of classes that correspond to lists in array
    protected ArrayList<String> observedClasses;

    // Global variable that stores the results given from the result class into a list
    protected ArrayList<Result> resultList;

    // Global variable that sets k for k Nearest Neighbor
    protected int k = 5;

    // creates an array of the results with the top k nearest neighbor
    protected Result[] knn;

    // Constructor that takes a ProcessData object
    public KNearestNeighbors(ProcessData processData) {

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

        processedPointsList = new ArrayList<>();

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
        initializeProcessedPointLists();

        for (int i = 0; i < testingDataSet.getDataSet().size(); i++) {
            // Use VDM to find distances
            getVDM(testingDataSet.getDataPoint(i));
            // Sort the distances from closest to furthest
            quickSort(resultList, 0, resultList.size() - 1);
            // Get K nearest neighbors
            setKNN();
            // Classify the point with majority class of neighbors
            String classification = majorityKNN();
            // Add result to list for metric calculations
            processedPointsList.get(observedClasses.indexOf(classification)).add(testingDataSet.getDataPoint(i));

        }

        //=============================================
        // Run method to print metrics;
        
        printMetrics();
    }

    /**
     * This method calculates the Value Difference Metric for the test dataPoint
     *
     * @param test
     */
    public void getVDM(DataPoint test) {
        resultList = new ArrayList<>();
        String[] testFeatures = test.getFeatures();
        String[] trainingFeatures;
        DataPoint train;
        double dist;
        Result distResult = null;
        double testAttCount;
        double testClassCount;
        double trainAttCount;
        double trainClassCount;
        
        // Find distance from test point to every training point
        for (int i = 0; i < trainingDataSet.size(); i++) {
            dist = 0.0;
            train = trainingDataSet.getDataPoint(i);
            
            // For each feature
            for (int j = 0; j < testFeatures.length; j++) {
                trainingFeatures = train.getFeatures();

                // For each class
                for (int x = 0; x < observedClasses.size(); x++) {
                    
                    //find number of points with same value for the current attribute
                    testAttCount = findAttCount(testFeatures[j], j);  
                    //find number of points with same value for the current attribute and same class as current class
                    testClassCount = findClassCount(testFeatures[j], testFeatures[classIndex], j); 
                    trainAttCount = findAttCount(trainingFeatures[j], j);
                    trainClassCount = findClassCount(trainingFeatures[j], trainingFeatures[classIndex], j);

                    //Value Difference Metric distance formula
                    dist += Math.pow(Math.abs((testClassCount / testAttCount) - (trainClassCount / trainAttCount)), 2);
                    
                    // Create new DataPoint distance pair
                    distResult = new Result(dist, train);
                }
            }
            //add the resulting distance and DataPoint to the resultList
            resultList.add(distResult); 
        }
    }

    /**
     * This method sets the 5 K Nearest Neighbors to the first five Results in
     * the sorted resultList
     *
     */
    private void setKNN() {
        knn = new Result[k];
        for (int i = 0; i < k; i++) {
            knn[i] = resultList.get(i);
        }
    }

    /**
     * Returns the majority class of the k nearest neighbors
     *
     * @return
     */
    private String majorityKNN() {
        int majorityClassIndex = 0;
        int majorityClassValue = Integer.MIN_VALUE;
        int[] classArray = new int[observedClasses.size()];
        
        // Count the occurnce of each class in the k nearest neighbors
        for (int i = 0; i < observedClasses.size(); i++) {
            String observed = observedClasses.get(i);
            
            for (int j = 0; j < knn.length; j++) {
                String nnClass = knn[j].getDataPoint().getFeatures()[classIndex];
                if (observed.equals(nnClass)) {
                    classArray[i]++;
                }
            }
        }
        
        // Get index of majority class
        for (int x = 0; x < classArray.length; x++) {
            
            if (classArray[x] > majorityClassValue) {
                majorityClassIndex = x;
                majorityClassValue = classArray[x];
            }
        }
        
        return observedClasses.get(majorityClassIndex);
    }

    /**
     * Finds number of points with same value for the current attribute
     *
     * @param value
     * @param index
     * @return double
     */
    private double findAttCount(String value, int index) {
        double count = 0.0;

        for (DataPoint dataPoint : trainingDataSet.getDataSet()) {
            if (dataPoint.getFeatures()[index].equals(value)) {
                count++;
            }
        }

        return count;
    }

    /**
     * Finds number of points with same value for the current attribute and same class as current class
     *
     * @param value
     * @param classVal
     * @param index
     * @return double
     */
    private double findClassCount(String value, String classVal, int index) {
        double count = 0.0;

        for (DataPoint dataPoint : trainingDataSet.getDataSet()) {
            if (dataPoint.getFeatures()[index].equals(value) && dataPoint.getFeatures()[classIndex].equals(classVal)) {
                count++;
            }
        }

        return count;
    }

    /**
     * Simple quickSort function to sort the array of results from
     * closest to furthest.
     *
     * @param results
     * @param low
     * @param high
     */
    public void quickSort(ArrayList<Result> results, int low, int high) {
        // Stopping cases
        if (results == null || results.isEmpty()) {
            return;
        }
        
        if (low >= high) {
            return;
        }
        
        // Find middle index and the pivot value
        int middle = low + (high - low) / 2;
        double pivot = results.get(middle).getDistance();
        
        // make left < pivot and right > pivot
        int i = low, j = high;
        while(i <= j) {
            while(results.get(i).getDistance() < pivot) {
                i++;
            }
            
            while (results.get(j).getDistance() > pivot) {
                j--;
            }
            
            if (i <= j) {
                Result temp = results.get(i);
                results.set(i, results.get(j));
                results.set(j, temp);
                i++;
                j--;
            }
        }
        
        // recursively sort two sub list
        if (low < j) {
            quickSort(results, low, j);
        }
        
        if (high > i) {
            quickSort(results, i, high);
        }
    }

    /**
     * Clear the lists of processed points
     */
    protected void initializeProcessedPointLists() {

        // Initialize processed datapoint arraylist to size of number of unique classes
        processedPointsList.clear();
        for (int addedLists = 0; addedLists < classCounter; addedLists++) {
            ArrayList<DataPoint> listToAdd = new ArrayList<>();
            processedPointsList.add(listToAdd);
        }
    }

    /**
     * Print the metrics
     */
    public void printMetrics() {

        // Track the total percentage of correct points 
        int correctPointCounter = 0;
        int totalPointCounter = 0;

        // We'll also track the percentage of correct points by class
        int[] correctPointsByClass = new int[classCounter];

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
                    correctPointsByClass[listIterator] = (correctPointsByClass[listIterator] + 1);
                }
            }
        }

        // Build results to output
        String resultString
                = "KNN Classification of " + dataSetName + ":\n"
                + "   =========================\n"
                + "   Total points: " + totalPointCounter + "\n"
                + "   Correct points: " + correctPointCounter + "\n"
                + "   Percentage correct: " + ((double) correctPointCounter
                / (double) totalPointCounter * 100.0) + "%\n"
                + "   =========================\n";

        // Add results per each class
        for (int i = 0; i < classCounter; i++) {

            resultString += "   Total classified as " + translateClassName(i) + ": "
                    + processedPointsList.get(i).size() + "\n";
            if (!processedPointsList.get(i).isEmpty()) {
                resultString += "   Percentage correct (" + translateClassName(i) + "): "
                        + ((double) correctPointsByClass[i] / (double) processedPointsList.get(i).size() * 100.0)
                        + "%\n";
            } else {
                resultString += "   Percentage correct (" + translateClassName(i) + "): 0%\n";
            }
        }
        resultString += "   =========================\n";

        // Print results
        System.out.println(resultString + "\n");
    }

    /**
     * Several of the targeted datasets use numerical values to represent class
     * names. This method translates these values into their relevant names.
     *
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
