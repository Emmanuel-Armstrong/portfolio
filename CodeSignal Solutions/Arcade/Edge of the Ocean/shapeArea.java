int shapeArea(int n) {
    int area = 0;
    
    if (n == 1){
        area = 1;
    }else if(n > 1){
        area = 1;
        for (int i = 0; i < n; i++){
            area += i*4;
        }
    }
        return area;
}
