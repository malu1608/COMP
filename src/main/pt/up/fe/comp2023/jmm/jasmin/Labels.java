package pt.up.fe.comp2023.jmm.jasmin;

public class Labels {
    private int counter;

    public Labels() {
        this.counter = 0;
    }

    public int increaseCounter() {
        return counter++;
    }
}
