package midend;

import utils.Pair;

import java.util.Collections;
import java.util.List;

public class ParameterFetch extends IntermediateCode implements IntroSpace, Definite, Assignment, Usage<ParameterFetch> {
    public final Value name;

    public ParameterFetch(Value name) {
        this.name = name;
    }

    @Override
    String display() {
        return "para " + name.toString();
    }

    @Override
    IntermediateCode execute(IntermediateVirtualMachine machine, LabelTable labelTable) {
        machine.createVar(name);
        machine.updateVar(name, machine.popArg());
        return next;
    }

    @Override
    public Pair<Value, Integer> getSize() {
        return Pair.of(name, 1);
    }

    @Override
    public Value left() {
        return name;
    }

    @Override
    public List<Value> right() {
        return Collections.singletonList(name);
    }

    @Override
    public Value getDef() {
        return name;
    }

    @Override
    public List<Value> getUse() {
        return Collections.singletonList(name);
    }

    @Override
    public ParameterFetch replaceUse(List<Value> uses) {
        return new ParameterFetch(uses.get(0));
    }
}
