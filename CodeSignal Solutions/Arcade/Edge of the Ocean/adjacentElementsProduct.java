int adjacentElementsProduct(int[] inputArray) {
    int largestProduct = -9999999;
    int product = 0;
    
    for (int i = 0; i < inputArray.length-1; i++){
            product = inputArray[i] * inputArray[i+1];
            
            if(product > largestProduct){
                largestProduct = product;
            }
            
    }
    
    return largestProduct;

}
