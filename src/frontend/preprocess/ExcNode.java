package frontend.preprocess;

public class ExcNode {
    private int line;

    public int getLine() {
        return line;
    }

    public String getExccode() {
        return exccode;
    }

    public String getInfo() {
        return info;
    }

    private String exccode;
    private String info;

    public ExcNode(int line, String exccode, String info) {
        this.line = line;
        this.exccode = exccode;
        this.info = info;
    }
}
