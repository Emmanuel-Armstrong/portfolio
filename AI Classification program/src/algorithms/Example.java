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

/**
 *
 * @author matthewrohrlach
 */
public class Example {
    
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
    
    // Constructor that takes a ProcessData object
    public Example (ProcessData processData) {
        
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
        
        // Run algorithm
        
        // Data points are added to their respective lists
        //================================================
        
        String thisPointClass;
        DataPoint thisDataPoint;
        int totalPointsToExamine = this.testingDataSet.getDataSet().size();
        
        // Put each point where it belongs (cheating for testing purposes)
        for (int pointIterator = 0; pointIterator < totalPointsToExamine; pointIterator++) {
            
            thisDataPoint = testingDataSet.getDataPoint(pointIterator);
            thisPointClass = thisDataPoint.getFeatures()[classIndex];
            processedPointsList.get(observedClasses.indexOf(thisPointClass)).add(thisDataPoint);
        }
        
        //=============================================
        
        // Run method to print metrics
        printMetrics();
    }
    
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
        String resultString =
                    "Example Classification of " + dataSetName + ":\n"
                        +  "   =========================\n"
                        +  "   Total points: " + totalPointCounter + "\n"
                        +  "   Correct points: " + correctPointCounter + "\n"
                        +  "   Percentage correct: " + ((double) correctPointCounter 
                                / (double) totalPointCounter * 100.0) + "%\n"
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
