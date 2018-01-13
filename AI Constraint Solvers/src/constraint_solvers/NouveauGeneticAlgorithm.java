/*
 * Copyright (C) 2016 matthewrohrlach
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
package constraint_solvers;

import graph.GraphColorGraph;
import graph.GraphColorVertex;
import java.awt.Color;
import java.util.ArrayList;
import java.util.Random;
import ui.GraphColorVisualizer;

/**
 *
 * @author matthewrohrlach
 * @author nwmoore
 */
public class NouveauGeneticAlgorithm {
    
    // Global variables:
    protected GraphColorGraph graphToColor;
    protected int maxAttempts;
    protected int numAttempts;
    protected int populationSize;
    protected int mutationPercent;
    protected int numToMutate;
    protected GraphColorVisualizer visualizer;
    
    ArrayList<GraphColorVertex> vertexList;
    ArrayList<GraphColorVertex> unassignedVerts;
    
    int[][] populationArray;
    double[] populationFitnesses;
    
    double averageFitness = 0.0;
    int bestFitnessIndex = 0;
    int lastBestFitnessIndex = 0;
    int bestFitnessIndexCycleCount = 0;
    double thisBestFitness = Integer.MIN_VALUE;
    double lastBestFitness = Integer.MIN_VALUE;
    
    ArrayList<Integer> parentPopulationList;
    ArrayList<Integer> childPopulationList;
    
    
    //=========Constructors================
    
    
    /**
     * Regular constructor
     * @param graphToColorIn
     * @param maxAttemptsIn 
     * @param populationSizeIn 
     * @param mutationPercentIn 
     */
    public NouveauGeneticAlgorithm(GraphColorGraph graphToColorIn, int maxAttemptsIn, 
            int populationSizeIn, int mutationPercentIn, int numToMutateIn) {
        
        // Initialize variables
        this.graphToColor = graphToColorIn;
        this.maxAttempts = maxAttemptsIn;
        this.populationSize = populationSizeIn;
        this.mutationPercent = mutationPercentIn;
        this.numToMutate = numToMutateIn;
        
        vertexList = graphToColor.getVertices();
        unassignedVerts = (ArrayList<GraphColorVertex>) vertexList.clone();
        
    }
    
    /**
     * Visualizer constructor
     * @param graphToColorIn
     * @param maxAttemptsIn
     * @param populationSizeIn
     * @param mutationPercentIn
     * @param visualizerIn 
     */
    public NouveauGeneticAlgorithm(GraphColorGraph graphToColorIn, int maxAttemptsIn, int populationSizeIn, 
            int mutationPercentIn, int numToMutateIn, GraphColorVisualizer visualizerIn) {
        
        // Initialize variables
        this.graphToColor = graphToColorIn;
        this.maxAttempts = maxAttemptsIn;
        this.visualizer = visualizerIn;
        this.populationSize = populationSizeIn;
        this.mutationPercent = mutationPercentIn;
        this.numToMutate = numToMutateIn;
        
        vertexList = graphToColor.getVertices();
        unassignedVerts = (ArrayList<GraphColorVertex>) vertexList.clone();
        
    }
    
    //=========Methods==================
    
    /**
     * Main method to start genetic algorithm with given parameters
     * @return 
     */
    public boolean solve() {
        
        visualizer.updateVisualizerTitle("Genetic Algorithm (" + graphToColor.getNumPoints() 
                    + " vertices, " + graphToColor.getSegmentList().size() + 
                    " segments, " + graphToColor.getNumColors() + " colors)");
        
        // Set the stage for the first run of crossover
        initializePopulation();
        
        numAttempts = 0;
        while (numAttempts <= maxAttempts && !graphToColor.isColored()) {
            doCrossover();
            assignFitnessToPopulation();
            numAttempts++;
        }
        
        if (graphToColor.isColored()) {
            System.out.println("Colored!");
            return true;
        }
        
        System.out.println("Not colored, too many attempts!");
        return false;
    }
    
    /**
     * Build a population of [populationSize] colorings of this graph.
     * Assign a fitness to each coloring, by total min-conflicts.
     */
    protected void initializePopulation() {
        
        populationArray = new int[populationSize][vertexList.size()];
        populationFitnesses = new double[populationSize];
        randomizePopulationColors();
        assignFitnessToPopulation();
        
    }
    
    /**
     * Assign a random color (an index in the color array) to every psuedo-vertex
     */
    protected void randomizePopulationColors() {
        
        Random rand = new Random();
        
        // For every vertex in our graph
        for (int i = 0; i < populationSize; i++) {
            
            for (int j = 0; j < vertexList.size(); j++) {
                
                populationArray[i][j] = rand.nextInt(graphToColor.getColors().size());
                
            }
            
        }
    }
    
