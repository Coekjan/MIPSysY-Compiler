package midend;

public class Print extends IntermediateCode {
    public final String string;

    public Print(String string) {
        this.string = string;
    }

    @Override
    String display() {
        return "printf " + string;
    }

    @Override
    IntermediateCode execute(IntermediateVirtualMachine machine, LabelTable labelTable) {
        char[] str = string.toCharArray();
        for (int i = 1; i < str.length - 1; ++i) {
            if (str[i] == '%' && i < str.length - 2 && str[i + 1] == 'd') {
                System.out.print(machine.popArg());
                ++i;
            } else if (str[i] == '\\' && i < str.length - 2 && str[i + 1] == 'n') {
                System.out.print('\n');
                ++i;
            } else {
                System.out.print(str[i]);
            }
        }
        return next;
    }
}
