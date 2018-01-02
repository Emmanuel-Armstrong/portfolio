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

import java.awt.geom.Line2D;
import java.util.ArrayList;
import java.util.Random;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Default methods and parameters for graph objects
 * @author Matthew Rohrlach
 */
public class ParentGraph {
    
    // This graph's main list of vertices
    protected ArrayList<ParentVertex> vertexList;
    
    // This graph's list of vertices that might support line segments
    protected ArrayList<ParentVertex> potentialSegmentVertices;
    
    // This graph's list of line segments
    protected ArrayList<Line2D> segmentList;
    
    // The number of vertices that will be built for this graph
    protected final int numPoints;
    
    
    /**
     * Regular constructor
     * @param numPointsIn 
     */
    public ParentGraph(int numPointsIn){
        this.numPoints = numPointsIn;
    }
    
    /**
     * Deep-cloning constructor
     * @param graphToClone 
     */
    public ParentGraph(ParentGraph graphToClone){
        
        // Copy values from object to clone
        this.segmentList = new ArrayList();
        this.vertexList = new ArrayList();
        
        this.numPoints = graphToClone.getNumPoints();
        
        for (int i = 0; i < graphToClone.getSegmentList().size(); i++) {
            segmentList.add((Line2D) graphToClone.getSegmentList().get(i).clone());
        }
        
        for (int i = 0; i < graphToClone.getVertices().size(); i++) {
            this.vertexList.addAll(graphToClone.getVertices());
        }
    }
    /**
     * Main graph-building logic
     */
    public void buildGraph(){
        Random rand = new Random();
        vertexList = new ArrayList<>();
        segmentList = new ArrayList<>();
        
        // Build list of vertices scattered across unit square
        for (int i = 0; i < numPoints; i++){
            vertexList.add(new ParentVertex(i, rand.nextDouble(), rand.nextDouble()));
        }
        
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
     * Attempt to place a line segment to a vertex's nearest available point
     * @param sourceIndex
     * @return true if segment placed
     */
    public boolean drawSegment(int sourceIndex){
        
        // Coordinate values
        int sourceKey = potentialSegmentVertices.get(sourceIndex).getKey();
        double sourceX = potentialSegmentVertices.get(sourceIndex).getXPos();
        double sourceY = potentialSegmentVertices.get(sourceIndex).getYPos();
        
        // Current destination focus variables
        int destinationKey;
        
        // Line segment object
        Line2D lineSegment = new Line2D.Double();
        
        // Iterate through the distance pairs of the source vertex
        for (int i = 0; i < potentialSegmentVertices.get(sourceIndex).getDistances().size(); i++) {
            
            // Find the key of the current most-near vertex, then the index in the graph's list
            destinationKey = potentialSegmentVertices.get(sourceIndex).getDistances().get(i).key;
            
            // Create this segment
            lineSegment.setLine(sourceX, sourceY, 
                    vertexList.get(destinationKey).getXPos(), 
                    vertexList.get(destinationKey).getYPos());
            
            // Assume this segment can be placed until proven otherwise
            boolean segmentCanBePlaced = true;
            
            if (segmentList.size() == 0){
                segmentList.add(lineSegment);
                vertexList.get(sourceKey).addConnection(destinationKey);
                vertexList.get(destinationKey).addConnection(sourceKey);
                return true;
            }
            
            // Check for an intersection with all other placed segments
            else {
                for (int j = 0; j < segmentList.size(); j++){
                    // First, check to see if a point is shared (no intersection)
                    
                    // Confusing if-statement voodoo to get Line2D working properly:
                    // (Line2D treats a shared point as an intersection)
                    
                    // If two points are equal, but other is not
                    if (!(lineSegment.getP2().equals(segmentList.get(j).getP2())) && 
                            (lineSegment.getP1().equals(segmentList.get(j).getP1()))){
                    }
                    
                    // If two points are equal, but other is not
                    else if (!(lineSegment.getP1().equals(segmentList.get(j).getP1())) && 
                            (lineSegment.getP2().equals(segmentList.get(j).getP2()))){
                    }
                    
                    // If two points are equal, but other is not
                    else if (!(lineSegment.getP2().equals(segmentList.get(j).getP1())) && 
                            (lineSegment.getP1().equals(segmentList.get(j).getP2()))){
                    }
                    
                    // If two points are equal, but other is not
                    else if (!(lineSegment.getP1().equals(segmentList.get(j).getP2())) && 
                            (lineSegment.getP2().equals(segmentList.get(j).getP1()))){
                    }
                    
                    // If no points are equal, check for intersection
                    else if (lineSegment.intersectsLine(segmentList.get(j))){
                        segmentCanBePlaced = false;
                        break;
                    }
                }
                
                // If the loop exited without a fail condition, add the segment
                if (segmentCanBePlaced) {
                    segmentList.add(lineSegment);
                    vertexList.get(sourceKey).addConnection(destinationKey);
                    vertexList.get(destinationKey).addConnection(sourceKey);
                    return true;
                }
            }
        }
        
        return false;
    }
    
    /**
     * Re-initialize the fitness of this entire graph
     */
    public void resetGraphFitness(){
        for (int i = 0; i < vertexList.size(); i++) {
            vertexList.get(i).setFitness(0);
        }
    }
    
    /**
     * Get list of vertices for graph
     * @return vertexList
     */
    public ArrayList getVertices(){
        return vertexList;
    }
    
    /**
     * Get specific vertex based on key, for uses with constraint solvers
     *
     * @param key
     * @return Vertex with given key
     */
    public ParentVertex getVertex(int key) {
        ParentVertex toReturn = null;
        for (int i = 0; i < vertexList.size(); i++) {
            if (vertexList.get(i).key == key) {
                toReturn = (ParentVertex) vertexList.get(i);
            }
        }
        return toReturn;
    }
    
    /**
     * Get protected value numPoints
     * @return 
     */
    public int getNumPoints(){
        return numPoints;
    }
    
    /**
     * Get protected collection segmentList
     * @return 
     */
    public ArrayList<Line2D> getSegmentList(){
        return segmentList;
    }
}
