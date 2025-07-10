public class Example2{
    // javac Example2.java --release 8 && jar cvf Example2.jar *.class && rm *.class
    // javac Example2.java --release 11 && jar cvf Example2.jar *.class && rm *.class
    public static void main(String[] args) {
        System.out.println("Hi");                   // not tainted
        System.out.println("Hello " + args[0]);     // arrayload: tainted (VULNERABLE)
        int taintedVar = Integer.parseInt(args[0]); // arrayload: tainted

        String [] copy = args; // assignment: tainted
        if(taintedVar > 2)
            System.out.println(copy[2]); // tainted by propagation (VULNERABLE)
        int ok = 12;
        System.out.println(ok); // not tainted
    }
}