package ex3;

public class Example3{

    // javac Example3.java --release 8 && jar cvf Example3.jar *.class && rm *.class
    public static void main(String[] args) {
        int x = Integer.parseInt(args[0]);
        int y = 0;
        int m = 0;
        y = foo(x);
        y = bar(m);
    }

    public static int foo(int p){
        p = 0;
        return p;
    }
    public static int bar(int p){
        int k = p+2;
        return k;
    }

    public static int baz(int p, int q  , int r){
        int j = p + q + r;
        return j;
    }
}