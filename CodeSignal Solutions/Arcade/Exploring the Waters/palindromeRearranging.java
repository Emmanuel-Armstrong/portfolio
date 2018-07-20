boolean palindromeRearranging(String inputString) {
    int count1 = 0;
    int Max_Char = 256;
    
    int count[] = new int[Max_Char];
    
    for (int i = 0; i < inputString.length(); i++){
        count[inputString.charAt(i)]++;
    }
    
    for (int i = 0; i < Max_Char; i++){
        if((count[i] & 1) == 1){
            count1++;
        }
    }
    
    if (count1 >= 2){
        return false;
    }else{
        return true;
    }

}
