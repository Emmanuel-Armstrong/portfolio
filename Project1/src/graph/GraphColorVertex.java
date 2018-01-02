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
import java.util.ArrayList;

/**
 * Project 1 - Graph Coloring Vertices
 * @author Matthew Rohrlach
 */
public class GraphColorVertex extends ParentVertex{
    
    private Color color;
    private boolean previouslyNonConflicted = false;
    private boolean pickedLast = false;
    private int possibleColors = 0;
    private ArrayList<Color> colorDomain;
    
    /**
     * Extend constructor from ParentVertex parent class
     * @param keyIn
     * @param xIn
     * @param yIn 
     */
    public GraphColorVertex(int keyIn, double xIn, double yIn){
        super(keyIn, xIn, yIn);
        color = Color.WHITE;
    }
    
    /**
     * Sets color of vertex
     * @param colorToSet 
     */
    public void setColor(Color colorToSet){
        color = colorToSet;
    }
    
    /**
     * gets color of vertex
     */
    public Color getColor(){
        return color;
    }
    
    /**
     * Toggles whether or not the vertex was once non conflicted
     * @param toggle 
     */
    public void togglePreviouslyNonConflicted(boolean toggle){
        this.previouslyNonConflicted = toggle;
    }
    
    /**
     * Gets whether or not the vertex was once non conflicted
     * @return boolean true if once not conflicted false if not
     */
    public boolean getPreviouslyNonConflicted() {
        return previouslyNonConflicted;
    }
    
    /**
     * Toggles whether or not the vertex was picked in the last iteration
     * @param toggle 
     */
    public void togglePickedLast(boolean toggle) {
        this.previouslyNonConflicted = toggle;
    }
    
    /**
     * Gets whether or not the vertex was picked last
     * @return boolean true if it was picked last false if not
     */
    public boolean getPickedLast() {
        return pickedLast;
    }
    
    /**
     * Sets the number of colors the vertex could be
     * @param inPossibleColors
     */
    public void setPossibleColors(int inPossibleColors) {
        possibleColors = inPossibleColors;
    }
    
    /**
     * Sets the domain of colors for the vertex
     * @param colors
     */
    public void setColorDomain(ArrayList<Color> colors){
        colorDomain = (ArrayList<Color>) colors.clone();
    }
    
    /**
     * Add a color to the domain of unique colors for the vertex
     * @param color
     */
    public void addToColorDomain(Color color){
        if (colorDomain.contains(color) == false){
            colorDomain.add(color);
        }
    }
    
    /**
     * Remove a color to the domain of unique colors for the vertex
     * @param color
     */
    public void removeFromColorDomain(Color color){
        if (colorDomain.contains(color) == true){
            colorDomain.remove(color);
        }
    }
    
    /**
     * Check if the color domain contains this color
     * @param colorToSearch
     * @return 
     */
    public boolean queryColorDomainFor(Color colorToSearch){
        
        return this.colorDomain.contains(colorToSearch);
    }
    
    /**
     * Gets the number of colors the vertex could be
     * @return int number of colors the vertex could be
     */
    public int getPossibleColors() {
        return possibleColors;
    }
    
    /**
     * Gets the domain of colors for a vertex
     * @return ArrayList<Color> current domain of colors
     */
    public ArrayList<Color> getColorDomain(){
        return colorDomain;
    }
    
    /**
     * Get the size of the color domain
     * @return 
     */
    public int getColorDomainSize() {
        return colorDomain.size();
    }
    
}
