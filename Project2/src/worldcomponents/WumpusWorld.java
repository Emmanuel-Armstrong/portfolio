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

import actorcomponents.KnowledgeFragment;
import actorcomponents.ReasoningInference;
import actors.Explorer;
import java.util.ArrayList;
import java.util.Random;
import java.util.logging.Level;
import java.util.logging.Logger;


/**
 *
 * @author nwmoore
 * @author matthewrohrlach
 */
public class WumpusWorld {

    Cell[] cells;
    Cell currentCell;
    Cell previousCell;
    int goldCellKey;
    Explorer explorer;
    int sideDimension;
    int pitProbability;
    int obstacleProbability;
    int wumpusProbability;
    
    int cellsExploredCounter;
    int killedByPitCounter;
    int killedByWumpusCounter;
    int wumpusCounter;
    int thisIterationCount;
    boolean maxActionsReached;
    
    ArrayList<Integer> deadWumpusCells;
    ArrayList<KnowledgeFragment> knowledgeToPass;
    
    boolean worldIsSolvable;
    
    boolean debug = false;
    boolean resurrectTheDead = true;

    /**
     * Regular constructor
     * @param explorerIn
     * @param sideDimensionIn
     * @param pitProbabilityIn
     * @param obstacleProbabilityIn
     * @param wumpusProbabilityIn 
     */
    public WumpusWorld(Explorer explorerIn, int sideDimensionIn, int pitProbabilityIn, int obstacleProbabilityIn, int wumpusProbabilityIn) {
          
        this.explorer = explorerIn;
        this.explorer.setKnowledge(knowledgeToPass);
        this.sideDimension = sideDimensionIn;
        this.pitProbability = pitProbabilityIn;
        this.obstacleProbability = obstacleProbabilityIn;
        this.wumpusProbability = wumpusProbabilityIn;
        this.worldIsSolvable = false;
        
        cells = new Cell[sideDimension * sideDimension];
        
        generateWorld();
        initializeKnowledge();
        this.explorer.setKnowledge(knowledgeToPass);
        
    }
    
    /**
     * Constructor without passing an explorer (used predominantly)
     * @param decisionMaker
     * @param sideDimensionIn
     * @param pitProbabilityIn
     * @param obstacleProbabilityIn
     * @param wumpusProbabilityIn 
     */
    public WumpusWorld(String decisionMaker, int sideDimensionIn, int pitProbabilityIn, int obstacleProbabilityIn, int wumpusProbabilityIn) {
          
        this.explorer = new Explorer(decisionMaker);
        this.sideDimension = sideDimensionIn;
        this.pitProbability = pitProbabilityIn;
        this.obstacleProbability = obstacleProbabilityIn;
        this.wumpusProbability = wumpusProbabilityIn;
        this.worldIsSolvable = false;
        
        cells = new Cell[sideDimension * sideDimension];
        
        generateWorld();
        initializeKnowledge();
        this.explorer.setKnowledge(knowledgeToPass);
    }
    
    
    
