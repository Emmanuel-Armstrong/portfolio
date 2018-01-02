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
import java.util.ArrayList;
import java.util.Random;
import java.awt.Color;
import java.lang.reflect.InvocationTargetException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.SwingUtilities;
import ui.GraphColorVisualizer;

/**
 *
 * @author nwmoore
 */
public class MinConflict {

    //min conflict
    GraphColorGraph graph;
    int maxSteps;
    int numIterations;
    ArrayList<GraphColorVertex> vertexList;
    Random rand = new Random();
    ArrayList<GraphColorVertex> currentConflictedVerts;
    GraphColorVisualizer visualizer;

    /**
     * Main constructor
     * @param inGraph
     * @param inMaxSteps
     */
    public MinConflict(GraphColorGraph inGraph, int inMaxSteps) {
        graph = inGraph;
        maxSteps = inMaxSteps;
    }
    
    /**
     * Visualizer constructor
     * @param inGraph
     * @param inMaxSteps
     * @param inVisualizer
     */
    public MinConflict(GraphColorGraph inGraph, int inMaxSteps, GraphColorVisualizer inVisualizer) {
        graph = inGraph;
        maxSteps = inMaxSteps;
        visualizer = inVisualizer;
    }

    /**
     * Main starting method to solve via min conflicts
     * @return boolean true if colored false if not
     */
    public boolean colorMinConflicts() {
        
        // Update visualizer title
        if (visualizer != null) {
            visualizer.updateVisualizerTitle("Min-Conflicts (" + graph.getNumPoints() 
                    + " vertices, " + graph.getSegmentList().size() + 
                    " segments, " +graph.getNumColors() + " colors)");
        }
        
        vertexList = graph.getVertices();
        currentConflictedVerts = vertexList;
        int randomIndex;
        GraphColorVertex chosenVertex = null;
        GraphColorVertex previouslyPickedVert;
        printConflictedVerts();
       
        for (numIterations = 0; numIterations < maxSteps; numIterations++) {
            previouslyPickedVert = chosenVertex;
        
            if (previouslyPickedVert != null)
                previouslyPickedVert.togglePickedLast(true);
            
            if (numIterations >= vertexList.size()) {
                if (graph.isColored()) {
                    return true;
                }
            }
            
            randomIndex = rand.nextInt(currentConflictedVerts.size());
            chosenVertex = currentConflictedVerts.get(randomIndex);
            System.out.println("Vertex choosen " + chosenVertex.getKey());
            
            // Assign a color to this vertex
            chosenVertex.setColor(conflicts(chosenVertex, graph.getColors()));
            System.out.println("Vertex colred " + graph.translateColor(chosenVertex.getColor()));
            // Update Visualizer if applicable
            updateVisualizer();
            
            currentConflictedVerts = updateConflictedVerts();
           
            printConflictedVerts();
            
            if (previouslyPickedVert != chosenVertex && previouslyPickedVert != null) {
                previouslyPickedVert.togglePickedLast(false);
            }

        }
        return false;
    }

    /**
     * Count number of conflicts for given vertex and assign color that
     * minimizes those conflicts
     *
     * @param vertex
     * @param colors
     * @return Color that minimizes conflicts for given vertex
     */
    private Color conflicts(GraphColorVertex vertex, ArrayList<Color> colors) {
        int[] numConflicts = new int[colors.size()];
        ArrayList<Integer> connectedVerts = vertex.getConnections();
        
        for (int i = 0; i < connectedVerts.size(); i++) {
        
            for (int j = 0; j < colors.size(); j++) {
            
                if (graph.getVertex(connectedVerts.get(i)).getColor() == colors.get(j)) {
                    numConflicts[j]++;
                }
            }
        }

        //Lists of minimal conflicted indexs corresponding to the index of a color
        ArrayList<Integer> minConflictedIndexs = new ArrayList();
        ArrayList<Integer> secondMinConflictedIndexs = new ArrayList();
        int minNumber = Integer.MAX_VALUE;
        int secondMin = minNumber;

        for (int k = 0; k < numConflicts.length; k++) {
           
            if (numConflicts[k] < minNumber) {
                secondMin = minNumber;
                minNumber = numConflicts[k];
            
            } else if (numConflicts[k] < secondMin && secondMin != numConflicts[k]) {
                secondMin = numConflicts[k];
            }
        }
        
        System.out.print("Number of conflicts ");
        
        for (int g =0; g < numConflicts.length; g++) {
        
            if(g == numConflicts.length - 1){
                System.out.println(numConflicts[g]);
            }
            else{
                System.out.print(numConflicts[g]);
            }
        }
       
        for (int l = 0; l < numConflicts.length; l++) {
        
            if (numConflicts[l] == minNumber) {
                minConflictedIndexs.add(l);
            }
            
            if (numConflicts[l] == secondMin) {
                secondMinConflictedIndexs.add(l);
            }
        }

        int colorIndex;
        
        if (vertex.getPreviouslyNonConflicted()) {
            vertex.togglePreviouslyNonConflicted(false);
            //switch to second least minimal color
        
            if (secondMinConflictedIndexs.size() == 1) {
                colorIndex = secondMinConflictedIndexs.get(0);
                return colors.get(colorIndex);
            } else {
                int randomNum = rand.nextInt(secondMinConflictedIndexs.size());
                colorIndex = secondMinConflictedIndexs.get(randomNum);
                return colors.get(colorIndex);
            }
        
        } else if (minConflictedIndexs.size() == 1) {
            colorIndex = minConflictedIndexs.get(0);
            return colors.get(colorIndex);
       
        } else {
            int randomNum = rand.nextInt(minConflictedIndexs.size());
            colorIndex = minConflictedIndexs.get(randomNum);
            return colors.get(colorIndex);
        }
        
    }

    /**
     * Updates the list of conflicted vertices
     * @return ArrayList<GraphColorVertex> containing vertices that are not colored 
     * or connected to a vertex with the same color
     */
    private ArrayList<GraphColorVertex> updateConflictedVerts() {
        ArrayList<GraphColorVertex> stillConflictedVerts = new ArrayList();
        
        for (int i = 0; i < vertexList.size(); i++) {
            GraphColorVertex vertexToCheck = vertexList.get(i);
        
            if (isConflicted(vertexToCheck)) {
                stillConflictedVerts.add(vertexToCheck);
            } else {
                stillConflictedVerts.remove(vertexToCheck);
                vertexToCheck.togglePreviouslyNonConflicted(true);
            }
        }
        return stillConflictedVerts;
    }

    /**
     * Checks if a vertex is conflicted
     * @param vertex
     * @return boolean true if conflicted false if not
     */
    private boolean isConflicted(GraphColorVertex vertex) {
        ArrayList<Integer> connectedVerts = vertex.getConnections();
       
        for (int i = 0; i < connectedVerts.size(); i++) {
        
            if (vertex.getColor() == Color.WHITE) {
                return true;
            }
            
            if (vertex.getColor() == graph.getVertex(connectedVerts.get(i)).getColor()) {
                return true;
            }
        }
        return false;
    }

    /**
     * Prints the list of conflicted vertices
     */
    private void printConflictedVerts() {
        System.out.print("Conflicted ");
       
        for (int i = 0; i < currentConflictedVerts.size(); i++) {
        
            if (i == currentConflictedVerts.size() - 1) {
                System.out.println(currentConflictedVerts.get(i).getKey());
            } else {
                System.out.print(currentConflictedVerts.get(i).getKey() + ", ");
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
     * @return int number of iterations
     */
    public int getNumIterations(){
        return numIterations;
    }
}
