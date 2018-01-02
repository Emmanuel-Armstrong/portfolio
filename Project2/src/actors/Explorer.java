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
package actors;

import actorcomponents.KnowledgeFragment;
import actorcomponents.ReactionaryInference;
import actorcomponents.ReasoningInference;
import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Random;
import java.util.Scanner;
import worldcomponents.Cell;

/**
 *
 * @author matthewrohrlach
 */
public class Explorer {
    
    public String name;
    
    protected int numArrows;
    protected int originalNumArrows;
    protected int screamsHeard;
    protected int points;
    protected int currentCellKey;
    protected int originalCellKey;
    protected int sideDimension;
    protected String facedDirection;
    protected String originalFacedDirection;
    protected String status;
    protected int deathCount;
    protected String decisionMaker;
    
    protected ArrayList<KnowledgeFragment> currentKnowledge;
    protected ReasoningInference reasonInference;
    protected ReactionaryInference reactionInference;
    
    /**
     * Regular constructor
     * @param decisionMakerIn 
     * @param currentKnowledgeIn 
     */
    public Explorer(String decisionMakerIn) {
        
        findRandomName();
        this.numArrows = 0;
        this.originalNumArrows = 0;
        this.screamsHeard = 0;
        this.points = 0;
        this.decisionMaker = decisionMakerIn;
        this.currentCellKey = 0;
        this.originalCellKey = 0;
        this.status = "ALIVE";
        this.deathCount = 0;
        this.facedDirection = "NORTH";
        this.originalFacedDirection = facedDirection;
        
        if (decisionMaker.equals("REASONING")) {
            reasonInference = new ReasoningInference();
        }
        else if (decisionMaker.equals("REACTIONARY")) {
            reactionInference = new ReactionaryInference();
        }
    }
    
    /**
     * Set the new number of arrows
     * @param newNum 
     */
    public void setArrowNum(int newNum) {
        numArrows = newNum;
        originalNumArrows = newNum;
    }
    
    /**
     * Set the new status of this explorer
     * @param newStatus 
     */
    public void setStatus(String newStatus) {
        status = newStatus;
    }
    
    /**
     * Set the current cell's key recorded by this explorer
     * @param newKey 
     */
    public void setCurrentCellKey(int newKey) {
        currentCellKey = newKey;
    }
    
    /**
     * Set the starting cell key of this explorer
     * @param newKey 
     */
    public void setOriginalCellKey(int newKey) {
        originalCellKey = newKey;
    }
    
    /**
     * Set the faced direction of this explorer
     * @param newDirection 
     */
    public void setFacedDirection(String newDirection) {
        facedDirection = newDirection;
    }
    
    /**
     * Set the starting faced-direction of this explorer
     * @param newDirection 
     */
    public void setOriginalFacedDirection(String newDirection) {
        originalFacedDirection = newDirection;
    }
    
    /**
     * Overwrite original knowledge bank
     * @param newKnowledgeBank 
     */
    public void setKnowledge(ArrayList<KnowledgeFragment> newKnowledgeBank) {
        currentKnowledge = newKnowledgeBank;
    }
    
    /**
     * Given a current cell and that cell's senses report, make a decision through an inference class
     * @param currentCell
     * @param sensesReport
     * @return 
     */
    public String makeDecision(Cell currentCell, int sensesReport) {
        
        if (decisionMaker.equals("REASONING")) {
            String decisionToMake = reasonInference.findDecision(currentCell, sensesReport,
                    this);
            return decisionToMake;
        }
        else if (decisionMaker.equals("REACTIONARY")) {
            String decisionToMake = reactionInference.findDecision(currentCell, sensesReport, this);
            return decisionToMake;
        }
        
        return "Error";
    }
    
    /**
     * Rotate to face a direction, apply cost of rotation
     * @param targetDirection 
     */
    public void rotate(String targetDirection) {
        
        if (targetDirection.equals(facedDirection)) {
            // No rotation necessary
        }
        else if (targetDirection.equals("WEST") && (facedDirection.equals("EAST"))) {
            // 180 required, two points to rotate
            points += -2;
        }
        else if (targetDirection.equals("EAST") && (facedDirection.equals("WEST"))) {
            // 180 required, two points to rotate
            points += -2;
        }
        else if (targetDirection.equals("NORTH") && (facedDirection.equals("SOUTH"))) {
            // 180 required, two points to rotate
            points += -2;
        }
        else if (targetDirection.equals("SOUTH") && (facedDirection.equals("NORTH"))) {
            // 180 required, two points to rotate
            points += -2;
        }
        else {
            // 90 required, one point to rotate
            points += -1;
        }
        
        facedDirection = targetDirection;
    }
    
    /**
     * Record a wumpus death scream, heard in the dark
     */
    public void hearScream() {
        screamsHeard++;
        points += 10;
    }
    
