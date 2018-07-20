boolean areSimilar(int[] a, int[] b) {
    
    boolean answer = true;    
    ArrayList<Integer> sol = new ArrayList<>();
    
    for (int i = 0; i < a.length; i++){
        if (a[i] != b[i]){
            sol.add(i);
        }
    }
    if (sol.size() == 0){
        return true;
    }else if(sol.size() != 2){
        return false;
    }else if(sol.size() == 2){
        int sol1 = sol.get(0);
        int sol2 = sol.get(1);
    
        if (a[sol1] == b[sol2] && a[sol2] == b[sol1]){
            return true;
        }
    }
    
    return false;       
}
