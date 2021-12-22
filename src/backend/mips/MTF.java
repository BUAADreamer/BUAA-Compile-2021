package backend.mips;

public class MTF extends MipsCode {
    Namespace reg;
    String type; //0 mthi 1 mtlo 2 mfhi 3 mflo

    public MTF(Namespace reg, String type) {
        this.reg = reg;
        this.type = type;
    }

    @Override
    public String toString() {
        return String.format("%s %s\n", type, reg);
    }

    public Namespace getReg() {
        return reg;
    }

    public String getType() {
        return type;
    }

    public int getTransType() {
        if (type.equals("mthi") || type.equals("mtlo")) return 0;
        else return 1;
    }
}
