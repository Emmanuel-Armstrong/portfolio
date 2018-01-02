package actorcomponents;

import actors.Explorer;
import java.util.ArrayList;
import java.util.Random;
import java.util.Stack;
import java.util.logging.Level;
import java.util.logging.Logger;
import worldcomponents.Cell;

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

/**
 *
 * @author matthewrohrlach
 */
public class ReasoningInference {
 
    ArrayList<KnowledgeFragment> knowledge;
    ArrayList<KnowledgeFragment> safeUnvisitedList;
    ArrayList<KnowledgeFragment> visitedList;
    KnowledgeFragment currentFragment;
    KnowledgeFragment previousFragment;
    String previousAction;
    Explorer me;
    int sideDimension;
    int screamsHeard;
    int previousScreamsHeard;
    int numArrowsRemaining;
    int tempNumArrowsRemaining;
    
    Stack<String> pathToTake;
    Stack<KnowledgeFragment> cellsInPath;
    ArrayList<KnowledgeFragment> deadEndCells;
    ArrayList<KnowledgeFragment> destinationsToTake;
    int destination = Integer.MAX_VALUE;
    int popCounter;
    
    boolean verbose;
    
    /**
     * Regular constructor
     */
    public ReasoningInference() {
        safeUnvisitedList = new ArrayList<>();
        visitedList = new ArrayList<>();
        pathToTake = new Stack<>();
        cellsInPath = new Stack<>();
        deadEndCells = new ArrayList<>();
        destinationsToTake = new ArrayList<>();
        popCounter = 0;
        previousAction = "HALT";
        previousScreamsHeard = 0;
        numArrowsRemaining = Integer.MAX_VALUE;
        tempNumArrowsRemaining = Integer.MAX_VALUE;
        
        verbose = true;
    }
    
