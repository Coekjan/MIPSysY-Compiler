package midend;

import java.util.List;

public interface Assignment {
    Value left();

    List<Value> right();
}
