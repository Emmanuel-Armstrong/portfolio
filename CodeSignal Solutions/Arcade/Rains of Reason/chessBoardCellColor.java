boolean chessBoardCellColor(String cell1, String cell2) {
    String letters = "ABCDEFGH";
    int cell1Num = 0;
    int cell2Num = 0;
    boolean ans = false;
    
    
    for (int i = 0; i < letters.length(); i++){
        if (cell1.charAt(0) == letters.charAt(i)){
            cell1Num = i;
        }
        if (cell2.charAt(0) == letters.charAt(i)){
            cell2Num = i;
        }     
    }
    
    if ((cell1Num % 2 == cell1.charAt(1) % 2) && (cell2Num % 2 == cell2.charAt(1) % 2)){
        return true;
    }else if ((cell1Num % 2 != cell1.charAt(1) % 2) && (cell2Num % 2 != cell2.charAt(1) % 2)){
        return true;
    } else{
        return false;
    }
        
}