    /**
     * Build or rebuild all cells of the world
     */
    private void generateWorld() {
        
        wumpusCounter = 0;
        deadWumpusCells = new ArrayList<>();
        for (int i = 0; i < cells.length; i++) {
            
            // Get an assignment for each type of hazard
            boolean pitToAssign = false;
            boolean wumpusToAssign = false;
            boolean obstacleToAssign = false;
            
            Random rand = new Random();
            switch (rand.nextInt(3)) {
                // Assign a hazard to a cell, adjust probabilites such that the probability given represents the actual chance
                // that any given cell has that hazard.
                case 0:
                    pitToAssign = assignOnProbability((int)Math.round((double)pitProbability*(10/3)));
                    break;
                case 1:
                    obstacleToAssign = assignOnProbability((int)Math.round((double)obstacleProbability*(10/3)));
                    break;
                case 2:
                    wumpusToAssign = assignOnProbability((int)Math.round((double)wumpusProbability*(10/3)));
                    if (wumpusToAssign) {
                        wumpusCounter++;
                    }
                    break;
                default:
                    System.out.println("You shouldn't be here.");
            }
            
            //genrate cell with pit or wumpus or obsatcle, empty if none generated
            cells[i] = new Cell(i, pitToAssign, obstacleToAssign, wumpusToAssign);
            buildConnectionKeyList(i);
        
        }
        // Put gold in one of the empty cells
        // Start by adding every empty cell to a list
        ArrayList<Integer> emptyCells;
        emptyCells = new ArrayList<>();
        for (Cell cell : cells) {
            // If the cell is empty and not the southwest corner
            if (cell.getIsEmpty() && 
                    (cell.getKey() != ((sideDimension*sideDimension)-sideDimension))) {
                emptyCells.add(cell.getKey());
            }
        }
        
        // Gold and explorer placement
        Random rand = new Random();
        int goldEmptyIndex;
        int goldCellIndex;
        int explorerEmptyIndex;
        int explorerCellIndex;
        
        if (!emptyCells.isEmpty()) {
            
            // Place the gold in a random known-empty cell
            goldEmptyIndex = rand.nextInt(emptyCells.size());
            goldCellIndex = emptyCells.get(goldEmptyIndex);
            emptyCells.remove(goldEmptyIndex);
        }
        else {
            
            // Place the gold in a random cell and clear it out
            
            // While goldCellIndex = the southwest corner
            goldCellIndex = ((sideDimension*sideDimension)-sideDimension);
            while (goldCellIndex == ((sideDimension*sideDimension)-sideDimension)) {
                goldCellIndex = rand.nextInt(cells.length);
            }
            if (cells[goldCellIndex].getHasWumpus()) {
                wumpusCounter--;
            }
            cells[goldCellIndex].makeEmpty();
            
        }
        
        // Finish placing the gold
        cells[goldCellIndex].setGold(true);
        goldCellKey = cells[goldCellIndex].getKey();
        
        // (old method): Place the explorer randomly
        
//        if (!emptyCells.isEmpty()) {
//            
//            // Place explorer in a known empty cell
//            explorerEmptyIndex = rand.nextInt(emptyCells.size());
//            explorerCellIndex = emptyCells.get(explorerEmptyIndex);
//        }
//        else {
//            
//            // Place explorer somewhere random that does not have gold
//            explorerCellIndex = goldCellIndex;
//            while (explorerCellIndex == goldCellIndex) {
//                explorerCellIndex = rand.nextInt(cells.length);
//            }
//            if (cells[explorerCellIndex].getHasWumpus()) {
//                wumpusCounter--;
//            }
//            cells[explorerCellIndex].makeEmpty();
//        }
        
        // (new method): Place explorer in southwest corner of world
        
        explorerCellIndex = ((sideDimension*sideDimension)-sideDimension);
        if (cells[explorerCellIndex].getHasWumpus()) {
            wumpusCounter--;
        }
        cells[explorerCellIndex].makeEmpty();
        
        // Set explorer's position and give them the correct number of arrows
        explorer.setOriginalCellKey(cells[explorerCellIndex].getKey());
        explorer.setCurrentCellKey(explorer.getOriginalCellKey());
        currentCell = cells[explorerCellIndex];
        
        explorer.setArrowNum(wumpusCounter);
        
        worldIsSolvable = isSolvable(goldCellIndex);
        int i = 0;
    }
    
