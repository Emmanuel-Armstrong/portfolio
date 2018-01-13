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

import actors.Explorer;
import java.util.ArrayList;
import java.util.Random;
import java.util.Stack;
import worldcomponents.Cell;

/**
 * Class that uses suppositions about neighboring cells to formulate decisions
 * @author matthewrohrlach
 */
public class ReactionaryInference {
   
    ArrayList<Integer> timesVisitedEachCell;
    int[] timesVisitedEachCellArray = new int[625];
    ArrayList<Cell> visitedListOfCells;
    ArrayList<Integer> bannedListOfCells;
    ArrayList<Integer> safeUnvisitedListOfCells;
    ArrayList<Integer> visitedCellKeys;
    String previousAction;
    String nextAction;
    Explorer me;
    int numArrowsRemaining;
    int previousScreamsHeard;
    Stack<String> pathToTake;
    Cell previousCell;
    
    /**
     * Regular constructor
     */
    public ReactionaryInference(){
        previousAction = "HALT";
        timesVisitedEachCell = new ArrayList<>();
        visitedListOfCells = new ArrayList<>();
        bannedListOfCells = new ArrayList<>();
        
        safeUnvisitedListOfCells = new ArrayList<>();
        visitedCellKeys = new ArrayList<>();
        pathToTake = new Stack<>();
        previousCell = null;
    }
    
