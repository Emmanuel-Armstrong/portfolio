/*
 * Copyright (C) 2016 Emmanuel
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
import graph.VertexPair;
import java.awt.Color;
import java.util.ArrayList;
import ui.GraphColorVisualizer;

/**
 *
 * @author nwmoore
 */
public class BacktrackingMAC extends Backtracking {

    ArrayList<Color> colorsRemoved = new ArrayList();

    /**
     * Non visualizer constructor
     *
     * @param inGraph
     * @param inMaxSteps
     */
    public BacktrackingMAC(GraphColorGraph inGraph, int inMaxSteps) {
        super(inGraph, inMaxSteps);
    }

    /**
     * Visualizer constructor
     *
     * @param inGraph
     * @param inMaxSteps
     * @param inVisualizer
     */
    public BacktrackingMAC(GraphColorGraph inGraph, int inMaxSteps, GraphColorVisualizer inVisualizer) {
        super(inGraph, inMaxSteps, inVisualizer);
    }

    /**
     * Main starting method to solve via backtracking with MAC
     *
     * @param graph
     * @return boolean true if colored false if not
     */
    @Override
    public boolean backtrackSolver(GraphColorGraph graph) {
        numIterations = 0;
        
        // Update visualizer title
        if (visualizer != null) {
            visualizer.updateVisualizerTitle("Backtracking with MAC (" + graph.getNumPoints() 
                    + " vertices, " + graph.getSegmentList().size() + 
                    " segments, " +graph.getNumColors() + " colors)");
        }
        
        vertexList = graph.getVertices();
        unassignedVerts = (ArrayList<GraphColorVertex>) vertexList.clone();
        graph.initializeColorDomains();
        
        if (!backtrackMAC(graph)) {
            graph.reportColored(false);
            return false;
        }
        graph.reportColored(true);
        return true;
    }

    /**
     * Backtracking  with MAC method called recursively 
     *
     * @param graph
     * @param colors
     * @param vertex
     * @return boolean true if graph is colored
     */
    private boolean backtrackMAC(GraphColorGraph graph) {
        if (graph.isColored()) {
            return true;
        }
        
        if (numIterations > maxSteps) {
            return false;
        }
        
        numIterations++;

        GraphColorVertex vertex = getBestVertex();
        ArrayList<Color> colors = orderColorDomain(vertex);
        Color possibleColor;
        System.out.println("Vertex choosen " + vertex.getKey());
        System.out.println("Vertex color domain " + vertex.getColorDomain());

        for (int i = 0; i < colors.size(); i++) {
            possibleColor = colors.get(i);
           
            if (!isConflicted(vertex, possibleColor)) {
                vertex.setColor(possibleColor);
                System.out.println("Vertex colored " + graph.translateColor(vertex.getColor()));
                updateVisualizer();
            
                if (maxArcConsistency(graph, vertex)) {
                    unassignedVerts.remove(vertex);
             
                    if (backtrackMAC(graph)) {
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
     * Makes sure arc consistency is maintained 
     *
     * @param graph
     * @param vertex
     * @return boolean true if color vertex combination maintains arc consistency
     */
    private boolean maxArcConsistency(GraphColorGraph graph, GraphColorVertex vertex) {
        VertexPair arc;
        ArrayList<Integer> connectedVertKeys = vertex.getConnections();
        GraphColorVertex connectedVert;
        ArrayList<VertexPair> arcs = new ArrayList();

        if (forwardCheck(vertex, vertex.getColor())) {
            
            for (int i = 0; i < connectedVertKeys.size(); i++) {
            
                for (int j = 0; j < connectedVertKeys.size(); j++) {
                    connectedVert = graph.getVertex(connectedVertKeys.get(j));
                
                    if (unassignedVerts.contains(connectedVert)) {
                        arcs.add(new VertexPair(connectedVert, vertex));
                    }
                }
            }

            while (!arcs.isEmpty()) {
                arc = arcs.get(0);
                arcs.remove(arc);
                if (revise(arc)) {
                    
                    if (arc.getVertexOne().getColorDomain().isEmpty()) {
                        restoreAfterMACFailure(arc.getVertexOne(), colorsRemoved);
                        return false;
                    }
                    
                    for (int k = 0; k < connectedVertKeys.size(); k++) {
                        connectedVert = graph.getVertex(connectedVertKeys.get(k));
                    
                        if (connectedVert.getKey() != arc.getVertexOne().getKey()) {
                            arcs.add(new VertexPair(connectedVert, vertex));
                        }
                    }
                }
            }
        }
        return true;
    }

    /**
     * Checks to see if a vertex's color domain is reduced during MAC 
     *
     * @param arc
     * @return boolean true if domain is reduced
     */
    private boolean revise(VertexPair arc) {
        boolean revised = false;
        ArrayList<Color> colorDomainOne = arc.getVertexOne().getColorDomain();
        ArrayList<Color> colorDomainTwo = arc.getVertexTwo().getColorDomain();
        colorsRemoved.clear();
        Color colorInDomainOne;
        Color colorInDomainTwo;
        GraphColorVertex vertex = arc.getVertexOne();
        
        for (int i = 0; i < colorDomainOne.size(); i++) {
            colorInDomainOne = colorDomainOne.get(i);
        
            for (int j = 0; j < colorDomainTwo.size(); j++) {
                colorInDomainTwo = colorDomainTwo.get(j);
            
                if (colorDomainTwo.size() == 1 && forwardCheckConflicted(colorInDomainOne, colorInDomainTwo)) {
                    
                    colorDomainOne.remove(colorInDomainOne);
                    vertex.setColorDomain(colorDomainOne);
                    System.out.println("Vertex domain after MAC " + vertex.getColorDomain());
                    colorsRemoved.add(colorInDomainOne);
                    revised = true;
                }
            }
        }

        return revised;
    }
    
    /**
     * Restores the domain of the given vertex after a MAC failure
     * 
     * @param vertex
     * @param colorsToAdd
     */
    private void restoreAfterMACFailure(GraphColorVertex vertex, ArrayList<Color> colorsToAdd) {
        ArrayList<Color> currentDomain = vertex.getColorDomain();
        Color colorToAdd;
       
        for (int i = 0; i < colorsToAdd.size(); i++) {
            colorToAdd = colorsToAdd.get(i);
        
            if (!currentDomain.contains(colorToAdd)) {
                currentDomain.add(colorToAdd);
            }
        }
        vertex.setColorDomain(currentDomain);
    }

    /**
     * Performs forward checking on the given vertex
     * 
     * @param vertex
     * @param possibleColor
     * @return true if no domains reduced to zero
     */
    private boolean forwardCheck(GraphColorVertex vertex, Color possibleColor) {
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
            
                if (forwardCheckConflicted(possibleColor, colorToCheck)) {
                    connectedVertDomain.remove(j);
                    connectedVert.setColorDomain(connectedVertDomain);
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
    private boolean forwardCheckConflicted(Color possibleVertColor, Color colorToCheck) {
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

    /**
     * Prints the list of arcs in the graph
     * 
     * @param arcs
     */
    public void printArcs(ArrayList<VertexPair> arcs) {
        VertexPair arc;
        int key1;
        int key2;
        for (int i = 0; i < arcs.size(); i++) {
            arc = arcs.get(i);
            key1 = arc.getVertexOne().getKey();
            key2 = arc.getVertexTwo().getKey();
            System.out.println(key1 + " connected to " + key2);
        }
    }

}
