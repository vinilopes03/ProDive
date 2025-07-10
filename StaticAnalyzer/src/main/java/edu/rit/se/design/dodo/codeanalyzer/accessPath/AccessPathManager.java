package edu.rit.se.design.dodo.codeanalyzer.accessPath;

import com.ibm.wala.classLoader.IClass;
import com.ibm.wala.classLoader.IMethod;
import com.ibm.wala.ipa.callgraph.AnalysisOptions;
import com.ibm.wala.ipa.callgraph.AnalysisScope;
import com.ibm.wala.ipa.callgraph.impl.Everywhere;
import com.ibm.wala.ssa.*;
import com.ibm.wala.types.ClassLoaderReference;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class AccessPathManager {
    public void AccessPath() {

    }

/*
    public String getKAccessPaths(int k) {
        StringBuilder sb = new StringBuilder();
        sb.append(this.className).append(".").append(this.methodName);

        int interactions = 1;
        int iterator = (k-2);

        if (iterator <= values.size()-1) {

            while (interactions < k) {
                sb.append(".").append(values.get(iterator));
                iterator--;
                interactions++;
            }
            sb.append(".").append(this.keyValue);
        }
        else {
            //do a for that goes from the last to the first in the list

            for (int i = values.size()-1; i>= 0; i--){
                sb.append(".").append(values.get(i));
            }
            sb.append(".").append(this.keyValue);
        }
        return sb.toString();
    }


 */

    public static String getFullName(String methodName, List<String> values, String keyValue) {
        StringBuilder sb = new StringBuilder();
        sb.append(methodName).append(".");
        for (int i = values.size()-1; i>= 0; i--){
            sb.append(values.get(i)).append(".");
        }
        sb.append(keyValue);
        return sb.toString();
    }

    public static String getClassNameAndMethodName(IMethod method) {
        IClass declaringClass = method.getDeclaringClass();
        String className = declaringClass.getName().toString();
        String methodName = method.getName().toString();
        return className + "." + methodName;
    }

    public static HashMap generateTheVariables(String instructionString) {
        // Regular expression to match variable names which start with 'v' and are followed by digits
        Pattern pattern = Pattern.compile("\\bv\\d+\\b");

        String assignedVariable = null;
        List<String> usedVariables = new ArrayList<>();

        // Split the instruction at the '=' to separate assigned variable from the rest
        String[] parts = instructionString.split("=", 2);

        if (parts.length == 2) {
            // Get the assigned variable (before '=')
            String assignedTo = parts[0].trim();
            Matcher assignedMatcher = pattern.matcher(assignedTo);
            assignedVariable = assignedMatcher.find() ? assignedMatcher.group() : null;

            // Get the used variables (after '=' and before 'exception:')
            String afterEquals = parts[1].split("exception:")[0].trim();
            Matcher usedMatcher = pattern.matcher(afterEquals);
            while (usedMatcher.find()) {
                usedVariables.add(usedMatcher.group());
            }
        } else {
            // No '=' sign, so we consider all variables as used variables
            String withoutException = instructionString.split("exception:")[0].trim();
            Matcher usedMatcher = pattern.matcher(withoutException);
            while (usedMatcher.find()) {
                usedVariables.add(usedMatcher.group());
            }
        }

        // Extract operands like v3:#0
        Matcher operandMatcher = Pattern.compile("\\bv\\d+\\[#\\d+\\]").matcher(instructionString);
        while (operandMatcher.find()) {
            usedVariables.add(operandMatcher.group());
        }

        HashMap<String, List<String>> map = new HashMap<>();
        map.put(assignedVariable, usedVariables);

        return map;
    }

    public static TrieAccessPath generateMethodSummary(IMethod method, TrieAccessPath trie) {
        AnalysisOptions options = new AnalysisOptions();
        IRFactory<IMethod> irFactory = new DefaultIRFactory();
        IR ir = irFactory.makeIR(method, Everywhere.EVERYWHERE, options.getSSAOptions());

        SymbolTable symbolTable = ir.getSymbolTable();

        String classAndMethodName = getClassNameAndMethodName(method);
        String methodName = method.getName().toString();


        System.out.println("...................................." + classAndMethodName + "....................................");
        for (SSAInstruction instr : ir.getInstructions()) {
            if (instr != null) {
                System.out.println("----Instruction: " + instr.toString(symbolTable));
                HashMap variables =  generateTheVariables(instr.toString(symbolTable));

                for (Object key : variables.keySet()) {
                    String assignedVariable = (String) key;
                    List<String> usedVariables = (List<String>) variables.get(key);

                    if (assignedVariable != null) {
                        trie.insert(AccessPathManager.getFullName(methodName, usedVariables, assignedVariable));
                    }
                }

            }
        }

        return trie;
    }



    public static boolean isFromApplication(IClass klass, AnalysisScope scope) {
        return klass.getClassLoader().getReference().equals(ClassLoaderReference.Application);
    }
}