    /**
     * Take the current cell and find a decision to give to the explorer
     * @param currentCell
     * @param sensesReport
     * @param meIn
     * @return 
     */
    public String findDecision(Cell currentCell, int sensesReport, Explorer meIn) {
        me = meIn;
        numArrowsRemaining = me.getNumArrows();
        
        updateVisitedList(currentCell);
        updateSafeUnvisitedList(currentCell);
        
        // Handle the explorer bumping into an obstacle, as this alters their perceptions about what is safe in neighboring cells
        if (!(previousCell == null) && (previousCell.getKey() == currentCell.getKey()) 
                && (!previousAction.contains("FIRE") || previousAction.contains("AFTER")) && !previousAction.contains("HALT")) {
            int previousDestinationKey = matchActionToDestination(currentCell, previousAction);
            if (previousDestinationKey != -1) {
                if (!bannedListOfCells.contains(previousDestinationKey)) {
                    bannedListOfCells.add(previousDestinationKey);
                }
            }
        }
        
        // The explorer makes decisions about what they believe is in neighboring cells. A death prevents the current cell from being revisited.
        if (currentCell.getHasPit() || currentCell.getHasObstacle() || (currentCell.getHasWumpus() && numArrowsRemaining < 1)) {
            
            if (!bannedListOfCells.contains(currentCell.getKey())) {
                bannedListOfCells.add(currentCell.getKey());
            }
            previousCell = currentCell;
            previousAction = "HALT";
            return "HALT";
        }
        
        // Get the number of times the current cell has been visited
        int timesVisitedThisCell = timesVisitedEachCellArray[currentCell.getKey()];
        
        // If the gold is here, grab the gold
        if (currentCell.getHasGold()) {
            
            // There is only one action to take when the gold is underfoot
            nextAction = "GRAB";
        }
        // If there is a direction to fire in, fire until a scream is heard
        else if (pathToTake.size() > 0) {
            if (previousScreamsHeard < me.getScreamsHeard()) {
                pathToTake.clear();
                nextAction = me.getFacedDirection();
            }
            else {
                nextAction = pathToTake.pop();
            }
        }
        // If there is no smell or wind, pick a random direction
        else if (sensesReport == 0){
            
            // We're desperate
            if (timesVisitedThisCell > 10) {

                // Find an action inappropriately
                nextAction = getRandomAction(currentCell);
            }

            else {
                
                // Find an action appropriately
                nextAction = getAvailableAction(currentCell);
            }
        }
        
        // If there is smell, no wind
        else if (sensesReport == 1){
            
            
            // Fire if there are sufficient arrows
            if((currentCell.getConnectedCells().size()-1) <= numArrowsRemaining){
                
                fireUntilScream(currentCell);
                if (pathToTake.size() > 0) {
                    nextAction = pathToTake.pop();
                }
            }
            
            // Or go back
            else{
                
                // We're desperate
                if (timesVisitedThisCell > 15) {
                
                    // Find an action inappropriately
                    if (numArrowsRemaining > 0) {
                        fireUntilScream(currentCell);
                        if (pathToTake.size() > 0) {
                            nextAction = pathToTake.pop();
                        }
                    }
                    else {
                        nextAction = getRandomAction(currentCell);
                    }
                }
                
                // We're getting antsy
                else if (timesVisitedThisCell > 5) {
                
                    // Find an action appropriately
                    if (numArrowsRemaining > 0) {
                        fireUntilScream(currentCell);
                        if (pathToTake.size() > 0) {
                            nextAction = pathToTake.pop();
                        }
                    }
                    else {
                        nextAction = getAvailableAction(currentCell);
                    }
                }
                
                // This is fine
                else {
                    
                    nextAction = "BACK";
                }
            }
        }
        
        // If there is no smell, but wind
        else if (sensesReport == 2){
            
            // We're desperate
            if (timesVisitedThisCell > 15) {

                // Find an action inappropriately
                nextAction = getRandomAction(currentCell);
            }

            // We're getting antsy
            else if (timesVisitedThisCell > 5) {

                // Find an action appropriately
                nextAction = getAvailableAction(currentCell);
            }
            // This is fine
            else {
                
                nextAction = "BACK";
            }
            
        }
        
        // If there is both smell and wind
        else if (sensesReport == 3){
            
            // Fire if there are sufficient arrows
            if((currentCell.getConnectedCells().size()-1) <= numArrowsRemaining){
                
                fireUntilScream(currentCell);
                if (pathToTake.size() > 0) {
                    nextAction = pathToTake.pop();
                }
            }
            
            // Or go back
            else{
                
                // We're desperate
                if (timesVisitedThisCell > 30) {
                
                    // Find an action inappropriately
                    if (numArrowsRemaining > 0) {
                        fireUntilScream(currentCell);
                        if (pathToTake.size() > 0) {
                            nextAction = pathToTake.pop();
                        }
                    }
                    else {
                        nextAction = getRandomAction(currentCell);
                    }
                }
                
                // We're getting antsy
                else if (timesVisitedThisCell > 10) {
                
                    // Find an action appropriately
                    if (numArrowsRemaining > 0) {
                        fireUntilScream(currentCell);
                        if (pathToTake.size() > 0) {
                            nextAction = pathToTake.pop();
                        }
                    }
                    else {
                        nextAction = getAvailableAction(currentCell);
                    }
                }
                
                // This is fine
                else {
                    
                    nextAction = "BACK";
                }
            }
        }
        
        // There is no possible movement, do nothing to save points
        else{
            
            nextAction = "HALT";
        }
        previousCell = currentCell;
        previousScreamsHeard = me.getScreamsHeard();
        previousAction = nextAction;
        return nextAction;
    }
    
