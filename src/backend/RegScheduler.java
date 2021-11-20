package backend;

import midend.Value;
import utils.Pair;

import java.util.*;

public interface RegScheduler {
    /**
     * @return first: var-name, second: register-name
     */
    Pair<Value, Reg> overflow(Collection<Reg> holdRegs);

    /**
     * @param name var-name
     * @param holdRegs reg-ignored
     * @return <code>Optional.empty()</code> iff full, <code>Optional.of(reg-name)</code> iff not full
     */
    Optional<Reg> allocReg(Value name, Collection<Reg> holdRegs);

    void remove(Reg reg);

    void clear();

    Optional<Reg> find(Value name);

    Map<Reg, Value> current();
}
