package selkie.host;

import com.fasterxml.jackson.databind.ObjectMapper;

/** The one JSON mapper the data layer shares (thread-safe once configured). */
final class Json {

    static final ObjectMapper MAPPER = new ObjectMapper();

    private Json() {
    }
}
