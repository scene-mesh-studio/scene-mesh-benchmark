package com.scene.mesh.benchmark.n.model;

import com.scene.mesh.sdk.model.TerminalEvent;
import lombok.Data;

import java.util.List;

@Data
public class TestSuite {

    private String id;
    private String name;

    private String productId;
    private String secretKey;
    private String terminalId;
    private String protocol;

    List<TerminalEvent> terminalEvents;
    Long durationOfWaitingActions;
    List<String> expectedActionIds;
}
