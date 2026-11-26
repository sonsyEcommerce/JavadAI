
package com.javadai.agent.skills;

import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
public class SkillRegistry {

    private final List<Skill> skills;

    public SkillRegistry(List<Skill> skills) {
        this.skills = List.copyOf(skills);
    }

    public Optional<Skill> findSkill(String instruction) {
        return skills.stream()
                .filter(skill -> skill.supports(instruction))
                .findFirst();
    }

    public List<String> getAvailableSkills() {
        return skills.stream()
                .map(Skill::getName)
                .toList();
    }
}
