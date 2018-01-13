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
package client;

// TO-DO: Make these more precise when project is completed
import java.text.DecimalFormat;
import ui.GUI;
import worldcomponents.WumpusWorld;

/**
 * Main program driver for GUI creation
 * @author Matthew Rohrlach
 */
public class Main {
    
    // Global variables
    
    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        new Main().go();
    }
    
    /**
     * A non-static instance of main
     */
    public void go(){
        
        // ================
        
        // Call GUI
        GUI gui = new GUI();
        gui.buildMainGUI();
        
        // ===Test Code====
        
//        String decisionMaker = "REACTIONARY";
//        int sideDimension = 10;
//        
//        int wumpusProbability = 10;
//        int pitProbability = 10;
//        int obstacleProbability = 10;
//        
//        int numExplorers = 100;
//        int numActions = 1000;
//        
//        boolean ensureSolvability = true;
//        
//        WumpusWorld world = new WumpusWorld(decisionMaker, sideDimension, wumpusProbability, pitProbability, obstacleProbability);
//       
//        // Point-tracking
//        int thisPoints;
//        int totalPoints = 0;
//        double averagePoints;
//        
//        // Gold-finding tracking
//        int totalSolves = 0;
//        
//        // Arrow tracking
//        int thisOriginalNumberOfArrows;
//        int totalArrowsFired = 0;
//        int totalArrows = 0;
//        int totalScreamsHeard = 0;
//        int totalWumpuses = 0;
//        double averageWumpusesKilled;
//        double averageWumpusesPerWorld;
//        double averageArrowAccuracy;
//        double averageArrowsRemaining;
//        
//        // Death tracker
//        int totalWumpusDeaths = 0;
//        int totalPitDeaths = 0;
//        int totalMaxActionsReached = 0;
//        double averageWumpusDeaths;
//        double averagePitDeaths;
//        double averageDeaths;
//        
//        // Cells explored tracker
//        int totalCellsExplored = 0;
//        double averageCellsExplored;
//        
//        int explorerCount = 0;
//        while (explorerCount < numExplorers) {
//            if (ensureSolvability) {
//                while (!world.isSolvable(world.getGoldIndex())) {
//                    world = new WumpusWorld(decisionMaker, sideDimension, wumpusProbability, pitProbability, obstacleProbability);
//                }
//            }
//            world.printCells();
//            world.runWorld(numActions);
//
//            // Metrics tracking
//            thisPoints = world.getExplorerPoints();
//            if (world.getExplorerStatus().equals("RICH")) {
//                totalSolves++;
//            }
//            totalPoints += thisPoints;
//            thisOriginalNumberOfArrows = world.getExplorerRef().getOriginalNumArrows();
//            totalArrows += thisOriginalNumberOfArrows;
//            totalArrowsFired += (thisOriginalNumberOfArrows - world.getExplorerRef().getNumArrows());
//            totalScreamsHeard += world.getWumpusesKilledCounter();
//            totalWumpuses += world.getWumpusAmount();
//            totalWumpusDeaths += world.getKilledByWumpusCounter();
//            totalPitDeaths += world.getKilledByPitCounter();
//            if (world.isMaxActionsReached()){
//                totalMaxActionsReached++;
//            }
//            totalCellsExplored += world.getCellsExploredCounter();
//
//            // Restart world
//            world = new WumpusWorld(decisionMaker, sideDimension, wumpusProbability, pitProbability, obstacleProbability);
//            explorerCount++;
//        }
//        averagePoints = (double) totalPoints / (double) explorerCount;
//        averageCellsExplored = (double) totalCellsExplored / (double) explorerCount;
//        averageWumpusDeaths = (double) totalWumpusDeaths / (double) explorerCount;
//        averagePitDeaths = (double) totalPitDeaths / (double) explorerCount;
//        averageDeaths = (double) (totalWumpusDeaths + totalPitDeaths) / (double) explorerCount;
//        averageWumpusesKilled = (double) (totalScreamsHeard) / (double) explorerCount;
//        averageWumpusesPerWorld = (double) totalWumpuses / (double) explorerCount;
//        averageArrowsRemaining = (double) (((double) (totalArrows - totalArrowsFired) / (double) totalArrows) * 100);
//        averageArrowAccuracy = (double) (((double) totalScreamsHeard / (double) totalArrowsFired) * 100);
//        
//        DecimalFormat decimalFormat = new DecimalFormat("#.##");
//        String averageArrowsRemainingString = decimalFormat.format(averageArrowsRemaining);
//        String averageArrowsAccuracyString = decimalFormat.format(averageArrowAccuracy);
//        
//        // Print metrics to GUI
//        System.out.println("\n====Results of "+decisionMaker+" "+sideDimension+" x "+sideDimension+
//                    " (max "+numActions+" actions)========" +
//                "\n   ====POINTS===================================" +
//                "\n         Total times gold found: " + totalSolves + "/" + explorerCount +
//                "\n         Average points per explorer: " + averagePoints +
//                "\n   ====EXPLORATION=============================" +
//                "\n         Total cells explored: " + totalCellsExplored +
//                "\n         Average cells explored per explorer: " + Math.round(averageCellsExplored)  + "/" + (sideDimension*sideDimension) +
//                "\n   ====DEATHS=================================" +
//                "\n         Total times died to Wumpus: " + totalWumpusDeaths +
//                "\n         Total times died to pit: " + totalPitDeaths +
//                "\n         Total times died: " + (totalWumpusDeaths + totalPitDeaths) +
//                "\n         Total times max actions reached: " + totalMaxActionsReached +
//                "\n         Average deaths to Wumpus per explorer: " + averageWumpusDeaths +
//                "\n         Average deaths to pit per explorer: " + averagePitDeaths +
//                "\n         Average deaths per explorer: " + averageDeaths +
//                "\n   ====ARROWS================================" +
//                "\n         Total Wumpuses: " + totalWumpuses +
//                "\n         Total Wumpuses killed: " + totalScreamsHeard + "/" + totalWumpuses +
//                "\n         Average Wumpuses per world: " + averageWumpusesPerWorld +
//                "\n         Average Wumpuses killed per explorer: " + averageWumpusesKilled +
//                "\n         Average arrows remaining per explorer: " + averageArrowsRemainingString + " percent" +
//                "\n         Average arrow accuracy per explorer: " + averageArrowsAccuracyString + " percent");
//        System.out.println("");
        
    }
}
