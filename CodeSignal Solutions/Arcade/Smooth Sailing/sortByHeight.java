int[] sortByHeight(int[] a) {
    int[] temp = null;
    int count = a.length;
    int tempNum = 0;
    int k = 0;
    int j = 0;
    
    for (int i = 0; i < a.length; i++){
        if (a[i] == -1){
            count--;
        } 
    }
    
    temp = new int[count];
    
    for (int i = 0; i < a.length; i++){
            if (a[i] != -1){
                temp[k] = a[i];
                a[i] = 0;
                k++;
            }
    }
    
    Arrays.sort(temp);
        
    for (int i = 0; i < a.length; i++){
        if(a[i] == 0){
            a[i] = temp[j];
            j++;
        }
    }
    
    return a;    

}
