/*
 * Copyright (C) 2016 matthewrohrlach
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
package actorcomponents;

import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * The knowledge known about a specific cell
 * @author matthewrohrlach
 */
public class KnowledgeFragment {
    
    // Key of cell that this object holds knowledge about
    final int key;
    
    // The suspicions attached to this cell
    boolean wumpusSuspicion;
    boolean pitSuspicion;
    
    // Whether or not this cell HAS a hazard (or has safety) (Only set once)
    boolean wumpusFound;
    boolean pitFound;
    boolean obstacleFound;
    boolean safe;
    boolean visited;
    
    // Whether or not the hazard and safety values have been set in stone
    boolean wumpusConfirmationAllowed;
    boolean wumpusSet;
    boolean pitSet;
    boolean obstacleSet;
    boolean safeSet;
    
    boolean verbose = true;
    
    final ArrayList<KnowledgeFragment> connectedFragments;
    final ArrayList<KnowledgeFragment> suspectedWumpusNeighbors;
    final ArrayList<KnowledgeFragment> suspectedPitNeighbors;
    
    /**
     * Regular constructor
     * @param keyIn 
     */
    public KnowledgeFragment(int keyIn) {
        
        key = keyIn;
        connectedFragments = new ArrayList<>();
        suspectedWumpusNeighbors = new ArrayList<>();
        suspectedPitNeighbors = new ArrayList<>();
        
        wumpusSuspicion = false;
        pitSuspicion = false;
        
        wumpusFound = false;
        pitFound = false;
        obstacleFound = false;
        safe = false;
        visited = false;
        
        wumpusConfirmationAllowed = true;
        wumpusSet = false;
        pitSet = false;
        obstacleSet = false;
        safeSet = false;
        
    }
    
    /**
     * Provide the sensed inputs in this cell and correct suspicions accordingly
     * @param sensesReport 
     */
    public void updateThisFragment(int sensesReport) {
        
        switch (sensesReport) {
            
            // No smell and no wind
            case 0:
                // Suspect no neighbors
                this.suspectedWumpusNeighbors.clear();
                this.suspectedPitNeighbors.clear();
                // Set all neighbors to safe
                for (KnowledgeFragment neighbor : connectedFragments) {
                    neighbor.setSafe();
                }
                // Allow confirmation of Wumpus if it was previously disallowed
                if (!wumpusConfirmationAllowed) {
                    this.setWumpusConfirmationAllowed(true);
                }
                break;
            
            // Smell and no wind
            case 1:
                // Suspect no neighbors of a pit
                this.suspectedPitNeighbors.clear();
                // Suspect all unsafe neighbors of a Wumpus
                for (KnowledgeFragment neighbor : connectedFragments) {
                    if (!neighbor.isSafe()) {
                        if ((!neighbor.isWumpusSet()) || neighbor.isWumpusFound()) {
                            this.addToSuspectedWumpusNeighbors(neighbor);
                        }
                    }
                    else {
                        this.removeFromSuspectedWumpusNeighbors(neighbor);
                    }
                }
                // Prevent any neighbor from ever being called a pit
                for (KnowledgeFragment neighbor : connectedFragments) {
                    neighbor.setPitFound(false);
                }
                
                // Re-allow Wumpus confirmation
                if (!wumpusConfirmationAllowed) {
                    this.setWumpusConfirmationAllowed(true);
                }
                this.identifyWumpusHazardInNeighbors();
                break;
                
            // No smell and wind
            case 2:
                // Suspect no neighbor of a Wumpus
                this.suspectedWumpusNeighbors.clear();
                // Suspect all unsafe neighbors of a pit
                for (KnowledgeFragment neighbor : connectedFragments) {
                    if (!neighbor.isSafe()) {
                        if ((!neighbor.isPitSet()) || neighbor.isPitFound()) {
                            this.addToSuspectedPitNeighbors(neighbor);
                        }
                    }
                    else {
                        this.removeFromSuspectedPitNeighbors(neighbor);
                    }
                }
                // Prevent any neighbor from ever being called a Wumpus
                for (KnowledgeFragment neighbor : connectedFragments) {
                    neighbor.setWumpusFound(false);
                }
                
                if (!wumpusConfirmationAllowed) {
                    this.setWumpusConfirmationAllowed(true);
                }
                this.identifyPitHazardInNeighbors();
                break;
                
            // Smell and wind
            case 3:
                // Suspect all unsafe neighbors of Wumpus and pit
                for (KnowledgeFragment neighbor : connectedFragments) {
                    if (!neighbor.isSafe()) {
                        if ((!neighbor.isWumpusSet()) || neighbor.isWumpusFound()) {
                            this.addToSuspectedWumpusNeighbors(neighbor);
                        }
                        if ((!neighbor.isPitSet()) || neighbor.isPitFound()) {
                            this.addToSuspectedPitNeighbors(neighbor);
                        }
                    }
                    else {
                        this.removeFromSuspectedWumpusNeighbors(neighbor);
                        this.removeFromSuspectedPitNeighbors(neighbor);
                    }
                }
                
                if (!wumpusConfirmationAllowed) {
                    this.setWumpusConfirmationAllowed(true);
                }
                this.identifyHazardInNeighbors();
                break;
                
            // Error
            default:
                System.err.println("Bad senses report!");
                break;
        }
    }
    
