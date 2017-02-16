/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package HashTable;

/**
 *
 * @author Emmanuel Armstrong
 */
public class HashTable {
    private Node[] table;
    private int size ;
 
    /* Constructor */
    public HashTable(int tableSize)
    {
        table = new Node[nextPrime(tableSize)];
        size = 0;
    }
        
    
    public int getSize()
    {
        return size;
    }
    
    public boolean isEmpty()
    {
        return size == 0;
    }
    
    //method to clear hash table 
    public void makeEmpty()
    {
        int l = table.length;
        table = new Node[l];
        size = 0;
    }
    
    private int myhash(Integer x)
    {
        int hashVal = x.hashCode();
        hashVal %= table.length;
        if (hashVal < 0)
            hashVal += table.length;
        return hashVal;
    }

    public void insert(int val)
    {
        size++;
        int pos = myhash(val);        
        Node root = table[pos];
        root = insert(root, val);
        table[pos] = root;        
    }
    
    //method to insert data 
    private Node insert(Node node, int data)
    {
        if (node == null)
            node = new Node(data);
        else
        {
            if (data <= node.data)
                node.left = insert(node.left, data);
            else
                node.right = insert(node.right, data);
        }
        return node;
    }
    
    //method to find a value
    public void find(int val){
        int pos = myhash(val);        
        Node root = table[pos];
        try
        {
            root = findNode(val, root); 
            int data = root.data;
            System.out.println("Found " + data);
            
        }
        catch (Exception e)
        {
            System.out.println("\nElement not present\n");        
        }                
    }
    
    //method to find the node containing said value
    public Node findNode(int val, Node node){
    if(node != null){
        if(node.data == val){
           return node;
        } else {
            Node foundNode = findNode(val, node.left);
            if(foundNode == null) {
                foundNode = findNode(val, node.right);
            }
            return foundNode;
         }
    } else {
        return null;
    }
}

    private static int nextPrime(int n)
    {
        if (n % 2 == 0)
            n++;
        for ( ; !isPrime(n); n += 2);
 
        return n;
    }
    
    private static boolean isPrime(int n)
    {
        if (n == 2 || n == 3)
            return true;
        if (n == 1 || n % 2 == 0)
            return false;
        for (int i = 3; i * i <= n; i += 2)
            if (n % i == 0)
                return false;
        return true;
    }
    
    public void printHashTable()
    {
        System.out.println();
        for (int i = 0; i < table.length; i++)
        {
            System.out.print (i + ":  ");            
            inorder(table[i]);
            System.out.println();
        }
    }  
    
    private void inorder(Node r)
    {
        if (r != null)
        {
            inorder(r.left);
            System.out.print(r.data +" ");
            inorder(r.right);
        }
    }  
    
    //Extra credit methods for deletion
    public void remove(int val)
    {
        int pos = myhash(val);        
        Node root = table[pos];
        try
        {
            root = delete(root, val);    
            size--;
        }
        catch (Exception e)
        {
            System.out.println("\nElement not present\n");        
        }        
        table[pos] = root;        
    }
    
    private Node delete(Node root, int val)
    {
        Node p, p2, n;
        if (root.data == val)
        {
               Node lt, rt;
            lt = root.left;
            rt = root.right;
            if (lt == null && rt == null)
                return null;
            else if (lt == null)
            {
                p = rt;
                return p;
            }
            else if (rt == null)
            {
                p = lt;
                return p;
            }
            else
            {
                p2 = rt;
                p = rt;
                while (p.left != null)
                    p = p.left;
                p.left = lt;
                return p2;
            }
        }
        if (val < root.data)
        {
            n = delete(root.left, val);
            root.left = n;
        }
        else
        {
            n = delete(root.right, val);
            root.right = n;             
        }
        return root;
    }
}