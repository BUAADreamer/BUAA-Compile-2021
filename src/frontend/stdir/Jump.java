package frontend.stdir;

public class Jump extends IRCode {
    Label label;

    public Jump(Label label) {
        this.label = label;
    }

    @Override
    public String toString() {
        return String.format("goto %s\n", label.getName());
    }

    public Label getLabel() {
        return label;
    }

    public void setLabel(Label label) {
        this.label = label;
    }
}

