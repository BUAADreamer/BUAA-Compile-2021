package frontend.stdir;

public class FuncRet extends IRCode {
    Sym sym;

    public FuncRet(Sym sym) {
        this.sym = sym;
    }

    @Override
    public String toString() {
        if (sym == null) return "ret\n";
        return "ret " + sym + "\n";
    }

    public Sym getSym() {
        return sym;
    }
}
