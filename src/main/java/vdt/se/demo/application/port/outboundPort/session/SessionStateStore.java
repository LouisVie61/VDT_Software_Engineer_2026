package vdt.se.demo.application.port.outboundPort.session;

import vdt.se.demo.domain.iql.SessionState;
import java.util.Optional;

public interface SessionStateStore {
    Optional<SessionState> load(String sessionId);
    void save(String sessionId, SessionState state);
}
