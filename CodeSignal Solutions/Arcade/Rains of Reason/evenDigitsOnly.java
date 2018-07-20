boolean evenDigitsOnly(int n) {
    int length = Integer.toString(n).length();
    int[] numArr = new int[length];
    boolean ans = true;
    
    //take the mod 10 of the int and add that to the array, getting the first number. then devide the int by 10;
    for (int i = 0; i < numArr.length; i++){
        numArr[i] = n % 10;
        n /= 10;
    }
    
    
    //check if each element in numArr is even.
    for (int i = 0; i < numArr.length; i++){
        if ((numArr[i] % 2) == 0){
            ans = true;
        }else{
            return false;
        }
    }
    
    return ans;
}
