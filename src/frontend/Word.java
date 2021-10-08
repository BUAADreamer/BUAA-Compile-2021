package frontend;

public class Word {
    private String typeCode;
    private String content;
    private int line;

    public Word(String typeCode, String content, int line) {
        this.typeCode = typeCode;
        this.content = content;
        this.line = line;
    }

    public String getTypeCode() {
        return typeCode;
    }

    public String getContent() {
        return content;
    }

    public int getLine() {
        return line;
    }

    public String getOutPut() {
        return typeCode + " " + content + "\n";
    }

    @Override
    public String toString() {
        return "Word{" +
                "typeCode='" + typeCode + '\'' +
                ", content='" + content + '\'' +
                ", line=" + line +
                '}';
    }
}
