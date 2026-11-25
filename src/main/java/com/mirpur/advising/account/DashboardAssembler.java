package com.mirpur.advising.account;

import java.util.Map;

public interface DashboardAssembler {
    String role();
    Object assemble(Map < String, Object > user);
}
