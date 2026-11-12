package com.dozamigos.dozamigos.sync;

import java.util.List;

public record UpdatePlan(String packVersion, List<ModChange> changes) {
    public boolean isEmpty() {
        return changes == null || changes.isEmpty();
    }

    public long count(ChangeKind kind) {
        return changes.stream().filter(c -> c.kind() == kind).count();
    }

    public long count(ChangeKind kind, FileKind target) {
        return changes.stream().filter(c -> c.kind() == kind && c.target() == target).count();
    }
}
