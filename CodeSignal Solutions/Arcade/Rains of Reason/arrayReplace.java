int[] arrayReplace(int[] inputArray, int elemToReplace, int substitutionElem) {
    
    // loop through the array and each time it sees elemToReplace it puts substitutionElem in that spot in the array
    for (int i = 0; i < inputArray.length; i++){
        if (inputArray[i] == elemToReplace){
            inputArray[i] = substitutionElem;
        }
    }
    
    return inputArray;

}