    /**
     * Loops decision making until gold is found or explorer is dead, then returns the points
     * @param maxIterations
     * @return 
     */
    public int runWorld(int maxIterations) {
        
        cellsExploredCounter = 0;
        killedByPitCounter = 0;
        killedByWumpusCounter = 0;
        maxActionsReached = false;
        thisIterationCount = 0;
        ArrayList<Cell> uniqueCells = new ArrayList<>();
        
        System.out.println("Explorer is in cell " + currentCell.getKey() + ", facing " + explorer.getFacedDirection() +".");
        
        // Run decision-making until there are no more actions to make or the explorer is dead at the end of an action (not resurrected)
        while(explorer.getStatus().equals("ALIVE") && thisIterationCount < maxIterations) {
            
            // If this cell was previously-unvisited, increment the count of cells explored
            if (!uniqueCells.contains(currentCell)) {
                uniqueCells.add(currentCell);
                cellsExploredCounter++;
            }
            
            previousCell = currentCell;
            // Make the explorer take action
            if (takeExplorerAction(explorer.makeDecision(currentCell, buildSensesReport()))) {
                
            }
            else {
                System.out.println("Explorer bumps their head on a wall.");
            }
            
            // Adjust point total to reflect the action (rotations automatically subtract points)
            explorer.changePoints(-1);
            
            // Test if explorer has been killed or found gold
            
            System.out.println("Explorer is in cell " + currentCell.getKey() + " with " 
                    + explorer.getPoints() + " points, facing " + explorer.getFacedDirection() + ".");
            
            // If debug mode is enabled, test the assumptions made by the explorer for judgment errors
            if (debug) {

                this.testKnowledge();
            }
            
            if (currentCell.getHasWumpus()) {
                
                // Explorer dies and loses 1000 points
                explorer.dies();
                this.killedByWumpusCounter++;
                explorer.changePoints(-1000);
                System.out.println("\n" + explorer.name + " attempted to fight a wumpus with their bare fists in cell " 
                        + currentCell.getKey() + " with " +
                        explorer.getPoints() + " points. It didn't go well.");
                if (!worldIsSolvable) {
                    System.out.println("Resistance is futile.");
                }
                else {
                    System.out.println("Maybe they could've done things differently?");
                }
                
                // Explorer comes back to life or world goes unsolved
                if (resurrectTheDead) {
                    System.out.println(explorer.name + " laughs at the pitiful Wumpus, and walks casually away from its feeble attacks!");
                    explorer.resurrects();
                    
                    // Let the explorer see the death cell's senses, ignore it's decision, then reset it to the last visited cell
                    explorer.makeDecision(currentCell, buildSensesReport());
                    explorer.setCurrentCellKey(previousCell.getKey());
                    currentCell = previousCell;
                    
                    System.out.println("Explorer is in cell " + currentCell.getKey() + " with " 
                    + explorer.getPoints() + " points, facing " + explorer.getFacedDirection() + ".");
                }
                else {
                    return explorer.getPoints();
                }
            }
            else if (currentCell.getHasPit()) {
                
                // Explorer dies and loses 1000 points
                explorer.dies();
                this.killedByPitCounter++;
                explorer.changePoints(-1000);
                System.out.println("\n" + explorer.name + " fell into a bottomless pit in cell " 
                        + currentCell.getKey() + " with " +
                        explorer.getPoints() + " points.");
                if (!worldIsSolvable) {
                    System.out.println("It was inevitable.");
                }
                else {
                    System.out.println("Maybe they could've done things differently?");
                }
                
                // Explorer goes back to life or world goes unsolved
                if (resurrectTheDead) {
                    System.out.println(explorer.name + " flys out of the pit, cackling like a maniac! They're untouchable!");
                    explorer.resurrects();
                    
                    // Let the explorer see the death cell's senses, ignore it's decision, then reset it to the last visited cell
                    explorer.makeDecision(currentCell, buildSensesReport());
                    explorer.setCurrentCellKey(previousCell.getKey());
                    currentCell = previousCell;
                    
                    System.out.println("Explorer is in cell " + currentCell.getKey() + " with " 
                    + explorer.getPoints() + " points, facing " + explorer.getFacedDirection() + ".");
                }
                else {
                    return explorer.getPoints();
                }
            }
            // Explorer has grabbed the gold successfully
            else if (explorer.getStatus().equals("RICH")) {
                
                System.out.println("\n" + explorer.name + " found the gold, and now has " +
                        explorer.getPoints() + " points.");
                if (!worldIsSolvable) {
                    System.out.println("What? This world isn't even solvable!\n");
                }
                else {
                    System.out.println(explorer.name + " teleports from the world, riches in tow.\n");
                }
                
                return explorer.getPoints();
            }
            
            thisIterationCount++;
        }
        
        // The explorer doesn't know if the world is solvable. This is for testing purposes.
        if (!worldIsSolvable) {
            System.out.println("Max number of actions reached! The world seemed impossible, anyway.\n");
        }
        else {
            System.out.println("Max number of actions reached! Maybe the explorer could've had better luck?\n");
        }
        maxActionsReached = true;
        return explorer.getPoints();
    }
    
