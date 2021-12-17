package backend;

import midend.IntermediateCode;
import midend.Value;
import utils.Pair;

import java.util.*;
import java.util.stream.Collectors;

public class ColorScheduler implements RegScheduler {
    private final Map<String, RegAllocator> allocators;
    private final Map<Reg, Value> cur = new HashMap<>();
    private final List<Reg> inUse = new LinkedList<>();
    private final List<Reg> free = new LinkedList<>(regs.stream()
            .filter(r -> !r.isSaved()).collect(Collectors.toList()));
    private final Map<Value, Reg> mem = new HashMap<>();
    private String currentContext;

    public ColorScheduler(Map<String, RegAllocator> allocators) {
        this.allocators = allocators;
    }

    @Override
    public Pair<Value, Reg> overflow(Collection<Reg> holdRegs) {
        Reg res = inUse.get(0);
        for (Reg reg : inUse) {
            if (!holdRegs.contains(reg)) {
                res = reg;
                break;
            }
        }
        final Value value = cur.get(res);
        inUse.remove(res);
        free.add(res);
        return Pair.of(value, res);
    }

    @Override
    public Optional<Reg> allocReg(Value name, Collection<Reg> holdRegs) {
        final Optional<Reg> reg = allocators.get(currentContext).findReg(name);
        if (reg.isPresent()) {
            mem.put(name, reg.get());
            cur.put(reg.get(), name);
            return reg;
        }
        if (!free.isEmpty()) {
            final Reg r = free.remove(0);
            inUse.add(r);
            cur.put(r, name);
            return Optional.of(r);
        } else {
            return Optional.empty();
        }
    }

    @Override
    public void remove(Reg reg) {
        if (inUse.contains(reg)) {
            inUse.remove(reg);
            free.add(reg);
        }
        cur.remove(reg);
    }

    @Override
    public void clear() {
        mem.clear();
        cur.clear();
        inUse.clear();
        free.clear();
        free.addAll(regs.stream().filter(r -> !r.isSaved()).collect(Collectors.toList()));
    }

    @Override
    public Optional<Reg> find(Value name) {
        for (Reg r : cur.keySet()) {
            if (cur.get(r).equals(name)) {
                if (inUse.contains(r)) {
                    inUse.remove(r);
                    inUse.add(r);
                }
                return Optional.of(r);
            }
        }
        if (mem.containsKey(name)) {
            return Optional.of(mem.get(name));
        }
        return Optional.empty();
    }

    @Override
    public Map<Reg, Value> current() {
        return cur;
    }

    @Override
    public void switchContext(String context) {
        currentContext = context;
        clear();
    }

    @Override
    public boolean active(IntermediateCode code, Value value) {
        return allocators.get(currentContext).active(code, value);
    }
}
