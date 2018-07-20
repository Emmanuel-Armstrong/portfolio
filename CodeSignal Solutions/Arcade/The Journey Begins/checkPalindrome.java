boolean checkPalindrome(String inputString) {
    String reverse = new StringBuilder(inputString).reverse().toString();
    
    if(reverse.equals(inputString)){
        return true;
    }else{
        return false;
    }
    
    

}
