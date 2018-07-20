int arrayMaximalAdjacentDifference(int[] inputArray) {
    
    int maxDifference = 0;
    
    for (int i = 0; i < inputArray.length-1; i++){
            if(Math.abs(inputArray[i] - inputArray[i+1]) > maxDifference){
                maxDifference = Math.abs(inputArray[i] - inputArray[i+1]);
            }
    }
    
    return maxDifference;

}
