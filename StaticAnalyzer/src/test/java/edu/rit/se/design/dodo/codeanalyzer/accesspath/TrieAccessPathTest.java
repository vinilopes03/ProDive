package edu.rit.se.design.dodo.codeanalyzer.accesspath;

import edu.rit.se.design.dodo.codeanalyzer.accessPath.TrieAccessPath;
import org.junit.Test;
import org.junit.Assert;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class TrieAccessPathTest {

    //inserting values into the trie
    public TrieAccessPath insertTestData(){
        TrieAccessPath trie = new TrieAccessPath("className");
        trie.insert("method1.var1.var2");
        trie.insert("method1.var1.var3");
        trie.insert("method2.var1.var2");
        trie.insert("method2.var1.var5");
        trie.insert("method2.var1.var6");
        return trie;
    }


    @Test
    public void testLookForFullName() {
        TrieAccessPath trie = insertTestData();

        //Test for the case where the full name is found in the trie
        assertTrue((trie.lookForFullName(trie.getRoot(), "method1.var1.var2")).equals("method1.var1.var2"));

        //Test for the case where the full name is not found in the trie
        assertFalse((trie.lookForFullName(trie.getRoot(), "method1.var1.var4")).equals("method1.var1.var4"));
    }

    @Test

    public void preOrderTraversal() {
        TrieAccessPath trie = insertTestData();

        //PreOrderTraversal for the case where the data is found in the trie, it returns the first found
        assertTrue((trie.preOrderTraversal(trie.getRoot(), "var6")).getData().equals("var6"));

        //PreOrderTraversal for the case where the data is not found in the trie, it returns null
        Assert.assertNull(trie.preOrderTraversal(trie.getRoot(), "var7"));
    }

}
