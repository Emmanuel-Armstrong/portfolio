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
package data_components;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Random;

/**
 *
 * @author nwmoore
 */
public class ProcessData implements Serializable{

    private DataSet dataSet = new DataSet();
    protected final String fileName;
    public final boolean initialized;
    protected int classIndex;
    protected boolean tenBins;
    
    /**
     * Main constructor to process data. Discretizes continuous values
     * and replaces missing values
     * @param fileIn
     */
    public ProcessData(String fileIn, boolean tenBinsIn){
        
        fileName = fileIn;
        boolean tenBins = tenBinsIn;
        
        // Read the file line by line
        try {

            FileReader fileReader = new FileReader(fileName);
            BufferedReader bufferReader = new BufferedReader(fileReader);
            String line;
            
            // Make a DataPoint object from each line and add it
            // to the DataSet
            while ((line = bufferReader.readLine()) != null) {
                line = line.trim();
                
                // Handle ID number fields in certain datasets
                if (fileName.equals("breast-cancer-wisconsin.data.txt") || fileName.equals("glass.data.txt")) {
                    
                    int indexOfFirstComma = line.indexOf(",");
                    line = line.substring(indexOfFirstComma+1);
                }
                DataPoint dataPoint = new DataPoint(line.split(","));
                dataSet.addToSet(dataPoint);
            }

        } catch (Exception e) {
            System.err.println("Error while reading file line by line:" + e.getMessage());
            initialized = false;
            return;
        }
        
        // Store the index of all DataPoints' actual class
        if (!fileName.equals("house-votes-84.data.txt")) {
            this.classIndex = (dataSet.getDataPoint(0).getFeatures().length - 1);
        }
        else {
            this.classIndex = (0);
        }

        // Replace missing values
        if (!fileName.equals("house-votes-84.data.txt")) {
            fixMissingValues();
        }
        
        // Discretize continuous values
        makeDiscrete();
        dataSet.initialize10FoldSet();
        dataSet.printSet();
        initialized = true;
    }

    /**
     * Makes continuous values discrete by putting
     * them into one of three bins, low, medium, high
     */
    public final void makeDiscrete() {
        
        // Get first DataPoint
        DataPoint firstDataPoint = dataSet.getDataPoint(0);
        String[] features = firstDataPoint.getFeatures();
        
        double max;
        double min;
        double currentValue;
        double splitValue;
        
        // Split into ten bins
        if (tenBins) {
            
            double attoCap;
            double femtoCap;
            double picoCap;
            double nanoCap;
            double microCap;
            double milliCap;
            double centiCap;
            double deciCap;
            double dekaCap;
            //double hectoCap;

            for (int i = 0; i < features.length; i++) {

                // If DataPoint contains a continuous value make it discrete
                if (features[i].contains(".")) {
                    max = Double.MIN_VALUE;
                    min = Double.MAX_VALUE;

                    // Calculate the minimum and maximum value of the feature
                    for (int j = 0; j < dataSet.size(); j++) {
                        currentValue = Double.parseDouble(dataSet.getDataPoint(j).getFeatures()[i]);
                        if (currentValue > max) {
                            max = currentValue;
                        }
                        if (currentValue < min) {
                            min = currentValue;
                        }
                    }
                
                    // Split the range between min and max into 10 bins
                    splitValue = (max - min) / 10;
                    attoCap = min + splitValue;
                    femtoCap = attoCap + splitValue;
                    picoCap = femtoCap + splitValue;
                    nanoCap = picoCap + splitValue;
                    microCap = nanoCap + splitValue;
                    milliCap = microCap + splitValue;
                    centiCap = milliCap + splitValue;
                    deciCap = centiCap + splitValue;
                    dekaCap = deciCap + splitValue;

                    // Place feature value into appropriate discrete bin
                    for (int k = 0; k < dataSet.size(); k++) {
                        currentValue = Double.parseDouble(dataSet.getDataPoint(k).getFeatures()[i]);
                        if (currentValue <= attoCap) {
                            dataSet.getDataPoint(k).getFeatures()[i] = "atto";
                        } else if (currentValue > attoCap && currentValue <= femtoCap) {
                            dataSet.getDataPoint(k).getFeatures()[i] = "femto";
                        } else if (currentValue > femtoCap && currentValue <= picoCap) {
                            dataSet.getDataPoint(k).getFeatures()[i] = "pico";
                        } else if (currentValue > picoCap && currentValue <= nanoCap) {
                            dataSet.getDataPoint(k).getFeatures()[i] = "nano";
                        } else if (currentValue > nanoCap && currentValue <= microCap) {
                            dataSet.getDataPoint(k).getFeatures()[i] = "micro";
                        } else if (currentValue > microCap && currentValue <= milliCap) {
                            dataSet.getDataPoint(k).getFeatures()[i] = "milli";
                        } else if (currentValue > milliCap && currentValue <= centiCap) {
                            dataSet.getDataPoint(k).getFeatures()[i] = "centi";
                        } else if (currentValue > centiCap && currentValue <= deciCap) {
                            dataSet.getDataPoint(k).getFeatures()[i] = "deci";
                        } else if (currentValue > deciCap && currentValue <= dekaCap) {
                            dataSet.getDataPoint(k).getFeatures()[i] = "deka";
                        } else {
                            dataSet.getDataPoint(k).getFeatures()[i] = "hecto";
                        }
                    }
                }
            }
        }
        
        // Split into 3 bins
        else {
            
            double low;
            double medium;

            for (int i = 0; i < features.length; i++) {

                // If DataPoint contains a continuous value make it discrete
                if (features[i].contains(".")) {
                    max = Double.MIN_VALUE;
                    min = Double.MAX_VALUE;

                    // Calculate the minimum and maximum value of the feature
                    for (int j = 0; j < dataSet.size(); j++) {
                        currentValue = Double.parseDouble(dataSet.getDataPoint(j).getFeatures()[i]);
                        if (currentValue > max) {
                            max = currentValue;
                        }
                        if (currentValue < min) {
                            min = currentValue;
                        }
                    }
                
                    // Split the range between min and max into 10 bins
                    splitValue = (max - min) / 3;
                    low = min + splitValue;
                    medium = low + splitValue;

                    // Place feature value into appropriate discrete bin
                    for (int k = 0; k < dataSet.size(); k++) {
                        currentValue = Double.parseDouble(dataSet.getDataPoint(k).getFeatures()[i]);
                        if (currentValue <= low) {
                            dataSet.getDataPoint(k).getFeatures()[i] = "low";
                        } else if (currentValue > low && currentValue <= medium) {
                            dataSet.getDataPoint(k).getFeatures()[i] = "medium";
                        } else {
                            dataSet.getDataPoint(k).getFeatures()[i] = "high";
                        }
                    }
                }
            }
        }
    }
    
