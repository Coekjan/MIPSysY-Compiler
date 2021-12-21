package backend;

import midend.IntermediateCode;
import midend.Value;
import utils.Pair;

import java.util.*;

public interface RegScheduler {
    List<Reg> regs = Arrays.asList(
            Reg.T0, Reg.T1, Reg.T2, Reg.T3, Reg.T4, Reg.T5, Reg.T6, Reg.T7, Reg.T8, Reg.T9, Reg.S12, Reg.S13,
            Reg.S0, Reg.S1, Reg.S2, Reg.S3, Reg.S4, Reg.S5, Reg.S6, Reg.S7, Reg.S8, Reg.S9, Reg.S10, Reg.S11, Reg.S14
    );
    /**
     * @return first: var-name, second: register-name
     */
    Pair<Value, Reg> overflow(IntermediateCode code, Collection<Reg> holdRegs);

    /**
     * @param name var-name
     * @param holdRegs reg-ignored
     * @return <code>Optional.empty()</code> iff full,
     * <code>Optional.of(reg-name)</code> iff not full
     */
    Optional<Reg> allocReg(Value name, Collection<Reg> holdRegs);

    /**
     * remove the usage of the specified register
     * @param reg the specified register
     */
    void remove(Reg reg);

    /**
     * clear all registers in use
     */
    void clear();

    /**
     * find the register already mapped to <code>name</code>
     * @param name variable name
     * @return if found, return <code>Optional.of(reg)</code>; else,
     * return <code>Optional.empty()</code>
     */
    Optional<Reg> find(Value name);

    /**
     * get current mapping between registers and variables
     * @return map between registers and variables
     */
    Map<Reg, Value> current();

    /**
     * switch context to the specified context
     * @param context the specified context
     */
    void switchContext(String context);

    /**
     * tell whether the variable <code>value</code> is active(liveness
     * analysis) at <code>code</code>
     * @param code the specified intermediate code
     * @param value the variable name
     * @return if the variable <code>value</code> is active(liveness
     * analysis) at <code>code</code>, return <code>true</code>;
     * else, return <code>false</code>
     */
    boolean active(IntermediateCode code, Value value);

    /**
     * tell whether the register is used as global register
     * @param reg the specified registers
     * @return if the register is used as global register, return
     * <code>true</code>; else, return <code>false</code>
     */
    boolean isGlobalReg(Reg reg);
}
