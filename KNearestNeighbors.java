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
import data_components.knn.DistanceCompare;
import data_components.knn.Result;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
 
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

    protected int k = 5;

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

        // Constructor calls final method to classify dataset
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
        //String[] features;
        List<Result> result = new ArrayList<>();

        // Run algorithm
        // Data points are added to their respective lists
        //================================================
//        for (int i = 0; i < trainingDataSet.size(); i++) {
//            features = trainingDataSet.getDataPoint(i).getFeatures();
//        }

        for (int i = 0; i < testingDataSet.size(); i++) {
            for (int j = 0; j < trainingDataSet.size(); j++) {

                double dist = 0.0;
                for (String feature : testingDataSet.getDataPoint(i).getFeatures()) {
                    dist = getVDM(testingDataSet.getDataPoint(i), trainingDataSet.getDataPoint(j));
                
                double distance = Math.pow(dist, 2);
                result.add(new Result(distance, trainingDataSet.getDataPoint(i)));
                }   
            }
        }

        Collections.sort(result, new DistanceCompare());
        Result[] sorted = new Result[k];
        
        for (int i = 0; i < sorted.length; i++) {
            sorted[i] = result.get(i);
           // processedPointsList.add(sorted[]);
        }
        
        
        //System.out.println("Class of new instance is:" + majClass);

        //=============================================
        // Run method to print metrics;
        printMetrics();
    }

    public double getVDM(DataPoint train1, DataPoint train2) {

        double dist = 0.0;
        double train1AttCount = 0.0;
        double train1ClassCount = 0.0;
        double train2AttCount = 0.0;
        double train2ClassCount = 0.0;

        for (int i = 0; i < train1.getFeatures().length; i++) {
            train1AttCount = train1AttCount + findAttCount(train1);
            train1ClassCount += train1ClassCount + findClassCount(train1);
        }

        for (int i = 0; i < train2.getFeatures().length; i++) {
            train2AttCount = train2AttCount + findAttCount(train2);
            train2ClassCount = train2AttCount + findClassCount(train2);
        }

        for (String feature : train2.getFeatures()) {
            //if (train2.getFeatures().length == train1.getFeatures().length) {
                dist += Math.abs(((train1AttCount == 0.0) ? 0.0 : (train1ClassCount / train1AttCount)) - ((train2AttCount == 0.0) ? 0.0 : train2ClassCount / train2AttCount));
            //}
        }

        return dist;
    }

    private double findAttCount(DataPoint in) {
        //creates a counter that adds one for every attribute
        int count = 0;

        for (int i = 0; i < in.getFeatures().length; i++) {
                for (String feature : in.getFeatures()) {
                   count++;
            }
        }
        return count;
    }

    private double findClassCount(DataPoint in) {
        //creates a counter that adds 1 for every attribute in a particular class
        int count = 0;

        for (int i = 0; i < in.getFeatures().length; i++) {
                for (String feature : in.getFeatures()) {
                        if (in.getFeatures()[classIndex] == null ? in.getFeatures()[classIndex] == null : in.getFeatures()[classIndex].equals(in.getFeatures()[classIndex])) {
                             count++;
                        }
                }
        }

        return count;
    }


//    private static Result findMajority(Result[] ar) {
//        Set<Result> hash = new HashSet<>(Arrays.asList(ar));
//
//        Result[] uniqueVals = hash.toArray(new Result[0]);
//
//        int[] count = new int[uniqueVals.length];
//
//        for (int i = 0; i < uniqueVals.length; i++) {
//            for (int j = 0; j < ar.length; j++) {
//                if (ar[j].equals(uniqueVals[i])) {
//                    count[i]++;
//                }
//            }
//        }
//
//        int max = count[0];
//
//        for (int i = 1; i < count.length; i++) {
//            if (count[i] > max) {
//                max = count[i];
//            }
//        }
//
//        int freq = 0;
//
//        //find out how many times max appears
//        for (int i = 0; i < count.length; i++) {
//            if (count[i] == max) {
//                freq++;
//            }
//        }
//
//        int index = -1;
//
//        if (freq == 1) {
//            for (int i = 0; i < count.length; i++) {
//                if (count[i] == max) {
//                    index = i;
//                    break;
//                }
//            }
//            return uniqueVals[index];
//        } else {
//            int[] x = new int[freq];
//            int y = 0;
//            for (int i = 0; i < count.length; i++) {
//                if (count[i] == max) {
//                    x[y] = i;
//                    y++;
//                }
//            }
//
//            Random genRand = new Random();
//            int rIndex = genRand.nextInt(x.length);
//            int newIndex = x[rIndex];
//
//            return uniqueVals[newIndex];
//        }
//
//    }

    // Method that prints metrics
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
                = "Example Classification of " + dataSetName + ":\n"
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
