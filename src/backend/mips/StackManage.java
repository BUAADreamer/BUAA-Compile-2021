package backend.mips;

public class StackManage extends MipsCode {
    Namespace reg;
    int type;
    int offset;

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

    public StackManage(int type, int offset) {
        this.type = type;
        this.offset = offset;
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
        } else if (type == 4) {
            return "subi $sp $sp " + offset + "\n";
        } else if (type == 5) {
            return "addi $sp $sp " + offset + "\n";
        }
        return null;
    }
}
