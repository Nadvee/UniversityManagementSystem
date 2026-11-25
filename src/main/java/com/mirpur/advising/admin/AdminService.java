package com.mirpur.advising.admin;

import com.mirpur.advising.advising.AdvisingControlService;

import com.mirpur.advising.account.AccountCreator;
import java.util.*;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service public class AdminService {
    private final Map < String, AccountCreator > creators;
    private final OfferingService offerings;
    private final AssignmentService assignments;
    private final AdvisingControlService windows;
    public AdminService(List < AccountCreator > creators, OfferingService offerings, AssignmentService assignments, AdvisingControlService windows) {
        this.creators = creators.stream().collect(Collectors.toUnmodifiableMap(AccountCreator::role, x -> x));
        this.offerings = offerings;
        this.assignments = assignments;
        this.windows = windows;
    }
    @Transactional public Object account(long actor, String role, Map < String, Object > body) {
        return creators.get(role).create(actor, body);
    }
    public Object offering(long actor, Long id, Map < String, Object > body) {
        return offerings.offering(actor, id, body);
    }
    public Object offeringStatus(long actor, long id, boolean offered) {
        return offerings.offeringStatus(actor, id, offered);
    }
    public Object assign(long actor, long choice, long section) {
        return assignments.assign(actor, choice, section);
    }
    public Object window(long actor, Map < String, Object > body) {
        return windows.window(actor, body);
    }
    public Object setWindow(long actor, long id, boolean open) {
        return windows.setWindow(actor, id, open);
    }
    public void deleteWindow(long actor, long id) {
        windows.deleteWindow(actor, id);
    }
}
