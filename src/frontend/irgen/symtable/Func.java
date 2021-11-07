package frontend.irgen.symtable;

import java.util.ArrayList;

public class Func extends SymTable {
    private String name;
    private ArrayList<Var> params;
    private String type; //void int

    public Func(String name, ArrayList<Var> params, String type) {
        this.name = name;
        this.params = params;
        this.type = type;
    }

    @Override
    public String getName() {
        return name;
    }


}