    /**
     * Corrects the suspicions of this cell, without creating an assumption about hazards
     */
    public void updateThisFragmentWithoutAssumptions() {
        
        // Update the suspicion lists of this cell's knowledge fragment
        for (KnowledgeFragment neighbor : connectedFragments) {
            if (neighbor.isSafe()) {
                this.removeFromSuspectedWumpusNeighbors(neighbor);
                this.removeFromSuspectedPitNeighbors(neighbor);
            }
            if (!neighbor.isWumpusSuspicious()) {
                this.removeFromSuspectedWumpusNeighbors(neighbor);
            }
            if (!neighbor.isPitSuspicious()) {
                this.removeFromSuspectedPitNeighbors(neighbor);
            }
        }
    }
    
    /**
     * Corrects the suspicions of this cell, then creates assumptions about both hazards
     */
    public void updateThisFragmentInKnowledge() {
        
        // Update suspicion lists and identify hazards
        this.updateThisFragmentWithoutAssumptions();
        this.identifyHazardInNeighbors();
    }
    
    /**
     * Creates an assumption about a hazard within a cell, if there is only possibility for that hazard
     */
    public void identifyHazardInNeighbors() {
        
        // Identify hazards of both types
        this.identifyWumpusHazardInNeighbors();
        this.identifyPitHazardInNeighbors();
    }
    
    /**
     * Creates an assumption about a Wumpus hazard within a cell, if there is only possibility for that hazard
     */
    public void identifyWumpusHazardInNeighbors() {
        
        // If there is one remaining unsafe suspect, that suspect is a Wumpus
        if (suspectedWumpusNeighbors.size() == 1 && this.wumpusConfirmationAllowed) {
            KnowledgeFragment confirmedWumpus = suspectedWumpusNeighbors.get(0);
            boolean reportIfVerbose = (!confirmedWumpus.isWumpusFound());
            confirmedWumpus.setWumpusFound(true);
            if (verbose && reportIfVerbose) {
                System.out.println("Cell " + confirmedWumpus.getKey() 
                        + " was the last cell in Cell " + this.getKey() + "'s Wumpus suspicion list.");
            }
        }
    }
    
    /**
     * Creates an assumption about a pit hazard within a cell, if there is only possibility for that hazard
     */
    public void identifyPitHazardInNeighbors() {
        
        // If there is one remaining unsafe suspect, that suspect is a pit
        if (suspectedPitNeighbors.size() == 1) {
            KnowledgeFragment confirmedPit = suspectedPitNeighbors.get(0);
            boolean reportIfVerbose = (!confirmedPit.isPitFound());
            confirmedPit.setPitFound(true);
            if (verbose && reportIfVerbose) {
                System.out.println("Cell " + confirmedPit.getKey() 
                        + " was the last cell in Cell " + this.getKey() + "'s pit suspicion list.");
            }
        }
    }
    
    /**
     * Add a knowledge fragment to the list of neighbors of this fragment
     * @param neighbor 
     */
    public void addConnectedFragment(KnowledgeFragment neighbor) {
        
        if (!connectedFragments.contains(neighbor)) {
            this.connectedFragments.add(neighbor);
        }
    }
    
    /**
     * Remove a fragment from the connections of this fragment
     * @param neighbor 
     */
    public void removeConnectedFragment(KnowledgeFragment neighbor) {
        
        this.connectedFragments.remove(neighbor);
    }
    
