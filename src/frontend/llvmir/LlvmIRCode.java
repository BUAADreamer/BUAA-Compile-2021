package frontend.llvmir;

public abstract class LlvmIRCode {
    public abstract String getType();

    @Override
    public String toString() {
        return super.toString();
    }

    public abstract int getRegNum();

    public abstract String getName();
}
