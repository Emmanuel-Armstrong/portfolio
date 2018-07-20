int commonCharacterCount(String s1, String s2) {
    char[] arr1 = s1.toCharArray();
    char[] arr2 = s2.toCharArray();
    int count = 0;


    for (int i = 0; i < arr1.length; i++){
        for (int j = 0; j < arr2.length; j++){
            if (arr1[i] == arr2[j]){
                count++;
                arr1[i] = 0;
                arr2[j] = 1;
            }
        }
        
    }
    
    return count;
}
