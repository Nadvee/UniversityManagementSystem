package com.mirpur.advising;

import com.mirpur.advising.account.AccountCreator;
import com.mirpur.advising.account.StudentAccountCreator;
import com.mirpur.advising.account.TeacherAccountCreator;
import com.mirpur.advising.shared.config.RuntimeSettings;
import com.mirpur.advising.admin.AdminRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.junit.jupiter.api.Test;
import java.util.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;
import static org.assertj.core.api.Assertions.*;

class StrategyContractTests {
    @Test void bothAccountCreatorsRequireInitialPasswordSetup() {
        var repository=mock(AdminRepository.class);
        var encoder=mock(PasswordEncoder.class);
        when(repository.accountDepartmentsList(any())).thenReturn(List.of(Map.of("id",1L)));
        when(repository.accountUsersInsert(any(),any(),any(),any())).thenReturn(10L);
        when(encoder.encode(anyString())).thenReturn("hashed-password");
        List<AccountCreator> creators=List.of(new StudentAccountCreator(repository,encoder),new TeacherAccountCreator(repository,encoder));
        for(var creator:creators){
            String login=creator.role().equals("STUDENT")?"S001":"T001";
            var result=creator.create(1,Map.of("loginId",login,"name","Name","subject","CSE","departmentCode","CSE"));
            assertThat(result.get("id")).isEqualTo(10L);
            verify(repository).accountUsersInsert(login,"hashed-password",creator.role(),"Name");
        }
        verify(encoder,times(2)).encode(anyString());
        verify(encoder,never()).encode("1234");
        verify(repository,times(2)).requirePasswordSetup(10L);
    }

}
