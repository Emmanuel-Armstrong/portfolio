boolean variableName(String name) {
   boolean ans = false;
   
   if (Character.isDigit(name.charAt(0))){
      return false;
   }
   
   for (int i = 0; i < name.length(); i++){
      if ((name.charAt(i) == '_') || Character.isLetter(name.charAt(i)) || Character.isDigit(name.charAt(i))){
         ans = true;
      }else{
         return false;
      }
   }
   return ans;

}
