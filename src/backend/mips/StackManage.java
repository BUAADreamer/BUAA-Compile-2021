package backend.mips;

public class StackManage extends MipsCode {
    Namespace reg;
    int type;

    /**
     * @param reg
     * @param type 0:store to stack 1:load to reg 2:stack-=4 3:stack+=4
     */
    public StackManage(Namespace reg, int type) {
        this.reg = reg;
        this.type = type;
    }

    public StackManage(int type) {
        this.type = type;
    }

    @Override
    public String toString() {
        if (type == 0) {
            return String.format("sw %s 0($sp)\n", reg);
        } else if (type == 1) {
            return String.format("lw %s 0($sp)\n", reg);
        } else if (type == 2) {
            return "subi $sp $sp 4\n";
        } else if (type == 3) {
            return "addi $sp $sp 4\n";
        }
        return null;
    }
}
