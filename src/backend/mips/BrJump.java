package backend.mips;

public class BrJump extends MipsCode {
    Namespace reg1;
    Namespace reg2;
    Namespace label;
    String jbcode;
    int type; //0 j 1 b

    public BrJump(Namespace reg1, Namespace reg2, Namespace label, String jbcode) {
        this.reg1 = reg1;
        this.reg2 = reg2;
        this.label = label;
        this.jbcode = jbcode;
        this.type = 1;
    }

    public BrJump(Namespace label, String jbcode) {
        this.label = label;
        this.jbcode = jbcode;
        this.type = 0;
    }


    @Override
    public String toString() {
        if (type == 0) {
            return String.format("%s %s\n", jbcode, label);
        } else {
            return String.format("%s %s %s %s\n", jbcode, reg1, reg2, label);
        }
    }

    public Namespace getReg1() {
        return reg1;
    }

    public Namespace getReg2() {
        return reg2;
    }
}
