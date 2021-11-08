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
            res.append("push " + sym + "\n");
        }
        res.append("call " + func.getName() + "\n");
        if (func.getType().equals("int")) res.append(lsym + " = RET\n");
        return res.toString();
    }

    public Sym getLsym() {
        return lsym;
    }

    public Func getFunc() {
        return func;
    }

    public ArrayList<Sym> getParams() {
        return params;
    }
}
