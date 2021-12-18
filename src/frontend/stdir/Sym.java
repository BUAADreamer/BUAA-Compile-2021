package frontend.stdir;

import frontend.irgen.symtable.SymTable;
import frontend.irgen.symtable.Var;

import java.util.ArrayList;
import java.util.Objects;

public class Sym {
    private int value;
    private int type;
    private String name;
    private SymTable symbol;
    private ArrayList<Sym> params;
    private Var arrayname;
    private Sym index;

    /**
     * constructor of number
     *
     * @param value
     */
    public Sym(int value) {
        this.type = 0;
        this.value = value;
    }

    /**
     * constructor of temp var
     *
     * @param name
     */
    public Sym(String name) {
        this.type = 1;
        this.name = name;
    }

    /**
     * constructor of temp var declined
     *
     * @param symbol
     */
    public Sym(SymTable symbol) {
        this.type = 2;
        this.symbol = symbol;
    }

    public Sym(Var arrayname, Sym index) {
        if (index == null) {
            this.type = 3;
        } else {
            this.type = 4;
        }
        this.arrayname = arrayname;
        this.index = index;
    }

    public Sym(String name, boolean flag) {
        this.type = 5;
        this.name = name;
    }

    public Sym(String name, String index) {
        this.type = 6;
        this.name = name + "[" + index + "]";
    }


    @Override
    public String toString() {
        if (type == 0) return String.valueOf(value);
        else if (type == 1) return name;
        else if (type == 2) return symbol.toString();
        else if (type == 3) return "&" + arrayname.toString();
        else if (type == 4) return String.format("&%s[%s]", arrayname, index);
        else if (type == 5) return name;
        else if (type == 6) return name;
        return "";
    }

    public String getTypeOut() {
        return ((Var) symbol).getTypeOut();
    }

    public Boolean isconst() {
        return ((Var) symbol).isIsconst();
    }

    public SymTable getSymbol() {
        return symbol;
    }

    public int getType() {
        return type;
    }

    public ArrayList<Sym> getParams() {
        return params;
    }

    public String getName() {
        return name;
    }

    public int getValue() {
        return value;
    }

    public void setValue(int value) {
        this.value = value;
    }

    public Sym getIndex() {
        return index;
    }

    public Var getArrayname() {
        return arrayname;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Sym sym = (Sym) o;
        return value == sym.value && type == sym.type && Objects.equals(name, sym.name) && Objects.equals(symbol, sym.symbol) && Objects.equals(params, sym.params) && Objects.equals(arrayname, sym.arrayname) && Objects.equals(index, sym.index) && toString().equals(sym.toString());
    }

    @Override
    public int hashCode() {
        return Objects.hash(value, type, name, symbol, params, arrayname, index, toString());
    }

    /**
     * @param sym
     * @return
     */
    public static Sym isVar(Sym sym) {
        if (sym == null) return null;
        String s = sym.toString();
        if (s.length() > 0 && (s.charAt(0) == 't' || s.charAt(0) == '%' || s.charAt(0) == '@')) {
            return sym;
        }
        if (s.length() > 0 && (s.charAt(0) == '&')) {
            return new Sym(sym.getArrayname().toString(), true);
        }
        return null;
    }
}
