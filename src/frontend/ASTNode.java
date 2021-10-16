package frontend;

import java.util.ArrayList;

public class ASTNode {
    private String type;
    private String name;
    private boolean isEnd;
    private ArrayList<ASTNode> astChildNodes;
    private ASTNode parentNode;
    private Word word;

    public ASTNode(String type, String name, boolean isEnd, Word word) {
        this.type = type;
        this.name = name;
        this.isEnd = isEnd;
        this.word = word;
        this.astChildNodes = new ArrayList<>();
    }

    public void addChildNode(ASTNode node) {
        this.astChildNodes.add(node);
    }

    public void addChildNodes(ArrayList<ASTNode> nodes) {
        this.astChildNodes.addAll(nodes);
    }

    public void setParentNode(ASTNode parentNode) {
        this.parentNode = parentNode;
    }

    public ASTNode getParentNode() {
        return parentNode;
    }

    @Override
    public String toString() {
        if (isEnd) {
            return type + name;
        } else {
            return type;
        }
    }
}
