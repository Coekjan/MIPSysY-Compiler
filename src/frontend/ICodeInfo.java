package frontend;

import midend.IntermediateCode;
import midend.Value;
import utils.Pair;

public class ICodeInfo extends Pair<IntermediateCode, IntermediateCode> {
    public final Value finalSym;
    public final int tempCount;

    public ICodeInfo(IntermediateCode first, IntermediateCode second, Value finalSym, int tempCount) {
        super(first, second);
        this.finalSym = finalSym;
        this.tempCount = tempCount;
    }
}