    /**
     * Fire in all cells but backwards
     * @param currentCell 
     */
    public void fireUntilScream(Cell currentCell){
        switch (me.getFacedDirection()){
            case "WEST":
                // East is back
                if (matchActionToDestination(currentCell, "WEST") != -1) {
                    pathToTake.push("FIREWEST");
                }
                if (matchActionToDestination(currentCell, "NORTH") != -1) {
                    pathToTake.push("FIRENORTH");
                }
                if (matchActionToDestination(currentCell, "SOUTH") != -1) {
                    pathToTake.push("FIRESOUTH");
                }
                break;
            case "EAST":
                // West is back
                if (matchActionToDestination(currentCell, "EAST") != -1) {
                    pathToTake.push("FIREEAST");
                }
                if (matchActionToDestination(currentCell, "NORTH") != -1) {
                    pathToTake.push("FIRENORTH");
                }
                if (matchActionToDestination(currentCell, "SOUTH") != -1) {
                    pathToTake.push("FIRESOUTH");
                }
                break;
            case "NORTH":
                // South is back
                if (matchActionToDestination(currentCell, "WEST") != -1) {
                    pathToTake.push("FIREWEST");
                }
                if (matchActionToDestination(currentCell, "EAST") != -1) {
                    pathToTake.push("FIREEAST");
                }
                if (matchActionToDestination(currentCell, "NORTH") != -1) {
                    pathToTake.push("FIRENORTH");
                }
                break;
            case "SOUTH":
                // North is back
                if (matchActionToDestination(currentCell, "WEST") != -1) {
                    pathToTake.push("FIREWEST");
                }
                if (matchActionToDestination(currentCell, "EAST") != -1) {
                    pathToTake.push("FIREEAST");
                }
                if (matchActionToDestination(currentCell, "SOUTH") != -1) {
                    pathToTake.push("FIRESOUTH");
                }
                break;
            default:
                // There is no back
                if (matchActionToDestination(currentCell, "WEST") != -1) {
                    pathToTake.push("FIREWEST");
                }
                if (matchActionToDestination(currentCell, "EAST") != -1) {
                    pathToTake.push("FIREEAST");
                }
                if (matchActionToDestination(currentCell, "NORTH") != -1) {
                    pathToTake.push("FIRENORTH");
                }
                if (matchActionToDestination(currentCell, "SOUTH") != -1) {
                    pathToTake.push("FIRESOUTH");
                }
                break;
        }
    }
    
    /**
     * Find a random unvisited, possible direction
     * @param currentCell
     * @return 
     */
    public String getAvailableAction(Cell currentCell) {
        Random rand = new Random();
            
        ArrayList<String> bestActions = new ArrayList<>();
        ArrayList<String> actions = new ArrayList<>();
        actions.add("WEST");
        actions.add("EAST");
        actions.add("NORTH");
        actions.add("SOUTH");
           
        // Test each direction for a visited or unreachable destination
        ArrayList<String> actionsToRemove = new ArrayList<>();
        for (String action : actions) {

            int destination = matchActionToDestination(currentCell, action);
            if (destination == -1) {

                actionsToRemove.add(action);
            }
            else if (visitedCellKeys.contains(destination)) {

                actionsToRemove.add(action);
            }
            else if (bannedListOfCells.contains(destination)) {
                
                actionsToRemove.add(action);
            }
            else if (safeUnvisitedListOfCells.contains(destination)) {
                bestActions.add(action);
            }
        }
        
        // Remove untenable directions
        for (String actionToRemove : actionsToRemove) {
            
            actions.remove((actionToRemove));
        }
            
        // Allow a random direction if no direction is possible or unvisited
        if (actions.isEmpty()) {
            actions = new ArrayList<>();
            actions.add("WEST");
            actions.add("EAST");
            actions.add("NORTH");
            actions.add("SOUTH");
            
            // Restart the proces, but remove unreachable or banned cells
            actionsToRemove = new ArrayList<>();
            for (String action : actions) {

                int destination = matchActionToDestination(currentCell, action);
                if (destination == -1) {

                    actionsToRemove.add(action);
                }
                else if (bannedListOfCells.contains(destination)) {

                    actionsToRemove.add(action);
                }
            }

            // Remove untenable directions
            for (String actionToRemove : actionsToRemove) {

                actions.remove((actionToRemove));
            }
        }
        
        // If there are actions that are safe and unvisited, try them
        if (bestActions.size() > 0) {
            return bestActions.get(rand.nextInt(bestActions.size()));
        }
        // If no directions whatsoever are possible (banned, etc.)
        else if (actions.isEmpty()) {
            return "Halt";
        }
        // Else if possible pick a random non-backwards direction
        return actions.get(rand.nextInt(actions.size()));
        
    }
    
