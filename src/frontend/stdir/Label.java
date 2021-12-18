package frontend.stdir;

import java.util.Objects;

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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Label label = (Label) o;
        return Objects.equals(name, label.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name);
    }
}
