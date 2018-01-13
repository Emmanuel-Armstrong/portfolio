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

import java.util.ArrayList;
import java.util.Random;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Default methods and parameters for vertex objects
 * @author Matthew Rohrlach
 */
public class ParentVertex {
    
    // Unique key
    protected final int key;
    
    // x-coordinate
    protected final double xPos;
    
    // y-coordinate
    protected final double yPos;
    
    // List of connected vertices (referred to by key)
    protected ArrayList<Integer> connectedKeys;
    protected int degree;
    
    // List of distances to all other vertices, paired with key
    protected ArrayList<VertexDistancePair> distances;
    
    // Fitness of the function, as assigned by some fitness function
    protected int fitness;
    
    
    
    /**
     * Vertex creation with given key and coords
     * @param keyIn 
     */
    public ParentVertex(int keyIn, double xIn, double yIn){
        this.key = keyIn;
        this.xPos = xIn;
        this.yPos = yIn;
        connectedKeys = new ArrayList<>();
        degree = 0;
        fitness = 0;
    }
    
    /**
     * Vertex creation with random key and coords
     * @param keyIn 
     */
    public ParentVertex(){
        Random rand = new Random();
        key = rand.nextInt(Integer.MAX_VALUE);
        xPos = rand.nextDouble();
        yPos = rand.nextDouble();
        connectedKeys = new ArrayList<>();
        degree = 0;
        fitness = 0;
    }
    
    
    
    
    /**
     * Build a list of vertex distance pairs for use in line segment placement
     * @param allVertices 
     */
    public void buildDistances(ArrayList<ParentVertex> allVertices){
        distances = new ArrayList<>();
        
        // For all vertices in this graph
        for (int i = 0; i < allVertices.size(); i++){
            
            // If the vertex at hand is not the same vertex
            if (this.key != allVertices.get(i).getKey()){
                double distanceToAdd = 
                    // Euclidean distance function (this vertex to list vertex)
                    Math.sqrt(
                        Math.pow((allVertices.get(i).getXPos() - this.xPos), 2) + 
                        Math.pow((allVertices.get(i).getYPos() - this.yPos), 2));

                // If this is the first distance pair, add it in
                if (distances.size() == 0){
                    distances.add(new VertexDistancePair(allVertices.get(i).getKey(), distanceToAdd));
                }

                else {
                    // Place this new vertexDistancePair where it belongs:
                    // nearest to furthest distance
                    for (int j = 0; j < distances.size(); j++){
                        if (distanceToAdd <= distances.get(j).distance){
                            distances.add(j, new VertexDistancePair(allVertices.get(i).getKey(), distanceToAdd));
                            break;
                        }
                        else if (j == distances.size()-1){
                            distances.add(new VertexDistancePair(allVertices.get(i).getKey(), distanceToAdd));
                            break;
                        }
                    }
                }
            }
        }
    }
    
    /**
     * Add a connection to this vertex connection list (must reciprocate!)
     * @param keyToAdd 
     */
    public void addConnection(int keyToAdd){
        if (!connectedKeys.contains(keyToAdd)){
            connectedKeys.add(keyToAdd);
            degree++;
        }
    }
    
    /**
     * Drop a connection from this vertex connection list (must reciprocate!)
     * @param keyToDrop 
     */
    public void dropConnection(int keyToDrop){
        int removeIndex = connectedKeys.indexOf(keyToDrop);
        if (removeIndex != -1){
            connectedKeys.remove(removeIndex);
            degree--;
        }
    }
    
    /**
     * Test if a connection has been made to a specific vertex
     * @param keyToTest
     * @return 
     */
    public boolean testConnection(int keyToTest){
        if (connectedKeys.contains(keyToTest)){
            return true;
        }
        else{
            return false;
        }
    }
    
    /**
     * Overwrite the fitness of the vertex
     * @param newFitness 
     */
    public void setFitness(int newFitness) {
        fitness = newFitness;
    }
    
    /**
     * Return key of this vertex
     * @return 
     */
    public int getKey(){
        return key;
    }
    
    /**
     * Return x position of this vertex
     * @return 
     */
    public double getXPos(){
        return xPos;
    }
    
    /**
     * Return y position of this vertex
     * @return 
     */
    public double getYPos(){
        return yPos;
    }
    
    /**
     * Return list of connections from this vertex
     * @return 
     */
    public ArrayList<Integer> getConnections(){
        return connectedKeys;
    }
    
    /**
     * Return degree (number of connections) of this vertex
     * @return 
     */
    public int getDegree(){
        return degree;
    }
    
    /**
     * Get the fitness of the vertex, as set by some fitness function
     * @return 
     */
    public int getFitness(){
        return fitness;
    }
    
    /**
     * Return list of distances to connected vertices from this vertex
     * @return 
     */
    public ArrayList<VertexDistancePair> getDistances(){
        return distances;
    }
    
    public Object getClone(){
        Object returnVertex = null;
        try {
            returnVertex = this.clone();
        } 
        catch (CloneNotSupportedException ex) {
            Logger.getLogger(ParentVertex.class.getName()).log(Level.SEVERE, null, ex);
        }
        
        return returnVertex;
    }
}
