package org.gooru.nucleus.handlers.classes.processors.repositories.activejdbc;

import java.util.Iterator;
import java.util.List;

/**
 * Created by ashish on 4/3/16.
 */
public final class Utils {

    public static String convertListToPostgresArrayStringRepresentation(List<String> input) {
        int approxSize = ((input.size() + 1) * 36); // Length of UUID is around
                                                    // 36 chars
        Iterator<String> it = input.iterator();
        if (!it.hasNext()) {
            return "{}";
        }

        StringBuilder sb = new StringBuilder(approxSize);
        sb.append('{');
        for (;;) {
            String s = it.next();
            sb.append('"').append(s).append('"');
            if (!it.hasNext()) {
                return sb.append('}').toString();
            }
            sb.append(',');
        }

    }

    private Utils() {
        throw new AssertionError();
    }
}
