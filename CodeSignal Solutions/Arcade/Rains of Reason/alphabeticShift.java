String alphabeticShift(String inputString) {
    String alphabet = "abcdefghijklmnopqrstuvwxyz";
    String ans = "";
        
    
    for (int i = 0; i < inputString.length(); i++){
        for (int j = 0; j < alphabet.length(); j++){
            if(inputString.charAt(i) == 'z'){
                ans += alphabet.charAt(0);
                break;
            }else if (inputString.charAt(i) == alphabet.charAt(j)){
                
                ans += alphabet.charAt(j+1);
            }
        }
    }
    
    return ans;
}
