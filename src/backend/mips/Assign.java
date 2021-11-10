package backend.mips;

public class Assign extends MipsCode {
    int type; //0 li 1 move
    Namespace sym1;
    Namespace sym2;
    int value;

    public Assign(Namespace sym1, int value) {
        this.sym1 = sym1;
        this.value = value;
        this.type = 0;
    }

    public Assign(Namespace sym1, Namespace sym2) {
        this.sym1 = sym1;
        this.sym2 = sym2;
        this.type = 1;
        if (sym2 != null && sym2.getType() == 1) {
            this.type = 0;
            this.value = sym2.getValue();
        }
    }

    @Override
    public String toString() {
        if (type == 0) {
            return String.format("li %s 0x%x\n", sym1, value);
        } else if (type == 1) {
            return String.format("move %s %s\n", sym1, sym2);
        }
        return "";
    }
}
