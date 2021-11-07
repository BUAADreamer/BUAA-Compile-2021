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


}