    /**
     * Takes an action and enacts it on the world
     * @param actionToTake
     * @return 
     */
    protected boolean takeExplorerAction(String actionToTake) {
        
        if (actionToTake.contains("AFTER") || actionToTake.equals("EAST")) {
            System.out.println("\n" + explorer.name + " made an " +
                actionToTake + " movement.");
        }
        else {
            System.out.println("\n" + explorer.name + " made a " +
                actionToTake + " movement.");
        }
        
        
        // Establish what "back" means
        String backDirection;
        int nextCellOffset;
        switch (explorer.getFacedDirection()) {
            case "WEST":
                // Back is east
                backDirection = "EAST";
                nextCellOffset = 1;
                break;
            case "EAST":
                // Back is west
                backDirection = "WEST";
                nextCellOffset = -1;
                break;
            case "NORTH":
                // Back is south
                backDirection = "SOUTH";
                nextCellOffset = sideDimension;
                break;
            default:
                // Back is north
                backDirection = "NORTH";
                nextCellOffset = -sideDimension;
                break;
        }
        
        // Attempt the action
        // ============ Simple moves ======================
        // Move west if possible
        switch (actionToTake) {
            case "WEST":
                explorer.rotate("WEST");
                if (isDirectionPossible(currentCell, "WEST")) {
                    currentCell = cells[explorer.getCurrentCellKey()-1];
                    explorer.setCurrentCellKey(currentCell.getKey());
                    return true;
                }
                else {
                    return false;
                }
            case "EAST":
                explorer.rotate("EAST");
                if (isDirectionPossible(currentCell, "EAST")) {
                    currentCell = cells[explorer.getCurrentCellKey()+1];
                    explorer.setCurrentCellKey(currentCell.getKey());
                    return true;
                }
                else {
                    return false;
                }
            case "NORTH":
                explorer.rotate("NORTH");
                if (isDirectionPossible(currentCell, "NORTH")) {
                    currentCell = cells[explorer.getCurrentCellKey()-sideDimension];
                    explorer.setCurrentCellKey(currentCell.getKey());
                    return true;
                }
                else {
                    return false;
                }
            case "SOUTH":
                explorer.rotate("SOUTH");
                if (isDirectionPossible(currentCell, "SOUTH")) {
                    currentCell = cells[explorer.getCurrentCellKey()+sideDimension];
                    explorer.setCurrentCellKey(currentCell.getKey());
                    return true;
                }
                else {
                    return false;
                }
            case "FIRE":
                handleArrowFired(explorer.getFacedDirection());
                return true;
            case "FIREWEST":
                explorer.rotate("WEST");
                handleArrowFired("WEST");
                return true;
            case "FIREEAST":
                explorer.rotate("EAST");
                handleArrowFired("EAST");
                return true;
            case "FIRENORTH":
                explorer.rotate("NORTH");
                handleArrowFired("NORTH");
                return true;
            case "FIRESOUTH":
                explorer.rotate("SOUTH");
                handleArrowFired("SOUTH");
                return true;
            case "AFTERFIREWEST":
                explorer.rotate("WEST");
                handleArrowFired("WEST");
                if (isDirectionPossible(currentCell, "WEST")) {
                    
                    currentCell = cells[explorer.getCurrentCellKey()-1];
                    explorer.setCurrentCellKey(currentCell.getKey());
                    return true;
                }
                else {
                    return false;
                }
            case "AFTERFIREEAST":
                explorer.rotate("EAST");
                handleArrowFired("EAST");
                if (isDirectionPossible(currentCell, "EAST")) {
                    
                    currentCell = cells[explorer.getCurrentCellKey()+1];
                    explorer.setCurrentCellKey(currentCell.getKey());
                    return true;
                }
                else {
                    return false;
                }
            case "AFTERFIRENORTH":
                explorer.rotate("NORTH");
                handleArrowFired("NORTH");
                if (isDirectionPossible(currentCell, "NORTH")) {
                    
                    currentCell = cells[explorer.getCurrentCellKey()-sideDimension];
                    explorer.setCurrentCellKey(currentCell.getKey());
                    return true;
                }
                else {
                    return false;
                }
            case "AFTERFIRESOUTH":
                explorer.rotate("SOUTH");
                handleArrowFired("SOUTH");
                if (isDirectionPossible(currentCell, "SOUTH")) {
                    
                    currentCell = cells[explorer.getCurrentCellKey()+sideDimension];
                    explorer.setCurrentCellKey(currentCell.getKey());
                    return true;
                }
                else {
                    return false;
                }
            case "BACK":
                explorer.rotate(backDirection);
                if (isDirectionPossible(currentCell, backDirection)) {
                    currentCell = cells[explorer.getCurrentCellKey()+nextCellOffset];
                    explorer.setCurrentCellKey(currentCell.getKey());
                    return true;
                }
                else {
                    return false;
                }
            case "FIREBACK":
                explorer.rotate(backDirection);
                handleArrowFired(backDirection);
                break;
            case "AFTERFIREBACK":
                explorer.rotate(backDirection);
                handleArrowFired(backDirection);
                if (isDirectionPossible(currentCell, backDirection)) {
                    currentCell = cells[explorer.getCurrentCellKey()+nextCellOffset];
                    explorer.setCurrentCellKey(currentCell.getKey());
                    return true;
                }
                else {
                    return false;
                }
            case "GRAB":
                // Offset default movement cost, as there is no movement
                explorer.changePoints(1);
                // If the gold is here
                if (currentCell.getHasGold()) {
                    explorer.setStatus("RICH");
                    explorer.changePoints(1000);
                    return true;
                }
                // Else the explorer is poorly-programmed
                else {
                    System.out.println("Explorer tried to grab the gold, but there is no gold here!");
                    return true;
                }
            case "HALT":
                // Offset default movement cost, as there is no movement
                explorer.changePoints(1);
                return true;
            default:
                break;
        }
        
        return false;
    }
    
