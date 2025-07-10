package edu.rit.se.design.dodo.codeanalyzer.accessPath;

import java.util.ArrayList;
import java.util.List;

public class TrieNode {
    private String  data;
    private List<TrieNode> children;
    private boolean isLeaf;
    private final TrieNode parent;
    private final int height;

    public TrieNode(TrieNode parent, String data, int height)
    {
        if (data == null)
            throw new IllegalArgumentException("Data cannot be null.");
        this.parent = parent;
        this.data = data;
        this.isLeaf = false;
        this.height = height;
        this.children = new ArrayList<>();
    }

    public TrieNode getParent() {
        return this.parent;
    }

    public String getData() {
        return this.data;
    }

    public int getHeight() {
        return this.height;
    }

    public void addChild(TrieNode child) {
        this.children.add(child);
    }


    public TrieNode getChild(String part) {
        for (TrieNode child : this.children) {
            if (child.getData().equals(part)) {
                return child;
            }
        }
        return null;
    }

    public void setLeaf(boolean b) {
        this.isLeaf = b;
    }

    public boolean isLeaf() {
        return this.isLeaf;
    }

    public List<TrieNode> getChildren() {
        return this.children;
    }
}