    /**
     * Add to the list of this cell's suspicions about its neighbors' wumpus occupants
     * @param neighbor 
     */
    public void addToSuspectedWumpusNeighbors(KnowledgeFragment neighbor) {
        
        if (!(suspectedWumpusNeighbors.contains(neighbor)) && this.isVisited()) {
            this.suspectedWumpusNeighbors.add(neighbor);
            neighbor.setWumpusSuspicion(true);
            if (verbose && !neighbor.isWumpusFound()) {
                System.out.println("Cell " + this.getKey() + " suspects cell " + neighbor.getKey() + " of having a Wumpus.");
            }
            else if (verbose && neighbor.isWumpusFound()) {
                System.out.println("Cell " + this.getKey() + " already knows cell " + neighbor.getKey() + " has a Wumpus.");
            }
        }
    }
    
    /**
     * Add to the list of this cell's suspicions about its neighbors' pits
     * @param neighbor 
     */
    public void addToSuspectedPitNeighbors(KnowledgeFragment neighbor) {
        
        if (!suspectedPitNeighbors.contains(neighbor) && this.isVisited()) {
            this.suspectedPitNeighbors.add(neighbor);
            neighbor.setPitSuspicion(true);
            if (verbose && !neighbor.isPitFound()) {
                System.out.println("Cell " + this.getKey() + " suspects cell " + neighbor.getKey() + " of having a pit.");
            }
            else if (verbose && neighbor.isPitFound()) {
                System.out.println("Cell " + this.getKey() + " already knows cell " + neighbor.getKey() + " has a pit.");
            }
        }
    }
    
    /**
     * Clear a neighboring cell of wumpus suspicion
     * @param neighbor 
     */
    public void removeFromSuspectedWumpusNeighbors(KnowledgeFragment neighbor) {
        
        if (this.suspectedWumpusNeighbors.contains(neighbor)) {
            this.suspectedWumpusNeighbors.remove(neighbor);
            neighbor.setWumpusSuspicion(false);
            if (verbose) {
                System.out.println("Cell " + this.getKey() + " no longer suspects cell " + neighbor.getKey() + " of having a Wumpus.");
            }
        }
    }
    
    /**
     * Clear a neighboring cell of pit suspicion
     * @param neighbor 
     */
    public void removeFromSuspectedPitNeighbors(KnowledgeFragment neighbor) {
        
        if (this.suspectedPitNeighbors.contains(neighbor)) {
            this.suspectedPitNeighbors.remove(neighbor);
            neighbor.setPitSuspicion(false);
            if (verbose) {
                System.out.println("Cell " + this.getKey() + " no longer suspects cell " + neighbor.getKey() + " of having a pit.");
            }
        }
    }
    
    /**
     * Handle an event wherein an arrow is fired into the cell represented by this fragment
     */
    public void arrowFiredIntoFragment() {
        
        for (KnowledgeFragment neighbor : connectedFragments) {
            if (neighbor.isSuspiciousOfWumpusInCell(this)) {
                neighbor.removeFromSuspectedWumpusNeighbors(this);
            }
        }
        this.setWumpusFound(false);
    }
    
    /**
     * Delete everything known about this fragment
     */
    public void clearKnowledgeFragment() {
        
        // Reset all variables
        suspectedWumpusNeighbors.clear();
        suspectedPitNeighbors.clear();
        
        wumpusSuspicion = false;
        pitSuspicion = false;
        
        wumpusFound = false;
        pitFound = false;
        obstacleFound = false;
        safe = false;
        visited = false;
        
        pitSet = false;
        obstacleSet = false;
        safeSet = false;
    }
    
    /**
     * Set this cell's wumpus suspiciousness
     * @param newValue 
     */
    public void setWumpusSuspicion(boolean newValue) {
        
        this.wumpusSuspicion = newValue;
    }
    
    /**
     * Set this cell's pit suspiciousness
     * @param newValue 
     */
    public void setPitSuspicion(boolean newValue) {
        
        this.pitSuspicion = newValue;
    }
    
