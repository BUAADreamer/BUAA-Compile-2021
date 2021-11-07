package frontend.stdir;

public class Label extends IRCode {
    private String name;

    public Label(String name) {
        this.name = name;
    }

    @Override
    public String toString() {
        return name + ":\n";
    }

    public String getName() {
        return name;
    }
}
