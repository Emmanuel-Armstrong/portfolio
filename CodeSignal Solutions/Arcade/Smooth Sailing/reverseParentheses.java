String reverseParentheses(String s) {
    char[] solution = s.toCharArray();
    int[] openPosStack = new int[solution.length];
    int openPosTop = -1;

    for (int i = 0; i < solution.length; i++) {
        switch (solution[i]) {
          case '(':
            openPosStack[++openPosTop] = i;
            break;
          case ')':
            if (openPosTop < 0) {
                throw new IllegalArgumentException("Parenthesis mismatch");
            }
            int a, b;
            for (a = openPosStack[openPosTop--], b = i; a < b; a++, b--) {
                char swap = solution[a];
                solution[a] = solution[b];
                solution[b] = swap;
            }
        }
    }
    if (openPosTop >= 0) {
        throw new IllegalArgumentException("Parenthesis mismatch");
    }

    // Remove parentheses from output
    int o = 0;
    for (int i = 0; i < solution.length; i++) {
        switch (solution[i]) {
          case '(': case ')':
            break;
          default:
            solution[o++] = solution[i];
        }
    }
    return new String(solution, 0, o);
}
