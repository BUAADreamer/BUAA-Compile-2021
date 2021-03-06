package frontend.stdir;

import frontend.irgen.symtable.Var;

import java.util.ArrayList;

public class ArrayDecl extends IRCode {
    Sym sym;
    ArrayList<Sym> lenval;
    ArrayList<Sym> arrayval;
    Boolean hasInitVal;

    public ArrayDecl(Sym sym, ArrayList<Sym> arrayval, ArrayList<Sym> lenval, Boolean hasInitVal) {
        this.sym = sym;
        this.arrayval = arrayval;
        this.lenval = lenval;
        this.hasInitVal = hasInitVal;
    }

    private String arraydecl() {
        StringBuilder res = new StringBuilder("");
        res.append(sym);
        for (int i = 0; i < lenval.size(); i++) {
            res.append("[" + lenval.get(i) + "]");
        }
        return res.toString();
    }

    @Override
    public String toString() {
        StringBuilder res = new StringBuilder("");
        if (((Var) sym.getSymbol()).isIsconst()) res.append("const ");
        if (hasInitVal) {
            if (arrayval.size() == 0) {
                res.append(String.format("arr int %s\n", arraydecl()));
            } else {
                res.append(String.format("arr int %s\n", ((Var) sym.getSymbol()).toString1()));
                for (int i = 0; i < arrayval.size(); i++) {
                    res.append(String.format("%s[%d] = %s\n", sym, i, arrayval.get(i)));
                }
            }
        } else {
            if (arrayval.size() == 0) {
                res.append(String.format("arr int %s\n", arraydecl()));
            } else {
                res.append(String.format("arr int %s\n", ((Var) sym.getSymbol()).toString1()));
            }
        }
        return res.toString();
    }

    public Sym getSym() {
        return sym;
    }

    public Sym getSym1() {
        return new Sym(sym.toString(), true);
    }

    public ArrayList<Sym> getArrayval() {
        return arrayval;
    }

    public ArrayList<Sym> getLenval() {
        return lenval;
    }

    public int getSpace() {
        if (lenval.size() == 1) {
            return lenval.get(0).getValue() * 4;
        } else {
            return lenval.get(1).getValue() * lenval.get(0).getValue() * 4;
        }
    }

    public Boolean getHasInitVal() {
        return hasInitVal;
    }

    public void setArrayval(ArrayList<Sym> arrayval) {
        this.arrayval = arrayval;
    }

    public void setHasInitVal(Boolean hasInitVal) {
        this.hasInitVal = hasInitVal;
    }

}
