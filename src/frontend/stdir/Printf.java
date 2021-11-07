package frontend.stdir;

import java.util.ArrayList;

public class Printf extends IRCode {
    ArrayList<Sym> syms;
    Sym sym;

    public Printf(ArrayList<Sym> syms, Sym sym) {
        this.syms = syms;
        this.sym = sym;
    }

    private String syms2str() {
        StringBuilder res = new StringBuilder("");
        for (int i = 0; i < syms.size(); i++) {
            res.append(syms.get(i));
            if (i != syms.size() - 1) res.append("-");
        }
        return res.toString();
    }

    @Override
    public String toString() {
        if (syms.size() > 0) {
            return String.format("printf %s %s\n", sym, syms2str());
        }
        return String.format("printf %s\n", sym);
    }

    public Sym getSym() {
        return sym;
    }

    public ArrayList<Sym> getSyms() {
        return syms;
    }
}
