package com.example.pifagor.service;

import com.example.pifagor.model.Group;
import com.example.pifagor.repository.GroupRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class GroupService {

    private final GroupRepository repository;

    public GroupService(GroupRepository repository) {
        this.repository = repository;
    }

    public List<Group> getAllGroups() {
        return repository.findAll();
    }

    public Group getByName(String name) {
        return repository.findByName(name);
    }

    public Group save(Group group) {
        // формуємо поле name автоматично
        group.setName(group.getDay1() + group.getTime1() + " " + group.getDay2() + group.getTime2());
        return repository.save(group);
    }
}