    /**
     * Assign a fitness to every coloring in our population
     */
    protected void assignFitnessToPopulation() {
        
        ArrayList<Integer> connectedVerts;
        GraphColorVertex sourceVertex;
        GraphColorVertex connectedVertex;
        int connectedKey;
        double graphFitness;
        double vertexFitness;
        double totalFitness = 0;
        Color sourceVertexColor;
        int unavailableColorNumbers;
        
        for (int populationIndex = 0; populationIndex < populationSize; populationIndex++) {
            
            graphFitness = 0;
            applyPopulationColors(populationIndex);
            graphToColor.updateColorDomains();
            
            for (int i = 0; i < vertexList.size(); i++) {

                vertexFitness = 0;
                sourceVertex = vertexList.get(i);
                sourceVertexColor = sourceVertex.getColor();
                connectedVerts = sourceVertex.getConnections();

                for (int j = 0; j < connectedVerts.size(); j++) {

                    connectedKey = connectedVerts.get(j);
                    connectedVertex = graphToColor.getVertex(connectedKey);

                    if (sourceVertexColor.equals(connectedVertex.getColor())) {
                        vertexFitness--;
                    }
                }
                
                // Fitness decreases most with conflicting vertices of high degree and low domain size
                unavailableColorNumbers = graphToColor.getNumColors() - sourceVertex.getColorDomainSize();
                graphFitness = graphFitness + (vertexFitness * sourceVertex.getDegree() * unavailableColorNumbers);
            }
            
            // Fitness is the number of bad edges, or conflicting vertex pairs / 2
            populationFitnesses[populationIndex] = (graphFitness/2);
            totalFitness += (graphFitness/2);
        }
        
        averageFitness = totalFitness / populationSize;
        showFittestColoring();
    }
    
    /**
     * Build a subpopulation of parents through gladiatorial combat.
     * Build a subpopulation of children through genetic crossover.
     */
    protected void doCrossover() {
        
        Random rand = new Random();
        int parentOne;
        int parentTwo;
        
        childPopulationList = new ArrayList();
        int[] childOne;
        int[] childTwo;
        int crossoverPoint;
        boolean childOnePlaced;
        boolean childTwoPlaced;
        
        // Set up our list of parents for this iteration
        buildParentPopulation();
        
        // Take our list of parents and make children
        for (int i = 0; i < (populationSize/2); i++) {
            
            // Pick two random parents, initialize two children
            parentOne = parentPopulationList.get(rand.nextInt(parentPopulationList.size()));
            parentTwo = parentPopulationList.get(rand.nextInt(parentPopulationList.size()));
            childOne = new int[vertexList.size()];
            childTwo = new int[vertexList.size()];
            
            // The point at which we swap entries in our array
            crossoverPoint = rand.nextInt(vertexList.size());
            
            // Give each child their parents genetics initially
            for (int c = 0; c < crossoverPoint; c++) {
                
                childOne[c] = populationArray[parentOne][c];
                childTwo[c] = populationArray[parentTwo][c];
                
            }
            
            for (int c = crossoverPoint; c < (vertexList.size()); c++) {
                
                childOne[c] = populationArray[parentTwo][c];
                childTwo[c] = populationArray[parentOne][c];
                
            }
            
            childOnePlaced = false;
            childTwoPlaced = false;
            
            for (int p = 0; p < populationSize; p++) {
                
                if (populationArray[p][0] == -1) {
                    if (!childOnePlaced) {
                        
                        childPopulationList.add(p);
                        for (int cp = 0; cp < vertexList.size(); cp++) {
                            populationArray[p][cp] = childOne[cp];
                        }
                        childOnePlaced = true;
                        continue;
                        
                    }
                    else if (!childTwoPlaced) {
                        
                        childPopulationList.add(p);
                        for (int cp = 0; cp < vertexList.size(); cp++) {
                            populationArray[p][cp] = childTwo[cp];
                        }
                        childTwoPlaced = true;
                        continue;
                        
                    }
                    else if (childOnePlaced && childTwoPlaced) {
                        break;
                    }
                }
            }
        }
        
        //System.out.println("\nList of Parent Indexes: \n" + parentPopulationList);
        parentPopulationList.clear();
        mutateChildren();
        childPopulationList.clear();
        
    }
    
