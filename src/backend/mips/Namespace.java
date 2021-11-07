package backend.mips;

public class Namespace {
    int type; //0 reg 1 num 2 label
    int reg;
    int value;
    String label;

    /**
     * @param num
     * @param type 1-->number 0-->regNum
     */
    public Namespace(int num, int type) {
        if (type == 0) {
            this.reg = num;
        } else {
            this.value = num;
        }
        this.type = type;
    }

    public Namespace(String label) {
        this.label = label;
        this.type = 2;
    }

    @Override
    public String toString() {
        if (type == 0) {
            return "$" + reg;
        } else if (type == 1) {
            return String.valueOf(value);
        } else if (type == 2) {
            return label;
        }
        return "";
    }

    public int getType() {
        return type;
    }

    public int getReg() {
        return reg;
    }

    public int getValue() {
        return value;
    }

    public String getLabel() {
        return label;
    }
}
