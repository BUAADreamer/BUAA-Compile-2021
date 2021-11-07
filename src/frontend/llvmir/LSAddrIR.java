package frontend.llvmir;

public class LSAddrIR extends LlvmIRCode {

    private String type; //0 load 1 store 2 allocate
    Namespace namespace1;
    Namespace namespace2;
    Namespace namespace3;

    public LSAddrIR(String type, Namespace namespace1, Namespace namespace2) {
        this.type = type;
        this.namespace1 = namespace1;
        this.namespace2 = namespace2;
    }

    public LSAddrIR(String type, Namespace namespace1, Namespace namespace2, Namespace namespace3) {
        this.type = type;
        this.namespace1 = namespace1;
        this.namespace2 = namespace2;
        this.namespace3 = namespace3;
    }

    @Override
    public String toString() {
        if (type.equals("2")) {
            return namespace1.toString() + " = alloca " + namespace2.toString(); //%1 = alloca %2
        } else if (type.equals("0")) {
            return namespace1.toString() + " = load *" + namespace2.toString() + ", " + namespace3; //%1 = load *%2, %3
        } else {
            return "store " + namespace1 + ", *" + namespace2 + ", " + namespace3; //store %1, *%2, %3
        }
    }

    @Override
    public String getType() {
        return type;
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