    /**
     * Record an arrow fired into the dark
     */
    public void arrowFired() {
        if (numArrows > 0) {
            numArrows--;
            points -= 10;
        }
        else {
            System.out.println("Imaginary arrow fired!");
        }
        
        // Report remaining arrow number
        if (numArrows == 1) {
            System.out.println(name + " has " + numArrows +
                    " arrow remaining.");
        }
        else {
            System.out.println(name + " has " + numArrows +
                    " arrows remaining.");
        }
    }
    
    /**
     * Give the explorer a certain number of points, negative or positive
     * @param pointsToAdd 
     */
    public void changePoints(int pointsToAdd) {
        this.points += pointsToAdd;
    }
    
    /**
     * Reset the point total of this explorer
     */
    public void clearPoints() {
        this.points = 0;
    }
    
    /**
     * Reset inference classes
     */
    public void clearInferences() {
        if (decisionMaker.equals("REASONING")) {
            reasonInference = new ReasoningInference();
        }
        else if (decisionMaker.equals("REACTIONARY")) {
            reactionInference = new ReactionaryInference();
        }
    }
    
    /**
     * Give this explorer a random name
     */
    public final void findRandomName() {
        try {
            File nameList = new File("firstnames");
            Scanner scan = new Scanner(nameList);
            String foundName = "Indiana Jones";
            Random rand = new Random();
            int numberOfNames = 19470;
            int nameIndex = rand.nextInt(numberOfNames);
            
            for (int i = 0; i < nameIndex; i++){
                foundName = scan.nextLine();
            }
            
            name = foundName;
        } 
        
        catch (FileNotFoundException ex) {
            name = ("Indiana Jones");
        }
    }
    
    /**
     * Makes this explorer deceased
     */
    public void dies() {
        
        this.setStatus("DEAD");
        this.deathCount++;
    }
    
    /**
     * Brings this explorer back to life
     */
    public void resurrects() {
        
        this.setStatus("ALIVE");
        if (this.deathCount == 1) {
            Random rand = new Random();
            int epitaphNumber = rand.nextInt(20);
            
            // Give this explorer a one-time title that indicates a previous death
            switch (epitaphNumber) {
                
                case 0:
                    this.name += " the Zombie";
                    break;
                case 1:
                    this.name += " the Immortal";
                    break;
                case 2:
                    this.name += " the Undying";
                    break;
                case 3:
                    this.name += " the Eternal";
                    break;
                case 4:
                    this.name += " the Dead-ite";
                    break;
                case 5:
                    this.name += " the Walker";
                    break;
                case 6:
                    this.name = "The ghost of " + this.name;
                    break;
                case 7:
                    this.name += " the Skeleton";
                    break;
                case 8:
                    this.name += " the Reborn";
                    break;
                case 9:
                    this.name += " the Decomposing";
                    break;
                case 10:
                    this.name += " 2: Electric Boogaloo";
                    break;
                case 11:
                    this.name += "'s son";
                    break;
                case 12:
                    this.name += "'s daughter";
                    break;
                case 13:
                    this.name += " Jr.";
                    break;
                case 14:
                    this.name += " the Vampire";
                    break;
                case 15:
                    this.name += " the Recently-Deceased";
                    break;
                case 16:
                    this.name += " the Partially-Chewed";
                    break;
                case 17:
                    this.name = "The remains of " + this.name;
                    break;
                case 18:
                    this.name += " the Hard-To-Kill";
                    break;
                case 19:
                    this.name += " the Lich";
                    break;
            }
        }
    }
    
    /**
     * Get key of current cell
     * @return 
     */
    public int getCurrentCellKey() {
        return this.currentCellKey;
    }
    
    /**
     * Get key of starting cell
     * @return 
     */
    public int getOriginalCellKey() {
        return this.originalCellKey;
    }
    
    /**
     * Get number of arrows remaining
     * @return 
     */
    public int getNumArrows() {
        return this.numArrows;
    }
    
    /**
     * Get starting number of arrows
     * @return 
     */
    public int getOriginalNumArrows() {
        return this.originalNumArrows;
    }
    
    /**
     * Get number of screams recorded by this explorer
     * @return 
     */
    public int getScreamsHeard() {
        return this.screamsHeard;
    }
    /**
     * Get the status text of this explorer
     * @return 
     */
    public String getStatus() {
        return this.status;
    }
    
    /**
     * Get the number of times that this explorer has died
     * @return 
     */
    public int getDeathCount() {
        return this.deathCount;
    }
    
    /**
     * Get the point total of this explorer
     * @return 
     */
    public int getPoints() {
        return this.points;
    }
    
    /**
     * Get the faced direction of this explorer
     * @return 
     */
    public String getFacedDirection() {
        return this.facedDirection;
    }
    
    /**
     * Get the starting faced-direction of this explorer
     * @return 
     */
    public String getOriginalFacedDirection() {
        return this.originalFacedDirection;
    }
    
    /**
     * Get the knowledge stored by this explorer
     * @return 
     */
    public ArrayList<KnowledgeFragment> getKnowledge() {
        return this.currentKnowledge;
    }
}
