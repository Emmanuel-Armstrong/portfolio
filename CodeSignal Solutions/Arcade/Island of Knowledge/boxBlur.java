int[][] boxBlur(int[][] image) {
    int h = image.length-2;
    int w = image[0].length-2;
    int[][] blurImage = new int[h][w];
    
    for (int i = 1; i < image.length-1; i++){
        for (int j = 1; j < image[0].length-1; j++){
            int count = 0;
            for (int k = i-1; k <= i+1; k++){
                for (int l = j-1; l <= j+1; l++){
                    count += image[k][l];
                }
            }
            blurImage[i-1][j-1] = count/9;
        }
    }
    return blurImage;

}
