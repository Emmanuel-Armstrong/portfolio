String[] addBorder(String[] picture) {
    String[] newPicture = new String[(picture.length) + 2];
    char[] border = new char[picture[0].length() + 2];
    int count = 0;
  
    
    for (int i = 0; i < border.length; i++){
        border[i] = '*';
    }
    
    String b = new String(border);  
    
    for (int i = 0; i < newPicture.length; i++){
            if (i == 0 || i == newPicture.length-1){
                newPicture[i] = b;
            }else if (count <= picture.length){
                newPicture[i] = '*' + picture[count] + '*';
                count++;
            }
    }
    return newPicture;

}
