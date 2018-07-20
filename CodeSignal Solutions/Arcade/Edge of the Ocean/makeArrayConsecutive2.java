int makeArrayConsecutive2(int[] statues) {
    int count = 0;
    Arrays.sort(statues);
    for (int i = 0; i < statues.length-1; i++){
        
        int difference = statues[i+1] - statues[i]; 
        if (difference > 1){
            count += difference - 1;
        }
    }
    return count;
}
