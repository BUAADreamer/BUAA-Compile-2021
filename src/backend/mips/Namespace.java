package backend.mips;

import java.util.Objects;

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
            return String.format("0x%x", value);
        } else if (type == 2) {
            return label;
        }
        return "";
    }

    /**
     * @return 0:reg 1:value
     */
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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Namespace namespace = (Namespace) o;
        return type == namespace.type && reg == namespace.reg && value == namespace.value && Objects.equals(label, namespace.label);
    }

    @Override
    public int hashCode() {
        return Objects.hash(type, reg, value, label);
    }

    public boolean isGlobal() {
        return reg >= 16 && reg <= 23;
    }
}
