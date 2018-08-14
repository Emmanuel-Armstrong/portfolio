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

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Random;

/**
 *
 * @author nwmoore
 */
public class DataSet implements Serializable{
    ArrayList<DataPoint> dataSet;
    protected DataSet[] dataSet10;
    protected boolean tenFoldInitialized;
    
    /**
     * Main constructor.
     */
    public DataSet() {
        dataSet = new ArrayList<>();
        dataSet10 = new DataSet[10];
        tenFoldInitialized = false;
    }
    
    /**
     * Adds a DataPoint to the DataSet
     * @param data
     */
    public void addToSet(DataPoint data) {
        dataSet.add(data);
    }
    
    /**
     * Prints the DataSet
     */
    public void printSet() {
        System.out.println();
        for (DataPoint point : dataSet) {
            point.printPoint();
            System.out.println();
        }
        System.out.println();
    }
    
    /**
     * Gets the DataSet 
     * @return 
     */
    public ArrayList<DataPoint> getDataSet() {
        return dataSet;
    }
    
    /**
     * Gets individual DataPoint at the given index
     * @param index
     * @return
     */
    public DataPoint getDataPoint(int index) {
        return dataSet.get(index);
    }
    
    /**
     * Return the size of the DataSet
     * @return 
     */
    public int size() {
        return dataSet.size();
    }
    
    /**
     * Initialize this dataset's set of ten unique subsamples.
     */
    public void initialize10FoldSet() {
        
        ArrayList<DataPoint> thisDataSetClone = new ArrayList<>(dataSet);
        DataPoint currentDataPoint;
        int sampleSize = (this.size() / 10);
        Random rand = new Random();
        
        // Initialize all ten subsamples
        for (int i = 0; i < 10; i++) {
            dataSet10[i] = new DataSet();
        }
        
        // For every subsample in the array of ten
        for (DataSet currentSample : dataSet10) {
            
            // Until the subsample size is reached
            for (int currentSampleSize = 0; currentSampleSize < sampleSize; currentSampleSize++) {
                
                // Quit if empty
                if (thisDataSetClone.isEmpty()) {
                    break;
                }
                
                // Then take a random datapoint to add to this sample
                currentDataPoint = thisDataSetClone.get(rand.nextInt(thisDataSetClone.size()));
                currentSample.addToSet(currentDataPoint);
                thisDataSetClone.remove(currentDataPoint);
            }
        }
        
        // If there are unaffiliated datapoints, distribute them randomly
        int randomSampleIndex;
        while (!thisDataSetClone.isEmpty()) {
            
            randomSampleIndex = rand.nextInt(10);
            currentDataPoint = thisDataSetClone.get(rand.nextInt(thisDataSetClone.size()));
            dataSet10[randomSampleIndex].addToSet(currentDataPoint);
            thisDataSetClone.remove(currentDataPoint);
        }
        
        tenFoldInitialized = true;
    }
    
    /**
     * Test if every datapoint's class matches the first datapoint's class
     * @return 
     */
    public boolean isPureSet(int classIndex) {
        
        String classToMatch = dataSet.get(0).getFeatures()[classIndex];
        for (DataPoint point : this.dataSet) {
            
            if (!point.getFeatures()[classIndex].equals(classToMatch)) {
                
                return false;
            }
        }
        return true;
    }
    
    /**
     * Get the subset that corresponds to a certain index. Create if necessary.
     * @param index
     * @return 
     */
    public DataSet getSubsetAt(int index) {
        
        if (tenFoldInitialized) {
            return this.dataSet10[index];
        }
        else {
            initialize10FoldSet();
            return this.dataSet10[index];
        }
    }
}
