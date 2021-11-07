package frontend.stdir;

import frontend.irgen.symtable.Func;

import java.util.ArrayList;

public class FuncCall extends IRCode {
    private Sym lsym;
    private Func func;
    private ArrayList<Sym> params;

    public FuncCall(Sym lsym, Func func, ArrayList<Sym> params) {
        this.lsym = lsym;
        this.func = func;
        this.params = params;
    }

    @Override
    public String toString() {
        StringBuilder res = new StringBuilder("");
        for (Sym sym : params) {
            if (sym.getType() == 3) {
                for (Sym rparam : sym.getParams()) {
                    res.append("push " + rparam + "\n");
                }
            } else res.append("push " + sym + "\n");
        }
        res.append("call " + func.getName() + "\n");
        res.append(lsym + " = RET\n");
        return res.toString();
    }
}
