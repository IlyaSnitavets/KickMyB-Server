package org.kickmyb.server;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.kickmyb.server.account.MUser;
import org.kickmyb.server.account.MUserRepository;
import org.kickmyb.server.task.MTask;
import org.kickmyb.server.task.MTaskRepository;
import org.kickmyb.server.task.ServiceTask;
import org.kickmyb.transfer.AddTaskRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.Date;
import java.util.Optional;

import static org.assertj.core.api.Fail.fail;
import static org.junit.jupiter.api.Assertions.assertEquals;

// TODO pour celui ci on aimerait pouvoir mocker l'utilisateur pour ne pas avoir à le créer

// https://reflectoring.io/spring-boot-mock/#:~:text=This%20is%20easily%20done%20by,our%20controller%20can%20use%20it.

@ExtendWith(SpringExtension.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT, classes = KickMyBServerApplication.class)
@TestPropertySource(locations = "classpath:application-test.properties")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
//@ActiveProfiles("test")
class ServiceTaskTests {


    @Autowired
    MUserRepository repoUser;

    @Autowired
    MTaskRepository repoTask;
    @Autowired
    private MUserRepository userRepository;
    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private ServiceTask serviceTask;

    @Test
    void testAddTask() throws ServiceTask.Empty, ServiceTask.TooShort, ServiceTask.Existing {
        MUser u = new MUser();
        u.username = "M. Test";
        u.password = passwordEncoder.encode("Passw0rd!");
        userRepository.saveAndFlush(u);

        AddTaskRequest atr = new AddTaskRequest();
        atr.name = "Tâche de test";
        atr.deadline = Date.from(new Date().toInstant().plusSeconds(3600));

        serviceTask.addOne(atr, u);

        assertEquals(1, serviceTask.home(u.id).size());
    }

    @Test
    void testAddTaskEmpty()  {
        MUser u = new MUser();
        u.username = "M. Test";
        u.password = passwordEncoder.encode("Passw0rd!");
        userRepository.saveAndFlush(u);

        AddTaskRequest atr = new AddTaskRequest();
        atr.name = "";
        atr.deadline = Date.from(new Date().toInstant().plusSeconds(3600));

        try{
            serviceTask.addOne(atr, u);
            fail("Aurait du lancer ServiceTask.Empty");
        } catch (Exception e) {
            assertEquals(ServiceTask.Empty.class, e.getClass());
        }
    }

    @Test
    void testAddTaskTooShort() throws ServiceTask.Empty, ServiceTask.TooShort, ServiceTask.Existing {
        MUser u = new MUser();
        u.username = "M. Test";
        u.password = passwordEncoder.encode("Passw0rd!");
        userRepository.saveAndFlush(u);

        AddTaskRequest atr = new AddTaskRequest();
        atr.name = "o";
        atr.deadline = Date.from(new Date().toInstant().plusSeconds(3600));

        try{
            serviceTask.addOne(atr, u);
            fail("Aurait du lancer ServiceTask.TooShort");
        } catch (Exception e) {
            assertEquals(ServiceTask.TooShort.class, e.getClass());
        }
    }

    @Test
    void testAddTaskExisting() throws ServiceTask.Empty, ServiceTask.TooShort, ServiceTask.Existing {
        MUser u = new MUser();
        u.username = "M. Test";
        u.password = passwordEncoder.encode("Passw0rd!");
        userRepository.saveAndFlush(u);

        AddTaskRequest atr = new AddTaskRequest();
        atr.name = "Bonne tâche";
        atr.deadline = Date.from(new Date().toInstant().plusSeconds(3600));

        try{
            serviceTask.addOne(atr, u);
            serviceTask.addOne(atr, u);
            fail("Aurait du lancer ServiceTask.Existing");
        } catch (Exception e) {
            assertEquals(ServiceTask.Existing.class, e.getClass());
        }
    }


    @Test
    public void deleteTaskwithOKID() throws Exception {
        MUser user = new MUser();
        user.username = "test1";
        user.password = "pass123";
        repoUser.save(user);

        MTask task = new MTask();
        task.name = "tâche à supprimer";
        task.creationDate = new Date();
        task.deadline = new Date(System.currentTimeMillis());
        repoTask.save(task);
        user.tasks.add(task);
        repoUser.save(user);

        MUser refreshed = repoUser.findByUsername("test1").get();
        Assertions.assertEquals(1, refreshed.tasks.size());

        serviceTask.deleteTask(task.id, refreshed);

        Optional<MTask> deleted = repoTask.findById(task.id);
        Assertions.assertTrue(deleted.isEmpty());
    }

    @Test
    public void deleteTask_withInvalidId_shouldThrow() {
        MUser user = new MUser();
        user.username = "test2";
        user.password = "pass123";
        repoUser.save(user);

        Assertions.assertThrows(Exception.class, () -> {
            serviceTask.deleteTask(9999L, user);
        });
    }
    @Test
    public void deleteTask_byAnotherUser_shouldThrow() {
        MUser alice = new MUser();
        alice.username = "alice";
        alice.password = "pass";
        repoUser.save(alice);

        MTask task = new MTask();
        task.name = "tâche d'Alice";
        task.creationDate = new Date();
        task.deadline = new Date(System.currentTimeMillis());
        repoTask.save(task);
        alice.tasks.add(task);
        repoUser.save(alice);

        MUser bob = new MUser();
        bob.username = "bob";
        bob.password = "pass";
        repoUser.save(bob);

        Assertions.assertThrows(IllegalAccessException.class, () -> {
            serviceTask.deleteTask(task.id, bob);
        });
    }
}
