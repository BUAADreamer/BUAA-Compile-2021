package backend.mips;

public class Calculate extends MipsCode {
    Namespace reg1;
    Namespace reg2;
    Namespace reg3;
    Namespace value;
    String op;
    int type; //0 I型指令 1 R型指令

    /**
     * @param reg1 left exp
     * @param reg2 right exp1
     * @param reg3 right exp2 or imnumber
     * @param op   oprand
     */
    public Calculate(Namespace reg1, Namespace reg2, Namespace reg3, String op) {
        this.reg1 = reg1;
        this.reg2 = reg2;
        this.reg3 = reg3;
        this.type = 1;
        if (this.reg3 != null && this.reg3.getType() == 1) {
            this.type = 0;
            this.value = this.reg3;
        }
        this.op = transop(op);
    }

    private String transop(String op) {
        if (type == 0) {
            switch (op) {
                case "+":
                    return "addi";
                case "-":
                    return "subi";
                case "slti":
                    return "slti";
                case "sltiu":
                    return "sltiu";
                case "sll":
                    return "sll";
                default:
                    return op;
            }
        } else {
            if (op.equals("+")) {
                return "add";
            } else if (op.equals("-")) {
                return "sub";
            } else if (op.equals("/") || op.equals("%")) {
                return "div";
            } else if (op.equals("*")) {
                return "mult";
            } else if (op.equals("slti") || op.equals("sltiu")) {
                return "slt";
            }
        }
        return "";
    }

    @Override
    public String toString() {
        if (op.equals("div") || op.equals("mult")) {
            return String.format("%s %s %s\n", op, reg2, reg3);
        }
        if (type == 0) {
            return String.format("%s %s %s %s\n", op, reg1, reg2, value);
        } else {
            return String.format("%s %s %s %s\n", op, reg1, reg2, reg3);
        }
    }
}