    /**
     * Tests a direction for the ability to move in that direction
     * @param sourceCell
     * @param direction
     * @return 
     */
    public boolean isDirectionPossible(Cell sourceCell, String direction) {
        
        int currentCellKey = sourceCell.getKey();
        ArrayList<Integer> currentConnectedCells = sourceCell.getConnectedCells();
        
        switch (direction) {
            case "WEST":
                // West is possible?
                if (currentConnectedCells.contains(currentCellKey-1)) {
                    // Return true if direction does not have obstacle
                    return (!cells[currentCellKey-1].getHasObstacle());
                }   break;
            case "EAST":
                // East is possible?
                if (currentConnectedCells.contains(currentCellKey+1)) {
                    // Return true if direction does not have obstacle
                    return (!cells[currentCellKey+1].getHasObstacle());
                }   break;
            case "NORTH":
                // North is possible?
                if (currentConnectedCells.contains(currentCellKey-sideDimension)) {
                    // Return true if direction does not have obstacle
                    return (!cells[currentCellKey-sideDimension].getHasObstacle());
                }   break;
            case "SOUTH":
                // South is possible?
                if (currentConnectedCells.contains(currentCellKey+sideDimension)) {
                    // Return true if direction does not have obstacle
                    return (!cells[currentCellKey+sideDimension].getHasObstacle());
                }   break;
            default:
                break;
        }
        
        return false;
    }
    
    /**
     * Handles an arrow's traversal in a direction
     * @param directionFired
     * @return 
     */
    protected boolean handleArrowFired(String directionFired) {
        if (explorer.getNumArrows() > 0) {
            
            // Decrement explorer's arrow count
            explorer.arrowFired();
            
            Cell tempCell = currentCell;
            int nextCellOffset;
            
            // Establish the increment in key to reach the next cell in that direction
            switch (directionFired) {
                case "WEST":
                    // Next cell is west
                    nextCellOffset = -1;
                    break;
                case "EAST":
                    // Next cell is east
                    nextCellOffset = 1;
                    break;
                case "NORTH":
                    // Next cell is north
                    nextCellOffset = -sideDimension;
                    break;
                default:
                    // Next cell is south
                    nextCellOffset = sideDimension;
                    break;
            }
            
            boolean alwaysFalse = false;
            // Run through every cell in a direction until no cell can be reached
            while (!alwaysFalse) {
                
                // If the next cell in that direction is not blocked
                if (isDirectionPossible (tempCell, directionFired)) {
                    // Send arrow through cells until a wumpus is hit (THE EXPLORER CANNOT SEE THIS)
                    tempCell = cells[tempCell.getKey()+nextCellOffset];
                    System.out.println("Arrow flies into cell " + tempCell.getKey() + "!");
                    if (tempCell.getHasWumpus()) {

                        // Note the dead wumpus location, kill the wumpus, report the scream
                        deadWumpusCells.add(tempCell.getKey());
                        tempCell.killWumpus();
                        explorer.hearScream();
                        System.out.println("A wumpus roars as it dies!");
                        return true;
                    }
                }
                
                // Otherwise report the failure (no scream heard)
                else {
                    System.out.println("The arrow strikes something hard with a resounding clang.");
                    return false;
                }
            }
        }
        
        // There are no arrows, report failure
        System.out.println("The explorer reaches for an arrow, but finds none.");
        return false;
    }
    
