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

/**
 * Contains an edge
 * @author matthewrohrlach
 */
public class Edge{
    
    protected NodeTan destination;
    protected NodeTan origin;
    protected double weight = 1.0;
    
    public Edge(NodeTan originIn, NodeTan destinationIn) {
        
        this.origin = originIn;
        this.destination = destinationIn;
    }
    
    public Edge(NodeTan originIn, NodeTan destinationIn, double weightIn) {
        
        this.origin = originIn;
        this.destination = destinationIn;
        this.weight = weightIn;
    }
    
    public void setWeight(double newWeight) {
        
        this.weight = newWeight;
    }
    
    public void reverse() {
        
        NodeTan temp = this.destination;
        this.destination = this.origin;
        this.origin = temp;
    }
    
    public double getWeight() {
        
        return this.weight;
    }
    
    public NodeTan getDestination() {
        
        return this.destination;
    }
    
    public int getDestinationIndex() {
        
        return this.destination.getAttributeIndex();
    }
    
    public NodeTan getOrigin() {
        
        return this.origin;
    }
}
