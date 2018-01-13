/*
 * Copyright (C) 2016 nwmoore
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
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.SwingUtilities;
import ui.GraphColorVisualizer;

/**
 *
 * @author nwmoore
 */
public class Backtracking {

    //
    GraphColorGraph graph;
    GraphColorVisualizer visualizer;
    int maxSteps;
    int numIterations;
    ArrayList<GraphColorVertex> vertexList;
    ArrayList<GraphColorVertex> unassignedVerts;

    /**
     * Non visualizer constructor
     *
     * @param inGraph
     * @param inMaxSteps
     */
    public Backtracking(GraphColorGraph inGraph, int inMaxSteps) {
        maxSteps = inMaxSteps;
        graph = inGraph;
        vertexList = graph.getVertices();
        unassignedVerts = (ArrayList<GraphColorVertex>) vertexList.clone();
    }

    /**
     * Visualizer constructor
     *
     * @param inGraph
     * @param inMaxSteps
     * @param inVisualizer
     */
    public Backtracking(GraphColorGraph inGraph, int inMaxSteps, GraphColorVisualizer inVisualizer) {
        maxSteps = inMaxSteps;
        graph = inGraph;
        vertexList = graph.getVertices();
        unassignedVerts = (ArrayList<GraphColorVertex>) vertexList.clone();
        visualizer = inVisualizer;
    }

    /**
     * Main starting method to solve via backtracking
     *
     * @param graph
     * @return boolean true if colored false if not
     */
    public boolean backtrackSolver(GraphColorGraph graph) {
        numIterations = 0;
        
        // Update visualizer title
        if (visualizer != null) {
            visualizer.updateVisualizerTitle("Backtracking (" + graph.getNumPoints() 
                    + " vertices, " + graph.getSegmentList().size() + 
                    " segments, " +graph.getNumColors() + " colors)");
        }
        
        if (!backtrack(graph, graph.getColors(), getVertWithHighestDegree())) {
            graph.reportColored(false);
            return false;
        }
        
        graph.reportColored(true);
        return true;
    }

    /**
     * Backtracking method called recursively 
     *
     * @param graph
     * @param colors
     * @param vertex
     * @return boolean true if graph is colored
     */
    private boolean backtrack(GraphColorGraph graph, ArrayList<Color> colors, GraphColorVertex vertex) {
        if (vertex == null || graph.isColored()) {
            return true;
        }
        
        if (numIterations > maxSteps) {
            return false;
        }
        
        numIterations++;
        System.out.println("Vertex choosen " + vertex.getKey());
        
        // Test each color with the vertex parameter
        for (int i = 0; i < colors.size(); i++) {
            
            if (!isConflicted(vertex, colors.get(i))) {
                vertex.setColor(colors.get(i));
                System.out.println("Vertex colored " + graph.translateColor(vertex.getColor()));
                // Update Visualizer view if applicable
                updateVisualizer();

                unassignedVerts.remove(vertex);
            
                if (backtrack(graph, graph.getColors(), getBestVertex())) {
                    return true;
                }
                
                vertex.setColor(Color.WHITE);
                unassignedVerts.add(vertex);

            }
        }
        // If no vertex remaining has a possible color
        return false;
    }

    /**
     * Gets the best vertex to color next, either one with highest
     * degree or fewest possible colors
     * @return GraphColorVertex true if colored false if not
     */
    protected GraphColorVertex getBestVertex() {
        int possibleColors = 0;
        GraphColorVertex vertex;
        int[] conflicts;

        for (int i = 0; i < unassignedVerts.size(); i++) {
            vertex = unassignedVerts.get(i);
            conflicts = calcConflicts(vertex, graph.getColors());
            
            for (int j = 0; j < conflicts.length; j++) {
                
                if (conflicts[j] == 0) {
                    possibleColors++;
                }
            }
            
            vertex.setPossibleColors(possibleColors);
            possibleColors = 0;
        }
        
        int lowestPossibleColors = Integer.MAX_VALUE;
        GraphColorVertex possibleVert;
        //set to vert with most connections
        GraphColorVertex chosenVert = getVertWithHighestDegree();
        
        for (int k = 0; k < unassignedVerts.size(); k++) {
            possibleVert = unassignedVerts.get(k);
            possibleColors = possibleVert.getPossibleColors();
           
            if (possibleColors < lowestPossibleColors && possibleColors != graph.getColors().size()) {
                lowestPossibleColors = possibleColors;
                chosenVert = possibleVert;
            }
        }
        return chosenVert;
    }

