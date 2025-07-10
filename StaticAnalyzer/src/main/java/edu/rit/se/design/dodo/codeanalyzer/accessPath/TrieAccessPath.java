package edu.rit.se.design.dodo.codeanalyzer.accessPath;

import java.util.ArrayList;
import java.util.List;

public class TrieAccessPath
{

    private TrieNode root;

    public TrieAccessPath(String data){
        root = new TrieNode(null, data, 0);
    }

    public void insert(String data){
        String[] parts = data.split("\\.");
        TrieNode current = root;
        for (int i = 0; i < parts.length; i++) {
            TrieNode child = current.getChild(parts[i]);
            if (child == null) {
                child = new TrieNode(current, parts[i], i);
                current.addChild(child);
            }
            current = child;
        }
        current.setLeaf(true);
    }


    public TrieNode getRoot() {
        return root;
    }

    public void setRoot(TrieNode root) {
        this.root = root;
    }


    public TrieNode preOrderTraversal(TrieNode node, String data){
        if (node.getData().equals(data)) {
            System.out.println(node.getData());
            return node;
        }
        for (TrieNode child : node.getChildren()) {
            TrieNode result = preOrderTraversal(child, data);
            if (result != null) {
                return result;
            }
        }
        return null;
    }

    public TrieNode levelOrderTraversal(TrieNode node, String data){
        List<TrieNode> queue = new ArrayList<>();
        queue.add(node);
        while (!queue.isEmpty()) {
            TrieNode current = queue.remove(0);
            System.out.println(current.getData());
            if (current.getData().equals(data)) {
                return current;
            }
            queue.addAll(current.getChildren());
        }
        return null;
    }


    public String lookForFullName(TrieNode node, String Path){
        String [] nodesData = Path.split("\\.");
        String fullName = "";
        for (String data : nodesData) {
            //node = className
            TrieNode child = node.getChild(data);
            if (child != null) {
                fullName += child.getData() + ".";
                node = child;
            }
        }
        if(fullName.endsWith("."))
            fullName = fullName.substring(0, fullName.length()-1);


        return fullName;
    }
    public void printTree(TrieNode node, String path){

        if(node.isLeaf()){
            System.out.println(path);
            return;
        }
        for (TrieNode child : node.getChildren()) {

            printTree(child, path + " -> " + child.getData());
        }
    }
        /*
    public static void main(String[] args) {
        Trie trie = new Trie("className");
        trie.insert("method1.var1.var2");
        trie.insert("method1.var1.var3");
        trie.insert("method2.var1.var2");

        System.out.println("fullName: " + trie.lookForFullName(trie.getRoot(), "method1.var1.var2"));


    }
        */



}
