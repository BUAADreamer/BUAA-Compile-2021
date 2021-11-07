package frontend.stdir;

public class Exp extends IRCode {
    String op;
    Sym lsym;
    Sym rsym1;
    Sym rsym2;

    public Exp(String op, Sym lsym, Sym rsym1, Sym rsym2) {
        this.op = op;
        this.lsym = lsym;
        this.rsym1 = rsym1;
        this.rsym2 = rsym2;
    }

    @Override
    public String toString() {
        if (op.equals("=")) {
            return String.format("%s = %s\n", lsym, rsym1);
        }
        return String.format("%s = %s %s %s\n", lsym, rsym1, op, rsym2);
    }

    public Sym getLsym() {
        return lsym;
    }

    public Sym getRsym1() {
        return rsym1;
    }

    public Sym getRsym2() {
        return rsym2;
    }

    public String getOp() {
        return op;
    }
}
