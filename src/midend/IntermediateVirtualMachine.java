package midend;

import utils.Pair;

import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class IntermediateVirtualMachine {
    private final List<Map<Value, Integer>> varStack = new LinkedList<>();
    private final List<Map<AddrValue, Pair<Integer, Integer>>> arrStack = new LinkedList<>();
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

    public List<Map<Value, List<Integer>>> getProgStack() {
        final List<Map<Value, List<Integer>>> var = varStack.stream()
                .map(m -> m.keySet().stream()
                        .filter(k -> k.symbol.length() > 1 && !Character.isDigit(k.symbol.charAt(1)))
                        .collect(Collectors.toMap(k -> k, k -> Collections.singletonList(m.get(k)))))
                .collect(Collectors.toList());
        final List<Map<Value, List<Integer>>> arr = arrStack.stream()
                .map(m -> m.keySet().stream()
                        .collect(Collectors.toMap(
                                k -> (Value) k,
                                k -> Arrays.stream(arrayMem).boxed().collect(Collectors.toList())
                                        .subList(m.get(k).first, m.get(k).second + 1))))
                .collect(Collectors.toList());
        return new ArrayList<Map<Value, List<Integer>>>(var) {{
            addAll(arr);
        }};
    }

    public void run(LabelTable lt) {
        run(lt, p -> false);
    }

    public void run(LabelTable lt, Predicate<IntermediateCode> exitCond) {
        IntermediateCode p = head;
        varStack.add(new HashMap<>());
        arrStack.add(new HashMap<>());
        while (!(p instanceof Exit)) {
            if (exitCond.test(p)) break;
            p = p.execute(this, lt);
        }
    }

    int getInt() {
        return scanner.nextInt();
    }

    int findVar(Value name) {
        if (name.symbol.equals(Return.RET_SYM)) return returnValue;
        if (varStack.get(0).containsKey(name)) {
            return varStack.get(0).get(name);
        }
        return varStack.get(varStack.size() - 1).get(name);
    }

    void updateVar(Value name, int val) {
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

    void createVar(Value name) {
        varStack.get(0).put(name, 0);
    }

    void createArr(AddrValue name, int size) {
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

    void setReturnValue(int returnValue) {
        this.returnValue = returnValue;
    }
}
