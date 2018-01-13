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

// TO-DO: Make these more precise when project is completed
import graph.*;
import constraint_solvers.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import ui.*;

/**
 * Main program driver for GUI creation
 * @author Matthew Rohrlach
 */
public class Main {
    private int numSolved = 0;
    final int numGraphs = 1;
    private int numColors = 4;
    private GraphColorGraph[] graphSet = new GraphColorGraph[1];
       

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        new Main().go();
    }
    
    public void go(){
        
//        GraphColorVisualizer testVisual = new GraphColorVisualizer(null, 0);
//        testVisual.startVisualizer();
//        GraphColorGraph graph = new GraphColorGraph(5,4);
//        graphSet[0] = graph;
//        graphSet[0].buildGraph();
//        System.out.println("Output for Genetic Algorithm");
//        NouveauGeneticAlgorithm ngaSolver = new NouveauGeneticAlgorithm(graphSet[0], 6000, 100, testVisual);
//        ngaSolver.solve();
//        
//        try {
//            Thread.sleep(20000);
//        } catch (InterruptedException ex) {
//            Logger.getLogger(Main.class.getName()).log(Level.SEVERE, null, ex);
//        }
//        graph = new GraphColorGraph(5,4);
//        graphSet[0] = graph;
//        graphSet[0].buildGraph();
//        System.out.println("Output for Backtracking");
//        Backtracking btSolver = new Backtracking(graphSet[0], 6000, testVisual);
//        btSolver.backtrackSolver(graphSet[0]);
//        
//        try {
//            Thread.sleep(20000);
//        } catch (InterruptedException ex) {
//            Logger.getLogger(Main.class.getName()).log(Level.SEVERE, null, ex);
//        }
//        resetGraphSetColors();
//        System.out.println("\n\n");
//        
//        System.out.println("Output for Backtracking /W FC");
//        BacktrackingWFC fcSolver = new BacktrackingWFC(graphSet[0], 6000, testVisual);
//        fcSolver.backtrackSolver(graphSet[0]);
//        
//        try {
//            Thread.sleep(20000);
//        } catch (InterruptedException ex) {
//            Logger.getLogger(Main.class.getName()).log(Level.SEVERE, null, ex);
//        }
//        resetGraphSetColors();
//        System.out.println("\n\n");
//        
//        System.out.println("Output for Backtracking /W MAC");
//        BacktrackingMAC macSolver = new BacktrackingMAC(graphSet[0], 6000, testVisual);
//        macSolver.backtrackSolver(graphSet[0]);
//        
//        try {
//            Thread.sleep(20000);
//        } catch (InterruptedException ex) {
//            Logger.getLogger(Main.class.getName()).log(Level.SEVERE, null, ex);
//        }
//        resetGraphSetColors();
//        System.out.println("\n\n");
//        
//        System.out.println("Output for Min Conflicts");
//        MinConflict mcSolver = new MinConflict(graphSet[0], 6000, testVisual);
//        mcSolver.colorMinConflicts();
//        
//        int verticesPerGraph = 10;
//        System.out.println("Building set of " + numGraphs + " graphs...");
//        graphSet = new GraphColorGraph[numGraphs];
//        for (int i = 0; i < numGraphs; i++){
//            switch (i) {
//                // 10% complete (20 vertices)
//                case ((int)(numGraphs*.1-1)):
//                    verticesPerGraph = 20;
//                    System.out.println("Starting " + verticesPerGraph + " vertex graphs.");
//                    break;
//                    
//                // 20% complete (30 vertices)
//                case ((int)(numGraphs*.2-1)):
//                    verticesPerGraph = 30;
//                    System.out.println("Starting " + verticesPerGraph + " vertex graphs.");
//                    break;
//                    
//                // 30% complete (40 vertices)
//                case ((int)(numGraphs*.3-1)):
//                    verticesPerGraph = 40;
//                    System.out.println("Starting " + verticesPerGraph + " vertex graphs.");
//                    break;
//                    
//                // 40% complete (50 vertices)
//                case ((int)(numGraphs*.4-1)):
//                    verticesPerGraph = 50;
//                    System.out.println("Starting " + verticesPerGraph + " vertex graphs.");
//                    break;
//                    
//                // 50% complete (60 vertices)
//                case ((int)(numGraphs*.5-1)):
//                    verticesPerGraph = 60;
//                    System.out.println("Starting " + verticesPerGraph + " vertex graphs.");
//                    break;
//                    
//                // 60% complete (70 vertices)
//                case ((int)(numGraphs*.6-1)):
//                    verticesPerGraph = 70;
//                    System.out.println("Starting " + verticesPerGraph + " vertex graphs.");
//                    break;
//                    
//                // 70% complete (80 vertices)
//                case ((int)(numGraphs*.7-1)):
//                    verticesPerGraph = 80;
//                    System.out.println("Starting " + verticesPerGraph + " vertex graphs.");
//                    break;
//                    
//                // 80% complete (90 vertices)
//                case ((int)(numGraphs*.8-1)):
//                    verticesPerGraph = 90;
//                    System.out.println("Starting " + verticesPerGraph + " vertex graphs.");
//                    break;
//                    
//                // 90% complete (100 vertices)
//                case ((int)(numGraphs*.9-1)):
//                    verticesPerGraph = 100;
//                    System.out.println("Starting " + verticesPerGraph + " vertex graphs.");
//                    break;
//                    
//                // 100% complete (end)
//                case ((int)(numGraphs-1)):
//                    System.out.println("Completed " + graphSet.length + " graphs.");
//                    break;
//            }
//            
//            graphSet[i] = new GraphColorGraph(verticesPerGraph, numColors);
//            graphSet[i].buildGraph();
//        }
//        
//        int printNumber = 0;
//        verticesPerGraph = 10;
//
//        int[] macTries = new int[numGraphs];
//        for (int i = 0; i < numGraphs; i++){
//            BacktrackingMAC macSolver = new BacktrackingMAC(graphSet[i], 6000, testVisual);
//            if(macSolver.backtrackSolver(graphSet[i])) {
//                numSolved++;
//                macTries[i] = macSolver.getNumIterations();
//            }
//            printNumber++;
//            if(printNumber == numGraphs*.1){
//                printNumber = 0;
//                calcMetrics(macTries, "Backtracking with MAC", verticesPerGraph, numSolved);
//                macTries = new int[numGraphs];
//                numSolved = 0;
//                verticesPerGraph += 10;
//            }
//        }
//        
//        System.out.println("\n");
//        verticesPerGraph = 10;
//        numSolved = 0;
//        resetGraphSetColors();
//
//        int[] fcTries = new int[numGraphs];
//        for (int i = 0; i < numGraphs; i++){
//            BacktrackingWFC btwfcSolver = new BacktrackingWFC(graphSet[i], 6000, testVisual);
//            if(btwfcSolver.backtrackSolver(graphSet[i])) {
//                numSolved++;
//                fcTries[i] = btwfcSolver.getNumIterations();
//            }
//            printNumber++;
//            if(printNumber == numGraphs*.1){
//                printNumber = 0;
//                calcMetrics(fcTries, "Backtracking with Forward Check", verticesPerGraph, numSolved);
//                fcTries = new int[numGraphs];
//                numSolved = 0;
//                verticesPerGraph += 10;
//            }
//        }
//        
//        System.out.println("\n");
//        verticesPerGraph = 10;
//        numSolved = 0;
//        resetGraphSetColors();
//
//        int[] btTries = new int[numGraphs];
//        for (int i = 0; i < numGraphs; i++){
//            Backtracking btSolver = new Backtracking(graphSet[i], 6000, testVisual);
//            if(btSolver.backtrackSolver(graphSet[i])) {
//                numSolved++;
//                btTries[i] = btSolver.getNumIterations();
//            }
//            printNumber++;
//            if(printNumber == numGraphs*.1){
//                printNumber = 0;
//                calcMetrics(btTries, "Backtracking", verticesPerGraph, numSolved);
//                btTries = new int[numGraphs];
//                numSolved = 0;
//                verticesPerGraph += 10;
//            }
//        }
//        
//        System.out.println("\n");
//        verticesPerGraph = 10;
//        numSolved = 0;
//        resetGraphSetColors();
//
//        int[] mcTries = new int[numGraphs];
//        for (int i = 0; i < numGraphs; i++){
//            graphSet[i].buildGraph();
//            MinConflict minSolver = new MinConflict(graphSet[i], 6000, testVisual);
//            if(minSolver.colorMinConflicts()) {
//                numSolved++;
//                mcTries[i] = minSolver.getNumIterations();
//            }
//            printNumber++;
//            if(printNumber == numGraphs*.1){
//                printNumber = 0;
//                calcMetrics(mcTries, "Min-Conflict", verticesPerGraph, numSolved);
//                mcTries = new int[numGraphs];
//                numSolved = 0;
//                verticesPerGraph += 10;
//            }
//        }
//
//        try {
//            if (testVisual.isRunning())Thread.sleep(50000);
//            testVisual.endVisualizer();
//        } catch (InterruptedException ex) {
//            Logger.getLogger(Main.class.getName()).log(Level.SEVERE, null, ex);
//        }
        
        
        // ================
        
        // Call GUI
        GUI gui = new GUI();
        gui.buildMainGUI();
        
        // GUI builds graphs and saves them
        
        // Jpanel GUI points to functions and parameters
        
        // Functions do work on saved graphs
        
        // GUI reports outputs
    }
    
    /**
     * Calculate metrics
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
        
        
        System.out.println("\n" + type + " " + numVertices + ": ");
        System.out.println("Number of colors: " + numColors);
        System.out.println("Graphs solved: " + numSolved);
        System.out.println("total tries: " + sum);
        if (min != 2147483647){
            System.out.println("min tries: " + min);
        }
        else {
            System.out.println("min tries: no successes");
        }
        if (max != Integer.MAX_VALUE){
            System.out.println("max tries: " + max);
        }
        else {
            System.out.println("max tries: " + -1);
        }
        System.out.println("avg tries: " + average);
    }
    
    /**
     * Reset graph colors
     */
    public void resetGraphSetColors(){
        for (int i = 0; i < numGraphs; i++){
            graphSet[i].buildColors(numColors);
        }
    }
}
