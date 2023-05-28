package pt.up.fe.comp2023.jmm.jasmin;

import java.util.HashSet;
import java.util.Set;

public class Limits {
    private int stackSize;
    private int maxSize;
    private Set<Integer> regs;

    public Limits() {
        reset();
    }

    public void updateStack(int diff) {
        stackSize += diff;
        maxSize = Math.max(stackSize, maxSize);
    }

    public void updateRegisters(int regNum) {
        regs.add(regNum);
    }

    public void registersUntil(int size) {
        for (int i = 0; i < size; i++) {
            regs.add(i);
        }
    }

    public void reset() {
        stackSize = 0;
        maxSize = 0;
        regs = new HashSet<>();
    }

    public int getStackLimit() {
        return maxSize;
    }

    public int getLocalsLimit() {
        return regs.size();
    }
}
