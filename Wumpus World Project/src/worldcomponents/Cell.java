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
package worldcomponents;

import java.util.ArrayList;

/**
 * Cell objects exist as places within the world, and may contain hazards or gold.
 * 
 * @author nwmoore
 * @author matthewrohrlach
 */
public class Cell {
    
    private final int key;
    private boolean isEmpty;
    private boolean hasPit;
    private boolean hasObstacle;
    private boolean hasWumpus;
    private boolean hasOriginalWumpus;
    private boolean hasGold = false;
    private ArrayList<Integer> connectedCellKeys;
    
    /**
     * Regular constructor
     * @param inKey
     * @param inHasPit
     * @param inHasObstacle
     * @param inHasWumpus 
     */
    public Cell(int inKey, boolean inHasPit, boolean inHasObstacle, boolean inHasWumpus) {
        this.key = inKey;
        this.hasPit = inHasPit;
        this.hasObstacle = inHasObstacle;
        this.hasOriginalWumpus = inHasWumpus;
        this.hasWumpus = this.hasOriginalWumpus;
        
        this.isEmpty = !hasPit && !hasObstacle && !hasWumpus;
        this.connectedCellKeys = new ArrayList<>();
    }
    
    /**
     * Clear hazards (not gold) and set empty flag
     */
    public void makeEmpty() {
        hasPit = false;
        hasObstacle = false;
        hasWumpus = false;
        isEmpty = true;
    }
    
    /**
     * Replace the connected cell keys of this cell with another arraylist
     * @param inConnectedCells 
     */
    public void setConnectedCells(ArrayList<Integer> inConnectedCells) {
        connectedCellKeys = (ArrayList<Integer>) inConnectedCells.clone();
    }
    
    /**
     * Add a single cell's key to the list of connections
     * @param cellKeyToAdd 
     */
    public void addConnectedCell(int cellKeyToAdd) {
        if (!connectedCellKeys.contains(cellKeyToAdd)) {
            connectedCellKeys.add(cellKeyToAdd);
        }
    }
    
    /**
     * Remove a single cell's key from the list of connections
     * @param cellToAdd 
     */
    public void dropConnectedCell(Cell cellToAdd) {
        connectedCellKeys.remove(cellToAdd.getKey());
    }
    
    /**
     * Set the gold flag of this cell
     * @param gold 
     */
    public void setGold(boolean gold) {
        hasGold = gold;
    }
    
    /**
     * Remove the wumpus from this cell
     */
    public void killWumpus() {
        hasWumpus = false;
        
        // Make the cell empty if it is now empty
        if (!(hasPit || hasObstacle || isEmpty)) {
            isEmpty = true;
        }
    }
    
    /**
     * Restore the wumpus to this cell
     */
    public void resurrectWumpus() {
        
        if (hasOriginalWumpus) {
            hasWumpus = true;
            isEmpty = false;
        }
    }
    
    /**
     * Get the key value of this cell
     * @return 
     */
    public int getKey() {
        return key;
    }
    
    /**
     * Get the empty flag value of this cell
     * @return 
     */
    public boolean getIsEmpty() {
        return isEmpty;
    }
    
    /**
     * Get the pit flag value of this cell
     * @return 
     */
    public boolean getHasPit() {
        return hasPit;
    }
    
    /**
     * Get the obstacle flag value of this cell
     * @return 
     */
    public boolean getHasObstacle() {
        return hasObstacle;
    }
    
    /**
     * Get the current wumpus flag value of this cell
     * @return 
     */
    public boolean getHasWumpus() {
        return hasWumpus;
    }
    
    /**
     * Get the original wumpus flag value of this cell
     * @return 
     */
    public boolean getHasOriginalWumpus() {
        return hasOriginalWumpus;
    }
    
    /**
     * Get the gold flag value of this cell
     * @return 
     */
    public boolean getHasGold() {
        return hasGold;
    }
    
    /**
     * Get this cell's list of connected keys
     * @return 
     */
    public ArrayList<Integer> getConnectedCells() {
        return connectedCellKeys;
    }
}
