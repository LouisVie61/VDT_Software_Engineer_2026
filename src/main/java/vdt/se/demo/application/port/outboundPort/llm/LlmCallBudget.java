package vdt.se.demo.application.port.outboundPort.llm;

public final class LlmCallBudget {
    private int remaining;

    public LlmCallBudget(int maximumCalls) {
        if (maximumCalls < 1) throw new IllegalArgumentException("maximumCalls must be positive");
        this.remaining = maximumCalls;
    }

    public boolean tryConsume() {
        if (remaining == 0) return false;
        remaining--;
        return true;
    }

    public boolean hasRemaining() { return remaining > 0; }
}