    /**
     * Set this cell's (currently) guaranteed wumpus state. A change to false is permanent.
     * @param newValue
     */
    public void setWumpusFound(boolean newValue) {
        
        boolean oldValue = this.wumpusFound;
        if (!wumpusSet) {
            
            // Wumpus status set to false permanently, if it hasn't been set to false already
            if (!newValue) {

                wumpusSet = true;
                this.wumpusSuspicion = false;
                this.wumpusFound = false;
                if (verbose) {
                    if (!oldValue) {
                        System.out.println("Cell " + this.getKey() + " does not have a Wumpus.");
                    }
                    else {
                        System.out.println("Cell " + this.getKey() + " no longer has a Wumpus.");
                    }
                }
                
                // Mark this cell as safe if it has been established that there is no pit here also
                if (!this.isPitFound() && this.isPitSet() && !this.isSafe()) {
                    this.setSafe();
                }
                
                // Update the neighbors of this fragment with the implication of this cell not having a Wumpus and/or pit
                for (KnowledgeFragment connectorFragment : connectedFragments) {

                    // Update this neighbor as stated above
                    connectorFragment.updateThisFragmentWithoutAssumptions();
                }
                for (KnowledgeFragment connectorFragment : connectedFragments) {

                    // Update this neighbor as stated above
                    connectorFragment.identifyWumpusHazardInNeighbors();
                }
            }
            // Wumpus status set to true, report only if it was previously false
            else {

                this.wumpusSuspicion = true;
                this.wumpusFound = true;
                if (verbose && (!oldValue)) {
                    System.out.println("Cell " + this.getKey() + " has a Wumpus.");
                }
                this.setPitFound(false);
            }
        }
    }
     
    /**
     * Set this cell's permanently guaranteed pit state 
     * @param newValue
     */
    public void setPitFound(boolean newValue) {
        
        if (!pitSet) {
            this.pitFound = newValue;
            this.pitSuspicion = newValue;
            this.pitSet = true;
            if (verbose) {
                if (!newValue) {
                    System.out.println("Cell " + this.getKey() + " does not have a pit.");
                }
                else {
                    System.out.println("Cell " + this.getKey() + " has a pit.");
                }
            }
            if (newValue) {
                this.setWumpusFound(false);
            }
            else {
                
                // Mark this cell as safe if it has been established that there is no Wumpus here also
                if (!this.isWumpusFound() && this.isWumpusSet() && !this.isSafe()) {
                    this.setSafe();
                }
                
                // Update the neighbors of this fragment with the implication of this cell not having a pit and/or Wumpus
                for (KnowledgeFragment connectorFragment : connectedFragments) {

                    // Update this neighbor as stated above
                    connectorFragment.updateThisFragmentWithoutAssumptions();
                }
                for (KnowledgeFragment connectorFragment : connectedFragments) {

                    // Update this neighbor as stated above
                    connectorFragment.identifyPitHazardInNeighbors();
                }
            } 
        }
    }
    
    /**
     * Set this cell's permanently guaranteed obstacle state
     * @param newValue 
     * 
     * 
     */
    public void setObstacleFound(boolean newValue) {
        if (!obstacleSet) {
            this.obstacleFound = newValue;
            this.obstacleSet = true;
            if (verbose && newValue) {
                System.out.println("Cell " + this.getKey() + " has an obstacle.");
            }
        }
        // Wipe out all connections to this fragment (not the cell), if true
        if (this.obstacleFound == true && this.obstacleSet == true) {
            for (KnowledgeFragment neighbor : connectedFragments) {
                neighbor.removeConnectedFragment(this);
            }
            this.connectedFragments.clear();
        }
    }
    
    /**
     * Set this cell's permanently guaranteed safety 
     */
    public void setSafe() {
        if (!safeSet) {
            this.safe = true;
            
            this.wumpusSuspicion = false;
            this.setWumpusFound(false);
            this.pitSuspicion = false;
            this.setPitFound(false);
            
            this.safeSet = true;
            this.wumpusSet = true;
            this.pitSet = true;
            
            // Update the neighbors of this fragment with the implication of this cell being safe
            for (KnowledgeFragment connectorFragment : connectedFragments) {

                // Update this neighbor as stated above
                connectorFragment.updateThisFragmentInKnowledge();
            }
            
            if (verbose) {
                System.out.println("Cell " + this.getKey() + " is safe, yet unvisited!");
            }
        }
    }
    
    /**
     * Set this cell as visited
     */
    public void setVisited() {
        
        // A cell's fragment will only be marked visited if it is also safe
        if (!this.visited) {
            this.visited = true;
            if (verbose) {
                System.out.println("Cell " + this.getKey() + " is visited!");
            }
            if (verbose) {
                System.out.println("|\nv\nKnowledge gained: ");
            }
        }
    }
    
