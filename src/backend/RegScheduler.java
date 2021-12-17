package backend;

import midend.IntermediateCode;
import midend.Value;
import utils.Pair;

import java.util.*;

public interface RegScheduler {
    List<Reg> regs = Arrays.asList(
            Reg.T0, Reg.T1, Reg.T2, Reg.T3, Reg.T4, Reg.T5, Reg.T6, Reg.T7, Reg.T8, Reg.T9, Reg.T10, Reg.T11,
            Reg.S0, Reg.S1, Reg.S2, Reg.S3, Reg.S4, Reg.S5, Reg.S6, Reg.S7, Reg.S8, Reg.S9, Reg.S10, Reg.S11
    );
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

    void switchContext(String context);

    boolean active(IntermediateCode code, Value value);
}
