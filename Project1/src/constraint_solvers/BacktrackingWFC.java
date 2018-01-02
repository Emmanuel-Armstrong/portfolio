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
import java.util.ArrayList;
import ui.GraphColorVisualizer;

/**
 *
 * @author nwmoore
 */
public class BacktrackingWFC extends Backtracking {


    /**
     * Non visualizer constructor
     *
     * @param inGraph
     * @param inMaxSteps
     */
    public BacktrackingWFC(GraphColorGraph inGraph, int inMaxSteps) {
        super(inGraph, inMaxSteps);
    }

    /**
     * Visualizer constructor
     *
     * @param inGraph
     * @param inMaxSteps
     * @param inVisualizer
     */
    public BacktrackingWFC(GraphColorGraph inGraph, int inMaxSteps, GraphColorVisualizer inVisualizer) {
        super(inGraph, inMaxSteps, inVisualizer);
    }

    /**
     * Main starting method to solve via backtracking with FC
     *
     * @param graph
     * @return boolean true if colored false if not
     */
    @Override
    public boolean backtrackSolver(GraphColorGraph graph) {
        numIterations = 0;
        
        //Update visualizer title
        if (visualizer != null) {
            visualizer.updateVisualizerTitle("Backtracking with Forward Check (" + graph.getNumPoints() 
                    + " vertices, " + graph.getSegmentList().size() + 
                    " segments, " +graph.getNumColors() + " colors)");
        }
        
        vertexList = graph.getVertices();
        unassignedVerts = (ArrayList<GraphColorVertex>) vertexList.clone();
        graph.initializeColorDomains();
        
        if (!backtrackWFC(graph)) {
            graph.reportColored(false);
            return false;
        }
        graph.reportColored(true);
        return true;
    }

    /**
     * Backtracking  with FC method called recursively 
     *
     * @param graph
     * @return boolean true if graph is colored
     */
    private boolean backtrackWFC(GraphColorGraph graph) {
        if (unassignedVerts.isEmpty() || graph.isColored()) {
            return true;
        }
        
        if (numIterations > maxSteps) {
            return false;
        }
        
        numIterations++;
        
        GraphColorVertex vertex = getBestVertex();
        ArrayList<Color> colors = vertex.getColorDomain();
        Color possibleColor;
        System.out.println("Vertex choosen " + vertex.getKey());
        System.out.println("Vertex color domain " + vertex.getColorDomain());
        
        for (int i = 0; i < colors.size(); i++) {
            possibleColor = colors.get(i);
            
            if (!isConflicted(vertex, possibleColor)) {
               
                if (fowardCheck(vertex, possibleColor)) {
                    vertex.setColor(possibleColor);
                    System.out.println("Vertex colored " + graph.translateColor(vertex.getColor()));
                    updateVisualizer();
                    unassignedVerts.remove(vertex);
                    
                    if (backtrackWFC(graph)) {
                        return true;
                    }
                }

                vertex.setColor(Color.WHITE);
                restoreDomain(vertex, possibleColor);
            }
        }
        unassignedVerts.add(vertex);
        return false;
    }

    /**
     * Performs forward checking on the given vertex
     * 
     * @param vertex
     * @param possibleColor
     * @return true if no domains reduced to zero
     */
    private boolean fowardCheck(GraphColorVertex vertex, Color possibleColor) {
        ArrayList<Integer> connectedVertKeys = vertex.getConnections();
        GraphColorVertex connectedVert;
        ArrayList<Color> connectedVertDomain;
        Color colorToCheck;

        for (int i = 0; i < connectedVertKeys.size(); i++) {
            connectedVert = graph.getVertex(connectedVertKeys.get(i));
            connectedVertDomain = (ArrayList<Color>) connectedVert.getColorDomain().clone();
            
            if (!unassignedVerts.contains(connectedVert)) {
                continue;
            }
            
            for (int j = 0; j < connectedVertDomain.size(); j++) {
                colorToCheck = connectedVertDomain.get(j);
            
                if (fowardCheckConflicted(possibleColor, colorToCheck)) {
                    System.out.println("Neighbors domain before FC " + connectedVert.getColorDomain());
                    connectedVertDomain.remove(j);
                    connectedVert.setColorDomain(connectedVertDomain);
                    System.out.println("Neighbors domain after FC " + connectedVert.getColorDomain());
                }
                
                if (connectedVertDomain.isEmpty()) {
                    return false;
                }
            }
        }
        return true;
    }

    /**
     * Checks to see if the two colors are the same
     * 
     * @param possibleVertColor
     * @param colorToCheck
     * @return true if colors are equal 
     */
    private boolean fowardCheckConflicted(Color possibleVertColor, Color colorToCheck) {
        return possibleVertColor.equals(colorToCheck);
    }

    /**
     * Restores the domain of the given vertex after a forward check failure
     * 
     * @param vertex
     * @param colorToAdd
     */
    private void restoreDomain(GraphColorVertex vertex, Color colorToAdd) {
        ArrayList<Integer> connectedVertKeys = vertex.getConnections();
        GraphColorVertex connectedVert;
        ArrayList<Color> connectedVertDomain;
        
        for (int i = 0; i < connectedVertKeys.size(); i++) {
            connectedVert = graph.getVertex(connectedVertKeys.get(i));
            connectedVertDomain = (ArrayList<Color>) connectedVert.getColorDomain().clone();
        
            if (unassignedVerts.contains(connectedVert) && !connectedVertDomain.contains(colorToAdd)) {
            
                if (graph.isColorOfVertexAllowed(connectedVert, colorToAdd)) {
                    connectedVertDomain.add(colorToAdd);
                    connectedVert.setColorDomain(connectedVertDomain);
                }
            }
        }
    }
}
