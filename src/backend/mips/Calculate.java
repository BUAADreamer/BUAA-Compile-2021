package backend.mips;

public class Calculate extends MipsCode {
    Namespace reg1;
    Namespace reg2;
    Namespace reg3;
    Namespace value;
    String op;
    int type; //0 I型指令 1 R型指令


    public Calculate(Namespace reg1, Namespace reg2, Namespace reg3, String op) {
        this.reg1 = reg1;
        this.reg2 = reg2;
        this.reg3 = reg3;
        if (reg2.getType() == 1) {
            this.reg3 = reg2;
            this.reg2 = reg3;
        }
        this.type = 1;
        if (reg3.getType() == 1) {
            this.type = 0;
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
            }
        } else {
            if (op.equals("+")) {
                return "add";
            } else if (op.equals("-")) {
                return "sub";
            } else if (op.equals("/") || op.equals("%")) {
                return "div";
            } else {
                return "mult";
            }
        }
        return "";
    }

    @Override
    public String toString() {
        if (op.equals("div") || op.equals("mult")) {
            return String.format("%s %s %s", op, reg1, reg2);
        }
        if (type == 0) {
            return String.format("%s %s %s %s", op, reg1, reg2, value);
        } else {
            return String.format("%s %s %s %s", op, reg1, reg2, reg3);
        }
    }
}