    /**
     * Similar to get availableAction, but visited directions are on the table automatically
     * @param currentCell
     * @return 
     */
    public String getRandomAction(Cell currentCell) {
        Random rand = new Random();
            
        ArrayList<String> bestActions = new ArrayList<>();
        ArrayList<String> actions = new ArrayList<>();
        actions.add("WEST");
        actions.add("EAST");
        actions.add("NORTH");
        actions.add("SOUTH");
           
        // Test each direction for a visited or unreachable destination
        ArrayList<String> actionsToRemove = new ArrayList<>();
        for (String action : actions) {

            int destination = matchActionToDestination(currentCell, action);
            if (destination == -1) {

                actionsToRemove.add(action);
            }
            else if (bannedListOfCells.contains(destination)) {
                
                actionsToRemove.add(action);
            }
            else if (safeUnvisitedListOfCells.contains(destination)) {
                bestActions.add(action);
            }
        }
        
        // Remove untenable directions
        for (String actionToRemove : actionsToRemove) {
            
            actions.remove((actionToRemove));
        }
        
        // If there are actions that are safe and unvisited, try them
        if (bestActions.size() > 0) {
            return bestActions.get(rand.nextInt(bestActions.size()));
        }
        // If no directions whatsoever are possible (banned, etc.)
        else if (actions.isEmpty()) {
            return "Halt";
        }
        // Else if possible pick a random non-backwards direction
        return actions.get(rand.nextInt(actions.size()));
        
    }
    
    /**
     * Add a cell to the places this explorer has seen
     * @param sourceCell 
     */
    public void updateVisitedList(Cell sourceCell){
        
        if(!visitedListOfCells.contains(sourceCell)){
            visitedListOfCells.add(sourceCell);    
            visitedCellKeys.add(sourceCell.getKey());
        }
        int unvisitedIndex = safeUnvisitedListOfCells.indexOf(sourceCell.getKey());
        if (unvisitedIndex != -1) {
            safeUnvisitedListOfCells.remove(unvisitedIndex);
        }
        
        timesVisitedEachCellArray[sourceCell.getKey()]++;
    }
    
    /**
     * Add an unvisited cell that had no smell or wind to the list of safe, unvisited cells this explorer has seen
     * @param sourceCell 
     */
    public void updateSafeUnvisitedList(Cell sourceCell){
        
        for(int i = 0; i < sourceCell.getConnectedCells().size(); i++){
        
            if(!safeUnvisitedListOfCells.contains(sourceCell.getConnectedCells().get(i)) && !visitedCellKeys.contains(sourceCell.getConnectedCells().get(i))){
                            
                    safeUnvisitedListOfCells.add(sourceCell.getConnectedCells().get(i));
            }
        }
    }
    
    /**
     * Turn the given action into a neighbor of the current fragment, if possible
     * @param sourceCell
     * @param action
     * @return 
     */
    protected int matchActionToDestination(Cell sourceCell, String action) {
        
        int currentKey = sourceCell.getKey();
        int returnDestination = -1;
        
        if (action.contains("FIRE") && !action.contains("AFTER")) {
            
            returnDestination = sourceCell.getKey();
        }
        else if (action.contains("WEST")) {
            
            for (int currentNeighbor : sourceCell.getConnectedCells()) {
                
                if (currentNeighbor == (currentKey-1)) {
                    returnDestination = currentNeighbor;
                    break;
                }
            }
        }
        else if (action.contains("EAST")) {
            
            for (int currentNeighbor : sourceCell.getConnectedCells()) { 
                
                if (currentNeighbor == (currentKey+1)) {
                    returnDestination = currentNeighbor;
                    break;
                }
            }
        }
        else if (action.contains("NORTH")) {
            
            for (int currentNeighbor : sourceCell.getConnectedCells()) {
                
                if (currentNeighbor < (currentKey-1)) {
                    returnDestination = currentNeighbor;
                    break;
                }
            }
        }
        else if (action.contains("SOUTH")) {
            
            for (int currentNeighbor : sourceCell.getConnectedCells()) {
                
                if (currentNeighbor > (currentKey+1)) {
                    returnDestination = currentNeighbor;
                    break;
                }
            }
        }
        
        return returnDestination;
    }
}
