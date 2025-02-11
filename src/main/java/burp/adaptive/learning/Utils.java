package burp.adaptive.learning;

import burp.api.montoya.http.handler.HttpRequestToBeSent;
import burp.api.montoya.http.message.HttpRequestResponse;
import burp.api.montoya.http.message.params.HttpParameter;
import burp.api.montoya.http.message.params.HttpParameterType;
import burp.api.montoya.http.message.requests.HttpRequest;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.regex.Pattern;

import static burp.adaptive.learning.LearningExtension.*;
import static burp.adaptive.learning.LearningExtension.responseHistory;

public class Utils {
    public static HttpRequest modifyRequest(HttpRequestToBeSent req, String type, String name, String value) {
        return switch (type) {
            case "header" -> req.withRemovedHeader(name).withAddedHeader(name, value);
            case "URL", "BODY", "COOKIE", "JSON" -> {
                if (type.equals("URL")) {
                    value = api.utilities().urlUtils().encode(value);
                }
                yield req.withUpdatedParameters(HttpParameter.parameter(name, value, HttpParameterType.valueOf(type)));
            }
            default -> req;
        };
    }
    public static String randomString(int length) throws IllegalArgumentException {
        String CHARACTERS = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
        SecureRandom RANDOM = new SecureRandom();
        if (length <= 0) {
            throw new IllegalArgumentException("Length must be greater than 0");
        }
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            sb.append(CHARACTERS.charAt(RANDOM.nextInt(CHARACTERS.length())));
        }

        return sb.toString();
    }
    public static HashMap<String, Object> calculateFingerprint(HttpRequestResponse resp) {
        String[] keys = new String[]{"\",\"", "true", "false", "\"\"", "[]", "</html>", "error", "exception", "invalid", "warning", "stack", "sql syntax", "divisor", "divide", "ora-", "division", "infinity", "<script", "<div"};
        String[] newLines = new String[]{"\r" ,"\n", "\u2028", "\u2029"};
        String[] spaces = new String[]{" ", "\t", "\u00a0"};
        HashMap<String, Object> fingerprint = new HashMap<>();
        if (!resp.hasResponse()) {
            fingerprint.put("status", 0);
            return fingerprint;
        }
        String body = resp.response().bodyToString().toLowerCase();
        fingerprint.put("status", resp.response().statusCode());
        fingerprint.put("title", getTitle(body));
        for (String key : keys) {
            fingerprint.put(key, countString(body, key));
        }
        int newLineCount = 0;
        for (String newLine : newLines) {
            newLineCount += countString(body, newLine);
        }
        fingerprint.put("New lines", newLineCount);
        int spaceCount = 0;
        for (String space : spaces) {
            spaceCount += countString(body, space);
        }
        fingerprint.put("Spaces", spaceCount);
        return fingerprint;
    }

    static int countString(String input, String find) {
        return input.split(Pattern.quote(find), -1).length - 1;
    }

    static String getTitle(String body) {
        try {
            if (!body.contains("<title")) {
                return "";
            }
            return body.split("<title", 1)[1].split(">", 1)[1].split("<")[0];
        } catch (Exception e) {
            return "";
        }
    }
    public static void resetHistory() {
        requestHistoryPos = 1;
        requestHistory = new ArrayList<>();
        responseHistory = new ArrayList<>();
    }
}
