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
package client;

import algorithms.*;
import data_components.ProcessData;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInput;
import java.io.ObjectInputStream;
import java.io.ObjectOutput;
import java.io.ObjectOutputStream;
import java.io.OutputStream;

/**
 * Main program driver for GUI creation
 * @author nwmoore, Matthew Rohrlach
 */
public class Main {
    
    // Global variables
    private boolean saveEnabled = true;
    private boolean loadEnabled = false;
    
    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        new Main().go();
    }
    
    /**
     * A non-static instance of main
     */
    public void go(){
        
        // ================
        
        // Call GUI
//        GUI gui = new GUI();
//        gui.buildMainGUI();
        
        // ===Test Code====
        
        ProcessData processedData = null;
        String dataSetFileName;
        
        dataSetFileName = "breast-cancer-wisconsin.data.txt";
//        dataSetFileName = "house-votes-84.data.txt";
//        dataSetFileName = "soybean-small.data.txt";
//        dataSetFileName = "glass.data.txt";
//        dataSetFileName = "iris.data.txt";
        
        // Load a pre-saved processed dataset, if possible and loading is enabled
        if (loadEnabled) {
            try {

                InputStream dataSetFile = new FileInputStream(dataSetFileName + ".sav");
                InputStream dataSetBuffer = new BufferedInputStream(dataSetFile);
                try (ObjectInput dataSetInput = new ObjectInputStream(dataSetBuffer)) {
                    processedData = (ProcessData) dataSetInput.readObject();
                }
                processedData.printDataSet();

            } 
            catch (IOException | ClassNotFoundException er) {
                 System.out.println("Processed data to restore not found or not readable!");
            }
        }
        
        // If no data set was loaded, attempt to build the relevant dataset
        if (processedData == null || !processedData.initialized) {
            if (dataSetFileName.contains("glass") || dataSetFileName.contains("soybean")) {
                // 3-bin discretization
                processedData = new ProcessData(dataSetFileName, false);
            }
            else {
                // 10-bin discretization
                processedData = new ProcessData(dataSetFileName, true);
            }
            
            if (processedData.initialized) {
            
                // Save the processed dataset to a file if successful and saving is enabled
                if (saveEnabled) {
                    try {

                    OutputStream dataSetFile = new FileOutputStream(dataSetFileName + ".sav");
                    OutputStream dataSetBuffer = new BufferedOutputStream(dataSetFile);
                    try (ObjectOutput dataSetOutput = new ObjectOutputStream(dataSetBuffer)) {
                        dataSetOutput.writeObject(processedData);
                    }

                    } 
                    catch (IOException ew) {
                        System.out.println("IO Error: Could not write processed data.");
                    }
                }
            }
        }
        
        // If there is an initialized dataset after loading or building, proceed with algorithms
        if (processedData.initialized) {
            
            // Do stuff
            ID3 iD3Algorithm = new ID3(processedData);
            NaiveBayes nBAlgorithm = new NaiveBayes(processedData);
            KNearestNeighbors kNN = new KNearestNeighbors(processedData);
            TANaiveBayes tANBAlgorithm = new TANaiveBayes(processedData);
//            Example exampleAlgorithm = new Example(processedData);
        }
    }
}
