package frontend.llvmir;

public class CalcuIR extends IRCode {

    //%0 = add %X, %X

    //add sub mul shl srem
    private String type;
    private Namespace namespace;
    private Namespace lname;
    private Namespace rname;

    public CalcuIR(String type, Namespace namespace, Namespace lname, Namespace rname) {
        this.type = type;
        this.rname = rname;
        this.lname = lname;
        this.namespace = namespace;
    }

    @Override
    public String getType() {
        return type;
    }

    @Override
    public int getRegNum() {
        return namespace.getRegNum();
    }

    @Override
    public String getName() {
        return namespace.getName();
    }

    @Override
    public String toString() {
        return String.format("%s = %s %s, %s", namespace.toString(), type, lname.toString(), rname.toString());
    }
}
