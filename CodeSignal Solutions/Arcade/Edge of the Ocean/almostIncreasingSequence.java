boolean almostIncreasingSequence(int[] sequence) {
    int count = 0;
    
    for (int i = 0; i < sequence.length-1; i++){
        if (sequence[i] >= sequence[i+1]){
            count += 1;
        }
        if ((i-1) >= 0 && sequence[i-1] >= sequence[i+1]){
            if((sequence.length - 2 > i) && (sequence[i] >= sequence[i+2])){
                return false;
            }
        }

    }
    if (count <= 1){
        return true;
    }else{
        return false;
    }
}
