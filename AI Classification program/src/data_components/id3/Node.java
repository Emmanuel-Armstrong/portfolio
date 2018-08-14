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
package data_components.id3;

import data_components.DataSet;
import java.util.ArrayList;

/**
 * An object which composes a tree. Represents an attribute of a datapoint.
 * (i.e. color in a dataset of cars)
 * @author matthewrohrlach
 */
public class Node {
    
    public int attributeIndex = -1;
    protected Node parent;
    protected ArrayList<Node> children;
    protected ArrayList<String> childrenAttributeTypes;
    
    protected DataSet dataSet;
    protected ArrayList<Integer> availableClassifiers;
    
    protected boolean leafNode = false;
    protected String leafClass;
    
    public Node(Node parentIn, DataSet dataSetIn) {
    
        parent = parentIn;
        children = new ArrayList<>();
        childrenAttributeTypes = new ArrayList<>();
        dataSet = dataSetIn;
        availableClassifiers = new ArrayList<>(parent.getAvailableClassifiers());
    }
    
    public Node(DataSet dataSetIn, ArrayList<Integer> availableClassifiersIn) {
    
        parent = null;
        children = new ArrayList<>();
        childrenAttributeTypes = new ArrayList<>();
        dataSet = dataSetIn;
        availableClassifiers = availableClassifiersIn;
    }
    
    /**
     * Adds a child node to this node's list of children with a given attribute type
     * Example: This parent represents color. A child is added that represents weight.
     * An attribute value/type of blue points to that child.
     * @param childToAdd 
     * @param attributeType 
     */
    public void addChild(Node childToAdd, String attributeType) {
        
        if (!children.contains(childToAdd)) {
            children.add(childToAdd);
            childrenAttributeTypes.add(attributeType);
        }
    }
    
    /**
     * Return the child node that matches a given attribute type/value (i.e. blue)
     * @param attributeTypeToFind
     * @return 
     */
    public Node findChild(String attributeTypeToFind) {
        
        int childIndex = childrenAttributeTypes.indexOf(attributeTypeToFind);
        return children.get(childIndex);
    }
    
    public String findLeafClass() {
        
        return this.leafClass;
    }
    
    public boolean isLeafNode() {
        
        return this.leafNode;
    }
    
    public void removeClassifier(int classifier) {
        
        int classifierIndex = this.availableClassifiers.indexOf(classifier);
        availableClassifiers.remove(classifierIndex);
    }
    
    public void setAttributeIndex(int attributeIndexIn) {
        
        this.attributeIndex = attributeIndexIn;
    }
    
    public void setLeafNode(int classIndex) {
        
        this.leafNode = true;
        if (!this.dataSet.getDataSet().isEmpty()) {
            this.leafClass = this.dataSet.getDataPoint(0).getFeatures()[classIndex];
        }
        int i = 0;
    }
    
    public void unsetLeafNode() {
        
        this.leafNode = false;
        this.leafClass = null;
    }
    
    public void setLeafClass(String newLeafClass) {
        
        this.leafClass = newLeafClass;
    }
    
    public DataSet getDataSet() {
        
        return this.dataSet;
    }
    
    public ArrayList<Node> getChildren() {
        
        return this.children;
    }
    
    public Node getParent() {
        
        return this.parent;
    }
    
    public ArrayList<Integer> getAvailableClassifiers() {
        
        return this.availableClassifiers;
    }
    
    public int getAttributeIndex() {
        
        return this.attributeIndex;
    }
    
    public String getLeafClass() {
        
        return this.leafClass;
    }
}