    /**
     * Update knowledge with current position, then use logical reasoning to make a decision
     * @param currentCell
     * @param sensesReport
     * @param meIn
     * @return 
     */
    public String findDecision(Cell currentCell, int sensesReport, Explorer meIn) {
        
        /**
         * The logical rules that compose the reasoning of each explorer are
         * spread between this inference class (the decision-making) and the
         * knowledge fragment class objects (representing the knowledge that
         * the explorer has about their world).
         * 
         * We understand that the rules might be hard to decipher, so here they
         * are in a human-readable format:
         * 
         * DECISION-MAKING STEPS:
         * 
         * World asks Explorer to make decision.
         * Explorer asks their reasoning inference class to give them an action.
         * Explorer gives action to world as a decision.
         * World accepts the decision and provides the consequences to the Explorer.
         * Repeat the above until gold is found or max number of actions is reached
         * 
         * DECISION-MAKING RULES:
         * 
         * Choose the first true action(actionToTake):
         *  gold(currentCell) ⇒ action(grab), at highest priority, ensures the gold is always grabbed if possible
         *      OR
         *  currentlyPathingToSpecificCell(inferenceClass) ⇒ action(nextPathAction), a list of actions that take the explorer to a cell from knowledge
         *      OR
         *  (destinationsAwaitingPathing ∧ destinationPossible(nextDestination) ⇒ (createPath(nextDestination) ∧ action(nextPathAction)), build path to next scheduled destination
         *      OR
         *  (safeUnvisitedCellExists(knowledge) ∧ destinationPossible(nextSafeUnvisitedCell(knowledge))) ⇒ (createPath(nextSafeUnvisitedCell(knowledge)) ∧ action(nextPathAction)), build path to next safe unvisited cell
         *      OR
         *  ((unvisitedFrontierCellExists(knowledge) ∧ destinationPossible(nextFrontierUnvisitedCell(knowledge))) ⇒ (createPath(nextFrontierUnvisitedCell(knowledge)) ∧ action(nextPathAction)), build path to best unvisited frontier cell by weighting distances
         *      ⇒
         *  nextCellHasWumpusSuspicion(knowledge) ⇒ fireIntoNextCell(nextFrontierUnvisitedCell)), pathing to a risky frontier cell implies that cell will be fired into beforehand
         *      OR
         *  action(halt), the world is unsolvable, either through certain-death (no remaining arrows) or blocked gold
         * 
         * KNOWLEDGE-FORMING RULES:
         * 
         *  All knowledge is represented by knowledge fragments that themselves represent cells.
         *  Each visited cell's fragment maintains a list of suspicions about its neighbors, determined through rules below.
         * 
         *  ∀ currentCell, ¬hazard(currentCell) ⇒ isVisited(currentCell) ∧ isSafe(currentCell)      no hazard confirms the cell as visited and safe
         *  ∀ currentCell, isSameCellUnexpectedly(currentCell, previousCell) ⇒ isObstacle(formerDestinationCell)        being returned to the same cell unexpectedly
         *  ∀ cell, isSafe(cell) ⇔ ¬hasWumpus(currentCell) ∧ ¬hasPit(currentCell)       all cells are safe if they have no wumpus or pit
         *  ∀ cell, isVisited(cell) ⇒ isSafe(currentCell) ∧ ¬isObstacle(currentCell)        all visited cells imply that they are also safe and have no obstacle
         *  ∀ neighboringCell, hasWumpusSuspicion(neighboringCell) ⇔ hasSmell(currentCell) ∧ ¬isSafe(neighboringCell)       all unsafe neighbors of a cell with a smell are suspected
         *  ∀ neighboringCell, hasPitSuspicion(neighboringCell) ⇔ hasWind(currentCell) ∧ ¬isSafe(neighboringCell)       all unsafe neighbors of a cell with wind are suspected
         *  ∀ currentCell, ¬hasSmell(currentCell) ∧ ¬hasWind(currentCell) ⇒ ∀ neighboringCell, isSafe(neighboringCell)      no smell and no wind implies safe neighbors for all visited cells
         *  ∀ cell, onlyOneWumpusSuspect(cell) ∧ ¬isSafe(suspectCell) ⇒ hasWumpus(suspectCell)      for all cells, only having one remaining unsafe Wumpus suspect implies a Wumpus in that neighbor
         *  ∀ cell, onlyOnePitSuspect(cell) ∧ ¬isSafe(suspectCell) ⇒ hasPit(suspectCell)        for all cells, only having one remaining unsafe pit suspect implies a pit in that neighbor
         *  ∀ cell, hasWumpus(cell) ⇔ ∀ neighborCell, hasSmell(neighborCell)        for all cells, a cell may contain a Wumpus if and only if all of its neighbors have a smell
         *  ∀ cell, hasPit(cell) ⇔ ∀ neighborCell, hasWind(neighborCell)        for all cells, a cell may contain a pit if and only if all of its neighbors have wind
         *  
         **/     
        
        // Get the knowledge of this explorer
        me = meIn;
        knowledge = me.getKnowledge();
        numArrowsRemaining = me.getNumArrows();
        tempNumArrowsRemaining = numArrowsRemaining;
        updateSideDimension();
        
        // Get the knowledge fragment associated with the current cell
        previousFragment = currentFragment;
        currentFragment = knowledge.get(currentCell.getKey());
        
        // Check to see if a Wumpus was killed in the last action:
        screamsHeard = me.getScreamsHeard();
        if (screamsHeard > previousScreamsHeard) {
            
            // Add every visited cell in the faced direction with a confirmed adjacent Wumpus to a destination list.
            // If a Wumpus was killed offscreen, we need to update our safe, unvisited list.
            // Every destination will be visited in nearest to furthest order.
            
            buildWumpusTestingDestinationList();
        }
        previousScreamsHeard = screamsHeard;
        
        // Handle an incidental visit to a destination in the list
        if (destinationsToTake.contains(currentFragment)) {
            destinationsToTake.remove(currentFragment);
        }
        
        // Handle an obstacle encounter if a movement was made
        //  "~A & B & (~C | D | E)"
        if (!(previousFragment == null) && (previousFragment.getKey() == currentFragment.getKey()) 
                && (!previousAction.contains("FIRE") || previousAction.contains("AFTER")) && !previousAction.contains("HALT")) {
            KnowledgeFragment previousDestination = matchActionToDestination(currentFragment, previousAction);
            if (previousDestination != null) {
                previousDestination.setObstacleFound(true);
            }
            // Path is ruined, end pathing
            pathToTake.clear();
            destination = Integer.MAX_VALUE;
            safeUnvisitedList.remove(previousDestination);
            System.out.println("\"I hit an obstacle! Better stop and find my bearings.\"");
        }
        
        // Update the current knowledge fragment
        if (currentCell.getIsEmpty() || currentCell.getHasGold()) {
            currentFragment.setSafe();
            currentFragment.setVisited();
            
            // Update the knowledge database with the new knowledge that this cell is safe
            updateVisitedList(currentFragment);
        }
        // A wumpus has killed this explorer
        else if (currentCell.getHasWumpus()) {
            currentFragment.setWumpusFound(true);
            pathToTake.clear();
            destination = Integer.MAX_VALUE;
            return "HALT";
        }
        // A pit has killed this explorer
        else if (currentCell.getHasPit()) {
            currentFragment.setPitFound(true);
            pathToTake.clear();
            destination = Integer.MAX_VALUE;
            return "HALT";
        }
        // Something has gone wrong, and the current cell should not be visitable
        else if (currentCell.getHasObstacle()) {
            currentFragment.setObstacleFound(true);
            pathToTake.clear();
            destination = Integer.MAX_VALUE;
            System.err.println("The explorer is inside an obstacle! They suffocate. How did that even happen?");
            return "HALT";
        }
        // Something has gone VERY wrong, and current cell is not empty and also empty
        else {
            System.err.println("The explorer has entered a super-position state! The current cell is empty, yet not empty.");
            pathToTake.clear();
            destination = Integer.MAX_VALUE;
            return "HALT";
        }
        
        // Apply the senses of this cell to the fragment's knowledge of its surroundings
        currentFragment.setObstacleFound(false);
        currentFragment.updateThisFragment(sensesReport);
        
        // Update the knowledge of safe, unvisited cells created by previous or above updates
        updateSafeUnvisitedList();
        
        // Assert the pathing action was successful
        if (pathToTake.isEmpty() && (destination != currentFragment.getKey() 
                && destination < knowledge.size() 
                && !knowledge.get(destination).isObstacleFound())) {
            System.err.println("Error reaching destination!");
            destination = Integer.MAX_VALUE;
            try {
                Thread.sleep(5000);
            } catch (InterruptedException ex) {
                Logger.getLogger(ReasoningInference.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        
        Random rand = new Random();
        String nextAction;
        
        // If the gold is in this cell
        if (currentCell.getHasGold()) {
            
            // There is only one action to take when the gold is underfoot
            nextAction = "GRAB";
        }
        // Else, if there is no path to follow currently
        else if (pathToTake.isEmpty()) {
            
            // Try every safe, unvisited cell first
            if (safeUnvisitedList.size() > 0) {
                
                if (safeUnvisitedList.size() > 1) {
                    System.out.println("\n\"There are cells I know are safe, but I haven't visited them yet.\"");
                }
                else {
                    System.out.println("\n\"There is a cell I know is safe, but I haven't visited it yet.\"");
                }
                
                int safeUnvisitedIndex = 0;
                double minDistanceToSafeUnvisited = Integer.MAX_VALUE;
                double thisDistanceToSafeUnvisited;
                
                // Find the nearest safe unvisited cell with the greatest number of unvisited neighbors
                for (int i = 0; i < safeUnvisitedList.size(); i++) {
                    
                    thisDistanceToSafeUnvisited = (double) distanceBetweenCellsInActions(safeUnvisitedList.get(i))
                            / (double) safeUnvisitedList.get(i).getNumberOfUnvisitedNeighbors(); 
                    
                    if (thisDistanceToSafeUnvisited < minDistanceToSafeUnvisited) {
                        
                        safeUnvisitedIndex = i;
                        minDistanceToSafeUnvisited = thisDistanceToSafeUnvisited;
                    }
                }
                
                // If a path was created successfully
                if (buildPathToCell(safeUnvisitedList.get(safeUnvisitedIndex))) {
                    
                    destination = safeUnvisitedList.get(safeUnvisitedIndex).getKey();
                    System.out.println("\"Let's try moving from cell " + currentFragment.getKey() + " to " + destination + "!\"");
                    if (pathToTake.size() > 0) {
                        nextAction = pathToTake.pop();
                    }
                    else {
                        nextAction = ("HALT");
                    }
                }
                else { 
                    nextAction = ("HALT");
                }
            }
            
            // Else if there are cell destinations that need to be visited
            else if (destinationsToTake.size() > 0) {
                
                System.out.println("\n\"I need to revisit a cell that might not have a Wumpus anymore.\"");
                
                // Sort the destination list by distance from the current fragment's cell
                sortListByDistanceFromSource(destinationsToTake);
                
                // Get the next destination in the list (i.e. the closest cell to visit)
                KnowledgeFragment nextDestination = destinationsToTake.get(0);
                destinationsToTake.remove(0);
                
                if (buildPathToCell(nextDestination)) {
                    destination = nextDestination.getKey();
                    System.out.println("\"Let's try moving from cell " + currentFragment.getKey() + " to " + destination + "!\"");
                    if (pathToTake.size() > 0) {
                        nextAction = pathToTake.pop();
                    }
                    else {
                        nextAction = ("HALT");
                    }
                }
                else { 
                    nextAction = ("HALT");
                }
            }
            
            // Pick the nearest non-suspicious (or, failing that, suspicious) unvisited cell along the cell frontier
            else {
                
                System.out.println("\n\"I don't have any good options. I should pick the nearest cell I trust the most.\"");
                
                // This int is the minimum, weighted distance to the target cell
                double minimumWeightedDistance = Integer.MAX_VALUE;
                double weightedDistance;
                
                // Start target cell at a randomly chosen cell within the world
                KnowledgeFragment frontierDestination = knowledge.get(rand.nextInt(sideDimension*sideDimension));
                
                // Make sure this randomly chosen cell is not known-dangerous or visited
                while(frontierDestination.isWumpusFound() || frontierDestination.isPitFound() || frontierDestination.isObstacleFound() || frontierDestination.isVisited()) {
                        
                    frontierDestination = knowledge.get(rand.nextInt(sideDimension*sideDimension));
                }
                
                ArrayList<KnowledgeFragment> tiedDestinations = new ArrayList<>();
                
                // For every visited cell
                for (KnowledgeFragment visitedFragment : visitedList) {
                    
                    // Take that visited cell's neighbors and assign them a cost of traversal
                    for (KnowledgeFragment neighborFragment : visitedFragment.getConnectedFragments()) {
                        
                        // Exclude the neighbors that are useless for exploration
                        //!(neighborFragment.isObstacleFound() 
                        //        || neighborFragment.isPitFound() 
                        //        || neighborFragment.isVisited())
                        if (!neighborFragment.isObstacleFound() && !neighborFragment.isPitFound()
                                && !neighborFragment.isVisited()) {
                            
                            weightedDistance = distanceBetweenCellsInActions(neighborFragment);
                            
                            // Completely disregard the fragment if it is a known wumpus square, and there are no arrows
                            if (neighborFragment.isWumpusFound() && me.getNumArrows() < 1) {
                                continue;
                            }
                            
                            // Disregard the fragment if it has no unvisited neighbors
                            if (!neighborFragment.isNeighborOfUnvisited()) {
                                continue;
                            }
                            
                            // Weight the fragment negatively if it is suspected of having a pit
                            if (neighborFragment.isPitSuspicious()) {
                                weightedDistance += 10000;
                                
                                // Then scale this weighted distance by the size of the smallest suspicion list to which it belongs (higher is better)
                                weightedDistance /= (double) neighborFragment.getPitSuspicionScore() * 2;
                            }
                            
                            // Weight the fragment negatively if it is suspected of having a wumpus, and there are no arrows
                            if (neighborFragment.isWumpusSuspicious() && me.getNumArrows() < 1) {
                                weightedDistance += 10000;
                                
                                // Then scale this weighted distance by the size of the smallest suspicion list to which it belongs (higher is better)
                                weightedDistance /= (double) neighborFragment.getWumpusSuspicionScore() * 2;
                            }
                            
                            // Scale weight by number of unvisited connectors of this neighbor (higher is better)
                            weightedDistance /= (double) neighborFragment.getNumberOfUnvisitedNeighbors();
                            
                            // Test to see if this is a better destination from the current cell
                            if (weightedDistance < minimumWeightedDistance) {
                                
                                tiedDestinations.clear();
                                minimumWeightedDistance = weightedDistance;
                                frontierDestination = neighborFragment;
                                tiedDestinations.add(neighborFragment);
                            }
                            
                            // Add to list for tie-breaking
                            else if (weightedDistance == minimumWeightedDistance) {
                                
                                tiedDestinations.add(neighborFragment);
                            }
                        }
                    }
                }
                
                if (tiedDestinations.size() > 0) {
                    
                    frontierDestination = tiedDestinations.get(rand.nextInt(tiedDestinations.size()));
                }
                
                // Flavor text for when the frontier destination might have a Wumpus
                if (frontierDestination.isWumpusSuspicious() || frontierDestination.isWumpusFound()) {
                    
                    if (me.getNumArrows() > 0) {
                        System.out.println("\"I know a cell that might have a Wumpus. I still have an arrow to find out...\"");
                    }
                    
                    else {
                        System.out.println(""
                                + "\"I know a cell that might have a Wumpus. No arrows, but who cares?\"");
                    }
                }
                
                System.out.println("\"I picked cell " + frontierDestination.getKey() + " with a weighted distance of " + minimumWeightedDistance + ".\"");
                
                // Try to go to the target frontier cell
                if (buildPathToCell(frontierDestination)) {
                    destination = frontierDestination.getKey();
                    System.out.println("\"Let's try moving from cell " + currentFragment.getKey() + " to " + destination + "!\"");
                    if (pathToTake.size() > 0) {
                        nextAction = pathToTake.pop();
                    }
                    else {
                        nextAction = ("HALT");
                    }
                }
                else {
                    nextAction = ("HALT");
                }
            }
        }
        else {
            // Else continue following the path
            nextAction = pathToTake.pop();
        }
        
        previousAction = nextAction;
        return nextAction;
    }
    
    /**
     * Use recursion to push the necessary directions to reach a target to a stack
     * Starting backwards, generate a path through visited cells or to the nearest visited cell
     * @param currentPathFragment 
     * @return  
     */
    public boolean buildPathToCell(KnowledgeFragment currentPathFragment) {
        
        // Base case: The currently-focused cell is the destination
        if (currentPathFragment.getKey() == currentFragment.getKey()) {
            cellsInPath.clear();
            deadEndCells.clear();
            return true;
        }
        
        // Record this cell as part of the path to be taken towards the goal
        cellsInPath.push(currentPathFragment);
        
        KnowledgeFragment nextPathFragment;
        boolean wumpusInNextMove = false;
        Random rand = new Random();
        
        // If the currently-focused cell has visited neighbors
        if (currentPathFragment.isNeighborOfVisited()) {
            
            // Find the neighbor nearest to the overall destination.
            int minDistanceToEnd = Integer.MAX_VALUE;
            int distanceToEnd;
            ArrayList<KnowledgeFragment> tiedNeighborDistances = new ArrayList<>();
            KnowledgeFragment nearestNeighborToEnd;
            
            for (KnowledgeFragment currentPathNeighbor 
                    : currentPathFragment.getConnectedFragments()) {
                
                // First check if this neighbor is visited and not a dead end:
                if (currentPathNeighbor.isVisited() && !deadEndCells.contains(currentPathNeighbor) 
                        && !cellsInPath.contains(currentPathNeighbor)) {
                    
                    distanceToEnd = distanceBetweenCells(currentPathNeighbor, currentFragment);
                    // If this is the nearest neighbor to the end
                    if (distanceToEnd < minDistanceToEnd) {

                        minDistanceToEnd = distanceToEnd;
                        tiedNeighborDistances.clear();
                        tiedNeighborDistances.add(currentPathNeighbor);
                    }
                    // If this is tied with the nearest neighbor to the end
                    else if (distanceToEnd == minDistanceToEnd) {
                        tiedNeighborDistances.add(currentPathNeighbor);
                    }
                }
            }
            
            // Break ties in neighbor distances to the end goal
            if (tiedNeighborDistances.size() > 0) {
                nearestNeighborToEnd = 
                        tiedNeighborDistances.get(
                                rand.nextInt(tiedNeighborDistances.size()));
                nextPathFragment = nearestNeighborToEnd;
            }
            else {
                nextPathFragment = null;
            }
        }
        // If the currently-focused cell has no visited neighbors
        else {
            
            // Find the neighbor nearest to the nearest visited cell.
            // First, find the nearest visited cell:
            int minDistanceToVisited = Integer.MAX_VALUE;
            int distanceToVisited;
            int distanceFromVisitedToEnd;
            int minDistanceFromVisitedToEnd;
            KnowledgeFragment nearestVisitedFragment = null;
            
            for (KnowledgeFragment visitedPathFragment : visitedList) {
                
                distanceToVisited = 
                        distanceBetweenCells(currentPathFragment, visitedPathFragment);
                // If this is the nearest visited cell
                if (distanceToVisited < minDistanceToVisited) {
                    
                    minDistanceToVisited = distanceToVisited;
                    nearestVisitedFragment = visitedPathFragment;
                }
                // If this visited cell is equidistant with the nearest visited cell
                else if (distanceToVisited == minDistanceToVisited) {
                    
                    // Compare distance to the end goal
                    distanceFromVisitedToEnd = distanceBetweenCells(visitedPathFragment, currentFragment);
                    minDistanceFromVisitedToEnd = distanceBetweenCells(nearestVisitedFragment, currentFragment);
                    if (distanceFromVisitedToEnd < minDistanceFromVisitedToEnd) {
                        
                        nearestVisitedFragment = visitedPathFragment;
                    }
                    // If distance to the end goal is the same from both visited cells
                    else if (distanceFromVisitedToEnd == minDistanceFromVisitedToEnd) {
                        
                        // Prevent de-referencing a null pointer, or
                        if (nearestVisitedFragment == null) {
                            nearestVisitedFragment = visitedPathFragment;
                        }
                        // Rank by key as a way of guaranteeing a deterministic preference
                        else if (visitedPathFragment.getKey() < nearestVisitedFragment.getKey()) {
                            nearestVisitedFragment = visitedPathFragment;
                        }
                    }
                }
            }
            
            // Next, find the neighbor nearest to that visted cell
            int minDistanceFromNeighbor = Integer.MAX_VALUE;
            int distanceFromNeighbor;
            ArrayList<KnowledgeFragment> tiedNeighborDistances = new ArrayList<>();
            KnowledgeFragment nearestNeighborToVisited;
            
            // For every neighbor of the currently-focused cell
            for (KnowledgeFragment currentPathNeighbor 
                    : currentPathFragment.getConnectedFragments()) {
                
                // If there is no pit or obstacle in this neighbor, and it has not been banned...
                //!(currentPathNeighbor.isObstacleFound() || currentPathNeighbor.isPitFound()
                //        || deadEndCells.contains(currentPathNeighbor) 
                //        || cellsInPath.contains(currentPathNeighbor))
                if (!currentPathNeighbor.isObstacleFound() && !currentPathNeighbor.isPitFound()
                        && !deadEndCells.contains(currentPathNeighbor) && !cellsInPath.contains(currentPathNeighbor)) {
                    
                    // If there is no wumpus or still the capability to kill a Wumpus
                    if (tempNumArrowsRemaining > 0 || !currentPathNeighbor.isWumpusFound()) {
                        tempNumArrowsRemaining--;
                        
                        distanceFromNeighbor = distanceBetweenCells(currentPathNeighbor, nearestVisitedFragment);
                        // If this is the nearest neighbor to the nearest visited cell
                        if (distanceFromNeighbor < minDistanceFromNeighbor) {

                            minDistanceFromNeighbor = distanceFromNeighbor;
                            tiedNeighborDistances.clear();
                            tiedNeighborDistances.add(currentPathNeighbor);
                        }
                        // If this is tied with the nearest neighbor
                        else if (distanceFromNeighbor == minDistanceFromNeighbor) {
                            tiedNeighborDistances.add(currentPathNeighbor);
                        }
                    }
                }
            }
            
            // Add a fire into this next cell action if applicable
            if (tiedNeighborDistances.size() > 0) {
                // Break the tie, if applicable, of nearest visited cells
                 nearestNeighborToVisited = 
                    tiedNeighborDistances.get(
                        rand.nextInt(tiedNeighborDistances.size()));
                
                nextPathFragment = nearestNeighborToVisited;
            }
            else {
                
                nextPathFragment = null;
            }
        }
        
        // If there is no next fragment, end the path-building from this fragment
        if (nextPathFragment == null) {
            
            // If this is the last fragment, and path-building failed, end path-building
            if (cellsInPath.size() == 1) {
                cellsInPath.clear();
                deadEndCells.clear();
                pathToTake.clear();
                return false;
            }
            // If this is not the last fragment in the path built, mark this path as a dead-end
            else {
                // Remove this cell from the path
                cellsInPath.pop();
                // Next cell is the previous cell
                nextPathFragment = cellsInPath.pop();
                // Remove this cell from consideration
                deadEndCells.add(currentPathFragment);
                // Remove the action taken to get to this cell
                pathToTake.pop();
            }
        }
        else {
            // Translate fragment into a direction to push, actions are added in reverse order
            // Because the method works backwards, actions are reversed, also
            int differenceInKeys = currentPathFragment.getKey() - nextPathFragment.getKey();
            
            // Handle the proper action when a wumpus is suspected or known to exist in this fragment's cell
            if (currentPathFragment.isWumpusFound() || currentPathFragment.isWumpusSuspicious()) {
                wumpusInNextMove = true;
            }
            
            // Next direction is south
            if (differenceInKeys > 1) {
                if (wumpusInNextMove && (me.getNumArrows() > 0)) {
                    pathToTake.push("SOUTH");
                    pathToTake.push("FIRESOUTH");
                }
                else {
                    pathToTake.push("SOUTH");
                }
            }
            // Next direction is east
            else if (differenceInKeys == 1) {
                if (wumpusInNextMove && (me.getNumArrows() > 0)) {
                    pathToTake.push("EAST");
                    pathToTake.push("FIREEAST");
                }
                else {
                    pathToTake.push("EAST");
                }
            }
            // Next direction is west
            else if (differenceInKeys == (-1)) {
                if (wumpusInNextMove && (me.getNumArrows() > 0)) {
                    pathToTake.push("WEST");
                    pathToTake.push("FIREWEST");
                }
                else {
                    pathToTake.push("WEST");
                }
            }
            else { // Next direction is north
                if (wumpusInNextMove && (me.getNumArrows() > 0)) {
                    pathToTake.push("NORTH");
                    pathToTake.push("FIRENORTH");
                }
                else {
                    pathToTake.push("NORTH");
                }
            }
        }
        
        return buildPathToCell(nextPathFragment);
    }
    
    /**
     * Create a list of visited cells that must be re-tested for an adjacent Wumpus
     */
    protected void buildWumpusTestingDestinationList() {
        
        // First, revisit any visited cell that may be affected by an arrow fired in this direction hitting a Wumpus
        
        // From the current cell, continuing in the direction of the previous action, find the next cell
        KnowledgeFragment possibleWumpusKilledCell = matchArrowPathToDestination(currentFragment, previousAction);
        while (possibleWumpusKilledCell != null) {
            
            // If the previous cell is not the current cell
            if (currentFragment != possibleWumpusKilledCell) {
                // If there is at least one visible neighbor...
                if (possibleWumpusKilledCell.isNeighborOfVisited()) {

                    // Find all visited neighbors, and add them as future destinations
                    for (KnowledgeFragment neighbor : possibleWumpusKilledCell.getConnectedFragments()) {

                        // Limit destinations to those with pre-conceived notions of Wumpus positions
                        if (neighbor.isVisited() && 
                                (neighbor.isNeighborOfWumpusSuspicion() || neighbor.isNeighborOfWumpusFound())) {
                            neighbor.setWumpusConfirmationAllowed(false);

                            // Then add this neighbor to the list of future destinations
                            if (!destinationsToTake.contains(neighbor)) {
                                destinationsToTake.add(neighbor);
                                
                                if (verbose && !(neighbor.getKey() == currentFragment.getKey())) {
                                    System.out.println("\"Need to revisit Cell " + neighbor.getKey() + " to see if it still has a Wumpus around.\"");
                                }
                            }
                        }
                    }
                }
            }
            possibleWumpusKilledCell = matchArrowPathToDestination(possibleWumpusKilledCell, previousAction);
        }
    }
    
    /**
     * Find the distance in cardinal-direction moves between two cells
     * @param source
     * @param destination
     * @return 
     */
    protected int distanceBetweenCells(KnowledgeFragment source, KnowledgeFragment destination) {
        
        int distanceY = Math.abs((source.getKey() / sideDimension) - (destination.getKey() / sideDimension));
        int distanceX = Math.abs((source.getKey() % sideDimension) - (destination.getKey() % sideDimension));
        int returnDistance = distanceY + distanceX;
        
        return returnDistance;
    }
    
    /**
     * Return a distance in the number of actions necessary to move to a cell (rotations not counted, yet)
     * @param destination
     * @return 
     */
    protected int distanceBetweenCellsInActions(KnowledgeFragment destination) {
        
        int numberOfActions;
        int currentPathSize = pathToTake.size();
        if (destination != currentFragment) {
            
            // Backup the current path
            Stack<String> temporaryPath = new Stack<>();
            for (int i = 0; i < currentPathSize; i++) {
                temporaryPath.push(pathToTake.pop());
            }
            
            // Build a path to the destination target and count the number of actions
            if (buildPathToCell(destination)) {
                
                numberOfActions = pathToTake.size();
                pathToTake.clear();
            }
            // If the destination is impossible, the action cost is max value
            else {
                numberOfActions = Integer.MAX_VALUE;
            }

            // If the path needed to be backed up, restore the path
            if (temporaryPath.size() > 0) {
                for (int i = 0; i < temporaryPath.size(); i++) {
                    pathToTake.push(temporaryPath.pop());
                }
                temporaryPath.clear();
            }
        }
        else {
            numberOfActions = 0;
        }
        
        // Assert that the path was restored to its original size
        if (currentPathSize != pathToTake.size()) {
            System.err.println("Error restoring path!");
        }
        
        // Return the number of actions it would take to reach this cell
        return numberOfActions;
    }
    
    /**
     * Arrange a list of fragments in least to greatest cell distance from the given fragment's cell
     * @param list 
     */
    protected void sortListByDistanceFromSource(ArrayList<KnowledgeFragment> list) {
        
        ArrayList<KnowledgeFragment> newList = new ArrayList<>();
        int originalListSize = list.size();
        int minDistance;
        int minDistanceIndex = 0;
        int thisDistance;
        for (int i = 0; i < originalListSize; i++) {
            minDistance = Integer.MAX_VALUE;
            for (int j = 0; j < list.size(); j++) {
            
                thisDistance = distanceBetweenCellsInActions(list.get(j));
                if (thisDistance < minDistance) {
                    minDistanceIndex = j;
                    minDistance = thisDistance;
                }
                else if (thisDistance == minDistance) {
                    
                    if (list.get(j).getKey() < currentFragment.getKey()) {
                        minDistanceIndex = j;
                        minDistance = thisDistance;
                    }
                }
            }
            newList.add(list.get(minDistanceIndex));
            list.remove(minDistanceIndex);
        }
        list.clear();
        for (KnowledgeFragment orderedFragment : newList) {
            list.add(orderedFragment);
        }
    }
    
    /**
     * Turn the given action into a neighbor of the current fragment
     * @param sourceFragment
     * @param action
     * @return 
     */
    protected KnowledgeFragment matchActionToDestination(KnowledgeFragment sourceFragment, String action) {
        
        int currentKey = sourceFragment.getKey();
        KnowledgeFragment returnDestination = null;
        
        if (action.contains("FIRE") && !action.contains("AFTER")) {
            
            returnDestination = sourceFragment;
        }
        else if (action.contains("WEST")) {
            
            for (KnowledgeFragment currentNeighbor : sourceFragment.getConnectedFragments()) {
                
                if (currentNeighbor.getKey() == (currentKey-1)) {
                    returnDestination = currentNeighbor;
                    break;
                }
            }
        }
        else if (action.contains("EAST")) {
            
            for (KnowledgeFragment currentNeighbor : sourceFragment.getConnectedFragments()) {
                
                if (currentNeighbor.getKey() == (currentKey+1)) {
                    returnDestination = currentNeighbor;
                    break;
                }
            }
        }
        else if (action.contains("NORTH")) {
            
            for (KnowledgeFragment currentNeighbor : sourceFragment.getConnectedFragments()) {
                
                if (currentNeighbor.getKey() == (currentKey-sideDimension)) {
                    returnDestination = currentNeighbor;
                    break;
                }
            }
        }
        else if (action.contains("SOUTH")) {
            
            for (KnowledgeFragment currentNeighbor : sourceFragment.getConnectedFragments()) {
                
                if (currentNeighbor.getKey() == (currentKey+sideDimension)) {
                    returnDestination = currentNeighbor;
                    break;
                }
            }
        }
        
        if (returnDestination == null) {
            returnDestination = sourceFragment;
            System.out.println("\"Heading to that cell is impossible or certain-death!\"");
        }
        
        return returnDestination;
    }
    
    /**
     * Give the cell in the direction of the last fired arrow
     * @param sourceFragment
     * @param action
     * @return 
     */
    protected KnowledgeFragment matchArrowPathToDestination(KnowledgeFragment sourceFragment, String action) {
        
        int currentKey = sourceFragment.getKey();
        KnowledgeFragment returnDestination = null;
        
        if (action.contains("WEST")) {
            
            for (KnowledgeFragment currentNeighbor : sourceFragment.getConnectedFragments()) {
                
                if (currentNeighbor.getKey() == (currentKey-1)) {
                    returnDestination = currentNeighbor;
                    break;
                }
            }
        }
        else if (action.contains("EAST")) {
            
            for (KnowledgeFragment currentNeighbor : sourceFragment.getConnectedFragments()) {
                
                if (currentNeighbor.getKey() == (currentKey+1)) {
                    returnDestination = currentNeighbor;
                    break;
                }
            }
        }
        else if (action.contains("NORTH")) {
            
            for (KnowledgeFragment currentNeighbor : sourceFragment.getConnectedFragments()) {
                
                if (currentNeighbor.getKey() == (currentKey-sideDimension)) {
                    returnDestination = currentNeighbor;
                    break;
                }
            }
        }
        else if (action.contains("SOUTH")) {
            
            for (KnowledgeFragment currentNeighbor : sourceFragment.getConnectedFragments()) {
                
                if (currentNeighbor.getKey() == (currentKey+sideDimension)) {
                    returnDestination = currentNeighbor;
                    break;
                }
            }
        }
        
        return returnDestination;
    }
    
    /**
     * Use inferences made from collected knowledge to determine safe, unvisited cells
     */
    public void updateSafeUnvisitedList() {
        
        // Update the list of unvisited, save cells
        for (KnowledgeFragment fragment : knowledge) {
            
            if (fragment.isSafe() && !fragment.isObstacleFound() 
                    && !fragment.isVisited() 
                    && !safeUnvisitedList.contains(fragment)) {
            
                safeUnvisitedList.add(fragment);
            }
            
            else if (!fragment.isPitFound() 
                    && fragment.isPitSet() 
                    && !fragment.isWumpusFound() 
                    && fragment.isWumpusSet() 
                    && !fragment.isObstacleFound() 
                    && !fragment.isVisited() 
                    && !safeUnvisitedList.contains(fragment)) {
            
                fragment.setSafe();
                safeUnvisitedList.add(fragment);
            }
        }
    }
    
    /**
     * Mark the current cell as visited, remove it from safe frontier of cells if necessary
     * @param fragmentVisited 
     */
    public void updateVisitedList(KnowledgeFragment fragmentVisited) {
        
        // Add to list of visited (known-safe and visitable) cells
        if (!visitedList.contains(fragmentVisited)) {
            visitedList.add(fragmentVisited);
        }
        
        // Remove from list of safe, yet not known-visitable, cells
        if (safeUnvisitedList.contains(fragmentVisited)) {
            safeUnvisitedList.remove(fragmentVisited);
        }
    }
    
    /**
     * Ensure that the visited list is completely up-to-date
     */
    public void updateEntireVisitedList() {
        
        for (KnowledgeFragment fragment : knowledge) {
            
            if (fragment.isVisited()) {
                
                if (!visitedList.contains(fragment)) {
                    visitedList.add(fragment);
                }
                
                safeUnvisitedList.remove(fragment);
            }
        }
    }
    
    /**
     * Update the side dimension known by the class. The explorer does not use this to make a decision.
     */
    public final void updateSideDimension() {
        
        sideDimension = (int)Math.ceil(Math.sqrt(knowledge.size()));
        if ((sideDimension % 5) != 0) {
            System.err.print("Incorrect side dimension assigned!");
        }
    }
    
    /**
     * Set the position from which decisions will be made
     * @param newPosition 
     */
    public void setCurrentFragment(KnowledgeFragment newPosition) {
        
        this.currentFragment = newPosition;
    }
    
    /**
     * Replace the entire knowledge bank (Will be overwritten by explorer's knowledge if findAction() is called!)
     * @param newKnowledge 
     */
    public void setKnowledge(ArrayList<KnowledgeFragment> newKnowledge) {
        
        this.knowledge = newKnowledge;
    }
}
