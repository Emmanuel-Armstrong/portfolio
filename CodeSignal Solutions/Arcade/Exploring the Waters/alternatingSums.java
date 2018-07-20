int[] alternatingSums(int[] a) {
    int count1 = 0;
    int count2 = 0;
    boolean toggle = false;
    List<Integer> team1 = new ArrayList<Integer>();
    List<Integer> team2 = new ArrayList<Integer>();
    int[] solution = new int[2];
    
    for (int i = 0; i < a.length; i++){
        if (toggle == false){
            team1.add(a[i]);
            toggle = true;
        }else if (toggle == true){
            team2.add(a[i]);
            toggle = false;
        }
    }
    
    for (int i = 0; i < team1.size(); i++){
        count1 += team1.get(i);
    }
    for (int i = 0; i < team2.size(); i++){
        count2 += team2.get(i);
    }
    
    solution[0] = count1;
    solution[1] = count2;
    
    return solution;

}
