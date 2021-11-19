package backend;

import java.util.*;

public class TagTable {
    private final Map<String, MIPSCode> tagsMap = new HashMap<>();
    private final Map<MIPSCode, List<String>> codeMap = new HashMap<>();

    public MIPSCode find(String tag) {
        if (tagsMap.containsKey(tag)) return tagsMap.get(tag);
        throw new IllegalArgumentException();
    }

    public Optional<List<String>> find(MIPSCode code) {
        if (codeMap.containsKey(code)) return Optional.of(codeMap.get(code));
        else return Optional.empty();
    }

    public void assign(String tag, MIPSCode code) {
        tagsMap.put(tag, code);
        if (!codeMap.containsKey(code)) {
            codeMap.put(code, new LinkedList<>());
        }
        codeMap.get(code).add(0, tag);
    }
}
