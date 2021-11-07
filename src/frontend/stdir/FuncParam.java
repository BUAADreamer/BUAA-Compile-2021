package frontend.stdir;

public class FuncParam extends IRCode {
    private Sym sym;

    public FuncParam(Sym sym) {
        this.sym = sym;
    }

    @Override
    public String toString() {
        return String.format("para %s %s\n", sym.getTypeOut(), sym);
    }
}
