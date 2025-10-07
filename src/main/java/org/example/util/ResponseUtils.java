package org.example.util;

public class ResponseUtils {

    public static String extractFirstJsonBlock(String text) {
        if (text == null) return null;

        int fenceStart = text.indexOf("```");
        if (fenceStart >= 0) {
            int fenceEnd = text.indexOf("```", fenceStart + 3);
            if (fenceEnd > fenceStart) {
                String block = text.substring(fenceStart + 3, fenceEnd).trim();
                if (block.startsWith("json")) {
                    block = block.substring(4).trim();
                }
                return block;
            }
        }

        int objStart = text.indexOf('{');
        int objEnd = text.lastIndexOf('}');
        if (objStart >= 0 && objEnd > objStart) {
            return text.substring(objStart, objEnd + 1).trim();
        }
        return null;
    }
}