    /**
     * Calculates the vertex's conflicts with each color
     *
     * @param vertex
     * @param colors
     * @return int[] containing the number of conflicts with each color in
     * colors
     */
    protected int[] calcConflicts(GraphColorVertex vertex, ArrayList<Color> colors) {
        int[] numConflicts = new int[colors.size()];
        ArrayList<Integer> connectedVerts = vertex.getConnections();
        for (int i = 0; i < connectedVerts.size(); i++) {
        
            for (int j = 0; j < colors.size(); j++) {
            
                if (graph.getVertex(connectedVerts.get(i)).getColor() == colors.get(j)) {
                    numConflicts[j]++;
                }
            }
        }
        return numConflicts;
    }

    /**
     * Gets the vertex with the highest degree
     *
     * @return GraphColorVertex first vertex with highest degree
     */
    protected GraphColorVertex getVertWithHighestDegree() {
        if (!unassignedVerts.isEmpty()) {
            GraphColorVertex highestDegreeVert = unassignedVerts.get(0);
            GraphColorVertex vertex;
            int vertexDegree;
            int highestDegree = Integer.MIN_VALUE;
            
            for (int i = 0; i < unassignedVerts.size(); i++) {
                vertex = unassignedVerts.get(i);
                vertexDegree = vertex.getConnections().size();
                if (vertexDegree > highestDegree) {
                    highestDegree = vertexDegree;
                    highestDegreeVert = vertex;
                }
            }
            return highestDegreeVert;
            
        } else {
            return null;
        }
    }
    
    /**
     * Orders the color domain of the given vertex. The color that 
     * that leaves the most options open for neighbors is first
     * @param vertex
     * @return GraphColorVertex first vertex with highest degree
     */
    protected ArrayList<Color> orderColorDomain(GraphColorVertex vertex) {
        ArrayList<Color> currentDomain = (ArrayList<Color>) vertex.getColorDomain().clone();
        ArrayList<Integer> connectedVerts = vertex.getConnections();
        Integer[][] data = new Integer[currentDomain.size()][2];
        ArrayList<Color> connectedVertDomain;
        Color colorToCheck;
        GraphColorVertex connectedVertex;

        for (int k = 0; k < currentDomain.size(); k++) {
            data[k] = new Integer[]{0, k};
        }

        for (int i = 0; i < connectedVerts.size(); i++) {
            connectedVertex = graph.getVertex(connectedVerts.get(i));
            connectedVertDomain = connectedVertex.getColorDomain();
           
            for (int j = 0; j < currentDomain.size(); j++) {
                colorToCheck = currentDomain.get(j);
            
                if (connectedVertDomain.contains(colorToCheck)) {
                    data[j][0]++;
                }
            }
        }

        Arrays.sort(data, new Comparator<Integer[]>() {
            @Override
            public int compare(Integer[] removals1, Integer[] removals2) {
                return removals1[0].compareTo(removals2[0]);
            }
        });

        ArrayList<Color> orderdDomain = new ArrayList();
        for (int x = 0; x < currentDomain.size(); x++) {

            orderdDomain.add(currentDomain.get(data[x][1]));
        }
        
        vertex.setColorDomain(orderdDomain);
        return vertex.getColorDomain();
    }

    /**
     * Checks to see if the color will cause a conflict for the vertex
     *
     * @param vertex
     * @param color
     * @return boolean true if conflict caused false if not
     */
    protected boolean isConflicted(GraphColorVertex vertex, Color color) {
        ArrayList<Integer> connectedVerts = vertex.getConnections();
       
        for (int i = 0; i < connectedVerts.size(); i++) {
        
            if (color.equals(graph.getVertex(connectedVerts.get(i)).getColor())) {
                return true;
            }
        }
        return false;
    }

    /**
     * Prints the list of vertices that have not been assigned a color
     */
    protected void printUnassingedVerts() {
        System.out.print("unassigned ");
        
        for (int i = 0; i < unassignedVerts.size(); i++) {
         
            if (i == unassignedVerts.size() - 1) {
                System.out.println(unassignedVerts.get(i).getKey());
            
            } else {
                System.out.print(unassignedVerts.get(i).getKey() + ", ");
            }
        }
    }

    /**
     * Updates the visualizer so the coloring process can be seen
     */
    protected void updateVisualizer() {
        
        if (visualizer != null && visualizer.isRunning()) {
            visualizer.updateVisualizer(graph);
                    
        }
    }

    /**
     * Prints the number of iterations needed to color the graph
     *
     * @return int number of iterations
     */
    public int getNumIterations() {
        return numIterations;
    }

}
