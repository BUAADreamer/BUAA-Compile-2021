package frontend.llvmir;

public class BRIR extends LlvmIRCode {
    private Namespace judge;
    private Namespace cond;
    private String type;
    private Namespace label;

    //b type
    public BRIR(Namespace judge, Namespace cond, String type, Namespace label) {
        this.judge = judge;
        this.cond = cond;
        this.type = type;
        this.label = label;
    }

    //j type
    public BRIR(String type, Namespace label) {
        this.type = type;
        this.label = label;
    }


    public Namespace getLabel() {
        return label;
    }

    @Override
    public String toString() {
        if (type.equals("j")) return "jump " + label;
        else if (type.equals("ret")) return "ret " + label;
        return type + " " + judge + ", " + cond + ", " + label;
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
