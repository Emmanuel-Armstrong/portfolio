/*
 * Copyright (C) 2016 matthewrohrlach, nwmoore, emmanuel-armstrong
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
package data_components.tan;

import java.util.ArrayList;

/**
 * An object which composes a tree. Represents an attribute of a datapoint.
 * @author matthewrohrlach
 */
public class NodeTan {
    
    public int attributeIndex = -1;
    protected NodeTan parent;
    protected ArrayList<Edge> edges;
    
    public NodeTan(NodeTan parentIn, int attributeIndexIn) {
    
        parent = null;
        edges = new ArrayList<>();
        attributeIndex = attributeIndexIn;
    }
    
    public void addEdge(NodeTan destination) {
        
        for (Edge edge : edges) {
            
            if (edge.getDestination() == destination) {
                return;
            }
        }
        edges.add(new Edge(this, destination));
    }
    
    public void addEdge(Edge newEdge) {
        
        if (!edges.contains(newEdge)) {
            edges.add(newEdge);
        }
    }
    
    public void dropEdge(Edge edgeToDrop) {
        
        edges.remove(edgeToDrop);
    }
    
    public NodeTan findEdgeDestination(int attributeIndex) {
        
        for (Edge edge : edges) {
            
            if (edge.getDestinationIndex() == attributeIndex) {
                return edge.getDestination();
            }
        }
        System.out.println("Destination of that attribute not found!");
        return null;
    }
    
    public Edge findEdge(int attributeIndex) {
        
        for (Edge edge : edges) {
            
            if (edge.getDestinationIndex() == attributeIndex) {
                return edge;
            }
        }
        System.out.println("Destination of that attribute not found!");
        return null;
    }
    
    public Edge findMaxEdgeByWeight() {
        
        double bestWeight = -1;
        double currentWeight;
        Edge bestWeightEdge = null;
        
        for (int i = 0; i < edges.size(); i++) {
            
            currentWeight = edges.get(i).getWeight();
            if (currentWeight > bestWeight) {
                
                bestWeight = currentWeight;
                bestWeightEdge = edges.get(i);
            }
        }
        
        return bestWeightEdge;
    }
    
    public void findParent() {
        
        for (Edge nodeEdge : edges ) {
            
            if (nodeEdge.origin != this) {
                
                parent = nodeEdge.origin;
            }
        }
    }
    
    public void setParent(NodeTan newParent) {
        
        this.parent = newParent;
    }
    
    
    public void setAttributeIndex(int attributeIndexIn) {
        
        this.attributeIndex = attributeIndexIn;
    }
    
    public int numberOfChildren() {
        
        int childCount = 0;
        for (Edge nodeEdge : edges) {
            
            if (nodeEdge.getDestination() != this) {
                childCount++;
            }
        }
        
        return childCount;
    }
    
    public ArrayList<Edge> getEdges() {
        
        return this.edges;
    }
    
    public NodeTan getParent() {
        
        return this.parent;
    }
    
    public int getAttributeIndex() {
        
        return this.attributeIndex;
    }
}
