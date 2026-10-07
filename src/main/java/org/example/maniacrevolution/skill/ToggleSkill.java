package org.example.maniacrevolution.skill;

import java.util.List;

/** Equipment-bound skill: exactly one persistent state, with no replaceable perk slot. */
public record ToggleSkill<S>(String id, List<S> states) {
    public ToggleSkill {
        states = List.copyOf(states);
        if (states.size() < 2 || states.stream().distinct().count() != states.size())
            throw new IllegalArgumentException("A toggle skill needs distinct states");
    }
    public S initial() { return states.get(0); }
    public S next(S current) {
        int index = states.indexOf(current);
        if (index < 0) throw new IllegalArgumentException("Unknown stance");
        return states.get((index + 1) % states.size());
    }
}
