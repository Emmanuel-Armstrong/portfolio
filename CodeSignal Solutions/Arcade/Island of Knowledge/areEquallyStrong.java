boolean areEquallyStrong(int yourLeft, int yourRight, int friendsLeft, int friendsRight) {
    int[] yourArms = new int[2];
    int[] friendsArms = new int[2];
    
    yourArms[0] = yourLeft;
    yourArms[1] = yourRight;
    
    friendsArms[0] = friendsLeft;
    friendsArms[1] = friendsRight;
    
    if ( (yourArms[0] == friendsArms[0] || yourArms[0] == friendsArms[1]) && (yourArms[1] == friendsArms[0] || yourArms[1] == friendsArms[1])){
        return true;
    }else{
        return false;
    }

}
