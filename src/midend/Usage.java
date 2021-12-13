package midend;

import java.util.List;

public interface Usage<T extends IntermediateCode> {
    List<Value> getUse();

    T replaceUse(List<Value> uses);
}
