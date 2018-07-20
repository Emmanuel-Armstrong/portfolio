int avoidObstacles(int[] inputArray) {
    for (int i = 2;; i++){
        boolean t = true;
        for (int j : inputArray){
            t = t && j%i !=0; 
        }
        if(t){
            return i;
        }
    }

}
