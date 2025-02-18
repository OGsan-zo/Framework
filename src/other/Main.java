package other;

public class Main {
    public static void main(String[] args) {
        VerbAction action1 = new VerbAction("GET", "doSomething");
        VerbAction action2 = new VerbAction("GET", "doSomething");
        VerbAction action3 = new VerbAction("POST", "doSomething");

        System.out.println(action1.equals(action2)); // true
        System.out.println(action1.equals(action3)); // false


    }
}
