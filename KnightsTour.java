/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package knightstour;

/**
 *
 * @author Emmanuel Armstrong
 */

import java.util.ArrayList;
import java.util.List;

public class KnightsTour{

    // Indicates that a square is not visited
    private static int NOT_VISITED = -1;
    
    // Height of board
    private int h;
    
    // Width of board
    private int w;
    
    // Number of solutions
    public int solutionsCount;
    
    //creates the board for the solution
    private int[][] solutionBoard;

    
    
   
    public KnightsTour(int w, int h){
        
        solutionsCount = 0;

        this.w = w;
        this.h = h;

        solutionBoard = new int[h][w];
        
        for (int i = 0; i < h; i++){
            
            for (int j = 0; j < w; j++){
                
                solutionBoard[i][j] = NOT_VISITED;
            }
        }
    }

    // Method to return possible knight destinations
    private List<Coordinates> getDestination(int x, int y){
        
        List<Coordinates> coordList = new ArrayList<Coordinates>();
        if (x + 2 < w && y - 1 >= 0)
            coordList.add(new Coordinates(x + 2, y - 1)); //move right and up
        if (x + 1 < w && y - 2 >= 0)
            coordList.add(new Coordinates(x + 1, y - 2)); //move up and right
        if (x - 1 >= 0 && y - 2 >= 0)
            coordList.add(new Coordinates(x - 1, y - 2)); //move up and left
        if (x - 2 >= 0 && y - 1 >= 0)
            coordList.add(new Coordinates(x - 2, y - 1)); //move left and up
        if (x - 2 >= 0 && y + 1 < h)
            coordList.add(new Coordinates(x - 2, y + 1)); //move left and down
        if (x - 1 >= 0 && y + 2 < h)
            coordList.add(new Coordinates(x - 1, y + 2)); //move down and left
        if (x + 1 < w && y + 2 < h)
            coordList.add(new Coordinates(x + 1, y + 2)); //move down and right
        if (x + 2 < w && y + 1 < h)
            coordList.add(new Coordinates(x + 2, y + 1)); //move right and down
        return coordList;
    }

    // Moves the knight
    private void move(int x, int y, int turnNum){
        
        solutionBoard[y][x] = turnNum;
        
        if (turnNum == (w * h) - 1){
            
            solutionsCount++;
            printSolution();
            return;
            
        }else{
            
            for (Coordinates coords : getDestination(x, y)){
                
                if (solutionBoard[coords.getY()][coords.getX()] == NOT_VISITED){
                    
                    move(coords.getX(), coords.getY(), turnNum + 1);
                    solutionBoard[coords.getY()][coords.getX()] = NOT_VISITED; //reset the square
                }
            }
        }
    }

    // Method to print the board
    private void printSolution(){
     
     /*   
     * 0 is the Starting position
     * 1 is the first move
     * 2 is the second move and so on
     */
        System.out.println("Solution #" + solutionsCount);
        
        for (int i = 0; i < solutionBoard.length; i++){
            
            for (int j = 0; j < solutionBoard[i].length; j++){
                
                System.out.print(solutionBoard[i][j] + " ");
            
            }
            
            System.out.println("");
        }
        
        System.out.println("");
    }
    
    
    public int getSolutionsCount(){
        return solutionsCount;
    } 
    

    public void solve(){
        
        for (int i = 0; i < h; i++){
            
            for (int j = 0; j < w; j++){
                
                move(j, i, 0);
                solutionBoard[i][j] = NOT_VISITED; //reset the square
            }
        }
    }
}
