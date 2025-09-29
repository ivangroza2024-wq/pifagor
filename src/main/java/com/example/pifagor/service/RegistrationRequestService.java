package com.example.pifagor.service;

import com.example.pifagor.model.Group;
import com.example.pifagor.model.RegistrationRequest;
import com.example.pifagor.model.Role;
import com.example.pifagor.model.User;
import com.example.pifagor.repository.RegistrationRequestRepository;
import com.example.pifagor.repository.GroupRepository;
import com.example.pifagor.repository.UserRepository;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class RegistrationRequestService {

    private final RegistrationRequestRepository repository;
    private final GroupRepository groupRepository;
    private final UserRepository userRepository;

    // тимчасові дані користувачів
    private final Map<Long, String> roleMap = new HashMap<>();
    private final Map<Long, Long> groupMap = new HashMap<>();
    private final Map<Long, Long> childMap = new HashMap<>(); // ✅ нова мапа для вибраної дитини

    public RegistrationRequestService(RegistrationRequestRepository repository,
                                      GroupRepository groupRepository,
                                      UserRepository userRepository) {
        this.repository = repository;
        this.groupRepository = groupRepository;
        this.userRepository = userRepository;
    }

    public void saveRole(Long telegramId, String role) {
        roleMap.put(telegramId, role);
    }

    public String getRole(Long telegramId) {
        return roleMap.getOrDefault(telegramId, "unknown");
    }

    public void savePendingGroup(Long telegramId, Long groupId) {
        groupMap.put(telegramId, groupId);
    }

    public Long getPendingGroup(Long telegramId) {
        return groupMap.get(telegramId);
    }

    // ✅ зберігаємо обраного учня для батьків
    public void savePendingChild(Long telegramId, Long childId) {
        childMap.put(telegramId, childId);
    }

    public Long getPendingChild(Long telegramId) {
        return childMap.get(telegramId);
    }

    /** Створюємо заявку після вибору (дитини або ПІБ) */
    public RegistrationRequest createRequest(Long telegramId, String fullName) {
        String role = getRole(telegramId);
        Long groupId = getPendingGroup(telegramId);

        if (role.equals("unknown") || groupId == null) {
            throw new IllegalStateException("Не вибрано роль або групу");
        }

        RegistrationRequest req = new RegistrationRequest();
        req.setTelegramId(telegramId);
        req.setGroupId(groupId);
        req.setRequestedRole(role.equals("child") ? Role.STUDENT : Role.PARENT);
        req.setApproved(false);

        if ("child".equals(role)) {
            // дитина вводить своє ПІБ
            req.setName(fullName);
            // якщо батько вже обрав цю дитину, зберігаємо Telegram ID батька
            Long parentId = getPendingChild(telegramId); // якщо у тебе логіка де parentId зберігається тут
            if (parentId != null) {
                req.setId(parentId);
            }
        } else if ("parent".equals(role)) {
            // батьки обрали дитину
            Long childId = getPendingChild(telegramId);
            req.setName(fullName); // ПІБ батьків
            req.setPendingChildId(childId); // посилання на дитину
        }


        return repository.save(req);
    }

    public List<RegistrationRequest> getPendingRequests() {
        return repository.findByApprovedFalse();
    }

    /** Підтвердження заявки + створення користувача */
    public RegistrationRequest approve(Long requestId) {
        // 1️⃣ Знаходимо заявку
        RegistrationRequest req = repository.findById(requestId)
                .orElseThrow(() -> new RuntimeException("Request not found"));
        req.setApproved(true);
        repository.save(req);

        // 2️⃣ Перевіряємо, чи вже є користувач із цим telegramId
        User existingUser = userRepository.findByTelegramId(req.getTelegramId());
        if (existingUser == null) {
            // 3️⃣ Створюємо нового користувача
            User user = User.builder()
                    .telegramId(req.getTelegramId())
                    .name(req.getName())
                    .role(req.getRequestedRole())
                    .group(groupRepository.findById(req.getGroupId()).orElse(null))
                    .approved(true)
                    .build();

            // 4️⃣ Якщо це батьки — прив'язуємо дитину
            if (req.getRequestedRole() == Role.PARENT && req.getPendingChildId() != null) {
                User child = userRepository.findById(req.getPendingChildId())
                        .orElseThrow(() -> new IllegalStateException("Учень не знайдений"));

                user.setChild(child);                  // батько отримує child
                child.setParentTelegramId(user.getTelegramId()); // дитина отримує Telegram ID батька
                userRepository.save(child);            // зберігаємо дитину
            }

            // 5️⃣ Зберігаємо батька або дитину
            userRepository.save(user);
        }

        return req;
    }


    public void reject(Long requestId) {
        repository.deleteById(requestId);
    }

    public List<RegistrationRequest> getConfirmedChildrenByGroup(Long groupId) {
        return repository.findByGroupIdAndApprovedTrue(groupId);
    }

    public String getGroupName(Long groupId) {
        return groupRepository.findById(groupId)
                .map(Group::getName)
                .orElse("Невідома група");
    }

    /** Перевірка чи користувач підтверджений */
    public boolean isApproved(Long telegramUserId) {
        User user = userRepository.findByTelegramId(telegramUserId);
        return user != null && user.isApproved();
    }
}
