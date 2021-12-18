package frontend.stdir;

import frontend.irgen.symtable.SymTable;
import frontend.irgen.symtable.Var;

import java.util.ArrayList;

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
            if (index1.getType() == 0 && index2.getType() == 0) {
                type = 0;
                index1 = new Sym(index2.getValue() + index1.getValue() * array.getN2());
                res.append(String.format("%s[%s] = %s\n",
                        array, index1, rsym));
                return res.toString();
            }
            res.append(String.format("%s = %s * %s\n", tmp1, index1, array.getN2()));
            res.append(String.format("%s = %s + %s\n", tmp2, index2, tmp1));
            res.append(String.format("%s[%s] = %s\n", array, tmp2, rsym));
        } else if (type == 2) {
            res.append(String.format("%s = %s[%s]\n", lsym, array, index1));
        } else {
            if (index1.getType() == 0 && index2.getType() == 0) {
                type = 2;
                index1 = new Sym(index2.getValue() + index1.getValue() * array.getN2());
                res.append(String.format("%s = %s[%s]\n",
                        lsym, array, index1));
                return res.toString();
            }
            res.append(String.format("%s = %s * %s\n", tmp1, index1, array.getN2()));
            res.append(String.format("%s = %s + %s\n", tmp2, index2, tmp1));
            res.append(String.format("%s = %s[%s]\n", lsym, array, tmp2));
        }
        return res.toString();
    }

    public ArrayList<IRCode> trans() {
        ArrayList<IRCode> ans = new ArrayList<>();
        if (type == 1 || type == 3) {
            ans.add(new Exp("*", tmp1, index1, new Sym(array.getN2())));
            ans.add(new Exp("+", tmp2, index2, tmp1));
            if (type == 1) {
                type = 0;
                index1 = tmp2;
            } else if (type == 3) {
                type = 2;
                index1 = tmp2;
            }
        }
        return ans;
    }

    public int getType() {
        return type;
    }

    public Sym getLsym() {
        return lsym;
    }

    public Sym getRsym() {
        return rsym;
    }

    public Sym getTmp1() {
        return tmp1;
    }

    public Sym getTmp2() {
        return tmp2;
    }

    public Sym getIndex1() {
        return index1;
    }

    public Sym getIndex2() {
        return index2;
    }

    public Var getArray() {
        return array;
    }

    public Sym getArraySym() {
        return new Sym(array, index1);
    }

    public Sym getArraySym1() {
        return new Sym(array.toString(), true);
    }

    public Sym getArraySym2() {
        return new Sym(array.toString(), index1.toString());
    }
}
