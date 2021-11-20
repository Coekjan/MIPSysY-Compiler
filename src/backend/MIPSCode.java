package backend;

import midend.AssignBinaryOperation;
import utils.LinkedNode;

public abstract class MIPSCode extends LinkedNode<MIPSCode> {
    abstract String stringify();

    @Override
    public String toString() {
        return stringify();
    }

    public static class Comment extends MIPSCode {
        public final String s;

        public Comment(String s) {
            this.s = s;
        }

        @Override
        String stringify() {
            return "# " + s;
        }
    }

    public static class NopCode extends MIPSCode {
        @Override
        String stringify() {
            return "nop";
        }
    }

    public static class LoadImmCode extends MIPSCode {
        public final Reg reg;
        public final Imm imm;

        public LoadImmCode(Reg reg, Imm imm) {
            this.reg = reg;
            this.imm = imm;
        }

        @Override
        String stringify() {
            return "li " + reg + ", " + imm;
        }
    }

    public static class BinaryRegImmCode extends MIPSCode {
        public enum Op {
            ADDIU, SUBIU, MUL, DIV, REM, ANDI, ORI, SGT, SGE, SLTI, SLE, SEQ, SNE, SLL;

            @Override
            public String toString() {
                return name().toLowerCase();
            }

            public static Op fromBinary(AssignBinaryOperation.BinaryOperation op) {
                switch (op) {
                    case ADD: return ADDIU;
                    case SUB: return SUBIU;
                    case MUL: return MUL;
                    case DIV: return DIV;
                    case MOD: return REM;
                    case AND: return ANDI;
                    case OR: return ORI;
                    case GT: return SGT;
                    case GE: return SGE;
                    case LT: return SLTI;
                    case LE: return SLE;
                    case EQ: return SEQ;
                    case NE: return SNE;
                }
                throw new IllegalArgumentException();
            }
        }

        public final Op op;
        public final Reg target;
        public final Reg source;
        public final Imm imm;

        public BinaryRegImmCode(Op op, Reg target, Reg source, Imm imm) {
            this.op = op;
            this.target = target;
            this.source = source;
            this.imm = imm;
        }

        @Override
        String stringify() {
            return op.toString() + " " + target + ", " + source + ", " + imm;
        }
    }

    public static class BinaryRegRegCode extends MIPSCode {
        public enum Op {
            ADDU, SUBU, MUL, DIV, REM, AND, OR, SGT, SGE, SLT, SLE, SEQ, SNE;

            @Override
            public String toString() {
                return name().toLowerCase();
            }

            public static BinaryRegRegCode.Op fromBinary(AssignBinaryOperation.BinaryOperation op) {
                switch (op) {
                    case ADD: return ADDU;
                    case SUB: return SUBU;
                    case MUL: return MUL;
                    case DIV: return DIV;
                    case MOD: return REM;
                    case AND: return AND;
                    case OR: return OR;
                    case GT: return SGT;
                    case GE: return SGE;
                    case LT: return SLT;
                    case LE: return SLE;
                    case EQ: return SEQ;
                    case NE: return SNE;
                }
                throw new IllegalArgumentException();
            }
        }

        public final Op op;
        public final Reg dest;
        public final Reg source;
        public final Reg target;

        public BinaryRegRegCode(Op op, Reg dest, Reg source, Reg target) {
            this.op = op;
            this.dest = dest;
            this.source = source;
            this.target = target;
        }

        @Override
        String stringify() {
            return op.toString() + " " + dest + ", " + source + ", " + target;
        }
    }

    public static class LoadCode extends MIPSCode {
        public final Reg target;
        public final Address address;

        public LoadCode(Reg target, Address address) {
            this.target = target;
            this.address = address;
        }

        @Override
        String stringify() {
            return "lw " + target + ", " + address;
        }
    }

    public static class StoreCode extends MIPSCode {
        public final Reg target;
        public final Address address;

        public StoreCode(Reg target, Address address) {
            this.target = target;
            this.address = address;
        }

        @Override
        String stringify() {
            return "sw " + target + ", " + address;
        }
    }

    public static class BranchNotEq extends MIPSCode {
        public final Reg r1;
        public final Reg r2;
        public final String tag;

        public BranchNotEq(Reg r1, Reg r2, String tag) {
            this.r1 = r1;
            this.r2 = r2;
            this.tag = tag;
        }

        @Override
        String stringify() {
            return "bne " + r1 + ", " + r2 + ", " + tag;
        }
    }

    public static class JumpAndLink extends MIPSCode {
        public final String tag;

        public JumpAndLink(String tag) {
            this.tag = tag;
        }

        @Override
        String stringify() {
            return "jal " + tag;
        }
    }

    public static class JumpCode extends MIPSCode {
        public final String tag;

        public JumpCode(String tag) {
            this.tag = tag;
        }

        @Override
        String stringify() {
            return "j " + tag;
        }
    }

    public static class JumpReg extends MIPSCode {
        public final Reg reg;

        public JumpReg(Reg reg) {
            this.reg = reg;
        }

        @Override
        String stringify() {
            return "jr " + reg;
        }
    }

    public static class Syscall extends MIPSCode {
        @Override
        String stringify() {
            return "syscall";
        }
    }

    public static class LoadLabelAddressCode extends MIPSCode {
        public final Reg reg;
        public final String label;

        public LoadLabelAddressCode(Reg reg, String label) {
            this.reg = reg;
            this.label = label;
        }

        @Override
        String stringify() {
            return "la " + reg + ", " + label;
        }
    }

    public static class LoadAddressCode extends MIPSCode {
        public final Reg reg;
        public final Address address;

        public LoadAddressCode(Reg reg, Address address) {
            this.reg = reg;
            this.address = address;
        }

        @Override
        String stringify() {
            return "la " + reg + ", " + address;
        }
    }
}
