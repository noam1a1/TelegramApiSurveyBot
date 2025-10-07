package org.example.model;

public class ChatGPTResponse {
    private final boolean success;
    private final String errorCode;
    private final String extra;

    public ChatGPTResponse(boolean success, String errorCode, String extra) {
        this.success = success;
        this.errorCode = (errorCode != null && !errorCode.isBlank()) ? errorCode : null;
        this.extra = (extra != null && !extra.isBlank()) ? extra : null;
    }

    public boolean isSuccess() { return success; }
    public String getErrorCode() { return errorCode; }
    public String getExtra() { return extra; }

    @Override
    public String toString() {
        return "ChatGPTResponse{" +
                "success=" + success +
                ", errorCode='" + errorCode + '\'' +
                ", extra='" + extra + '\'' +
                '}';
    }
}