    /**
     * Flips a coin with a certain probability, returns the outcome
     * @param probabilityPercent
     * @return 
     */
    protected boolean assignOnProbability(int probabilityPercent) {
        Random rand = new Random();
        return (rand.nextInt(100) < probabilityPercent);
    }
    
    /**
     * Creates the connection list of each cell in the world
     * @param sourceKey 
     */
    protected void buildConnectionKeyList(int sourceKey) {
        
        // Handle edge cases with boolean values
        boolean findRightConnection = true;
        boolean findLeftConnection = true;
        if ((sourceKey % sideDimension) == 0 || (sourceKey == 0)) {
            findLeftConnection = false;
        }
        if ((sourceKey + 1) % sideDimension == 0) {
            findRightConnection = false;
        }
        
        // Add a left connection if not on edge of grid
        if (findLeftConnection) {
            if ((sourceKey - 1) >= 0) {
                cells[sourceKey].addConnectedCell(sourceKey-1);
            }
        }
        
        // Add a right connection if not on edge of grid
        if (findRightConnection) {
            if ((sourceKey + 1) < (sideDimension * sideDimension)) {
                cells[sourceKey].addConnectedCell(sourceKey+1);
            }
        }
        
        // Add a north connection if it wouldn't be out of bounds
        if ((sourceKey - sideDimension) >= 0) {
            cells[sourceKey].addConnectedCell(sourceKey-sideDimension);
        }
        
        // Add a south connection if it wouldn't be out of bounds
        if ((sourceKey + sideDimension) < (sideDimension * sideDimension)) {
            cells[sourceKey].addConnectedCell(sourceKey+sideDimension);
        }
    }
    
    /**
     * For the current cell:
     * reports 0 for nothing, 1 for just smell, 2 for just wind, 3 for both
     * @return 
     */
    public int buildSensesReport() {
        currentCell = cells[(explorer.getCurrentCellKey())];
        int sensesReport = 0;
        boolean hasSmell = false;
        boolean hasWind = false;
        
        for (Integer connectedCell : currentCell.getConnectedCells()) {
            if (cells[connectedCell].getHasWumpus()) {
                hasSmell = true;
            }
            if (cells[connectedCell].getHasPit()) {
                hasWind = true;
            }
        }
        
        // Only smell
        if (hasSmell) {
            sensesReport = 1;
        }
        // Only wind
        if (hasWind) {
            sensesReport = 2;
        }
        // Smell and wind
        if (hasSmell && hasWind) {
            sensesReport = 3;
        }
        
        System.out.println("Smell: " + hasSmell + " , Wind: " + hasWind + ", Report: " + sensesReport);
        
        return sensesReport;
    }
    
    /**
     * Resurrects all wumpuses, creates fresh explorer
     */
    public void resetWorld() {
        
        // Find all dead wumpuses
        for (int deadWumpusIndex = 0; deadWumpusIndex < deadWumpusCells.size(); deadWumpusIndex++) {
            
            // Bring all dead wumpuses back to life
            cells[deadWumpusCells.get(deadWumpusIndex)].resurrectWumpus();
        }
        
        // Reset explorer position
        explorer.setCurrentCellKey(explorer.getOriginalCellKey());
        currentCell = cells[explorer.getOriginalCellKey()];
        explorer.setFacedDirection(explorer.getOriginalFacedDirection());
        
        // Reset explorer arrow number
        explorer.setArrowNum(wumpusCounter);
        
        // Change explorer name
        explorer.findRandomName();
        
        // Reset explorer points
        explorer.clearPoints();
        
        initializeKnowledge();
        explorer.setKnowledge(knowledgeToPass);
        
        // Reset inference classes
        explorer.clearInferences();
        
        // Resurrect explorer
        explorer.setStatus("ALIVE");
        
    }
    
