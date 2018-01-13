/*
 * Copyright (C) 2016 Matthew Rohrlach
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

import constraint_solvers.*;
import graph.GraphColorGraph;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;
import ui.GUI;
import ui.GraphColorVisualizer;

/**
 * Handles all specific calls and calculations endemic to Project 1
 * @author Matthew Rohrlach
 */
public class ProjectOne implements LogicHandler{
    
    // Variables to pass as references
    GraphColorVisualizer visualizer;
    long visualizerSpeed;
    boolean visualizerActivated;
    GUI gui;
    
    // Variables to use for graph-building
    final int numGraphs;
    
    // Variables to use for constraint-solvers
    int numAttempts;
    private int numSolved = 0;
    private int numColors = 4;
    private int loopCount = 0;
    
    
    private ArrayList<GraphColorGraph[]> graphSetList;
    private GraphColorGraph[] graphSet10;
    private GraphColorGraph[] graphSet20;
    private GraphColorGraph[] graphSet30;
    private GraphColorGraph[] graphSet40;
    private GraphColorGraph[] graphSet50;
    private GraphColorGraph[] graphSet60;
    private GraphColorGraph[] graphSet70;
    private GraphColorGraph[] graphSet80;
    private GraphColorGraph[] graphSet90;
    private GraphColorGraph[] graphSet100;
    
    
    public ProjectOne(boolean inVisualizerActivated, 
            long visualizerSpeedIn, int inNumGraphs, int inNumAttempts,
            GUI inGUI, boolean threeColorIn, boolean fourColorIn){
        visualizerActivated = inVisualizerActivated;
        visualizerSpeed = visualizerSpeedIn;
        gui = inGUI;
        numGraphs = inNumGraphs;
        numAttempts = inNumAttempts;
        
        if (fourColorIn && threeColorIn){
            numColors = 4;
            loopCount++;
        }
        else if (fourColorIn && !threeColorIn){
            numColors = 4;
        }
        else if (!fourColorIn && threeColorIn){
            numColors = 3;
        }
    }
    