    /**
     * Enables or disables verbose mode, which offers more information about decision-making
     * @param newValue 
     */
    public void setVerboseMode(boolean newValue) {
       
        this.verbose = newValue;
    }
    
    /**
     * Change the ability of this cell to be confirmed as a Wumpus
     * @param newValue 
     */
    public void setWumpusConfirmationAllowed(boolean newValue) {
        
        boolean oldValue = this.wumpusConfirmationAllowed;
        if (verbose && oldValue != newValue) {
            if (newValue) {
                
                System.out.println("Cell " + this.getKey() + " is no longer blocked from assuming a Wumpus is in a neighbor.");
            }
            else {
                
                System.out.println("Cell " + this.getKey() + " is blocked from assuming a Wumpus is in a neighbor.");
            }
        }
        this.wumpusConfirmationAllowed = newValue;
    }
    
    /**
     * Return the safety of this cell
     * @return 
     */
    public boolean isSafe() {
        
        return this.safe;
    }
    
    /**
     * Return whether or not this cell has been visited
     * @return 
     */
    public boolean isVisited() {
        
        return this.visited;
    }
    
    /**
     * Return whether or not this cell is suspected of wumpus sympathizing
     * @return 
     */
    public boolean isWumpusSuspicious() {
        
        return this.wumpusSuspicion;
    }
    
    /**
     * Return whether or not this cell is suspected of being a great trash dump
     * @return 
     */
    public boolean isPitSuspicious() {
        
        return this.pitSuspicion;
    }
    
    /**
     * Return if a Wumpus was discovered in this cell
     * @return 
     */
    public boolean isWumpusFound() {
        
        return this.wumpusFound;
    }
    
    /**
     * Return if a pit was discovered in this cell
     * @return 
     */
    public boolean isPitFound() {
        
        return this.pitFound;
    }
    
    /**
     * Return if an obstacle was discovered in this cell
     * @return 
     */
    public boolean isObstacleFound() {
        
        return this.obstacleFound;
    }
    
    /**
     * Return whether or not this fragment has a visited neighbor
     * @return 
     */
    public boolean isNeighborOfVisited() {
        
        for (KnowledgeFragment neighbor : connectedFragments) {
            
            if (neighbor.isVisited()) {
                return true;
            }
        }
        return false;
    }
    
    /**
     * Return whether or not this fragment has an unvisited neighbor
     * @return 
     */
    public boolean isNeighborOfUnvisited() {
        
        for (KnowledgeFragment neighbor : connectedFragments) {
            
            if (!neighbor.isVisited()) {
                return true;
            }
        }
        return false;
    }
    
    /**
     * Return whether or not this fragment has a neighbor with a confirmed Wumpus
     * @return 
     */
    public boolean isNeighborOfWumpusFound() {
        
        for (KnowledgeFragment neighbor : connectedFragments) {
            
            if (neighbor.isWumpusFound()) {
                return true;
            }
        }
        return false;
    }
    
    /**
     * Return whether or not this fragment has a neighbor with a suspected Wumpus
     * @return 
     */
    public boolean isNeighborOfWumpusSuspicion() {
        
        return (this.suspectedWumpusNeighbors.size() > 0);
    }
    
    /**
     * Return if confirming this cell is a Wumpus is allowed
     * @return 
     */
    public boolean isWumpusConfirmationAllowed() {
        
        return this.wumpusConfirmationAllowed;
    }
    
    /**
     * Return whether or not the Wumpus status of this cell is permanent
     * @return 
     */
    public boolean isWumpusSet() {
        
        return this.wumpusSet;
    }
    
    /**
     * Return whether or not the pit status of this cell is permanent
     * @return 
     */
    public boolean isPitSet() {
        
        return this.pitSet;
    }
    
    /**
     * Return whether or not the obstacle status of this cell is permanent
     * @return 
     */
    public boolean isObstacleSet() {
        
        return this.obstacleSet;
    }
    
    /**
     * Return whether or not the given fragment is a suspect of this fragment
     * @param neighbor
     * @return 
     */
    public boolean isSuspiciousOfWumpusInCell(KnowledgeFragment neighbor) {
        
        return (suspectedWumpusNeighbors.contains(neighbor));
    }
    
    /**
     * Return whether or not the given fragment is a suspect of this fragment
     * @param neighbor
     * @return 
     */
    public boolean isSuspiciousOfPitInCell(KnowledgeFragment neighbor) {
        
        return (suspectedPitNeighbors.contains(neighbor));
    }
    
