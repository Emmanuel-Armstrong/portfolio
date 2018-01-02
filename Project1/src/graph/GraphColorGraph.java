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
package graph;

import java.awt.Color;
import java.util.List;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Random;

/**
 * Project 1 - Graph Coloring Graphs
 *
 * @author Matthew Rohrlach
 */
public class GraphColorGraph extends ParentGraph {

    private ArrayList<Color> colors;
    private final List<Color> allColors;
    private int numColors;

    /**
     * Regular constructor
     * @param numPointsIn
     * @param numColorsIn 
     */
    public GraphColorGraph(int numPointsIn, int numColorsIn) {
        super(numPointsIn);
        
        //Initialize lists
        colors = new ArrayList();
        allColors = Arrays.asList(Color.RED, Color.BLUE, Color.YELLOW,
                Color.GREEN, Color.MAGENTA, Color.ORANGE, Color.PINK,
                Color.CYAN);

        // Add certain colors to list, depending on input
        numColors = numColorsIn;
    }
    
    /**
     * Deep-cloning constructor
     * @param graphToClone 
     */
    public GraphColorGraph(GraphColorGraph graphToClone){
        super(graphToClone);
        
        // Copy values from object to clone
        this.colors = (ArrayList<Color>) graphToClone.getColors().clone();
        this.allColors = (List<Color>) graphToClone.getAllColors();
        this.numColors = graphToClone.getNumColors();
        
        this.buildColors(numColors);
    }

    /**
     * Main graph-building logic
     */
    @Override
    public void buildGraph(){
        
        Random rand = new Random();
        vertexList = new ArrayList<>();
        segmentList = new ArrayList<>();
        
        // Build list of vertices scattered across unit square
        for (int i = 0; i < numPoints; i++){
            vertexList.add(new GraphColorVertex(i, rand.nextDouble(), rand.nextDouble()));
        }
        
        // Initialize color lists, reset vertices to white if they are not
        buildColors(numColors);
        
        // Build a distance list for each vertex
        for (int j = 0; j < numPoints; j++){
            vertexList.get(j).buildDistances(vertexList);
        }
        
        // Duplicate vertex list for segment-building
        potentialSegmentVertices = (ArrayList<ParentVertex>) vertexList.clone();
        
        // Choose random vertex, call method to add a line segment
        boolean doneWithSegments = false;
        int randomIndex;
        
        while (!doneWithSegments){
            randomIndex = rand.nextInt(potentialSegmentVertices.size());
            
            if (!drawSegment(randomIndex)) {
                potentialSegmentVertices.remove(randomIndex);
            }
            
            if (potentialSegmentVertices.size() < 1){
                doneWithSegments = true;
                break;
            }
        }
    }
    
    /**
     * Build color list to a given number of colors, reset vertex colors
     * @param newNumColors 
     */
    public void buildColors(int newNumColors){
        
        // Set all vertices in this graph to white
        GraphColorVertex vertexToBlank;
        for (int c = 0; c < vertexList.size(); c++){
            vertexToBlank = (GraphColorVertex) vertexList.get(c);
            vertexToBlank.setColor(Color.WHITE);
        }
        
        colors.clear();
        
        // Add colors in a deterministic way
        numColors = newNumColors;
        switch (numColors) {
            case 3:
                colors.add(Color.ORANGE);
                colors.add(Color.PINK);
                colors.add(Color.CYAN);
                break;

            case 4:
                colors.add(Color.RED);
                colors.add(Color.BLUE);
                colors.add(Color.GREEN);
                colors.add(Color.YELLOW);
                break;

            default:
                if (numColors > 8 || numColors < 1){
                    System.out.println("Unsupported amount of colors! Defaulting to 4.\n");
                    numColors = 4;
                }
                for (int i = 0; i < numColors; i++){
                    colors.add(allColors.get(i));
                }
                break;
        }
        

        // Add colors randomly
//        Random rand = new Random();
//        int addedColors = 0;
//        
//        while (addedColors < numColors && addedColors < allColors.size()) {
//            
//            Color colorToAdd = allColors.get(rand.nextInt(allColors.size()));
//            if (!colors.contains(colorToAdd)){
//                colors.add(colorToAdd);
//                addedColors++;
//            }
//            
//        }
        
        
        for (int i = 0; i < vertexList.size(); i++) {
            ((GraphColorVertex)vertexList.get(i)).setColorDomain(colors);
        }
    }

    /**
     * Check if graph is colored
     *
     * @return boolean true if colored false if not
     */
    public boolean isColored() {
        GraphColorVertex vertToCheck;
        ArrayList<Integer> connections;
        for (int i = 0; i < vertexList.size(); i++) {
            vertToCheck = (GraphColorVertex) vertexList.get(i);
            connections = vertexList.get(i).getConnections();
            if (vertToCheck.getColor().equals(Color.WHITE)) {
                return false;
            }
            for (int j = 0; j < connections.size(); j++) {
                if (vertToCheck.getColor().equals(getVertex(connections.get(j)).getColor()) ) {
                    return false;
                }
            }
        }
        return true;
    }
    
    public boolean isColorOfVertexAllowed(GraphColorVertex vertex, Color colorToTest){
        
        Color connectorColor;
        int connectorKey;
        
        if (colorToTest.equals(Color.WHITE)){
            return true;
        }
        
        else for (int i = 0; i < vertex.getConnections().size(); i++) {
            
            connectorKey = vertex.getConnections().get(i);
            connectorColor = this.getVertex(connectorKey).getColor();
            
            if (colorToTest.equals(connectorColor)){
                return false;
            }
            
        }
        return true;
    }
    
