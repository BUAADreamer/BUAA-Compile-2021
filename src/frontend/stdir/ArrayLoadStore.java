package frontend.stdir;

import frontend.irgen.symtable.SymTable;
import frontend.irgen.symtable.Var;

public class ArrayLoadStore extends IRCode {
    Sym lsym;
    Sym rsym;
    Sym tmp1;
    Sym tmp2;
    Sym index1;
    Sym index2;
    Var array;
    int type; //0 store int[] 1 store int[][] 2 load int[] 3 load int[][]

    public ArrayLoadStore(Sym sym, Sym index1, SymTable array, int type) {
        this.index1 = index1;
        this.array = (Var) array;
        this.type = type;
        if (type == 0) {
            rsym = sym;
        } else {
            lsym = sym;
        }
    }

    public ArrayLoadStore(Sym sym, Sym tmp1, Sym tmp2, Sym index1, Sym index2, SymTable array, int type) {
        this.tmp1 = tmp1;
        this.tmp2 = tmp2;
        this.index1 = index1;
        this.index2 = index2;
        this.array = (Var) array;
        this.type = type;
        if (type == 1) {
            rsym = sym;
        } else {
            lsym = sym;
        }
    }

    @Override
    public String toString() {
        StringBuilder res = new StringBuilder("");
        if (type == 0) {
            res.append(String.format("%s[%s] = %s\n", array, index1, rsym));
        } else if (type == 1) {
            res.append(String.format("%s = %s * %s\n", tmp1, index1, array.getN2()));
            res.append(String.format("%s = %s + %s\n", tmp2, index2, tmp1));
            res.append(String.format("%s[%s] = %s\n", array, tmp2, rsym));
        } else if (type == 2) {
            res.append(String.format("%s = %s[%s]\n", lsym, array, index1));
        } else {
            res.append(String.format("%s = %s * %s\n", tmp1, index1, array.getN2()));
            res.append(String.format("%s = %s + %s\n", tmp2, index2, tmp1));
            res.append(String.format("%s = %s[%s]\n", lsym, array, index1));
        }
        return res.toString();
    }
}
