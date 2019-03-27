package ivchain.capability;

public class ChainingPlayer implements IChainTracker {
    private String chainName = "";
    private byte chainValue = 0;

    @Override
    public String getChainName() {
        return chainName;
    }

    @Override
    public void setChainName(String name) {
        chainName = name;
    }

    @Override
    public byte getChainValue() {
        return chainValue;
    }

    @Override
    public void incrementChainValue(boolean reset) {
        if (reset)
            chainValue = 1;
        else if (chainValue < 30)
            chainValue++;
    }

    @Override
    public void setChainValue(byte value) {
        chainValue = (byte)Math.max(0, value);
    }
}
