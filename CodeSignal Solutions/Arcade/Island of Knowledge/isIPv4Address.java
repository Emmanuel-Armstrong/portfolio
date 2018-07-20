boolean isIPv4Address(String inputString) {
    int max = 255;
    int dotCount = 0;
    int length = inputString.length();
    String[] splitted = inputString.split("\\.");
   
    
    if(inputString.charAt(0) == '.' || inputString.charAt(length-1) == '.'){
        return false;
    }
    
    for (int i = 0; i < length-1; i++){
        if (inputString.charAt(i) == '.'){
            dotCount++;
        }
     }
    
    if (dotCount != 3){
        return false;
    }
    
    for (int i = 0; i < splitted.length; i++){
        if(splitted[i].matches(".*[a-z].*")){
            return false;
        }else if (splitted[i].equals("")){
            return false;
        }else if (splitted[i].length() > 10){
            return false;            
        }else if (Integer.parseInt(splitted[i]) < 0 || Integer.parseInt(splitted[i]) > max){
            return false;
        }
    }
    
    return true;
    

}
