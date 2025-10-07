package org.example.util;

import org.example.model.Question;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class SurveyJsonParser {

    public static List<Question> parseQuestions(String json) {
        if (json == null) return Collections.emptyList();
        String array = extractQuestionsArray(json);
        if (array == null) return Collections.emptyList();

        List<String> objs = splitTopLevelObjects(array);
        List<Question> out = new ArrayList<>();

        for (String obj : objs) {
            String text = extractStringValue(obj, "text");
            List<String> options = extractStringArray(obj, "options");
            if (text != null && options != null && options.size() >= 2 && options.size() <= 4) {
                out.add(new Question(text, options));
            }
        }

        if (out.size() > 3) return new ArrayList<>(out.subList(0, 3));
        return out;
    }

    private static String extractQuestionsArray(String json) {
        int key = json.indexOf("\"questions\"");
        if (key < 0) return null;

        int bracketStart = json.indexOf('[', key);
        if (bracketStart < 0) return null;

        boolean inString = false, escape = false;
        int depth = 0;

        for (int i = bracketStart; i < json.length(); i++) {
            char c = json.charAt(i);
            if (inString) {
                if (escape) { escape = false; }
                else if (c == '\\') { escape = true; }
                else if (c == '"') { inString = false; }
            } else {
                if (c == '"') { inString = true; }
                else if (c == '[') depth++;
                else if (c == ']') {
                    depth--;
                    if (depth == 0) {
                        return json.substring(bracketStart, i + 1);
                    }
                }
            }
        }
        return null;
    }

    private static List<String> splitTopLevelObjects(String arrayWithBrackets) {
        List<String> list = new ArrayList<>();
        int i = 0;
        if (arrayWithBrackets.length() >= 2 && arrayWithBrackets.charAt(0) == '[') i = 1;
        int end = arrayWithBrackets.length() - 1;

        boolean inString = false, escape = false;
        int depth = 0, start = -1;

        for (; i < end; i++) {
            char c = arrayWithBrackets.charAt(i);
            if (inString) {
                if (escape) { escape = false; }
                else if (c == '\\') { escape = true; }
                else if (c == '"') { inString = false; }
            } else {
                if (c == '"') { inString = true; }
                else if (c == '{') {
                    if (depth == 0) start = i;
                    depth++;
                } else if (c == '}') {
                    depth--;
                    if (depth == 0 && start != -1) {
                        list.add(arrayWithBrackets.substring(start, i + 1));
                        start = -1;
                    }
                }
            }
        }
        return list;
    }

    private static String extractStringValue(String obj, String key) {
        String k = "\"" + key + "\"";
        int idx = obj.indexOf(k);
        if (idx < 0) return null;

        int colon = obj.indexOf(':', idx + k.length());
        if (colon < 0) return null;

        int i = colon + 1;
        while (i < obj.length() && Character.isWhitespace(obj.charAt(i))) i++;
        if (i >= obj.length() || obj.charAt(i) != '"') return null;
        i++;

        StringBuilder sb = new StringBuilder();
        boolean escape = false;
        for (; i < obj.length(); i++) {
            char c = obj.charAt(i);
            if (escape) { sb.append(c); escape = false; }
            else if (c == '\\') { escape = true; }
            else if (c == '"') { break; }
            else { sb.append(c); }
        }
        return sb.toString();
    }

    private static List<String> extractStringArray(String obj, String key) {
        String k = "\"" + key + "\"";
        int idx = obj.indexOf(k);
        if (idx < 0) return null;

        int bracketStart = obj.indexOf('[', idx + k.length());
        if (bracketStart < 0) return null;

        boolean inString = false, escape = false;
        int depth = 0, end = -1;

        for (int i = bracketStart; i < obj.length(); i++) {
            char c = obj.charAt(i);
            if (inString) {
                if (escape) { escape = false; }
                else if (c == '\\') { escape = true; }
                else if (c == '"') { inString = false; }
            } else {
                if (c == '"') { inString = true; }
                else if (c == '[') depth++;
                else if (c == ']') {
                    depth--;
                    if (depth == 0) { end = i; break; }
                }
            }
        }
        if (end == -1) return null;

        String inner = obj.substring(bracketStart + 1, end);
        List<String> out = new ArrayList<>();
        boolean collecting = false;
        StringBuilder sb = new StringBuilder();
        escape = false;

        for (int j = 0; j < inner.length(); j++) {
            char c = inner.charAt(j);
            if (!collecting) {
                if (c == '"') { collecting = true; sb.setLength(0); }
            } else {
                if (escape) { sb.append(c); escape = false; }
                else if (c == '\\') { escape = true; }
                else if (c == '"') { out.add(sb.toString()); collecting = false; }
                else { sb.append(c); }
            }
        }
        return out;
    }
}