    /**
     * Initialize the blank list of knowledge fragments.
     * This knowledge is not used by the world, but by the inference classes.
     */
    private void initializeKnowledge() {
        
        knowledgeToPass = new ArrayList<>();
        ArrayList<Integer> connectedKeys;
        KnowledgeFragment connectedFragment;
        
        for (int i = 0; i < (sideDimension*sideDimension); i++) {
            
            knowledgeToPass.add(i, new KnowledgeFragment(i));
        }
        
        for (int i = 0; i < knowledgeToPass.size(); i++) {
            connectedKeys = cells[i].getConnectedCells();
            
            for (int j = 0; j < connectedKeys.size(); j++) {
                connectedFragment = knowledgeToPass.get(connectedKeys.get(j));
                knowledgeToPass.get(i).addConnectedFragment(connectedFragment);
                
            }
        }
    }
    
    /**
     * Replace explorer with a given explorer object
     * @param explorerIn 
     */
    public void setExplorer(Explorer explorerIn) {
        this.explorer = explorerIn;
    }
    
    /**
     * Return true if the world is solvable (TESTING PURPOSES ONLY)
     * @param goldIndex
     * @return 
     */
    public boolean isSolvable(int goldIndex) {
        
        // Make a standalone reasoning class for the build path method
        ReasoningInference testReasoning = new ReasoningInference();
        
        // Make a fake knowledge set
        ArrayList<KnowledgeFragment> testKnowledge = new ArrayList<>();
        ArrayList<Integer> connectedKeys;
        KnowledgeFragment connectedFragment;
        
        for (int i = 0; i < (sideDimension*sideDimension); i++) {
            
            testKnowledge.add(i, new KnowledgeFragment(i));
            testKnowledge.get(i).setVerboseMode(false);
        }
        
        for (int i = 0; i < testKnowledge.size(); i++) {
            connectedKeys = cells[i].getConnectedCells();
            
            for (int j = 0; j < connectedKeys.size(); j++) {
                connectedFragment = testKnowledge.get(connectedKeys.get(j));
                testKnowledge.get(i).addConnectedFragment(connectedFragment);
                
            }
        }
        
        // Fill out the knowledge bank with the powers of telepathy
        for (KnowledgeFragment fragment : testKnowledge) {
            
            if (cells[fragment.getKey()].getHasPit()) {
                fragment.setPitFound(true);
            }
            if (cells[fragment.getKey()].getHasObstacle()) {
                fragment.setObstacleFound(true);
            }
            if (cells[fragment.getKey()].getIsEmpty() || cells[fragment.getKey()].getHasWumpus()) {
                fragment.setSafe();
                fragment.setVisited();
            }
        }
        
        // Replace the uninitialized knowledge of testReasoning with testKnowledge
        testReasoning.setKnowledge(testKnowledge);
        testReasoning.updateSideDimension();
        
        // Update the visited list of testReasoning
        testReasoning.updateEntireVisitedList();
        
        // Make decisions as if the Explorer were at the current cell
        testReasoning.setCurrentFragment(testKnowledge.get(currentCell.getKey()));
        
        // Test to see if a path to the gold is possible
        return testReasoning.buildPathToCell(testKnowledge.get(goldIndex));
    }
    
