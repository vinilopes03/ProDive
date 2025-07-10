public class Example1{
    // javac Example1.java --release 8 && jar cvf Example1.jar *.class && rm *.class
    public static void main(String[] args) {
        System.out.println("Hi");                   // not tainted
        System.out.println("Hello " + args[0]);     // arrayload: tainted
        int taintedVar = Integer.parseInt(args[0]); // arrayload: tainted

        String [] copy = args; // assignment: tainted
        System.out.println(copy[2]); // tainted by propagation
        int ok = 12;
        System.out.println(ok); // not tainted
    }
}