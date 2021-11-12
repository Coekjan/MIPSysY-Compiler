package midend;

import utils.Pair;

import java.util.*;

public class IntermediateVirtualMachine {
    private final List<Map<String, Integer>> varStack = new LinkedList<>();
    private final List<Map<String, Pair<Integer, Integer>>> arrStack = new LinkedList<>();
    private final Stack<IntermediateCode> callReturnStack = new Stack<>();
    private final Stack<Integer> argStack = new Stack<>();
    private final int[] arrayMem = new int[0x10000];
    private int memPeak = -1;
    private int returnValue = 0;
    private final IntermediateCode head;
    private final Scanner scanner = new Scanner(System.in);

    public IntermediateVirtualMachine(IntermediateCode head) {
        this.head = head;
    }

    public void run(LabelTable lt) {
        IntermediateCode p = head;
        varStack.add(new HashMap<>());
        arrStack.add(new HashMap<>());
        while (!(p instanceof Exit)) {
            p = p.execute(this, lt);
        }
    }

    int getInt() {
        return scanner.nextInt();
    }

    int findVar(String name) {
        if (name.equals(Return.RET_SYM)) return returnValue;
        if (varStack.get(0).containsKey(name)) {
            return varStack.get(0).get(name);
        }
        return varStack.get(varStack.size() - 1).get(name);
    }

    void updateVar(String name, int val) {
        if (varStack.get(0).containsKey(name)) {
            varStack.get(0).put(name, val);
        } else {
            varStack.get(varStack.size() - 1).put(name, val);
        }
    }

    int load(int address) {
        return arrayMem[address];
    }

    void save(int address, int value) {
        arrayMem[address] = value;
    }

    void createVar(String name) {
        varStack.get(0).put(name, 0);
    }

    void createArr(String name, int size) {
        final int low = memPeak + 1;
        final int high = low + size - 1;
        memPeak = high;
        arrStack.get(0).put(name, Pair.of(low, high));
        varStack.get(0).put(name, low);
    }

    void pushArg(int value) {
        argStack.push(value);
    }

    int popArg() {
        return argStack.pop();
    }

    void callFuncWithLink(IntermediateCode next) {
        callReturnStack.push(next);
        varStack.add(0, new HashMap<>());
        arrStack.add(0, new HashMap<>());
    }

    IntermediateCode returnWithValue(int value) {
        returnValue = value;
        varStack.remove(0);
        final Collection<Pair<Integer, Integer>> space = arrStack.remove(0).values();
        memPeak = space.stream().map(p -> p.first).min(Integer::compare).orElse(memPeak + 1) - 1;
        return callReturnStack.pop();
    }

    public void setReturnValue(int returnValue) {
        this.returnValue = returnValue;
    }
}