    /**
     * Build a subpopulation of parents through gladiatorial combat.
     */
    protected void buildParentPopulation() {
        parentPopulationList = new ArrayList();
        
        int myrmidonOneIndex;
        int myrmidonTwoIndex;
        double myrmidonOneFitness;
        double myrmidonTwoFitness;
        
        Random rand = new Random();
        
        while (parentPopulationList.size() < (populationSize/2)) {
            
            myrmidonOneIndex = rand.nextInt(populationSize);
            myrmidonTwoIndex = rand.nextInt(populationSize);
            
            myrmidonOneFitness = populationFitnesses[myrmidonOneIndex];
            myrmidonTwoFitness = populationFitnesses[myrmidonTwoIndex];
            
            // Myrmidon 1 is better
            if (myrmidonOneFitness > myrmidonTwoFitness) {
                parentPopulationList.add(myrmidonOneIndex);
            }
            
            // Myrmidon 2 is better
            else if (myrmidonOneFitness < myrmidonTwoFitness) {
                parentPopulationList.add(myrmidonTwoIndex);
            }
            
            // Whoever bleeds out first; a coin flip
            else {
                if (rand.nextDouble() < .5) {
                    parentPopulationList.add(myrmidonOneIndex);
                }
                else {
                    parentPopulationList.add(myrmidonTwoIndex);
                }
            }
            
        }
        
        for (int i = 0; i < populationSize; i++) {
            if (!parentPopulationList.contains(i)){
                for (int j = 0; j < vertexList.size(); j++) {
                    populationArray[i][j] = -1;
                }
            }
        }
        
    }
    
    /**
     * Mutate the children!
     */
    protected void mutateChildren() {
        
        Random rand = new Random();
        int mutatePoint;
        int mutationIncrease = 0;
        
        // Increase mutation chance if hitting a local optimum
        for (int i = 0; i < bestFitnessIndexCycleCount; i++) {
                mutationIncrease += 5;
        }
        
        // Up the mutation increase if at a local minimum
        for (int i = 0; i < childPopulationList.size(); i++) {
        
            // Mutate the given number of vertices in each child
            if (rand.nextInt(100) < mutationPercent + mutationIncrease) {
                for (int n = 0; n < (vertexList.size()/2); n++) {

                    mutatePoint = rand.nextInt(vertexList.size());

                    populationArray[childPopulationList.get(i)][mutatePoint] =
                            rand.nextInt(graphToColor.getColors().size());

                }
            }
        }
        
    }
    
    /**
     * Set the graph to the colors of this coloring
     * @param populationIndex 
     */
    protected void applyPopulationColors(int populationIndex) {
        
        Color colorToSet;
        for (int j = 0; j < vertexList.size(); j++) {
                
            colorToSet = graphToColor.getColors().get(populationArray[populationIndex][j]);
            vertexList.get(j).setColor(colorToSet);
                
        }
        
    }
    
    /**
     * Report the best fitness of the current population
     */
    protected void showFittestColoring() {
        
        lastBestFitnessIndex = bestFitnessIndex;
        bestFitnessIndex = 0;
        
        // Track historical fitnesses for local optimum detection
        lastBestFitness = thisBestFitness;
        thisBestFitness = Integer.MIN_VALUE;
        
        double currentFitness;
        
        for (int populationFitnessIterator = 0; populationFitnessIterator < populationSize; populationFitnessIterator++) {
            
            currentFitness = populationFitnesses[populationFitnessIterator];
            
            if (currentFitness > thisBestFitness) {
                
                thisBestFitness = currentFitness;
                bestFitnessIndex = populationFitnessIterator;
                
            }
        }
        
        System.out.println("Best fitness of population " + (numAttempts+1) + ": " 
                + (int)thisBestFitness + ", Index: " + bestFitnessIndex + ", Average fitness: " +
                averageFitness);
        
        // Track cycles of best fitness indices for local optimum detection
        if (lastBestFitness == thisBestFitness) {
            bestFitnessIndexCycleCount++;
        }
        else {
            bestFitnessIndexCycleCount = 0;
        }
        
        applyPopulationColors(bestFitnessIndex);
        updateVisualizer();
        
        
    }
    
    protected Color getRandomColor() {
        Random rand = new Random();
        int colorIndex = rand.nextInt(graphToColor.getColors().size());
        return (graphToColor.getColors().get(colorIndex));
    }
    
    /**
     * Updates the visualizer so the coloring process can be seen
     */
    protected void updateVisualizer() {
        
        if (visualizer != null && visualizer.isRunning()) {
            visualizer.updateVisualizer(graphToColor);
        }
    }

    /**
     * Prints the number of iterations needed to color the graph
     *
     * @return int number of iterations
     */
    public int getNumIterations() {
        return numAttempts;
    }
    
}