    /**
     * Return the unique key of this knowledge fragment
     * @return 
     */
    public int getKey() {
       
        return this.key;
    }
    
    /**
     * Return this fragment's list of connected fragments
     * @return 
     */
    public ArrayList<KnowledgeFragment> getConnectedFragments() {
        
        return this.connectedFragments;
    }
    
    /**
     * Return the number of visited neighbors of this fragment
     * @return 
     */
    public int getNumberOfVisitedNeighbors() {
        
        int returnCount = 0;
        for (KnowledgeFragment neighbor : connectedFragments) {
            
            if (neighbor.isVisited()) {
                returnCount++;
            }
        }
        return returnCount;
    }
    
    /**
     * Return the number of unvisited neighbors of this fragment
     * @return 
     */
    public int getNumberOfUnvisitedNeighbors() {
        
        int returnCount = 0;
        for (KnowledgeFragment neighbor : connectedFragments) {
            
            if (!neighbor.isVisited()) {
                returnCount++;
            }
        }
        return returnCount;
    }
    
    /**
     * Returns the size of the number of suspected Wumpus neighbors
     * @return 
     */
    public int getWumpusSuspicionListSize() {
        
        return this.suspectedWumpusNeighbors.size();
    }
    
    /**
     * Returns the size of the number of suspected Wumpus neighbors
     * @return 
     */
    public int getPitSuspicionListSize() {
        
        return this.suspectedPitNeighbors.size();
    }
    
    /**
     * Return the size of the smallest suspicion list to which this cell belongs
     * Smaller means a tighter amount of suspicion on this cell!
     * @return 
     */
    public int getWumpusSuspicionScore() {
        
        int smallestSuspicionListSize = Integer.MAX_VALUE;
        int thisSuspicionListSize;
        boolean pause = false;
        
        for (KnowledgeFragment neighbor : connectedFragments) {
            
            if (neighbor.isVisited()) {
                
                if (neighbor.isSuspiciousOfWumpusInCell(this)) {
                    
                    thisSuspicionListSize = neighbor.getWumpusSuspicionListSize();
                    
                    if (thisSuspicionListSize < smallestSuspicionListSize) {
                        
                        smallestSuspicionListSize = thisSuspicionListSize;
                    }
                }
                else {
                    
                    System.err.println("Cell " + neighbor.getKey() + " should be suspicous of a Wumpus in cell " + this.getKey() + ", but it isn't!");
                    pause = true;
                }
            }
        }
        
        // If an error was encountered, pause for effect
        if (pause) {
            
            try {
                Thread.sleep(5000);
            } catch (InterruptedException ex) {
                Logger.getLogger(KnowledgeFragment.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        
        return smallestSuspicionListSize;
    }
    
    /**
     * Return the size of the smallest suspicion list to which this cell belongs
     * Smaller means a tighter amount of suspicion on this cell!
     * @return 
     */
    public int getPitSuspicionScore() {
        
        int smallestSuspicionListSize = Integer.MAX_VALUE;
        int thisSuspicionListSize;
        boolean pause = false;
        
        for (KnowledgeFragment neighbor : connectedFragments) {
            
            if (neighbor.isVisited()) {
                
                if (neighbor.isSuspiciousOfPitInCell(this)) {
                    
                    thisSuspicionListSize = neighbor.getPitSuspicionListSize();
                    
                    if (thisSuspicionListSize < smallestSuspicionListSize) {
                        
                        smallestSuspicionListSize = thisSuspicionListSize;
                    }
                }
                else {
                    
                    System.err.println("Cell " + neighbor.getKey() + " should be suspicous of a pit in cell " + this.getKey() + ", but it isn't!");
                    pause = true;
                }
            }
        }
        
        // If the "suspicion" is a guarantee, it shouldn't be
        if (smallestSuspicionListSize == 1) {
            System.err.println("Cell " + this.getKey() + " should have a confirmed Wumpus, but it does not!");
            this.setWumpusFound(true);
            pause = true;
        }
        
        // If an error was encountered, pause for effect
        if (pause) {
            
            try {
                Thread.sleep(5000);
            } catch (InterruptedException ex) {
                Logger.getLogger(KnowledgeFragment.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        
        
        
        
        return smallestSuspicionListSize;
    }
}