    public void solve(){
        // Do Project 1
        
        if (visualizerActivated){
            visualizer = new GraphColorVisualizer(null, visualizerSpeed);
            visualizer.startVisualizer();
        }
        
        // Allocate space for all graphs
        graphSet10 = new GraphColorGraph[numGraphs/10];
        graphSet20 = new GraphColorGraph[numGraphs/10];
        graphSet30 = new GraphColorGraph[numGraphs/10];
        graphSet40 = new GraphColorGraph[numGraphs/10];
        graphSet50 = new GraphColorGraph[numGraphs/10];
        graphSet60 = new GraphColorGraph[numGraphs/10];
        graphSet70 = new GraphColorGraph[numGraphs/10];
        graphSet80 = new GraphColorGraph[numGraphs/10];
        graphSet90 = new GraphColorGraph[numGraphs/10];
        graphSet100 = new GraphColorGraph[numGraphs/10];
        
        graphSetList = new ArrayList();
        graphSetList.add(graphSet10);
        graphSetList.add(graphSet20);
        graphSetList.add(graphSet30);
        graphSetList.add(graphSet40);
        graphSetList.add(graphSet50);
        graphSetList.add(graphSet60);
        graphSetList.add(graphSet70);
        graphSetList.add(graphSet80);
        graphSetList.add(graphSet90);
        graphSetList.add(graphSet100);
        
        // Initialize all graphs
        int totalGraphsBuilt = 0;
        printToGUI("\nBuilding set of " + numGraphs + " graphs.");
        for(int graphSetNumber = 0; graphSetNumber < 10; graphSetNumber++) {
            for (int graphIndex = 0; graphIndex < (numGraphs/10); graphIndex++) {
                graphSetList.get(graphSetNumber)[graphIndex] = new GraphColorGraph(((graphSetNumber*10)+10), numColors);
                graphSetList.get(graphSetNumber)[graphIndex].buildGraph();
            }
            printToGUI("Created " + graphSetList.get(graphSetNumber).length 
                    + " graphs with " + ((graphSetNumber*10)+10) + " vertices.");
        }
        for(int graphSetNumber = 0; graphSetNumber < 10; graphSetNumber++) {
            for (int graphIndex = 0; graphIndex < (numGraphs/10); graphIndex++) {
                graphSetList.get(graphSetNumber)[graphIndex].buildGraph();
                totalGraphsBuilt++;
            }
            printToGUI("Built " + graphSetList.get(graphSetNumber).length 
                    + " graphs with " + ((graphSetNumber*10)+10) + " vertices.");
        }
        printToGUI("Finished building " + totalGraphsBuilt + " graphs.\n");
        
        
        int printNumber = 0;
        int verticesPerGraph = 10;
        int[] triesPerGraph;

        
        // Run regular backtracking
        triesPerGraph = new int[numGraphs/10];
        for(int graphSetNumber = 0; graphSetNumber < 10; graphSetNumber++) {
            for (int graphIndex = 0; graphIndex < (numGraphs/10); graphIndex++) {
                Backtracking solver = new Backtracking(graphSetList.get(graphSetNumber)[graphIndex], numAttempts, visualizer);
                if(solver.backtrackSolver(graphSetList.get(graphSetNumber)[graphIndex])) {
                    numSolved++;
                    triesPerGraph[graphIndex] = solver.getNumIterations();
                }
                printNumber++;
                if(printNumber == numGraphs*.1){
                    printNumber = 0;
                    calcMetrics(triesPerGraph, "Backtracking", verticesPerGraph, numSolved);
                    triesPerGraph = new int[numGraphs/10];
                    numSolved = 0;
                    verticesPerGraph += 10;
                }
            }
        }
        
        printToGUI("\n");
        verticesPerGraph = 10;
        numSolved = 0;
        resetGraphSetColors();

        // Run backtracking with forward check
        triesPerGraph = new int[numGraphs/10];
        for(int graphSetNumber = 0; graphSetNumber < 10; graphSetNumber++) {
            for (int graphIndex = 0; graphIndex < (numGraphs/10); graphIndex++) {
                BacktrackingWFC wfcSolver = new BacktrackingWFC(graphSetList.get(graphSetNumber)[graphIndex], numAttempts, visualizer);
                if(wfcSolver.backtrackSolver(graphSetList.get(graphSetNumber)[graphIndex])) {
                    numSolved++;
                    triesPerGraph[graphIndex] = wfcSolver.getNumIterations();
                }
                printNumber++;
                if(printNumber == numGraphs*.1){
                    printNumber = 0;
                    calcMetrics(triesPerGraph, "Backtracking with Forward Check", verticesPerGraph, numSolved);
                    triesPerGraph = new int[numGraphs/10];
                    numSolved = 0;
                    verticesPerGraph += 10;
                }
            }
        }
        
        printToGUI("\n");
        verticesPerGraph = 10;
        numSolved = 0;
        resetGraphSetColors();

        // Run backtracking with MAC
        triesPerGraph = new int[numGraphs/10];
        for(int graphSetNumber = 0; graphSetNumber < 10; graphSetNumber++) {
            for (int graphIndex = 0; graphIndex < (numGraphs/10); graphIndex++) {
                BacktrackingMAC macSolver = new BacktrackingMAC(graphSetList.get(graphSetNumber)[graphIndex], numAttempts, visualizer);
                if(macSolver.backtrackSolver(graphSetList.get(graphSetNumber)[graphIndex])) {
                    numSolved++;
                    triesPerGraph[graphIndex] = macSolver.getNumIterations();
                }
                printNumber++;
                if(printNumber == numGraphs*.1){
                    printNumber = 0;
                    calcMetrics(triesPerGraph, "Backtracking with MAC", verticesPerGraph, numSolved);
                    triesPerGraph = new int[numGraphs/10];
                    numSolved = 0;
                    verticesPerGraph += 10;
                }
            }
        }
        
        printToGUI("\n");
        verticesPerGraph = 10;
        numSolved = 0;
        resetGraphSetColors();

        // Run min-conflicts
        triesPerGraph = new int[numGraphs/10];
        for(int graphSetNumber = 0; graphSetNumber < 10; graphSetNumber++) {
            for (int graphIndex = 0; graphIndex < (numGraphs/10); graphIndex++) {
                MinConflict minSolver = new MinConflict(graphSetList.get(graphSetNumber)[graphIndex], numAttempts, visualizer);
                if(minSolver.colorMinConflicts()) {
                    numSolved++;
                    triesPerGraph[graphIndex] = minSolver.getNumIterations();
                }
                printNumber++;
                if(printNumber == numGraphs*.1){
                    printNumber = 0;
                    calcMetrics(triesPerGraph, "Min-Conflicts", verticesPerGraph, numSolved);
                    triesPerGraph = new int[numGraphs/10];
                    numSolved = 0;
                    verticesPerGraph += 10;
                }
            }
        }
        
        printToGUI("\n");
        verticesPerGraph = 10;
        numSolved = 0;
        resetGraphSetColors();
        
        // Run genetic algorithm
        triesPerGraph = new int[numGraphs/10];
        for(int graphSetNumber = 0; graphSetNumber < 10; graphSetNumber++) {
            for (int graphIndex = 0; graphIndex < (numGraphs/10); graphIndex++) {
                NouveauGeneticAlgorithm ngaSolver = new NouveauGeneticAlgorithm(graphSetList.get(graphSetNumber)[graphIndex],
                        numAttempts*10, 1000, 20,
                        4, visualizer);
                if(ngaSolver.solve()) {
                    numSolved++;
                    triesPerGraph[graphIndex] = ngaSolver.getNumIterations();
                }
                printNumber++;
                if(printNumber == numGraphs*.1){
                    printNumber = 0;
                    calcMetrics(triesPerGraph, "Genetic Algorithm", verticesPerGraph, numSolved);
                    triesPerGraph = new int[numGraphs/10];
                    numSolved = 0;
                    verticesPerGraph += 10;
                }
            }
        }
        
        printToGUI("\n");
        verticesPerGraph = 10;
        numSolved = 0;
        resetGraphSetColors();

        try {
            if (visualizer.isRunning())Thread.sleep(10000);
            visualizer.endVisualizer();
        } catch (InterruptedException ex) {
            Logger.getLogger(Main.class.getName()).log(Level.SEVERE, null, ex);
        }
        
        if (loopCount > 0) {
            loopCount--;
            numColors = 3;
            solve();
        }
    }
    
    
    /**
     * Print metrics of a graphSet
     * @param data
     * @param type
     * @param numVertices
     * @param numSolved 
     */
    public void calcMetrics(int[] data, String type, int numVertices, int numSolved){
        
        int min = Integer.MAX_VALUE;
        int max = Integer.MIN_VALUE;
        int sum = 0;
        for (int i = 0; i < data.length; i++){
            if (data[i] > max){
                max = data[i];
            }
            if (data[i] < min && data[i] != 0){
                min = data[i];
            }
            sum += data[i];
            
        }
        
        int average = 0;
        if (numSolved != 0){
            average = sum / numSolved;
        }
        
        printToGUI("\n" + type + " " + numVertices + ": ");
        printToGUI("Number of colors: " + numColors);
        printToGUI("Graphs solved: " + numSolved);
        printToGUI("total tries: " + sum);
        if (!(min >= 2147483000)){
            printToGUI("min tries: " + min);
        }
        else {
            printToGUI("min tries: No successes");
        }
        if (max != Integer.MAX_VALUE){
            printToGUI("max tries: " + max);
        }
        else {
            printToGUI("max tries: -1");
        }
        printToGUI("avg tries: " + average);
    }
    
    public void resetGraphSetColors(){
        for(int graphSetNumber = 0; graphSetNumber < 10; graphSetNumber++) {
            for (int graphIndex = 0; graphIndex < (numGraphs/10); graphIndex++) {
                graphSetList.get(graphSetNumber)[graphIndex].buildColors(numColors);
            }
        }
    }
    
    public void printToGUI(String stringToPrint) {
        System.out.println(stringToPrint);
        if (gui != null) {
            gui.printToOutputBox(stringToPrint);
        }
    }
}
