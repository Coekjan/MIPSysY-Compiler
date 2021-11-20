package backend;

import midend.Value;
import utils.Pair;

import java.util.*;

public class LoopScheduler implements RegScheduler {
    private final List<Reg> regs = Arrays.asList(
            Reg.T0, Reg.T1, Reg.T2, Reg.T3, Reg.T4, Reg.T5, Reg.T6, Reg.T7,
            Reg.S0, Reg.S1, Reg.S2, Reg.S3, Reg.S4, Reg.S5, Reg.S6, Reg.S7
    );

    private final Map<Reg, Value> cur = new HashMap<>();
    private final List<Reg> inUse = new LinkedList<>();
    private final List<Reg> free = new LinkedList<>(regs);

    @Override
    public Pair<Value, Reg> overflow(Collection<Reg> holdRegs) {
        assert !inUse.isEmpty();
        final Reg reg = getReg(inUse, holdRegs);
        final Value v = cur.remove(reg);
        inUse.remove(reg);
        free.add(0, reg);
        return Pair.of(v, reg);
    }

    private Reg getReg(List<Reg> from, Collection<Reg> holdRegs) {
        for (Reg r : from) {
            if (!holdRegs.contains(r)) return r;
        }
        return null;
    }

    @Override
    public Optional<Reg> allocReg(Value name, Collection<Reg> holdRegs) {
        final Reg r;
        if (!free.isEmpty()) {
            r = free.remove(0);
            inUse.add(0, r);
        } else {
            return Optional.empty();
        }
        cur.put(r, name);
        return Optional.of(r);
    }

    @Override
    public void remove(Reg reg) {
        inUse.remove(reg);
        free.add(0, reg);
        cur.remove(reg);
    }

    @Override
    public void clear() {
        cur.clear();
        inUse.clear();
        free.clear();
        free.addAll(regs);
    }

    @Override
    public Optional<Reg> find(Value name) {
        for (Reg r : inUse) {
            if (cur.get(r).equals(name)) {
                return Optional.of(r);
            }
        }
        return Optional.empty();
    }

    @Override
    public Map<Reg, Value> current() {
        return cur;
    }
}
