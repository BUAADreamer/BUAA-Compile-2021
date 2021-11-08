package frontend.stdir;

public class CondBranch extends IRCode {
    Sym cmpa;
    Sym cmpb;
    String type;
    Label label;

    public CondBranch(Sym cmpa, Sym cmpb, String type, Label label) {
        this.cmpa = cmpa;
        this.cmpb = cmpb;
        this.type = type;
        this.label = label;
    }

    @Override
    public String toString() {
        return String.format("cmp %s, %s\n%s %s\n", cmpa, cmpb, type, label.getName());
    }

    public String getType() {
        return type;
    }

    public Sym getCmpa() {
        return cmpa;
    }

    public Sym getCmpb() {
        return cmpb;
    }

    public Label getLabel() {
        return label;
    }
}
