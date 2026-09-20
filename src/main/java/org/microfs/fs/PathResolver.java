package org.microfs.fs;

import java.util.*;

/**
 * Path resolver supporting absolute, relative, . and ..
 */
public class PathResolver {

    public static List<String> tokenize(String path) {
        if (path == null || path.isEmpty()) return Collections.singletonList("");
        String[] parts = path.split("/");
        List<String> tokens = new ArrayList<>();
        for (String part : parts) {
            if (part.isEmpty() || part.equals(".")) {
                // skip
            } else if (part.equals("..")) {
                tokens.remove(tokens.isEmpty() ? -1 : tokens.size() - 1);
            } else {
                tokens.add(part);
            }
        }
        return tokens;
    }

    public static String canonicalize(String currentPath, String targetPath) {
        List<String> current = tokenize(currentPath);
        List<String> target = tokenize(targetPath);
        List<String> result = new ArrayList<>(current);
        if (targetPath.startsWith("/")) {
            result.clear();
        }
        for (String token : target) {
            if (token.equals(".")) {
                // ignore
            } else if (token.equals("..")) {
                if (!result.isEmpty() && !(result.size() == 1 && result.get(0).isEmpty())) {
                    result.remove(result.size() - 1);
                }
            } else {
                result.add(token);
            }
        }
        return "/" + String.join("/", result);
    }
}
