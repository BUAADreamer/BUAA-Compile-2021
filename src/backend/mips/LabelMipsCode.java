package backend.mips;

public class LabelMipsCode extends MipsCode {
    String label;

    public LabelMipsCode(String label) {
        this.label = label;
    }

    @Override
    public String toString() {
        return label;
    }
}