    /**
     * Test the assumptions made about every cell for errors in logic (TESTING PURPOSES ONLY)
     */
    public void testKnowledge() {
        
        KnowledgeFragment thisFragment;
        Cell thisCell;
        boolean pause = false;
        knowledgeToPass = explorer.getKnowledge();
        for (int i = 0; i < knowledgeToPass.size(); i++) {
            
            thisFragment = knowledgeToPass.get(i);
            thisCell = cells[i];
            
            if (thisFragment.isWumpusFound() && (!thisCell.getHasOriginalWumpus())) {
                System.err.println("False positive Wumpus in Fragment " + thisFragment.getKey());
                pause = true;
            }
            if (thisFragment.isPitFound() && !thisCell.getHasPit()) {
                System.err.println("False positive Pit in Fragment " + thisFragment.getKey());
                pause = true;
            }
            if (thisFragment.isObstacleFound() && !thisCell.getHasObstacle()) {
                System.err.println("False positive Obstacle in Fragment " + thisFragment.getKey());
                pause = true;
            }
            if (!thisFragment.isWumpusFound() && thisCell.getHasWumpus() && thisFragment.isWumpusSet()) {
                System.err.println("False negative Wumpus in Fragment " + thisFragment.getKey());
                pause = true;
            }
            if (!thisFragment.isPitFound() && thisCell.getHasPit() && thisFragment.isPitSet()) {
                System.err.println("False negative Pit in Fragment " + thisFragment.getKey());
                pause = true;
            }
            if (!thisFragment.isObstacleFound() && thisCell.getHasObstacle() && thisFragment.isObstacleSet()) {
                System.err.println("False negative Obstacle in Fragment " + thisFragment.getKey());
                pause = true;
            }
        }
        
        if (pause) {
                try {
                    Thread.sleep(5000);
                } catch (InterruptedException ex) {
                    Logger.getLogger(WumpusWorld.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
    }
    
    /**
     * Return whether or not this explorer exhausted their actions and failed
     * @return 
     */
    public boolean isMaxActionsReached() {
        
        return this.maxActionsReached;
    }
    
    /**
     * Gets the cell that should contain the explorer
     * @return 
     */
    public Cell getCurrentCell() {
        
        return this.currentCell;
    }
    
    /**
     * Return the location of the gold in this world
     * @return 
     */
    public int getGoldIndex() {
        
        return this.goldCellKey;
    }
    
    /**
     * Gets the explorer's number of points
     * @return 
     */
    public int getExplorerPoints() {
        
        return this.explorer.getPoints();
    }
    
    /**
     * Return the iteration that runWorld is currently on
     * @return 
     */
    public int getIterationCount() {
        
        return this.thisIterationCount;
    }
    
    /**
     * Return a reference to the current explorer
     * @return 
     */
    public Explorer getExplorerRef() {
        
        return this.explorer;
    }
    
    /**
     * Return the status of the current explorer
     * @return 
     */
    public String getExplorerStatus() {
        
        return this.explorer.getStatus();
    }
    
    /**
     * Return the number of unique cells explored by current explorer
     * @return 
     */
    public int getCellsExploredCounter() {
        
        return this.cellsExploredCounter;
    }
    
    /**
     * Return the number of times a wumpus has killed the current explorer
     * @return 
     */
    public int getKilledByWumpusCounter() {
        
        return this.killedByWumpusCounter;
    }
    
    /**
     * Return the number of times a pit has killed the current explorer
     * @return 
     */
    public int getKilledByPitCounter() {
        
        return this.killedByPitCounter;
    }
    
    /**
     * Return the number of wumpuses killed by the current explorer
     * @return 
     */
    public int getWumpusesKilledCounter() {
        
        return this.explorer.getScreamsHeard();
    }
    
    /**
     * Return the orginal number of wumpuses in this world
     * @return 
     */
    public int getWumpusAmount() {
        
        return this.wumpusCounter;
    }
    
    /**
     * Prints the keys of every cell
     */
    public void printCellKeys() {
        for (Cell cell : cells) {
            System.out.println(cell.getKey());
        }
    }
    
    /**
     * Prints the attributes and flags of every cell
     */
    public void printCells() {
        for (Cell cell : cells) {
            System.out.println("Cell "+cell.getKey()+", hasWumpus: "+cell.getHasWumpus()+","
                    + " hasPit: "+cell.getHasPit()+", hasObstacle: "+cell.getHasObstacle()+","
                    + " isEmpty: "+cell.getIsEmpty()+", hasGold: "+cell.getHasGold());
        }
        System.out.println("\n");
    }
}