    /**
     * Replaces the missing values in the DataSet.
     * using the mean value of that feature
     */
    public final void fixMissingValues() {
        DataPoint dataPoint;
        String[] features;
        
        // Grab each DataPoint
        for (int i = 0; i < dataSet.size(); i++) {
            dataPoint = dataSet.getDataPoint(i);
            features = dataPoint.getFeatures();

            // If a feature value is missing replace using mean feature value of class
            for (int j = 0; j < features.length; j++) {
                if (features[j].equals("?")) {
//                    features[j] = Integer.toString(calculateMean(j));
//                    features[j] = calculateProbabilisticFeature(j);
                    features[j] = Integer.toString(calculateMeanForClass(j,dataPoint));
                }
            }
        }
    }
    
    /**
     * Returns the mean of the feature at the given index.
     * Downside: Reduces variablity
     * @param index
     * @return 
     */
    public int calculateMean(int index) {
        int numDataPoints = 0;
        int totalFeatureValue = 0;
        double unRoundedMean = 0;
        String[] features;
        
        
        // Calculate the total value of the feature
        for (int i = 0; i < dataSet.size(); i++) {
            features = dataSet.getDataPoint(i).getFeatures();
            if (!features[index].equals("?")) {
                totalFeatureValue += Integer.parseInt(features[index]);
            }
            numDataPoints++;
        }
        
        // Calculate the mean and then round to an int
        unRoundedMean = (double) totalFeatureValue / (double) numDataPoints;
        int mean = (int) Math.round(unRoundedMean);
        return mean;
    }
    
    /**
     * Returns the mean of the feature at the given index with respect to the class of the missing value.
     * @param index
     * @param dataPoint
     * @return 
     */
    public int calculateMeanForClass(int index, DataPoint dataPoint) {
        int numDataPoints = 0;
        int totalFeatureValue = 0;
        double unRoundedMean = 0;
        String classValueToAccept = dataPoint.getFeatures()[this.classIndex];
        String dataSetClassValue;
        String[] features;
        
        // Calculate the total value of the feature for datapoints with the same class
        for (int i = 0; i < dataSet.size(); i++) {
            
            // If the class is a match... 
            dataSetClassValue = dataSet.getDataPoint(i).getFeatures()[this.classIndex];
            if (dataSetClassValue.equals(classValueToAccept)) {
                
                // ...and the feature value is known...
                features = dataSet.getDataPoint(i).getFeatures();
                if (!features[index].equals("?")) {
                    
                    // ...factor the datapoint into the calculation
                    totalFeatureValue += Integer.parseInt(features[index]);
                }
                numDataPoints++;
            }
        }
        
        // Calculate the mean and then round to an int
        unRoundedMean = (double) totalFeatureValue / (double) numDataPoints;
        int mean = (int) Math.round(unRoundedMean);
        return mean;
    }
    
    /**
     * Returns a random value chosen from all known values.
     * Works best when the majority of values of a feature are known.
     * @param index
     * @return 
     */
    public String calculateProbabilisticFeature(int index) {
        ArrayList<String> listOfFeatures = new ArrayList<>();
        String[] features;
        Random rand = new Random();
        
        // Add every known value to a list
        for (int i = 0; i < dataSet.size(); i++) {
            features = dataSet.getDataPoint(i).getFeatures();
            if (!features[index].equals("?")) {
                listOfFeatures.add(features[index]);
            }
        }
        
        // Return a randomly-chosen value from that list
        return listOfFeatures.get(rand.nextInt(listOfFeatures.size()));
    }
    
    /**
     * Print the dataSet associated with this ProcessData object
     */
    public void printDataSet() {
        
        this.dataSet.printSet();
    }
    
    /**
     * Returns the DataSet associated with this ProcessData object
     * @return 
     */
    public DataSet getDataSet() {
        
        return this.dataSet;
    }
    
    /**
     * Returns the file name of the DataSet associated with this ProcessData object
     * @return 
     */
    public String getDataSetName() {
        
        return this.fileName;
    }
    
    /**
     * Returns the DataPoint at the given index of the DataSet associated with this ProcessData object
     * @param index
     * @return 
     */
    public DataPoint getDataPoint(int index) {
        
        return this.dataSet.getDataPoint(index);
    }
    
    /**
     * Return the index of class values of the associated DataSet
     * @return 
     */
    public int getClassIndex() {
        
        return this.classIndex;
    }
}
