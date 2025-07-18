package io.hexlet.typoreporter.web;

import com.github.database.rider.core.api.configuration.DBUnit;
import com.github.database.rider.spring.api.DBRider;
import io.hexlet.typoreporter.repository.AccountRepository;
import io.hexlet.typoreporter.test.DBUnitEnumPostgres;
import io.hexlet.typoreporter.web.model.SignupAccountModel;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.support.ResourceBundleMessageSource;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static com.github.database.rider.core.api.configuration.Orthography.LOWERCASE;
import static io.hexlet.typoreporter.test.Constraints.POSTGRES_IMAGE;
import static io.hexlet.typoreporter.test.factory.EntitiesFactory.ACCOUNT_INCORRECT_EMAIL;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
@Transactional
@DBRider
@DBUnit(caseInsensitiveStrategy = LOWERCASE, dataTypeFactoryClass = DBUnitEnumPostgres.class, cacheConnection = false)
class SignupControllerIT {

    @Container
    static PostgreSQLContainer<?> postgreSQLContainer = new PostgreSQLContainer<>(POSTGRES_IMAGE)
        .withPassword("inmemory")
        .withUsername("inmemory");

    @DynamicPropertySource
    static void datasourceProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgreSQLContainer::getJdbcUrl);
        registry.add("spring.datasource.password", postgreSQLContainer::getPassword);
        registry.add("spring.datasource.username", postgreSQLContainer::getUsername);
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private AccountRepository accountRepository;

    private static final String EMAIL_UPPER_CASE = "EMAIL_ADDRESS@GOOGLE.COM";
    private static final String EMAIL_LOWER_CASE = EMAIL_UPPER_CASE.toLowerCase();

    private final SignupAccountModel model = new SignupAccountModel(
        "model_upper_case",
        EMAIL_UPPER_CASE,
        "password", "password",
        "firstName", "lastName",
        "EMAIL");

    private final SignupAccountModel anotherModelWithSameButLowerCaseEmail = new SignupAccountModel(
        "model_lower_case",
        EMAIL_LOWER_CASE,
        "another_password", "another_password",
        "another_firstName", "another_lastName",
        "EMAIL");

    private static ResourceBundleMessageSource source;

    @BeforeAll
    static void init() {
        source = new ResourceBundleMessageSource();
        source.setBasename("messages_en");
    }

    @Test
    void createAccountWithIgnoreEmailCase() throws Exception {
        assertThat(accountRepository.count()).isEqualTo(0L);

        mockMvc.perform(post("/signup")
            .param("username", model.getUsername())
            .param("email", model.getEmail())
            .param("password", model.getPassword())
            .param("confirmPassword", model.getConfirmPassword())
            .param("firstName", model.getFirstName())
            .param("lastName", model.getLastName())
            .with(csrf()));
        assertThat(accountRepository.findAccountByEmail(EMAIL_UPPER_CASE)).isEmpty();
        assertThat(accountRepository.findAccountByEmail(EMAIL_LOWER_CASE)).isNotEmpty();
        assertThat(accountRepository.count()).isEqualTo(1L);

        mockMvc.perform(post("/signup")
            .param("username", anotherModelWithSameButLowerCaseEmail.getUsername())
            .param("email", anotherModelWithSameButLowerCaseEmail.getEmail())
            .param("password", anotherModelWithSameButLowerCaseEmail.getPassword())
            .param("confirmPassword", anotherModelWithSameButLowerCaseEmail.getConfirmPassword())
            .param("firstName", anotherModelWithSameButLowerCaseEmail.getFirstName())
            .param("lastName", anotherModelWithSameButLowerCaseEmail.getLastName())
            .with(csrf()));
        assertThat(accountRepository.count()).isEqualTo(1L);
    }

    @Test
    void createAccountWithEmptyNames() throws Exception {
        String username = "testEmptyNamesUser";
        String email = "testemptynames@test.ru";
        String password = "P@$$w0rd";
        String emptyName = "";
        mockMvc.perform(post("/signup")
                .param("username", username)
                .param("email", email)
                .param("password", password)
                .param("confirmPassword", password)
                .param("firstName", emptyName)
                .param("lastName", emptyName)
                .with(csrf()))
            .andReturn();
        assertThat(accountRepository.findAccountByEmail(email)).isNotEmpty();
    }

    @Test
    void createAccountWithWrongEmailDomain() throws Exception {
        String userName = "testUser";
        String password = "_Qwe1234";
        String wrongEmailDomain = "test@test";
        mockMvc.perform(post("/signup")
            .param("username", userName)
            .param("email", wrongEmailDomain)
            .param("password", password)
            .param("confirmPassword", password)
            .param("firstName", userName)
            .param("lastName", userName)
            .with(csrf()));
        assertThat(accountRepository.findAccountByEmail(wrongEmailDomain)).isEmpty();
    }

    void createAccountWithWrongPassword() throws Exception {
        String userName = "testUser";
        String wrongPass = "pass";
        String email = "test@test.ru";
        mockMvc.perform(post("/signup")
            .param("username", userName)
            .param("email", email)
            .param("password", wrongPass)
            .param("confirmPassword", wrongPass)
            .param("firstName", userName)
            .param("lastName", userName)
            .with(csrf())).andExpect((status().isFound()));
        assertThat(accountRepository.findAccountByEmail(email)).isEmpty();

    }

    @Test
    void signupInAccountWithBadEmail() throws Exception {
        model.setEmail(ACCOUNT_INCORRECT_EMAIL);
        var response = mockMvc.perform(post("/signup")
                .param("username", model.getUsername())
                .param("email", model.getEmail())
                .param("password", model.getPassword())
                .param("confirmPassword", model.getConfirmPassword())
                .param("firstName", model.getFirstName())
                .param("lastName", model.getLastName())
                .with(csrf()))
            .andReturn();
        var body = response.getResponse().getContentAsString();
        assertThat(body).contains(String.format("The email &quot;%s&quot; is not valid", ACCOUNT_INCORRECT_EMAIL));
    }

    @Test
    void signupInPasswordsDontMatch() throws Exception {
        model.setConfirmPassword("WrongPassword123");
        var response = mockMvc.perform(post("/signup")
                .param("username", model.getUsername())
                .param("email", model.getEmail())
                .param("password", model.getPassword())
                .param("confirmPassword", model.getConfirmPassword())
                .param("firstName", model.getFirstName())
                .param("lastName", model.getLastName())
                .with(csrf()))
            .andReturn();
        var body = response.getResponse().getContentAsString();
        var result = accountRepository.findAccountByEmail(model.getEmail().toLowerCase());

        assertThat(body).contains(source.getMessage("alert.passwords-dont-match", null, null));
        assertThat(result).isEmpty();

    }

}
