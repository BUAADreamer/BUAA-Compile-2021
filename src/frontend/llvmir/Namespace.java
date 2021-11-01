package frontend.llvmir;

public class Namespace {
    private int identtype; // name or reg
    private int regNum;
    private String name;
    private int type; //% or @

    public Namespace(int regNum, int type) {
        this.regNum = regNum;
        this.type = type;
        this.identtype = 1;
    }

    public int getIdenttype() {
        return identtype;
    }

    public int getRegNum() {
        return regNum;
    }

    public void setRegNum(int regNum) {
        this.regNum = regNum;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setType(int type) {
        if (type == 2) identtype = 0;
        this.type = type;
    }

    public String getName() {
        return name;
    }

    public int getType() {
        return type;
    }

    public Namespace(String name, int type) {
        this.name = name;
        this.type = type;
        this.identtype = 0;
    }

    @Override
    public String toString() {
        String ans = "";
        if (type == 0) ans += "%";
        else if (type == 1) ans += "@";
        else if (type == 3) ans += "#"; //label
        if (identtype == 0) ans += name;
        else ans += regNum;
        return ans;
    }
}
