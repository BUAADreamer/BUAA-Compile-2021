package frontend.llvmir;

import java.util.ArrayList;

public class IOIR extends IRCode {
    Namespace namespace;
    String type; // 0:getint 1:printf
    ArrayList<Namespace> namespaces;

    public IOIR(Namespace namespace, String type) {
        this.namespace = namespace;
        this.type = type;
    }

    public IOIR(Namespace namespace, ArrayList<Namespace> namespaces, String type) {
        this.namespace = namespace;
        this.namespaces = namespaces;
        this.type = type;
    }

    @Override
    public String toString() {
        if (type.equals("0")) return namespace.toString() + " = getint()";
        else return "printf " + namespace.toString() + ", " + namespaces;
    }

    @Override
    public String getType() {
        return null;
    }

    @Override
    public int getRegNum() {
        return 0;
    }

    @Override
    public String getName() {
        return null;
    }
}