    public void addColorToVertexConnects(GraphColorVertex sourceVertex, Color colorToModify) {
        
        GraphColorVertex connectedVertex;
        ArrayList<Integer> connectionKeys = sourceVertex.getConnections();
        
        // For every connected vertex, which would've previously had an altered color domain
        for (int i = 0; i < connectionKeys.size(); i++) {
            connectedVertex = getVertex(connectionKeys.get(i));
            
            // White vertex is an unassigned vertex
            if (connectedVertex.getColor().equals(Color.WHITE)) {
                
                // If the color was not previously removed from the color domain
                if (isColorOfVertexAllowed(connectedVertex, colorToModify)) {
                    
                    // Add this color back to the connected vertex color domain
                    connectedVertex.addToColorDomain(colorToModify);
                    
                }
            }
        }
    }
    
    public void removeColorFromVertexConnects(GraphColorVertex sourceVertex, Color colorToModify) {
        
        GraphColorVertex connectedVertex;
        ArrayList<Integer> connectionKeys = sourceVertex.getConnections();
        
        // For every connected vertex, which would've previously had an altered color domain
        for (int i = 0; i < connectionKeys.size(); i++) {
            connectedVertex = getVertex(connectionKeys.get(i));
            
            // White vertex is an unassigned vertex
            if (connectedVertex.getColor().equals(Color.WHITE)) {
                connectedVertex.removeFromColorDomain(colorToModify);
            }
            
        }
    }
    
    /**
     * Clear the color domains of all vertices in the list
     */
    public void initializeColorDomains() {
        for (int i = 0; i < vertexList.size(); i++) {
            ((GraphColorVertex)vertexList.get(i)).setColorDomain(colors);
        }
    }
    
    /**
     * Make sure the color domain for every GraphColorVertex is up-to-date.
     */
    public void updateColorDomains() {
        
        Color connectedVertexColor;
        GraphColorVertex sourceVertex;
        GraphColorVertex connectedVertex;
        
        for (int i = 0; i < vertexList.size(); i++) {
            
            sourceVertex = (GraphColorVertex) vertexList.get(i);
            
            for (int j = 0; j < sourceVertex.getConnections().size(); j++) {
                
                connectedVertex = (GraphColorVertex) getVertex(sourceVertex.getConnections().get(j));
                connectedVertexColor = connectedVertex.getColor();
                
                if (sourceVertex.queryColorDomainFor(connectedVertexColor)) {
                    sourceVertex.removeFromColorDomain(connectedVertexColor);
                }
            }
        }
    }
    
    /**
     * Turn a colored boolean condition into a string to for reporting
     * @param colored
     * @return 
     */
    public String reportColored(boolean colored) {
        if (colored) {
            return "Graph was colored";
        }
        else {
            return "No solution found";
        }
    }
    
    /**
     * Map a color to a simple name of that color
     * @param inputString
     * @return 
     */
    public String translateColor(Color inputColor) {
        
        String outputString = inputColor.toString();
        if (inputColor.equals(Color.WHITE)) {
            outputString = "White";
        }
        
        else if (inputColor.equals(Color.RED)){
            outputString = "Red";
        }
        
        else if (inputColor.equals(Color.BLUE)){
            outputString = "Blue";
        }
        
        else if (inputColor.equals(Color.YELLOW)){
            outputString = "Yellow";
        }
        
        else if (inputColor.equals(Color.GREEN)){
            outputString = "Green";
        }
        
        else if (inputColor.equals(Color.MAGENTA)){
            outputString = "Magenta";
        }
        
        else if (inputColor.equals(Color.ORANGE)){
            outputString = "Orange";
        }
        
        else if (inputColor.equals(Color.PINK)){
            outputString = "Pink";
        }
        
        else if (inputColor.equals(Color.CYAN)){
            outputString = "Cyan";
        }
        
        return outputString;
    }

    /**
     * Print the connections and colors of this graph
     */
    public void printVertexColors() {
        ArrayList<Integer> connections;

        for (int i = 0; i < vertexList.size(); i++) {
            connections = vertexList.get(i).getConnections();
            System.out.print(vertexList.get(i).key + " connected to ");
            for (int j = 0; j < connections.size(); j++) {
                System.out.print(getVertex(connections.get(j)).getKey() + " " + getVertex(connections.get(j)).getColor());
            }
            System.out.println();
        }
    }
    
    /**
     * Get specific vertex based on key, for uses with constraint solvers
     *
     * @param key
     * @return Vertex with given key
     */
    @Override
    public GraphColorVertex getVertex(int key) {
        GraphColorVertex toReturn = null;
        for (int i = 0; i < vertexList.size(); i++) {
            if (vertexList.get(i).key == key) {
                toReturn = (GraphColorVertex) vertexList.get(i);
            }
        }
        return toReturn;
    }
    
    /**
     * Get private collection colors
     * @return 
     */
    public ArrayList<Color> getColors(){
        return colors;
    }
    
    /**
     * Get private collection allColors
     * @return 
     */
    public List<Color> getAllColors(){
        return allColors;
    }
    
    /**
     * Get private integer numColors
     * @return 
     */
    public int getNumColors(){
        return numColors;
    }
}
