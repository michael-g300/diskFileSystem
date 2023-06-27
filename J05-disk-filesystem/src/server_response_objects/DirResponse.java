package server_response_objects;

import java.util.List;

public record DirResponse(String result, List<String> files) {
}
