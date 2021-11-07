package frontend.irgen.symtable;

public class Var extends SymTable {
    int n1;//dim1
    int n2;//dim2
    boolean isconst;//是否是常量
    int type;//0:int 1:int[] 2:int[][]
    String name;
    int level; //0代表是全局变量 其他是局部变量
    int addr;

    public Var(int n1, boolean isconst, String name, int level) {
        this.n1 = n1;
        this.isconst = isconst;
        this.name = name;
        this.type = 1;
        this.level = level;
    }

    public Var(int n1, int n2, boolean isconst, String name, int level) {
        this.n1 = n1;
        this.n2 = n2;
        this.isconst = isconst;
        this.name = name;
        this.type = 2;
        this.level = level;
    }

    public Var(boolean isconst, String name, int level) {
        this.isconst = isconst;
        this.name = name;
        this.level = level;
        this.type = 0;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String toString() {
        StringBuilder res = new StringBuilder("");
        res.append(level == 0 ? "@" : "%");
        res.append(name);
        return res.toString();
    }

    public String toString1() {
        StringBuilder res = new StringBuilder("");
        res.append(toString());
        if (type == 1) {
            res.append(String.format("[%d]", n1));
        } else if (type == 2) {
            res.append(String.format("[%d][%d]", n1, n2));
        }
        return res.toString();
    }

    public String getTypeOut() {
        return type == 0 ? "int" : type == 1 ? "int[]" : "int[][]";
    }

    public boolean isIsconst() {
        return isconst;
    }

    public int getN1() {
        return n1;
    }

    public int getN2() {
        return n2;
    }

    public int getType() {
        return type;
    }

    public void setAddr(int addr) {
        this.addr = addr;
    }

    public int getAddr() {
        return addr;
    }
}
