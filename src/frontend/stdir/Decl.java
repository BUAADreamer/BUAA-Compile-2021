package frontend.stdir;

public class Decl extends IRCode {
    private Sym sym;
    private Sym rsym;

    public Decl(Sym sym, Sym rsym) {
        this.sym = sym;
        this.rsym = rsym;
    }

    public Sym getRsym() {
        return rsym;
    }

    public Sym getSym() {
        return sym;
    }

    @Override
    public String toString() {
        if (sym.isconst()) {
            return String.format("const int %s = %s\n", sym, rsym);
        }
        return String.format("var int %s = %s\n", sym, rsym);
    }
}
