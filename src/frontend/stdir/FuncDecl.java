package frontend.stdir;

import frontend.irgen.symtable.Func;
import frontend.irgen.symtable.SymTable;

import java.util.ArrayList;

public class FuncDecl extends IRCode {
    private Func func; //function sym
    private int type; //0 void 1 int

    public FuncDecl(Func func, int type) {
        this.func = func;
        this.type = type;
    }

    @Override
    public String toString() {
        StringBuilder res = new StringBuilder("");
        res.append(type == 0 ? "void " : "int ");
        res.append(func.getName() + "()\n");
        return res.toString();
    }

    public Func getFunc() {
        return func;
    }
}
