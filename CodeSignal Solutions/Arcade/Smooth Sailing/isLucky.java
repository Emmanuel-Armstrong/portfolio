boolean isLucky(int n) {
    char[] num = (""+n).toCharArray();
    int[] h1 = new int[num.length/2];
    int[] h2 = new int[num.length/2];
    
    int sum1 = 0;
    int sum2 = 0;
    
        for (int i = 0; i < h1.length; i++){
            sum1 += num[i];
        }
        for (int i = 0; i < h2.length; i++){
            sum2 += num[h1.length + i];
        }
        if (sum1 == sum2){
            return true;
        }else{
            return false;
        }

}
