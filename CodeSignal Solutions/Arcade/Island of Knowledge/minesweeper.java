int[][] minesweeper(boolean[][] matrix) {
    int height = matrix.length;
    int width = matrix[0].length;
    int[][] mines = new int[height][width];
    
    //create a new matrix filled with Zeros 
    for (int i = 0; i < height; i++){
        for (int j = 0; j < width; j++){
            mines[i][j] = 0;
        }
    }
    
    //loop through the matrix then compare each cell with every surrounding cell,  if they are different add 1 to the corresponding cell in the solution matrix
    for (int i = 0; i < height; i++){
        for (int j = 0; j < width; j++){
            for (int k = Math.max(0,i-1); k <= Math.min(i+1, matrix.length-1); k++){
                for (int l = Math.max(0, j-1); l <= Math.min(j+1, matrix[0].length-1); l++){
                    if(matrix[k][l] && (i != k || l != j)){
                        mines[i][j]++;
                    }
                    
                }
            }
        }    
    }
    return mines;

}
